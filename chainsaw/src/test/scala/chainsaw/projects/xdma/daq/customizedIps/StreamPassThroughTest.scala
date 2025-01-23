package chainsaw.projects.xdma.daq.customizedIps

import org.scalatest.funsuite.AnyFunSuiteLike
import spinal.core.IntToBuilder
import spinal.core.sim._
import spinal.lib.sim.{StreamDriver, StreamMonitor, StreamReadyRandomizer}

import scala.language.postfixOps

/** template for simulating modules supporting Stream interface
  */
class StreamPassThroughTest extends AnyFunSuiteLike {

  def testStreamModule(
      data: Seq[Seq[Short]],
      frameGapPoints: Int,
      finalPoints: Int,
      upstreamDutyCycle: Double = 1.0,
      downstreamDutyCycle: Double = 1.0
  ): Array[Array[Int]] = {

    val pulseCount = data.length
    val result = data.map(frame => Array.fill(frame.length)(0)).toArray
    var pokeRowId, pokeColId, peekRowId, peekColId = 0

    SimConfig.withWave.compile(StreamPassThrough()).doSim { dut =>
      // driver thread
      fork {
        var finalCountDown = finalPoints // extra cycles for monitor thread to collect output data
        var gapCountDown = frameGapPoints
        var state = "run"

        val driver = StreamDriver(dut.dataIn, dut.clockDomain) { payload =>
          state match {
            case "run" => // poking pulse data into DUT
              payload.data #= data(pokeRowId)(pokeColId) // x0
              pokeColId += 1
              val last = pokeColId == data(pokeRowId).length
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
                gapCountDown = frameGapPoints
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
        driver.setFactor(upstreamDutyCycle.toFloat)
      }

      fork { // monitor thread
        StreamReadyRandomizer(dut.dataOut, dut.clockDomain).setFactor(downstreamDutyCycle.toFloat) // downstream always ready
        val monitor = StreamMonitor(dut.dataOut, dut.clockDomain) { payload =>
          result(peekRowId)(peekColId) = payload.data.toInt
          peekColId += 1
          val last = peekColId == result(peekRowId).length
          if (last) {
            peekRowId += 1
            peekColId = 0
          }

        }
      }

      // main thread = initialization + running
      dut.dataIn.valid #= false
      dut.dataIn.last #= false
      dut.dataOut.ready #= false
      dut.clockDomain.forkStimulus(100 MHz) // using actual frequency to avoid failures in IP simulation
      while (true) { dut.clockDomain.waitSampling() }
    }
    result
  }

  test("test fixed pattern") {

    val frameCount = 5
    val frameValidPoints = 100
    val frameGapPoints = 0
    val finalPoints = 100
    val upstreamDutyCycle = 0.5
    val downstreamDutyCycle = 0.5

    val raw = (10 until frameValidPoints + 10).map(_.toShort)
    val data = Seq.fill(frameCount)(raw)
    val result = testStreamModule(data, frameGapPoints, finalPoints, upstreamDutyCycle, downstreamDutyCycle)
    result.foreach(ret => assert(ret.zip(raw).forall { case (a, b) => a == b }, s"result = ${ret.mkString(",")}"))

  }
}
