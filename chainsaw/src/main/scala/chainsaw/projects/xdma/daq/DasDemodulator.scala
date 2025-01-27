package chainsaw.projects.xdma.daq
import spinal.core._
import spinal.lib._

import scala.collection.Seq
import scala.language.postfixOps
import scala.math
import ku060Ips._

case class DasDemodulator() extends Module {

  val clk, rstn = in Bool ()
  val dataClockDomain = new ClockDomain(
    clock = clk,
    reset = rstn,
    config = ClockDomainConfig(resetKind = SYNC, resetActiveLevel = LOW),
    frequency = FixedFrequency(250 MHz)
  )

  def fragment[T <: Data](data: T, last: Bool): Fragment[T] = {
    val fragment = Fragment(HardType(data))
    fragment.fragment := data
    fragment.last := last
    fragment
  }

  val vectorWidth = 2

  val streamIn = slave Stream Fragment(Vec(SInt(16 bits), 4)) // earlier data in lower index
  val streamOut = master Stream Fragment(Bits(64 bits)) // earlier data in lower bits
  val en = in Bool () // output demodulated phase when enabled, raw data when disabled

  val channel0 = streamIn.fragment.take(vectorWidth)
  val channel1 = streamIn.fragment.takeRight(vectorWidth)

  new ClockingArea(dataClockDomain) {

    val Seq(streamForAlgo, streamForRaw) = StreamFork(streamIn, 2, synchronous = true)

    // datapath for raw data
    val rawData = channel1(1) ## channel0(1) ## channel1(0) ## channel0(0)
    val rawStream = streamForRaw.translateWith(fragment(rawData, streamIn.last))

    // datapath for demodulation,streamForAlgo -> demodulatedStream
    val demodulatedStream = Stream Fragment Bits(64 bits) // output of this branch

    val dem80 = ComponentDemodulator(80 MHz)
    streamForAlgo >> dem80.streamIn
    dem80.streamOut.translateFragmentWith(dem80.streamOut.fragment.asBits) >> demodulatedStream // FIXME: data order

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
