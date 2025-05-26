### recreate and build Axku5Daq Vivado project from scratch

1. generate submodules & top-level module

   TODO   

2. run .tcl script to build project including a block design
   ```shell
   vivado -source <project_name.tcl>
   ```
3. every time you create/update block design inside the project, rebind clock domains introduced by user logic, using following commands in Vivado TCL Console, then validate design
   ```shell
   set_property CONFIG.CLK_DOMAIN Axku5Peripheral_PCIe_0_axi_aclk [get_bd_intf_pins /ChainsawDaqDataPath_0/controlIn]
   set_property CONFIG.FREQ_HZ 125000000 [get_bd_intf_pins /ChainsawDaqDataPath_0/controlIn]
   set_property CONFIG.CLK_DOMAIN Axku5Peripheral_jesd204_buffer_0_IBUF_DS_ODIV2 [get_bd_intf_pins /ChainsawDaqDataPath_0/dataIn]
   set_property CONFIG.FREQ_HZ 250000000 [get_bd_intf_pins /ChainsawDaqDataPath_0/dataIn]
   set_property CONFIG.CLK_DOMAIN Axku5Peripheral_jesd204_buffer_0_IBUF_DS_ODIV2 [get_bd_intf_pins /ChainsawDaqDataPath_0/dataOut]
   set_property CONFIG.FREQ_HZ 250000000 [get_bd_intf_pins /ChainsawDaqDataPath_0/dataOut]

   ```
4. synth,impl & bitgen

### update Axku5Daq Vivado project

1. update submodule(s)
2. update top module
3. update block design
4. save your changes
   1. remove "Utility sources" from your project
   2. create IP scripts using `write_ip_tcl` and remove "IP sources" from your project
   3. save project using `write_project_tcl`
   4. add clock binding commands before `validate_bd_design` in the script
   5. add IP sourcing commands before "Adding sources referenced in BDs" in the script
      ```shell
      source ../chainsaw/src/main/resources/projectIps/Atan2.tcl
      source ../chainsaw/src/main/resources/projectIps/DdsCompiler.tcl
      source ../chainsaw/src/main/resources/projectIps/LowpassFir.tcl

      ```