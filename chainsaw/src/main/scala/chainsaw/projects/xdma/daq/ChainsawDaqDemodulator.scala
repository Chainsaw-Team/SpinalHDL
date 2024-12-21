package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.core.sim.{SimBoolPimper, SimClockDomainHandlePimper, SimConfig, fork, simSuccess}
import spinal.lib._
import spinal.lib.bus.amba4.axis.sim.{Axi4StreamMaster, Axi4StreamSlave}
import spinal.lib.bus.amba4.axis.{Axi4Stream, Axi4StreamConfig}
import spinal.lib.sim.StreamDriver

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, Queue}
import scala.language.postfixOps
import scala.util.Random
import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._
import spinal.lib.fsm._
import spinal.lib.bus._

case class ChainsawDaqDemodulator() extends Module {

  // TODO: learning using pipeline utilities for Stream

  val streamIn = slave Stream Fragment(Vec(SInt(16 bits), 4)) // earlier data in lower index
  val streamOut = master Stream Fragment(Bits(64 bits)) // earlier data in lower bits
  val channelSelection = in Bool () // select X|Y
  val xyEnabled = in Bool () // enable both X and Y

  val channel0 = streamIn.fragment.take(2)
  val channel1 = streamIn.fragment.takeRight(2)

  // channel selection
  val summation = channel0
    .zip(channel1)
    .map { case (x, y) => ((x +^ y) >> 1).asBits }

  streamOut.fragment := summation(1) ## summation(1) ## summation(0) ## summation(0)
  streamOut.valid := streamIn.valid
  streamIn.ready := streamOut.ready
  streamOut.last := streamIn.last

}

object ChainsawDaqDemodulator extends App {
  SpinalConfig().generateVerilog(ChainsawDaqDemodulator())
}

case class DemodulatorTest() extends Module {

  val dataInConfig = Axi4StreamConfig(dataWidth = 16, useLast = true) // 2 channel, 4 int16 in each channel
  val dataIn = slave(Axi4Stream(dataInConfig))
  val dataOutConfig = Axi4StreamConfig(dataWidth = 4, useLast = true) // 1 channel, 2 int16 in each channel
  val dataOut = master(Axi4Stream(dataOutConfig))

  dataOut.data := dataIn.data.takeLow(32)
  dataOut.last := dataIn.last
  dataIn.ready := dataOut.ready
  dataOut.valid := dataIn.valid

}

object DemodulatorTest {

  def apply(pulses: mutable.Queue[mutable.Queue[Short]]) = {
    val result = ArrayBuffer[ArrayBuffer[Short]]()
    SimConfig.withWave.compile(ChainsawDaqDemodulator()).doSim { dut =>
      dut.clockDomain.forkStimulus(2)
      assert(pulses.head.length % dut.streamIn.fragment.length == 0)
      // driver thread
      fork {
        val driver = StreamDriver(dut.streamIn, dut.clockDomain) { payload =>
          if (pulses.isEmpty) {
            simSuccess()
          } else {
            payload.fragment.foreach { sint =>
              sint #= pulses.head.dequeue()
            }
            if (pulses.head.isEmpty) {
              println("next pulse")
              pulses.dequeue()
              payload.last #= true
            } else payload.last #= false
            true
          }
        }
      }
      // monitor thread
      fork {
        val randomizer = StreamReadyRandomizer(dut.streamOut, dut.clockDomain)
        randomizer.setFactor(1.0f) // downstream always ready
        val monitor = StreamMonitor(dut.streamOut, dut.clockDomain) { payload =>
          if (result.isEmpty) result.append(ArrayBuffer[Short]())
          val bits = payload.fragment.toBigInt.toString(2).reverse.padTo(64, '0').reverse
          val int16s = bits
            .grouped(16)
            .toSeq
            .reverse
            .map(str => Integer.parseInt(str, 2).toShort)
            .map(int => if (int > ((1 << 15) - 1)) int - (1 << 16) else int)
            .map(_.toShort)
          int16s.foreach(result.last += _)
          if (payload.last.toBoolean) result.append(ArrayBuffer[Short]())
        }
        monitor
      }

      while (true) { dut.clockDomain.waitSampling() }
    }
    result
  }
}
