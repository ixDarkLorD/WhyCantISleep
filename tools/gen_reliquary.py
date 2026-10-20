"""Generates the heartwood reliquary's animated model (a CoolCatLib armature): the box and its lid on a hinge, with
clips for the lid swinging open and shut, drawn on the client by ArmatureBlockRenderer (ReliquaryClient).

Writes assets/whycantisleep/armatures/heartwood_reliquary.{geo,animation,armature}.json and its textures
(textures/armatures/heartwood_reliquary.png and _glow.png), painted from the reliquary's block textures in
gen_between_blocks.py (the boards, the iron, the lid's rune ring), so the block model (still used for the item) and the
animated one match. Run after gen_between_blocks.py.

Bedrock coordinates: centred on the block, x and z -8 to 8, y up from 0; the front faces north (-z), the hinge is at
the back of the lid's underside.
"""
import json
import math
import os

from PIL import Image

import gen_between_blocks as blocks

ASSETS = blocks.ASSETS
NS = blocks.NS
TEX = 128


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


class Model:
    def __init__(self):
        self.bones = []
        self.cubes = []  # (cube dict, (w, h, d), material)
        self.cursor = [0, 0, 0]

    def bone(self, name, pivot, parent=None):
        b = {"name": name, "pivot": pivot, "cubes": []}
        if parent:
            b["parent"] = parent
        self.bones.append(b)
        return b

    def cube(self, bone, origin, size, material):
        w, h, d = (max(1, math.ceil(v)) for v in size)
        uw, uh = 2 * (w + d), d + h
        x, y, row = self.cursor
        if x + uw > TEX:
            x, y, row = 0, y + row, 0
        self.cursor = [x + uw, y, max(row, uh)]
        assert y + uh <= TEX, "texture full"
        c = {"origin": origin, "size": size, "uv": [x, y]}
        bone["cubes"].append(c)
        self.cubes.append((c, (w, h, d), material))


def build():
    m = Model()
    root = m.bone("root", [0, 0, 0])
    # The box: heartwood boards on four iron feet, two iron bands round it, the clasp's lower half on its front.
    m.cube(root, [-6.5, 1, -6.5], [13, 8, 13], "wood")
    for x, z in ((-7, -7), (4.5, -7), (-7, 4.5), (4.5, 4.5)):
        m.cube(root, [x, 0, z], [2.5, 2, 2.5], "iron")
    for x in (-4.5, 3):
        m.cube(root, [x, 1, -6.7], [1.5, 8, 13.4], "iron")
    m.cube(root, [-1.5, 5.5, -6.9], [3, 3.5, 0.4], "iron")
    # The lid, on its hinge at the back of its underside: boards overhanging the box a little, a raised panel with
    # the rune ring on top, the bands over it, the clasp's plate hanging at the front with its red stone.
    lid = m.bone("lid", [0, 9, 7], "root")
    m.cube(lid, [-7, 9, -7], [14, 3, 14], "wood")
    m.cube(lid, [-6, 12, -6], [12, 1, 12], "top")
    for x in (-4.5, 3):
        m.cube(lid, [x, 9, -7.2], [1.5, 3.2, 14.4], "iron")
    m.cube(lid, [-1.5, 8.5, -7.5], [3, 3.5, 0.5], "iron")
    m.cube(lid, [-0.75, 9.5, -7.8], [1.5, 1.5, 0.3], "gem")
    return m


def paint(m):
    """Box UV, each cube's faces painted from its material's block texture; the lid's top face gets the rune ring."""
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    glow = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    wood, iron, top = blocks.reliquary_wood(), blocks.reliquary_iron(), blocks.reliquary_wood(True)
    for c, (w, h, d), material in m.cubes:
        u, v = c["uv"]
        faces = {"up": (u + d, v, w, d), "down": (u + d + w, v, w, d), "east": (u, v + d, d, h),
                 "north": (u + d, v + d, w, h), "west": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h)}
        for face, (fx, fy, fw, fh) in faces.items():
            for py in range(fh):
                for px in range(fw):
                    if material == "gem":
                        r = ((px - (fw - 1) / 2) ** 2 + (py - (fh - 1) / 2) ** 2) ** 0.5
                        col = blocks.HOT if r < 0.6 else blocks.RED
                        img.putpixel((fx + px, fy + py), col + (255,))
                        glow.putpixel((fx + px, fy + py), col + (255,))
                        continue
                    if material == "top" and face == "up":
                        src = top
                        sx, sy = px * 16 // fw, py * 16 // fh
                    else:
                        src = wood if material in ("wood", "top") else iron
                        sx, sy = (px + fx) % 16, (py + fy) % 16
                    col = src.getpixel((sx, sy))
                    img.putpixel((fx + px, fy + py), col)
                    # The rune ring on the lid glows a little, as the stone does.
                    if material == "top" and face == "up" and col[:3] in (blocks.RED, blocks.HOT):
                        glow.putpixel((fx + px, fy + py), col)
    return img, glow


def frames(*pairs):
    return {f"{t}": {"post": [a, 0, 0], "lerp_mode": "catmullrom"} for t, a in pairs}


OPEN = -100  # the lid's turn when it stands open, degrees about its hinge


def clips():
    a = {
        # Shut, still.
        "closed": {"loop": "hold_on_last_frame", "animation_length": 0.05, "bones": {"lid": {"rotation": [0, 0, 0]}}},
        # Heavy to start, then swinging up past open, and settling back.
        "open": {"loop": "hold_on_last_frame", "animation_length": 0.6, "bones": {"lid": {"rotation": frames(
            (0.0, 0), (0.12, -14), (0.38, OPEN - 12), (0.5, OPEN + 4), (0.6, OPEN))}}},
        # Lifted a touch, then dropping shut, a small bounce on the box.
        "close": {"loop": "hold_on_last_frame", "animation_length": 0.5, "bones": {"lid": {"rotation": frames(
            (0.0, OPEN), (0.1, OPEN - 6), (0.34, 0), (0.41, -7), (0.5, 0))}}},
    }
    return {"format_version": "1.8.0", "animations": {f"animation.heartwood_reliquary.{k}": v for k, v in a.items()}}


def main():
    m = build()
    import geo_overlaps
    geo_overlaps.separate(m.bones)                                      # no two faces left fighting in one plane
    arm = os.path.join(ASSETS, "armatures")
    write(os.path.join(arm, "heartwood_reliquary.geo.json"), {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.heartwood_reliquary", "texture_width": TEX, "texture_height": TEX},
        "bones": m.bones}]})
    write(os.path.join(arm, "heartwood_reliquary.animation.json"), clips())
    write(os.path.join(arm, "heartwood_reliquary.armature.json"), {
        "glow_textures": [f"{NS}:textures/armatures/heartwood_reliquary_glow.png"],
        "transition": {"duration": 0.12, "easing": "sine_in_out"},
    })
    img, glow = paint(m)
    os.makedirs(os.path.join(ASSETS, "textures", "armatures"), exist_ok=True)
    img.save(os.path.join(ASSETS, "textures", "armatures", "heartwood_reliquary.png"))
    glow.save(os.path.join(ASSETS, "textures", "armatures", "heartwood_reliquary_glow.png"))
    print("cubes:", len(m.cubes))


if __name__ == "__main__":
    main()
