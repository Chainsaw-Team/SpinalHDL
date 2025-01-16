package chainsaw.projects.xdma

import spinal.core._
import spinal.core.sim._

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

    def sim = SimConfig.withXSim.withWave // 使用XSim进行仿真
      .withXilinxDevice("XCKU060-FFVA1156-2-i".toLowerCase())
      .withXSimSourcesPaths(
        xciSourcesPaths = ArrayBuffer(),
        bdSourcesPaths = ArrayBuffer()
      )
  }

}
