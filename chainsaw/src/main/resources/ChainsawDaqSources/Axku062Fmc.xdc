# FMC physical constraints for AXKU062, including FMC-HPC, FMC-LPC1 and FMC-LPC2

###############################################################################
# FMC-HPC, 8 gigabit transceiver enabled, LA & HA user data enabled(HB unused)
###############################################################################

# gigabit clocks
set_property PACKAGE_PIN M5 [get_ports {fmc_hpc_GBTCLK_M2C_N[0]}]
set_property PACKAGE_PIN M6 [get_ports {fmc_hpc_GBTCLK_M2C_P[0]}]
set_property PACKAGE_PIN H6 [get_ports {fmc_hpc_GBTCLK_M2C_P[1]}]
set_property PACKAGE_PIN H5 [get_ports {fmc_hpc_GBTCLK_M2C_N[1]}]

# gigabit data, starting from GTHE3 @ X1Y12
set_property PACKAGE_PIN M2 [get_ports {fmc_hpc_DP_M2C_P[0]}]
set_property PACKAGE_PIN M1 [get_ports {fmc_hpc_DP_M2C_N[0]}]
set_property PACKAGE_PIN K2 [get_ports {fmc_hpc_DP_M2C_P[1]}]
set_property PACKAGE_PIN K1 [get_ports {fmc_hpc_DP_M2C_N[1]}]
set_property PACKAGE_PIN H2 [get_ports {fmc_hpc_DP_M2C_P[2]}]
set_property PACKAGE_PIN H1 [get_ports {fmc_hpc_DP_M2C_N[2]}]
set_property PACKAGE_PIN F2 [get_ports {fmc_hpc_DP_M2C_P[3]}]
set_property PACKAGE_PIN F1 [get_ports {fmc_hpc_DP_M2C_N[3]}]
#set_property PACKAGE_PIN D2 [get_ports {fmc_hpc_DP_M2C_P[4]}]
#set_property PACKAGE_PIN D1 [get_ports {fmc_hpc_DP_M2C_N[4]}]
#set_property PACKAGE_PIN A4 [get_ports {fmc_hpc_DP_M2C_P[5]}]
#set_property PACKAGE_PIN A3 [get_ports {fmc_hpc_DP_M2C_N[5]}]
#set_property PACKAGE_PIN B2 [get_ports {fmc_hpc_DP_M2C_P[6]}]
#set_property PACKAGE_PIN B1 [get_ports {fmc_hpc_DP_M2C_N[6]}]
#set_property PACKAGE_PIN E4 [get_ports {fmc_hpc_DP_M2C_P[7]}]
#set_property PACKAGE_PIN E3 [get_ports {fmc_hpc_DP_M2C_N[7]}]

#set_property PACKAGE_PIN N4 [get_ports {fmc_hpc_DP_C2M_P[0]}]
#set_property PACKAGE_PIN N3 [get_ports {fmc_hpc_DP_C2M_N[0]}]
#set_property PACKAGE_PIN L4 [get_ports {fmc_hpc_DP_C2M_P[1]}]
#set_property PACKAGE_PIN L3 [get_ports {fmc_hpc_DP_C2M_N[1]}]
#set_property PACKAGE_PIN J4 [get_ports {fmc_hpc_DP_C2M_P[2]}]
#set_property PACKAGE_PIN J3 [get_ports {fmc_hpc_DP_C2M_N[2]}]
#set_property PACKAGE_PIN G4 [get_ports {fmc_hpc_DP_C2M_P[3]}]
#set_property PACKAGE_PIN G3 [get_ports {fmc_hpc_DP_C2M_N[3]}]
#set_property PACKAGE_PIN D6 [get_ports {fmc_hpc_DP_C2M_P[4]}]
#set_property PACKAGE_PIN D5 [get_ports {fmc_hpc_DP_C2M_N[4]}]
#set_property PACKAGE_PIN B6 [get_ports {fmc_hpc_DP_C2M_P[5]}]
#set_property PACKAGE_PIN B5 [get_ports {fmc_hpc_DP_C2M_N[5]}]
#set_property PACKAGE_PIN C4 [get_ports {fmc_hpc_DP_C2M_P[6]}]
#set_property PACKAGE_PIN C3 [get_ports {fmc_hpc_DP_C2M_N[6]}]
#set_property PACKAGE_PIN F6 [get_ports {fmc_hpc_DP_C2M_P[7]}]
#set_property PACKAGE_PIN F5 [get_ports {fmc_hpc_DP_C2M_N[7]}]

# user clocks
set_property PACKAGE_PIN D25 [get_ports {fmc_hpc_CLK_M2C_N[0]}]
set_property PACKAGE_PIN E25 [get_ports {fmc_hpc_CLK_M2C_P[0]}]
set_property PACKAGE_PIN G11 [get_ports {fmc_hpc_CLK_M2C_N[1]}]
set_property PACKAGE_PIN H11 [get_ports {fmc_hpc_CLK_M2C_P[1]}]

# user data LA, 00,01,17,18 are CC compatible
set_property PACKAGE_PIN E23 [get_ports fmc_hpc_LA_N_0]
set_property PACKAGE_PIN E22 [get_ports fmc_hpc_LA_P_0]
set_property PACKAGE_PIN C23 [get_ports fmc_hpc_LA_N_1]
set_property PACKAGE_PIN D23 [get_ports fmc_hpc_LA_P_1]
set_property PACKAGE_PIN A25 [get_ports fmc_hpc_LA_N_2]
set_property PACKAGE_PIN B25 [get_ports fmc_hpc_LA_P_2]
set_property PACKAGE_PIN A28 [get_ports fmc_hpc_LA_N_3]
set_property PACKAGE_PIN A27 [get_ports fmc_hpc_LA_P_3]
set_property PACKAGE_PIN B27 [get_ports fmc_hpc_LA_N_4]
set_property PACKAGE_PIN C27 [get_ports fmc_hpc_LA_P_4]
set_property PACKAGE_PIN C24 [get_ports fmc_hpc_LA_N_5]
set_property PACKAGE_PIN D24 [get_ports fmc_hpc_LA_P_5]
set_property PACKAGE_PIN A29 [get_ports fmc_hpc_LA_N_6]
set_property PACKAGE_PIN B29 [get_ports fmc_hpc_LA_P_6]
set_property PACKAGE_PIN C28 [get_ports fmc_hpc_LA_N_7]
set_property PACKAGE_PIN D28 [get_ports fmc_hpc_LA_P_7]
set_property PACKAGE_PIN E27 [get_ports fmc_hpc_LA_N_8]
set_property PACKAGE_PIN F27 [get_ports fmc_hpc_LA_P_8]
set_property PACKAGE_PIN B26 [get_ports fmc_hpc_LA_N_9]
set_property PACKAGE_PIN C26 [get_ports fmc_hpc_LA_P_9]
set_property PACKAGE_PIN A24 [get_ports fmc_hpc_LA_N_10]
set_property PACKAGE_PIN B24 [get_ports fmc_hpc_LA_P_10]
set_property PACKAGE_PIN D26 [get_ports fmc_hpc_LA_N_11]
set_property PACKAGE_PIN E26 [get_ports fmc_hpc_LA_P_11]
set_property PACKAGE_PIN D29 [get_ports fmc_hpc_LA_N_12]
set_property PACKAGE_PIN E28 [get_ports fmc_hpc_LA_P_12]
set_property PACKAGE_PIN B22 [get_ports fmc_hpc_LA_N_13]
set_property PACKAGE_PIN B21 [get_ports fmc_hpc_LA_P_13]
set_property PACKAGE_PIN D21 [get_ports fmc_hpc_LA_N_14]
set_property PACKAGE_PIN D20 [get_ports fmc_hpc_LA_P_14]
set_property PACKAGE_PIN A20 [get_ports fmc_hpc_LA_N_15]
set_property PACKAGE_PIN B20 [get_ports fmc_hpc_LA_P_15]
set_property PACKAGE_PIN C22 [get_ports fmc_hpc_LA_N_16]
set_property PACKAGE_PIN C21 [get_ports fmc_hpc_LA_P_16]
set_property PACKAGE_PIN F9 [get_ports fmc_hpc_LA_N_17]
set_property PACKAGE_PIN G9 [get_ports fmc_hpc_LA_P_17]
set_property PACKAGE_PIN F10 [get_ports fmc_hpc_LA_N_18]
set_property PACKAGE_PIN G10 [get_ports fmc_hpc_LA_P_18]
set_property PACKAGE_PIN B11 [get_ports fmc_hpc_LA_N_19]
set_property PACKAGE_PIN C11 [get_ports fmc_hpc_LA_P_19]
set_property PACKAGE_PIN A12 [get_ports fmc_hpc_LA_N_20]
set_property PACKAGE_PIN A13 [get_ports fmc_hpc_LA_P_20]
set_property PACKAGE_PIN J11 [get_ports fmc_hpc_LA_N_21]
set_property PACKAGE_PIN K11 [get_ports fmc_hpc_LA_P_21]
set_property PACKAGE_PIN D11 [get_ports fmc_hpc_LA_N_22]
set_property PACKAGE_PIN E11 [get_ports fmc_hpc_LA_P_22]
set_property PACKAGE_PIN H13 [get_ports fmc_hpc_LA_N_23]
set_property PACKAGE_PIN J13 [get_ports fmc_hpc_LA_P_23]
set_property PACKAGE_PIN H9 [get_ports fmc_hpc_LA_N_24]
set_property PACKAGE_PIN J9 [get_ports fmc_hpc_LA_P_24]
set_property PACKAGE_PIN J10 [get_ports fmc_hpc_LA_N_25]
set_property PACKAGE_PIN K10 [get_ports fmc_hpc_LA_P_25]
set_property PACKAGE_PIN D10 [get_ports fmc_hpc_LA_N_26]
set_property PACKAGE_PIN E10 [get_ports fmc_hpc_LA_P_26]
set_property PACKAGE_PIN C9 [get_ports fmc_hpc_LA_N_27]
set_property PACKAGE_PIN D9 [get_ports fmc_hpc_LA_P_27]
set_property PACKAGE_PIN A9 [get_ports fmc_hpc_LA_N_28]
set_property PACKAGE_PIN B9 [get_ports fmc_hpc_LA_P_28]
set_property PACKAGE_PIN A10 [get_ports fmc_hpc_LA_N_29]
set_property PACKAGE_PIN B10 [get_ports fmc_hpc_LA_P_29]
set_property PACKAGE_PIN H8 [get_ports fmc_hpc_LA_N_30]
set_property PACKAGE_PIN J8 [get_ports fmc_hpc_LA_P_30]
set_property PACKAGE_PIN E8 [get_ports fmc_hpc_LA_N_31]
set_property PACKAGE_PIN F8 [get_ports fmc_hpc_LA_P_31]
set_property PACKAGE_PIN C8 [get_ports fmc_hpc_LA_N_32]
set_property PACKAGE_PIN D8 [get_ports fmc_hpc_LA_P_32]
set_property PACKAGE_PIN K8 [get_ports fmc_hpc_LA_N_33]
set_property PACKAGE_PIN L8 [get_ports fmc_hpc_LA_P_33]

# user data HA, 00,01,17 are CC compatible
set_property PACKAGE_PIN F17 [get_ports fmc_hpc_HA_N_0]
set_property PACKAGE_PIN F18 [get_ports fmc_hpc_HA_P_0]
set_property PACKAGE_PIN E17 [get_ports fmc_hpc_HA_N_1]
set_property PACKAGE_PIN E18 [get_ports fmc_hpc_HA_P_1]
set_property PACKAGE_PIN H16 [get_ports fmc_hpc_HA_N_2]
set_property PACKAGE_PIN H17 [get_ports fmc_hpc_HA_P_2]
set_property PACKAGE_PIN L18 [get_ports fmc_hpc_HA_N_3]
set_property PACKAGE_PIN L19 [get_ports fmc_hpc_HA_P_3]
set_property PACKAGE_PIN C17 [get_ports fmc_hpc_HA_N_4]
set_property PACKAGE_PIN C18 [get_ports fmc_hpc_HA_P_4]
set_property PACKAGE_PIN A18 [get_ports fmc_hpc_HA_N_5]
set_property PACKAGE_PIN A19 [get_ports fmc_hpc_HA_P_5]
set_property PACKAGE_PIN J18 [get_ports fmc_hpc_HA_N_6]
set_property PACKAGE_PIN J19 [get_ports fmc_hpc_HA_P_6]
set_property PACKAGE_PIN B19 [get_ports fmc_hpc_HA_N_7]
set_property PACKAGE_PIN C19 [get_ports fmc_hpc_HA_P_7]
set_property PACKAGE_PIN H18 [get_ports fmc_hpc_HA_N_8]
set_property PACKAGE_PIN H19 [get_ports fmc_hpc_HA_P_8]
set_property PACKAGE_PIN C14 [get_ports fmc_hpc_HA_N_9]
set_property PACKAGE_PIN D14 [get_ports fmc_hpc_HA_P_9]
set_property PACKAGE_PIN A14 [get_ports fmc_hpc_HA_N_10]
set_property PACKAGE_PIN B14 [get_ports fmc_hpc_HA_P_10]
set_property PACKAGE_PIN B16 [get_ports fmc_hpc_HA_N_11]
set_property PACKAGE_PIN B17 [get_ports fmc_hpc_HA_P_11]
set_property PACKAGE_PIN F19 [get_ports fmc_hpc_HA_N_12]
set_property PACKAGE_PIN G19 [get_ports fmc_hpc_HA_P_12]
set_property PACKAGE_PIN A15 [get_ports fmc_hpc_HA_N_13]
set_property PACKAGE_PIN B15 [get_ports fmc_hpc_HA_P_13]
set_property PACKAGE_PIN J16 [get_ports fmc_hpc_HA_N_14]
set_property PACKAGE_PIN K16 [get_ports fmc_hpc_HA_P_14]
set_property PACKAGE_PIN K17 [get_ports fmc_hpc_HA_N_15]
set_property PACKAGE_PIN K18 [get_ports fmc_hpc_HA_P_15]
set_property PACKAGE_PIN D18 [get_ports fmc_hpc_HA_N_16]
set_property PACKAGE_PIN D19 [get_ports fmc_hpc_HA_P_16]
set_property PACKAGE_PIN G16 [get_ports fmc_hpc_HA_N_17]
set_property PACKAGE_PIN G17 [get_ports fmc_hpc_HA_P_17]
set_property PACKAGE_PIN K15 [get_ports fmc_hpc_HA_N_18]
set_property PACKAGE_PIN L15 [get_ports fmc_hpc_HA_P_18]
set_property PACKAGE_PIN G14 [get_ports fmc_hpc_HA_N_19]
set_property PACKAGE_PIN G15 [get_ports fmc_hpc_HA_P_19]
set_property PACKAGE_PIN D16 [get_ports fmc_hpc_HA_N_20]
set_property PACKAGE_PIN E16 [get_ports fmc_hpc_HA_P_20]
set_property PACKAGE_PIN J14 [get_ports fmc_hpc_HA_N_21]
set_property PACKAGE_PIN J15 [get_ports fmc_hpc_HA_P_21]
set_property PACKAGE_PIN D15 [get_ports fmc_hpc_HA_N_22]
set_property PACKAGE_PIN E15 [get_ports fmc_hpc_HA_P_22]
set_property PACKAGE_PIN F14 [get_ports fmc_hpc_HA_N_23]
set_property PACKAGE_PIN F15 [get_ports fmc_hpc_HA_P_23]

# I2C
set_property PACKAGE_PIN K12 [get_ports fmc_hpc_SCL]
set_property PACKAGE_PIN L12 [get_ports fmc_hpc_SDA]

###############################################################################
# FMC-LPC 1, 1 gigabit transceiver enabled, LA user data enabled, 1.8V
###############################################################################

# user clocks
set_property PACKAGE_PIN AA23 [get_ports {fmc_lpc_1_CLK_M2C_N[0]}]
set_property PACKAGE_PIN Y23 [get_ports {fmc_lpc_1_CLK_M2C_P[0]}]
set_property PACKAGE_PIN AB31 [get_ports {fmc_lpc_1_CLK_M2C_N[1]}]
set_property PACKAGE_PIN AB30 [get_ports {fmc_lpc_1_CLK_M2C_P[1]}]

# user data LA
set_property PACKAGE_PIN W24 [get_ports fmc_lpc_1_LA_N_0]
set_property PACKAGE_PIN W23 [get_ports fmc_lpc_1_LA_P_0]
set_property PACKAGE_PIN AA25 [get_ports fmc_lpc_1_LA_N_1]
set_property PACKAGE_PIN AA24 [get_ports fmc_lpc_1_LA_P_1]
set_property PACKAGE_PIN W21 [get_ports fmc_lpc_1_LA_N_2]
set_property PACKAGE_PIN V21 [get_ports fmc_lpc_1_LA_P_2]
set_property PACKAGE_PIN V23 [get_ports fmc_lpc_1_LA_N_3]
set_property PACKAGE_PIN V22 [get_ports fmc_lpc_1_LA_P_3]
set_property PACKAGE_PIN AB26 [get_ports fmc_lpc_1_LA_N_4]
set_property PACKAGE_PIN AB25 [get_ports fmc_lpc_1_LA_P_4]
set_property PACKAGE_PIN W29 [get_ports fmc_lpc_1_LA_N_5]
set_property PACKAGE_PIN V29 [get_ports fmc_lpc_1_LA_P_5]
set_property PACKAGE_PIN Y27 [get_ports fmc_lpc_1_LA_N_6]
set_property PACKAGE_PIN Y26 [get_ports fmc_lpc_1_LA_P_6]
set_property PACKAGE_PIN U22 [get_ports fmc_lpc_1_LA_N_7]
set_property PACKAGE_PIN U21 [get_ports fmc_lpc_1_LA_P_7]
set_property PACKAGE_PIN W26 [get_ports fmc_lpc_1_LA_N_8]
set_property PACKAGE_PIN V26 [get_ports fmc_lpc_1_LA_P_8]
set_property PACKAGE_PIN T23 [get_ports fmc_lpc_1_LA_N_9]
set_property PACKAGE_PIN T22 [get_ports fmc_lpc_1_LA_P_9]
set_property PACKAGE_PIN U25 [get_ports fmc_lpc_1_LA_N_10]
set_property PACKAGE_PIN U24 [get_ports fmc_lpc_1_LA_P_10]
set_property PACKAGE_PIN AC24 [get_ports fmc_lpc_1_LA_N_11]
set_property PACKAGE_PIN AB24 [get_ports fmc_lpc_1_LA_P_11]
set_property PACKAGE_PIN U27 [get_ports fmc_lpc_1_LA_N_12]
set_property PACKAGE_PIN U26 [get_ports fmc_lpc_1_LA_P_12]
set_property PACKAGE_PIN Y28 [get_ports fmc_lpc_1_LA_N_13]
set_property PACKAGE_PIN W28 [get_ports fmc_lpc_1_LA_P_13]
set_property PACKAGE_PIN V28 [get_ports fmc_lpc_1_LA_N_14]
set_property PACKAGE_PIN V27 [get_ports fmc_lpc_1_LA_P_14]
set_property PACKAGE_PIN Y25 [get_ports fmc_lpc_1_LA_N_15]
set_property PACKAGE_PIN W25 [get_ports fmc_lpc_1_LA_P_15]
set_property PACKAGE_PIN AB22 [get_ports fmc_lpc_1_LA_N_16]
set_property PACKAGE_PIN AA22 [get_ports fmc_lpc_1_LA_P_16]
set_property PACKAGE_PIN AB32 [get_ports fmc_lpc_1_LA_N_17]
set_property PACKAGE_PIN AA32 [get_ports fmc_lpc_1_LA_P_17]
set_property PACKAGE_PIN AD31 [get_ports fmc_lpc_1_LA_N_18]
set_property PACKAGE_PIN AD30 [get_ports fmc_lpc_1_LA_P_18]
set_property PACKAGE_PIN AB29 [get_ports fmc_lpc_1_LA_N_19]
set_property PACKAGE_PIN AA29 [get_ports fmc_lpc_1_LA_P_19]
set_property PACKAGE_PIN W31 [get_ports fmc_lpc_1_LA_N_20]
set_property PACKAGE_PIN V31 [get_ports fmc_lpc_1_LA_P_20]
set_property PACKAGE_PIN AG30 [get_ports fmc_lpc_1_LA_N_21]
set_property PACKAGE_PIN AF30 [get_ports fmc_lpc_1_LA_P_21]
set_property PACKAGE_PIN AE30 [get_ports fmc_lpc_1_LA_N_22]
set_property PACKAGE_PIN AD29 [get_ports fmc_lpc_1_LA_P_22]
set_property PACKAGE_PIN AG34 [get_ports fmc_lpc_1_LA_N_23]
set_property PACKAGE_PIN AF33 [get_ports fmc_lpc_1_LA_P_23]
set_property PACKAGE_PIN AG29 [get_ports fmc_lpc_1_LA_N_24]
set_property PACKAGE_PIN AF29 [get_ports fmc_lpc_1_LA_P_24]
set_property PACKAGE_PIN AF32 [get_ports fmc_lpc_1_LA_N_25]
set_property PACKAGE_PIN AE32 [get_ports fmc_lpc_1_LA_P_25]
set_property PACKAGE_PIN AG32 [get_ports fmc_lpc_1_LA_N_26]
set_property PACKAGE_PIN AG31 [get_ports fmc_lpc_1_LA_P_26]
set_property PACKAGE_PIN AF34 [get_ports fmc_lpc_1_LA_N_27]
set_property PACKAGE_PIN AE33 [get_ports fmc_lpc_1_LA_P_27]
set_property PACKAGE_PIN AF27 [get_ports fmc_lpc_1_LA_N_28]
set_property PACKAGE_PIN AE27 [get_ports fmc_lpc_1_LA_P_28]
set_property PACKAGE_PIN AF28 [get_ports fmc_lpc_1_LA_N_29]
set_property PACKAGE_PIN AE28 [get_ports fmc_lpc_1_LA_P_29]
set_property PACKAGE_PIN AD28 [get_ports fmc_lpc_1_LA_N_30]
set_property PACKAGE_PIN AC28 [get_ports fmc_lpc_1_LA_P_30]
set_property PACKAGE_PIN Y33 [get_ports fmc_lpc_1_LA_N_31]
set_property PACKAGE_PIN W33 [get_ports fmc_lpc_1_LA_P_31]
set_property PACKAGE_PIN Y32 [get_ports fmc_lpc_1_LA_N_32]
set_property PACKAGE_PIN Y31 [get_ports fmc_lpc_1_LA_P_32]
set_property PACKAGE_PIN Y30 [get_ports fmc_lpc_1_LA_N_33]
set_property PACKAGE_PIN W30 [get_ports fmc_lpc_1_LA_P_33]

# gigabit clocks
set_property PACKAGE_PIN V5 [get_ports {fmc_lpc_1_GBTCLK_M2C_N[0]}]
set_property PACKAGE_PIN V6 [get_ports {fmc_lpc_1_GBTCLK_M2C_P[0]}]

# gigabit data
set_property PACKAGE_PIN R3 [get_ports {fmc_lpc_1_DP_C2M_N[1]}]
set_property PACKAGE_PIN R4 [get_ports {fmc_lpc_1_DP_C2M_P[1]}]
set_property PACKAGE_PIN P1 [get_ports {fmc_lpc_1_DP_M2C_N[0]}]
set_property PACKAGE_PIN P2 [get_ports {fmc_lpc_1_DP_M2C_P[0]}]

set_property PACKAGE_PIN AC21 [get_ports fmc_lpc_1_SCL]
set_property PACKAGE_PIN AB21 [get_ports fmc_lpc_1_SDA]

###############################################################################
# FMC-LPC 2, no gigabit transceiver enabled, LA user data enabled, 3.3V
###############################################################################

# user clocks
set_property PACKAGE_PIN N26 [get_ports {fmc_lpc_2_CLK_M2C_N[0]}]
set_property PACKAGE_PIN P26  [get_ports {fmc_lpc_2_CLK_M2C_P[0]}]
set_property PACKAGE_PIN AH12 [get_ports {fmc_lpc_2_CLK_M2C_N[1]}]
set_property PACKAGE_PIN AG12 [get_ports {fmc_lpc_2_CLK_M2C_P[1]}]

# user data LA, 00,01,17,18 are CC compatible
set_property PACKAGE_PIN P25 [get_ports fmc_lpc_2_LA_N_0]
set_property PACKAGE_PIN P24 [get_ports fmc_lpc_2_LA_P_0]
set_property PACKAGE_PIN M26 [get_ports fmc_lpc_2_LA_N_1]
set_property PACKAGE_PIN M25 [get_ports fmc_lpc_2_LA_P_1]
set_property PACKAGE_PIN R26 [get_ports fmc_lpc_2_LA_N_2]
set_property PACKAGE_PIN R25 [get_ports fmc_lpc_2_LA_P_2]
set_property PACKAGE_PIN L27 [get_ports fmc_lpc_2_LA_N_3]
set_property PACKAGE_PIN M27 [get_ports fmc_lpc_2_LA_P_3]
set_property PACKAGE_PIN R27 [get_ports fmc_lpc_2_LA_N_4]
set_property PACKAGE_PIN T27 [get_ports fmc_lpc_2_LA_P_4]
set_property PACKAGE_PIN J25 [get_ports fmc_lpc_2_LA_N_5]
set_property PACKAGE_PIN J24 [get_ports fmc_lpc_2_LA_P_5]
set_property PACKAGE_PIN K27 [get_ports fmc_lpc_2_LA_N_6]
set_property PACKAGE_PIN K26 [get_ports fmc_lpc_2_LA_P_6]
set_property PACKAGE_PIN H26 [get_ports fmc_lpc_2_LA_N_7]
set_property PACKAGE_PIN J26 [get_ports fmc_lpc_2_LA_P_7]
set_property PACKAGE_PIN P23 [get_ports fmc_lpc_2_LA_N_8]
set_property PACKAGE_PIN R23 [get_ports fmc_lpc_2_LA_P_8]
set_property PACKAGE_PIN G27 [get_ports fmc_lpc_2_LA_N_9]
set_property PACKAGE_PIN H27 [get_ports fmc_lpc_2_LA_P_9]
set_property PACKAGE_PIN P21 [get_ports fmc_lpc_2_LA_N_10]
set_property PACKAGE_PIN P20 [get_ports fmc_lpc_2_LA_P_10]
set_property PACKAGE_PIN K25 [get_ports fmc_lpc_2_LA_N_11]
set_property PACKAGE_PIN L25 [get_ports fmc_lpc_2_LA_P_11]
set_property PACKAGE_PIN M24 [get_ports fmc_lpc_2_LA_N_12]
set_property PACKAGE_PIN N24 [get_ports fmc_lpc_2_LA_P_12]
set_property PACKAGE_PIN M22 [get_ports fmc_lpc_2_LA_N_13]
set_property PACKAGE_PIN N22 [get_ports fmc_lpc_2_LA_P_13]
set_property PACKAGE_PIN M21 [get_ports fmc_lpc_2_LA_N_14]
set_property PACKAGE_PIN N21 [get_ports fmc_lpc_2_LA_P_14]
set_property PACKAGE_PIN K23 [get_ports fmc_lpc_2_LA_N_15]
set_property PACKAGE_PIN L22 [get_ports fmc_lpc_2_LA_P_15]
set_property PACKAGE_PIN H24 [get_ports fmc_lpc_2_LA_N_16]
set_property PACKAGE_PIN J23 [get_ports fmc_lpc_2_LA_P_16]
set_property PACKAGE_PIN AG10 [get_ports fmc_lpc_2_LA_N_17]
set_property PACKAGE_PIN AF10 [get_ports fmc_lpc_2_LA_P_17]
set_property PACKAGE_PIN AH11 [get_ports fmc_lpc_2_LA_N_18]
set_property PACKAGE_PIN AG11 [get_ports fmc_lpc_2_LA_P_18]
set_property PACKAGE_PIN AD8 [get_ports fmc_lpc_2_LA_N_19]
set_property PACKAGE_PIN AD9 [get_ports fmc_lpc_2_LA_P_19]
set_property PACKAGE_PIN AJ8 [get_ports fmc_lpc_2_LA_N_20]
set_property PACKAGE_PIN AJ9 [get_ports fmc_lpc_2_LA_P_20]
set_property PACKAGE_PIN AG9 [get_ports fmc_lpc_2_LA_N_21]
set_property PACKAGE_PIN AF9 [get_ports fmc_lpc_2_LA_P_21]
set_property PACKAGE_PIN AF8 [get_ports fmc_lpc_2_LA_N_22]
set_property PACKAGE_PIN AE8 [get_ports fmc_lpc_2_LA_P_22]
set_property PACKAGE_PIN AE10 [get_ports fmc_lpc_2_LA_N_23]
set_property PACKAGE_PIN AD10 [get_ports fmc_lpc_2_LA_P_23]
set_property PACKAGE_PIN AP10 [get_ports fmc_lpc_2_LA_N_24]
set_property PACKAGE_PIN AP11 [get_ports fmc_lpc_2_LA_P_24]
set_property PACKAGE_PIN AN12 [get_ports fmc_lpc_2_LA_N_25]
set_property PACKAGE_PIN AM12 [get_ports fmc_lpc_2_LA_P_25]
set_property PACKAGE_PIN AL9 [get_ports fmc_lpc_2_LA_N_26]
set_property PACKAGE_PIN AK10 [get_ports fmc_lpc_2_LA_P_26]
set_property PACKAGE_PIN AL8 [get_ports fmc_lpc_2_LA_N_27]
set_property PACKAGE_PIN AK8 [get_ports fmc_lpc_2_LA_P_27]
set_property PACKAGE_PIN AH8 [get_ports fmc_lpc_2_LA_N_28]
set_property PACKAGE_PIN AH9 [get_ports fmc_lpc_2_LA_P_28]
set_property PACKAGE_PIN AL13 [get_ports fmc_lpc_2_LA_N_29]
set_property PACKAGE_PIN AK13 [get_ports fmc_lpc_2_LA_P_29]
set_property PACKAGE_PIN AJ13 [get_ports fmc_lpc_2_LA_N_30]
set_property PACKAGE_PIN AH13 [get_ports fmc_lpc_2_LA_P_30]
set_property PACKAGE_PIN AE11 [get_ports fmc_lpc_2_LA_N_31]
set_property PACKAGE_PIN AD11 [get_ports fmc_lpc_2_LA_P_31]
set_property PACKAGE_PIN AF13 [get_ports fmc_lpc_2_LA_N_32]
set_property PACKAGE_PIN AE13 [get_ports fmc_lpc_2_LA_P_32]
set_property PACKAGE_PIN AF12 [get_ports fmc_lpc_2_LA_N_33]
set_property PACKAGE_PIN AE12 [get_ports fmc_lpc_2_LA_P_33]

# I2C
set_property PACKAGE_PIN K21 [get_ports fmc_lpc_2_SCL]
set_property PACKAGE_PIN K22 [get_ports fmc_lpc_2_SDA]

###############################################################################
# general I/O standard constraints
###############################################################################

# user data pins are set as single-ended by default, when used as differential pairs, set them in project-specific constraints
set_property IOSTANDARD LVCMOS18 [get_ports fmc_hpc_LA_*]
set_property IOSTANDARD LVCMOS18 [get_ports fmc_hpc_HA_*]
set_property IOSTANDARD LVCMOS18 [get_ports fmc_lpc_1_LA_*]
set_property IOSTANDARD LVCMOS33 [get_ports fmc_lpc_2_LA_*]

set_property IOSTANDARD LVCMOS33 [get_ports fmc_lpc_2_CLK_*]
# TODO: all IOSTANDARD constraints

###############################################################################
# project-specific constraints
###############################################################################

# LVDS pairs in ChainsawDaq project
set_property IOSTANDARD LVDS [get_ports fmc_hpc_LA_P_0]
set_property IOSTANDARD LVDS [get_ports fmc_hpc_LA_N_0]
set_property IOSTANDARD LVDS [get_ports fmc_hpc_LA_P_1]
set_property IOSTANDARD LVDS [get_ports fmc_hpc_LA_N_1]
set_property IOSTANDARD LVDS [get_ports fmc_hpc_LA_P_3]
set_property IOSTANDARD LVDS [get_ports fmc_hpc_LA_N_3]

