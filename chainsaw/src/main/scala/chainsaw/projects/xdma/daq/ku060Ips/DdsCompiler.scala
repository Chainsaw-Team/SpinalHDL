package chainsaw.projects.xdma.daq.ku060Ips

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axis.Axi4Stream
import spinal.lib.bus.amba4.axis.Axi4StreamConfig
        
        
case class DdsCompiler() extends BlackBox {

        val s_axis_phase_Config = Axi4StreamConfig(dataWidth = 3, useLast = true)
        val s_axis_phase = slave(Axi4Stream(s_axis_phase_Config))
        s_axis_phase.setNameForEda()
        

        val m_axis_data_Config = Axi4StreamConfig(dataWidth = 4, useLast = true)
        val m_axis_data = master(Axi4Stream(m_axis_data_Config))
        m_axis_data.setNameForEda()
        
  val aclk = in Bool ()
  val event_pinc_invalid = out Bool ()
  val event_poff_invalid = out Bool ()

  addRTLPath(raw"/home/ltr/SpinalHDL/KU060IP/DdsCompiler/DdsCompiler.xci")

  mapCurrentClockDomain(aclk, reset=null, enable=null)
}

