package chainsaw.projects.xdma.daq

import org.scalatest.funsuite.AnyFunSuiteLike
import spinal.core.sim._
import spinal.core.{HertzNumber, IntToBuilder}
import spinal.lib.sim._

import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps

// TODO: index based -> data based
class DasDemodulatorTest extends AnyFunSuiteLike {

  def testDasDemodulator(
      enableDemodulation:Boolean,
      pulseGapPoints: Int,
      testConfigs: Seq[TestConfig]
  ): Array[Array[Int]] = {

    // reading stimulus
    val dataAllX = NpyReader("./chainsaw-python/das/raw_data_x.npy")
    val dataAllY = NpyReader("./chainsaw-python/das/raw_data_y.npy")
    val resultAllInt16 = ArrayBuffer[Array[Int]]()

    Config.sim.compile(DasDemodulator()).doSim { dut =>
      // state variables
      var pokeRowId, pokeColId, peekRowId, peekColId, peekFloatRowId, peekFloatColId = 0
      var pulseCount, pulseValidPoints = 0
      var dataX = Array[Array[Int]]()
      var dataY = Array[Array[Int]]()
      var resultInt16 = Array[Array[Int]]()
      var resultFloat32 = Array[Array[Float]]()

      // initializing threads
      dut.dataClockDomain.forkStimulus(250 MHz)
      // driver thread
      val threadDriver = fork {
        var gapCountDown = pulseGapPoints / 2
        var state = "run"

        val driver = StreamDriver(dut.streamIn, dut.dataClockDomain) { payload =>
          state match {
            case "run" => // poking pulse data into DUT
              dut.pulseValidPointsIn #= dataX(pokeRowId).length / 2
              payload.fragment(0) #= dataX(pokeRowId)(pokeColId) // x0
              payload.fragment(1) #= dataX(pokeRowId)(pokeColId + 1) // x1
              payload.fragment(2) #= dataY(pokeRowId)(pokeColId) // y0
              payload.fragment(3) #= dataY(pokeRowId)(pokeColId + 1) // y1
              pokeColId += 2
              val last = pokeColId == pulseValidPoints
              payload.last #= last
              if (last) {
                pokeRowId += 1
                pokeColId = 0
//                print(f"\rsimulating: $pokeRowId/$pulseCount")
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
                gapCountDown = pulseGapPoints / 2
                state = if (pokeRowId == pulseCount) "end" else "run"
              }
              false
            case "end" =>
              payload.last #= false
              if (pokeRowId == 0 && pokeColId == 0) state = "run"
              false
          }
        }
        driver.setFactor(1.0f) // continuous input
      }

      val threadMonitorFixed = fork { // monitor thread
        StreamReadyRandomizer(dut.streamOut, dut.dataClockDomain).setFactor(1.0f) // downstream always ready
        val monitor = StreamMonitor(dut.streamOut, dut.dataClockDomain) { payload =>
          // for int16 * 4
          val elements = payload.fragment.map(_.toBigInt.toInt)
          elements.zipWithIndex.foreach { case (int, i) => resultInt16(peekRowId)(peekColId + i) = int }
          peekColId += elements.length
          val last = payload.last.toBoolean
          if (last) {
            if (peekColId != pulseValidPoints * 2) println(s"pulse $peekRowId not finished: $peekColId / ${pulseValidPoints * 2}")
            peekRowId += 1
            peekColId = 0
          }

        }
      }

      def doSimOnce(config: TestConfig): Unit = {

        println(s"config = $config")
        pulseCount = config.pulseCount
        pulseValidPoints = config.pulseValidPoints
        // reset for stream Driver
        dataX = dataAllX.take(pulseCount).map(_.takeRight(pulseValidPoints))
        dataY = dataAllY.take(pulseCount).map(_.takeRight(pulseValidPoints))
        pokeRowId = 0
        pokeColId = 0
        // reset for stream monitor
        resultInt16 = Array.fill(pulseCount)(Array.fill(pulseValidPoints * 2)(0))
        resultFloat32 = Array.fill(pulseCount)(Array.fill(pulseValidPoints)(0f))

        // reset
        dut.dataClockDomain.assertReset()
        dut.dataClockDomain.waitActiveEdge(50)

        // initialization
        peekRowId = 0
        peekColId = 0
        peekFloatRowId = 0
        peekFloatColId = 0

        dut.streamIn.valid #= false
        dut.streamIn.last #= false
        dut.streamOut.ready #= false
        dut.demodulationEnabled #= config.demodulationEnabled == 1
        dut.gaugePointsIn #= config.gaugePoints / 2
        dut.pulseValidPointsIn #= pulseValidPoints / 2

        dut.dataClockDomain.waitActiveEdge(50)
        dut.dataClockDomain.deassertReset()

        waitUntil(pokeRowId == pulseCount)
        dut.dataClockDomain.waitSampling(200)
        println()
        println(
          s"peekRowId = $peekRowId, peekFloatRowId = $peekFloatRowId, peekColId = $peekColId, peekFloatColId = $peekFloatColId"
        )
        resultAllInt16 ++= resultInt16

      }

      testConfigs.foreach(doSimOnce)
      simSuccess()
    }
    resultAllInt16.toArray
  }

  test("test fixed pattern") {

    val testConfigs = Seq(
      TestConfig(gaugePoints = 100, pulseCount = 5, pulseValidPoints = 2000, demodulationEnabled = 1),
      TestConfig(gaugePoints = 100, pulseCount = 5, pulseValidPoints = 2000, demodulationEnabled = 0),
      TestConfig(gaugePoints = 50, pulseCount = 5, pulseValidPoints = 1000, demodulationEnabled = 1),
    )

    val result = testDasDemodulator(enableDemodulation = true, pulseGapPoints = 500, testConfigs)

    writeInt16("full_result.bin", result.flatten)

  }

}
