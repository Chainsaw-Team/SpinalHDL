package spinal.lib.eda.xilinx.boards.alinx

import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.xilinx.ultrascale.IBUFDS
import spinal.lib.com.fmc._
import spinal.lib.com.pcie._
import spinal.lib.com.uart._
import spinal.lib.eda.xilinx._

import java.io.File
import scala.language.postfixOps

/** ALINX Z7p development board
  */
abstract class Z7p extends Component with XilinxBoard {

  // pins with fixed direction
  lazy val sys_clk_p, sys_clk_n, rst_n = in Bool ()
  lazy val pcie = slave(new Pcie(8)) // PCIe

  // board definition
  override val xdcFile: File = new File("Z7p.xdc")

  override val device: XilinxDevice =
    new XilinxDevice(family = UltraScale, part = "XCZU7EV-FFVC1156-2-I".toLowerCase(), fMax = 200 MHz)

  override lazy val defaultClockDomain: ClockDomain = {
    // LVDS CLK -> single ended clk
    val clk = IBUFDS.Lvds2Clk(sys_clk_p, sys_clk_n)
    val clockDomainConfig: ClockDomainConfig =
      ClockDomainConfig(clockEdge = RISING, resetKind = BOOT, resetActiveLevel = LOW)
    new ClockDomain(clock = clk, config = clockDomainConfig, frequency = FixedFrequency(device.fMax))
  }

}
