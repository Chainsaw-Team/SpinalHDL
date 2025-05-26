package chainsaw.examples

import chainsaw.projects.xdma.daq.Config.gen
import chainsaw.projects.xdma.daq.TARGET_DEVICE
import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._
import spinal.lib.fsm._
import spinal.lib.bus._

import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps

case class ModuleWithIp() extends Module {

  val a, b = in UInt  (32 bits)
  val c = out UInt (33 bits)

//  val adder =


}

object XSimWithIpExample {

  SimConfig.withXSim.withWave // using XSim
    .withConfig(gen)
    .withXilinxDevice(TARGET_DEVICE.part)
    .withXSimSourcesPaths(
      xciSourcesPaths = ArrayBuffer(),
      bdSourcesPaths = ArrayBuffer()
    )


}
