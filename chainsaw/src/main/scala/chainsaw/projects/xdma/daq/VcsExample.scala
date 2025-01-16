package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.core.sim._
import spinal.sim.VCSFlags
import spinal.lib.blackbox.xilinx.ultrascale.IBUFDS

import scala.language.postfixOps

case class dds_compiler_0() extends BlackBox {

  val aclk = in Bool ()
  val s_axis_phase_tvalid = in Bool ()
  val s_axis_phase_tdata = in UInt (16 bits)
  val s_axis_phase_tlast = in Bool ()
  val m_axis_data_tvalid = out Bool ()
  val m_axis_data_tdata = out UInt (48 bits)
  val m_axis_data_tlast = out Bool ()
  addRTLPath("/home/ltr/KU060IP/dds_compiler_0/sim/dds_compiler_0.vhd")

}

case class DdsDut() extends Component {

  val dds = dds_compiler_0()
  val clk = in Bool ()
  val cd = new ClockDomain(clk)

  val s_axis_phase_tvalid = in Bool ()
  val s_axis_phase_tdata = in UInt (16 bits)
  val s_axis_phase_tlast = in Bool ()
  val m_axis_data_tvalid = out Bool ()
  val m_axis_data_tdata = out UInt (48 bits)
  val m_axis_data_tlast = out Bool ()
  dds.aclk := clk
  dds.s_axis_phase_tvalid := s_axis_phase_tvalid
  dds.s_axis_phase_tdata := s_axis_phase_tdata
  dds.s_axis_phase_tlast := s_axis_phase_tlast
  m_axis_data_tvalid := dds.m_axis_data_tvalid
  m_axis_data_tdata := dds.m_axis_data_tdata
  m_axis_data_tlast := dds.m_axis_data_tlast

}

object DdsDut extends App {

  val flags = VCSFlags( // flags for VCS & VERDI 2016 on Ubuntu2024.04.1 LTS, with gcc & gcc 13.3.0
    compileFlags = List(
//      "/home/ltr/xilinxip_lib/glbl.v"
    ),
    elaborateFlags = List(
      "-LDFLAGS -no-pie",
      "-LDFLAGS -Wl,-no-as-needed",
      "-LDFLAGS -fno-lto", // to avoid toolchain version conflict, disable LTO(link-time optimization) by -fno-lto
      "-CFLAGS -fPIE",
//      "work.glbl"
    )
  )

  SimConfig
    .withVCS(flags)
    .withVCSSimSetup(setupFile = "/home/ltr/xilinxip_lib/synopsys_sim.setup", beforeAnalysis = null)
    .withWave
    .doSim(DdsDut()) { dut =>
      val clockDomain = dut.clockDomain
      clockDomain.forkStimulus(10)
      (0 until 10).foreach { i =>
        dut.s_axis_phase_tvalid #= true
        dut.s_axis_phase_tlast #= true
        dut.s_axis_phase_tdata #= i
        println(dut.m_axis_data_tdata.toBigInt)

        clockDomain.waitSampling()

      }
      simSuccess()

    }

}

case class SimpleModule() extends Component {

  val clkP, clnkN = in Bool ()
  val clkOut = out Bool ()

  val dataIn = in UInt (8 bits)
  val dataOut = out UInt (8 bits)

  dataOut := dataIn
  clkOut := IBUFDS.Lvds2Clk(clkP, clnkN)

}

object SimpleModule extends App {

  val flags = VCSFlags( // flags for VCS & VERDI 2016 on Ubuntu2024.04.1 LTS, with gcc & gcc 13.3.0
    compileFlags = List(
    ),
    elaborateFlags = List(
      "-LDFLAGS -no-pie",
      "-LDFLAGS -Wl,-no-as-needed",
      "-LDFLAGS -fno-lto", // to avoid toolchain version conflict, disable LTO(link-time optimization) by -fno-lto
      "-CFLAGS -fPIE"
    )
  )

  SimConfig
    .withXSim
//    .withVCS(flags)
    .withWave
    .doSim(SimpleModule()) { dut =>
      val clockDomain = dut.clockDomain
      clockDomain.forkStimulus(10)
      (0 until 10).foreach { i =>
        dut.dataIn #= i
        dut.clkP #= i % 2 == 0
        dut.clnkN #= i % 2 == 1
        println(dut.clkOut.toBoolean)

        clockDomain.waitSampling()

      }
      simSuccess()

    }

}
