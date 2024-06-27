package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.xilinx.ultrascale.{IBUFDS, OBUFDS}
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.bus.regif.AccessType._
import spinal.lib.bus.regif._
import spinal.lib.eda.xilinx.boards.alinx.Axku062
import spinal.lib.blackbox.xilinx.s7.IBUF
import spinal.lib.bus.amba4.axilite.AxiLite4

import scala.language.postfixOps

// FIXME: when output set by asOutput is not driven, elaboration won't failed, this may lead to bad output pins
// FIXME: base addr has no effect on the generated RTL, when base addr is not 0, you should set register address starting from base address

case class ChainsawDaq() extends Axku062 {

  // extra pins
  val ddr4 = Ddr4Interface() // TODO: move to Axku062

  val peripheral = new Peripheral_wrapper()
  peripheral.sys_clk_200M := defaultClockDomain.clock

  // PCIe
  peripheral.pcie_clk_clk_n := pcie.clk_n
  peripheral.pcie_clk_clk_p := pcie.clk_p
  peripheral.pcie_rstn := pcie.perst

  peripheral.pcie_mgt_rxn := pcie.rx_n
  peripheral.pcie_mgt_rxp := pcie.rx_p
  pcie.tx_n := peripheral.pcie_mgt_txn
  pcie.tx_p := peripheral.pcie_mgt_txp

  // DDR4
  peripheral.ddr4_rtl_0 <> ddr4
  peripheral.ddr4_rst := False

  val controlClockingArea = new ClockingArea(defaultClockDomain) {
    val registerSpace = ChainsawDaqRegisterSpace(peripheral.m_axi_lite_user)

    // LEDs for system status
    val pcieHeartBeat = CounterFreeRun(1 << 25)

    led.clearAll()
    led(0) := pcieHeartBeat.msb
    led(1) := peripheral.ddr4_init_done
  }

}

case class ChainsawDaqRegisterSpace(user: AxiLite4) extends Area {

  val user_base_addr = 0x00000
  val userBusIf = AxiLite4BusInterface(user, SizeMapping(user_base_addr, 0x10000))

  val versionReg = userBusIf.newReg("firmware version")
  val revision = versionReg.field(UInt(8 bits), RO)
  val minor = versionReg.field(UInt(8 bits), RO)
  val major = versionReg.field(UInt(8 bits), RO)
  revision := 0x00
  minor := 0x01
  major := 0x00

  val rwTestReg = userBusIf.newReg("reserved RW field for testing AXI4-Lite read/write")
  val rwTest = rwTestReg.field(Bits(32 bits), RW)

  userBusIf.accept(HtmlGenerator("UserRegisterSpace", "AP"))
  userBusIf.accept(CHeaderGenerator("UserRegisterSpace", "AP"))
  userBusIf.accept(PythonHeaderGenerator("UserRegisterSpace", "AP"))

}
