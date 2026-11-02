"""Draws the footprints the blocking monsters leave (Pillow): a clawed print, pixel art at 16x16, toes up, one for each
foot (the right a mirror of the left). White in shades of grey so the game tints them each monster's highlight color;
drawn added to the scene, so the brighter a pixel, the more it glows: the claws brightest, then the toes, the pad's
middle, and its rim. Read by client/highlight/BlockerTrails.

Run from the repository root: python tools/gen_footprints.py
"""
from pathlib import Path

from PIL import Image, ImageOps

OUT = Path("common/src/main/resources/assets/whycantisleep/textures/vfx")

# The left foot, toes up: c claw, t toe, p pad, r the pad's rim. The outer (left) toe sits a little lower, as on a foot.
LEFT = [
    "................",
    "........c.......",
    "........t...c...",
    "....c..ttt..t...",
    "....t..ttt.ttt..",
    "...ttt.ttt.ttt..",
    "...ttt..t...t...",
    "....t...........",
    ".....rrrrrr.....",
    "....rppppppr....",
    "....rppppppr....",
    "....rppppppr....",
    ".....rppppr.....",
    "......rppr......",
    ".......rr.......",
    "................",
]
SHADES = {"c": 255, "t": 205, "p": 170, "r": 110}


def draw(rows):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = image.load()
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            if key != ".":
                px[x, y] = (255, 255, 255, SHADES[key])
    return image


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    left = draw(LEFT)
    left.save(OUT / "footprint_left.png")
    ImageOps.mirror(left).save(OUT / "footprint_right.png")
    print("wrote", OUT / "footprint_left.png", OUT / "footprint_right.png")


if __name__ == "__main__":
    main()
