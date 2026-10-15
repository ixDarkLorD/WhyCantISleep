"""Generates the labyrinth's five creatures as CoolCatLib armatures (deep/mob, drawn by LabyrinthMobsClient): for each,
assets/whycantisleep/armatures/<name>.{geo,animation,armature}.json and textures/armatures/<name>(_glow).png.

    mourner       a risen corpse hunched in its burial shroud, hood up, torn strips hanging from its arms, a rope at its
                  waist, its jaw slack, bony fingers reaching, blood tears down a grey face
    briar_archer  a gaunt figure of twisted, knotted roots, a ribcage of them, a wooden skull crowned with antler-like
                  branches and thorns, a quiver of arrows on its back, its bow in hand
    thornling     a knot of brambles: a thorax and a swollen abdomen bristling with thorns, six jointed legs, mandibles,
                  a cluster of six red eyes
    weeping_wisp  a tear of blood hung in the air: a shell round a bright core, a weeping face, a crown of droplets, two
                  more circling it, veins beaded with drops trailing under it
    hedge_warden  a hulking guard of clipped hedge over a stone frame: breastplate, pauldrons, bracers, fists and knee
                  caps of carved stone, a mask for a face with eyes burning in it, tufts and red berries in the hedge

Bedrock coordinates, front to the north (-z). Textures are box-UV, each face painted by its material (folds and blood
on cloth, grain and knots on root, mortar and cracks on stone, leaves and thorns on hedge and bramble), shaded at its
edges; faces ("<kind>_face" materials) are painted on a head's front, their eyes and tears in the glow texture.
"""
import json
import math
import os
import random

from PIL import Image

import gen_gardener as gg

ASSETS = gg.ASSETS
NS = gg.NS
TEX = 256
T = "q.anim_time"

PALETTES = {
    "cloth": [(52, 48, 54), (70, 66, 72), (92, 87, 90), (114, 108, 108), (132, 126, 122)],
    "skin": [(84, 92, 86), (106, 114, 104), (128, 136, 122), (150, 156, 140), (172, 176, 160)],
    "root": [(30, 22, 20), (46, 34, 28), (62, 48, 38), (80, 64, 50), (100, 82, 64)],
    "bramble": [(18, 22, 14), (30, 36, 22), (44, 52, 30), (60, 66, 38), (82, 70, 44)],
    "hedge": [(14, 22, 16), (22, 34, 24), (32, 48, 34), (44, 62, 44), (60, 78, 54)],
    "stone": [(30, 26, 38), (44, 39, 56), (58, 52, 72), (74, 67, 90), (94, 86, 112)],
    "thorn": [(60, 48, 40), (88, 72, 58), (118, 100, 82), (144, 128, 106), (170, 156, 132)],
    "string": [(120, 110, 92), (146, 136, 116), (170, 160, 140), (192, 182, 162), (210, 200, 182)],
    "bone": [(140, 130, 108), (160, 150, 124), (172, 162, 136), (186, 176, 150), (196, 186, 160)],  # the tomb's own (tomb_bone)
    "rope": [(70, 54, 36), (92, 72, 48), (114, 92, 62), (136, 112, 78), (156, 130, 92)],
    "iron": [(30, 30, 36), (44, 44, 52), (62, 62, 72), (84, 84, 96), (110, 110, 124)],
    "rust": [(64, 32, 20), (92, 48, 28), (120, 66, 36), (140, 84, 48), (110, 104, 100)],
}
GLOW = {"eye": [(255, 60, 70), (255, 150, 140)], "tear": [(150, 8, 26), (220, 28, 48)],
        "core": [(170, 14, 34), (230, 50, 64), (255, 140, 140), (255, 210, 200)], "vein": [(130, 8, 28), (200, 26, 46)],
        "drop": [(200, 20, 40), (255, 120, 120)], "berry": [(170, 20, 30), (230, 60, 60)]}
# Faces: the material the rest of the head is, and how to paint its front.
FACES = {"revenant_face": "bone", "mourner_face": "skin", "archer_face": "root", "warden_mask": "stone", "thornling_face": "bramble", "wisp_face": "core"}


class Model(gg.Model):
    def __init__(self, stretch=(1.0, 1.0, 1.0)):
        super().__init__()
        self.k = stretch  # x, y, z: a model built taller (or wider) than it is written

    def s(self, v):
        return [round(v[i] * self.k[i], 3) for i in range(3)]

    def bone(self, name, pivot, parent=None, rotation=None, locators=None):
        return super().bone(name, self.s(pivot), parent, rotation,
                            {k: self.s(v) for k, v in locators.items()} if locators else None)

    def cube(self, bone, origin, size, material, rotation=None, pivot=None, inflate=0.0):
        origin, size = self.s(origin), self.s(size)
        pivot = self.s(pivot) if pivot else None
        w, h, d = (max(1, math.ceil(v)) for v in size)
        uw, uh = 2 * (w + d), d + h
        x, y, row = self.cursor
        if x + uw > TEX:
            x, y, row = 0, y + row, 0
        self.cursor = [x + uw, y, max(row, uh)]
        assert y + uh <= TEX, "texture full"
        # Each face's place on the texture, given whole: CoolCatLib's box UV would use the cube's exact (fractional)
        # size and read past what is painted; these are the whole-pixel areas paint() fills (down written flipped, as
        # Bedrock's per-face UV has it).
        c = {"origin": origin, "size": size, "uv": {
            "up": {"uv": [x + d, y], "uv_size": [w, d]}, "down": {"uv": [x + d + w, y + d], "uv_size": [w, -d]},
            "east": {"uv": [x, y + d], "uv_size": [d, h]}, "north": {"uv": [x + d, y + d], "uv_size": [w, h]},
            "west": {"uv": [x + d + w, y + d], "uv_size": [d, h]}, "south": {"uv": [x + 2 * d + w, y + d], "uv_size": [w, h]}}}
        if rotation:
            c["rotation"] = rotation
            c["pivot"] = pivot or [origin[i] + size[i] / 2 for i in range(3)]
        if inflate:
            c["inflate"] = inflate
        bone["cubes"].append(c)
        self.cubes.append((c, (w, h, d), material, (x, y)))


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def pick(pal, k):
    return pal[max(0, min(len(pal) - 1, k))]


def base_pixel(rng, material, face, px, py, fw, fh, seed):
    """One pixel of a plain material's face, with its own pattern."""
    pal = PALETTES[material]
    k = 2 + (rng.random() < 0.3) - (rng.random() < 0.3)
    if face == "up":
        k += 1
    elif face == "down":
        k -= 1
    if material == "cloth":
        if px % 3 == (seed % 3):
            k -= 1  # folds
        if py >= fh - 1 and rng.random() < 0.5:
            return pick(pal, 0)  # frayed hem, dark
        if face != "up" and (px * 7 + seed) % 11 == 0 and py < (seed * 3 + px) % max(2, fh):
            return (96, 14, 24)  # old blood run down from above
    elif material in ("root", "thorn"):
        if (px + seed) % 3 == 0:
            k -= 1  # grain
        if rng.random() < 0.04:
            k -= 2  # a knot
    elif material == "skin":
        if rng.random() < 0.05:
            return (70, 56, 84)  # veins under it
    elif material == "stone":
        if py % 4 == 3 or (px + (py // 4) * 3) % 6 == 0:
            k = 0  # mortar
        elif rng.random() < 0.03:
            k = 0  # cracks
    elif material in ("hedge", "bramble"):
        if rng.random() < 0.12:
            k += 1 if rng.random() < 0.5 else -1
        if material == "bramble" and rng.random() < 0.07:
            return pick(PALETTES["thorn"], 3)
    elif material == "rope":
        if (px + py) % 2 == 0:
            k -= 1
    return pick(pal, k)


def face_pixel(kind, px, py, fw, fh):
    """A head's front, by kind: (colour, glows) or None to leave the base material."""
    cx = (fw - 1) / 2
    if kind == "mourner_face":
        ey = round(fh * 0.4)
        for ex in (round(cx - fw * 0.22), round(cx + fw * 0.22)):
            if abs(px - ex) <= 1 and abs(py - ey) <= 0:
                return (10, 6, 10), False  # hollow sockets
            if px == ex and py == ey:
                return (255, 70, 80), True
            if px == ex and ey < py <= ey + 3:
                return (200, 20, 40), True  # tears
        if abs(px - cx) <= 1.5 and py == round(fh * 0.78):
            return ((30, 20, 24) if (px % 2) else (190, 180, 160)), False  # mouth and teeth
    elif kind == "revenant_face":
        ey = round(fh * 0.45)
        for ex in (round(cx - fw * 0.22), round(cx + fw * 0.22)):
            if abs(px - ex) <= 1 and abs(py - ey) <= 1:
                return ((255, 60, 50), True) if (px == ex and py == ey) else ((16, 10, 8), False)  # sockets, a light in each
        if abs(px - cx) <= 0.6 and ey + 2 <= py <= ey + 3:
            return (24, 16, 12), False  # the nose's hole
        if py >= fh - 1 and abs(px - cx) <= 2:
            return ((200, 190, 166) if px % 2 else (30, 20, 16)), False  # upper teeth
    elif kind == "archer_face":
        ey = round(fh * 0.38)
        for ex in (round(cx - fw * 0.2), round(cx + fw * 0.2)):
            if abs(px - ex) <= 1 and abs(py - ey) <= 1:
                return ((255, 70, 70), True) if (px == ex and py == ey) else ((8, 4, 4), False)
        if abs(px - cx) <= 2 and py == round(fh * 0.75):
            return (10, 6, 4), False
    elif kind == "warden_mask":
        ey = round(fh * 0.35)
        if py == ey - 1 and abs(px - cx) >= 1:
            return pick(PALETTES["stone"], 4), False  # the brow
        for ex in (round(cx - fw * 0.25), round(cx + fw * 0.25)):
            if abs(px - ex) <= 1 and py == ey:
                return (255, 90, 60), True
        if abs(px - cx) <= 2 and py == round(fh * 0.72):
            return (14, 10, 18), False
        if px in (0, fw - 1) or py in (0, fh - 1):
            return pick(PALETTES["stone"], 0), False
    elif kind == "thornling_face":
        for ex, ey in ((cx - 1.5, 1), (cx + 1.5, 1), (cx - 0.5, 2), (cx + 0.5, 2), (cx - 2.5, 2), (cx + 2.5, 2)):
            if round(ex) == px and ey == py:
                return (255, 60, 70), True
    elif kind == "wisp_face":
        ey = round(fh * 0.45)
        for ex in (round(cx - fw * 0.2), round(cx + fw * 0.2)):
            if abs(px - ex) <= 0 and py == ey:
                return (60, 4, 14), False  # eyes shut
            if px == ex and ey < py <= ey + 2:
                return (255, 210, 200), True
    return None


def paint(m, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    glow = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    for n, (c, (w, h, d), material, (u, v)) in enumerate(m.cubes):
        faces = {"up": (u + d, v, w, d), "down": (u + d + w, v, w, d), "east": (u, v + d, d, h),
                 "north": (u + d, v + d, w, h), "west": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h)}
        for face, (fx, fy, fw, fh) in faces.items():
            for py in range(fh):
                for px in range(fw):
                    at = (fx + px, fy + py)
                    if material in GLOW:
                        pal = GLOW[material]
                        if material == "core":
                            r = math.hypot(px - (fw - 1) / 2, py - (fh - 1) / 2) / max(1.0, max(fw, fh) / 2)
                            col = pick(pal, int((1 - r) * len(pal)))
                        else:
                            col = pick(pal, int((1 - py / max(1, fh)) * len(pal)))
                        img.putpixel(at, col + (255,))
                        glow.putpixel(at, col + (255,))
                        continue
                    base = FACES.get(material, material)
                    if material in FACES and face == "north":
                        painted = face_pixel(material, px, py, fw, fh)
                        if painted:
                            col, glows = painted
                            img.putpixel(at, col + (255,))
                            if glows:
                                glow.putpixel(at, col + (255,))
                            continue
                    if base == "core":
                        r = math.hypot(px - (fw - 1) / 2, py - (fh - 1) / 2) / max(1.0, max(fw, fh) / 2)
                        col = pick(GLOW["core"], int((1 - r) * 4))
                        img.putpixel(at, col + (255,))
                        glow.putpixel(at, col + (255,))
                        continue
                    col = base_pixel(rng, base, face, px, py, fw, fh, n)
                    if col is None:
                        continue
                    # Shaded at its edges, darker low on its sides.
                    edge = px == 0 or py == 0 or px == fw - 1 or py == fh - 1
                    f = (0.82 if edge else 1.0) * (1.0 - 0.18 * (py / max(1, fh - 1)) if face not in ("up", "down") else 1.0)
                    img.putpixel(at, shade(col, f) + (255,))
    return img, glow


def frames(*pairs):
    return {f"{t}": {"post": list(v), "lerp_mode": "catmullrom"} for t, v in pairs}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def humanoid_walk(arms_forward=0.0, swing=32, stride=40, knee=True):
    b = {
        "right_leg": {"rotation": [f"{stride} * math.sin({T} * 360)", 0, 0]},
        "left_leg": {"rotation": [f"-{stride} * math.sin({T} * 360)", 0, 0]},
        "right_arm": {"rotation": [f"{-arms_forward} - {swing} * math.sin({T} * 360)", 0, 0]},
        "left_arm": {"rotation": [f"{-arms_forward} + {swing} * math.sin({T} * 360)", 0, 0]},
        "body": {"rotation": [4, f"4 * math.sin({T} * 360)", 0]},
    }
    if knee:
        b["right_shin"] = {"rotation": [f"math.max(0, 40 * math.sin({T} * 360 + 90))", 0, 0]}
        b["left_shin"] = {"rotation": [f"math.max(0, -40 * math.sin({T} * 360 + 90))", 0, 0]}
    return b


def legs(m, parent, xs, top, foot, material, thick=4.0, knee_cap=None):
    """Two legs in two parts (thigh and shin, bending at the knee), from `top` down to the ground."""
    for side, x in (("right_leg", xs[0]), ("left_leg", xs[1])):
        leg = m.bone(side, [x, top, 0], parent)
        mid = top / 2
        m.cube(leg, [x - thick / 2, mid, -thick / 2], [thick, top - mid, thick], material)
        shin = m.bone(side.replace("leg", "shin"), [x, mid, 0], side)
        m.cube(shin, [x - thick / 2 + 0.25, 1, -thick / 2 + 0.25], [thick - 0.5, mid - 1 + 0.5, thick - 0.5], material)
        m.cube(shin, [x - thick / 2 - 0.25, 0, -thick / 2 - 1], [thick + 0.5, 1.5, thick + 1], foot)
        if knee_cap:
            m.cube(shin, [x - thick / 2 - 0.3, mid - 2, -thick / 2 - 0.6], [thick + 0.6, 2.5, 1.2], knee_cap)


def mourner():
    m = Model()
    m.bone("root", [0, 0, 0])
    legs(m, "root", (-2, 2), 12, "skin", "cloth", thick=3.6)
    body = m.bone("body", [0, 12, 0], "root")
    m.cube(body, [-4, 12, -2], [8, 11, 4], "cloth")
    m.cube(body, [-4.5, 2, -2.6], [9, 11, 5.2], "cloth")  # the shroud's skirt
    m.cube(body, [-4.6, 12, -2.7], [9.2, 1.2, 5.4], "rope")  # a rope at the waist
    m.cube(body, [-1, 9, -2.9], [1, 4, 0.4], "rope")  # its knot, hanging
    m.cube(body, [-1, 14, -2.3], [2, 7, 0.4], "tear")  # blood down its front
    m.cube(body, [-3.5, 17, 1.8], [7, 6, 1.6], "cloth", inflate=0.1)  # the hump of its back
    head = m.bone("head", [0, 23, -1], "body")
    m.cube(head, [-3.5, 23, -4.5], [7, 7, 7], "mourner_face")
    m.cube(head, [-4.5, 23.5, -5], [9, 8, 8.5], "cloth", inflate=0.15)  # the hood
    m.cube(head, [-4.5, 30.5, -2], [9, 1.5, 5], "cloth")  # its peak, sagging back
    m.cube(head, [-3, 23.5, -5.5], [1, 6, 0.6], "cloth")  # the hood's edge either side of the face
    m.cube(head, [2, 23.5, -5.5], [1, 6, 0.6], "cloth")
    jaw = m.bone("jaw", [0, 24, -3], "head")
    m.cube(jaw, [-2.5, 21.5, -4.6], [5, 2.5, 4], "skin")
    for side, x in (("right_arm", -5.5), ("left_arm", 5.5)):
        arm = m.bone(side, [x, 22, 0], "body")
        m.cube(arm, [x - 1.5, 15, -1.5], [3, 7, 3], "cloth")  # the sleeve
        m.cube(arm, [x - 2, 13.5, -2], [4, 2, 4], "cloth", inflate=0.1)
        m.cube(arm, [x - 0.5, 9, 1.4], [1, 6, 0.4], "cloth")  # a torn strip hanging from it
        fore = m.bone(side.replace("arm", "forearm"), [x, 15, 0], side)
        m.cube(fore, [x - 1.25, 9, -1.25], [2.5, 6, 2.5], "skin")
        for i, fx in enumerate((-1, 0, 1)):
            m.cube(fore, [x + fx - 0.35, 6, -1 + i * 0.3], [0.7, 3, 0.7], "bone")  # fingers
    a = {
        "idle": {"loop": True, "animation_length": 3.0, "bones": {
            "right_arm": {"rotation": [f"-75 + 4 * math.sin({T} * 120)", 6, 0]}, "left_arm": {"rotation": [f"-70 + 4 * math.sin({T} * 120 + 60)", -6, 0]},
            "right_forearm": {"rotation": [-15, 0, 0]}, "left_forearm": {"rotation": [-20, 0, 0]},
            "head": {"rotation": [f"10 + 6 * math.sin({T} * 60)", f"14 * math.sin({T} * 40)", 0]}, "body": {"rotation": [14, 0, 0]},
            "jaw": {"rotation": [f"8 + 8 * math.max(0, math.sin({T} * 120))", 0, 0]}}},
        "walk": {"loop": True, "animation_length": 1.3, "bones": dict(humanoid_walk(arms_forward=78, swing=8, stride=26),
                                                                       body={"rotation": [16, f"5 * math.sin({T} * 277)", f"4 * math.sin({T} * 277)"]},
                                                                       head={"rotation": [8, f"10 * math.sin({T} * 277)", f"6 * math.sin({T} * 277)"]},
                                                                       jaw={"rotation": [12, 0, 0]})},
        "attack": {"loop": False, "animation_length": 0.45, "bones": {
            "right_arm": {"rotation": frames((0.0, [-75, 0, 0]), (0.15, [-125, 0, 0]), (0.28, [-55, 0, 0]), (0.45, [-75, 0, 0]))},
            "left_arm": {"rotation": frames((0.0, [-70, 0, 0]), (0.15, [-120, 0, 0]), (0.28, [-50, 0, 0]), (0.45, [-70, 0, 0]))},
            "jaw": {"rotation": frames((0.0, [8, 0, 0]), (0.15, [30, 0, 0]), (0.45, [8, 0, 0]))}}},
    }
    return m, a, {"look_at": {"head": {"max_yaw": 50, "max_pitch": 30, "smoothing": 0.15}},
                  "springs": {"right_arm": {"stiffness": 200, "damping": 14, "weight": 0.3}}}


def briar_archer():
    m = Model()
    m.bone("root", [0, 0, 0])
    legs(m, "root", (-2, 2), 12, "root", "root", thick=2.2)
    body = m.bone("body", [0, 12, 0], "root")
    m.cube(body, [-1, 12, -1], [2, 12, 2], "root")  # the spine
    for i in range(4):
        y = 14 + i * 2.4
        m.cube(body, [-3.5, y, -2], [7, 1, 4], "root", inflate=0.05)  # ribs of root
        m.cube(body, [-0.5, y - 0.5, -2.4], [1, 2, 0.5], "thorn")
    m.cube(body, [-4.5, 22, -2], [9, 2.5, 4], "root")  # the shoulders
    quiver = m.bone("quiver", [2, 22, 2], "body")
    m.cube(quiver, [0.5, 12, 1.5], [3, 9, 3], "rope", rotation=[0, 0, -20], pivot=[2, 22, 2])
    for i, (x, z) in enumerate(((1, 2), (2, 3), (2.5, 2))):
        m.cube(quiver, [x, 20, z], [0.5, 4, 0.5], "string", rotation=[0, 0, -20], pivot=[2, 22, 2])
    head = m.bone("head", [0, 24, 0], "body")
    m.cube(head, [-3, 24, -3], [6, 6.5, 6], "archer_face")
    m.cube(head, [-2.5, 23, -3.3], [5, 1.5, 3], "root")  # its jaw
    for x, z, rz, ry in ((-2.5, -1, 25, 0), (1.5, -1, -25, 0), (-3, 1, 40, 20), (2, 1, -40, -20)):
        m.cube(head, [x, 30, z], [1, 5, 1], "root", rotation=[0, ry, rz], pivot=[x + 0.5, 30, z + 0.5])  # branches
        m.cube(head, [x + (1 if rz < 0 else -1), 33, z], [1, 2, 0.6], "thorn", rotation=[0, ry, rz * 1.5], pivot=[x + 0.5, 33, z + 0.3])
    for side, x in (("right_arm", -5), ("left_arm", 5)):
        arm = m.bone(side, [x, 23, 0], "body")
        m.cube(arm, [x - 1, 16, -1], [2, 7, 2], "root")
        m.cube(arm, [x - 1.4, 21, -1.4], [2.8, 1.5, 2.8], "root")  # a knot at the shoulder
        fore = m.bone(side.replace("arm", "forearm"), [x, 16, 0], side, locators={"right_hand": [x, 10, -0.5]} if side == "right_arm" else None)
        m.cube(fore, [x - 0.9, 10, -0.9], [1.8, 6.5, 1.8], "root")
        m.cube(fore, [x - 1.3, 14.5, -1.3], [2.6, 1, 2.6], "string")  # sinew bound at the elbow
    a = {
        "idle": {"loop": True, "animation_length": 3.0, "bones": {
            "body": {"rotation": [f"4 + 2 * math.sin({T} * 120)", 0, 0]}, "right_arm": {"rotation": [f"-8 + 3 * math.sin({T} * 120)", 0, 4]},
            "left_arm": {"rotation": [f"-6 + 3 * math.sin({T} * 120 + 60)", 0, -4]}, "right_forearm": {"rotation": [-15, 0, 0]},
            "left_forearm": {"rotation": [-10, 0, 0]}, "head": {"rotation": [0, 0, f"4 * math.sin({T} * 60)"]}}},
        "walk": {"loop": True, "animation_length": 1.0, "bones": humanoid_walk(arms_forward=6, swing=26, stride=36)},
        "aim": {"loop": True, "animation_length": 2.0, "bones": {
            "right_arm": {"rotation": [-90, -8, 0]}, "left_arm": {"rotation": [-90, 30, 0]}, "body": {"rotation": [0, -10, 0]},
            "right_forearm": {"rotation": [0, 0, 0]}, "left_forearm": {"rotation": [-30, 0, 0]},
            "head": {"rotation": [0, 0, f"2 * math.sin({T} * 180)"]}}},
    }
    return m, a, {"look_at": {"head": {"max_yaw": 60, "max_pitch": 40, "smoothing": 0.2}}}


def thornling():
    m = Model()
    m.bone("root", [0, 0, 0])
    body = m.bone("body", [0, 6, 0], "root")
    m.cube(body, [-3.5, 4, -5], [7, 4.5, 5], "bramble")  # thorax
    abdomen = m.bone("abdomen", [0, 6.5, 0], "body")
    m.cube(abdomen, [-4.5, 4, 0], [9, 6, 8], "bramble")
    m.cube(abdomen, [-3.5, 9, 1], [7, 2, 6], "bramble", inflate=0.2)
    rng = random.Random(5)
    for i in range(9):
        x, z = rng.uniform(-4, 3), rng.uniform(0.5, 7)
        m.cube(abdomen, [x, 10, z], [1, rng.uniform(2, 3.5), 1], "thorn", rotation=[-25 + rng.uniform(-10, 10), 0, rng.uniform(-25, 25)],
               pivot=[x + 0.5, 10, z + 0.5])
    head = m.bone("head", [0, 6, -5], "body")
    m.cube(head, [-3, 3.5, -9], [6, 4.5, 4], "thornling_face")
    for side, x, rz in (("right_mandible", -2, 15), ("left_mandible", 1, -15)):
        mand = m.bone(side, [x + 0.5, 4, -9], "head")
        m.cube(mand, [x, 2.5, -11], [1, 1.5, 2.5], "thorn", rotation=[0, rz, 0], pivot=[x + 0.5, 3, -9])
    for i, z in enumerate((-4, -1.5, 1)):
        for side, sx in (("right", -1), ("left", 1)):
            leg = m.bone(f"{side}_leg_{i}", [3.5 * sx, 6, z], "body")
            x0 = 3.5 if sx > 0 else -10.5
            m.cube(leg, [x0, 6, z - 0.6], [7, 1.4, 1.4], "bramble", rotation=[0, 0, -30 * sx], pivot=[3.5 * sx, 6.5, z])  # up to the knee
            tip = m.bone(f"{side}_foot_{i}", [9.5 * sx, 9, z], f"{side}_leg_{i}")
            m.cube(tip, [9.5 * sx - 0.6, 0, z - 0.6], [1.2, 9, 1.2], "bramble", rotation=[0, 0, 15 * sx], pivot=[9.5 * sx, 9, z])
            m.cube(tip, [9.5 * sx - 0.4, 6, z - 1.2], [0.8, 1.6, 0.8], "thorn")
    bones = {}
    for i in range(3):
        for side, sx in (("right", -1), ("left", 1)):
            ph = (i * 120 + (0 if sx > 0 else 60))
            bones[f"{side}_leg_{i}"] = {"rotation": [0, f"18 * math.sin({T} * 720 + {ph})", f"{12 * sx} * math.max(0, math.sin({T} * 720 + {ph} + 90))"]}
    a = {
        "idle": {"loop": True, "animation_length": 2.0, "bones": {
            "body": {"position": [0, f"0.3 * math.sin({T} * 180)", 0]}, "abdomen": {"rotation": [f"4 * math.sin({T} * 180)", 0, 0]},
            "head": {"rotation": [f"6 * math.sin({T} * 90)", f"10 * math.sin({T} * 60)", 0]},
            "right_mandible": {"rotation": [0, f"10 * math.sin({T} * 400)", 0]}, "left_mandible": {"rotation": [0, f"-10 * math.sin({T} * 400)", 0]}}},
        "walk": {"loop": True, "animation_length": 0.5, "bones": dict(bones, body={"position": [0, f"0.4 * math.abs(math.sin({T} * 720))", 0]},
                                                                      abdomen={"rotation": [f"6 * math.sin({T} * 720)", 0, 0]})},
        "attack": {"loop": False, "animation_length": 0.35, "bones": {
            "head": {"rotation": frames((0.0, [0, 0, 0]), (0.12, [-25, 0, 0]), (0.22, [20, 0, 0]), (0.35, [0, 0, 0]))},
            "right_mandible": {"rotation": frames((0.0, [0, 0, 0]), (0.12, [0, 35, 0]), (0.22, [0, -10, 0]), (0.35, [0, 0, 0]))},
            "left_mandible": {"rotation": frames((0.0, [0, 0, 0]), (0.12, [0, -35, 0]), (0.22, [0, 10, 0]), (0.35, [0, 0, 0]))}}},
    }
    return m, a, {}


def weeping_wisp():
    m = Model()
    m.bone("root", [0, 0, 0])
    core = m.bone("core", [0, 8, 0], "root")
    m.cube(core, [-2, 5, -2], [4, 4, 4], "core")  # the bright heart of it
    m.cube(core, [-3.5, 4, -3.5], [7, 6, 7], "wisp_face", inflate=-0.05)  # its shell
    m.cube(core, [-2.5, 10, -2.5], [5, 2.5, 5], "tear")
    m.cube(core, [-1.5, 12.5, -1.5], [3, 2, 3], "tear")
    m.cube(core, [-0.5, 14.5, -0.5], [1, 2, 1], "drop")
    for x, z in ((-3, -3), (2, -3), (-3, 2), (2, 2)):
        m.cube(core, [x, 3, z], [1, 1.5, 1], "drop")  # its rim, dripping
    orbit = m.bone("orbit", [0, 8, 0], "core")
    for x, z in ((-6, 0), (5, 0)):
        m.cube(orbit, [x, 7, z - 0.5], [1, 1.5, 1], "drop")
    strands = m.bone("strands", [0, 4, 0], "core")
    for i, (x, z) in enumerate(((-2, -1), (1, -2), (-1, 2), (2, 1), (0, 0))):
        length = 7 + i % 3 * 2
        m.cube(strands, [x - 0.4, 4 - length, z - 0.4], [0.8, length, 0.8], "vein")
        m.cube(strands, [x - 0.6, 4 - length * 0.6, z - 0.6], [1.2, 1, 1.2], "drop")
        m.cube(strands, [x - 0.7, 3 - length, z - 0.7], [1.4, 1.4, 1.4], "drop")
    a = {
        "idle": {"loop": True, "animation_length": 2.0, "bones": {
            "core": {"position": [0, f"1.2 * math.sin({T} * 180)", 0]},
            "orbit": {"rotation": [0, f"{T} * 180", 0]},
            "strands": {"rotation": [f"10 * math.sin({T} * 180 + 90)", 0, f"8 * math.sin({T} * 150)"]}}},
        "walk": {"loop": True, "animation_length": 1.0, "bones": {
            "core": {"position": [0, f"0.8 * math.sin({T} * 360)", 0], "rotation": [12, 0, 0]},
            "orbit": {"rotation": [0, f"{T} * 360", 0]},
            "strands": {"rotation": [25, 0, f"10 * math.sin({T} * 360)"]}}},
    }
    return m, a, {}


def hedge_warden():
    m = Model()
    m.bone("root", [0, 0, 0])
    legs(m, "root", (-3.5, 3.5), 16, "stone", "hedge", thick=5.0, knee_cap="stone")
    body = m.bone("body", [0, 16, 0], "root")
    m.cube(body, [-7, 16, -5], [14, 15, 10], "hedge")
    m.cube(body, [-5, 18, -5.8], [10, 9, 1.2], "stone")  # the breastplate
    m.cube(body, [-1, 19, -6.3], [2, 7, 0.6], "stone")
    m.cube(body, [-7.5, 15, -5.5], [15, 2, 11], "stone")  # a girdle of stone
    rng = random.Random(9)
    for i in range(6):
        x, y = rng.uniform(-6, 5), rng.uniform(17, 29)
        m.cube(body, [x, y, -5.3], [1, 1, 0.5], "berry")
    for x in (-6, 0, 4):
        m.cube(body, [x, 31, -3], [2.5, 2, 5], "hedge", inflate=0.2)  # tufts on its back
    head = m.bone("head", [0, 31, -1], "body")
    m.cube(head, [-5, 31, -4.5], [10, 9, 8], "hedge", inflate=0.2)
    m.cube(head, [-4, 31.5, -6.5], [8, 8, 2], "warden_mask")
    m.cube(head, [-4.5, 38.5, -6.8], [9, 1.5, 2.5], "stone")  # the mask's brow
    for side, x in (("right_arm", -10), ("left_arm", 10)):
        arm = m.bone(side, [x, 29, 0], "body")
        m.cube(arm, [x - 3.5, 26, -4], [7, 6, 8], "stone")  # pauldron
        m.cube(arm, [x - 2.5, 18, -3], [5, 9, 6], "hedge")
        fore = m.bone(side.replace("arm", "forearm"), [x, 18, 0], side)
        m.cube(fore, [x - 2.5, 9, -3], [5, 9, 6], "hedge")
        m.cube(fore, [x - 2.8, 11, -3.3], [5.6, 4, 6.6], "stone")  # bracer
        m.cube(fore, [x - 3, 5, -3.5], [6, 5, 7], "stone")  # the fist
    a = {
        "idle": {"loop": True, "animation_length": 4.0, "bones": {
            "body": {"rotation": [f"3 + 1.5 * math.sin({T} * 90)", 0, 0]},
            "right_arm": {"rotation": [f"-4 + 2 * math.sin({T} * 90)", 0, 6]}, "left_arm": {"rotation": [f"-4 + 2 * math.sin({T} * 90 + 90)", 0, -6]},
            "right_forearm": {"rotation": [-10, 0, 0]}, "left_forearm": {"rotation": [-10, 0, 0]}}},
        "walk": {"loop": True, "animation_length": 1.6, "bones": dict(humanoid_walk(arms_forward=6, swing=18, stride=22),
                                                                       body={"rotation": [6, f"5 * math.sin({T} * 225)", f"3 * math.sin({T} * 225)"]})},
        "attack": {"loop": False, "animation_length": 0.8, "bones": {
            "right_arm": {"rotation": frames((0.0, [0, 0, 6]), (0.3, [-150, 0, 10]), (0.5, [-40, 0, 6]), (0.8, [0, 0, 6]))},
            "left_arm": {"rotation": frames((0.0, [0, 0, -6]), (0.3, [-150, 0, -10]), (0.5, [-40, 0, -6]), (0.8, [0, 0, -6]))},
            "body": {"rotation": frames((0.0, [3, 0, 0]), (0.3, [-12, 0, 0]), (0.5, [18, 0, 0]), (0.8, [3, 0, 0]))}}},
    }
    return m, a, {"look_at": {"head": {"max_yaw": 40, "max_pitch": 25, "smoothing": 0.1}}}


def tomb_revenant():
    """The one laid in the stone tomb, risen: the tomb's own skeleton (the same yellowed bone as gen_decor's
    tomb_bone), its jaw hanging open, red lights in its sockets, rags of its shroud on it, the rusted blade it was
    buried with. Tall and gaunt: written at a skeleton's size and built a quarter taller (2.5 blocks)."""
    m = Model(stretch=(1.06, 1.25, 1.06))
    m.bone("root", [0, 0, 0])
    for side, x in (("right_leg", -2), ("left_leg", 2)):
        leg = m.bone(side, [x, 12, 0], "root")
        m.cube(leg, [x - 0.75, 6, -0.75], [1.5, 6, 1.5], "bone")  # thigh
        m.cube(leg, [x - 1.1, 5.5, -1.1], [2.2, 1.2, 2.2], "bone")  # knee
        shin = m.bone(side.replace("leg", "shin"), [x, 6, 0], side)
        m.cube(shin, [x - 0.6, 1, -0.6], [1.2, 5, 1.2], "bone")
        m.cube(shin, [x - 1.2, 0, -2.4], [2.4, 1, 3.2], "bone")  # foot
    body = m.bone("body", [0, 12, 0], "root")
    m.cube(body, [-3.5, 11, -1.5], [7, 2, 3], "bone")  # pelvis
    m.cube(body, [-0.6, 12, 0], [1.2, 12, 1.2], "bone")  # spine
    for i in range(5):
        y = 15 + i * 1.7
        half = 3.5 - i * 0.2
        m.cube(body, [-half + 0.9, y, -2.2], [2 * half - 1.8, 0.9, 0.9], "bone")  # ribs: round the front,
        m.cube(body, [-half, y - 0.05, -2.2], [0.9, 1.0, 3], "bone")              # down each side to the spine
        m.cube(body, [half - 0.9, y - 0.05, -2.2], [0.9, 1.0, 3], "bone")
    m.cube(body, [-4.5, 23, -1], [9, 1.2, 2], "bone")  # collarbones
    m.cube(body, [-3.8, 9, -1.9], [7.6, 6, 0.4], "cloth")  # rags of the shroud at its waist
    m.cube(body, [1.5, 17, 1.4], [2.5, 7, 0.4], "cloth")  # and over its back
    head = m.bone("head", [0, 24, 0], "body")
    m.cube(head, [-3.5, 26, -3.5], [7, 6.5, 7], "revenant_face")
    m.cube(head, [-0.5, 24, -0.5], [1, 2, 1], "bone")  # neck
    jaw = m.bone("jaw", [0, 26.5, -1], "head")
    m.cube(jaw, [-2.5, 24.5, -3.6], [5, 2, 4], "bone")
    for side, x in (("right_arm", -5), ("left_arm", 5)):
        arm = m.bone(side, [x, 23, 0], "body")
        m.cube(arm, [x - 1.2, 21.5, -1.2], [2.4, 2.2, 2.4], "bone")  # shoulder
        m.cube(arm, [x - 0.6, 16, -0.6], [1.2, 6, 1.2], "bone")
        fore = m.bone(side.replace("arm", "forearm"), [x, 16, 0], side)
        m.cube(fore, [x - 0.9, 15.4, -0.9], [1.8, 1.2, 1.8], "bone")  # elbow
        m.cube(fore, [x - 0.5, 10.5, -0.5], [1, 5, 1], "bone")
        m.cube(fore, [x - 0.9, 8.5, -0.9], [1.8, 2.2, 1.8], "bone")  # hand
        if side == "right_arm":
            blade = m.bone("blade", [x, 9.5, 0], "right_forearm")
            m.cube(blade, [x - 0.4, 8.5, -2.5], [0.8, 2, 5], "iron")  # hilt and guard
            m.cube(blade, [x - 0.25, 9, -14], [0.5, 1.6, 11.5], "rust")  # the blade, pointing forward
    a = {
        # Lying on its back in the tomb, it sits up, draws its legs in and stands (TombRevenantEntity.RISE_TICKS).
        "rise": {"loop": "hold_on_last_frame", "animation_length": 1.7, "bones": {
            "root": {"position": frames((0.0, [0, 4, -20]), (0.6, [0, 2, -9]), (1.2, [0, -1, -2]), (1.7, [0, 0, 0])),
                     "rotation": frames((0.0, [-90, 0, 0]), (0.6, [-40, 0, 0]), (1.2, [-8, 0, 0]), (1.7, [0, 0, 0]))},
            "right_shin": {"rotation": frames((0.0, [0, 0, 0]), (0.8, [70, 0, 0]), (1.7, [0, 0, 0]))},
            "left_shin": {"rotation": frames((0.0, [0, 0, 0]), (0.9, [80, 0, 0]), (1.7, [0, 0, 0]))},
            "right_leg": {"rotation": frames((0.0, [0, 0, 0]), (0.8, [-60, 0, 0]), (1.7, [0, 0, 0]))},
            "left_leg": {"rotation": frames((0.0, [0, 0, 0]), (0.9, [-70, 0, 0]), (1.7, [0, 0, 0]))},
            "right_arm": {"rotation": frames((0.0, [0, 0, 20]), (0.5, [-30, 0, 40]), (1.2, [-20, 0, 10]), (1.7, [-10, 0, 4]))},
            "left_arm": {"rotation": frames((0.0, [0, 0, -20]), (0.5, [-30, 0, -40]), (1.2, [-20, 0, -10]), (1.7, [-10, 0, -4]))},
            "head": {"rotation": frames((0.0, [30, 0, 0]), (0.7, [-20, 0, 0]), (1.3, [10, 15, 0]), (1.7, [0, 0, 0]))},
            "jaw": {"rotation": frames((0.0, [5, 0, 0]), (1.0, [35, 0, 0]), (1.7, [12, 0, 0]))}}},
        "idle": {"loop": True, "animation_length": 3.0, "bones": {
            "body": {"rotation": [f"6 + 2 * math.sin({T} * 120)", 0, 0]},
            "head": {"rotation": [f"-4 + 4 * math.sin({T} * 60)", f"12 * math.sin({T} * 40)", f"6 * math.sin({T} * 90)"]},
            "jaw": {"rotation": [f"10 + 8 * math.max(0, math.sin({T} * 120))", 0, 0]},
            "right_arm": {"rotation": [-25, 0, 6]}, "right_forearm": {"rotation": [-30, 0, 0]},
            "left_arm": {"rotation": [f"-6 + 3 * math.sin({T} * 120)", 0, -6]}}},
        "walk": {"loop": True, "animation_length": 1.1, "bones": dict(humanoid_walk(arms_forward=10, swing=24, stride=34),
                                                                       right_arm={"rotation": [-25, 0, 6]}, right_forearm={"rotation": [-30, 0, 0]},
                                                                       jaw={"rotation": [14, 0, 0]})},
        "attack": {"loop": False, "animation_length": 0.5, "bones": {
            "right_arm": {"rotation": frames((0.0, [-25, 0, 6]), (0.18, [-150, 0, 20]), (0.32, [-40, 0, -10]), (0.5, [-25, 0, 6]))},
            "body": {"rotation": frames((0.0, [6, 0, 0]), (0.18, [-6, -20, 0]), (0.32, [14, 20, 0]), (0.5, [6, 0, 0]))},
            "jaw": {"rotation": frames((0.0, [10, 0, 0]), (0.2, [40, 0, 0]), (0.5, [10, 0, 0]))}}},
    }
    return m, a, {"look_at": {"head": {"max_yaw": 55, "max_pitch": 35, "smoothing": 0.15}}}


MOBS = {"mourner": mourner, "briar_archer": briar_archer, "thornling": thornling, "weeping_wisp": weeping_wisp, "hedge_warden": hedge_warden, "tomb_revenant": tomb_revenant}


def main():
    for i, (name, build) in enumerate(MOBS.items()):
        m, clips, extra = build()
        arm = os.path.join(ASSETS, "armatures")
        write(os.path.join(arm, f"{name}.geo.json"), {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": f"geometry.{name}", "texture_width": TEX, "texture_height": TEX}, "bones": m.bones}]})
        write(os.path.join(arm, f"{name}.animation.json"),
              {"format_version": "1.8.0", "animations": {f"animation.{name}.{k}": v for k, v in clips.items()}})
        write(os.path.join(arm, f"{name}.armature.json"), dict({
            "glow_textures": [f"{NS}:textures/armatures/{name}_glow.png"],
            "transition": {"duration": 0.2, "easing": "sine_in_out"}}, **extra))
        img, glow = paint(m, 700 + i)
        if name == "tomb_revenant":
            # Its eyes are lit by their glow alone (dark sockets under it), so they can be out while it lies in its
            # tomb and kindle as it rises (LabyrinthMobsClient dims the glow).
            for y in range(img.height):
                for x in range(img.width):
                    if glow.getpixel((x, y))[3] > 0:
                        img.putpixel((x, y), (16, 10, 8, 255))
        os.makedirs(os.path.join(ASSETS, "textures", "armatures"), exist_ok=True)
        img.save(os.path.join(ASSETS, "textures", "armatures", f"{name}.png"))
        glow.save(os.path.join(ASSETS, "textures", "armatures", f"{name}_glow.png"))
        print(name, len(m.cubes), "cubes")


if __name__ == "__main__":
    main()
