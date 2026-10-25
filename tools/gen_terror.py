# Generates the night terror's shadows as pixel art, for NightTerrorClient to draw in the world:
#   textures/vfx/terror_demon.png  the demon at the foot of the bed (64 by 128): tall, gaunt and hunched, a long
#                                  skull bowed between shoulders hunched past it, sunk cheeks, a jaw hanging open on
#                                  thin pale teeth, ribs showing, long thin arms knobbed at the elbows ending in long
#                                  fingers curling in, clawed; below the waist only smoke, and smoke curling up off
#                                  its shoulders. Eyeless: the client lights its eyes, at DEMON_EYES
#   textures/vfx/terror_hand_<variant>.png  hands reaching up from beside the bed (32 by 64), out of smoke, clawed,
#                                  in HANDS' variants: splayed, grasping, beckoning, skeletal and gnarled
#   textures/vfx/terror_fog.png    a soft, lumpy bank of fog (white, tinted when drawn): the rolling smoke and the
#                                  pools of shadow
# Shaded as pixel art: a dark outline, a crimson rim where light from above catches the edge, a smoky texture inside,
# lines for ribs, tendons and hollows; dithered where it turns to smoke. The rim is only a catch of light, one pixel.
# Run from the project root: python tools/gen_terror.py
import math
import os
import random

from PIL import Image

OUT = 'common/src/main/resources/assets/whycantisleep/textures/vfx/'
BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]
# The shadows' tones: the core, a slightly lighter smoky mid-tone through it, the dark outline, the lines drawn inside
# (ribs, tendons, hollows), the crimson rim where light from above catches the edge and its bright line, and claws
# and teeth.
CORE = (7, 3, 9)
MID = (17, 6, 16)
OUTLINE = (2, 0, 3)
DETAIL = (34, 9, 22)
RIM = (58, 10, 24)
RIM_BRIGHT = (112, 22, 38)
CLAW = (120, 26, 38)
TOOTH = (150, 132, 124)
DEMON_SIZE = (64, 128)
HAND_SIZE = (32, 64)
# Where the demon's eyes are in its texture (keep in step with NightTerrorClient): left and right, x then y.
DEMON_EYES = ((27, 31), (37, 31))


def dithered(x, y, amount):
    return amount * 16.0 > BAYER[y % 4][x % 4] + 0.5


def capsule(px, py, ax, ay, bx, by, radius):
    # How far inside a rounded line from a to b a point is, in pixels (negative outside).
    dx, dy = bx - ax, by - ay
    t = max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / max(1e-6, dx * dx + dy * dy)))
    return radius - math.hypot(px - (ax + t * dx), py - (ay + t * dy))


def ellipse(px, py, cx, cy, rx, ry, turn=0.0):
    c, s = math.cos(turn), math.sin(turn)
    x, y = (px - cx) * c + (py - cy) * s, -(px - cx) * s + (py - cy) * c
    return (1.0 - math.hypot(x / rx, y / ry)) * min(rx, ry)


def smooth_noise(seed, scale):
    rng = random.Random(seed)
    grid = {}

    def at(gx, gy):
        if (gx, gy) not in grid:
            grid[(gx, gy)] = rng.random()
        return grid[(gx, gy)]

    def noise(x, y):
        gx, gy = x / scale, y / scale
        x0, y0 = math.floor(gx), math.floor(gy)
        fx, fy = gx - x0, gy - y0
        fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        top = at(x0, y0) * (1 - fx) + at(x0 + 1, y0) * fx
        bottom = at(x0, y0 + 1) * (1 - fx) + at(x0 + 1, y0 + 1) * fx
        return top * (1 - fy) + bottom * fy

    return noise


def paint(size, inside, lines, smoke_from, wisps, seed, light=(0.55, -0.85)):
    """Pixel art from a shape: `inside(x, y)` is how far into it a point is (pixels). The edge gets a dark outline, and
    a crimson rim where it faces the light; inside, a smoky texture, and `lines` (a list of (shape, color) drawn over
    it where the shape is positive). Below `smoke_from` (a share of the height) it thins into dithered smoke; `wisps`
    (a shape, or None) are faint smoke curling off it."""
    width, height = size
    texture = smooth_noise(seed, 5.0)
    image = Image.new('RGBA', size)
    pixels = image.load()
    for y in range(height):
        smoke = max(0.0, (y / height - smoke_from) / (1.0 - smoke_from)) if smoke_from < 1.0 else 0.0
        for x in range(width):
            px, py = x + 0.5, y + 0.5
            depth = inside(px, py)
            if depth < 0.0:
                if wisps is not None and wisps(px, py) > 0.0 and dithered(x, y, 0.45):
                    pixels[x, y] = DETAIL + (90,)
                continue
            # Which way the edge faces here, for the rim light.
            gx = inside(px + 1, py) - inside(px - 1, py)
            gy = inside(px, py + 1) - inside(px, py - 1)
            facing = -(gx * light[0] + gy * light[1]) / max(1e-6, math.hypot(gx, gy))
            if depth < 1.0:
                color = RIM_BRIGHT if facing > 0.75 else RIM if facing > 0.35 else OUTLINE
            else:
                color = MID if dithered(x, y, max(0.0, texture(px, py) - 0.45) * 1.6) else CORE
                for shape, line_color in lines:
                    if shape(px, py) > 0.0:
                        color = line_color
            alpha = 248
            if smoke > 0.0:
                keep = 1.0 - smoke
                if not dithered(x, y, keep * 1.15):
                    continue
                alpha = round(248 * (0.5 + 0.5 * keep))
            pixels[x, y] = color + (alpha,)
    return image


def demon():
    rng = random.Random(7)
    tendrils = [(rng.uniform(24, 40), rng.uniform(2.0, 3.6), rng.uniform(0, math.tau)) for _ in range(6)]

    def inside(x, y):
        parts = [
            ellipse(x, y, 32, 29, 7.5, 12.0, 0.08),                # a long skull, bowed
            ellipse(x, y, 32.5, 42, 5.0, 4.5),                     # the jaw, hanging open
            capsule(x, y, 32, 44, 32, 49, 3.0),                    # what neck there is
            capsule(x, y, 15, 30, 27, 46, 5.0),                    # shoulders hunched up past the head
            capsule(x, y, 49, 30, 37, 46, 5.0),
            capsule(x, y, 32, 46, 32, 84, 8.5 - max(0.0, (y - 52) / 7.0)),  # a narrow body, wasting to nothing
            capsule(x, y, 15, 30, 9, 64, 2.8),                     # the left arm, long and thin
            ellipse(x, y, 9, 64, 3.2, 3.2),                        # a knobbed elbow
            capsule(x, y, 9, 64, 12, 98, 2.2),
            capsule(x, y, 49, 30, 55, 64, 2.8),                    # the right arm
            ellipse(x, y, 55, 64, 3.2, 3.2),
            capsule(x, y, 55, 64, 52, 98, 2.2),
        ]
        # Hands: a narrow palm, and long fingers curling in, in two joints.
        for hand, side in ((12, -1), (52, 1)):
            parts.append(ellipse(x, y, hand, 101, 3.2, 4.0))
            for k in range(4):
                spread = (k - 1.5) * 2.2
                knuckle = (hand + spread, 103)
                joint = (hand + spread * 1.4 + side * 0.5, 112)
                tip = (hand + spread * 1.2 - side * 1.5 * (k % 2 + 1), 120)
                parts.append(capsule(x, y, *knuckle, *joint, 1.0))
                parts.append(capsule(x, y, *joint, *tip, 0.8))
        # Below the waist, smoke twisting down.
        for tx, radius, phase in tendrils:
            if y > 76:
                sway = 3.5 * math.sin(y / 10.0 + phase)
                parts.append(radius * (1.0 - (y - 76) / 52.0) - abs(x - (tx + sway)))
        return max(parts)

    def wisps(x, y):
        # Smoke curling up off the shoulders.
        curls = [-1.0]
        for sx, sy, phase in ((14, 26, 0.0), (50, 26, 2.0)):
            if y < sy:
                curls.append(0.9 - abs(x - (sx + 2.5 * math.sin((sy - y) / 4.0 + phase))) - (sy - y) * 0.06)
        return max(curls)

    def mouth(x, y):
        return ellipse(x, y, 32.5, 40.5, 3.4, 1.4)

    def teeth(x, y):
        # Thin teeth hanging from the top of the mouth and rising from the bottom.
        for tx in (30, 32, 34, 36):
            if abs(x - (tx + 0.5)) < 0.6 and 39.0 <= y < 40.5:
                return 1.0
        for tx in (31, 33, 35):
            if abs(x - (tx + 0.5)) < 0.6 and 41.0 <= y < 42.0:
                return 1.0
        return -1.0

    def sockets(x, y):
        return max(ellipse(x, y, ex + 0.5, ey + 0.5, 2.6, 2.0) for ex, ey in DEMON_EYES)

    def hollows(x, y):
        # The cheeks sunk in under the eyes.
        return max(capsule(x, y, 27, 35, 28, 38, 0.6), capsule(x, y, 38, 35, 37, 38, 0.6))

    def ribs(x, y):
        bands = [-1.0]
        for row in (55, 60, 65, 70):
            bend = 1.5 * ((x - 32) / 7.0) ** 2
            if 26 < x < 38 and abs(x - 32) > 0.8:
                bands.append(0.6 - abs(y - (row - bend)))
        return max(bands)

    def sternum(x, y):
        return capsule(x, y, 32, 50, 32, 72, 0.6)

    lines = [(ribs, DETAIL), (sternum, DETAIL), (hollows, DETAIL), (sockets, OUTLINE), (mouth, OUTLINE), (teeth, TOOTH)]
    image = paint(DEMON_SIZE, inside, lines, 0.86, wisps, 3)
    # Claws: the fingertips' last pixels, dark red.
    width, height = DEMON_SIZE
    pixels = image.load()
    for x in range(width):
        for y in range(114, 124):
            if pixels[x, y][3] > 0 and pixels[x, y + 1][3] == 0:
                pixels[x, y] = CLAW + (255,)
    return image


# The hands' variants: how each is built, all in a 32 by 64 texture, thumb to the left. Each finger is a chain of
# points from its knuckle (on the palm) to its tip, and how thick it is; the thumb likewise. `bones` draws the forearm
# as two thin bones rather than flesh.
HANDS = {
    # Splayed wide, clawing at the air.
    'splayed': {
        'fingers': [((11, 26), (9, 15), (8, 3)), ((14.5, 26), (13.5, 15), (14, 0.5)), ((18, 26), (19, 15), (21, 1)),
                    ((21.5, 26), (24, 15), (27, 5))],
        'thumb': ((10, 32), (5, 25), (3, 18)), 'width': 1.35, 'palm': (6.2, 5.6), 'arm': 4.6, 'seed': 5,
    },
    # Hooked to grab: each finger rises and then curls down and in.
    'grasping': {
        'fingers': [((11, 26), (9, 14), (11, 8), (14, 11)), ((14.5, 26), (14, 12), (16.5, 6), (19, 10)),
                    ((18, 26), (19, 13), (21.5, 8), (23, 13)), ((21.5, 26), (24, 16), (26, 12), (26.5, 17))],
        'thumb': ((10, 32), (6, 26), (8, 21)), 'width': 1.45, 'palm': (6.4, 5.8), 'arm': 4.8, 'seed': 9,
    },
    # Beckoning: one long finger reaching, bent at its last joint; the rest curled down into the palm.
    'beckoning': {
        'fingers': [((12.5, 26), (12, 14), (13, 4), (16, 0.5)), ((15.5, 26), (16, 22), (15, 25)),
                    ((18.5, 26), (19.5, 22), (18.5, 25)), ((21, 27), (22.5, 24), (21.5, 27))],
        'thumb': ((10, 32), (7, 27), (9, 24)), 'width': 1.4, 'palm': (6.0, 5.6), 'arm': 4.5, 'seed': 13,
    },
    # Skeletal: thin bones, very long nails, a narrow palm, the forearm two bones.
    'skeletal': {
        'fingers': [((12, 27), (10, 16), (8, 7), (7, 1)), ((14.5, 26), (14, 14), (14, 5), (14.5, 0)),
                    ((17, 26), (18, 15), (19.5, 6), (20.5, 0.5)), ((19.5, 27), (22, 17), (24, 9), (25.5, 4))],
        'thumb': ((11.5, 32), (8, 26), (6, 20)), 'width': 0.8, 'palm': (4.6, 5.0), 'arm': 2.0, 'seed': 17, 'bones': True,
        'claw': 3,
    },
    # Gnarled: crooked, knotted fingers, each bent three ways.
    'gnarled': {
        'fingers': [((11, 26), (8, 19), (10, 12), (6, 5)), ((14.5, 26), (15.5, 18), (12.5, 10), (15, 2)),
                    ((18, 26), (17, 18), (21, 11), (19, 3)), ((21.5, 26), (25, 20), (23.5, 13), (27, 8))],
        'thumb': ((10, 32), (5, 27), (6, 21), (3, 17)), 'width': 1.3, 'palm': (6.2, 5.6), 'arm': 4.7, 'seed': 21,
    },
}


def hand(spec):
    fingers, thumb, width = spec['fingers'], spec['thumb'], spec['width']
    bones = spec.get('bones', False)

    def chain(x, y, points, radius):
        best = -99.0
        for (ax, ay), (bx, by) in zip(points, points[1:]):
            best = max(best, capsule(x, y, ax, ay, bx, by, radius))
            radius *= 0.85
        # Knobbed at each joint.
        for jx, jy in points[1:-1]:
            best = max(best, ellipse(x, y, jx, jy, width * 1.15, width * 1.15))
        return best

    def inside(x, y):
        if bones:
            arm = max(capsule(x, y, 14.5, 64, 15, 37, spec['arm'] * 0.6), capsule(x, y, 18, 64, 17.5, 37, spec['arm'] * 0.5))
        else:
            arm = capsule(x, y, 16, 64, 16, 38, spec['arm'] - (64 - y) * 0.03)
        parts = [arm, ellipse(x, y, 16, 31, *spec['palm']), chain(x, y, thumb, width * 1.2)]
        for finger in fingers:
            parts.append(ellipse(x, y, finger[0][0], finger[0][1], width * 1.3, width * 1.3))
            parts.append(chain(x, y, finger, width))
        return max(parts)

    def tendons(x, y):
        if bones:
            return -1.0
        return max(capsule(x, y, 14, 58, 13.5, 36, 0.5), capsule(x, y, 18, 58, 18.5, 36, 0.5))

    def knuckles(x, y):
        return max(ellipse(x, y, f[0][0], f[0][1], 0.9, 0.9) for f in fingers)

    image = paint(HAND_SIZE, inside, [(tendons, DETAIL), (knuckles, DETAIL)], 0.78, None, spec['seed'], light=(-0.4, -0.9))
    # Claws: each fingertip's last pixels (more for long nails), dark red; and the thumb's.
    size_x, size_y = HAND_SIZE
    pixels = image.load()
    reach = spec.get('claw', 2)
    for points in fingers + [thumb]:
        (ax, ay), (bx, by) = points[-2], points[-1]
        length = max(1e-6, ((bx - ax) ** 2 + (by - ay) ** 2) ** 0.5)
        for k in range(reach):
            px, py = int(bx - (bx - ax) / length * k), int(by - (by - ay) / length * k)
            for dx in (-1, 0):
                qx = px + dx
                if 0 <= qx < size_x and 0 <= py < size_y and pixels[qx, py][3] > 0:
                    pixels[qx, py] = CLAW + (255,)
    return image


def fog():
    # A soft bank of fog: white (tinted when drawn), round and fading out, lumpy with layered noise, filtered smoothly.
    size = 64
    rng = random.Random(11)
    grid = {(gx, gy): rng.random() for gx in range(-1, 34) for gy in range(-1, 34)}

    def noise(x, y, scale):
        gx, gy = x / scale, y / scale
        x0, y0 = math.floor(gx), math.floor(gy)
        fx, fy = gx - x0, gy - y0
        fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        top = grid[(x0, y0)] * (1 - fx) + grid[(x0 + 1, y0)] * fx
        bottom = grid[(x0, y0 + 1)] * (1 - fx) + grid[(x0 + 1, y0 + 1)] * fx
        return top * (1 - fy) + bottom * fy

    image = Image.new('RGBA', (size, size))
    pixels = image.load()
    for y in range(size):
        for x in range(size):
            d = math.hypot((x + 0.5) / size * 2 - 1, (y + 0.5) / size * 2 - 1)
            falloff = max(0.0, 1.0 - d) ** 1.6
            lumps = 0.55 * noise(x, y, 8.0) + 0.3 * noise(x, y, 4.0) + 0.15 * noise(x, y, 2.0)
            alpha = max(0.0, min(1.0, falloff * (0.4 + 1.1 * lumps)))
            pixels[x, y] = (255, 255, 255, round(alpha * 255))
    return image


os.makedirs(OUT, exist_ok=True)
demon().save(OUT + 'terror_demon.png')
for name, spec in HANDS.items():
    hand(spec).save(OUT + 'terror_hand_' + name + '.png')
fog().save(OUT + 'terror_fog.png')
