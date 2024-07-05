package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.core.sim.SpinalSimConfig
import spinal.lib._
import spinal.lib.blackbox.xilinx.ultrascale.{IBUFDS, OBUFDS}
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.bus.regif.AccessType._
import spinal.lib.bus.regif._
import spinal.lib.eda.xilinx.boards.alinx.Axku062
import spinal.lib.blackbox.xilinx.s7.IBUF
import spinal.lib.bus.amba4.axilite.AxiLite4
import spinal.lib.bus.amba4.axis.Axi4Stream.Axi4Stream
import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._
import spinal.lib.fsm._
import spinal.lib.bus._

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
  val hmc7044Resetn = fmc_hpc.LA_N(7).asOutput()
  fmc_hpc.LA_P(15).asOutput() := False // HMC7044 sync,disabled
  fmc_hpc.LA_P(7).asOutput() := peripheral.hmc7044_sclk
  fmc_hpc.LA_P(9).asOutput() := peripheral.hmc7044_slen
  fmc_hpc.LA_N(9).asInOut() <> peripheral.hmc7044_sdio
  val hmc7044Gpio4 = fmc_hpc.LA_P(11).asInput()
  val hmc7044Gpio3 = fmc_hpc.LA_P(12).asInput()

  // AD9695 controller
  val ad9695PowerDown = fmc_hpc.LA_P(5).asOutput() // = reset
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

  // clock domains
  val pcieClockDomain =
    ClockDomain(
      clock = peripheral.pcie_user_clk,
      reset = peripheral.pcie_user_rstn,
      config = ClockDomainConfig(clockEdge = RISING, resetKind = SYNC, resetActiveLevel = LOW)
    )

  val dataClockDomain = new ClockDomain(
    clock = peripheral.jesd204_rx_core_clk_out,
    reset = peripheral.jesd204_rx_aresetn,
    config = ClockDomainConfig(resetKind = SYNC, resetActiveLevel = LOW), // TODO: ASYNC or SYNC?
    frequency = FixedFrequency(250 MHz)
  )

  val controlClockingArea = new ClockingArea(pcieClockDomain) {

    // register space
    val registerSpace = ChainsawDaqRegisterSpace(peripheral.pcie_axi_lite_user)

    // reset logic
    hmc7044Resetn := !registerSpace.hmc7044Reset
    ad9695PowerDown := registerSpace.ad9695Reset
    peripheral.jesd204_rx_reset := registerSpace.jesd204Reset

    // LEDs for system status

    led_test.clearAll()
    led_test(0) := peripheral.pcie_interrupt // interrupt, green
//    led_test(1) := !bootClockingArea.bootRstn // booting, red

    led.clearAll()
    led(0) := peripheral.pcie_link_up
    led(1) := peripheral.ddr4_init_done
    led(2) := hmc7044Gpio4
    led(3) := adc9695GpioA0

    // debug
    peripheral.hmc7044_rstn := hmc7044Resetn
    peripheral.ad9695_powerdown := ad9695PowerDown
  }

  val dataClockingArea = new ClockingArea(dataClockDomain) {

    val rxFlow = Flow(cloneOf(peripheral.jesd204_m_axis_data_tdata))
    rxFlow.payload := peripheral.jesd204_m_axis_data_tdata
    rxFlow.valid := peripheral.jesd204_m_axis_data_tvalid
    val datapath = ChainsawDaqDatapath(rxFlow)

    // debug
    peripheral.channel_1_data := datapath.channel1(0)
    peripheral.channel_2_data := datapath.channel2(0)

  }

}

case class ChainsawDaqRegisterSpace(user: AxiLite4) extends Area {

  val user_base_addr = 0x00000
  val userBusIf = AxiLite4BusInterface(user, SizeMapping(user_base_addr, 0x10000))

  val versionReg = userBusIf.newRegAt(user_base_addr, "firmware version")
  val revision = versionReg.field(UInt(8 bits), RO)
  val minor = versionReg.field(UInt(8 bits), RO)
  val major = versionReg.field(UInt(8 bits), RO)
  revision := 0x00
  minor := 0x01
  major := 0x00

  val jesd204ResetReg = userBusIf.newRegAt(user_base_addr + 4, "reset JESD204")
  val jesd204Reset = jesd204ResetReg.field(Bool(), RW, 1, "reset JESD204, asserted by default")
  val hmc7044ResetReg = userBusIf.newRegAt(user_base_addr + 8, "reset HMC7044")
  val hmc7044Reset = hmc7044ResetReg.field(Bool(), RW, 1, "reset HMC7044, asserted by default")
  val ad9695ResetReg = userBusIf.newRegAt(user_base_addr + 12, "reset AD9695")
  val ad9695Reset = ad9695ResetReg.field(Bool(), RW, 1, "reset ad9695, asserted by default")

  val rwTestReg = userBusIf.newRegAt(user_base_addr + 16, "reserved RW field for testing AXI4-Lite read/write")
  val rwTest = rwTestReg.field(Bits(32 bits), RW)

  userBusIf.accept(HtmlGenerator("UserRegisterSpace", "AP"))
  userBusIf.accept(CHeaderGenerator("UserRegisterSpace", "AP"))
  userBusIf.accept(PythonHeaderGenerator("UserRegisterSpace", "AP"))

}

case class ChainsawDaqDatapath(rxData: Flow[Bits]) extends Area {

  def mapper(bitsIn: Bits, base: Int) = {
    (0 until 4).map { i =>
      val baseHigher = base + i * 8
      val baseLower = base + (i + 4) * 8
      val all = bitsIn(baseHigher + 7 downto baseHigher) ## bitsIn(baseLower + 7 downto baseLower)
      val controlBits = all.takeLow(2)
      all.takeHigh(14)
    }
  }

  val channel1 = mapper(rxData.payload, 0)
  val channel2 = mapper(rxData.payload, 64)

}
