package chainsaw.projects.xdma.daq

import chainsaw.projects.xdma.axku5Daq.MysoowFmc
import spinal.core._
import spinal.lib.CounterFreeRun
import spinal.lib.blackbox.xilinx.ultrascale.{IBUFDS, OBUFDS}
import spinal.lib.eda.xilinx.boards.alinx.Axku062

// TODO: FL1010 & GPU module
// useful tcl commands
// generate memory configuration file
// write_cfgmem  -format bin -size 32 -interface SPIx8 -loadbit {up 0x00000000 "C:/Users/lsfan/Desktop/Axku062Daq/Axku062Daq.runs/impl_1/Axku062Daq.bit" } -force -file "C:/Users/lsfan/Desktop/Axku062Daq/Axku062Daq.runs/impl_1/Axku062Daq.bin"
// save project
// write_project_tcl -force C:/Users/lsfan/Documents/GitHub/SpinalHDL/chainsaw/src/main/resources/VivadoProjects/Axku062Daq/<project name>.tcl
// # binding clock domains
// set_property CONFIG.CLK_DOMAIN Peripheral_PCIe_0_axi_aclk [get_bd_intf_pins /Datapath/controlIn]
// set_property CONFIG.FREQ_HZ 125000000 [get_bd_intf_pins /Datapath/controlIn]
// set_property CONFIG.CLK_DOMAIN Peripheral_util_ds_buf_0_0_IBUF_DS_ODIV2 [get_bd_intf_pins /Datapath/dataIn]
// set_property CONFIG.FREQ_HZ 250000000 [get_bd_intf_pins /Datapath/dataIn]
// set_property CONFIG.CLK_DOMAIN Peripheral_util_ds_buf_0_0_IBUF_DS_ODIV2 [get_bd_intf_pins /Datapath/dataOut]
// set_property CONFIG.FREQ_HZ 250000000 [get_bd_intf_pins /Datapath/dataOut]
import scala.language.postfixOps

case class Axku062Daq() extends Axku062 {

  // pins configuration
  val ddr4 = Ddr4Interface(addrWidth = 17, dataWidthInByte = 8)
  val SMA_CLK_P, SMA_CLK_N = Bool()
  // avoid "not driven" error

  // board connection
  val fl1010 = Fl1010(fmc_lpc_2)
  val mysoowFmc = MysoowFmc(fmc_hpc)

  fmc_hpc.DP_C2M_P.setAsDirectionLess() // disable unused output
  fmc_hpc.DP_C2M_N.setAsDirectionLess()

  val peripheral = new Peripheral_wrapper()
  peripheral.sys_clk_200M := defaultClockDomain.clock

  // PCIe
  peripheral.pcie_rstn := pcie.perst
  peripheral.pcie_clk_clk_n := pcie.clk_n
  peripheral.pcie_clk_clk_p := pcie.clk_p

  Seq(pcie.tx_n, pcie.tx_p, pcie.rx_n, pcie.rx_p).foreach(_.setWidth(4))

  peripheral.pcie_mgt_rxn := pcie.rx_n
  peripheral.pcie_mgt_rxp := pcie.rx_p
  pcie.tx_n := peripheral.pcie_mgt_txn
  pcie.tx_p := peripheral.pcie_mgt_txp

  // DDR4
  peripheral.ddr4_rst := False // disabled
  peripheral.ddr4_rtl <> ddr4

  // HMC7044
  mysoowFmc.hmc7044_sclk := peripheral.hmc7044_sclk
  mysoowFmc.hmc7044_slen := peripheral.hmc7044_slen
  mysoowFmc.hmc7044_sdio <> peripheral.hmc7044_sdio
  mysoowFmc.hmc7044_rstn := peripheral.hmc7044Resetn
  mysoowFmc.hmc7044_sync := False // disabled

  // AD9695
  mysoowFmc.adc1_sclk := peripheral.ad9695_sclk
  mysoowFmc.adc1_csn := peripheral.ad9695_slen
  mysoowFmc.adc1_sdio <> peripheral.ad9695_sdio
  mysoowFmc.adc1_powerdown := peripheral.ad9695PowerDown

  // JESD204
  val adc_core_clk = IBUFDS.Lvds2Clk(mysoowFmc.adc1_core_clk_p, mysoowFmc.adc1_core_clk_n)
  val adc_sysref = IBUFDS.Lvds2Clk(mysoowFmc.adc1_sysref_p, mysoowFmc.adc1_sysref_n)
  val (adcSyncP, adcSyncN) = OBUFDS.Clk2Lvds(peripheral.jesd204_rx_sync)

  peripheral.jesd204_rx_reset := peripheral.jesd204Reset
  peripheral.jesd204_refclk_clk_p := mysoowFmc.adc1_mgt_clk_p
  peripheral.jesd204_refclk_clk_n := mysoowFmc.adc1_mgt_clk_n
  peripheral.jesd204_rx_sysref := adc_sysref
  peripheral.jesd204_rxp := mysoowFmc.adc1_data_p
  peripheral.jesd204_rxn := mysoowFmc.adc1_data_n
  mysoowFmc.adc1_sync_p := adcSyncP
  mysoowFmc.adc1_sync_n := adcSyncN

  // pulse generation
  fl1010.J2_P.head.asOutput() := peripheral.pulse_gen_0
  fl1010.J2_N.head.asOutput() := False
  fl1010.J2_P.last.asOutput() := peripheral.pulse_gen_1
  fl1010.J2_N.last.asOutput() := False

  // DEBUG
  val debugClockingArea = new ClockingArea(defaultClockDomain) {
    val divider_factor = 10
    val divider = CounterFreeRun(divider_factor)
    fl1010.J2_P(1).asOutput() := RegNext(divider.value < (divider_factor / 2))
    fl1010.J2_N(1).asOutput() := RegNext(divider.value >= (divider_factor / 2))

    // HMC7044 output
    fl1010.J2_P(2).asOutput() := adc_sysref
    fl1010.J2_N(2).asOutput() := False
    fl1010.J2_P(3).asOutput() := adc_core_clk
    fl1010.J2_N(3).asOutput() := False

    // LEDs
    led.assignDontCare()
    led(0) := peripheral.pcie_link_up
    led(1) := peripheral.ddr4_init_done

    led_test.clearAll()
  }

}
