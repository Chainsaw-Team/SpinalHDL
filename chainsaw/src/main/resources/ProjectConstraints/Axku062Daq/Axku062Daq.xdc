create_clock -name data_clk -period 4.0 -waveform {0.0 2.0} [get_nets peripheral/Peripheral_i/Datapath/dataClk]
# create_generated_clock -name data_clk [get_nets peripheral/Peripheral_i/jesd204_bufg_gt/BUFG_GT_O[0]]
# create_clock -name control_clk -period 8.0 -waveform {0.0 4.0} [get_nets peripheral/Peripheral_i/Datapath/controlClk]

