package chainsaw.projects.xdma

import chainsaw.projects.xdma.daq.Ddr4Interface
import spinal.core._
import spinal.lib.eda.xilinx.boards.alinx.Axku062

// TODO: FL1010 & GPU module

// save vivado project by generating .tcl script
// write_project_tcl -force C:/Users/lsfan/Documents/GitHub/SpinalHDL/chainsaw/src/main/resources/ChainsawDaqSources/CreateChainsawDaq.tcl
// cd C:/Users/lsfan/Documents/GitHub/SpinalHDL/chainsaw/src/main/resources/ChainsawDaqSources
// source CreateChainsawDaq.tcl

// generating memory(flash) configuration file
// write_cfgmem  -format mcs -size 32 -interface SPIx8 -loadbit {up 0x00000000 "C:/Users/lsfan/Desktop/ChainsawDaq/1GHz/ChainsawDaq.bit" } -file "C:/Users/lsfan/Desktop/ChainsawDaq/1GHz/ChainsawDaq.mcs"

// binding user interfaces with clocks
// set_property CONFIG.CLK_DOMAIN Peripheral_jesd204_0_rx_core_clk_out [get_bd_intf_pins /ChainsawDaqDataPath_0/dataIn]
// set_property CONFIG.FREQ_HZ 250000000 [get_bd_intf_pins /ChainsawDaqDataPath_0/dataIn]
// set_property CONFIG.CLK_DOMAIN Peripheral_jesd204_0_rx_core_clk_out [get_bd_intf_pins /ChainsawDaqDataPath_0/dataOut]
// set_property CONFIG.FREQ_HZ 250000000 [get_bd_intf_pins /ChainsawDaqDataPath_0/dataOut]
// set_property CONFIG.CLK_DOMAIN Peripheral_xdma_0_0_axi_aclk [get_bd_intf_pins /ChainsawDaqDataPath_0/controlIn]
// set_property CONFIG.FREQ_HZ 250000000 [get_bd_intf_pins /ChainsawDaqDataPath_0/controlIn]

import scala.language.postfixOps

case class ChainsawXdma() extends Axku062 {

  // pins configuration
//  val ddr4 = Ddr4Interface()

  val peripheral = new Xdma_wrapper()

  // PCIe
  peripheral.reset_rtl_0 := pcie.perst
  peripheral.diff_clock_rtl_0_clk_n := pcie.clk_n
  peripheral.diff_clock_rtl_0_clk_p := pcie.clk_p

//  Seq(pcie.tx_n, pcie.tx_p, pcie.rx_n, pcie.rx_p).foreach(_.setWidth(4))

  peripheral.pcie_7x_mgt_rtl_0_rxn := pcie.rx_n
  peripheral.pcie_7x_mgt_rtl_0_rxp := pcie.rx_p
  pcie.tx_n := peripheral.pcie_7x_mgt_rtl_0_txn
  pcie.tx_p := peripheral.pcie_7x_mgt_rtl_0_txp

//  // DDR4
//  peripheral.ddr4_rst := False // disabled
//  peripheral.ddr4_rtl <> ddr4

  // GPS IO

  // LEDs
  led.clearAll()
  led(0) := peripheral.user_lnk_up
  led(1) := peripheral.user_clk_heartbeat

  led_test.clearAll()

}
