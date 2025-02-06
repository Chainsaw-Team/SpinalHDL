package spinal.lib.bus.regif

import spinal.lib.bus.regif._

final case class DocPyHeader(
    name: String,
    override val prefix: String = "",
    regType: String = "u32"
) extends BusIfDoc {

  override val suffix: String = "py"

  override def body(): String = {

    def parseReg(regSlice: RegSlice): String = {

      val regName = regSlice.getName.toLowerCase
      val regAddress = s"0x%0${4}x".format(regSlice.getAddr)
      val fieldWidths = regSlice.getFields.map(_.getWidth)
      val fieldNames = regSlice.getFields.filterNot(_.getAccessType == AccessType.NA).map(_.getName)

      s"""
         |class $regName(Register32):
         |    address = $regAddress
         |    field_widths = [${fieldWidths.mkString(", ")}]
         |
         |    def __init__(self, ${fieldNames.map(name => f"$name=0").mkString(", ")}):
         |        ${fieldNames.map(name => s"self.$name = $name").mkString("\n        ")}
         |
         |""".stripMargin

    }
    val classDeclaration = s"""
                              |class Register32:
                              |    address: int
                              |    field_widths: list[int]
                              |
                              |    def to_value(self):
                              |        value = 0
                              |        shift = 0
                              |        for (field_name, width) in zip(self.__dict__.keys(), self.field_widths):
                              |            value += (getattr(self, field_name) << shift)
                              |            shift += width
                              |        return value
                              |
                              |    def from_value(self, value):
                              |        for field_name, width in zip(self.__dict__.keys(), self.field_widths):
                              |            field_value = value % (1 << width)
                              |            setattr(self, field_name, field_value)
                              |            value >>= width
                              |
                              |""".stripMargin

    classDeclaration + bi.slices.map(parseReg).mkString("")

  }
}
