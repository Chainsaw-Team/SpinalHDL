package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.xilinx.ultrascale.IBUFDS
import spinal.lib.com.fmc._
import spinal.lib.com.pcie._
import spinal.lib.com.uart._
import spinal.lib.eda.xilinx._

import java.io.File
import scala.language.postfixOps

abstract class Axku5 extends Component with XilinxBoard {

  // pins with fixed direction
  lazy val sys_clk_p, sys_clk_n = in Bool ()
  lazy val ddr4_clk_p, ddr4_clk_n = in Bool ()
  lazy val user_key_n = in Bits(4 bits)
  lazy val pcie = slave(new Pcie(8)) // PCIe
  lazy val uart = master(Uart()) // UART
  lazy val led = out Bits (4 bits)
  lazy val ddr4 = Ddr4Interface(17, 4)

  // TODO: ethernet interface, MIPI interface, micro SD
  // TODO: 40pin interface

  // pins without fixed direction
  lazy val user_40pin = Alinx40Pin()

  lazy val fmc_hpc: Fmc =
    master(new Fmc(FmcConfig(is_hpc = true, gigabitWidth = 8, useLa = true, useI2c = true))) // FMC-HPC


  // board definition
  override val xdcFile: File = new File("Axku5.xdc")

  override val device: XilinxDevice =
    new XilinxDevice(family = UltraScale, part = "XCKU5P-FFVB676-2-i".toLowerCase(), fMax = 400 MHz)

  override lazy val defaultClockDomain: ClockDomain = {
    // LVDS CLK -> single ended clk
    val clk = IBUFDS.Lvds2Clk(sys_clk_p, sys_clk_n)
    val clockDomainConfig: ClockDomainConfig =
      ClockDomainConfig(clockEdge = RISING, resetKind = BOOT, resetActiveLevel = LOW)
    new ClockDomain(clock = clk, config = clockDomainConfig, frequency = FixedFrequency(200 MHz))
  }

  lazy val ddrClockDomain: ClockDomain = {
    val clk = IBUFDS.Lvds2Clk(ddr4_clk_p, ddr4_clk_n)
    val clockDomainConfig: ClockDomainConfig =
      ClockDomainConfig(clockEdge = RISING, resetKind = BOOT, resetActiveLevel = LOW)
    new ClockDomain(clock = clk, config = clockDomainConfig, frequency = FixedFrequency(200 MHz))
  }

}
