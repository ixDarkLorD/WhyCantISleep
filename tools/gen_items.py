"""Draws the mod's two bedside tools as 16x16 item textures (Pillow), each centered in its square:
the Dreamcatcher (a leather-wrapped wooden hoop on a loop, a web of string round a violet bead, and three feathers on
beaded strings) and the Bedside Lantern (an iron lantern with a ring handle, an amethyst set in its cap, and violet
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

# The lantern, pixel by pixel: rows 1 to 14, its outline symmetric about the middle, so it sits centered.
LANTERN = [
    "................",
    "......OOOO......",
    ".....O....O.....",
    ".....O....O.....",
    "....OOOOOOOO....",
    "...OiiIVvIIIO...",
    "...OPPPPPPPPO...",
    "...OkpvvvvppO...",
    "...OkvVyyVvpO...",
    "...OVVyWWyVvO...",
    "...OvvVccVvpO...",
    "...OpvvccvppO...",
    "...OPPPPPPPPO...",
    "...OiiIIIIIIO...",
    "....OOOOOOOO....",
    "................",
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

    # The web: a ring round the bead, and the four diagonals from the bead out to the hoop.
    for y in range(16):
        for x in range(16):
            if 2.0 <= math.hypot(x - HOOP_X, y - HOOP_Y) <= 2.6:
                put(x, y, "t")
    for step in (1, 2, 3):
        for x, y in ((7 - step, 6 - step), (8 + step, 6 - step), (7 - step, 7 + step), (8 + step, 7 + step)):
            put(x, y, "s")
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
    # Three feathers, each on a beaded string and tipped violet: one on each side, falling from the hoop, and one in
    # the middle under it.
    for fx, out in ((3, -1), (12, 1)):
        put(fx, 10, "t")
        put(fx, 11, "v")
        for y in (12, 13, 14):
            put(fx, y, "f")
            put(fx - out, y, "h" if y > 12 else ".")
        put(fx + out, 13, "g")
        put(fx, 15, "v")
    put(7, 13, "V")
    put(8, 13, "v")
    put(7, 14, "f")
    put(8, 14, "g")
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
