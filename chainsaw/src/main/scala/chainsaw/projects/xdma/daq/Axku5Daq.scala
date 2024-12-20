package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.xilinx.ultrascale.{IBUFDS, OBUFDS}
import spinal.lib.eda.xilinx.boards.alinx.Axku5

//set_property CONFIG.CLK_DOMAIN Axku5Peripheral_PCIe_0_axi_aclk [get_bd_intf_pins /ChainsawDaqDataPath_0/controlIn]
//set_property CONFIG.FREQ_HZ 125000000 [get_bd_intf_pins /ChainsawDaqDataPath_0/controlIn]
//set_property CONFIG.CLK_DOMAIN Axku5Peripheral_jesd204_buffer_0_IBUF_DS_ODIV2 [get_bd_intf_pins /ChainsawDaqDataPath_0/dataIn]
//set_property CONFIG.FREQ_HZ 250000000 [get_bd_intf_pins /ChainsawDaqDataPath_0/dataIn]
//set_property CONFIG.CLK_DOMAIN Axku5Peripheral_jesd204_buffer_0_IBUF_DS_ODIV2 [get_bd_intf_pins /ChainsawDaqDataPath_0/dataOut]
//set_property CONFIG.FREQ_HZ 250000000 [get_bd_intf_pins /ChainsawDaqDataPath_0/dataOut]


//
// write_cfgmem  -format bin -size 64 -interface SPIx8 -loadbit {up 0x00000000 "C:/Users/lsfan/Desktop/Axku062Daq/Axku062Daq.runs/impl_1/Axku062Daq.bit" } -force

case class Axku5Daq() extends Axku5 {

  val peripheral = new Axku5Peripheral_wrapper()

  // PCIe
  Seq(pcie.tx_n, pcie.tx_p, pcie.rx_n, pcie.rx_p).foreach(_.setWidth(4))
  peripheral.pcie_clk_clk_p := pcie.clk_p
  peripheral.pcie_clk_clk_n := pcie.clk_n
  peripheral.pcie_rstn := pcie.perst
  pcie.tx_p := peripheral.pcie_mgt_txp
  pcie.tx_n := peripheral.pcie_mgt_txn
  peripheral.pcie_mgt_rxp := pcie.rx_p
  peripheral.pcie_mgt_rxn := pcie.rx_n

  // DDR4
  peripheral.ddr4_rst := False
  peripheral.ddr4_ref_clk_clk_p := ddr4_clk_p
  peripheral.ddr4_ref_clk_clk_n := ddr4_clk_n
  peripheral.ddr4_rtl <> ddr4

  // TODO: make them all directionless + bool
  fmc_hpc.DP_C2M_P.setAsDirectionLess() // disable unused output
  fmc_hpc.DP_C2M_N.setAsDirectionLess()

  // LDF17x-1G14-D5 FMC part V1.0.3
  val mysoowFmc = MysoowFmc(fmc_hpc)

  // HMC7044 configuration
  mysoowFmc.hmc7044_rstn := peripheral.hmc7044Resetn
  mysoowFmc.hmc7044_sclk := peripheral.hmc7044_sclk
  mysoowFmc.hmc7044_slen := peripheral.hmc7044_slen
  mysoowFmc.hmc7044_sdio <> peripheral.hmc7044_sdio
  mysoowFmc.hmc7044_sync := False // disabled

  // AD9695 configuration
  mysoowFmc.adc1_powerdown := peripheral.ad9695PowerDown // disabled
  mysoowFmc.adc1_sclk := peripheral.ad9695_sclk
  mysoowFmc.adc1_csn := peripheral.ad9695_slen
  mysoowFmc.adc1_sdio <> peripheral.ad9695_sdio

  mysoowFmc.ch1_dc_sw := False // AC coupling
  mysoowFmc.ch2_dc_sw := False // AC coupling

  // JESD204 configuration
  val adc_core_clk = IBUFDS.Lvds2Clk(mysoowFmc.adc1_core_clk_p, mysoowFmc.adc1_core_clk_n)
  val adc_sysref = IBUFDS.Lvds2Clk(mysoowFmc.adc1_sysref_p, mysoowFmc.adc1_sysref_n)
  val (adcSyncP, adcSyncN) = OBUFDS.Clk2Lvds(peripheral.jesd204_rx_sync)

  peripheral.jesd204_rx_reset := peripheral.jesd204Reset
  peripheral.jesd204_drpclk := defaultClockDomain.clock
  peripheral.jesd204_refclk_clk_p := mysoowFmc.adc1_mgt_clk_p
  peripheral.jesd204_refclk_clk_n := mysoowFmc.adc1_mgt_clk_n
  peripheral.jesd204_rx_sysref := adc_sysref
  // adc_core_clk won't be used in our clocking scheme
//  peripheral.jesd204_rx_core_clk := adc_core_clk
  peripheral.jesd204_rxp := mysoowFmc.adc1_data_p
  peripheral.jesd204_rxn := mysoowFmc.adc1_data_n
  mysoowFmc.adc1_sync_p := adcSyncP
  mysoowFmc.adc1_sync_n := adcSyncN

  // pulse generation
  user_40pin.IO_P(15).asOutput() := peripheral.pulse_gen_0
  user_40pin.IO_N(15).asOutput() := False
  user_40pin.IO_P(16).asOutput() := peripheral.pulse_gen_1
  user_40pin.IO_N(16).asOutput() := False

  ////////DEBUG////////
  val debugClockingArea = new ClockingArea(defaultClockDomain){
    val divider_factor = 10
    val divider = CounterFreeRun(divider_factor)
    user_40pin.IO_P(0).asOutput() := RegNext(divider.value >= (divider_factor / 2))
    user_40pin.IO_N(0).asOutput() := RegNext(divider.value < (divider_factor / 2))
    // HMC7044 output
    user_40pin.IO_P(1).asOutput() := IBUFDS.Lvds2Clk(mysoowFmc.hmc7044_channel_2_p, mysoowFmc.hmc7044_channel_2_n)
    user_40pin.IO_N(1).asOutput() := False
    user_40pin.IO_P(2).asOutput() := IBUFDS.Lvds2Clk(mysoowFmc.hmc7044_channel_3_p, mysoowFmc.hmc7044_channel_3_n)
    user_40pin.IO_N(2).asOutput() := False
    user_40pin.IO_P(3).asOutput() := adc_sysref
    user_40pin.IO_N(3).asOutput() := False
    user_40pin.IO_P(4).asOutput() := adc_core_clk
    user_40pin.IO_N(4).asOutput() := False

    // LEDs
    led.assignDontCare()
    led(0) := peripheral.pcie_link_up
    led(1) := peripheral.ddr4_init_done
  }


}

