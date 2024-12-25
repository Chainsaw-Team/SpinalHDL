package chainsaw.projects.xdma.daq

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.amba4.axis.{Axi4Stream, Axi4StreamConfig}
import spinal.lib.sim._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps
import scala.math._

case class ChainsawDaqDemodulator() extends Module {

  // TODO: learning using pipeline utilities for Stream
  val vectorWidth = 2

  val streamIn = slave Stream Fragment(Vec(SInt(16 bits), 4)) // earlier data in lower index
  val streamOut = master Stream Fragment(Bits(64 bits)) // earlier data in lower bits
  val channelSelection = in Bool () // select X|Y
  val xyEnabled = in Bool () // enable both X and Y

  val channel0 = streamIn.fragment.take(vectorWidth)
  val channel1 = streamIn.fragment.takeRight(vectorWidth)

  // * carrier
  def gcd(a: BigInt, b: BigInt): BigInt = if (b == 0) a else gcd(b, a % b)
  def lcm(a: BigInt, b: BigInt): BigInt = a * b / gcd(a, b)

  val freqs = Seq(500e6, 80e6, 200e6).map(_.toInt).map(BigInt(_))
  val Seq(adcSamplingRate, carrier0Freq, carrier1Freq) = freqs
  val carrierSamples = lcm(vectorWidth, adcSamplingRate / freqs.reduce(gcd)) // 通过最大公共频率找出最小公共周期
  // TODO: DDS module
  println(s"Number of samples for one common period: $carrierSamples points")
  val indices = 0 until carrierSamples.toInt
  val phases0 = indices.map(_ * 2 * Pi * carrier0Freq.doubleValue() / adcSamplingRate.doubleValue())
  val phases1 = indices.map(_ * 2 * Pi * carrier1Freq.doubleValue() / adcSamplingRate.doubleValue())
  val carrier0Value = phases0.map(sin).map(_ * (1 << 15)).map(_.toInt)
  val carrier1Value = phases1.map(sin).map(_ * (1 << 15)).map(_.toInt)
  val carrier0Rom = Mem(carrier0Value.grouped(vectorWidth).map(vec => Vec(vec.map(S(_, 16 bits)))).toSeq)
  val carrier1Rom = Mem(carrier1Value.grouped(vectorWidth).map(vec => Vec(vec.map(S(_, 16 bits)))).toSeq)

  case class ComponentDemodulator(carrierFreq: BigInt, data: Stream[Fragment[Vec[SInt]]]) extends Area {

    val carrierSamples = lcm(vectorWidth, adcSamplingRate / gcd(adcSamplingRate, carrierFreq)) // 通过最大公共频率找出最小公共周期
    println(s"Number of samples for one common period: $carrierSamples points")
    val indices = 0 until carrierSamples.toInt
    val phases0 = indices.map(_ * 2 * Pi * carrier0Freq.doubleValue() / adcSamplingRate.doubleValue())
    val phases1 = indices.map(_ * 2 * Pi * carrier1Freq.doubleValue() / adcSamplingRate.doubleValue())
    val carrier0Value = phases0.map(sin).map(_ * (1 << 15)).map(_.toInt)
    val carrier1Value = phases1.map(sin).map(_ * (1 << 15)).map(_.toInt)
    val carrier0Rom = Mem(carrier0Value.grouped(vectorWidth).map(vec => Vec(vec.map(S(_, 16 bits)))).toSeq)
    val carrier1Rom = Mem(carrier1Value.grouped(vectorWidth).map(vec => Vec(vec.map(S(_, 16 bits)))).toSeq)

  }

  // channel selection
  val summation = channel0
    .zip(channel1)
    .map { case (x, y) => ((x +^ y) >> 1).asBits }

  streamOut.fragment := summation(1) ## summation(1) ## summation(0) ## summation(0)
  streamOut.valid := streamIn.valid
  streamIn.ready := streamOut.ready
  streamOut.last := streamIn.last

}

object ChainsawDaqDemodulator extends App {
  SpinalConfig().generateVerilog(ChainsawDaqDemodulator())
}
