"""Draws the Dark Between's block textures and writes everything a block needs besides its Java (WCISBlocks.java): its
blockstates, block and item models, loot table, tags and English name.

    python tools/gen_between_blocks.py

The palette is the Lurker's (LurkerPalette.java) with the Dark Between's own stone and wood tones. Every texture is 16 by 16,
drawn pixel by pixel from a fixed seed, so running it again gives the same files.
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
RES = os.path.join(ROOT, "common", "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "whycantisleep")
DATA = os.path.join(RES, "data", "whycantisleep")
NS = "whycantisleep"

# ---- Colours ----
STONE = [(20, 17, 26), (31, 26, 39), (42, 36, 52), (55, 48, 68), (70, 62, 86)]
MORTAR = (14, 12, 18)
CRIMSON = (138, 10, 36)
RED = (255, 45, 70)
HOT = (255, 170, 160)
ASH = [(42, 38, 40), (58, 52, 54), (78, 72, 74), (104, 98, 100)]
MOSS = [(24, 38, 40), (34, 52, 54), (48, 70, 70), (70, 96, 92)]
BARK = [(52, 48, 58), (72, 68, 78), (94, 90, 100)]
WOOD = [(118, 112, 122), (140, 134, 144), (162, 156, 166), (182, 177, 186)]
ROT = [(56, 50, 52), (74, 66, 66), (92, 84, 82)]
PAPER = [(70, 22, 34), (92, 32, 46), (116, 44, 60), (150, 74, 88)]
HEDGE = [(18, 26, 22), (28, 40, 32), (40, 54, 42), (66, 58, 44)]
IRON = [(22, 22, 28), (40, 40, 48), (64, 64, 74)]
ROSE = [(116, 40, 74), (178, 80, 120), (233, 182, 184), (255, 228, 232)]
CLEAR = (0, 0, 0, 0)


def new():
    return Image.new("RGBA", (16, 16), CLEAR)


def put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), c if len(c) == 4 else c + (255,))


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(c, f):
    return tuple(max(0, min(255, int(c[i] * f))) for i in range(3))


def grain(rng, palette, weights):
    return palette[rng.choices(range(len(palette)), weights)[0]]


# ---- Textures ----
# Stone is drawn clean: smooth faces with only a fine grain, every block and brick bevelled, lit along its top and
# left edges and shadowed along its bottom and right, as if lit from the upper left.
FACE = [(36, 31, 46), (44, 38, 56), (52, 46, 66), (62, 55, 78)]
LIP = (88, 79, 106)
SHADOW = (24, 20, 31)
GROOVE = (12, 10, 16)


def fine(rng, base, spread=0.05):
    """A colour with a fine, quiet grain: a few percent lighter or darker."""
    return shade(base, 1.0 + rng.uniform(-spread, spread))


def bevel(img, rng, x0, y0, x1, y1, base, lit=LIP, dark=SHADOW, inner=True):
    """A bevelled face from (x0, y0) to (x1, y1) inclusive: a soft gradient across it, a lit top and left edge, a
    dark bottom and right edge, and (inner) a second, softer step of each inside them."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            t = ((x - x0) / max(x1 - x0, 1) + (y - y0) / max(y1 - y0, 1)) / 2
            c = fine(rng, mix(shade(base, 1.08), shade(base, 0.9), t))
            if y == y0 or x == x0:
                c = lit if (y == y0 and x < x1) or (x == x0 and y < y1) else c
            elif y == y1 or x == x1:
                c = dark
            elif inner and (y == y0 + 1 or x == x0 + 1):
                c = mix(c, lit, 0.3)
            elif inner and (y == y1 - 1 or x == x1 - 1):
                c = mix(c, dark, 0.3)
            put(img, x, y, c)


LICHEN = [(58, 66, 52), (74, 84, 62)]
RUST = (92, 26, 34)


def grime(img, rng, amount=1.0, mortar=None):
    """Ages a stone texture: soot streaking down from the top, dirt gathered low, lichen in the joints, a few chips
    knocked out of edges, hairline cracks, and the odd stain of the Dark Between's red seeping through."""
    # Dirt, gathering towards the foot.
    for y in range(16):
        for x in range(16):
            r, g, b_, a = img.getpixel((x, y))
            if a == 0:
                continue
            dirt = 1.0 - 0.18 * amount * (y / 15) ** 2 - (0.06 * amount if rng.random() < 0.08 else 0.0)
            put(img, x, y, shade((r, g, b_), dirt))
    # Soot streaks, running down from the top in a few columns.
    for _ in range(int(3 * amount)):
        x = rng.randrange(16)
        length = rng.randint(3, 9)
        for y in range(length):
            r, g, b_, a = img.getpixel((x, y))
            put(img, x, y, shade((r, g, b_), 0.78 + 0.2 * y / length))
    # Lichen, in the mortar joints.
    if mortar is not None:
        for y in range(16):
            for x in range(16):
                if mortar(x, y) and rng.random() < 0.16 * amount:
                    put(img, x, y, rng.choice(LICHEN))
    # Chips: a pixel or two knocked out of an edge, dark where the stone is broken, lit on the far side.
    for _ in range(int(4 * amount)):
        x, y = rng.randrange(1, 15), rng.randrange(1, 15)
        if mortar is not None and mortar(x, y):
            continue
        put(img, x, y, GROOVE)
        put(img, x + 1, y + 1, mix(FACE[2], LIP, 0.5))
    # Hairline cracks.
    for _ in range(int(2 * amount)):
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(rng.randint(2, 5)):
            if 0 <= x < 16 and 0 <= y < 16:
                put(img, x, y, shade(img.getpixel((x, y))[:3], 0.55))
            x += rng.choice((-1, 0, 1))
            y += 1
    # Now and then, a red stain seeping out of a joint.
    if rng.random() < 0.35 * amount:
        x, y = rng.randrange(16), rng.randrange(12)
        for k in range(rng.randint(2, 4)):
            if y + k < 16:
                put(img, x, y + k, mix(img.getpixel((x, y + k))[:3], RUST, 0.6 - k * 0.12))
    return img


def stone_base(rng, img=None, lo=0, hi=None):
    img = img or new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, fine(rng, mix(FACE[2], FACE[1], (x + y) / 30.0), 0.07))
    return img


def dreamstone(variant=0):
    """Dreamstone in the raw: a smooth dark face with soft violet strata and a few fine flaws, weathered."""
    rng = random.Random(1 + variant * 101)
    img = stone_base(rng)
    for y in (4, 5, 11):
        for x in range(16):
            if rng.random() < 0.75:
                put(img, x, y, mix(img.getpixel((x, y))[:3], (70, 52, 88), 0.35 if y != 5 else 0.18))
    for start in ((3, 1), (12, 8)):
        x, y = start
        for _ in range(4):
            put(img, x, y, SHADOW)
            put(img, x + 1, y, mix(FACE[2], LIP, 0.4))
            x += rng.choice((0, 1))
            y += 1
    return grime(img, rng, 0.8)


def bricks(cracked=False, seed=2, variant=0):
    """Courses of dreamstone bricks, four high, each bevelled, in recessed mortar. Each variant lays them its own way:
    the joints fall in different places, some bricks long and some short, now and then a half brick or a patched-in
    stone of a different shade. Then aged: soot, dirt, lichen in the joints, chips. Cracked: worse, with clean cracks
    across them and a corner gone."""
    rng = random.Random(seed + variant * 37)
    img = new()
    joints = set()
    for y in range(16):
        for x in range(16):
            put(img, x, y, GROOVE if y % 4 == 3 else SHADOW)
            if y % 4 == 3:
                joints.add((x, y))
    for row in range(4):
        tone = [FACE[2], FACE[1], FACE[3], FACE[2]][(row + variant) % 4]
        # Where this course's joints fall: a running bond for the first variant, varied lengths for the others.
        x = -rng.randrange(0, 6) if variant else (0 if row % 2 == 0 else -4)
        while x < 16:
            length = 7 if variant == 0 else rng.choice((4, 5, 6, 7, 7, 8, 9))
            x0, x1 = x, x + length - 1
            patched = variant and rng.random() < 0.15
            base = shade(tone, rng.uniform(0.9, 1.06))
            if patched:
                base = mix(base, (70, 58, 64), 0.4)
            for y in range(row * 4, row * 4 + 3):
                for px in range(max(x0, 0), min(x1, 15) + 1):
                    ty, tx = y - row * 4, px - x0
                    c = fine(rng, base, 0.05)
                    if ty == 0:
                        c = mix(base, LIP, 0.6)
                    elif ty == 2:
                        c = shade(base, 0.72)
                    if tx == 0 and ty < 2:
                        c = mix(c, LIP, 0.4)
                    elif tx == length - 1:
                        c = shade(c, 0.78)
                    put(img, px, y, c)
            if 0 <= x1 + 1 < 16:
                for y in range(row * 4, row * 4 + 3):
                    put(img, x1 + 1, y, GROOVE)
                    joints.add((x1 + 1, y))
            x = x1 + 2
    if cracked:
        for _ in range(3):
            x, y = rng.randrange(16), rng.randrange(4)
            for _ in range(rng.randint(5, 9)):
                if 0 <= x < 15 and y < 16:
                    put(img, x, y, (6, 4, 8))
                    put(img, x + 1, y, mix(FACE[2], LIP, 0.5))
                x += rng.choice((-1, 0, 1))
                y += 1
        # A corner gone, the stone behind it in shadow.
        cx, cy = rng.choice(((13, 12), (1, 0), (12, 4)))
        for x, y in ((cx, cy), (cx + 1, cy), (cx + 1, cy + 1), (cx, cy + 1), (cx + 2, cy)):
            if 0 <= x < 16 and 0 <= y < 16:
                put(img, x, y, GROOVE)
                joints.add((x, y))
    return grime(img, rng, 1.6 if cracked else 1.0, mortar=lambda x, y: (x, y) in joints)


def veined_bricks():
    """Dreamstone bricks split by the Dark Between's veins: glowing red threads running along the mortar and breaking across
    the bricks, hottest where they branch, the stone round them darkened as if scorched."""
    rng = random.Random(61)
    img = bricks(False, 2, 2)
    for start in ((0, 7), (5, 0), (16, 11)):
        x, y = start
        dx = 1 if start[0] == 0 else (-1 if start[0] == 16 else 0)
        for _ in range(16):
            if 0 <= x < 16 and 0 <= y < 16:
                for nx, ny in ((x, y - 1), (x, y + 1), (x - 1, y), (x + 1, y)):
                    if 0 <= nx < 16 and 0 <= ny < 16 and img.getpixel((nx, ny))[:3] not in (RED, HOT, CRIMSON):
                        put(img, nx, ny, mix(img.getpixel((nx, ny))[:3], (40, 6, 14), 0.45))
                put(img, x, y, HOT if rng.random() < 0.12 else RED if rng.random() < 0.6 else CRIMSON)
            if dx:
                x += dx
                y += rng.choice((-1, 0, 0, 1))
            else:
                y += 1
                x += rng.choice((-1, 0, 0, 1))
    return img


def heartwood_seal():
    """Heartwood that seals the weeping tree's heart: the trunk's own bark (the sleepwood log's), so the door reads as
    part of the tree, with a few small runes cut into the grain between its splits, a faint red in the deepest of
    the cuts."""
    img = log_side()
    cut, deep = (30, 28, 34), (96, 16, 34)
    # Small runes, three pixels across, each a stroke down and a stroke or two off it, set between the splits.
    runes = [((5, 2), [(1, 0), (1, 1), (1, 2), (0, 1), (2, 3)]),
             ((10, 5), [(0, 0), (1, 1), (1, 2), (2, 0), (1, 3)]),
             ((5, 9), [(0, 0), (0, 1), (1, 1), (2, 1), (2, 2)]),
             ((10, 11), [(1, 0), (0, 1), (1, 1), (2, 2), (1, 3)])]
    for (ox, oy), strokes in runes:
        for i, (dx, dy) in enumerate(strokes):
            put(img, ox + dx, oy + dy, deep if i == 2 else cut)
    return img


def chiseled():
    """A bevelled block with a sunken almond carved in it, and in that an eye: red iris, a slit pupil, a glint."""
    rng = random.Random(3)
    img = new()
    bevel(img, rng, 0, 0, 15, 15, FACE[2])
    # The sunken field round the eye, its upper rim in shadow and its lower one lit, as a cut in stone is.
    for y in range(3, 13):
        for x in range(2, 14):
            dx = (x - 7.5) / 5.6
            dy = (y - 7.5) / 4.2
            d = dx * dx + dy * dy
            if d <= 1.0:
                c = fine(rng, FACE[1], 0.04)
                if d > 0.72:
                    c = SHADOW if y < 8 else LIP
                put(img, x, y, c)
    for y in range(4, 12):
        for x in range(3, 13):
            dx = (x - 7.5) / 4.6
            dy = (y - 7.5) / 3.0
            d = dx * dx + dy * dy
            if 0.7 < d <= 1.0:
                put(img, x, y, GROOVE)
            elif d <= 0.7:
                put(img, x, y, mix(FACE[3], LIP, 0.35))
    for y in range(5, 11):
        for x in range(6, 10):
            r = (x - 7.5) ** 2 + (y - 7.5) ** 2
            if r <= 5.5:
                put(img, x, y, CRIMSON if r > 2 else RED)
    for y in range(5, 11):
        put(img, 7, y, (6, 2, 4))
        put(img, 8, y, (6, 2, 4))
    put(img, 6, 6, HOT)
    return grime(img, rng, 0.6)


def engraved():
    """Dreamstone bricks with a band of runes cut across them: a course of long bricks above and below, and between
    them a smooth bevelled band carved with angular glyphs, each groove dark with a lit lip under it, one still
    smouldering a dull red."""
    rng = random.Random(21)
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, GROOVE)
    bevel(img, rng, 0, 0, 7, 2, FACE[2], inner=False)
    bevel(img, rng, 8, 0, 15, 2, FACE[1], inner=False)
    bevel(img, rng, 0, 12, 3, 14, FACE[1], inner=False)
    bevel(img, rng, 4, 12, 11, 14, FACE[2], inner=False)
    bevel(img, rng, 12, 12, 15, 14, FACE[3], inner=False)
    bevel(img, rng, 0, 4, 15, 10, FACE[3])
    glyphs = [
        [(0, 0), (0, 1), (0, 2), (1, 1), (2, 0), (2, 2)],
        [(1, 0), (0, 1), (2, 1), (1, 2), (0, 2), (2, 2)],
        [(0, 0), (1, 0), (2, 0), (2, 1), (1, 2), (0, 2)],
    ]
    for i, glyph in enumerate(glyphs):
        ox = 2 + i * 4
        ember = i == 1
        for gx, gy in glyph:
            x, y = ox + gx, 6 + gy
            put(img, x, y, mix(CRIMSON, GROOVE, 0.45) if ember else GROOVE)
            if (gx, gy + 1) not in glyph:
                put(img, x, y + 1, mix(CRIMSON, LIP, 0.45) if ember else LIP)
    return grime(img, rng, 0.8, mortar=lambda x, y: y in (3, 11))


def polished(variant=0):
    """Dreamstone ground smooth and squared: a clean bevel all round, a soft sheen falling across it from the upper
    left, and a faint inner border."""
    rng = random.Random(22 + variant * 53)
    img = new()
    bevel(img, rng, 0, 0, 15, 15, FACE[3])
    for y in range(2, 14):
        for x in range(2, 14):
            c = img.getpixel((x, y))[:3]
            sheen = max(0.0, 1.0 - abs((x + y) - 9) / 4.0) * 0.12
            put(img, x, y, shade(c, 1.0 + sheen))
    for i in range(3, 13):
        for x, y in ((i, 3), (3, i)):
            put(img, x, y, mix(img.getpixel((x, y))[:3], SHADOW, 0.25))
        for x, y in ((i, 12), (12, i)):
            put(img, x, y, mix(img.getpixel((x, y))[:3], LIP, 0.2))
    # Wear: a few fine scratches across the polish, catching the light.
    for _ in range(3):
        x, y = rng.randrange(3, 11), rng.randrange(3, 13)
        for k in range(rng.randint(2, 4)):
            put(img, x + k, y, mix(img.getpixel((x + k, y))[:3], LIP, 0.35))
    return grime(img, rng, 0.6)


def pillar_side():
    """A fluted column: five flutes down its face, each lit on its left and shadowed on its right, between narrow
    fillets, a bevelled band at its head and foot."""
    rng = random.Random(4)
    img = new()
    for y in range(16):
        for x in range(16):
            band = y in (0, 1, 14, 15)
            if band:
                c = LIP if y in (0, 14) else SHADOW
                if y in (1, 15):
                    c = SHADOW if y == 15 else shade(FACE[2], 0.85)
            else:
                f = (x - 1) % 3
                if x in (0, 15):
                    c = SHADOW if x == 15 else mix(FACE[3], LIP, 0.4)
                elif f == 0:
                    c = mix(FACE[3], LIP, 0.35)  # the fillet, catching the light
                elif f == 1:
                    c = fine(rng, FACE[1], 0.04)
                else:
                    c = fine(rng, shade(FACE[1], 0.75), 0.04)
            put(img, x, y, c)
    return grime(img, rng, 0.7)


def pillar_top():
    """The column's end: bevelled, rings cut in it round a centre."""
    rng = random.Random(5)
    img = new()
    bevel(img, rng, 0, 0, 15, 15, FACE[2])
    for y in range(2, 14):
        for x in range(2, 14):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 4.6 < r <= 5.4 or 2.0 < r <= 2.6:
                put(img, x, y, SHADOW if y < 8 else LIP)
            elif r <= 2.0:
                put(img, x, y, mix(FACE[3], LIP, 0.3))
    return img


def veined():
    rng = random.Random(6)
    img = dreamstone()
    # Glowing veins wandering across, hottest where they branch, each with a dark seam either side of it.
    for start in ((0, 3), (0, 11), (6, 0)):
        x, y = start
        horizontal = start[0] == 0
        for _ in range(18):
            put(img, x, y, RED if rng.random() < 0.55 else CRIMSON)
            if rng.random() < 0.12:
                put(img, x, y, HOT)
            if horizontal:
                put(img, x, y - 1, mix(img.getpixel((x, max(y - 1, 0)))[:3], GROOVE, 0.5)) if y > 0 else None
                x += 1
                y += rng.choice((-1, 0, 0, 1))
            else:
                y += 1
                x += rng.choice((-1, 0, 0, 1))
            if not (0 <= x < 16 and 0 <= y < 16):
                break
    return img


def ashen_soil():
    """Grey ash packed hard: soft clumps, lit on top, a few red embers buried in it."""
    rng = random.Random(7)
    img = new()
    for y in range(16):
        for x in range(16):
            cx, cy = x % 4, (y + (x // 4) * 2) % 4
            c = ASH[2] if (cx, cy) == (1, 0) else ASH[1] if cx + cy < 4 else ASH[0]
            put(img, x, y, fine(rng, c, 0.06))
    for x, y in ((3, 6), (11, 2), (13, 12), (6, 13)):
        put(img, x, y, CRIMSON)
    return img


def moss_top():
    """Grey-green moss in soft tufts: each lit at its top-left, darker in the hollows between."""
    rng = random.Random(8)
    img = new()
    for y in range(16):
        for x in range(16):
            cx, cy = (x + (y // 3) % 2 * 2) % 4, y % 3
            c = MOSS[3] if (cx, cy) == (1, 0) else MOSS[2] if cx + cy < 3 else MOSS[1] if cx + cy < 5 else MOSS[0]
            put(img, x, y, fine(rng, c, 0.05))
    for x, y in ((4, 9), (12, 3), (9, 14)):
        put(img, x, y, ASH[3])
    return img


def moss_side():
    rng = random.Random(9)
    img = ashen_soil()
    depth = [rng.choice((2, 3, 3, 4, 4, 5)) for _ in range(16)]
    for x in range(16):
        for y in range(depth[x]):
            put(img, x, y, fine(rng, MOSS[2] if y == 0 else MOSS[1], 0.05))
        put(img, x, depth[x], MOSS[0])
        put(img, x, depth[x] + 1, shade(ASH[1], 0.75))
    return img


def log_side():
    rng = random.Random(10)
    img = new()
    for x in range(16):
        tone = rng.choice((0, 1, 1, 2))
        for y in range(16):
            c = BARK[tone]
            if rng.random() < 0.18:
                c = BARK[max(0, tone - 1)]
            put(img, x, y, c)
    # Deep splits running down the bark.
    for x in (3, 9, 13):
        y = rng.randrange(0, 4)
        while y < 16:
            put(img, x, y, (30, 28, 34))
            y += 1
            if rng.random() < 0.15:
                y += 2
    return img


def log_top():
    img = new()
    for y in range(16):
        for x in range(16):
            r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            edge = min(x, y, 15 - x, 15 - y)
            if edge == 0:
                c = BARK[0]
            elif edge == 1:
                c = BARK[1]
            else:
                ring = int(r * 1.3) % 3
                c = WOOD[1 + (ring == 0)] if ring else WOOD[0]
            put(img, x, y, c)
    put(img, 7, 7, WOOD[0])
    put(img, 8, 8, WOOD[0])
    return img


def planks(palette=WOOD, seed=11, rotten=False):
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        board = y // 4
        for x in range(16):
            seam = (x + board * 5) % 16 == 0
            if y % 4 == 3:
                c = shade(palette[0], 0.7)
            elif seam:
                c = shade(palette[0], 0.8)
            else:
                c = grain(rng, palette[1:], [4, 3] + [1] * (len(palette) - 3))
                if y % 4 == 0:
                    c = mix(c, palette[-1], 0.3)
            put(img, x, y, c)
    if rotten:
        # Holes eaten through, and damp stains.
        for _ in range(4):
            x, y = rng.randrange(1, 15), rng.randrange(1, 15)
            for dx, dy in ((0, 0), (1, 0), (0, 1)):
                put(img, x + dx, y + dy, (12, 10, 12))
        for _ in range(10):
            x, y = rng.randrange(16), rng.randrange(16)
            put(img, x, y, (40, 46, 40))
    else:
        for _ in range(6):
            put(img, rng.randrange(16), rng.randrange(16), shade(palette[1], 0.85))
    return img


def wallpaper():
    rng = random.Random(12)
    img = new()
    for y in range(16):
        for x in range(16):
            c = grain(rng, PAPER[:2], [3, 2])
            # A faded damask: a diamond lattice with a small motif in each.
            if (x + y) % 8 == 0 or (x - y) % 8 == 0:
                c = PAPER[2]
            if (x % 8, y % 8) in ((4, 3), (4, 5), (3, 4), (5, 4)):
                c = PAPER[3]
            put(img, x, y, c)
    # A tear down one side, showing grey boards beneath, its edge curling pale.
    for y in range(16):
        width = 3 + int(2 * abs(((y * 7) % 11) / 11 - 0.5) * 2)
        for x in range(16 - width, 16):
            put(img, x, y, planks(ROT, 13).getpixel((x, y))[:3])
        put(img, 15 - width, y, (196, 170, 160))
    return img


def carpet():
    rng = random.Random(14)
    img = new()
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            c = grain(rng, PAPER[:3], [3, 3, 1])
            if edge == 1:
                c = (150, 120, 70)
            if rng.random() < 0.08:
                c = ASH[2]
            put(img, x, y, c)
    return img


def hedge():
    """Withered hedge: clumps of dead leaves, each lit at its top-left and shading into a dark hollow, gaps between
    them where the light shows through, dead twigs crossing it."""
    rng = random.Random(15)
    img = new()
    for cy in range(0, 16, 4):
        for cx in range(-2 if cy % 8 else 0, 16, 4):
            for y in range(cy, cy + 4):
                for x in range(cx, cx + 4):
                    if not (0 <= x < 16):
                        continue
                    lx, ly = x - cx, y - cy
                    if (lx, ly) in ((3, 3), (0, 3), (3, 0)) and rng.random() < 0.7:
                        continue  # the gaps between clumps
                    t = (lx + ly) / 6
                    c = HEDGE[2] if t < 0.2 else HEDGE[1] if t < 0.6 else HEDGE[0]
                    if rng.random() < 0.06:
                        c = HEDGE[3]
                    put(img, x, y, fine(rng, c, 0.05))
    for _ in range(3):
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(5):
            put(img, x, y, (64, 50, 42))
            x += 1
            y += rng.choice((-1, 1))
    return img


def crystal():
    img = new()
    rng = random.Random(16)
    # Three shards rising from the base, the middle tallest.
    for cx, top, half in ((4, 6, 1), (8, 1, 2), (12, 7, 1)):
        for y in range(top, 16):
            for x in range(cx - half, cx + half + 1):
                t = (y - top) / max(1, 15 - top)
                c = mix(ROSE[3], ROSE[1], t)
                if x == cx - half:
                    c = mix(c, (255, 255, 255), 0.3)
                if x == cx + half:
                    c = shade(c, 0.75)
                put(img, x, y, c)
        put(img, cx, top - 1, ROSE[3])
    return img


def bloom():
    img = new()
    # A thin stem with two leaves, under a drooping cup of pale, glowing petals.
    for y in range(8, 16):
        put(img, 8, y, (36, 52, 48))
    put(img, 7, 12, (48, 70, 64))
    put(img, 6, 11, (48, 70, 64))
    put(img, 9, 10, (48, 70, 64))
    put(img, 10, 9, (48, 70, 64))
    for y in range(3, 8):
        for x in range(5, 12):
            dx = x - 8
            dy = y - 5
            if dx * dx * 0.6 + dy * dy * 0.9 <= 6:
                put(img, x, y, ROSE[3] if dy < 1 else ROSE[2])
    put(img, 8, 7, HOT)
    put(img, 7, 8, ROSE[2])
    put(img, 9, 8, ROSE[2])
    return img


def thorns():
    rng = random.Random(17)
    img = new()
    # Black-violet stems twisting up, set with thorns.
    for base in (4, 8, 11):
        x = base
        for y in range(15, 1, -1):
            put(img, x, y, (34, 20, 36))
            if rng.random() < 0.3:
                put(img, x + rng.choice((-1, 1)), y, (60, 30, 52))
            if rng.random() < 0.25:
                x += rng.choice((-1, 1))
            if y < 5 and rng.random() < 0.3:
                break
    put(img, 8, 3, CRIMSON)
    return img


def veins_hanging():
    rng = random.Random(18)
    img = new()
    for x in (3, 7, 8, 12):
        length = rng.randrange(8, 16)
        cx = x
        for y in range(length):
            put(img, cx, y, RED if y < length - 2 else HOT)
            if rng.random() < 0.2:
                cx += rng.choice((-1, 1))
    return img


def reliquary_wood(top=False):
    """The heartwood reliquary's boards: dark heartwood laid across, a red grain running in it; on the lid's top, an
    iron cross over it and a ring of runes cut in the middle."""
    rng = random.Random(41 if top else 40)
    img = new()
    for y in range(16):
        board = y // 4
        for x in range(16):
            c = BARK[0] if y % 4 == 3 else BARK[1 if (x + board * 5) % 9 else 0]
            if y % 4 != 3 and rng.random() < 0.12:
                c = BARK[2]
            if y % 4 == 1 and (x + board * 3) % 7 == 0:
                c = CRIMSON  # the red grain
            put(img, x, y, fine(rng, c, 0.05))
    if top:
        for k in range(16):
            for a, b_ in ((7, k), (8, k), (k, 7), (k, 8)):
                put(img, a, b_, IRON[1] if (a in (7, 8)) != (b_ in (7, 8)) else IRON[2])
        for k in range(16):
            ang = k / 16 * math.tau
            put(img, round(7.5 + math.cos(ang) * 4.5), round(7.5 + math.sin(ang) * 4.5), RED if k % 2 else CRIMSON)
        for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
            put(img, x, y, HOT)
    return img


def reliquary_iron():
    """Dark iron bands, rivets down them."""
    img = new()
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            put(img, x, y, IRON[0] if edge else IRON[1])
    for y in range(2, 16, 4):
        for x in range(2, 16, 4):
            put(img, x, y, IRON[2])
    return img


def reliquary_clasp():
    """The clasp: an iron plate, a keyhole, and over it a red stone set in a ring."""
    img = new()
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            put(img, x, y, IRON[0] if edge == 0 else IRON[2] if edge == 1 and y < 8 else IRON[1])
    for x, y in ((7, 9), (8, 9), (7, 10), (8, 10), (7, 11), (7, 12), (8, 12), (8, 11)):
        put(img, x, y, (10, 8, 12))  # the keyhole
    for x in range(5, 11):
        for y in range(2, 8):
            d = (x - 7.5) ** 2 + (y - 4.5) ** 2
            if d <= 7:
                put(img, x, y, HOT if d < 1.2 else RED if d < 4 else CRIMSON)
    return img


def heart_portal_dormant():
    """The heart portal asleep: a membrane of dark heartwood stretched over the opening, veins of red under its skin
    that glow faintly, running in toward the middle of the nine (drawn so the veins meet across blocks)."""
    rng = random.Random(51)
    img = new()
    for y in range(16):
        for x in range(16):
            c = BARK[0] if (x * 3 + y * 5 + rng.randrange(3)) % 11 else BARK[1]
            put(img, x, y, fine(rng, c, 0.07))
    for x0, y0, dx, dy in ((0, 4, 1, 0), (0, 11, 1, 0), (5, 0, 0, 1), (10, 0, 0, 1)):
        x, y = x0, y0
        while 0 <= x < 16 and 0 <= y < 16:
            put(img, x, y, CRIMSON if (x + y) % 4 else RED)
            if rng.random() < 0.25:
                if dx:
                    y = max(0, min(15, y + rng.choice((-1, 1))))
                else:
                    x = max(0, min(15, x + rng.choice((-1, 1))))
            x, y = x + dx, y + dy
    return img


def urn():
    """An urn's sides: dark polished stone, a band of red runes round its belly, a fine line above and below."""
    rng = random.Random(82)
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, fine(rng, FACE[2], 0.05))
    for x in range(16):
        put(img, x, 5, LIP)
        put(img, x, 10, SHADOW)
        for y in (7, 8):
            put(img, x, y, CRIMSON if (x + y) % 3 == 0 else FACE[0])
    return img


FIRE_FRAMES = 8


def brazier_fire():
    """The Dark Between's fire, for braziers and fire lamps: a strip of frames (animated, brazier_fire.png.mcmeta), tongues
    of flame that lick up and sway, white-gold at their roots, orange, then red at their tips, a few sparks above."""
    rng = random.Random(83)
    img = Image.new("RGBA", (16, 16 * FIRE_FRAMES), (0, 0, 0, 0))
    gold, orange, red, deep = (255, 226, 150), (255, 140, 50), (240, 60, 40), (170, 20, 40)
    base = [rng.uniform(0.55, 1.0) for _ in range(16)]
    for f in range(FIRE_FRAMES):
        phase = f / FIRE_FRAMES * math.tau
        for x in range(1, 15):
            # How tall this tongue is now: its own height, breathing, highest in the middle of the fire.
            middle = 1.0 - abs(x - 7.5) / 8.0
            h = int(15 * middle * base[x] * (0.75 + 0.25 * math.sin(phase * 2 + x * 1.7)) + 2)
            sway = round(math.sin(phase + x * 0.9) * 0.8)
            for k in range(min(h, 15)):
                t = k / max(1, h - 1)
                c = gold if t < 0.2 else orange if t < 0.5 else red if t < 0.8 else deep
                px = max(0, min(15, x + (sway if t > 0.5 else 0)))
                img.putpixel((px, f * 16 + 15 - k), c + (255,))
        for _ in range(2):
            img.putpixel((rng.randrange(3, 13), f * 16 + rng.randrange(0, 4)), orange + (255,))
    return img


def brazier_coals():
    """Coals in the brazier's bowl: black, cracked through with glowing red."""
    rng = random.Random(84)
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, RED if rng.random() < 0.18 else HOT if rng.random() < 0.04 else (18, 12, 14))
    return img


VEIN_STRANDS = (3, 6, 9, 12)


def veins_weeping(tip):
    """Weeping veins. The body: strands running the full height, at the same places top and bottom so blocks stacked
    join into one long strand, swaying a pixel and back, dark at their cores and swollen in places. The tip: the same
    strands from the top, ending at different lengths each in a glowing drop."""
    rng = random.Random(19 if tip else 20)
    img = new()
    dark = (96, 8, 26)
    for i, x in enumerate(VEIN_STRANDS):
        end = 16 if not tip else rng.randrange(7, 15)
        sway = rng.randrange(4, 12)
        for y in range(end):
            cx = x + (1 if sway <= y < sway + 3 and i % 2 else 0)  # out and back, so it meets the next block
            put(img, cx, y, CRIMSON if (y + i) % 5 == 0 else RED)
            if (y + i * 3) % 7 == 0:
                put(img, cx + (1 if i < 2 else -1), y, dark)  # a swelling along it
        if tip:
            cx = x
            put(img, cx, end, HOT)
            put(img, cx, end + 1 if end < 15 else end, RED)
            put(img, cx - 1, end, RED)
    return img


# ---- Blocks: (name, kind, textures) ----
TEXTURES = {
    "dreamstone": dreamstone, "dreamstone_bricks": bricks, "cracked_dreamstone_bricks": lambda: bricks(True, 2),
    "chiseled_dreamstone": chiseled, "dreamstone_pillar": pillar_side, "dreamstone_pillar_top": pillar_top,
    "engraved_dreamstone_bricks": engraved, "polished_dreamstone": polished, "veined_dreamstone_bricks": veined_bricks, "heartwood_seal": heartwood_seal,
    "dreamstone_1": lambda: dreamstone(1), "dreamstone_2": lambda: dreamstone(2),
    "dreamstone_bricks_1": lambda: bricks(False, 2, 1), "dreamstone_bricks_2": lambda: bricks(False, 2, 2),
    "dreamstone_bricks_3": lambda: bricks(False, 2, 3),
    "cracked_dreamstone_bricks_1": lambda: bricks(True, 2, 1), "cracked_dreamstone_bricks_2": lambda: bricks(True, 2, 2),
    "polished_dreamstone_1": lambda: polished(1),
    "veined_dreamstone": veined, "ashen_soil": ashen_soil, "ashen_moss_top": moss_top, "ashen_moss_side": moss_side,
    "sleepwood_log": log_side, "sleepwood_log_top": log_top, "sleepwood_planks": planks,
    "rotten_sleepwood": lambda: planks(ROT, 13, True), "torn_wallpaper": wallpaper, "dusty_carpet": carpet,
    "withered_hedge": hedge, "wake_crystal": crystal, "lucid_bloom": bloom,
    "despair_thorns": thorns, "hanging_veins": veins_hanging,
    "weeping_veins": lambda: veins_weeping(True), "weeping_veins_plant": lambda: veins_weeping(False),
    "reliquary_wood": reliquary_wood, "reliquary_top": lambda: reliquary_wood(True), "reliquary_iron": reliquary_iron,
    "reliquary_clasp": reliquary_clasp, "heart_portal": heart_portal_dormant,
    "urn": urn, "brazier_fire": brazier_fire, "brazier_coals": brazier_coals,
}

NAMES = {
    "dreamstone": "Dreamstone", "dreamstone_bricks": "Dreamstone Bricks", "cracked_dreamstone_bricks": "Cracked Dreamstone Bricks",
    "chiseled_dreamstone": "Chiseled Dreamstone", "dreamstone_pillar": "Dreamstone Pillar",
    "engraved_dreamstone_bricks": "Engraved Dreamstone Bricks", "polished_dreamstone": "Polished Dreamstone",
    "polished_dreamstone_stairs": "Polished Dreamstone Stairs", "polished_dreamstone_slab": "Polished Dreamstone Slab",
    "dreamstone_wall": "Dreamstone Wall", "veined_dreamstone_bricks": "Veined Dreamstone Bricks", "heartwood_seal": "Heartwood Seal",
    "dreamstone_brick_stairs": "Dreamstone Brick Stairs", "dreamstone_brick_slab": "Dreamstone Brick Slab",
    "dreamstone_brick_wall": "Dreamstone Brick Wall", "veined_dreamstone": "Veined Dreamstone", "ashen_soil": "Ashen Soil",
    "ashen_moss": "Ashen Moss", "sleepwood_log": "Sleepwood Log", "sleepwood_planks": "Sleepwood Planks",
    "sleepwood_stairs": "Sleepwood Stairs", "sleepwood_slab": "Sleepwood Slab", "sleepwood_fence": "Sleepwood Fence",
    "rotten_sleepwood": "Rotten Sleepwood", "torn_wallpaper": "Torn Wallpaper", "dusty_carpet": "Dusty Carpet",
    "withered_hedge": "Withered Hedge", "dread_lantern": "Dread Lantern", "wake_crystal": "Wake Crystal",
    "lucid_bloom": "Lucid Bloom", "despair_thorns": "Despair Thorns", "hanging_veins": "Hanging Veins", "weeping_veins": "Weeping Veins", "heartwood_reliquary": "Heartwood Reliquary", "heart_portal": "Heart Portal",
    "dreamstone_urn": "Dreamstone Urn", "dreamstone_urn_small": "Small Dreamstone Urn",
    "dreamstone_amphora": "Dreamstone Amphora", "dreamstone_jar": "Dreamstone Jar", "stone_planter": "Stone Planter", "dread_brazier": "Dread Brazier", "dread_fire_lamp": "Dread Fire Lamp", "stone_tomb": "Stone Tomb", "dreamstone_spikes": "Dreamstone Spikes", "sleepwood_ladder": "Sleepwood Ladder",
    "dread_candelabra": "Dread Candelabra", "dread_candelabra_single": "Dread Candlestick",
    "dread_candelabra_double": "Twin Dread Candelabra", "dread_candelabra_grand": "Grand Dread Candelabra",
    "dread_chamberstick": "Dread Chamberstick", "dread_candle_stand": "Dread Candle Stand", "dread_dining_table": "Dread Dining Table", "dread_plate": "Dread Plate", "dread_goblet": "Dread Goblet",
    "scattered_bones": "Scattered Bones",
    "weeping_basin": "Weeping Basin", "mourning_statue": "Mourning Statue",
    "dread_iron_fence": "Dread Iron Fence", "dread_iron_gate": "Dread Iron Gate", "dreamstone_pier_base": "Dreamstone Pier Base",
    "dreamstone_pier_capital": "Dreamstone Pier Capital", "trimmed_hedge": "Trimmed Hedge",
}

PICKAXE = ["dreamstone_urn_small", "dreamstone_amphora", "dreamstone_jar", "dread_candle_stand", "dread_chamberstick", "dread_plate", "dread_goblet", "dread_candelabra", "dread_candelabra_single", "dread_candelabra_double", "dread_candelabra_grand", "weeping_basin", "mourning_statue", "dread_iron_fence", "dread_iron_gate", "dreamstone_pier_base",
           "dreamstone_pier_capital", "dreamstone_spikes", "stone_tomb", "dread_fire_lamp", "dreamstone_urn", "stone_planter", "dread_brazier", "dreamstone", "dreamstone_bricks", "cracked_dreamstone_bricks", "chiseled_dreamstone", "dreamstone_pillar",
           "dreamstone_brick_stairs", "dreamstone_brick_slab", "dreamstone_brick_wall", "veined_dreamstone", "dread_lantern",
           "wake_crystal", "engraved_dreamstone_bricks", "polished_dreamstone", "polished_dreamstone_stairs",
           "polished_dreamstone_slab", "dreamstone_wall", "veined_dreamstone_bricks"]
AXE = ["dread_dining_table", "sleepwood_ladder", "heartwood_reliquary", "sleepwood_log", "sleepwood_planks", "sleepwood_stairs", "sleepwood_slab", "sleepwood_fence", "rotten_sleepwood",
       "torn_wallpaper"]
SHOVEL = ["ashen_soil", "ashen_moss"]
HOE = ["withered_hedge", "trimmed_hedge"]


def tear_particle(n=32, k=8):
    """The particle of a glowing tear (TearGlowClient): a drop, round below and drawn to a point above, white so the
    particle tints it; drawn large and smoothed down, with a soft glow all round it and a brighter core."""
    from PIL import ImageDraw, ImageFilter
    size = n * k
    cx, cy, r, tip = 0.5 * size, 0.6 * size, 0.24 * size, 0.07 * size
    # The drop's outline: its round belly, and the two lines from the tip that meet the belly where they touch it.
    dist = cy - tip
    turn = math.acos(r / dist)
    left = (cx - r * math.sin(turn), cy - r * math.cos(turn))
    right = (cx + r * math.sin(turn), cy - r * math.cos(turn))
    drop = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(drop)
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=255)
    draw.polygon([(cx, tip), left, right], fill=255)
    core = Image.new("L", (size, size), 0)
    ImageDraw.Draw(core).ellipse([cx - r * 0.55, cy - r * 0.45, cx + r * 0.55, cy + r * 0.65], fill=255)
    core = core.filter(ImageFilter.GaussianBlur(r * 0.35))
    glow = drop.filter(ImageFilter.GaussianBlur(size * 0.06))
    out = Image.new("RGBA", (n, n))
    drop, core, glow = (i.resize((n, n), Image.LANCZOS) for i in (drop, core, glow))
    for y in range(n):
        for x in range(n):
            d, c, g = drop.getpixel((x, y)) / 255, core.getpixel((x, y)) / 255, glow.getpixel((x, y)) / 255
            alpha = max(d * (0.7 + 0.3 * c), g * 0.45)
            bright = 0.72 + 0.28 * c
            v = int(255 * bright)
            out.putpixel((x, y), (v, v, v, int(255 * min(1.0, alpha))))
    return out


def ash_particle(n=16, k=8):
    """A flake of ash (TearGlowClient): a torn, flat scrap, white so the particle tints it, its edge a little paler and
    softer than its middle, as burnt paper curls."""
    from PIL import ImageDraw, ImageFilter
    rng = random.Random(77)
    size = n * k
    c = size / 2
    points = []
    for i in range(9):
        turn = i / 9 * math.tau + rng.uniform(-0.2, 0.2)
        reach = size * rng.uniform(0.2, 0.42) * (0.7 if i % 3 == 1 else 1.0)  # torn: some edges bitten in
        points.append((c + math.cos(turn) * reach, c + math.sin(turn) * reach * 0.75))
    flake = Image.new("L", (size, size), 0)
    ImageDraw.Draw(flake).polygon(points, fill=255)
    flake = flake.filter(ImageFilter.GaussianBlur(k * 0.6)).resize((n, n), Image.LANCZOS)
    out = Image.new("RGBA", (n, n))
    for y in range(n):
        for x in range(n):
            a = flake.getpixel((x, y)) / 255
            v = int(255 * (0.75 + 0.25 * (1 - a)))  # the edge paler than the middle
            out.putpixel((x, y), (v, v, v, int(255 * min(1.0, a * 1.15))))
    return out


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def b(name):
    return f"{NS}:block/{name}"


def model(name, parent, textures):
    write(os.path.join(ASSETS, "models", "block", name + ".json"), {"parent": parent, "textures": textures})


def state(name, variants=None, multipart=None):
    write(os.path.join(ASSETS, "blockstates", name + ".json"), {"multipart": multipart} if multipart else {"variants": variants})


def item(name, model_ref):
    write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": model_ref}})


VARIANTS = {"dreamstone": 2, "dreamstone_bricks": 3, "cracked_dreamstone_bricks": 2, "polished_dreamstone": 1}


def cube(name, texture=None):
    model(name, "minecraft:block/cube_all", {"all": b(texture or name)})
    extra = VARIANTS.get(name, 0)
    for k in range(1, extra + 1):
        model(f"{name}_{k}", "minecraft:block/cube_all", {"all": b(f"{name}_{k}")})
    if extra:
        # Weighted so the plainest look is the commonest.
        choices = [{"model": b(name), "weight": 3}] + [{"model": b(f"{name}_{k}")} for k in range(1, extra + 1)]
        state(name, {"": choices})
    else:
        state(name, {"": {"model": b(name)}})
    item(name, b(name))


def pillar(name, side, top):
    model(name, "minecraft:block/cube_column", {"side": b(side), "end": b(top)})
    model(name + "_horizontal", "minecraft:block/cube_column_horizontal", {"side": b(side), "end": b(top)})
    state(name, {"axis=y": {"model": b(name)}, "axis=z": {"model": b(name + "_horizontal"), "x": 90},
                 "axis=x": {"model": b(name + "_horizontal"), "x": 90, "y": 90}})
    item(name, b(name))


def stairs(name, texture):
    t = {"bottom": b(texture), "top": b(texture), "side": b(texture)}
    model(name, "minecraft:block/stairs", t)
    model(name + "_inner", "minecraft:block/inner_stairs", t)
    model(name + "_outer", "minecraft:block/outer_stairs", t)
    variants = {}
    yaw = {"east": 0, "south": 90, "west": 180, "north": 270}
    for facing, y in yaw.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                m = name + ("" if shape == "straight" else "_" + shape.split("_")[0])
                r = y
                if shape.endswith("left"):
                    r = (y + 270) % 360
                x = 0
                if half == "top":
                    x = 180
                    if shape.endswith("left"):
                        r = y
                    elif shape != "straight":
                        r = (y + 90) % 360
                v = {"model": b(m)}
                if x:
                    v["x"] = x
                if r:
                    v["y"] = r
                if x or r:
                    v["uvlock"] = True
                variants[f"facing={facing},half={half},shape={shape}"] = v
    state(name, variants)
    item(name, b(name))


def slab(name, texture, full):
    t = {"bottom": b(texture), "top": b(texture), "side": b(texture)}
    model(name, "minecraft:block/slab", t)
    model(name + "_top", "minecraft:block/slab_top", t)
    state(name, {"type=bottom": {"model": b(name)}, "type=top": {"model": b(name + "_top")}, "type=double": {"model": b(full)}})
    item(name, b(name))


def wall(name, texture):
    model(name + "_post", "minecraft:block/template_wall_post", {"wall": b(texture)})
    model(name + "_side", "minecraft:block/template_wall_side", {"wall": b(texture)})
    model(name + "_side_tall", "minecraft:block/template_wall_side_tall", {"wall": b(texture)})
    model(name + "_inventory", "minecraft:block/wall_inventory", {"wall": b(texture)})
    parts = [{"when": {"up": "true"}, "apply": {"model": b(name + "_post")}}]
    for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for height, suffix in (("low", "_side"), ("tall", "_side_tall")):
            apply = {"model": b(name + suffix), "uvlock": True}
            if y:
                apply["y"] = y
            parts.append({"when": {side: height}, "apply": apply})
    state(name, multipart=parts)
    item(name, b(name + "_inventory"))


def fence(name, texture):
    model(name + "_post", "minecraft:block/fence_post", {"texture": b(texture)})
    model(name + "_side", "minecraft:block/fence_side", {"texture": b(texture)})
    model(name + "_inventory", "minecraft:block/fence_inventory", {"texture": b(texture)})
    parts = [{"apply": {"model": b(name + "_post")}}]
    for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        apply = {"model": b(name + "_side"), "uvlock": True}
        if y:
            apply["y"] = y
        parts.append({"when": {side: "true"}, "apply": apply})
    state(name, multipart=parts)
    item(name, b(name + "_inventory"))


def cross(name):
    model(name, "minecraft:block/cross", {"cross": b(name)})
    state(name, {"": {"model": b(name)}})
    write(os.path.join(ASSETS, "items", name + ".json"),
          {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})
    write(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": b(name)}})


def decor():
    """The Garden's furnishings: their models and textures are drawn in gen_decor.py."""
    import sys
    import gen_decor
    gen_decor.run(sys.modules[__name__])
    import gen_labyrinth_art  # the labyrinth's furnishings in detail, over gen_decor's
    gen_labyrinth_art.run(sys.modules[__name__])


def reliquary():
    """The heartwood reliquary's model: a box of heartwood boards on iron feet, banded in iron, a lid that overhangs it
    a little, a clasp at the front with its stone; open, the lid swings up on its hinge at the back."""
    wood, top, iron, clasp = b("reliquary_wood"), b("reliquary_top"), b("reliquary_iron"), b("reliquary_clasp")
    tex = {"wood": wood, "top": top, "iron": iron, "clasp": clasp, "particle": wood}

    def box(f, t, faces_tex, rot=None, top_tex=None):
        faces = {d: {"texture": "#" + faces_tex} for d in ("north", "south", "east", "west", "down")}
        faces["up"] = {"texture": "#" + (top_tex or faces_tex)}
        e = {"from": f, "to": t, "faces": faces}
        if rot:
            e["rotation"] = rot
        return e

    base = [box([1.5, 1, 1.5], [14.5, 9, 14.5], "wood")]
    for x0, z0 in ((1, 1), (12.5, 1), (1, 12.5), (12.5, 12.5)):
        base.append(box([x0, 0, z0], [x0 + 2.5, 2, z0 + 2.5], "iron"))  # the feet
    for x0 in (3.5, 11):
        base.append(box([x0, 1, 1.3], [x0 + 1.5, 9, 14.7], "iron"))  # the bands round the box
    base.append(box([6.5, 5.5, 1.1], [9.5, 9, 1.5], "clasp"))  # the clasp's lower half, on the box

    def lid(rot):
        parts = [box([1, 9, 1], [15, 12, 15], "wood", rot, "top"), box([2, 12, 2], [14, 13, 14], "wood", rot, "top")]
        for x0 in (3.5, 11):
            parts.append(box([x0, 9, 0.8], [x0 + 1.5, 12.2, 15.2], "iron", rot))
        parts.append(box([6.5, 8.5, 0.5], [9.5, 12, 1], "clasp", rot))  # the clasp's plate, hanging from the lid
        return parts

    hinge = {"origin": [8, 9, 15], "axis": "x", "angle": 45}
    for name, elements in (("heartwood_reliquary", base + lid(None)), ("heartwood_reliquary_open", base + lid(hinge))):
        write(os.path.join(ASSETS, "models", "block", name + ".json"), {"parent": "minecraft:block/block", "textures": tex,
                                                                       "elements": elements})
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for open_ in ("false", "true"):
            v = {"model": b("heartwood_reliquary" + ("_open" if open_ == "true" else ""))}
            if y:
                v["y"] = y
            variants[f"facing={facing},open={open_}"] = v
    state("heartwood_reliquary", variants)
    item("heartwood_reliquary", b("heartwood_reliquary"))


URNS = ("dreamstone_urn", "dreamstone_urn_small", "dreamstone_amphora", "dreamstone_jar")


def urn_loot(name):
    """An urn's loot: with silk touch, the urn itself; else what it kept (gameplay/urn_contents), or nothing in a
    blast."""
    silk = {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
        {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}
    write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), {"type": "minecraft:block", "pools": [
        {"rolls": 1.0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}], "conditions": [silk]},
        {"rolls": 1.0, "entries": [{"type": "minecraft:loot_table", "value": f"{NS}:gameplay/urn_contents"}],
         "conditions": [{"condition": "minecraft:inverted", "term": silk}, {"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{NS}:blocks/{name}"})


def urn_tables():
    """What an urn kept, mostly rubbish (bones, rotten flesh, string, dust, its own shards, often nothing), now and
    then a little of worth (nuggets of gold and iron, an amethyst shard, an emerald, a bottle of enchanting, very rarely
    a diamond); and what a haunted urn gives up when it is broken at last, which is better."""
    def item(name, weight, lo=1, hi=1):
        e = {"type": "minecraft:item", "name": name, "weight": weight}
        if hi > 1:
            e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
        return e
    junk = [{"type": "minecraft:empty", "weight": 14}, item("minecraft:bone", 10, 1, 2), item("minecraft:rotten_flesh", 8, 1, 2),
            item("minecraft:string", 6, 1, 2), item(f"{NS}:urn_shard", 12, 1, 3), item("minecraft:gunpowder", 4),
            item("minecraft:gray_dye", 4), item("minecraft:bone_meal", 5, 1, 3)]
    worth = [item("minecraft:gold_nugget", 5, 1, 4), item("minecraft:iron_nugget", 5, 1, 4), item("minecraft:amethyst_shard", 2),
             item("minecraft:emerald", 2), item("minecraft:experience_bottle", 2), item("minecraft:diamond", 1)]
    write(os.path.join(DATA, "loot_table", "gameplay", "urn_contents.json"), {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2}, "entries": junk + worth}],
        "random_sequence": f"{NS}:gameplay/urn_contents"})
    write(os.path.join(DATA, "loot_table", "entities", "haunted_urn.json"), {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [item(f"{NS}:urn_shard", 1, 2, 4)]},
        {"rolls": 1, "entries": [item("minecraft:gold_nugget", 1, 3, 8)]},
        {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2}, "entries": [
            item("minecraft:emerald", 6, 1, 2), item("minecraft:amethyst_shard", 5, 1, 3), item("minecraft:experience_bottle", 4, 1, 2),
            item("minecraft:gold_ingot", 3), item("minecraft:diamond", 1), {"type": "minecraft:empty", "weight": 4}]}],
        "random_sequence": f"{NS}:entities/haunted_urn"})


def loot(name, slab_block=False):
    entry = {"type": "minecraft:item", "name": f"{NS}:{name}"}
    pool = {"rolls": 1.0, "bonus_rolls": 0.0, "entries": [entry], "conditions": [{"condition": "minecraft:survives_explosion"}]}
    if slab_block:
        entry["functions"] = [{"function": "minecraft:set_count", "count": 2.0, "add": False, "conditions": [
            {"condition": "minecraft:block_state_property", "block": f"{NS}:{name}", "properties": {"type": "double"}}]},
                              {"function": "minecraft:explosion_decay"}]
        del pool["conditions"]
    if name == "stone_tomb":
        # Eight blocks, one item: only its head-left part drops it.
        pool["conditions"].append({"condition": "minecraft:block_state_property", "block": f"{NS}:{name}",
                                   "properties": {"side": "left", "segment": "0"}})
    if name == "mourning_statue":
        # Two blocks tall, one item: only its lower half drops it.
        pool["conditions"].append({"condition": "minecraft:block_state_property", "block": f"{NS}:{name}", "properties": {"half": "lower"}})
    if name == "dread_fire_lamp":
        # Four blocks tall, one item: only its base drops it.
        pool["conditions"].append({"condition": "minecraft:block_state_property", "block": f"{NS}:{name}", "properties": {"part": "base"}})
    write(os.path.join(DATA, "loot_table", "blocks", name + ".json"),
          {"type": "minecraft:block", "pools": [pool], "random_sequence": f"{NS}:blocks/{name}"})


def tag(path, values):
    write(os.path.join(RES, "data", "minecraft", "tags", "block", path + ".json"), {"values": [f"{NS}:{v}" for v in values]})


def main():
    tex = os.path.join(ASSETS, "textures", "block")
    os.makedirs(tex, exist_ok=True)
    for name, draw in TEXTURES.items():
        draw().save(os.path.join(tex, name + ".png"))

    for name in ("dreamstone", "dreamstone_bricks", "cracked_dreamstone_bricks", "chiseled_dreamstone", "veined_dreamstone",
                 "engraved_dreamstone_bricks", "polished_dreamstone", "veined_dreamstone_bricks", "heartwood_seal",
                 "ashen_soil", "sleepwood_planks", "rotten_sleepwood", "torn_wallpaper"):
        cube(name)
    model("withered_hedge", "minecraft:block/leaves", {"all": b("withered_hedge")})
    # Trimmed hedge: the hedge clipped to a wall twelve pixels thick along its axis, so piers stand proud of it.
    write(os.path.join(ASSETS, "models", "block", "trimmed_hedge.json"), {"parent": "minecraft:block/block", "render_type": "minecraft:cutout_mipped",
        "textures": {"all": b("withered_hedge"), "particle": b("withered_hedge")},
        "elements": [{"from": [0, 0, 2], "to": [16, 16, 14], "faces": {
            "north": {"uv": [0, 0, 16, 16], "texture": "#all", "tintindex": 0}, "south": {"uv": [0, 0, 16, 16], "texture": "#all", "tintindex": 0},
            "east": {"uv": [2, 0, 14, 16], "texture": "#all", "tintindex": 0, "cullface": "east"},
            "west": {"uv": [2, 0, 14, 16], "texture": "#all", "tintindex": 0, "cullface": "west"},
            "up": {"uv": [0, 2, 16, 14], "texture": "#all", "tintindex": 0, "cullface": "up"},
            "down": {"uv": [0, 2, 16, 14], "texture": "#all", "tintindex": 0, "cullface": "down"}}}]})
    state("trimmed_hedge", {"axis=x": {"model": b("trimmed_hedge")}, "axis=z": {"model": b("trimmed_hedge"), "y": 90}})
    item("trimmed_hedge", b("trimmed_hedge"))
    state("withered_hedge", {"": {"model": b("withered_hedge")}})
    item("withered_hedge", b("withered_hedge"))
    model("ashen_moss", "minecraft:block/cube_bottom_top", {"top": b("ashen_moss_top"), "bottom": b("ashen_soil"), "side": b("ashen_moss_side")})
    state("ashen_moss", {"": {"model": b("ashen_moss")}})
    item("ashen_moss", b("ashen_moss"))
    pillar("dreamstone_pillar", "dreamstone_pillar", "dreamstone_pillar_top")
    pillar("sleepwood_log", "sleepwood_log", "sleepwood_log_top")
    stairs("dreamstone_brick_stairs", "dreamstone_bricks")
    slab("dreamstone_brick_slab", "dreamstone_bricks", "dreamstone_bricks")
    wall("dreamstone_brick_wall", "dreamstone_bricks")
    wall("dreamstone_wall", "dreamstone")
    stairs("polished_dreamstone_stairs", "polished_dreamstone")
    slab("polished_dreamstone_slab", "polished_dreamstone", "polished_dreamstone")
    stairs("sleepwood_stairs", "sleepwood_planks")
    slab("sleepwood_slab", "sleepwood_planks", "sleepwood_planks")
    fence("sleepwood_fence", "sleepwood_planks")
    model("dusty_carpet", "minecraft:block/carpet", {"wool": b("dusty_carpet")})
    state("dusty_carpet", {"": {"model": b("dusty_carpet")}})
    item("dusty_carpet", b("dusty_carpet"))
    write(os.path.join(ASSETS, "items", "dread_lantern.json"), {"model": {"type": "minecraft:model", "model": f"{NS}:item/dread_lantern"}})
    write(os.path.join(ASSETS, "models", "item", "dread_lantern.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": b("dread_lantern")}})
    model("wake_crystal", "minecraft:block/cross", {"cross": b("wake_crystal")})
    crystal_states = {}
    for facing, rot in (("up", {}), ("down", {"x": 180}), ("north", {"x": 90}), ("south", {"x": 90, "y": 180}),
                        ("east", {"x": 90, "y": 90}), ("west", {"x": 90, "y": 270})):
        crystal_states[f"facing={facing}"] = dict({"model": b("wake_crystal")}, **rot)
    state("wake_crystal", crystal_states)
    write(os.path.join(ASSETS, "items", "wake_crystal.json"), {"model": {"type": "minecraft:model", "model": f"{NS}:item/wake_crystal"}})
    write(os.path.join(ASSETS, "models", "item", "wake_crystal.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": b("wake_crystal")}})
    for name in ("lucid_bloom", "despair_thorns", "hanging_veins"):
        cross(name)
    reliquary()
    decor()
    os.makedirs(os.path.join(ASSETS, "textures", "vfx"), exist_ok=True)
    tear_particle().save(os.path.join(ASSETS, "textures", "vfx", "tear.png"))
    ash_particle().save(os.path.join(ASSETS, "textures", "vfx", "ash.png"))
    write(os.path.join(tex, "brazier_fire.png.mcmeta"), {"animation": {"frametime": 2, "interpolate": True}})
    # The veined stones: the stone drawn as it is, lit by the world, and over it a layer of only the veins, lit by
    # itself (light_emission), so the veins glow and the stone round them doesn't.
    for name in ("veined_dreamstone", "veined_dreamstone_bricks"):
        full = Image.open(os.path.join(tex, name + ".png")).convert("RGBA")
        veins = Image.new("RGBA", full.size, (0, 0, 0, 0))
        for y in range(full.height):
            for x in range(full.width):
                r, g, bl, a = full.getpixel((x, y))
                if r > 110 and r > g * 2.2 and r > bl * 1.6:  # the red of the veins, not the stone
                    veins.putpixel((x, y), (r, g, bl, 255))
        veins.save(os.path.join(tex, name + "_veins.png"))
        faces = lambda key: {d: {"texture": key, "cullface": d} for d in ("down", "up", "north", "south", "west", "east")}
        write(os.path.join(ASSETS, "models", "block", name + ".json"), {
            "parent": "minecraft:block/block",
            "textures": {"all": b(name), "veins": b(name + "_veins"), "particle": b(name)},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces("#all")},
                         {"from": [0, 0, 0], "to": [16, 16, 16], "light_emission": 15, "faces": faces("#veins")}]})
    # The heart portal: a membrane while it sleeps; awake, nothing of the block shows (its rift is drawn on the client).
    model("heart_portal", "minecraft:block/cube_all", {"all": b("heart_portal")})
    write(os.path.join(ASSETS, "models", "block", "heart_portal_open.json"), {"textures": {"particle": b("heart_portal")}, "elements": []})
    state("heart_portal", {"active=false": {"model": b("heart_portal")}, "active=true": {"model": b("heart_portal_open")}})
    item("heart_portal", b("heart_portal"))
    # Weeping veins: a strand through the block, or its end at the bottom (tip), joining when they're stacked.
    model("weeping_veins", "minecraft:block/cross", {"cross": b("weeping_veins")})
    model("weeping_veins_plant", "minecraft:block/cross", {"cross": b("weeping_veins_plant")})
    state("weeping_veins", {f"tip=true,waterlogged={w}": {"model": b("weeping_veins")} for w in ("false", "true")}
          | {f"tip=false,waterlogged={w}": {"model": b("weeping_veins_plant")} for w in ("false", "true")})
    write(os.path.join(ASSETS, "items", "weeping_veins.json"), {"model": {"type": "minecraft:model", "model": f"{NS}:item/weeping_veins"}})
    write(os.path.join(ASSETS, "models", "item", "weeping_veins.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": b("weeping_veins")}})

    for name in NAMES:
        if name not in ("heartwood_seal", "heart_portal"):  # they drop nothing: no tool breaks them
            if name in URNS:
                urn_loot(name)
            else:
                loot(name, slab_block=name.endswith("_slab"))
    tag("mineable/pickaxe", PICKAXE)
    tag("mineable/axe", AXE)
    tag("mineable/shovel", SHOVEL)
    tag("mineable/hoe", HOE)
    tag("stairs", ["dreamstone_brick_stairs", "sleepwood_stairs", "polished_dreamstone_stairs"])
    tag("slabs", ["dreamstone_brick_slab", "sleepwood_slab", "polished_dreamstone_slab"])
    tag("walls", ["dreamstone_brick_wall", "dreamstone_wall"])
    tag("wooden_fences", ["sleepwood_fence"])
    tag("fences", ["dread_iron_fence"])
    tag("fence_gates", ["dread_iron_gate"])
    tag("logs", ["sleepwood_log"])
    tag("climbable", ["weeping_veins", "sleepwood_ladder"])
    tag("planks", ["sleepwood_planks"])

    lang_path = os.path.join(ASSETS, "lang", "en_us.json")
    with open(lang_path, encoding="utf-8") as f:
        lang = json.load(f)
    for name, title in NAMES.items():
        lang[f"block.{NS}.{name}"] = title
    lang[f"itemGroup.{NS}.dark_between"] = "Why Can't I Sleep?"
    lang[f"item.{NS}.urn_shard"] = "Urn Shard"
    lang[f"item.{NS}.dream_glider"] = "Dream Glider"
    lang[f"item.{NS}.lucid_spool"] = "Lucid Spool"
    lang[f"block.{NS}.dream_bridge"] = "Dream Thread"
    lang[f"entity.{NS}.sleepwalker"] = "Sleepwalker"
    lang[f"entity.{NS}.dread_manta"] = "Dread Manta"
    lang[f"entity.{NS}.dream_eater"] = "Dream Eater"
    lang[f"entity.{NS}.somnimoth"] = "Somnimoth"
    lang[f"entity.{NS}.faceless"] = "Faceless"
    lang[f"entity.{NS}.night_mare"] = "Night Mare"
    lang[f"entity.{NS}.hollow_hound"] = "Hollow Hound"
    lang[f"entity.{NS}.lullaby"] = "Lullaby"
    lang[f"entity.{NS}.gazer"] = "Gazer"
    lang[f"entity.{NS}.tick_tock"] = "Tick-Tock"
    lang[f"entity.{NS}.stitchling"] = "Stitchling"
    lang[f"entity.{NS}.gloom_jelly"] = "Gloom Jelly"
    lang[f"entity.{NS}.dream_hare"] = "Dream Hare"
    lang[f"entity.{NS}.grinning_clown"] = "Grinning Clown"
    lang[f"entity.{NS}.mime_clown"] = "Mime"
    lang[f"entity.{NS}.little_red"] = "Little Red Riding Hood"
    lang[f"message.{NS}.wolf_granny"] = "\"Come closer, dear... the better to see you with.\""
    # Boss combat (BossCombat): parrying, a broken guard, a critical strike; the Gardener's heart laid open.
    lang[f"message.{NS}.parry"] = "Parried!"
    lang[f"message.{NS}.stagger"] = "%s staggers!"
    lang[f"message.{NS}.critical"] = "Critical strike!"
    lang[f"message.{NS}.gardener_exposed"] = "Its ribcage opens. Strike the heart!"
    lang[f"message.{NS}.red_gift"] = "Little Red proudly hands you... something from her basket."
    lang[f"message.{NS}.red_gift_good"] = "Little Red gives you something lovely from her basket!"
    lang[f"message.{NS}.red_turns"] = "Her hood falls back. That is not a little girl's face."
    lang[f"message.{NS}.red_guilt"] = "What have you done?"
    lang[f"entity.{NS}.jack_in_the_box"] = "Jack-in-the-Box"
    lang[f"entity.{NS}.balloon_clown"] = "Balloon Clown"
    lang[f"entity.{NS}.funhouse_clown"] = "Funhouse Clown"
    lang[f"message.{NS}.mime_box"] = "Something you can't see closes in around you..."
    lang[f"message.{NS}.balloon_tied"] = "A balloon is tied to you!"
    lang[f"item.{NS}.swap_card"] = "Swap Card"
    lang[f"item.{NS}.swap_card.desc"] = "Use: change places with the creature you look at"
    lang[f"entity.{NS}.closet_boogeyman"] = "Closet Boogeyman"
    lang[f"entity.{NS}.big_bad_wolf"] = "Big Bad Wolf"
    lang[f"entity.{NS}.sandman"] = "Sandman"
    lang[f"entity.{NS}.tooth_taker"] = "Tooth Taker"
    lang[f"entity.{NS}.broodmother"] = "Broodmother"
    lang[f"entity.{NS}.broodling"] = "Broodling"
    lang[f"entity.{NS}.void_eel"] = "Void Eel"
    lang[f"entity.{NS}.hive_thing"] = "Hive-Thing"
    lang[f"entity.{NS}.crawlspace"] = "Crawlspace"
    lang[f"item.{NS}.closet_key"] = "Closet Key"
    lang[f"item.{NS}.closet_key.desc"] = "Use: hide; Invisibility, and pursuers lose you"
    lang[f"item.{NS}.huff_and_puff"] = "Huff and Puff"
    lang[f"item.{NS}.huff_and_puff.desc"] = "Use: blow away every creature around you"
    lang[f"item.{NS}.dream_sand"] = "Dream Sand"
    lang[f"item.{NS}.dream_sand.desc"] = "Use: a moment's sleep; Regeneration II, and a lift of mood"
    lang[f"item.{NS}.fairy_dust"] = "Fairy Dust"
    lang[f"item.{NS}.fairy_dust.desc"] = "Use: float a few blocks up, and land softly"
    lang[f"item.{NS}.broodsilk"] = "Broodsilk"
    lang[f"item.{NS}.broodsilk.desc"] = "Use: swing on a line of silk to the block you look at"
    lang[f"item.{NS}.void_scale"] = "Void Scale"
    lang[f"item.{NS}.void_scale.desc"] = "Use: return to where you last stood on solid ground"
    lang[f"item.{NS}.swarm_jar"] = "Swarm Jar"
    lang[f"item.{NS}.swarm_jar.desc"] = "Use: set a swarm on the hostile creatures around you"
    lang[f"item.{NS}.shrinking_skin"] = "Shrinking Skin"
    lang[f"item.{NS}.shrinking_skin.desc"] = "Use: shrink to half your size for a minute"
    lang[f"message.{NS}.sandman_sleep"] = "Your eyes close..."
    lang[f"message.{NS}.tooth_taker_stole"] = "Something took your %s!"
    lang[f"item.{NS}.mares_ember"] = "Mare's Ember"
    lang[f"item.{NS}.hollow_fang"] = "Hollow Fang"
    lang[f"item.{NS}.lullaby_box"] = "Lullaby Box"
    lang[f"item.{NS}.gazers_lens"] = "Gazer's Lens"
    lang[f"item.{NS}.stopwatch"] = "Stopwatch"
    lang[f"item.{NS}.living_thread"] = "Living Thread"
    lang[f"item.{NS}.gloom_gel"] = "Gloom Gel"
    lang[f"item.{NS}.hares_foot"] = "Hare's Foot"
    lang[f"item.{NS}.mares_ember.desc"] = "Use: Speed II for 45 seconds"
    lang[f"item.{NS}.hollow_fang.desc"] = "Use: reveals hostile creatures within 32 blocks for 15 seconds"
    lang[f"item.{NS}.lullaby_box.desc"] = "Use: restores lucidity and mood, yours and those nearby"
    lang[f"item.{NS}.gazers_lens.desc"] = "Use: Night Vision for 2 minutes"
    lang[f"item.{NS}.stopwatch.desc"] = "Use: nearly stops hostile creatures within 10 blocks"
    lang[f"item.{NS}.living_thread.desc"] = "Use: mends a quarter of the item in your other hand"
    lang[f"item.{NS}.gloom_gel.desc"] = "Use: Slow Falling for 60 seconds"
    lang[f"item.{NS}.hares_foot.desc"] = "Use: Jump Boost II for 60 seconds, and Luck"
    lang[f"entity.{NS}.haunted_urn"] = "Haunted Urn"
    lang[f"entity.{NS}.urn_shard"] = "Urn Shard"
    urn_tables()
    write(lang_path, lang)
    print("wrote", len(NAMES), "blocks")


if __name__ == "__main__":
    main()
