# Shared helpers for the mod's synthesized sounds (tools/gen_minigame_sounds.py, tools/gen_terror_sounds.py): mono
# signals as numpy arrays at RATE, shaped by sweeps, envelopes, spectral band filters, a plain reverb and saturation,
# and saved as Ogg Vorbis. The noise comes from one seeded generator, so a script makes the same sounds every run.
import numpy as np
import soundfile

RATE = 44100
rng = np.random.default_rng(7)


def time(seconds):
    return np.arange(int(seconds * RATE)) / RATE


def sweep(start, end, seconds, curve=6.0):
    """A sine whose pitch falls (or rises) from start to end Hz, quickly at first."""
    t = time(seconds)
    k = np.exp(-curve * t / seconds)
    freq = end + (start - end) * k
    return np.sin(2 * np.pi * np.cumsum(freq) / RATE)


def decay(seconds, length, attack=0.003):
    t = time(length)
    return np.minimum(1.0, t / attack) * np.exp(-t / seconds)


def noise(seconds):
    return rng.uniform(-1.0, 1.0, int(seconds * RATE))


def band(signal, low=None, high=None, soft=0.15):
    """Keeps the frequencies between low and high Hz, with soft edges, by masking the spectrum."""
    spectrum = np.fft.rfft(signal)
    freqs = np.fft.rfftfreq(len(signal), 1 / RATE)
    mask = np.ones_like(freqs)
    if low:
        mask *= 1 / (1 + (low / np.maximum(freqs, 1e-3)) ** (2 / soft))
    if high:
        mask *= 1 / (1 + (freqs / high) ** (2 / soft))
    return np.fft.irfft(spectrum * mask, len(signal))


def place(target, sound, at):
    start = int(at * RATE)
    end = min(len(target), start + len(sound))
    target[start:end] += sound[:end - start]


def reverb(signal, seconds, mix, dark=3000):
    """A plain room: the signal smeared through a dark, decaying noise tail."""
    tail = band(noise(seconds * 3) * np.exp(-time(seconds * 3) / seconds), high=dark)
    tail /= np.sqrt(np.sum(tail ** 2))
    wet = np.fft.irfft(np.fft.rfft(signal, len(signal) + len(tail)) * np.fft.rfft(tail, len(signal) + len(tail)))
    out = np.zeros(len(wet))
    out[:len(signal)] += signal
    return out + wet * mix


def warm(signal, drive):
    return np.tanh(signal * drive) / np.tanh(drive)


def fade_out(signal, seconds):
    n = min(len(signal), int(seconds * RATE))
    signal[-n:] *= np.linspace(1, 0, n) ** 2
    return signal


def trim(signal, floor=0.01):
    """Cuts the silence off the end: from where the sound stays under floor of its peak."""
    loud = np.nonzero(np.abs(signal) > floor * np.max(np.abs(signal)))[0]
    return signal[:loud[-1] + 1] if len(loud) else signal


def save(folder, name, signal, peak=0.9):
    signal = fade_out(trim(signal - np.mean(signal)), 0.05)
    signal = signal / np.max(np.abs(signal)) * peak
    soundfile.write(folder + name + '.ogg', signal.astype(np.float32), RATE, format='OGG', subtype='VORBIS')


def thump(start, end, seconds, length):
    """A body thump: a falling sine, warmed a little, with a soft noise knock on its front."""
    body = sweep(start, end, length) * decay(seconds, length)
    knock = band(noise(length), 80, 900) * decay(0.012, length)
    return warm(body + knock * 0.25, 1.8)


def bell(root, length, decay_seconds):
    """A struck bell: its partials (hum, prime, minor third, fifth, nominal and two above), the high ones dying first,
    each a hair out of tune with a slow beating, with a bright strike on the front."""
    t = time(length)
    partials = [(0.5, 0.5, 1.6), (1.0, 1.0, 1.0), (1.2, 0.45, 0.8), (1.5, 0.3, 0.7), (2.0, 0.55, 0.55),
                (2.51, 0.2, 0.35), (3.01, 0.12, 0.25)]
    out = np.zeros(len(t))
    for ratio, gain, life in partials:
        freq = root * ratio
        beating = 1 + 0.08 * np.sin(2 * np.pi * (0.7 + ratio * 0.3) * t)
        out += gain * beating * np.sin(2 * np.pi * freq * t + ratio) * np.exp(-t / (decay_seconds * life))
    strike = band(noise(length), 2500, 9000) * decay(0.006, length)
    return (out + strike * 0.3) * np.minimum(1.0, t / 0.002)
