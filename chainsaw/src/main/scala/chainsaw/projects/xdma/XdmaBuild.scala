package chainsaw.projects.xdma

import chainsaw.projects.xdma.ChainsawXdma
import spinal.core._
import utils.BlackBoxParser

import java.io.File

// task 1: create block design wrapper
object BuildXdmaBlackBox extends App {
  BlackBoxParser(
    from =
      new File("C:/Users/lsfan/Desktop/ChainsawXdma/ChainsawXdma.srcs/sources_1/bd/Xdma/hdl/Xdma_wrapper.v"),
    to = new File(new File("C:/Users/lsfan/Documents/GitHub/SpinalHDL/chainsaw/src/main/scala/chainsaw/projects/xdma"), "Xdma_wrapper.scala")
  )
}

// task 2: connect block design wrapper with top-level I/O in ChainsawXdma.scala

// task 3: generate top-level module
object GenerateXdmaTopModule extends App {
  SpinalConfig(targetDirectory = new File("C:/Users/lsfan/Documents/GitHub/SpinalHDL/chainsaw/src/main/resources/ChainsawXdmaSources").getAbsolutePath)
    .generateVerilog(ChainsawXdma())
}

// task 4: synth,impl & bitgen
