package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.xilinx.ultrascale.{IBUFDS, OBUFDS}
import spinal.lib.eda.xilinx.boards.alinx.Axku5

case class Axku5Daq() extends Axku5 {

  // board connection
  val mysoowFmc = MysoowFmc(fmc_hpc)
  fmc_hpc.DP_C2M_P.setAsDirectionLess() // disable unused output
  fmc_hpc.DP_C2M_N.setAsDirectionLess()

  val peripheral = Axku5Peripheral_wrapper()

  // PCIe Gen3 X 4
  Seq(pcie.tx_n, pcie.tx_p, pcie.rx_n, pcie.rx_p).foreach(_.setWidth(4))
  peripheral.pcie_rstn := pcie.perst
  peripheral.pcie_clk_clk_p := pcie.clk_p
  peripheral.pcie_clk_clk_n := pcie.clk_n

  peripheral.pcie_mgt_rxp := pcie.rx_p
  peripheral.pcie_mgt_rxn := pcie.rx_n
  pcie.tx_p := peripheral.pcie_mgt_txp
  pcie.tx_n := peripheral.pcie_mgt_txn

  // DDR4
  peripheral.ddr4_rst := False
  peripheral.ddr4_ref_clk_clk_p := ddr4_clk_p
  peripheral.ddr4_ref_clk_clk_n := ddr4_clk_n
  peripheral.ddr4_rtl <> ddr4

  // HMC7044 configuration
  mysoowFmc.hmc7044_sclk := peripheral.hmc7044_sclk
  mysoowFmc.hmc7044_slen := peripheral.hmc7044_slen
  mysoowFmc.hmc7044_sdio <> peripheral.hmc7044_sdio
  mysoowFmc.hmc7044_rstn := peripheral.hmc7044Resetn
  mysoowFmc.hmc7044_sync := False // disabled

  // AD9695 configuration
  mysoowFmc.adc1_powerdown := peripheral.ad9695PowerDown
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
  peripheral.jesd204_refclk_clk_p := mysoowFmc.adc1_mgt_clk_p //
  peripheral.jesd204_refclk_clk_n := mysoowFmc.adc1_mgt_clk_n
  peripheral.jesd204_rx_sysref := adc_sysref
  peripheral.jesd204_rxp := mysoowFmc.adc1_data_p
  peripheral.jesd204_rxn := mysoowFmc.adc1_data_n
  mysoowFmc.adc1_sync_p := adcSyncP
  mysoowFmc.adc1_sync_n := adcSyncN
  peripheral.jesd204_drpclk := defaultClockDomain.clock

  // pulse generation
  user_40pin.IO_P(15).asOutput() := peripheral.pulse_gen_0
  user_40pin.IO_N(15).asOutput() := False
  user_40pin.IO_P(16).asOutput() := peripheral.pulse_gen_1
  user_40pin.IO_N(16).asOutput() := False

  //////// DEBUG////////
  val debugClockingArea = new ClockingArea(defaultClockDomain) {
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
