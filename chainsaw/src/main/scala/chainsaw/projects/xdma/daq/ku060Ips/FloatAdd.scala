package chainsaw.projects.xdma.daq.ku060Ips

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axis.Axi4Stream
import spinal.lib.bus.amba4.axis.Axi4StreamConfig
        
        
case class FloatAdd() extends BlackBox {

        val s_axis_a_Config = Axi4StreamConfig(dataWidth = 4, useLast = true)
        val s_axis_a = slave(Axi4Stream(s_axis_a_Config))
        s_axis_a.setNameForEda()
        

        val s_axis_b_Config = Axi4StreamConfig(dataWidth = 4, useLast = true)
        val s_axis_b = slave(Axi4Stream(s_axis_b_Config))
        s_axis_b.setNameForEda()
        

        val m_axis_result_Config = Axi4StreamConfig(dataWidth = 4, useLast = true)
        val m_axis_result = master(Axi4Stream(m_axis_result_Config))
        m_axis_result.setNameForEda()
        
  val aclk = in Bool ()

  addRTLPath(raw"/home/ltr/SpinalHDL/KU060IP/FloatAdd/FloatAdd.xci")

  mapCurrentClockDomain(aclk, reset=null, enable=null)
}

