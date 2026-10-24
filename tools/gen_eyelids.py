# Generates the hiding squint's eyelids: textures/gui/sprites/hud/eyelids.png and eyelids_glow.png.
# The lids: black, with a clear almond in the middle whose edge shades off. The glow: a soft white band along that
# edge, which HidingClient tints red and draws over the lids only while the heartbeat minigame runs. Each texture's
# height is OPENING_SPAN times the almond's half-height at its middle, so the game stretches it to the squint and fills
# above and below the lids with black.
# Run from the project root: python tools/gen_eyelids.py
import math

from PIL import Image

WIDTH, HEIGHT = 512, 256
# The texture spans this many of the almond's middle half-heights, top to bottom (keep in step with HidingClient).
OPENING_SPAN = 2.5
# How much narrower the almond is at its ends than in its middle, and how wide its soft edge is (in half-heights; keep
# 1 + SOFT in step with EYELIDS_DARK in HidingClient).
PINCH = 0.35
SOFT = 0.25
# The glow's middle, how far it reaches to either side of it (in half-heights), and its strongest opacity.
GLOW_AT = 0.1
GLOW_REACH = 0.3
GLOW_ALPHA = 0.6

lids = Image.new('RGBA', (WIDTH, HEIGHT))
glow = Image.new('RGBA', (WIDTH, HEIGHT))
lid_pixels = lids.load()
glow_pixels = glow.load()
for x in range(WIDTH):
    u = (x + 0.5) / WIDTH * 2.0 - 1.0
    half = 1.0 - PINCH * u * u
    for y in range(HEIGHT):
        v = abs((y + 0.5) / HEIGHT * 2.0 - 1.0) * OPENING_SPAN / 2.0
        t = min(1.0, max(0.0, (v - half) / SOFT))
        alpha = t * t * (3.0 - 2.0 * t)
        lid_pixels[x, y] = (0, 0, 0, round(alpha * 255))
        # A cosine bump, so the glow fades off on both sides with no edge of its own.
        d = min(1.0, abs(v - half - GLOW_AT) / GLOW_REACH)
        glow_pixels[x, y] = (255, 255, 255, round((0.5 + 0.5 * math.cos(math.pi * d)) * GLOW_ALPHA * 255))
lids.save('common/src/main/resources/assets/whycantisleep/textures/gui/sprites/hud/eyelids.png')
glow.save('common/src/main/resources/assets/whycantisleep/textures/gui/sprites/hud/eyelids_glow.png')
