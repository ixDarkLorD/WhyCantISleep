# Generates the sleep debt moon: textures/gui/sprites/hud/moon_empty.png and moon_full.png (16x16).
# The empty moon is a dark disc with a faint rim; the full one a pale moon with craters. The HUD draws the full one
# over the empty one from the bottom up, as far as the days without sleep go.
# Run from the project root: python tools/gen_moon.py
from PIL import Image

SIZE = 16
OUT = 'common/src/main/resources/assets/whycantisleep/textures/gui/sprites/hud/'
CRATERS = [(5.5, 6.0, 1.6), (10.0, 9.5, 2.0), (6.5, 11.0, 1.1), (10.5, 4.5, 1.0)]


def disc(fill, rim, craters):
    image = Image.new('RGBA', (SIZE, SIZE))
    pixels = image.load()
    centre = (SIZE - 1) / 2.0
    radius = 6.8
    for x in range(SIZE):
        for y in range(SIZE):
            d = ((x - centre) ** 2 + (y - centre) ** 2) ** 0.5
            if d > radius + 0.5:
                continue
            color = rim if d > radius - 0.7 else fill
            if craters and d <= radius - 0.7:
                for cx, cy, r in CRATERS:
                    if ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5 <= r:
                        color = craters
            pixels[x, y] = color
    return image


disc((0x2E, 0x31, 0x38, 210), (0x4B, 0x4F, 0x57, 255), None).save(OUT + 'moon_empty.png')
disc((0xD4, 0xD4, 0xD4, 255), (0xF0, 0xF0, 0xF0, 255), (0xA0, 0xA4, 0xAC, 255)).save(OUT + 'moon_full.png')
