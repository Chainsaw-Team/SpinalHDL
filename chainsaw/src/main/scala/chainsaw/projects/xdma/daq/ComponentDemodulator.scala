package chainsaw.projects.xdma.daq

import chainsaw.projects.xdma.daq.ku060Ips._
import spinal.core._
import spinal.core.sim.SimDataPimper
import spinal.lib.{Fragment, _}

import scala.collection.Seq
import scala.language.postfixOps
import scala.math

case class ComponentDemodulator(carrierFreq: HertzNumber) extends Module {

  // TODO: 在没有特殊字段的前提下,blackbox中的Axi4Stream应当被提取为Stream或Flow,而不是完整的Axi4Stream
  val OUTPUT_WIDTH = 32
  val OUTPUT_FRACTIONAL_WIDTH = 20
  val OUTPUT_STRAIN_RESOLUTION = 0.025 / 0.23 / (1 << 20) // 0.23rad <-> 0.025με / gauge length
  println(s"strain/gauge length resolution = ${OUTPUT_STRAIN_RESOLUTION * 1e12}pε/m")

  def fragment[T <: Data](data: T, last: Bool): Fragment[T] = {
    val fragment = Fragment(HardType(data))
    fragment.fragment := data
    fragment.last := last
    fragment
  }

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
  val dds0, dds1 = DdsCompiler()

  //////////
  // stream datapath
  //////////

  //////////
  // step 1: complex sine wave generation & vectorization
  //////////
  val Seq(streamRawData, streamForCarrier) = StreamFork(streamIn, 2, synchronous = true)

  // StreamForCarrier -> DDS
  val ddss = Seq(dds0, dds1)
  ddss.zip(phaseOffsets).foreach { case (dds, offset) =>
    dds.s_axis_phase.valid := streamForCarrier.valid
    // driving 2 DDS module with different offset
    dds.s_axis_phase.data := getPhaseData(resync, inc = phaseInc, offset = offset)
    dds.s_axis_phase.last := streamForCarrier.last

    streamForCarrier.ready.allowOverride()
    streamForCarrier.ready := dds.s_axis_phase.ready
  }

  // DDS -> streamCarrier
  val streamCarrier = Stream Fragment Vec(SInt(16 bits), 4)
  dds0.m_axis_data.translateWith(
    fragment(
      Vec(
        dds0.m_axis_data.data(2 * DDS_OUTPUT_WIDTH - 1 downto DDS_OUTPUT_WIDTH).asSInt, // sin0
        dds0.m_axis_data.data(DDS_OUTPUT_WIDTH - 1 downto 0).asSInt, // cos0
        dds1.m_axis_data.data(2 * DDS_OUTPUT_WIDTH - 1 downto DDS_OUTPUT_WIDTH).asSInt, // sin1
        dds1.m_axis_data.data(DDS_OUTPUT_WIDTH - 1 downto 0).asSInt // cos1
      ),
      dds0.m_axis_data.last
    )
  ) >> streamCarrier
  dds1.m_axis_data.ready := streamCarrier.ready

  // streamRaw -> FIFO for alignment
  val streamRawBuffered = streamRawData.queue(16)

  val streamJoined0 = StreamJoin(streamRawBuffered, streamCarrier)
  val Seq(x0, x1, y0, y1) = streamJoined0.payload._1.fragment
  val Seq(sin0, cos0, sin1, cos1) = streamJoined0.payload._2.fragment

  val streamVec = Stream Fragment Vec(SInt(16 bits), 4)
  streamJoined0 // delay = 2
    .m2sPipe()
    .m2sPipe()
    .translateWith(
      fragment(
        Vec(
          RegNext(RegNext(sin0 * x0) + RegNext(sin0 * y0)).takeHigh(16).asSInt, // imag0
          RegNext(RegNext(cos0 * x0) + RegNext(cos0 * y0)).takeHigh(16).asSInt, // real0
          RegNext(RegNext(sin1 * x1) + RegNext(sin1 * y1)).takeHigh(16).asSInt, // imag1
          RegNext(RegNext(cos1 * x1) + RegNext(cos1 * y1)).takeHigh(16).asSInt // real1
        ),
        Delay(streamJoined0.payload._1.last, 2)
      )
    ) >> streamVec
  val Seq(imag0, real0, imag1, real1) = streamVec.fragment

  //    streamVec.translateWith(fragment(real1 ## real0 ## imag1 ## imag0, streamVec.last)) >> streamOut

  //////////
  // step 2: low-pass FIR
  //////////
  val firImag, firReal = LowpassFir()
  streamVec.ready.allowOverride()
  val streamImag = streamVec.translateWith(fragment(imag1 ## imag0, streamVec.last))
  val streamReal = streamVec.translateWith(fragment(real1 ## real0, streamVec.last))
  Seq(streamImag, streamReal).zip(Seq(firImag, firReal)).foreach { case (stream, fir) =>
    fir.s_axis_data.data := stream.fragment
    fir.s_axis_data.last := stream.last
    fir.s_axis_data.valid := stream.valid
    stream.ready := fir.s_axis_data.ready
  }
  val Seq(filteredReal0, filteredReal1) = firReal.m_axis_data.data.subdivideIn(32 bits)
  val Seq(filteredImag0, filteredImag1) = firImag.m_axis_data.data.subdivideIn(32 bits)
  // TODO: merge stream filtered
  val streamFilteredReal = firReal.m_axis_data.translateWith(
    fragment(Vec(filteredReal0.takeHigh(16), filteredReal1.takeHigh(16)), firReal.m_axis_data.last)
  )
  val streamFilteredImag = firImag.m_axis_data.translateWith(
    fragment(Vec(filteredImag0.takeHigh(16), filteredImag1.takeHigh(16)), firImag.m_axis_data.last)
  )

  //    streamFilteredReal.translateWith(
  //      fragment(
  //        streamFilteredReal.fragment(1) ## streamFilteredReal.fragment(0) ## streamFilteredImag.fragment(
  //          1
  //        ) ## streamFilteredImag.fragment(0),
  //        streamFilteredReal.last
  //      )
  //    ) >> streamOut
  //    streamFilteredImag.ready := streamOut.ready

  // step 3: get phase and magnitude
  // TODO: optimize CORDIC parameters,包括输入输出位数,iteration数量
  // 相比numpy,CORDIC需要输入具有更多的有效位数,面积允许的情况下,应使用较大的位宽
  val cordic0, cordic1 = Atan2()
  Seq(cordic0, cordic1).zipWithIndex.foreach { case (cordic, i) =>
    cordic.s_axis_cartesian.data := firImag.m_axis_data.data(getSlice(32, i)) ## firReal.m_axis_data
      .data((i + 1) * 32 - 1 downto i * 32)
    cordic.s_axis_cartesian.valid := streamFilteredReal.valid
    cordic.s_axis_cartesian.last := streamFilteredReal.last
  }
  streamFilteredReal.ready := cordic0.s_axis_cartesian.ready
  streamFilteredImag.ready := cordic0.s_axis_cartesian.ready

  cordic0.m_axis_dout.translateWith(
    fragment(
      cordic1.m_axis_dout.data ## cordic0.m_axis_dout.data ##
        cordic1.m_axis_dout.data ## cordic0.m_axis_dout.data,
      cordic0.m_axis_dout.last
    )
  ) >> streamOut
  cordic1.m_axis_dout.ready := streamOut.ready

  // step 4: phase manipulation,从这一步开始使用浮点计算
  // step 4.1 fixed2float
  // step 4.2 unwrap
  // step 4.3 spatial diff by gauge length
  // step 4.4 float2fixed

  // debug
  streamRawData.setName("streamRawData").simPublic()
  streamRawBuffered.setName("streamRawBuffered").simPublic()
  streamCarrier.setName("streamCarrier").simPublic()
  streamVec.setName("streamVec").simPublic()
  streamVec.fragment.foreach(_.simPublic())

}

object ComponentDemodulator extends App {
  Config.spinal.generateVerilog(ComponentDemodulator(80 MHz))
}
