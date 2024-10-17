###############################################################################
# User Time Names / User Time Groups / Time Specs
###############################################################################
create_clock -name sys_clk -period 10 [get_ports pcie_clk_p]
# 下面两行约束应当作用于XDMA IP的sys_clk输入
# set_clock_groups -name async20 -asynchronous -group [get_clocks sys_clk] -group [get_clocks -of_objects [get_pins -hierarchical -filter {NAME =~ *gen_channel_container[*].*gen_gthe3_channel_inst[*].GTHE3_CHANNEL_PRIM_INST/TXOUTCLK}]]
# set_clock_groups -name async21 -asynchronous -group [get_clocks -of_objects [get_pins -hierarchical -filter {NAME =~ *gen_channel_container[*].*gen_gthe3_channel_inst[*].GTHE3_CHANNEL_PRIM_INST/TXOUTCLK}]] -group [get_clocks sys_clk]
set_false_path -from [get_ports pcie_perst]
###############################################################################
# Pinout and Related I/O Constraints
###############################################################################
##### SYS RESET###########
set_property LOC [get_package_pins -filter {PIN_FUNC == IO_T3U_N12_PERSTN0_65}] [get_ports pcie_perst]
set_property PULLUP true [get_ports pcie_perst]
set_property IOSTANDARD LVCMOS33 [get_ports pcie_perst]
set_property CONFIG_VOLTAGE 1.8 [current_design]
set_property CFGBVS GND [current_design]

##### REFCLK_IBUF###########
set_property LOC AB6 [get_ports pcie_clk_p]
set_false_path -to [get_pins -hier *sync_reg[0]/D]
