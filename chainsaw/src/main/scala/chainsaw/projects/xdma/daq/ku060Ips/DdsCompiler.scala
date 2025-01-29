package chainsaw.projects.xdma.daq.ku060Ips

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axis.Axi4Stream
import spinal.lib.bus.amba4.axis.Axi4StreamConfig
        
        
case class DdsCompiler() extends BlackBox {
  val s_axis_phase = slave(Stream(Fragment(Bits(24 bits))))
  s_axis_phase.setNameForVivado()
  val m_axis_data = master(Stream(Fragment(Bits(32 bits))))
  m_axis_data.setNameForVivado()
  val aclk = in Bool ()
  val event_pinc_invalid = out Bool ()
  val event_poff_invalid = out Bool ()

  addRTLPath(raw"/home/ltr/IdeaProjects/SpinalHDL/KU060IP/DdsCompiler/DdsCompiler.xci")

  mapCurrentClockDomain(aclk, reset=null, enable=null)
}

