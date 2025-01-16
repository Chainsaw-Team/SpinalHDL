package chainsaw.projects.xdma.daq.ku060IPs

import chainsaw.projects.xdma.daq.Config
import chainsaw.projects.xdma.daq.ku060Ips.LowpassFir
import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.bus.amba4.axis._
import spinal.lib.sim._

import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps

case class LowpassFirDut() extends Module {

  val s_axis_data_Config = Axi4StreamConfig(dataWidth = 4, useLast = true)
  val s_axis_data = slave(Axi4Stream(s_axis_data_Config))
  s_axis_data.setNameForEda()

  val m_axis_data_Config = Axi4StreamConfig(dataWidth = 6, useLast = true)
  val m_axis_data = master(Axi4Stream(m_axis_data_Config))
  m_axis_data.setNameForEda()

  val aclk = in Bool ()

  val core = LowpassFir()
  core.aclk := aclk
  core.s_axis_data <> s_axis_data
  m_axis_data <> core.m_axis_data

  val dataClockDomain = new ClockDomain(
    clock = aclk,
    config = ClockDomainConfig(resetKind = SYNC, resetActiveLevel = LOW),
    frequency = FixedFrequency(250 MHz)
  )

}

object LowpassFirDut extends App {
  Config.spinal.generateVerilog(LowpassFirDut())

  def convolve(input: Seq[Int], kernel: Seq[Int]): Seq[Int] = {
    require(kernel.nonEmpty, "Kernel must not be empty")

    // 计算滑窗大小
    val kernelSize = kernel.length

    // 使用 sliding 创建滑动窗口，zip 乘积并求和
    input.sliding(kernelSize).map(window => window.zip(kernel).map { case (a, b) => a * b }.sum).toSeq
  }

  val data = Seq(0, 1, 0, 2, 4, 2, 1, 1, 4, 0, 4, 3, 1, 3, 3, 4, 2, 2, 4, 1)
  val coeffs = Seq(6, 0, -4, -3, 5, 6, -6, -13, 7, 44, 64, 44, 7, -13, -6, 6, 5, -3, -4, 0, 6)
  val result = ArrayBuffer[Int]() // result container

  Config.sim.compile(LowpassFirDut()).doSim { dut =>
    // initialization
    dut.dataClockDomain.forkStimulus(2)

    // driver thread
    fork {
      var dataCount = 0 // extra cycles for monitor thread to collect output data
      var countDown = 1000
      val driver = StreamDriver(dut.s_axis_data, dut.dataClockDomain) { payload =>
        if (dataCount * 2 < data.length) {
          val v0 = data(dataCount * 2)
          val v1 = data(dataCount * 2 + 1)
          payload.data #= (BigInt(v1) << 16) + BigInt(v0) // earlier data on lower bits
          payload.last #= false
          dataCount += 1
          true
        } else {
          if (countDown != 0) {
            countDown -= 1
          } else simSuccess()
          false
        }
      }
      driver.setFactor(0.5f) // continuous input
    }

    def twosComplementToInt(bits: String): Int = {
      require(bits.length == 24, "Input binary string must be exactly 24 bits long")

      // 转换为整数，检查最高位（符号位）
      val value = Integer.parseInt(bits, 2)
      if (bits.head == '1') {
        // 如果符号位是1，表示负数，要进行2's complement修正
        value - (1 << bits.length)
      } else {
        value
      }
    }

    // monitor
    fork { // monitor thread
      StreamReadyRandomizer(dut.m_axis_data, dut.dataClockDomain).setFactor(1.0f) // downstream always ready
      val monitor = StreamMonitor(dut.m_axis_data, dut.dataClockDomain) { payload =>
        val bits = payload.data.toBigInt.toString(2).reverse.padTo(48, '0').reverse
        bits.grouped(24).toSeq.reverse.foreach { bits =>
          result.append(twosComplementToInt(bits))
        } // earlier data on lower bits
      }
    }

    // main thread
    while (true) { dut.dataClockDomain.waitSampling() }

  }

  println(s"result length =  ${result.length}")
  println(result.mkString(","))
  println(coeffs.sum)
}
