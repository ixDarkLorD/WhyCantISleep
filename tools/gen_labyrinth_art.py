"""The labyrinth's own furnishings, drawn in detail: 32-pixel textures shaded as if lit from the upper left (highlights
on what stands out, shadow in what is cut in, gradients and wear), and models built round where they should be round
(each round part doubled at 45 degrees, an octagon), tapering in fine steps. Run by gen_between_blocks.py (decor),
after gen_decor.py, whose textures and models of the same names these replace:

    dark_iron                 hammered black iron (candelabra, fence, gate)
    candle_wax                old wax, drips frozen down it
    statue_stone / robe /     the mourning statue's weathered stone, its robe's folds, its hood, its plinth with a band
    hood / plinth / shadow    of carved runes, and the dark inside the hood
    font_stone, basin_face    the weeping basin's stone and the weeping face carved on its stem
    dreamstone_pier_*         the pier's moulded base, carved capital and ends
"""
import json
import math
import os
import random

from PIL import Image

N = 32  # texture size


# ---- Shading helpers ----
def clamp(v, lo=0.0, hi=1.0):
    return lo if v < lo else hi if v > hi else v


def mix(a, b, t):
    t = clamp(t)
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(c, f):
    return tuple(int(clamp(v * f, 0, 255)) for v in c)


class Noise:
    """Smooth value noise, tiling over the texture."""

    def __init__(self, seed, cells=8):
        rng = random.Random(seed)
        self.n = cells
        self.v = [[rng.random() for _ in range(cells)] for _ in range(cells)]

    def at(self, x, y):
        n = self.n
        fx, fy = x / N * n, y / N * n
        ix, iy = int(math.floor(fx)), int(math.floor(fy))
        tx, ty = fx - ix, fy - iy
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        g = lambda i, j: self.v[j % n][i % n]
        a = g(ix, iy) + (g(ix + 1, iy) - g(ix, iy)) * tx
        b = g(ix, iy + 1) + (g(ix + 1, iy + 1) - g(ix, iy + 1)) * tx
        return a + (b - a) * ty


def fbm(seed):
    layers = [Noise(seed, 4), Noise(seed + 1, 8), Noise(seed + 2, 16)]
    return lambda x, y: layers[0].at(x, y) * 0.55 + layers[1].at(x, y) * 0.3 + layers[2].at(x, y) * 0.15


def canvas():
    return Image.new("RGBA", (N, N), (0, 0, 0, 0))


def paint(img, fn):
    for y in range(N):
        for x in range(N):
            c = fn(x, y)
            if c is not None:
                img.putpixel((x, y), tuple(c[:3]) + ((c[3],) if len(c) > 3 else (255,)))
    return img


def carve(img, pixels, deep, lit, glow=None):
    """Cuts a shape into a texture: its pixels darkened, a shadow along their top edges and a light along their bottom
    edges (light from above), or filled with a glow."""
    s = set(pixels)
    for x, y in s:
        base = img.getpixel((x, y))[:3]
        img.putpixel((x, y), (glow if glow else shade(base, deep)) + (255,))
        if (x, y - 1) not in s and 0 <= y - 1:
            img.putpixel((x, y - 1), shade(img.getpixel((x, y - 1))[:3], deep * 0.9) + (255,))
        if (x, y + 1) not in s and y + 1 < N:
            img.putpixel((x, y + 1), shade(img.getpixel((x, y + 1))[:3], lit) + (255,))


# ---- Palette (the Dark Between's: LurkerPalette) ----
IRON = [(14, 13, 18), (28, 27, 34), (46, 45, 56), (78, 78, 92), (124, 124, 140)]
STONE = [(54, 48, 66), (74, 67, 88), (96, 88, 112), (122, 114, 138), (150, 142, 166)]
PALE = [(78, 72, 90), (110, 104, 122), (140, 134, 152), (168, 162, 178), (196, 190, 204)]
CRIMSON, RED, HOT = (138, 10, 36), (255, 45, 70), (255, 170, 160)


# ---- The pixel-art pass ----
BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


def ramp(dark, light, steps=6):
    """A pixel artist's ramp from dark to light: its shadows shifted cool and violet, its lights warm."""
    out = []
    for i in range(steps):
        t = i / (steps - 1)
        c = mix(dark, light, t)
        shift = (t - 0.5) * 2  # -1 in shadow, 1 in light
        out.append((int(clamp(c[0] + 10 * shift, 0, 255)), int(clamp(c[1] + 4 * shift, 0, 255)), int(clamp(c[2] - 8 * shift, 0, 255))))
    return out


RAMPS = {"stone": ramp((22, 17, 34), (196, 188, 212), 5), "pale": ramp((40, 33, 54), (228, 222, 234), 5),
         "iron": ramp((8, 7, 14), (150, 150, 170), 5), "wax": ramp((110, 86, 62), (252, 246, 230), 4),
         "red": [(70, 4, 20), (124, 8, 32), (186, 20, 48), (240, 50, 74), (255, 140, 140)]}


def pixel_art(img, material, dither=0.0):
    """Brings a texture onto its material's ramp (and its reds onto the red ramp), each pixel to the nearest step but
    nudged by a 4x4 ordered dither where it falls between two, so tones blend in a deliberate checker instead of a
    smear."""
    out = img.copy()
    lum = lambda c: c[0] * 0.3 + c[1] * 0.59 + c[2] * 0.11
    # Each texture stretched over its whole ramp: its darkest pixel the deepest shadow, its brightest the highlight.
    plain = [lum(c[:3]) for c in img.getdata() if c[3] and c[0] - max(c[1], c[2]) <= 60]
    lo_l, hi_l = (min(plain), max(plain)) if plain else (0, 255)
    reds = [lum(c[:3]) for c in img.getdata() if c[3] and c[0] - max(c[1], c[2]) > 60]
    lo_r, hi_r = (min(reds), max(reds)) if reds else (0, 255)
    for y in range(img.height):
        for x in range(img.width):
            r, g, b_, a = img.getpixel((x, y))
            if a == 0:
                continue
            red = r - max(g, b_) > 60
            steps = RAMPS["red" if red else material]
            lo, hi = (lo_r, hi_r) if red else (lo_l, hi_l)
            t = clamp((lum((r, g, b_)) - lo) / max(1.0, hi - lo))
            pos = t * (len(steps) - 1) + (BAYER[y % 4][x % 4] / 16.0 - 0.47) * dither
            out.putpixel((x, y), steps[int(clamp(round(pos), 0, len(steps) - 1))] + (a,))
    return out


MATERIAL = {"dark_iron": "iron", "iron_round": "iron", "candle_wax": "wax", "statue_stone": "pale", "statue_robe": "pale",
            "statue_hood": "pale", "statue_face": "pale", "statue_chest": "pale"}


# ---- Textures ----
def dark_iron():
    """Hammered black iron: a soft sheen running down it, dents where the hammer struck (lit on their far side,
    shadowed on their near), a cold glint along its edges, rust gone red in its pits."""
    f, rust = fbm(301), fbm(305)
    rng = random.Random(302)
    dents = [(rng.uniform(0, N), rng.uniform(0, N), rng.uniform(1.5, 3.2)) for _ in range(14)]

    def px(x, y):
        v = 0.35 + 0.45 * f(x, y) + 0.12 * math.sin((x + y * 0.3) * 0.35)
        for dx, dy, r in dents:
            ddx, ddy = (x - dx + N / 2) % N - N / 2, (y - dy + N / 2) % N - N / 2
            d = math.hypot(ddx, ddy)
            if d < r:
                v += 0.28 * (ddx + ddy) / (2 * r) * (1 - d / r)  # a dent: light where it faces up-left
        c = mix(IRON[0], IRON[3], v)
        if rust(x * 1.7, y * 1.7) > 0.7:
            c = mix(c, (92, 30, 26), (rust(x * 1.7, y * 1.7) - 0.7) * 2.5)
        if (x * 7 + y * 13) % 97 == 0:
            c = IRON[4]  # a glint
        return c
    return paint(canvas(), px)


def candle_wax():
    """Old candle wax, bone-white gone yellow: lit on its left, shadowed on its right, drips frozen down it each with
    a glossy highlight, darker where it has run thick at the bottom."""
    rng = random.Random(311)
    drips = [(rng.randrange(N), rng.randrange(6, 22)) for _ in range(6)]

    def px(x, y):
        round_ = math.cos((x / N - 0.35) * math.pi)  # a cylinder lit from the left
        c = mix((150, 132, 108), (232, 220, 198), 0.35 + 0.5 * round_)
        c = mix(c, (130, 112, 90), max(0.0, (y - 22) / 10))
        for dx, length in drips:
            if abs(x - dx) <= 1 and y <= length:
                c = mix(c, (244, 236, 218), 0.6 if x == dx - 1 else 0.3)
                if y == length:
                    c = (250, 244, 228)
        return c
    img = paint(canvas(), px)
    for x in (15, 16):
        img.putpixel((x, 0), (26, 20, 18, 255))  # the wick
    return img


def statue_stone():
    """Weathered statue stone: pale grey-violet, soft mottling, grime washed down it in streaks, pits."""
    f, streak = fbm(321), fbm(322)
    rng = random.Random(323)
    pits = {(rng.randrange(N), rng.randrange(N)) for _ in range(9)}

    def px(x, y):
        v = 0.45 + 0.4 * f(x, y)
        v -= 0.25 * max(0.0, streak(x * 3.0, y * 0.35) - 0.55)  # grime washed down
        c = mix(PALE[0], PALE[4], v)
        if (x, y) in pits:
            c = shade(c, 0.6)
        if (x - 1, y - 1) in pits:
            c = shade(c, 1.15)
        return c
    return paint(canvas(), px)


def statue_robe():
    """A robe in stone: deep rounded folds falling down it, each lit on its left flank and shadowed in its crease,
    the folds swaying a little as they fall, grime in the creases; a red tear-stain run down it, glossy."""
    f, sway = fbm(331), fbm(332)
    def px(x, y):
        u = (x + 2.5 * (sway(x, y * 0.5) - 0.5)) / 8.0 * math.tau
        fold = math.sin(u)          # -1 in a crease, 1 on a ridge
        slope = math.cos(u)         # which way the fold's side faces: +1 to the light (left)
        v = 0.45 + 0.28 * fold + 0.18 * slope + 0.12 * (f(x, y) - 0.5)
        c = mix(PALE[0], PALE[4], v)
        if fold < -0.85:
            c = shade(c, 0.7)  # deep in the crease
        if 18 <= x <= 20 and 4 <= y <= 27:
            t = (y - 4) / 23
            stain = mix(RED, CRIMSON, t)
            c = mix(c, stain, 0.85 - 0.3 * abs(x - 19))
            if x == 18 and y % 5 == 0:
                c = mix(stain, (255, 200, 200), 0.6)  # its gloss
        return c
    return paint(canvas(), px)


def statue_hood():
    """The hood's cloth in stone: broad soft folds, a fine weave of weathering."""
    f = fbm(341)

    def px(x, y):
        u = x / 11.0 * math.tau
        v = 0.5 + 0.22 * math.sin(u) + 0.14 * math.cos(u) + 0.14 * (f(x, y) - 0.5) - 0.12 * (y / N)
        return mix(PALE[0], PALE[4], v)
    return paint(canvas(), px)


def statue_plinth():
    """The plinth: polished dark stone with a lit top edge and a shadowed foot, a band of carved runes round it, cut in
    with their shadows, and a crack."""
    f = fbm(351)
    rng = random.Random(352)

    def px(x, y):
        v = 0.4 + 0.3 * f(x, y)
        c = mix(STONE[0], STONE[3], v)
        if y < 2:
            c = mix(STONE[3], STONE[4], 0.6 - y * 0.3)
        elif y > 29:
            c = shade(c, 0.55)
        elif 10 <= y <= 21:
            c = shade(c, 0.85)  # the band, sunk a little
        if y in (9, 22):
            c = STONE[4] if y == 22 else STONE[0]
        return c
    img = paint(canvas(), px)
    runes = []
    for k in range(4):
        ox = 1 + k * 8
        shape = rng.choice([[(1, 0), (1, 1), (1, 2), (1, 3), (1, 4), (0, 1), (2, 3)], [(0, 0), (1, 1), (2, 2), (1, 3), (0, 4), (2, 0)],
                            [(0, 0), (0, 1), (0, 2), (0, 3), (0, 4), (1, 2), (2, 2), (2, 0), (2, 4)], [(1, 0), (0, 1), (2, 1), (1, 2), (1, 3), (1, 4)]])
        runes += [(ox + 2 + dx, 13 + dy) for dx, dy in shape]
    carve(img, runes, 0.45, 1.3)
    carve(img, [(23, y) for y in range(23, 30)] + [(24, 26), (25, 27)], 0.5, 1.2)  # a crack
    return img


def statue_shadow():
    """The dark inside the hood: near black, a faint gradient to the brim."""
    return paint(canvas(), lambda x, y: mix((6, 4, 10), (30, 22, 36), (1 - y / N) * 0.6))


def rounded(palette, seed, spec=0.0, grain=0.08):
    """A texture for a part to wear whole: lit like a cylinder from the upper left (a highlight at a third of the way
    across, falling into shadow to the right), a lit edge along its top, its foot in shadow, a little grain."""
    f = fbm(seed)

    def px(x, y):
        u = x / (N - 1)
        v = 0.08 + 0.82 * max(0.0, math.cos((u - 0.3) * math.pi * 0.95))   # round, lit from the left
        v += spec * math.exp(-((u - 0.24) / 0.05) ** 2)                        # a glint
        if x < 2:
            v = max(v, 0.82)                                                    # rim light down its left edge
        if x > 29:
            v = 0.02                                                            # its dark right edge
        if y < 2:
            v += 0.3 - y * 0.12                                                 # the lit top edge
        v -= 0.4 * max(0.0, (y - 22) / 9.0)                                     # the shadowed foot
        if y > 30:
            v = min(v, 0.06)
        v += grain * 0.4 * (f(x, y) - 0.5)
        return mix(palette[0], palette[4], v)
    return paint(canvas(), px)


def iron_round():
    """Black iron worn round: lit along its left, a cold glint, deep shadow on its right and at its foot."""
    return rounded(IRON, 401, spec=0.35, grain=0.12)


def font_round():
    """The basin's dreamstone worn round: softly lit along its left, shadow gathering on its right and at its foot."""
    return rounded(STONE, 402, spec=0.08, grain=0.1)


def font_rim():
    """The basin's rim: its top edge bright where it rolls over, shadow under the roll."""
    f = fbm(403)

    def px(x, y):
        v = 0.85 - 0.6 * (y / (N - 1)) ** 0.7 + 0.12 * math.cos((x / (N - 1) - 0.3) * math.pi) + 0.08 * (f(x, y) - 0.5)
        if y < 3:
            v += 0.12
        return mix(STONE[0], STONE[4], v)
    return paint(canvas(), px)


def dreamstone_pillar():
    """A fluted dreamstone column, lit from the upper left: each flute a dark crease in the shadow of the fillet to its
    left, a hollow brightening across to its lit far wall, then the fillet's bright edge; at the drum's head a band with
    a lit top and a shadow cast under it, at its foot a lit ledge over a dark joint. A crack down one flute, dark with a
    lit lip; a few chips in the fillets; mottling kept faint so the forms read."""
    f = fbm(411)
    rng = random.Random(413)
    chips = {(rng.randrange(N), rng.randrange(4, 27)) for _ in range(10)}
    crack = set()
    x, y = 13, 5
    while y < 21:
        crack.add((x, y))
        y += 1
        x += rng.choice((-1, 1)) if rng.random() < 0.6 else 0
        x = max(10, min(14, x))
    across = [1.0, 0.08, 0.18, 0.3, 0.42, 0.55, 0.68, 0.5]     # fillet, crease, the hollow brightening, its far wall's edge
    head = [0.95, 0.72, 0.08, 0.22]                           # band: lit top, its face, the shadow it casts, dim under
    foot = [0.88, 0.62, 0.38, 0.05]                           # the ledge's lit top, its face, its foot, the dark joint

    def px(x, y):
        if y < 4:
            v = head[y]
        elif y >= 28:
            v = foot[y - 28]
        else:
            v = across[x % 8]
            if y in (4, 5):
                v *= 0.75                                     # in the band's shadow
        v += 0.05 * (f(x, y) - 0.5)
        c = mix(STONE[0], STONE[4], v)
        if (x, y) in chips and x % 8 in (0, 7):
            c = shade(c, 0.45)
        if (x, y) in crack:
            c = mix(STONE[0], STONE[1], 0.2)
        elif (x - 1, y) in crack:
            c = shade(c, 1.3)
        return c
    return paint(canvas(), px)


def dreamstone_pillar_top():
    """The column's end: a bevelled square, the drum's bed cut in rings round a worn boss, mottled and grained."""
    f = fbm(421)
    rng = random.Random(422)

    def px(x, y):
        r = math.hypot(x - 15.5, y - 15.5)
        v = 0.55 + 0.12 * (f(x, y) - 0.5) + (rng.random() - 0.5) * 0.05
        if x < 2 or y < 2:
            v += 0.22
        elif x > 29 or y > 29:
            v -= 0.25
        if 12.5 < r < 13.6 or 8.0 < r < 8.9:
            v -= 0.26 if (x + y) > 31 else 0.18                  # rings cut in
        elif 13.6 <= r < 14.2 or 8.9 <= r < 9.4:
            v += 0.12                                            # their lit outer lips
        elif r < 4.5:
            v += 0.16 * (1 - r / 4.5) - 0.08 * ((x - 15.5 + y - 15.5) / 9)  # the boss, lit on its upper left
        return mix(STONE[0], STONE[4], v)
    return paint(canvas(), px)


def iron_detail(img, x0, cols, rng):
    """Hammer dents: a dark texel with a lit one over its left (a lit lip), now and then down the strip."""
    if cols < 4:
        return
    for _ in range(cols // 3 + 2):
        x, y = x0 + rng.randrange(1, cols - 1), rng.randrange(3, 47)
        img.putpixel((x, y), IRON_BANDS[0] + (255,))
        img.putpixel((x - 1, y - 1), IRON_BANDS[4] + (255,))


def stone_detail(img, x0, cols, rng):
    """Pits (dark with a lit lip under them, as light falls from above) and, on wide strips, a hairline crack."""
    if cols < 4:
        return
    for _ in range(cols // 2 + 2):
        x, y = x0 + rng.randrange(1, cols - 1), rng.randrange(3, 47)
        img.putpixel((x, y), STONE_BANDS[1] + (255,))
        if y + 1 < 48:
            img.putpixel((x, y + 1), STONE_BANDS[4] + (255,))
    if cols >= 12:
        x, y = x0 + rng.randrange(3, cols - 3), 3
        while y < 20:
            img.putpixel((x, y), STONE_BANDS[0] + (255,))
            img.putpixel((x + 1, y), STONE_BANDS[4] + (255,))
            y += 1
            x = max(x0 + 1, min(x0 + cols - 3, x + rng.choice((-1, 0, 0, 1))))


def wax_detail(img, x0, cols, rng):
    """Drips: from the melted pool at the top, runs of brighter wax down the candle, each ending in a bead, a shadow
    texel on its right."""
    for _ in range(max(1, cols // 2)):
        x = x0 + rng.randrange(0, max(1, cols - 1))
        length = rng.randrange(2, 7)
        for y in range(2, 2 + length):
            img.putpixel((x, y), WAX_BANDS[5] + (255,))
            if x + 1 < x0 + cols:
                img.putpixel((x + 1, y), WAX_BANDS[2] + (255,))
        img.putpixel((x, 2 + length), WAX_BANDS[4] + (255,))


def candelabra_iron():
    return soft_banded_texture(SOFT_IRON, 601, soft_iron_detail, speckle=5)


def candelabra_wax():
    return soft_banded_texture(SOFT_WAX, 602, soft_wax_detail, speckle=9)


# ---- The candelabra in Minecraft's own manner: a few close shades, not stripes ----
# As the game's iron, lanterns and candles are drawn: each strip a soft lit edge on its left and a soft shadow on its
# right over a body of two or three near shades in a speckled grain, a lit row along its top; tops lit along their
# north and west edges; undersides plain and dark. Six shades each, close together.
SOFT_IRON = [(20, 19, 26), (32, 31, 40), (44, 43, 54), (58, 57, 70), (76, 75, 90), (100, 99, 116)]
SOFT_WAX = [(168, 146, 112), (190, 170, 138), (210, 194, 164), (226, 214, 188), (238, 230, 208), (248, 243, 226)]


def soft_level(cols, i, y):
    """The shade (0..5) of a strip's texel, each on a palette shade exactly: its body, a soft lit edge, a soft
    shadowed one, a lit top row."""
    if cols <= 2:
        level = 3 if i == 0 else 2
    elif i == 0:
        level = 3
    elif i == cols - 1:
        level = 1
    else:
        level = 2
    return level + (1 if y == 0 else 0)


def soft_banded_texture(pal, seed, detail, speckle=6):
    """The candelabra's textures, laid out as banded_texture's (a strip for every width, a top tile and an underside
    tile), in the soft manner: close shades, no outlines or glints, and a speckle as Minecraft's own textures have:
    about one texel in `speckle` a shade lighter or darker than its neighbours."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))

    def put(x, y, level):
        h = ((x * 73856093) ^ (y * 19349663) ^ seed) % (speckle * 2)
        level += 1 if h == 0 else -1 if h == 1 else 0
        img.putpixel((x, y), pal[max(0, min(5, level))] + (255,))
    for w, x0 in BAND_START.items():
        cols = w * SCALE
        for i in range(cols):
            for y in range(48):
                put(x0 + i, y, soft_level(cols, i, y))
        if detail:
            detail(img, pal, x0, cols, rng)
    for (tx, ty), lit in ((TOP_TILE, True), (UNDER_TILE, False)):
        for y in range(24):
            for x in range(24):
                put(tx + x, ty + y, (4 if (x < 1 or y < 1) else 3) if lit else 1)
    return img


def soft_iron_detail(img, pal, x0, cols, rng):
    """A few hammer dents (a dark texel, its lip lit over its left) and a rust speck now and then."""
    if cols < 4:
        return
    for _ in range(cols // 4 + 1):
        x, y = x0 + rng.randrange(1, cols - 1), rng.randrange(3, 47)
        img.putpixel((x, y), pal[1] + (255,))
        img.putpixel((x - 1, y - 1), pal[4] + (255,))
    for _ in range(cols // 4):
        x, y = x0 + rng.randrange(1, cols - 1), rng.randrange(3, 47)
        img.putpixel((x, y), (74, 40, 36, 255))


def soft_wax_detail(img, pal, x0, cols, rng):
    """Soft drips from the melted top: a run a shade lighter, a shade of shadow on its right, a bead at its end."""
    for _ in range(max(1, cols // 3)):
        x = x0 + rng.randrange(0, max(1, cols - 1))
        length = rng.randrange(2, 6)
        for y in range(1, 1 + length):
            img.putpixel((x, y), pal[4] + (255,))
            if x + 1 < x0 + cols:
                img.putpixel((x + 1, y), pal[2] + (255,))
        img.putpixel((x, 1 + length), pal[5] + (255,))


def basin_stone():
    return banded_texture(STONE_BANDS, 603, stone_detail)


def basin_rim():
    return banded_texture(RIM_BANDS, 605, stone_detail)


# ---- The basin's stone, at 32 (two texels to a model pixel, no uv given), in the labyrinth's own colours ----
def stone_ramp():
    """Six shades taken from the labyrinth's polished dreamstone, dark to light, stretched a little past its own
    darkest and lightest so carving can sink and edges can catch the light."""
    img = Image.open(os.path.join(TEX_DIR, "polished_dreamstone.png")).convert("RGB")
    cols = sorted(set(img.getdata()), key=lambda c: c[0] * 0.3 + c[1] * 0.59 + c[2] * 0.11)
    lo, hi = cols[0], cols[-1]
    deep = shade(lo, 0.55)
    bright = tuple(min(255, int(v * 1.35 + 12)) for v in hi)
    return [mix(deep, bright, t) for t in (0.0, 0.18, 0.36, 0.55, 0.76, 1.0)]


def soften(img):
    """Brings a basin texture to the stem's soft pattern: every stone shade folded into the band between the ramp's
    second and fifth shades (so joints, bevels and carving stay, but gently), reds left as they are."""
    R = stone_ramp()
    lum = lambda c: c[0] * 0.3 + c[1] * 0.59 + c[2] * 0.11
    levels = [lum(c) for c in R]
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            c = img.getpixel((x, y))
            if c[3] == 0 or c[0] - max(c[1], c[2]) > 50:
                continue
            l = lum(c[:3])
            i = 0
            while i < len(levels) - 2 and l > levels[i + 1]:
                i += 1
            level = i + clamp((l - levels[i]) / max(1.0, levels[i + 1] - levels[i]))
            level = 1.2 + level / 5.0 * 2.4
            lo = int(level)
            out.putpixel((x, y), mix(R[lo], R[lo + 1], level - lo) + (c[3],))
    return out


def basin_canvas(fn):
    img = Image.new("RGBA", (32, 32))
    for y in range(32):
        for x in range(32):
            img.putpixel((x, y), tuple(fn(x, y)) + (255,))
    return img


def basin_polished():
    """Dressed polished dreamstone: blocks of 16 texels, each bevelled (its top and left edges catching the light, its
    bottom and right edges in shadow), softly mottled, a corner chipped here and there, a hairline crack."""
    R = stone_ramp()
    f = fbm(701)
    rng = random.Random(702)
    chips = {(rng.randrange(32), rng.randrange(32)) for _ in range(6)}

    def px(x, y):
        u, v = x % 16, y % 16
        if u == 0 or v == 0:
            k = 5 if (u == 0 and v == 0) else 4
        elif u == 1 or v == 1:
            k = 3
        elif u == 15 or v == 15:
            k = 0
        elif u == 14 or v == 14:
            k = 1
        else:
            m = f(x, y)
            k = 1 if m < 0.32 else 3 if m > 0.7 else 2
        if (x, y) in chips:
            k = 0
        if (x - 1, y - 1) in chips:
            k = 4
        if x == 21 and 3 < y < 11 or x == 22 and 10 <= y < 14:
            k = 0                                   # a hairline crack
        return R[k]
    return basin_canvas(px)


def basin_bricks():
    """Dreamstone bricks: staggered, eight texels high, each lit along its top and left, shadowed along its bottom and
    right, sunk in dark mortar; one cracked, one with a thin red vein through it."""
    R = stone_ramp()
    f = fbm(711)

    def px(x, y):
        row = y // 8
        bx = (x + (8 if row % 2 else 0)) % 16
        v = y % 8
        if v == 7 or bx == 15:
            return R[0]                             # mortar
        if v == 0 or bx == 0:
            return R[4]
        if v == 6 or bx == 14:
            return R[1]
        m = f(x, y)
        k = 2 if m < 0.6 else 3
        if (row, (x + (8 if row % 2 else 0)) // 16) == (1, 1) and 2 <= v <= 5 and bx in (6, 7, 8) and (bx - 6) == (v - 2) // 1 % 3:
            return R[0]                             # a crack
        if row == 2 and bx == 9 and 1 <= v <= 5:
            return (176, 18, 46)                    # a red vein
        if row == 2 and bx == 10 and v == 3:
            return (240, 50, 74)
        return R[k]
    return basin_canvas(px)


def basin_rim():
    """The rim: a bright rolled lip over a carved band of sunk diamonds, each in shadow on its upper inner side and lit
    along its lower edge."""
    R = stone_ramp()

    def px(x, y):
        if y % 16 < 2:
            return R[5] if y % 16 == 0 else R[4]
        if y % 16 == 15:
            return R[0]
        cx = (x // 8) * 8 + 4
        cy = (y // 16) * 16 + 8
        d = abs(x + 0.5 - cx) + abs(y + 0.5 - cy)
        if d < 3.2:
            return R[0] if (y + 0.5) < cy else R[1]
        if 3.2 <= d < 4.2:
            return R[4] if (y + 0.5) > cy else R[1]
        return R[3]
    return basin_canvas(px)


def basin_pillar(dim=0.0, outline=0.0, seed=731):
    """The stem's masonry at 32, natural rather than ruled: small dreamstone bricks (nine texels by five) laid in
    staggered courses in dark mortar, each lit along its top and left, shadowed along its bottom and right, every brick
    a little lighter or darker than its neighbours and mottled within, corners chipped here and there (a dark notch
    with a lit edge under it) and a crack across a brick or two."""
    R = stone_ramp()
    f = fbm(seed)
    rng = random.Random(seed + 1)
    tone = {}
    chips = set()
    cracks = set()
    for row in range(6):
        for col in range(5):
            tone[(row, col)] = rng.choice((-1, 0, 0, 0, 1))
            if rng.random() < 0.3:
                chips.add((row, col, rng.choice(("tl", "tr", "bl", "br"))))
            if rng.random() < 0.08:
                cracks.add((row, col))

    def px(x, y):
        # Soft: everything kept between the ramp's second and fifth shades, the mortar only a shade under the brick,
        # the edges a half step lit or shaded, neighbouring bricks a half step apart, the wear faint.
        row = y // 6
        off = 5 if row % 2 else 0
        bx = (x + off) % 10
        col = ((x + off) // 10) % 5
        v = y % 6
        if v == 5 or bx == 9:
            mortar = 1.35 - dim - outline * 1.1                  # mortar: shallow, or deeper when outlined
            mortar = max(0.0, mortar)
            return mix(R[int(mortar)], R[min(5, int(mortar) + 1)], mortar - int(mortar))
        t = tone[(row % 6, col)] * 0.35
        m = f(x, y)
        level = 2.4 + (m - 0.5) * 0.6 + t
        if v == 0:
            level += 0.8 + outline * 0.4                        # its lit top
        elif bx == 0:
            level += 0.4 + outline * 0.2
        elif v == 4 or bx == 8:
            level -= 0.5 + outline * 0.4                        # its shaded foot
        level -= dim
        for cr, cc, corner in chips:
            if (cr, cc) == (row % 6, col):
                hit = {"tl": (0, 0), "tr": (0, 8), "bl": (4, 0), "br": (4, 8)}[corner]
                if (v, bx) == hit:
                    level -= 0.7                                # a worn corner, faint
        if (row % 6, col) in cracks and 1 <= v <= 3 and bx == 3 + v:
            level -= 0.6                                        # a faint crack
        level = max(1.2 - dim - outline * 0.5, min(3.6, level))
        lo = int(level)
        return mix(R[lo], R[lo + 1], level - lo)
    return basin_canvas(px)


def basin_base():
    """The plinth's masonry: big formed bricks (twelve texels by seven) in soft, shallow mortar, each pillow-shaded so
    it reads rounded and solid: light rolling over its top and left, shadow gathering to its bottom and right, its
    corners rounded off into the joint, its face mottled; a touch brighter than the stem."""
    R = stone_ramp()
    f = fbm(751)
    rng = random.Random(752)
    tone = {(r, c): rng.uniform(-0.3, 0.3) for r in range(5) for c in range(3)}

    def px(x, y):
        row = y // 8
        off = 6 if row % 2 else 0
        bx = (x + off) % 12
        col = ((x + off) // 12) % 3
        v = y % 8
        if v == 7 or bx == 11:
            return mix(R[2], R[3], 0.2)                          # the joint: soft and shallow
        if (bx, v) in ((0, 0), (10, 0), (0, 6), (10, 6)):
            return mix(R[2], R[3], 0.5)                          # rounded corners, melting into the joint
        u, w = bx / 10.0, v / 6.0                                # across the brick, 0..1
        level = 2.2 + tone[(row % 5, col)]
        level += 0.9 * (1 - w) ** 2 - 0.8 * w ** 2               # its top lit, its foot in shadow
        level += 0.35 * (1 - u) ** 3 - 0.35 * u ** 3             # its left lit, its right shaded
        level += (f(x, y) - 0.5) * 0.45                           # mottled
        level += 0.35                                            # brighter than the stem a touch
        level = max(1.4, min(4.5, level))
        lo = int(level)
        return mix(R[lo], R[min(5, lo + 1)], level - lo)
    return basin_canvas(px)


def stain(img, blots, seed):
    """Soaks blood into a stone texture: each blot (centre x, y, radius x, radius y, in texels) a stain whose middle
    is deep and its edges thin, the stone's own shading showing through (so its joints and bricks run on through it),
    a darker ring where it has soaked in at its rim, a wet glint now and then."""
    rng = random.Random(seed)
    f = fbm(seed)
    blood, deep = (150, 14, 34), (82, 6, 20)
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            best = 0.0
            for cx, cy, rx, ry in blots:
                d = math.hypot((x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry) + (f(x * 2, y * 2) - 0.5) * 0.5
                best = max(best, clamp(1.15 - d))
            if best <= 0:
                continue
            stone = img.getpixel((x, y))[:3]
            lum = (stone[0] * 0.3 + stone[1] * 0.59 + stone[2] * 0.11) / 120.0      # the stone's light under it
            wet = mix(deep, blood, clamp(lum))
            if 0.0 < best < 0.3:
                c = mix(stone, shade(wet, 0.7), 0.45)                              # the soaked rim, dark
            else:
                c = mix(stone, wet, clamp(0.55 + best * 0.4))
                if best > 0.75 and rng.random() < 0.06:
                    c = mix(c, (220, 70, 80), 0.6)                                 # a wet glint
            out.putpixel((x, y), c + (255,))
    return out


def blend_foot(img, base, top=19, foot=26):
    """Fades the foot of the stem and the corner posts into the base: their side faces read texel rows 12 to 26 (stem)
    and 18 to 26 (posts), standing on the plinth at row 26; from `top` down to `foot` the stone is mixed ever more
    toward the base's own (a light touch at first, mostly base at the foot), and its last row darkened a little where
    it sits on the plinth. Reds (the tear, the blood) are left alone."""
    out = img.copy()
    for y in range(top, foot):
        t = ((y - top + 1) / (foot - top)) ** 1.4 * 0.75
        for x in range(img.width):
            c = img.getpixel((x, y))
            if c[0] - max(c[1], c[2]) > 50:
                continue
            m = mix(c[:3], base.getpixel((x, y))[:3], t)
            if y == foot - 1:
                m = shade(m, 0.85)                       # where it sits on the plinth
            out.putpixel((x, y), m + (c[3],))
    return out


# The planter's front: its trough is x 2 to 14, y 1 to 6, so its north face reads u 4 to 28, rows 20 to 30.
def planter_slab():
    """The planter's own stone: irregular dressed slabs (courses of six to ten texels, slabs of six to fourteen across,
    every course laid differently) in soft shallow joints, each slab pillow-shaded (light over its top and left, shadow
    toward its foot and right), its face tooled with faint diagonal chisel strokes; soft, as the basin is."""
    R = stone_ramp()
    f = fbm(781)
    rng = random.Random(782)
    courses, y = [], 0
    while y < 32:
        h = rng.choice((6, 7, 8, 9, 10))
        h = min(h, 32 - y) if 32 - y - h >= 5 or 32 - y - h == 0 else 32 - y
        cuts, x = [], rng.randrange(0, 8)
        while x < 32 + 14:
            cuts.append(x)
            x += rng.choice((6, 8, 10, 12, 14))
        courses.append((y, h, cuts, rng.uniform(-0.25, 0.25)))
        y += h

    def px(x, y):
        for top, h, cuts, tone in courses:
            if top <= y < top + h:
                v = y - top
                left = max(c for c in cuts if c <= x + 32) - 32 if min(cuts) > x else max(c for c in cuts if c <= x)
                right = min((c for c in cuts if c > x), default=x + 14)
                if v == h - 1 or x == right - 1:
                    return mix(R[1], R[2], 0.5)                      # the joint, soft and shallow
                u, w = (x - left) / max(1, right - left - 1), v / max(1, h - 2)
                level = 2.25 + tone + ((left * 7 + top) % 5 - 2) * 0.08
                level += 0.8 * (1 - w) ** 2 - 0.65 * w ** 2           # pillowed: top lit, foot shaded
                level += 0.3 * (1 - u) ** 3 - 0.3 * u ** 3
                if (x + y) % 4 == 0:
                    level -= 0.12                                     # the chisel's diagonal tooling, faint
                level += (f(x, y) - 0.5) * 0.35
                level = max(1.3, min(4.2, level))
                lo = int(level)
                return mix(R[lo], R[lo + 1], level - lo)
        return R[2]
    img = basin_canvas(px)
    return planter_slab_detail(img, courses, R)


def planter_slab_detail(img, courses, R):
    """The slabs' marks, drawn over their shading: chips out of slab corners (dark, a lit lip under them), pits that
    catch light on their lower edge, a hairline crack across a slab or two, a thin red vein through one, a small carved
    rosette of thorns in relief on a few, and the stone darker where weather sat along the foot of each course."""
    rng = random.Random(783)
    put = lambda x, y, c: 0 <= x < 32 and 0 <= y < 32 and img.putpixel((x, y), c + (255,))
    get = lambda x, y: img.getpixel((x % 32, y % 32))[:3]
    slabs = []
    for top, h, cuts, _ in courses:
        for i in range(len(cuts) - 1):
            left, right = cuts[i] % 32, cuts[i + 1]
            slabs.append((left, top, right - cuts[i] - 1, h - 1))
    for left, top, w, h in slabs:
        # Weathering along its foot.
        for x in range(left, left + w):
            put(x % 32, top + h - 1, shade(get(x, top + h - 1), 0.88))
        # A chip out of a corner, now and then.
        if rng.random() < 0.45 and w > 4:
            cx = left if rng.random() < 0.5 else left + w - 2
            put(cx % 32, top, R[1]); put((cx + 1) % 32, top, R[1]); put(cx % 32, top + 1, R[1])
            put((cx + 1) % 32, top + 1, R[4])
        # Pits.
        for _ in range(rng.randint(1, 3)):
            if w > 3 and h > 3:
                x, y = left + rng.randrange(1, w - 1), top + rng.randrange(1, h - 2)
                put(x % 32, y, shade(get(x, y), 0.62)); put(x % 32, y + 1, shade(get(x, y + 1), 1.18))
    big = [sl for sl in slabs if sl[2] >= 10 and sl[3] >= 6]
    # A carved rosette of thorns in relief: four points round a boss, lit on their upper left, shadowed below right.
    for left, top, w, h in big[:2]:
        cx, cy = left + w // 2, top + h // 2
        for dx, dy in ((0, -2), (0, 2), (-2, 0), (2, 0), (0, 0)):
            x, y = (cx + dx) % 32, cy + dy
            put(x, y, mix(R[3], R[4], 0.5) if (dx, dy) != (0, 0) else R[4])
            put((x + 1) % 32, y + 1, shade(get(x + 1, y + 1), 0.7))
        for dx, dy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            put((cx + dx) % 32, cy + dy, shade(get(cx + dx, cy + dy), 0.82))
    # A hairline crack across one slab, a red vein through another.
    if big:
        left, top, w, h = big[-1]
        x, y = left + 1, top + 1
        while x < left + w - 1 and y < top + h - 1:
            put(x % 32, y, R[1])
            put(x % 32, y + 1, shade(get(x, y + 1), 1.12))
            x += 1
            y += rng.choice((0, 1)) if rng.random() < 0.5 else 0
    if len(slabs) > 3:
        left, top, w, h = slabs[3]
        y = top + h // 2
        for x in range(left + 1, left + w - 1):
            put(x % 32, y, (150, 18, 40) if x % 3 else (210, 40, 66))
            y = max(top + 1, min(top + h - 2, y + rng.choice((-1, 0, 0, 1))))
    return img


def basin_underside():
    """The basin's undersides: the labyrinth's stone, plain and in shadow, only softly mottled, a faint grain of tooling
    across it, so nothing busy shows from below."""
    R = stone_ramp()
    f = fbm(821)

    def px(x, y):
        level = 1.75 + (f(x, y) - 0.5) * 0.4 - (0.08 if (x + y) % 4 == 0 else 0.0)
        lo = int(level)
        return mix(R[lo], R[lo + 1], level - lo)
    return basin_canvas(px)


def pixel_grain(x, y, seed=0x5bd1):
    """A stone's pixel grain, as the statue's skin has: a quarter step lighter or darker here and there, by a hash of
    the texel, so it reads as single pixels rather than a smooth mottle."""
    h = (x * 73856093 ^ y * 19349663 ^ seed) & 0xFFFF
    return (-0.25, 0.0, 0.0, 0.0, 0.25, 0.0, -0.25, 0.25)[h % 8]


def stepped(R, level):
    """A ramp level as pixel art: rounded to a half step, so each texel is one of a few clear shades."""
    level = max(0.0, min(4.99, round(level * 2) / 2))
    k = int(level)
    return mix(R[k], R[min(5, k + 1)], level - k)


def planter_trim(pixel=False, iron=False):
    """The planter's mouldings (pixel: the statue's, in its pixel-art style: grain, pits, half steps), laid out in regions the trim pieces read by explicit uv (see moulding()):
    rows 0-1 a band (a bright lip over a body, a soft dentil every six texels), row 4 a toe's face, row 6 a bead caught
    in the light, row 8 a fillet in shadow, rows 10-11 a cap (bright arris over a lit face), rows 12-15 the undersides in
    shadow, and rows 16-31 the tops: lit, brightest along the outer arris (its first row and first column) as a bevel
    is."""
    R = iron_ramp() if iron else stone_ramp()
    f = fbm(811)

    def px(x, y):
        # The same stone as the trough's slabs (planter_slab): its tones, its mottling and its faint diagonal tooling,
        # each part of the profile only a step lighter or darker than the slab face it frames.
        if y >= 16:
            level = 3.35 if (y == 16 or x == 0) else 2.95 if (y == 17 or x == 1) else 2.6
        else:
            level = {0: 3.25, 1: 2.0 if x % 6 == 5 else 2.3, 4: 2.25, 6: 3.0, 8: 1.55, 10: 3.4, 11: 2.45}.get(y, 1.3 if y >= 12 else 2.3)
        if iron:
            level = 2.4 + (level - 2.4) * 1.15 + 0.35                                  # its profile, gently
            # Its shadows soft: never down into the ramp's near-black, which read as hard black lines.
            return stepped(R, max(2.0, min(4.9, level + metal_grain(x, y, 0x1f3))))
        if pixel:
            level += 0.4 + pixel_grain(x, y, 0x77a1)                        # a step lighter, as the statue is
            if ((x * 2654435761 ^ y * 40503 ^ 0x77a1) >> 7) % 19 == 0:
                level -= 0.75                                               # a pit
            return stepped(R, max(1.0, min(4.5, level)))
        if (x + y) % 4 == 0:
            level -= 0.1
        level += (f(x, y) - 0.5) * 0.35
        level = max(1.3, min(4.2, level))
        lo = int(level)
        return mix(R[lo], R[lo + 1], level - lo)
    return basin_canvas(px)


def planter_soil():
    """The planter's bed at 32 (two texels to a model pixel, so it reads fine where the moss block's 16 reads coarse):
    dark ashen loam, fine tufts of the ashen moss in small clumps (each a deep base, a mid body and a lit tip on its upper
    left), a few pale pebbles and flecks of ash, and a soft shadow round its edge where the rim overhangs it (the bed
    shows texels 6 to 26 across, 8 to 24 down)."""
    rng = random.Random(801)
    f = fbm(802)
    loam = [(20, 30, 32), (26, 38, 40), (33, 46, 48), (40, 55, 56)]
    moss = [(34, 52, 54), (47, 69, 69), (62, 88, 84), (78, 106, 100)]
    img = Image.new("RGBA", (32, 32))
    for y in range(32):
        for x in range(32):
            m = f(x * 2, y * 2)
            k = 3 if m > 0.68 else 2 if m > 0.45 else 1 if m > 0.25 else 0
            img.putpixel((x, y), loam[k] + (255,))
    # The moss: clumps of tufts, thickest where the noise is high, each tuft two or three texels.
    for _ in range(170):
        x, y = rng.randrange(32), rng.randrange(32)
        if f(x * 2 + 40, y * 2 + 40) < 0.42:
            continue
        for dx, dy, k in ((0, 0, 1), (1, 0, 0), (0, 1, 0), (-1, 0, 2), (0, -1, 2), (-1, -1, 3)):
            if rng.random() < (0.9 if k < 3 else 0.55):
                img.putpixel(((x + dx) % 32, (y + dy) % 32), moss[k] + (255,))
    # Pebbles (a lit texel over a shadowed one) and flecks of ash.
    for _ in range(9):
        x, y = rng.randrange(1, 31), rng.randrange(1, 30)
        img.putpixel((x, y), (96, 92, 96, 255))
        img.putpixel((x, y + 1), (44, 48, 52, 255))
    for _ in range(14):
        img.putpixel((rng.randrange(32), rng.randrange(32)), (104, 98, 100, 255))
    # The rim's shadow round the bed's edge.
    for y in range(32):
        for x in range(32):
            d = min(x - 6, 25 - x, y - 8, 23 - y)
            if 0 <= d < 2:
                img.putpixel((x, y), shade(img.getpixel((x, y))[:3], 0.6 if d == 0 else 0.8) + (255,))
    return img


def planter_post():
    """The planter's posts and feet: soft vertical fluting, each flute a gentle hollow between rounded ribs, lit on
    its left and falling to shadow on its right, with a lit band at the head of each drum and a soft line under it."""
    R = stone_ramp()
    f = fbm(791)

    def px(x, y):
        t = (x % 6) / 5.0
        level = 2.4 + 0.6 * math.cos(t * math.pi * 2 - 0.6) * 0.8      # rib and hollow, lit from the left
        if y % 16 == 0:
            level = 3.6                                                # the drum's lit head
        elif y % 16 == 1:
            level -= 0.4
        elif y % 16 == 15:
            level = 1.6                                                # the soft joint under it
        if y % 16 in (5, 6) and x % 3 == 1:
            level -= 0.7                                               # a carved band of notches round each drum
        elif y % 16 == 7:
            level += 0.3                                               # its lit lower lip
        level += (f(x, y) - 0.5) * 0.3
        level = max(1.3, min(4.0, level))
        lo = int(level)
        return mix(R[lo], R[lo + 1], level - lo)
    img = basin_canvas(px)
    rng = random.Random(792)
    for _ in range(9):                                                 # nicks along the ribs and worn pits
        x, y = rng.randrange(32), rng.randrange(2, 30)
        img.putpixel((x, y), shade(img.getpixel((x, y))[:3], 0.62) + (255,))
        img.putpixel((x, y + 1), shade(img.getpixel((x, y + 1))[:3], 1.15) + (255,))
    x = 20                                                             # a faint crack down one flute
    for y in range(3, 13):
        img.putpixel((x, y), shade(img.getpixel((x, y))[:3], 0.7) + (255,))
        x += rng.choice((0, 0, 1, -1))
    return img


def planter_panel_soft():
    """The trough's front: its dressed slabs, and sunk in them a panel (texels 6 to 26 across, 21 to 29 down), its frame
    lit along its lower right and shadowed along its upper left as a recess is."""
    R = stone_ramp()
    img = planter_slab()
    # The panel sits well inside the face (which reads texels 4 to 28 across, rows 20 to 30), clear of its edges,
    # its recess shaded softly: a gentle shadow along its top and left, a soft light along its bottom and right.
    for y in range(22, 29):
        for x in range(8, 25):
            c = img.getpixel((x, y))[:3]
            if 9 <= x <= 23 and 23 <= y <= 27:
                c = mix(c, R[2], 0.5)                          # the sunk field, calmer than the stone round it
                if x == 9 or y == 23:
                    c = mix(c, R[1], 0.6)                       # its soft shadowed upper and left edges
            elif x == 24 or y == 28:
                c = mix(c, R[4], 0.6)                           # the frame lit below and right
            else:
                c = mix(c, R[1], 0.45)                          # and softly dark above and left
            img.putpixel((x, y), c + (255,))
    return img


def basin_pillar_top():
    """The stem's end at 32: a bevelled square, a ring cut in it round a worn boss."""
    R = stone_ramp()

    def px(x, y):
        r = math.hypot(x - 15.5, y - 15.5)
        if x < 1 or y < 1:
            return R[5]
        if x > 30 or y > 30:
            return R[0]
        if 9 < r < 11:
            return R[0] if (x + y) > 31 else R[1]
        if 11 <= r < 12:
            return R[4]
        if r < 4:
            return R[4] if (x + y) < 30 else R[3]
        return R[2]
    return basin_canvas(px)


# The tear on the stem's front: small, carved in the middle of the face the stem shows (texels 10 to 22 across, 12 to
# 26 down: the stem's north face reads uv [5, 6, 11, 13]).
TEAR_CX, TEAR_CY = 16.0, 19.5


def tear_cells():
    """The tear's texels: a drop about six across and nine high, its point up."""
    def inside(x, y):
        dx, dy = x + 0.5 - TEAR_CX, y + 0.5 - TEAR_CY
        if dy >= 0:
            return (dx / 2.4) ** 2 + (dy / 2.8) ** 2 <= 1
        return dy > -5 and abs(dx) <= 2.4 * (1 + dy / 5.0)
    return {(x, y) for x in range(11, 22) for y in range(12, 25) if inside(x, y)}


def basin_stem():
    """The stem's front: its brick, and the tear carved small in it, its carved edge dark on its upper left and lit on
    its lower right, its hollow a deep red (the glowing tear itself is the overlay over it: basin_tear_glow)."""
    R = stone_ramp()
    img = basin_pillar()
    cells = tear_cells()
    for x, y in cells:
        img.putpixel((x, y), (70, 4, 20, 255))
    for x in range(10, 23):
        for y in range(11, 26):
            if (x, y) in cells:
                continue
            if any(n in cells for n in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1))):
                lower_right = (x - 1, y) in cells or (x, y - 1) in cells
                img.putpixel((x, y), mix(R[4], R[5], 0.5) + (255,) if lower_right else R[1] + (255,))
    return img


def basin_tear_glow(frames=8):
    """The tear's glow, animated (frames down a strip, basin_tear_glow.png.mcmeta) and drawn bright in the dark
    (light_emission on its element): only the tear's texels, the rest clear. Each frame it swells and fades: deep red at
    its rim to a bright core at its heart and a highlight on its upper left, all brightening and dimming together."""
    cells = tear_cells()
    reds = [(96, 6, 24), (150, 12, 34), (204, 26, 52), (246, 70, 92), (255, 168, 168)]
    img = Image.new("RGBA", (32, 32 * frames), (0, 0, 0, 0))
    for f in range(frames):
        pulse = 0.5 - 0.5 * math.cos(f / frames * math.tau)   # 0 dim .. 1 bright .. 0
        for x, y in cells:
            dx, dy = x + 0.5 - TEAR_CX, y + 0.5 - (TEAR_CY + 0.5)
            core = clamp(1.0 - math.hypot(dx / 2.4, dy / 3.2))   # 1 at its heart, 0 at its rim
            v = 0.2 + 0.35 * core + 0.45 * pulse * (0.4 + 0.6 * core)
            if dx < -0.5 and -2.5 < dy < 0:
                v += 0.25 * (0.5 + pulse)                         # the highlight on its upper left
            v = clamp(v) * 4
            lo = min(3, int(v))
            img.putpixel((x, f * 32 + y), mix(reds[lo], reds[lo + 1], v - lo) + (255,))
    return img


HAND16 = {"candelabra_iron": candelabra_iron, "candelabra_wax": candelabra_wax}


def statue_face():
    """The statue's face, as a player's head is drawn: a carved brow lit along its top, eyes shut and sunk in shadow, a
    nose, a mouth turned down, a red tear run from one eye; a hood's edge framing it."""
    img = statue_stone()
    for y in range(N):
        for x in range(N):
            if x < 3 or x > 28 or y < 4:  # the hood's edge round the face
                img.putpixel((x, y), shade(img.getpixel((x, y))[:3], 0.62 if y >= 4 else 0.7) + (255,))
    for x in range(6, 26):
        img.putpixel((x, 11), shade(img.getpixel((x, 11))[:3], 1.25) + (255,))  # the brow, catching the light
    carve(img, [(x, 13) for x in range(7, 14)] + [(x, 13) for x in range(18, 25)], 0.5, 1.15)      # under the brow
    carve(img, [(x, 15) for x in range(8, 13)] + [(x, 15) for x in range(19, 24)], 0.35, 1.1)      # shut eyes
    carve(img, [(15, 17), (16, 17), (15, 18), (16, 19), (15, 20), (16, 20)], 0.7, 1.2)              # the nose's side
    carve(img, [(x, 24) for x in range(11, 21)] + [(10, 25), (21, 25)], 0.45, 1.15)                # the mouth
    for y in range(16, 31):                                                                         # a tear
        img.putpixel((10, y), mix(RED, CRIMSON, (y - 16) / 15) + (255,))
    return img


def statue_chest():
    """The statue's chest: its robe gathered under a carved collar and a belt with a clasp."""
    img = statue_robe()
    for x in range(N):
        for y in (2, 3):
            img.putpixel((x, y), shade(img.getpixel((x, y))[:3], 1.2 if y == 2 else 0.7) + (255,))  # the collar
        for y in (19, 20, 21):
            belt = shade(PALE[2], 1.15) if y == 19 else mix(PALE[1], PALE[2], 0.5) if y == 20 else shade(PALE[1], 0.75)
            img.putpixel((x, y), belt + (255,))                                                          # the belt
    for x in range(14, 18):
        for y in range(18, 23):
            img.putpixel((x, y), mix(CRIMSON, RED, 0.3) + (255,) if (x in (15, 16) and y in (19, 20, 21)) else shade(PALE[3], 1.1) + (255,))
    return img


def planter_panel():
    """The planter's front: a sunk panel in a moulded frame (lit on its top and left, shadowed on its bottom and right)
    with a weeping eye carved in the middle of it, a red tear under it."""
    img = font_stone()
    for y in range(N):
        for x in range(N):
            c = img.getpixel((x, y))[:3]
            if 3 <= x <= 28 and 4 <= y <= 27:
                c = shade(c, 0.78)  # the sunk field
                if x == 3 or y == 4:
                    c = shade(c, 0.6)  # its shadowed top and left inner edge
                if x == 28 or y == 27:
                    c = shade(c, 1.35)
            elif x in (1, 2) or y in (2, 3):
                c = shade(c, 1.25)
            elif x in (29, 30) or y in (28, 29):
                c = shade(c, 0.7)
            img.putpixel((x, y), c + (255,))
    eye = [(x, y) for x in range(9, 23) for y in range(11, 21) if ((x - 15.5) / 6.5) ** 2 + ((y - 15.5) / 4.2) ** 2 <= 1]
    carve(img, eye, 0.55, 1.25)
    for x, y in eye:
        if ((x - 15.5) / 2.4) ** 2 + ((y - 15.5) / 2.4) ** 2 <= 1:
            img.putpixel((x, y), mix(CRIMSON, RED, 0.4) + (255,))  # the iris
    for y in range(21, 27):
        img.putpixel((15, y), mix(RED, CRIMSON, (y - 21) / 6) + (255,))
    return img


def font_stone():
    """The weeping basin's stone: the Dark Between's dreamstone, dressed smooth, softly mottled, a little lighter at its top
    edges where the light falls, grime at its foot."""
    f = fbm(361)

    def px(x, y):
        v = 0.42 + 0.35 * f(x, y) + 0.12 * (1 - y / N)
        return mix(STONE[0], STONE[4], v)
    return paint(canvas(), px)


def basin_face():
    """The front of the basin's stem: fluted stone with a weeping face carved in it, its brow lit, its eyes shut and
    sunk in shadow, a red tear running down from each."""
    img = font_stone()
    for y in range(N):
        for x in (0, 1, 30, 31):
            img.putpixel((x, y), shade(img.getpixel((x, y))[:3], 1.25 if x < 2 else 0.6) + (255,))
    # The face: a raised oval, lit on its upper left.
    for y in range(4, 28):
        for x in range(6, 26):
            d = ((x - 15.5) / 9.5) ** 2 + ((y - 15) / 11.5) ** 2
            if d <= 1:
                light = 1.0 + 0.35 * ((15.5 - x) / 9.5 + (15 - y) / 11.5) * 0.5 - 0.15 * d
                img.putpixel((x, y), shade(img.getpixel((x, y))[:3], light) + (255,))
    brow = [(x, 10) for x in range(9, 23)]
    carve(img, [(x, 11) for x in range(9, 14)] + [(x, 11) for x in range(18, 23)], 0.4, 1.2)  # under the brow
    carve(img, [(x, 13) for x in range(10, 14)] + [(x, 13) for x in range(18, 22)], 0.35, 1.1)  # the shut eyes
    for x, y in brow:
        img.putpixel((x, y), shade(img.getpixel((x, y))[:3], 1.3) + (255,))
    carve(img, [(15, 16), (16, 16), (15, 17), (16, 18)], 0.6, 1.2)  # the nose's shadow
    carve(img, [(x, 22) for x in range(12, 20)] + [(11, 23), (20, 23)], 0.4, 1.15)  # the mouth, downturned
    for x in (11, 20):  # the tears
        for y in range(14, 31):
            t = (y - 14) / 16
            img.putpixel((x, y), mix(RED, CRIMSON, t) + (255,))
            if y % 6 == 0:
                img.putpixel((x - 1, y), mix(RED, HOT, 0.4) + (255,))
    return img


def flutes(x):
    """A fluted shaft's shading across it: concave flutes six pixels wide between narrow lit fillets."""
    k = x % 8
    if k == 0:
        return 1.25  # the fillet, catching the light
    if k == 7:
        return 0.75
    t = (k - 1) / 5  # across the flute: its left side in shadow, its right side lit
    return 0.7 + 0.45 * t


def moulded(profile, top_rows, seed):
    """A column's moulded part: `profile` gives, for each row, how far the surface stands out (bigger is further out);
    a row stepping out below the one above faces up and is lit, one stepping in is in shadow. Rows past `top_rows`
    (or before) are the fluted shaft."""
    f = fbm(seed)

    def px(x, y):
        out = profile.get(y)
        if out is None:
            v = 0.5 * flutes(x)
            return mix(STONE[0], STONE[4], v + 0.12 * (f(x, y) - 0.5))
        above = profile.get(y - 1, out)
        below = profile.get(y + 1, out)
        slope = (out - above) * 0.55 + (below - out) * -0.25  # stepping out under the row above: facing up, lit
        v = 0.42 + 0.06 * out + slope + 0.1 * (f(x, y) - 0.5)
        c = mix(STONE[0], STONE[4], v)
        if x in (0, 1):
            c = shade(c, 1.12)
        if x in (30, 31):
            c = shade(c, 0.78)
        return c
    return px


def pier_base_side():
    """The pier's foot, lit row by row as the mouldings turn to and from the light above: the flutes of the shaft, a
    fillet and the shadow under it, a small torus bright on its crown and dark beneath, a deep scotia, a fat torus
    rounding away into shadow, the dark line where it sits on the plinth, and the plinth, its top edge lit."""
    f = fbm(371)
    light = {8: 0.88, 9: 0.32, 10: 0.92, 11: 0.78, 12: 0.58, 13: 0.36, 14: 0.22, 15: 0.28, 16: 0.46,
             17: 0.96, 18: 0.92, 19: 0.82, 20: 0.68, 21: 0.52, 22: 0.38, 23: 0.27, 24: 0.2,
             25: 0.9, 26: 0.66, 27: 0.6, 28: 0.58, 29: 0.56, 30: 0.52, 31: 0.3}

    def px(x, y):
        if y < 8:
            v = 0.5 * flutes(x)
        else:
            v = light[y] + 0.05 * math.sin(x * 0.4)
            if x in (0, 1):
                v += 0.08
            if x in (30, 31):
                v -= 0.1
        return mix(STONE[0], STONE[4], v + 0.1 * (f(x, y) - 0.5))
    img = paint(canvas(), px)
    for x, y in ((6, 28), (19, 27), (26, 29)):  # a few chips knocked out of the plinth
        carve(img, [(x, y), (x + 1, y)], 0.55, 1.15)
    return img


def pier_capital_side():
    """The pier's head: the flutes ending at an astragal, a bell of carved leaves (each ribbed, lit on its left, a red
    vein down its rib, curling over at its tip), and the abacus over all with a rune cut in it."""
    f = fbm(381)
    base = moulded({**{y: 2.5 + 1.2 * math.sin((y - 27) / 3 * math.pi) for y in range(27, 30)}}, 0, 382)

    def px(x, y):
        if y >= 30:
            return mix(STONE[0], STONE[4], 0.5 * flutes(x))
        if y >= 27:
            return base(x, y)
        if y >= 8:  # the bell of leaves
            k = x % 16
            across = (k - 7.5) / 7.5  # -1 to 1 across a leaf
            top = 8 + int(abs(across) * 5)
            if y < top:
                return mix(STONE[0], STONE[1], 0.5)  # behind the leaves
            v = 0.55 + 0.3 * (-across) * 0.6 + 0.2 * (1 - abs(across)) - 0.15 * ((y - 8) / 19) + 0.08 * (f(x, y) - 0.5)
            c = mix(STONE[0], STONE[4], v)
            if abs(across) < 0.1:
                c = mix(CRIMSON, RED, 0.3 + 0.4 * (1 - (y - 8) / 19)) if y > 12 else STONE[4]  # the vein down the rib
            if y == top:
                c = shade(STONE[4], 1.05)  # the curl, catching the light
            if y == top + 1:
                c = shade(c, 0.7)
            return c
        if y in (6, 7):
            return mix(STONE[0], STONE[1], 0.4)  # the shadow under the abacus
        v = 0.75 - y * 0.06 + 0.08 * (f(x, y) - 0.5)  # the abacus, lit on its top edge
        return mix(STONE[1], STONE[4], v)
    img = paint(canvas(), px)
    carve(img, [(15, 2), (16, 2), (14, 3), (17, 3), (15, 4), (16, 4), (15, 3)], 0.4, 1.2, glow=mix(RED, CRIMSON, 0.4))
    return img


def pier_end():
    """The end of a pier stone: a bevelled square, rings cut in it round a carved rose."""
    f = fbm(391)

    def px(x, y):
        c = mix(STONE[1], STONE[3], 0.45 + 0.3 * f(x, y))
        if x < 2 or y < 2:
            c = shade(c, 1.25)
        if x > 29 or y > 29:
            c = shade(c, 0.65)
        r = math.hypot(x - 15.5, y - 15.5)
        if 10.5 < r < 11.8 or 5.5 < r < 6.6:
            c = shade(c, 0.55 if (x + y) > 31 else 0.7)
        elif r < 5.5:
            petal = math.cos(math.atan2(y - 15.5, x - 15.5) * 6)
            c = shade(c, 0.85 + 0.3 * petal * (1 - r / 5.5))
        return c
    return paint(canvas(), px)


TEXTURES = {"dark_iron": dark_iron, "candle_wax": candle_wax, "statue_stone": statue_stone, "statue_robe": statue_robe,
            "statue_hood": statue_hood, "dreamstone_pillar": dreamstone_pillar, "dreamstone_pillar_top": dreamstone_pillar_top, "iron_round": iron_round, "font_round": font_round, "font_rim": font_rim, "statue_face": statue_face, "statue_chest": statue_chest, "planter_panel": planter_panel, "statue_plinth": statue_plinth, "statue_shadow": statue_shadow, "font_stone": font_stone,
            "basin_face": basin_face, "dreamstone_pier_base_side": pier_base_side,
            "dreamstone_pier_capital_side": pier_capital_side, "dreamstone_pier_end": pier_end}


# ---- Models ----
def el(f, t, tex, rot=None, emit=0, faces=("north", "south", "east", "west", "up", "down"), uv=None, **per_face):
    """An element; its faces read the texture where they lie (no uv given), or as told. per_face: a texture for a
    face by its direction (up="...", north="...")."""
    e = {"from": [round(v, 3) for v in f], "to": [round(v, 3) for v in t],
         "faces": {d: dict({"texture": "#" + per_face.get(d, tex)}, **({"uv": uv} if uv else {})) for d in faces}}
    if rot:
        e["rotation"] = rot
    if emit:
        e["light_emission"] = emit
    return e


def octagon(cx, cz, y0, y1, half, tex, **kw):
    """A round part as a stepped circle: a box wide across x, one wide across z, and one filling the corners between
    them (no turned boxes, which overlap into an eight-pointed star). Their tops and bottoms are set a hair apart, so
    no two faces lie in one plane and flicker."""
    out = []
    for k, (a, b) in enumerate(((1.0, 0.42), (0.42, 1.0), (0.74, 0.74))):
        d = 0.012 * k
        out.append(el([cx - half * a, y0 + d, cz - half * b], [cx + half * a, y1 - d, cz + half * b], tex, **kw))
    return out


def taper(cx, cz, y0, y1, half0, half1, steps, tex, **kw):
    """A round part narrowing (or swelling) from half0 at y0 to half1 at y1, in fine octagonal steps."""
    out = []
    for i in range(steps):
        a, b = y0 + (y1 - y0) * i / steps, y0 + (y1 - y0) * (i + 1) / steps
        h = half0 + (half1 - half0) * (i + 0.5) / steps
        out += octagon(cx, cz, a, b, h, tex, **kw)
    return out


def bulb(cx, cz, y0, y1, half, tex, steps=4):
    """A knot or bulb: swelling round and narrowing again."""
    out = []
    for i in range(steps):
        a, b = y0 + (y1 - y0) * i / steps, y0 + (y1 - y0) * (i + 1) / steps
        t = (i + 0.5) / steps
        out += octagon(cx, cz, a, b, half * (0.55 + 0.45 * math.sin(t * math.pi)), tex)
    return out


def cross(cx, cz, y0, y1, half, tex, emit=0):
    return [el([cx - half, y0, cz], [cx + half, y1, cz], tex, rot={"origin": [cx, y0, cz], "axis": "y", "angle": a}, emit=emit,
               faces=("north", "south"), uv=[0, 0, 16, 16]) for a in (45, -45)]


def spear(x, z, y0, w):
    """A spear head: a collar, and a point diamond-shaped from every side (a block turned 45 degrees two ways)."""
    return [el([x - w, y0, z - w], [x + w, y0 + 0.8, z + w], "iron"),
            el([x - w * 0.75, y0 + 0.8, z - w * 0.75], [x + w * 0.75, y0 + 2.4, z + w * 0.75], "iron",
               rot={"origin": [x, y0 + 1.6, z], "axis": "x", "angle": 45}),
            el([x - w * 0.75, y0 + 0.8, z - w * 0.75], [x + w * 0.75, y0 + 2.4, z + w * 0.75], "iron",
               rot={"origin": [x, y0 + 1.6, z], "axis": "z", "angle": 45}),
            el([x - 0.15, y0 + 2.4, z - 0.15], [x + 0.15, y0 + 3.4, z + 0.15], "iron")]


# ---- Banded shading at 32 (the pixel artist's cylinder, two texels to a model pixel): each part shaded across its
# width in flat bands, a dark outline at either side, a one-texel glint left of its middle, a bright rim along its top,
# and details drawn into it (dents, pits, cracks, drips). The texture is 96 texels square (six to a uv unit): side
# strips for every width a part can be in its top 48 rows, and below them a tile for tops (lit) and one for
# undersides (shadowed). Every face is given the strip for its own width, so nothing is ever stretched.
SCALE = 2                                       # texels per model pixel
TEX = 96                                        # the texture's size
UV = TEX / 16                                   # texels per uv unit
BAND_WIDTHS = (1, 2, 3, 4, 6, 8, 10, 12)
BAND_START = {}
_x = 0
for _w in BAND_WIDTHS:
    BAND_START[_w] = _x
    _x += _w * SCALE
TOP_TILE, UNDER_TILE = (0, 48), (48, 48)        # where the 24x24 tiles for tops and undersides are


def band(cols, i):
    """The shade (0 darkest .. 5 glint) of texel column i across a strip `cols` texels wide."""
    if cols <= 2:
        return (4, 2)[i] if cols == 2 else 3
    if i == cols - 1:
        return 0                                # the dark outline on its shadowed side
    if i == 0:
        return 1                                # and a darker edge on its lit side
    t = (i + 0.5) / cols
    if abs(t - 0.28) < 0.5 / cols:
        return 5                                # the glint
    v = math.cos((t - 0.3) * math.pi * 0.95)
    return 4 if v > 0.85 else 3 if v > 0.55 else 2 if v > 0.2 else 1


def banded_texture(pal, seed, detail):
    """pal: six colours, darkest to the glint. detail(img, x0, cols, rng) draws a material's marks into a strip."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    for w, x0 in BAND_START.items():
        cols = w * SCALE
        for i in range(cols):
            k = band(cols, i)
            for y in range(48):
                kk = min(5, k + 2) if y == 0 else min(5, k + 1) if y == 1 else k  # the bright rim along its top
                img.putpixel((x0 + i, y), pal[kk] + (255,))
        if detail:
            detail(img, x0, cols, rng)
    for (tx, ty), lit in ((TOP_TILE, True), (UNDER_TILE, False)):
        for y in range(24):
            for x in range(24):
                if lit:
                    k = 4 if (x < 2 or y < 2) else 3
                    if (x + y) % 11 == 0 and rng.random() < 0.5:
                        k = 2                    # a worn speck
                else:
                    k = 1 if (x < 1 or y < 1) else 0 if rng.random() < 0.15 else 1
                img.putpixel((tx + x, ty + y), pal[k] + (255,))
    return img


def banded(elements, textures):
    """Gives each face of elements wearing one of `textures` the strip (or tile) for its own size, one model pixel to
    two texels."""
    for e in elements:
        f, t = e["from"], e["to"]
        sx, sy, sz = (max(1, round(t[i] - f[i])) for i in range(3))
        for d, face in e["faces"].items():
            if face["texture"].lstrip("#") not in textures:
                continue
            if d in ("up", "down"):
                tx, ty = TOP_TILE if d == "up" else UNDER_TILE
                face["uv"] = [tx / UV, ty / UV, (tx + min(24, sx * SCALE)) / UV, (ty + min(24, sz * SCALE)) / UV]
                continue
            w, h = {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy)}[d]
            w = min(BAND_WIDTHS, key=lambda b: (abs(b - w), b))
            x0 = BAND_START[w]
            face["uv"] = [x0 / UV, 0, (x0 + w * SCALE) / UV, min(48, h * SCALE) / UV]
    return elements


IRON_BANDS = [(10, 9, 16), (30, 29, 40), (54, 53, 68), (86, 85, 104), (128, 127, 148), (196, 196, 212)]
WAX_BANDS = [(132, 104, 74), (176, 150, 116), (210, 190, 158), (232, 220, 194), (246, 240, 224), (255, 253, 244)]
STONE_BANDS = [(20, 15, 30), (42, 35, 56), (66, 58, 82), (96, 88, 114), (130, 122, 150), (176, 168, 194)]
RIM_BANDS = [(42, 35, 56), (70, 62, 88), (100, 92, 120), (132, 124, 152), (170, 162, 190), (214, 206, 228)]


def metal_grain(x, y, seed):
    """Worked iron's surface: faint brushed streaks running along it (each row, in runs of five texels, a little
    lighter or darker), a light pixel grain over them, and now and then a short bright scratch."""
    h = lambda *v: (sum(a * b for a, b in zip(v, (73856093, 19349663, 83492791))) ^ seed) & 0xFFFF
    level = ((h(y, x // 5) % 3) - 1) * 0.2                  # the brushing
    level += pixel_grain(x, y, seed) * 0.8                  # the grain
    if h(x // 3, y, 7) % 41 == 0:
        level += 0.6                                        # a scratch catching the light
    return level


def candelabra_foot_top():
    """The top of the candelabra's foot's broad step, framed (it wears all of it): a lit outer edge on its north and
    west and a dark one on its south and east, a band of brushed iron, a cut groove, a lit lip inside it, and the
    brushed field within."""
    R = iron_ramp()

    def px(x, y):
        d = min(x, y, 31 - x, 31 - y)
        if d <= 1:
            level = 3.3 if (x <= 1 or y <= 1) and x < 30 and y < 30 else 2.2       # the outline, its shadow soft
        elif d == 4:
            level = 2.2                                                             # the groove, a soft shadow
        elif d == 5:
            level = 3.1 if (x == 5 or y == 5) else 2.3                              # its lit lip
        else:
            level = 2.7
        return stepped(R, level + metal_grain(x, y, 0x2c7))
    return basin_canvas(px)


def iron_ramp():
    """The labyrinth's dread iron: the dreamstone's own six shades, darkened, so its iron sits with its stone (a cold
    violet black, its lit edges the stone's grey)."""
    return [shade(c, 0.42 + 0.1 * k) for k, c in enumerate(stone_ramp())]


def candelabra_stem():
    """Twisted iron for the candelabra's stem and arms: soft diagonal ribs winding round it, each a lit ridge over a
    shadowed groove, in clean steps (read where they lie, so a face of any width shows the twist)."""
    R = iron_ramp()

    def px(x, y):
        k = (x + y // 2) % 6
        return stepped(R, (3.2, 2.9, 2.6, 2.0, 2.3, 2.6)[k] + metal_grain(x, y, 0x4e1) * 0.7)
    return basin_canvas(px)


def candelabra_gem():
    """A round red gem in its top left three texels (its face, a pixel and a half across): a cabochon, its corners in
    shadow, a glint near its upper left, deep red falling darker to its lower right; the rest of the texture its sides."""
    # Round: its corners fall into shadow, a glint near its upper left, its body deep red going darker to the lower right.
    gem = [[(92, 8, 24), (226, 64, 80), (128, 12, 34), (92, 8, 24)],
           [(226, 64, 80), (255, 206, 206), (176, 20, 46), (128, 12, 34)],
           [(128, 12, 34), (176, 20, 46), (74, 6, 20), (74, 6, 20)],
           [(92, 8, 24), (128, 12, 34), (74, 6, 20), (60, 4, 16)]]
    R = iron_ramp()

    # The gem's face drawn finer (six texels on its pixel and a half, at u 8 to 11): a cut stone, its girdle a dark
    # outline all round, its crown lit on the upper left, a table in the middle with a white sparkle,
    # its pavilion facets darkening to the lower right.
    O = (70, 6, 20)
    L1, L2, L3, Md, D1, D2 = (255, 226, 226), (240, 110, 120), (214, 48, 66), (170, 20, 44), (120, 12, 32), (84, 6, 22)
    cut = [[O, O, O, O, O, O],
           [O, L2, L2, L3, Md, O],
           [O, L2, L1, L3, Md, O],
           [O, L3, L3, Md, D1, O],
           [O, Md, Md, D1, D2, O],
           [O, O, O, O, O, O]]

    def px(x, y):
        if 16 <= x < 22 and y < 6:
            return cut[y][x - 16]
        if x < 4 and y < 4:
            return gem[y][x]
        if 8 <= x < 13 and y < 5:                     # the setting's face (five texels, two and a half pixels)
            i, j = x - 8, y
            d = min(i, j, 4 - i, 4 - j)
            if d == 0:
                level = 3.4 if (i == 0 or j == 0) and i < 4 and j < 4 else 2.1          # its rim, lit on the upper left
            else:
                level = 1.8                                                            # the opening, in shadow
            return stepped(R, level)
        return (128, 12, 34) if (x + y) % 2 else (150, 16, 40)
    return basin_canvas(px)


# The candelabras, by how many candles they hold: each slot is where a candle stands (its foot's middle, in model pixels,
# facing north). CandelabraBlock (WCISBlocks) is given the same places; keep them alike.
CANDELABRA_SLOTS = {
    "dread_candelabra_single": [(8, 12, 8)],
    "dread_candelabra_double": [(3, 12, 8), (13, 12, 8)],
    "dread_candelabra": [(3, 11, 8), (8, 13, 8), (13, 11, 8)],          # the arms low, the middle raised
    "dread_candelabra_grand": [(8, 14, 8), (3, 12, 8), (13, 12, 8), (8, 12, 3), (8, 12, 13)],
    "dread_chamberstick": [(8, 3.5, 8)],                                 # a small holder, carried by its ring
}


def candelabra(name, with_candles=False):
    """After the references the author gave, in the labyrinth's dread iron (iron_ramp: its dreamstone darkened): a
    moulded foot stepping up, its broad step's top framed; a stem of twisted iron with a moulded knot low and a greater
    one high, a round red gem set into it front and back; twisted arms stepping out and up to moulded drip trays, one for each of
    its candles (CANDELABRA_SLOTS), each with a socket (a ring round a dark hole) for its candle; undersides clean (candelabra_under, framed on the broad ones). The candles are no
    part of the block's model: any of the game's candles set in it are drawn by CandelabraClient. with_candles: for
    its item, plain candles burning in every slot."""
    slots = CANDELABRA_SLOTS[name]
    S = lambda f, t: el(f, t, "stem")
    def M(f, t, kind):
        part = moulding(f, t, kind, "wn")
        part["_kind"] = kind                                       # kept while it is built, so it can be cut round a gem
        return part
    centre = (8, 8) in [(x, z) for x, _, z in slots]
    crosswise = any(z != 8 for _, _, z in slots)
    if name == "dread_chamberstick":
        # A chamberstick: a shallow framed dish with a lip, a short twisted stem, a tray for its socket, and on its
        # east side a ring for a finger with a thumb rest over it.
        e = [M([3, 0, 3], [13, 0.5, 13], "toe"), M([3.5, 0.5, 3.5], [12.5, 1, 12.5], "band")]
        e[1]["faces"]["up"] = {"texture": "#foot", "uv": [0, 0, 16, 16]}                    # the dish, framed
        e += [M([3.5, 1, 3.5], [12.5, 1.5, 4], "bead"), M([3.5, 1, 12], [12.5, 1.5, 12.5], "bead"),  # its lip
              M([3.5, 1, 4], [4, 1.5, 12], "bead"), M([12, 1, 4], [12.5, 1.5, 12], "bead")]
        e += [M([6.5, 1, 6.5], [9.5, 1.5, 9.5], "bead"), S([7, 1.5, 7], [9, 3, 9]),               # its stem
              M([6, 3, 6], [10, 3.5, 10], "cap")]                                                # the socket's tray
        e += [S([12.5, 0.5, 7.5], [14.5, 1, 8.5]), S([14, 1, 7.5], [15, 3, 8.5]),                # the ring
              S([12.5, 3, 7.5], [15, 3.5, 8.5]), M([12.5, 3.5, 7], [14.5, 4, 9], "bead")]        # and its thumb rest
    else:
        # The foot: a moulded base of dread iron, stepped up as the labyrinth's pedestals are.
        e = [M([3, 0, 3], [13, 0.5, 13], "toe"), M([3.5, 0.5, 3.5], [12.5, 1.5, 12.5], "band"),
             M([5, 1.5, 5], [11, 2, 11], "bead"), M([6, 2, 6], [10, 3, 10], "cap")]
        e[1]["faces"]["up"] = {"texture": "#foot", "uv": [0, 0, 16, 16]}                        # its broad step's top, framed
        # The stem of twisted iron, with a moulded knot low on it and a greater one high.
        e += [S([7, 3, 7], [9, 5, 9]), M([6.5, 5, 6.5], [9.5, 6, 9.5], "cap"), S([7, 6, 7], [9, 8, 9])]
        e += [M([6.5, 8, 6.5], [9.5, 9, 9.5], "band"), M([6.5, 9, 6.5], [9.5, 9.5, 9.5], "bead"), M([7, 9.5, 7], [9, 10, 9], "fillet")]
        # A gold gem front and back: on the high knot, or the low one where arms run out front and back over it.
        gy = 5.5 if crosswise else 9.0                                   # the gem's middle
        # Each gem is set into the knot: an open frame of iron two and a half pixels square, and the gem peeking out of it
        # inside its opening, the opening's walls in shadow.
        # The frame is the knot's own face, flush with it (and with the ring under it), so the knot's mouldings are cut
        # round it: where the frame stands they keep only their middle, the frame and gem in front of it.
        fx0, fx1, fy0, fy1 = 6.75, 9.25, gy - 1.25, gy + 1.25
        cut = []
        for part in e:
            kind = part.get("_kind")
            f, t = part["from"], part["to"]
            if not kind or t[0] <= fx0 or f[0] >= fx1 or t[1] <= fy0 or f[1] >= fy1 or f[2] >= 7.25 or t[2] <= 8.75:
                cut.append(part)
                continue
            if f[0] < fx0:
                cut.append(M([f[0], f[1], f[2]], [fx0, t[1], t[2]], kind))
            if t[0] > fx1:
                cut.append(M([fx1, f[1], f[2]], [t[0], t[1], t[2]], kind))
            cut.append(M([max(f[0], fx0), f[1], max(f[2], 7.25)], [min(t[0], fx1), t[1], min(t[2], 8.75)], kind))
        e = cut
        # (The gem peeks a quarter pixel out of its frame: only the part standing proud is built, so its sides never lie
        # in the plane of the frame's own walls.)
        for front, face_z, back_z, gem_z0, gem_z1 in (("north", 6.5, 7.25, 6.25, 6.5), ("south", 9.5, 8.75, 9.5, 9.75)):
            z0, z1 = min(face_z, back_z), max(face_z, back_z)
            lo, hi = gy - 0.75, gy + 0.75
            bars = [((6.75, gy + 0.75), (9.25, gy + 1.25), "down"), ((6.75, gy - 1.25), (9.25, gy - 0.75), "up"),
                    ((6.75, lo), (7.25, hi), "east"), ((8.75, lo), (9.25, hi), "west")]
            for (a, c), (b_, d), inner in bars:
                bar = el([a, c, z0], [b_, d, z1], "stem")
                # Its face reads the setting's own face (gem texture, u 4 to 6.5, v 0 to 2.5) where it lies in the frame.
                if front == "north":
                    u0, u1 = 4 + (9.25 - b_), 4 + (9.25 - a)
                else:
                    u0, u1 = 4 + (a - 6.75), 4 + (b_ - 6.75)
                bar["faces"][front] = {"texture": "#gem", "uv": [u0, gy + 1.25 - d, u1, gy + 1.25 - c]}
                bar["faces"][inner] = {"texture": "#gem", "uv": [5, 1, 6, 2]}                      # the opening's wall, dark
                e.append(bar)
            # The gem, peeking out, round: its corner texels cut away (a full-height middle, and either side of it a piece a
            # texel shorter at top and bottom), each piece reading its own part of the cut face (u 8 to 11, two to a pixel).
            stones = []
            for x0, x1, y0, y1 in ((7.5, 8.5, lo, hi), (7.25, 7.5, lo + 0.25, hi - 0.25), (8.5, 8.75, lo + 0.25, hi - 0.25)):
                piece = el([x0, y0, gem_z0], [x1, y1, gem_z1], "gem", emit=4, faces=(front, "east", "west", "up", "down"))
                u0, u1 = (8 + (8.75 - x1) * 2, 8 + (8.75 - x0) * 2) if front == "north" else (8 + (x0 - 7.25) * 2, 8 + (x1 - 7.25) * 2)
                for d in piece["faces"]:
                    piece["faces"][d]["uv"] = [u0, (hi - y1) * 2, u1, (hi - y0) * 2] if d == front else [1.5, 1.5, 2, 2]
                stones.append(piece)
            # Its table, standing a hair proud of the stone's face, catching the light.
            out = -0.1 if front == "north" else 0.1
            tz0, tz1 = sorted((gem_z0 if front == "north" else gem_z1, (gem_z0 if front == "north" else gem_z1) + out))
            table = el([7.75, gy - 0.25, tz0], [8.25, gy + 0.25, tz1], "gem", emit=4)
            for d in table["faces"]:
                table["faces"][d]["uv"] = [9, 1, 10, 2] if d == front else [9.5, 1.5, 10, 2]
            # Its seat: the frame's opening filled with iron behind it, so its cut corners show the setting, not a hole.
            seat = el([7.25, lo, min(face_z, back_z)], [8.75, hi, max(face_z, back_z)], "stem", faces=(front,))
            e += [seat] + stones + [table]
        if centre:
            top = next(y for x, y, z in slots if (x, z) == (8, 8)) - 1           # the neck rises to its tray
            e.append(S([7, 10, 7], [9, top, 9]))                                                   # the neck
            wide = 5 if len(slots) == 1 else 6
            e.append(M([wide, top, wide], [16 - wide, top + 1, 16 - wide], "cap"))                   # its tray
        else:
            e += [S([7, 10, 7], [9, 11, 9]), M([6.5, 11, 6.5], [9.5, 11.5, 9.5], "bead"),          # a finial instead
                  M([7.5, 11.5, 7.5], [8.5, 12.5, 8.5], "cap")]
        # The arms, twisted too: out from the high knot, up to their trays at their slots' heights. For each: its run out
        # (x0, z0, x1, z1), its upright and its tray across (x0, z0, x1, z1).
        arms = {(3, 8): ((4, 7, 7, 9), (2, 7, 4, 9), (1, 6, 5, 10)), (13, 8): ((9, 7, 12, 9), (12, 7, 14, 9), (11, 6, 15, 10)),
                (8, 3): ((7, 4, 9, 7), (7, 2, 9, 4), (6, 1, 10, 5)), (8, 13): ((7, 9, 9, 12), (7, 12, 9, 14), (6, 11, 10, 15))}
        for x, y, z in slots:
            if (x, z) in arms:
                run, up, tray = arms[(x, z)]
                e += [S([run[0], 8, run[1]], [run[2], 9, run[3]]), S([up[0], 8, up[1]], [up[2], y - 1, up[3]]),
                      M([tray[0], y - 1, tray[1]], [tray[2], y, tray[3]], "cap")]
    # A socket on each tray for its candle: a raised ring half a pixel thick round a dark hole two pixels across, so a
    # candle set in it stands in a cup (its foot hidden in the ring).
    for x, y, z in slots:
        e += [M([x - 1.5, y, z - 1.5], [x + 1.5, y + 1, z - 1], "cap"), M([x - 1.5, y, z + 1], [x + 1.5, y + 1, z + 1.5], "cap"),
              M([x - 1.5, y, z - 1], [x - 1, y + 1, z + 1], "cap"), M([x + 1, y, z - 1], [x + 1.5, y + 1, z + 1], "cap")]
        hole = el([x - 1, y + 0.02, z - 1], [x + 1, y + 0.02, z + 1], "trim", faces=("up",), uv=[0, 6, 1, 7])
        e.append(hole)
    for part in e:
        part.pop("_kind", None)
    # Clean undersides: the broad ones framed, the narrow ones the plain middle of it.
    for part in e:
        down = part["faces"].get("down")
        if not down or down["texture"] not in ("#trim", "#stem"):
            continue
        w, d = part["to"][0] - part["from"][0], part["to"][2] - part["from"][2]
        part["faces"]["down"] = {"texture": "#under", "uv": [0, 0, 16, 16] if min(w, d) >= 4 else [6, 6, 6 + w / 2, 6 + d / 2]}
    tex = {"trim": "candelabra_trim", "stem": "candelabra_stem", "foot": "candelabra_foot_top", "under": "candelabra_under",
           "gem": "candelabra_gem"}
    if with_candles:
        for x, y, z in slots:
            e.append(el([x - 1, y, z - 1], [x + 1, y + 3 + (1 if (x, z) == (8, 8) else 0), z + 1], "wax"))
            e += cross(x, z, e[-1]["to"][1], e[-1]["to"][1] + 3, 1.3, "flame", emit=15)
        tex.update({"wax": "candelabra_wax", "flame": "candle_flame"})
        e = banded(e, ("wax",))
    return tex, e


def tomb_underside():
    """The stone tomb's underside, one tile to each of its eight blocks: the tomb's own decor stone (its 16-texel tile,
    its colours, its lit top and left edge and dark bottom and right), with a carving cut into it as its walls' panels
    are: an inset square groove with a lit lip inside it, and a small diamond in the middle, so the bottom reads as a
    clean grid of panels in the same stone as the rest of it."""
    img = Image.open(os.path.join(TEX_DIR, "decor_stone.png")).convert("RGBA")
    cols = sorted(set(img.getdata()), key=lambda c: c[0] + c[1] + c[2])
    dark, light = cols[0], cols[-1]
    mid_dark = cols[len(cols) // 5]
    for y in range(16):
        for x in range(16):
            d = min(x, y, 15 - x, 15 - y)
            if d == 3:
                img.putpixel((x, y), mid_dark)                            # the groove
            elif d == 4 and (x == 4 or y == 4):
                img.putpixel((x, y), light)                               # its lit lip, upper left
            r = abs(x - 7.5) + abs(y - 7.5)
            if 1 <= r < 2:
                img.putpixel((x, y), dark if (x + y) >= 15 else light)    # the diamond, cut and lit
    return img


def candle_stand(part, with_candle=False):
    """A part of the dread candle stand (CandleStandBlock), which stacks, in its own plain square manner: a square foot
    plate raised on four small bun feet, a plain square post of dread iron ringed with moulded collars, and at its head
    a cup under a wide square drip pan with a small spike rising at each corner and a boss the candle stands on. Parts:
    single (a stand a block tall on its own), base (foot and post), middle (post with a collar), top (post, a collar,
    the cup and pan). with_candle: for its item, a plain candle burning on it."""
    M = lambda f, t, kind: moulding(f, t, kind, "wn")
    P = lambda y0, y1: el([7, y0, 7], [9, y1, 9], "bar")
    foot = [el([x, 0, z], [x + 1.5, 0.5, z + 1.5], "bar") for x in (4, 10.5) for z in (4, 10.5)]   # bun feet
    foot += [M([4, 0.5, 4], [12, 1.5, 12], "band"), M([5.5, 1.5, 5.5], [10.5, 2, 10.5], "bead"),
             M([6.5, 2, 6.5], [9.5, 2.5, 9.5], "fillet")]
    foot[4]["faces"]["up"] = {"texture": "#foot", "uv": [0, 0, 16, 16]}
    head = [M([6.5, 8.5, 6.5], [9.5, 9, 9.5], "bead"), M([6, 9, 6], [10, 10.5, 10], "band"),       # collar and cup
            M([4.5, 10.5, 4.5], [11.5, 11, 11.5], "cap"),                                          # the drip pan
            M([7, 11, 7], [9, 12, 9], "cap")]                                                      # the boss
    for x in (4.5, 10.75):                                                                         # spikes at its corners
        for z in (4.5, 10.75):
            head.append(el([x, 11, z], [x + 0.75, 12.5, z + 0.75], "stem"))
    collar = lambda y: M([6.5, y, 6.5], [9.5, y + 1, 9.5], "band")
    if part == "single":
        e = foot + [P(2.5, 8.5), collar(5)] + head
    elif part == "base":
        e = foot + [P(2.5, 16), collar(9)]
    elif part == "middle":
        e = [P(0, 16), collar(7.5)]
    else:
        e = [P(0, 8.5), collar(3.5)] + head
    for p_ in e:                                                                                    # clean undersides
        down = p_["faces"].get("down")
        if down and down["texture"] in ("#trim", "#bar", "#stem"):
            w, d = p_["to"][0] - p_["from"][0], p_["to"][2] - p_["from"][2]
            p_["faces"]["down"] = {"texture": "#under", "uv": [0, 0, 16, 16] if min(w, d) >= 4 else [6, 6, 6 + w / 2, 6 + d / 2]}
    tex = {"bar": "dread_iron", "trim": "candelabra_trim", "stem": "candelabra_stem", "foot": "candelabra_foot_top",
           "under": "candelabra_under"}
    if with_candle:
        e.append(el([7, 12, 7], [9, 16, 9], "wax"))
        e += cross(8, 8, 16, 19, 1.3, "flame", emit=15)
        tex.update({"wax": "candelabra_wax", "flame": "candle_flame"})
        e = banded(e, ("wax",))
    return tex, e


# ---- Crossing the Dark Between: the Lucid Spool's walkway, the Dream Glider, the spool itself ----
def dream_bridge_texture():
    """The dream-thread walkway: a weave of deep violet threads, the threads crossing it lit where they cross, a few
    brighter strands running through it, pixel art in a handful of violets."""
    violets = [(34, 14, 58), (58, 26, 96), (92, 48, 150), (140, 90, 210), (210, 170, 255)]
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = 1
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                k = 2                                                             # the weave
            if (x + y) % 4 == 0 and (x - y) % 4 == 0:
                k = 3                                                             # lit where its threads cross
            if x in (0, 15) or y in (0, 15):
                k = 0 if (x + y) % 2 else 1                                       # its frayed edge
            if (x * 7 + y * 3) % 23 == 0:
                k = 4                                                             # a bright strand
            img.putpixel((x, y), violets[k] + (255,))
    return img


def dream_glider_icon():
    """The Dream Glider's icon: two pale moth wings of cloth spread on a dark sleepwood cross, eyespots on them."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    wing = [(150, 140, 168), (178, 168, 194), (204, 196, 216)]
    frame = [(40, 34, 44), (62, 54, 66)]
    for y in range(16):
        for x in range(16):
            dx, dy = abs(x - 7.5), y - 5.5
            if dy >= -4 and dy <= 4 and dx <= 7 and (dx / 7.5) ** 2 + (dy / 5) ** 2 <= 1 and dx > 0.5:
                k = 2 if dy < -1 else 1 if dy < 2 else 0
                if (x in (3, 12)) and y == 5:
                    img.putpixel((x, y), (96, 70, 150, 255))                      # an eyespot on each wing
                    continue
                img.putpixel((x, y), wing[k] + (255,))
    for y in range(1, 15):
        img.putpixel((7, y), frame[1] + (255,))                                   # the spar
        img.putpixel((8, y), frame[0] + (255,))
    for x in range(1, 15):
        img.putpixel((x, 3), frame[1] + (255,))                                   # the cross piece
    img.putpixel((7, 15), frame[0] + (255,))
    return img


def lucid_spool_icon():
    """The Lucid Spool's icon: a spool of pale sleepwood, its flanges top and bottom, wound with glowing violet thread,
    a loose end trailing from it."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    wood = [(98, 92, 104), (128, 122, 134), (160, 154, 166)]
    thread = [(100, 50, 170), (150, 90, 230), (210, 170, 255)]
    for y in range(3, 13):
        for x in range(4, 12):
            if y in (3, 12):
                img.putpixel((x, y), wood[2 if y == 3 else 0] + (255,))           # its flanges
            elif 5 <= x <= 10:
                k = 2 if x == 6 else 0 if x == 10 else 1
                if (y + x) % 3 == 0:
                    k = max(0, k - 1)                                             # the windings
                img.putpixel((x, y), thread[k] + (255,))
    for y in range(2, 14):
        for x in (3, 12):
            if y in (2, 13):
                continue
            img.putpixel((x, y), wood[1] + (255,)) if y in (3, 12) else None
    for x, y in ((11, 9), (12, 10), (13, 11), (13, 12), (14, 13)):
        img.putpixel((x, y), thread[2] + (255,))                                  # its loose end
    return img


def candelabra_under():
    """The candelabra's undersides: its iron in shadow, soft and clean, in close blended tones, with a lit arris round the edge, a faint
    chisel line inside it and a small round mark in the middle (a broad underside wears all of it, a narrow one only
    its plain middle)."""
    R = iron_ramp()

    f = fbm(6131)

    def px(x, y):
        level = 2.45 + (f(x, y) - 0.5) * 0.15                  # smooth, only the faintest mottle
        d = min(x, y, 31 - x, 31 - y)
        if d == 0:
            level += 0.35
        elif d == 3:
            level -= 0.2
        if 3.5 <= math.hypot(x - 15.5, y - 15.5) < 4.5:
            level -= 0.2
        lo = int(level)
        return mix(R[lo], R[lo + 1], level - lo)               # close tones, blended, not stepped
    return basin_canvas(px)


def spill_flow(frames=8):
    """The spilling blood's own animation: dark red streaks running down, and a band of glow falling down through them
    two texels a frame (top row first in the texture is the top of each run), fading in a tail above it."""
    rng = random.Random(721)
    streak = [rng.choice((0, 0, 1)) for _ in range(16)]
    img = Image.new("RGBA", (16, 16 * frames))
    # The pool's own colours (tears_surface): deep red to a muted crimson, the glow only a little past it.
    dark, glow = (70, 6, 16), (132, 16, 32)
    for f in range(frames):
        band = (f * 2) % 16                          # the glow's row this frame: falling as frames go on
        for y in range(16):
            behind = (band - y) % 16                 # how far above the glow (its soft tail trails behind it)
            light = math.exp(-behind / 4.0) if behind < 12 else 0.0
            ahead = (y - band) % 16
            light = max(light, 0.6 * math.exp(-ahead / 1.8) if ahead < 6 else 0.0)  # a soft leading edge
            for x in range(16):
                base = 0.06 if streak[x] else 0.0
                img.putpixel((x, f * 16 + y), mix(dark, glow, min(1.0, base + 0.7 * light)) + (255,))
    return img


def spill():
    """Blood brimming over the basin: here and there it runs over the rim (a sheet across its top), down the outside
    of the rim and the bowl in runs of different lengths, one past the bowl's foot to hang as a drop and land in a
    small pool on the plinth. All of the tears' glowing texture, a hair off the stone so nothing flickers."""
    T = lambda f, t, faces, uv: el(f, t, "spill", emit=9, faces=faces, uv=uv)
    e = []
    # Front (north): a long run that falls to a drop and a pool, and a short one.
    e += [T([6, 15.03, 2], [7, 15.03, 3], ("up",), [6, 2, 7, 3]),
          T([6, 11, 1.97], [7, 15.03, 1.97], ("north",), [6, 1, 7, 5]),
          T([6.2, 9.4, 1.75], [6.8, 11, 2.0], ("north", "east", "west", "down"), [6, 6, 7, 8]),
]
    e += [T([10, 15.03, 2], [11, 15.03, 3], ("up",), [10, 2, 11, 3]),
          T([10, 12.5, 1.97], [11, 15.03, 1.97], ("north",), [10, 1, 11, 3.5]),
          T([10.2, 12.0, 1.8], [10.8, 12.5, 2.0], ("north", "east", "west", "down"), [10, 4, 11, 5])]
    # Left (west): over the rim and down the side, ending in a bead.
    e += [T([2, 15.03, 7], [3, 15.03, 8], ("up",), [2, 7, 3, 8]),
          T([1.97, 11.5, 7], [1.97, 15.03, 8], ("west",), [7, 1, 8, 4.5]),
          T([1.8, 11.0, 7.2], [2.0, 11.6, 7.8], ("west", "north", "south", "down"), [7, 5, 8, 6])]
    # Right (east): a short run.
    e += [T([13, 15.03, 9], [14, 15.03, 10], ("up",), [13, 9, 14, 10]),
          T([14.03, 13, 9], [14.03, 15.03, 10], ("east",), [9, 1, 10, 3])]
    return e


def weeping_basin():
    """A font carved from the labyrinth's own stone, so it sits among the hedges, piers and paving as one of them: a
    two-step plinth and corner buttresses of polished dreamstone, a stem of the fluted dreamstone pillar with a tear
    carved in its front (basin_stem), a bowl of dreamstone bricks with chamfered corners under a rim of chiseled
    dreamstone, brimming with the garden's glowing tears. On whole pixels with no uv given, so every face takes the
    block textures one texel to a pixel, exactly as the blocks round it do."""
    P = lambda f, t: el(f, t, "polished")
    # The plinth: big formed bricks in soft joints (basin_base).
    # Where the drip lands the blood is soaked into the stone itself: the faces it falls on wear stained copies of
    # the base texture (basin_base_stained_top / _front), the same texels, so the bricks run on through it.
    e = [el([1, 0, 1], [15, 2, 15], "base", up="stain_top", north="stain_front"), el([2, 2, 2], [14, 3, 14], "base", up="stain_top", north="stain_upper")]
    stem = el([5, 3, 5], [11, 10, 11], "pillar", north="stem", up="pillar_top", down="pillar_top")
    stem["faces"]["north"]["uv"] = [5, 6, 11, 13]
    e.append(stem)
    # The tear's glow, over the carved tear on the stem's front: the same texels, lit by itself, pulsing.
    e.append(el([5, 3, 4.97], [11, 10, 4.97], "tear_glow", emit=12, faces=("north",), uv=[5, 6, 11, 13]))
    # The corner posts: brick piers like the stem, their tops its carved end.
    e += [el([x, 3, z], [x + 1, 7, z + 1], "pillar", up="pillar_top", down="pillar_top") for x, z in ((4, 4), (11, 4), (4, 11), (11, 11))]
    e.append(P([4, 10, 4], [12, 11, 12]))
    e += [el(f, t, "bricks") for f, t in (([2, 11, 3], [14, 14, 13]), ([3, 11, 2], [13, 14, 3]), ([3, 11, 13], [13, 14, 14]))]
    e += [el(f, t, "rim") for f, t in (([2, 14, 2], [14, 15, 3]), ([2, 14, 13], [14, 15, 14]),
                                       ([2, 14, 3], [3, 15, 13]), ([13, 14, 3], [14, 15, 13]))]
    e.append(el([3, 14.5, 3], [13, 14.5, 13], "tears", emit=9, faces=("up",)))
    # Seen from below it is plain: every stone face turned down (the plinth's foot, the bowl's overhang, the step and
    # the posts' ends) is the same quiet stone in shadow, with none of the brickwork that the sides carry.
    for part in e:
        down = part["faces"].get("down")
        if down and down["texture"] in ("#base", "#polished", "#bricks", "#rim", "#pillar", "#pillar_top"):
            down["texture"] = "#under"
    e += spill()
    return {"under": "basin_underside", "polished": "basin_polished", "pillar": "basin_pillar", "pillar_top": "basin_pillar_top",
            "stem": "basin_stem", "bricks": "basin_bricks", "rim": "basin_rim", "tears": "weeping_tears", "spill": "weeping_spill",
            "still": "weeping_spill_still",
            "tear_glow": "basin_tear_glow", "base": "basin_base",
            "stain_top": "basin_base_stained_top", "stain_front": "basin_base_stained_front",
            "stain_upper": "basin_base_stained_upper"}, e


# The statue's skin: one 64-texel atlas laid out as a player's skin is, a texel to a pixel of the player's model. Each
# part is a box (w across, h tall, d deep) whose faces sit round its corner (u0, v0): its top and bottom over, then its
# west side, front, east side and back in a row.
STATUE_PARTS = {"head": (0, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4), "leg": (0, 16, 4, 12, 4),
                "upper_arm": (40, 16, 4, 6, 4), "forearm": (40, 26, 4, 10, 4)}


def skin_faces(part):
    """Where each face of a statue part lies in its skin: {direction: (x, y, w, h)} in texels."""
    u0, v0, w, h, d = STATUE_PARTS[part]
    return {"up": (u0 + d, v0, w, d), "down": (u0 + d + w, v0, w, d), "west": (u0, v0 + d, d, h),
            "north": (u0 + d, v0 + d, w, h), "east": (u0 + d + w, v0 + d, d, h), "south": (u0 + 2 * d + w, v0 + d, w, h)}


def skinned(f, t, part, rot=None):
    """A statue part's box wearing its region of the skin, each face its own."""
    e = el(f, t, "skin", rot=rot)
    for direction, (x, y, w, h) in skin_faces(part).items():
        e["faces"][direction]["uv"] = [x / 4, y / 4, (x + w) / 4, (y + h) / 4]
    return e


def statue_skin():
    """The mourner's skin, painted face by face as a Minecraft character's is, in the labyrinth's own stone (the stone
    ramp, in half steps, so every pixel reads): a deep hood with a seam and a lit brim over a carved face in its shadow
    (a lit brow, shut eyes, a nose lit on its left, a mouth turned down, a red tear run from one eye); a robe under a
    capelet, a red-jewelled clasp at the throat and a belt with a buckle, its folds falling in creases that wander and
    stop as cloth does; sleeves with a lit cuff and the hands bare at their ends, fingers marked; the robe's skirt over
    the legs, a hem and the toes of its feet. Light from the upper left on every face: lit top edges, shadowed feet;
    weathering through all of it: a grain of stone, pits, a crack or two, and moss crept up from the ground."""
    R = stone_ramp()
    W = 64
    lv = {}
    colour = {}

    def grain(x, y):
        h = (x * 73856093 ^ y * 19349663 ^ 0x5bd1) & 0xFFFF
        return (-0.25, 0.0, 0.0, 0.0, 0.25, 0.0, -0.25, 0.25)[h % 8]

    def put(x, y, level):
        lv[(x, y)] = level

    def cloth(fx, fy, w, h, seed, base=2.6):
        """Cloth falling down a face: wandering creases (dark, their lit side to their left), a lit top edge, a
        shadowed foot."""
        rng = random.Random(seed)
        creases = []
        c = rng.randrange(1, 3)
        while c < w - 1:
            creases.append((c, rng.randrange(0, max(1, h // 3)), rng.randrange(h * 2 // 3, h + 1), rng.random() * 6))
            c += rng.choice((2, 3, 3, 4))
        for j in range(h):
            for i in range(w):
                level = base + 0.35 * (1 - j / max(1, h - 1)) - 0.35 * (j / max(1, h - 1))
                for cx, top, bottom, phase in creases:
                    if top <= j < bottom:
                        x = cx + (1 if math.sin(j * 0.7 + phase) > 0.6 else 0)
                        if i == x:
                            level -= 0.7
                        elif i == x - 1:
                            level += 0.45
                if j == 0:
                    level += 0.4
                if j == h - 1:
                    level -= 0.4
                put(fx + i, fy + j, level)

    def flat(fx, fy, w, h, level):
        for j in range(h):
            for i in range(w):
                put(fx + i, fy + j, level)

    # ---- The head: a hood, and the face in it ----
    F = skin_faces("head")
    x, y, w, h = F["up"]
    cloth(x, y, w, h, 901, 2.5)
    for j in range(h):                                              # the hood's seam over its crown
        put(x + 3, y + j, 3.2)
        put(x + 4, y + j, 1.8)
    for side, seed in (("west", 902), ("east", 903), ("south", 904)):
        cloth(*F[side], seed, 2.4)
    x, y, w, h = F["south"]
    for j in range(h):                                              # its seam down the back
        put(x + 3, y + j, 3.0)
        put(x + 4, y + j, 1.7)
    x, y, w, h = F["down"]
    flat(x, y, w, h, 1.4)
    x, y, w, h = F["north"]                                         # the face, deep in the hood
    for j in range(h):
        for i in range(w):
            if j == 0:
                level = 3.5                                         # the hood's brim, catching the light
            elif i in (0, 7):
                level = 2.4 if i == 0 else 1.9                      # the hood's sides round the face
            elif j == 1:
                level = 1.6                                         # the brim's shadow over the brow
            else:
                level = 3.1 - 0.15 * (j - 2) - (0.4 if i == 6 else 0.0)   # the face, pale, its right side in shade
            put(x + i, y + j, level)
    for i in range(1, 7):
        put(x + i, y + 2, 3.6 if i not in (3, 4) else 3.3)          # the brow
    for i in (1, 2, 5, 6):
        put(x + i, y + 3, 1.5)                                      # shut eyes, sunk
        put(x + i, y + 4, 2.6)
    put(x + 3, y + 4, 3.7)                                          # the nose, lit on its left
    put(x + 4, y + 4, 2.3)
    put(x + 3, y + 5, 3.3)
    put(x + 4, y + 5, 2.1)
    for i in (2, 5):
        put(x + i, y + 7, 1.8)                                      # the mouth, turned down
    for i in (3, 4):
        put(x + i, y + 6, 1.6)
    for j in range(4, 8):                                           # a tear run from its left eye
        colour[(x + 5, y + j)] = mix((150, 14, 34), (98, 6, 22), (j - 4) / 3)
    colour[(x + 5, y + 4)] = (216, 40, 62)

    # ---- The body: a robe under a capelet ----
    F = skin_faces("body")
    for side, seed in (("west", 911), ("east", 912), ("south", 913)):
        cloth(*F[side], seed, 2.5)
    x, y, w, h = F["south"]
    for j in range(4):                                              # the hood's drape down the back
        for i in range(w):
            if abs(i - 3.5) < 4 - j:
                put(x + i, y + j, 2.9 - 0.25 * j - (0.5 if abs(i - 3.5) > 2.5 - j else 0.0))
    x, y, w, h = F["north"]
    cloth(x, y, w, h, 914, 2.5)
    for i in range(w):
        put(x + i, y, 3.4)                                          # the collar
        for j in (1, 2, 3):
            put(x + i, y + j, 2.9 - 0.2 * j + (0.3 if i < 3 else 0.0))   # the capelet over the shoulders
        put(x + i, y + 4, 1.8)                                      # its shadowed edge
        put(x + i, y + 7, 2.1)                                      # the belt
        put(x + i, y + 8, 1.6)
    for j in (1, 2):
        put(x + 3, y + j, 1.4)                                      # the throat, in shadow
        put(x + 4, y + j, 1.3)
    colour[(x + 3, y + 3)] = (240, 80, 92)                          # the clasp: a red jewel, lit on its left
    colour[(x + 4, y + 3)] = (150, 14, 38)
    put(x + 3, y + 7, 3.7)                                          # the buckle
    put(x + 4, y + 7, 3.2)
    put(x + 3, y + 8, 2.8)
    put(x + 4, y + 8, 1.4)
    x, y, w, h = F["up"]
    flat(x, y, w, h, 3.0)
    for i in (3, 4):
        put(x + i, y + 1, 1.4)                                      # where the neck rises
        put(x + i, y + 2, 1.6)
    x, y, w, h = F["down"]
    flat(x, y, w, h, 1.4)

    # ---- The arms: sleeves, a cuff, the hands bare at the end ----
    F = skin_faces("upper_arm")
    for side, seed in (("west", 921), ("north", 922), ("east", 923), ("south", 924)):
        cloth(*F[side], seed, 2.5)
    x, y, w, h = F["up"]
    flat(x, y, w, h, 3.0)
    x, y, w, h = F["down"]
    flat(x, y, w, h, 1.5)
    F = skin_faces("forearm")
    for side, seed in (("west", 931), ("north", 932), ("east", 933), ("south", 934)):
        x, y, w, h = F[side]
        cloth(x, y + 5, w, h - 5, seed, 2.4)                        # the sleeve, below
        for i in range(w):
            for j in range(3):
                put(x + i, y + j, 3.3 - 0.2 * j - (0.6 if i % 2 == 1 and j < 2 else 0.0))   # the hand, its fingers
            put(x + i, y + 3, 3.7)                                  # the cuff, lit
            put(x + i, y + 4, 1.8)                                  # and its shadow on the sleeve
    x, y, w, h = F["up"]
    for j in range(h):
        for i in range(w):
            put(x + i, y + j, 3.2 - (0.7 if (i + j) % 3 == 0 else 0.0))   # the knuckles of the hands
    x, y, w, h = F["down"]
    flat(x, y, w, h, 1.3)

    # ---- The legs: the robe's skirt, a hem, the toes ----
    F = skin_faces("leg")
    for side, seed in (("west", 941), ("north", 942), ("east", 943), ("south", 944)):
        x, y, w, h = F[side]
        cloth(x, y, w, h - 2, seed, 2.4)
        for i in range(w):
            put(x + i, y + h - 2, 3.1)                              # the hem, lit
            put(x + i, y + h - 1, 1.6)
    x, y, w, h = F["north"]
    put(x + 1, y + h - 1, 3.0)                                      # the toes under the hem
    put(x + 2, y + h - 1, 2.6)
    for side in ("up", "down"):
        flat(*F[side], 1.5)

    # ---- Weathering: pits and cracks, and moss crept up from the ground over the hem ----
    rng = random.Random(951)
    keys = sorted(lv)
    for _ in range(22):
        x, y = rng.choice(keys)
        if (x, y) not in colour:
            lv[(x, y)] -= 0.8
            if (x + 1, y + 1) in lv:
                lv[(x + 1, y + 1)] += 0.4
    for x0, y0, steps in ((22, 25, 4), (52, 22, 3), (26, 10, 3)):    # cracks, on the body, an arm, the hood
        for k in range(steps):
            if (x0 + (k + 1) // 2, y0 + k) in lv:
                lv[(x0 + (k + 1) // 2, y0 + k)] = 1.3
    moss = [(34, 52, 54), (47, 69, 69), (62, 88, 84)]
    for x, y in keys:
        if 16 <= y < 32 and x < 16 and y >= 28 and rng.random() < (0.35 if y >= 30 else 0.15):
            colour[(x, y)] = moss[rng.randrange(3)]                 # moss on the skirt's hem

    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    for (x, y), level in lv.items():
        level = round((level + 0.4 + grain(x, y)) * 2) / 2   # a step lighter, as the stone round it
        level = max(0.0, min(5.0, level))
        k = min(4, int(level))
        img.putpixel((x, y), mix(R[k], R[k + 1], level - k) + (255,))
    for (x, y), c in colour.items():
        img.putpixel((x, y), tuple(c) + (255,))
    return img


def statue_pedestal():
    """The mourner's pedestal, framed as the planter is (planter_trim): a toe and a bead over it, and over them the die: a sunk panel on every side carved with a row of runes round a red tear (statue_pedestal's panel strip,
    by explicit uv), held between four corner blocks standing proud of it, each carved with a rosette; and over all a
    cornice overhanging the die, that the figure stands on."""
    def die(f, t, region):
        e = el(f, t, "ped")
        for d in e["faces"]:
            e["faces"][d]["uv"] = region
        return e

    e = [moulding([0.5, 0, 0.5], [15.5, 0.5, 15.5], "toe", "wn"),                          # the toe
         moulding([1, 0.5, 1], [15, 1, 15], "bead", "wn"),                                  # a bead over it
         die([2, 1, 2], [14, 2.5, 14], [0, 0.5, 12, 2])]                                   # the die, its panels
    for x, z in ((1.5, 1.5), (12.5, 1.5), (1.5, 12.5), (12.5, 12.5)):                       # the corner blocks
        e.append(die([x, 1, z], [x + 2, 2.5, z + 2], [0, 8, 2, 9.5]))
    cornice = moulding([1, 2.5, 1], [15, 3, 15], "bead", "wn")                              # the cornice, overhanging,
    cornice["faces"]["up"] = {"texture": "#ptop", "uv": [1, 1, 15, 15]}                     # its top carved
    e.append(cornice)
    # Seen from below it is plain: every face turned down is the same quiet stone in shadow, read where it lies.
    for part in e:
        part["faces"]["down"] = {"texture": "#under"}
    e[0]["faces"]["down"] = {"texture": "#pbottom", "uv": [1, 1, 15, 15]}                  # but its foot: the top's carving, quieter
    return e


def statue_underside():
    """The pedestal's underside: its stone in shadow, only the statue's pixel grain on it and, faint, a soft arris
    round the toe's edge, a chisel line inside it, and a mason's mark in its middle (a ring with a tear's stroke)."""
    R = stone_ramp()

    def px(x, y):
        level = 1.9 + pixel_grain(x, y, 0x2b3d) * 0.5
        d = min(x - 1, y - 1, 30 - x, 30 - y)                       # in from the toe's edge (its face reads texels 1 to 30)
        if d == 0:
            level += 0.5                                            # a soft arris round its edge
        elif d == 2:
            level -= 0.35                                           # a faint chisel line inside it
        r = math.hypot(x - 15.5, y - 15.5)
        if 3.5 <= r < 4.5:
            level -= 0.35                                           # the mason's mark in the middle: a faint ring,
        if r < 3.5 and abs(x - 15.5) < 1 and y >= 13:
            level -= 0.35                                           # a tear's stroke falling in it
        return stepped(R, level)
    return basin_canvas(px)


def statue_pedestal_top(under=False):
    """The pedestal's top (its cornice's up face reads texels 2 to 30 both ways, two to a pixel, north at the top and
    west at the left, the light from the north-west): a bevelled rim, lit along its north and west edges and shadowed
    along its south and east; a sunk groove framing it, its inner wall lit; a carved ring round the figure's feet,
    notched with runes where it crosses the compass, a lit lip inside it; flagstone joints within the ring; a carved
    mark in each corner; and a few drops of the statue's red tears fallen in front of its feet. Pixel art, in the
    statue's grain and half steps. under: the same carving for its underside, quieter: half as deep, in shadow, no
    tears."""
    R = stone_ramp()
    f = fbm(971)
    lv, red = {}, {}
    c = 15.5                                                                    # the middle, in texels
    for y in range(32):
        for x in range(32):
            i, j = x - 2, y - 2                                                 # within the face, 0..27
            d = min(i, j, 27 - i, 27 - j)                                       # in from its edge
            level = 2.6 + (f(x, y) - 0.5) * 0.5
            if d <= 0:
                level = 3.7 if (i == 0 or j == 0) and i < 27 and j < 27 else 1.9          # the rim's bevel
            elif d == 1:
                level = 3.1
            elif d == 3:
                level = 1.5                                                     # the groove
            elif d == 4:
                level = 3.3 if (i == 4 or j == 4) else 2.1                      # its walls: lit inside on the north-west
            r = math.hypot(x + 0.5 - c - 0.5, y + 0.5 - c - 0.5)
            if 7.6 <= r < 8.6:
                level = 1.2                                                     # the carved ring
            elif 6.6 <= r < 7.6:
                level = 3.7 if (x + y) < 2 * c + 1 else 2.0                     # its inner lip
            elif 8.6 <= r < 9.6:
                level = 2.0 if (x + y) < 2 * c + 1 else 3.3                     # its outer wall, lit on the south-east
            elif r < 6.6 and (x == 16 or y == 16):
                level -= 0.7                                                    # flagstone joints inside it
            if r >= 9.6 and 6 <= d <= 8:
                if min(abs(i - j), abs(i + j - 27)) == 0:
                    level = 1.2                                                 # a carved mark in each corner
                elif abs(i - j) == 1 or abs(i + j - 27) == 1:
                    level = 3.5 if (i < j) == (i < 14) else 2.2                 # its lit edge
            lv[(x, y)] = level
    for ang in (0, 90, 180, 270):                                               # rune notches on the ring
        a = math.radians(ang)
        for k in (-1, 0, 1):
            x = int(round(c + 0.5 + 8.1 * math.cos(a) + k * math.sin(a)))
            y = int(round(c + 0.5 + 8.1 * math.sin(a) - k * math.cos(a)))
            if (x, y) in lv:
                lv[(x, y)] = 3.6 if k == 0 else 2.4
    if not under:
        for x, y in ((13, 7), (14, 8), (18, 6), (17, 9), (12, 9)):              # tears fallen at its front (north)
            red[(x, y)] = (150, 14, 34) if (x + y) % 2 else (98, 6, 22)
        red[(14, 8)] = (216, 40, 62)
    img = Image.new("RGBA", (32, 32))
    for (x, y), level in lv.items():
        if under:
            level = 2.6 + (level - 2.6) * 0.75 - 0.6                           # shallower, and in shadow
        level += 0.4 + pixel_grain(x, y, 0x51e7)
        if ((x * 2654435761 ^ y * 40503 ^ 0x51e7) >> 7) % 23 == 0:
            level -= 0.75                                                       # a pit
        img.putpixel((x, y), stepped(R, level) + (255,))
    for (x, y), col in red.items():
        img.putpixel((x, y), col + (255,))
    return img


def statue_pedestal_texture():
    """The pedestal's carving, in the stone ramp: in its top rows (0 to 3, 24 across: the die's panel, two texels to a
    pixel) a sunk panel, shadowed under the cornice, a row of runes cut in it (each lit on its upper left where the
    light catches the cut's far wall, dark where it sinks), a red tear in its middle, its foot lit; and at rows 16 to
    18 (a corner block's face) a rosette in a bevelled square. The rest is plain stone, for the faces no one sees."""
    R = stone_ramp()
    f = fbm(961)
    lv = {(x, y): 2.6 + (f(x, y) - 0.5) * 0.5 for x in range(32) for y in range(32)}
    red = {}
    for x in range(24):
        lv[(x, 0)] = 1.6                                                    # under the cornice
        lv[(x, 1)] = lv[(x, 2)] = 2.3                                       # the sunk field
        lv[(x, 3)] = 3.4                                                    # its lit foot
    runes = [((0, 0), (1, 1)), ((0, 1), (1, 0)), ((0, 0), (0, 1), (1, 1)), ((1, 0), (0, 1), (1, 1))]
    for k, x0 in enumerate((2, 6, 15, 19)):
        for dx, dy in runes[k % 4]:
            lv[(x0 + dx, 1 + dy)] = 1.3                                     # the cut
            if (x0 + dx - 1, 1 + dy) in lv and lv[(x0 + dx - 1, 1 + dy)] > 1.5:
                lv[(x0 + dx - 1, 1 + dy)] = 3.2                             # its lit far wall
    red[(11, 1)], red[(12, 1)] = (216, 40, 62), (150, 14, 34)              # the tear
    red[(11, 2)], red[(12, 2)] = (150, 14, 34), (98, 6, 22)
    rosette = ((3.5, 3.5, 3.5, 1.7), (3.5, 3.8, 2.2, 1.7), (2.6, 1.7, 1.7, 1.5))  # 4 across, 3 down
    for j, row in enumerate(rosette):
        for i, level in enumerate(row):
            lv[(i, 16 + j)] = level
    img = Image.new("RGBA", (32, 32))
    for (x, y), level in lv.items():
        level += 0.4 + pixel_grain(x, y, 0x3c19)                                # pixel art, a step lighter, as the statue
        if ((x * 2654435761 ^ y * 40503 ^ 0x3c19) >> 7) % 17 == 0 and y > 3:
            level -= 0.75                                                       # a pit
        img.putpixel((x, y), stepped(R, level) + (255,))
    for (x, y), c in red.items():
        img.putpixel((x, y), c + (255,))
    return img


def mourning_statue():
    """A mourner in stone, built as a player is: head, body, two arms and two legs in a player's proportions (scaled
    to 0.9 to stand on its plinth within two blocks), its head bowed and its arms raised toward its face, all wearing
    one skin (statue_skin, laid out as a player's is). The whole figure is the lower half's model (it may reach up to
    32 pixels); the upper half draws nothing of its own."""
    k = 0.875                                     # a pixel of the player's model, in the block's pixels
    foot = 3.0                                    # it stands on its pedestal (and stays within two blocks)
    cx, cz = 8.0, 8.0

    def box(x0, y0, z0, x1, y1, z1):              # from player-model pixels (x, z from the figure's middle) to block
        return [cx + x0 * k, foot + y0 * k, cz + z0 * k], [cx + x1 * k, foot + y1 * k, cz + z1 * k]

    e = statue_pedestal()
    # Legs, side by side, and the body over them.
    for x0 in (-4, 0):
        e.append(skinned(*box(x0, 0, -2, x0 + 4, 12, 2), "leg"))
    e.append(skinned(*box(-4, 12, -2, 4, 24, 2), "body"))
    # The arms, bent as a mourner's are, weeping into its hands: from each shoulder the upper arm hangs forward 40
    # degrees to the elbow, and from the elbow the forearm rises nearly upright, leaning in 14 degrees, to the hands
    # held against the sides of the bowed face. The forearm reaches a pixel back past the elbow into the upper arm, so
    # the joint is closed and reads as a bend. (A positive turn about x swings what is below the pivot forward, what
    # is above it back; any angle is allowed.)
    upper, fore = 40.0, -14.0
    elbow_y, elbow_z = 22 - 6 * math.cos(math.radians(upper)), -6 * math.sin(math.radians(upper))
    for x0, fx0 in ((-8, -7), (4, 3)):
        e.append(skinned(*box(x0, 16, -2, x0 + 4, 22, 2), "upper_arm",
                         rot={"origin": [cx + (x0 + 2) * k, foot + 22 * k, cz], "axis": "x", "angle": upper}))
        e.append(skinned(*box(fx0, elbow_y - 1, elbow_z - 2, fx0 + 4, elbow_y + 8, elbow_z + 2), "forearm",
                         rot={"origin": [cx + (fx0 + 2) * k, foot + elbow_y * k, cz + elbow_z * k], "axis": "x", "angle": fore}))
    # The head, bowed forward from the neck: its face to the front, toward its hands.
    e.append(skinned(*box(-4, 24, -4, 4, 32, 4), "head", rot={"origin": [cx, foot + 24 * k, cz], "axis": "x", "angle": -22.5}))
    return {"skin": "statue_skin", "trim": "statue_trim", "ped": "statue_pedestal", "ptop": "statue_pedestal_top", "pbottom": "statue_pedestal_bottom",
            "under": "statue_underside"}, e, []


def dreamstone_urn():
    """An urn of the labyrinth's soft dreamstone, framed as the planter is: a moulded foot, a short stem, a bead under a
    chamfered belly of soft stone with a carved frieze round it (urn_body), a bead at its shoulder, a neck, a moulded
    lip, a lid with a knob, and a handle carved either side."""
    e = [moulding([5, 0, 5], [11, 1, 11], "band", "wn"),                                               # the foot
         moulding([6, 1, 6], [10, 1.5, 10], "fillet", "wn"),
         el([6.5, 1.5, 6.5], [9.5, 2.5, 9.5], "body", down="under"),                                   # the stem
         moulding([5.5, 2.5, 5.5], [10.5, 3, 10.5], "bead", "wn"),
         # The belly, its corners chamfered: two crossed boxes, and a third over and under them for its shoulders.
         el([4, 3.5, 5], [12, 6.5, 11], "body"), el([5, 3.5, 4], [11, 6.5, 12], "body"),
         el([5, 3, 5], [11, 7, 11], "body", down="under"),
         moulding([5.5, 7, 5.5], [10.5, 7.5, 10.5], "bead", "wn"),                                     # its shoulder
         el([6.5, 7.5, 6.5], [9.5, 9.5, 9.5], "body"),                                                  # the neck
         moulding([5.5, 9.5, 5.5], [10.5, 10.5, 10.5], "cap", "wn"),                                    # the lip
         moulding([6, 10.5, 6], [10, 11, 10], "fillet", "wn"),                                          # the lid
         moulding([6.5, 11, 6.5], [9.5, 11.5, 9.5], "bead", "wn"),
         moulding([7.5, 11.5, 7.5], [8.5, 12.5, 8.5], "cap", "wn"),                                    # its knob
         # The handles, carved from it, from the belly up to the shoulder.
         moulding([3, 4.5, 7.5], [4, 8, 8.5], "band", "w"), moulding([12, 4.5, 7.5], [13, 8, 8.5], "band", "e"),
         moulding([3, 8, 7.5], [5.5, 9, 8.5], "cap", "w"), moulding([10.5, 8, 7.5], [13, 9, 8.5], "cap", "e")]
    return {"body": "urn_body", "trim": "planter_trim", "under": "basin_underside"}, e


def urn_body():
    """The urns' stone, worn and broken: soft dreamstone with a light grain, lightly mottled, faint throwing rings, grime
    rising from the ground; near the shoulder a chevron frieze, low down a row of studs; round the belly (rows 18 to
    24) a carved band of runes, each glyph cut dark with its lit far wall, between a groove over it and a lit lip
    under it; hairline cracks wandering down it (each cut dark, its lower edge catching the light); chips knocked off it
    (a pale scar over a shadow); and a star of shatter cracks where something once struck it."""
    R = stone_ramp()
    f = fbm(831)
    rng = random.Random(832)
    lv = {(x, y): 2.55 + (f(x, y) - 0.5) * 0.2 - (0.05 if (x + y) % 4 == 0 else 0.0) for x in range(32) for y in range(32)}
    for (x, y) in lv:
        if y % 4 == 0:
            lv[(x, y)] -= 0.15                                                    # the throwing rings, faint
        if y > 26:
            lv[(x, y)] -= 0.25 * (y - 26) / 5 + (0.1 if f(x * 2, y * 2) > 0.6 else 0.0)   # grime from the ground
    # A chevron frieze near the shoulder (rows 9 to 13): a zigzag cut between two fine lines.
    for x in range(32):
        lv[(x, 9)] = 1.6
        lv[(x, 13)] = 3.35
        zy = 10 + (x % 6 if x % 6 < 3 else 5 - x % 6)
        lv[(x, zy)] = 1.15
        if zy + 1 < 13:
            lv[(x, zy + 1)] = 3.45                                                # its lit lower edge
    # A row of studs low down (row 27), each a lit dome over its shadow.
    for x in range(2, 32, 5):
        lv[(x, 27)] = 3.8
        lv[(x, 28)] = 1.3
        lv[(x - 1, 27)] = 2.9
    # The rune band.
    for x in range(32):
        lv[(x, 18)] = 1.7
        lv[(x, 24)] = 3.3
        for y in range(19, 24):
            lv[(x, y)] = 2.2 + (f(x, y) - 0.5) * 0.2
    glyphs = [((1, 0), (1, 1), (1, 2), (0, 3), (2, 3)), ((0, 0), (2, 0), (1, 1), (1, 2), (1, 3)),
              ((0, 0), (0, 1), (0, 2), (0, 3), (1, 1), (2, 2)), ((1, 0), (0, 1), (2, 1), (1, 2), (1, 3)),
              ((0, 0), (1, 0), (2, 0), (1, 1), (0, 2), (2, 2), (1, 3))]
    for k, x0 in enumerate(range(1, 30, 5)):
        for dx, dy in glyphs[k % len(glyphs)]:
            x, y = x0 + dx, 20 + dy
            if x < 32:
                lv[(x, y)] = 1.3                                                  # the cut
                if (x + 1, y) in lv and lv[(x + 1, y)] > 1.6:
                    lv[(x + 1, y)] = 3.0                                          # its lit far wall
    # Hairline cracks wandering down.
    for _ in range(6):
        x, y = rng.randrange(32), rng.randrange(0, 20)
        for _ in range(rng.randrange(5, 12)):
            if (x, y) in lv and not 18 <= y <= 24 and not 9 <= y <= 13:
                lv[(x, y)] = 1.35
                if (x, y + 1) in lv:
                    lv[(x, y + 1)] = max(lv[(x, y + 1)], 3.0)
            x = (x + rng.choice((-1, 0, 0, 1))) % 32
            y += 1
    # Chips knocked off.
    for _ in range(9):
        x, y = rng.randrange(1, 30), rng.randrange(1, 30)
        if 17 <= y <= 25 or 8 <= y <= 14:
            continue
        for dx, dy in ((0, 0), (1, 0), (0, 1)):
            lv[(x + dx, y + dy)] = 3.6
        lv[(x, y + 2)] = 1.3
        lv[(x + 1, y + 1)] = 1.4
    # A star of shatter cracks where it was struck.
    cx, cy = 22, 8
    for ang in range(0, 360, 60):
        a = math.radians(ang + 15)
        for r in range(1, 5):
            x, y = int(round(cx + r * math.cos(a))), int(round(cy + r * math.sin(a)))
            if (x, y) in lv:
                lv[(x, y)] = 1.35
    lv[(cx, cy)] = 1.2

    def px(x, y):
        level = max(0.0, min(4.99, lv[(x, y)] + pixel_grain(x, y, 0x0c31) * 0.15))   # a soft grain over it all
        lo = int(level)
        return mix(R[lo], R[lo + 1], level - lo)
    return basin_canvas(px)


def urn_plain():
    """The jar's calmer stone: the urns' soft dreamstone, only faintly mottled, faint throwing rings, a soft grain and a
    few worn chips; in rows 18 to 24 the urns' rune band (its belly reads it there, once round)."""
    R = stone_ramp()
    f = fbm(841)
    rune = urn_body()
    img = Image.new("RGBA", (32, 32))
    for y in range(32):
        for x in range(32):
            if 18 <= y <= 24:
                img.putpixel((x, y), rune.getpixel((x, y)))
                continue
            level = 2.55 + (f(x, y) - 0.5) * 0.15 - (0.12 if y % 4 == 0 else 0.0) + pixel_grain(x, y, 0x0c41) * 0.15
            if ((x * 2654435761 ^ y * 40503) >> 6) % 53 == 0:
                level += 0.5                                                          # a worn chip, pale
            lo = int(level)
            img.putpixel((x, y), mix(R[lo], R[lo + 1], level - lo) + (255,))
    return img


def urn_variant(name):
    """The other urns, in the same stone, framing and wear as the urn (urn_body, planter_trim): a small squat jar with
    a dark mouth; a tall amphora with a long neck and a loop handle either side; a wide lidded jar with a knob."""
    M = lambda f, t, kind, out="wn": moulding(f, t, kind, out)
    B = lambda f, t: el(f, t, "body")
    mouth = lambda x0, y, x1: el([x0, y + 0.02, x0], [x1, y + 0.02, x1], "trim", faces=("up",), uv=[0, 6, 1, 7])
    if name == "dreamstone_urn_small":
        e = [M([6, 0, 6], [10, 0.5, 10], "band"), B([5, 0.5, 5], [11, 5, 11]),
             B([4.5, 1, 5.5], [11.5, 4.5, 10.5]), B([5.5, 1, 4.5], [10.5, 4.5, 11.5]),
             M([5.5, 5, 5.5], [10.5, 5.5, 10.5], "bead"), B([6.5, 5.5, 6.5], [9.5, 6.5, 9.5]),
             M([6, 6.5, 6], [10, 7.5, 10], "cap"), mouth(6.75, 7.5, 9.25)]
    elif name == "dreamstone_amphora":
        e = [M([6, 0, 6], [10, 0.5, 10], "band"), B([6.75, 0.5, 6.75], [9.25, 1.5, 9.25]),
             B([5.5, 1.5, 5.5], [10.5, 3, 10.5]), B([4.5, 3, 4.5], [11.5, 9, 11.5]),
             B([4, 4, 5], [12, 8, 11]), B([5, 4, 4], [11, 8, 12]),
             B([5.5, 9, 5.5], [10.5, 10.5, 10.5]), M([6, 10.5, 6], [10, 11, 10], "bead"),
             B([6.75, 11, 6.75], [9.25, 14.5, 9.25]), M([6, 14.5, 6], [10, 15.5, 10], "cap"), mouth(6.75, 15.5, 9.25)]
        for x0, x1, xv0, xv1, out in ((10.5, 12, 11.5, 12.5, "e"), (4, 5.5, 3.5, 4.5, "w")):           # its handles
            e += [M([x0, 13, 7.5], [x1, 13.5, 8.5], "bead", out), M([xv0, 9, 7.5], [xv1, 13.5, 8.5], "band", out)]
    else:                                                                                              # the wide jar
        # A storage jar, taller than it is wide: a stepped foot, a short taper out to a tall straight-sided belly, a
        # sloped shoulder, a bead at the foot of a narrow neck, a flared moulded rim, a low lid with a knob, and a loop
        # handle on each shoulder.
        Ob = lambda y0, y1, r: octagon(8, 8, y0, y1, r, "body")
        e = [M([5, 0, 5], [11, 1, 11], "band"), M([5.5, 1, 5.5], [10.5, 1.5, 10.5], "bead")]          # the foot
        belly = Ob(4.5, 8.5, 5.75)
        for part in belly:                                     # the rune band, once round the belly's middle
            for d in ("north", "south", "east", "west"):
                w = (part["to"][0] - part["from"][0]) if d in ("north", "south") else (part["to"][2] - part["from"][2])
                part["faces"][d]["uv"] = [0, 8.5, w, 12.5]
        e += Ob(1.5, 3, 4.25) + Ob(3, 4.5, 5.25) + belly                                              # the belly
        e += Ob(8.5, 9.5, 5.0) + Ob(9.5, 10.5, 4.0)                                                    # the shoulder
        e += [M([5, 10.5, 5], [11, 11, 11], "bead")] + Ob(11, 12, 2.75)                                # bead, neck
        e += [M([4.75, 12, 4.75], [11.25, 13, 11.25], "cap")]                                          # the rim
        e += octagon(8, 8, 13, 13.5, 2.5, "trim") + [M([7.25, 13.5, 7.25], [8.75, 14.25, 8.75], "cap")]  # lid, knob
        for x0, x1, xt0, xt1, out in ((13.25, 14, 12.5, 14, "e"), (2, 2.75, 2, 3.5, "w")):              # the handles
            e += [M([x0, 6.5, 7.25], [x1, 9, 8.75], "band", out), M([xt0, 9, 7.25], [xt1, 9.5, 8.75], "bead", out)]
        for part in e:
            if part["faces"].get("up", {}).get("texture") == "#trim" and "uv" not in part["faces"]["up"]:
                part["faces"]["up"]["uv"] = [0.5, 8.5, 0.5 + (part["to"][0] - part["from"][0]), 8.5 + (part["to"][2] - part["from"][2])]
    for part in e:
        if part["faces"].get("down", {}).get("texture") == "#body":
            part["faces"]["down"] = {"texture": "#under"}
    return {"body": "urn_plain" if name == "dreamstone_jar" else "urn_body", "trim": "planter_trim", "under": "basin_underside"}, e


def fire_lamp_stone(part):
    """The fire lamp's stonework, framed as the planter is: at its foot a stepped base moulding (a band, a bead, a band
    stepped in, a fillet), its post the planter's soft fluted stone with a slender column at each corner, and at its
    head a moulded capital under the iron. The iron and the fire are kept from gen_decor's model."""
    post = [el([5, 0, 5], [11, 16, 11], "post")]
    flutes = lambda y0, y1: [el([x, y0, z], [x + 1, y1, z + 1], "post") for x, z in ((4.5, 4.5), (10.5, 4.5), (4.5, 10.5), (10.5, 10.5))]
    if part == "base":
        return [moulding([1.5, 0, 1.5], [14.5, 1, 14.5], "band", "wn"), moulding([2.5, 1, 2.5], [13.5, 1.5, 13.5], "bead", "wn"),
                moulding([3.5, 1.5, 3.5], [12.5, 2.5, 12.5], "band", "wn"), moulding([4.5, 2.5, 4.5], [11.5, 3, 11.5], "fillet", "wn"),
                el([5, 3, 5], [11, 16, 11], "post")] + flutes(3, 16)
    if part == "shaft":
        return post + flutes(0, 16)
    return [moulding([4.5, 0, 4.5], [11.5, 1, 11.5], "band", "wn"), moulding([4, 1, 4], [12, 2, 12], "cap", "wn")]


# Where each part of a moulding's profile lies in planter_trim (v, in model units, and its height there).
TRIM_ROWS = {"band": 0, "toe": 2, "bead": 3, "fillet": 4, "cap": 5}


def moulding(f, t, kind, out):
    """A piece of moulding with real depth: its sides read the row of planter_trim for its part of the profile (kind),
    its top the lit bevel, brightest along its outer arrises (out: the directions it faces outward, of "n", "s", "e",
    "w"), its underside the shadow."""
    e = el(f, t, "trim")
    h = t[1] - f[1]
    v = TRIM_ROWS[kind]
    across = {"north": (16 - t[0], 16 - f[0]), "south": (f[0], t[0]), "east": (16 - t[2], 16 - f[2]), "west": (f[2], t[2])}
    for d, (u0, u1) in across.items():
        e["faces"][d]["uv"] = [u0, v, u1, v + h]
    w, dz = t[0] - f[0], t[2] - f[2]
    # The top: u runs along x and v along z, so the region's lit first column and row land on the outer edges.
    u0, u1 = (0, w) if "w" in out else (w, 0) if "e" in out else (0.5, 0.5 + w)
    v0, v1 = (8, 8 + dz) if "n" in out else (8 + dz, 8) if "s" in out else (8.5, 8.5 + dz)
    e["faces"]["up"]["uv"] = [u0, v0, u1, v1]
    e["faces"]["down"]["uv"] = [0, 6, min(w, 16), 6 + min(dz, 2)]
    return e


def stone_planter():
    """A planter of the labyrinth's own soft dreamstone, built as the weeping basin is (32-texel textures, so half
    pixels still land on whole texels): a solid moulded plinth, a trough of soft brick
    with a plain sunk panel on its front and back, and framed all round in mouldings with real depth: a stepped base
    moulding along its foot (a toe, a bead over it, a fillet set back over that), a bead under its rim, a moulded rim
    ring over it, and at each corner a pilaster whose shaft is set back between a stepped base and a necked, overhanging
    cap; moss soil filling it, and in it whatever flowers are set there (drawn by PlanterClient)."""
    # The plinth: one solid moulded slab under the whole of it, its underside plain and in shadow, so it sits clean.
    e = [moulding([0.5, 0, 2.5], [15.5, 1, 13.5], "band", "wn")]
    e.append(el([2, 1, 4], [14, 6, 12], "brick", north="panel", south="panel"))                    # the trough

    def grown(x, z, y0, y1, g):
        """A pilaster's box, grown by g (or set back, g < 0) on its two outer sides."""
        sx, sz = (-1 if x == 1 else 1), (-1 if z == 3 else 1)
        return ([x - (g if sx < 0 else 0), y0, z - (g if sz < 0 else 0)],
                [x + 2 + (g if sx > 0 else 0), y1, z + 2 + (g if sz > 0 else 0)])

    for x, z in ((1, 3), (13, 3), (1, 11), (13, 11)):                                              # the pilasters
        out = ("w" if x == 1 else "e") + ("n" if z == 3 else "s")
        for y0, y1, g, kind in ((1, 1.5, 0.5, "toe"), (1.5, 2, 0, "bead"), (2, 2.5, -0.5, "fillet"),
                                (6, 6.5, 0, "bead"), (6.5, 7, -0.5, "fillet"), (7, 8, 0.5, "cap")):
            e.append(moulding(*grown(x, z, y0, y1, g), kind, out))
        e.append(el(*grown(x, z, 2.5, 6, -0.5), "base"))                                           # the shaft, set back
    # The base moulding along the trough's foot, its steps level with the pilasters' so one profile runs all round.
    for y0, y1, d, kind in ((1, 1.5, 1.5, "toe"), (1.5, 2, 1, "bead"), (2, 2.5, 0.5, "fillet")):
        e += [moulding([3, y0, 4 - d], [13, y1, 4], kind, "n"), moulding([3, y0, 12], [13, y1, 12 + d], kind, "s"),
              moulding([2 - d, y0, 5], [2, y1, 11], kind, "w"), moulding([14, y0, 5], [14 + d, y1, 11], kind, "e")]
    # The bead under the rim, half as deep, stepping the rim's overhang down to the trough's face.
    e += [moulding([3, 5.5, 3.5], [13, 6, 4], "bead", "n"), moulding([3, 5.5, 12], [13, 6, 12.5], "bead", "s"),
          moulding([1.5, 5.5, 5], [2, 6, 11], "bead", "w"), moulding([14, 5.5, 5], [14.5, 6, 11], "bead", "e")]
    e += [moulding([3, 6, 3], [13, 7, 4], "band", "n"), moulding([3, 6, 12], [13, 7, 13], "band", "s"),     # the rim
          moulding([1, 6, 5], [3, 7, 11], "band", "w"), moulding([13, 6, 5], [15, 7, 11], "band", "e")]
    e.append(el([3, 6, 4], [13, 6.5, 12], "soil", faces=("up",)))                                  # the soil
    # Its flowers are no part of the model: any flower set in it is drawn where it was set (PlanterBlockEntity).
    return ({"base": "planter_post", "brick": "planter_slab", "panel": "planter_panel_soft", "trim": "planter_trim",
             "soil": "planter_soil"}, e)


# ---- The labyrinth's ironwork: the fence, its gate and the brazier, in the candelabra's dread iron ----
# The same language as the candelabra (iron_ramp, the dreamstone darkened): moulded feet, collars, caps and rails read
# candelabra_trim by explicit uv (moulding()), with lit bevelled tops and shadowed undersides; bars, shafts and plain
# parts dread_iron, a calm iron after Minecraft's own (a gentle light down one side, a little grain, no harsh marks);
# spear heads and twisted parts candelabra_stem.
def dread_iron():
    """Plain worked iron for bars, shafts and bodies: the dread iron's middle shade, a gentle light on the left of every
    four-texel run and a soft shade on its right (so a face of any width reads rounded without stripes of contrast), a
    light grain, a rare scratch."""
    R = iron_ramp()

    def px(x, y):
        level = 2.6 + (0.25 if x % 4 == 0 else -0.2 if x % 4 == 3 else 0.0)
        return stepped(R, level + metal_grain(x, y, 0x3a9) * 0.4)
    return basin_canvas(px)


def brazier_bowl():
    """The brazier's bowl: dread iron, a lit lip along its top, a band of rivets round its middle (each a lit texel
    over a shaded one), a soft shadow toward its foot."""
    R = iron_ramp()

    def px(x, y):
        level = 2.6 - 0.25 * max(0, y - 22) / 8 + (0.15 if x % 4 == 0 else 0.0)
        if y % 16 in (8, 9):
            level = 2.2                                                   # the riveted band
            if x % 6 in (2, 3):
                level = 3.4 if y % 16 == 8 else 1.9                       # a rivet, lit on its top
        return stepped(R, level + metal_grain(x, y, 0x5c3) * 0.4)
    return basin_canvas(px)


def iron_spear(x, z, y0, w):
    """A spear head in dread iron: a moulded collar, and a point diamond-shaped from every side."""
    return [moulding([x - w, y0, z - w], [x + w, y0 + 0.5, z + w], "bead", "wn"),
            el([x - w * 0.75, y0 + 0.5, z - w * 0.75], [x + w * 0.75, y0 + 2.1, z + w * 0.75], "stem",
               rot={"origin": [x, y0 + 1.3, z], "axis": "x", "angle": 45}),
            el([x - w * 0.75, y0 + 0.5, z - w * 0.75], [x + w * 0.75, y0 + 2.1, z + w * 0.75], "stem",
               rot={"origin": [x, y0 + 1.3, z], "axis": "z", "angle": 45}),
            el([x - 0.25, y0 + 2.1, z - 0.25], [x + 0.25, y0 + 3.1, z + 0.25], "bar")]


IRON_TEX = {"bar": "dread_iron", "trim": "candelabra_trim", "stem": "candelabra_stem", "foot": "candelabra_foot_top"}


def iron_post(cx, cz, top=14.0):
    """A square post of dread iron: a moulded foot (toe and band), a plain shaft, a collar, a moulded cap, a spear."""
    M = lambda f, t, kind: moulding(f, t, kind, "wn")
    return [M([cx - 2.5, 0, cz - 2.5], [cx + 2.5, 0.5, cz + 2.5], "toe"), M([cx - 2, 0.5, cz - 2], [cx + 2, 1.5, cz + 2], "band"),
            el([cx - 1.5, 1.5, cz - 1.5], [cx + 1.5, top, cz + 1.5], "bar", down="trim"),
            M([cx - 1.75, 6.5, cz - 1.75], [cx + 1.75, 7, cz + 1.75], "bead"),                      # a collar halfway up
            M([cx - 2, top, cz - 2], [cx + 2, top + 1, cz + 2], "cap"), M([cx - 1.5, top + 1, cz - 1.5], [cx + 1.5, top + 1.5, cz + 1.5], "bead")] + \
        iron_spear(cx, cz, top + 1.5, 0.9)


def fence():
    """The dread iron fence: a moulded post with a spear finial; each side two moulded rails, and between them two
    bars with spear heads, each collared where it meets a rail and at its middle."""
    post = iron_post(8, 8)
    M = lambda f, t, kind: moulding(f, t, kind, "wn")
    side = [M([7.25, 2.5, 0], [8.75, 3.5, 6.5], "band"), M([7.25, 12, 0], [8.75, 13, 6.5], "band")]
    for z in (1.75, 4.5):
        side += [el([7.5, 0.5, z - 0.5], [8.5, 15, z + 0.5], "bar"),
                 M([7.25, 7.25, z - 0.75], [8.75, 7.75, z + 0.75], "bead")]                         # its collar
        side += iron_spear(8, z, 15, 0.6)
    return post, side


def gate():
    """The dread iron gate: two leaves between moulded posts with spear finials, each leaf a frame of moulded rails
    with collared spear-headed bars and a twisted cross brace, a crest over their meeting; open, the leaves swung in."""
    M = lambda f, t, kind: moulding(f, t, kind, "wn")
    posts = []
    for cx in (1.0, 15.0):
        posts += [M([cx - 1, 0, 6.25], [cx + 1, 1, 9.75], "band"),
                  el([cx - 0.75, 1, 6.5], [cx + 0.75, 14.5, 9.5], "bar", down="trim"),
                  M([cx - 1, 14.5, 6.25], [cx + 1, 15.5, 9.75], "cap")] + iron_spear(cx, 8, 15.5, 0.75)

    def leaf(x0, x1):
        e = [M([x0, 2, 7.25], [x1, 3, 8.75], "band"), M([x0, 12, 7.25], [x1, 13, 8.75], "band"),
             M([x0, 7, 7.5], [x1, 7.5, 8.5], "bead")]
        span = x1 - x0
        for k in range(3):
            x = x0 + span * (k + 0.5) / 3
            e += [el([x - 0.5, 0.5, 7.5], [x + 0.5, 14.5, 8.5], "bar"), *iron_spear(x, 8, 14.5, 0.55)]
        mid = (x0 + x1) / 2  # a twisted brace crossing between the rails
        e += [el([mid - 1.5, 9.25, 7.6], [mid + 1.5, 9.75, 8.4], "stem", rot={"origin": [mid, 9.5, 8], "axis": "z", "angle": 45}),
              el([mid - 1.5, 9.25, 7.6], [mid + 1.5, 9.75, 8.4], "stem", rot={"origin": [mid, 9.5, 8], "axis": "z", "angle": -45})]
        return e

    left, right = leaf(2.25, 7.95), leaf(8.05, 13.75)
    crest = [M([6.5, 13, 7.5], [9.5, 13.5, 8.5], "bead")] + iron_spear(8, 8, 13.5, 0.8)

    def swing(elements, hx, sign):
        out = []
        for e in elements:
            pts = [(e["from"][0], e["from"][2]), (e["to"][0], e["to"][2])]
            turned = [(hx + (z - 8) * sign, 8 + (x - hx) * sign) for x, z in pts]
            xs, zs = [p[0] for p in turned], [p[1] for p in turned]
            e2 = dict(e, **{"from": [round(min(xs), 3), e["from"][1], round(min(zs), 3)], "to": [round(max(xs), 3), e["to"][1], round(max(zs), 3)]})
            # Its faces turn with it: what faced north faces east (or west), and so on round.
            turn = {"north": "east", "east": "south", "south": "west", "west": "north"} if sign > 0 else \
                {"north": "west", "west": "south", "south": "east", "east": "north"}
            e2["faces"] = {turn.get(d, d): face for d, face in e["faces"].items()}
            if "rotation" in e:
                o = e["rotation"]["origin"]
                axis = {"x": "z", "z": "x"}.get(e["rotation"]["axis"], "y")
                e2["rotation"] = dict(e["rotation"], axis=axis, origin=[round(hx + (o[2] - 8) * sign, 3), o[1], round(8 + (o[0] - hx) * sign, 3)])
            out.append(e2)
        return out

    return posts + left + right + crest, posts + swing(left, 2.25, 1) + swing(right, 13.75, -1)


# ---- The haunted dining room: its table, plates, goblets and bones ----
def table_runner():
    """The table's runner, in chunky pixel art at 16 (a texel to a pixel): a tattered velvet in four clear crimson
    shades, folds stepping diagonally across it, the odd lit crest and darker thread; a thread of tarnished gold along
    both its edges (texels 4 and 11 across, where the runner's edges lie), and worn spots gone dark."""
    crimson = [(52, 6, 18), (84, 10, 28), (116, 18, 38), (150, 32, 50)]
    worn = {(7, 3), (8, 3), (9, 11), (6, 13), (6, 14)}
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if x in (4, 11):
                c = (150, 112, 46) if y % 2 else (112, 82, 32)                       # the gold thread
            elif (x, y) in worn:
                c = (32, 6, 12)                                                      # worn through to the dark
            else:
                k = (1, 2, 2, 1)[((x + y) // 3) % 4]                                 # the folds, in steps
                h = ((x * 73856093) ^ (y * 19349663) ^ 0x9101) % 11
                k = 3 if h == 0 else 0 if h == 1 else k                              # a crest, a darker thread
                c = crimson[k]
            img.putpixel((x, y), c + (255,))
    return img


def sleepwood_ramp():
    """Six shades of the sleepwood (its log's own grey-violet), dark to light."""
    return [mix((22, 20, 26), (112, 106, 120), t) for t in (0.0, 0.2, 0.38, 0.56, 0.76, 1.0)]


def wood_level(R, level):
    """A level on the sleepwood ramp, blended smoothly between its shades."""
    level = max(0.0, min(4.999, level))
    k = int(level)
    return mix(R[k], R[k + 1], level - k)


def wood_streak(a, b, seed):
    """Wood grain as streaks: each line along the grain (a) its own tone, drifting slowly along its length (b)."""
    h = ((a * 2654435761) ^ seed) & 0xFFFF
    return ((h % 7) - 3) * 0.16 + 0.12 * math.sin(b * 0.35 + (h % 13)) + (0.35 if h % 23 == 0 else 0.0)


def table_leg():
    """The table's turned legs (read where they lie: a texel row is a height, two to a pixel, row 0 at the top): soft
    wood grain running down them (long fine streaks, a knot now and then), blended, and the turning cut in at its
    heights: the foot's lit edge (row 30), a bead low on the shaft (rows 26, 27), the collar (rows 22, 23), and two ring
    grooves on the shaft (rows 14, 15 and 9, 10), each lit over its shadow."""
    R = sleepwood_ramp()
    f = fbm(9201)
    grain = fbm(9202)
    rings = {30: 0.7, 31: -0.6, 26: 0.5, 27: -0.5, 22: 0.7, 23: -0.2, 14: 0.45, 15: -0.55, 9: 0.45, 10: -0.55}

    def px(x, y):
        level = 2.4 + wood_streak(x, y, 0x9201) + (f(x, y) - 0.5) * 0.2      # the grain, streaking down
        if math.hypot(x - 20.5, (y - 17.5) * 0.6) < 1.6:
            level -= 0.6                                                        # a knot
        return wood_level(R, level + rings.get(y, 0.0))
    return basin_canvas(px)


def table_apron():
    """The table's apron and stretcher: the grain running along them, blended; over the apron's rows (4 to 7) a lit
    top edge, a cut groove under it, its foot in shadow; the stretcher's rows (25 to 27) lit along their top."""
    R = sleepwood_ramp()
    f = fbm(9211)
    grain = fbm(9212)
    edges = {4: 0.6, 5: 0.1, 6: -0.55, 7: -0.3, 25: 0.4, 27: -0.4}

    def px(x, y):
        level = 2.4 + wood_streak(y, x, 0x9211) + (f(x, y) - 0.5) * 0.2      # the grain, streaking along
        return wood_level(R, level + edges.get(y, 0.0))
    return basin_canvas(px)


def plate_pewter():
    """The plate's pewter: a pale cold grey, the dish's well a little darker, a lit rim, a soft grain."""
    R = stone_ramp()

    def px(x, y):
        level = 3.3 + metal_grain(x, y, 0x77e) * 0.4
        return stepped(R, level)
    return basin_canvas(px)


def plate_meat():
    """A cut of rotten meat: dark red going grey-green, its fat a sickly cream, flecks of rot."""
    f = fbm(9131)

    def px(x, y):
        v = f(x * 2, y * 2)
        c = mix((92, 22, 26), (140, 44, 40), v)
        if v > 0.72:
            c = (188, 170, 128)                                                      # fat
        elif v < 0.2:
            c = (70, 82, 52)                                                         # rot
        return c
    return basin_canvas(px)


def dining_table():
    """The haunted table, in two models joined by its blockstate (multipart): its top, a run of sleepwood planks under
    the crimson runner, a moulded apron round its edges; and an end (drawn at its north edge, turned to whichever end
    has no table past it), two turned legs and a stretcher between them, and the runner hanging over the edge."""
    top = [el([0, 14, 0], [16, 16, 16], "planks", down="planks"),                                  # the board
           el([0.5, 12.5, 0], [1.5, 14, 16], "apron"), el([14.5, 12.5, 0], [15.5, 14, 16], "apron"),     # the apron
           el([5, 16, 0], [11, 16.05, 16], "runner", faces=("up",))]                                 # the runner
    # Its edges torn away in places (the gold-threaded edge, a pixel wide, broken off along it).
    for x0, runs in ((4, ((0, 5), (6.5, 11), (12, 16))), (11, ((0, 3), (4, 9.5), (10.5, 16)))):
        top += [el([x0, 16, z0], [x0 + 1, 16.05, z1], "runner", faces=("up",)) for z0, z1 in runs]
    end = [el([0.5, 12.5, 0.5], [15.5, 14, 1.5], "apron"),                                             # the apron's end
           # The runner hanging over the end, frayed: a strip to each pixel across, each torn off at its own length.
           *[el([x, 16 - n, -0.05], [x + 1, 16.05, 0], "runner", faces=("north", "south"))
             for x, n in zip(range(4, 12), (3, 5, 4, 5.5, 3.5, 5, 2.5, 4))]]
    for x in (1, 13):                                                                                # turned legs
        end += [el([x, 0, 1], [x + 2, 1, 3], "leg"), el([x + 0.25, 1, 1.25], [x + 1.75, 4, 2.75], "leg"),
                el([x, 4, 1], [x + 2, 5, 3], "leg"), el([x + 0.25, 5, 1.25], [x + 1.75, 12.5, 2.75], "leg")]
    end.append(el([3, 2.5, 1.5], [13, 3.5, 2.5], "apron"))                                             # the stretcher
    tex = {"planks": "sleepwood_planks", "leg": "table_leg", "apron": "table_apron", "runner": "table_runner"}
    return tex, top, end


def dread_plate(serving):
    """A pewter plate: a raised rim round a sunk well; holding bones (two crossed and a small skull) or a cut of meat."""
    e = [el([4, 0, 4], [12, 0.5, 12], "pewter"),
         el([3.5, 0.5, 3.5], [12.5, 1, 4.5], "pewter"), el([3.5, 0.5, 11.5], [12.5, 1, 12.5], "pewter"),
         el([3.5, 0.5, 4.5], [4.5, 1, 11.5], "pewter"), el([11.5, 0.5, 4.5], [12.5, 1, 11.5], "pewter")]
    if serving == "bones":
        e += [el([5, 0.5, 7.25], [11, 1.25, 8], "bone", rot={"origin": [8, 0.9, 7.6], "axis": "y", "angle": 30}),
              el([5, 0.55, 8], [11, 1.4, 8.75], "bone", rot={"origin": [8, 0.9, 8.4], "axis": "y", "angle": -30}),
              el([6.5, 0.5, 5], [9, 2.5, 7], "bone")]                                                # a small skull
    elif serving == "meat":
        e += [el([5.5, 0.5, 6], [10.5, 2, 10], "meat"), el([6, 2, 6.5], [9.5, 2.5, 9.5], "meat")]
    return {"pewter": "plate_pewter", "bone": "tomb_bone", "meat": "plate_meat"}, e


def dread_goblet():
    """An iron goblet: a moulded foot, a slender stem with a knot, a cup with a lit lip, blood in it."""
    M = lambda f, t, kind: moulding(f, t, kind, "wn")
    e = [M([6, 0, 6], [10, 0.5, 10], "band"), el([7.5, 0.5, 7.5], [8.5, 2.5, 8.5], "bar"),
         M([7, 1.5, 7], [9, 2, 9], "bead"), M([6.5, 2.5, 6.5], [9.5, 3, 9.5], "bead"),
         el([6, 3, 6], [10, 5.5, 10], "bar"), M([5.75, 5.5, 5.75], [10.25, 6, 10.25], "cap"),
         el([6.5, 6.02, 6.5], [9.5, 6.02, 9.5], "blood", faces=("up",), emit=4)]                  # blood brimming
    return dict(IRON_TEX, blood="weeping_tears"), e


def scattered_bones():
    """A few bones scattered flat: two long bones crossing, a short one, a jaw, a small skull on its side."""
    e = [el([2, 0, 7], [11, 1, 8], "bone", rot={"origin": [6.5, 0.5, 7.5], "axis": "y", "angle": 22.5}),
         el([5, 0, 3], [12, 1, 4], "bone", rot={"origin": [8.5, 0.5, 3.5], "axis": "y", "angle": -22.5}),
         el([10, 0, 10], [14, 0.75, 10.75], "bone"), el([3, 0, 11], [6, 0.6, 13], "bone"),
         el([9, 0, 12], [12, 2.5, 14.5], "bone")]
    return {"bone": "tomb_bone"}, e


def brazier():
    """The dread brazier, in the candelabra's language: a moulded foot stepping up, its broad step's top framed; a
    stem of twisted iron with a moulded knot; a stepped cup rising to a riveted bowl under a moulded rim, glowing coals
    in it. (Its fire is kept from gen_decor's model.)"""
    M = lambda f, t, kind: moulding(f, t, kind, "wn")
    e = [M([2, 0, 2], [14, 0.5, 14], "toe"), M([2.5, 0.5, 2.5], [13.5, 1.5, 13.5], "band"),
         M([4.5, 1.5, 4.5], [11.5, 2, 11.5], "bead"), M([5.5, 2, 5.5], [10.5, 3, 10.5], "cap")]
    e[1]["faces"]["up"] = {"texture": "#foot", "uv": [0, 0, 16, 16]}
    e += [el([6.5, 3, 6.5], [9.5, 6, 9.5], "stem"), M([5.5, 6, 5.5], [10.5, 7, 10.5], "cap")]          # stem, knot
    e += [M([4, 7, 4], [12, 7.5, 12], "bead"), M([3, 7.5, 3], [13, 8.5, 13], "band"),                # the cup
          el([1.5, 8.5, 1.5], [14.5, 12, 14.5], "bowl", down="trim")]                               # the bowl
    # The rim: a moulded ring round the bowl's mouth, the coals sunk inside it.
    e += [M([1, 12, 1], [15, 13, 2.5], "cap"), M([1, 12, 13.5], [15, 13, 15], "cap"),
          M([1, 12, 2.5], [2.5, 13, 13.5], "cap"), M([13.5, 12, 2.5], [15, 13, 13.5], "cap")]
    for part in e:                                                                                 # clean undersides
        down = part["faces"].get("down")
        if down and down["texture"] in ("#trim", "#stem", "#bowl"):
            w, d = part["to"][0] - part["from"][0], part["to"][2] - part["from"][2]
            part["faces"]["down"] = {"texture": "#under", "uv": [0, 0, 16, 16] if min(w, d) >= 4 else [6, 6, 6 + w / 2, 6 + d / 2]}
    return dict(IRON_TEX, bowl="brazier_bowl", under="candelabra_under"), e


def pier_part(name):
    """A pier stone that stands out past the shaft: its core a full block, and round it a moulding a pixel proud (the
    plinth at a base's foot, the abacus at a capital's head), so the pier reads in depth against what is beside it."""
    side, end = f"{name}_side", "dreamstone_pier_end"
    core = el([0, 0, 0], [16, 16, 16], "side", up="end", down="end")
    if name.endswith("base"):
        extra = [el([-1, 0, -1], [17, 4, 17], "side", up="end", down="end", uv=[0, 12, 16, 16])]
        for e in extra:  # the plinth's sides read the plinth rows of the texture
            for d in ("north", "south", "east", "west"):
                e["faces"][d]["uv"] = [0, 12, 16, 16]
            e["faces"]["up"]["uv"] = e["faces"]["down"]["uv"] = [0, 0, 16, 16]
    else:
        extra = [el([-1, 13, -1], [17, 16, 17], "side", up="end", down="end")]
        for e in extra:
            for d in ("north", "south", "east", "west"):
                e["faces"][d]["uv"] = [0, 0, 16, 3]
            e["faces"]["up"]["uv"] = e["faces"]["down"]["uv"] = [0, 0, 16, 16]
    for d in ("north", "south", "east", "west"):
        core["faces"][d]["cullface"] = d
    return {"side": side, "end": end}, [core] + extra


def match_tones(img, reference):
    """A texture brought into another's tones, as pixel art: each texel keeps its place from dark to light within its
    own texture (so its carving, fluting and bands stay), and takes the colour the reference has at that same place
    (so it is drawn in the reference's own palette). Reds are kept as they are."""
    lum = lambda c: c[0] * 0.3 + c[1] * 0.59 + c[2] * 0.11
    ref = sorted(reference.getdata(), key=lum)
    src = img.convert("RGBA")
    texels = [(x, y) for y in range(src.height) for x in range(src.width)]
    stone = [p for p in texels if not (src.getpixel(p)[0] - max(src.getpixel(p)[1:3]) > 50)]
    ranked = sorted(stone, key=lambda p: lum(src.getpixel(p)))
    out = src.copy()
    for i, p in enumerate(ranked):
        c = ref[min(len(ref) - 1, int(i / max(1, len(ranked) - 1) * (len(ref) - 1)))]
        out.putpixel(p, tuple(c) + (src.getpixel(p)[3],))
    return out


def run(g):
    """g: the gen_between_blocks module (paths and writers)."""
    tex_dir = os.path.join(g.ASSETS, "textures", "block")
    for name, draw in HAND16.items():
        draw().save(os.path.join(tex_dir, name + ".png"))  # drawn by hand at 16: no pass over them
    global TEX_DIR
    TEX_DIR = tex_dir
    for name, draw in TEXTURES.items():
        img = draw()
        if name != "statue_shadow":
            # Pixel art: drawn at 32 for its forms, finished at Minecraft's 16 in a few flat shades, hard-edged.
            img = pixel_art(img.resize((16, 16), Image.BOX), MATERIAL.get(name, "stone"))
        img.save(os.path.join(tex_dir, name + ".png"))
    for name, draw in (("basin_polished", basin_polished), ("basin_bricks", basin_bricks), ("basin_rim", basin_rim),
                       ("basin_pillar_top", basin_pillar_top)):
        soften(draw()).save(os.path.join(tex_dir, name + ".png"))  # in the labyrinth's colours, soft as the stem
    base = basin_base()
    base.save(os.path.join(tex_dir, "basin_base.png"))  # the plinth
    # The stains where the drip lands, on the texels those faces read (two to a model pixel; a top reads u = x, v = z,
    # a north face u = 16 - x, v = 16 - y): a pool along the bottom ledge's front, three spatters on the step above;
    # and down the front, the run over the edge and its drip.
    # (The upper step's top, along its front edge, z 2 to 3: texel rows 4 to 6, the pool there feeding the spill.)
    stain(base, [(13.0, 3.0, 6.5, 1.6), (9.5, 3.4, 2.4, 1.2), (13.0, 5.0, 6.0, 1.3), (8.5, 5.6, 1.2, 0.9),
                 (17.4, 5.6, 1.1, 0.9)], 761).save(os.path.join(tex_dir, "basin_base_stained_top.png"))
    # The upper step's front (x 3.5 to 9.5, y 2 to 3: texels 13 to 25 across, rows 26 to 28): soaked across, running
    # down over it to the ledge below.
    stain(base, [(19.0, 26.6, 6.4, 1.2), (15.0, 27.5, 1.0, 1.3), (19.5, 27.6, 1.4, 1.5), (23.5, 27.4, 0.9, 1.2)],
          763).save(os.path.join(tex_dir, "basin_base_stained_upper.png"))
    # Down the front: the pool on the ledge (x 3.5 to 9.5, texels 13 to 25 on the north face) wraps over its edge
    # along the face's top, and runs down from it, the longest where the drip lands.
    stain(base, [(19.0, 28.4, 6.8, 1.1), (14.5, 29.2, 0.9, 1.4), (17.5, 29.6, 0.8, 1.6), (20.0, 30.0, 1.3, 2.2),
                 (23.0, 29.3, 0.8, 1.3)], 762).save(os.path.join(tex_dir, "basin_base_stained_front.png"))
    for name, draw in (("basin_pillar", basin_pillar), ("basin_stem", basin_stem)):
        blend_foot(draw(), base).save(os.path.join(tex_dir, name + ".png"))  # drawn soft already; its foot into the base
    basin_tear_glow().save(os.path.join(tex_dir, "basin_tear_glow.png"))
    planter_panel_soft().save(os.path.join(tex_dir, "planter_panel_soft.png"))
    planter_slab().save(os.path.join(tex_dir, "planter_slab.png"))
    planter_soil().save(os.path.join(tex_dir, "planter_soil.png"))
    planter_trim().save(os.path.join(tex_dir, "planter_trim.png"))
    basin_underside().save(os.path.join(tex_dir, "basin_underside.png"))
    statue_skin().save(os.path.join(tex_dir, "statue_skin.png"))
    statue_pedestal_texture().save(os.path.join(tex_dir, "statue_pedestal.png"))
    planter_trim(pixel=True).save(os.path.join(tex_dir, "statue_trim.png"))
    statue_pedestal_top().save(os.path.join(tex_dir, "statue_pedestal_top.png"))
    statue_pedestal_top(under=True).save(os.path.join(tex_dir, "statue_pedestal_bottom.png"))
    statue_underside().save(os.path.join(tex_dir, "statue_underside.png"))  # its mouldings, in its pixel-art style  # the mourner, painted as a character's skin
    planter_post().save(os.path.join(tex_dir, "planter_post.png"))
    g.write(os.path.join(tex_dir, "basin_tear_glow.png.mcmeta"), {"animation": {"frametime": 3, "interpolate": True}})
    # The spilling blood: the tears' own frames in their own order, so the glow on the runs moves up them; and the
    # pool it leaves on the plinth, still (the tears' first frame alone, not animated).
    tears = Image.open(os.path.join(tex_dir, "weeping_tears.png"))
    spill_flow().save(os.path.join(tex_dir, "weeping_spill.png"))
    g.write(os.path.join(tex_dir, "weeping_spill.png.mcmeta"), {"animation": {"frametime": 2, "interpolate": True}})
    tears.crop((0, 0, tears.width, tears.width)).save(os.path.join(tex_dir, "weeping_spill_still.png"))
    b = g.b

    def write(name, textures, elements, **extra):
        g.write(os.path.join(g.ASSETS, "models", "block", name + ".json"),
                dict({"parent": "minecraft:block/block", "textures": dict({k: b(v) for k, v in textures.items()}, particle=b(next(iter(textures.values())))),
                      "elements": elements}, **extra))

    planter_trim(iron=True).save(os.path.join(tex_dir, "candelabra_trim.png"))
    candelabra_stem().save(os.path.join(tex_dir, "candelabra_stem.png"))
    candelabra_foot_top().save(os.path.join(tex_dir, "candelabra_foot_top.png"))
    candelabra_gem().save(os.path.join(tex_dir, "candelabra_gem.png"))
    candelabra_under().save(os.path.join(tex_dir, "candelabra_under.png"))
    # The stone tomb's underside (its parts are gen_decor's, run before this): every face turned down on its plinth
    # wears the patterned slab.
    tomb_underside().save(os.path.join(tex_dir, "tomb_underside.png"))
    for name in sorted(os.listdir(os.path.join(g.ASSETS, "models", "block"))):
        if not name.startswith("stone_tomb_"):
            continue
        path = os.path.join(g.ASSETS, "models", "block", name)
        with open(path, encoding="utf-8") as fh:
            model = json.load(fh)
        model["textures"]["under"] = b("tomb_underside")
        for part in model["elements"]:
            if part["from"][1] == 0 and "down" in part["faces"]:
                part["faces"]["down"] = {"texture": "#under"}
        g.write(path, model)
    turns = (("north", 0), ("east", 90), ("south", 180), ("west", 270))
    for name in CANDELABRA_SLOTS:
        tex, els = candelabra(name)
        write(name, tex, els, ambientocclusion=False)
        tex, els = candelabra(name, with_candles=True)
        write(name + "_item", tex, els, ambientocclusion=False)
        # Its states turn with its facing (any number of candles); its item shows it with plain candles burning.
        g.state(name, {f"facing={f}": dict({"model": b(name)}, **({"y": y} if y else {})) for f, y in turns})
        g.item(name, b(name + "_item"))
    tex, els = weeping_basin()
    write("weeping_basin", tex, els)
    tex, lower, upper = mourning_statue()
    write("mourning_statue_lower", tex, lower)
    write("mourning_statue_upper", tex, upper)
    write("mourning_statue_item", tex, lower,
          display={"gui": {"rotation": [30, 225, 0], "translation": [0, -3, 0], "scale": [0.36, 0.36, 0.36]},
                   "ground": {"scale": [0.25, 0.25, 0.25]}, "fixed": {"scale": [0.4, 0.4, 0.4]},
                   "thirdperson_righthand": {"rotation": [75, 45, 0], "scale": [0.25, 0.25, 0.25]},
                   "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.25, 0.25, 0.25]}})
    tex, els = stone_planter()
    write("stone_planter", tex, els)
    urn_body().save(os.path.join(tex_dir, "urn_body.png"))
    urn_plain().save(os.path.join(tex_dir, "urn_plain.png"))
    tex, els = dreamstone_urn()
    write("dreamstone_urn", tex, els)
    for name in ("dreamstone_urn_small", "dreamstone_amphora", "dreamstone_jar"):
        tex, els = urn_variant(name)
        write(name, tex, els)
        g.state(name, {f"facing={f}": dict({"model": b(name)}, **({"y": y} if y else {}))
                       for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))})
        g.item(name, b(name))
    # The urn shard: a curved sherd of the urns' stone, a rune half on it, its broken edges pale.
    shard = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    R = stone_ramp()
    outline = {(5, 3), (6, 3), (7, 3), (8, 4), (9, 4), (10, 5), (11, 6), (11, 7), (12, 8), (12, 9), (11, 10), (10, 11), (9, 12),
               (8, 12), (7, 12), (6, 11), (5, 10), (4, 9), (4, 8), (4, 7), (4, 6), (4, 5), (5, 4)}
    for y in range(16):
        for x in range(16):
            inside = 4 <= x <= 12 and 3 <= y <= 12 and (x - 8) ** 2 / 20 + (y - 7.5) ** 2 / 25 <= 1.05
            if (x, y) in outline:
                shard.putpixel((x, y), R[4] + (255,) if x + y < 15 else R[1] + (255,))
            elif inside:
                shard.putpixel((x, y), stepped(R, 2.6 + (0.4 if x < 7 else -0.2 if x > 10 else 0.0)) + (255,))
    for x, y in ((7, 6), (7, 7), (7, 8), (8, 7), (9, 9)):
        shard.putpixel((x, y), R[0] + (255,))                                    # a rune, half of it
    shard.save(os.path.join(g.ASSETS, "textures", "item", "urn_shard.png"))
    g.write(os.path.join(g.ASSETS, "models", "item", "urn_shard.json"),
            {"parent": "minecraft:item/generated", "textures": {"layer0": f"{g.NS}:item/urn_shard"}})
    g.write(os.path.join(g.ASSETS, "items", "urn_shard.json"), {"model": {"type": "minecraft:model", "model": f"{g.NS}:item/urn_shard"}})
    # The fire lamp: its stonework new, its iron and fire as gen_decor (run before this) wrote them.
    for part in ("base", "shaft", "top"):
        path = os.path.join(g.ASSETS, "models", "block", "dread_fire_lamp_" + part + ".json")
        with open(path, encoding="utf-8") as f:
            old = json.load(f)
        kept = [e for e in old["elements"] if not any(face["texture"] in ("#stone", "#post") for face in e["faces"].values())]
        write("dread_fire_lamp_" + part, {"post": "planter_post", "trim": "planter_trim", "iron": "wrought_iron",
                                          "coals": "brazier_coals", "fire": "brazier_fire"}, fire_lamp_stone(part) + kept)
    post, side = fence()
    write("dread_iron_fence_post", IRON_TEX, post, ambientocclusion=False)
    write("dread_iron_fence_side", IRON_TEX, side, ambientocclusion=False)
    closed, opened = gate()
    write("dread_iron_gate", IRON_TEX, closed, ambientocclusion=False)
    write("dread_iron_gate_open", IRON_TEX, opened, ambientocclusion=False)
    # The candle stand, in its stacking parts (any number of candles; facing as the others).
    for part in ("single", "base", "middle", "top"):
        tex, els = candle_stand(part)
        write("dread_candle_stand_" + part, tex, els, ambientocclusion=False)
    tex, els = candle_stand("single", with_candle=True)
    write("dread_candle_stand_item", tex, els, ambientocclusion=False)
    g.state("dread_candle_stand", {f"facing={f},part={pt}": dict({"model": b("dread_candle_stand_" + pt)}, **({"y": y} if y else {}))
                                   for f, y in turns for pt in ("single", "base", "middle", "top")})
    g.item("dread_candle_stand", b("dread_candle_stand_item"))
    # Crossing the Dark Between: the dream-thread walkway (a thin glowing deck, its top at the block's top), and the
    # Dream Glider's and Lucid Spool's icons.
    dream_bridge_texture().save(os.path.join(tex_dir, "dream_bridge.png"))
    write("dream_bridge", {"thread": "dream_bridge"}, [el([0, 13, 0], [16, 16, 16], "thread", emit=9)])
    g.state("dream_bridge", {"": {"model": b("dream_bridge")}})
    for name, draw in (("dream_glider", dream_glider_icon), ("lucid_spool", lucid_spool_icon)):
        draw().save(os.path.join(g.ASSETS, "textures", "item", name + ".png"))
        g.write(os.path.join(g.ASSETS, "models", "item", name + ".json"),
                {"parent": "minecraft:item/handheld" if name == "dream_glider" else "minecraft:item/generated",
                 "textures": {"layer0": f"{g.NS}:item/{name}"}})
        g.write(os.path.join(g.ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": f"{g.NS}:item/{name}"}})
    recipes = os.path.join(g.DATA, "recipe")
    g.write(os.path.join(recipes, "dream_glider.json"), {"type": "minecraft:crafting_shaped", "category": "equipment",
                                                          "pattern": ["WWW", "SSS", " S "], "key": {"W": "#minecraft:wool", "S": "minecraft:stick"},
                                                          "result": {"id": f"{g.NS}:dream_glider", "count": 1}})
    g.write(os.path.join(recipes, "lucid_spool.json"), {"type": "minecraft:crafting_shaped", "category": "equipment",
                                                         "pattern": [" S ", "SAS", " S "], "key": {"S": "minecraft:string", "A": "minecraft:amethyst_shard"},
                                                         "result": {"id": f"{g.NS}:lucid_spool", "count": 1}})
    # The open Dark Between's creatures' loot: a sleepwalker's string and now and then a bone, a manta's membrane.
    def drops(name, entries):
        g.write(os.path.join(g.DATA, "loot_table", "entities", name + ".json"), {"type": "minecraft:entity", "pools": [
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": item, "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}},
                {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
                 "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}]} for item, lo, hi in entries],
            "random_sequence": f"{g.NS}:entities/{name}"})
    drops("sleepwalker", [("minecraft:string", 0, 2), ("minecraft:bone", 0, 1)])
    drops("dread_manta", [("minecraft:phantom_membrane", 0, 2)])
    drops("dream_eater", [("minecraft:leather", 0, 2), ("minecraft:amethyst_shard", 0, 2)])
    drops("somnimoth", [("minecraft:glowstone_dust", 0, 1)])
    drops("faceless", [("minecraft:ender_pearl", 0, 1), ("minecraft:black_dye", 0, 1)])
    # The haunted dining room's furnishings.
    for name, draw in (("table_runner", table_runner), ("plate_pewter", plate_pewter), ("plate_meat", plate_meat),
                       ("table_leg", table_leg), ("table_apron", table_apron)):
        draw().save(os.path.join(tex_dir, name + ".png"))
    tex, top, end = dining_table()
    write("dread_dining_table_top", tex, top)
    write("dread_dining_table_end", tex, end)
    write("dread_dining_table_item", tex, top + end + [dict(p_, **{"from": [p_["from"][0], p_["from"][1], 16 - p_["to"][2]],
                                                                  "to": [p_["to"][0], p_["to"][1], 16 - p_["from"][2]]}) for p_ in end])
    # Its top along its axis; an end wherever no table joins it (neg: toward north or west, pos: south or east).
    turn = {("z", "neg"): 0, ("z", "pos"): 180, ("x", "neg"): 270, ("x", "pos"): 90}
    g.state("dread_dining_table", multipart=[{"when": {"axis": a}, "apply": dict({"model": b("dread_dining_table_top")}, **({"y": 90} if a == "x" else {}))}
                                             for a in ("x", "z")] +
            [{"when": {"axis": a, side: "false"}, "apply": dict({"model": b("dread_dining_table_end")}, **({"y": turn[(a, side)]} if turn[(a, side)] else {}))}
             for a in ("x", "z") for side in ("neg", "pos")])
    g.item("dread_dining_table", b("dread_dining_table_item"))
    for serving in ("empty", "bones", "meat"):
        tex, els = dread_plate(serving)
        write("dread_plate_" + serving, tex, els)
    g.state("dread_plate", {f"facing={f},serving={sv}": dict({"model": b("dread_plate_" + sv)}, **({"y": y} if y else {}))
                            for f, y in turns for sv in ("empty", "bones", "meat")})
    g.item("dread_plate", b("dread_plate_bones"))
    for name, make in (("dread_goblet", dread_goblet), ("scattered_bones", scattered_bones)):
        tex, els = make()
        write(name, tex, els)
        g.state(name, {f"facing={f}": dict({"model": b(name)}, **({"y": y} if y else {})) for f, y in turns})
        g.item(name, b(name))
    # The brazier: its ironwork new, its coals and fire as gen_decor (run before this) wrote them.
    dread_iron().save(os.path.join(tex_dir, "dread_iron.png"))
    brazier_bowl().save(os.path.join(tex_dir, "brazier_bowl.png"))
    path = os.path.join(g.ASSETS, "models", "block", "dread_brazier.json")
    with open(path, encoding="utf-8") as fh:
        old = json.load(fh)
    fire = [e for e in old["elements"] if any(face["texture"] in ("#fire", "#coals") for face in e["faces"].values())
            and all(face["texture"] in ("#fire", "#coals") for face in e["faces"].values())]
    for e in fire:
        if all(face["texture"] == "#coals" for face in e["faces"].values()):
            e["from"], e["to"] = [2.5, 12.6, 2.5], [13.5, 12.6, 13.5]                 # the coals, sunk inside the rim
    tex, els = brazier()
    write("dread_brazier", dict(tex, coals="brazier_coals", fire="brazier_fire"), els + fire, ambientocclusion=False)
    for name in ("dreamstone_pier_base", "dreamstone_pier_capital"):
        tex, els = pier_part(name)
        write(name, tex, els)
        horizontal = [dict(e) for e in els]
        write(name + "_horizontal", tex, horizontal)
    # Last: the pillar and the piers brought into the tones of the dreamstone round them (they had come out pale).
    reference = Image.open(os.path.join(tex_dir, "polished_dreamstone.png")).convert("RGB")
    for name in ("dreamstone_pillar", "dreamstone_pillar_top", "dreamstone_pier_base_side", "dreamstone_pier_capital_side",
                 "dreamstone_pier_end"):
        path = os.path.join(tex_dir, name + ".png")
        match_tones(Image.open(path), reference).save(path)
