//package chainsaw.projects.xdma.daq.ku060Ips
//
//import chainsaw.projects.xdma.daq.Config
//import org.scalatest.funsuite.AnyFunSuiteLike
//import spinal.core._
//import spinal.core.sim._
//import spinal.lib._
//import spinal.lib.sim._
//import spinal.lib.fsm._
//import spinal.lib.bus._
//import spinal.lib.experimental.math.Floating32
//
//import scala.language.postfixOps
//
//class Int32ToFloatTest extends AnyFunSuiteLike {
//
//  case class Int32ToFloatDut() extends Module {
//
//    val dataIn = in SInt (32 bits)
//    val dataOut = out(Floating32())
//    val dut = Int32ToFloat()
//
//    dut.s_axis_a.last.clear()
//    dut.s_axis_a.fragment.assignFromBits(dataIn.asBits)
//    dataOut.assignFromBits(dut.m_axis_result.fragment)
//
//    dut.s_axis_a.valid.set()
//    dut.m_axis_result.ready.set()
//  }
//
//  test("Int32ToFloat") {
//
//    Config.sim.doSim(new Int32ToFloatDut) { dut =>
//      dut.clockDomain.forkStimulus(250 MHz)
//
//      (0 until 100).foreach { i =>
//        dut.dataIn #= -5284432
//        dut.clockDomain.waitSampling()
//        println(dut.dataOut.toFloat)
//      }
//
//      dut.clockDomain.waitSampling(10)
//
//    }
//
//  }
//
//}
