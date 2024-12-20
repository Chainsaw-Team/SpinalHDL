package spinal.lib.eda.xilinx.boards.alinx

import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.xilinx.ultrascale.IBUFDS
import spinal.lib.com.ddr.Ddr4Interface
import spinal.lib.com.pcie._
import spinal.lib.com.uart._
import spinal.lib.com.fmc._
import spinal.lib.eda.xilinx._

import java.io.File
import scala.language.postfixOps

/** ALINX Axku062 development board
  *
  * @see
  *   [[https://alinx.com/detail/568]] for sales information and manual
  */
abstract class Axku062 extends Component with XilinxBoard {

  lazy val ddr4 = Ddr4Interface(addrWidth = 17, dataWidthInByte = 8)
  lazy val sys_clk_p, sys_clk_n, rst_n = in Bool ()
  lazy val pcie = slave(new Pcie(8)) // PCIe
  // TODO: SFP interface
  // TODO: ethernet
  lazy val uart = master(Uart()) // UART
  lazy val fmc_hpc: Fmc =
    master(new Fmc(FmcConfig(is_hpc = true, gigabitWidth = 8, useLa = true, useI2c = true))) // FMC-HPC
  lazy val fmc_lpc_1 = master(
    new Fmc(FmcConfig(is_hpc = false, gigabitWidth = 1, useLa = true, useI2c = true))
  ) // FMC-LPC1
  lazy val fmc_lpc_2 = master(
    new Fmc(FmcConfig(is_hpc = false, gigabitWidth = 0, useLa = true, useI2c = true))
  ) // FMC-LPC2
  // TODO: micro SD
  lazy val sma_clk_p, sma_clk_n = inout(Analog(Bool()))
  // TODO: I2C for EEPROM and temp sensor
  lazy val user_key_n = in Bool ()
  lazy val led_test = out Bits (2 bits) // 0 is greed, 1 is read
  lazy val led = out Bits (4 bits)

  // board definition
  override val xdcFile: File = new File("Axku062.xdc")

  override val device: XilinxDevice =
    new XilinxDevice(family = UltraScale, part = "XCKU060-FFVA1156-2-i".toLowerCase(), fMax = 200 MHz)

  override lazy val defaultClockDomain: ClockDomain = {
    // LVDS CLK -> single ended clk
    val clk = IBUFDS.Lvds2Clk(sys_clk_p, sys_clk_n)
    val clockDomainConfig: ClockDomainConfig =
      ClockDomainConfig(clockEdge = RISING, resetKind = BOOT, resetActiveLevel = LOW)
    new ClockDomain(clock = clk, config = clockDomainConfig, frequency = FixedFrequency(device.fMax))
  }

  // extra information
  val ddr4PartNameInVivado = "MT40A512M16HA-083E"
  val flashPartNameInVivado = "mt25ql128-spi-x1_x2_x4_x8"

}
