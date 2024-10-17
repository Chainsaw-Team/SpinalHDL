package chainsaw.projects.xdma.daq

import spinal.core._
import utils.BlackBoxParser

import java.io.File


// task 1: generate submodules
object GenerateSubModules extends App {

  val config =
    SpinalConfig(
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW, resetKind = SYNC),
      targetDirectory = axku062DaqRtlSource.getAbsolutePath
    )
  config.generateVerilog(AdiSpiCtrl(50)) // sclk frequency = 125MHz / 50 = 1MHz < 2.5MHz < 10MHz
  config.generateVerilog(ChainsawDaqDataPath())

}

// task 2: in Vivado Block Design: 1. add RTL module 2. connect interfaces 3. binding user interfaces with clocks 4. edit address

// binding user interfaces with clocks
// set_property CONFIG.CLK_DOMAIN Peripheral_jesd204_0_rx_core_clk_out [get_bd_intf_pins /ChainsawDaqDataPath_0/dataIn]
// set_property CONFIG.FREQ_HZ 250000000 [get_bd_intf_pins /ChainsawDaqDataPath_0/dataIn]
// set_property CONFIG.CLK_DOMAIN Peripheral_jesd204_0_rx_core_clk_out [get_bd_intf_pins /ChainsawDaqDataPath_0/dataOut]
// set_property CONFIG.FREQ_HZ 250000000 [get_bd_intf_pins /ChainsawDaqDataPath_0/dataOut]
// set_property CONFIG.CLK_DOMAIN Peripheral_xdma_0_0_axi_aclk [get_bd_intf_pins /ChainsawDaqDataPath_0/controlIn]
// set_property CONFIG.FREQ_HZ 250000000 [get_bd_intf_pins /ChainsawDaqDataPath_0/controlIn]

// task 3: create block design wrapper
object BuildBlackBox extends App {
  BlackBoxParser(
    from = new File("c:/Users/lsfan/Desktop/Axku062Daq/Axku062Daq.gen/sources_1/bd/Peripheral/hdl/Peripheral_wrapper.v"),
    to = new File(axku062DaqScalaSource, "Peripheral_wrapper.scala")
  )
}

// task 4: connect block design wrapper with top-level I/O in DasTop.scala

// task 5: generate top-level module
object GenerateTopModule extends App {
  SpinalConfig(targetDirectory = axku062DaqRtlSource.getAbsolutePath)
    .generateVerilog(Axku062Daq())
}

// task 6: synth,impl & bitgen

