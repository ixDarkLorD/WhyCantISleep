# Generates the hiding squint's eyelids, one pair of textures per style (the client config's eyelidStyle):
# textures/gui/sprites/hud/eyelids/<style>.png and <style>_glow.png.
# The lids: black, with a clear almond in the middle whose edge shades off. The glow: a band along that edge, which
# HidingClient tints red and draws over the lids only while the heartbeat minigame runs. Each texture's height is
# OPENING_SPAN times the almond's half-height at its middle, so the game stretches it to the squint and fills above and
# below the lids with black.
# Styles: smooth (large, a smooth edge), and three pixel-art ones drawn small, so the game's stretch makes chunky
# pixels: stepped (the edge in a few flat tones), dithered (each pixel clear or black, ordered-dithered) and pixel (the
# default: the flat tones, dithered from one to the next).
# Run from the project root: python tools/gen_eyelids.py
import math
import os

from PIL import Image

OUT = 'common/src/main/resources/assets/whycantisleep/textures/gui/sprites/hud/eyelids/'
# The textures span this many of the almond's middle half-heights, top to bottom (keep in step with HidingClient).
OPENING_SPAN = 2.5
# How much narrower the almond is at its ends than in its middle (keep in step with EYELIDS_CLEAR in HidingClient).
PINCH = 0.35
# How many tones the stepped edges go through, from clear to black.
STEPS = 4
# The glow's middle, how far it reaches to either side of it (in half-heights), and its strongest opacity; the pixel
# styles draw it in GLOW_STEPS flat tones, at GLOW_STRENGTH.
GLOW_AT = 0.1
GLOW_REACH = 0.3
GLOW_ALPHA = 0.6
GLOW_STEPS = 4
GLOW_STRENGTH = 0.8
# A 4x4 ordered dither, for stepping from one tone to the next in pixels.
BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]
# Each style's size and soft edge's width (in half-heights; keep 1 + the pixel styles' SOFT in step with EYELIDS_DARK
# in HidingClient where it has one).
STYLES = {
    'smooth': (512, 256, 0.25),
    'stepped': (128, 64, 0.3),
    'dithered': (128, 64, 0.3),
    'pixel': (128, 64, 0.3),
}


def lid_alpha(style, alpha, x, y):
    dither = (BAYER[y % 4][x % 4] + 0.5) / 16.0
    if style == 'stepped':
        return math.ceil(alpha * STEPS - 1e-6) / STEPS if alpha > 0.02 else 0.0
    if style == 'dithered':
        return 1.0 if alpha > dither else 0.0
    if style == 'pixel':
        # The step below this alpha, or the one above where the dither says so.
        step = alpha * STEPS
        return min(STEPS, int(step) + (1 if step - int(step) > dither else 0)) / STEPS
    return alpha


os.makedirs(OUT, exist_ok=True)
for style, (width, height, soft) in STYLES.items():
    lids = Image.new('RGBA', (width, height))
    glow = Image.new('RGBA', (width, height))
    lid_pixels = lids.load()
    glow_pixels = glow.load()
    for x in range(width):
        u = (x + 0.5) / width * 2.0 - 1.0
        half = 1.0 - PINCH * u * u
        for y in range(height):
            v = abs((y + 0.5) / height * 2.0 - 1.0) * OPENING_SPAN / 2.0
            t = min(1.0, max(0.0, (v - half) / soft))
            alpha = t * t * (3.0 - 2.0 * t)
            lid_pixels[x, y] = (0, 0, 0, round(lid_alpha(style, alpha, x, y) * 255))
            # A cosine bump, so the glow fades off on both sides; in flat steps for the pixel styles.
            d = min(1.0, abs(v - half - GLOW_AT) / GLOW_REACH)
            g = (0.5 + 0.5 * math.cos(math.pi * d)) * GLOW_ALPHA
            if style != 'smooth':
                g = math.floor(g * GLOW_STEPS + 0.5) / GLOW_STEPS * GLOW_STRENGTH if g > 0.05 else 0.0
            glow_pixels[x, y] = (255, 255, 255, round(g * 255))
    lids.save(OUT + style + '.png')
    glow.save(OUT + style + '_glow.png')
