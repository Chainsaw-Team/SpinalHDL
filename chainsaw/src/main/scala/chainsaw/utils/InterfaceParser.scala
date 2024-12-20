package chainsaw.utils

import utils.IoDefinition

import scala.collection.mutable.ArrayBuffer

abstract class InterfaceParser {

  case class Interface(
      interfaceName: String,
      fields: Seq[IoDefinition]
  )

  val identifier: String
  val requiredFields: Seq[String]
  val optionalFields: Seq[String]

  private def allFields: Seq[String] = requiredFields ++ optionalFields

  def getInterfaceGroups(candidates: ArrayBuffer[IoDefinition]): Seq[Interface] = {

    val candidateGroups = candidates
      .flatMap { candidate => // get interface fields
        val ioName = candidate.name
        if (!ioName.toLowerCase().contains(identifier)) None // must contain identifier
        else {
          allFields.find(field => ioName.endsWith(s"_$field")) match { // must end with field name
            case Some(field) =>
              val interfaceName = ioName.replace(s"_$field", "")
              val ioDefinition =
                IoDefinition(candidate.direction, candidate.bitWidth, candidate.name, interfaceName, field)
              Some(ioDefinition)
            case None => None
          }
        }
      }
      .groupBy(_.interfaceName)
      .filter { case (_, fields) => // must contain all required fields
        requiredFields.forall(fields.map(_.fieldName).contains)
      }
      .map { case (interfaceName, fields) => Interface(interfaceName, fields) }
      .toSeq

    candidateGroups.foreach(interface => // remove interface fields from original IO definitions
      interface.fields.foreach { field =>
        val ioName = field.interfaceName + "_" + field.fieldName
        candidates -= candidates.find(_.name == ioName).get
      }
    )

    candidateGroups

  }

  def getDefinition(interface: Interface): String

  def apply(candidates: ArrayBuffer[IoDefinition]): Seq[String] =
    getInterfaceGroups(candidates).map(getDefinition)

}

// TODO: infer DDR4 interface for different data width
object Ddr4Parser extends InterfaceParser {
  override val identifier: String = "ddr4"
  override val requiredFields: Seq[String] =
    Seq("act_n", "adr", "ba", "bg", "ck_c", "ck_t", "cke", "cs_n", "dm_n", "dq", "dqs_c", "dqs_t", "odt", "reset_n")
  override val optionalFields: Seq[String] = Seq()

  override def getDefinition(interface: Interface): String = {
    println(s"found DDR4 interface elements:\n\t${interface.fields.map(_.name).mkString("\n\t")}")
    val definitions = interface.fields
    val dqs = definitions.find(_.name.contains("dqs_c")).get
    val dataWidthInByte = dqs.bitWidth

    s"""
       |  val ${interface.interfaceName} = Ddr4Interface(17, $dataWidthInByte)
       |""".stripMargin
  }
}

// TODO: currently, all fields are marked as required, find out the optional ones
object Axi4LiteParser extends InterfaceParser {
  override val identifier: String = "axi_lite"
  override val requiredFields: Seq[String] = {
    Seq("araddr", "arprot", "arready", "arvalid") ++
      Seq("awaddr", "awprot", "awready", "awvalid") ++
      Seq("bready", "bresp", "bvalid") ++
      Seq("rdata", "rready", "rresp", "rvalid") ++
      Seq("wdata", "wready", "wstrb", "wvalid")

  }
  override val optionalFields: Seq[String] = Seq("arburst", "arcache", "arlen", "arlock", "arqos", "arsize") ++
    Seq("awburst", "awcache", "awlen", "awlock", "awqos", "awsize") ++
    Seq("rlast") ++
    Seq("wlast")

  override def getDefinition(interface: Axi4LiteParser.Interface): String = {
    println(s"found AXI4-Lite interface elements:\n\t${interface.fields.map(_.name).mkString("\n\t")}")
    val interfaceName = interface.interfaceName
    val definitions = interface.fields
    val addr = definitions.find(_.name.contains("addr")).get
    val addrWidth = addr.bitWidth
    val direction = if (addr.direction == "output") "master" else "slave"
    val dataWidth = definitions.find(_.name.contains("data")).get.bitWidth
    val config =
      s"val ${interfaceName}_Config = AxiLite4Config(addressWidth = $addrWidth, dataWidth = $dataWidth)"
    s"""
       |  $config
       |  val $interfaceName = $direction(AxiLite4(${interfaceName}_Config))
       |  $interfaceName.setNameForEda()
       |""".stripMargin
  }
}

object Axi4StreamParser extends InterfaceParser {
  override val identifier: String = "axis"
  override val requiredFields: Seq[String] = Seq("tvalid", "tready", "tdata")
  override val optionalFields: Seq[String] = Seq("tlast", "tkeep", "tuser", "tid", "tdest")

  override def getDefinition(interface: Axi4StreamParser.Interface): String = {
    val name = interface.interfaceName
    val definitions = interface.fields
    println(s"found AXI4-Stream interface elements:\n\t${interface.fields.map(_.name).mkString("\n\t")}")
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
}

// TODO: AXI4 parser
