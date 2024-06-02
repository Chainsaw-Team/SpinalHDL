package spinal.lib.eda.xilinx

import org.slf4j.LoggerFactory

import java.io.File
import scala.io.Source
import scala.util.Try

// TODO: clean up this file
case class VivadoReport(logFile: File, device: XilinxDevice) {

  val log = Source.fromFile(logFile)
  val lines = log.getLines.toSeq
  val report = lines.mkString("\n")

  val ConstraintPeriod = ParseReportUtils.getDoubleAfterHeader(header = "Requirement", report = report)
  val Slack = ParseReportUtils.getDoubleAfterHeader(header = s"Slack\\s*\\(\\w*\\)", report = report)
  val Frequency = 1.0 / (ConstraintPeriod - Slack) * 1e9

  val utilization = device.family match {
    case UltraScale     => getUltrascaleWithHierarchy
    case UltraScalePlus => getUltrascaleWithHierarchy
    case Series7        => VivadoUtil() // TODO: implement a method for Series7
  }

  def getUltrascaleWithHierarchy: VivadoUtil = {
    val start = lines.lastIndexWhere(_.contains("Utilization by Hierarchy"))
    var tab = 0
    val topLine = lines
      .drop(start)
      .dropWhile { line =>
        if (line.startsWith("+")) tab += 1
        tab < 2
      }(1) // TODO: extract info of sub-modules
    val segments = topLine.split("\\|").map(_.trim)
    val lut = segments(4).toInt // logic LUT only
    val ff = segments(7).toInt
    val bram36 = segments(8).toInt
    val uram288 = segments(10).toInt
    val dsp = segments(11).toInt
    val carry8 = ParseReportUtils.getIntAfterHeader(header = "CARRY8", report = report)
    // TODO: carry chain extraction
    VivadoUtil(
      lut = lut,
      ff = ff,
      dsp = dsp,
      bram36 = bram36,
      uram288 = uram288,
      carry8 = carry8
    )
  }

  def showInfosReport(): Unit = {
    val parseLogger =
      LoggerFactory.getLogger(s"Get utilization and timing information in log file which generating by Vivado")

    parseLogger.info(s"Parsing utilization information in ${logFile.getName}")

    // cell usage
    val parseItem = Seq(
      "Frequency",
      "CARRY4",
      "CARRY8",
      "FDCE",
      "FDRE",
      "LUT2",
      "LUT3",
      "LUT4",
      "LUT5",
      "LUT6",
      "MUXF7",
      "RAM32M16"
    )

    val parseValues =
      Seq(Frequency) ++ parseItem.tail.map(item => ParseReportUtils.getIntAfterHeader(header = item, report = report))

    // making table
    val itemWidth = parseItem.map(_.length).max max 6
    val valuesWidth: Int = parseValues.map(_.toString.length).max max 9
    val header =
      s"|" + s"${" " * ((itemWidth - 4) / 2)}item${" " * ((itemWidth - 3) / 2)}" +
        s"|" + s"${" " * ((valuesWidth - 5) / 2)}value${" " * ((valuesWidth - 4) / 2)}" + s"|\n" +
        s"|" + s"${" " * ((itemWidth - 4) / 2)}----${" " * ((itemWidth - 3) / 2)}" +
        s"|" + s"${" " * ((valuesWidth - 6) / 2)}------${" " * ((valuesWidth - 5) / 2)}" + s"|\n"
    val body = parseItem
      .zip(parseValues)
      .map { case (cell, number) =>
        s"|" + s"${" " * ((itemWidth - cell.length) / 2)}$cell${" " * ((itemWidth - cell.length + 1) / 2)}" +
          s"|" + s"${" " * ((valuesWidth - number.toString.length) / 2)}$number${" " * ((valuesWidth - number.toString.length + 1) / 2)}" + s"|"
      }
      .mkString("\n")

    val errorInfo = {
      ParseReportUtils
        .getPatternAfterHeader(header = "ERROR", pattern = ".*", separator = ':', report = report)
        .getOrElse(s"")
    }

    if (errorInfo.isEmpty) {
      parseLogger.info(s"Parsing result:\n$header$body")
    } else
      parseLogger.error(s"Find Error:\n$errorInfo")
  }
}

object ParseReportUtils {

  // regEx functions
  private val intFind = "[0-9]\\d*"
  private val doubleFind = "-?(?:[1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*|0\\.0+|0)"

  def getPatternAfterHeader(header: String, pattern: String, separator: Char = '|', report: String): Try[String] = {
    val regex = s"\\$separator?\\s*$header\\s*\\$separator?\\s*($pattern)\\s*\\$separator?"
    Try(regex.r.findFirstMatchIn(report).get.group(1))
  }

  def getIntAfterHeader(header: String, separator: Char = '|', report: String): Int =
    getPatternAfterHeader(header, intFind, separator, report).getOrElse("-1").toInt

  def getDoubleAfterHeader(header: String, separator: Char = ':', report: String): Double =
    getPatternAfterHeader(header, doubleFind, separator, report)
      .getOrElse("-1")
      .toDouble
}
