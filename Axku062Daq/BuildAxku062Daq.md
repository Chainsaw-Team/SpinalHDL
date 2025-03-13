### recreate and build Axku062Daq Vivado project from scratch

1. generate submodules & top-level module

   ```scala
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
   ```
2. run .tcl script to build project including a block design
   ```shell
   vivado -source <project_name.tcl>
   ```
3. every time you create/update block design inside the project, rebind clock domains introduced by user logic, using following commands in Vivado TCL Console, then validate design
   ```shell
   set_property CONFIG.CLK_DOMAIN Peripheral_PCIe_0_axi_aclk [get_bd_intf_pins /Datapath/controlIn]
   set_property CONFIG.FREQ_HZ 125000000 [get_bd_intf_pins /Datapath/controlIn]
   set_property CONFIG.CLK_DOMAIN Peripheral_jesd204_buffer_0_IBUF_DS_ODIV2 [get_bd_intf_pins /Datapath/dataIn]
   set_property CONFIG.FREQ_HZ 250000000 [get_bd_intf_pins /Datapath/dataIn]
   set_property CONFIG.CLK_DOMAIN Peripheral_jesd204_buffer_0_IBUF_DS_ODIV2 [get_bd_intf_pins /Datapath/dataOut]
   set_property CONFIG.FREQ_HZ 250000000 [get_bd_intf_pins /Datapath/dataOut]
   ```
4. synth,impl & bitgen

### update Axku062Daq Vivado project

1. update submodule(s)
2. update top module
3. update block design