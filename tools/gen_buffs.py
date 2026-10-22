# Generates the buff, debuff and neutral icons: textures/gui/sprites/hud/buff.png, debuff.png and neutral.png (9x9), and
# the pips of the tooltip meter, set in the text: pip_soothing.png, pip_unsettling.png and pip_empty.png (8x8, a glyph's size).
# A rounded square in the color of what it means (green for better sleep, red for worse) with a white chevron pointing
# up or down, as on the sleep card, the sleep status popup and the tooltips of things that change sleep quality.
# Run from the project root: python tools/gen_buffs.py
from PIL import Image

SIZE = 9
OUT = 'common/src/main/resources/assets/whycantisleep/textures/gui/sprites/hud/'
# The chevron, pointing up, row by row (x from, x to inclusive).
CHEVRON = {3: (4, 4), 4: (3, 5), 5: (2, 6)}
CHEVRON_INNER = {5: (4, 4)}


def icon(fill, edge, up):
    image = Image.new('RGBA', (SIZE, SIZE))
    pixels = image.load()
    for x in range(SIZE):
        for y in range(SIZE):
            corner = (x in (0, SIZE - 1)) and (y in (0, SIZE - 1))
            if corner:
                continue
            border = x in (0, SIZE - 1) or y in (0, SIZE - 1)
            pixels[x, y] = edge if border else fill
    for row, (start, end) in CHEVRON.items():
        y = row if up else SIZE - 1 - row
        for x in range(start, end + 1):
            pixels[x, y] = (255, 255, 255, 255)
    for row, (start, end) in CHEVRON_INNER.items():
        y = row if up else SIZE - 1 - row
        for x in range(start, end + 1):
            pixels[x, y] = fill
    return image


icon((0x2E, 0xA0, 0x4A, 255), (0x55, 0xFF, 0x55, 255), True).save(OUT + 'buff.png')
icon((0xA0, 0x2A, 0x2A, 255), (0xFF, 0x55, 0x55, 255), False).save(OUT + 'debuff.png')

# Neutral: blue, with a white bar across instead of a chevron.
neutral = icon((0x2A, 0x5A, 0xA0, 255), (0x55, 0xAA, 0xFF, 255), True)
neutral_pixels = neutral.load()
for y in range(3, 6):
    for x in range(2, 7):
        neutral_pixels[x, y] = (0x2A, 0x5A, 0xA0, 255)
for x in range(2, 7):
    neutral_pixels[x, 4] = (255, 255, 255, 255)
neutral.save(OUT + 'neutral.png')


# The tooltip meter's pips: a short bar across the middle of a glyph, a pixel of room after it for the next.
def pip(color):
    image = Image.new('RGBA', (8, 8))
    pixels = image.load()
    for x in range(0, 7):
        for y in range(3, 6):
            pixels[x, y] = color
    return image


pip((0x55, 0xFF, 0x55, 255)).save(OUT + 'pip_soothing.png')
pip((0xFF, 0x55, 0x55, 255)).save(OUT + 'pip_unsettling.png')
pip((0x4B, 0x4F, 0x57, 255)).save(OUT + 'pip_empty.png')
