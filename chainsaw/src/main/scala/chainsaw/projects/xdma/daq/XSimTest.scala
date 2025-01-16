package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.core.sim.SimConfig

case class XSim() extends Module {
  val a = in Bool ()
  val b = out Bool ()
  b := RegNext(a)
}

object XSim {
  def main(args: Array[String]): Unit = {

    SimConfig.withXSim.withWave.compile(new XSim())
  }
}
