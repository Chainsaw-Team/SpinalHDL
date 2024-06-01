###############################################################################
# User Time Names / User Time Groups / Time Specs
###############################################################################
create_clock -name sys_clk -period 10 [get_ports pcie_clk_p]
set_clock_groups -name async20 -asynchronous -group [get_clocks sys_clk] -group [get_clocks -of_objects [get_pins -hierarchical -filter {NAME =~ *gen_channel_container[*].*gen_gthe3_channel_inst[*].GTHE3_CHANNEL_PRIM_INST/TXOUTCLK}]]
set_clock_groups -name async21 -asynchronous -group [get_clocks -of_objects [get_pins -hierarchical -filter {NAME =~ *gen_channel_container[*].*gen_gthe3_channel_inst[*].GTHE3_CHANNEL_PRIM_INST/TXOUTCLK}]] -group [get_clocks sys_clk]
#set_false_path -through [get_pins XDMA_EXAMPLE_i/inst/pcie3_ip_i/inst/XDMA_EXAMPLE_pcie3_ip_pcie3_uscale_top_inst/pcie3_uscale_wrapper_inst/PCIE_3_1_inst/CFGMAX*]
#set_false_path -through [get_nets XDMA_EXAMPLE_i/inst/cfg_max*]

set_false_path -from [get_ports pcie_perst]


###############################################################################
# User Physical Constraints
###############################################################################

# system clock from on-board oscillator
create_clock -period 5.000 [get_ports sys_clk_p]

make_diff_pair_ports sys_clk_p sys_clk_n
set_property PACKAGE_PIN AK16 [get_ports sys_clk_n]
set_property PACKAGE_PIN AK17 [get_ports sys_clk_p]
set_property IOSTANDARD DIFF_SSTL12 [get_ports sys_clk_n]
set_property IOSTANDARD DIFF_SSTL12 [get_ports sys_clk_p]

# system reset from on-board key, active-low
set_property PACKAGE_PIN N27 [get_ports {rst_n}]
set_property IOSTANDARD LVCMOS18 [get_ports {rst_n}]

# PCIe
# set_property PACKAGE_PIN AB1 [get_ports {pcie_rx_n[0]}]
# set_property PACKAGE_PIN AB2 [get_ports {pcie_rx_p[0]}]
# set_property PACKAGE_PIN AD1 [get_ports {pcie_rx_n[1]}]
# set_property PACKAGE_PIN AD2 [get_ports {pcie_rx_p[1]}]
# set_property PACKAGE_PIN AF1 [get_ports {pcie_rx_n[2]}]
# set_property PACKAGE_PIN AF2 [get_ports {pcie_rx_p[2]}]
# set_property PACKAGE_PIN AH1 [get_ports {pcie_rx_n[3]}]
# set_property PACKAGE_PIN AH2 [get_ports {pcie_rx_p[3]}]
# set_property PACKAGE_PIN AJ3 [get_ports {pcie_rx_n[4]}]
# set_property PACKAGE_PIN AJ4 [get_ports {pcie_rx_p[4]}]
# set_property PACKAGE_PIN AK1 [get_ports {pcie_rx_n[5]}]
# set_property PACKAGE_PIN AK2 [get_ports {pcie_rx_p[5]}]
# set_property PACKAGE_PIN AM1 [get_ports {pcie_rx_n[6]}]
# set_property PACKAGE_PIN AM2 [get_ports {pcie_rx_p[6]}]
# set_property PACKAGE_PIN AP1 [get_ports {pcie_rx_n[7]}]
# set_property PACKAGE_PIN AP2 [get_ports {pcie_rx_p[7]}]
# set_property PACKAGE_PIN AC3 [get_ports {pcie_tx_n[0]}]
# set_property PACKAGE_PIN AC4 [get_ports {pcie_tx_p[0]}]
# set_property PACKAGE_PIN AE3 [get_ports {pcie_tx_n[1]}]
# set_property PACKAGE_PIN AE4 [get_ports {pcie_tx_p[1]}]
# set_property PACKAGE_PIN AG3 [get_ports {pcie_tx_n[2]}]
# set_property PACKAGE_PIN AG4 [get_ports {pcie_tx_p[2]}]
# set_property PACKAGE_PIN AH5 [get_ports {pcie_tx_n[3]}]
# set_property PACKAGE_PIN AH6 [get_ports {pcie_tx_p[3]}]
# set_property PACKAGE_PIN AK5 [get_ports {pcie_tx_n[4]}]
# set_property PACKAGE_PIN AK6 [get_ports {pcie_tx_p[4]}]
# set_property PACKAGE_PIN AL3 [get_ports {pcie_tx_n[5]}]
# set_property PACKAGE_PIN AL4 [get_ports {pcie_tx_p[5]}]
# set_property PACKAGE_PIN AM5 [get_ports {pcie_tx_n[6]}]
# set_property PACKAGE_PIN AM6 [get_ports {pcie_tx_p[6]}]
# set_property PACKAGE_PIN AN3 [get_ports {pcie_tx_n[7]}]
# set_property PACKAGE_PIN AN4 [get_ports {pcie_tx_p[7]}]
set_property PACKAGE_PIN AB5 [get_ports {pcie_clk_n}]
set_property PACKAGE_PIN AB6 [get_ports {pcie_clk_p}]
set_property PACKAGE_PIN K22 [get_ports {pcie_perst}]

# UART
set_property PACKAGE_PIN AJ11 [get_ports {uart_rxd}]
set_property PACKAGE_PIN AM9 [get_ports {uart_txd}]

# SMA
set_property PACKAGE_PIN G12 [get_ports {sma_clk_n}]
set_property PACKAGE_PIN H12 [get_ports {sma_clk_p}]

set_property IOSTANDARD LVCMOS18 [get_ports {sma*}]

# LED
set_property PACKAGE_PIN E12 [get_ports {led[0]}]
set_property PACKAGE_PIN F12 [get_ports {led[1]}]
set_property PACKAGE_PIN L9 [get_ports {led[2]}]
set_property PACKAGE_PIN H23 [get_ports {led[3]}]
set_property PACKAGE_PIN E13 [get_ports {led_test[0]}]
set_property PACKAGE_PIN F13 [get_ports {led_test[1]}]

set_property IOSTANDARD LVCMOS18 [get_ports {led*}]

# user key, active-low
set_property PACKAGE_PIN N23 [get_ports {user_key_n}]
set_property IOSTANDARD LVCMOS18 [get_ports {user_key_n}]

###############################################################################
# PCIe Pinout and Related I/O Constraints
###############################################################################
##### SYS RESET###########
set_property LOC [get_package_pins -filter {PIN_FUNC == IO_T3U_N12_PERSTN0_65}] [get_ports pcie_perst]
set_property PULLUP true [get_ports pcie_perst]
set_property IOSTANDARD LVCMOS18 [get_ports pcie_perst]
set_property CONFIG_VOLTAGE 1.8 [current_design]
set_property CFGBVS GND [current_design]

##### REFCLK_IBUF###########
set_property LOC AB6 [get_ports pcie_clk_p]

###############################################################################
# Flash Programming Settings: Uncomment as required by your design
###############################################################################
set_property CONFIG_MODE SPIx8 [current_design]
set_property BITSTREAM.CONFIG.SPI_BUSWIDTH 8 [current_design]
set_property BITSTREAM.CONFIG.CONFIGRATE 50 [current_design]
set_property BITSTREAM.CONFIG.GENERAL.COMPRESS TRUE [current_design]

#---------------------- Adding waiver for exdes level constraints --------------------------------#

create_waiver -type DRC -id {REQP-1839} -tags "1166691" -scope -internal -user "xdma" -desc "DRC expects synchronous pins to be provided to BRAM inputs. Since synchronization is present one stage before, it is safe to ignore" -objects [list [get_cells -hierarchical -filter {NAME =~ {*/blk_mem_xdma_inst/U0/inst_blk_mem_gen/*.ram}}] [get_cells -hierarchical -filter {NAME =~ {*/AXI_BRAM_CTL/U0/gint_inst*.mem_reg*} && PRIMITIVE_TYPE =~ {*BRAM*}}] [get_cells -hierarchical -filter {NAME =~ {*xdma_inst/U0/gint_inst*.mem_reg*} && PRIMITIVE_TYPE =~ {*BRAM*}}] [get_cells -hierarchical -filter {NAME =~ {*axi_bram_gen_bypass_inst/U0/gint_inst*.mem_reg*} && PRIMITIVE_TYPE =~ {*BRAM*}}] [get_cells -hierarchical -filter {NAME =~ {*/blk_mem_axiLM_inst/U0/inst_blk_mem_gen/*.ram}}] [get_cells -hierarchical -filter {NAME =~ {*/blk_mem_gen_bypass_inst/U0/inst_blk_mem_gen/*.ram}}]]

create_waiver -type CDC -id {CDC-1} -tags "1165825" -scope -internal -user "xdma" -desc "PCIe reset path -Safe to waive" -from [get_ports pcie_perst] -to [get_pins -hier -filter {NAME =~ {*/user_clk_heartbeat_reg[*]/R}}]




