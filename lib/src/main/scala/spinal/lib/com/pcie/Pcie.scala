package spinal.lib.com.pcie

import spinal.core._
import spinal.lib.IMasterSlave

import scala.language.postfixOps

class Pcie(laneWidth: Int) extends Bundle with IMasterSlave {
  assert(Seq(1, 2, 4, 8, 16).contains(laneWidth), "laneWidth must be 1,2,4,8 or 16")
  val perst, clk_p, clk_n = Bool()
  val rx_p, rx_n, tx_p, tx_n = Bits(laneWidth bits)
  this.setName("pcie")

  override def asMaster(): Unit = {
    out(perst, clk_p, clk_n, rx_p, rx_n)
    in(tx_p, tx_n)
  }
}
