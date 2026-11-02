"""Draws the mod's two bedside tools as 16x16 item textures (Pillow):
the Dreamcatcher (a wooden hoop with a string web, a violet bead and three feathers) and the Bedside Lantern (a small
iron lantern of violet glass round a candle flame). Violet is the bed check's color, the dark spots' too.

Run from the repository root: python tools/gen_items.py
"""
import math
from pathlib import Path

from PIL import Image

OUT = Path("common/src/main/resources/assets/whycantisleep/textures/item")

WOOD_DARK = (92, 58, 30, 255)
WOOD = (140, 96, 52, 255)
WOOD_LIGHT = (176, 128, 74, 255)
STRING = (222, 214, 196, 255)
STRING_DIM = (170, 162, 148, 255)
VIOLET = (138, 107, 255, 255)
VIOLET_LIGHT = (190, 172, 255, 255)
VIOLET_DARK = (88, 64, 180, 255)
FEATHER = (236, 236, 232, 255)
FEATHER_SHADE = (190, 190, 186, 255)
IRON_DARK = (44, 44, 52, 255)
IRON = (74, 74, 86, 255)
IRON_LIGHT = (112, 112, 126, 255)
FLAME = (255, 214, 120, 255)
FLAME_CORE = (255, 246, 210, 255)


def dreamcatcher():
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = image.load()
    cx, cy, r = 7.5, 6.0, 5.2
    # The web first, so the hoop draws over its ends: a small ring round the bead, and four threads from it to the hoop,
    # the rest of the hoop left open.
    for y in range(16):
        for x in range(16):
            if 2.0 <= math.hypot(x - cx, y - cy) <= 2.7:
                px[x, y] = STRING_DIM
    for angle in (45, 135, 225, 315):
        a = math.radians(angle)
        for step in range(5):
            t = 2.6 + step * (r - 3.2) / 4
            x, y = round(cx + math.cos(a) * t), round(cy + math.sin(a) * t)
            px[x, y] = STRING
    # The hoop: a ring of wood, lit on its upper left.
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if r - 0.7 <= d <= r + 0.6:
                lit = (x - cx) + (y - cy) < -1.5
                shade = (x - cx) + (y - cy) > 2.5
                px[x, y] = WOOD_LIGHT if lit else WOOD_DARK if shade else WOOD
    # The bead in the web's middle.
    px[7, 6] = VIOLET_LIGHT
    px[8, 6] = VIOLET
    px[7, 7] = VIOLET
    px[8, 7] = VIOLET_DARK
    # The loop it hangs by.
    px[7, 0] = STRING_DIM
    # Three feathers on strings from the hoop's foot, the middle one longest, each tipped violet.
    for fx, top, length in ((4, 10, 4), (7, 12, 3), (11, 10, 4)):
        for y in range(top, top + 1):
            px[fx, y] = STRING
        for i in range(length):
            y = top + 1 + i
            if y > 15:
                break
            px[fx, y] = FEATHER if i < length - 1 else VIOLET
            if fx + 1 < 16 and i < length - 1:
                px[fx + 1, y] = FEATHER_SHADE
    return image


def lantern():
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = image.load()
    # The handle.
    for x in range(6, 10):
        px[x, 0] = IRON_DARK
    px[6, 1] = IRON_DARK
    px[9, 1] = IRON_DARK
    # The cap.
    for x in range(5, 11):
        px[x, 2] = IRON_LIGHT if x < 8 else IRON
    for x in range(4, 12):
        px[x, 3] = IRON
    # The glass: violet, lighter towards the flame, framed by iron posts.
    for y in range(4, 12):
        for x in range(4, 12):
            if x in (4, 11):
                px[x, y] = IRON_DARK
                continue
            d = math.hypot(x - 7.5, y - 8.0)
            px[x, y] = VIOLET_LIGHT if d < 1.8 else VIOLET if d < 3.2 else VIOLET_DARK
    # The candle's flame.
    px[7, 7] = FLAME
    px[8, 7] = FLAME
    px[7, 8] = FLAME_CORE
    px[8, 8] = FLAME_CORE
    px[7, 9] = FLAME
    px[7, 10] = STRING
    px[8, 10] = STRING_DIM
    # The base.
    for x in range(4, 12):
        px[x, 12] = IRON
    for x in range(5, 11):
        px[x, 13] = IRON_DARK
    return image


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    dreamcatcher().save(OUT / "dreamcatcher.png")
    lantern().save(OUT / "bedside_lantern.png")
    print("wrote", OUT / "dreamcatcher.png", OUT / "bedside_lantern.png")
