package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.amba4.axilite._
import spinal.lib.bus.amba4.axis._
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.bus.regif.AccessType._
import spinal.lib.bus.regif._

import scala.language.postfixOps

/** This module is the datapath of ChainsawDaq, implementing following functions:
 *  1. generate a pair of period & width adjustable pulse
 *  2. encapsulate data from JESD204 into frames synced with the pulse generated
 *  3. control reset
 *  4.
 *
 */
case class ChainsawDaqDataPath() extends Component {

  // clock and reset inputs
  val controlClk, controlRstn, dataClk, dataRstn = in Bool ()

  // AXI4-Lite interface controlling this module
  val controlInConfig: AxiLite4Config = AxiLite4Config(addressWidth = 32, dataWidth = 32)
  val controlIn = slave(AxiLite4(controlInConfig))

  // data from jesd204
  val dataInConfig = Axi4StreamConfig(dataWidth = 16)
  val dataIn = slave(Axi4Stream(dataInConfig))

  // data to AXI-DMA
  val dataOutConfig = Axi4StreamConfig(dataWidth = 8, useLast = true)
  val dataOut = master(Axi4Stream(dataOutConfig))

  // other outputs
  val channel0Probe, channel1Probe = out SInt (16 bits) // waveform output to ILA
  val dataOverflow = out Bool () // indicator of overflow to ILA
  val pulse0, pulse1 = out Bool () // pulse output to SMA
  val hmc7044Resetn, ad9695PowerDown, jesd204Reset = out Bool () // reset output to submodules

  controlIn.setNameForEda()
  dataIn.setNameForEda()
  dataOut.setNameForEda()

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

    val userBaseAddr = 0x00000 // must be 0 when you use newReg rather than newRegAt
    val userBusIf = AxiLite4BusInterface(controlIn, SizeMapping(userBaseAddr, 0x10000))

    val versionReg = userBusIf.newRegAt(userBaseAddr, "firmware version")
    val revision = versionReg.field(UInt(8 bits), RO)
    val minor = versionReg.field(UInt(8 bits), RO)
    val major = versionReg.field(UInt(8 bits), RO)
    revision := 0x00
    minor := 0x01
    major := 0x00

    // reset control
    val jesd204ResetReg = userBusIf.newReg("reset JESD204")
    val jesd204Reset = jesd204ResetReg.field(Bool(), RW, 1, "reset JESD204, asserted by default")
    val hmc7044ResetReg = userBusIf.newReg("reset HMC7044")
    val hmc7044Reset = hmc7044ResetReg.field(Bool(), RW, 1, "reset HMC7044, asserted by default")
    val ad9695ResetReg = userBusIf.newReg("reset AD9695")
    val ad9695Reset = ad9695ResetReg.field(Bool(), RW, 1, "reset ad9695, asserted by default")

    // test control
    val rwTestReg = userBusIf.newReg("reserved RW field for testing AXI4-Lite read/write")
    val rwTest = rwTestReg.field(Bits(32 bits), RW)

    // datapath control
    val datapathControlReg = userBusIf.newReg("control mux in datapath")
    val testMode =
      datapathControlReg.field(Bool(), RW, 1, "incrementing number will be generated by datapath when turned on")
    val channelSelection = datapathControlReg.field(UInt(1 bits), RW, "select channel 0/1 as data source")

    // pulse generation control
    val pulsePeriodReg = userBusIf.newReg("pulse period / 4ns")
    val pulseLengthReg = userBusIf.newReg("pulse length / 4ns")
    val pulseWidthReg = userBusIf.newReg("pulse width / 4ns")
    val pulseWidthDelayReg = userBusIf.newReg("pulse0 -> pulse1 duration / 4ns")
    val preTriggerLengthReg = userBusIf.newReg("pre-trigger duration / 4ns")
    val postTriggerLengthReg = userBusIf.newReg("post-trigger duration / 4ns")
    val decimationReg = userBusIf.newReg("decimation factor, 1-16")

    val pulsePeriod = pulsePeriodReg.field(UInt(32 bits), RW, 2048)
    val pulseLength = pulseLengthReg.field(UInt(32 bits), RW, 1023)
    val pulseWidth = pulseWidthReg.field(UInt(32 bits), RW, 10)
    val pulseWidthDelay = pulseWidthDelayReg.field(UInt(32 bits), RW, 10)
    val preTriggerLength = preTriggerLengthReg.field(UInt(32 bits), RW)
    val postTriggerLength = postTriggerLengthReg.field(UInt(32 bits), RW, 0)
    val decimation = decimationReg.field(UInt(4 bits), RW, 1)

    userBusIf.accept(HtmlGenerator("UserRegisterSpace", "AP"))
    userBusIf.accept(CHeaderGenerator("UserRegisterSpace", "AP"))
    userBusIf.accept(PythonHeaderGenerator("UserRegisterSpace", "AP"))

  }

  // reset connection
  hmc7044Resetn := !controlClockingArea.hmc7044Reset
  ad9695PowerDown := controlClockingArea.ad9695Reset
  jesd204Reset := controlClockingArea.jesd204Reset


  def getControlData[T <: Data](ctrl: T) = { // control clock domain -> data clock domain
    ctrl.addTag(crossClockDomain)
    Delay(ctrl, 3)
  }

  val dataClockingArea = new ClockingArea(dataClockDomain) {
    // datapath parameters
    val periodCounterWidth = 20
    val packetIdWidth = 16

    println(
      f"counter width = $periodCounterWidth, " +
        f"max period = ${(1 << periodCounterWidth) * 4} ns, " +
        f"min frequency = ${1.0 / ((1 << periodCounterWidth) * 4).toDouble * 1e9} Hz"
    )

    import controlClockingArea._

    // manipulating rx data
    // remap rx data -> int16 segments
    def mapper(bitsIn: Bits, base: Int) = { // earlier data in higher bits (63:48 is segment0, 47:32 is segment1, etc.)
      (0 until 4)
        .map { i =>
          val baseHigher = base + i * 8
          val baseLower = base + (i + 4) * 8
          val all = bitsIn(baseHigher + 7 downto baseHigher) ## bitsIn(baseLower + 7 downto baseLower)
          val controlBits = all.takeLow(2)
          all.takeHigh(14) ## B("00")
        }
        .reverse
        .reduce(_ ## _)
    }

    val channel0Segments: Bits = mapper(dataIn.payload.data, 0)

    // a pipelined calculating circuit example:
    //    val original = mapper(dataIn.payload.data, 0)
    //    val step0Result = original.subdivideIn(16 bits).map(_.asSInt).map(sint => RegNext(sint * 2)).reduce(_ @@ _) // step0
    //    val channel0Segments = step0Result

    val channel1Segments: Bits = mapper(dataIn.payload.data, 64)
    channel0Probe assignFromBits channel0Segments.takeLow(16)
    channel1Probe assignFromBits channel1Segments.takeLow(16)

    // TODO: linear fix for rx data, coefficients saved in register file

    // generate pulses and a sampling window
    val counterForPulsePeriod = CounterFreeRun(1 << periodCounterWidth)
    when(counterForPulsePeriod.value === getControlData(pulsePeriod))(counterForPulsePeriod.clear())

    def getDuration(start: UInt, end: UInt) = {
      val durationStart = counterForPulsePeriod.value === RegNext(start.resize(periodCounterWidth))
      val durationEnd = counterForPulsePeriod.value === RegNext(end.resize(periodCounterWidth))
      val duration = RegInit(False)
      when(RegNext(durationStart).rise())(duration.set())
      when(RegNext(durationEnd).rise())(duration.clear())
      (duration, RegNext(durationEnd))
    }

    val (pulse0Valid, _) = getDuration(U(0, 32 bits), getControlData(pulseWidth))
    val (pulse1Valid, _) =
      getDuration(getControlData(pulseWidthDelay), getControlData(pulseWidthDelay) + getControlData(pulseWidth))
    val (dataValid, dataLast) =
      getDuration(getControlData(postTriggerLength), getControlData(postTriggerLength) + getControlData(pulseLength))

    pulse0 := pulse0Valid
    pulse1 := pulse1Valid

    // test data source
    val counterForTest = Counter(1 << 14)
    when(!dataValid)(counterForTest.clear())
    when(dataValid)(counterForTest.increment())

    //  FIXME: for correct decimation, rx -> segments mapping should be modified
    val counterForDecimation = CounterFreeRun(16)
    when(counterForDecimation.value === (getControlData(decimation) - 1) || !dataValid)(counterForDecimation.clear())
    val decimationValid = counterForDecimation.value === counterForDecimation.value.getZero

    val counterForPacket = Counter(1 << packetIdWidth, inc = dataLast)

    // sampled data as a free-running stream
    val streamRaw = Stream(Fragment(Bits(64 bits)))
    streamRaw.valid := dataValid && decimationValid
    streamRaw.last := dataLast
    dataOverflow := streamRaw.valid && !streamRaw.ready

    when(getControlData(controlClockingArea.testMode)) { // test data -> AXI DMA
      (0 until 4).foreach(i => streamRaw.fragment(i * 16, 16 bits) := (counterForTest.value @@ U(i, 2 bits)).asBits)
    }.otherwise { // JESD204 -> AXI DMA
      streamRaw.fragment := Mux(getControlData(channelSelection.asBool), channel1Segments, channel0Segments)
    }

    // header insertion & buffering
    val header = B(0, (64 - packetIdWidth) bits) ## counterForPacket
    val streamBuffered = streamRaw.queue(1024) // buffer between free-running & standard stream interface
    val streamWithHeader = streamBuffered.insertHeader(header) // inserted after buffer as this may stall upstream

    // TODO: method driving AXI4-Stream with Stream[Bits] or Stream[Fragment[Bits]]
    dataOut.valid := streamWithHeader.valid
    dataOut.payload.last := streamWithHeader.last
    dataOut.payload.data := streamWithHeader.fragment
    streamWithHeader.ready := dataOut.ready

  }

}

object ChainsawDaqDataPath {
  def main(args: Array[String]): Unit = {
    SpinalSimConfig().withWave.compile(ChainsawDaqDataPath()).doSim { dut =>
      dut.dataOut.ready #= true
      dut.controlClockDomain.forkStimulus(10)
      dut.dataClockDomain.forkStimulus(2)
      dut.dataClockDomain.waitSampling(20000)
    }
  }
}
