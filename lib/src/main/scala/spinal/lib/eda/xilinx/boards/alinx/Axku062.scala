package spinal.lib.eda.xilinx.boards.alinx

import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.xilinx.ultrascale.IBUFDS
import spinal.lib.com.pcie._
import spinal.lib.com.uart._
import spinal.lib.eda.xilinx._

import java.io.File

/** ALINX Axku062 development board
  *
  * @see
  *   [[https://alinx.com/detail/568]] for sales information and manual
  */
abstract class Axku062 extends Component with XilinxBoard {

  println("latest version!")

  // pins with fixed direction
  lazy val sys_clk_p, sys_clk_n, rst_n = in Bool ()
  lazy val user_key_n = in Bool ()
  lazy val pcie = slave(new Pcie(8)) // PCIe
  lazy val uart = master(Uart()) // UART
  lazy val led_test = out Bits (2 bits)
  lazy val led = out Bits (4 bits)

  // pins without fixed direction
  lazy val sma_clk_p, sma_clk_n = inout Bool ()

  // TODO: fmc definition

  //  lazy val FMC1_LPC, FMC2_LPC       = FmcLpc() // FMC-LPC
  //  lazy val SMA_CLKIN_P, SMA_CLKIN_N = Bool()   // SMA
  //
  //  // enable pins
  //  def useFmc1(asMaster: Boolean): Unit = { // Fmc1 of AXKU401 has no clock I/O
  //    if (asMaster) FMC1_LPC.asMaster() else FMC1_LPC.asSlave()
  //  }
  //  def useFmc2(asMaster: Boolean, dataOnly: Boolean): Unit = {
  //    if (asMaster) FMC2_LPC.asMaster() else FMC2_LPC.asSlave()
  //    if (!dataOnly) FMC2_LPC.asCarrier()
  //  }

  // board definition
  override val xdcFile: File = new File("Axku062.xdc")

  override val device: XilinxDevice =
    new XilinxDevice(family = UltraScale, part = "XCKU060-FFVA1156-2-i".toLowerCase(), fMax = 200 MHz)

  override lazy val defaultClockDomain: ClockDomain = {
    // LVDS CLK -> single ended clk
    val clk = IBUFDS.Lvds2Clk(sys_clk_p, sys_clk_n) // FIXME: but isn't accessible in the null component.
    val clockDomainConfig: ClockDomainConfig =
      ClockDomainConfig(clockEdge = RISING, resetKind = ASYNC, resetActiveLevel = LOW)
    new ClockDomain(clock = clk, reset = rst_n, config = clockDomainConfig, frequency = FixedFrequency(device.fMax))
  }

}
