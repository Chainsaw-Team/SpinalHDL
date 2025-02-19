package chainsaw.projects.xdma.daq.customizedIps

import chainsaw.projects.xdma.daq.{Config, StreamFragmentUtils}
import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._
import spinal.lib.fsm._
import spinal.lib.bus._

import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps
import scala.util.Random


case class Lzoc32() extends BlackBox {

  setDefinitionName("LZOC_32_comb_uid2")

  val I = in Bits (32 bits)
  val OZB = in Bool ()
  val O = out Bits (6 bits)

  addRTLPath("chainsaw/src/main/resources/LZOC.vhdl")

}

case class Normalize32() extends Module {

  val dataIn = slave(Stream(Fragment(Vec(SInt(32 bits), 2))))
  val dataOut = master(Stream(Fragment(Vec(SInt(32 bits), 2))))

  val lzocs = Seq.fill(2)(Lzoc32())
  val shifts: Seq[UInt] = lzocs.zip(dataIn.fragment).map { case (lzoc, data) =>
    lzoc.I := data.asBits
    lzoc.OZB := data.msb
    lzoc.O.asUInt
  }

  dataIn.ready.allowOverride()

  val streamShifts = dataIn.translateFragmentWith(Vec(shifts)).m2sPipe()
  val Seq(shift0, shift1) = streamShifts.fragment

  val shift = Mux(shift0 <= shift1, shift0, shift1) - 1// for signed input, -1 to protect sign bit
//  val shift = U(0, 6 bits) // for signed, -1 to protect sign bit
  val streamShift = streamShifts.translateFragmentWith(shift).m2sPipe()

  val streamRaw = dataIn.m2sPipe().m2sPipe()
  val Seq(data0, data1) = streamRaw.fragment

  val streamOut = streamRaw.translateFragmentWith(Vec(data0 |<< streamShift.fragment, data1 |<< streamShift.fragment))
  streamShift.ready := streamOut.ready

  streamOut.m2sPipe() >> dataOut

}

object Normalize32 extends App {

//  Config.synth(Normalize32())

  val golden = ArrayBuffer[Double]()
  val yours = ArrayBuffer[Double]()

  Config.sim.doSim(Normalize32()) { dut =>


    dut.clockDomain.forkStimulus(250 MHz)

    dut.dataIn.valid #= false
    dut.dataOut.ready #= true

    for (i <- 1 until 100) {
      val value0 = Random.nextInt(1 << 30) - (1 << 29)
      val value1 = Random.nextInt(1 << 30) - (1 << 29)
      dut.dataIn.valid #= true
      dut.dataIn.fragment(0) #= value0
      dut.dataIn.fragment(1) #= value1
      golden.append(value0.toDouble / value1.toDouble)

      if (dut.dataOut.valid.toBoolean) {
        val normalized0 = dut.dataOut.fragment(0).toBigInt.toDouble
        val normalized1 = dut.dataOut.fragment(1).toBigInt.toDouble
        yours.append(normalized0 / normalized1)
      }

      dut.clockDomain.waitSampling()
    }

    for (i <- 1 until 10){
      if (dut.dataOut.valid.toBoolean) {
        dut.dataIn.valid #= false
        val normalized0 = dut.dataOut.fragment(0).toBigInt.toDouble
        val normalized1 = dut.dataOut.fragment(1).toBigInt.toDouble
        yours.append(normalized0 / normalized1)
      }

      dut.clockDomain.waitSampling()
    }

  }

  golden.zip(yours).foreach { case (g, y) => assert(g == y, s"$g != $y")}

}
