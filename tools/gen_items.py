"""Draws the mod's two bedside tools as 16x16 item textures (Pillow), each centered in its square:
the Dreamcatcher (a slim wooden hoop on a loop, a diamond web round a violet bead, and three feathers) and the Bedside Lantern (an iron lantern, round-bellied and footed, with a ring handle, an amethyst set in its cap, and violet
glass round a candle's flame, flickering: an animated strip). Violet is the bed check's color, the dark spots' too. Light falls from the upper left.

Run from the repository root: python tools/gen_items.py
"""
import json
import math
from pathlib import Path

from PIL import Image

OUT = Path("common/src/main/resources/assets/whycantisleep/textures/item")

# The dreamcatcher, pixel by pixel, symmetric about the middle: a hoop one pixel thick (lit to the upper left, shaded to
# the lower right) hung by a loop, a web of a wide diamond of string tied to the hoop by eight threads and a small ring of
# thread round a violet bead, and three
# slim feathers at different levels, lit on their left and tipped violet: the two at the sides short and high, the
# middle one long and low.
DREAMCATCHER = [
    ".......tt.......",
    "......LLWW......",
    "....LL.tt.WW....",
    "...Lt..ss..tW...",
    "...L.ts..st.W...",
    "..L..s.tt.s..W..",
    "..Wts.tVvt.stD..",
    "..Wts.tvpt.stD..",
    "..W..s.tt.s..D..",
    "...W.ts..st.D...",
    "...Wt..ss..tD...",
    "...tWW.tt.DDt...",
    "...f..WWDD..f...",
    "..fg...fg...fg..",
    "...v...fh...v...",
    ".......vp.......",
]
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

# The lantern, pixel by pixel: a rounded wire handle (lit on its left) riveted to the cap, a brimmed cap, a narrow neck, a rounded belly of glass, a waist and a
# flared foot, its outline symmetric about the middle, so it sits centered.
LANTERN = [
    "......nnNN......",
    ".....n....N.....",
    ".....n....N.....",
    ".....OO..OO.....",
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
    # The handle: blackened iron with a violet sheen, dark enough to stand out on a light slot and violet enough on a dark one.
    "n": (112, 92, 176, 255),    # handle, lit
    "N": (54, 40, 102, 255),     # handle, in shadow
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
    return from_rows(DREAMCATCHER, DREAMCATCHER_COLORS)


# The flame's flicker, as rows of the glass (rows 7 to 10 of LANTERN) that change from frame to frame: still, leaning
# left, stretched tall, low and dim, leaning right. FLICKER is the order they play in, each FRAME_TICKS long.
FLAMES = [
    {},
    {8: "...OkvyyVVvpO...", 9: "...OVyWWyVVvO..."},
    {7: "....OkpvyppO....", 8: "...OkvVyWVvpO...", 9: "...OVVyWWyVvO..."},
    {8: "...OkvVVVVvpO...", 9: "...OVVVyyVVvO..."},
    {8: "...OkvVVyyvpO...", 9: "...OVVVyWWyvO..."},
]
FLICKER = [0, 1, 0, 2, 2, 0, 3, 0, 4, 0]
FRAME_TICKS = 3


def lantern():
    """The lantern as an animated strip: its frames one under another, for the .mcmeta beside it to play."""
    strip = Image.new("RGBA", (16, 16 * len(FLAMES)), (0, 0, 0, 0))
    for index, changes in enumerate(FLAMES):
        rows = [changes.get(y, row) for y, row in enumerate(LANTERN)]
        strip.paste(from_rows(rows, LANTERN_COLORS), (0, 16 * index))
    return strip


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    dreamcatcher().save(OUT / "dreamcatcher.png")
    lantern().save(OUT / "bedside_lantern.png")
    animation = {"animation": {"frametime": FRAME_TICKS, "frames": FLICKER}}
    (OUT / "bedside_lantern.png.mcmeta").write_text(json.dumps(animation, indent=2) + chr(10))
    print("wrote", OUT / "dreamcatcher.png", OUT / "bedside_lantern.png")
