package utils

import scala.io.Source
import java.io._
import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

object BlackBoxParser {

  case class IoDefinition(
      direction: String,
      bitWidth: Int,
      name: String
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
    // interface recognitions
    def isAxiLite: Boolean = name.toLowerCase().contains("axi_lite")
    def isAxiStream: Boolean = name.toLowerCase().contains("AXIS")
    def isNotAxi: Boolean = !isAxiLite && !isAxiStream
    def isDdr4: Boolean = name.toLowerCase().contains("ddr4_rtl")
    def isNotInterface = isNotAxi && !isDdr4

    def getAxiName: String = {
      val index = name.lastIndexOf("_")
      if (index != -1) name.substring(0, index) else name
    }

    def getDdr4Name:String = name.split("_").take(3).mkString("_")
  }

  def apply(from: File, to: File): Unit = {
    println(s"\nparsing ${from.getName} to ${to.getName}")
    val source = Source.fromFile(from)
    val lines =
      try source.mkString
      finally source.close()

    val modulePattern: Regex = raw"(?s)module\s+(\w+)\s*(.*?);\s*endmodule".r
    // TODO: keep the comment at the same line
//    val ioPattern: Regex = raw"^\s*(input|output|inout)\s*(?:\[(\d+)\s*:\s*(\d+)\])?\s*(\w+).*".r
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

      val lines = moduleContent.split("\n")
      val definitions: Array[IoDefinition] = lines.flatMap(line => ioPattern.findAllMatchIn(line).map(getIoDefinition))
      val definitionsRaw = definitions.filter(_.isNotInterface)

      // TODO: infer DDR4 interface for different data width
      // DDR4
      val ddr4CandidateGroups = definitions.filter(_.isDdr4).groupBy(_.getDdr4Name)
      val ddr4Declarations = ddr4CandidateGroups.map { case (name, definitions) =>
        println(f"found DDR4 interface element: \n\t${definitions.map(_.name).mkString("\n\t")}")
        val config = s"val $name = Ddr4Interface()"
        println(s"inferred AXI4-Lite interface config: $config")
        s"""
           |  $config
           |""".stripMargin
      }

      // TODO: infer axi interfaces(as an option), including AXI4, AXI4-Lite, AXI4-Stream
      // AXI4-Lite
      val axi4LiteCandidateGroups = definitions.filter(_.isAxiLite).groupBy(_.getAxiName)
      val axi4LiteDeclarations = axi4LiteCandidateGroups.map { case (name, definitions) =>
        println(f"found AXI4-Lite interface element: \n\t${definitions.map(_.name).mkString("\n\t")}")
        val addr = definitions.find(_.name.contains("addr")).get
        val addrWidth = addr.bitWidth
        val direction = if (addr.direction == "output") "master" else "slave"
        val dataWidth = definitions.find(_.name.contains("data")).get.bitWidth
        val config = s"val ${name}_Config = AxiLite4Config(addressWidth = $addrWidth, dataWidth = $dataWidth)"
        println(s"inferred AXI4-Lite interface config: $config")
        s"""
          |  $config
          |  val $name = $direction(AxiLite4(${name}_Config))
          |  $name.setNameForEda()
          |""".stripMargin
      }

      // AXI4-Stream
      val axi4StreamCandidateGroups = definitions.filter(_.isAxiStream).groupBy(_.getAxiName)
      val axi4StreamDeclarations = axi4StreamCandidateGroups.map { case (name, definitions) =>
        println(definitions.map(_.name).mkString("\n"))
        val data = definitions.find(_.name.contains("tdata")).get
        assert(data.bitWidth % 8 == 0, s"invalid AXI4-Stream bit width ${data.bitWidth}")
        val dataWidthInBytes = data.bitWidth / 8
        val direction = if (data.direction == "output") "master" else "slave"
        val fields = ArrayBuffer[String]()
        fields += s"dataWidth = $dataWidthInBytes"
        if (definitions.exists(_.name.contains("tlast"))) fields += "useLast = true"
        if (definitions.exists(_.name.contains("tkeep"))) fields += "useKeep = true"
        if (definitions.exists(_.name.contains("tuser"))) fields += "useUser = true"
        definitions.find(_.name.contains("tid")) match {
          case Some(id) => fields ++= Seq("useId = true", s"idWidth = ${id.bitWidth}")
          case None     =>
        }
        definitions.find(_.name.contains("dest")) match {
          case Some(dest) => fields ++= Seq("useDest = true", s"destWidth = ${dest.bitWidth}")
          case None       =>
        }
        val config = s"val ${name}_Config = Axi4StreamConfig(${fields.mkString(", ")})"
        println(
          s"infer axi4-stream: $config"
        )
        s"""
           |  $config
           |  val $name = $direction(Axi4Stream(${name}_Config))
           |  $name.setNameForEda()
           |""".stripMargin
      }

      val declarations: Array[String] =
        definitionsRaw.map(_.getDeclaration) ++ axi4LiteDeclarations ++ axi4StreamDeclarations ++ ddr4Declarations

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
