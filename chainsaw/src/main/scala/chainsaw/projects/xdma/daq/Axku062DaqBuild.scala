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
// run following command in [[axku062DaqRtlDir]]
// vivado -source <project_name.tcl>

// 3. every time you create/update block design inside the project, rebind clock domains introduced by user logic, using following commands in Vivado TCL Console, then validate design
// set_property CONFIG.CLK_DOMAIN Peripheral_PCIe_0_axi_aclk [get_bd_intf_pins /Datapath/controlIn]
// set_property CONFIG.FREQ_HZ 125000000 [get_bd_intf_pins /Datapath/controlIn]
// set_property CONFIG.CLK_DOMAIN Peripheral_jesd204_buffer_0_IBUF_DS_ODIV2 [get_bd_intf_pins /Datapath/dataIn]
// set_property CONFIG.FREQ_HZ 250000000 [get_bd_intf_pins /Datapath/dataIn]
// set_property CONFIG.CLK_DOMAIN Peripheral_jesd204_buffer_0_IBUF_DS_ODIV2 [get_bd_intf_pins /Datapath/dataOut]
// set_property CONFIG.FREQ_HZ 250000000 [get_bd_intf_pins /Datapath/dataOut]

// 4. synth,impl & bitgen

//////////
// you can upgrade Axku062Daq by following steps:
//////////

// 1. modify existing submodules / add new submodules in Scala and generate them using [[GenerateSubModules]](you may need to add extra modules)
// 2. update / modify block design in Vivado, regenerate its wrapper HDL file
// 3. create block design wrapper in Scala by following using python:
// TODO:
// 4. connect block design wrapper with top-level I/O in Axku062Daq.scala
// 5. regenerate top-level module using [[GenerateTopModule]]
// 6. synth,impl & bitgen
// 7. to save your upgrade in Vivado, run following command in Vivado TCL console to update <project_name.tcl>
// !! before overwrite the script, ensure that utils files are deleted, or "local or imported" files won't be <none>
// write_project_tcl -force ../<project_name.tcl>

//////////
// before you commit a
//////////

// some useful tcl commands
// write_cfgmem  -format mcs -size 32 -interface SPIx8 -loadbit {up 0x00000000 "/home/ltr/IdeaProjects/SpinalHDL/Axku062Daq/Axku062Daq/Axku062Daq.runs/impl_1/Axku062Daq.bit" } -force -file "/home/ltr/IdeaProjects/SpinalHDL/Axku062Daq/Axku062Daq/Axku062Daq.runs/impl_1/Axku062Daq.mcs"