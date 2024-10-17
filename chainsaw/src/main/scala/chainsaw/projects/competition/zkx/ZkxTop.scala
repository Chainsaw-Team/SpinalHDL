package chainsaw.projects.competition.zkx

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.sim._
import spinal.lib.fsm._
import spinal.lib.bus._

// write_project_tcl -force C:/Users/lsfan/Documents/GitHub/SpinalHDL/chainsaw/src/main/resources/ZkxSources/createZ7p.tcl

case class ZkxTop() extends Z7P {

  val peripheral = new Peripheral_wrapper()

  peripheral.pcie_clk_clk_p := pcie.clk_p
  peripheral.pcie_clk_clk_n := pcie.clk_n
  peripheral.pcie_perst := pcie.perst
  peripheral.pcie_mgt_rxp <> pcie.rx_p
  peripheral.pcie_mgt_rxn <> pcie.rx_n
  peripheral.pcie_mgt_txp <> pcie.tx_p
  peripheral.pcie_mgt_txn <> pcie.tx_n

  pl_led := peripheral.pcie_heartbeat

}
