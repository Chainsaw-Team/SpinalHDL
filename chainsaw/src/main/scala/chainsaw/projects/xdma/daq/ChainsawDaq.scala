package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.lib.blackbox.xilinx.ultrascale.{IBUFDS, OBUFDS}
import spinal.lib.eda.xilinx.boards.alinx.Axku062

// write_project_tcl -force C:/Users/lsfan/Documents/GitHub/SpinalHDL/chainsaw/src/main/resources/ChainsawDaqSources/CreateChainsawDaq.tcl

import scala.language.postfixOps

// FIXME: when output set by asOutput is not driven, elaboration won't failed, this may lead to bad output pins
// FIXME: base addr has no effect on the generated RTL, when base addr is not 0, you should set register address starting from base address

case class ChainsawDaq() extends Axku062 {

  // pins configuration
  val ddr4 = Ddr4Interface()
  // avoid "not driven" error
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

  // HMC7044 controller
  fmc_hpc.LA_N(7).asOutput() := peripheral.hmc7044Resetn
  fmc_hpc.LA_P(15).asOutput() := False // HMC7044 sync,disabled
  fmc_hpc.LA_P(7).asOutput() := peripheral.hmc7044_sclk
  fmc_hpc.LA_P(9).asOutput() := peripheral.hmc7044_slen
  fmc_hpc.LA_N(9).asInOut() <> peripheral.hmc7044_sdio
  val hmc7044Gpio4 = fmc_hpc.LA_P(11).asInput()
  val hmc7044Gpio3 = fmc_hpc.LA_P(12).asInput()

  // AD9695 controller
  fmc_hpc.LA_P(5).asOutput() := peripheral.ad9695PowerDown
  fmc_hpc.LA_N(5).asOutput() := peripheral.ad9695_sclk
  fmc_hpc.LA_N(4).asOutput() := peripheral.ad9695_slen
  fmc_hpc.LA_P(4).asInOut() := peripheral.ad9695_sdio
  val adc9695GpioA0 = fmc_hpc.LA_P(2).asInput()
  val adc9695GpioB0 = fmc_hpc.LA_N(2).asInput()

  // JESD204
  peripheral.jesd204_refclk_n := fmc_hpc.GBTCLK_M2C_N(0)
  peripheral.jesd204_refclk_p := fmc_hpc.GBTCLK_M2C_P(0)
  peripheral.jesd204_rx_sysref := IBUFDS.Lvds2Clk(fmc_hpc.LA_P(0).asInput(), fmc_hpc.LA_N(0).asInput())
  peripheral.jesd204_rxp := fmc_hpc.DP_M2C_P(3 downto 0)
  peripheral.jesd204_rxn := fmc_hpc.DP_M2C_N(3 downto 0)
  val (adcSyncP, adcSyncN) = OBUFDS.Clk2Lvds(peripheral.jesd204_rx_sync)
  fmc_hpc.LA_P(3).asOutput() := adcSyncP
  fmc_hpc.LA_N(3).asOutput() := adcSyncN

  // SMA output

}
