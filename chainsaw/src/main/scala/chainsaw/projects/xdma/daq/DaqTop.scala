package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._
import spinal.lib.fsm._
import spinal.lib.bus._
import spinal.lib.eda.xilinx.boards.alinx.Axku062
import spinal.lib.eda.xilinx.VivadoFlow

import scala.language.postfixOps

// generate memory configuration file
// write_cfgmem  -format bin -size 32 -interface SPIx8 -loadbit {up 0x00000000 "C:/Users/ltr/Desktop/XdmaDaq/test_bandwidth/DaqTestXdma.bit" } -file "C:/Users/ltr/Desktop/XdmaDaq/test_bandwidth/DaqXdma.bin"

class Axku062Xdma extends Axku062 {

  val xdma = new XDMA_wrapper() // XDMA block design wrapper

  // XMDA <-> PCIe
  xdma.CLK_IN_D_0_clk_n := pcie.clk_n
  xdma.CLK_IN_D_0_clk_p := pcie.clk_p
  xdma.pcie_mgt_0_rxn := pcie.rx_n
  xdma.pcie_mgt_0_rxp := pcie.rx_p
  pcie.tx_n := xdma.pcie_mgt_0_txn
  pcie.tx_p := xdma.pcie_mgt_0_txp
  xdma.sys_rst_n_0 := pcie.perst

  // indicating link up by led
  led_test.asBools.foreach(_ := xdma.user_lnk_up_0)

  // XDMA <-> User Logic by AXI4-Stream
  val axi_clk = xdma.axi_aclk_0
  val axi_rstn = xdma.axi_aresetn_0
  val clockDomainConfig: ClockDomainConfig =
    ClockDomainConfig(clockEdge = RISING, resetKind = SYNC, resetActiveLevel = LOW)
  val userClockDomain = new ClockDomain(
    clock = axi_clk,
    reset = axi_rstn,
    config = clockDomainConfig,
    frequency = FixedFrequency(250 MHz)
  )

}

case class DaqTestXdma() extends Axku062Xdma {

  userClockDomain on {

    // user logic: a counter keep feeding XDMA with incremental digits
    val counter = Counter(256, inc = xdma.S_AXIS_C2H_0_0_tready & xdma.S_AXIS_C2H_0_0_tvalid)
    (0 until 32).foreach(i =>
      xdma.S_AXIS_C2H_0_0_tdata(i * 8, 8 bits) := counter.value.asBits
    ) // feed every byte with same digit
    xdma.S_AXIS_C2H_0_0_tkeep.setAll() // always keep
//    xdma.S_AXIS_C2H_0_0_tlast.clear() // no last
    xdma.S_AXIS_C2H_0_0_tlast := counter.willOverflow // packet boundary
    xdma.S_AXIS_C2H_0_0_tvalid.set() // always valid

    counter.value.addAttribute("mark_debug", "true")

  }

  // debug
  val ready = Bool()
  ready := xdma.S_AXIS_C2H_0_0_tready
  ready.addAttribute("mark_debug", "true")
  val valid = Bool()
  valid := xdma.S_AXIS_C2H_0_0_tvalid
  valid.addAttribute("mark_debug", "true")
  val fire = valid & ready
  fire.addAttribute("mark_debug", "true")

  checkXdc()

}

case class DaqTop() extends Axku062Xdma {

  // PING-PONG buffer for AXI4-MM interface

  userClockDomain on {

    // user logic: a counter keep feeding XDMA with incremental digits
    val counter = Counter(256, inc = xdma.S_AXIS_C2H_0_0_tready & xdma.S_AXIS_C2H_0_0_tvalid)
    (0 until 32).foreach(i =>
      xdma.S_AXIS_C2H_0_0_tdata(i * 8, 8 bits) := counter.value.asBits
    ) // feed every byte with same digit
    xdma.S_AXIS_C2H_0_0_tkeep.setAll() // always keep
    xdma.S_AXIS_C2H_0_0_tlast.clear() // no last
    xdma.S_AXIS_C2H_0_0_tvalid.set() // always valid
  }

  checkXdc()

}

//object BuildDaq extends App{
//
//  VivadoFlow(vivadoPath = ???, workspacePath = ???, rtl = ???, family = ???, device = ???, frequencyTarget = ???, processorCount = ???)
//
//  SpinalVerilog(DaqTestXdma())
//
//
//}
