package chainsaw.projects.xdma.daq

import chainsaw.projects.xdma.daq.customizedIps.{DataDelay, DataDelayConfig}
import chainsaw.projects.xdma.daq.ku060Ips._
import spinal.core._
import spinal.core.sim.SimDataPimper
import spinal.lib.{Fragment, _}

import scala.collection.Seq
import scala.language.postfixOps
import scala.math

// TODO: 在没有特殊字段的前提下,blackbox中的Axi4Stream应当被提取为Stream或Flow,而不是完整的Axi4Stream
// TODO: optimize CORDIC parameters,包括输入输出位数,iteration数量

case class ComponentDemodulator(carrierFreq: HertzNumber) extends Module {

  val OUTPUT_WIDTH = 32
  val OUTPUT_FRACTIONAL_WIDTH = 20
  val OUTPUT_STRAIN_RESOLUTION = 0.025 / 0.23 / (1 << 20) // 0.23rad <-> 0.025με / gauge length
  println(s"strain/gauge length resolution = ${OUTPUT_STRAIN_RESOLUTION * 1e12}pε/m")

  def getSlice(length: Int, index: Int) = (index + 1) * length - 1 downto index * length

  val vectorWidth = 2

  val streamIn = slave Stream Fragment(Vec(SInt(16 bits), 4)) // earlier data in lower index
  val streamOut = master Stream Fragment(Bits(64 bits)) // earlier data in lower bits

  val channel0 = streamIn.fragment.take(vectorWidth)
  val channel1 = streamIn.fragment.takeRight(vectorWidth)

  //////////
  // parameter preparation
  //////////
  val DDS_MODULUS = 250 // using rasterized mode
  val PHASE_WIDTH = log2Up(DDS_MODULUS)
  val DDS_OUTPUT_WIDTH = 16
  val SAMPLING_FREQ = 250e6

  def ddsSynth(freq: Double) = {
    def round2even(x: Double) = {
      val a = math.ceil(x)
      val b = math.floor(x)
      (if (a.toInt % 2 == 0) a else b).toInt
    }
    val phaseInc = round2even(freq * (1 << PHASE_WIDTH) / SAMPLING_FREQ)
    val actualFreq = phaseInc * SAMPLING_FREQ / (1 << PHASE_WIDTH)
    val bias = math.abs(freq - actualFreq)
    println(s"inc = $phaseInc, bias = $bias Hz")
    phaseInc
  }

  def getPhaseData(resync: Bool, inc: Int, offset: Int) = {
    val dataFieldSize = math.ceil(PHASE_WIDTH.toDouble / 8).toInt * 8
    B(0, 7 bits) ## resync ## B(offset, dataFieldSize bits) ## B(inc, dataFieldSize bits)
  }

  val frameDone = RegInit(True)
  when(streamIn.last && streamIn.valid)(frameDone.set()).elsewhen(streamIn.valid)(frameDone.clear())
  val resync = frameDone && streamIn.valid
  val phaseInc = (carrierFreq.toDouble * DDS_MODULUS / SAMPLING_FREQ).toInt
  val phaseOffsets = Seq(0, phaseInc / 2)
  assert(phaseInc % 2 == 0)

  //////////
  // stream datapath
  //////////

  //////////
  // step 1: get synced carrier(fork-queue-join structure)
  //////////
  val Seq(streamRawData, streamForCarrier) = StreamFork(streamIn, 2) // fork
  // streamForCarrier -> DDS -> streamCarrier
  streamForCarrier.ready.allowOverride()
  val dds0, dds1 = DdsCompiler()
  streamForCarrier.translateFragmentWith(getPhaseData(resync, phaseInc, phaseOffsets(0))) >> dds0.s_axis_phase
  streamForCarrier.translateFragmentWith(getPhaseData(resync, phaseInc, phaseOffsets(1))) >> dds1.s_axis_phase
  val streamCarrier = dds0.m_axis_data.translateFragmentWith(
    Vec(
      dds0.m_axis_data.fragment(2 * DDS_OUTPUT_WIDTH - 1 downto DDS_OUTPUT_WIDTH).asSInt, // sin0
      dds0.m_axis_data.fragment(DDS_OUTPUT_WIDTH - 1 downto 0).asSInt, // cos0
      dds1.m_axis_data.fragment(2 * DDS_OUTPUT_WIDTH - 1 downto DDS_OUTPUT_WIDTH).asSInt, // sin1
      dds1.m_axis_data.fragment(DDS_OUTPUT_WIDTH - 1 downto 0).asSInt // cos1
    )
  )
  dds1.m_axis_data.ready := streamCarrier.ready
  val streamRawBuffered = streamRawData.queue(16) // queue
  val streamRawAndCarrier = StreamJoin(streamRawBuffered, streamCarrier) // join
  val Seq(x0, x1, y0, y1) = streamRawAndCarrier.payload._1.fragment
  val Seq(sin0, cos0, sin1, cos1) = streamRawAndCarrier.payload._2.fragment

//  streamRawAndCarrier.translateWith(
//    fragment(cos1 ## cos0 ## sin1 ## sin0, streamRawAndCarrier.payload._1.last)
//  ) >> streamOut

  //////////
  // step 2: get vector, 2 stage
  //////////
  val stage0 = Vec(sin0 * x0, sin0 * y0, cos0 * x0, cos0 * y0, sin1 * x1, sin1 * y1, cos1 * x1, cos1 * y1)
  val streamShifted = streamRawAndCarrier.translateWith(fragment(stage0, streamRawAndCarrier.payload._1.last)).m2sPipe()
  val stage1 = streamShifted.fragment.grouped(2).toSeq.map(pair => (pair(0) +^ pair(1)).takeHigh(16))
  val streamVec = streamShifted.translateWith(fragment(Vec(stage1), streamShifted.last)).m2sPipe()
  val Seq(imag0, real0, imag1, real1) = streamVec.fragment

//  streamVec.translateFragmentWith(imag1 ## imag0 ## real1 ## real0) >> streamOut

  //////////
  // step 3: get filtered
  //////////
  // streamVec -> FIR -> streamFiltered
  streamVec.ready.allowOverride()
  val firImag, firReal = LowpassFir()
  streamVec.translateFragmentWith(imag1 ## imag0) >> firImag.s_axis_data
  streamVec.translateFragmentWith(real1 ## real0) >> firReal.s_axis_data
  val streamFiltered = firReal.m_axis_data.translateFragmentWith(
    Vec(firReal.m_axis_data.fragment.subdivideIn(32 bits) ++ firImag.m_axis_data.fragment.subdivideIn(32 bits))
  )
  firImag.m_axis_data.ready := streamFiltered.ready
  val Seq(filteredReal0, filteredReal1, filteredImag0, filteredImag1) = streamFiltered.fragment

//  streamFiltered.translateFragmentWith(filteredImag1.takeHigh(16) ## filteredImag0.takeHigh(16) ## filteredReal1.takeHigh(16) ## filteredReal0.takeHigh(16)) >> streamOut

  //////////
  // step 4: phase manipulation
  //////////
  // 相比numpy,CORDIC需要输入具有更多的有效位数,面积允许的情况下,应使用较大的位宽
  streamFiltered.ready.allowOverride()
  val cordic0, cordic1 = Atan2()
  streamFiltered.translateFragmentWith(filteredImag0 ## filteredReal0) >> cordic0.s_axis_cartesian
  streamFiltered.translateFragmentWith(filteredImag1 ## filteredReal1) >> cordic1.s_axis_cartesian

  cordic0.m_axis_dout.translateFragmentWith(
    cordic1.m_axis_dout.fragment ## cordic0.m_axis_dout.fragment ##
      cordic1.m_axis_dout.fragment ## cordic0.m_axis_dout.fragment
  ) >> streamOut
  cordic1.m_axis_dout.ready := streamOut.ready

}

object ComponentDemodulator extends App {
  Config.spinal.generateVerilog(ComponentDemodulator(80 MHz))
}
