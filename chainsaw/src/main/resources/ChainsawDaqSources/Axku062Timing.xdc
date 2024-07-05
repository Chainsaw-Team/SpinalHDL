# false paths
#set_false_path -from [get_clocks -of_objects [get_pins blockDesign/XDMA_i/xdma/inst/pcie3_ip_i/inst/gt_top_i/phy_clk_i/bufg_gt_userclk/O]] -to [get_clocks sys_clk_p]
#set_false_path -from [get_clocks -of_objects [get_pins blockDesign/XDMA_i/ddr4/inst/u_ddr4_infrastructure/gen_mmcme3.u_mmcme_adv_inst/CLKOUT0]] -to [get_clocks sys_clk_p]