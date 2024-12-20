package spinal.lib.com.ddr

import spinal.core._

import scala.language.postfixOps

case class Ddr4Interface(addrWidth: Int, dataWidthInByte: Int) extends Bundle {

  val act_n = out Bool ()
  val adr = out Bits (addrWidth bits)
  val ba = out Bits (2 bits)
  val bg = out Bool ()
  val ck_c = out Bool ()
  val ck_t = out Bool ()
  val cke = out Bool ()
  val cs_n = out Bool ()
  val dm_n = inout(Analog(Bits(dataWidthInByte bits)))
  val dq = inout(Analog(Bits(dataWidthInByte * 8 bits)))
  val dqs_c = inout(Analog(Bits(dataWidthInByte bits)))
  val dqs_t = inout(Analog(Bits(dataWidthInByte bits)))
  val odt = out Bool ()
  val reset_n = out Bool ()

}
