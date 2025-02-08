package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axilite._
import spinal.lib.bus.amba4.axis._
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.bus.regif.AccessType._
import spinal.lib.bus.regif._

import scala.language.postfixOps

// TODO: linear fix for rx data, coefficients saved in register file

/** This module is the datapath of ChainsawDaq, implementing following functions:
  *  1. generate a pair of period & width adjustable pulse
  *  2. encapsulate data from JESD204 into frames synced with the pulse generated
  *  3. control reset
  *  4.
  */
case class ChainsawDaqDataPath(
    defaultPulseValidPoints: Int = 2000,
    defaultGaugePoints: Int = 50,
    defaultDemodulationEnabled: Int = 0
) extends Component {

  // clock and reset inputs
  val controlClk, controlRstn, dataClk, dataRstn = in Bool ()
  val controlClockDomain = ClockDomain(controlClk, controlRstn, config = DAS_CLOCK_DOMAIN_CONFIG)
  val dataClockDomain = ClockDomain(dataClk, dataRstn, config = DAS_CLOCK_DOMAIN_CONFIG, frequency = DATA_FREQUENCY)

  // AXI4-Lite interface controlling this module
  val controlInConfig: AxiLite4Config = AxiLite4Config(addressWidth = 32, dataWidth = 32)
  val controlIn = slave(AxiLite4(controlInConfig))

  // data from jesd204, earlier data in higher bits
  val dataInConfig = Axi4StreamConfig(dataWidth = 16) // TODO: translate it into vector of SInts
  val dataIn = slave(Axi4Stream(dataInConfig))

  // data to AXI-DMA, need y0 ## x0 ## y1 ## x1 for raw, r0 ## r0 ## r1 ## r1 for demodulated
  val dataOutConfig = Axi4StreamConfig(dataWidth = 8, useLast = true)
  val dataOut = master(Axi4Stream(dataOutConfig))

  controlIn.setNameForEda()
  dataIn.setNameForEda()
  dataOut.setNameForEda()

  // other outputs
  val channel0Probe, channel1Probe = out SInt (16 bits) // waveform output to ILA
  val dataOverflow = out Bool () // indicator of overflow to ILA
  val pulse0, pulse1 = out Bool () // pulse output to SMA
  val hmc7044Resetn, ad9695PowerDown, jesd204Reset = out Bool () // reset output to submodules

  // controller, mainly implemented by RegisterFile
  val controlClockingArea = new ClockingArea(controlClockDomain) {

    val userBaseAddr = 0x00000 // must be 0 when you use newReg rather than newRegAt
    val userBusIf = AxiLite4BusInterface(controlIn, SizeMapping(userBaseAddr, 0x10000))

    val versionReg = userBusIf.newRegAt(userBaseAddr, "firmware version")
    val revision = versionReg.field(UInt(8 bits), RO, resetValue = 0x00)
    val minor = versionReg.field(UInt(8 bits), RO, resetValue = 0x09)
    val major = versionReg.field(UInt(8 bits), RO, resetValue = 0x00)
    revision := 0x00
    minor := 0x09
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
    val demodulationEnabled = datapathControlReg.field(
      Bool(),
      RW,
      resetValue = defaultDemodulationEnabled,
      "when enabled, demodulated phase, instead of X & Y raw data will be transferred to DDR"
    )
    val gaugePoints =
      datapathControlReg.field(UInt(log2Up(GAUGE_POINTS_MAX + 1) bits), RW, defaultGaugePoints, "gauge length / 0.4m")

    // pulse generation control
    val pulsePeriodReg = userBusIf.newReg("pulse period / 4ns, determined by interrogation rate rate")
    val pulseLengthReg = userBusIf.newReg("fiber length / 0.4m")
    val pulseWidthReg = userBusIf.newReg("pulse width / 4ns")
    val pulseWidthDelayReg = userBusIf.newReg("pulse0 -> pulse1 duration / 4ns")
    val preTriggerLengthReg = userBusIf.newReg("pre-trigger duration / 4ns")
    val postTriggerLengthReg = userBusIf.newReg("post-trigger duration / 4ns")

    val pulsePeriod = pulsePeriodReg.field(UInt(32 bits), RW, resetValue = defaultPulseValidPoints * 2)
    val pulseLength = pulseLengthReg.field(UInt(32 bits), RW, resetValue = defaultPulseValidPoints)
    val pulseWidth = pulseWidthReg.field(UInt(32 bits), RW, 10)
    val pulseWidthDelay = pulseWidthDelayReg.field(UInt(32 bits), RW, 10)
    val preTriggerLength = preTriggerLengthReg.field(UInt(32 bits), RW) // FIXME: not used
    val postTriggerLength = postTriggerLengthReg.field(UInt(32 bits), RW, 0)

    // document and header file generation
    userBusIf.accept(DocHtml("UserRegisterSpace"))
    userBusIf.accept(DocCHeader("UserRegisterSpace"))
    userBusIf.accept(DocPyHeader("UserRegisterSpace"))

  }

  // reset connection
  hmc7044Resetn := !controlClockingArea.hmc7044Reset
  ad9695PowerDown := controlClockingArea.ad9695Reset
  jesd204Reset := controlClockingArea.jesd204Reset

  private def getControlData[T <: Data](ctrl: T) = { // control clock domain -> data clock domain
    ctrl.addTag(crossClockDomain)
    Delay(ctrl, 3)
  }

  val dataClockingArea = new ClockingArea(dataClockDomain) {

    import controlClockingArea._

    //////////
    // pulse generation datapath
    //////////
    val counterForPulsePeriod = CounterFreeRun(PULSE_PERIOD_POINTS_MAX)
    when(counterForPulsePeriod.value === getControlData(pulsePeriod))(counterForPulsePeriod.clear())

    def getDuration(start: UInt, end: UInt) = {
      val durationStart = counterForPulsePeriod.value === RegNext(start).resized
      val durationEnd = counterForPulsePeriod.value === RegNext(end).resized
      val duration = RegInit(False)
      when(RegNext(durationStart).rise())(duration.set())
      when(RegNext(durationEnd).rise())(duration.clear())
      (duration, RegNext(durationEnd))
    }

    val (pulse0Valid, _) = getDuration(U(0, 32 bits), getControlData(pulseWidth))
    val (pulse1Valid, _) =
      getDuration(getControlData(pulseWidthDelay), getControlData(pulseWidthDelay) + getControlData(pulseWidth))
    val (dataValid, dataLast) = // this duration defines which part of each period we should upload to the host
      getDuration(getControlData(postTriggerLength), getControlData(postTriggerLength) + getControlData(pulseLength))

    pulse0 := pulse0Valid
    pulse1 := pulse1Valid

    //////////
    // data bypass / demodulation datapath
    //////////
    // remap rx data -> int16 segments
    def mapper(bitsIn: Bits, base: Int) = { // earlier data in higher bits (63:48 is segment0, 47:32 is segment1, etc.)
      val elements = (0 until 4).map { i =>
        val baseHigher = base + i * 8
        val baseLower = base + (i + 4) * 8
        println(s"bits range = ${baseHigher + 7}:$baseHigher, ${baseLower + 7}:$baseLower")
        val all = bitsIn(baseHigher + 7 downto baseHigher) ## bitsIn(baseLower + 7 downto baseLower)
        val eventBits = all.takeLow(2) // TODO: record the function of event bits here
        (all.takeHigh(14) ## B("00")).asSInt
      }.reverse
      Vec(elements) // earlier data in lower index
    }

    // dataIn -> StreamRaw -> StreamDemodulated -> StreamWithHeader
    val channelX: Vec[SInt] = mapper(dataIn.payload.data, 0) // x0, x1, x2, x3
    val channelY: Vec[SInt] = mapper(dataIn.payload.data, 64) // y0, y1, y2, y3
    val Seq(x0, x1, y0, y1) = Seq(channelX(0), channelX(2), channelY(0), channelY(2)) // channel 0 & 1 1GHz -> 500MHz

    // sample dataIn according to the pulse generation parameters
    val streamRaw = Stream(Fragment(Vec(SInt(16 bits), 4)))
    streamRaw.valid := dataValid
    streamRaw.last := dataLast
    streamRaw.fragment := Vec(x0, x1, y0, y1)
    // streamRaw doesn't back pressure dataIn, theoretically, when downstream is not ready, overflow may happen
    dataIn.ready.set()

    val daqDemodulator = DasDemodulator()
    daqDemodulator.demodulationEnabled := getControlData(demodulationEnabled)
    daqDemodulator.gaugePointsIn := getControlData(gaugePoints).resized
    daqDemodulator.pulseValidPointsIn := getControlData(pulseLength).resized
    streamRaw >> daqDemodulator.streamIn
    val streamDemodulated = daqDemodulator.streamOut.translateFragmentWith(daqDemodulator.streamOut.fragment.asBits)
//    val streamDemodulated = streamRaw.translateFragmentWith(Vec(x1, y1, x0, y0).asBits) // 直接连接采集结果和st2mm

    // buffer between free-running & standard stream interface, should never be fully occupied
    val streamBuffered = streamDemodulated.queue(1024)
    // // header insertion after buffer as this may stall upstream
    val packetIdWidth = 16 // periodic packet id for data sync
    val counterForPacket = Counter(1 << packetIdWidth, inc = streamDemodulated.fire && streamDemodulated.last)
    val header = B(0, (64 - packetIdWidth) bits) ## counterForPacket
    val streamWithHeader = streamBuffered.insertHeader(header)
    dataOut.fromStreamFragment(streamWithHeader)

    // debug
    channel0Probe := channelX.head // for ILA
    channel1Probe := channelY.head
    dataOverflow := !streamDemodulated.ready

  }

}
