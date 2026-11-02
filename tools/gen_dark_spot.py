"""Draws the dark spot marker (Pillow): a soft rounded square of light lying on the floor where a monster could spawn,
white so the game tints it, a faint wash inside a brighter rim that fades out to the edge. Drawn added to the scene, so
its brightness is its alpha. Read by client/DarkSpots.

Run from the repository root: python tools/gen_dark_spot.py
"""
from pathlib import Path

from PIL import Image

OUT = Path("common/src/main/resources/assets/whycantisleep/textures/vfx/dark_spot.png")
SIZE = 64
# The square's half width (of the texture's half), how round its corners are, and where its rim sits and how wide.
HALF = 0.80
CORNER = 0.45
RIM = 0.70
RIM_WIDTH = 0.10
# The wash inside, and the rim at its brightest.
WASH = 0.22
RIM_PEAK = 0.75


def rounded_distance(x, y):
    """How far out a point is in a rounded square of half width HALF, 1 at its edge."""
    inner = HALF - CORNER
    dx = max(abs(x) - inner, 0.0)
    dy = max(abs(y) - inner, 0.0)
    corner = (dx * dx + dy * dy) ** 0.5
    if dx > 0.0 and dy > 0.0:
        return (inner + corner) / HALF
    return max(abs(x), abs(y)) / HALF


def smoothstep(edge0, edge1, x):
    t = min(max((x - edge0) / (edge1 - edge0), 0.0), 1.0)
    return t * t * (3.0 - 2.0 * t)


def main():
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = image.load()
    for j in range(SIZE):
        for i in range(SIZE):
            x = (i + 0.5) / SIZE * 2.0 - 1.0
            y = (j + 0.5) / SIZE * 2.0 - 1.0
            d = rounded_distance(x, y)
            # A faint wash that thins toward the rim, a soft bright band at the rim, and nothing past the edge.
            wash = WASH * (1.0 - 0.5 * smoothstep(0.0, RIM, d))
            rim = RIM_PEAK * max(0.0, 1.0 - abs(d - RIM) / RIM_WIDTH) ** 1.5
            edge = 1.0 - smoothstep(RIM, 1.0, d)
            alpha = min(1.0, (wash + rim) * edge)
            px[i, j] = (255, 255, 255, round(alpha * 255))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    image.save(OUT)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
