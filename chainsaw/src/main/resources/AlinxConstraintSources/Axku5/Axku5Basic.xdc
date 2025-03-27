###############################################################################
# User Flash Programming Settings
###############################################################################
set_property CONFIG_MODE SPIx8 [current_design]
set_property BITSTREAM.CONFIG.SPI_BUSWIDTH 8 [current_design]
set_property CONFIG_VOLTAGE 1.8 [current_design]
set_property BITSTREAM.GENERAL.COMPRESS TRUE [current_design]
set_property BITSTREAM.CONFIG.CONFIGRATE 85.0 [current_design]
set_property BITSTREAM.CONFIG.SPI_32BIT_ADDR YES [current_design]
set_property BITSTREAM.CONFIG.SPI_FALL_EDGE YES [current_design]

# system clock from on-board oscillator
set_property PACKAGE_PIN AC13 [get_ports sys_clk_p]
set_property PACKAGE_PIN AC14 [get_ports sys_clk_n]

# user LED
set_property PACKAGE_PIN J12 [get_ports {led[0]}]
set_property PACKAGE_PIN H14 [get_ports {led[1]}]
set_property PACKAGE_PIN F13 [get_ports {led[2]}]
set_property PACKAGE_PIN H12 [get_ports {led[3]}]
set_property IOSTANDARD LVCMOS33 [get_ports led*]

# user key

# user 40pin
set_property PACKAGE_PIN A10 [get_ports user_40pin_IO_N_0]
set_property PACKAGE_PIN B10 [get_ports user_40pin_IO_P_0]
set_property PACKAGE_PIN B11 [get_ports user_40pin_IO_N_1]
set_property PACKAGE_PIN C11 [get_ports user_40pin_IO_P_1]
set_property PACKAGE_PIN E10 [get_ports user_40pin_IO_N_2]
set_property PACKAGE_PIN E11 [get_ports user_40pin_IO_P_2]
set_property PACKAGE_PIN A9 [get_ports user_40pin_IO_N_3]
set_property PACKAGE_PIN B9 [get_ports user_40pin_IO_P_3]
set_property PACKAGE_PIN D10 [get_ports user_40pin_IO_N_4]
set_property PACKAGE_PIN D11 [get_ports user_40pin_IO_P_4]
set_property PACKAGE_PIN A12 [get_ports user_40pin_IO_N_15]
set_property PACKAGE_PIN A13 [get_ports user_40pin_IO_P_15]
set_property PACKAGE_PIN D13 [get_ports user_40pin_IO_N_16]
set_property PACKAGE_PIN D14 [get_ports user_40pin_IO_P_16]
set_property IOSTANDARD LVCMOS33 [get_ports user_40pin_IO_*]


set_property IOSTANDARD DIFF_SSTL12 [get_ports sys_clk_p]
set_property IOSTANDARD DIFF_SSTL12 [get_ports sys_clk_n]
set_property IOSTANDARD LVCMOS33 [get_ports led*]
set_property IOSTANDARD LVCMOS33 [get_ports user_40pin_IO_*]
create_clock -period 5.000 [get_ports sys_clk_p]
create_clock -period 5.000 [get_ports ddr4_clk_p]

####################################################################################
# Constraints from file : 'Axku5Ddr4.xdc'
####################################################################################

