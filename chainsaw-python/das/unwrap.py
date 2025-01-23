import numpy as np
import scipy.signal as signal
import matplotlib.pyplot as plt


def unwrap(data: np.ndarray):
    """
    unwrap电路的原型
    """
    data_delayed = np.insert(data[:-1], 0, 0)  # delay module
    deltas = data - data_delayed  # sub
    print(f"deltas: {deltas}")
    print(f"deltas+pi: {deltas + np.pi}")

    def fix_delta(delta):  # MUX + add
        det = int(delta - np.pi > 0) * 2 + int(delta + np.pi < 0)
        cand0 = delta
        cand1 = delta - 2 * np.pi
        cand2 = delta + 2 * np.pi
        # 使用MUX得到delta
        match det:
            case 0:
                delta_fixed = cand0
            case 1:
                delta_fixed = cand2
            case 2:
                delta_fixed = cand1
        return delta_fixed

    deltas_fixed = np.array([fix_delta(delta) for delta in deltas])
    print(f"delta_fixed: {deltas_fixed}")
    ret = np.cumsum(deltas_fixed)  # accumulator
    return ret


def generate_raw(threshold):
    # deltas = np.random.uniform(-threshold, threshold, 10)
    # return np.cumsum(deltas)
    return np.array([1.4384286318379038, 3.9438872007074313, 5.650995040576988, 7.280978481228561, 7.756021407297856,
                     7.823734742561309, 5.7598826655619835, 5.18695407357977, 6.725896220144346, 7.8947680507480325])


raw = generate_raw(np.pi)
phase = np.mod(raw, 2 * np.pi)
phase[phase > np.pi] -= 2 * np.pi
unwrapped = unwrap(phase)  # abs(delta) < π 时能够恢复
# unwrapped_parallel = np.empty_like(raw)  # abs(delta) < π/2 时能够恢复
# unwrapped_parallel[::2] = unwrap(phase[::2])
# unwrapped_parallel[1::2] = unwrap(phase[1::2])


for p in phase:
    print(p, end=',')
print()
for r in raw:
    print(r, end=',')

def plot_unwrap():
    plt.plot(raw)
    plt.plot(phase)
    plt.plot(unwrapped)
    # plt.plot(unwrapped_parallel)
    plt.legend(['raw', 'phase', 'unwrapped', 'unwrapped_parallel'])
    plt.savefig('unwrap.png')


plot_unwrap()
assert (np.abs(unwrapped - raw) < 1e-4).all
# assert (np.abs(unwrapped_parallel - raw) < 1e-4).all()
