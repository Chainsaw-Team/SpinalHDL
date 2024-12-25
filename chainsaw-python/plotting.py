import argparse
import os

import numpy as np
import matplotlib.pyplot as plt
from matplotlib.axes import Axes
from matplotlib.figure import Figure
import scipy.signal as signal


def enbw(signal):
    # 根据Matlab文档编写的enbw函数
    numer = np.sum(signal * signal)
    denom = np.abs(np.sum(signal)) ** 2
    return numer / denom * len(signal)


def seafom_fft(data, fs):
    """
    根据SEAFOM标准7.4.4节要求编写的FFT函数,将时间序列转换为ASD,其输入为时长1s的,与应变对应的radians时间序列,输出单位为rads/rt-Hz
    当输入为strain而非radians时,由于radians与strain存在正比例关系,输出单位为strain/rt-Hz
    """

    data = data.flatten()
    l_fft = len(data)
    fft_resolution = fs / l_fft

    w = signal.windows.blackmanharris(l_fft)  # 生成Blackman-Harris窗
    g_w = l_fft / np.sum(w)  # 计算窗函数增益

    windowed_data = data * w * g_w  # 加窗
    fft_data = np.fft.fft(windowed_data)  # FFT
    normalized_fft_data = fft_data / l_fft  # 归一化
    ssb = np.abs(np.sqrt(2) * normalized_fft_data[:int(l_fft / 2)])  # 单边带处理

    w_nbw = enbw(w)  #
    noise_bw = np.sqrt(w_nbw * fft_resolution)

    ssb_norm = ssb / noise_bw  # 噪声归一化,至此,我们得到了结果,单位为rads/rt-Hz
    freqs = fft_resolution * np.arange(0, int(l_fft / 2))

    return freqs, ssb_norm


def create_figure(figsize, nrows: int, ncols: int):
    fig: Figure = plt.figure(figsize=figsize)
    spec = plt.GridSpec(ncols=ncols, nrows=nrows, figure=fig)
    axes = [[fig.add_subplot(spec[i, j]) for j in range(ncols)] for i in range(nrows)]
    return fig, axes


def save_sequence_spectrum(file_path: str, data, fs, freq_distance=1e6):
    """绘制数据的频谱并计算其SFDR"""
    fig, axes = create_figure((16, 9), 2, 1)
    target0, target1 = axes[0][0], axes[1][0]

    target0.plot(data)
    if target1 is not None:
        freqs, ssb_norm = seafom_fft(data, fs)
        ssb_norm = 20 * np.log10(ssb_norm)
        ssb_norm = ssb_norm - np.max(ssb_norm)
        target1.plot(freqs, ssb_norm)

    def find_side_lobe(psd):
        distance_in_index = int(freq_distance / (fs / 2 / len(psd)))
        peaks, properties = signal.find_peaks(psd, distance=distance_in_index)
        peak_heights = properties['peak_heights'] if 'peak_heights' in properties else ssb_norm[peaks]

        # 找到主瓣：峰值最高的那个
        main_lobe_index = np.argmax(peak_heights)  # 主瓣索引
        main_lobe_peak = peaks[main_lobe_index]  # 主瓣位置

        # 排除主瓣后的旁瓣
        side_lobes = np.delete(peaks, main_lobe_index)  # 剔除主瓣的峰索引
        side_lobe_heights = np.delete(peak_heights, main_lobe_index)

        # 找到最大的旁瓣
        if len(side_lobe_heights) > 0:  # 确保有旁瓣
            largest_side_lobe_index = np.argmax(side_lobe_heights)  # 最大旁瓣索引
            largest_side_lobe = side_lobes[largest_side_lobe_index]  # 最大旁瓣位置
            return main_lobe_peak, largest_side_lobe

    main_lobe_index, side_lobe_index = find_side_lobe(ssb_norm)
    main_lobe_floor = ssb_norm[main_lobe_index]
    side_lobe_floor = ssb_norm[side_lobe_index]
    target1.axvline(freqs[main_lobe_index], color='r', linestyle='--')
    target1.axvline(freqs[side_lobe_index], color='r', linestyle='--')
    target1.axhline(main_lobe_floor, color='g', linestyle='--')
    target1.axhline(side_lobe_floor, color='b', linestyle='--')
    snr = main_lobe_floor - side_lobe_floor
    target1.legend([f'side to main {snr} dB'])

    fig.savefig(file_path)
    return snr



