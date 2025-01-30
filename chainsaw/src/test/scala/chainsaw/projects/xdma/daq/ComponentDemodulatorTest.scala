package chainsaw.projects.xdma.daq

import org.scalatest.funsuite.AnyFunSuiteLike
import spinal.core.sim._
import spinal.core.{HertzNumber, IntToBuilder}
import spinal.lib.sim._

import java.io.FileOutputStream
import java.nio.{ByteBuffer, ByteOrder}
import scala.language.postfixOps

class ComponentDemodulatorTest extends AnyFunSuiteLike {

  /** @param dataX two-dimensional data array, each row represent a pulse
    * @param dataY same as pulseX
    */
  def testComponentDemodulator(
      carrierFreq: HertzNumber,
      dataX: Seq[Seq[Short]],
      dataY: Seq[Seq[Short]],
      pulseGapPoints: Int
  ): (Array[Array[Int]], Array[Array[Float]]) = {
    val pulseCount = dataX.length
    val pulseValidPoints = dataX.head.length
    val resultInt16 = Array.fill(pulseCount)(Array.fill(pulseValidPoints * 2)(0))
    val resultFloat32 = Array.fill(pulseCount)(Array.fill(pulseValidPoints)(0f))
    var pokeRowId, pokeColId, peekRowId, peekColId, peekFloatRowId, peekFloatColId = 0

    Config.sim.compile(ComponentDemodulator(carrierFreq, debug = true)).doSim { dut =>
      // initialization
      dut.streamIn.valid #= false
      dut.streamIn.last #= false
      dut.streamOut.ready #= false
      dut.gaugePointsIn #= 50
      dut.clockDomain.forkStimulus(250 MHz)

      // driver thread
      fork {
        var finalCountDown = 200 // extra cycles for monitor thread to collect output data
        var gapCountDown = pulseGapPoints / 2
        var state = "run"

        val driver = StreamDriver(dut.streamIn, dut.clockDomain) { payload =>
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
              finalCountDown -= 1
              if (finalCountDown <= 0) {
                println()
                simSuccess()
              }
              false
          }
        }
        driver.setFactor(1.0f) // continuous input
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

      fork { // monitor thread
        StreamReadyRandomizer(dut.streamOutFloat, dut.clockDomain).setFactor(1.0f) // downstream always ready
        val monitor = StreamMonitor(dut.streamOutFloat, dut.clockDomain) { payload =>
          // for int16 * 4
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

      while (true) { dut.clockDomain.waitSampling() }
    }
    (resultInt16, resultFloat32)
  }

  def writeInt16(fileName: String, data: Seq[Int]): Unit = {
    val outputStream = new FileOutputStream(fileName)
    try {
      // 将数据按照 Little Endian 写入
      data.foreach { value =>
        val shortValue: Short = value.toShort // 转换为 Short 类型
        val bytes = Array(
          (shortValue & 0xff).toByte, // 取低字节
          ((shortValue >> 8) & 0xff).toByte // 取高字节
        )
        outputStream.write(bytes)
      }
    } finally {
      outputStream.close()
    }
  }

  def writeFloat32(fileName: String, data: Seq[Float]): Unit = {
    val outputStream = new FileOutputStream(fileName)
    try {
      // 将数据按照 Little Endian 写入
      data.foreach { value =>
        // 使用 ByteBuffer 将 Float 转换为 Little Endian 的字节数组
        val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putFloat(value)
        outputStream.write(buffer.array()) // 写入字节
      }
    } finally {
      outputStream.close()
    }
  }

  test("test fixed pattern") {

    val pulseCount = 10
    val pulseValidPoints = 2000

    val dataX = NpyReader("./chainsaw-python/das/raw_data_x.npy")
      .take(pulseCount)
      .toSeq
      .map(_.takeRight(pulseValidPoints).map(_.toShort).toSeq)

    val dataY = NpyReader("./chainsaw-python/das/raw_data_y.npy")
      .take(pulseCount)
      .toSeq
      .map(_.takeRight(pulseValidPoints).map(_.toShort).toSeq)

    val (result, resultFloat) = testComponentDemodulator(80 MHz, dataX, dataY, 0)

    writeFloat32("result_float32.bin", resultFloat.flatten)

    val upperResult = result.flatten.grouped(4).toSeq.flatMap(_.takeRight(2))
    val lowerResult = result.flatten.grouped(4).toSeq.flatMap(_.take(2))

    writeInt16("upper_result.bin", upperResult)
    writeInt16("lower_result.bin", lowerResult)

//     cd
//     cd  ~/SpinalHDL/simWorkspace/ComponentDemodulator/xsim
//     echo -e "open_wave_database ComponentDemodulator.wdb\nopen_wave_config /home/ltr/SpinalHDL/ComponentDemodulator.wcfg" > view_wave.tcl
//     vivado -source view_wave.tcl

  }

}
