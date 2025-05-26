package spinal.lib.eda.xilinx.boards.alinx

import spinal.core.{Area, SpinalConfig, True}
import spinal.lib.com.fmc.Fmc

case class FL9627(fmc: Fmc) extends Area {

  require(!fmc.config.is_hpc)

//  // EEPROM TODO: direction？
//  val GA0 = fmc.getSignal('C', 34).asInput() // EEPROM 地址位 0 位
//  val GA1 = fmc.getSignal('D', 35).asInput() // EEPROM 地址位 1 位
//  val SCL = fmc.getSignal('C', 30).asInput() // EEPROM 的 I2C 时钟
//  val SDA = fmc.getSignal('C', 31).asInput() // EEPROM 的 I2C 数据
//  // CH1 SMI TODO: direction？
//  val AD1_SMI_SCLK = fmc.getSignal('G', 19).asInput() // AD1 监控信号串行输出时钟信号
//  val AD1_SMI_SDFS = fmc.getSignal('G', 18).asInput() // AD1 监控信号串行输出数据帧同步信号
//  val AD1_SMI_SDO = fmc.getSignal('H', 19).asInput() // AD1 监控信号串行输出数据信号
//  // CH2 SMI TODO: direction？
//  val AD2_SMI_SCLK = fmc.getSignal('G', 37).asInput() // AD2 芯片监控信号串行输出时钟信号
//  val AD2_SMI_SDFS = fmc.getSignal('G', 36).asInput() // AD2 芯片监控信号串行输出数据帧同步信号
//  val AD2_SMI_SDO = fmc.getSignal('H', 37).asInput() // AD2 芯片监控信号串行输出数据信号
//  // Sync TODO: direction？
//  val AD_SYNC = fmc.getSignal('H', 20).asInput() // 数字同步信号

  // CH1 SPI
  val AD1_SPI_CS = fmc.getSignal('G', 9).asOutput() // AD1 芯片的 SPI 通信片选信号
  val AD1_SPI_SDIO = fmc.getSignal('G', 10).asInOut() // AD1 芯片的 SPI 通信数据信号
  val AD1_SPI_SCLK = fmc.getSignal('D', 9).asOutput() // AD1 芯片的 SPI 通信时钟信号
  // CH2 SPI
  val AD2_SPI_CS = fmc.getSignal('D', 21).asOutput() // AD2 芯片的 SPI 通信片选信号
  val AD2_SPI_SDIO = fmc.getSignal('D', 23).asInOut() // AD2 芯片的 SPI 通信数据信号
  val AD2_SPI_SCLK = fmc.getSignal('D', 24).asOutput() // AD2 芯片的 SPI 通信时钟信号

  // CH1 clocks
  val CLK1_125M = fmc.getSignal('D', 8).asOutput() // AD1 芯片的 125M 参考时钟输入
  val AD1_DCOP = fmc.getSignal('G', 6).asInput() // AD1 通道 A 和通道 B LVDS 的数据时钟输出-P.
  val AD1_DCON = fmc.getSignal('G', 7).asInput() // AD1 通道 A 和通道 B LVDS 的数据时钟输出-N.
  // CH2 clocks
  val CLK2_125M = fmc.getSignal('D', 20).asOutput() // AD2 芯片的 125M 参考时钟输入
  val AD2_DCOP = fmc.getSignal('C', 22).asInput() // AD2 通道 A 和通道 B LVDS 的数据时钟输出-P.
  val AD2_DCON = fmc.getSignal('C', 23).asInput() // AD2 通道 A 和通道 B LVDS 的数据时钟输出-N.

  // CH1 data
  val AD1_DOP = fmc.getSignal('H', 7).asInput() // AD1 通道 A 和通道 B LVDS 的数据 0 输出-P.
  val AD1_DON = fmc.getSignal('H', 8).asInput() // AD1 通道 A 和通道 B LVDS 的数据 0 输出-N.
  val AD1_D1P = fmc.getSignal('C', 10).asInput() // AD1 通道 A 和通道 B LVDS 的数据 1 输出-P.
  val AD1_D1N = fmc.getSignal('C', 11).asInput() // AD1 通道 A 和通道 B LVDS 的数据 1 输出-N.
  val AD1_D2P = fmc.getSignal('D', 11).asInput() // AD1 通道 A 和通道 B LVDS 的数据 2 输出-P.
  val AD1_D2N = fmc.getSignal('D', 12).asInput() // AD1 通道 A 和通道 B LVDS 的数据 2 输出-N.
  val AD1_D3P = fmc.getSignal('H', 10).asInput() // AD1 通道 A 和通道 B LVDS 的数据 3 输出-P.
  val AD1_D3N = fmc.getSignal('H', 11).asInput() // AD1 通道 A 和通道 B LVDS 的数据 3 输出-N.
  val AD1_D4P = fmc.getSignal('C', 14).asInput() // AD1 通道 A 和通道 B LVDS 的数据 4 输出-P.
  val AD1_D4N = fmc.getSignal('C', 15).asInput() // AD1 通道 A 和通道 B LVDS 的数据 4 输出-N.
  val AD1_D5P = fmc.getSignal('G', 12).asInput() // AD1 通道 A 和通道 B LVDS 的数据 5 输出-P.
  val AD1_D5N = fmc.getSignal('G', 13).asInput() // AD1 通道 A 和通道 B LVDS 的数据 5 输出-N.
  val AD1_D6P = fmc.getSignal('H', 13).asInput() // AD1 通道 A 和通道 B LVDS 的数据 6 输出-P.
  val AD1_D6N = fmc.getSignal('H', 14).asInput() // AD1 通道 A 和通道 B LVDS 的数据 6 输出-N.
  val AD1_D7P = fmc.getSignal('D', 14).asInput() // AD1 通道 A 和通道 B LVDS 的数据 7 输出-P.
  val AD1_D7N = fmc.getSignal('D', 15).asInput() // AD1 通道 A 和通道 B LVDS 的数据 7 输出-N.
  val AD1_D8P = fmc.getSignal('G', 15).asInput() // AD1 通道 A 和通道 B LVDS 的数据 8 输出-P.
  val AD1_D8N = fmc.getSignal('G', 16).asInput() // AD1 通道 A 和通道 B LVDS 的数据 8 输出-N.
  val AD1_D9P = fmc.getSignal('H', 16).asInput() // AD1 通道 A 和通道 B LVDS 的数据 9 输出-P.
  val AD1_D9N = fmc.getSignal('H', 17).asInput() // AD1 通道 A 和通道 B LVDS 的数据 9 输出-N.
  val AD1_D10P = fmc.getSignal('D', 17).asInput() // AD1 通道 A 和通道 B LVDS 的数据 10 输出-P.
  val AD1_D10N = fmc.getSignal('D', 18).asInput() // AD1 通道 A 和通道 B LVDS 的数据 10 输出-N.
  val AD1_D11P = fmc.getSignal('C', 18).asInput() // AD1 通道 A 和通道 B LVDS 的数据 11 输出-P.
  val AD1_D11N = fmc.getSignal('C', 19).asInput() // AD1 通道 A 和通道 B LVDS 的数据 11 输出-N.
  // CH2 data
  val AD2_DOP = fmc.getSignal('G', 21).asInput() // AD2 通道 A 和通道 B LVDS 的数据 0 输出-P.
  val AD2_DON = fmc.getSignal('G', 22).asInput() // AD2 通道 A 和通道 B LVDS 的数据 0 输出-N.
  val AD2_D1P = fmc.getSignal('H', 22).asInput() // AD2 通道 A 和通道 B LVDS 的数据 1 输出-P.
  val AD2_D1N = fmc.getSignal('H', 23).asInput() // AD2 通道 A 和通道 B LVDS 的数据 1 输出-N.
  val AD2_D2P = fmc.getSignal('C', 26).asInput() // AD2 通道 A 和通道 B LVDS 的数据 2 输出-P.
  val AD2_D2N = fmc.getSignal('C', 27).asInput() // AD2 通道 A 和通道 B LVDS 的数据 2 输出-N.
  val AD2_D3P = fmc.getSignal('G', 24).asInput() // AD2 通道 A 和通道 B LVDS 的数据 3 输出-P.
  val AD2_D3N = fmc.getSignal('G', 25).asInput() // AD2 通道 A 和通道 B LVDS 的数据 3 输出-N.
  val AD2_D4P = fmc.getSignal('H', 25).asInput() // AD2 通道 A 和通道 B LVDS 的数据 4 输出-P.
  val AD2_D4N = fmc.getSignal('H', 26).asInput() // AD2 通道 A 和通道 B LVDS 的数据 4 输出-N.
  val AD2_D5P = fmc.getSignal('D', 26).asInput() // AD2 通道 A 和通道 B LVDS 的数据 5 输出-P.
  val AD2_D5N = fmc.getSignal('D', 27).asInput() // AD2 通道 A 和通道 B LVDS 的数据 5 输出-N.
  val AD2_D6P = fmc.getSignal('G', 27).asInput() // AD2 通道 A 和通道 B LVDS 的数据 6 输出-P.
  val AD2_D6N = fmc.getSignal('G', 28).asInput() // AD2 通道 A 和通道 B LVDS 的数据 6 输出-N.
  val AD2_D7P = fmc.getSignal('H', 28).asInput() // AD2 通道 A 和通道 B LVDS 的数据 7 输出-P.
  val AD2_D7N = fmc.getSignal('H', 29).asInput() // AD2 通道 A 和通道 B LVDS 的数据 7 输出-N.
  val AD2_D8P = fmc.getSignal('G', 30).asInput() // AD2 通道 A 和通道 B LVDS 的数据 8 输出-P.
  val AD2_D8N = fmc.getSignal('G', 31).asInput() // AD2 通道 A 和通道 B LVDS 的数据 8 输出-N.
  val AD2_D9P = fmc.getSignal('H', 31).asInput() // AD2 通道 A 和通道 B LVDS 的数据 9 输出-P.
  val AD2_D9N = fmc.getSignal('H', 32).asInput() // AD2 通道 A 和通道 B LVDS 的数据 9 输出-N.
  val AD2_D10P = fmc.getSignal('G', 33).asInput() // AD2 通道 A 和通道 B LVDS 的数据 10 输出-P.
  val AD2_D10N = fmc.getSignal('G', 34).asInput() // AD2 通道 A 和通道 B LVDS 的数据 10 输出-N.
  val AD2_D11P = fmc.getSignal('H', 34).asInput() // AD2 通道 A 和通道 B LVDS 的数据 11 输出-P.
  val AD2_D11N = fmc.getSignal('H', 35).asInput() // AD2 通道 A 和通道 B LVDS 的数据 11 输出-N.

  // bitVectors

}
