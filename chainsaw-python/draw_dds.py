from plotting import *


def draw_dds(bin_file_path, png_file_path, fs, target_freq, freq_distance=1e6):
    """绘制数据的频谱并计算其SFDR"""
    with open(bin_file_path, "rb") as f:
        data = np.frombuffer(f.read(), dtype=">i4")

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
    # target1.axvline(freqs[main_lobe_index], color='r', linestyle='--')
    # target1.axvline(freqs[side_lobe_index], color='r', linestyle='--')
    target1.axhline(main_lobe_floor, color='g', linestyle='--')
    target1.axhline(side_lobe_floor, color='b', linestyle='--')
    sfdr = main_lobe_floor - side_lobe_floor
    target1.legend([f'SFDR: {sfdr} dB'])

    fig.savefig(png_file_path)
    return sfdr


if __name__ == '__main__':
    # os.chdir('../')
    # 设置参数解析器
    parser = argparse.ArgumentParser(description="Read a binary Int32 file and reshape into a matrix.")
    parser.add_argument("bin_file_path", type=str, help="Path to the binary file")
    parser.add_argument("png_file_path", type=str, help="Number of rows in the output matrix")
    parser.add_argument("fs", type=float, help="Number of columns in the output matrix")
    parser.add_argument("target_freq", type=float, help="Number of columns in the output matrix")
    args = parser.parse_args()
    draw_dds(args.bin_file_path, args.png_file_path, args.fs, args.target_freq)
