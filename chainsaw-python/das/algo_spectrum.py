import os

import scipy.signal as signal

from algo_ops import *

data_x = np.load("raw_data_x.npy")
data_y = np.load("raw_data_y.npy")

gauge_points = 100
# pulse_count = data_x.shape[0]
# pulse_valid_points = data_x.shape[1]
pulse_count = 10
pulse_valid_points = 2000
carrier_freq = 80e6

data_x = data_x[:pulse_count, -pulse_valid_points:]
data_y = data_y[:pulse_count, -pulse_valid_points:]
sin = get_sin(pulse_count, pulse_valid_points, True, carrier_freq, offset=2)  # PINC会在对应输出中直接生效,因此带有初始offset
cos = get_sin(pulse_count, pulse_valid_points, False, carrier_freq, offset=2)

vecReal = (data_x * cos + data_y * cos) >> 17
vecImag = (data_x * sin + data_y * sin) >> 17

filtered_real:np.ndarray = (signal.lfilter(fir_coeffs, 1, vecReal) / (1 << 24))
filtered_imag:np.ndarray = (signal.lfilter(fir_coeffs, 1, vecImag) / (1 << 24))

filtered_real = filtered_real.astype(np.float32)
filtered_imag = filtered_imag.astype(np.float32)

strain_real, strain_imag = get_phase_diff(
    filtered_real, filtered_imag,
    get_delayed(gauge_points, filtered_real, frame_based=True),
    get_delayed(gauge_points, filtered_imag, frame_based=True),
)

# TODO: 需要一套理论推导数值范围(阅读just right?),需要关注overflow
strain_rate_real, strain_rate_imag = get_phase_diff(
    strain_real, strain_imag,
    get_delayed(pulse_valid_points, strain_real, frame_based=False),
    get_delayed(pulse_valid_points, strain_imag, frame_based=False)
)

phase_scaling_factor = 1 << 13
phase = np.arctan2(strain_rate_imag, strain_rate_real)
phase = (phase * phase_scaling_factor).astype(np.int32)


your_upper = np.fromfile("../../upper_result.bin", dtype=np.int16).astype(np.float32)
your_lower = np.fromfile("../../lower_result.bin", dtype=np.int16).astype(np.float32)
your_float = np.fromfile("../../result_float32.bin", dtype=np.float32)

import shutil
if os.path.exists('test'):
    shutil.rmtree('test')
os.mkdir('test')

valid_range = range(5 * pulse_valid_points, 6 * pulse_valid_points)
plot_time_and_frequency(np.floor(np.log2(your_float)) + 16 + 1, 500e6, 'effective_bits.png', plot_range=valid_range)
plot_time_and_frequency(strain_rate_real.flatten(), 500e6, 'your_strain_rate_real_all.png', yours=your_float)
plot_time_and_frequency(strain_rate_real.flatten(), 500e6, 'your_strain_rate_real.png', yours=your_float, plot_range=valid_range)
plot_time_and_frequency(phase.flatten(), 500e6, 'your_phase.png', yours=your_lower, plot_range=valid_range)
plot_time_and_frequency(phase.flatten(), 500e6, 'your_phase_all.png', yours=your_lower)


# plot_waterfall(phase)
# plot_waterfall(your_lower.reshape(pulse_count, -1))