package chainsaw.projects.xdma
import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._
import spinal.lib.bus.amba4.axis._
import spinal.lib.bus.amba4.axilite._

import scala.language.postfixOps

class Xdma_wrapper extends BlackBox {
  val diff_clock_rtl_0_clk_n = in Bool()
  val diff_clock_rtl_0_clk_p = in Bool()
  val pcie_7x_mgt_rtl_0_rxn = in Bits(8 bits)
  val pcie_7x_mgt_rtl_0_rxp = in Bits(8 bits)
  val pcie_7x_mgt_rtl_0_txn = out Bits(8 bits)
  val pcie_7x_mgt_rtl_0_txp = out Bits(8 bits)
  val reset_rtl_0 = in Bool()
  val user_clk_heartbeat = out Bool()
  val user_lnk_up = out Bool()
  val user_resetn = out Bool()

  addRTLPath(raw"C:\Users\lsfan\Desktop\ChainsawXdma\ChainsawXdma.srcs\sources_1\bd\Xdma\hdl\Xdma_wrapper.v")
}
