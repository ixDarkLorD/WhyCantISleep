# Generates the hiding squint's eyelids: textures/gui/sprites/hud/eyelids.png.
# Black, with a clear almond in the middle whose edge blends off smoothly. The lids' edge is a soft grey rim, so the
# game can tint it: HidingClient draws it grey, and red while the heartbeat minigame runs. The texture's height is
# OPENING_SPAN times the almond's half-height at its middle, so the game stretches it to the squint and fills above
# and below with black. Large, so the stretch doesn't show steps in the blend.
# Run from the project root: python tools/gen_eyelids.py
import math

from PIL import Image

WIDTH, HEIGHT = 512, 256
# The texture spans this many of the almond's middle half-heights, top to bottom (keep in step with HidingClient).
OPENING_SPAN = 2.5
# How much narrower the almond is at its ends than in its middle, and how wide its soft edge is (in half-heights; keep
# 1 + SOFT in step with EYELIDS_DARK in HidingClient).
PINCH = 0.35
SOFT = 0.45
# How bright the rim is where the edge begins, before the tint: it darkens to black as the lid deepens.
RIM = 150


def smoothstep(t):
    t = min(1.0, max(0.0, t))
    return t * t * (3.0 - 2.0 * t)


image = Image.new('RGBA', (WIDTH, HEIGHT))
pixels = image.load()
for x in range(WIDTH):
    u = (x + 0.5) / WIDTH * 2.0 - 1.0
    half = 1.0 - PINCH * u * u
    for y in range(HEIGHT):
        v = abs((y + 0.5) / HEIGHT * 2.0 - 1.0) * OPENING_SPAN / 2.0
        # How far into the lid this pixel is: 0 at the opening's edge, 1 a SOFT's width in.
        t = min(1.0, max(0.0, (v - half) / SOFT))
        alpha = smoothstep(t)
        # Faint grey at the edge, deepening to black: the rim shows where the lid is still half clear.
        shade = round(RIM * math.pow(1.0 - t, 1.5))
        pixels[x, y] = (shade, shade, shade, round(alpha * 255))
image.save('common/src/main/resources/assets/whycantisleep/textures/gui/sprites/hud/eyelids.png')
