package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._
import spinal.lib.fsm._
import spinal.lib.bus._

case class DraftModule() extends Module {

//  val dataIn = in Bits (32 bits)
//  val dataOut = out Vec (Bits(8 bits), 4)
//
//  dataOut := dataIn.subdivideIn(4 slices)

//  val dataIn = in Vec (Bits(8 bits), 4)
//  val dataOut = out Bits (32 bits)
//  dataOut := dataIn.asBits

  val dataIn = in Vec (Bits(8 bits), 4)
  val dataOut = out Vec (Bits(8 bits), 4)
  dataOut := dataIn.asBits.subdivideIn(4 slices)

}

object DraftModule extends App {
  Config.spinal.generateVerilog(DraftModule())
}
