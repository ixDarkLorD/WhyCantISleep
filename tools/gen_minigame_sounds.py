# Generates the heartbeat minigame's sounds: assets/whycantisleep/sounds/heartbeat/*.ogg (mono Ogg Vorbis), all
# synthesized, so they can be tuned here and made again. Then run tools/gen_sounds_json.ps1 so sounds.json uses them.
#   sting       the warning slamming in: a deep impact with a dark metallic ring, swelling out into a long tail
#   beat        a heartbeat coming in: a low "lub-dub" with a soft thud on each
#   ready       a beat nearing the calm window: a breath of noise and a rising tone that swell up to it and stop
#               short, timed to the window (READY_SECONDS)
#   calm        a beat caught: a bass thump with a bell over it, ringing out (HidingMinigame plays it a step higher up
#               a minor pentatonic scale with each beat of a streak)
#   click       any click: a short muted tick
#   heart_lost  a strike: a low dissonant hit falling in pitch, with a crack over it
#   gasp        the third strike: a sharp breath in over a deep boom, and a long dark tail
# Run from the project root: python tools/gen_minigame_sounds.py
import os

import numpy as np

import synth
from synth import RATE, band, bell, decay, noise, place, reverb, rng, sweep, thump, time, warm

OUT = 'common/src/main/resources/assets/whycantisleep/sounds/heartbeat/'
# The time from the get-ready cue to the beat reaching the window: READY_LEAD of BEAT_TICKS in HidingMinigame.
READY_SECONDS = 0.32 * 30 / 20


def save(name, signal, peak=0.9):
    synth.save(OUT, name, signal, peak)


os.makedirs(OUT, exist_ok=True)

# The warning slamming in: a sub drop, a low hit, and a dark metallic ring under it, with a long tail.
length = 3.0
sting = thump(90, 32, 0.45, length) * 1.2
sting += bell(110, length, 1.4) * 0.35
sting += band(noise(length), 40, 400) * decay(0.08, length) * 0.4
save('sting', reverb(sting, 0.9, 0.35, 1800))

# A heartbeat coming in: lub, then a softer, slightly higher dub.
length = 0.75
beat = np.zeros(int(length * RATE))
place(beat, thump(72, 40, 0.09, 0.5), 0.0)
place(beat, thump(85, 46, 0.07, 0.4) * 0.7, 0.2)
save('beat', reverb(beat, 0.25, 0.15, 1200))

# Get ready: a breath of noise and a tone rising together, swelling until the beat reaches the window, then cut off
# with a tiny click so the moment to press is heard.
length = READY_SECONDS
t = time(length)
swell = (t / length) ** 2.5
air = band(noise(length), 900, 5000) * swell * 0.5
tone = sweep(330, 660, length, curve=-2.5) * swell * 0.4
tone += sweep(495, 990, length, curve=-2.5) * swell * 0.15
ready = np.concatenate([air + tone, band(noise(0.01), 3000, 10000) * 0.3])
save('ready', ready, peak=0.7)

# Caught: a bass thump with a bell (D5) ringing over it.
length = 2.6
calm = thump(65, 38, 0.3, length) * 1.1
calm += bell(587.33, length, 0.9) * 0.55
save('calm', reverb(calm, 0.8, 0.3, 4000))

# A click: a short, muted tick.
length = 0.06
click = band(noise(length), 1500, 6000) * decay(0.006, length) + np.sin(2 * np.pi * 1800 * time(length)) * decay(0.01, length) * 0.4
save('click', click, peak=0.6)

# A strike: a low tritone cluster falling away, a crack over it, and a thud.
length = 1.4
hit = np.zeros(int(length * RATE))
for freq, gain in ((98, 0.5), (138.6, 0.4), (196, 0.25), (277.2, 0.2)):
    hit += warm(sweep(freq * 1.3, freq * 0.75, length, curve=3.0), 3.0) * gain * decay(0.35, length)
hit += thump(80, 30, 0.2, length)
crack = band(noise(length), 2000, 12000) * decay(0.03, length)
crack *= (rng.uniform(0, 1, len(crack)) > 0.6)
hit += crack * 0.6
save('heart_lost', reverb(hit, 0.6, 0.3, 2500))

# The gasp: a sharp breath in, its noise rising through the throat's sounds, over a deep boom with a long tail.
length = 3.2
t = time(length)
breath_length = 0.55
breath = np.zeros(len(t))
inhale = noise(breath_length)
inhale = band(inhale, 500, 1400) * 0.6 + band(inhale, 1400, 3200) * np.linspace(0.2, 1.0, len(inhale)) * 0.6
inhale *= np.minimum(1.0, time(breath_length) / 0.04) * np.exp(-time(breath_length) / 0.35)
place(breath, inhale, 0.0)
boom = thump(70, 25, 0.9, length) * 1.3 + bell(73.4, length, 1.8) * 0.3
gasp = breath * 1.4 + boom * 0.55
save('gasp', reverb(gasp, 1.2, 0.4, 1500))
print('Wrote', ', '.join(sorted(f for f in os.listdir(OUT) if f.endswith('.ogg'))))
