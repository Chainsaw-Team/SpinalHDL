package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.lib._

import scala.language.postfixOps

case class ChainsawDaqDemodulator() extends Module {

  // TODO: learning using pipeline utilities for Stream

  val streamIn = slave Stream Fragment(Bits(128 bits))
  val streamOut = master Stream Fragment(Bits(64 bits))
  val channelSelection = in Bool () // select X|Y
  val xyEnabled = in Bool () // enable both X and Y

  //
  val channel0 = streamIn.fragment.takeLow(64)
  val channel1 = streamIn.fragment.takeHigh(64)

  // channel selection
  val summation = channel0
    .subdivideIn(16 bits)
    .zip(channel1.subdivideIn(16 bits))
    .map { case (x, y) => ((x.asSInt +^ y.asSInt) >> 1).asBits }
    .reduce(_ ## _)

  val selection = Mux(channelSelection, channel1, channel0)
  val fragment = Mux(xyEnabled, summation, selection)
//  streamIn.translateWith(Fragment(fragment)).m2sPipe() >> streamOut

  streamOut.fragment := fragment
  streamOut.valid := streamIn.valid
  streamIn.ready := streamOut.ready
  streamOut.last := streamIn.last

//
//      when(getControlData(controlClockingArea.testMode)) { // test data -> AXI DMA
//        (0 until 4).foreach(i => streamRaw.fragment(i * 16, 16 bits) := (counterForTest.value @@ U(i, 2 bits)).asBits)
//      }.otherwise { // JESD204 -> AXI DMA
//        streamRaw.fragment := Mux(getControlData(channelSelection.asBool), channel1Segments, channel0Segments)
//      }

}

object ChainsawDaqDemodulator extends App {
  SpinalConfig().generateVerilog(ChainsawDaqDemodulator())
}
