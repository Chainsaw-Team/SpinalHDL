package chainsaw.projects.xdma.daq.ku060Ips

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axis.Axi4Stream
import spinal.lib.bus.amba4.axis.Axi4StreamConfig
        
        
case class LowpassFir() extends BlackBox {
  val s_axis_data = slave(Stream(Fragment(Bits(32 bits))))
  s_axis_data.setNameForVivado()
  val m_axis_data = master(Stream(Fragment(Bits(64 bits))))
  m_axis_data.setNameForVivado()
  val aclk = in Bool ()

  addRTLPath(raw"/home/ltr/IdeaProjects/SpinalHDL/KU060IP/LowpassFir/LowpassFir.xci")

  mapCurrentClockDomain(aclk, reset=null, enable=null)
}

