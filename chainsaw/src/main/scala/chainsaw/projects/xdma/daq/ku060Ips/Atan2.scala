package chainsaw.projects.xdma.daq.ku060Ips

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axis.Axi4Stream
import spinal.lib.bus.amba4.axis.Axi4StreamConfig
        
        
case class Atan2() extends BlackBox {

        val s_axis_cartesian_Config = Axi4StreamConfig(dataWidth = 8, useLast = true)
        val s_axis_cartesian = slave(Axi4Stream(s_axis_cartesian_Config))
        s_axis_cartesian.setNameForEda()
        

        val m_axis_dout_Config = Axi4StreamConfig(dataWidth = 2, useLast = true)
        val m_axis_dout = master(Axi4Stream(m_axis_dout_Config))
        m_axis_dout.setNameForEda()
        
  val aclk = in Bool ()

  addRTLPath(raw"/home/ltr/SpinalHDL/KU060IP/Atan2/Atan2.xci")

  mapCurrentClockDomain(aclk, reset=null, enable=null)
}

