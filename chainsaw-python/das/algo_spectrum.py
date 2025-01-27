import matplotlib.pyplot as plt
import numpy as np
import scipy.signal as signal
from scipy.constants import golden

from algo_ops import *

# TODO: 不应flatten,应当基于

gauge_points = 100

pulse_valid_points = 2000
pulse_count = 50

# TODO: make it the same as algoVByVector

data_x = np.load("raw_data_x.npy")
data_y = np.load("raw_data_y.npy")
pulse_count = data_x.shape[0]
pulse_valid_points = data_x.shape[1]
data_x = data_x[:pulse_count, -pulse_valid_points:]
data_y = data_y[:pulse_count, -pulse_valid_points:]
sin80 = get_sin(pulse_count, pulse_valid_points, True, 80e6, offset=2)  # PINC会在对应输出中直接生效,因此带有初始offset
cos80 = get_sin(pulse_count, pulse_valid_points, False, 80e6, offset=2)

vecReal = (data_x * cos80 + data_y * cos80) >> 17
vecImag = (data_x * sin80 + data_y * sin80) >> 17

filtered_real = signal.lfilter(fir_coeffs, 1, vecReal) / (1 << 16)
filtered_imag = signal.lfilter(fir_coeffs, 1, vecImag) / (1 << 16)

strain_real, strain_imag = get_phase_diff(
    filtered_real, filtered_imag,
    get_delayed(gauge_points, filtered_real, frame_based=True),
    get_delayed(gauge_points, filtered_imag, frame_based=True),
)

strain_rate_real, strain_rate_imag = get_phase_diff(
    strain_real, strain_imag,
    get_delayed(pulse_valid_points, strain_real, frame_based=False),
    get_delayed(pulse_valid_points, strain_imag, frame_based=False)
)

phase = np.arctan2(strain_rate_imag, strain_rate_real)
print(f"shape = {phase.shape}")

# your_upper = np.fromfile("/home/ltr/SpinalHDL/upper_result.bin", dtype=np.int16).astype(np.float32)
# your_lower = np.fromfile("/home/ltr/SpinalHDL/lower_result.bin", dtype=np.int16).astype(np.float32)
# your_float = np.fromfile("/home/ltr/SpinalHDL/result_float32.bin", dtype=np.float32)
#
# # 将整数以 int16 表示的二进制输出
#
# plot_time_and_frequency(data_x.flatten(), 500e6, 'raw_x.png')
# plot_time_and_frequency(filtered_real.flatten(), 500e6, 'your_filtered_real.png', yours=your_lower)
# plot_time_and_frequency(filtered_imag.flatten(), 500e6, 'your_filtered_imag.png', yours=your_upper)
#
# plot_time_and_frequency(strain_rate_real.flatten(), 500e6, 'your_strain_rate_real.png', yours=your_float)

print(np.max(phase), np.min(phase))
skipped_ratio = 0.02
sorted_pixels = np.sort(phase.flatten())
drop = int(len(sorted_pixels) * skipped_ratio)  # 排除最大/最小的数据以避免极端值对图像对比度的影响
vmin = sorted_pixels[drop]
vmax = sorted_pixels[-drop]
plt.imshow(phase, aspect='auto', vmin=vmin, vmax=vmax, cmap='rainbow')
plt.savefig('waterfall.png')
