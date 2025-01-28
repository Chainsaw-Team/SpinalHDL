import matplotlib.pyplot as plt
import numpy as np


def swap(data):
    ret = np.zeros_like(data)
    ret[::2] = data[1::2]
    ret[1::2] = data[::2]
    return ret


def get_sin(pulse_count: int, pulse_valid_points, is_sin: bool, freq: float, offset: float = 0):
    phases = np.arange(offset, pulse_valid_points + offset, 1) * 2 * np.pi * freq / 500e6
    wave = np.sin(phases) if is_sin else np.cos(phases)
    wave = np.tile(wave, (pulse_count, 1))
    return (wave * (1 << 15)).astype(np.int32)


def get_delayed(delay_points, data: np.ndarray, frame_based=False):
    assert delay_points >= 0
    assert data.ndim <= 2
    if delay_points == 0:
        return data
    elif delay_points > 0:
        if frame_based:
            delayed = np.zeros_like(data)
            delayed[:, delay_points:] = data[:, :-delay_points]
            return delayed
        else:
            data_1d = data.flatten()
            delayed = np.zeros_like(data_1d)
            delayed[delay_points:] = data_1d[:-delay_points]
            return delayed.reshape(data.shape)


def get_delayed_by_frame(data, last_frame):
    delayed = np.empty_like(data)
    delayed[0, :] = last_frame
    delayed[1:, :] = data[:-1, :]
    return delayed


def get_phase_diff(real, imag, real_delayed, imag_delayed):
    target_real = real * real_delayed + imag * imag_delayed
    target_imag = real * imag_delayed - real_delayed * imag
    return target_real, target_imag


def plot_time_and_frequency(golden, fs, file_name, yours=None, plot_range=None):
    if plot_range is not None:
        golden = golden[plot_range]
        if yours is not None:
            yours = yours[plot_range]

    # 创建时间轴
    t = np.arange(len(golden)) / fs  # 时间轴，长度与数据序列一致

    # 频域计算 (傅里叶变换)
    fft_values = np.abs(np.fft.fft(golden))  # 傅里叶变换的幅值
    freqs = np.fft.fftfreq(len(golden), 1 / fs)  # 生成频率轴

    # 绘制时域和频域图
    plt.figure(figsize=(12, 6))

    # 子图 1：时域图
    plt.subplot(2, 1, 1)
    plt.plot(t, golden)
    if yours is not None:
        plt.plot(t, yours)
        plt.legend(["golden", "yours"])
    plt.title("Time Domain Signal")
    plt.xlabel("Time (s)")
    plt.ylabel("Amplitude")
    plt.grid()

    # 子图 2：频域图 (只绘制正频部分)
    plt.subplot(2, 1, 2)
    half_n = len(golden) // 2  # 傅里叶变换的对称性，只取正频部分
    plt.plot(freqs[:half_n], fft_values[:half_n])
    if yours is not None:
        fft_values = np.abs(np.fft.fft(yours))  # 傅里叶变换的幅值
        plt.plot(freqs[:half_n], fft_values[:half_n])
        plt.legend(["golden", "yours"])
    plt.title("Frequency Domain")
    plt.xlabel("Frequency (Hz)")
    plt.ylabel("Amplitude")
    plt.grid()

    # 调整布局并显示图像
    plt.tight_layout()
    plt.savefig(f"./test/{file_name}")


fir_coeffs = np.array(
    [38, 39, 40, 43, 47, 51, 57, 63, 71, 79, 89, 99, 110, 122, 135, 149, 163, 178, 194, 211, 228, 245, 263, 281, 300,
     319, 338, 357, 375, 394, 413, 431, 449, 466, 483, 500, 515, 530, 544, 558, 570, 581, 592, 601, 609, 616, 621, 626,
     629, 631, 631, 631, 629, 626, 621, 616, 609, 601, 592, 581, 570, 558, 544, 530, 515, 500, 483, 466, 449, 431, 413,
     394, 375, 357, 338, 319, 300, 281, 263, 245, 228, 211, 194, 178, 163, 149, 135, 122, 110, 99, 89, 79, 71, 63, 57,
     51, 47, 43, 40, 39, 38]).astype(np.float32)

def plot_waterfall(data):
    plt.figure(figsize=(12, 12))
    plt.subplot(1, 1, 1)
    plt.imshow(data, aspect='auto', vmin=-1000, vmax=1000, cmap='rainbow')
    plt.savefig('waterfall.png')