package chainsaw.projects.xdma.axku5Daq

import spinal.core._
import spinal.lib.com.fmc.Fmc

case class MysoowFmc(fmc:Fmc) extends Area {

  val version = "1.0.3"
  require(fmc.config.is_hpc)
  require(fmc.config.useLa)
  require(fmc.config.useI2c)

  // adc1 control
  lazy val adc1_sclk = fmc.LA_N(5).asOutput()
  lazy val adc1_csn = fmc.LA_N(4).asOutput()
  lazy val adc1_sdio = fmc.LA_P(4).asInOut()
  lazy val adc1_sync_p = fmc.LA_P(3).asOutput()
  lazy val adc1_sync_n = fmc.LA_N(3).asOutput()
  lazy val ch1_dc_sw = fmc.LA_P(10).asOutput()
  lazy val ch2_dc_sw = fmc.LA_N(10).asOutput()
  lazy val adc1_gpio_a = fmc.LA_P(2).asInOut()
  lazy val adc1_gpio_b = fmc.LA_N(2).asInOut()
  // adc1 clocks
  lazy val adc1_sysref_p = if (version == "1.0.1") fmc.LA_P(1).asInput() else fmc.LA_P(0).asInput()
  lazy val adc1_sysref_n = if (version == "1.0.1") fmc.LA_N(1).asInput() else fmc.LA_N(0).asInput()
  lazy val adc1_core_clk_p = if (version == "1.0.1") fmc.LA_P(0).asInput() else fmc.LA_P(1).asInput()
  lazy val adc1_core_clk_n = if (version == "1.0.1") fmc.LA_N(0).asInput() else fmc.LA_N(1).asInput()
  lazy val adc1_mgt_clk_p = fmc.GBTCLK_M2C_P(0).asInput()
  lazy val adc1_mgt_clk_n = fmc.GBTCLK_M2C_N(0).asInput()
  lazy val adc1_powerdown = fmc.LA_P(5).asOutput()
  // adc data
  lazy val adc1_data_p = fmc.DP_M2C_P(3 downto 0)
  lazy val adc1_data_n = fmc.DP_M2C_N(3 downto 0)
  // hmc7044 control
  lazy val hmc7044_gpio3 = fmc.LA_P(12).asInput()
  lazy val hmc7044_gpio4 = fmc.LA_P(11).asInput()
  lazy val hmc7044_rstn = fmc.LA_N(7).asOutput()
  lazy val hmc7044_sclk = fmc.LA_P(7).asOutput()
  lazy val hmc7044_sdio = fmc.LA_N(9).asInOut()
  lazy val hmc7044_slen = fmc.LA_P(9).asOutput()
  // hmc7044 clocks
  lazy val hmc7044_ref_clk_0_p = if (version == "1.0.1") fmc.CLK_M2C_P(1).asOutput() else fmc.config.useBidir generate fmc.CLK_BIDIR_P(0).asOutput()
  lazy val hmc7044_ref_clk_0_n = if (version == "1.0.1") fmc.CLK_M2C_N(1).asOutput() else fmc.config.useBidir generate fmc.CLK_BIDIR_N(0).asOutput()
  lazy val hmc7044_ref_clk_2_p = fmc.config.useBidir generate fmc.CLK_BIDIR_P(1).asOutput()
  lazy val hmc7044_ref_clk_2_n = fmc.config.useBidir generate fmc.CLK_BIDIR_N(1).asOutput()
  lazy val hmc7044_channel_2_p = fmc.CLK_M2C_P(0).asInput()
  lazy val hmc7044_channel_2_n = fmc.CLK_M2C_N(0).asInput()
  lazy val hmc7044_channel_3_p = if (version == "1.0.1") fmc.config.useBidir generate fmc.CLK_BIDIR_P(0).asInput() else fmc.CLK_M2C_P(1).asInput()
  lazy val hmc7044_channel_3_n = if (version == "1.0.1") fmc.config.useBidir generate fmc.CLK_BIDIR_N(0).asInput() else fmc.CLK_M2C_N(1).asInput()
  lazy val hmc7044_sync = fmc.LA_P(15)
  // I2C
  lazy val scl = fmc.SCL.asInOut()
  lazy val sda = fmc.SDA.asInOut()


}
