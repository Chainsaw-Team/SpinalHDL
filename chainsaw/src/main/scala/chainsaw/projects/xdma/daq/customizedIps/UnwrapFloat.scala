package chainsaw.projects.xdma.daq.customizedIps

import chainsaw.projects.xdma.daq.ku060Ips.{FloatAcc, FloatAdd, FloatSub}
import chainsaw.projects.xdma.daq._
import spinal.core._
import spinal.lib._
import spinal.lib.StreamJoin
import spinal.lib.bus.amba4.axis.Axi4Stream._
import spinal.lib.bus.amba4.axis._
import spinal.lib.cpu.riscv.impl.sync
import spinal.lib.experimental.math.{Floating, Floating32, IEEE754Converter}

import scala.math.Pi
import scala.language.postfixOps

case class UnwrapFloat() extends Module {

  // TODO: 增加AxiStream和Stream之间的转换方法
  // TODO: 将unwrap和datadelay改为基于Stream,而非Axi4Stream
  // TODO: fixed-point version of this module
  val FLOATING_OPERATION_LATENCY = 12
  def getFloatBits(value: Float) = B(IEEE754Converter.floatToBinary(value))

  val dataIn = slave Stream Fragment(Floating32())
  val dataInAsBits = dataIn.translateWith(fragment(dataIn.fragment.asBits, dataIn.last))
  val dataOut = master Stream Fragment(Floating32())

  val sub0 = FloatSub()
  val add0, add1, add2, add3 = FloatAdd()
  val acc0 = FloatAcc()

  // step0: delay
  val rawAndDelayed = DataDelay.getFixedDelayed(dataInAsBits, fixedDelay = 1)
  rawAndDelayed.ready.allowOverride()
  val delayedFlow = rawAndDelayed.translateWith(rawAndDelayed.fragment.takeHigh(32)).addFragmentLast(dataIn.last)
  val rawFlow = rawAndDelayed.translateWith(rawAndDelayed.fragment.takeLow(32)).addFragmentLast(dataIn.last)

  // step1: get delta
  sub0.s_axis_a.fromStreamFragment(rawFlow) // minuend
  sub0.s_axis_b.fromStreamFragment(delayedFlow) // subtrahend

  // step2: get candidates
  val deltaSources = StreamFork(sub0.m_axis_result, 5)
  val constants = Seq(Pi, -Pi, 2 * Pi, -2 * Pi).map(_.toFloat).map(getFloatBits)
  val Seq(det0, det1, cand2, cand1) =
    Seq(add0, add1, add2, add3).zip(constants).zip(deltaSources.take(4)).map {
      case ((add, bits), source) =>
        source >> add.s_axis_a
        add.s_axis_b.data := bits
        add.s_axis_b.last := source.last
        add.s_axis_b.valid := source.valid
        add.m_axis_result
    }

  val cand0 = deltaSources.last.queue(FLOATING_OPERATION_LATENCY + 3)
//  val cand0 = deltaSources.last
  val streamJoin = StreamJoin(Seq(cand0, cand1, cand2, det0, det1)) // sync by streamJoin

  // step3: determination
  val det = (!det1.data.msb ## det0.data.msb).asUInt // delta - pi > 0 ## delta + pi < 0
  val deltaFixed = det.mux(
    0 -> cand0.data,
    1 -> cand2.data,
    2 -> cand1.data,
    3 -> cand0.data // illegal
  )
// error case of a pipelined datapth
//  val fragmentDeltaFixed = fragment(RegNext(deltaFixed), RegNext(cand0.last))
//  val streamDeltaFixed = streamJoin.m2sPipe().translateWith(fragmentDeltaFixed)
  val fragmentDeltaFixed = fragment(deltaFixed, cand0.last)
  val streamDeltaFixed = streamJoin.translateWith(fragmentDeltaFixed).m2sPipe()

  // step4: accumulation
  acc0.s_axis_a.fromStreamFragment(streamDeltaFixed)
  acc0.m_axis_result.toStream.transmuteWith(HardType(Floating32())).addFragmentLast(acc0.m_axis_result.last) >> dataOut

}

object UnwrapFloat extends App {
//  Config.spinal.generateVerilog(new UnwrapFloat())
  val bin = IEEE754Converter.floatToBinary(1.5f)
  val sign = bin.head == '1'
  val exponent = bin.slice(1, 8)
  val mantissa = bin.takeRight(23)
  println(bin)
  println(sign + exponent + mantissa)
}
