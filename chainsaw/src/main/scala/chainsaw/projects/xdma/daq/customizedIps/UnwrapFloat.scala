package chainsaw.projects.xdma.daq.customizedIps

import chainsaw.projects.xdma.daq._
import chainsaw.projects.xdma.daq.ku060Ips.{FloatAcc, FloatAdd, FloatSub}
import spinal.core._
import spinal.lib.{StreamJoin, _}
import spinal.lib.experimental.math.{Floating32, IEEE754Converter}

import scala.language.postfixOps
import scala.math.Pi

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
  val delayedFlow = rawAndDelayed.translateWith(rawAndDelayed.fragment.head).addFragmentLast(dataIn.last)
  val rawFlow = rawAndDelayed.translateWith(rawAndDelayed.fragment.last).addFragmentLast(dataIn.last)

  // step1: get delta
  rawFlow >> sub0.s_axis_a // minuend
  delayedFlow >> sub0.s_axis_b // subtrahend

  // step2: get candidates
  val deltaSources = StreamFork(sub0.m_axis_result, 5)
  val constants = Seq(Pi, -Pi, 2 * Pi, -2 * Pi).map(_.toFloat).map(getFloatBits)
  val Seq(det0, det1, cand2, cand1) =
    Seq(add0, add1, add2, add3).zip(constants).zip(deltaSources.take(4)).map { case ((add, bits), source) =>
      source >> add.s_axis_a
      add.s_axis_b.fragment := bits
      add.s_axis_b.last := source.last
      add.s_axis_b.valid := source.valid
      add.m_axis_result
    }

  val cand0 = deltaSources.last.queue(FLOATING_OPERATION_LATENCY + 3)
//  val cand0 = deltaSources.last
  val streamJoin = StreamJoin(Seq(cand0, cand1, cand2, det0, det1)) // sync by streamJoin

  // step3: determination
  val det = (!det1.fragment.msb ## det0.fragment.msb).asUInt // delta - pi > 0 ## delta + pi < 0
  val deltaFixed = det.mux(
    0 -> cand0.fragment,
    1 -> cand2.fragment,
    2 -> cand1.fragment,
    3 -> cand0.fragment // illegal
  )
// error case of a pipelined datapth
//  val fragmentDeltaFixed = fragment(RegNext(deltaFixed), RegNext(cand0.last))
//  val streamDeltaFixed = streamJoin.m2sPipe().translateWith(fragmentDeltaFixed)
  val fragmentDeltaFixed = fragment(deltaFixed, cand0.last)
  val streamDeltaFixed = streamJoin.translateWith(fragmentDeltaFixed).m2sPipe()

  // step4: accumulation
  streamDeltaFixed >> acc0.s_axis_a
  acc0.m_axis_result.toStreamOfFragment
    .transmuteWith(HardType(Floating32()))
    .addFragmentLast(acc0.m_axis_result.last) >> dataOut

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
