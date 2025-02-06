package chainsaw.projects.xdma.daq.customizedIps

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axis.Axi4Stream._
import spinal.lib.bus.amba4.axis._

import scala.language.postfixOps

/** Configuration for the DataDelay module, defining its key parameters:
  *
  * @param hardType
  * @param delayMax     The maximum programmable delay in clock cycles.
  * @param paddingValue The value output during the delay period before valid data is ready.
  * @param lowLatency   When set, minimum delay = 1(otherwise 2). This option will result in the hardware implementation using async memory, which will lead to tighter timing constraints. On FPGAs, it will also cause significant overhead in distributed memory when delayMax is large.
  */
case class DataDelayConfig[T <: Data](
    hardType: HardType[T],
    delayMax: Int,
    paddingValue: Int = 0,
    lowLatency: Boolean = false
)

// FIXME： counter -> delayDone -> Mux -> dataOut may lead to bad critical path
/** The DataDelay module introduces a configurable delay to streaming data, with support for AXI4-Stream interfaces.
  * It delays the input data by a programmable number of clock cycles, defined by the `delayIn` signal,
  * input/output data share tvalid & tlast signal.
  *
  * Ports:
  * - **delayIn**: Input specifying the delay in clock cycles, sampled at the first cycle of each frame.
  * - **dataIn**: AXI4-Stream input carrying the data to be delayed.
  * - **dataOut**: A vector of delayed data and the input data, delayed data go first.
  *
  * @example
  * For a single frame of input data with `delayIn = 3` and `paddingValue = 0`,* stands for invalid data:
  * Input:  [A, *, B, *, C, D, E, F, Last]
  * Delayed: [0, *, 0, *, 0, A, B, C, Last]
  */
case class DataDelay[T <: Data](config: DataDelayConfig[T]) extends Module {

  import config._

  // I/O
  val delayIn = in UInt (log2Up(delayMax + 2) bits)
  val dataIn = slave(Stream(Fragment(hardType)))
  val dataOut = master(Stream(Fragment(Vec(hardType, 2))))

  val padding = hardType()
  padding.assignFromBits(B(paddingValue, dataIn.fragment.getBitsWidth bits))

  // states
  val delayInReg = Reg(HardType(delayIn)).init(delayMax + 1)
  val delayCounter = Counter(delayMax + 2)
  when(!delayCounter.willOverflowIfInc && dataIn.fire)(delayCounter.increment())
  val delayDone = delayCounter.value >= delayInReg

  // datapath
  // main path
  dataOut.arbitrationFrom(dataIn)
  dataOut.last := dataIn.last
  // delay path
  val minimumDelay = if (lowLatency) 1 else 2
  val fifo = StreamFifo(dataIn.payloadType, depth = delayMax, latency = minimumDelay)
  fifo.io.push.fragment := dataIn.fragment
  fifo.io.push.last := dataIn.last
  fifo.io.push.valid := dataIn.fire
  fifo.io.pop.ready := dataIn.fire && delayDone

  dataOut.fragment.allowOverride()
  dataOut.fragment := Mux(
    delayDone,
    Vec(fifo.io.pop.fragment, dataIn.fragment),
    Vec(padding, dataIn.fragment)
  )

  // initialization after each frame
  fifo.io.flush := dataIn.fire && dataIn.last
  when(dataIn.fire && dataIn.last) {
    delayInReg := delayMax + 1
    delayCounter.clear()
  }
  // read delay at the start of a frame
  when(dataIn.start)(delayInReg := delayIn)

  assert(delayInReg >= minimumDelay) // behavior is unpredictable when delay = 0

}

object DataDelay {
  def getFixedDelayed[T <: Data](stream: Stream[Fragment[T]], fixedDelay: Int) = {
    val dataDelay = DataDelay(DataDelayConfig(HardType(stream.fragment), fixedDelay))
    stream >> dataDelay.dataIn
    dataDelay.delayIn := U(fixedDelay)
    dataDelay.dataOut
  }
  def getDelayed[T <: Data](stream: Stream[Fragment[T]], delayMax: Int, delay: UInt) = {
    val dataDelay = DataDelay(DataDelayConfig(HardType(stream.fragment), delayMax))
    stream >> dataDelay.dataIn
    dataDelay.delayIn := delay
    dataDelay.dataOut
  }
}
