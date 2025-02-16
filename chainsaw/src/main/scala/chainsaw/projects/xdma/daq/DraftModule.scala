package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._
import spinal.lib.fsm._
import spinal.lib.bus._

import scala.language.postfixOps

case class DraftModule() extends Module {

//  val dataIn = in Bits (32 bits)
//  val dataOut = out Vec (Bits(8 bits), 4)
//
//  dataOut := dataIn.subdivideIn(4 slices)

//  val dataIn = in Vec (Bits(8 bits), 4)
//  val dataOut = out Bits (32 bits)
//  dataOut := dataIn.asBits

//  val dataIn = in Vec (Bits(8 bits), 4)
//  val dataOut = out Vec (Bits(8 bits), 4)
//  dataOut := dataIn.asBits.subdivideIn(4 slices)

//  val a, b = in SInt (17 bits)
////  val c = out SInt (34 bits)
//  val c = out(RegNext(a * b))

  val a = in SInt(17 bits)
  val b = out SInt(17 bits)
  b := a >> 6
}

object DraftModule extends App {
//  Config.gen.generateVerilog(DraftModule())
  Config.synth(DraftModule())
}
