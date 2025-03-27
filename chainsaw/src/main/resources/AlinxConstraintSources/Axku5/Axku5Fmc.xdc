###############################################################################
# FMC-HPC, 8 gigabit transceiver enabled, LA & HA user data enabled(HB unused)
###############################################################################

# gigabit clocks
set_property PACKAGE_PIN M6 [get_ports fmc_hpc_GBTCLK_M2C_N_0]
set_property PACKAGE_PIN M7 [get_ports fmc_hpc_GBTCLK_M2C_P_0]
set_property PACKAGE_PIN M2 [get_ports {fmc_hpc_DP_M2C_P[0]}]
set_property PACKAGE_PIN M1 [get_ports {fmc_hpc_DP_M2C_N[0]}]
set_property PACKAGE_PIN K2 [get_ports {fmc_hpc_DP_M2C_P[1]}]
set_property PACKAGE_PIN K1 [get_ports {fmc_hpc_DP_M2C_N[1]}]
set_property PACKAGE_PIN H2 [get_ports {fmc_hpc_DP_M2C_P[2]}]
set_property PACKAGE_PIN H1 [get_ports {fmc_hpc_DP_M2C_N[2]}]
set_property PACKAGE_PIN F2 [get_ports {fmc_hpc_DP_M2C_P[3]}]
set_property PACKAGE_PIN F1 [get_ports {fmc_hpc_DP_M2C_N[3]}]
set_property PACKAGE_PIN D2 [get_ports {fmc_hpc_DP_M2C_P[4]}]
set_property PACKAGE_PIN D1 [get_ports {fmc_hpc_DP_M2C_N[4]}]
set_property PACKAGE_PIN C4 [get_ports {fmc_hpc_DP_M2C_P[5]}]
set_property PACKAGE_PIN C3 [get_ports {fmc_hpc_DP_M2C_N[5]}]
set_property PACKAGE_PIN A4 [get_ports {fmc_hpc_DP_M2C_P[6]}]
set_property PACKAGE_PIN A3 [get_ports {fmc_hpc_DP_M2C_N[6]}]
set_property PACKAGE_PIN B2 [get_ports {fmc_hpc_DP_M2C_P[7]}]
set_property PACKAGE_PIN B1 [get_ports {fmc_hpc_DP_M2C_N[7]}]

# user clocks
set_property PACKAGE_PIN V24 [get_ports fmc_hpc_CLK_M2C_P_0]
set_property PACKAGE_PIN W24 [get_ports fmc_hpc_CLK_M2C_N_0]
set_property PACKAGE_PIN AB21 [get_ports fmc_hpc_CLK_M2C_P_1]
set_property PACKAGE_PIN AC21 [get_ports fmc_hpc_CLK_M2C_N_1]

# user data LA, 00,01,17,18 are CC compatible
set_property PACKAGE_PIN T25 [get_ports fmc_hpc_LA_P_0]
set_property PACKAGE_PIN U25 [get_ports fmc_hpc_LA_N_0]
set_property PACKAGE_PIN T24 [get_ports fmc_hpc_LA_P_1]
set_property PACKAGE_PIN U24 [get_ports fmc_hpc_LA_N_1]
set_property PACKAGE_PIN T22 [get_ports fmc_hpc_LA_P_3]
set_property PACKAGE_PIN T23 [get_ports fmc_hpc_LA_N_3]
set_property PACKAGE_PIN V26 [get_ports fmc_hpc_LA_N_4]
set_property PACKAGE_PIN U26 [get_ports fmc_hpc_LA_P_4]
set_property PACKAGE_PIN AA18 [get_ports fmc_hpc_LA_N_5]
set_property PACKAGE_PIN Y18 [get_ports fmc_hpc_LA_P_5]
set_property PACKAGE_PIN AA25 [get_ports fmc_hpc_LA_N_7]
set_property PACKAGE_PIN AA24 [get_ports fmc_hpc_LA_P_7]
set_property PACKAGE_PIN Y26 [get_ports fmc_hpc_LA_N_9]
set_property PACKAGE_PIN Y25 [get_ports fmc_hpc_LA_P_9]
set_property PACKAGE_PIN P24 [get_ports fmc_hpc_LA_N_10]
set_property PACKAGE_PIN N24 [get_ports fmc_hpc_LA_P_10]

# I2C

###############################################################################
# general I/O standard constraints
###############################################################################