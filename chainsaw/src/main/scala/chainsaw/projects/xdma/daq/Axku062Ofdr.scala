package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.lib.blackbox.xilinx.ultrascale.{IBUFDS, OBUFDS}
import spinal.lib.eda.xilinx.boards.alinx.{Axku062, FL9627, Fl1010}

// TODO: FL1010 & GPU module
// useful tcl commands

// generate memory configuration file
// write_cfgmem  -format bin -size 32 -interface SPIx8 -loadbit {up 0x00000000 "C:/Users/lsfan/Desktop/Axku062Daq/Axku062Daq.runs/impl_1/Axku062Daq.bit" } -force -file "C:/Users/lsfan/Desktop/Axku062Daq/Axku062Daq.runs/impl_1/Axku062Daq.bin"

// save project
// write_project_tcl -force ../<project name>.tcl

import scala.language.postfixOps

case class Axku062Ofdr() extends Axku062 {

  // board connection
  val fl9627 = FL9627(fmc_lpc_1)

  fmc_lpc_1.DP_C2M_P.setAsDirectionLess() // disable unused output
  fmc_lpc_1.DP_C2M_N.setAsDirectionLess()

  fl9627.CLK1_125M := True
  fl9627.AD1_SPI_CS := True
  fl9627.AD1_SPI_SCLK := True
  fl9627.CLK2_125M := True
  fl9627.AD2_SPI_CS := True
  fl9627.AD2_SPI_SCLK := True

}

object Axku062Ofdr extends App {
  SpinalConfig().generateVerilog(Axku062Ofdr())
}