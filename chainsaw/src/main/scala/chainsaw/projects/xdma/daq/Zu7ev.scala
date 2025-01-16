package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.lib._
import spinal.lib.blackbox.xilinx.ultrascale.IBUFDS
import spinal.lib.com.ddr.Ddr4Interface
import spinal.lib.com.pcie._
import spinal.lib.com.uart._
import spinal.lib.com.fmc._
import spinal.lib.eda.xilinx._

import spinal.lib.graphic._
import spinal.lib.graphic.vga._

import java.io.File
import scala.language.postfixOps
// import spinal.lib.eda.xilinx.boards.FmcHpc

/** PZ-ZU7EV development board
  *
  * @see
  *   [[\\192.168.2.12\DataServer\05-datasheet\PZ-ZYNQ UltraScalePlus MPSOC ZU7EV-11EG Platform]] for sales information and manual
  */
abstract class Zu7ev extends Component with XilinxBoard {

  lazy val ddr4 = Ddr4Interface(addrWidth = 18, dataWidthInByte = 8)
  lazy val sys_clk_p, sys_clk_n, rst_n = in Bool ()
  lazy val pcie = slave(new Pcie(4)) // PCIe
  // TODO: SFP interface
  // TODO: ethernet
  lazy val uart = master(Uart()) // UART
  lazy val fmc_hpc: Fmc =
    master(new Fmc(FmcConfig(is_hpc = true, gigabitWidth = 8, useLa = true, useI2c = true))) // FMC-HPC
  // TODO: micro SD
  lazy val sma_clk_p, sma_clk_n = inout(Analog(Bool()))
  lazy val vga = master(Vga(RgbConfig(8, 8, 8))) // VGA
  // lazy val user_key_n = in Bool ()
  // lazy val led_test = out Bits (2 bits) // 0 is greed, 1 is read
  lazy val led = out Bits (3 bits)
  lazy val key = in Bits (2 bits)

  // board definition
  override val xdcFile: File = new File("Zu7ev.xdc")

  override val device: XilinxDevice =
    new XilinxDevice(family = UltraScale, part = "xczu7ev-ffvc1156-2-i".toLowerCase(), fMax = 100 MHz)

  override lazy val defaultClockDomain: ClockDomain = {
    val clk = IBUFDS.Lvds2Clk(sys_clk_p, sys_clk_n)

    // LVDS CLK -> single ended clk
    val clockDomainConfig: ClockDomainConfig =
      ClockDomainConfig(clockEdge = RISING, resetKind = BOOT, resetActiveLevel = LOW)
    new ClockDomain(
      clock = clk,
      config = clockDomainConfig,
      frequency = FixedFrequency(device.fMax)
    )
  }

  // extra information
  val ddr4PartNameInVivado = "MT40A512M16LY-075"
  // val flashPartNameInVivado = "mt25ql128-spi-x1_x2_x4_x8" //todo add flash part number

}

case class Zu7evExample() extends Zu7ev {

  val debugClockingArea = new ClockingArea(defaultClockDomain){
    led.assignDontCare()
    led(0) := defaultClockDomain.clock
  }

}

object Zu7evExample extends App {
  SpinalConfig().generateVerilog(Zu7evExample())
}
