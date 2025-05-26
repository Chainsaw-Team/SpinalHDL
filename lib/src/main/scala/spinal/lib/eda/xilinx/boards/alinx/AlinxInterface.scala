package spinal.lib.eda.xilinx.boards.alinx

import spinal.core._
import spinal.lib.com.fmc.Fmc

case class Alinx40Pin() extends Bundle {
  val IO_P, IO_N = (0 until 17).map(_ => Analog(Bool()))
}


