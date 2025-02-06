package chainsaw.projects.xdma.daq.ku060Ips

import spinal.core._
import spinal.lib._
        
import spinal.lib.bus.amba4.axis.Axi4Stream
import spinal.lib.bus.amba4.axis.Axi4StreamConfig
import spinal.lib.bus.amba4.axis.Axi4Stream
import spinal.lib.bus.amba4.axis.Axi4StreamConfig

case class FloatAcc() extends BlackBox {
  val s_axis_a = slave(Stream(Fragment(Bits(32 bits))))
  s_axis_a.setNameForVivado()
  val m_axis_result = master(Stream(Fragment(Bits(32 bits))))
  m_axis_result.setNameForVivado()
  val aclk = in Bool()
  val aresetn = in Bool()

  addRTLPath(raw"/home/ltr/IdeaProjects/SpinalHDL/KU060IP/FloatAcc/FloatAcc.xci")

  mapCurrentClockDomain(aclk, reset=aresetn, enable=null, resetActiveLevel = LOW)
}

