package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axilite._
import spinal.lib.bus.amba4.axis._
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.bus.regif.AccessType._
import spinal.lib.bus.regif._

import scala.language.postfixOps

// TODO: pulse generation and frame builder
case class ChainsawDaqDataPath() extends Component {

  // clock and reset inputs
  val controlClk, controlRstn, dataClk, dataRstn = in Bool ()

  // AXI4-Lite interface controlling this module
  val controlInConfig = AxiLite4Config(addressWidth = 32, dataWidth = 32)
  val controlIn = slave(AxiLite4(controlInConfig))
  controlIn.setNameForEda()

  // data from jesd204
  val dataInConfig = Axi4StreamConfig(dataWidth = 16)
  val dataIn = slave(Axi4Stream(dataInConfig))
  dataIn.setNameForEda()

  // data to AXI-DMA
//  val dataOutConfig = Axi4StreamConfig(dataWidth = 128)
//  val dataOut = master(Axi4Stream(dataOutConfig))
  // other outputs
  val channel1, channel2 = out Bits (14 bits) // output to ILA
//  val pulse1, pulse2 = out Bool () // pulse output to SMA
  val hmc7044Resetn, ad9695PowerDown, jesd204Reset = out Bool () // reset output to submodules

  // pre-assignment
  dataIn.ready.set()

  // creating clock domains
  val controlClockDomain =
    ClockDomain(
      clock = controlClk,
      reset = controlRstn,
      config = ClockDomainConfig(resetKind = SYNC, resetActiveLevel = LOW)
    )

  val dataClockDomain = new ClockDomain(
    clock = dataClk,
    reset = dataRstn,
    config = ClockDomainConfig(resetKind = SYNC, resetActiveLevel = LOW),
    frequency = FixedFrequency(250 MHz)
  )

  val controlClockingArea = new ClockingArea(controlClockDomain) {

    val userBaseAddr = 0x00000
    val userBusIf = AxiLite4BusInterface(controlIn, SizeMapping(userBaseAddr, 0x10000))

    val versionReg = userBusIf.newRegAt(userBaseAddr, "firmware version")
    val revision = versionReg.field(UInt(8 bits), RO)
    val minor = versionReg.field(UInt(8 bits), RO)
    val major = versionReg.field(UInt(8 bits), RO)
    revision := 0x00
    minor := 0x01
    major := 0x00

    val jesd204ResetReg = userBusIf.newRegAt(userBaseAddr + 4, "reset JESD204")
    val jesd204Reset = jesd204ResetReg.field(Bool(), RW, 1, "reset JESD204, asserted by default")
    val hmc7044ResetReg = userBusIf.newRegAt(userBaseAddr + 8, "reset HMC7044")
    val hmc7044Reset = hmc7044ResetReg.field(Bool(), RW, 1, "reset HMC7044, asserted by default")
    val ad9695ResetReg = userBusIf.newRegAt(userBaseAddr + 12, "reset AD9695")
    val ad9695Reset = ad9695ResetReg.field(Bool(), RW, 1, "reset ad9695, asserted by default")

    val rwTestReg = userBusIf.newRegAt(userBaseAddr + 16, "reserved RW field for testing AXI4-Lite read/write")
    val rwTest = rwTestReg.field(Bits(32 bits), RW)

    val pulsePointsReg = userBusIf.newRegAt(userBaseAddr + 20, "pts/4 in single pulse")
    val pulsePoints = pulsePointsReg.field(UInt(32 bits), RW, "pts/4 in single pulse")

    userBusIf.accept(HtmlGenerator("UserRegisterSpace", "AP"))
    userBusIf.accept(CHeaderGenerator("UserRegisterSpace", "AP"))
    userBusIf.accept(PythonHeaderGenerator("UserRegisterSpace", "AP"))

  }

  // reset connection
  hmc7044Resetn := !controlClockingArea.hmc7044Reset
  ad9695PowerDown := controlClockingArea.ad9695Reset
  jesd204Reset := controlClockingArea.jesd204Reset

  val dataClockingArea = new ClockingArea(dataClockDomain) {

    // reorder rx to get meaningful data
    def mapper(bitsIn: Bits, base: Int) = {
      (0 until 4).map { i =>
        val baseHigher = base + i * 8
        val baseLower = base + (i + 4) * 8
        val all = bitsIn(baseHigher + 7 downto baseHigher) ## bitsIn(baseLower + 7 downto baseLower)
        val controlBits = all.takeLow(2)
        all.takeHigh(14)
      }
    }

    channel1 := mapper(dataIn.payload.data, 0)(0)
    channel2 := mapper(dataIn.payload.data, 64)(0)

    // pulse generator

    // frame builder

  }

}
