package chainsaw.projects.xdma.daq

import spinal.core.assert
import spinal.core.sim._
import spinal.lib.sim.{StreamDriver, StreamMonitor, StreamReadyRandomizer}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object TestDemodulator {

  def apply(
      pulsesX: mutable.Queue[mutable.Queue[Short]],
      pulsesY: mutable.Queue[mutable.Queue[Short]]
  ): mutable.Seq[ArrayBuffer[Short]] = {
    val result = ArrayBuffer[ArrayBuffer[Short]]()
    Config.sim.compile(ChainsawDaqDemodulator()).doSim { dut =>
      dut.dataClockDomain.forkStimulus(2)
      dut.en #= true
      assert(pulsesX.head.length % dut.streamIn.fragment.length == 0)
      val pulseCount = pulsesX.length
      var pulseId = 0
      // driver thread
      // TODO: extract methods to peek & poke frames using StreamDriver & StreamMonitor
      fork {
        // pulse by pulse simulation
        var count = 10000 // extra cycles for monitor thread to collect output data
        var gapCount = 1000 // cycles inserted between pulses
        var state = "run"

        val driver = StreamDriver(dut.streamIn, dut.dataClockDomain) { payload =>
          state match {
            case "run" =>
              val v0 = pulsesX.head.dequeue()
              val v1 = pulsesX.head.dequeue()
              val v2 = pulsesY.head.dequeue()
              val v3 = pulsesY.head.dequeue()
              payload.fragment(0) #= v0
              payload.fragment(1) #= v1
              payload.fragment(2) #= v2
              payload.fragment(3) #= v3
              val last = pulsesX.head.isEmpty
              payload.last #= last
              if (last) {
                pulseId += 1
                print(f"\rsimulating: ${pulseId}/$pulseCount")
                pulsesX.dequeue()
                pulsesY.dequeue()
                state = "gap"
              }
              true
            case "gap" =>
              payload.last #= false
              gapCount -= 1
              if (gapCount == 0) {
                gapCount = 1000
                if (pulsesX.isEmpty) state = "end"
                else state = "run"
              }
              false
            case "end" =>
              payload.last #= false
              count -= 1
              if (count == 0) {
                println()
                simSuccess()
              }
              false
          }
        }
        driver.setFactor(1.0f) // continuous input
      }

      fork { // monitor thread
        StreamReadyRandomizer(dut.streamOut, dut.dataClockDomain).setFactor(1.0f) // downstream always ready
        val monitor = StreamMonitor(dut.streamOut, dut.dataClockDomain) { payload =>
          if (result.isEmpty) result.append(ArrayBuffer[Short]())
          val bits = payload.fragment.toBigInt.toString(2).reverse.padTo(64, '0').reverse
          val uint16s = bits.grouped(16).toSeq.reverse.map(str => Integer.parseInt(str, 2).toShort)
          val validUint16s = uint16s.grouped(2).map(_.head).toSeq
          val sint16s = validUint16s.map(int => if (int > ((1 << 15) - 1)) int - (1 << 16) else int).map(_.toShort)
          sint16s.foreach(result.last += _)
          if (payload.last.toBoolean) result.append(ArrayBuffer[Short]())
        }
      }

      while (true) { dut.dataClockDomain.waitSampling() }
    }
    result.init
  }
}
