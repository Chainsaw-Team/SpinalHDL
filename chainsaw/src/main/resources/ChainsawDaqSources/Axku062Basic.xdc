# when a signal is declared as bool in the top module, you can't get it as "signal"[0], you must get it as "signal"

###############################################################################
# User Flash Programming Settings
###############################################################################
set_property CONFIG_MODE SPIx8 [current_design]
set_property BITSTREAM.CONFIG.SPI_BUSWIDTH 8 [current_design]
set_property BITSTREAM.CONFIG.CONFIGRATE 50 [current_design]


###############################################################################
# User Physical Constraints
###############################################################################

# system clock from on-board oscillator

set_property PACKAGE_PIN AK17 [get_ports sys_clk_p]
set_property PACKAGE_PIN AK16 [get_ports sys_clk_n]
set_property IOSTANDARD DIFF_SSTL12 [get_ports sys_clk_n]
set_property IOSTANDARD DIFF_SSTL12 [get_ports sys_clk_p]

# system reset from on-board key, active-low
set_property PACKAGE_PIN N27 [get_ports rst_n]
set_property IOSTANDARD LVCMOS33 [get_ports rst_n]

# UART

# SMA


# LED
set_property PACKAGE_PIN E12 [get_ports {led[0]}]
set_property PACKAGE_PIN F12 [get_ports {led[1]}]
set_property PACKAGE_PIN L9 [get_ports {led[2]}]
set_property PACKAGE_PIN H23 [get_ports {led[3]}]
set_property PACKAGE_PIN E13 [get_ports {led_test[0]}]
set_property PACKAGE_PIN F13 [get_ports {led_test[1]}]

set_property IOSTANDARD LVCMOS18 [get_ports led[0]]
set_property IOSTANDARD LVCMOS18 [get_ports led[1]]
set_property IOSTANDARD LVCMOS18 [get_ports led[2]]
# the only LED in BANK65
set_property IOSTANDARD LVCMOS33 [get_ports led[3]] 
set_property IOSTANDARD LVCMOS18 [get_ports led_test[0]]
set_property IOSTANDARD LVCMOS18 [get_ports led_test[1]]

# user key, active-low
set_property PACKAGE_PIN N23 [get_ports user_key_n]
set_property IOSTANDARD LVCMOS33 [get_ports user_key_n]

create_clock -period 5.000 [get_ports sys_clk_p]

####################################################################################
# Constraints from file : 'Axku062Pcie.xdc'
####################################################################################

