#
text = """VREF_B_M2C	GND	VREF_A_M2C	GND	PG_M2C	GND	PG_C2M	GND	CLK_DIR	GND
GND	CLK3_BIDIR_P	PRSNT_M2C_L	CLK1_M2C_P	GND	HA01_P_CC	GND	DP0_C2M_P	GND	DP1_M2C_P
GND	CLK3_BIDIR_N	GND	CLK1_M2C_N	GND	HA01_N_CC	GND	DP0_C2M_N	GND	DP1_M2C_N
CLK2_BIDIR_P	GND	CLK0_M2C_P	GND	HA00_P_CC	GND	GBTCLK0_M2C_P	GND	DP9_M2C_P	GND
CLK2_BIDIR_N	GND	CLK0_M2C_N	GND	HA00_N_CC	GND	GBTCLK0_M2C_N	GND	DP9_M2C_N	GND
GND	HA03_P	GND	LA00_P_CC	GND	HA05_P	GND	DP0_M2C_P	GND	DP2_M2C_P
HA02_P	HA03_N	LA02_P	LA00_N_CC	HA04_P	HA05_N	GND	DP0_M2C_N	GND	DP2_M2C_N
HA02_N	GND	LA02_N	GND	HA04_N	GND	LA01_P_CC	GND	DP8_M2C_P	GND
GND	HA07_P	GND	LA03_P	GND	HA09_P	LA01_N_CC	GND	DP8_M2C_N	GND
HA06_P	HA07_N	LA04_P	LA03_N	HA08_P	HA09_N	GND	LA06_P	GND	DP3_M2C_P
HA06_N	GND	LA04_N	GND	HA08_N	GND	LA05_P	LA06_N	GND	DP3_M2C_N
GND	HA11_P	GND	LA08_P	GND	HA13_P	LA05_N	GND	DP7_M2C_P	GND
HA10_P	HA11_N	LA07_P	LA08_N	HA12_P	HA13_N	GND	GND	DP7_M2C_N	GND
HA10_N	GND	LA07_N	GND	HA12_N	GND	LA09_P	LA10_P	GND	DP4_M2C_P
GND	HA14_P	GND	LA12_P	GND	HA16_P	LA09_N	LA10_N	GND	DP4_M2C_N
HA17_P_CC	HA14_N	LA11_P	LA12_N	HA15_P	HA16_N	GND	GND	DP6_M2C_P	GND
HA17_N_CC	GND	LA11_N	GND	HA15_N	GND	LA13_P	GND	DP6_M2C_N	GND
GND	HA18_P	GND	LA16_P	GND	HA20_P	LA13_N	LA14_P	GND	DP5_M2C_P
HA21_P	HA18_N	LA15_P	LA16_N	HA19_P	HA20_N	GND	LA14_N	GND	DP5_M2C_N
HA21_N	GND	LA15_N	GND	HA19_N	GND	LA17_P_CC	GND	GBTCLK1_M2C_P	GND
GND	HA22_P	GND	LA20_P	GND	HB03_P	LA17_N_CC	GND	GBTCLK1_M2C_N	GND
HA23_P	HA22_N	LA19_P	LA20_N	HB02_P	HB03_N	GND	LA18_P_CC	GND	DP1_C2M_P
HA23_N	GND	LA19_N	GND	HB02_N	GND	LA23_P	LA18_N_CC	GND	DP1_C2M_N
GND	HB01_P	GND	LA22_P	GND	HB05_P	LA23_N	GND	DP9_C2M_P	GND
HB00_P_CC	HB01_N	LA21_P	LA22_N	HB04_P	HB05_N	GND	GND	DP9_C2M_N	GND
HB00_N_CC	GND	LA21_N	GND	HB04_N	GND	LA26_P	LA27_P	GND	DP2_C2M_P
GND	HB07_P	GND	LA25_P	GND	HB09_P	LA26_N	LA27_N	GND	DP2_C2M_N
HB06_P_CC	HB07_N	LA24_P	LA25_N	HB08_P	HB09_N	GND	GND	DP8_C2M_P	GND
HB06_N_CC	GND	LA24_N	GND	HB08_N	GND	TCK	GND	DP8_C2M_N	GND
GND	HB11_P	GND	LA29_P	GND	HB13_P	TDI	SCL	GND	DP3_C2M_P
HB10_P	HB11_N	LA28_P	LA29_N	HB12_P	HB13_N	TDO	SDA	GND	DP3_C2M_N
HB10_N	GND	LA28_N	GND	HB12_N	GND	3P3VAUX	GND	DP7_C2M_P	GND
GND	HB15_P	GND	LA31_P	GND	HB19_P	TMS	GND	DP7_C2M_N	GND
HB14_P	HB15_N	LA30_P	LA31_N	HB16_P	HB19_N	TRST_L	GA0	GND	DP4_C2M_P
HB14_N	GND	LA30_N	GND	HB16_N	GND	GA1	12P0V	GND	DP4_C2M_N
GND	HB18_P	GND	LA33_P	GND	HB21_P	3P3V	GND	DP6_C2M_P	GND
HB17_P_CC	HB18_N	LA32_P	LA33_N	HB20_P	HB21_N	GND	12P0V	DP6_C2M_N	GND
HB17_N_CC	GND	LA32_N	GND	HB20_N	GND	3P3V	GND	GND	DP5_C2M_P
GND	VIO_B_M2C	GND	VADJ	GND	VADJ	GND	3P3V	GND	DP5_C2M_N
VIO_B_M2C	GND	VADJ	GND	VADJ	GND	3P3V	GND	RES0	GND"""

import numpy as np

rows = text.split("\n")
elements = [row.split("\t") for row in rows]
row_names = [str(i + 1) for i in np.arange(40)]
col_names = ['K', 'J', 'H', 'G', 'F', 'E', 'D', 'C', 'B', 'A']

import re


def reformat(input_string: str):
    sub_strings = input_string.split('_')
    if len(sub_strings) == 1:
        return input_string
    else:
        header = sub_strings[0]

        # 使用正则表达式找到所有数字
        numbers = re.findall(r'\d+', header)

        # 将找到的数字从字符串中移除
        header_without_numbers = re.sub(r'\d+', '', header)
        if len(numbers) == 0:
            return input_string
        else:
            return f"{header_without_numbers}{input_string.replace(header, '')}({int(numbers[0])})"


for i in range(40):
    for j in range(10):
        signal = elements[i][j]
        if signal not in ['GND', '12P0V', '3P3V']:
            print(f"('{col_names[j]}', {row_names[i]}) -> {reformat(signal)},")

pinout_0 = """Pin Number
C35
C37
D32
C34
D35
D8
G6
G7
H7
H8
C10
C11
D11
D12
H10
H11
C14
C15
G12
G13
H13
H14
D14
D15
G15
G16
H16
H17
D17
D18
C18
C19
G9
G10
D9
G19
G18
Signal Name
+12V
+12V
+3.3V
GA0
GA1
CLK1_125M
AD1_DCO+
AD1_DCO-
AD1_DO+
AD1_DO-
AD1_D1+
AD1_D1-
AD1_D2+
AD1_D2-
AD1_D3+
AD1_D3-
AD1_D4+
AD1_D4-
AD1_D5+
AD1_D5-
AD1_D6+
AD1_D6-
AD1_D7+
AD1_D7-
AD1_D8+
AD1_D8-
AD1_D9+
AD1_D9-
AD1_D10+
AD1_D10-
AD1_D11+
AD1_D11-
AD1_SPI_CS
AD1_SPI_SDIO
AD1_SPI_SCLK
AD1_SMI_SCLK
AD1_SMI_SDFS
Description
12V 电源输入
12V 电源输入
3.3V 电源输入
EEPROM 地址位 0 位
EEPROM 地址位 1 位
AD1 芯片的 125M 参考时钟输入
AD1 通道 A 和通道 B LVDS 的数据时钟输出-P.
AD1 通道 A 和通道 B LVDS 的数据时钟输出-N.
AD1 通道 A 和通道 B LVDS 的数据 0 输出-P.
AD1 通道 A 和通道 B LVDS 的数据 0 输出-N.
AD1 通道 A 和通道 B LVDS 的数据 1 输出-P.
AD1 通道 A 和通道 B LVDS 的数据 1 输出-N.
AD1 通道 A 和通道 B LVDS 的数据 2 输出-P.
AD1 通道 A 和通道 B LVDS 的数据 2 输出-N.
AD1 通道 A 和通道 B LVDS 的数据 3 输出-P.
AD1 通道 A 和通道 B LVDS 的数据 3 输出-N.
AD1 通道 A 和通道 B LVDS 的数据 4 输出-P.
AD1 通道 A 和通道 B LVDS 的数据 4 输出-N.
AD1 通道 A 和通道 B LVDS 的数据 5 输出-P.
AD1 通道 A 和通道 B LVDS 的数据 5 输出-N.
AD1 通道 A 和通道 B LVDS 的数据 6 输出-P.
AD1 通道 A 和通道 B LVDS 的数据 6 输出-N.
AD1 通道 A 和通道 B LVDS 的数据 7 输出-P.
AD1 通道 A 和通道 B LVDS 的数据 7 输出-N.
AD1 通道 A 和通道 B LVDS 的数据 8 输出-P.
AD1 通道 A 和通道 B LVDS 的数据 8 输出-N.
AD1 通道 A 和通道 B LVDS 的数据 9 输出-P.
AD1 通道 A 和通道 B LVDS 的数据 9 输出-N.
AD1 通道 A 和通道 B LVDS 的数据 10 输出-P.
AD1 通道 A 和通道 B LVDS 的数据 10 输出-N.
AD1 通道 A 和通道 B LVDS 的数据 11 输出-P.
AD1 通道 A 和通道 B LVDS 的数据 11 输出-N.
AD1 芯片的 SPI 通信片选信号
AD1 芯片的 SPI 通信数据信号
AD1 芯片的 SPI 通信时钟信号
AD1 监控信号串行输出时钟信号
AD1 监控信号串行输出数据帧同步信号"""

def convert(pinout:str):
    content = pinout.split("\n")
    count = len(content) // 3
    for i in range(count):
        name = content[count * 1 + i]
        name = name.replace('+', 'P')
        name = name.replace('-', 'N')
        pin = content[count * 0 + i]
        pin = f"'{pin[0]}', {pin[1:]}"
        description = content[count * 2 + i]
        print(f"val {name} = fmc.signalMap({pin}) // {description}")

pinout_1 = """H19
D20
C22
C23
G21
G22
H22
H23
C26
C27
G24
G25
H25
H26
D26
D27
G27
G28
H28
H29
G30
G31
H31
H32
G33
G34
H34
H35
D21
D23
D24
G37
G36
H37
H20
C30
C31
G39
H40
AD1_SMI_SDO
CLK2_125M
AD2_DCO+
AD2_DCO-
AD2_DO+
AD2_DO-
AD2_D1+
AD2_D1-
AD2_D2+
AD2_D2-
AD2_D3+
AD2_D3-
AD2_D4+
AD2_D4-
AD2_D5+
AD2_D5-
AD2_D6+
AD2_D6-
AD2_D7+
AD2_D7-
AD2_D8+
AD2_D8-
AD2_D9+
AD2_D9-
AD2_D10+
AD2_D10-
AD2_D11+
AD2_D11-
AD2_SPI_CS
AD2_SPI_SDIO
AD2_SPI_SCLK
AD2_SMI_SCLK
AD2_SMI_SDFS
AD2_SMI_SDO
AD_SYNC
SCL
SDA
VADJ
VADJ
AD1 监控信号串行输出数据信号
AD2 芯片的 125M 参考时钟输入
AD2 通道 A 和通道 B LVDS 的数据时钟输出-P.
AD2 通道 A 和通道 B LVDS 的数据时钟输出-N.
AD2 通道 A 和通道 B LVDS 的数据 0 输出-P.
AD2 通道 A 和通道 B LVDS 的数据 0 输出-N.
AD2 通道 A 和通道 B LVDS 的数据 1 输出-P.
AD2 通道 A 和通道 B LVDS 的数据 1 输出-N.
AD2 通道 A 和通道 B LVDS 的数据 2 输出-P.
AD2 通道 A 和通道 B LVDS 的数据 2 输出-N.
AD2 通道 A 和通道 B LVDS 的数据 3 输出-P.
AD2 通道 A 和通道 B LVDS 的数据 3 输出-N.
AD2 通道 A 和通道 B LVDS 的数据 4 输出-P.
AD2 通道 A 和通道 B LVDS 的数据 4 输出-N.
AD2 通道 A 和通道 B LVDS 的数据 5 输出-P.
AD2 通道 A 和通道 B LVDS 的数据 5 输出-N.
AD2 通道 A 和通道 B LVDS 的数据 6 输出-P.
AD2 通道 A 和通道 B LVDS 的数据 6 输出-N.
AD2 通道 A 和通道 B LVDS 的数据 7 输出-P.
AD2 通道 A 和通道 B LVDS 的数据 7 输出-N.
AD2 通道 A 和通道 B LVDS 的数据 8 输出-P.
AD2 通道 A 和通道 B LVDS 的数据 8 输出-N.
AD2 通道 A 和通道 B LVDS 的数据 9 输出-P.
AD2 通道 A 和通道 B LVDS 的数据 9 输出-N.
AD2 通道 A 和通道 B LVDS 的数据 10 输出-P.
AD2 通道 A 和通道 B LVDS 的数据 10 输出-N.
AD2 通道 A 和通道 B LVDS 的数据 11 输出-P.
AD2 通道 A 和通道 B LVDS 的数据 11 输出-N.
AD2 芯片的 SPI 通信片选信号
AD2 芯片的 SPI 通信数据信号
AD2 芯片的 SPI 通信时钟信号
AD2 芯片监控信号串行输出时钟信号
AD2 芯片监控信号串行输出数据帧同步信号
AD2 芯片监控信号串行输出数据信号
数字同步信号
EEPROM 的 I2C 时钟
EEPROM 的 I2C 数据
VADJ 电源输入
VADJ 电源输入"""

convert(pinout_0)
convert(pinout_1)