package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.lib.CounterFreeRun
import spinal.lib.blackbox.xilinx.ultrascale.{IBUFDS, OBUFDS}
import spinal.lib.eda.xilinx.boards.alinx.{Axku062, Fl1010}

import scala.language.postfixOps

case class Axku062Daq() extends Axku062 {

  // board connection
  val fl1010 = Fl1010(fmc_lpc_2) // connect FL1010 with LPC2
  val mysoowFmc = MysoowFmc(fmc_hpc) // connect mysoow ADC FMC with HPC

  fmc_hpc.DP_C2M_P.setAsDirectionLess() // disable unused output
  fmc_hpc.DP_C2M_N.setAsDirectionLess()

  val peripheral = Peripheral_wrapper()
  peripheral.sys_clk_200M := defaultClockDomain.clock

  // PCIe Gen3 X 4 // TODO: redesign as X8
  Seq(pcie.tx_n, pcie.tx_p, pcie.rx_n, pcie.rx_p).foreach(_.setWidth(4))
  peripheral.pcie_rstn := pcie.perst
  peripheral.pcie_clk_clk_n := pcie.clk_n
  peripheral.pcie_clk_clk_p := pcie.clk_p
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
  mysoowFmc.adc1_powerdown := peripheral.ad9695PowerDown
  mysoowFmc.adc1_sclk := peripheral.ad9695_sclk
  mysoowFmc.adc1_csn := peripheral.ad9695_slen
  mysoowFmc.adc1_sdio <> peripheral.ad9695_sdio

  mysoowFmc.ch1_dc_sw := False // AC coupling
  mysoowFmc.ch2_dc_sw := False // AC coupling

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

}

object Axku062Daq {
  def main(args: Array[String]): Unit = {
    SpinalConfig().generateVerilog(Axku062Daq())
  }
}
