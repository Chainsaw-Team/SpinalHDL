package chainsaw.projects.xdma.daq.ku060Ips

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axis.Axi4Stream
import spinal.lib.bus.amba4.axis.Axi4StreamConfig
        
        
case class Atan2() extends BlackBox {
  val s_axis_cartesian = slave(Stream(Fragment(Bits(64 bits))))
  s_axis_cartesian.setNameForVivado()
  val m_axis_dout = master(Stream(Fragment(Bits(16 bits))))
  m_axis_dout.setNameForVivado()
  val aclk = in Bool ()

  addRTLPath(raw"/home/ltr/SpinalHDL/KU060IP/Atan2/Atan2.xci")

  mapCurrentClockDomain(aclk, reset=null, enable=null)
}

