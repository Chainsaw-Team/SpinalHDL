package chainsaw.projects.xdma.daq

import spinal.core._
import utils.BlackBoxParser

import java.io.File

//////////
// you can rebuild Axku062Daq Vivado project by following steps:
//////////

// 1: generate submodules & top-level module
object GenerateSubModules extends App {
  val config =
    SpinalConfig(
      defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW, resetKind = SYNC),
      targetDirectory = axku062DaqRtlDir.getAbsolutePath
    )
  config.generateVerilog(AdiSpiCtrl(50)) // sclk frequency = 125MHz / 50 = 1MHz < 2.5MHz < 10MHz
  config.generateVerilog(ChainsawDaqDataPath())
}

object GenerateTopModule extends App {
  SpinalConfig(targetDirectory = axku062DaqRtlDir.getAbsolutePath)
    .generateVerilog(Axku062Daq())
}

// 2. run .tcl script to build project including a block design
// run following command in [[axku062DaqRtlDir]]
// vivado -source <project_name.tcl>

// 3. every time you create/update block design inside the project, rebind clock domains introduced by user logic, using following commands in Vivado TCL Console
// set_property CONFIG.CLK_DOMAIN Peripheral_PCIe_0_axi_aclk [get_bd_intf_pins /Datapath/controlIn]
// set_property CONFIG.FREQ_HZ 125000000 [get_bd_intf_pins /Datapath/controlIn]
// set_property CONFIG.CLK_DOMAIN Peripheral_jesd204_buffer_0_IBUF_DS_ODIV2 [get_bd_intf_pins /Datapath/dataIn]
// set_property CONFIG.FREQ_HZ 250000000 [get_bd_intf_pins /Datapath/dataIn]
// set_property CONFIG.CLK_DOMAIN Peripheral_jesd204_buffer_0_IBUF_DS_ODIV2 [get_bd_intf_pins /Datapath/dataOut]
// set_property CONFIG.FREQ_HZ 250000000 [get_bd_intf_pins /Datapath/dataOut]

// 4. synth,impl & bitgen

//////////
// you can upgrade Axku062Daq by following steps:
//////////

// 1. modify existing submodules / add new submodules in Scala and generate them using [[GenerateSubModules]](you may need to add extra modules)
// 2. update / modify block design in Vivado, regenerate its wrapper HDL file
// 3. create block design wrapper in Scala by following task:
object BuildBlackBox extends App {
  val bdPath = "Axku062Daq/Axku062Daq.gen/sources_1/bd/Peripheral/hdl/Peripheral_wrapper.v"
  BlackBoxParser(
    from = new File(axku062DaqRtlDir, bdPath),
    to = new File(daqScalaSource, "Axku062Wrapper.scala")
  )
}
// 4. connect block design wrapper with top-level I/O in Axku062Daq.scala
// 5. regenerate top-level module using [[GenerateTopModule]]
// 6. synth,impl & bitgen
// 7. to save your upgrade in Vivado, run following command in Vivado TCL console to update <project_name.tcl>
// !! before overwrite the script, ensure that utils files are deleted, or "local or imported" files won't be <none>
// write_project_tcl -force ../<project_name.tcl>

// for IP location version control
//# 获取当前工程中的所有 IP
//  set ips [get_ips]
//
//# 如果没有找到任何 IP 核，提示并退出
//if {[llength $ips] == 0} {
//  puts "No IP cores found in the current project."
//  return
//}
//
//# 指定导出脚本文件的目录
//set output_dir "./ip_tcl_scripts"
//file mkdir $output_dir
//
//# 遍历项目中的所有 IP，将每个 IP 的定制化过程保存为一个 .tcl 文件
//  foreach ip $ips {
//  # 获取 IP 的名称
//  set ip_name [get_property NAME $ip]
//
//  # 定义输出路径和脚本名称
//  set tcl_file "$output_dir/${ip_name}.tcl"
//
//  # 使用 write_ip_tcl 将 IP 的定制化配置导出为单独的 .tcl 文件
//    write_ip_tcl -force $ip $tcl_file
//
//  # 输出成功信息
//  puts "Exported IP: $ip_name to file: $tcl_file"
//}
//
//puts "All IPs have been successfully exported to $output_dir."
