"""Builds the Weeping Gardener, the lord of the Garden of Despair: its model, texture and animations for CoolCatLib's
Armature system, and the Weeping Heart it drops.

    python tools/gen_gardener.py

It writes, under assets/whycantisleep:
    armatures/gardener.geo.json         the model: bones and box-UV cubes, Bedrock's format (front is -z)
    armatures/gardener.animation.json   its clips
    armatures/gardener.armature.json    materials, springs, head tracking, its glow texture
    textures/armatures/gardener.png      the texture, and gardener_glow.png drawn over it at full brightness
    textures/item/weeping_heart.png      the heart it drops, and its item model

The Gardener: a gaunt thing of dead sleepwood some three and a half blocks tall, on long jointed legs. A hood of
withered leaves over a pale mask with one weeping red eye, a crown of dead branches through the hood; a ribcage of
bark open at the front over a heart that glows; a tattered cloak of leaves down its back and veins hanging from its
hood; one long arm ending in a great scythe, the other in long claws.

Clips: idle, walk, dormant (kneeling, head bowed, before the weeping tree), awaken, swing (the scythe, wide), slam
(both arms down: roots burst under its foes), sink and rise (it goes down into the ground and comes up elsewhere),
roar (at half its health), death. Rotations in degrees, positions in pixels; on a limb a negative x swings it forward.
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "whycantisleep")
NS = "whycantisleep"
TEX = 256

BARK = [(34, 30, 40), (48, 43, 56), (62, 56, 72), (80, 73, 92)]
LEAF = [(14, 20, 17), (22, 32, 26), (32, 44, 34), (52, 48, 36)]
BONE = [(150, 142, 138), (178, 170, 164), (204, 197, 190), (226, 220, 214)]
CRIMSON = (138, 10, 36)
RED = (255, 45, 70)
HOT = (255, 190, 180)
VEIN = (110, 14, 30)


# ---- The model ----
class Model:
    def __init__(self):
        self.bones = []
        self.cubes = []  # (bone index, cube dict, material)
        self.cursor = [0, 0, 0]  # x, y, row height for the UV packer

    def bone(self, name, pivot, parent=None, rotation=None, locators=None):
        b = {"name": name, "pivot": pivot, "cubes": []}
        if parent:
            b["parent"] = parent
        if rotation:
            b["rotation"] = rotation
        if locators:
            b["locators"] = locators
        self.bones.append(b)
        return b

    def cube(self, bone, origin, size, material, rotation=None, pivot=None, inflate=0.0):
        w, h, d = (max(1, math.ceil(v)) for v in size)
        uw, uh = 2 * (w + d), d + h
        x, y, row = self.cursor
        if x + uw > TEX:
            x, y, row = 0, y + row, 0
        self.cursor = [x + uw, y, max(row, uh)]
        assert y + uh <= TEX, "texture full"
        c = {"origin": origin, "size": size, "uv": [x, y]}
        if rotation:
            c["rotation"] = rotation
            c["pivot"] = pivot or [origin[i] + size[i] / 2 for i in range(3)]
        if inflate:
            c["inflate"] = inflate
        bone["cubes"].append(c)
        self.cubes.append((c, (w, h, d), material))


# The cloak's lengths: (length, half its width, rest turn back from its parent's, degrees).
CLOAK = [(7, 8, 32), (7, 8.5, -16), (7, 9, -20), (7, 9.5, -10)]
CLOAK_COLLIDERS = [{"point": "back_upper", "radius": 5.0}, {"point": "back_lower", "radius": 5.0},
                   {"point": "hips_back", "radius": 4.8}, {"point": "right_leg_back", "radius": 3.2},
                   {"point": "left_leg_back", "radius": 3.2}] + [{"point": f"spike_{i}", "radius": 1.8} for i in range(4)]


def build():
    m = Model()
    m.bone("root", [0, 0, 0])
    m.bone("hips", [0, 30, 0], "root")
    hips = m.bones[-1]
    m.cube(hips, [-5, 27, -3], [10, 5, 6], "bark")
    # Legs, jointed at hip, knee and ankle: a thigh, a long shin, a splayed foot of roots. Each joint is a rounded
    # knuckle of wood, turned 45 degrees about the way it bends, and the lengths either side overlap into it, so the
    # limb bends there cleanly with no gap.
    for side, sx in (("right", -1), ("left", 1)):
        x = 3 * sx
        thigh = m.bone(f"{side}_leg", [x, 30, 0], "hips")
        m.cube(thigh, [x - 2, 17, -2], [4, 14, 4], "bark")
        m.cube(thigh, [x - 2.5, 28, -2.5], [5, 4, 5], "bark", rotation=[45, 0, 0], pivot=[x, 30, 0])  # the hip joint
        shin = m.bone(f"{side}_shin", [x, 18, 0], f"{side}_leg")
        m.cube(shin, [x - 2.2, 16.5, -2.2], [4.4, 3, 3], "bark", rotation=[45, 0, 0], pivot=[x, 18, 0])  # the knee
        m.cube(shin, [x - 1.5, 5, -1.5], [3, 13.5, 3], "bark")
        foot = m.bone(f"{side}_foot", [x, 5, 0], f"{side}_shin")
        m.cube(foot, [x - 1.8, 3.6, -1.8], [3.6, 2.8, 2.8], "bark", rotation=[45, 0, 0], pivot=[x, 5, 0])  # the ankle
        m.cube(foot, [x - 2.5, 0, -4], [5, 4, 6], "bark")
        m.cube(foot, [x - 2.5, 0, -6], [1, 1, 2], "root")
        m.cube(foot, [x + 1.5, 0, -6], [1, 1, 2], "root")
        m.cube(foot, [x - 0.5, 0, -7], [1, 1, 3], "root")
        m.cube(foot, [x - 0.5, 0, 2], [1, 1, 2], "root")  # a spur at the heel
    # The body bends twice: low at the waist (torso) and again under the ribs (chest).
    torso = m.bone("torso", [0, 32, 0], "hips")
    m.cube(torso, [-1.5, 31, 0.5], [3, 9, 3], "bark")  # the lower spine
    for i in range(2):
        y = 34 + i * 2.6
        m.cube(torso, [-6, y, -4], [2, 1.5, 7], "bark")
        m.cube(torso, [4, y, -4], [2, 1.5, 7], "bark")
        m.cube(torso, [-4.5, y, -4.5], [3, 1.5, 1], "bark")
        m.cube(torso, [1.5, y, -4.5], [3, 1.5, 1], "bark")
    chest = m.bone("chest", [0, 39, 1], "torso")
    m.cube(chest, [-1.5, 38, 0.5], [3, 9, 3], "bark")  # the upper spine
    m.cube(chest, [-2, 38, 0], [4, 3, 4], "bark", rotation=[45, 0, 0], pivot=[0, 39.5, 2])  # where the spine bends
    for i in range(2, 4):
        y = 34 + i * 2.6
        m.cube(chest, [-6, y, -4], [2, 1.5, 7], "bark")
        m.cube(chest, [4, y, -4], [2, 1.5, 7], "bark")
        m.cube(chest, [-4.5, y, -4.5], [3, 1.5, 1], "bark")
        m.cube(chest, [1.5, y, -4.5], [3, 1.5, 1], "bark")
    m.cube(chest, [-8, 44, -3.5], [16, 4, 7], "bark")  # shoulders
    # Plates of bark over the shoulders, a collar of dead leaves, and thorns down the spine.
    m.cube(chest, [-11, 44, -4], [5, 3, 8], "bark", rotation=[0, 0, 18], pivot=[-8, 46, 0])
    m.cube(chest, [6, 44, -4], [5, 3, 8], "bark", rotation=[0, 0, -18], pivot=[8, 46, 0])
    m.cube(chest, [-6, 46, -4.5], [12, 3, 9], "leaf", inflate=0.2)
    # (Set low enough down the back that the cloak, hung from the shoulders, falls clear over the top one.)
    for i in range(4):
        bone = torso if i < 2 else chest
        y = 30 + i * 3.2
        m.cube(bone, [-0.5, y, 3], [1, 1, 4], "root", rotation=[35, 0, 0], pivot=[0, y + 0.5, 3])
        # A collider at its point, for the cloak to drape over (see CLOAK below).
        bone.setdefault("locators", {})[f"spike_{i}"] = [0, y + 0.5 + 2.3, 6.3]
    # Colliders for the cloak: the back, the small of it, the hips, the thighs.
    chest.setdefault("locators", {})["back_upper"] = [0, 41, -1]
    torso.setdefault("locators", {})["back_lower"] = [0, 34, -1]
    hips.setdefault("locators", {})["hips_back"] = [0, 29, -0.5]
    for b_ in m.bones:
        if b_["name"] in ("right_leg", "left_leg"):
            b_.setdefault("locators", {})[b_["name"] + "_back"] = [b_["pivot"][0], 23, 0]
    m.cube(chest, [-6.5, 40, 2], [13, 5, 2], "vein")
    heart = m.bone("heart", [0, 40, -1], "chest", locators={"heart_core": [0, 40, -1]})
    m.cube(heart, [-2, 38, -3], [4, 4, 4], "heart")
    m.cube(heart, [-1.5, 38.5, -3.5], [3, 3, 1], "heart")
    # The cloak: four lengths, each on its own spring (gardener.armature.json), so it flows and folds as it moves.
    # Hung from behind the shoulders, its first length slopes back over the thorns of the spine, the rest falling
    # nearly straight; colliders on the back, the thorns, the hips and thighs keep it out of the body as it swings.
    # (Cubes are drawn hanging straight down from each joint; the joints' rest turns slope them.)
    parent, top = "chest", 48
    for i, (length, half, turn) in enumerate(CLOAK):
        seg = m.bone(f"cloak_{i}", [0, top, 4.5], parent, rotation=[turn, 0, 0])
        m.cube(seg, [-half, top - length - 0.4, 4.5], [2 * half, length + 0.4, 1], "leaf")  # overlapping the next
        parent, top = f"cloak_{i}", top - length
    for i, x in enumerate((-9, -5.5, -2, 1.5, 5, 7.5)):
        m.cube(m.bones[-1], [x, top - 7 + (i % 2) * 3, 4.6], [2, 7 - (i % 2) * 3, 0.5], "leaf")
    neck = m.bone("neck", [0, 48, 0], "chest")
    m.cube(neck, [-1.5, 47, -1.5], [3, 4, 3], "bark")
    head = m.bone("head", [0, 51, -1], "neck", locators={"eye": [-1.5, 55.5, -7]})
    m.cube(head, [-5, 50, -6], [10, 11, 10], "leaf", inflate=0.3)
    m.cube(head, [-3.5, 51, -6.7], [7, 8, 1], "mask")
    m.cube(head, [-4, 60, -1], [1, 9, 1], "bark", rotation=[0, 0, 22], pivot=[-3.5, 60, -0.5])
    m.cube(head, [3, 60, -1], [1, 9, 1], "bark", rotation=[0, 0, -22], pivot=[3.5, 60, -0.5])
    m.cube(head, [-0.5, 60, 1], [1, 12, 1], "bark", rotation=[-14, 0, 0], pivot=[0, 60, 1.5])
    m.cube(head, [-7, 66, -1], [3, 1, 1], "bark", rotation=[0, 0, -30], pivot=[-5, 66, -0.5])
    m.cube(head, [4, 66, -1], [3, 1, 1], "bark", rotation=[0, 0, 30], pivot=[5, 66, -0.5])
    veil = m.bone("veil", [0, 51, -5.5], "head")
    for x in (-4.5, -2.5, 1.5, 3.5):
        m.cube(veil, [x, 38 + abs(x) * 0.6, -6.9], [1, 13 - abs(x) * 0.6, 0.5], "vein")
    # Arms, jointed at shoulder, elbow and wrist, each joint a knuckle the lengths overlap into. The right hand holds
    # the scythe (an OBJ model on its "grip" point, gardener.armature.json); the left ends in long claws.
    for side, sx in (("right", -1), ("left", 1)):
        x = 9 * sx
        arm = m.bone(f"{side}_arm", [x, 46, 0], "chest")
        m.cube(arm, [x - 2.2, 43.5, -2.2], [4.4, 4, 4.4], "bark", rotation=[0, 0, 45], pivot=[x, 46, 0])  # the shoulder
        m.cube(arm, [x - 1.5, 33.5, -1.5], [3, 12.5, 3], "bark")
        fore = m.bone(f"{side}_forearm", [x, 34, 0], f"{side}_arm")
        m.cube(fore, [x - 2, 32.6, -2], [4, 2.8, 2.8], "bark", rotation=[45, 0, 0], pivot=[x, 34, 0])  # the elbow
        m.cube(fore, [x - 1.5, 21, -1.5], [3, 13.5, 3], "bark")
        for k in range(3):
            m.cube(fore, [x + 1.2 * sx, 23 + k * 4, -0.5], [2, 1, 1], "root", rotation=[0, 0, -30 * sx], pivot=[x + 1.5 * sx, 23.5 + k * 4, 0])
        hand = m.bone(f"{side}_hand", [x, 21, 0], f"{side}_forearm")
        m.cube(hand, [x - 1.8, 19.6, -1.8], [3.6, 2.8, 2.8], "bark", rotation=[45, 0, 0], pivot=[x, 21, 0])  # the wrist
        m.cube(hand, [x - 2, 16, -2], [4, 4.5, 4], "bark")
        for f, fx in enumerate((-1.5, 0, 1.5)):
            length = 9 if side == "left" else 4
            finger = m.bone(f"{side}_finger_{f}", [x + fx, 16, -1 + f * 0.5], f"{side}_hand")
            m.cube(finger, [x + fx - 0.5, 16 - length, -1.5 + f * 0.5], [1, length, 1], "root")
        if side == "right":
            m.cube(hand, [x - 2.5, 16.5, -2.6], [1, 3, 1], "root")  # the thumb, round the shaft
    # The scythe's own joint, in the right hand, which the clips turn; the model itself is an OBJ on its grip.
    # Its grip point is on this joint (not the hand), so the clips' turns of it carry the scythe.
    m.bone("scythe", [-9, 18, 0], "right_hand", locators={"grip": [-9, 18, 0], "blade_tip": [-9.5, 49, -33]})
    return m


# ---- The texture ----
STEEL = [(30, 29, 36), (52, 51, 60), (82, 81, 94), (130, 130, 144), (200, 202, 214), (238, 238, 246)]
PALE = [(78, 72, 84), (98, 92, 104), (120, 114, 126), (142, 136, 148)]
LEATHER = [(46, 16, 22), (70, 24, 32), (96, 38, 44)]


def shade(c, f):
    return tuple(max(0, min(255, int(c[i] * f))) for i in range(3))


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def paint(m):
    """Each face of each cube, its material's pattern, then lit as a block in the world is: tops brightest,
    undersides darkest, sides fading from a lit upper edge down into shadow, corners a little darker."""
    rng = random.Random(77)
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    glow = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))

    def put(im, x, y, c):
        if 0 <= x < TEX and 0 <= y < TEX:
            im.putpixel((x, y), c if len(c) == 4 else c + (255,))

    def pick(palette, weights):
        return palette[rng.choices(range(len(palette)), weights[:len(palette)])[0]]

    for c, (w, h, d), material in m.cubes:
        u, v = c["uv"]
        faces = {  # name: (x, y, width, height)
            "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
        }
        knot = (rng.randrange(max(1, w)), rng.randrange(max(1, h)))
        for name, (fx, fy, fw, fh) in faces.items():
            side = name not in ("top", "bottom")
            grain_shift = [rng.randrange(4) for _ in range(fw + 1)]
            for y in range(fh):
                for x in range(fw):
                    px, py = fx + x, fy + y
                    t = y / max(fh - 1, 1)
                    if material == "bark":
                        col = pick(BARK, (2, 5, 4, 1))
                        # Grain: dark furrows running down it, wandering a little.
                        if (x * 3 + (y + grain_shift[x]) // 5) % 4 == 0:
                            col = shade(BARK[0], 0.8)
                        if side and fw >= 3 and fh >= 6 and abs(x - knot[0] % fw) + abs(y - knot[1] % fh) <= 1:
                            col = (20, 16, 24) if (x, y) == (knot[0] % fw, knot[1] % fh) else BARK[3]
                        if name == "top" and rng.random() < 0.35:
                            col = pick(LEAF, (1, 3, 3, 1))  # moss
                        if side and rng.random() < 0.012:
                            col = VEIN
                            put(glow, px, py, (*CRIMSON, 160))
                    elif material == "shaft":
                        col = pick(PALE, (2, 5, 4, 2))
                        if (y + 2) % 7 == 0:
                            col = shade(PALE[0], 0.7)  # rings in the wood
                        elif (x + y // 4) % 3 == 0:
                            col = shade(col, 0.88)
                    elif material == "leaf":
                        # Clumps: each 3x3 a heap, lit at its top-left, dark in the gaps between.
                        cx, cy = x % 3, y % 3
                        col = LEAF[2] if (cx, cy) == (0, 0) else LEAF[1] if cx + cy < 3 else LEAF[0]
                        if rng.random() < 0.25:
                            col = pick(LEAF, (2, 4, 3, 1))
                        if rng.random() < 0.05:
                            col = (70, 24, 32)
                    elif material == "root":
                        col = pick(BARK, (4, 4, 2, 0))
                    elif material == "mask":
                        col = mix(BONE[3], BONE[1], t)
                        if rng.random() < 0.18:
                            col = mix(col, BONE[0], 0.5)
                    elif material == "blade":
                        # Steel: a dark spine along the top, brightening down to the honed edge.
                        if side and name in ("left", "right"):
                            col = mix(STEEL[1], STEEL[3], t * t)
                            if y == fh - 1:
                                col = STEEL[5]
                            elif y == fh - 2 and fh > 3:
                                col = STEEL[4]
                            elif y == 0:
                                col = STEEL[0]
                            if fh >= 4 and y == fh // 2 - 1 and x % 3 != 2 and rng.random() < 0.6:
                                col = (90, 16, 30)  # runes etched in it, smouldering
                                put(glow, px, py, (*CRIMSON, 150))
                        elif name == "bottom":
                            col = STEEL[5]
                        else:
                            col = STEEL[2]
                    elif material == "steel":
                        col = mix(STEEL[2], STEEL[1], t)
                        if y == 0 and side:
                            col = STEEL[3]
                        if (x + y) % 4 == 0 and fw > 1 and fh > 1 and rng.random() < 0.5:
                            col = STEEL[4]  # a rivet
                    elif material == "grip":
                        band = (x + y) % 4
                        col = LEATHER[2] if band == 0 else LEATHER[1] if band < 3 else LEATHER[0]
                    elif material == "gem":
                        r = ((x - fw / 2 + 0.5) ** 2 + (y - fh / 2 + 0.5) ** 2) ** 0.5
                        col = HOT if r < 0.8 else RED
                        put(glow, px, py, col + (255,))
                    elif material == "vein":
                        col = VEIN if rng.random() < 0.7 else CRIMSON
                        if rng.random() < 0.4:
                            put(glow, px, py, (*RED, 120))
                    else:  # heart
                        r = ((x - fw / 2 + 0.5) ** 2 + (y - fh / 2 + 0.5) ** 2) ** 0.5 / max(fw, fh)
                        col = HOT if r < 0.2 else RED if r < 0.45 else CRIMSON
                        put(glow, px, py, col + (255,))
                    # Light: as the world lights a block, with a lit upper edge and darker corners.
                    if material not in ("heart", "gem"):
                        f = {"top": 1.18, "bottom": 0.58}.get(name, 1.06 - 0.32 * t)
                        if side and y == 0:
                            f *= 1.18
                        if side and fw > 2 and x in (0, fw - 1):
                            f *= 0.84
                        if name == "top" and (x == 0 or y == 0):
                            f *= 1.08
                        if material == "mask" and name == "front" and y <= 1:
                            f *= 0.55  # the hood's shadow on the brow
                        col = shade(col, f)
                    put(img, px, py, col)
            if material == "mask" and name == "front":
                # The face: two deep hollows under a carved brow, the left with a red eye in it, weeping a red
                # streak down the mask; cracks across it; a thin slit of a mouth.
                for x in range(0, fw):
                    put(img, fx + x, fy + 1, shade(BONE[0], 0.55))
                for ex, lit in ((1, True), (4, False)):
                    for y in range(2, 4):
                        for x in range(ex, ex + 2):
                            put(img, fx + x, fy + y, (10, 6, 10) if y == 3 else (24, 18, 22))
                    if lit:
                        put(img, fx + ex, fy + 3, RED)
                        put(img, fx + ex + 1, fy + 3, CRIMSON)
                        put(glow, fx + ex, fy + 3, RED + (255,))
                        put(glow, fx + ex + 1, fy + 3, CRIMSON + (220,))
                        for y in range(4, fh):
                            put(img, fx + ex, fy + y, CRIMSON if y < fh - 1 else VEIN)
                            put(glow, fx + ex, fy + y, CRIMSON + (max(60, 200 - y * 20),))
                for (x, y) in ((5, 4), (6, 5), (5, 6), (2, 0), (3, 1)):
                    if x < fw and y < fh:
                        put(img, fx + x, fy + y, shade(BONE[0], 0.7))
                for x in range(2, 5):
                    put(img, fx + x, fy + 6, (34, 24, 28))
    return img, glow


# ---- The clips ----
def frames(*pairs, lerp="catmullrom"):
    return {f"{t}": {"post": list(v), "lerp_mode": lerp} for t, v in pairs}


def clips():
    a = {}
    T = "q.anim_time"
    # How the scythe sits in the fist: the shaft across it (the grip point is the scythe joint's, so this turn carries
    # the OBJ). Left at 0 the shaft would run up the forearm and through the shoulder; every clip sets it.
    HOLD = [60, 0, 0]
    # Standing, it never quite straightens: hips sunk, knees bent, feet kept flat, back curled over, elbows bent,
    # the scythe's hand held out. It breathes, its head turns, its fingers twitch.
    a["idle"] = {"loop": True, "animation_length": 4.0, "bones": {
        "hips": {"position": [0, f"-1.6 + 0.3 * math.sin({T} * 90)", 0]},
        "right_leg": {"rotation": [-16, 0, 3]}, "left_leg": {"rotation": [-10, 0, -3]},
        "right_shin": {"rotation": [28, 0, 0]}, "left_shin": {"rotation": [20, 0, 0]},
        "right_foot": {"rotation": [-12, 0, 0]}, "left_foot": {"rotation": [-10, 0, 0]},
        "torso": {"rotation": [f"6 + 2 * math.sin({T} * 90)", f"3 * math.sin({T} * 45)", 0]},
        "chest": {"rotation": [f"10 + 2 * math.sin({T} * 90 + 30)", 0, 0]},
        "head": {"rotation": [f"-6 + 3 * math.sin({T} * 90 + 60)", f"10 * math.sin({T} * 30)", f"4 * math.sin({T} * 52)"]},
        "right_arm": {"rotation": [f"-14 + 3 * math.sin({T} * 90)", 0, f"8 + 2 * math.sin({T} * 70)"]},
        "left_arm": {"rotation": [f"-6 + 3 * math.sin({T} * 90 + 90)", 0, f"-10 - 2 * math.sin({T} * 70)"]},
        "right_forearm": {"rotation": [-38, 0, 0]}, "left_forearm": {"rotation": [-30, 0, 0]},
        "right_hand": {"rotation": [10, 0, 0]}, "left_hand": {"rotation": [f"-10 + 4 * math.sin({T} * 120)", 0, 0]},
        "left_finger_0": {"rotation": [f"-15 + 10 * math.sin({T} * 200)", 0, 0]},
        "left_finger_1": {"rotation": [f"-20 + 12 * math.sin({T} * 200 + 50)", 0, 0]},
        "left_finger_2": {"rotation": [f"-15 + 10 * math.sin({T} * 200 + 100)", 0, 0]},
        "heart": {"scale": [f"1 + 0.12 * math.pow(math.max(0, math.sin({T} * 180)), 6)"] * 3},
        "scythe": {"rotation": HOLD},
    }}
    # Long, stalking strides: the knee folds as the leg swings through, the ankle keeps the foot to the ground.
    swing_r = f"(-30 * math.sin({T} * 225) - 10)"
    knee_r = f"(14 + math.max(0, 46 * math.sin({T} * 225 + 90)))"
    swing_l = f"(30 * math.sin({T} * 225) - 10)"
    knee_l = f"(14 + math.max(0, -46 * math.sin({T} * 225 + 90)))"
    a["walk"] = {"loop": True, "animation_length": 1.6, "bones": {
        "right_leg": {"rotation": [swing_r, 0, 0]}, "left_leg": {"rotation": [swing_l, 0, 0]},
        "right_shin": {"rotation": [knee_r, 0, 0]}, "left_shin": {"rotation": [knee_l, 0, 0]},
        "right_foot": {"rotation": [f"-({swing_r} + {knee_r}) * 0.8", 0, 0]},
        "left_foot": {"rotation": [f"-({swing_l} + {knee_l}) * 0.8", 0, 0]},
        "hips": {"position": [0, f"-2 - 1.2 * math.abs(math.sin({T} * 225))", 0]},
        "torso": {"rotation": [12, f"-6 * math.sin({T} * 225)", 0]},
        "chest": {"rotation": [8, f"-4 * math.sin({T} * 225)", 0]},
        "head": {"rotation": [-8, f"5 * math.sin({T} * 225)", 0]},
        "right_arm": {"rotation": [f"-14 + 16 * math.sin({T} * 225)", 0, 8]},
        "left_arm": {"rotation": [f"-10 - 22 * math.sin({T} * 225)", 0, -10]},
        "right_forearm": {"rotation": [-40, 0, 0]},
        "left_forearm": {"rotation": [f"-30 - 15 * math.max(0, math.sin({T} * 225))", 0, 0]},
        "scythe": {"rotation": [HOLD[0] + 6, 0, 0]},
    }}
    dormant = {
        "root": {"position": [0, -13, 0]},
        "right_leg": {"rotation": [-80, 0, 6]}, "right_shin": {"rotation": [84, 0, 0]}, "right_foot": {"rotation": [-4, 0, 0]},
        "left_leg": {"rotation": [8, 0, -4]}, "left_shin": {"rotation": [92, 0, 0]}, "left_foot": {"rotation": [40, 0, 0]},
        "torso": {"rotation": [20, 0, 0]}, "chest": {"rotation": [18, 0, 0]}, "head": {"rotation": [38, 0, 0]},
        "right_arm": {"rotation": [-38, 0, 10]}, "right_forearm": {"rotation": [-30, 0, 0]},
        "left_arm": {"rotation": [-14, 0, -18]}, "left_forearm": {"rotation": [-40, 0, 0]}, "left_hand": {"rotation": [-20, 0, 0]},
        "scythe": {"rotation": [62, 0, 0]},
    }
    a["dormant"] = {"loop": True, "animation_length": 6.0, "bones": dict(dormant, heart={"scale": [f"0.85 + 0.08 * math.max(0, math.sin({T} * 60))"] * 3})}
    standing = {"root": [0, 0, 0], "right_leg": [-16, 0, 0], "right_shin": [28, 0, 0], "right_foot": [-12, 0, 0],
                "left_leg": [-10, 0, 0], "left_shin": [20, 0, 0], "left_foot": [-10, 0, 0],
                "torso": [4, 0, 0], "chest": [-10, 0, 0], "head": [-30, 0, 0], "right_arm": [-30, 0, 40], "right_forearm": [-40, 0, 0],
                "left_arm": [-30, 0, -40], "left_forearm": [-35, 0, 0], "left_hand": [-10, 0, 0], "scythe": HOLD}
    awaken = {}
    for joint, pose in dormant.items():
        key = "position" if joint == "root" else "rotation"
        start = pose[key]
        end = standing[joint]
        settle = end if joint == "scythe" else [v * 0.3 for v in end]  # the scythe keeps its hold
        awaken[joint] = {key: frames((0.0, start), (0.6, start), (1.8, end), (2.4, settle))}
    awaken["heart"] = {"scale": frames((0.0, [0.9] * 3), (1.6, [1.0] * 3), (1.8, [1.7] * 3), (2.4, [1.0] * 3))}
    a["awaken"] = {"loop": False, "animation_length": 2.4, "bones": awaken}
    a["swing"] = {"loop": False, "animation_length": 1.2, "bones": {
        "torso": {"rotation": frames((0.0, [4, 0, 0]), (0.4, [8, 45, 0]), (0.55, [14, -50, 0]), (0.8, [10, -40, 0]), (1.2, [4, 0, 0]))},
        "right_arm": {"rotation": frames((0.0, [-10, 0, 8]), (0.4, [-120, 30, 70]), (0.55, [-80, -20, 20]), (0.8, [-60, -30, 10]), (1.2, [-10, 0, 8]))},
        "right_forearm": {"rotation": frames((0.0, [-18, 0, 0]), (0.4, [-40, 0, 0]), (0.55, [-5, 0, 0]), (1.2, [-18, 0, 0]))},
        "scythe": {"rotation": frames((0.0, HOLD), (0.4, [80, 0, 0]), (0.55, [80, 0, 0]), (0.8, [75, 0, 0]), (1.2, HOLD))},
        "left_arm": {"rotation": frames((0.0, [-4, 0, -8]), (0.4, [-30, 0, -50]), (0.55, [10, 0, -20]), (1.2, [-4, 0, -8]))},
        "head": {"rotation": frames((0.0, [8, 0, 0]), (0.4, [0, 30, 0]), (0.55, [10, -30, 0]), (1.2, [8, 0, 0]))},
    }}
    a["slam"] = {"loop": False, "animation_length": 1.4, "bones": {
        "scythe": {"rotation": HOLD},
        "right_arm": {"rotation": frames((0.0, [-10, 0, 8]), (0.5, [-170, 0, 20]), (0.75, [-50, 0, 8]), (1.1, [-50, 0, 8]), (1.4, [-10, 0, 8]))},
        "left_arm": {"rotation": frames((0.0, [-4, 0, -8]), (0.5, [-165, 0, -42]), (0.75, [-50, 0, -8]), (1.1, [-50, 0, -8]), (1.4, [-4, 0, -8]))},
        "torso": {"rotation": frames((0.0, [4, 0, 0]), (0.5, [-18, 0, 0]), (0.75, [40, 0, 0]), (1.1, [36, 0, 0]), (1.4, [4, 0, 0]))},
        "head": {"rotation": frames((0.0, [8, 0, 0]), (0.5, [-30, 0, 0]), (0.75, [20, 0, 0]), (1.4, [8, 0, 0]))},
        "hips": {"position": frames((0.0, [0, 0, 0]), (0.75, [0, -4, 0]), (1.1, [0, -4, 0]), (1.4, [0, 0, 0]))},
        "right_leg": {"rotation": frames((0.0, [0, 0, 0]), (0.75, [-20, 0, 0]), (1.4, [0, 0, 0]))},
        "left_leg": {"rotation": frames((0.0, [0, 0, 0]), (0.75, [16, 0, 0]), (1.4, [0, 0, 0]))},
        "right_shin": {"rotation": frames((0.0, [0, 0, 0]), (0.75, [30, 0, 0]), (1.4, [0, 0, 0]))},
        "left_shin": {"rotation": frames((0.0, [0, 0, 0]), (0.75, [24, 0, 0]), (1.4, [0, 0, 0]))},
        "heart": {"scale": frames((0.0, [1, 1, 1]), (0.75, [1.6, 1.6, 1.6]), (1.4, [1, 1, 1]))},
    }}
    a["sink"] = {"loop": "hold_on_last_frame", "animation_length": 1.0, "bones": {
        "scythe": {"rotation": HOLD},
        "root": {"position": frames((0.0, [0, 0, 0]), (0.25, [0, 2, 0]), (1.0, [0, -64, 0]))},
        "right_arm": {"rotation": frames((0.0, [-10, 0, 8]), (0.25, [-20, 0, 60]), (1.0, [-160, 0, 20]))},
        "left_arm": {"rotation": frames((0.0, [-4, 0, -8]), (0.25, [-20, 0, -60]), (1.0, [-155, 0, -45]))},
        "head": {"rotation": frames((0.0, [8, 0, 0]), (0.25, [-30, 0, 0]), (1.0, [-40, 0, 0]))},
    }}
    a["rise"] = {"loop": False, "animation_length": 1.2, "bones": {
        "scythe": {"rotation": HOLD},
        "root": {"position": frames((0.0, [0, -64, 0]), (0.7, [0, 3, 0]), (1.0, [0, 0, 0]))},
        "right_arm": {"rotation": frames((0.0, [-160, 0, 20]), (0.7, [-40, 0, 70]), (1.2, [-10, 0, 8]))},
        "left_arm": {"rotation": frames((0.0, [-155, 0, -45]), (0.7, [-40, 0, -70]), (1.2, [-4, 0, -8]))},
        "head": {"rotation": frames((0.0, [-40, 0, 0]), (0.7, [-25, 0, 0]), (1.2, [8, 0, 0]))},
    }}
    a["roar"] = {"loop": False, "animation_length": 1.8, "bones": {
        "scythe": {"rotation": HOLD},
        "torso": {"rotation": frames((0.0, [4, 0, 0]), (0.4, [-20, 0, 0]), (1.4, [-20, 0, 0]), (1.8, [4, 0, 0]))},
        "head": {"rotation": [f"q.anim_time < 1.4 ? -45 + 4 * math.sin(q.anim_time * 2400) : 8", 0, 0]},
        "right_arm": {"rotation": frames((0.0, [-10, 0, 8]), (0.4, [-50, 0, 80]), (1.4, [-55, 0, 85]), (1.8, [-10, 0, 8]))},
        "left_arm": {"rotation": frames((0.0, [-4, 0, -8]), (0.4, [-50, 0, -80]), (1.4, [-55, 0, -85]), (1.8, [-4, 0, -8]))},
        "heart": {"scale": frames((0.0, [1, 1, 1]), (0.4, [1.9, 1.9, 1.9]), (1.4, [1.7, 1.7, 1.7]), (1.8, [1, 1, 1]))},
    }}
    a["death"] = {"loop": "hold_on_last_frame", "animation_length": 2.6, "bones": {
        "scythe": {"rotation": HOLD},
        "root": {"position": frames((0.0, [0, 0, 0]), (0.8, [0, -13, 0]), (2.0, [0, -13, 0]), (2.6, [0, -22, 4])),
                 "rotation": frames((0.0, [0, 0, 0]), (0.8, [0, 0, 0]), (2.0, [10, 0, 0]), (2.6, [75, 0, 0]))},
        "right_leg": {"rotation": frames((0.0, [0, 0, 0]), (0.8, [-80, 0, 6]))},
        "right_shin": {"rotation": frames((0.0, [0, 0, 0]), (0.8, [84, 0, 0]))},
        "left_leg": {"rotation": frames((0.0, [0, 0, 0]), (0.8, [8, 0, -4]))},
        "left_shin": {"rotation": frames((0.0, [0, 0, 0]), (0.8, [92, 0, 0]))},
        "head": {"rotation": frames((0.0, [-30, 0, 0]), (0.8, [-50, 0, 0]), (2.0, [40, 0, 0]))},
        "right_arm": {"rotation": frames((0.0, [-10, 0, 8]), (0.8, [-90, 0, 60]), (2.0, [-10, 0, 20]))},
        "left_arm": {"rotation": frames((0.0, [-4, 0, -8]), (0.8, [-90, 0, -60]), (2.0, [-10, 0, -20]))},
        "heart": {"scale": frames((0.0, [1.8, 1.8, 1.8]), (0.8, [1.4, 1.4, 1.4]), (2.6, [0.2, 0.2, 0.2]))},
    }}
    return {"format_version": "1.8.0", "animations": {f"animation.gardener.{k}": v for k, v in a.items()}}


# ---- The scythe, an OBJ model on the right hand's grip ----
SCYTHE_TEX = 64


class Obj:
    """An OBJ being written: positions, UVs and normals, and faces in parts (each a usemtl)."""

    def __init__(self):
        self.v, self.vt, self.vn = [], [], []
        self.parts = {}

    def vert(self, p):
        self.v.append(p)
        return len(self.v)

    def uv(self, u, v):
        # From texture pixels (y down) to OBJ's (v up).
        self.vt.append((u / SCYTHE_TEX, 1.0 - v / SCYTHE_TEX))
        return len(self.vt)

    def normal(self, n):
        length = math.sqrt(sum(c * c for c in n)) or 1.0
        self.vn.append(tuple(c / length for c in n))
        return len(self.vn)

    def face(self, part, corners):
        """corners: (position index, uv index, normal index). Every face is written both ways round, with its normal
        turned for the back, so the model shows from every side whatever way a face was wound."""
        self.parts.setdefault(part, []).append(corners)
        back = []
        for p, t, n in reversed(corners):
            nx, ny, nz = self.vn[n - 1]
            self.vn.append((-nx, -ny, -nz))
            back.append((p, t, len(self.vn)))
        self.parts[part].append(back)

    def flat(self, part, points, uvs):
        """A flat face through the points (counter-clockwise from outside), its normal worked out from them."""
        a, b, c = points[0], points[1], points[2]
        u = [b[i] - a[i] for i in range(3)]
        w = [c[i] - a[i] for i in range(3)]
        n = self.normal((u[1] * w[2] - u[2] * w[1], u[2] * w[0] - u[0] * w[2], u[0] * w[1] - u[1] * w[0]))
        self.face(part, [(self.vert(p), self.uv(*t), n) for p, t in zip(points, uvs)])

    def write(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write("# The Weeping Gardener's scythe (tools/gen_gardener.py). Units are blocks; the grip is at the origin.\n")
            for p in self.v:
                f.write("v %.5f %.5f %.5f\n" % p)
            for t in self.vt:
                f.write("vt %.5f %.5f\n" % t)
            for n in self.vn:
                f.write("vn %.5f %.5f %.5f\n" % n)
            for part, faces in self.parts.items():
                # Props pick a part by its object name (o), so each part is named as well as given a material.
                f.write(f"o {part}\nusemtl {part}\n")
                for corners in faces:
                    f.write("f " + " ".join(f"{p}/{t}/{n}" for p, t, n in corners) + "\n")


def ring(obj, part, y0, y1, r0, r1, u0, u1, v0, v1, sides=8, cz=0.0):
    """A tapering octagonal tube along the shaft, from y0 (radius r0) to y1 (radius r1), smooth-shaded, its texture
    wrapped once round it."""
    for k in range(sides):
        a0 = k / sides * math.tau
        a1 = (k + 1) / sides * math.tau
        pts = [(math.cos(a0) * r0, y0, cz + math.sin(a0) * r0), (math.cos(a1) * r0, y0, cz + math.sin(a1) * r0),
               (math.cos(a1) * r1, y1, cz + math.sin(a1) * r1), (math.cos(a0) * r1, y1, cz + math.sin(a0) * r1)]
        ua = u0 + (u1 - u0) * k / sides
        ub = u0 + (u1 - u0) * (k + 1) / sides
        uvs = [(ua, v1), (ub, v1), (ub, v0), (ua, v0)]
        normals = [obj.normal((math.cos(a), 0.0, math.sin(a))) for a in (a0, a1, a1, a0)]
        obj.face(part, [(obj.vert(p), obj.uv(*t), n) for p, t, n in zip(pts, uvs, normals)])


def cap(obj, part, y, r, up, u, v, sides=8):
    """The flat end of a tube, a fan of triangles."""
    centre = (0.0, y, 0.0)
    for k in range(sides):
        a0 = k / sides * math.tau
        a1 = (k + 1) / sides * math.tau
        p0 = (math.cos(a0) * r, y, math.sin(a0) * r)
        p1 = (math.cos(a1) * r, y, math.sin(a1) * r)
        pts = [centre, p1, p0] if up else [centre, p0, p1]
        obj.flat(part, pts, [(u, v), (u + 2, v), (u, v + 2)])


def blade(obj, part, start, length, rise, fall, height, thickness, stations, backwards=False):
    """A curved blade: its spine runs from start forward (-z, or +z backwards) for `length`, rising then falling
    away; its edge hangs `height` below the spine at the root and narrows to nothing at the tip. Its section is a
    wedge, `thickness` across at the spine and sharp at the edge, so it reads as a cutting edge from any side."""
    direction = 1.0 if backwards else -1.0
    rows = []
    for k in range(stations + 1):
        s = k / stations
        z = start[2] + direction * length * s
        y = start[1] + rise * math.sin(s * math.pi * 0.7) - fall * s ** 2.6
        h = height * (1.0 - s) ** 0.75
        t = thickness * (1.0 - s * 0.75)
        # The edge sweeps a little forward of the spine, so the blade is a crescent.
        edge_z = z + direction * 0.12 * math.sin(s * math.pi) * (1.0 - s)
        rows.append(((-t, y, z), (t, y, z), (0.0, y - h, edge_z), s))
    for k in range(stations):
        (l0, r0, e0, s0), (l1, r1, e1, s1) = rows[k], rows[k + 1]
        u0, u1 = 32 + s0 * 31, 32 + s1 * 31
        # The two flats, spine to edge; then the spine's top.
        if backwards:
            obj.flat(part, [l0, e0, e1, l1], [(u0, 1), (u0, 31), (u1, 31), (u1, 1)])
            obj.flat(part, [r0, r1, e1, e0], [(u0, 1), (u1, 1), (u1, 31), (u0, 31)])
            obj.flat(part, [l0, l1, r1, r0], [(u0, 0), (u1, 0), (u1, 1), (u0, 1)])
        else:
            obj.flat(part, [l0, l1, e1, e0], [(u0, 1), (u1, 1), (u1, 31), (u0, 31)])
            obj.flat(part, [r0, e0, e1, r1], [(u0, 1), (u0, 31), (u1, 31), (u1, 1)])
            obj.flat(part, [l0, r0, r1, l1], [(u0, 0), (u0, 1), (u1, 1), (u1, 0)])
    l, r, e, _ = rows[0]
    obj.flat(part, [l, e, r] if not backwards else [l, r, e], [(33, 2), (33, 30), (34, 2)])


def scythe_obj():
    obj = Obj()
    S = "scythe"
    # The shaft: pale sleepwood, rings in it, from the spike at its foot to the collar at its head.
    ring(obj, S, -1.32, 1.72, 0.05, 0.046, 0, 16, 63, 0)
    # Iron bands, leather wrapped where it is held (round the grip, and higher up).
    for y0, y1 in ((-0.78, -0.72), (0.52, 0.58), (1.36, 1.42)):
        ring(obj, S, y0, y1, 0.064, 0.064, 24, 32, 6, 4)
    for y0, y1 in ((-0.32, 0.34), (0.86, 1.08)):
        ring(obj, S, y0, y1, 0.062, 0.062, 16, 24, 31, 0)
    # The foot: a collar, then a spike.
    ring(obj, S, -1.42, -1.30, 0.08, 0.07, 24, 32, 10, 6)
    cap(obj, S, -1.30, 0.07, True, 26, 8)
    ring(obj, S, -1.80, -1.42, 0.002, 0.06, 32, 40, 31, 16, sides=6)
    # The head: a flared collar the blade is set in.
    ring(obj, S, 1.68, 1.78, 0.06, 0.10, 24, 32, 14, 10)
    ring(obj, S, 1.78, 2.02, 0.10, 0.085, 24, 32, 20, 14)
    cap(obj, S, 2.02, 0.085, True, 26, 18)
    # The blade, from the collar forward and down to its point; and a short spike thrusting back from the collar.
    blade(obj, S, (0.0, 2.02, -0.06), 2.15, 0.10, 0.85, 0.46, 0.035, 28)
    blade(obj, S, (0.0, 1.98, 0.08), 0.42, 0.02, 0.10, 0.12, 0.025, 6, backwards=True)
    # Hooks of thorn on its spine, leaning back.
    for s in (0.28, 0.48, 0.66):
        z = -0.06 - 2.15 * s
        y = 2.02 + 0.10 * math.sin(s * math.pi * 0.7) - 0.85 * s ** 2.6
        tip = (0.0, y + 0.13, z + 0.07)
        base = [(-0.02, y, z - 0.04), (0.02, y, z - 0.04), (0.02, y, z + 0.04), (-0.02, y, z + 0.04)]
        for i in range(4):
            obj.flat(S, [base[i], base[(i + 1) % 4], tip], [(26, 24), (28, 24), (27, 22)])
    # The stone in the collar, front and back: a small octahedron, glowing (drawn emissive).
    for gz in (-0.105, 0.105):
        c = (0.0, 1.9, gz)
        r = 0.045
        top, bottom = (c[0], c[1] + r, c[2]), (c[0], c[1] - r, c[2])
        around = [(c[0] + r, c[1], c[2]), (c[0], c[1], c[2] + r), (c[0] - r, c[1], c[2]), (c[0], c[1], c[2] - r)]
        for i in range(4):
            a, b = around[i], around[(i + 1) % 4]
            obj.flat("gem", [top, b, a], [(28, 34), (30, 38), (26, 38)])
            obj.flat("gem", [bottom, a, b], [(28, 38), (26, 34), (30, 34)])
    return obj


def scythe_texture():
    rng = random.Random(91)
    img = Image.new("RGBA", (SCYTHE_TEX, SCYTHE_TEX), (0, 0, 0, 255))

    def put(x, y, c):
        img.putpixel((x, y), c + (255,))

    # Pale sleepwood (x 0-15): grain down it, a ring every eight.
    for y in range(64):
        for x in range(16):
            c = PALE[1] if (x + y // 6) % 5 else PALE[0]
            if (x * 3 + y) % 7 == 0:
                c = PALE[2]
            if y % 8 == 0:
                c = shade(PALE[0], 0.7)
            put(x, y, shade(c, 1.0 + rng.uniform(-0.05, 0.05)))
    # Leather wrapping (x 16-23): diagonal bands, lit edges.
    for y in range(32):
        for x in range(16, 24):
            band = (x + y) % 4
            put(x, y, LEATHER[2] if band == 0 else LEATHER[1] if band < 3 else LEATHER[0])
    # Iron (x 24-31): a dark ground, a lit band and rivets.
    for y in range(32):
        for x in range(24, 32):
            c = mix(STEEL[2], STEEL[1], (y % 8) / 7)
            if y % 8 == 0:
                c = STEEL[3]
            if (x + y) % 5 == 0 and y % 8 == 4:
                c = STEEL[4]
            put(x, y, c)
    # The blade (x 32-63, y 0-31): u along it, v from the dark spine (top) to the honed edge (bottom). A hammered
    # grey, darkening to the spine and brightening to the edge, a bright bevel line where it is ground, a fuller
    # (groove) along it, and runes etched in a row that smoulder red.
    for y in range(32):
        for x in range(32, 64):
            t = y / 31
            c = mix(STEEL[1], STEEL[3], t ** 1.6)
            if 22 <= y <= 23:
                c = STEEL[4]  # the bevel line
            elif y >= 28:
                c = STEEL[5]  # the edge
            elif y in (7, 8):
                c = STEEL[0] if y == 7 else STEEL[2]  # the fuller
            elif y == 0:
                c = STEEL[0]
            c = shade(c, 1.0 + rng.uniform(-0.04, 0.04))
            put(x, y, c)
    for x in range(34, 62, 3):
        for y in (12, 13, 14):
            if rng.random() < 0.55:
                put(x, y, (120, 18, 36))
    # The stone (x 24-31, y 32-39) and the spike (x 32-39, y 16-31: iron going to a bright point).
    for y in range(32, 40):
        for x in range(24, 32):
            r = ((x - 28) ** 2 + (y - 36) ** 2) ** 0.5
            put(x, y, HOT if r < 1.2 else RED if r < 2.6 else CRIMSON)
    return img


# ---- The heart it drops ----
def veil_worn():
    """The Weeping Veil as worn (an equipment texture, the humanoid layout: the head's box at the top left, 8 a side).
    A hood of the tree's dead leaves over the crown, sides and back, its brim low over the brow, and over the face a
    curtain of red veins hanging from the brim, the eyes only a faint glow between them."""
    rng = random.Random(77)
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def leaf(x, y, dark=0):
        c = LEAF[max(0, rng.choice((1, 1, 2, 2, 3)) - dark)]
        if rng.random() < 0.06:
            c = CRIMSON
        img.putpixel((x, y), c + (255,))

    # The crown (top, 8..16 x 0..8) and the underside's rim (left blank: one never sees it).
    for y in range(8):
        for x in range(8, 16):
            leaf(x, y)
    # The sides and the back (right 0..8, left 16..24, back 24..32; rows 8..16): leaves to the jaw, ragged at the hem.
    for x0 in (0, 16, 24):
        for x in range(x0, x0 + 8):
            hem = 15 - (rng.random() < 0.4)
            for y in range(8, hem + 1):
                leaf(x, y, dark=1 if y > 12 else 0)
    # The face (8..16, rows 8..16): the brim over the brow, then the curtain of veins, the eyes glowing between.
    for x in range(8, 16):
        for y in (8, 9):
            leaf(x, y, dark=1)
    for i, x in enumerate((8, 10, 11, 13, 15)):
        length = rng.randint(4, 6)
        for y in range(10, 10 + length):
            img.putpixel((x, y), (RED if (y + i) % 3 else CRIMSON) + (255,))
        img.putpixel((x, 10 + length - 1), HOT + (255,))
    for x, y in ((9, 11), (14, 11)):
        img.putpixel((x, y), (255, 120, 120, 200))  # the eyes, faint
    return img


def veil_item():
    """The Weeping Veil's icon: the hood seen from the front, its curtain of veins over a dark face."""
    rng = random.Random(78)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(1, 15):
        for x in range(2, 14):
            d = ((x - 7.5) / 6.0) ** 2 + ((y - 6.5) / 7.0) ** 2
            if d <= 1.0 and not (y > 4 and 4 <= x <= 11 and y < 14):
                c = LEAF[rng.choice((1, 2, 2, 3))] if d < 0.8 else LEAF[0]
                img.putpixel((x, y), c + (255,))
    for y in range(5, 14):
        for x in range(4, 12):
            img.putpixel((x, y), (16, 10, 14, 255))  # the dark under the hood
    for x, y in ((6, 7), (9, 7)):
        img.putpixel((x, y), (255, 110, 110, 255))
    for i, x in enumerate((4, 6, 7, 9, 11)):
        for y in range(5, 5 + rng.randint(6, 9)):
            img.putpixel((x, min(y, 15)), (RED if (y + i) % 3 else CRIMSON) + (255,))
    return img


def heart_item():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            # A heart's shape: two lobes and a point.
            dx = abs(x - 7.5)
            inside = ((dx - 3) ** 2 + (y - 5) ** 2 <= 10) or (y >= 5 and y <= 13 - dx * 1.1)
            if not inside:
                continue
            t = ((x - 6) ** 2 + (y - 6) ** 2) ** 0.5
            col = HOT if t < 1.5 else RED if t < 4 else CRIMSON
            img.putpixel((x, y), col + (255,))
    # Bark grown round it, and a tear of red running from it.
    for x, y in ((3, 4), (4, 3), (11, 3), (12, 4), (5, 11), (10, 11), (7, 14), (8, 14)):
        img.putpixel((x, y), BARK[1] + (255,))
    for y in range(12, 16):
        img.putpixel((9, y), CRIMSON + (255,))
    return img


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def main():
    m = build()
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.gardener", "texture_width": TEX, "texture_height": TEX},
        "bones": m.bones}]}
    arm = os.path.join(ASSETS, "armatures")
    write(os.path.join(arm, "gardener.geo.json"), geo)
    write(os.path.join(arm, "gardener.animation.json"), clips())
    write(os.path.join(arm, "gardener.armature.json"), {
        "materials": {"heart": "emissive"},
        "glow_textures": [f"{NS}:textures/armatures/gardener_glow.png"],
        # The cloak, a chain: stiff at the shoulders, looser and heavier towards its hem, so it trails and ripples.
        "springs": dict({f"cloak_{i}": {"stiffness": 70 - i * 14, "damping": 5.5 - i * 0.8, "gravity": 1.5 + i * 1.5,
                                        "max_angle": 55 + i * 10, "colliders": CLOAK_COLLIDERS} for i in range(len(CLOAK))},
                        veil={"stiffness": 70, "damping": 6, "max_angle": 35}),
        "look_at": {"head": {"max_yaw": 55, "max_pitch": 35, "smoothing": 0.12}},
        # The scythe: an OBJ on the right hand's grip, its stone glowing.
        "props": [
            {"point": "grip", "model": f"{NS}:models/obj/gardener_scythe.obj", "texture": f"{NS}:textures/obj/gardener_scythe.png",
             "part": "scythe", "material": "cutout"},
            {"point": "grip", "model": f"{NS}:models/obj/gardener_scythe.obj", "texture": f"{NS}:textures/obj/gardener_scythe.png",
             "part": "gem", "material": "emissive"},
        ],
        "transition": {"duration": 0.25, "easing": "sine_in_out"},
    })
    img, glow = paint(m)
    os.makedirs(os.path.join(ASSETS, "textures", "armatures"), exist_ok=True)
    img.save(os.path.join(ASSETS, "textures", "armatures", "gardener.png"))
    glow.save(os.path.join(ASSETS, "textures", "armatures", "gardener_glow.png"))
    scythe_obj().write(os.path.join(ASSETS, "models", "obj", "gardener_scythe.obj"))
    os.makedirs(os.path.join(ASSETS, "textures", "obj"), exist_ok=True)
    scythe_texture().save(os.path.join(ASSETS, "textures", "obj", "gardener_scythe.png"))
    os.makedirs(os.path.join(ASSETS, "textures", "item"), exist_ok=True)
    heart_item().save(os.path.join(ASSETS, "textures", "item", "weeping_heart.png"))
    # The Weeping Veil, kept in the weeping tree's heart: its icon, and its look when worn (an equipment asset).
    veil_item().save(os.path.join(ASSETS, "textures", "item", "weeping_veil.png"))
    write(os.path.join(ASSETS, "items", "weeping_veil.json"), {"model": {"type": "minecraft:model", "model": f"{NS}:item/weeping_veil"}})
    write(os.path.join(ASSETS, "models", "item", "weeping_veil.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/weeping_veil"}})
    write(os.path.join(ASSETS, "equipment", "weeping_veil.json"), {"layers": {"humanoid": [{"texture": f"{NS}:weeping_veil"}]}})
    os.makedirs(os.path.join(ASSETS, "textures", "entity", "equipment", "humanoid"), exist_ok=True)
    veil_worn().save(os.path.join(ASSETS, "textures", "entity", "equipment", "humanoid", "weeping_veil.png"))
    write(os.path.join(ASSETS, "items", "weeping_heart.json"), {"model": {"type": "minecraft:model", "model": f"{NS}:item/weeping_heart"}})
    write(os.path.join(ASSETS, "models", "item", "weeping_heart.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/weeping_heart"}})
    print("cubes:", len(m.cubes), "texture rows used to", m.cursor[1] + m.cursor[2])


if __name__ == "__main__":
    main()
