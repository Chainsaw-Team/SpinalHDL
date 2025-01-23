package chainsaw.projects.xdma.daq.ku060Ips

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axis.Axi4Stream
import spinal.lib.bus.amba4.axis.Axi4StreamConfig
        
        
case class LowpassFir() extends BlackBox {

        val s_axis_data_Config = Axi4StreamConfig(dataWidth = 4, useLast = true)
        val s_axis_data = slave(Axi4Stream(s_axis_data_Config))
        s_axis_data.setNameForEda()
        

        val m_axis_data_Config = Axi4StreamConfig(dataWidth = 8, useLast = true)
        val m_axis_data = master(Axi4Stream(m_axis_data_Config))
        m_axis_data.setNameForEda()
        
  val aclk = in Bool ()

  addRTLPath(raw"/home/ltr/SpinalHDL/KU060IP/LowpassFir/LowpassFir.xci")

  mapCurrentClockDomain(aclk, reset=null, enable=null)
}

