###############################################################################
# User Time Names / User Time Groups / Time Specs
###############################################################################
create_clock -name sys_clk -period 10 [get_ports pcie_clk_p]

set_false_path -through [get_pins xdma_0_i/inst/pcie3_ip_i/inst/pcie3_uscale_top_inst/pcie3_uscale_wrapper_inst/PCIE_3_1_inst/CFGMAX*]
set_false_path -through [get_nets xdma_0_i/inst/cfg_max*]

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
#
set_false_path -to [get_pins -hier *sync_reg[0]/D]