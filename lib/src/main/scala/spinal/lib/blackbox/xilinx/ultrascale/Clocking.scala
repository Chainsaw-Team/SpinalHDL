package spinal.lib.blackbox.xilinx.ultrascale

import spinal.core._

case class IBUFDS() extends BlackBox {
  val I, IB = in Bool ()
  val O = out Bool ()
}

object IBUFDS {

  /** convert LVDS clock pair into single-ended clock
    */
  def Lvds2Clk(p: Bool, n: Bool): Bool = {
    val buf = IBUFDS()
    buf.I := p
    buf.IB := n
    buf.O
  }
}

case class OBUFDS() extends BlackBox {
  val I = in Bool ()
  val O, OB = out Bool ()
}

object OBUFDS {

  /** convert single-ended clock into LVDS clock pair
    */
  def Clk2Lvds(i: Bool): (Bool, Bool) = {
    val buf = OBUFDS()
    buf.I := i
    (buf.O, buf.OB)
  }
}

// TODO: a general IBUFDS_GT interface generating primitives according to part name
// TODO: add generics
case class IBUFDS_GTHE3() extends BlackBox {
  val CEB = in Bool () // active-Low asynchronous clock enable signal for the clock buffer
  val I, IB = in Bool ()
  val O, ODIV2 = out Bool ()
}

object IBUFDS_GTHE3 {
  def LvdsGt2Clk(p: Bool, n: Bool): (Bool, Bool) = {
    val buf = IBUFDS_GTHE3()
    buf.I := p
    buf.IB := n
    buf.CEB := False
    (buf.O, buf.ODIV2)
  }
}

case class IOBUF() extends BlackBox {
  val I, T = in Bool ()
  val O = out Bool ()
  val IO = inout(Analog(Bool()))
}

object IOBUF {
  def io2to1(toIo: Bool, fromIo: Bool, toIoEnable: Bool) = {
    val buf = IOBUF()
    buf.I := toIo
    buf.T := toIoEnable
    fromIo := buf.O
    buf.IO
  }
}
