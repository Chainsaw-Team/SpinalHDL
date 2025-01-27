package chainsaw.projects.xdma.daq.customizedIps

import chainsaw.projects.xdma.daq.{ComponentDemodulator, Config}
import org.scalatest.funsuite.AnyFunSuiteLike
import spinal.core._
import spinal.core.sim._
import spinal.lib.sim.{StreamDriver, StreamMonitor, StreamReadyRandomizer}

import scala.language.postfixOps

class DataDelayTest extends AnyFunSuiteLike {

  def testDataDelay(
      data: Seq[Seq[Short]],
      delayMax: Int,
      delays: Seq[Int],
      paddingValue: Int,
      pulseGapPoints: Int
  ): Array[Array[Int]] = {
    val pulseCount = data.length
    val pulseValidPoints = data.head.length
    val result = Array.fill(pulseCount)(Array.fill(pulseValidPoints)(0))
    var pokeRowId, pokeColId, peekRowId, peekColId = 0

    val dataWidth = 8

//    Config.sim.compile(DataDelay(DataDelayConfig(dataWidth, delayMax, paddingValue))).doSim { dut =>
    SimConfig.withWave.compile(DataDelay(DataDelayConfig(HardType(Bits(8 bits)), delayMax, paddingValue))).doSim { dut =>
      // initialization
      dut.dataIn.valid #= false
      dut.dataIn.last #= false
      dut.dataOut.ready #= true
      dut.delayIn #= delays.head
      dut.clockDomain.forkStimulus(2)

      // driver thread
      fork {
        var finalCountDown = 10000 // extra cycles for monitor thread to collect output data
        var gapCountDown = pulseGapPoints
        var state = "run"

        val driver = StreamDriver(dut.dataIn, dut.clockDomain) { payload =>
          state match {
            case "run" => // poking pulse data into DUT
              dut.delayIn #= delays(pokeRowId)
              payload.fragment #= data(pokeRowId)(pokeColId) // x0
//              println(f"poking ${data(pokeRowId)(pokeColId)},rowId=$pokeRowId,colId=$pokeColId")
              pokeColId += 1
              val last = pokeColId == pulseValidPoints
              payload.last #= last
              if (last) {
                pokeRowId += 1
                pokeColId = 0
                print(f"\rsimulating: $pokeRowId/$pulseCount")
                state =
                  if (gapCountDown > 0) "gap"
                  else if (pokeRowId == pulseCount) "end"
                  else "run"
              }
              true
            case "gap" => // poking gap data(invalid) into DUT
              payload.last #= false
              gapCountDown -= 1
              if (gapCountDown <= 0) { // state transition
                gapCountDown = pulseGapPoints
                state = if (pokeRowId == pulseCount) "end" else "run"
              }
              false
            case "end" =>
              payload.last #= false
              finalCountDown -= 1
              if (finalCountDown <= 0) {
                println()
                simSuccess()
              }
              false
          }
        }
        driver.setFactor(0.5f) // continuous input
      }

      def twosComplementToInt(bits: String): Int = {
        // 转换为整数，检查最高位（符号位）
        val value = Integer.parseInt(bits, 2)
        if (bits.head == '1') {
          // 如果符号位是1，表示负数，要进行2's complement修正
          value - (1 << bits.length)
        } else {
          value
        }
      }

      fork { // monitor thread
        StreamReadyRandomizer(dut.dataOut, dut.clockDomain).setFactor(0.5f) // downstream always ready
        val monitor = StreamMonitor(dut.dataOut, dut.clockDomain) { payload =>
          result(peekRowId)(peekColId) = payload.fragment.head.toInt >> dataWidth
//          println(f"appending ${payload.data.toInt},rowId=$peekRowId,colId=$peekColId")
          peekColId += 1
          val last = peekColId == pulseValidPoints
          if (last) {
            peekRowId += 1
            peekColId = 0
          }


        }
      }

      while (true) { dut.clockDomain.waitSampling() }
    }
    result // as some function relies on signal last, behavior of the first pulse may differ from the others, drop it
  }

  test("test fixed pattern") {

    val pulseValidPoints = 100
    val delayMax = 50
    val paddingValue = 17
    val delays = Seq(1, 3, 5, delayMax, 5, 3, 1) // require delay > 1

    def testWithGap(pulseGapPoints: Int): Unit = {
      val raw = (10 until pulseValidPoints + 10).map(_.toShort)
      val data = Seq.fill(delays.length)(raw)

      (0 until 10).foreach { _ =>
        val result = testDataDelay(data, delayMax, delays, paddingValue, pulseGapPoints)
        println(result.map(_.mkString(",")).mkString("\n"))
        result.zip(delays).foreach { case (delayed, delay) =>
          assert(
            delayed
              .zip(Seq.fill(delay)(paddingValue) ++ raw.take(pulseValidPoints - delay))
              .forall { case (a, b) => a == b },
            s"delayed = ${delayed.mkString(",")}"
          )
        }
      }
    }

    testWithGap(0)
    testWithGap(10)

  }
}
