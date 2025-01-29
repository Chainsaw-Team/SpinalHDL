package chainsaw.projects.xdma.daq.ku060Ips

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axis.Axi4Stream
import spinal.lib.bus.amba4.axis.Axi4StreamConfig
        
        
case class FloatToFixed32_16() extends BlackBox {
  val s_axis_a = slave(Stream(Fragment(Bits(32 bits))))
  s_axis_a.setNameForVivado()

            val m_axis_result_Config = Axi4StreamConfig(dataWidth = 4, useLast = true, useUser = true, userWidth = 1)
            val m_axis_result = master(Axi4Stream(m_axis_result_Config))
            m_axis_result.setNameForEda()
            
  val aclk = in Bool ()

  addRTLPath(raw"/home/ltr/IdeaProjects/SpinalHDL/KU060IP/FloatToFixed32_16/FloatToFixed32_16.xci")

  mapCurrentClockDomain(aclk, reset=null, enable=null)
}

