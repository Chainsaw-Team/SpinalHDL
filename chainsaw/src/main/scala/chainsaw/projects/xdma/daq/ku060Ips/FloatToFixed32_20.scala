package chainsaw.projects.xdma.daq.ku060Ips

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axis.Axi4Stream
import spinal.lib.bus.amba4.axis.Axi4StreamConfig
        
        
case class FloatToFixed32_20() extends BlackBox {

        val s_axis_a_Config = Axi4StreamConfig(dataWidth = 4)
        val s_axis_a = slave(Axi4Stream(s_axis_a_Config))
        s_axis_a.setNameForEda()
        

        val m_axis_result_Config = Axi4StreamConfig(dataWidth = 4)
        val m_axis_result = master(Axi4Stream(m_axis_result_Config))
        m_axis_result.setNameForEda()
        
  val aclk = in Bool ()

  addRTLPath(raw"/home/ltr/SpinalHDL/KU060IP/FloatToFixed32_20/FloatToFixed32_20.xci")

  mapCurrentClockDomain(aclk, reset=null, enable=null)
}

