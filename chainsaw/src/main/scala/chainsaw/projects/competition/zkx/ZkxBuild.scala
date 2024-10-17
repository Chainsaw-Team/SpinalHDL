package chainsaw.projects.competition.zkx

import utils.BlackBoxParser

import java.io.File
import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._
import spinal.lib.fsm._
import spinal.lib.bus._

object BuildBlackBox extends App {

  val scalaSourceDir = new File(
    "C:\\Users\\lsfan\\Documents\\GitHub\\SpinalHDL\\chainsaw\\src\\main\\scala\\chainsaw\\projects\\competition\\zkx"
  )

  BlackBoxParser(
    from =
      new File("C:/Users/lsfan/Desktop/Z7pExample/Z7pExample.srcs/sources_1/bd/Peripheral/hdl/Peripheral_wrapper.v"),
    to = new File(scalaSourceDir, "Peripheral_wrapper.scala")
  )
}

object GenerateTopModule extends App {

  val rtlSourceDir = new File(
    "C:\\Users\\lsfan\\Documents\\GitHub\\SpinalHDL\\chainsaw\\src\\main\\resources\\ZkxSources"
  )

  SpinalConfig(targetDirectory = rtlSourceDir.getAbsolutePath)
    .generateVerilog(ZkxTop())
}
