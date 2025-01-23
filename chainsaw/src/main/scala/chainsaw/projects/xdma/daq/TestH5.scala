package chainsaw.projects.xdma.daq

import chainsaw.projects.xdma.daq.{ComponentDemodulator, DemodulatorTest}
import io.jhdf.HdfFile
import spinal.core.assert
import spinal.core.sim._
import spinal.lib.sim._

import java.io._
import java.nio.ByteBuffer
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, Queue}

object DemodulatorTest {

  def apply(pulses: mutable.Queue[mutable.Queue[Short]]): mutable.Seq[ArrayBuffer[Short]] = {
    val result = ArrayBuffer[ArrayBuffer[Short]]()
    SimConfig.withWave.compile(DasDemodulator()).doSim { dut =>
      dut.clockDomain.forkStimulus(2)
      assert(pulses.head.length % dut.streamIn.fragment.length == 0)
      // driver thread
      fork {
        var count = 10000 // extra cycles for monitor thread to collect output data
        val driver = StreamDriver(dut.streamIn, dut.clockDomain) { payload =>
          if (pulses.isEmpty) {
            if (count == 0) simSuccess()
            else {
              count -= 1
              false
            }
          } else {
            val v0 = pulses.head.dequeue()
            val v1 = pulses.head.dequeue()
            payload.fragment(0) #= v0
            payload.fragment(1) #= v1
            payload.fragment(2) #= v0
            payload.fragment(3) #= v1
//            payload.fragment.foreach { sint =>
//              sint #= pulses.head.dequeue()
//            }
            if (pulses.head.isEmpty) {
              println("next pulse")
              pulses.dequeue()
              payload.last #= true
            } else payload.last #= false
            true
          }
        }
      }

      fork { // monitor thread
        StreamReadyRandomizer(dut.streamOut, dut.clockDomain).setFactor(1.0f) // downstream always ready
        val monitor = StreamMonitor(dut.streamOut, dut.clockDomain) { payload =>
          if (result.isEmpty) result.append(ArrayBuffer[Short]())
          val bits = payload.fragment.toBigInt.toString(2).reverse.padTo(64, '0').reverse
          val uint16s = bits.grouped(16).toSeq.reverse.map(str => Integer.parseInt(str, 2).toShort)
          val validUint16s = uint16s.grouped(2).map(_.head).toSeq
          val sint16s = validUint16s.map(int => if (int > ((1 << 15) - 1)) int - (1 << 16) else int).map(_.toShort)
          sint16s.foreach(result.last += _)
          if (payload.last.toBoolean) result.append(ArrayBuffer[Short]())
        }
      }

      while (true) { dut.clockDomain.waitSampling() }
    }
    result.init
  }
}

object TestH5 extends App {

  // 从H5文件中读出数据
  val hdf5File = new HdfFile(new File("/home/ltr/DasPython/DasTest/test/spatial_resolution.h5"))
  val rawData = hdf5File.getDatasetByPath("/raw_data_x").getData.asInstanceOf[Array[Array[Short]]]
  val stimulus = mutable.Queue(rawData.map(row => mutable.Queue(row: _*)): _*)
  println(f"shape = ${rawData.length} X ${rawData.head.length}")

  // 将数据通过Stream接口写入解调单元进行仿真,仿真工具为verilator
  val demodulated = DemodulatorTest(stimulus) // 取部分pulses
  println(demodulated.map(_.length).mkString(" "))

  // 写入二进制文件
  val binaryFile: String = "/home/ltr/DasData/example.bin"
  val dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(binaryFile)))
  val flattenedMatrix = new Array[Short](rawData.length * rawData.head.length)
  var index = 0
  for (row <- demodulated) {
    System.arraycopy(row.toArray, 0, flattenedMatrix, index, row.length)
    index += row.length
  }
  val byteBuffer = ByteBuffer.allocate(flattenedMatrix.length * 2)
  val shortBuffer = byteBuffer.asShortBuffer
  shortBuffer.put(flattenedMatrix)
  dos.write(byteBuffer.array)
}

