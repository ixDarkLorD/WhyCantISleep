# Generates the hiding squint's eyelids: textures/gui/sprites/hud/eyelids.png.
# Black, with a clear almond in the middle whose edge shades off. The texture's height is OPENING_SPAN times the
# almond's half-height at its middle, so the game stretches it to the squint and fills above and below with black.
# Run from the project root: python tools/gen_eyelids.py
from PIL import Image

WIDTH, HEIGHT = 512, 256
# The texture spans this many of the almond's middle half-heights, top to bottom (keep in step with HidingClient).
OPENING_SPAN = 2.5
# How much narrower the almond is at its ends than in its middle, and how wide its soft edge is (in half-heights).
PINCH = 0.35
SOFT = 0.25

image = Image.new('RGBA', (WIDTH, HEIGHT))
pixels = image.load()
for x in range(WIDTH):
    u = (x + 0.5) / WIDTH * 2.0 - 1.0
    half = 1.0 - PINCH * u * u
    for y in range(HEIGHT):
        v = abs((y + 0.5) / HEIGHT * 2.0 - 1.0) * OPENING_SPAN / 2.0
        t = min(1.0, max(0.0, (v - half) / SOFT))
        alpha = t * t * (3.0 - 2.0 * t)
        pixels[x, y] = (0, 0, 0, round(alpha * 255))
image.save('common/src/main/resources/assets/whycantisleep/textures/gui/sprites/hud/eyelids.png')
