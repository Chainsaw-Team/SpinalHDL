package utils

import scala.io.Source
import java.io._
import scala.util.matching.Regex

object BlackBoxParser {

  def apply(from: File, to: File): Unit = {
    println(s"\nparsing ${from.getName} to ${to.getName}")
    val source = Source.fromFile(from)
    val lines =
      try source.mkString
      finally source.close()

    val modulePattern: Regex = raw"(?s)module\s+(\w+)\s*(.*?);\s*endmodule".r
    // TODO: keep the comment at the same line
    val ioPattern: Regex = raw"^\s*(input|output|inout)\s*(?:\[(\d+)\s*:\s*(\d+)\])?\s*(\w+).*".r

    for (m <- modulePattern.findAllMatchIn(lines)) {
      val moduleName = m.group(1)
      val moduleContent = m.group(2)
//      println(f"module content:\n $moduleContent")

      def getIoDeclaration(m: Regex.Match) = {
        val Seq(direction, end, start, name) = m.subgroups
        val size =
          if (start != null && end != null) end.toInt - start.toInt + 1
          else 1
        // TODO: inout -> verilog
        val dataType = if (size > 1) "Bits" else "Bool"
        val bits = if (size > 1) s"$size bits" else ""
        val dataDefinition = direction match {
          case "input"  => s"in $dataType($bits)"
          case "output" => s"out $dataType($bits)"
          case "inout"  => s"inout(Analog($dataType($bits)))"
        }
        val ret = s"  val $name = $dataDefinition\n"
        print(s"$ret")
        ret
      }

      val ios: Array[String] =
        moduleContent.split("\n").flatMap(line => ioPattern.findAllMatchIn(line).map(getIoDeclaration).toList)

      // TODO: infer axi interfaces(as an option)

      // creating SpinalHDL BlackBox
      val writer = new PrintWriter(to)
      writer.write(s"import spinal.core._\nclass $moduleName extends BlackBox {\n")
      ios.foreach(input => writer.write(input))
      writer.write("  noIoPrefix()\n")
      val filePathString = "\"" + from.getAbsolutePath + "\""
      writer.write(s"  addRTLPath(raw$filePathString)\n}\n")
      writer.close()
    }
  }

  def main(args: Array[String]): Unit = {
    BlackBoxParser(
      new File(
        "C:\\Users\\ltr\\Documents\\GitHub\\SpinalHDL\\chainsaw\\src\\main\\resources\\LDF172_1G14_ref_utf8\\sources_1\\new\\sys_top_axku062.v"
      ),
      new File("MysoowSysTop.scala")
    )
    BlackBoxParser(new File("xdma.v"), new File("xdma.scala"))
  }
}
