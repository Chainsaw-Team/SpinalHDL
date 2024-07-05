package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.xilinx.ultrascale.{IBUFDS, OBUFDS}
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.bus.regif.AccessType._
import spinal.lib.bus.regif._
import spinal.lib.eda.xilinx.boards.alinx.Axku062

import scala.language.postfixOps

// FIXME: when output set by asOutput is not driven, elaboration won't failed, this may lead to bad output pins

case class DaqTop() extends Axku062 {

  val blockDesign = new XDMA_wrapper() // XDMA block design wrapper

  //////////////
  // connecting BD with top level I/O
  //////////////

  // XMDA <-> PCIe
  blockDesign.xdma_clk_clk_n := pcie.clk_n
  blockDesign.xdma_clk_clk_p := pcie.clk_p
  blockDesign.xdma_rstn := pcie.perst

  blockDesign.pcie_mgt_0_rxn := pcie.rx_n
  blockDesign.pcie_mgt_0_rxp := pcie.rx_p
  pcie.tx_n := blockDesign.pcie_mgt_0_txn
  pcie.tx_p := blockDesign.pcie_mgt_0_txp

  // DDR4 IP <-> DDR4 Pins
  val ddr4 = Ddr4Interface()
  blockDesign.ddr4_rtl_0_act_n <> ddr4.act_n
  blockDesign.ddr4_rtl_0_adr <> ddr4.adr
  blockDesign.ddr4_rtl_0_ba <> ddr4.ba
  blockDesign.ddr4_rtl_0_bg <> ddr4.bg
  blockDesign.ddr4_rtl_0_ck_c <> ddr4.ck_c
  blockDesign.ddr4_rtl_0_ck_t <> ddr4.ck_t
  blockDesign.ddr4_rtl_0_cke <> ddr4.cke
  blockDesign.ddr4_rtl_0_cs_n <> ddr4.cs_n
  blockDesign.ddr4_rtl_0_dm_n <> ddr4.dm_n
  blockDesign.ddr4_rtl_0_dq <> ddr4.dq
  blockDesign.ddr4_rtl_0_dqs_c <> ddr4.dqs_c
  blockDesign.ddr4_rtl_0_dqs_t <> ddr4.dqs_t
  blockDesign.ddr4_rtl_0_odt <> ddr4.odt
  blockDesign.ddr4_rtl_0_reset_n <> ddr4.reset_n

  blockDesign.ddr4_clk_200M := defaultClockDomain.clock
  blockDesign.ddr4_rst := False

  // avoid "not driven" error
  fmc_hpc.DP_C2M_P.setAsDirectionLess() // disable unused output
  fmc_hpc.DP_C2M_N.setAsDirectionLess()
  fmc_lpc_2.CLK_M2C_P.setAsDirectionLess()
  fmc_lpc_2.CLK_M2C_N.setAsDirectionLess()

  // FMC HPC <-> JESD204B

  //////////////
  // FMC HPC <->  HMC7044
  //////////////
  // basic
  val hmc7044Resetn = Bool()
  fmc_hpc.LA_N(7).asOutput() := hmc7044Resetn // HMC7044 reset
  fmc_hpc.LA_P(15).asOutput() := False // HMC7044 sync,disabled
  // HMC7044 SPI interface
  fmc_hpc.LA_P(7).asOutput() := blockDesign.hmc7044_sclk
  fmc_hpc.LA_P(9).asOutput() := blockDesign.hmc7044_slen
  fmc_hpc.LA_N(9).asInOut() <> blockDesign.hmc7044_sdata
  // HMC7044 GPIO
  val hmc7044Gpio4 = fmc_hpc.LA_P(11).asInput() // HMC7044 GPIO4
  val hmc7044Gpio3 = fmc_hpc.LA_P(12).asInput() // HMC7044 GPIO3

  // clocks from HMC7044

  //////////////
  // FMC HPC <->  AD9695
  //////////////
  // AD9695 SPI interface
  fmc_hpc.LA_N(5).asOutput() := blockDesign.ad9695_sclk
  fmc_hpc.LA_N(4).asOutput() := blockDesign.ad9695_csb
  fmc_hpc.LA_P(4).asInOut() := blockDesign.ad9695_sdio
  val ad9695PowerDown = Bool()
  fmc_hpc.LA_P(5).asOutput() := ad9695PowerDown // AD9695 power down, disabled
  // AD9695 GPIO
  val adcGpioA0 = fmc_hpc.LA_P(2).asInput() // ADC FD_A/GPIO_A0
  val adcGpioB0 = fmc_hpc.LA_N(2).asInput() // ADC FD_B/GPIO_B0

  blockDesign.ad9695_gpio_a0 := adcGpioA0
  blockDesign.ad9695_gpio_b0 := adcGpioB0
  blockDesign.hmc7044_gpio3 := hmc7044Gpio3
  blockDesign.hmc7044_gpio4 := hmc7044Gpio4

  //////////////
  // FMC HPC <-> JESD204B
  //////////////
  val adcMgtClkP = fmc_hpc.GBTCLK_M2C_P(0)
  val adcMgtClkN = fmc_hpc.GBTCLK_M2C_N(0)
  val adcCoreClkP = fmc_hpc.LA_P(1).asInput()
  val adcCoreClkN = fmc_hpc.LA_N(1).asInput()
  val adcSysrefClkP = fmc_hpc.LA_P(0).asInput()
  val adcSysrefClkN = fmc_hpc.LA_N(0).asInput()
  val adcSysrefClk = IBUFDS.Lvds2Clk(adcSysrefClkP, adcSysrefClkN) // to JESD204B RX SYSREF

  val (adcSyncP, adcSyncN) = OBUFDS.Clk2Lvds(blockDesign.jesd204_rx_sync)

  blockDesign.jesd204_refclk_n := adcMgtClkN
  blockDesign.jesd204_refclk_p := adcMgtClkP
  blockDesign.jesd204_glblclk_n := adcCoreClkN
  blockDesign.jesd204_glblclk_p := adcCoreClkP
  blockDesign.jesd204_rx_sysref := adcSysrefClk
  fmc_hpc.LA_P(3).asOutput() := adcSyncP
  fmc_hpc.LA_N(3).asOutput() := adcSyncN
  blockDesign.jesd204_rxp := fmc_hpc.DP_M2C_P(3 downto 0)
  blockDesign.jesd204_rxn := fmc_hpc.DP_M2C_N(3 downto 0)

  // indicating status via LEDs
  led.clearAll()
  led(0) := blockDesign.pcie_done // indicating PCIe status
  led(1) := blockDesign.ddr4_done // indicating DDR4 status
  led(2) := hmc7044Gpio4 // indicating HMC7044 status

  // Data Clock Domain - 250MHz data clock domain generated by JESD204 IP
  val dataClockDomain = new ClockDomain(
    clock = blockDesign.jesd204_rx_clk,
    reset = blockDesign.jesd204_rx_rstn,
    config = ClockDomainConfig(resetKind = ASYNC, resetActiveLevel = LOW), // TODO: ASYNC or SYNC?
    frequency = FixedFrequency(250 MHz)
  )

  // for XMDA-Stream verification
  val defaultClockingArea = new ClockingArea(defaultClockDomain) {
    // User register file
    val user_base_addr = 0x0
    val controlRegIf = AxiLite4BusInterface(
      blockDesign.M_AXI_LITE_0,
      SizeMapping(user_base_addr, 0x1000)
    ) // FIXME: base addr has no effect on the generated RTL
    val regReset = controlRegIf.newRegAt(0x00 + user_base_addr, "system control")
    val regTestMode = controlRegIf.newRegAt(0x04 + user_base_addr, "system control")
    val regPostSampleLength = controlRegIf.newRegAt(0x08 + user_base_addr, "sample length after a trigger")
    val regPulsePeriodLength =
      controlRegIf.newRegAt(0x0c + user_base_addr, "frequency of user clockDomain / frequency of pulse generated")
    val regPulseWidth =
      controlRegIf.newRegAt(0x10 + user_base_addr, "frequency of user clockDomain / frequency of pulse generated")

    // reset triggers
    val pllReset = regReset.field(Bool(), RW, 1, "HMC7044 device reset")
    val adReset = regReset.field(Bool(), RW, 1, "AD9695 power down")
    val jesd204DeviceReset = regReset.field(Bool(), RW, 1, "JESD204 system reset, including core logic and transceiver")
    val jesd204AxiReset =
      regReset.field(Bool(), RW, 0, "JESD204 axi-lite interface reset, reset configuration registers")
    val axiDmaReset = regReset.field(Bool(), RW, 0, "AXI DMA system reset")

    // test setting
    val testModeOn = regTestMode.field(Bool(), RW, 0, "sending known pattern instead of data from ADC")

    val postSampleLength = regPostSampleLength.field(UInt(32 bits), RW, 125000)
    val pulsePeriodLength = regPulsePeriodLength.field(UInt(32 bits), RW, 125000)
    val pulseWidthLength = regPulseWidth.field(UInt(32 bits), RW, 25)

    controlRegIf.accept(HtmlGenerator("regif", "AP"))
    controlRegIf.accept(CHeaderGenerator("header", "AP"))

    def createReset(trigger: Bool, polarity: Polarity, cycles: Int) = {
      assert(cycles >= 2)
      val timeout = new Timeout(cycles, init = True) // initial state = do not reset
      when(trigger.rise(True))(timeout.clear())
      when(timeout.rise(False))(trigger.clear())
      polarity match {
        case HIGH => !timeout.implicitValue
        case LOW  => timeout.implicitValue
      }
    }

    hmc7044Resetn := RegNext(!pllReset, init = False)
    ad9695PowerDown := RegNext(adReset, init = True)
    blockDesign.jesd204_rx_reset := RegNext(jesd204DeviceReset, init = True)
    blockDesign.jesd204_aresetn := !jesd204AxiReset
    blockDesign.axi_dma_resetn := axiDmaReset

    // debug
//    blockDesign.hmc7044_resetn := hmc7044Resetn
//    blockDesign.ad9695PowerDown := ad9695PowerDown
    blockDesign.reset_probe := hmc7044Resetn
  }

  def getControlData[T <: Data](ctrl: T) = {
    ctrl.addTag(crossClockDomain)
    Delay(ctrl, 3)
  }

  dataClockDomain on {

    // pulse generation
    val pulseGenCounter = CounterFreeRun(250000) // lower bound = 1KHz
    when(pulseGenCounter.value === getControlData(defaultClockingArea.pulsePeriodLength))(pulseGenCounter.clear())
    val pulse0 = pulseGenCounter.value < getControlData(defaultClockingArea.pulseWidthLength)
    val pulse1 = Delay(pulse0, 25, init = False)
//    fmc_lpc_2.LA_P.foreach(pin => pin.asOutput() := pulse0)
//    fmc_lpc_2.LA_N.foreach(pin => pin.asOutput() := False)

    // data mapping
    val rx_data = blockDesign.jesd204_rx_data_tdata
    val ch1Data1 = rx_data(7 downto 0) ## rx_data(39 downto 32)
    val ch1Data2 = rx_data(15 downto 8) ## rx_data(47 downto 40)
    val ch1Data3 = rx_data(23 downto 16) ## rx_data(55 downto 48)
    val ch1Data4 = rx_data(31 downto 24) ## rx_data(63 downto 56)

    val ch2Data1 = rx_data(71 downto 64) ## rx_data(103 downto 96)
    val ch2Data2 = rx_data(79 downto 72) ## rx_data(111 downto 104)
    val ch2Data3 = rx_data(87 downto 80) ## rx_data(119 downto 112)
    val ch2Data4 = rx_data(95 downto 88) ## rx_data(127 downto 120)

    val rx_valid = blockDesign.jesd204_rx_data_tvalid
    val segments = Seq(ch1Data1, ch1Data2, ch1Data3, ch1Data4, ch2Data1, ch2Data2, ch2Data3, ch2Data4)

    // free-run counter
    val counter = Counter(1 << 13, inc = blockDesign.S_AXIS_0.fire)

    when(getControlData(defaultClockingArea.testModeOn)) {
      (0 until 8).foreach(i =>
        blockDesign.S_AXIS_0.data(i * 16, 16 bits) := RegNext((counter.value @@ U(i, 3 bits)).asBits)
      )
      blockDesign.S_AXIS_0.valid.set()
    }.otherwise { // JESD204 -> AXI DMA
      segments.zipWithIndex.foreach { case (segment, i) =>
        blockDesign.S_AXIS_0.data(i * 16, 16 bits) := segment
      }
      blockDesign.S_AXIS_0.valid := rx_valid
    }
    blockDesign.S_AXIS_0.last := counter.willOverflow
    blockDesign.S_AXIS_0.keep.setAll()

    // debug
//    blockDesign.ch1Data1 := ch1Data1(15 downto 2)
//    blockDesign.ch2Data1 := ch2Data1(15 downto 2)
//    blockDesign.rx_valid := rx_valid

  }

}
