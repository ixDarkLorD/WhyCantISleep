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
withered leaves over a pale mask with one eye weeping blood, roots grown out of its head; a ribcage of
bark open at the front over a heart that glows; a tattered cloak of leaves down its back and veins hanging from its
hood; one long arm ending in a great scythe, the other in long claws.

Clips: idle, walk, run (chasing a far-off target, or weeping), dormant (kneeling, head bowed, before the weeping tree), awaken, swing (the scythe, wide), slam
(both arms down: roots burst under its foes), sink and rise (it goes down into the ground and comes up elsewhere),
roar (at half its health), death. Rotations in degrees, positions in pixels; on a limb a negative x swings it forward.
"""
import json
import math
import os
import random

import rig_pose as rp
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


# The cloak's lengths, top to hem: (length, half its width, rest turn back from its parent's, degrees). It is woven of
# CLOAK_COLUMNS chains side by side, each length of each a spring, linked across to its neighbours (cloth).
# Its shape at rest is worked out from where it lies (the line of its inner face, top to hem, (y, z)): from behind the
# shoulders (just behind the collar of leaves, so it hangs over it rather than through it), then resting on the back-most corner of each thorn of the spine (SPIKE_CORNERS: each thorn is a 1x1x4 bar
# tilted up and back 35 degrees from its root; its corner nearest the cloak is half its width back from its tip), so a
# joint of the cloak lies exactly on each, then down over the small of the back and the hips to its hem.
SPIKE_ROOTS = [30 + i * 3.2 for i in range(4)]
SPIKE_CORNERS = [(round(y + 0.5 + 4 * math.sin(math.radians(35)) - 0.5 * math.cos(math.radians(35)), 3),
                  round(3 + 4 * math.cos(math.radians(35)) + 0.5 * math.sin(math.radians(35)), 3)) for y in SPIKE_ROOTS]
CLOAK_PATH = [(48, 4.85)] + [(y, z + 0.06) for y, z in reversed(SPIKE_CORNERS)] + [(27.5, 6.3), (22, 5.9)]
CLOAK_HALVES = [8, 8.3, 8.6, 9, 9.3, 9.5]
# The thorns sit on the seam between the cloak's two middle columns, whose weights hang either side of it; so at each
# thorn a ball under each of those columns, its surface just at the thorn's back-most corner, holds the cloak on the
# thorns: it rests there and goes no further in.
SPIKE_BALL = 2.5
SPIKE_SIDE = 2.2


def _cloak():
    """(length, half its width, rest turn back from its parent's, degrees) for each length, from CLOAK_PATH."""
    out, before = [], 0.0
    for k in range(len(CLOAK_PATH) - 1):
        (y0, z0), (y1, z1) = CLOAK_PATH[k], CLOAK_PATH[k + 1]
        lean = math.degrees(math.atan2(z1 - z0, y0 - y1))                       # back from straight down
        out.append((round(math.hypot(y0 - y1, z1 - z0), 3), CLOAK_HALVES[k], round(lean - before, 3)))
        before = lean
    return out


CLOAK = _cloak()
CLOAK_COLUMNS = 4
CLOAK_COLLIDERS = [{"point": "back_upper", "radius": 5.0}, {"point": "back_lower", "radius": 5.0},
                   {"point": "hips_back", "radius": 5.4}, {"point": "right_leg_back", "radius": 3.8},
                   {"point": "left_leg_back", "radius": 3.8}, {"point": "right_leg_calf", "radius": 3.4},
                   {"point": "left_leg_calf", "radius": 3.4}]
CLOAK_COLLIDERS += [{"point": f"spike_{i}_{side}", "radius": SPIKE_BALL + 0.06} for i in range(4) for side in "lr"]
# Down each arm (upper arm, elbow, forearm in three, hand), and along the scythe's shaft and blade: they swing back
# through where the cloak hangs, running and swinging.
CLOAK_COLLIDERS += [{"point": f"{side}_{part}", "radius": r} for side in ("right", "left")
                    for part, r in (("arm_ball", 2.8), ("elbow_ball", 2.9), ("forearm_upper", 2.8), ("forearm_ball", 2.8),
                                    ("forearm_lower", 2.8), ("hand_ball", 3.0))]
CLOAK_COLLIDERS += [{"point": f"shaft_{k}", "radius": 1.9} for k in range(8)] + [{"point": f"blade_{k}", "radius": 2.0} for k in range(3)]


# Roots from its head, as paths of points (x, y, z) and their thickness at the base and the tip.
ROOTS = [
    ([(-2.5, 61, -0.5), (-3.8, 63.8, -1), (-6, 66.2, -0.8), (-8.6, 67.2, -0.3), (-10.4, 66.2, 0.2)], 1.7, 0.6),
    ([(2.5, 61, -0.2), (3.6, 64.2, -0.6), (5.6, 66.8, -0.2), (8.2, 67.6, 0.4), (9.8, 66.4, 1.0)], 1.7, 0.6),
    ([(0.3, 61, 1.8), (0.8, 64.5, 2.8), (-0.2, 68.2, 4.0), (0.6, 71, 5.6), (1.6, 72.2, 6.6)], 1.5, 0.5),
    ([(-1.2, 61, -3.8), (-2.2, 63.4, -4.6), (-2.6, 65.4, -6.0)], 1.0, 0.5),
    ([(2, 61, -3), (3.2, 63, -3.6), (3.6, 64.6, -4.8)], 0.9, 0.45),
    ([(-5.2, 58, 0.5), (-6.4, 54, 1.2), (-6.9, 50, 2.0), (-6.4, 46.5, 2.8)], 1.3, 0.5),
    ([(5.2, 57, 1.0), (6.2, 53.5, 1.6), (6.6, 49.8, 1.4), (7.4, 47, 0.6)], 1.2, 0.5),
    ([(-4.6, 60.4, -6.2), (-5.0, 57.5, -6.6), (-4.6, 55, -6.9)], 0.7, 0.35),
]


def root(m, bone, path, thick, thin, material="root"):
    """A root along a path of points: a bar for each stretch, tapering from `thick` to `thin`, each turned (about where
    it starts) to point at the next and running exactly from bend to bend, and at each bend a gnarled knot turned
    against them, so it reads as one root and no two faces lie in one plane where it bends."""
    n = len(path) - 1
    widths = [round(thick + (thin - thick) * k / max(1, n - 1), 2) for k in range(n)]
    for k in range(n):
        (ax, ay, az), (bx, by, bz) = path[k], path[k + 1]
        dx, dy, dz = bx - ax, by - ay, bz - az
        length = math.sqrt(dx * dx + dy * dy + dz * dz)
        dx, dy, dz = dx / length, dy / length, dz / length
        t = widths[k]
        # A bar standing up (+y) from its start, turned to point along the stretch: forward/back about x (+x turns its
        # top toward -z), then over about z (+z leans its top toward +x).
        rx = round(-math.degrees(math.asin(max(-1.0, min(1.0, dz)))), 2)
        rz = round(math.degrees(math.atan2(dx, dy)), 2)
        # The first bar starts a little inside the head it grows from; the rest start at their bend, inside its knot.
        sink = t * 0.6 if k == 0 else 0.0
        m.cube(bone, [round(ax - t / 2, 3), round(ay - sink, 3), round(az - t / 2, 3)], [t, round(length + sink, 3), t], material,
               rotation=[rx, 0, rz], pivot=[ax, ay, az])
        if k > 0:
            # The knot, a gnarl closing the bend: a little wider than the bars, and turned 45 degrees against them on two
            # axes so none of its faces ever lies in the plane of one of theirs (no two faces fight).
            knot = round(max(widths[k - 1], t) * 1.15, 2)
            m.cube(bone, [round(ax - knot / 2, 3), round(ay - knot / 2, 3), round(az - knot / 2, 3)], [knot, knot, knot], material,
                   rotation=[round(rx + 45, 2), 45, rz], pivot=[ax, ay, az])

def ribs(m, parent, half, rows):
    """The ribs of one half of the cage (its rows), each side a joint (rib_r_<half>, rib_l_<half>) hinged at the back of
    its side bars: a side bar along the flank and a front bar curving in toward the breastbone."""
    rows = list(rows)
    mid = 34 + (rows[0] + rows[-1]) * 1.3 + 0.75
    for side, sx in (("r", -1), ("l", 1)):
        rib = m.bone(f"rib_{side}_{half}", [5 * sx, round(mid, 2), 2.5], parent)
        for i in rows:
            y = 34 + i * 2.6
            m.cube(rib, [-6 if sx < 0 else 4, y, -4], [2, 1.5, 7], "bark")
            m.cube(rib, [-4.5 if sx < 0 else 1.5, y, -4.5], [3, 1.5, 1], "bark")


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
    # Its ribs: each side of each half of the cage a joint hinged at the back of its side bar, so it can swing open like
    # a door on the heart (its stagger) and slam shut (struck in the heart).
    ribs(m, "torso", "low", range(2))
    chest = m.bone("chest", [0, 39, 1], "torso")
    m.cube(chest, [-1.5, 38, 0.5], [3, 9, 3], "bark")  # the upper spine
    m.cube(chest, [-2, 38, 0], [4, 3, 4], "bark", rotation=[45, 0, 0], pivot=[0, 39.5, 2])  # where the spine bends
    ribs(m, "chest", "high", range(2, 4))
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
        # Colliders under the cloak's middle columns at its back-most corner, the point the cloak rests on (see
        # CLOAK_PATH and SPIKE_BALL): it lies on it, no further.
        cy, cz = SPIKE_CORNERS[i]
        locators = bone.setdefault("locators", {})
        locators[f"spike_{i}"] = [0, cy, cz]
        locators[f"spike_{i}_l"] = [SPIKE_SIDE, cy, round(cz - SPIKE_BALL, 3)]
        locators[f"spike_{i}_r"] = [-SPIKE_SIDE, cy, round(cz - SPIKE_BALL, 3)]
    # Colliders for the cloak: the back, the small of it, the hips, the thighs.
    chest.setdefault("locators", {})["back_upper"] = [0, 41, -1]
    torso.setdefault("locators", {})["back_lower"] = [0, 34, -1]
    hips.setdefault("locators", {})["hips_back"] = [0, 29, -0.5]
    for b_ in m.bones:
        if b_["name"] in ("right_leg", "left_leg"):
            b_.setdefault("locators", {})[b_["name"] + "_back"] = [b_["pivot"][0], 23, 0]
            b_["locators"][b_["name"] + "_calf"] = [b_["pivot"][0], 16, 0.5]                    # lower, for its hem
    m.cube(chest, [-6.5, 40, 2], [13, 5, 2], "vein")
    heart = m.bone("heart", [0, 40, -1], "chest", locators={"heart_core": [0, 40, -1]})
    m.cube(heart, [-2, 38, -3], [4, 4, 4], "heart")
    m.cube(heart, [-1.5, 38.5, -3.5], [3, 3, 1], "heart")
    neck = m.bone("neck", [0, 48, 0], "chest")
    m.cube(neck, [-1.5, 47, -1.5], [3, 4, 3], "bark")
    # Its tears: they well under its eye (tear_well), run down the mask and fall from its edge (tear_drip), drawn as
    # particles (GardenerClient), not pixels.
    head = m.bone("head", [0, 51, -1], "neck", locators={"eye": [-1.5, 55.5, -7], "tear_well": [-1.5, 54.7, -6.95],
                                                         "tear_drip": [-1.5, 50.8, -6.95]})
    m.cube(head, [-5, 50, -6], [10, 11, 10], "leaf", inflate=0.3)
    m.cube(head, [-3.5, 51, -6.7], [7, 8, 1], "mask")
    # Roots grown out of its head: thick ones rising and twisting out into a crown, thinner ones creeping down over
    # the sides of the hood to its shoulders, one down beside its face.
    for path, thick, thin in ROOTS:
        root(m, head, path, thick, thin)
    m.bone("veil", [0, 51, -5.5], "head")                                                      # (kept: its spring; nothing hangs on it now)
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
    # Colliders for the cloak along the shaft (the OBJ's shaft runs along this joint's y, from 29 below the grip to 35
    # above) and its blade (from the head of the shaft forward to its tip).
    scythe_points = {"grip": [-9, 18, 0], "blade_tip": [-9.5, 49, -33]}
    for k, dy in enumerate(range(-24, 36, 8)):
        scythe_points[f"shaft_{k}"] = [-9, 18 + dy, 0]
    for k, (y, z) in enumerate(((50, -8), (50.5, -17), (50, -26))):
        scythe_points[f"blade_{k}"] = [-9.2, y, z]
    m.bone("scythe", [-9, 18, 0], "right_hand", locators=scythe_points)
    # Colliders for the cloak down each arm: upper arm, elbow, forearm, hand.
    for side, sx in (("right", -1), ("left", 1)):
        x = 9 * sx
        arm = next(b for b in m.bones if b["name"] == f"{side}_arm").setdefault("locators", {})
        arm.update({f"{side}_arm_ball": [x, 40, 0], f"{side}_elbow_ball": [x, 34, 0]})
        next(b for b in m.bones if b["name"] == f"{side}_forearm").setdefault("locators", {}).update(
            {f"{side}_forearm_ball": [x, 27.5, 0], f"{side}_forearm_upper": [x, 31.5, 0], f"{side}_forearm_lower": [x, 23, 0]})
        hand = next(b for b in m.bones if b["name"] == f"{side}_hand")
        hand.setdefault("locators", {})[f"{side}_hand_ball"] = [x, 18, 0]
    # The cloak, last: its colliders on the arms and the scythe must be on joints posed before it.
    # The cloak, as cloth: CLOAK_COLUMNS chains of four lengths side by side, each length on its own spring
    # (gardener.armature.json) and woven to the lengths beside it, so it flows, ripples across and folds as it moves.
    # Hung from behind the shoulders, its first lengths slope back over the thorns of the spine, the rest falling
    # nearly straight; colliders on the back, the thorns, the hips and thighs keep it out of the body as it swings.
    # (Cubes are drawn hanging straight down from each joint; the joints' rest turns slope them.)
    for c in range(CLOAK_COLUMNS):
        parent, top = "chest", 48
        for i, (length, half, turn) in enumerate(CLOAK):
            width = 2 * half / CLOAK_COLUMNS
            x0 = -half + c * width
            back = CLOAK_PATH[0][1]                                                          # where it hangs from, behind the collar
            seg = m.bone(f"cloak_{c}_{i}", [round(x0 + width / 2, 3), top, back], parent, rotation=[turn, 0, 0])
            m.cube(seg, [x0, top - length, back], [width, length, 1], "leaf")                    # meeting the next (skinned to it)
            # By face, and only the faces that show: none where it meets the length above or below it, or the column
            # beside it (skinned, those meet exactly, and drawn they would fight as the cloth bends). Bedrock's east face
            # is the low-x side.
            cube = seg["cubes"][-1]
            (u, v), (w, h, d) = cube["uv"], m.cubes[-1][1]
            cube["box_uv"] = [u, v]
            faces = {"up": {"uv": [u + d, v], "uv_size": [w, d]}, "down": {"uv": [u + d + w, v + d], "uv_size": [w, -d]},
                     "east": {"uv": [u, v + d], "uv_size": [d, h]}, "north": {"uv": [u + d, v + d], "uv_size": [w, h]},
                     "west": {"uv": [u + d + w, v + d], "uv_size": [d, h]}, "south": {"uv": [u + 2 * d + w, v + d], "uv_size": [w, h]}}
            if i > 0:
                del faces["up"]
            if i < len(CLOAK) - 1:
                del faces["down"]
            if c > 0:
                del faces["east"]
            if c < CLOAK_COLUMNS - 1:
                del faces["west"]
            cube["uv"] = faces
            parent, top = f"cloak_{c}_{i}", top - length
        # Its hem, in tatters: leaves of different lengths hanging from each column's last length.
        for k in range(2):
            x = -CLOAK[-1][1] + (c + 0.15 + k * 0.45) * 2 * CLOAK[-1][1] / CLOAK_COLUMNS
            short = (c + k) % 2
            m.cube(m.bones[-1], [round(x, 2), top - 7 + short * 3, 4.6], [2, 7 - short * 3, 0.5], "leaf")
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
        u, v = c.get("box_uv", c["uv"])                                 # (a cube drawn by face keeps its box's place)
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
                        # Where its tears run: a red rim under the eye, wet, and below it only a faint stain worn into
                        # the bone (the tears themselves are drawn as they fall, GardenerClient).
                        put(img, fx + ex, fy + 4, shade(CRIMSON, 0.85))
                        put(glow, fx + ex, fy + 4, CRIMSON + (90,))
                        for y in range(5, fh):
                            put(img, fx + ex, fy + y, mix(BONE[1], VEIN, 0.3 - 0.04 * (y - 5)))
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
    # Running it down: a loping reaper's run, twice the walk's pace. It leans far into it, its long legs reaching out
    # and folding high behind, with a moment of flight in each stride (its hips rise as both legs are off the ground);
    # its clawed arm pumps against the stride, the scythe trails low and back, ready; its head stays level, fixed on
    # its prey (rising against the lean), only a little jolt from each landing.
    R = f"{T} * 514.3"                                                                    # one stride a 0.7 s
    run_r = f"(-48 * math.sin({R}) - 18)"
    run_knee_r = f"(22 + math.max(0, 78 * math.sin({R} + 80)))"
    run_l = f"(48 * math.sin({R}) - 18)"
    run_knee_l = f"(22 + math.max(0, -78 * math.sin({R} + 80)))"
    a["run"] = {"loop": True, "animation_length": 0.7, "bones": {
        "right_leg": {"rotation": [run_r, 0, 0]}, "left_leg": {"rotation": [run_l, 0, 0]},
        "right_shin": {"rotation": [run_knee_r, 0, 0]}, "left_shin": {"rotation": [run_knee_l, 0, 0]},
        "right_foot": {"rotation": [f"-({run_r} + {run_knee_r}) * 0.7 + 10", 0, 0]},
        "left_foot": {"rotation": [f"-({run_l} + {run_knee_l}) * 0.7 + 10", 0, 0]},
        "hips": {"position": [0, f"-3.5 + 2.2 * math.abs(math.sin({R} + 30))", 0]},
        "torso": {"rotation": [f"26 + 3 * math.sin({R} * 2)", f"-9 * math.sin({R})", 0]},
        "chest": {"rotation": [f"14 + 2 * math.sin({R} * 2 + 40)", f"-6 * math.sin({R})", 0]},
        "head": {"rotation": [f"-30 - 3 * math.sin({R} * 2 + 80)", f"12 * math.sin({R})", 0]},
        "right_arm": {"rotation": [f"8 + 8 * math.sin({R})", 0, 42]},                                    # held out wide, clear of the cloak
        "right_forearm": {"rotation": [-24, 0, 0]}, "right_hand": {"rotation": [8, 0, 0]},
        "left_arm": {"rotation": [f"-28 - 46 * math.sin({R})", 0, -14]},                                  # the claws, pumping
        "left_forearm": {"rotation": [f"-46 - 20 * math.max(0, -math.sin({R}))", 0, 0]},
        "left_hand": {"rotation": [-14, 0, 0]},
        "left_finger_0": {"rotation": [-30, 0, 0]}, "left_finger_1": {"rotation": [-34, 0, 0]}, "left_finger_2": {"rotation": [-30, 0, 0]},
        "scythe": {"rotation": [HOLD[0] + 92, 0, 0]},                                                  # the scythe trailing low behind
        "heart": {"scale": [f"1 + 0.18 * math.pow(math.max(0, math.sin({R} * 2)), 4)"] * 3},       # pounding
    }}
    # ---- Its combat (BossCombat, GardenerEntity): parried, staggered, struck in the heart ----
    OPEN = 72                                                                               # how far its ribs swing open
    reel = {"torso": [-10, 0, 4], "chest": [-14, 0, 0], "head": [18, 10, -12], "right_leg": [-20, 0, 0], "right_shin": [42, 0, 0],
            "left_leg": [10, 0, 0], "left_shin": [30, 0, 0], "right_foot": [-14, 0, 0], "left_foot": [-20, 0, 0],
            "right_arm": [10, 0, 35], "right_forearm": [-20, 0, 0], "left_arm": [5, 0, -40], "left_forearm": [-15, 0, 0],
            "scythe": [HOLD[0] + 40, 0, 0]}
    rest = {"torso": [6, 0, 0], "chest": [10, 0, 0], "head": [-6, 0, 0], "right_leg": [-16, 0, 0], "right_shin": [28, 0, 0],
            "left_leg": [-10, 0, 0], "left_shin": [20, 0, 0], "right_foot": [-12, 0, 0], "left_foot": [-10, 0, 0],
            "right_arm": [-14, 0, 8], "right_forearm": [-38, 0, 0], "left_arm": [-6, 0, -10], "left_forearm": [-30, 0, 0], "scythe": HOLD}
    ribs_open = {"rib_r_low": [0, OPEN, 0], "rib_r_high": [0, OPEN - 6, 0], "rib_l_low": [0, -OPEN, 0], "rib_l_high": [0, -(OPEN - 6), 0]}
    ribs_shut = {k: [0, 0, 0] for k in ribs_open}
    # Its guard broken: it reels back a step, sagging at the knees, arms flung out, and its ribs snap open on its heart.
    stagger = {k: {"rotation": frames((0.0, rest[k]), (0.22, [v * 1.25 for v in reel[k]]), (0.6, reel[k]))} for k in reel}
    stagger.update({k: {"rotation": frames((0.0, [0, 0, 0]), (0.18, [0, 0, 0]), (0.32, [0, v[1] * 1.15, 0]), (0.6, v))} for k, v in ribs_open.items()})
    stagger["hips"] = {"position": frames((0.0, [0, 0, 0]), (0.22, [0, -3.5, 0]), (0.6, [0, -3, 0]))}
    stagger["heart"] = {"scale": frames((0.0, [1, 1, 1]), (0.32, [1.6, 1.6, 1.6]), (0.6, [1.3, 1.3, 1.3])),
                        "position": frames((0.0, [0, 0, 0]), (0.6, [0, 0, -1.2]))}
    a["stagger"] = {"loop": False, "animation_length": 0.6, "bones": stagger}
    # Reeling, open: swaying where it stands, its heart beating hard in the open cage.
    B = f"{T} * 300"
    a["staggered"] = {"loop": True, "animation_length": 1.2, "bones": dict(
        {k: {"rotation": v} for k, v in reel.items() if k not in ("torso", "head")},
        torso={"rotation": [f"-10 + 2 * math.sin({B})", 0, f"4 + 3 * math.sin({B} * 0.5)"]},
        head={"rotation": [f"18 + 3 * math.sin({B})", f"10 + 6 * math.sin({B} * 0.5)", -12]},
        hips={"position": [0, f"-3 + 0.4 * math.sin({B})", 0]},
        heart={"scale": [f"1.3 + 0.18 * math.pow(math.max(0, math.sin({B} * 2)), 6)"] * 3, "position": [0, 0, -1.2]},
        **{k: {"rotation": [0, f"{v[1]} + {4 if v[1] > 0 else -4} * math.sin({B})", 0]} for k, v in ribs_open.items()})}
    # Steadying: rising out of its reel, its ribs drawing shut.
    recover = {k: {"rotation": frames((0.0, reel[k]), (0.7, rest[k]))} for k in reel}
    recover.update({k: {"rotation": frames((0.0, v), (0.45, [0, 0, 0]))} for k, v in ribs_open.items()})
    recover["hips"] = {"position": frames((0.0, [0, -3, 0]), (0.7, [0, 0, 0]))}
    recover["heart"] = {"scale": frames((0.0, [1.3, 1.3, 1.3]), (0.45, [1, 1, 1])), "position": frames((0.0, [0, 0, -1.2]), (0.45, [0, 0, 0]))}
    a["recover"] = {"loop": False, "animation_length": 0.7, "bones": recover}
    # Struck in the heart: thrown back, its heart flaring, its ribs slamming shut on the blade; then doubled over,
    # clutching its chest, and straightening.
    hurt = {"torso": [[-26, 0, 0], [16, 0, 0], [6, 0, 0]], "chest": [[-20, 0, 0], [18, 0, 0], [10, 0, 0]],
            "head": [[-35, 0, 0], [30, 0, 0], [-6, 0, 0]], "right_arm": [[30, 0, 50], [-40, 0, -14], [-14, 0, 8]],
            "left_arm": [[30, 0, -55], [-55, 0, 14], [-6, 0, -10]], "right_forearm": [[-10, 0, 0], [-70, 0, 0], [-38, 0, 0]],
            "left_forearm": [[-10, 0, 0], [-75, 0, 0], [-30, 0, 0]], "scythe": [[HOLD[0] + 50, 0, 0], [HOLD[0] + 20, 0, 0], HOLD]}
    struck = {k: {"rotation": frames((0.0, reel[k]), (0.12, v[0]), (0.75, v[1]), (1.5, v[2]))} for k, v in hurt.items()}
    for k in ("right_leg", "right_shin", "left_leg", "left_shin", "right_foot", "left_foot"):
        struck[k] = {"rotation": frames((0.0, reel[k]), (0.75, reel[k]), (1.5, rest[k]))}
    struck.update({k: {"rotation": frames((0.0, v), (0.12, [0, v[1] * 1.2, 0]), (0.3, [0, -v[1] * 0.08, 0]), (0.4, [0, 0, 0]))}
                   for k, v in ribs_open.items()})
    struck["hips"] = {"position": frames((0.0, [0, -3, 0]), (0.75, [0, -4, 0]), (1.5, [0, 0, 0]))}
    struck["heart"] = {"scale": frames((0.0, [1.3, 1.3, 1.3]), (0.12, [2.0, 2.0, 2.0]), (0.4, [0.8, 0.8, 0.8]), (1.5, [1, 1, 1])),
                       "position": frames((0.0, [0, 0, -1.2]), (0.3, [0, 0, 0]))}
    a["heart_struck"] = {"loop": False, "animation_length": 1.5, "bones": struck}
    # Parried: its scythe knocked back up over its shoulder, it rocks back off its swing, and gathers itself.
    a["deflected"] = {"loop": False, "animation_length": 0.8, "bones": {
        "right_arm": {"rotation": frames((0.0, [-100, 20, 60]), (0.15, [-165, 40, 95]), (0.5, [-60, 0, 30]), (0.8, [-14, 0, 8]))},
        "right_forearm": {"rotation": frames((0.0, [-30, 0, 0]), (0.15, [-10, 0, 0]), (0.8, [-38, 0, 0]))},
        "torso": {"rotation": frames((0.0, [8, 30, 0]), (0.15, [-12, 35, 0]), (0.5, [-4, 10, 0]), (0.8, [6, 0, 0]))},
        "chest": {"rotation": frames((0.0, [10, 0, 0]), (0.15, [-8, 0, 0]), (0.8, [10, 0, 0]))},
        "head": {"rotation": frames((0.0, [-6, 0, 0]), (0.15, [-20, -15, 0]), (0.8, [-6, 0, 0]))},
        "scythe": {"rotation": frames((0.0, HOLD), (0.15, [HOLD[0] - 40, 0, 0]), (0.8, HOLD))},
        "right_leg": {"rotation": frames((0.0, [-16, 0, 0]), (0.15, [-6, 0, 0]), (0.8, [-16, 0, 0]))},
        "left_leg": {"rotation": frames((0.0, [-10, 0, 0]), (0.15, [10, 0, 0]), (0.8, [-10, 0, 0]))},
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
    grounded(a, HOLD, T)
    return {"format_version": "1.8.0", "animations": {f"animation.gardener.{k}": v for k, v in a.items()}}


# ---- Its grounded clips: posed on the ground, worked out (tools/rig_pose.py) ----
# Kneeling, rising, falling, slamming and dying are posed from key poses solved against the ground: the knee it
# kneels on touches it, the planted foot lies flat on it, its claws rest on it, the scythe's blade strikes it, its
# second hand holds the shaft; between keys each clip is settled on the ground sample by sample (its whole body raised
# or lowered until its lowest point rests on it), so nothing sinks into the ground or floats over it. Every clip starts
# and ends on the idle's first pose, so it flows into and out of the rest.

def contacts():
    """The points of it that may touch the ground: feet (soles, toes, heel spurs), knees and shins, hands, claws,
    elbows, its head, shoulders and hips, and the scythe (its foot, its head, its blade)."""
    pts = []
    for side, sx in (("right", -1), ("left", 1)):
        x = 3 * sx
        foot, shin = f"{side}_foot", f"{side}_shin"
        pts += [(foot, [x + dx, 0, z]) for dx in (-2.5, 2.5) for z in (-6, -4, 2)] + [(foot, [x, 0, -7]), (foot, [x, 0, 4])]
        pts += [(shin, [x, 18, -2.2]), (shin, [x, 15.9, 0])]
        pts += [(shin, [x + dx, y, z]) for dx in (-1.5, 1.5) for y in (6, 12) for z in (-1.5, 1.5)]
        hx = 9 * sx
        hand = f"{side}_hand"
        pts += [(hand, [hx + dx, 16, z]) for dx in (-2, 2) for z in (-2, 2)]
        pts += [(f"{side}_forearm", [hx, 21, 0]), (f"{side}_forearm", [hx, 34, 0]), (f"{side}_arm", [hx, 40, 0])]
        for f, fx in enumerate((-1.5, 0, 1.5)):
            length = 9 if side == "left" else 4
            pts.append((f"{side}_finger_{f}", [hx + fx, 16 - length, -1 + f * 0.5]))
    pts += [("head", [x, y, z]) for x in (-5, 5) for y in (50, 61) for z in (-6, 4)]
    pts += [("chest", [x, y, z]) for x in (-8, 8) for y in (44, 48) for z in (-3.5, 3.5)]
    pts += [("hips", [x, 27, z]) for x in (-5, 5) for z in (-3, 3)]
    pts += [("heart", [0, 38, -3]), ("chest", [0, 40, -4.5])]
    pts += [("scythe", [-9, -11, 0]), ("scythe", [-9, 53, 0]), ("scythe", [-9.5, 49, -33]), ("scythe", [-9.2, 50, -17]), ("scythe", [-9.2, 50, -25])]
    return pts


CONTACTS = contacts()
# What sets it on the ground: its body, not what it holds. The scythe must never lift it (it was, at the ends of every
# clip: hanging from the fist it reached lower than the feet); it gives way to the ground instead (_scythe_clear).
BODY = [c for c in CONTACTS if c[0] != "scythe"]
# The points of the scythe, and of a hand: where its second hand grips the shaft (in the scythe joint's frame, the shaft
# runs along y from 29 below the grip to 35 above), and the palm of the left hand.
SHAFT_LOW = [-9, -11, 0]
BLADE_TIP = [-9.5, 49, -33]
LEFT_PALM = [9, 18, 0]


def _copy(pose):
    return {b: {k: list(v) for k, v in ch.items()} for b, ch in pose.items()}


def _set(pose, **bones):
    """The pose with these bones turned (degrees) or, for "<bone>_pos", moved."""
    out = _copy(pose)
    for name, v in bones.items():
        if name.endswith("_pos"):
            out.setdefault(name[:-4], {})["position"] = list(v)
        else:
            out.setdefault(name, {})["rotation"] = list(v)
    return out


def _ground(rig, pose, points=None):
    """The pose moved (its root) so the lowest point of its body (or of `points`) rests on the ground."""
    pose = _copy(pose)
    root = pose.setdefault("root", {}).setdefault("position", [0.0, 0.0, 0.0])
    low = rp.lowest(rig, rig.world(pose), points or BODY)
    root[1] -= low
    return pose


def _at(rig, pose, bone, point):
    return rp.Rig.at(rig.world(pose), bone, point)


def _plant_leg(rig, pose, side, z):
    """The leg of `side` turned (hip and knee) so its ankle stands 5 above the ground, `z` forward, and its foot
    turned flat on the ground."""
    x = -3 if side == "right" else 3
    rp.solve(rig, pose, [(f"{side}_leg", 0), (f"{side}_shin", 0)],
             lambda w: (rp.Rig.at(w, f"{side}_shin", [x, 5, 0])[1] - 5.0) ** 2 + (rp.Rig.at(w, f"{side}_shin", [x, 5, 0])[2] - z) ** 2)
    rp.solve(rig, pose, [(f"{side}_foot", 0)],
             lambda w: (rp.Rig.at(w, f"{side}_foot", [x, 0, -5])[1] - rp.Rig.at(w, f"{side}_foot", [x, 0, 2])[1]) ** 2)
    return pose


def _tuck_foot(rig, pose, side):
    """The foot of a knee on the ground turned so its toes are tucked under, on the ground, as the knee is."""
    x = -3 if side == "right" else 3
    knee = lambda w: rp.Rig.at(w, f"{side}_shin", [x, 18, -2.2])[1]
    rp.solve(rig, pose, [(f"{side}_foot", 0)], lambda w: (rp.Rig.at(w, f"{side}_foot", [x, 0, -7])[1] - knee(w)) ** 2)
    return pose


def _kneel(rig, pose, down="left", up_z=-10.0):
    """Kneeling: the knee of `down` on the ground, its shin lying back along it, toes tucked; the other foot planted
    flat, `up_z` forward. The whole set on the ground at the knee."""
    x = 3 if down == "left" else -3
    pose = _set(pose, **{f"{down}_leg": [0, 0, 4 if down == "left" else -4], f"{down}_shin": [90, 0, 0]})
    pose = _tuck_foot(rig, pose, down)
    pose = _ground(rig, pose, [(f"{down}_shin", [x, 18, -2.2]), (f"{down}_shin", [x, 15.9, 0])])
    other = "right" if down == "left" else "left"
    pose = _plant_leg(rig, pose, other, up_z)
    return pose


KNEES = [("left_shin", [3, 18, -2.2]), ("left_shin", [3, 15.9, 0]), ("right_shin", [-3, 18, -2.2]), ("right_shin", [-3, 15.9, 0])]
FEET = [(f"{side}_foot", [3 * sx + dx, 0, z]) for side, sx in (("right", -1), ("left", 1)) for dx in (-2.5, 2.5) for z in (-6, -4, 2)]


def _kneel_both(rig, pose):
    """On both knees: both shins along the ground, toes tucked, thighs upright; set on the ground at the knees."""
    pose = _set(pose, right_leg=[0, 0, -4], left_leg=[0, 0, 4], right_shin=[90, 0, 0], left_shin=[90, 0, 0])
    pose = _tuck_foot(rig, pose, "right")
    pose = _tuck_foot(rig, pose, "left")
    return _ground(rig, pose, KNEES)


def _scythe_down(rig, pose):
    """The scythe let fall: turned in the slack fist until it lies along the ground, its foot and its head on it."""
    rp.solve(rig, pose, [("scythe", 0), ("scythe", 2)],
             lambda w: (rp.Rig.at(w, "scythe", SHAFT_LOW)[1] - 0.4) ** 2 + (rp.Rig.at(w, "scythe", [-9, 53, 0])[1] - 1.2) ** 2
             + (rp.Rig.at(w, "scythe", BLADE_TIP)[1] - 0.4) ** 2)
    return pose


SCYTHE_POINTS = [SHAFT_LOW, [-9, 53, 0], BLADE_TIP, [-9.2, 50, -17], [-9.2, 50, -25], [-9, 21, 0]]


def _scythe_rest(rig, pose, lie=False):
    """The scythe turned in the fist so no part of it is in the ground and its lowest point rests on it (the body
    already set on the ground: the scythe gives way to it rather than propping it up). With `lie`, laid along the
    ground as near flat as it goes."""
    def error(w):
        ys = [rp.Rig.at(w, "scythe", q)[1] for q in SCYTHE_POINTS]
        e = sum(50.0 * max(0.0, 0.2 - y) ** 2 for y in ys) + (min(ys) - 0.3) ** 2
        if lie:
            e += 0.05 * (ys[0] - ys[1]) ** 2
        return e
    starts = [{"scythe": [x, y, 0]} for x in (0, 60, 120, 180, 240, 300) for y in (-60, 0, 60)]
    return _best(rig, pose, [("scythe", 0), ("scythe", 1), ("scythe", 2)], error, starts)


def _scythe_clear_hold(rig, pose, margin=1.0):
    """The scythe kept in the fist's own plane (no twist), turned to the angle nearest its usual hold at which all of
    it is at least `margin` clear of the ground: for key poses held close to the ground (clutching the chest)."""
    best = None
    for x in range(-180, 181, 5):
        trial = _set(pose, scythe=[HOLD_TURN + x, 0, 0])
        w = rig.world(trial, rig.chain(["scythe"]))
        if min(rp.Rig.at(w, "scythe", q)[1] for q in SCYTHE_POINTS) >= margin and (best is None or abs(x) < abs(best)):
            best = x
    return _set(pose, scythe=[HOLD_TURN + (best if best is not None else 0), 0, 0])


class _ScytheClear:
    """The scythe giving way to the ground through a clip: at each sample, if any of it would be in the ground, it
    (only) is turned in the fist until it clears and rests on the ground, dragging. Its correction carries over from
    sample to sample (and eases off again once the ground lets it), so it slides along the ground smoothly rather than
    jumping between turns (between samples a jump would swing it through the ground)."""

    def __init__(self, rig):
        self.rig = rig
        self.carry = [0.0, 0.0]

    def __call__(self, pose):
        rig = self.rig
        pose.setdefault("scythe", {}).setdefault("rotation", [HOLD_TURN, 0, 0])
        wanted = list(pose["scythe"]["rotation"])
        guess = [wanted[0] + self.carry[0], wanted[1], wanted[2] + self.carry[1]]
        pose["scythe"]["rotation"] = list(guess)

        def error(w):
            ys = [rp.Rig.at(w, "scythe", q)[1] for q in SCYTHE_POINTS]
            r = pose["scythe"]["rotation"]
            return (sum(50.0 * max(0.0, 0.15 - y) ** 2 for y in ys)
                    + 0.004 * ((r[0] - wanted[0]) ** 2 + (r[2] - wanted[2]) ** 2)
                    + 0.02 * ((r[0] - guess[0]) ** 2 + (r[2] - guess[2]) ** 2))
        world = rig.world(pose, rig.chain(["scythe"]))
        clear = min(rp.Rig.at(world, "scythe", q)[1] for q in SCYTHE_POINTS) >= 0.0
        if not clear or self.carry != [0.0, 0.0]:
            rp.solve(rig, pose, [("scythe", 0), ("scythe", 2)], error, step=4.0)
            world = rig.world(pose, rig.chain(["scythe"]))
            if min(rp.Rig.at(world, "scythe", q)[1] for q in SCYTHE_POINTS) < -0.3:
                # Badly stuck (the blade still turned down into the ground): searched from turns further round,
                # the one that clears nearest to where it was kept.
                _best(rig, pose, [("scythe", 0), ("scythe", 2)], error,
                      [{"scythe": [guess[0] + dx, guess[1], guess[2] + dz]} for dx in (-40, -20, 0, 20, 40) for dz in (-40, -20, 0, 20, 40)])
        r = pose["scythe"]["rotation"]
        carry = [r[0] - wanted[0], r[2] - wanted[2]]
        # Letting go of the ground gently: a correction may grow at once (to clear the ground) but eases off only a
        # few degrees a sample, so the scythe lifts off the ground rather than snapping back.
        eased = [old + max(-6.0, min(6.0, new - old)) if abs(new) < abs(old) else new for new, old in zip(carry, self.carry)]
        if eased != carry:
            trial = _copy(pose)
            trial["scythe"]["rotation"] = [wanted[0] + eased[0], r[1], wanted[2] + eased[1]]
            w = rig.world(trial, rig.chain(["scythe"]))
            if min(rp.Rig.at(w, "scythe", q)[1] for q in SCYTHE_POINTS) >= -0.05:
                pose["scythe"]["rotation"] = trial["scythe"]["rotation"]
                carry = eased
        self.carry = carry
        if abs(self.carry[0]) < 0.05 and abs(self.carry[1]) < 0.05:
            self.carry = [0.0, 0.0]
        return pose


def _hands_off_ground(rig, pose):
    """Each hand lifted just clear of the ground if it reaches into it (claws resting on it rather than through it)."""
    for side, sx in (("right", -1), ("left", 1)):
        tip = f"{side}_finger_1"
        length = 9 if side == "left" else 4
        point = [9 * sx, 16 - length, -0.5]
        if rp.Rig.at(rig.world(pose), tip, point)[1] < 0.3:
            rp.solve(rig, pose, [(f"{side}_forearm", 0), (f"{side}_arm", 2)],
                     lambda w, tip=tip, point=point: (max(0.0, 0.5 - rp.Rig.at(w, tip, point)[1])) ** 2 * 10
                     + 0.01 * (rp.Rig.at(w, tip, point)[1] - 0.5) ** 2)
    return pose


def _plant_scythe(rig, pose, z=-14.0, lean=8.0):
    """The scythe planted as a staff: its foot on the ground `z` forward (beside the right foot), its shaft near upright
    (leaning `lean` px over its length), the right arm and the turn of the scythe in the fist solved together."""
    def error(w):
        low, top = rp.Rig.at(w, "scythe", SHAFT_LOW), rp.Rig.at(w, "scythe", [-9, 53, 0])
        upright = (top[0] - low[0]) ** 2 + (top[2] - low[2] + lean) ** 2
        return (low[1] - 0.2) ** 2 * 4 + 0.3 * ((low[2] - z) ** 2 + (low[0] + 12) ** 2) + 0.05 * upright
    return _best(rig, pose, [("right_arm", 0), ("right_arm", 2), ("right_forearm", 0), ("scythe", 0)], error,
                 [{"right_arm": [-40, 0, 20], "right_forearm": [-40, 0, 0], "scythe": [HOLD_TURN + 30, 0, 0]},
                  {"right_arm": [-60, 0, 10], "right_forearm": [-20, 0, 0], "scythe": [HOLD_TURN + 60, 0, 0]},
                  {"right_arm": [-20, 0, 30], "right_forearm": [-60, 0, 0], "scythe": [HOLD_TURN, 0, 0]}])


def _best(rig, pose, unknowns, error, starts):
    """Solves from each of several starting turns of the unknowns, and keeps the best (a search can stall in a fold of
    its error; starting elsewhere finds the way round it)."""
    best, best_e = None, None
    for start in starts:
        trial = _copy(pose)
        for bone, v in start.items():
            trial.setdefault(bone, {})["rotation"] = list(v)
        rp.solve(rig, trial, unknowns, error)
        e = error(rig.world(trial))
        if best_e is None or e < best_e:
            best, best_e = trial, e
    pose.clear()
    pose.update(best)
    return pose


def _blade_down(rig, pose, ground=0.5):
    """The scythe turned in the fist so its blade's tip strikes the ground."""
    rp.solve(rig, pose, [("scythe", 0)], lambda w: (rp.Rig.at(w, "scythe", BLADE_TIP)[1] - ground) ** 2)
    return pose


def _grip_shaft(rig, pose, along=None):
    """The left hand brought to the shaft: both hands on the scythe. `along` is where on the shaft, from the right
    hand's grip (above it positive); by default wherever on it the left hand reaches best, a hand's width or more from
    the right hand."""
    choices = [along] if along is not None else [-16.0, -11.0, 11.0, 16.0]
    starts = [{"left_arm": [x, y, z], "left_forearm": [f, 0, 0]} for x in (-160, -100, -50) for y in (-30, 0, 30) for z in (40, -20) for f in (-40,)]
    best, best_e = None, None
    for at in choices:
        def error(w, at=at):
            target = rp.Rig.at(w, "scythe", [-9, 18 + at, 0])
            return float(((rp.Rig.at(w, "left_hand", LEFT_PALM) - target) ** 2).sum())
        trial = _best(rig, _copy(pose), [("left_arm", 0), ("left_arm", 1), ("left_arm", 2), ("left_forearm", 0), ("left_hand", 0),
                                        ("left_hand", 2), ("chest", 1)], error, starts)
        e = error(rig.world(trial))
        if best_e is None or e < best_e:
            best, best_e = trial, e
    pose.clear()
    pose.update(best)
    return pose


def _shaft_gap(w):
    """How far the left palm is from the scythe's shaft."""
    import numpy as np
    palm = rp.Rig.at(w, "left_hand", LEFT_PALM)
    a, b = rp.Rig.at(w, "scythe", SHAFT_LOW), rp.Rig.at(w, "scythe", [-9, 53, 0])
    u = float(np.clip(np.dot(palm - a, b - a) / np.dot(b - a, b - a), 0.1, 0.45))
    return float(np.linalg.norm(palm - (a + u * (b - a))))


def _claws_down(rig, pose, x=11.0, z=-8.0):
    """The left hand reached down so its claws rest on the ground beside it, (x, z)."""
    def error(w):
        tip = rp.Rig.at(w, "left_finger_1", [9, 7, -0.5])
        return (tip[1] - 0.4) ** 2 + 0.2 * ((tip[0] - x) ** 2 + (tip[2] - z) ** 2)
    rp.solve(rig, pose, [("left_arm", 0), ("left_arm", 2), ("left_forearm", 0)], error)
    return pose


def _patch_scythe(rig, clip, depth=4):
    """A settled clip mended between its samples: wherever the scythe, turning from one sample to the next, would pass
    through the ground between them, a sample is put in at the midpoint with the scythe cleared there (again within
    the new halves, up to `depth` times)."""
    keys = clip["bones"]["scythe"]["rotation"]
    for _ in range(depth):
        times = sorted(keys, key=float)
        added = 0
        for a_, b_ in zip(times, times[1:]):
            mid = round((float(a_) + float(b_)) / 2, 5)
            pose = rp.sample(clip, mid)
            w = rig.world(pose, rig.chain(["scythe"]))
            if min(rp.Rig.at(w, "scythe", q)[1] for q in SCYTHE_POINTS) >= -0.3:
                continue
            _ScytheClear(rig)(pose)
            keys[f"{mid}"] = {"post": [round(c, 3) for c in pose["scythe"]["rotation"]], "lerp_mode": "linear"}
            added += 1
        if not added:
            break
    clip["bones"]["scythe"]["rotation"] = {t: keys[t] for t in sorted(keys, key=float)}
    return clip


def _keys(*timed):
    """Key poses at times as a clip's bones: each bone's channels keyed at every time (a bone a pose leaves out keeps
    its pose before)."""
    names = set()
    for _, pose in timed:
        names |= set(pose)
    bones = {}
    for name in names:
        for ch in ("rotation", "position", "scale"):
            keys, last = {}, None
            for t, pose in timed:
                v = pose.get(name, {}).get(ch, last)
                if v is None:
                    v = [1, 1, 1] if ch == "scale" else [0, 0, 0]
                if ch == "rotation" and last is not None:
                    # Each turn taken the short way from the last key's (300 after 165 is -60: without this it would
                    # spin the long way round between them).
                    v = [c + 360.0 * round((p - c) / 360.0) for c, p in zip(v, last)]
                last = v
                keys[f"{t}"] = {"post": [round(c, 3) for c in v], "lerp_mode": "catmullrom"}
            if any(name in pose and ch in pose[name] for _, pose in timed):
                bones.setdefault(name, {})[ch] = keys
    return bones


HOLD_TURN = 60


def grounded(a, HOLD, T):
    """Its kneeling, staggering, recovering, struck, slamming, waking and dying clips, posed on the ground."""
    import rig_pose  # noqa: F401  (rp, at module level)
    rig = rp.Rig(build().bones)
    for clip in a.values():
        rp.seamless(clip)                                               # every loop joins up where it loops
    # Its standing clips settled on the ground first (their bent knees and sunk hips had its feet in it), so every
    # other clip starts from where it truly stands.
    for name in ("idle", "walk", "run", "swing", "deflected", "roar"):
        a[name] = _patch_scythe(rig, rp.settle(rig, a[name], BODY, step=0.05 if a[name]["animation_length"] <= 2 else 0.1, after=_ScytheClear(rig)))
    stand = rp.sample(a["idle"], 0.0)
    for name in ("swing", "deflected", "roar"):
        clip = a[name]
        for bone, chans in stand.items():
            for ch, v in chans.items():
                if bone == "root":
                    continue
                clip["bones"].setdefault(bone, {}).setdefault(ch, [round(c, 3) for c in v])
        for bone, chans in clip["bones"].items():
            for ch, keys in chans.items():
                if not isinstance(keys, dict) or ch == "position" and bone == "root":
                    continue
                at = stand.get(bone, {}).get(ch)
                if at is None:
                    at = [1, 1, 1] if ch == "scale" else [0, 0, 0]
                times = sorted(keys, key=float)
                for t in (times[0], times[-1]):
                    keys[t]["post"] = [round(v, 3) for v in at]
        a[name] = _patch_scythe(rig, rp.settle(rig, clip, BODY, step=0.025, after=_ScytheClear(rig)))
    stand.setdefault("root", {})["position"] = [0, 0, 0]
    stand = _ground(rig, stand)
    OPEN = 72
    ribs_open = {"rib_r_low": [0, OPEN, 0], "rib_r_high": [0, OPEN - 6, 0], "rib_l_low": [0, -OPEN, 0], "rib_l_high": [0, -(OPEN - 6), 0]}
    ribs_wide = {k: [0, v[1] * 1.18, 0] for k, v in ribs_open.items()}
    ribs_shut = {k: [0, 0, 0] for k in ribs_open}
    ribs_half = {k: [0, v[1] * 0.55, 0] for k, v in ribs_open.items()}

    def with_ribs(pose, ribs, heart=1.0, heart_z=0.0):
        pose = _set(pose, **ribs)
        pose["heart"] = {"scale": [heart] * 3, "position": [0, 0, heart_z]}
        return pose

    # ---- Kneeling, dormant: on its left knee, bowed over the scythe planted before it, its claws on the ground.
    dormant = _set(stand, torso=[22, 0, 0], chest=[18, 0, 0], head=[38, 0, 0], neck=[0, 0, 0],
                   right_arm=[-46, 0, 14], right_forearm=[-34, 0, 0], right_hand=[6, 0, 0], left_hand=[-24, 0, 0])
    dormant = _kneel(rig, dormant, "left", -10.0)
    dormant = _plant_scythe(rig, dormant, -16.0, 4.0)
    dormant = _claws_down(rig, dormant, 11.0, -7.0)
    dormant = _ground(rig, dormant)
    a["dormant"] = _patch_scythe(rig, rp.settle(rig, {"loop": True, "animation_length": 6.0, "bones": dict(
        _keys((0.0, with_ribs(dormant, ribs_shut, 0.85))),
        torso={"rotation": [f"22 + 1.2 * math.sin({T} * 60)", 0, 0]},
        head={"rotation": [f"38 + 1.5 * math.sin({T} * 60 + 40)", 0, 0]},
        heart={"scale": [f"0.85 + 0.08 * math.max(0, math.sin({T} * 60))"] * 3})}, BODY, step=0.25, after=_ScytheClear(rig)))

    # ---- Waking: stillness, a beat of its heart, its head lifting, a push up off the knee on the scythe, standing
    # tall and spread with its heart flaring, then settling into its stance.
    lift = _set(dormant, head=[6, 0, 0], torso=[14, 0, 0], chest=[8, 0, 0])
    # The push up: its hips raised halfway, both feet now planted (the right where it was, the left drawn in under it),
    # its weight on the staff. Set on the ground by its feet; the staff planted to fit, not propping the body up.
    push = _set(dormant, torso=[30, 0, 0], chest=[14, 0, 0], head=[-10, 0, 0])
    push["root"]["position"] = [0, round((dormant["root"]["position"][1] + stand["root"]["position"][1]) / 2, 2), 0]
    push = _plant_leg(rig, push, "right", -10.0)
    push = _plant_leg(rig, push, "left", 1.0)
    push = _ground(rig, push, FEET)
    push = _plant_scythe(rig, push, -17.0, 2.0)
    if rp.lowest(rig, rig.world(push), [("scythe", q) for q in SCYTHE_POINTS]) < 0.0:
        push = _scythe_rest(rig, push)
    push = _hands_off_ground(rig, push)
    tall = _set(stand, torso=[-12, 0, 0], chest=[-10, 0, 0], head=[-30, 0, 0], right_arm=[-40, 0, 55], left_arm=[-40, 0, -55],
                right_forearm=[-20, 0, 0], left_forearm=[-20, 0, 0], right_leg=[-6, 0, 3], left_leg=[-4, 0, -3],
                right_shin=[10, 0, 0], left_shin=[8, 0, 0], right_foot=[-4, 0, 0], left_foot=[-4, 0, 0])
    tall = _ground(rig, tall)
    awaken = _keys((0.0, with_ribs(dormant, ribs_shut, 0.9)), (0.6, with_ribs(dormant, ribs_shut, 0.9)), (0.9, with_ribs(lift, ribs_shut, 1.5)),
                   (1.35, with_ribs(push, ribs_shut, 1.1)), (1.85, with_ribs(tall, ribs_half, 1.8)), (2.4, with_ribs(stand, ribs_shut, 1.0)))
    a["awaken"] = _patch_scythe(rig, rp.settle(rig, {"loop": False, "animation_length": 2.4, "bones": awaken}, BODY, after=_ScytheClear(rig)))

    # ---- Staggered: thrown back a step, then down on its left knee, its ribs flung open on its heart, which swells
    # and comes forward; it leans on the scythe, its claws on the ground, its head lolling back.
    recoil = _set(stand, torso=[-14, 0, 6], chest=[-12, 0, 0], head=[-24, 12, 0], right_arm=[6, 0, 40], left_arm=[4, 0, -46],
                  right_forearm=[-14, 0, 0], left_forearm=[-12, 0, 0], right_leg=[-26, 0, 3], left_leg=[16, 0, -3],
                  right_shin=[30, 0, 0], left_shin=[26, 0, 0], scythe=[HOLD[0] + 30, 0, 0])
    recoil = _ground(rig, recoil)
    knelt = _set(stand, torso=[4, 0, 6], chest=[-14, 0, 0], head=[-14, 18, -16], right_arm=[-38, 0, 26], right_forearm=[-30, 0, 0],
                 right_hand=[0, 0, 0], left_hand=[-30, 0, 0])
    knelt = _kneel(rig, knelt, "left", -9.0)
    knelt = _plant_scythe(rig, knelt, -15.0, 10.0)
    knelt = _claws_down(rig, knelt, 12.0, -6.0)
    knelt = _ground(rig, knelt)
    impact = _set(knelt, torso=[12, 0, 6], head=[6, 18, -16])
    impact["root"]["position"] = [0, knelt["root"]["position"][1] - 1.0, 0]
    stagger = _keys((0.0, with_ribs(stand, ribs_shut)), (0.16, with_ribs(recoil, ribs_shut, 1.2)),
                    (0.42, with_ribs(impact, ribs_wide, 1.6, -1.0)), (0.7, with_ribs(knelt, ribs_open, 1.3, -1.2)))
    a["stagger"] = _patch_scythe(rig, rp.settle(rig, {"loop": False, "animation_length": 0.7, "bones": stagger}, BODY, after=_ScytheClear(rig)))
    B = f"{T} * 225"
    staggered = dict(_keys((0.0, with_ribs(knelt, ribs_open, 1.3, -1.2))))
    staggered["torso"] = {"rotation": [f"4 + 2.5 * math.sin({B})", 0, f"6 + 2 * math.sin({B} * 0.5)"]}
    staggered["head"] = {"rotation": [f"-14 + 4 * math.sin({B})", f"18 + 8 * math.sin({B} * 0.5)", -16]}
    staggered["heart"] = {"scale": [f"1.3 + 0.2 * math.pow(math.max(0, math.sin({B} * 2)), 6)"] * 3, "position": [0, 0, -1.2]}
    for k, v in ribs_open.items():
        staggered[k] = {"rotation": [0, f"{v[1]} + {4 if v[1] > 0 else -4} * math.sin({B})", 0]}
    a["staggered"] = _patch_scythe(rig, rp.settle(rig, {"loop": True, "animation_length": 1.6, "bones": staggered}, BODY, after=_ScytheClear(rig)))

    # ---- Recovering: a heave on the scythe and a push off its claws, the left foot planted, rising with its ribs
    # drawing shut, into its stance.
    heave = _set(knelt, torso=[26, 0, 0], chest=[10, 0, 0], head=[0, 0, 0], right_arm=[-24, 0, 20], right_forearm=[-12, 0, 0])
    heave = _plant_scythe(rig, _claws_down(rig, _ground(rig, heave), 12.0, -9.0), -15.0, 4.0)
    heave = _ground(rig, heave)
    halfway = _set(stand, torso=[24, 0, 0], chest=[12, 0, 0], head=[-4, 0, 0], left_leg=[-44, 0, -3], left_shin=[66, 0, 0],
                   left_foot=[-22, 0, 0], right_leg=[-30, 0, 3], right_shin=[58, 0, 0], right_foot=[-28, 0, 0])
    halfway = _ground(rig, halfway)
    recover = _keys((0.0, with_ribs(knelt, ribs_open, 1.3, -1.2)), (0.35, with_ribs(heave, ribs_half, 1.2, -0.6)),
                    (0.7, with_ribs(halfway, ribs_shut, 1.0)), (1.05, with_ribs(stand, ribs_shut)))
    a["recover"] = _patch_scythe(rig, rp.settle(rig, {"loop": False, "animation_length": 1.05, "bones": recover}, BODY, after=_ScytheClear(rig)))

    # ---- Struck in the heart: arched back on its knee, arms flung, its heart flaring and its ribs flung wider, then
    # slamming shut on the blow; doubled over clutching its chest; rising, into its stance.
    arched = _set(knelt, torso=[-24, 0, 0], chest=[-22, 0, 0], head=[-42, 0, 0], right_arm=[28, 0, 58], left_arm=[24, 0, -62],
                  right_forearm=[-10, 0, 0], left_forearm=[-10, 0, 0], scythe=[HOLD[0] + 50, 0, 0])
    arched = _ground(rig, arched)
    arched = _scythe_clear_hold(rig, arched)
    clutch = _set(knelt, torso=[34, 0, 0], chest=[22, 0, 0], head=[30, 0, 0], right_arm=[-58, 0, -16], left_arm=[-62, 0, 18],
                  right_forearm=[-78, 0, 0], left_forearm=[-82, 0, 0], scythe=[HOLD[0] + 20, 0, 0])
    clutch = _ground(rig, clutch)
    clutch = _scythe_clear_hold(rig, clutch)
    struck = _keys((0.0, with_ribs(knelt, ribs_open, 1.3, -1.2)), (0.12, with_ribs(arched, ribs_wide, 2.0, -1.6)),
                   (0.3, with_ribs(arched, {k: [0, -v[1] * 0.06, 0] for k, v in ribs_open.items()}, 0.8)),
                   (0.42, with_ribs(_set(arched, torso=[-10, 0, 0]), ribs_shut, 0.8)), (0.85, with_ribs(clutch, ribs_shut, 0.9)),
                   (1.25, with_ribs(halfway, ribs_shut)), (1.6, with_ribs(stand, ribs_shut)))
    a["heart_struck"] = _patch_scythe(rig, rp.settle(rig, {"loop": False, "animation_length": 1.6, "bones": struck}, BODY, step=0.025, after=_ScytheClear(rig)))

    # ---- The slam, two-handed: the scythe raised high behind its head in both hands, rising onto the balls of its
    # feet, its heart swelling; then a lunge and a great downward stroke, driving the blade into the ground before it
    # (its tip strikes at 0.75 s, the tick its roots are called), held there, heaving; and drawn back.
    raised = _set(stand, torso=[-16, 0, 0], chest=[-12, 0, 0], head=[-26, 0, 0], right_arm=[-168, 0, -32], right_forearm=[-26, 0, 0],
                  right_hand=[0, 0, 0], scythe=[HOLD[0] + 70, 0, 0], right_leg=[-8, 0, 3], left_leg=[-4, 0, -3],
                  right_shin=[12, 0, 0], left_shin=[10, 0, 0], right_foot=[8, 0, 0], left_foot=[8, 0, 0])
    raised = _grip_shaft(rig, raised)
    rp.solve(rig, raised, [("right_arm", 2), ("right_arm", 1), ("left_arm", 0), ("left_arm", 1), ("left_arm", 2), ("left_forearm", 0)],
             lambda w: _shaft_gap(w) ** 2)
    raised = _ground(rig, raised)
    struck_down = _set(stand, torso=[46, 0, 0], chest=[16, 0, 0], head=[14, 0, 0], right_arm=[-62, 0, -34], right_forearm=[-20, 0, 0],
                       right_hand=[0, 0, 0], right_leg=[-52, 0, 4], right_shin=[66, 0, 0], left_leg=[24, 0, -4], left_shin=[30, 0, 0])
    struck_down = _plant_leg(rig, _ground(rig, struck_down), "right", -14.0)
    struck_down = _ground(rig, struck_down)
    for _ in range(2):
        struck_down = _blade_down(rig, struck_down, 0.3)
        struck_down = _grip_shaft(rig, struck_down)
        struck_down = _ground(rig, struck_down)
    heaving = _set(struck_down, torso=[40, 0, 0], head=[4, 0, 0])
    for _ in range(2):
        heaving = _grip_shaft(rig, _blade_down(rig, _ground(rig, heaving), 0.3))
    slam = _keys((0.0, stand), (0.45, with_ribs(raised, ribs_shut, 1.4)), (0.62, with_ribs(_grip_shaft(rig, _set(raised, torso=[-20, 0, 0], head=[-30, 0, 0])), ribs_shut, 1.5)),
                 (0.75, with_ribs(struck_down, ribs_shut, 1.7)), (1.05, with_ribs(heaving, ribs_shut, 1.2)), (1.4, with_ribs(stand, ribs_shut)))
    a["slam"] = _patch_scythe(rig, rp.settle(rig, {"loop": False, "animation_length": 1.4, "bones": slam}, BODY, step=0.025, after=_ScytheClear(rig)))

    # ---- Dying: a convulsion (thrown back, arms flung, its ribs burst open and its heart flaring), a stagger back,
    # down on both knees, the scythe slipping from its fingers to the ground, swaying, its heart guttering, and a
    # slow collapse forward on to the ground before it, its heart a dim ember. Held.
    convulse = _set(stand, torso=[-24, 0, 0], chest=[-16, 0, 0], head=[-50, 0, 0], right_arm=[-120, 0, 70], left_arm=[-120, 0, -70],
                    right_forearm=[-10, 0, 0], left_forearm=[-10, 0, 0], left_hand=[20, 0, 0], scythe=[HOLD[0] + 40, 0, 0])
    convulse = _ground(rig, convulse)
    reel = _set(recoil, torso=[-8, 0, -6], head=[-30, -10, 0])
    reel = _ground(rig, reel)
    knees = _set(stand, torso=[8, 0, 0], chest=[4, 0, 0], head=[30, 0, 0], right_arm=[-6, 0, 18], left_arm=[-6, 0, -20],
                 right_forearm=[-20, 0, 0], left_forearm=[-20, 0, 0])
    knees = _kneel_both(rig, knees)
    knees = _scythe_rest(rig, _hands_off_ground(rig, knees))
    slipping = _set(knees, right_hand=[30, 0, 0], head=[38, 0, 6])
    slipping = _scythe_rest(rig, _hands_off_ground(rig, slipping), lie=True)
    sway = _set(slipping, torso=[18, 0, 6], head=[46, 0, 10])
    sway = _scythe_rest(rig, _hands_off_ground(rig, sway), lie=True)
    fallen = _set(knees, right_hand=[30, 0, 0], head=[20, 0, 18], right_arm=[-150, 0, 34], left_arm=[-150, 0, -30],
                  right_forearm=[-10, 0, 0], left_forearm=[-10, 0, 0])
    # Bowed forward from the knees on to the ground, its head turned aside as it goes down: the bow at the waist and
    # the turn of the head searched (within a body's reach) for the pose that, set on the ground, lies flattest, its
    # knees, its chest and its cheek all on it.
    fallen["chest"]["rotation"] = [24, 0, 0]
    best = None
    for bow in range(50, 111, 5):
        for nod in range(-50, 31, 20):
            for turn in (0, 70):
                for sweep in (-160, -120, 40, 75):
                    trial = _set(fallen, torso=[bow, 0, 6], head=[nod, turn, 0], right_arm=[sweep, 0, 30], left_arm=[sweep, 0, -30],
                                 right_forearm=[-10, 0, 0], left_forearm=[-10, 0, 0])
                    trial = _ground(rig, trial)
                    w = rig.world(trial, rig.chain(["left_shin", "chest", "head"]))
                    e = rp.Rig.at(w, "left_shin", [3, 18, -2.2])[1] + rp.Rig.at(w, "chest", [0, 44, -3.5])[1]
                    if best is None or e < best[0]:
                        best = (e, bow, nod, turn, sweep)
    fallen = _set(fallen, torso=[best[1], 0, 6], head=[best[2], best[3], 0], right_arm=[best[4], 0, 30], left_arm=[best[4], 0, -30],
                  right_forearm=[-10, 0, 0], left_forearm=[-10, 0, 0])
    fallen = _ground(rig, fallen, [("chest", [0, 44, -3.5]), ("head", [0, 50, -6]), ("left_shin", [3, 18, -2.2]), ("right_shin", [-3, 18, -2.2])])
    for side, sx in (("right", -1), ("left", 1)):
        hand = f"{side}_hand"
        rp.solve(rig, fallen, [(f"{side}_arm", 0), (f"{side}_arm", 2), (f"{side}_forearm", 0)],
                 lambda w, hand=hand, sx=sx: (rp.Rig.at(w, hand, [9 * sx, 16, 0])[1] - 1.0) ** 2
                 + 0.05 * (rp.Rig.at(w, hand, [9 * sx, 16, 0])[0] - 14 * sx) ** 2)
    fallen = _scythe_rest(rig, _hands_off_ground(rig, fallen), lie=True)
    death = _keys((0.0, with_ribs(stand, ribs_shut, 1.2)), (0.25, with_ribs(convulse, ribs_wide, 2.2, -1.2)),
                  (0.6, with_ribs(reel, ribs_open, 1.6, -0.8)), (1.05, with_ribs(knees, ribs_open, 1.3, -0.6)),
                  (1.6, with_ribs(slipping, ribs_half, 1.0)), (2.2, with_ribs(sway, ribs_half, 0.8)),
                  (2.9, with_ribs(fallen, ribs_half, 0.5)), (3.4, with_ribs(fallen, ribs_half, 0.25)))
    a["death"] = _patch_scythe(rig, rp.settle(rig, {"loop": "hold_on_last_frame", "animation_length": 3.4, "bones": death}, BODY, after=_ScytheClear(rig)))
    a["death"]["loop"] = "hold_on_last_frame"


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
    import geo_overlaps
    # No two faces left fighting in one plane; but not the cloak, whose lengths meet exactly and are skinned as one
    # (inflating them apart would make them overlap).
    geo_overlaps.separate([b for b in m.bones if not b["name"].startswith("cloak_")])
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.gardener", "texture_width": TEX, "texture_height": TEX},
        "bones": m.bones}]}
    arm = os.path.join(ASSETS, "armatures")
    write(os.path.join(arm, "gardener.geo.json"), geo)
    write(os.path.join(arm, "gardener.animation.json"), clips())
    write(os.path.join(arm, "gardener.armature.json"), {
        "materials": {"heart": "emissive"},
        "glow_textures": [f"{NS}:textures/armatures/gardener_glow.png"],
        # The cloak, cloth: each chain stiff at the shoulders, looser and heavier towards its hem, so it trails and
        # ripples; each length woven to the ones beside it (softly, so it bunches and folds), and a light wind on the
        # lower lengths so it never hangs quite still.
        # Skinned as one sheet, so it bends between its joints and no seam opens between its lengths or its columns.
        "cloth": [{"joints": [[f"cloak_{c}_{i}" for c in range(CLOAK_COLUMNS)] for i in range(len(CLOAK))]}],
        # Leaves, but heavy with damp and stitched thick: it hangs heavily and moves with its wearer (follow: only its own
        # swinging is damped, so it keeps up rather than trailing), well damped so the six lengths never whip, a firm bend
        # so it holds its curve round the thorns of the spine, little air drag or flutter; a faint stir at the hem.
        "springs": dict({f"cloak_{c}_{i}": dict({"fabric": "leaves", "mass": 1.5, "damping": 14.0, "gravity": 3.0, "follow": 0.9,
                                                 "air_drag": 0.015, "flutter": 0.04, "bend": 24.0, "link_stiffness": 0.45,
                                                 "stiffness": round(120 - i * 10, 1), "max_angle": 25 + i * 4,
                                                 "colliders": CLOAK_COLLIDERS, "length": CLOAK[i][0],
                                                 "links": [f"cloak_{k}_{i}" for k in (c - 1, c + 1) if 0 <= k < CLOAK_COLUMNS]},
                                                **({"wind": 0.1} if i >= len(CLOAK) - 2 else {}))
                         for c in range(CLOAK_COLUMNS) for i in range(len(CLOAK))},
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
    # The cloak's cubes kept their box's place on the texture for painting; the model needs only their faces.
    for bone in m.bones:
        for cube in bone["cubes"]:
            cube.pop("box_uv", None)
    write(os.path.join(arm, "gardener.geo.json"), geo)
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
