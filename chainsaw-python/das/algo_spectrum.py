import os
from dataclasses import dataclass
from typing import List

import numpy as np
import shutil

from algo_ops import *

# TODO: 需要一套理论推导数值范围(阅读just right?),需要关注overflow

data_width = 16
shift_values = [15, 15, 14]
data_x = np.load("raw_data_x.npy")
data_y = np.load("raw_data_y.npy")


@dataclass
class TestConfig:
    pulse_count: int
    pulse_valid_points: int
    gauge_points: int
    demodulation_enabled: bool = True


def test_das(test_config: TestConfig, data_index: int):
    test_name = f"test_das_{test_config.pulse_count}_{test_config.pulse_valid_points}_{test_config.gauge_points}_{test_config.demodulation_enabled}"
    all_components = [(80e6, True), (80e6, False)]
    all_strain_rate = [test_component(test_config, data_index, carrier_freq, using_x) for carrier_freq, using_x in
                       all_components]
    all_real = [real for real, imag in all_strain_rate]
    all_imag = [imag for real, imag in all_strain_rate]
    real = np.sum(np.array(all_real), axis=0)
    imag = np.sum(np.array(all_imag), axis=0)
    phase = np.arctan2(imag, real)
    phase_scaling_factor = 1 << 13
    phase = (phase * phase_scaling_factor).astype(np.int32)

    data_length = test_config.pulse_count * test_config.pulse_valid_points * 2
    data_range = np.arange(data_index, data_index + data_length)
    das_result = np.loadtxt("../../full_result.bin", dtype=np.int32)[data_range]

    if os.path.exists(test_name):
        shutil.rmtree(test_name)
    os.mkdir(test_name)

    valid_range = range(3 * test_config.pulse_valid_points + 250, 4 * test_config.pulse_valid_points)  # 避开第一个和最后一个脉冲
    if test_config.demodulation_enabled:
        plot_time_and_frequency(phase.flatten()[valid_range], 500e6, f'{test_name}/phase.png',
                                yours=das_result[::2][valid_range])
    else:
        raw_data = data_x[:test_config.pulse_count, -test_config.pulse_valid_points:]
        your_x = np.empty_like(raw_data.flatten())
        your_x[::2] = das_result[2::4]
        your_x[1::2] = das_result[0::4]
        plot_time_and_frequency(raw_data.flatten()[valid_range], 500e6, f'{test_name}/raw.png',
                                yours=your_x[valid_range])


def test_component(test_config: TestConfig, data_index: int,
                   carrier_freq: float = 80e6, using_x: bool = True, compare=False):
    test_name = f"test_component_{test_config.pulse_count}_{test_config.pulse_valid_points}_{test_config.gauge_points}_{test_config.demodulation_enabled}"

    raw_data = data_x if using_x else data_y

    gauge_points = test_config.gauge_points
    pulse_count = test_config.pulse_count
    # pulse_count = data_x.shape[0]
    pulse_valid_points = test_config.pulse_valid_points
    # pulse_valid_points = data_x.shape[1]

    raw_data = raw_data[:pulse_count, -pulse_valid_points:]
    # PINC会在对应输出中直接生效,因此带有初始offset
    sin = get_sin(pulse_count, pulse_valid_points, True, carrier_freq, offset=2, data_width=data_width)
    cos = get_sin(pulse_count, pulse_valid_points, False, carrier_freq, offset=2, data_width=data_width)

    vecReal = (raw_data * cos) >> shift_values[0]
    vecImag = (raw_data * sin) >> shift_values[0]

    filtered_real: np.ndarray = (signal.lfilter(fir_coeffs, 1, vecReal)).astype(np.int64) >> shift_values[1]
    filtered_imag: np.ndarray = (signal.lfilter(fir_coeffs, 1, vecImag)).astype(np.int64) >> shift_values[1]

    # spatial downsample
    # filtered_real[:, ::2] = filtered_real[:, 1::2]
    # filtered_imag[:, ::2] = filtered_imag[:, 1::2]

    strain_real, strain_imag = get_phase_diff(
        filtered_real, filtered_imag,
        get_delayed(gauge_points, filtered_real, frame_based=True),
        get_delayed(gauge_points, filtered_imag, frame_based=True),
    )
    strain_real = strain_real >> shift_values[2]
    strain_imag = strain_imag >> shift_values[2]

    strain_rate_real, strain_rate_imag = get_phase_diff(
        strain_real, strain_imag,
        get_delayed(pulse_valid_points, strain_real, frame_based=False),
        get_delayed(pulse_valid_points, strain_imag, frame_based=False)
    )
    strain_rate_real = strain_rate_real
    strain_rate_imag = strain_rate_imag

    phase_scaling_factor = 1 << 13
    phase = np.arctan2(strain_rate_imag, strain_rate_real)
    phase = (phase * phase_scaling_factor).astype(np.int32)

    if compare:

        if os.path.exists(test_name):
            shutil.rmtree(test_name)
        os.mkdir(test_name)

        valid_range = range(3 * pulse_valid_points, 4 * pulse_valid_points)  # 避开第一个和最后一个脉冲

        # 比对ComponentDemodulator输出数据(strain_rate矢量),应当达到完全一致
        data_length = test_config.pulse_count * test_config.pulse_valid_points * 2
        data_range = np.arange(data_index, data_index + data_length)
        result = np.loadtxt("../../result.bin", dtype=np.int32)[data_range]
        plot_time_and_frequency(strain_rate_real.flatten()[valid_range], 500e6, f'{test_name}/strain_rate_real.png',
                                result[::2][valid_range])
        plot_time_and_frequency(strain_rate_imag.flatten()[valid_range], 500e6, f'{test_name}/strain_rate_imag.png',
                                result[1::2][valid_range])
        save_hist([raw_data, vecReal, filtered_real, strain_real, strain_rate_real])
        # plot_waterfall(phase)

    return (strain_rate_real, strain_rate_imag)


if __name__ == '__main__':

    # for das demodulator
    test_configs = [
        TestConfig(5, 2000, 100, True),
        TestConfig(5, 2000, 100, False),
        TestConfig(5, 1000, 50, True),
    ]
    data_idx = 0
    for test_config in test_configs:
        test_das(test_config, data_idx)
        data_length = test_config.pulse_count * test_config.pulse_valid_points * 2
        data_idx += data_length
