package chainsaw.projects.xdma.daq
import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._
import spinal.lib.bus.amba4.axis._
import spinal.lib.bus.amba4.axilite._
import spinal.lib.com.ddr.Ddr4Interface

import scala.language.postfixOps

class Axku5Peripheral_wrapper extends BlackBox {

  val ddr4_rtl = Ddr4Interface(17, 4)
  val ad9695PowerDown = out Bool()
  val ad9695_sclk = out Bool()
  val ad9695_sdio = inout(Analog(Bool()))
  val ad9695_slen = out Bool()
  val ddr4_init_done = out Bool()
  val ddr4_ref_clk_clk_n = in Bool()
  val ddr4_ref_clk_clk_p = in Bool()
  val ddr4_rst = in Bool()
  val hmc7044Resetn = out Bool()
  val hmc7044_sclk = out Bool()
  val hmc7044_sdio = inout(Analog(Bool()))
  val hmc7044_slen = out Bool()
  val jesd204Reset = out Bool()
  val jesd204_drpclk = in Bool()
  val jesd204_refclk_clk_n = in Bool()
  val jesd204_refclk_clk_p = in Bool()
  val jesd204_rx_reset = in Bool()
  val jesd204_rx_sync = out Bool()
  val jesd204_rx_sysref = in Bool()
  val jesd204_rxn = in Bits(4 bits)
  val jesd204_rxp = in Bits(4 bits)
  val pcie_clk_clk_n = in Bool()
  val pcie_clk_clk_p = in Bool()
  val pcie_link_up = out Bool()
  val pcie_mgt_rxn = in Bits(4 bits)
  val pcie_mgt_rxp = in Bits(4 bits)
  val pcie_mgt_txn = out Bits(4 bits)
  val pcie_mgt_txp = out Bits(4 bits)
  val pcie_rstn = in Bool()
  val pulse_gen_0 = out Bool()
  val pulse_gen_1 = out Bool()

  addRTLPath(raw"C:\Users\lsfan\Documents\GitHub\SpinalHDL\.\Axku5Daq\Axku5Daq\Axku5Daq.gen\sources_1\bd\Axku5Peripheral\hdl\Axku5Peripheral_wrapper.v")
}
