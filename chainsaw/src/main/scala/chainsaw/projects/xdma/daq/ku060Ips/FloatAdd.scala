package chainsaw.projects.xdma.daq.ku060Ips

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axis.Axi4Stream
import spinal.lib.bus.amba4.axis.Axi4StreamConfig
        
        
case class FloatAdd() extends BlackBox {
  val s_axis_a = slave(Stream(Fragment(Bits(32 bits))))
  s_axis_a.setNameForVivado()
  val s_axis_b = slave(Stream(Fragment(Bits(32 bits))))
  s_axis_b.setNameForVivado()
  val m_axis_result = master(Stream(Fragment(Bits(32 bits))))
  m_axis_result.setNameForVivado()
  val aclk = in Bool ()

  addRTLPath(raw"/home/ltr/IdeaProjects/SpinalHDL/KU060IP/FloatAdd/FloatAdd.xci")

  mapCurrentClockDomain(aclk, reset=null, enable=null)
}

