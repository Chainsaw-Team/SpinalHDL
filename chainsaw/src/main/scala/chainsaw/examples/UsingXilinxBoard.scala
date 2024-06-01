package org.chainsaw

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._
import spinal.lib.fsm._
import spinal.lib.bus._

import spinal.lib.eda.xilinx.boards.alinx.Axku062

case class Axku062Led() extends Axku062{

  defaultClockDomain on {
    led_test.asBools.foreach(_ := user_key_n)
  }

}

object Axku062Led {
  def main(args: Array[String]): Unit = {
    SpinalVerilog(new Axku062Led)
  }
}

