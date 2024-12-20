package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.core.{GlobalData, SpinalError}
import spinal.lib.bus.regif._

import java.io.PrintWriter
import scala.collection.mutable
import scala.language.postfixOps

case class PythonHeaderGenerator(
    fileName: String,
    prefix: String,
    regType: String = "u32"
) extends BusIfVisitor {
  val words = "\\w*".r
  prefix match {
    case words(_*) => null
    case _         => SpinalError(s"${prefix} should be Valid naming : '[A-Za-z0-9_]+'")
  }

  case class Reg(name: String, addr: Long)

  case class Field(name: String, width: Long, accessType: AccessType)

  case class Type(name: String, var fields: List[FieldDescr])

  val regs: mutable.ListBuffer[RegDescr] = mutable.ListBuffer[RegDescr]()
  val types: mutable.ListBuffer[Type] = mutable.ListBuffer[Type]()
  var regLength: Int = 0

  def begin(busDataWidth: Int): Unit = {}

  def visit(descr: BaseDescriptor): Unit = {
    descr match {
      case descr: RegDescr => regDescrVisit(descr)
      case _               => ???
    }
  }

  private def regDescrVisit(descr: RegDescr): Unit = {
    def nameLen = descr.getName.length()

    if (nameLen > regLength)
      regLength = nameLen

    regs += descr
    types += Type(descr.getName, descr.getFieldDescrs)
  }

  def end(): Unit = {
    val pc = GlobalData.get.phaseContext
    val targetPath = s"${pc.config.targetDirectory}/${fileName}.py"
    val pw = new PrintWriter(targetPath)

    pw.write(s"""
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
         |""".stripMargin)

    regs.zip(types).foreach { case (reg, t) =>
      val regAddress = s"0x%0${4}x".format(reg.getAddr)
      val fieldWidths = t.fields.map(_.getWidth)
      val fieldOffsets = fieldWidths.scan(0)(_ + _).init
      val fieldNames = t.fields.filterNot(_.getAccessType() == AccessType.NA).map(_.getName)

      // a class supporting read/write for each reg
      // TODO: read methods for different fields of a register
      pw.write(s"""
                  |class ${t.name.toLowerCase()}(Register32):
                  |    address = ${regAddress}
                  |    field_widths = [${fieldWidths.mkString(", ")}]
                  |
                  |    def __init__(self, ${fieldNames.map(name => f"${name}=0").mkString(", ")}):
                  |        ${fieldNames.map(name => s"self.$name = $name").mkString("\n        ")}
                  |
                  |""".stripMargin)
    }

    val regByteCount: Int = types.head.fields.map(_.getWidth()).sum / 8

    pw.close()
  }
}
