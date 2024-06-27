package spinal.lib.eda.xilinx.boards.alinx

import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.xilinx.ultrascale.IBUFDS
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

  // pins with fixed direction
  lazy val sys_clk_p, sys_clk_n, rst_n = in Bool ()
  lazy val user_key_n = in Bool ()
  lazy val pcie = slave(new Pcie(8)) // PCIe
  lazy val uart = master(Uart()) // UART
  lazy val led_test = out Bits (2 bits) // 0 is greed, 1 is read
  lazy val led = out Bits (4 bits)

  // pins without fixed direction
  lazy val sma_clk_p, sma_clk_n = inout(Analog(Bool()))

  lazy val fmc_hpc: Fmc =
    master(new Fmc(FmcConfig(is_hpc = true, gigabitWidth = 8, useLa = true, useI2c = true))) // FMC-HPC
  lazy val fmc_lpc_1 = master(
    new Fmc(FmcConfig(is_hpc = false, gigabitWidth = 1, useLa = true, useI2c = true))
  ) // FMC-LPC1
  lazy val fmc_lpc_2 = master(
    new Fmc(FmcConfig(is_hpc = false, gigabitWidth = 0, useLa = true, useI2c = true))
  ) // FMC-LPC2

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

}
