package utils

import scala.io.Source
import java.io._
import scala.util.matching.Regex

object BlackBoxParser {

  def apply(from: File, to: File): Unit = {
    val source = Source.fromFile(from)
    val lines =
      try source.mkString
      finally source.close()

    val modulePattern: Regex = raw"(?s)module\s+(\w+)\s*(.*?);\s*endmodule".r
    val inputPattern: Regex = raw"input\s+(?:\[(\d+):(\d+)\]\s*)?(\w+);".r
    val outputPattern: Regex = raw"output\s+(?:\[(\d+):(\d+)\]\s*)?(\w+);".r

    for (m <- modulePattern.findAllMatchIn(lines)) {
      val moduleName = m.group(1)
      val moduleContent = m.group(2)

      def getIoDeclaration(m: Regex.Match, prefix: String) = {
        val name = m.group(3)
        val size =
          if (m.group(1) != null && m.group(2) != null) m.group(1).toInt - m.group(2).toInt + 1
          else 1
        val dataType = if (size > 1) "Bits" else "Bool"
        val bits = if (size > 1) s"$size bits" else ""
        s"  val $name = $prefix $dataType($bits)\n"
      }

      val inputs = inputPattern.findAllMatchIn(moduleContent).map(getIoDeclaration(_, "in")).toList
      val outputs = outputPattern.findAllMatchIn(moduleContent).map(getIoDeclaration(_, "out")).toList

      // TODO: infer axi interfaces(as an option)

      // creating SpinalHDL BlackBox
      val writer = new PrintWriter(to)
      writer.write(s"import spinal.core._\nclass $moduleName extends BlackBox {\n")
      inputs.foreach(input => writer.write(input))
      outputs.foreach(output => writer.write(output))
      writer.write("  noIoPrefix()\n}\n")
      writer.close()
    }
  }

  def main(args: Array[String]): Unit = {
    BlackBoxParser(new File("xdma.v"), new File("xdma.scala"))
  }
}
