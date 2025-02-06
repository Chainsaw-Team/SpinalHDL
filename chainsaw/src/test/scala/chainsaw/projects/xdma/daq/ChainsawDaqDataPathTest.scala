package chainsaw.projects.xdma.daq

import org.scalatest.funsuite.AnyFunSuiteLike
import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.amba4.axilite.{AxiLite4, AxiLite4Config}
import spinal.lib.sim._

import scala.collection.mutable.ArrayBuffer

class ChainsawDaqDataPathTest extends AnyFunSuiteLike {

  case class ChainsawDaqDataPathDut(testConfig: TestConfig) extends Module {

    // for testability of ChainsawDaqDataPath
    val dataIn = slave(Stream(Vec(SInt(16 bits), 8)))
    val dataOut = master(Stream(Fragment(Vec(SInt(16 bits), 4))))
    val controlInConfig: AxiLite4Config = AxiLite4Config(addressWidth = 32, dataWidth = 32)
    val controlIn = slave(AxiLite4(controlInConfig))

    val core = ChainsawDaqDataPath(
      defaultGaugePoints = testConfig.gaugePoints,
      defaultPulseValidPoints = testConfig.pulseValidPoints,
      defaultDemodulationEnabled = testConfig.demodulationEnabled
    )

    core.dataIn.fromStream(dataIn.translateWith(dataIn.payload.asBits))
//    core.dataOut.toStreamFragment.translateWith(core.dataOut.) >> dataOut


  }

  def testDatapath(
      carrierFreq: HertzNumber,
      pulseGapPoints: Int,
      testConfig: TestConfig
  ): (Array[Array[Int]], Array[Array[Float]]) = {

    // reading stimulus
    val dataAllX = NpyReader("./chainsaw-python/das/raw_data_x.npy")
    val dataAllY = NpyReader("./chainsaw-python/das/raw_data_y.npy")
    val resultAllInt16 = ArrayBuffer[Array[Int]]()
    val resultAllFloat32 = ArrayBuffer[Array[Float]]()

    Config.sim
      .compile(
        ChainsawDaqDataPath(
          defaultGaugePoints = testConfig.gaugePoints,
          defaultPulseValidPoints = testConfig.pulseValidPoints,
          defaultDemodulationEnabled = testConfig.demodulationEnabled
        )
      )
      .doSim { dut =>
        // state variables
        var pokeRowId, pokeColId, peekRowId, peekColId, peekFloatRowId, peekFloatColId = 0
        var pulseCount, pulseValidPoints = 0
        var dataX = Array[Array[Int]]()
        var dataY = Array[Array[Int]]()
        var resultInt16 = Array[Array[Int]]()
        var resultFloat32 = Array[Array[Float]]()

        // initializing threads
        dut.clockDomain.forkStimulus(250 MHz)
        // driver thread
        val threadDriver = fork {
          var gapCountDown = pulseGapPoints / 2
          var state = "run"

          val driver = StreamDriver(dut.dataIn, dut.clockDomain) { payload =>
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
          StreamReadyRandomizer(dut.streamOut, dut.clockDomain).setFactor(1.0f) // downstream always ready
          val monitor = StreamMonitor(dut.streamOut, dut.clockDomain) { payload =>
            // for int16 * 4
            val elements = payload.fragment.map(_.toBigInt.toInt)
            elements.zipWithIndex.foreach { case (int, i) => resultInt16(peekRowId)(peekColId + i) = int }
            peekColId += elements.length
            val last = peekColId == pulseValidPoints * 2
            if (last) {
              peekRowId += 1
              peekColId = 0
            }

          }
        }

        val threadMonitorFloat = fork { // monitor thread
          StreamReadyRandomizer(dut.streamOutFloat, dut.clockDomain).setFactor(1.0f) // downstream always ready
          val monitor = StreamMonitor(dut.streamOutFloat, dut.clockDomain) { payload =>
            // for float32 * 2
            val elements = payload.fragment.map(_.toFloat)
            elements.zipWithIndex.foreach { case (int, i) => resultFloat32(peekFloatRowId)(peekFloatColId + i) = int }
            peekFloatColId += elements.length
            val last = peekFloatColId == pulseValidPoints
            if (last) {
              peekFloatRowId += 1
              peekFloatColId = 0
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
          peekRowId = 0
          peekColId = 0
          peekFloatRowId = 0
          peekFloatColId = 0
          println(s"params done")

          // initialization
          dut.streamIn.valid #= false
          dut.streamIn.last #= false
          dut.streamOut.ready #= false
          dut.gaugePointsIn #= config.gaugePoints / 2
          dut.pulseValidPointsIn #= pulseValidPoints / 2
          println(s"params done")
          // reset
          dut.clockDomain.assertReset()
          dut.clockDomain.waitActiveEdge(100)
          dut.clockDomain.deassertReset()
          println(s"reset done")

          waitUntil(pokeRowId == pulseCount)
          dut.clockDomain.waitSampling(200)
          println()
          println(
            s"peekRowId = $peekRowId, peekFloatRowId = $peekFloatRowId, peekColId = $peekColId, peekFloatColId = $peekFloatColId"
          )
          resultAllInt16 ++= resultInt16
          resultAllFloat32 ++= resultFloat32

        }

        testConfigs.foreach(doSimOnce)
        simSuccess()
      }
    (resultAllInt16.toArray, resultAllFloat32.toArray)
  }

  test("test fixed pattern") {

    val testConfigs = Seq(
      TestConfig(100, 5, 2000),
      TestConfig(50, 5, 1000)
    )
    val (result, resultFloat) = testDatapath(80 MHz, 0, testConfigs)
    println(s"result lengths = ${result.map(_.length).mkString(",")}")

    writeFloat32("result_float32.bin", resultFloat.flatten)

    val upperResult = result.flatten.grouped(4).toSeq.flatMap(_.takeRight(2))
    val lowerResult = result.flatten.grouped(4).toSeq.flatMap(_.take(2))

    writeInt16("upper_result.bin", upperResult)
    writeInt16("lower_result.bin", lowerResult)

  }

}
