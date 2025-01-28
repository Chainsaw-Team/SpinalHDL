package chainsaw.projects.xdma.daq

import chainsaw.projects.xdma.daq.customizedIps.{DataDelay, DataDelayConfig}
import chainsaw.projects.xdma.daq.ku060Ips._
import spinal.core._
import spinal.core.sim.SimDataPimper
import spinal.lib.experimental.math.Floating32
import spinal.lib.{Fragment, _}

import scala.collection.Seq
import scala.language.postfixOps
import scala.math

// TODO: 在没有特殊字段的前提下,blackbox中的Axi4Stream应当被提取为Stream或Flow,而不是完整的Axi4Stream
// TODO: optimize CORDIC parameters,包括输入输出位数,iteration数量
// TODO: test when frameSize / gaugeLength change

case class ComponentDemodulator(carrierFreq: HertzNumber, debug: Boolean = false) extends Module {

  type BitStream = Stream[Fragment[Bits]]

  val GAUGE_POINTS_MAX = 250
  val PULSE_VALID_POINTS_MAX = 2000

  val OUTPUT_WIDTH = 32
  val OUTPUT_FRACTIONAL_WIDTH = 20
  val OUTPUT_STRAIN_RESOLUTION = 0.025 / 0.23 / (1 << 20) // 0.23rad <-> 0.025με / gauge length
  println(s"strain/gauge length resolution = ${OUTPUT_STRAIN_RESOLUTION * 1e12}pε/m")
  println(s"gauge length max = ${GAUGE_POINTS_MAX * 2 * 0.2}m")
  println(s"fiber length max = ${PULSE_VALID_POINTS_MAX * 2 * 0.2}m")

  val streamIn = slave Stream Fragment(Vec(SInt(16 bits), 4)) // x0, x1, y0, y1
  val streamOut = master Stream Fragment(Vec(SInt(16 bits), 4))
  val gaugePointsIn = in UInt (log2Up(GAUGE_POINTS_MAX + 1) bits)
  val pulseValidPointsIn = in UInt (log2Up(PULSE_VALID_POINTS_MAX + 1) bits)
  // export floating data when debugging
  val streamOutFloat = debug generate (master Stream Fragment(Vec(Floating32(), 2)))

  //////////
  // parameter preparation
  //////////
  val DDS_MODULUS = 250 // using rasterized mode
  val PHASE_WIDTH = log2Up(DDS_MODULUS)
  val DDS_OUTPUT_WIDTH = 16
  val SAMPLING_FREQ = 250e6

  def getPhaseData(resync: Bool, inc: Int, offset: Int) = { // according to DDS IP data structure
    val dataFieldSize = math.ceil(PHASE_WIDTH.toDouble / 8).toInt * 8
    B(0, 7 bits) ## resync ## B(offset, dataFieldSize bits) ## B(inc, dataFieldSize bits)
  }

  def mult(a: Stream[Fragment[Bits]], b: Stream[Fragment[Bits]]) = {
    val mult = FloatMult()
    a >> mult.s_axis_a
    b >> mult.s_axis_b
    mult.m_axis_result
  }

  def add(a: Stream[Fragment[Bits]], b: Stream[Fragment[Bits]]) = {
    val mult = FloatAdd()
    a >> mult.s_axis_a
    b >> mult.s_axis_b
    mult.m_axis_result
  }

  def sub(a: Stream[Fragment[Bits]], b: Stream[Fragment[Bits]]) = {
    val mult = FloatSub()
    a >> mult.s_axis_a
    b >> mult.s_axis_b
    mult.m_axis_result
  }

  def get_diff(r: BitStream, rd: BitStream, i: BitStream, id: BitStream) = {
    val left = Seq(r, i, r, i)
    val right = Seq(rd, id, id, rd)
    left.foreach(_.ready.allowOverride())
    right.foreach(_.ready.allowOverride())
    val diffStage0 = left.zip(right).map { case (a, b) => mult(a, b) }
    val Seq(a, b, c, d) = diffStage0
    Seq(add(a, b), sub(c, d))
  }

  val resync = streamIn.start
  val phaseInc = (carrierFreq.toDouble * DDS_MODULUS / SAMPLING_FREQ).toInt
  val phaseOffsets = Seq(0, phaseInc / 2)
  assert(phaseInc % 2 == 0)

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

  //////////
  // step 2: get vector with 2-stage pipelining
  //////////
  val stage0 = Vec(sin0 * x0, sin0 * y0, cos0 * x0, cos0 * y0, sin1 * x1, sin1 * y1, cos1 * x1, cos1 * y1)
  val streamShifted = streamRawAndCarrier.translateWith(fragment(stage0, streamRawAndCarrier.payload._1.last)).m2sPipe()
  val stage1 = streamShifted.fragment.grouped(2).toSeq.map(pair => (pair(0) +^ pair(1)).takeHigh(16)) // equals to >> 17
  val streamVec = streamShifted.translateWith(fragment(Vec(stage1), streamShifted.last)).m2sPipe()
  val Seq(imag0, real0, imag1, real1) = streamVec.fragment

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

  //////////
  // step 4: get spatial diffed
  //////////
  // streamFiltered -> fixed2float -> streamFloat
  val fixed2Floats = Seq.fill(4)(Fixed32_24ToFloat())
  streamFiltered.ready.allowOverride()
  (0 until 4).foreach { i =>
    streamFiltered.translateFragmentWith(streamFiltered.fragment(i)) >>
      fixed2Floats(i).s_axis_a
  }
  val streamFloat =
    fixed2Floats.head.m_axis_result.translateFragmentWith(
      Vec(fixed2Floats.map(_.m_axis_result.fragment))
    )
  fixed2Floats.tail.foreach(_.m_axis_result.ready := streamFloat.ready)

  // streamFloat -> delay ->  streamFloatDelayed
  val gaugeDelay = DataDelay(DataDelayConfig(HardType(streamFloat.fragment), GAUGE_POINTS_MAX, 0))
  streamFloat >> gaugeDelay.dataIn
  gaugeDelay.delayIn := gaugePointsIn
  val streamFloatDelayed = gaugeDelay.dataOut.translateFragmentWith(gaugeDelay.dataOut.fragment.head)

  streamFloat.ready.allowOverride()
  streamFloatDelayed.ready.allowOverride()
  val Seq(r0, r1, i0, i1) = streamFloat.fragment.map(bits => streamFloat.translateFragmentWith(bits))
  val Seq(r0d, r1d, i0d, i1d) = streamFloatDelayed.fragment.map(bits => streamFloatDelayed.translateFragmentWith(bits))

  val strain = get_diff(r0, r0d, i0, i0d) ++ get_diff(r1, r1d, i1, i1d)
  val streamStrain = strain.head.translateFragmentWith(Vec(strain.map(_.fragment)))
  strain.tail.foreach(_.ready := streamStrain.ready)
  val Seq(strainR0, strainI0, strainR1, strainI1) = streamStrain.fragment

  //////////
  // step 5: get time diffed
  //////////
  // streamStrain -> delay ->  streamStrainDelayed
  val pulseDelay = DataDelay(DataDelayConfig(HardType(streamStrain.fragment), PULSE_VALID_POINTS_MAX, 0))
  streamStrain >> pulseDelay.dataIn
  pulseDelay.dataIn.last.allowOverride()
  pulseDelay.dataIn.last.clear() // TODO: set when pulseValidPoints change
  pulseDelay.delayIn := pulseValidPointsIn
  val streamStrainDelayed = pulseDelay.dataOut.translateFragmentWith(pulseDelay.dataOut.fragment.head)

  streamStrain.ready.allowOverride()
  streamStrainDelayed.ready.allowOverride()
  val Seq(sr0, si0, sr1, si1) = streamStrain.fragment.map(bits => streamStrain.translateFragmentWith(bits))
  val Seq(sr0d, si0d, sr1d, si1d) =
    streamStrainDelayed.fragment.map(bits => streamStrainDelayed.translateFragmentWith(bits))
  val strainRate = get_diff(sr0, sr0d, si0, si0d) ++ get_diff(sr1, sr1d, si1, si1d)
  val streamStrainRate = strainRate.head.translateFragmentWith(Vec(strainRate.map(_.fragment)))
  strainRate.tail.foreach(_.ready := streamStrainRate.ready)
  val Seq(strainRateR0, strainRateI0, strainRateR1, strainRateI1) = streamStrainRate.fragment

  if (debug) {
    streamOutFloat.arbitrationFrom(streamStrainRate)
    streamOutFloat.fragment(0).assignFromBits(strainRateR0)
    streamOutFloat.fragment(1).assignFromBits(strainRateR1)
    streamOutFloat.last := streamStrainRate.last
  }

  //////////
  // step 6: get phase in rad
  //////////
  // streamStrainRate -> float2fixed -> streamStrainRateFixed
  val float2Fixeds = Seq.fill(4)(FloatToFixed32_16())
  streamStrainRate.ready.allowOverride()
  (0 until 4).foreach { i =>
    streamStrainRate.translateFragmentWith(streamStrainRate.fragment(i)) >> float2Fixeds(i).s_axis_a
  }
  val streamStrainRateFixed =
    float2Fixeds.head.m_axis_result.translateWith(
      fragment(Vec(float2Fixeds.map(_.m_axis_result.data)), float2Fixeds.head.m_axis_result.last)
    )
  float2Fixeds.tail.foreach(_.m_axis_result.ready := streamStrainRateFixed.ready)
  val Seq(strainRateFR0, strainRateFI0, strainRateFR1, strainRateFI1) = streamStrainRateFixed.fragment

  // streamStrainRateFixed -> CORDIC -> streamOut
  streamStrainRateFixed.ready.allowOverride()
  val cordic0, cordic1 = Atan2()
  streamStrainRateFixed.translateFragmentWith(strainRateFI0 ## strainRateFR0) >> cordic0.s_axis_cartesian
  streamStrainRateFixed.translateFragmentWith(strainRateFI1 ## strainRateFR1) >> cordic1.s_axis_cartesian
  cordic0.m_axis_dout.translateFragmentWith(
    Vec(Seq(cordic0, cordic1, cordic0, cordic1).map(_.m_axis_dout.fragment.asSInt))
  ) >> streamOut
  cordic1.m_axis_dout.ready := streamOut.ready

}

object ComponentDemodulator extends App {
  Config.spinal.generateVerilog(ComponentDemodulator(80 MHz, debug = true))
}
