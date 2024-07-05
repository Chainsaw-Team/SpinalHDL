package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.blackbox.xilinx.ultrascale.IOBUF
import spinal.lib.bus.amba4.axilite.AxiLite4.resp._
import spinal.lib.bus.amba4.axilite._
import spinal.lib.bus.amba4.axilite.sim._
import spinal.lib.fsm._

import scala.language.postfixOps

/** a controller converting AXI4-Lite read/write events into ADI device read/write events through serial port interface for ADI chips
  * @param dividerRatio  default clock frequency / sclk frequency
  * @param useInOut use sdio = inout(Analog(Bool())) instead of sdi & sdo
  * @see [[https://wiki.analog.com/_media/resources/technical-guides/adispi_rev_1p0_customer.pdf]]
  */
case class AdiSpiCtrl(dividerRatio: Int, useInOut: Boolean = true) extends Component {

  // a AXI4-Lite interface compatible with XMDA user channel
  val axiLite4Config = AxiLite4Config(addressWidth = 32, dataWidth = 32)
  val user = slave(AxiLite4(axiLite4Config))
  user.setNameForEda()

  assert(dividerRatio % 2 == 0, "dividerRatio should be even")

  val INSTRUCTION_WIDTH = 24
  val DATA_WIDTH = 8
  val INFO_WIDTH = INSTRUCTION_WIDTH - DATA_WIDTH // read/write + multibyte + address

  // SPI interface
  val sclk = out Bool ()
  val csb = out Bool () // enable, active-low
  val sdi = Bool()
  val sdo = Bool()
  val sdReadEnable = Bool() // for 4-wire -> 3-wire conversion

  val sdio = useInOut generate inout(Analog(Bool()))
  if (useInOut) {
    sdio <> IOBUF.io2to1(sdo, sdi, sdReadEnable)
  } else {
    in(sdi)
    out(sdo, sdReadEnable)
  }

  val sdWriteEnable = Bool()
  sdReadEnable := !sdWriteEnable

  // generate sclk using a divider
  val sclkGen = CounterFreeRun(dividerRatio / 2)
  val sclkReg = RegInit(False)
  sclkReg.toggleWhen(sclkGen.willOverflow)
  sclk := sclkReg

  val stateCounter = Counter(INSTRUCTION_WIDTH)
  val instructionReg = Reg(Bits(INSTRUCTION_WIDTH bits))
  instructionReg.init(0x000000)

  // pre-assignment
  user.aw.ready.clear()
  user.w.ready.clear()
  user.ar.ready.clear()
  user.r.valid.clear()
  user.r.payload.data.clearAll()
  user.r.resp := OKAY
  user.b.valid.clear()
  user.b.resp := OKAY
  csb.set()
  sdo.clear()
  sdWriteEnable.clear()

  val arDone, rDone, awDone, wDone, bDone = RegInit(False)

  def atFallingEdge(statement: => Unit) = when(sclkReg.fall())(statement)
  def atRisingEdge(statement: => Unit) = when(sclkReg.fall())(statement)

  // debug
  val instruction_probe = out cloneOf instructionReg
  instruction_probe := instructionReg

  // state machine
  val ctrlFsm = new StateMachine {
    val IDLE = makeInstantEntry()
    val GET_WRITE_ADDR, GET_WRITE_DATA, WRITE, RESP = new State()
    val GET_READ_ADDR, WRITE_INFO, READ_DATA, SET_READ_DATA = new State()
    val WAIT = new State()

    // state transition logic
    IDLE.whenIsActive(
      when(user.aw.valid)(goto(GET_WRITE_ADDR)) // start write sequence
        .elsewhen(user.ar.valid)(goto(GET_READ_ADDR)) // start read sequence
    )

    GET_WRITE_ADDR.whenIsActive(when(awDone)(goto(GET_WRITE_DATA)))
    GET_WRITE_DATA.whenIsActive(atFallingEdge(when(wDone)(goto(WRITE))))
    WRITE.whenIsActive(atFallingEdge(when(stateCounter.value === INSTRUCTION_WIDTH - 1)(goto(RESP))))
    RESP.whenIsActive(when(bDone)(goto(WAIT)))

    GET_READ_ADDR.whenIsActive(atFallingEdge(when(arDone)(goto(WRITE_INFO))))
    WRITE_INFO.whenIsActive(atFallingEdge(when(stateCounter.value === INFO_WIDTH - 1)(goto(READ_DATA))))
    READ_DATA.whenIsActive(atFallingEdge(when(stateCounter.value === DATA_WIDTH - 1)(goto(SET_READ_DATA))))
    SET_READ_DATA.whenIsActive(when(rDone)(goto(WAIT)))

    WAIT.whenIsActive(atFallingEdge(when(stateCounter.value === 2)(goto(IDLE)))) // wait for 3 sclk cycle

    // workload: updating IO, instructionReg, and stateCounter
    GET_WRITE_ADDR.whenIsActive { // get address from AXI4-Lite interface
      instructionReg.msb.clear() // 0 for write
      when(user.aw.valid && !awDone) { // wait for user.aw.valid
        instructionReg(INSTRUCTION_WIDTH - 1 - 1 downto DATA_WIDTH) := user.aw.payload.addr.takeLow(INFO_WIDTH - 1)
        user.aw.ready.set()
        awDone.set()
      }
    }
    GET_WRITE_ADDR.onExit(awDone.clear())

    GET_WRITE_DATA.whenIsActive { // get data from AXI4-Lite interface
      when(user.w.valid && !wDone) { // wait for user.w.valid
        instructionReg(7 downto 0) := MuxOH(
          user.w.strb.asBools,
          user.w.payload.data.subdivideIn(4 slices)
        ) // 32 bit alignment
        user.w.ready.set()
        wDone.set()
      }
    }
    GET_WRITE_DATA.onExit(wDone.clear())

    WRITE.whenIsActive {
      sdWriteEnable.set()
      sdo := instructionReg.msb
      csb.clear() // enable HMC7044
      atFallingEdge {
        when(stateCounter.value === INSTRUCTION_WIDTH - 1) {
          stateCounter.clear()
        }.otherwise {
          stateCounter.increment()
        }
      }
      atRisingEdge(instructionReg := instructionReg.rotateLeft(1))
    }

    RESP.whenIsActive {
      user.b.valid := !bDone // keep valid until response done
      when(user.b.fire) {
        bDone.set()
      }
    }
    RESP.onExit(bDone.clear())

    GET_READ_ADDR.whenIsActive {
      instructionReg.msb.set() // 1 for read
      when(user.ar.valid && !arDone) { // wait for user.ar.valid
        instructionReg(INSTRUCTION_WIDTH - 1 - 1 downto DATA_WIDTH) := user.ar.payload.addr.takeLow(INFO_WIDTH - 1)
        user.ar.ready.set()
        arDone.set()
      }
    }
    GET_READ_ADDR.onExit(arDone.clear())

    WRITE_INFO.whenIsActive {
      sdWriteEnable.set()
      sdo := instructionReg.msb
      csb.clear()
      atFallingEdge {
        when(stateCounter.value === INFO_WIDTH - 1) {
          stateCounter.clear()
        }.otherwise {
          stateCounter.increment()
        }
      }
      atRisingEdge(instructionReg := instructionReg.rotateLeft(1))
    }

    READ_DATA.whenIsActive {
      csb.clear()
      instructionReg.msb := sdi
      atFallingEdge {

        when(stateCounter.value === DATA_WIDTH - 1) {
          stateCounter.clear()
        }.otherwise {
          stateCounter.increment()
        }
      }
      atRisingEdge(instructionReg := instructionReg.rotateLeft(1))
    }

    SET_READ_DATA.whenIsActive {
      switch(instructionReg(9 downto 8)) { // 32 bit alignment
        is(B"00")(user.r.payload.data(7 downto 0) := instructionReg(7 downto 0))
        is(B"01")(user.r.payload.data(15 downto 8) := instructionReg(7 downto 0))
        is(B"10")(user.r.payload.data(23 downto 16) := instructionReg(7 downto 0))
        is(B"11")(user.r.payload.data(31 downto 24) := instructionReg(7 downto 0))
      }
      user.r.valid := !rDone // keep valid until read done
      when(user.r.fire) {
        rDone.set()
      }
    }
    SET_READ_DATA.onExit(rDone.clear())

    WAIT.whenIsActive {
      atFallingEdge(stateCounter.increment())
    }
    WAIT.onExit(stateCounter.clear())
  }

}

object AdiSpiCtrl extends App {

  SimConfig.withFstWave
    .withConfig(
      SpinalConfig(defaultConfigForClockDomains = ClockDomainConfig(resetActiveLevel = LOW, resetKind = SYNC))
    )
    .compile(AdiSpiCtrl(50))
    .doSim { dut =>
      val userDriver = AxiLite4Driver(dut.user, dut.clockDomain)
      dut.clockDomain.forkStimulus(2)
      userDriver.write(0x000020c9, 0x000000f0) //  simulate until a write event is done
      userDriver.read(0x000020c9) // simulate until a read event is done
    }
}
