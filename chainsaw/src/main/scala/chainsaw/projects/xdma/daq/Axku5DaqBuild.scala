package chainsaw.projects.xdma.daq

import spinal.core._
import utils.BlackBoxParser

import java.io.File

//////////
// you can rebuild Axku5Daq Vivado project by following steps:
//////////

// 1: generate submodules & top-level module
object GenerateAxku5SubModules extends App {
  val config =
    SpinalConfig(
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW, resetKind = SYNC),
      targetDirectory = axku5DaqRtlDir.getAbsolutePath
    )
  config.generateVerilog(AdiSpiCtrl(50)) // sclk frequency = 125MHz / 50 = 1MHz < 2.5MHz < 10MHz
  config.generateVerilog(ChainsawDaqDataPath())
}

object GenerateAxku5TopModule extends App {
  SpinalConfig(targetDirectory = axku5DaqRtlDir.getAbsolutePath)
    .generateVerilog(Axku5Daq())
}

// 2. run .tcl script to build project including a block design
// run following command in Vivado TCL console, working directory should be [[axku5DaqRtlDir]]
// cd axku5DaqRtlDir
// source <project_name.tcl>

// 3. synth,impl & bitgen

//////////
// you can upgrade Axku062Daq by following steps:
//////////

// 1. modify existing submodules / add new submodules in Scala and generate them using [[GenerateAxku5SubModules]](you may need to add extra modules)
// 2. update / modify block design in Vivado, regenerate its wrapper HDL file
// 3. create block design wrapper in Scala by following task:
object BuildAxku5BlackBox extends App {
  val bdPath = "Axku5/Axku5.gen/sources_1/bd/Axku5Peripheral/hdl/Axku5Peripheral_wrapper.v"
  BlackBoxParser(
    from = new File(axku5DaqRtlDir, bdPath),
    to = new File(daqScalaSource, "Axku5Wrapper.scala")
  )
}
// 4. connect block design wrapper with top-level I/O in DasTop.scala
// 5. regenerate top-level module using [[GenerateAxku5TopModule]]
// 6. synth,impl & bitgen
// 7. to save your upgrade in Vivado, run following command in Vivado TCL console to update <project_name.tcl>
// !! before overwrite the script, ensure that utils files are deleted, or "local or imported" files won't be <none>
// write_project_tcl -force ../<project_name.tcl>