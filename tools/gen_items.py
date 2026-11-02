"""Draws the mod's two bedside tools as 16x16 item textures (Pillow), each centered in its square:
the Dreamcatcher (a leather-wrapped wooden hoop on a loop, a diamond web round a violet bead, and three
feathers on strings) and the Bedside Lantern (an iron lantern, round-bellied and footed, with a ring handle, an amethyst set in its cap, and violet
glass round a candle's flame). Violet is the bed check's color, the dark spots' too. Light falls from the upper left.

Run from the repository root: python tools/gen_items.py
"""
import math
from pathlib import Path

from PIL import Image

OUT = Path("common/src/main/resources/assets/whycantisleep/textures/item")

# The dreamcatcher's hoop: centered between pixels (7.5, 6.5), so it sits evenly, its wood band between these radii,
# outlined outside them.
HOOP_X, HOOP_Y = 7.5, 6.5
HOOP_IN, HOOP_OUT, HOOP_EDGE = 3.6, 4.85, 5.6

DREAMCATCHER_COLORS = {
    "O": (58, 34, 18, 255),      # outline
    "D": (98, 62, 32, 255),      # wood, in shadow
    "W": (140, 94, 50, 255),     # wood
    "L": (186, 134, 78, 255),    # wood, lit
    "R": (116, 52, 40, 255),     # leather wrap
    "r": (152, 74, 56, 255),     # leather wrap, lit
    "s": (232, 224, 206, 255),   # string
    "t": (170, 160, 142, 255),   # string, dim
    "V": (200, 184, 255, 255),   # violet, lit
    "v": (138, 107, 255, 255),   # violet
    "p": (84, 58, 168, 255),     # violet, deep
    "f": (244, 244, 240, 255),   # feather
    "g": (206, 206, 210, 255),   # feather, soft
    "h": (150, 150, 164, 255),   # feather, shadow
}

# The lantern, pixel by pixel: a ring handle, a brimmed cap, a narrow neck, a rounded belly of glass, a waist and a
# flared foot, its outline symmetric about the middle, so it sits centered.
LANTERN = [
    "................",
    "......OOOO......",
    ".....O....O.....",
    ".....O....O.....",
    ".....OOOOOO.....",
    "....OiIVvIIO....",
    ".....OPPPPO.....",
    "....OkpvvppO....",
    "...OkvVyyVvpO...",
    "...OVVyWWyVvO...",
    "...OvVyWWyVpO...",
    "...OvvVccVvpO...",
    "....OpvccvpO....",
    ".....OPPPPO.....",
    "....OiIIIIIO....",
    "...OOOOOOOOOO...",
]
LANTERN_COLORS = {
    "O": (28, 28, 36, 255),      # outline
    "P": (54, 54, 68, 255),      # iron, dark
    "I": (90, 90, 106, 255),     # iron
    "i": (138, 138, 156, 255),   # iron, lit
    "p": (72, 48, 152, 255),     # glass, deep
    "v": (124, 92, 240, 255),    # glass
    "V": (172, 150, 255, 255),   # glass, lit
    "k": (226, 216, 255, 255),   # glint
    "y": (255, 196, 84, 255),    # flame
    "W": (255, 246, 206, 255),   # flame's heart
    "c": (234, 222, 192, 255),   # candle
}


def from_rows(rows, colors):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = image.load()
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            if key != ".":
                px[x, y] = colors[key]
    return image


def dreamcatcher():
    grid = [["."] * 16 for _ in range(16)]

    def put(x, y, key):
        if 0 <= x < 16 and 0 <= y < 16:
            grid[y][x] = key

    # The web: a diamond round the bead, tied to the hoop at its four points by short threads.
    for y in range(16):
        for x in range(16):
            dx, dy = abs(x - HOOP_X), abs(y - HOOP_Y)
            if dx + dy == 3:
                put(x, y, "s")
            elif dx + dy == 4 and (dx < 1 or dy < 1):
                put(x, y, "t")
    # The hoop: wood, lit to the upper left and shaded to the lower right, wrapped in leather at its four sides, outlined.
    for y in range(16):
        for x in range(16):
            dx, dy = x - HOOP_X, y - HOOP_Y
            d = math.hypot(dx, dy)
            if HOOP_IN <= d <= HOOP_OUT:
                light = -(dx + dy) / max(d, 1e-6)
                wrap = abs(dx) < 1 or abs(dy) < 1
                if wrap:
                    put(x, y, "r" if light > 0 else "R")
                else:
                    put(x, y, "L" if light > 0.5 else "D" if light < -0.5 else "W")
            elif HOOP_OUT < d <= HOOP_EDGE:
                put(x, y, "O")
    # The bead, in the web's middle.
    put(7, 6, "V")
    put(8, 6, "v")
    put(7, 7, "v")
    put(8, 7, "p")
    # The loop it hangs by.
    put(7, 0, "t")
    put(8, 0, "t")
    # Three feathers on strings, each two pixels wide: lit on its left, in shadow on its right, tipped violet. They
    # hang at different levels: the two at the sides short and high, from the hoop's lower edge, the middle one long
    # and low, under the hoop.
    for inner, outer in ((3, 2), (12, 13)):
        lit, dark = min(inner, outer), max(inner, outer)
        put(inner, 10, "t")
        put(inner, 11, "f")
        put(lit, 12, "f")
        put(dark, 12, "h")
        put(lit, 13, "v")
        put(dark, 13, "p")
    put(7, 13, "f")
    put(8, 13, "g")
    put(7, 14, "f")
    put(8, 14, "h")
    put(7, 15, "v")
    put(8, 15, "p")
    return from_rows(["".join(row) for row in grid], DREAMCATCHER_COLORS)


def lantern():
    return from_rows(LANTERN, LANTERN_COLORS)


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    dreamcatcher().save(OUT / "dreamcatcher.png")
    lantern().save(OUT / "bedside_lantern.png")
    print("wrote", OUT / "dreamcatcher.png", OUT / "bedside_lantern.png")
