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
    def spinal = SpinalConfig(
      targetDirectory = "hw/gen",
      defaultConfigForClockDomains = ClockDomainConfig(
        resetActiveLevel = HIGH
      ),
      onlyStdLogicVectorAtTopLevelIo = true
    )

    def sim = {

      val hint =
        """
          |cd
          |cd  ~/SpinalHDL/simWorkspace/$ModuleName/xsim
          |echo -e "open_wave_database $ModuleName.wdb\nopen_wave_config /home/ltr/SpinalHDL/$ModuleName.wcfg" > view_wave.tcl
          |vivado -source view_wave.tcl
          |""".stripMargin

      println(hint)

      SimConfig.withXSim.withWave // 使用XSim进行仿真
        .withXilinxDevice("XCKU060-FFVA1156-2-i".toLowerCase())
        .withXSimSourcesPaths(
          xciSourcesPaths = ArrayBuffer(),
          bdSourcesPaths = ArrayBuffer()
        )

    }
  }

  def getFrameHead[T <: Data](stream: Stream[Fragment[T]]) = {
    val frameDone = RegInit(True)
    when(stream.last && stream.valid)(frameDone.set()).elsewhen(stream.valid)(frameDone.clear())
    frameDone && stream.valid
  }

  def getAxiFrameHead(stream: Stream[Axi4StreamBundle]) = {
    val frameDone = RegInit(True)
    when(stream.last && stream.valid)(frameDone.set()).elsewhen(stream.valid)(frameDone.clear())
    frameDone && stream.valid
  }

  def fragment[T <: Data](data: T, last: Bool): Fragment[T] = {
    val fragment = Fragment(HardType(data))
    fragment.fragment := data
    fragment.last := last
    fragment
  }

  implicit class StreamFragmentUtils[T <: Data](stream: Stream[Fragment[T]]) {
    def translateFragmentWith[T2 <: Data](data: T2) = stream.translateWith(fragment(data, stream.last))
  }

//  implicit class FloatingPointUtils(f: Floating) {
//    def isNormalized = f.exponent =/= f.exponent.getZero && f.exponent =/= f.exponent.getAllTrue
//
//    def isDenormalized = f.exponent === f.exponent.getZero
//
//    def isSpecial = f.exponent === f.exponent.getAllTrue
//
//    def significandValue = Mux(isNormalized, B(1, 1 bits) ## f.mantissa, B(0, 1 bits) ## f.mantissa).asUInt
//
//    def exponentValue = Mux(isNormalized, f.exponent, B(1, f.exponentSize bits)).asUInt
//
//    def isSinglePrecision: Boolean = f.exponentSize == 8 && f.mantissaSize == 23
//
//    def isDoublePrecision: Boolean = f.exponentSize == 11 && f.mantissaSize == 52
//  }

}
