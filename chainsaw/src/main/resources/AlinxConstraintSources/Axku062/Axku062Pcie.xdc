###############################################################################
# User Time Names / User Time Groups / Time Specs
###############################################################################
create_clock -period 10.000 -name sys_clk [get_ports pcie_clk_p]
# 下面两行约束应当作用于XDMA IP的sys_clk输入
# set_clock_groups -name async20 -asynchronous -group [get_clocks sys_clk] -group [get_clocks -of_objects [get_pins -hierarchical -filter {NAME =~ *gen_channel_container[*].*gen_gthe3_channel_inst[*].GTHE3_CHANNEL_PRIM_INST/TXOUTCLK}]]
# set_clock_groups -name async21 -asynchronous -group [get_clocks -of_objects [get_pins -hierarchical -filter {NAME =~ *gen_channel_container[*].*gen_gthe3_channel_inst[*].GTHE3_CHANNEL_PRIM_INST/TXOUTCLK}]] -group [get_clocks sys_clk]
set_false_path -from [get_ports pcie_perst]
###############################################################################
# Pinout and Related I/O Constraints
###############################################################################
##### SYS RESET###########
set_property LOC PCIE_3_1_X0Y0 [get_cells peripheral/Peripheral_i/PCIe/inst/pcie3_ip_i/inst/Peripheral_PCIe_0_pcie3_ip_pcie3_uscale_top_inst/pcie3_uscale_wrapper_inst/PCIE_3_1_inst]
set_property PACKAGE_PIN K22 [get_ports pcie_perst]
set_property PULLTYPE PULLUP [get_ports pcie_perst]
set_property IOSTANDARD LVCMOS33 [get_ports pcie_perst]
set_property CONFIG_VOLTAGE 1.8 [current_design]
set_property CFGBVS GND [current_design]

##### REFCLK_IBUF###########
set_property PACKAGE_PIN AB6 [get_ports pcie_clk_p]
set_false_path -to [get_pins -hier {*sync_reg[0]/D}]

