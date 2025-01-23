package chainsaw.projects.xdma.daq.customizedIps

import spinal.core._
import spinal.lib.bus.amba4.axis.{Axi4Stream, Axi4StreamConfig}
import spinal.lib.{master, slave}

case class StreamPassThrough() extends Module{

  val dataInConfig = Axi4StreamConfig(2, useLast = true)
  val dataIn = slave(Axi4Stream(dataInConfig))

  val dataOutConfig = Axi4StreamConfig(2, useLast = true)
  val dataOut = master(Axi4Stream(dataInConfig))

  dataIn >> dataOut

}
