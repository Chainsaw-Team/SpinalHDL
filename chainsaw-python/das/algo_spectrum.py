from dataclasses import dataclass
import os
from typing import List
from unittest import case

import scipy.signal as signal
from fontTools.mtiLib import parseLookupRecords

from algo_ops import *


@dataclass
class TestConfig:
    pulse_count: int
    pulse_valid_points: int
    gauge_points: int
    demodulation_enabled: bool = True


def test_once(test_config: TestConfig, test_type: str, data_index: int):
    test_name = f"test_{test_type}_{test_config.pulse_count}_{test_config.pulse_valid_points}_{test_config.gauge_points}_{test_config.demodulation_enabled}"
    data_x = np.load("raw_data_x.npy")
    data_y = np.load("raw_data_y.npy")

    gauge_points = test_config.gauge_points
    pulse_count = test_config.pulse_count
    pulse_valid_points = test_config.pulse_valid_points
    carrier_freq = 80e6

    data_x = data_x[:pulse_count, -pulse_valid_points:]
    data_y = data_y[:pulse_count, -pulse_valid_points:]
    sin = get_sin(pulse_count, pulse_valid_points, True, carrier_freq, offset=2)  # PINC会在对应输出中直接生效,因此带有初始offset
    cos = get_sin(pulse_count, pulse_valid_points, False, carrier_freq, offset=2)

    vecReal = (data_x * cos + data_y * cos) >> 17
    vecImag = (data_x * sin + data_y * sin) >> 17

    filtered_real: np.ndarray = (signal.lfilter(fir_coeffs, 1, vecReal) / (1 << 24))
    filtered_imag: np.ndarray = (signal.lfilter(fir_coeffs, 1, vecImag) / (1 << 24))

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

    import shutil

    if os.path.exists(test_name):
        shutil.rmtree(test_name)
    os.mkdir(test_name)

    valid_range = range(3 * pulse_valid_points, 4 * pulse_valid_points)
    match test_type:
        case "component":
            data_length = test_config.pulse_count * test_config.pulse_valid_points
            data_range = np.arange(data_index, data_index + data_length)
            your_upper = np.fromfile("../../upper_result.bin", dtype=np.int16).astype(np.float32)[data_range]
            your_lower = np.fromfile("../../lower_result.bin", dtype=np.int16).astype(np.float32)[data_range]
            your_float = np.fromfile("../../result_float32.bin", dtype=np.float32)[data_range]
            plot_time_and_frequency(np.floor(np.log2(your_float)) + 16 + 1, 500e6, f'{test_name}/effective_bits.png',
                                    plot_range=valid_range)
            plot_time_and_frequency(strain_rate_real.flatten(), 500e6, f'{test_name}/your_strain_rate_real_all.png',
                                    yours=your_float)
            plot_time_and_frequency(strain_rate_real.flatten(), 500e6, f'{test_name}/your_strain_rate_real.png',
                                    yours=your_float,
                                    plot_range=valid_range)
            plot_time_and_frequency(phase.flatten(), 500e6, f'{test_name}/your_phase.png', yours=your_lower,
                                    plot_range=valid_range)
            plot_time_and_frequency(phase.flatten(), 500e6, f'{test_name}/your_phase_all.png', yours=your_lower)
        case "das":
            data_length = test_config.pulse_count * test_config.pulse_valid_points * 2
            data_range = np.arange(data_index, data_index + data_length)
            your_full = np.fromfile("../../full_result.bin", dtype=np.int16).astype(np.float32)[data_range]
            if not test_config.demodulation_enabled:
                your_data_x = np.zeros_like(data_x.flatten())
                your_data_y = np.zeros_like(data_y.flatten())
                your_data_x[::2] = your_full[2::4]
                your_data_x[1::2] = your_full[0::4]
                your_data_y[::2] = your_full[3::4]
                your_data_y[1::2] = your_full[1::4]
                plot_time_and_frequency(data_x.flatten(), 500e6, f'{test_name}/your_data_x.png', yours=your_data_x,
                                        plot_range=valid_range)
                plot_time_and_frequency(data_y.flatten(), 500e6, f'{test_name}/your_data_y.png', yours=your_data_y,
                                        plot_range=valid_range)
            else:
                your_data = np.zeros_like(phase.flatten())
                your_data[::2] = your_full[2::4]
                your_data[1::2] = your_full[0::4]
                plot_time_and_frequency(phase.flatten(), 500e6, f'{test_name}/your_phase.png', yours=your_data,
                                        plot_range=valid_range)


def test_all(test_configs: List[TestConfig], test_type: str):
    data_idx = 0
    for test_config in test_configs:
        test_once(test_config, test_type, data_idx)
        data_length = test_config.pulse_count * test_config.pulse_valid_points * (1 if test_type == "component" else 2)
        data_idx += data_length


# for component demodulator
test_configs = [TestConfig(5, 2000, 100), TestConfig(5, 1000, 50)]
test_all(test_configs, "component")
# for das demodulator
test_configs = [
    TestConfig(5, 2000, 100, True),
    TestConfig(5, 2000, 100, False),
    TestConfig(5, 1000, 50, True),
]
test_all(test_configs, "das")
