package chainsaw.projects.xdma.daq.customizedIps

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axis.Axi4Stream._
import spinal.lib.bus.amba4.axis._

import scala.language.postfixOps

/** Configuration for the DataDelay module, defining its key parameters:
  *
  * @param dataWidth     The width of the input data in bits.
  * @param delayMax      The maximum programmable delay in clock cycles.
  * @param paddingValue  The value output during the delay period before valid data is ready.
  */
case class DataDelayConfig(dataWidth: Int, delayMax: Int, paddingValue: Int = 0)

// FIXME： counter -> delayDone -> Mux -> dataOut may lead to bad critical path
/** The DataDelay module introduces a configurable delay to streaming data, with support for AXI4-Stream interfaces.
  * It delays the input data by a programmable number of clock cycles, defined by the `delayIn` signal,
  * input/output data share tvalid & tlast signal.
  *
  * Ports:
  * - **delayIn**: Input specifying the delay in clock cycles, sampled at the first cycle of each frame.
  * - **dataIn**: AXI4-Stream input carrying the data to be delayed.
  * - **dataOut**: Delayed data and the input data, delayed data at the higher bits.
  *
  * @example
  * For a single frame of input data with `delayIn = 3` and `paddingValue = 0`,* stands for invalid data:
  * Input:  [A, *, B, *, C, D, E, F, Last]
  * Delayed: [0, *, 0, *, 0, A, B, C, Last]
  */
case class DataDelay(delayConfig: DataDelayConfig) extends Module {

  import delayConfig._

  // I/O
  val delayIn = in UInt (log2Up(delayMax + 2) bits)
  val dataIn = slave(Stream(Fragment(Bits(dataWidth bits))))
  val dataOut = master(Stream(Fragment(Bits(dataWidth * 2 bits))))

  // states
  val delayInReg = RegInit(delayIn)
  val delayCounter = Counter(delayMax + 2)
  when(!delayCounter.willOverflowIfInc && dataIn.fire)(delayCounter.increment())
  val delayDone = delayCounter.value >= delayInReg

  // datapath
  // main path
  dataOut.arbitrationFrom(dataIn)
  dataOut.last := dataIn.last
  // delay path
  val fifo = StreamFifo(dataIn.payloadType, depth = delayMax, latency = 0)
  fifo.io.push.fragment := dataIn.fragment
  fifo.io.push.last := dataIn.last
  fifo.io.push.valid := dataIn.fire
  fifo.io.pop.ready := dataIn.fire && delayDone
  dataOut.fragment.allowOverride()
  dataOut.fragment := Mux(
    delayDone,
    fifo.io.pop.fragment ## dataIn.fragment,
    B(paddingValue, dataWidth bits) ## dataIn.fragment
  )

  // initialization after each frame
  fifo.io.flush := dataIn.fire && dataIn.last
  when(dataIn.fire && dataIn.last) {
    delayInReg := delayMax + 1
    delayCounter.clear()
  }
  // read delay a the start of a frame
  when(dataIn.start)(delayInReg := delayIn)

  assert(delayInReg > 0) // behavior is unpredictable when delay = 0

}

object DataDelay {
  def getFixedDelayed(stream: Stream[Fragment[Bits]], fixedDelay: Int) = {
    val dataDelay = DataDelay(DataDelayConfig(stream.fragment.getBitsWidth, fixedDelay))
    stream >> dataDelay.dataIn
    dataDelay.delayIn := U(fixedDelay)
    dataDelay.dataOut
  }
  def getDelayed(stream: Stream[Fragment[Bits]], delayMax: Int, delay: UInt) = {
    val dataDelay = DataDelay(DataDelayConfig(stream.fragment.getBitsWidth, delayMax))
    stream >> dataDelay.dataIn
    dataDelay.delayIn := delay
    dataDelay.dataOut
  }
}
