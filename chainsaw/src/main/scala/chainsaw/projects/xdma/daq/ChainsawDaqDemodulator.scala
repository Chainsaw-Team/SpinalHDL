package chainsaw.projects.xdma.daq
import spinal.core._
import spinal.lib._

import scala.collection.Seq
import scala.language.postfixOps
import scala.math
import ku060Ips._

case class ChainsawDaqDemodulator() extends Module {

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

    // datapath for demodulation
    val demodulatedStream = Stream Fragment Bits(64 bits) // output of this branch

    val PHASE_WIDTH = 18
    val SAMPLING_FREQ = dataClockDomain.frequency.getValue.toDouble
    println(SAMPLING_FREQ)
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

    // TODO: need offset
    // TODO: need phaseInc calculator
    val dds = dds_compiler_0()
    val resync = streamForAlgo.last
    val phaseInc80 = ddsSynth(80e6)
    dds.aclk := clk

    dds.s_axis_phase.valid := streamForAlgo.valid
    dds.s_axis_phase.data := getPhaseData(resync, inc = phaseInc80, offset = 0)
    dds.s_axis_phase.last := streamForAlgo.last
    streamForAlgo.ready := dds.s_axis_phase.ready

    demodulatedStream.fragment := dds.m_axis_data.data
    demodulatedStream.last := dds.m_axis_data.last
    demodulatedStream.valid := dds.m_axis_data.valid
    dds.m_axis_data.ready := demodulatedStream.ready

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

object ChainsawDaqDemodulator extends App {
  Config.spinal.generateVerilog(ChainsawDaqDemodulator())
}
