package spinal.lib.eda.xilinx

/** Record utilization of resources in Xilinx device
  */
case class VivadoUtil(
    lut: Double,
    ff: Double,
    dsp: Double,
    bram36: Double,
    uram288: Double,
    carry8: Double // TODO: need carry4 for Series7 devices?
) {

  def withLut(lut: Double): VivadoUtil = VivadoUtil(lut, ff, dsp, bram36, uram288, carry8)
  def withFf(ff: Double): VivadoUtil = VivadoUtil(lut, ff, dsp, bram36, uram288, carry8)
  def withDsp(dsp: Double): VivadoUtil = VivadoUtil(lut, ff, dsp, bram36, uram288, carry8)
  def withBram(bram36: Double): VivadoUtil =
    VivadoUtil(lut, ff, dsp, bram36, uram288, carry8)
  def withUram(uram288: Double): VivadoUtil =
    VivadoUtil(lut, ff, dsp, bram36, uram288, carry8)
  def withCarry(carry8: Double): VivadoUtil =
    VivadoUtil(lut, ff, dsp, bram36, uram288, carry8)

  private val getNames = Seq("lut", "ff", "dsp", "bram36", "uram288", "carry8")
  private def getValues = Seq(lut, ff, dsp, bram36, uram288, carry8)

  // arithmetic operations, +, -, * for new VivadoUtil, / for util percentage
  def +(that: VivadoUtil): VivadoUtil = VivadoUtil(
    this.getValues.zip(that.getValues).map { case (a, b) => a + b }
  )

  def -(that: VivadoUtil): VivadoUtil = VivadoUtil(
    this.getValues.zip(that.getValues).map { case (a, b) => a - b }
  )

  def *(k: Int): VivadoUtil = VivadoUtil(this.getValues.map(_ * k))
  def *(k: Double): VivadoUtil = VivadoUtil(this.getValues.map(_ * k))

  // to get percentage
  def /(that: VivadoUtil): VivadoUtil = VivadoUtil(this.getValues.zip(that.getValues).map { case (a, b) => a / b })

  // relational operations, carry8 is not considered as it won't be specified in many cases
  def <(that: VivadoUtil): Boolean =
    this.getValues.zip(that.getValues).forall { case (a, b) => a < b }

  def >(that: VivadoUtil): Boolean = that < this

  def <=(that: VivadoUtil): Boolean =
    this.getValues.zip(that.getValues).forall { case (a, b) => a <= b }

  def >=(that: VivadoUtil): Boolean = that <= this

  // TODO: regard resources in CLB aspect, may differ for different device families
  def clbCost(isPipeline: Boolean): Double = ???

  override def toString: String = {
    "\n\t" + getNames
      .map(_.toUpperCase)
      .zip(getValues.map(_.toString))
      .map { case (name, value) => s"$name = $value" }
      .mkString("\n\t")
  }
}

object VivadoUtil {

  def apply(
      lut: Double = 0,
      ff: Double = 0,
      dsp: Double = 0,
      bram36: Double = 0,
      uram288: Double = 0,
      carry8: Double = 0
  ) = new VivadoUtil(lut, ff, dsp, bram36, uram288, carry8)

  def apply(values: Seq[Double]): VivadoUtil = {
    require(values.forall(_ >= 0))
    new VivadoUtil(values(0), values(1), values(2), values(3), values(4), values(5))
  }

  // TODO: a factory method for generating VivadoUtil from a .rpt/.log file

  def main(args: Array[String]): Unit = { // usage

    val deviceCapacity =
      VivadoUtil(lut = 1182240, ff = 2364480, dsp = 6840, bram36 = 2160, uram288 = 960, carry8 = 147780)
    val moduleAUtil = VivadoUtil(100, 100, 100, 100, 100, 100)
    val moduleBUtil = VivadoUtil(100, 100, 100, 100, 100, 100)
    val topModuleUtil = moduleAUtil + moduleBUtil // sum up submodule utilization

    println(s"topModuleUtil: $topModuleUtil")
    assert(
      deviceCapacity * 0.8 >= topModuleUtil,
      "your design run out of resources"
    ) // check whether current utilization(extracted from a report) is acceptable
    println(s"utilization percentage: ${topModuleUtil / deviceCapacity}")
  }
}
