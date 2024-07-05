package utils

import chainsaw.utils.{Axi4LiteParser, Axi4StreamParser, Ddr4Parser}

import scala.io.Source
import java.io._
import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

case class IoDefinition(
    direction: String,
    bitWidth: Int,
    name: String,
    interfaceName: String = "",
    fieldName: String = ""
) {
  def getDeclaration: String = {
    val dataType = if (bitWidth > 1) "Bits" else "Bool"
    val bits = if (bitWidth > 1) s"$bitWidth bits" else ""
    val dataDefinition = direction match {
      case "input"  => s"in $dataType($bits)"
      case "output" => s"out $dataType($bits)"
      case "inout"  => s"inout(Analog($dataType($bits)))"
    }
    s"  val $name = $dataDefinition\n"
  }
}

object BlackBoxParser {

  def apply(from: File, to: File, parsingInterface: Boolean = true): Unit = {

    println(s"\nparsing ${from.getName} to ${to.getName}")

    val source = Source.fromFile(from)
    val lines =
      try source.mkString
      finally source.close()

    val modulePattern: Regex = raw"(?s)module\s+(\w+)\s*(.*?);\s*endmodule".r
    // TODO: keep the comment at the same line
    val ioPattern: Regex = raw"^\s*(input|output|inout)\s*(reg)?\s*(?:\[(\d+)\s*:\s*(\d+)\])?\s*(\w+).*".r

    for (m <- modulePattern.findAllMatchIn(lines)) {
      val moduleName = m.group(1)
      val moduleContent = m.group(2)
//      println(f"module content:\n $moduleContent")

      def getIoDefinition(m: Regex.Match): IoDefinition = {
        val Seq(direction, reg, end, start, name) = m.subgroups
        val size =
          if (start != null && end != null) end.toInt - start.toInt + 1
          else 1
        IoDefinition(direction, size, name)
      }

      // parsing io definitions
      val lines = moduleContent.split("\n")
      val definitions = ArrayBuffer[IoDefinition]()
      lines.flatMap(line => ioPattern.findAllMatchIn(line).map(getIoDefinition)).foreach(definitions.append(_))

      val declarations = ArrayBuffer[String]()
      if (parsingInterface) { // parsing interfaces
        declarations ++= Ddr4Parser(definitions)
        declarations ++= Axi4LiteParser(definitions)
        declarations ++= Axi4StreamParser(definitions)
      }
      declarations ++= definitions.map(_.getDeclaration)

      // creating SpinalHDL BlackBox
      val writer = new PrintWriter(to)
      val imports =
        """
          |import spinal.core._
          |import spinal.lib._
          |import spinal.lib.bus.amba4.axi._
          |import spinal.lib.bus.amba4.axis._
          |import spinal.lib.bus.amba4.axilite._
          |
          |import scala.language.postfixOps
          |
          |""".stripMargin
      writer.write(imports)
      writer.write(s"class $moduleName extends BlackBox {\n")
      declarations.foreach(input => writer.write(input))
      val filePathString = "\"" + from.getAbsolutePath + "\""
      writer.write(s"\n  addRTLPath(raw$filePathString)\n}\n")
      writer.close()
    }
  }
}
