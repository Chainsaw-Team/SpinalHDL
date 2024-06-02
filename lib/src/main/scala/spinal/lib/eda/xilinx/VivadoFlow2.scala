package spinal.lib.eda.xilinx

import org.apache.commons.io.FileUtils
import org.slf4j._
import spinal.core._
import spinal.lib.DoCmd
import spinal.lib.eda.bench.Rtl
import sun.nio.cs.UTF_8

import java.io.File
import scala.language.postfixOps

// TODO: replace VivadoFlow
object VivadoFlow2 {

  def apply(
      vivadoPath: String,
      workspacePath: String,
      rtl: Rtl,
      device: XilinxDevice,
      taskType: VivadoTaskType,
      xdcFile: Option[File] = None
  ): Option[VivadoReport] = {

    // directory & file location
    val topName = rtl.getTopModuleName()
    val genScriptDir = new File(workspacePath, s"genScript_$topName")
    val tclFile = new File(genScriptDir, "run_vivado.tcl")
    val xdcInUse = new File(genScriptDir, s"$topName.xdc")
    val logFile = new File(genScriptDir, s"$topName.log")

    FileUtils.deleteDirectory(genScriptDir)
    FileUtils.forceMkdir(genScriptDir)

    // generate constraint file
    xdcFile match {
      case Some(file) => FileUtils.copyFile(file, xdcInUse)
      case None =>
        val targetPeriod = device.fMax.toTime
        val clkConstr = s"""create_clock -period ${(targetPeriod * 1e9) toBigDecimal} [get_ports clk]"""
        FileUtils.write(xdcInUse, clkConstr, new UTF_8)
    }

    // copy source files
    val sourceFiles = rtl.getRtlPaths().map { path =>
      val sourceFile = new File(path)
      val targetFile = new File(genScriptDir, sourceFile.getName)
      FileUtils.copyFile(sourceFile, targetFile)
      targetFile
    }

    val vivadoLogger = LoggerFactory.getLogger(s"VivadoFlow")

    def genScript(): String = {
      vivadoLogger.info(s"Generating tcl script by user configuration...")

      var script = "" // constructing script by inserting commands

      def getReadCommand(sourceFile: File): String = {
        val sourcePath = sourceFile.getAbsolutePath.replace(File.separator, "/")
        if (sourcePath.endsWith(".sv")) s"read_verilog -sv $sourcePath \n"
        else if (sourcePath.endsWith(".v")) s"read_verilog $sourcePath \n"
        else if (sourcePath.endsWith(".vhdl") || sourcePath.endsWith(".vhd")) s"read_vhdl $sourcePath \n"
        else if (sourcePath.endsWith(".bin")) "\n"
        else throw new IllegalArgumentException(s"invalid RTL source path $sourcePath")
      }

      // create project
      script += s"create_project ${rtl.getTopModuleName()} ${genScriptDir.getAbsolutePath.replace(File.separator, "/")} -part ${device.part} -force\n"
      script += s"set_property PART ${device.part} [current_project]\n"

      // reading RTL sources
      sourceFiles.foreach(file => script += getReadCommand(file))

      def addReadXdcTask(): Unit = {
        script += s"read_xdc ${xdcInUse.getAbsolutePath.replace(File.separator, "/")}\n"
      }

      def addSynthTask(activateOOC: Boolean = true): Unit = {
        script += s"synth_design -part ${device.part}  -top ${rtl.getTopModuleName()} ${if (activateOOC) "-mode out_of_context"
          else ""}\t"
        script += s"\nwrite_checkpoint -force ${rtl.getTopModuleName()}_after_synth.dcp\n"
        script += s"report_timing\n"
      }

      def addImplTask(): Unit = {
        script += "opt_design\n"
        script += "place_design -directive Explore\n"
        script += "report_timing\n"
        script += s"write_checkpoint -force ${rtl.getTopModuleName()}_after_place.dcp\n"
        script += "phys_opt_design\n"
        script += "report_timing\n"
        script += s"write_checkpoint -force ${rtl.getTopModuleName()}_after_place_phys_opt.dcp\n"
        script += "route_design\n"
        script += s"write_checkpoint -force ${rtl.getTopModuleName()}_after_route.dcp\n"
        script += "report_timing\n"
        script += "phys_opt_design\n"
        script += "report_timing\n"
        script += s"write_checkpoint -force ${rtl.getTopModuleName()}_after_route_phys_opt.dcp\n"
      }

      def addBitStreamTask(): Unit = {
        script += s"write_bitstream -force ${rtl.getTopModuleName()}.bit\n"
      }

      def addReportUtilizationTask(depth: Int = 10): Unit = {
        script += s"report_utilization -hierarchical -hierarchical_depth $depth\n"
      }

      addReadXdcTask()

      taskType match {
        case PROGEN => // do nothing
        case SYNTH =>
          addSynthTask()
          addReportUtilizationTask()
        case IMPL =>
          addSynthTask()
          addImplTask()
          addReportUtilizationTask()
        case BITGEN =>
          addSynthTask(activateOOC = false)
          addImplTask()
          addBitStreamTask()
          addReportUtilizationTask()
      }

      FileUtils.write(tclFile, script, new UTF_8)
      vivadoLogger.info(s"Finish Vivado script generation...")
      script
    }

    def runScript(): Unit = {
      vivadoLogger.info(s"Running VivadoFlow...")
      // run vivado
      DoCmd.doCmd(
        s"$vivadoPath/vivado -stack 2000 -nojournal -log ${logFile.getAbsolutePath
            .replace(File.separator, "/")} -mode batch -source ${tclFile.getAbsolutePath.replace(File.separator, "/")}",
        genScriptDir.getAbsolutePath.replace(File.separator, "/")
      )
      vivadoLogger.info(s"Finish VivadoFlow...")
    }

    genScript() // generate tcl scripts
    runScript() // run tcl scripts
    if (taskType != PROGEN) Some(VivadoReport(logFile, device)) else None
  }

}
