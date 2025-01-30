package chainsaw.projects.xdma

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.amba4.axis.Axi4Stream
import spinal.lib.bus.amba4.axis.Axi4Stream.{Axi4Stream, Axi4StreamBundle}
import spinal.lib.UIntPimper

import java.io.File
import scala.collection.mutable.ArrayBuffer

package object daq {

  val daqScalaSource = new File("./chainsaw/src/main/scala/chainsaw/projects/xdma/daq")
  val axku062DaqRtlDir = new File("./Axku062Daq")
  val axku5DaqRtlDir = new File("./Axku5Daq")

  object Config {
    def spinal: SpinalConfig = SpinalConfig(
      targetDirectory = "hw/gen",
      defaultConfigForClockDomains = ClockDomainConfig(
        resetActiveLevel = HIGH
      ),
      onlyStdLogicVectorAtTopLevelIo = true
    )

    def sim: SpinalSimConfig = {

      val hint =
        """
          |cd
          |cd  ~/SpinalHDL/simWorkspace/$ModuleName/xsim
          |echo -e "open_wave_database $ModuleName.wdb\nopen_wave_config /home/ltr/SpinalHDL/$ModuleName.wcfg" > view_wave.tcl
          |vivado -source view_wave.tcl
          |""".stripMargin

      println(hint)

      SimConfig.withXSim.withWave // using XSim
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
    def translateFragmentWith[T2 <: Data](data: T2): Stream[Fragment[T2]] = stream.translateWith(fragment(data, stream.last))
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

}
