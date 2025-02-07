package chainsaw.projects.xdma

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.amba4.axis.Axi4Stream
import spinal.lib.bus.amba4.axis.Axi4Stream.{Axi4Stream, Axi4StreamBundle}
import spinal.lib.UIntPimper
import spinal.lib.generator_backup.Handle.initImplicit

import java.io.{File, FileOutputStream}
import java.nio.{ByteBuffer, ByteOrder}
import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps

package object daq {

  val DAS_CLOCK_DOMAIN_CONFIG = ClockDomainConfig(resetKind = SYNC, resetActiveLevel = LOW)
  val DATA_FREQUENCY = FixedFrequency(250 MHz)

  val daqScalaSource = new File("./chainsaw/src/main/scala/chainsaw/projects/xdma/daq")
  val axku062DaqRtlDir = new File("./Axku062Daq")
  val axku5DaqRtlDir = new File("./Axku5Daq")

  object Config { // default RTL generation &
    def gen: SpinalConfig = SpinalConfig(
      targetDirectory = "hw/gen",
      defaultClockDomainFrequency = DATA_FREQUENCY,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
      onlyStdLogicVectorAtTopLevelIo = true
    )

    def sim: SpinalSimConfig = {
      SimConfig.withXSim.withWave // using XSim
        .withConfig(gen)
        .withXilinxDevice("XCKU060-FFVA1156-2-i".toLowerCase())
        .withXSimSourcesPaths(
          xciSourcesPaths = ArrayBuffer(),
          bdSourcesPaths = ArrayBuffer()
        )
    }
  }

  def fragment[T <: Data](data: T, last: Bool): Fragment[T] = {
    val fragment = Fragment(HardType(data))
    fragment.fragment := data
    fragment.last := last
    fragment
  }

  implicit class StreamFragmentUtils[T <: Data](stream: Stream[Fragment[T]]) {
    def translateFragmentWith[T2 <: Data](data: T2): Stream[Fragment[T2]] =
      stream.translateWith(fragment(data, stream.last))
  }

  import org.nd4j.linalg.factory.Nd4j
  import org.nd4j.linalg.api.ndarray.INDArray

  object NpyReader {
    def apply(npyPath: String): Array[Array[Int]] = {
      // 提供 .npy 文件路径

      // 使用 ND4J 加载 .npy 文件
      val data: INDArray = Nd4j.createFromNpyFile(new java.io.File(npyPath))

      // 如果需要将数据转换为 Scala 的二维数组
      val shape = data.shape()
      val rows = shape(0)
      val cols = shape(1)
      Array.tabulate(rows.toInt, cols.toInt) { (i, j) => data.getInt(i, j) }

    }
  }

  // top-level parameters
  val GAUGE_POINTS_MAX = 250
//  val PULSE_VALID_POINTS_MAX = 125000 // 50km / 0.2m / 2
  val PULSE_VALID_POINTS_MAX = 5000 // 50km / 0.2m / 2
  val PULSE_PERIOD_POINTS_MAX = 1 << 28
  val CARRIER_FREQS = Seq(80 MHz)
  val OUTPUT_STRAIN_RESOLUTION =
    0.025 / 1e6 / 0.23 / (1 << 13) // 0.23rad <-> 0.025με / gauge length, output format fixed16_13

  println("system parameters:")
  println(s"\tinterrogation rate min = ${1.0 / (PULSE_PERIOD_POINTS_MAX * 4).toDouble * 1e9} Hz")
  println(s"\tstrain/gauge length resolution = ${OUTPUT_STRAIN_RESOLUTION * 1e12}pε/m")
  println(s"\tgauge length max = ${GAUGE_POINTS_MAX * 2 * 0.2}m")
  println(s"\tfiber length max = ${PULSE_VALID_POINTS_MAX * 2 * 0.2}m")
  println()

  case class TestConfig(gaugePoints: Int, pulseCount: Int, pulseValidPoints: Int, demodulationEnabled: Int = 0)

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

}
