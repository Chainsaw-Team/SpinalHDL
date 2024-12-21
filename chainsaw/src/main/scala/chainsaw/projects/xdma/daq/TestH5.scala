package chainsaw.projects.xdma.daq

import chainsaw.projects.xdma.daq.{ChainsawDaqDemodulator, DemodulatorTest}
import io.jhdf.HdfFile
import spinal.core.sim._
import spinal.lib.sim._

import java.io._
import java.nio.ByteBuffer
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, Queue}

object TestH5 extends App {

  // 从H5文件中读出数据
  val hdf5File = new HdfFile(new File("/home/ltr/DasData/example.h5"))
  val rawData = hdf5File.getDatasetByPath("/raw_data").getData.asInstanceOf[Array[Array[Short]]]
  val stimulus = mutable.Queue(rawData.map(row => mutable.Queue(row: _*)): _*)
  println(f"shape = ${rawData.length} X ${rawData.head.length}")
  Seq.tabulate(1,4) { (i, j) => println(f"${stimulus(i)(j)}")}

  // 将数据通过Stream接口写入解调单元进行仿真,仿真工具为verilator
  val demodulated = DemodulatorTest(stimulus.take(2)) // 取部分pulses
  Seq.tabulate(1,4) { (i, j) => println(f"${demodulated(i)(j)}")}

  println(demodulated.map(_.length).mkString(" "))

  // 写入二进制文件-
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
