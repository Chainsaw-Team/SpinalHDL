package chainsaw.projects.xdma.daq
import spinal.core._
import spinal.lib.{Fragment, _}

import scala.collection.Seq
import scala.language.postfixOps
import scala.math
import ku060Ips._

case class DasDemodulator() extends Module {

  val clk, rstn = in Bool ()
  val gaugePointsIn = in UInt (log2Up(GAUGE_POINTS_MAX + 1) bits)
  val pulseValidPointsIn = in UInt (log2Up(PULSE_VALID_POINTS_MAX + 1) bits)

  val dataClockDomain = new ClockDomain(
    clock = clk,
    reset = rstn,
    config = ClockDomainConfig(resetKind = SYNC, resetActiveLevel = LOW),
    frequency = FixedFrequency(250 MHz)
  )

  val streamIn = slave Stream Fragment(Vec(SInt(16 bits), 4)) // earlier data in lower index
  val streamOut = master Stream Fragment(Vec(SInt(16 bits), 4)) // earlier data in lower bits
  val en = in Bool () // output demodulated phase when enabled, raw data when disabled

  val Seq(x0, x1, y0, y1) = streamIn.fragment // earlier data in higher index

  new ClockingArea(dataClockDomain) {
    streamIn.ready.allowOverride()

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
    when(en) {
      rawStream.ready.set()
      demodulatedStream <> streamOut
    }.otherwise {
      demodulatedStream.ready.set()
      rawStream <> streamOut
    }

  }

}

object DasDemodulator extends App {
  Config.spinal.generateVerilog(DasDemodulator())
}
