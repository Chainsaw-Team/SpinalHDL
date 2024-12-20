import chainsaw.projects.xdma.daq.ChainsawDaqDemodulator
import io.jhdf.HdfFile
import spinal.core.sim._
import spinal.lib.sim._

import java.io._
import java.nio.ByteBuffer
import scala.collection.mutable.ArrayBuffer

object ReadHDF5WithJHDF extends App {

  // 从H5文件中读出数据
  val hdf5File = new HdfFile(new File("C:\\Users\\DAS\\Desktop\\DasData\\backup\\example.h5"))
  val rawData = hdf5File.getDatasetByPath("/raw_data").getData.asInstanceOf[Array[Array[Short]]]
  println(f"shape = ${rawData.length} X ${rawData.head.length}")

  // 将数据通过Stream接口写入解调单元进行仿真,仿真工具为verilator

  // 将数据保存为二进制文件
  val binaryFilePath: String = "C:\\Users\\DAS\\Desktop\\DasData\\demodulated.bin"

  // 写入二进制文件
  try {
    val dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(binaryFilePath)))
    try {
      // 写入矩阵的行数和列数 (写元数据，使用 int 类型存储行列数)
      // 将矩阵展平为一维 short 数组
      val flattenedMatrix = new Array[Short](rawData.length * rawData.head.length)
      var index = 0
      for (row <- rawData) {
        System.arraycopy(row, 0, flattenedMatrix, index, row.length)
        index += row.length
      }
      // 使用 ByteBuffer 将 short 写为字节数组
      val byteBuffer = ByteBuffer.allocate(flattenedMatrix.length * 2) // 每个 short 占 2 字节

      val shortBuffer = byteBuffer.asShortBuffer
      shortBuffer.put(flattenedMatrix)
      // 一次性写入矩阵数据
      dos.write(byteBuffer.array)
      System.out.println("矩阵已成功保存到二进制文件: " + binaryFilePath)
    } catch {
      case e: IOException =>
        e.printStackTrace()
    } finally if (dos != null) dos.close()
  }

  SimConfig.withWave.compile(ChainsawDaqDemodulator()).doSim { dut =>
    StreamDriver(dut.streamIn, dut.clockDomain) { payload =>
      payload.fragment #= 0
      payload.last #= false
      println("driver")
      true
    }
    val result = ArrayBuffer[Int]()
    StreamMonitor(dut.streamOut, dut.clockDomain) { payload =>
      result += payload.fragment.toInt
      println("monitor")
    }

    dut.clockDomain.forkStimulus(2)
    dut.clockDomain.waitSampling(100)


  }

  println(rawData(0)(0))
  println(rawData(0)(1))
  println(rawData(1)(0))
  println(rawData(1)(1))

  //  val rawDataString = rawData.map(_.mkString(" ")).mkString("\n")
  //  println(rawDataString)

}
