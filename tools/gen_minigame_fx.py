# Generates the heartbeat minigame's screen effects under textures/gui/sprites/hud/. The band and the veins are pixel
# art, hard-edged on a few tones; only the smoke drifting off the band is soft, a faint wash.
#  - minigame_smoke_back.png and minigame_smoke_front.png: the band that runs across the screen, near-black with a
#    warm ember-red cast (warmer and more orange than the crimson veins, so the two stay apart), its wavy edges
#    stepping off in dithered pixels; the front one has an ember rim along its edges.
#  - minigame_smoke_wisps.png: smooth, faint smoke billowing off the band's top and bottom, like a wash.
#    All three repeat from left to right, so the game lays them side by side and slides them past each other.
#  - minigame_veins_1.png to minigame_veins_3.png: veins creeping in from the edges of the screen, each texture
#    reaching further than the one before; the game shows more of them, and brighter, the worse the minigame goes.
#    Each tapers from three pixels at the edge to a point, wherever it ends (its own tip or the texture's reach), rimmed
#    dark, with a lighter core along the thick of it.
#  - minigame_vignette.png: a dark red edge round the screen, stepping off to clear in dithered rings.
# Run from the project root: python tools/gen_minigame_fx.py
import math
import random

from PIL import Image, ImageDraw, ImageFilter

OUT = 'common/src/main/resources/assets/whycantisleep/textures/gui/sprites/hud/'
# A 4x4 ordered dither, for stepping from one tone to the next in pixels.
BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


def dithered(x, y, amount):
    return amount * 16.0 > BAYER[y % 4][x % 4] + 0.5


# ---- The band ----
SMOKE_W, SMOKE_H = 256, 96
MID = SMOKE_H // 2


def wave(x, waves, phase):
    # Waves whose periods divide the width, so the band joins up with itself.
    return sum(amp * math.sin(2.0 * math.pi * k * x / SMOKE_W + phase + i * 1.7) for i, (k, amp) in enumerate(waves))


def edges(half, waves, seed):
    rng = random.Random(seed)
    top_phase, bottom_phase = rng.uniform(0, math.tau), rng.uniform(0, math.tau)
    return [(round(MID - half + wave(x, waves, top_phase)), round(MID + half + wave(x, waves, bottom_phase))) for x in range(SMOKE_W)]


def back_band(half, waves, seed):
    # The haze behind: soft red-black, its edges dithered out over three pixels, a faint light along its top.
    image = Image.new('RGBA', (SMOKE_W, SMOKE_H))
    pixels = image.load()
    for x, (top, bottom) in enumerate(edges(half, waves, seed)):
        for y in range(max(0, top - 3), min(SMOKE_H, bottom + 4)):
            inside = min(y - top, bottom - y)
            if inside < 0:
                if dithered(x, y, 1.0 + inside / 4.0):
                    pixels[x, y] = (20, 7, 6, 90)
            elif y - top < 2:
                pixels[x, y] = (62, 22, 16, 150)
            else:
                pixels[x, y] = (20, 7, 6, 165)
    return image


def front_band(half, waves, seed):
    # The band itself, lit from above: a bright ember rim on its top edge and a softer row under it, a lighter top
    # stepping down through dithered tones to a dark body, a darker bottom with a shadowed rim, and a dithered shadow
    # cast under it. Faint streaks of lighter smoke run through it, repeating as it does.
    rng = random.Random(seed + 100)
    streaks = []
    # Scattered: anywhere along the band, at any height in its body, short or long, none in step with the others.
    for _ in range(30):
        streaks.append((rng.randrange(SMOKE_W), rng.uniform(0.18, 0.85), rng.choice((rng.randint(4, 10), rng.randint(10, 30)))))
    rim_light = (226, 104, 64, 235)
    rim_soft = (128, 46, 30, 230)
    top_tone = (44, 15, 11, 232)
    body = (20, 7, 6, 236)
    deep = (10, 3, 3, 240)
    rim_shadow = (3, 1, 1, 245)
    under = (56, 20, 14, 225)
    streak = (84, 32, 22, 236)
    image = Image.new('RGBA', (SMOKE_W, SMOKE_H))
    pixels = image.load()
    lines = edges(half, waves, seed)
    for x, (top, bottom) in enumerate(lines):
        height = bottom - top
        for y in range(top, bottom + 1):
            if not 0 <= y < SMOKE_H:
                continue
            depth = y - top
            share = depth / max(1, height)
            if depth == 0:
                color = rim_light
            elif depth == 1:
                color = rim_soft
            elif y == bottom:
                color = rim_shadow
            elif y == bottom - 1:
                # A thin ember light off the floor below, under the shadowed rim.
                color = under
            elif share < 0.22:
                color = top_tone if share < 0.12 or dithered(x, y, 1.0 - (share - 0.12) / 0.1) else body
            elif share > 0.7:
                color = deep if share > 0.8 or dithered(x, y, (share - 0.7) / 0.1) else body
            else:
                color = body
            pixels[x, y] = color
        # The shadow it casts: two dithered rows under it.
        for k, amount in ((1, 0.75), (2, 0.4)):
            y = bottom + k
            if y < SMOKE_H and dithered(x, y, amount):
                pixels[x, y] = (0, 0, 0, 150)
    # The streaks: crisp pixels, smoothed by hand in tones rather than blurred. Each rides a gentle curve that follows
    # the band; its middle is a bright core with a half-tone pixel above and below where the curve leans, and it
    # thins to half-tone, then a quarter-tone, at its ends. Tones are mixed into the body under them, so they sit in it.
    def tint(x, y, k):
        top, bottom = lines[x]
        if top + 2 < y < bottom - 2:
            base = pixels[x, y]
            pixels[x, y] = tuple(round(b + (s - b) * k) for b, s in zip(base[:3], streak[:3])) + (base[3],)

    for sx, place, length in streaks:
        curve = rng.uniform(-2.5, 2.5)
        for i in range(length):
            x = (sx + i) % SMOKE_W
            top, bottom = lines[x]
            f = i / max(1, length - 1)
            exact = top + (bottom - top) * place + curve * math.sin(math.pi * f)
            y = round(exact)
            ends = min(i, length - 1 - i)
            strength = 0.1 if ends == 0 else 0.22 if ends == 1 else 0.45
            tint(x, y, strength)
            if ends >= 2:
                # A part of a pixel's worth on the side the curve leans to, and a trace on the other, for a smooth,
                # soft line without a blur.
                lean = exact - y
                side = 1 if lean > 0 else -1
                tint(x, y + side, 0.15 + 0.2 * abs(lean))
                tint(x, y - side, 0.08)
    return image


back_band(26, [(1, 2.8), (3, 1.5), (7, 0.5)], 1).save(OUT + 'minigame_smoke_back.png')
front_band(21, [(2, 1.9), (5, 1.0), (9, 0.4)], 2).save(OUT + 'minigame_smoke_front.png')


def wisps(seed):
    # Soft puffs billowing up off the band's top and down off its bottom, blurred into a faint wash; drawn three times
    # as wide and cut from the middle, so the blur wraps round and it repeats like the band.
    rng = random.Random(seed)
    scale = 4
    w, h = SMOKE_W * scale, SMOKE_H * scale
    mask = Image.new('L', (w * 3, h))
    draw = ImageDraw.Draw(mask)
    for _ in range(70):
        x = rng.uniform(0, w * 3)
        up = rng.random() < 0.5
        y = h / 2.0 + (-1 if up else 1) * rng.uniform(21, 27) * scale
        rise = (-1 if up else 1) * rng.uniform(3, 9) * scale
        r = rng.uniform(4, 9) * scale
        for step in range(4):
            cy = y + rise * step / 3.0
            cr = r * (1.0 - 0.18 * step)
            draw.ellipse((x - cr, cy - cr * 0.7, x + cr, cy + cr * 0.7), fill=rng.randint(70, 120))
            x += rng.uniform(-2, 2) * scale
    mask = mask.filter(ImageFilter.GaussianBlur(4.0 * scale)).crop((w, 0, 2 * w, h))
    image = Image.new('RGBA', (w, h))
    pixels = image.load()
    mask_px = mask.load()
    for x in range(w):
        for y in range(h):
            a = mask_px[x, y]
            if a:
                pixels[x, y] = (28, 10, 8, a)
    return image.resize((SMOKE_W, SMOKE_H), Image.LANCZOS)


wisps(3).save(OUT + 'minigame_smoke_wisps.png')

# ---- The veins ----
VEIN_W, VEIN_H = 320, 180
REACH = [0.38, 0.68, 1.0]
RIM = (24, 2, 5, 220)
DEEP = (78, 7, 12, 255)
BODY = (124, 14, 22, 255)
CORE = (180, 52, 60, 255)


def grow(rng, x, y, angle, length, width, along, span, depth, veins):
    # A vein as a list of points with their width and how far along the whole it is (0 at the edge, 1 at the tips).
    points = []
    steps = max(4, int(length / 1.5))
    bend = 0.0
    for i in range(steps + 1):
        f = i / steps
        points.append((x, y, width * (1.0 - 0.7 * f), along + span * f))
        bend = max(-0.08, min(0.08, bend * 0.8 + rng.uniform(-0.05, 0.05)))
        angle += bend
        x += math.cos(angle) * length / steps
        y += math.sin(angle) * length / steps
        if depth < 3 and i > 3 and rng.random() < (0.07 if depth < 2 else 0.04):
            side = rng.choice((-1, 1))
            grow(rng, x, y, angle + side * rng.uniform(0.3, 0.65), length * (1.0 - f) * rng.uniform(0.5, 0.8),
                 points[-1][2] * 0.7, along + span * f, span * (1.0 - f) * 0.9, depth + 1, veins)
    veins.append((depth, points))


def all_veins(seed):
    rng = random.Random(seed)
    veins = []
    cx, cy = VEIN_W / 2.0, VEIN_H / 2.0
    starts = [(rng.uniform(0.04, 0.96) * VEIN_W, -2) for _ in range(6)]
    starts += [(rng.uniform(0.04, 0.96) * VEIN_W, VEIN_H + 2) for _ in range(6)]
    starts += [(-2, rng.uniform(0.08, 0.92) * VEIN_H) for _ in range(4)]
    starts += [(VEIN_W + 2, rng.uniform(0.08, 0.92) * VEIN_H) for _ in range(4)]
    starts += [(-2, -2), (VEIN_W + 2, -2), (-2, VEIN_H + 2), (VEIN_W + 2, VEIN_H + 2)]
    for sx, sy in starts:
        angle = math.atan2(cy - sy, cx - sx) + rng.uniform(-0.45, 0.45)
        length = math.hypot(cx - sx, cy - sy) * rng.uniform(0.45, 0.7)
        grow(rng, sx, sy, angle, length, 3.4, 0.0, 1.0, 0, veins)
    return veins


# Every vein narrows to a point over its last TAPER pixels, wherever it ends: at its own tip, or where a texture's reach
# cuts it short, so no layer shows a blunt end. Below a pixel's width the tip dithers out.
TAPER = 12.0


def samples(points, reach):
    # The vein walked in half-pixel steps up to where it ends, each with its width tapered toward that end and how far
    # along the whole it is.
    walked = []
    for (x0, y0, w0, t0), (x1, y1, w1, t1) in zip(points, points[1:]):
        if t0 > reach:
            break
        steps = max(1, int(math.hypot(x1 - x0, y1 - y0) * 2))
        for i in range(steps):
            f = i / steps
            walked.append((x0 + (x1 - x0) * f, y0 + (y1 - y0) * f, w0 + (w1 - w0) * f, t0 + (t1 - t0) * f))
    if not walked:
        return []
    out = []
    total = len(walked) * 0.5
    for i, (x, y, w, t) in enumerate(walked):
        left = total - i * 0.5
        k = min(1.0, left / TAPER)
        out.append((x, y, w * k * k * (3.0 - 2.0 * k), t))
    return out


def stamp(pixels, cx, cy, radius, color):
    # A round dab of pixels: every pixel whose middle is within the radius of the point.
    r = int(math.ceil(radius))
    for px in range(int(cx) - r - 1, int(cx) + r + 2):
        for py in range(int(cy) - r - 1, int(cy) + r + 2):
            if 0 <= px < VEIN_W and 0 <= py < VEIN_H and (px + 0.5 - cx) ** 2 + (py + 0.5 - cy) ** 2 <= radius * radius:
                pixels[px, py] = color


def body(pixels, walked, reach):
    for x, y, w, t in walked:
        if w >= 0.9:
            stamp(pixels, x, y, w / 2.0 + 0.15, DEEP if t / reach < 0.45 else BODY)
        elif dithered(int(x), int(y), w / 0.9):
            # The very tip: single pixels, thinning out.
            px, py = int(x), int(y)
            if 0 <= px < VEIN_W and 0 <= py < VEIN_H:
                pixels[px, py] = BODY


VEINS = all_veins(11)
for level, reach in enumerate(REACH, 1):
    image = Image.new('RGBA', (VEIN_W, VEIN_H))
    pixels = image.load()
    walks = [samples(points, reach) for depth, points in VEINS]
    # Rims first, under everything, a pixel round the body while it is wide enough to carry one; then the bodies,
    # deep near the edge and redder further in; then a one-pixel core along the thickest of them, which thins away
    # with the vein.
    for walked in walks:
        for x, y, w, t in walked:
            if w >= 1.2:
                stamp(pixels, x, y, w / 2.0 + 1.0, RIM)
    for walked in walks:
        body(pixels, walked, reach)
    for walked in walks:
        for x, y, w, t in walked:
            if w >= 2.4:
                stamp(pixels, x - 0.3, y - 0.3, 0.5, CORE)
    image.save(OUT + 'minigame_veins_%d.png' % level)

# ---- The vignette: dark red at the edges, stepping off to clear in dithered rings ----
VIG_W, VIG_H = 160, 90
image = Image.new('RGBA', (VIG_W, VIG_H))
pixels = image.load()
for x in range(VIG_W):
    for y in range(VIG_H):
        u = (x + 0.5) / VIG_W * 2.0 - 1.0
        v = (y + 0.5) / VIG_H * 2.0 - 1.0
        d = math.sqrt(u * u * 0.9 + v * v * 1.15)
        a = max(0.0, min(1.0, (d - 0.5) / 0.65))
        # Four tones, dithered from each to the next.
        step = a * 4.0
        tone = int(step) + (1 if dithered(x, y, step - int(step)) else 0)
        if tone:
            pixels[x, y] = (34, 2, 6, min(4, tone) * 56)
image.save(OUT + 'minigame_vignette.png')
