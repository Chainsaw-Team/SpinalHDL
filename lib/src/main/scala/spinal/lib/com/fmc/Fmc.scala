package spinal.lib.com.fmc

import spinal.core._
import spinal.lib._
import spinal.lib.com.jtag

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
    useBidir: Boolean = false,
    useGa: Boolean = false
    // TODO: JTAG pins
)

/** FMC(Fpga Mezzanine Card) interface
  * @param config FMC configuration
  */
class Fmc(val config: FmcConfig) extends Bundle with IMasterSlave {

  /* bi-directional signals are declared as Sequences of Analog(Bools),the direction should be set in application logic
  e.g.
    application_in := fmc.LA_P(0).asInput()
    fmc.LA_N(0).asOutput() := application_out
   */

  if (config.is_hpc) assert(config.gigabitWidth <= 10, "HPC requires <= 10 gigabit data lanes")
  else assert(config.gigabitWidth <= 1, "LPC requires <= 1 gigabit data lanes")

  // Gigabit Clocks, connected to GT? transceiver,width = 1 for LPC, 2 for HPC
  val widthGbtClk = if (config.is_hpc) 2 else 1
  val GBTCLK_M2C_P, GBTCLK_M2C_N = (0 until widthGbtClk).map(_ => (config.gigabitWidth > 0) generate Bool())

  // Gigabit Data, connected to GT? transceiver, set_property may failed on them,width = 1 for LPC, 10 for HPC
  val DP_M2C_P, DP_M2C_N, DP_C2M_P, DP_C2M_N = (config.gigabitWidth > 0) generate Bits(config.gigabitWidth bits)

  // User Clocks
  val CLK_M2C_P, CLK_M2C_N = (0 until 2).map(_ => Bool()) // user clock for LPC, mezzanine-to-carrier
  val CLK_DIR = config.useBidir generate Bool() // determine the direction of CLK_BIDIR
  val CLK_BIDIR_P, CLK_BIDIR_N =
    (0 until 2).map(_ => config.useBidir generate Bool()) // extra user clock for HPC, bi-directional

  // User Data, can be used as single-ended/pairs, bi-directional, CC for clock capable
  val LA_P, LA_N = (0 until 34).map(_ => config.useLa generate Analog(Bool())) // user data for LPC, 00,01,17,18 are CC
  val HA_P, HA_N = (0 until 24).map(_ => config.useHa generate Analog(Bool())) // user data for HPC, 00,01,17 are CC
  val HB_P, HB_N = (0 until 22).map(_ => config.useHa generate Analog(Bool())) // user data for HPC, 00,06,17 are CC

  // I2C TODO: use SpinalHDL built-in I2C interface
  val SDA, SCL = config.useI2c generate Analog(Bool()) // I2C serial clock & data
  // Geographical address
  val GA0, GA1 = config.useGa generate Bool()

  override def asMaster(): Unit = {
    in(DP_M2C_P, DP_M2C_N) // M2C as input
    out(DP_C2M_P, DP_C2M_N) // C2M as output
  }

  def getSignal(columnIndex: Char, rowIndex: Int) = (columnIndex, rowIndex) match {
    case ('B', 1)  => CLK_DIR
    case ('J', 2)  => CLK_BIDIR_P(3)
    case ('G', 2)  => CLK_M2C_P(1)
    case ('E', 2)  => HA_P(1)
    case ('C', 2)  => DP_C2M_P(0)
    case ('A', 2)  => DP_M2C_P(1)
    case ('J', 3)  => CLK_BIDIR_N(3)
    case ('G', 3)  => CLK_M2C_N(1)
    case ('E', 3)  => HA_N(1)
    case ('C', 3)  => DP_C2M_N(0)
    case ('A', 3)  => DP_M2C_N(1)
    case ('K', 4)  => CLK_BIDIR_P(2)
    case ('H', 4)  => CLK_M2C_P(0)
    case ('F', 4)  => HA_P(0)
    case ('D', 4)  => GBTCLK_M2C_P(0)
    case ('B', 4)  => DP_M2C_P(9)
    case ('K', 5)  => CLK_BIDIR_N(2)
    case ('H', 5)  => CLK_M2C_N(0)
    case ('F', 5)  => HA_N(0)
    case ('D', 5)  => GBTCLK_M2C_N(0)
    case ('B', 5)  => DP_M2C_N(9)
    case ('J', 6)  => HA_P(3)
    case ('G', 6)  => LA_P(0)
    case ('E', 6)  => HA_P(5)
    case ('C', 6)  => DP_M2C_P(0)
    case ('A', 6)  => DP_M2C_P(2)
    case ('K', 7)  => HA_P(2)
    case ('J', 7)  => HA_N(3)
    case ('H', 7)  => LA_P(2)
    case ('G', 7)  => LA_N(0)
    case ('F', 7)  => HA_P(4)
    case ('E', 7)  => HA_N(5)
    case ('C', 7)  => DP_M2C_N(0)
    case ('A', 7)  => DP_M2C_N(2)
    case ('K', 8)  => HA_N(2)
    case ('H', 8)  => LA_N(2)
    case ('F', 8)  => HA_N(4)
    case ('D', 8)  => LA_P(1)
    case ('B', 8)  => DP_M2C_P(8)
    case ('J', 9)  => HA_P(7)
    case ('G', 9)  => LA_P(3)
    case ('E', 9)  => HA_P(9)
    case ('D', 9)  => LA_N(1)
    case ('B', 9)  => DP_M2C_N(8)
    case ('K', 10) => HA_P(6)
    case ('J', 10) => HA_N(7)
    case ('H', 10) => LA_P(4)
    case ('G', 10) => LA_N(3)
    case ('F', 10) => HA_P(8)
    case ('E', 10) => HA_N(9)
    case ('C', 10) => LA_P(6)
    case ('A', 10) => DP_M2C_P(3)
    case ('K', 11) => HA_N(6)
    case ('H', 11) => LA_N(4)
    case ('F', 11) => HA_N(8)
    case ('D', 11) => LA_P(5)
    case ('C', 11) => LA_N(6)
    case ('A', 11) => DP_M2C_N(3)
    case ('J', 12) => HA_P(11)
    case ('G', 12) => LA_P(8)
    case ('E', 12) => HA_P(13)
    case ('D', 12) => LA_N(5)
    case ('B', 12) => DP_M2C_P(7)
    case ('K', 13) => HA_P(10)
    case ('J', 13) => HA_N(11)
    case ('H', 13) => LA_P(7)
    case ('G', 13) => LA_N(8)
    case ('F', 13) => HA_P(12)
    case ('E', 13) => HA_N(13)
    case ('B', 13) => DP_M2C_N(7)
    case ('K', 14) => HA_N(10)
    case ('H', 14) => LA_N(7)
    case ('F', 14) => HA_N(12)
    case ('D', 14) => LA_P(9)
    case ('C', 14) => LA_P(10)
    case ('A', 14) => DP_M2C_P(4)
    case ('J', 15) => HA_P(14)
    case ('G', 15) => LA_P(12)
    case ('E', 15) => HA_P(16)
    case ('D', 15) => LA_N(9)
    case ('C', 15) => LA_N(10)
    case ('A', 15) => DP_M2C_N(4)
    case ('K', 16) => HA_P(17)
    case ('J', 16) => HA_N(14)
    case ('H', 16) => LA_P(11)
    case ('G', 16) => LA_N(12)
    case ('F', 16) => HA_P(15)
    case ('E', 16) => HA_N(16)
    case ('B', 16) => DP_M2C_P(6)
    case ('K', 17) => HA_N(17)
    case ('H', 17) => LA_N(11)
    case ('F', 17) => HA_N(15)
    case ('D', 17) => LA_P(13)
    case ('B', 17) => DP_M2C_N(6)
    case ('J', 18) => HA_P(18)
    case ('G', 18) => LA_P(16)
    case ('E', 18) => HA_P(20)
    case ('D', 18) => LA_N(13)
    case ('C', 18) => LA_P(14)
    case ('A', 18) => DP_M2C_P(5)
    case ('K', 19) => HA_P(21)
    case ('J', 19) => HA_N(18)
    case ('H', 19) => LA_P(15)
    case ('G', 19) => LA_N(16)
    case ('F', 19) => HA_P(19)
    case ('E', 19) => HA_N(20)
    case ('C', 19) => LA_N(14)
    case ('A', 19) => DP_M2C_N(5)
    case ('K', 20) => HA_N(21)
    case ('H', 20) => LA_N(15)
    case ('F', 20) => HA_N(19)
    case ('D', 20) => LA_P(17)
    case ('B', 20) => GBTCLK_M2C_P(1)
    case ('J', 21) => HA_P(22)
    case ('G', 21) => LA_P(20)
    case ('E', 21) => HB_P(3)
    case ('D', 21) => LA_N(17)
    case ('B', 21) => GBTCLK_M2C_N(1)
    case ('K', 22) => HA_P(23)
    case ('J', 22) => HA_N(22)
    case ('H', 22) => LA_P(19)
    case ('G', 22) => LA_N(20)
    case ('F', 22) => HB_P(2)
    case ('E', 22) => HB_N(3)
    case ('C', 22) => LA_P(18)
    case ('A', 22) => DP_C2M_P(1)
    case ('K', 23) => HA_N(23)
    case ('H', 23) => LA_N(19)
    case ('F', 23) => HB_N(2)
    case ('D', 23) => LA_P(23)
    case ('C', 23) => LA_N(18)
    case ('A', 23) => DP_C2M_N(1)
    case ('J', 24) => HB_P(1)
    case ('G', 24) => LA_P(22)
    case ('E', 24) => HB_P(5)
    case ('D', 24) => LA_N(23)
    case ('B', 24) => DP_C2M_P(9)
    case ('K', 25) => HB_P(0)
    case ('J', 25) => HB_N(1)
    case ('H', 25) => LA_P(21)
    case ('G', 25) => LA_N(22)
    case ('F', 25) => HB_P(4)
    case ('E', 25) => HB_N(5)
    case ('B', 25) => DP_C2M_N(9)
    case ('K', 26) => HB_N(0)
    case ('H', 26) => LA_N(21)
    case ('F', 26) => HB_N(4)
    case ('D', 26) => LA_P(26)
    case ('C', 26) => LA_P(27)
    case ('A', 26) => DP_C2M_P(2)
    case ('J', 27) => HB_P(7)
    case ('G', 27) => LA_P(25)
    case ('E', 27) => HB_P(9)
    case ('D', 27) => LA_N(26)
    case ('C', 27) => LA_N(27)
    case ('A', 27) => DP_C2M_N(2)
    case ('K', 28) => HB_P(6)
    case ('J', 28) => HB_N(7)
    case ('H', 28) => LA_P(24)
    case ('G', 28) => LA_N(25)
    case ('F', 28) => HB_P(8)
    case ('E', 28) => HB_N(9)
    case ('B', 28) => DP_C2M_P(8)
    case ('K', 29) => HB_N(6)
    case ('H', 29) => LA_N(24)
    case ('F', 29) => HB_N(8)
    case ('B', 29) => DP_C2M_N(8)
    case ('J', 30) => HB_P(11)
    case ('G', 30) => LA_P(29)
    case ('E', 30) => HB_P(13)
    case ('C', 30) => SCL
    case ('A', 30) => DP_C2M_P(3)
    case ('K', 31) => HB_P(10)
    case ('J', 31) => HB_N(11)
    case ('H', 31) => LA_P(28)
    case ('G', 31) => LA_N(29)
    case ('F', 31) => HB_P(12)
    case ('E', 31) => HB_N(13)
    case ('C', 31) => SDA
    case ('A', 31) => DP_C2M_N(3)
    case ('K', 32) => HB_N(10)
    case ('H', 32) => LA_N(28)
    case ('F', 32) => HB_N(12)
    case ('B', 32) => DP_C2M_P(7)
    case ('J', 33) => HB_P(15)
    case ('G', 33) => LA_P(31)
    case ('E', 33) => HB_P(19)
    case ('B', 33) => DP_C2M_N(7)
    case ('K', 34) => HB_P(14)
    case ('J', 34) => HB_N(15)
    case ('H', 34) => LA_P(30)
    case ('G', 34) => LA_N(31)
    case ('F', 34) => HB_P(16)
    case ('E', 34) => HB_N(19)
    case ('C', 34) => GA0
    case ('A', 34) => DP_C2M_P(4)
    case ('K', 35) => HB_N(14)
    case ('H', 35) => LA_N(30)
    case ('F', 35) => HB_N(16)
    case ('D', 35) => GA1
    case ('A', 35) => DP_C2M_N(4)
    case ('J', 36) => HB_P(18)
    case ('G', 36) => LA_P(33)
    case ('E', 36) => HB_P(21)
    case ('B', 36) => DP_C2M_P(6)
    case ('K', 37) => HB_P(17)
    case ('J', 37) => HB_N(18)
    case ('H', 37) => LA_P(32)
    case ('G', 37) => LA_N(33)
    case ('F', 37) => HB_P(20)
    case ('E', 37) => HB_N(21)
    case ('B', 37) => DP_C2M_N(6)
    case ('K', 38) => HB_N(17)
    case ('H', 38) => LA_N(32)
    case ('F', 38) => HB_N(20)
    case ('A', 38) => DP_C2M_P(5)
    case ('A', 39) => DP_C2M_N(5)
  }

}
