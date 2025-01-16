package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.amba4.axis.{Axi4Stream, Axi4StreamConfig}
import spinal.lib.sim._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps
import scala.math._

case class ChainsawDaqDemodulator() extends Module {

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

  val Seq(streamForAlgo, streamForRaw) = StreamFork(streamIn, 2, synchronous = true)

  // datapath for raw data
  val rawData = channel1(1) ## channel0(1) ## channel1(0) ## channel0(0)
  val rawStream = streamForRaw.translateWith(fragment(rawData, streamIn.last))

  // datapath for demodulation
  val summation = channel0.zip(channel1).map { case (x, y) => RegNext(((x +^ y) >> 1).asBits) }
  val demodulatedData = summation(1) ## summation(1) ## summation(0) ## summation(0)
  val demodualtedLast = RegNext(streamIn.last)
  val demodulatedStream = streamForAlgo.m2sPipe().translateWith(fragment(demodulatedData, demodualtedLast))

  // output
  when(en) {
//    streamForRaw.ready.set()
    rawStream.ready.set()
    demodulatedStream <> streamOut
  }.otherwise {
//    streamForAlgo.ready.set()
    demodulatedStream.ready.set()
    rawStream <> streamOut
  }
}

object ChainsawDaqDemodulator extends App {
  SpinalConfig().generateVerilog(ChainsawDaqDemodulator())
}
