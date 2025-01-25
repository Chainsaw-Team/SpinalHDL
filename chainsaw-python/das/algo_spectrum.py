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
    # data = data[200:]
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

import numpy as np

# 读取 phase_result.bin
your_upper = np.fromfile("/home/ltr/SpinalHDL/upper_result.bin", dtype=np.int16)
your_lower = np.fromfile("/home/ltr/SpinalHDL/lower_result.bin", dtype=np.int16)

# yours = yours[:100]
plot_time_and_frequency(your_upper, 500e6, 'your_upper.png')
plot_time_and_frequency(your_lower, 500e6, 'your_lower.png')


