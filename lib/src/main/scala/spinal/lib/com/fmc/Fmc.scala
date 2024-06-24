package spinal.lib.com.fmc

import spinal.core._
import spinal.lib._

import scala.language.postfixOps

/** FMC(Fpga Mezzanine Card) interface configuration, including HPC and LPC
  * @param is_hpc HPC or LPC
  * @param gigabitWidth number of gigabit data lanes
  * @param useBidir use bi-directional user clock or not
  * @param useLa use user-defined data starting with LA or not
  * @param useHa use user-defined data starting with HA or not
  * @param useHb use user-defined data starting with HB or not
  * @param useI2c use I2C interface or not
  * @see [[https://fmchub.github.io/appendix/VITA57_FMC_HPC_LPC_SIGNALS_AND_PINOUT.html]] for pin definition
  */
case class FmcConfig(
    is_hpc: Boolean,
    gigabitWidth: Int,
    useLa: Boolean = false,
    useHa: Boolean = false,
    useHb: Boolean = false,
    useI2c: Boolean = false,
    useBidir: Boolean = false
)

/** FMC(Fpga Mezzanine Card) interface
  * @param config FMC configuration
  */
class Fmc(config: FmcConfig) extends Bundle with IMasterSlave {

  /* bi-directional signals are declared as Sequences of Analog(Bools),the direction should be set in application logic
  e.g.
    application_in := fmc.LA_P(0).asInput()
    fmc.LA_N(0).asOutput() := application_out
   */

  if (config.is_hpc) assert(config.gigabitWidth <= 10, "HPC requires <= 10 gigabit data lanes")
  else assert(config.gigabitWidth <= 1, "LPC requires <= 1 gigabit data lanes")

  // Gigabit Clocks, connected to GT? transceiver,width = 1 for LPC, 2 for HPC
  val GBTCLK_M2C_P, GBTCLK_M2C_N = Bits((if (config.is_hpc) 2 else 1) bits)

  // Gigabit Data, connected to GT? transceiver, set_property may failed on them,width = 1 for LPC, 10 for HPC

  val DP_M2C_P, DP_M2C_N, DP_C2M_P, DP_C2M_N = (config.gigabitWidth > 0) generate Bits(config.gigabitWidth bits)

  // User Clocks
  val CLK_M2C_P, CLK_M2C_N = Bits(2 bits) // user clock for LPC, mezzanine-to-carrier
  val CLK_DIR = config.useBidir generate Bool() // determine the direction of CLK_BIDIR
  val CLK_BIDIR_P, CLK_BIDIR_N = config.useBidir generate Bits(2 bits) // extra user clock for HPC, bi-directional

  // User Data, can be used as single-ended/pairs, bi-directional, CC for clock capable
  val LA_P, LA_N = (0 until 34).map(_ => config.useLa generate Analog(Bool())) // user data for LPC, 00,01,17,18 are CC
  val HA_P, HA_N = (0 until 24).map(_ => config.useHa generate Analog(Bool())) // user data for HPC, 00,01,17 are CC
  val HB_P, HB_N = (0 until 22).map(_ => config.useHa generate Analog(Bool())) // user data for HPC, 00,06,17 are CC

  // I2C TODO: use SpinalHDL built-in I2C interface
  val SDA, SCL = config.useI2c generate Analog(Bool()) // I2C serial clock & data

  override def asMaster(): Unit = {
    in(GBTCLK_M2C_P, GBTCLK_M2C_N, DP_M2C_P, DP_M2C_N, CLK_M2C_P, CLK_M2C_N) // M2C as input
    out(DP_C2M_P, DP_C2M_N) // C2M as output
    out(CLK_DIR) // TODO: direction?
  }

}
