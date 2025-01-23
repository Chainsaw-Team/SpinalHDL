package chainsaw.projects.xdma.daq

import org.scalatest.funsuite.AnyFunSuiteLike
import spinal.core.{HertzNumber, Info, IntToBuilder}
import spinal.core.sim._
import spinal.lib.sim._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps

class ComponentDemodulatorTest extends AnyFunSuiteLike {

  /** @param dataX two-dimensional data array, each row represent a pulse
    * @param dataY same as pulseX
    */
  def testComponentDemodulator(
      carrierFreq: HertzNumber,
      dataX: Seq[Seq[Short]],
      dataY: Seq[Seq[Short]],
      pulseGapPoints: Int
  ): Array[Array[Int]] = {
    val pulseCount = dataX.length
    val pulseValidPoints = dataX.head.length
    val result = Array.fill(pulseCount)(Array.fill(pulseValidPoints * 2)(0))
    var pokeRowId, pokeColId, peekRowId, peekColId = 0

    Config.sim.compile(ComponentDemodulator(carrierFreq)).doSim { dut =>
      // initialization
      dut.streamIn.valid #= false
      dut.streamIn.last #= false
      dut.streamOut.ready #= false
      dut.clockDomain.forkStimulus(2)

      // driver thread
      fork {
        var finalCountDown = 10000 // extra cycles for monitor thread to collect output data
        var gapCountDown = pulseGapPoints / 2
        var state = "run"

        val driver = StreamDriver(dut.streamIn, dut.clockDomain) { payload =>
          state match {
            case "run" => // poking pulse data into DUT
              payload.fragment(0) #= dataX(pokeRowId)(pokeColId) // x0
              payload.fragment(1) #= dataX(pokeRowId)(pokeColId + 1) // x1
              payload.fragment(2) #= dataY(pokeRowId)(pokeColId) // y0
              payload.fragment(3) #= dataY(pokeRowId)(pokeColId + 1) // y1
              pokeColId += 2
              val last = pokeColId == pulseValidPoints
              payload.last #= last
              if (last) {
                pokeRowId += 1
                pokeColId = 0
                print(f"\rsimulating: $pokeRowId/$pulseCount")
                state =
                  if (gapCountDown > 0) "gap"
                  else if (pokeRowId == pulseCount) "end"
                  else "run"
              }
              true
            case "gap" => // poking gap data(invalid) into DUT
              payload.last #= false
              gapCountDown -= 1
              if (gapCountDown <= 0) { // state transition
                gapCountDown = pulseGapPoints / 2
                state = if (pokeRowId == pulseCount) "end" else "run"
              }
              false
            case "end" =>
              payload.last #= false
              finalCountDown -= 1
              if (finalCountDown <= 0) {
                println()
                simSuccess()
              }
              false
          }
        }
        driver.setFactor(1.0f) // continuous input
      }

      def twosComplementToInt(bits: String): Int = {
        // 转换为整数，检查最高位（符号位）
        val value = Integer.parseInt(bits, 2)
        if (bits.head == '1') {
          // 如果符号位是1，表示负数，要进行2's complement修正
          value - (1 << bits.length)
        } else {
          value
        }
      }

      fork { // monitor thread
        StreamReadyRandomizer(dut.streamOut, dut.clockDomain).setFactor(1.0f) // downstream always ready
        val monitor = StreamMonitor(dut.streamOut, dut.clockDomain) { payload =>
          // for bits
          val bits = payload.fragment.toBigInt.toString(2).reverse.padTo(64, '0').reverse
          val elements = bits.grouped(16).toSeq.reverse.map(twosComplementToInt) // lower bits -> earlier data
          // for vec
//          val elements = payload.fragment.map(_.toBigInt.toInt)
          elements.zipWithIndex.foreach { case (int, i) => result(peekRowId)(peekColId + i) = int }
          peekColId += elements.length
          val last = peekColId == pulseValidPoints * 2
          if (last) {
            peekRowId += 1
            peekColId = 0
          }

        }
      }

      while (true) { dut.clockDomain.waitSampling() }
    }
    result.tail // as some function relies on signal last, behavior of the first pulse may differ from the others, drop it
  }

  test("test fixed pattern") {

    val pulseValidPoints = 1024
    def getSin(isSin: Boolean, freq: HertzNumber, offset: Double = 0.0) = {
      val phases = (0 until pulseValidPoints).map(_ + offset).map(_ * 2 * Math.PI * freq.toDouble / 500e6)
      phases.map(if (isSin) Math.sin else Math.cos).map(_ * 32767).map(_.toShort)

    }

    val dataX = Seq.fill(5)(getSin(isSin = true, 20 MHz, 0.0))
    val dataY = Seq.fill(5)(getSin(isSin = false, 30 MHz, 0.0))
    val result = testComponentDemodulator(80 MHz, dataX, dataY, 0)
    println(result.map(_.mkString(",")).mkString("\n"))
    println("phase result")
    println(result.head.grouped(4).toSeq.flatMap(_.takeRight(2)).mkString(","))
    println("imag result")
    println(result.head.grouped(4).toSeq.flatMap(_.take(2)).mkString(","))
//    println(result.head.grouped(4).toSeq.flatMap(seq => Seq(seq(1), seq(3))).mkString(","))

//     cd
//     cd  ~/SpinalHDL/simWorkspace/ComponentDemodulator/xsim
//     echo -e "open_wave_database ComponentDemodulator.wdb\nopen_wave_config /home/ltr/SpinalHDL/ComponentDemodulator.wcfg" > view_wave.tcl
//     vivado -source view_wave.tcl

  }

}
