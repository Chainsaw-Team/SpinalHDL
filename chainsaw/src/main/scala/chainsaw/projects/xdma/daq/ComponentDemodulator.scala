package chainsaw.projects.xdma.daq

import chainsaw.projects.xdma.daq.customizedIps.{DataDelay, DataDelayConfig}
import chainsaw.projects.xdma.daq.ku060Ips.{DdsCompiler, LowpassFir}
import spinal.core._
import spinal.lib.eda.xilinx.UltraScalePlus
import spinal.lib.{Fragment, _}

import scala.collection.Seq
import scala.language.postfixOps
import scala.math

// TODO: 将module中的scaling策略同步到python工程中

case class ComponentDemodulator(carrierFreq: HertzNumber, inverse: Boolean = false, ramType: String = "Block")
    extends Module {

  val streamIn = slave Stream Fragment(Vec(SInt(16 bits), 2)) // x0, x1
  val streamOut = master Stream Fragment(Vec(SInt(32 bits), 4)) // r0, i0, r1, i1
  val gaugePointsIn = in UInt (log2Up(GAUGE_POINTS_MAX + 1) bits)
  val pulseValidPointsIn = in UInt (log2Up(PULSE_VALID_POINTS_MAX + 1) bits)

  val fifoDepthMax = if (TARGET_DEVICE.family == UltraScalePlus) 4096 else 1024

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

  def get_diff(r: SIntStream, rd: SIntStream, i: SIntStream, id: SIntStream): Seq[SIntStream] = {

    val Seq(rs, rds, is, ids) = Seq(r, rd, i, id).map(_.fragment)
    val stage0 = Vec(rs * rds, is * ids, rs * ids, is * rds)
    val streamStage0 = r.translateFragmentWith(stage0).m2sPipe()
    Seq(rd, i, id).foreach(_.ready := streamStage0.ready)
    streamStage0.ready.allowOverride()
    val Seq(a, b, c, d) = streamStage0.fragment
    val stage1 = Vec(a +^ b, c -^ d)
    stage1.map(sint => streamStage0.translateFragmentWith(sint).m2sPipe())
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
  val Seq(sin0Pre, cos0Pre, sin1Pre, cos1Pre) = Seq(
    dds0.m_axis_data.fragment(2 * DDS_OUTPUT_WIDTH - 1 downto DDS_OUTPUT_WIDTH).asSInt, // sin0
    dds0.m_axis_data.fragment(DDS_OUTPUT_WIDTH - 1 downto 0).asSInt, // cos0
    dds1.m_axis_data.fragment(2 * DDS_OUTPUT_WIDTH - 1 downto DDS_OUTPUT_WIDTH).asSInt, // sin1
    dds1.m_axis_data.fragment(DDS_OUTPUT_WIDTH - 1 downto 0).asSInt // cos1
  )
  val streamCarrier = dds0.m_axis_data.translateFragmentWith(
    Vec(if (inverse) -sin0Pre else sin0Pre, cos0Pre, if (inverse) -sin1Pre else sin1Pre, cos1Pre)
  )

  dds1.m_axis_data.ready := streamCarrier.ready
  val streamRawBuffered = streamRawData.queue(16) // queue
  val streamRawAndCarrier = StreamJoin(streamRawBuffered, streamCarrier) // join
  val Seq(x0, x1) = streamRawAndCarrier.payload._1.fragment
  val Seq(sin0, cos0, sin1, cos1) = streamRawAndCarrier.payload._2.fragment

  //////////
  // step 2: get vector with 2-stage pipelining
  //////////
  val streamVec = streamRawAndCarrier
    .translateWith(
      fragment(
        Vec(
          Seq(sin0 * x0, cos0 * x0, sin1 * x1, cos1 * x1)
            .map(sint => sint(shiftedSignificandWidth - 1 downto shiftedSignificandWidth - shiftedTargetWidth))
        ),
        streamRawAndCarrier.payload._1.last
      )
    )
    .m2sPipe()
  val Seq(imag0, real0, imag1, real1) = streamVec.fragment

  //////////
  // step 3: get filtered
  //////////
  // streamVec -> FIR -> streamFiltered
  streamVec.ready.allowOverride()
  val firImag, firReal = LowpassFir()
  streamVec.translateFragmentWith(imag1 ## imag0) >> firImag.s_axis_data
  streamVec.translateFragmentWith(real1 ## real0) >> firReal.s_axis_data

  // downsample by 2
  val streamFiltered = firReal.m_axis_data.translateFragmentWith(
    Vec(
      firReal.m_axis_data.fragment
        .subdivideIn(32 bits)
        .map(bits => bits(filteredSignificandWidth - 1 downto filteredSignificandWidth - filteredTargetWidth).asSInt)
        .take(1) ++
        firImag.m_axis_data.fragment
          .subdivideIn(32 bits)
          .map(bits => bits(filteredSignificandWidth - 1 downto filteredSignificandWidth - filteredTargetWidth).asSInt)
          .take(1)
    )
  )
  firImag.m_axis_data.ready := streamFiltered.ready
  val Seq(filteredReal0, filteredImag0) = streamFiltered.fragment

  //////////
  // step 4: get spatial diffed
  //////////
  // streamFiltered -> delay ->  streamFilteredDelayed
  val gaugeDelay = DataDelay(
    DataDelayConfig(HardType(streamFiltered.fragment), GAUGE_POINTS_MAX, fifoDepthMax = fifoDepthMax, paddingValue = 0)
  )
  streamFiltered >> gaugeDelay.dataIn
  gaugeDelay.delayIn := gaugePointsIn
  gaugeDelay.dataOut.ready.allowOverride()
  val streamFilteredDelayed = gaugeDelay.dataOut.translateFragmentWith(gaugeDelay.dataOut.fragment.head)
  val streamFilteredRaw = gaugeDelay.dataOut.translateFragmentWith(gaugeDelay.dataOut.fragment.last)

  streamFilteredRaw.ready.allowOverride()
  streamFilteredDelayed.ready.allowOverride()
  val Seq(r0, i0) = streamFilteredRaw.fragment.map(bits => streamFilteredRaw.translateFragmentWith(bits))
  val Seq(r0d, i0d) = streamFilteredDelayed.fragment.map(bits => streamFilteredDelayed.translateFragmentWith(bits))

  val strain = get_diff(r0, r0d, i0, i0d)
  val streamStrain = strain.head.translateFragmentWith(
    Vec(
      strain
        .map(_.fragment)
        .map(sint => sint(strainSignificandWidth - 1 downto strainSignificandWidth - strainTargetWidth))
    )
  )
  strain.tail.foreach(_.ready := streamStrain.ready)
  val Seq(strainR0, strainI0) = streamStrain.fragment

  //////////
  // step 5: get time diffed
  //////////
  // streamStrain -> delay ->  streamStrainDelayed
  val pulseDelay = DataDelay(
    DataDelayConfig(
      HardType(streamStrain.fragment),
      PULSE_VALID_POINTS_MAX,
      fifoDepthMax = fifoDepthMax,
      frameBased = false
    )
  )
  streamStrain >> pulseDelay.dataIn
//  pulseDelay.dataIn.last.allowOverride()
//  pulseDelay.dataIn.last.clear() // must be, or last will reset DataDelay
  pulseDelay.delayIn := pulseValidPointsIn
  pulseDelay.dataOut.ready.allowOverride()
  val streamStrainDelayed = pulseDelay.dataOut.translateFragmentWith(pulseDelay.dataOut.fragment.head)
  val streamStrainRaw = pulseDelay.dataOut.translateFragmentWith(pulseDelay.dataOut.fragment.last)
  // bypass signal last
//  streamStrainDelayed.last.allowOverride()
//  streamStrainDelayed.last := RegNextWhen(streamStrain.last, streamStrain.fire)
//  streamStrainRaw.last.allowOverride()
//  streamStrainRaw.last := RegNextWhen(streamStrain.last, streamStrain.fire)

  streamStrainRaw.ready.allowOverride()
  streamStrainDelayed.ready.allowOverride()
  val Seq(sr0, si0) = streamStrainRaw.fragment.map(bits => streamStrainRaw.translateFragmentWith(bits))
  val Seq(sr0d, si0d) = streamStrainDelayed.fragment.map(bits => streamStrainDelayed.translateFragmentWith(bits))
  val strainRate = get_diff(sr0, sr0d, si0, si0d)
  val streamStrainRate = strainRate.head.translateFragmentWith(
    Vec(
      strainRate.map(
        _.fragment(strainRateSignificandWidth - 1 downto strainRateSignificandWidth - strainRateTargetWidth)
      )
    )
  )
  strainRate.tail.foreach(_.ready := streamStrainRate.ready)
  val Seq(strainRateR0, strainRateI0) = streamStrainRate.fragment
  streamStrainRate.translateFragmentWith(Vec(strainRateR0, strainRateI0, strainRateR0, strainRateI0)) >> streamOut

}

object ComponentDemodulator extends App {
  Config.gen.generateVerilog(ComponentDemodulator(80 MHz))
}
