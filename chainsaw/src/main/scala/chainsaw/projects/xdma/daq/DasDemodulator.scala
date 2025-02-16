package chainsaw.projects.xdma.daq
import chainsaw.projects.xdma.daq.customizedIps.Normalize32
import chainsaw.projects.xdma.daq.ku060Ips.Atan2
import spinal.core._
import spinal.lib.{Fragment, _}

import scala.collection.Seq
import scala.language.postfixOps

case class DasDemodulator() extends Module {

//  val clk, rstn = in Bool ()

  val streamIn = slave Stream Fragment(Vec(SInt(16 bits), 4)) // earlier data in lower index
  val streamOut = master Stream Fragment(Vec(SInt(16 bits), 4)) // earlier data in lower bits

  val demodulationEnabled = in Bool () // output demodulated phase when enabled, raw data when disabled
  val gaugePointsIn = in UInt (log2Up(GAUGE_POINTS_MAX + 1) bits)
  val pulseValidPointsIn = in UInt (log2Up(PULSE_VALID_POINTS_MAX + 1) bits)

  def change(data: Data) = RegNext(data) =/= data
  val changed = change(demodulationEnabled) || change(gaugePointsIn) || change(pulseValidPointsIn)

  // constructing
  val resetCountdown = Timeout(100)
  when(changed)(resetCountdown.clear())
  val datapathRstn = ClockDomain.current.readResetWire && resetCountdown

  val dataClockDomain = new ClockDomain(
    ClockDomain.current.clock,
    datapathRstn,
    config = DAS_CLOCK_DOMAIN_CONFIG,
    frequency = DATA_FREQUENCY
  )

  val Seq(x0, x1, y0, y1) = streamIn.fragment

  new ClockingArea(dataClockDomain) {
    val streamInGated = streamIn.continueWhen(datapathRstn) // disconnected during reset
    streamInGated.ready.allowOverride()

    // TODO: considering using stream arbiter
    // datapath for raw data
    val rawStream = streamInGated.translateWith(fragment(Vec(x1, y1, x0, y0), streamIn.last))
    // datapath for demodulation,streamForAlgo -> demodulatedStream
    val demodulatedStream = Stream Fragment Vec(SInt(16 bits), 4) // output of this branch

    //////////
    // step 1: component demodulation
    //////////
    val strainRateStreams: Seq[Stream[Fragment[Vec[SInt]]]] = CARRIER_FREQS.flatMap { freq =>
      val demX, demY = ComponentDemodulator(freq)
      streamInGated.ready.allowOverride()
      streamInGated.translateFragmentWith(Vec(x0, x1)) >> demX.streamIn
      streamInGated.translateFragmentWith(Vec(y0, y1)) >> demY.streamIn
      Seq(demX, demY).foreach { dem =>
        dem.gaugePointsIn := gaugePointsIn
        dem.pulseValidPointsIn := pulseValidPointsIn
      }
      Seq(demX.streamOut, demY.streamOut)
    }

    //////////
    // step 2: merge
    //////////

    def merge(streams: Seq[Stream[Fragment[Vec[SInt]]]]): Seq[Stream[Fragment[Vec[SInt]]]] = {
      if (streams.length == 1)
        Seq(streams.head.translateFragmentWith(Vec(streams.head.fragment.map(_.takeHigh(32).asSInt))))
      else {
        val half = streams.length / 2
        val sums = streams.take(half).zip(streams.takeRight(half)).map { case (s0, s1) =>
          val ret = s0.translateFragmentWith(Vec(s0.fragment.zip(s1.fragment).map { case (a, b) => a +^ b })).m2sPipe()
          s1.ready := ret.ready
          ret
        }
        if (streams.length % 2 == 1) merge(sums :+ streams.last.m2sPipe())
        else merge(sums)
      }
    }

    val streamMerged = merge(strainRateStreams).head
    val Seq(strainRateR0, strainRateI0, strainRateR1, strainRateI1) = streamMerged.fragment

    //////////
    // step 3: normalization
    //////////
    val normalizer0, normalizer1 = Normalize32()
    streamMerged.ready.allowOverride()
    streamMerged.translateFragmentWith(Vec(strainRateI0, strainRateR0)) >> normalizer0.dataIn
    streamMerged.translateFragmentWith(Vec(strainRateI1, strainRateR1)) >> normalizer1.dataIn

    //////////
    // step 4: get phase in rad using CORDIC
    //////////
    // normalizer -> CORDIC -> streamDemodulated
    val cordic0, cordic1 = Atan2()
    streamMerged.ready.allowOverride()
    // Atan2 input format = 1QN([-2, 2)), valid range = [-1, 1], thus, do right shift by 1 before assignment
    normalizer0.dataOut.translateFragmentWith(
      (normalizer0.dataOut.fragment(0) |>> 1) ## (normalizer0.dataOut.fragment(1) |>> 1)
    ) >> cordic0.s_axis_cartesian
    normalizer1.dataOut.translateFragmentWith(
      (normalizer1.dataOut.fragment(0) |>> 1) ## (normalizer1.dataOut.fragment(1) |>> 1)
    ) >> cordic1.s_axis_cartesian

    cordic0.m_axis_dout.translateFragmentWith(
      Vec(Seq(cordic0, cordic0, cordic1, cordic1).map(_.m_axis_dout.fragment.asSInt))
    ) >> demodulatedStream
    cordic1.m_axis_dout.ready := demodulatedStream.ready

    streamInGated.ready.set() // no back pressure

    // output
    when(demodulationEnabled) {
      rawStream.ready.set()
      demodulatedStream <> streamOut
    }.otherwise {
      demodulatedStream.ready.set()
      rawStream <> streamOut
    }

    // counter for debug
    val outputCounter = Counter(PULSE_VALID_POINTS_MAX, inc = streamOut.fire)
    when(streamOut.fire && streamOut.last)(outputCounter.clear())
    outputCounter.value.setName("outputCounter")
    out(outputCounter.value)

  }

}

object DasDemodulator extends App {
  Config.gen.generateVerilog(DasDemodulator())
}
