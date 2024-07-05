package chainsaw.projects.xdma.daq

import spinal.core._
import utils.BlackBoxParser

import java.io.File


// task 1: generate submodules
object GenerateSubModules extends App {

  val config =
    SpinalConfig(
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW, resetKind = SYNC),
      targetDirectory = rtlSourceDir.getAbsolutePath
    )
  config.generateVerilog(AdiSpiCtrl(50)) // sclk frequency = 125MHz / 50 = 1MHz < 2.5MHz < 10MHz
  config.generateVerilog(ChainsawDaqDataPath())

}

// task 2: in Vivado Block Design: 1. add RTL module 2. connect interfaces 3. edit address

// task 3: create block design wrapper
object BuildBlackBox extends App {
  BlackBoxParser(
    from = new File("C:/Users/lsfan/Desktop/ChainsawDaq/ChainsawDaq.srcs/sources_1/bd/Peripheral/hdl/Peripheral_wrapper.v"),
    to = new File(scalaSourceDir, "Peripheral_wrapper.scala")
  )
}

// task 4: connect block design wrapper with top-level I/O in DasTop.scala

// task 5: generate top-level module
object GenerateTopModule extends App {
  SpinalConfig(targetDirectory = rtlSourceDir.getAbsolutePath)
    .generateVerilog(ChainsawDaq())
}

// task 6: synth,impl & bitgen
