package chainsaw.projects.xdma.daq

import spinal.core._
import utils.BlackBoxParser

import java.io.File

// task 1: generate submodules
object GenerateSubModules extends App {

  val config =
    SpinalConfig(
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = HIGH, resetKind = SYNC),
      targetDirectory = rtlSourceDir.getAbsolutePath
    )
  config.generateVerilog(Hmc7044Ctrl(100)) // sclk frequency = 250MHz / 100 = 2.5MHz < 10MHz
  config.generateVerilog(AdiSpiCtrl(100)) // sclk frequency = 250MHz / 100 = 2.5MHz < 10MHz
}

// task 2: in Vivado Block Design: 1. add RTL module 2. connect interfaces 3. edit address

// task 3: create block design wrapper
object BuildBlackBox extends App {

  BlackBoxParser(
    from = new File("C:/Users/ltr/Documents/AXKU062/DaqTop/DaqTop.srcs/sources_1/bd/XDMA/hdl/XDMA_wrapper.v"),
    to = new File(scalaSourceDir, "XDMA_wrapper.scala")
  )
//  BlackBoxParser(
//    from = new File("C:/Users/ltr/Documents/AXKU062/ChainsawDaq/ChainsawDaq.srcs/sources_1/bd/Peripheral/hdl/Peripheral_wrapper.v"),
//    to = new File(scalaSourceDir, "Peripheral_wrapper.scala"),
//    parsingInterface = false
//  )
//  BlackBoxParser(
//    from = new File("c:/Users/ltr/Desktop/ChainsawDaq/ChainsawDaq.gen/sources_1/bd/Peripheral/hdl/Peripheral_wrapper.v"),
//    to = new File(scalaSourceDir, "Peripheral_wrapper.scala")
//  )
}

// task 4: connect block design wrapper with top-level I/O in DasTop.scala

// task 5: generate top-level module
object GenerateTopModule extends App {
  SpinalConfig(targetDirectory = rtlSourceDir.getAbsolutePath)
    .generateVerilog(DaqTop())
//  SpinalConfig(targetDirectory = rtlSourceDir.getAbsolutePath)
//    .generateVerilog(ChainsawDaq())
}

// task 6: synth,impl & bitgen
