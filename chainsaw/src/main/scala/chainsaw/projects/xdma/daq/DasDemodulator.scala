package chainsaw.projects.xdma.daq
import spinal.core._
import spinal.lib.{Fragment, _}

import scala.collection.Seq
import scala.language.postfixOps
import scala.math
import ku060Ips._

case class DasDemodulator() extends Module {

  val clk, rstn = in Bool ()
  val dataClockDomain = new ClockDomain(clk, rstn,config =DAS_CLOCK_DOMAIN_CONFIG, frequency = DATA_FREQUENCY)

  val streamIn = slave Stream Fragment(Vec(SInt(16 bits), 4)) // earlier data in lower index
  val streamOut = master Stream Fragment(Vec(SInt(16 bits), 4)) // earlier data in lower bits

  val demodulationEnabled = in Bool () // output demodulated phase when enabled, raw data when disabled
  val gaugePointsIn = in UInt (log2Up(GAUGE_POINTS_MAX + 1) bits)
  val pulseValidPointsIn = in UInt (log2Up(PULSE_VALID_POINTS_MAX + 1) bits)

  val Seq(x0, x1, y0, y1) = streamIn.fragment

  new ClockingArea(dataClockDomain) {
    streamIn.ready.allowOverride()

    // TODO: considering using stream arbiter
    // datapath for raw data
    val rawStream = streamIn.translateWith(fragment(Vec(x1, y1, x0, y0), streamIn.last))

    // datapath for demodulation,streamForAlgo -> demodulatedStream
    val demodulatedStream = Stream Fragment Vec(SInt(16 bits), 4) // output of this branch

    val componentDemodulators = CARRIER_FREQS.map(freq => ComponentDemodulator(freq))
    componentDemodulators.foreach { dem =>
      streamIn.translateFragmentWith(Vec(x0, x1, y0, y1)) >> dem.streamIn
      dem.gaugePointsIn := gaugePointsIn
      dem.pulseValidPointsIn := pulseValidPointsIn
      val Seq(r0, r1, _, _) = dem.streamOut.fragment
      dem.streamOut.translateFragmentWith(Vec(r1, r1, r0, r0)) >> demodulatedStream
    }

    streamIn.ready.set() // no back pressure

    // output
    when(demodulationEnabled) {
      rawStream.ready.set()
      demodulatedStream <> streamOut
    }.otherwise {
      demodulatedStream.ready.set()
      rawStream <> streamOut
    }

    // counter for debug
    val outputCounter = Counter(PULSE_VALID_POINTS_MAX, inc=streamOut.fire)
    when(streamOut.fire && streamOut.last)(outputCounter.clear())
    outputCounter.value.setName("outputCounter")
    out(outputCounter.value)
  }

}

object DasDemodulator extends App {
  Config.spinal.generateVerilog(DasDemodulator())
}
