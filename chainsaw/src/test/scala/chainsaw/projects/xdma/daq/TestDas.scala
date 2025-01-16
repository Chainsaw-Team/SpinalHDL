package chainsaw.projects.xdma.daq

import io.jhdf.HdfFile

import java.io.{BufferedOutputStream, DataOutputStream, File, FileOutputStream}
import java.nio.ByteBuffer
import scala.collection.mutable

object TestDas {

  def main(args: Array[String]): Unit = {
    val workingDir = new File("/home/ltr/DasPython/DasTest/test")
    // 从H5文件中读出数据
    val hdf5File = new HdfFile(new File(workingDir, "example.h5"))
    val rawDataX = hdf5File.getDatasetByPath("/raw_data_x").getData.asInstanceOf[Array[Array[Short]]]
    val rawDataY = hdf5File.getDatasetByPath("/raw_data_y").getData.asInstanceOf[Array[Array[Short]]]
    val stimulusX = mutable.Queue(rawDataX.map(row => mutable.Queue(row: _*)): _*).take(10)
    val stimulusY = mutable.Queue(rawDataY.map(row => mutable.Queue(row: _*)): _*).take(10)

    // 将数据通过Stream接口写入解调单元进行仿真,仿真工具为XSim
    val demodulated = TestDemodulator(stimulusX, stimulusY) // 取部分pulses
    println(f"output pulse count = ${demodulated.count(_.nonEmpty)}")

    // 写入二进制文件
    val binaryFile: String = new File(workingDir, "example.bin").getAbsolutePath
    val dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(binaryFile)))
    val flattenedMatrix = new Array[Short](rawDataX.length * rawDataX.head.length)
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

}
