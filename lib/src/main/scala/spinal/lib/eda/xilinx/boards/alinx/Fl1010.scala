package spinal.lib.eda.xilinx.boards.alinx

import spinal.core.Area
import spinal.lib.com.fmc.Fmc

/**
 * @param fmc
 */
case class Fl1010(fmc: Fmc) extends Area {

  // TODO: using Alinx40Pin

  require(!fmc.config.is_hpc)

  val mappingJ2 = Seq(17, 18, 23, 26, 27, 28, 29, 24, 25, 21, 22, 31, 30, 33, 32, 19, 20)
  val mappingJ3 = Seq(15, 16, 11, 0, 2, 3, 12, 7, 8, 4, 14, 13, 9, 10, 5, 6, 1)

  val J2_N = mappingJ2.map(fmc.LA_N(_))
  val J2_P = mappingJ2.map(fmc.LA_P(_))
  val J3_N = mappingJ3.map(fmc.LA_N(_))
  val J3_P = mappingJ3.map(fmc.LA_P(_))

}
