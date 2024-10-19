package chainsaw.projects.xdma.daq

import spinal.core._
import utils.BlackBoxParser

import java.io.File

//////////
// you can rebuild Axku062Daq Vivado project by following steps:
//////////

// 1: generate submodules & top-level module
object GenerateSubModules extends App {
  val config =
    SpinalConfig(
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW, resetKind = SYNC),
      targetDirectory = axku062DaqRtlDir.getAbsolutePath
    )
  config.generateVerilog(AdiSpiCtrl(50)) // sclk frequency = 125MHz / 50 = 1MHz < 2.5MHz < 10MHz
  config.generateVerilog(ChainsawDaqDataPath())
}

object GenerateTopModule extends App {
  SpinalConfig(targetDirectory = axku062DaqRtlDir.getAbsolutePath)
    .generateVerilog(Axku062Daq())
}

// 2. run .tcl script to build project including a block design
// run following command in Vivado TCL console, working directory should be [[axku062DaqRtlDir]]
// cd axku062DaqRtlDir
// source <project_name.tcl>

// 3. synth,impl & bitgen

//////////
// you can upgrade Axku062Daq by following steps:
//////////

// 1. modify existing submodules / add new submodules in Scala and generate them using [[GenerateSubModules]](you may need to add extra modules)
// 2. update / modify block design in Vivado, regenerate its wrapper HDL file
// 3. create block design wrapper in Scala by following task:
object BuildBlackBox extends App {
  val bdPath = "Axku062Daq/Axku062Daq.gen/sources_1/bd/Peripheral/hdl/Peripheral_wrapper.v"
  BlackBoxParser(
    from = new File(axku062DaqRtlDir, bdPath),
    to = new File(daqScalaSource, "Axku062Wrapper.scala")
  )
}
// 4. connect block design wrapper with top-level I/O in DasTop.scala
// 5. regenerate top-level module using [[GenerateTopModule]]
// 6. synth,impl & bitgen
// 7. to save your upgrade in Vivado, run following command in Vivado TCL console to update <project_name.tcl>
// !! before overwrite the script, ensure that utils files are deleted, or "local or imported" files won't be <none>
// write_project_tcl -force ../<project_name.tcl>