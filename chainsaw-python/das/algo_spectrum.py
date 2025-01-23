import matplotlib.pyplot as plt
import numpy as np
import scipy.signal as signal

pulse_valid_points = 1024
gauge_points = 10


def swap(data):
    ret = np.zeros_like(data)
    ret[::2] = data[1::2]
    ret[1::2] = data[::2]
    return ret


def get_sin(is_sin: bool, freq: float, offset: float = 0):
    phases = np.arange(offset, pulse_valid_points + offset, 1) * 2 * np.pi * freq / 500e6
    wave = np.sin(phases) if is_sin else np.cos(phases)
    return (wave * 32767).astype(np.int32)


def plot_time_and_frequency(data, fs, file_name):
    """
    绘制给定数据的时域和频域图。

    参数:
    - data: 输入信号数据序列 (1D array-like)
    - fs: 采样率，单位 Hz (float)

    输出:
    - 绘制信号的时域图和频域图
    """
    data = data[200:]
    # 创建时间轴
    t = np.arange(len(data)) / fs  # 时间轴，长度与数据序列一致

    # 频域计算 (傅里叶变换)
    fft_values = np.abs(np.fft.fft(data))  # 傅里叶变换的幅值
    freqs = np.fft.fftfreq(len(data), 1 / fs)  # 生成频率轴

    # 绘制时域和频域图
    plt.figure(figsize=(12, 6))

    # 子图 1：时域图
    plt.subplot(2, 1, 1)
    plt.plot(t, data)
    plt.title("Time Domain Signal")
    plt.xlabel("Time (s)")
    plt.ylabel("Amplitude")
    plt.grid()

    # 子图 2：频域图 (只绘制正频部分)
    plt.subplot(2, 1, 2)
    half_n = len(data) // 2  # 傅里叶变换的对称性，只取正频部分
    plt.plot(freqs[:half_n], fft_values[:half_n])
    plt.title("Frequency Domain")
    plt.xlabel("Frequency (Hz)")
    plt.ylabel("Amplitude")
    plt.grid()

    # 调整布局并显示图像
    plt.tight_layout()
    plt.savefig(file_name)


fir_coeffs = np.array(
    [38, 39, 40, 43, 47, 51, 57, 63, 71, 79, 89, 99, 110, 122, 135, 149, 163, 178, 194, 211, 228, 245, 263, 281, 300,
     319, 338, 357, 375, 394, 413, 431, 449, 466, 483, 500, 515, 530, 544, 558, 570, 581, 592, 601, 609, 616, 621, 626,
     629, 631, 631, 631, 629, 626, 621, 616, 609, 601, 592, 581, 570, 558, 544, 530, 515, 500, 483, 466, 449, 431, 413,
     394, 375, 357, 338, 319, 300, 281, 263, 245, 228, 211, 194, 178, 163, 149, 135, 122, 110, 99, 89, 79, 71, 63, 57,
     51, 47, 43, 40, 39, 38]).astype(np.float32)

data_x = get_sin(True, 20e6)
data_y = get_sin(False, 30e6)
sin80 = get_sin(True, 80e6)
cos80 = get_sin(False, 80e6)

vecReal = (data_x * cos80 + data_y * cos80) >> 16
vecImag = (data_x * sin80 + data_y * sin80) >> 16

filtered_real = signal.lfilter(fir_coeffs, 1, vecReal).astype(np.int32) >> 16
filtered_imag = signal.lfilter(fir_coeffs, 1, vecImag).astype(np.int32) >> 16

phase = np.atan2(filtered_imag, filtered_real)
magnitude = filtered_imag ** 2 + filtered_real ** 2

unwrapped = np.unwrap(phase)

strain = unwrapped - np.mean(unwrapped)

plot_time_and_frequency(vecReal, 500e6, 'goldenReal.png')
plot_time_and_frequency(vecImag, 500e6, 'goldenImag.png')
plot_time_and_frequency(filtered_real, 500e6, 'goldenFilteredReal.png')
plot_time_and_frequency(phase, 500e6, 'goldenPhase.png')
plot_time_and_frequency(unwrapped, 500e6, 'goldenUnwrapped.png')

yours = np.array(
    [16141, 16221, 19445, 22107, 21877, 18921, 19392, 21759, 23941, 23851, 19116, 17778, 18864, 19961, 20383, 20536,
     21157, 22081, 22534, 21601, 19859, 19268, 19764, 20567, 21171, 21461, 21606, 21728, 21663, 21124, 20382, 20163,
     20586, 21253, 21691, 21673, 21469, 21418, 21449, 21257, 20902, 20821, 21158, 21622, 21793, 21559, 21336, 21435,
     21670, 21662, 21384, 21219, 21402, 21675, 21698, 21481, 21459, 21794, 22160, 22136, 21658, 21271, 21337, 21593,
     21711, 21719, 21949, 22427, 22764, 22460, 21538, 20936, 21100, 21642, 22159, 22503, 22812, 23187, 23305, 22512,
     20878, 20166, 20894, 22283, 23554, 24025, 23667, 23509, 23765, 22672, 19218, 18777, 20860, 23769, -25231, 25503,
     20594, 21673, 24760, -24971, 16077, 24244, -19180, -11104, -2873, 5666, 14504, 23565, -17654, 10058, 18797, 24963,
     -22734, -21077, -17399, -11520, -3449, 25731, -16189, -7314, 1460, 10034, 18334, -25027, -16999, -8865, -389,
     10697, -11840, -2443, 6233, 14652, 22886, -20438, -12287, -4011, 4571, 14536, -7314, 2555, 11184, 19507, -23789,
     -15659, -7442, 932, 9594, 19084, -3833, 7506, 16077, 24244, -19180, -11104, -2873, 5666, 14504, 23565, -17654,
     10058, 18797, 24963, -22734, -21077, -17399, -11520, -3449, 25731, -16189, -7314, 1460, 10034, 18334, -25027,
     -16999, -8865, -389, 10697, -11840, -2443, 6233, 14652, 22886, -20438, -12287, -4011, 4571, 14536, -7314, 2555,
     11184, 19507, -23789, -15659, -7442, 932, 9594, 19084, -3833, 7506, 16077, 24244, -19180, -11104, -2873, 5666,
     14504, 23565, -17654, 10058, 18797, 24963, -22734, -21077, -17399, -11520, -3449, 25731, -16189, -7314, 1460,
     10034, 18334, -25027, -16999, -8865, -389, 10697, -11840, -2443, 6233, 14652, 22886, -20438, -12287, -4011, 4571,
     14536, -7314, 2555, 11184, 19507, -23789, -15659, -7442, 932, 9594, 19084, -3833, 7506, 16077, 24244, -19180,
     -11104, -2873, 5666, 14504, 23565, -17654, 10058, 18797, 24963, -22734, -21077, -17399, -11520, -3449, 25731,
     -16189, -7314, 1460, 10034, 18334, -25027, -16999, -8865, -389, 10697, -11840, -2443, 6233, 14652, 22886, -20438,
     -12287, -4011, 4571, 14536, -7314, 2555, 11184, 19507, -23789, -15659, -7442, 932, 9594, 19084, -3833, 7506, 16077,
     24244, -19180, -11104, -2873, 5666, 14504, 23565, -17654, 10058, 18797, 24963, -22734, -21077, -17399, -11520,
     -3449, 25731, -16189, -7314, 1460, 10034, 18334, -25027, -16999, -8865, -389, 10697, -11840, -2443, 6233, 14652,
     22886, -20438, -12287, -4011, 4571, 14536, -7314, 2555, 11184, 19507, -23789, -15659, -7442, 932, 9594, 19084,
     -3833, 7506, 16077, 24244, -19180, -11104, -2873, 5666, 14504, 23565, -17654, 10058, 18797, 24963, -22734, -21077,
     -17399, -11520, -3449, 25731, -16189, -7314, 1460, 10034, 18334, -25027, -16999, -8865, -389, 10697, -11840, -2443,
     6233, 14652, 22886, -20438, -12287, -4011, 4571, 14536, -7314, 2555, 11184, 19507, -23789, -15659, -7442, 932,
     9594, 19084, -3833, 7506, 16077, 24244, -19180, -11104, -2873, 5666, 14504, 23565, -17654, 10058, 18797, 24963,
     -22734, -21077, -17399, -11520, -3449, 25731, -16189, -7314, 1460, 10034, 18334, -25027, -16999, -8865, -389,
     10697, -11840, -2443, 6233, 14652, 22886, -20438, -12287, -4011, 4571, 14536, -7314, 2555, 11184, 19507, -23789,
     -15659, -7442, 932, 9594, 19084, -3833, 7506, 16077, 24244, -19180, -11104, -2873, 5666, 14504, 23565, -17654,
     10058, 18797, 24963, -22734, -21077, -17399, -11520, -3449, 25731, -16189, -7314, 1460, 10034, 18334, -25027,
     -16999, -8865, -389, 10697, -11840, -2443, 6233, 14652, 22886, -20438, -12287, -4011, 4571, 14536, -7314, 2555,
     11184, 19507, -23789, -15659, -7442, 932, 9594, 19084, -3833, 7506, 16077, 24244, -19180, -11104, -2873, 5666,
     14504, 23565, -17654, 10058, 18797, 24963, -22734, -21077, -17399, -11520, -3449, 25731, -16189, -7314, 1460,
     10034, 18334, -25027, -16999, -8865, -389, 10697, -11840, -2443, 6233, 14652, 22886, -20438, -12287, -4011, 4571,
     14536, -7314, 2555, 11184, 19507, -23789, -15659, -7442, 932, 9594, 19084, -3833, 7506, 16077, 24244, -19180,
     -11104, -2873, 5666, 14504, 23565, -17654, 10058, 18797, 24963, -22734, -21077, -17399, -11520, -3449, 25731,
     -16189, -7314, 1460, 10034, 18334, -25027, -16999, -8865, -389, 10697, -11840, -2443, 6233, 14652, 22886, -20438,
     -12287, -4011, 4571, 14536, -7314, 2555, 11184, 19507, -23789, -15659, -7442, 932, 9594, 19084, -3833, 7506, 16077,
     24244, -19180, -11104, -2873, 5666, 14504, 23565, -17654, 10058, 18797, 24963, -22734, -21077, -17399, -11520,
     -3449, 25731, -16189, -7314, 1460, 10034, 18334, -25027, -16999, -8865, -389, 10697, -11840, -2443, 6233, 14652,
     22886, -20438, -12287, -4011, 4571, 14536, -7314, 2555, 11184, 19507, -23789, -15659, -7442, 932, 9594, 19084,
     -3833, 7506, 16077, 24244, -19180, -11104, -2873, 5666, 14504, 23565, -17654, 10058, 18797, 24963, -22734, -21077,
     -17399, -11520, -3449, 25731, -16189, -7314, 1460, 10034, 18334, -25027, -16999, -8865, -389, 10697, -11840, -2443,
     6233, 14652, 22886, -20438, -12287, -4011, 4571, 14536, -7314, 2555, 11184, 19507, -23789, -15659, -7442, 932,
     9594, 19084, -3833, 7506, 16077, 24244, -19180, -11104, -2873, 5666, 14504, 23565, -17654, 10058, 18797, 24963,
     -22734, -21077, -17399, -11520, -3449, 25731, -16189, -7314, 1460, 10034, 18334, -25027, -16999, -8865, -389,
     10697, -11840, -2443, 6233, 14652, 22886, -20438, -12287, -4011, 4571, 14536, -7314, 2555, 11184, 19507, -23789,
     -15659, -7442, 932, 9594, 19084, -3833, 7506, 16077, 24244, -19180, -11104, -2873, 5666, 14504, 23565, -17654,
     10058, 18797, 24963, -22734, -21077, -17399, -11520, -3449, 25731, -16189, -7314, 1460, 10034, 18334, -25027,
     -16999, -8865, -389, 10697, -11840, -2443, 6233, 14652, 22886, -20438, -12287, -4011, 4571, 14536, -7314, 2555,
     11184, 19507, -23789, -15659, -7442, 932, 9594, 19084, -3833, 7506, 16077, 24244, -19180, -11104, -2873, 5666,
     14504, 23565, -17654, 10058, 18797, 24963, -22734, -21077, -17399, -11520, -3449, 25731, -16189, -7314, 1460,
     10034, 18334, -25027, -16999, -8865, -389, 10697, -11840, -2443, 6233, 14652, 22886, -20438, -12287, -4011, 4571,
     14536, -7314, 2555, 11184, 19507, -23789, -15659, -7442, 932, 9594, 19084, -3833, 7506, 16077, 24244, -19180,
     -11104, -2873, 5666, 14504, 23565, -17654, 10058, 18797, 24963, -22734, -21077, -17399, -11520, -3449, 25731,
     -16189, -7314, 1460, 10034, 18334, -25027, -16999, -8865, -389, 10697, -11840, -2443, 6233, 14652, 22886, -20438,
     -12287, -4011, 4571, 14536, -7314, 2555, 11184, 19507, -23789, -15659, -7442, 932, 9594, 19084, -3833, 7506, 16077,
     24244, -19180, -11104, -2873, 5666, 14504, 23565, -17654, 10058, 18797, 24963, -22734, -21077, -17399, -11520,
     -3449, 25731, -16189, -7314, 1460, 10034, 18334, -25027, -16999, -8865, -389, 10697, -11840, -2443, 6233, 14652,
     22886, -20438, -12287, -4011, 4571, 14536, -7314, 2555, 11184, 19507, -23789, -15659, -7442, 932, 9594, 19084,
     -3833, 7506, 16077, 24244, -19180, -11104, -2873, 5666, 14504, 23565, -17654, 10058, 18797, 24963, -22734, -21077,
     -17399, -11520, -3449, 25731, -16189, -7314, 1460, 10034, 18334, -25027, -16999, -8865, -389, 10697, -11840, -2443,
     6233, 14652, 22886, -20438, -12287, -4011, 4571, 14536, -7314, 2555, 11184, 19507, -23789, -15659, -7442, 932,
     9594, 19084, -3833, 7506, 16077, 24244, -19180, -11104, -2873, 5666, 14504, 23565, -17654, 10058, 18797, 24963,
     -22734, -21077, -17399, -11520, -3449, 25731, -16189, -7314, 1460, 10034, 18334, -25027])

plot_time_and_frequency(yours, 500e6, 'yours.png')
yours = yours / (1 << 13)
your_unwrapped = np.unwrap(yours)
plot_time_and_frequency(your_unwrapped, 500e6, 'yours_unwrapped.png')
# plot_time_and_frequency(yours[::2], 250e6, 'yours0.png')
# plot_time_and_frequency(yours[1::2], 250e6, 'yours1.png')
