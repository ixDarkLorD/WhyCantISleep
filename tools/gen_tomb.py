"""Generates the stone tomb's lid as an animated armature (TombClient draws it on the tomb's head-left part):
assets/whycantisleep/armatures/stone_tomb_lid.{geo,animation,armature}.json and its texture, put together from the
lid tiles gen_decor.py draws (tomb_lid_<side>_<segment>.png). Run after gen_deep_blocks.py.

Bedrock coordinates, centred on the head-left block, the head to the north (-z): the lid covers that block and the
one to its right (east: bedrock -x) and runs four blocks south.
"""
import json
import os

from PIL import Image

import gen_deep_blocks as blocks

ASSETS = blocks.ASSETS
NS = blocks.NS
TEX = 64


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def texture():
    """The lid's top (32 by 64, from the eight tiles) at the left; plain worn stone for its edges at the right."""
    tex = os.path.join(ASSETS, "textures", "block")
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))  # the lid's own part, put under the Revenant's below
    top = Image.new("RGBA", (32, 64))
    for si, side in enumerate(("left", "right")):
        for seg in range(4):
            top.paste(Image.open(os.path.join(tex, f"tomb_lid_{side}_{seg}.png")).convert("RGBA"), (si * 16, seg * 16))
    img.paste(top.rotate(180), (0, 0))  # the armature's up face runs the other way round from the block's
    stone = Image.open(os.path.join(tex, "decor_stone.png")).convert("RGBA")
    for y in range(0, TEX, 16):
        img.paste(stone, (32, y))
        img.paste(stone, (48, y))
    return img


def frames(*pairs):
    return {f"{t}": {"post": list(v), "lerp_mode": "catmullrom"} for t, v in pairs}


SLID = [-21, -2.5, 4]     # where the lid ends up: pushed off the tomb toward its right, its far edge down on the ground
TIPPED = [0, 6, -13]     # and turned a little, tipped down at its far edge


def main():
    edge = {"uv": [32, 0], "uv_size": [32, 2]}
    long_edge = {"uv": [32, 8], "uv_size": [32, 2]}
    lid = {"name": "lid", "pivot": [-8, 11, 24], "cubes": [
        {"origin": [-24, 10, -8], "size": [32, 2, 64], "uv": {
            "up": {"uv": [0, 0], "uv_size": [32, 64]}, "down": {"uv": [32, 16], "uv_size": [32, 32]},
            "north": edge, "south": edge, "east": long_edge, "west": long_edge}},
        # A low rim under the lid's edge, so it reads as a slab sitting on the tomb.
        {"origin": [-23.5, 9.5, -7.5], "size": [31, 0.5, 63], "uv": {d: {"uv": [32, 0], "uv_size": [16, 1]} for d in
                                                                   ("up", "down", "north", "south", "east", "west")}},
    ]}
    # Only the lid: the dead lying under it is drawn as itself, the Tomb Revenant held at the start of its rise
    # (TombClient), so the one that rises is the very one that lay there.
    write(os.path.join(ASSETS, "armatures", "stone_tomb_lid.geo.json"), {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.stone_tomb_lid", "texture_width": TEX, "texture_height": TEX},
        "bones": [{"name": "root", "pivot": [0, 0, 0]}, dict(lid, parent="root")]}]})
    anims = {
        "closed": {"loop": "hold_on_last_frame", "animation_length": 0.05, "bones": {"lid": {"position": [0, 0, 0], "rotation": [0, 0, 0]}}},
        # Heavy: it lifts and grates a little, then slides aside, tips down at one edge, and settles with a jolt.
        "open": {"loop": "hold_on_last_frame", "animation_length": 1.1, "bones": {"lid": {
            "position": frames((0.0, [0, 0, 0]), (0.2, [0, 1.2, 0]), (0.3, [-0.6, 1.2, 0.2]), (0.85, [SLID[0] - 1, SLID[1] + 0.6, SLID[2]]),
                               (0.97, SLID), (1.1, SLID)),
            "rotation": frames((0.0, [0, 0, 0]), (0.3, [0, 0.5, 0]), (0.85, [0, TIPPED[1], TIPPED[2] + 3]), (0.97, TIPPED), (1.04, [0, TIPPED[1], TIPPED[2] + 1.5]),
                               (1.1, TIPPED))}}},
        "opened": {"loop": "hold_on_last_frame", "animation_length": 0.05, "bones": {"lid": {"position": SLID, "rotation": TIPPED}}},
    }
    write(os.path.join(ASSETS, "armatures", "stone_tomb_lid.animation.json"),
          {"format_version": "1.8.0", "animations": {f"animation.stone_tomb_lid.{k}": v for k, v in anims.items()}})
    write(os.path.join(ASSETS, "armatures", "stone_tomb_lid.armature.json"), {"transition": {"duration": 0.1, "easing": "sine_in_out"}})
    stale = os.path.join(ASSETS, "textures", "armatures", "stone_tomb_lid_glow.png")
    if os.path.exists(stale):
        os.remove(stale)
    os.makedirs(os.path.join(ASSETS, "textures", "armatures"), exist_ok=True)
    texture().save(os.path.join(ASSETS, "textures", "armatures", "stone_tomb_lid.png"))
    print("stone_tomb_lid written")


if __name__ == "__main__":
    main()
