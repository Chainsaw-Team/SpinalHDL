package chainsaw.projects.xdma

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import spinal.lib.eda.xilinx._

import java.io.{File, FileOutputStream}
import java.nio.{ByteBuffer, ByteOrder}
import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps
import scala.reflect.ClassTag

import java.io.PrintWriter
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

package object daq {

  //////////
  // DAS system parameters
  //////////
  val DAS_CLOCK_DOMAIN_CONFIG = ClockDomainConfig(resetKind = SYNC, resetActiveLevel = LOW)
  val DATA_FREQUENCY = FixedFrequency(250 MHz)
  val CONTROL_FREQUENCY = FixedFrequency(125 MHz)
  val GAUGE_POINTS_MAX = 250 // 100m / 0.2m / 2
  val PULSE_VALID_POINTS_MAX = 125000 // 50km / 0.2m / 2
  val PULSE_PERIOD_POINTS_MAX = 1 << 28
  val PULSE_PULSE_DELAY_POINTS_MAX = 250
  val PULSE_0_FREQS = Seq(80 MHz)
  val PULSE_1_FREQS = Seq(200 MHz)
  val DAS_DATAPATH_WIDTH = 16
  // 0.23rad <-> 0.025με / gauge length, output format fixed16_13
  val OUTPUT_STRAIN_RESOLUTION = 0.025 / 1e6 / 0.23 / (1 << 13)

  val TARGET_DEVICE =
    new XilinxDevice(family = UltraScale, part = "XCKU060-FFVA1156-2-i".toLowerCase(), fMax = 200 MHz)
//    new XilinxDevice(family = UltraScalePlus, part = "xcku5p-ffvb676-2-i".toLowerCase(), fMax = 200 MHz)

  // target device

  // width alongside datapath
  val shiftedSignificandWidth = DAS_DATAPATH_WIDTH + (DAS_DATAPATH_WIDTH - 1)
  val shiftedTargetWidth = DAS_DATAPATH_WIDTH
  val filteredSignificandWidth = shiftedTargetWidth + (DAS_DATAPATH_WIDTH - 1)
  val filteredTargetWidth = DAS_DATAPATH_WIDTH
  val strainSignificandWidth = filteredTargetWidth + (DAS_DATAPATH_WIDTH - 1) + 1 - 2
  val strainTargetWidth = DAS_DATAPATH_WIDTH
  val strainRateSignificandWidth = strainTargetWidth + (DAS_DATAPATH_WIDTH - 1) + 1
  val strainRateTargetWidth = strainRateSignificandWidth
  val mergedSignificandWidth = strainRateTargetWidth + log2Up(6)
  println(
    s"shift right values = " +
      s"${shiftedSignificandWidth - shiftedTargetWidth}, " +
      s"${filteredSignificandWidth - filteredTargetWidth}, " +
      s"${strainSignificandWidth - strainTargetWidth}, " +
      s"${strainRateSignificandWidth - strainRateTargetWidth}"
  )

  println("system parameters:")
  println(s"target device = ${TARGET_DEVICE.part}")
  println(s"\tinterrogation rate min = ${1.0 / (PULSE_PERIOD_POINTS_MAX * 4).toDouble * 1e9} Hz")
  println(s"\tstrain/gauge length resolution = ${OUTPUT_STRAIN_RESOLUTION * 1e12}pε/m")
  println(s"\tgauge length max = ${GAUGE_POINTS_MAX * 2 * 0.2}m")
  println(s"\tfiber length max = ${PULSE_VALID_POINTS_MAX * 2 * 0.2}m")
  println(s"\tpulse-pulse delay max = ${PULSE_PULSE_DELAY_POINTS_MAX * 2 * 2}ns")
  println()

  //////////
  // Vivado project paths
  //////////
  val daqScalaSource = new File("./chainsaw/src/main/scala/chainsaw/projects/xdma/daq")
  val axku062DaqRtlDir = new File("./Axku062Daq")
  val axku5DaqRtlDir = new File("./Axku5Daq")
  val resourceDir = new File("./chainsaw/src/main/resources")

  //////////
  // Tasks
  //////////
  object Config { // default RTL generation &

    val vivadoPath = "/tools/Xilinx/Vivado/2024.1/bin"

    def gen: SpinalConfig = SpinalConfig(
      targetDirectory = "hw/gen",
      defaultClockDomainFrequency = DATA_FREQUENCY,
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW),
      onlyStdLogicVectorAtTopLevelIo = true
    )

    def sim: SpinalSimConfig = { // simulation using XSim
      SimConfig.withXSim.withWave // using XSim
        .withConfig(gen)
        .withXilinxDevice(TARGET_DEVICE.part)
        .withXSimSourcesPaths(
          xciSourcesPaths = ArrayBuffer(),
          bdSourcesPaths = ArrayBuffer()
        )
    }

    def synth(top: => Module): VivadoReport = {
      VivadoFlow2(
        vivadoPath = vivadoPath,
        workspacePath = "./synthWorkspace",
        rtl = Rtl(gen.generateVerilog(top)),
        device = TARGET_DEVICE,
        taskType = SYNTH
      ).get
    }

    def impl(top: => Module): VivadoReport = {
      VivadoFlow2(
        vivadoPath = vivadoPath,
        workspacePath = "./synthWorkspace",
        rtl = Rtl(gen.generateVerilog(top)),
        device = TARGET_DEVICE,
        taskType = IMPL
      ).get
    }
  }

  //////////
  // Stream/Flow/Fragment Utils
  //////////
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

  type SIntStream = Stream[Fragment[SInt]]
  type SIntVecStream = Stream[Fragment[Vec[SInt]]]

  implicit class SintStreamUtils(stream: SIntStream) {
    def resize(targetWidth: Int, significandWidth: Int = -1): SIntStream = {
      stream.translateFragmentWith(stream.fragment(significandWidth - 1 downto significandWidth - targetWidth))
    }
  }

  //////////
  // Read/Write numpy data
  //////////

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

  object CsvWriter {
    def apply[T: ClassTag](matrix: Array[Array[T]], path: String): Unit = {
      require(matrix.nonEmpty, "Matrix cannot be empty")
      require(matrix.forall(_.length == matrix.head.length), "All rows must have the same number of columns")

      // 写入文件
      val writer = new PrintWriter(path)
      val content = matrix.map(_.mkString(" ")).mkString("\n")
      writer.write(content)
      writer.close()
    }

    def apply[T: ClassTag](matrix: Array[T], path: String): Unit = {

      // 写入文件
      val writer = new PrintWriter(path)
      val content = matrix.mkString(" ")
      writer.write(content)
      writer.close()
    }
  }

  // TODO: using nd4j to create numpy matrix file directly
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

  case class TestConfig(
      gaugePoints: Int,
      pulseCount: Int,
      pulseValidPoints: Int,
      demodulationEnabled: Int = 0,
      pulsePulseDelayPoints: Int = 100
  )

}
