# Generates the night terror's sounds: assets/whycantisleep/sounds/terror/*.ogg (mono Ogg Vorbis), synthesized with
# tools/synth.py. Then run tools/gen_sounds_json.ps1 so sounds.json uses them. NightTerrorClient plays them.
#   drone    under the whole terror (up to 12 s): a low, dissonant cluster and a dark wind, swelling all the way, its
#            throb quickening; cut off at the jolt
#   emerge   the demon rising from the floor: a swell drawn in backwards, a deep impact, gravel and a long groan
#   whisper  voices that aren't: breaths shaped into half-words, here and there, far back in a dark room
#   creep    a hand coming up: bones cracking, and a slow creak
#   stare    face to face: a high, sour cluster of strings rising and swelling, over a sub that swells with it
#   jolt     the end: a huge hit, a clang that doesn't ring true, and a shriek, into a long dark tail
#   wake     waking from it: a sharp breath in, and with it a quick rising arpeggio blooming into a short minor chord
#            that doesn't resolve, melodic but tense (about two and a half seconds)
# Run from the project root: python tools/gen_terror_sounds.py
import os

import numpy as np

import synth
from synth import RATE, band, bell, decay, noise, place, reverb, rng, sweep, thump, time, warm

OUT = 'common/src/main/resources/assets/whycantisleep/sounds/terror/'


def save(name, signal, peak=0.9):
    synth.save(OUT, name, signal, peak)


def saw(freq, length, harmonics=12):
    """A soft sawtooth: its first harmonics, each weaker."""
    t = time(length)
    return sum(np.sin(2 * np.pi * freq * k * t) / k for k in range(1, harmonics + 1))


def swell(length, attack, release=0.3):
    t = time(length)
    return np.minimum(1.0, t / attack) * np.minimum(1.0, (length - t) / release)


def formant(signal, centers, width=0.25):
    """Shapes noise into a voice-like sound by keeping a few bands (formants)."""
    return sum(band(signal, c * (1 - width), c * (1 + width)) for c in centers)


os.makedirs(OUT, exist_ok=True)

# The drone: A, B flat and E low (a semitone and a tritone), sawtooth-soft, with a dark wind; it swells the whole way
# and throbs, slowly at first and faster towards the end.
length = 12.5
t = time(length)
grow = (t / length) ** 1.3
cluster = (saw(55.0, length) + saw(58.27, length) * 0.8 + saw(82.41, length) * 0.6)
cluster = band(cluster, None, 420)
throb = 0.65 + 0.35 * np.sin(2 * np.pi * np.cumsum(0.3 + 3.2 * grow ** 2) / RATE)
wind = band(noise(length), 90, 600) * (0.4 + 0.6 * np.abs(np.sin(2 * np.pi * 0.11 * t + 1.0)))
drone = warm((cluster * 0.5 * throb + wind * 0.8) * (0.25 + 0.75 * grow), 1.6) * swell(length, 1.5, 0.05)
save('drone', reverb(drone, 1.0, 0.25, 900)[:len(drone)], peak=0.8)

# The demon rising: a noise swell drawn in backwards, then a deep hit, gravel churning, and a long groan falling.
length = 3.4
emerge = np.zeros(int(length * RATE))
backwards = band(noise(1.0), 60, 1500) * time(1.0) ** 3
place(emerge, backwards * 0.6, 0.0)
place(emerge, thump(70, 22, 0.6, 2.4) * 1.3, 1.0)
gravel = band(noise(2.2), 40, 320) * decay(0.7, 2.2) * (rng.uniform(0, 1, int(2.2 * RATE)) > 0.55)
place(emerge, gravel * 0.8, 1.0)
groan_t = time(2.2)
groan = warm(np.sin(2 * np.pi * np.cumsum(95 - 40 * groan_t / 2.2 + 3 * np.sin(2 * np.pi * 5 * groan_t)) / RATE), 3.0)
place(emerge, band(groan, None, 700) * decay(1.2, 2.2) * 0.5, 1.15)
save('emerge', reverb(emerge, 1.2, 0.4, 1200))

# Whispers: breaths shaped into half-words, scattered, far back in a dark room.
length = 3.2
whisper = np.zeros(int(length * RATE))
for i in range(9):
    word = rng.uniform(0.15, 0.45)
    shape = formant(noise(word), [rng.uniform(500, 900), rng.uniform(1100, 1900), rng.uniform(2300, 3200)])
    shape *= np.sin(np.pi * time(word) / word) ** 2
    place(whisper, shape * rng.uniform(0.4, 1.0), rng.uniform(0.0, length - word))
save('whisper', reverb(whisper, 1.4, 0.6, 2500), peak=0.7)

# A hand coming up: a cluster of sharp cracks, and a slow, rough creak under them.
length = 1.6
creep = np.zeros(int(length * RATE))
for i in range(14):
    crack = band(noise(0.03), 900, 6000) * decay(0.004, 0.03)
    place(creep, crack * rng.uniform(0.3, 1.0), rng.uniform(0.0, 0.9) ** 1.5)
creak_t = time(1.2)
creak = np.sin(2 * np.pi * np.cumsum(190 - 50 * creak_t / 1.2) / RATE) * (rng.uniform(0, 1, len(creak_t)) > 0.3)
place(creep, band(creak, 80, 1500) * swell(1.2, 0.2, 0.4) * 0.35, 0.2)
save('creep', reverb(creep, 0.5, 0.3, 3000), peak=0.75)

# Face to face: a high, sour cluster rising and swelling, with a tremble, over a sub swelling with it.
length = 2.4
t = time(length)
stare = np.zeros(len(t))
for f in (1320.0, 1398.0, 1480.0, 1568.0):
    rising = f * (1 + 0.08 * (t / length) ** 2) * (1 + 0.006 * np.sin(2 * np.pi * 6.5 * t + f))
    stare += np.sin(2 * np.pi * np.cumsum(rising) / RATE)
stare = band(stare, 600, None) * (t / length) ** 1.5 * (0.7 + 0.3 * np.sin(2 * np.pi * 11 * t))
sub = np.sin(2 * np.pi * 42 * t) * (t / length) ** 2
save('stare', reverb(stare * 0.5 + sub * 0.6, 0.8, 0.3, 4000), peak=0.75)

# The jolt: a huge hit, a clang that doesn't ring true, and a shriek, into a long dark tail.
length = 3.5
jolt = thump(95, 18, 0.7, length) * 1.5
jolt += band(noise(length), 30, 9000) * decay(0.12, length) * 0.9
jolt += bell(180, length, 0.9) * 0.4 + bell(254.5, length, 0.7) * 0.3
shriek_t = time(0.8)
shriek = formant(noise(0.8), [1800, 2900, 4200]) * np.sin(np.pi * shriek_t / 0.8) ** 0.5
place(jolt, shriek * 0.6, 0.02)
save('jolt', reverb(warm(jolt, 1.4), 1.6, 0.45, 1800))

# Waking: a sharp breath in (rising through the throat), and with it a quick rising arpeggio of soft bells (A, C, E, B)
# that blooms into a short A minor chord with a ninth, fading without resolving. About two and a half seconds.
length = 2.4
wake = np.zeros(int(length * RATE))
breath_t = time(0.5)
breath = noise(0.5)
breath = band(breath, 500, 1300) * 0.5 + band(breath, 1300, 3200) * np.linspace(0.2, 1.0, len(breath)) * 0.6
breath *= np.minimum(1.0, breath_t / 0.03) * np.exp(-breath_t / 0.3)
place(wake, breath * 1.2, 0.0)
for i, f in enumerate((220.0, 261.63, 329.63, 493.88)):
    place(wake, bell(f * 2, 1.9, 0.55) * 0.22, 0.04 + i * 0.06)
pad_t = time(2.0)
pad = sum(np.sin(2 * np.pi * f * pad_t * (1 + 0.003 * np.sin(2 * np.pi * 0.7 * pad_t + f))) for f in (220.0, 261.63, 329.63, 493.88))
pad = band(pad, None, 2400) * np.minimum(1.0, pad_t / 0.4) * np.exp(-np.maximum(0.0, pad_t - 0.4) / 0.5) * 0.18
place(wake, pad, 0.15)
save('wake', reverb(wake, 0.6, 0.3, 5000)[:int(3.0 * RATE)])
print('Wrote', ', '.join(sorted(f for f in os.listdir(OUT) if f.endswith('.ogg'))))
