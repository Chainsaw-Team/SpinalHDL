package chainsaw.examples

import chainsaw.projects.xdma.daq.Config
import spinal.core._
import spinal.lib.eda.bench.Rtl
import spinal.lib.eda.xilinx.{SYNTH, UltraScale, VivadoFlow2, XilinxDevice}

import scala.language.postfixOps

case class Shifter() extends Module {

  val data = in Bits (8 bits)
  val shift = in UInt (2 bits)
  val shifted = out Bits (8 bits)
  shifted := data |<< shift

}

object Shifter extends App {

  val report = VivadoFlow2(
    vivadoPath = "/tools/Xilinx/Vivado/2024.1/bin",
    workspacePath = "./synthWorkspace",
    rtl = Rtl(Config.gen.generateVerilog(new Shifter())),
    device = new XilinxDevice(family = UltraScale, part = "XCKU060-FFVA1156-2-i".toLowerCase(), fMax = 200 MHz),
    taskType = SYNTH
  ).get
  println(report)

}
