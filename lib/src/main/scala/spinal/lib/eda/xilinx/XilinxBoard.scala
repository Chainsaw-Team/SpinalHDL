package spinal.lib.eda.xilinx

import spinal.core._

import java.io.File

trait XilinxBoard {
  val xdcFile: File // physical constraint file for top module
  val device: XilinxDevice
  val defaultClockDomain: ClockDomain

  def checkXdc(): Unit = {
    assert(xdcFile.exists(), s"xdc file ${xdcFile.getAbsoluteFile} not found, visit vendor website /  ")
  }
}
