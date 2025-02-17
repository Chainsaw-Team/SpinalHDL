package chainsaw.projects.xdma.daq.customizedIps

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axis.Axi4Stream._
import spinal.lib.bus.amba4.axis._

import scala.language.postfixOps

/** Configuration for the DataDelay module, defining its key parameters:
  *
  * @param delayMax     The maximum programmable delay in clock cycles.
  * @param fifoDepthMax The maximum depth of a single FIFO, when delayMax > fifoDepthMax, DataDelay will be implemented by multiple cascaded FIFOs.
  *                     Smaller fifoDepthMax will result in to higher latency but easier timing closure.
  * @param paddingValue The value output during the delay period before valid data is ready.
  * @param lowLatency   When set, latency of each FIFO = 1(otherwise 2).
  *                     This option will result in the hardware implementation using async memory, which will impact timing closure.
  *                     On FPGAs, it will also cause significant overhead in distributed memory when delayMax is large.
  * @param frameBased   When set, signal last of a frame will flush buffers and reset states.
  */
case class DataDelayConfig[T <: Data](
    hardType: HardType[T],
    delayMax: Int,
    fifoDepthMax: Int = 1 << 17,
    paddingValue: Int = 0,
    lowLatency: Boolean = false,
    frameBased: Boolean = true
) {
  assert(isPow2(fifoDepthMax), "fifoDepthMax must be a power of 2")
  val fifoDepthSum = 1 << log2Up(delayMax)
  val fifoCount = if (fifoDepthSum / fifoDepthMax == 0) 1 else fifoDepthSum / fifoDepthMax
  val fifoDepth = if (fifoCount == 1) delayMax else fifoDepthMax
  val fifoLatency = if (lowLatency) 1 else 2
  val minimumDelay =
    fifoCount * (fifoLatency + 1) // actual delay smaller than this will result in unpredictable behavior. FIXME: (fifoLatency + 1)?
  println(s"fifoCount = $fifoCount, fifoDepth = $fifoDepth, minimumDelay = $minimumDelay")
}

// TODO： delay counter -> delayDone -> Mux -> dataOut may need optimization
// TODO： registered output
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
  val dataDelayed = Stream(Fragment(Vec(hardType, 2)))
  val dataOut = master(Stream(Fragment(Vec(hardType, 2))))

  val padding = hardType()
  padding.assignFromBits(B(paddingValue, dataIn.fragment.getBitsWidth bits))

  // states
  val delayInReg = Reg(HardType(delayIn)).init(delayMax + 1)
  val delayCounter = Counter(delayMax + 2)
  when(!delayCounter.willOverflowIfInc && dataIn.fire)(delayCounter.increment())
  val delayDone = delayCounter.value >= delayInReg

  val softReset = if (frameBased) dataIn.fire && dataIn.last else False
  // datapath
  // main path
  dataDelayed.arbitrationFrom(dataIn)
  dataDelayed.last := dataIn.last
  // delay path
  val fifos = Seq.fill(fifoCount)(StreamFifo(dataIn.payloadType, depth = fifoDepth, latency = fifoLatency))
  fifos.foreach(_.io.flush := softReset)
  fifos.head.io.push.fragment := dataIn.fragment // head is special
  fifos.head.io.push.last := dataIn.last
  fifos.head.io.push.valid := dataIn.fire
  fifos.last.io.pop.ready := dataIn.fire && delayDone // tail is special
  fifos.init.zip(fifos.tail).foreach { case (prev, next) =>
    prev.io.pop >> next.io.push
  }
  // select data between main path and delay path
  dataDelayed.fragment.allowOverride()
  dataDelayed.fragment := Mux(
    delayDone,
    Vec(fifos.last.io.pop.fragment, dataIn.fragment),
    Vec(padding, dataIn.fragment)
  )

  // initialization after each frame
  when(softReset) {
    delayInReg := delayMax + 1
    delayCounter.clear()
  }
  // read delay at the start of a frame
  when(dataIn.start)(delayInReg := delayIn)

  dataDelayed.m2sPipe() >> dataOut

  // debug
  assert(delayInReg >= minimumDelay)

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
