# Generates the night terror's shadows as pixel art, for NightTerrorClient to draw in the world:
#   textures/vfx/terror_demon.png  the demon at the foot of the bed, a flipbook of 64 by 128 frames stacked down
#                                  the texture: FORM_FRAMES of it forming out of a formless column of shadow, then
#                                  GLITCH frames of glitchy poses (NightTerrorClient plays them in stutters). Tall,
#                                  gaunt and hunched, a long
#                                  skull bowed between shoulders hunched past it, sunk cheeks, a jaw hanging open on
#                                  thin pale teeth, a body of shadow with no bones showing, thinning to smoke from
#                                  under the chest, long thin arms knobbed at the elbows ending in long fingers
#                                  curling in, clawed; below the waist only smoke, and smoke curling up off its
#                                  shoulders. Eyeless: the client lights its eyes, at DEMON_EYES
#   textures/vfx/terror_hand_<variant>.png  hands reaching up out of smoke, clawed, in HANDS' variants (splayed,
#                                  grasping, beckoning, skeletal, gnarled), each a flipbook of HAND_FRAMES 32 by 64
#                                  frames stacked down the texture, the fingers curling from open to a clenched claw
#   textures/vfx/terror_arm_upper.png, terror_arm_lower.png  the arms the hands reach on (64 by 16, drawn along: the
#                                  left edge at the shoulder or the elbow): the upper arm, dissolving into smoke at
#                                  its start, and the forearm, thinning to the wrist
#   textures/vfx/terror_fog.png    a soft, lumpy bank of fog (white, tinted when drawn): the rolling smoke and the
#                                  pools of shadow
#   textures/gui/sprites/hud/terror_vignette.png  the dark closing in round the view as the terror nears (160 by 90,
#                                  stretched over the screen): clear in the middle, darkening to the edge, which is
#                                  VIGNETTE_EDGE, fully dark, so the client can fill past it in the same color
# Shaded as pixel art: a dark outline, a crimson rim where light from above catches the edge, a smoky texture inside,
# lines for tendons and hollows; dithered where it turns to smoke. The rim is only a catch of light, one pixel.
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
# Where the demon's belly turns to shadow, from under the chest to where its smoke begins.
BELLY_FROM = 58
BELLY_TO = 96
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


def paint(size, inside, lines, smoke_from, wisps, seed, light=(0.55, -0.85), rim=True, fill=1.0):
    """Pixel art from a shape: `inside(x, y)` is how far into it a point is (pixels). The edge gets a dark outline, and
    a crimson rim where it faces the light; inside, a smoky texture, and `lines` (a list of (shape, color) drawn over
    it where the shape is positive). Below `smoke_from` (a share of the height) it thins into dithered smoke; `wisps`
    (a shape, or None) are faint smoke curling off it. Without `rim` the edge is as the inside, with no outline or
    catch of light; `fill` under 1 leaves it thin and see-through, as unformed shadow."""
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
            if fill < 1.0 and not dithered(x, y, fill * 1.15):
                continue
            if depth < 1.0 and rim:
                color = RIM_BRIGHT if facing > 0.75 else RIM if facing > 0.35 else OUTLINE
            else:
                color = MID if dithered(x, y, max(0.0, texture(px, py) - 0.45) * 1.6) else CORE
                for shape, line_color in lines:
                    if shape(px, py) > 0.0:
                        color = line_color
            alpha = round(248 * (0.45 + 0.55 * fill))
            if smoke > 0.0:
                keep = 1.0 - smoke
                if not dithered(x, y, keep * 1.15):
                    continue
                alpha = round(alpha * (0.5 + 0.5 * keep))
            pixels[x, y] = color + (alpha,)
    return image


# The demon's flipbook: FORM_FRAMES forming out of shadow (the last of them the whole demon), then a frame for each
# GLITCH pose. A pose moves the head (dx, dy, a turn), opens the jaw wider, hitches the shoulders, moves the elbows (and
# the hands with them), and tears rows of it sideways (from, to, shift), as a glitch. NightTerrorClient's
# DEMON_FRAME_EYES follows the head's moves: keep them in step (the script prints them).
FORM_FRAMES = 8
GLITCH = [
    {},
    {'head': (2, 0, 0.18)},
    {'head': (-3, 1, -0.1), 'jaw': 1.0},
    {'hitch': (-3, 2), 'elbows': (-3, -4, 2, 3)},
    {'head': (1, 0, 0.0), 'tears': [(24, 33, 4), (60, 66, -3)]},
    {'head': (0, 3, -0.25), 'jaw': 1.0, 'tears': [(40, 46, -3)]},
    {'elbows': (-2, -7, 2, -7), 'hitch': (-2, -2), 'jaw': 0.6},
    {'head': (-2, -2, 0.3), 'tears': [(20, 28, -5), (85, 95, 4)]},
]


def demon_eyes(pose):
    """Where the eyes are in a pose: moved with the head, and with any tear across their row."""
    dx, dy, _ = pose.get('head', (0, 0, 0.0))
    eyes = []
    for ex, ey in DEMON_EYES:
        x, y = ex + dx, ey + dy
        for y0, y1, shift in pose.get('tears', []):
            if y0 <= y < y1:
                x += shift
        eyes.append((x, y))
    return eyes


def demon(pose=None, formed=1.0):
    """One frame of the demon: in a pose, and formed this far out of shadow (0 a formless column, 1 the demon)."""
    pose = pose or {}
    hx, hy, turn = pose.get('head', (0, 0, 0.0))
    jaw = pose.get('jaw', 0.0)
    hitch_l, hitch_r = pose.get('hitch', (0, 0))
    exl, eyl, exr, eyr = pose.get('elbows', (0, 0, 0, 0))
    rng = random.Random(7)
    tendrils = [(rng.uniform(24, 40), rng.uniform(2.0, 3.6), rng.uniform(0, math.tau)) for _ in range(6)]
    lumps = smooth_noise(29, 4.0)
    shoulder_l, shoulder_r = (15, 30 + hitch_l), (49, 30 + hitch_r)
    elbow_l, elbow_r = (9 + exl, 64 + eyl), (55 + exr, 64 + eyr)
    hand_l, hand_r = (12 + exl * 0.6, 101 + eyl * 0.6), (52 + exr * 0.6, 101 + eyr * 0.6)

    def body(x, y):
        return capsule(x, y, 32, 46, 32, 84, 8.5 - max(0.0, (y - 52) / 7.0))

    def limbs(x, y):
        return max(capsule(x, y, *shoulder_l, *elbow_l, 2.8), ellipse(x, y, *elbow_l, 3.2, 3.2),
                   capsule(x, y, *elbow_l, hand_l[0], hand_l[1] - 3, 2.2),
                   capsule(x, y, *shoulder_r, *elbow_r, 2.8), ellipse(x, y, *elbow_r, 3.2, 3.2),
                   capsule(x, y, *elbow_r, hand_r[0], hand_r[1] - 3, 2.2))

    def figure(x, y):
        parts = [
            ellipse(x, y, 32 + hx, 29 + hy, 7.5, 12.0, 0.08 + turn),       # a long skull, bowed
            ellipse(x, y, 32.5 + hx, 42 + hy + jaw, 5.0, 4.5 + jaw * 1.5),  # the jaw, hanging open
            capsule(x, y, 32 + hx * 0.5, 44 + hy * 0.5, 32, 49, 3.0),      # what neck there is
            capsule(x, y, *shoulder_l, 27, 46, 5.0),                       # shoulders hunched up past the head
            capsule(x, y, *shoulder_r, 37, 46, 5.0),
            body(x, y),                                                    # a narrow body, wasting to nothing
            limbs(x, y),                                                   # long thin arms, knobbed at the elbows
        ]
        # Hands: a narrow palm, and long fingers curling in, in two joints.
        for (cx, cy), side in ((hand_l, -1), (hand_r, 1)):
            parts.append(ellipse(x, y, cx, cy, 3.2, 4.0))
            for k in range(4):
                spread = (k - 1.5) * 2.2
                knuckle = (cx + spread, cy + 2)
                joint = (cx + spread * 1.4 + side * 0.5, cy + 11)
                tip = (cx + spread * 1.2 - side * 1.5 * (k % 2 + 1), cy + 19)
                parts.append(capsule(x, y, *knuckle, *joint, 1.0))
                parts.append(capsule(x, y, *joint, *tip, 0.8))
        # Below the waist, smoke twisting down.
        for tx, radius, phase in tendrils:
            if y > 76:
                sway = 3.5 * math.sin(y / 10.0 + phase)
                parts.append(radius * (1.0 - (y - 76) / 52.0) - abs(x - (tx + sway)))
        return max(parts)

    def column(x, y):
        # Formless shadow: a lumpy column standing on a pool of it.
        return max(ellipse(x, y, 32, 78, 13, 48), ellipse(x, y, 32, 123, 20, 5))

    ease = formed * formed * (3 - 2 * formed)

    def inside(x, y):
        if formed >= 1.0:
            return figure(x, y)
        return column(x, y) * (1.0 - ease) + figure(x, y) * ease + (1.0 - ease) * 6.0 * (lumps(x, y) - 0.5)

    def wisps(x, y):
        # Smoke curling up off the shoulders.
        curls = [-1.0]
        for sx, sy, phase in ((14, 26 + hitch_l, 0.0), (50, 26 + hitch_r, 2.0)):
            if y < sy:
                curls.append(0.9 - abs(x - (sx + 2.5 * math.sin((sy - y) / 4.0 + phase))) - (sy - y) * 0.06)
        return max(curls)

    def mouth(x, y):
        return ellipse(x, y, 32.5 + hx, 40.5 + hy + jaw * 0.8, 3.4, 1.4 + jaw)

    def teeth(x, y):
        # Thin teeth hanging from the top of the mouth and rising from the bottom.
        x, y = x - hx, y - hy
        for tx in (30, 32, 34, 36):
            if abs(x - (tx + 0.5)) < 0.6 and 39.0 <= y < 40.5:
                return 1.0
        for tx in (31, 33, 35):
            if abs(x - (tx + 0.5)) < 0.6 and 41.0 + jaw * 1.6 <= y < 42.0 + jaw * 1.6:
                return 1.0
        return -1.0

    def sockets(x, y):
        return max(ellipse(x, y, ex + hx + 0.5, ey + hy + 0.5, 2.6, 2.0) for ex, ey in DEMON_EYES)

    def hollows(x, y):
        # The cheeks sunk in under the eyes.
        return max(capsule(x, y, 27 + hx, 35 + hy, 28 + hx, 38 + hy, 0.6), capsule(x, y, 38 + hx, 35 + hy, 37 + hx, 38 + hy, 0.6))

    shaped = formed >= 0.85
    lines = [(hollows, DETAIL), (sockets, OUTLINE), (mouth, OUTLINE), (teeth, TOOTH)] if shaped else []
    # Smoke curls off its shoulders only once it has them.
    image = paint(DEMON_SIZE, inside, lines, 0.86, wisps if formed >= 0.6 else None, 3, rim=formed >= 0.6, fill=0.35 + 0.65 * ease)
    width, height = DEMON_SIZE
    pixels = image.load()
    # The belly is shadow, not flesh: from under the chest down it thins into smoke, a soft fade broken up by drifting
    # noise rather than a regular pattern, fainter as it goes (the arms beside it stay whole).
    drift = smooth_noise(11, 2.5)
    for y in range(BELLY_FROM, BELLY_TO):
        t = (y - BELLY_FROM) / (BELLY_TO - BELLY_FROM)
        fade = t * t * (3 - 2 * t)
        for x in range(width):
            px, py = x + 0.5, y + 0.5
            if pixels[x, y][3] == 0 or body(px, py) < 0.0 or limbs(px, py) > -0.5:
                continue
            if drift(px, py * 0.6) < 0.75 * fade:
                pixels[x, y] = (0, 0, 0, 0)
                continue
            color = CORE if drift(px + 40, py) > 0.45 else MID
            pixels[x, y] = color + (round(pixels[x, y][3] * (1.0 - 0.45 * fade)),)
    # Claws: the fingertips' last pixels, dark red.
    if shaped:
        for x in range(width):
            for y in range(110, height - 1):
                if pixels[x, y][3] > 0 and pixels[x, y + 1][3] == 0:
                    pixels[x, y] = CLAW + (255,)
    # Glitch: rows torn sideways.
    for y0, y1, shift in pose.get('tears', []):
        for y in range(y0, y1):
            row = [pixels[x, y] for x in range(width)]
            for x in range(width):
                source = x - shift
                pixels[x, y] = row[source] if 0 <= source < width else (0, 0, 0, 0)
    return image


def demon_flipbook():
    frames = [demon(None, k / (FORM_FRAMES - 1)) for k in range(FORM_FRAMES)] + [demon(pose) for pose in GLITCH]
    width, height = DEMON_SIZE
    sheet = Image.new('RGBA', (width, height * len(frames)))
    for k, frame in enumerate(frames):
        sheet.paste(frame, (0, k * height))
    return sheet


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


# The hands' flipbook: the fingers curling from as drawn (0) to a clenched claw (1).
HAND_FRAMES = 5


def curled(points, curl, strength=1.0):
    """A finger's chain curled: each joint bends it further towards the viewer and down into the palm, so seen from in
    front it shortens and, bent far enough, hooks back down."""
    out = [points[0]]
    bend = 0.0
    for i, ((ax, ay), (bx, by)) in enumerate(zip(points, points[1:])):
        bend += curl * strength * (0.55 + 0.35 * i)
        x, y = out[-1]
        out.append((x + (bx - ax) * (1.0 - 0.25 * curl * strength), y + (by - ay) * math.cos(bend)))
    return tuple(out)


def hand(spec, curl=0.0):
    width = spec['width']
    fingers = [curled(finger, curl) for finger in spec['fingers']]
    thumb = curled(spec['thumb'], curl, 0.6)
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


VIGNETTE_EDGE = (10, 0, 3)


def vignette():
    """Dark closing in round the view: clear within half the way to the edge, then darkening smoothly to fully dark at
    it, a little lumpy, as if the dark crept in."""
    width, height = 160, 90
    lumps = smooth_noise(53, 9.0)
    image = Image.new('RGBA', (width, height))
    pixels = image.load()
    for y in range(height):
        for x in range(width):
            dx = (x + 0.5) / width * 2 - 1
            dy = (y + 0.5) / height * 2 - 1
            # Towards the nearest edge, so the edge itself is dark all along, corners and sides alike.
            reach = max(abs(dx), abs(dy)) * 0.6 + math.hypot(dx, dy) / math.sqrt(2) * 0.4
            reach += (lumps(x, y) - 0.5) * 0.12 * reach
            t = max(0.0, min(1.0, (reach - 0.45) / 0.55))
            edge = 1.0 if x in (0, width - 1) or y in (0, height - 1) else t * t * (3 - 2 * t)
            pixels[x, y] = VIGNETTE_EDGE + (round(255 * edge),)
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


ARM_SIZE = (64, 16)


def arm(upper):
    """An arm drawn along the texture, left to right: the upper arm (thick, dissolving into smoke at its left, where it
    comes out of the shadow by the bed) or the forearm (thinning to the wrist at its right)."""
    width, height = ARM_SIZE
    start, end = (5.2, 4.2) if upper else (4.4, 3.0)

    def inside(x, y):
        radius = start + (end - start) * x / width
        # A knob where the forearm meets the elbow.
        knob = ellipse(x, y, 3, 8, 4.6, 4.6) if not upper else -9.0
        return max(capsule(x, y, -4, 8, width + 4, 8, radius), knob)

    def tendons(x, y):
        return max(capsule(x, y, 8, 6.8, 60, 7.2, 0.45), capsule(x, y, 16, 9.6, 58, 9.2, 0.4))

    image = paint(ARM_SIZE, inside, [(tendons, DETAIL)], 1.0, None, 41 if upper else 43, light=(0.0, -1.0))
    pixels = image.load()
    if upper:
        # Out of the shadow: the first third thins into smoke, ever fewer and fainter pixels.
        drift = smooth_noise(47, 2.5)
        for x in range(width):
            fade = max(0.0, 1.0 - x / (width * 0.35))
            for y in range(height):
                if pixels[x, y][3] == 0:
                    continue
                if drift(x + 0.5, y + 0.5) < fade * 0.9:
                    pixels[x, y] = (0, 0, 0, 0)
                else:
                    pixels[x, y] = pixels[x, y][:3] + (round(pixels[x, y][3] * (1.0 - 0.5 * fade)),)
    return image


def hand_flipbook(spec):
    width, height = HAND_SIZE
    sheet = Image.new('RGBA', (width, height * HAND_FRAMES))
    for k in range(HAND_FRAMES):
        sheet.paste(hand(spec, k / (HAND_FRAMES - 1)), (0, k * height))
    return sheet


os.makedirs(OUT, exist_ok=True)
demon_flipbook().save(OUT + 'terror_demon.png')
for name, spec in HANDS.items():
    hand_flipbook(spec).save(OUT + 'terror_hand_' + name + '.png')
arm(True).save(OUT + 'terror_arm_upper.png')
arm(False).save(OUT + 'terror_arm_lower.png')
# The eyes of each glitch pose, for NightTerrorClient's DEMON_FRAME_EYES (pixels, left then right).
print('glitch eyes:', [demon_eyes(pose) for pose in GLITCH])
fog().save(OUT + 'terror_fog.png')
vignette().save('common/src/main/resources/assets/whycantisleep/textures/gui/sprites/hud/terror_vignette.png')
