package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.core.sim._

import java.io.File

// TODO: generate LZOC from local flopoco

case class LZOC() extends BlackBox {

  setDefinitionName("LZOC_32_comb_uid2")

  val I = in Bits (32 bits)
  val OZB = in Bool ()
  val O = out Bits (6 bits)

  addRTLPath(new File(resourceDir, "LZOC.vhdl").getAbsolutePath)

}

object LZOC extends App {

  case class LZOCDut() extends Component {
    val I = in Bits (32 bits)
    val OZB = in Bool ()
    val O = out Bits (6 bits)
    val lzoc = LZOC()
    lzoc.I := I
    lzoc.OZB := OZB
    O := lzoc.O
  }

  Config.sim.doSim(LZOCDut()) { dut =>
    (0 until 100).foreach { i =>
      dut.I #= i
      dut.OZB #= false
      sleep(2)
      println(dut.O.toBigInt)
    }

  }

}
