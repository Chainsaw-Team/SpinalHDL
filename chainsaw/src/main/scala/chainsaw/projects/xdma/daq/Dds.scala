package chainsaw.projects.xdma.daq

import spinal.core._

import scala.language.postfixOps
import scala.math._

case class DdsConfig(clockFreq: HertzNumber, amplitudePrecision: Int, phasePrecision: Int, lutAddrWidth: Int) {

  // TODO: dithering and taylor series optimization

  assert(lutAddrWidth <= phasePrecision)
  assert(amplitudePrecision <= 32)

  val phaseDepth = 1 << phasePrecision
  val lutDepth = 1 << lutAddrWidth
  val phaseTruncationWidth = phasePrecision - lutAddrWidth

  val amplitudeFactor = 1 << (amplitudePrecision - 1)
  val frequencyResolution = clockFreq / phaseDepth
  val lutContent = (0 until lutDepth).map(_ * 2 * Pi / lutDepth).map(sin).map(_ * amplitudeFactor).map(_.toInt)
  println(f"dds module generated: \n\tLUT size: ${lutDepth * amplitudePrecision}bits, frequency resolution: ${frequencyResolution.toDouble / 1e6}MHz")

  /** helper function for specific frequency
    * @return return the phase increment that DDS need to generate wave a given frequency
    */
  def synth(targetFreq: HertzNumber): Int = {

    val phaseIncrement = (targetFreq / frequencyResolution.toDouble).toInt
    val actualTargetFreq = phaseIncrement * frequencyResolution.toDouble
    val actualIndicies = (0 until 1 << 10).map(i => ((i * phaseIncrement) % phaseDepth) >> phaseTruncationWidth)
    val actualLutContent = actualIndicies.map(lutContent)
    println(f"when synthesizing ${targetFreq.toDouble / 1e6}MHz")
    println(f"\tphase increment: $phaseIncrement, actual target frequency: ${actualTargetFreq / 1e6}MHz")

    // draw SFDR for given target frequency
    val binFilePath = f"dds@${targetFreq.toDouble / 1e6}MHz.bin"
    val pngFilePath = f"dds@${targetFreq.toDouble / 1e6}MHz.png"

    SaveBinFile(binFilePath, actualLutContent)
    if (
      RunPython("draw_dds.py", Seq(binFilePath, pngFilePath, clockFreq.toDouble.toString, targetFreq.toDouble.toString))
    )
      println(f"\tview $pngFilePath for SFDR")
    else println("skip SFDR drawing as draw_dds.py failed")

    phaseIncrement
  }

}

case class Dds(ddsConfig: DdsConfig) extends Module {
  // TODO: RTL implementation

}

object DdsConfig extends App {
  val ddsConfig = DdsConfig(500 MHz, 16, 16, 16)
  ddsConfig.synth(200 MHz)
  ddsConfig.synth(80 MHz)
  ddsConfig.synth(76 MHz)
  ddsConfig.synth(84 MHz)
}
