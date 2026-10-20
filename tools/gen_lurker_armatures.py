"""The Lurker's four bodies as CoolCatLib armatures (assets/whycantisleep/armatures/lurker_<kind>.*).

Each kind is laid out here the way it used to be built in Java (vanilla ModelParts: offsets from the parent, boxes from
the pivot, Y down, radians), then written as Bedrock geometry. Their skins (textures/entity/lurker/<kind>.png and
<kind>_eyes.png, from tools/gen_lurker_textures.ps1) are unchanged: every box keeps its place on them. Their motion is
procedural, in LurkerRenderer, which moves these joints by name.

Converting: a bone's pivot is its summed offsets (x mirrored, Y flipped up from 24); a box's origin is its far corner
the same way; a rotation is (x, -y, -z) in degrees (Y up instead of down reverses turns about x and z, on top of
Bedrock's own reversal of x and y as CoolCatLib reads it).
"""
import json

import geo_overlaps
import math
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "whycantisleep", "armatures")
NS = "whycantisleep"


class Model:
    def __init__(self, size):
        self.size = size
        self.parts = {}
        self.order = []

    def part(self, name, parent=None, offset=(0, 0, 0), rot=(0, 0, 0), uv=(0, 0), boxes=()):
        """A vanilla-style part: offset from its parent, rotation in radians, boxes (x, y, z, w, h, d) from its pivot,
        all on the skin from uv (or a box may carry its own uv as a seventh and eighth value)."""
        assert name not in self.parts, name
        at = offset if parent is None else tuple(a + b for a, b in zip(self.parts[parent]["at"], offset))
        self.parts[name] = {"parent": parent, "at": at, "rot": rot, "uv": uv, "boxes": list(boxes)}
        self.order.append(name)
        return name

    def geo(self, identifier):
        bones = []
        for name in self.order:
            p = self.parts[name]
            vx, vy, vz = p["at"]
            bone = {"name": name, "pivot": [r(-vx), r(24 - vy), r(vz)]}
            if p["parent"]:
                bone["parent"] = p["parent"]
            if any(p["rot"]):
                rx, ry, rz = p["rot"]
                # Bedrock's turns as CoolCatLib reads them: x and y reversed, in a space whose Y is up where vanilla's is
                # down, which reverses turns about x and z again: so (x, -y, -z).
                bone["rotation"] = [r(math.degrees(rx)), r(-math.degrees(ry)), r(-math.degrees(rz))]
            cubes = []
            for box in p["boxes"]:
                x, y, z, w, h, d = box[:6]
                uv = box[6:8] if len(box) > 6 else p["uv"]
                cubes.append({"origin": [r(-(vx + x + w)), r(24 - (vy + y + h)), r(vz + z)], "size": [r(w), r(h), r(d)], "uv": list(uv)})
            bone["cubes"] = cubes
            bones.append(bone)
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry." + identifier, "texture_width": self.size, "texture_height": self.size},
            "bones": bones}]}


def r(v):
    v = round(v, 4)
    return int(v) if v == int(v) else v


def eyes(m, head, spread, y, z, u, v):
    """Two eyes on a head's front face: a ball, a slit pupil standing proud of it, a lid hinged along its top."""
    for side, x in (("right_eye", -spread), ("left_eye", spread)):
        m.part(side, head, (x, y, z), uv=(u, v), boxes=[(-1, -1, -0.35, 2, 2, 1)])
        m.part(side + "_pupil", side, uv=(u + 8, v), boxes=[(-0.3, -0.8, -0.5, 0.6, 1.6, 1)])
        m.part(side + "_lid", side, (0, -1.1, 0), uv=(u + 14, v), boxes=[(-1.1, 0, -0.65, 2.2, 2.2, 1)])


def stalker():
    m = Model(64)
    m.part("chest", None, (0, 15, 1), uv=(0, 19), boxes=[(-4, -2.5, -8, 8, 5, 8)])
    m.part("belly", "chest", (0, 0.2, 0), uv=(0, 32), boxes=[(-3, -2, 0, 6, 4, 7)])
    m.part("carapace", "chest", uv=(0, 52), boxes=[(-4.5, -3.5, -7.5, 9, 1, 7)])
    m.part("chest_spines", "chest", uv=(48, 0), boxes=[(-1, -5.5, -7, 2, 3, 2), (-1, -4.5, -4, 2, 2, 2), (-1, -4, -1, 2, 2, 2)])
    m.part("belly_spines", "belly", uv=(48, 0), boxes=[(-1, -3.5, 1.5, 2, 2, 2), (-0.5, -3, 4.5, 1, 1, 2)])
    m.part("ribs", "chest", uv=(56, 0), boxes=[(x, -2, z, 1, 4, 1) for z in (-7, -5, -3) for x in (-5, 4)])
    parent = "belly"
    for i in range(4):
        tail = m.part(f"tail_{i}", parent, (0, -0.5 if i == 0 else 0, 7 if i == 0 else 5), uv=(28, 32), boxes=[(-1, -1, 0, 2, 2, 5)])
        spike = 2 if i < 2 else 1
        m.part(f"tail_spike_{i}", tail, uv=(48, 0), boxes=[(-0.5, -1 - spike, 1.5, 1, spike, 2)])
        parent = tail
    m.part("tail_blade", parent, uv=(34, 20), boxes=[(-0.5, -2.5, 4, 1, 5, 4)])

    m.part("head", None, (0, 14, -7), uv=(0, 0), boxes=[(-3.5, -3, -6, 7, 5, 6)])
    for side, x, yaw in (("right_horn", -2.5, -0.3), ("left_horn", 2.5, 0.3)):
        m.part(side, "head", (x, -3, -3), (0.6, yaw, 0), uv=(34, 20), boxes=[(-0.5, -0.5, 0, 1, 1, 4)])
        m.part(side + "_tip", side, (0, 0, 4), (0.55, 0, 0), uv=(34, 20), boxes=[(-0.5, -0.5, 0, 1, 1, 3)])
    m.part("brow", "head", uv=(34, 44), boxes=[(-3.5, -3.6, -6.6, 7, 1, 2)])
    m.part("fangs", "head", uv=(34, 48), boxes=[(-3.1, 1.5, -6.3, 0.7, 1.8, 0.7), (2.4, 1.5, -6.3, 0.7, 1.8, 0.7)])
    eyes(m, "head", 1.5, -1, -6, 0, 46)
    m.part("jaw", "head", (0, 2, 0), uv=(0, 11), boxes=[(-3.5, 0, -6, 7, 2, 6)])

    for name, x, z in (("right_arm", -4, -6), ("left_arm", 4, -6), ("right_leg", -3.5, 5), ("left_leg", 3.5, 5)):
        m.part(name, None, (x, 14, z), uv=(32, 0), boxes=[(-1, -7, -1, 2, 7, 2)])
        m.part(name + "_spur", name, uv=(56, 6), boxes=[(-0.5, -10, -0.5, 1, 3, 1)])
        lower = m.part(name + "_lower", name, (0, -7, 0), uv=(40, 0), boxes=[(-1, 0, -1, 2, 15, 2)])
        m.part(name + "_joint", lower, uv=(40, 50), boxes=[(-1.5, -1.5, -1.5, 3, 3, 3)])
        m.part(name + "_claws", lower, uv=(56, 6), boxes=[(-1.5, 14, -3.5, 1, 1, 3), (-0.5, 14, -4, 1, 1, 3), (0.5, 14, -3.5, 1, 1, 3)])
    return m


def flicker():
    m = Model(64)
    m.part("torso", None, (0, 12, 0), uv=(0, 0), boxes=[(-3, -8, -2, 6, 8, 4)])
    m.part("ribs", "torso", uv=(48, 20), boxes=[(x, y, -2.5, 1, 1, 5) for y in (-7, -5, -3) for x in (-4, 3)])
    m.part("shoulder_blades", "torso", uv=(48, 12), boxes=[(-4.5, -11, 0, 1, 4, 2), (3.5, -11, 0, 1, 4, 2)])
    parent = "torso"
    for i in range(4):
        thin = i >= 2
        spine = m.part(f"spine_{i}", parent, (0, 0 if i == 0 else 4, 0), uv=(8, 12) if thin else (0, 12),
                       boxes=[(-0.5, 0, -0.5, 1, 4, 1)] if thin else [(-1, 0, -1, 2, 4, 2)])
        if not thin:
            m.part(f"vertebra_{i}", spine, uv=(48, 12), boxes=[(-0.5, 1, 1, 1, 2, 2)])
        parent = spine

    m.part("head", "torso", (0, -8, 0), uv=(20, 0), boxes=[(-2.5, -6, -4, 5, 6, 6)])
    m.part("jaw", "head", (0, -0.5, 1.5), uv=(0, 20), boxes=[(-2, 0, -5.5, 4, 2, 5)])
    for side, x, roll in (("right_horn", -1.8, -0.25), ("left_horn", 1.8, 0.25)):
        m.part(side, "head", (x, -6, 0), (-0.35, 0, roll), uv=(44, 0), boxes=[(-0.5, -5, -0.5, 1, 5, 1)])
        m.part(side + "_tip", side, (0, -5, 0), (-0.5, 0, 0), uv=(44, 0), boxes=[(-0.5, -5, -0.5, 1, 5, 1)])
    eyes(m, "head", 1.2, -3.6, -4, 0, 46)

    for side, x in (("right", -4), ("left", 4)):
        m.part(side + "_arm", "torso", (x, -7, 0), uv=(28, 20), boxes=[(-1, 0, -1, 2, 9, 2)])
        m.part(side + "_forearm", side + "_arm", (0, 9, 0), uv=(36, 20), boxes=[(-1, 0, -1, 2, 9, 2)])
        m.part(side + "_claws", side + "_forearm", uv=(44, 20), boxes=[(-1.5, 9, -0.5, 1, 4, 1), (-0.5, 9, -1, 1, 5, 1), (0.5, 9, -0.5, 1, 4, 1)])
    return m


def brute():
    m = Model(128)
    m.part("hips", None, (0, 17, 1), uv=(48, 0), boxes=[(-5, -5, -4, 10, 5, 8)])
    m.part("torso", "hips", (0, -5, 0), uv=(0, 0), boxes=[(-7, -12, -5, 14, 12, 10)])
    m.part("plates", "torso", uv=(84, 0), boxes=[(-5, -13.5, -4, 10, 2, 8)])
    m.part("spikes", "torso", uv=(84, 12), boxes=[(-1, -17, -3, 2, 4, 2), (-1, -16, 0.5, 2, 3, 2), (-1, -10, 5, 2, 2, 3), (-1, -5, 5, 2, 2, 2)])
    m.part("shoulders", "torso", uv=(0, 70), boxes=[(-13, -13, -3, 6, 3, 6), (7, -13, -3, 6, 3, 6)])

    m.part("head", "torso", (0, -8, -5), uv=(0, 24), boxes=[(-3.5, -4, -6, 7, 6, 6)])
    m.part("brow", "head", uv=(72, 24), boxes=[(-3.5, -4.6, -6.6, 7, 1, 2)])
    m.part("right_horn", "head", (-3.5, -3.5, -2), (-0.5, 0.9, 0), uv=(56, 24), boxes=[(-1, -1, -4, 2, 2, 4)])
    m.part("left_horn", "head", (3.5, -3.5, -2), (-0.5, -0.9, 0), uv=(56, 24), boxes=[(-1, -1, -4, 2, 2, 4)])
    eyes(m, "head", 1.6, -1.8, -6, 0, 60)
    m.part("jaw", "head", (0, 2, 0), uv=(26, 24), boxes=[(-3.5, 0, -6.5, 7, 2, 6)])
    m.part("tusks", "jaw", uv=(52, 24), boxes=[(-3.6, -3, -6.9, 1, 3, 1), (2.6, -3, -6.9, 1, 3, 1)])

    for side, x in (("right", -9.5), ("left", 9.5)):
        m.part(side + "_arm", "torso", (x, -10, 0), uv=(0, 40), boxes=[(-2.5, 0, -2.5, 5, 8, 5)])
        m.part(side + "_forearm", side + "_arm", (0, 8, 0), uv=(20, 40), boxes=[(-3, 0, -3, 6, 9, 6)])
        m.part(side + "_fist", side + "_forearm", uv=(44, 40), boxes=[(-3.5, 9, -4, 7, 4, 7)])
    for side, x in (("right", -3.5), ("left", 3.5)):
        m.part(side + "_leg", None, (x, 17, 1), uv=(72, 40), boxes=[(-2.5, 0, -2.5, 5, 7, 5)])
        m.part(side + "_foot", side + "_leg", uv=(92, 40), boxes=[(-3, 5, -5, 6, 2, 7)])
    return m


def leech():
    m = Model(64)
    m.part("head", None, (0, 20.5, -5), uv=(0, 0), boxes=[(-3, -2.5, -5, 6, 5, 5)])
    eyes(m, "head", 1.6, -1.2, -5, 0, 46)
    for name, x, y in (("right_mandible", -2.6, 0.8), ("left_mandible", 2.6, 0.8), ("top_mandible", 0, -0.6), ("bottom_mandible", 0, 2.2)):
        m.part(name, "head", (x, y, -5), uv=(22, 0), boxes=[(-0.5, -0.5, -3, 1, 1, 3)])
    sizes = [(7, 6), (6, 5), (5, 4), (4, 3), (2, 2)]
    skins = [(0, 10), (0, 21), (0, 31), (24, 10), (24, 18)]
    parent = None
    for i, (w, h) in enumerate(sizes):
        drop = 0 if i == 0 else (sizes[i - 1][1] - h) / 2
        seg = m.part(f"segment_{i}", parent, (0, 20, -5) if i == 0 else (0, drop, 5), uv=skins[i], boxes=[(-w / 2, -h / 2, 0, w, h, 5)])
        if i < 4:
            fin = 2 if i < 2 else 1
            m.part(f"fin_{i}", seg, uv=(32, 0), boxes=[(-0.5, -h / 2 - fin, 1.5, 1, fin, 2)])
        if i < 3:
            reach = h / 2 - 1
            m.part(f"right_leg_{i}", seg, (-w / 2, reach, 2.5), (0, 0, 0.9), uv=(36, 0), boxes=[(-0.5, 0, -0.5, 1, 4, 1)])
            m.part(f"left_leg_{i}", seg, (w / 2, reach, 2.5), (0, 0, -0.9), uv=(36, 0), boxes=[(-0.5, 0, -0.5, 1, 4, 1)])
        parent = seg
    m.part("stinger", parent, (0, 0, 5), (0.5, 0, 0), uv=(24, 25), boxes=[(-0.5, -1, 0, 1, 1, 3)])
    return m


KINDS = {"stalker": stalker, "flicker": flicker, "brute": brute, "leech": leech}


def write(path, data):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def run():
    os.makedirs(OUT, exist_ok=True)
    for kind, build in KINDS.items():
        name = "lurker_" + kind
        geo = build().geo(name)
        geo_overlaps.separate(geo["minecraft:geometry"][0]["bones"])    # no two faces left fighting in one plane
        write(os.path.join(OUT, name + ".geo.json"), geo)
        write(os.path.join(OUT, name + ".armature.json"), {
            "textures": [f"{NS}:textures/entity/lurker/{kind}.png"],
            "glow_textures": [f"{NS}:textures/entity/lurker/{kind}_eyes.png"]})
    print("lurker armatures:", ", ".join(KINDS))


if __name__ == "__main__":
    run()
