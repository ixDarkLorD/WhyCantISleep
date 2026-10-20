"""Generates the labyrinth's five creatures as CoolCatLib armatures (between/mob, drawn by LabyrinthMobsClient): for each,
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
import geo_overlaps

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
    # The open Dark Between's creatures: a sleepwalker's dirty nightgown, pallid skin and long dark hair; the manta's
    # shadow and its veined wing membrane.
    "gown": [(112, 110, 118), (140, 138, 144), (166, 164, 168), (188, 186, 188), (208, 206, 204)],
    "pale": [(112, 118, 132), (134, 140, 152), (154, 160, 170), (174, 180, 188), (194, 200, 206)],
    "hair": [(12, 10, 14), (22, 18, 24), (32, 28, 36), (44, 40, 48), (58, 54, 62)],
    "manta": [(10, 8, 16), (18, 14, 26), (28, 22, 38), (38, 30, 50), (52, 42, 66)],
    "membrane": [(14, 10, 22), (24, 18, 34), (36, 28, 48), (48, 38, 62), (62, 50, 78)],
    # The Dream Eater's hide, the night sky with stars in it; a Somnimoth's dusty fur and its wings; the Faceless's
    # smooth pale skin and its black suit.
    "hide": [(16, 14, 34), (24, 20, 48), (34, 28, 62), (46, 38, 78), (60, 50, 96)],
    "dust": [(70, 64, 78), (90, 84, 98), (110, 104, 118), (130, 124, 136), (150, 144, 156)],
    "wing": [(56, 50, 66), (78, 72, 88), (102, 94, 112), (124, 116, 132), (146, 138, 152)],
    "faceless": [(176, 170, 164), (190, 184, 178), (202, 197, 190), (212, 208, 202), (222, 218, 212)],
    "suit": [(10, 10, 14), (16, 16, 20), (22, 22, 28), (30, 30, 36), (40, 40, 48)],
    # The Dark Between's wild creatures (BetweenCreatures): the Night Mare's hide, the Hollow Hound's smoke, the
    # Lullaby's porcelain and lace, the Gazer's eyeball and flesh, the Tick-Tock's brass, the Stitchling's stuffing and
    # yarn (its patches are drawn by patch_pixel), the Gloom Jelly's bell and frills, the Dream Hare's fur.
    "nightmare": [(14, 10, 20), (22, 16, 30), (32, 24, 42), (44, 34, 56), (58, 46, 72)],
    "smoke": [(26, 26, 30), (38, 38, 44), (52, 52, 58), (68, 68, 74), (86, 86, 92)],
    "porcelain": [(178, 168, 160), (198, 188, 178), (214, 206, 196), (228, 222, 212), (240, 236, 228)],
    "lace": [(70, 60, 80), (90, 80, 100), (110, 100, 120), (132, 122, 140), (152, 144, 160)],
    "eyeball": [(176, 160, 160), (200, 186, 184), (220, 208, 204), (232, 224, 220), (244, 238, 234)],
    "flesh": [(60, 24, 34), (80, 34, 46), (100, 46, 58), (122, 60, 72), (142, 76, 88)],
    "brass": [(70, 50, 20), (100, 74, 30), (130, 98, 42), (160, 124, 56), (190, 156, 80)],
    "patch": [(60, 40, 40), (80, 56, 52), (100, 72, 64), (120, 90, 78), (140, 108, 92)],
    "stuffing": [(180, 176, 170), (200, 196, 190), (214, 210, 204), (226, 222, 216), (236, 232, 226)],
    "yarn": [(60, 20, 30), (80, 30, 40), (100, 40, 52), (120, 52, 64), (140, 64, 76)],
    "jelly": [(40, 30, 80), (56, 44, 104), (74, 60, 130), (92, 78, 156), (112, 98, 180)],
    "frill": [(70, 50, 120), (90, 66, 146), (112, 84, 170), (134, 104, 192), (156, 126, 210)],
    "hare": [(150, 150, 166), (170, 170, 184), (188, 188, 200), (204, 204, 214), (220, 220, 228)],
    # The Grinning Clown: its harlequin suit (diamonds drawn in base_pixel), ruff and gloves, greasepaint, hair, shoes,
    # pompoms, mallet and balloons.
    "harlequin": [(30, 18, 26), (44, 26, 38), (58, 34, 50), (74, 44, 64), (90, 56, 78)],
    "ruff": [(170, 164, 160), (194, 188, 184), (212, 208, 204), (226, 222, 218), (238, 236, 232)],
    "greasepaint": [(196, 190, 184), (212, 206, 200), (224, 220, 214), (234, 230, 226), (244, 242, 238)],
    "clown_hair": [(150, 50, 20), (186, 70, 26), (214, 94, 34), (234, 122, 46), (246, 150, 64)],
    "shoe": [(70, 12, 18), (96, 18, 26), (124, 26, 34), (150, 38, 44), (172, 52, 58)],
    "pom": [(170, 140, 30), (200, 168, 40), (224, 194, 56), (240, 214, 76), (250, 230, 110)],
    "mallet": [(140, 30, 36), (172, 44, 48), (200, 62, 62), (222, 84, 80), (240, 110, 100)],
    "balloon_red": [(130, 14, 26), (168, 24, 38), (200, 36, 50), (226, 60, 70), (246, 110, 116)],
    "balloon_violet": [(70, 30, 120), (96, 44, 156), (124, 62, 190), (152, 86, 218), (184, 124, 240)],
    # The other clowns: the Mime's stripes, the Jack-in-the-Box's toy box, the Balloon Clown's candy stripes and rainbow
    # hair and its balloons, the Funhouse Clown's checks.
    "mime_stripes": [(20, 20, 24), (30, 30, 34), (40, 40, 46), (206, 204, 200), (226, 224, 220)],
    "toybox": [(110, 24, 32), (140, 34, 42), (166, 46, 52), (190, 62, 66), (210, 82, 84)],
    "candy": [(200, 100, 150), (220, 120, 170), (236, 146, 190), (246, 172, 208), (252, 196, 224)],
    "afro": [(200, 60, 60), (230, 150, 50), (230, 210, 70), (90, 190, 100), (80, 150, 220)],
    "funhouse": [(60, 30, 84), (84, 44, 112), (108, 58, 140), (60, 130, 74), (80, 160, 94)],
    "balloon_cyan": [(20, 110, 140), (30, 140, 170), (50, 170, 200), (90, 200, 226), (150, 230, 246)],
    # Little Red Riding Hood: her red cape and hood, white blouse, dark bodice, blue skirt, boots, fair hair, rosy skin,
    # her basket's gingham cloth and the bread in it.
    "cape_red": [(120, 16, 24), (150, 24, 32), (178, 36, 42), (204, 52, 56), (226, 74, 74)],
    "blouse": [(196, 192, 186), (214, 210, 204), (228, 224, 218), (238, 236, 232), (246, 244, 240)],
    "bodice": [(30, 22, 24), (44, 32, 34), (58, 42, 44), (72, 54, 56), (88, 68, 70)],
    "skirt": [(40, 56, 104), (52, 72, 128), (66, 90, 150), (84, 110, 172), (104, 130, 190)],
    "skirt_fold": [(28, 38, 76), (36, 50, 96), (46, 64, 114), (58, 80, 132), (72, 96, 150)],        # the skirt's inner pleats, in shadow
    "boot": [(52, 32, 20), (70, 44, 28), (90, 58, 38), (110, 74, 50), (130, 92, 64)],
    "hair_fair": [(180, 140, 60), (204, 164, 80), (222, 186, 104), (236, 206, 130), (246, 224, 160)],
    "rosy": [(200, 150, 130), (220, 170, 150), (236, 190, 170), (246, 206, 188), (252, 222, 206)],
    # Her true form: her cape gone to rags, her blouse stained, her nails grown black.
    "red_rags": [(70, 8, 14), (96, 14, 22), (120, 20, 28), (146, 30, 36), (168, 44, 48)],
    "bloodied": [(150, 140, 132), (172, 162, 152), (190, 180, 170), (204, 196, 186), (216, 210, 202)],
    "hair_lank": [(58, 48, 34), (76, 64, 44), (94, 80, 56), (112, 96, 68), (130, 112, 80)],
    "claw": [(10, 8, 10), (20, 16, 18), (32, 26, 28), (46, 38, 40), (60, 52, 54)],
    "gingham": [(150, 24, 32), (190, 40, 46), (226, 220, 214), (238, 234, 228), (246, 244, 240)],
    "bread": [(130, 80, 30), (160, 104, 44), (186, 128, 60), (208, 152, 80), (226, 178, 104)],
    "balloon_yellow": [(160, 120, 20), (200, 160, 30), (230, 196, 50), (246, 220, 90), (252, 238, 150)],
    "balloon_green": [(30, 110, 50), (40, 140, 64), (60, 170, 84), (100, 200, 116), (156, 230, 160)],
    # Children's-story nightmares and phobias (author, 2026-10-19): the closet's wood and the dark in it, the Wolf's fur,
    # shawl and bonnet, the Sandman's cloak and sack, the Tooth Taker's skin and wings, the Broodmother's carapace,
    # belly and eggs, the Void Eel, the Hive-Thing's pitted flesh (holes drawn in base_pixel), the Crawlspace's skin.
    "closet": [(34, 22, 18), (48, 32, 26), (62, 42, 34), (78, 54, 42), (94, 68, 54)],
    "shadow": [(6, 4, 10), (12, 8, 18), (18, 12, 26), (26, 18, 36), (34, 24, 46)],
    "wolf_fur": [(46, 40, 36), (64, 56, 50), (84, 74, 64), (104, 92, 80), (124, 112, 98)],
    "shawl": [(70, 16, 24), (92, 24, 34), (114, 34, 44), (136, 48, 56), (156, 64, 70)],
    "bonnet": [(176, 170, 160), (196, 190, 180), (212, 206, 198), (226, 222, 214), (238, 234, 228)],
    "sand_cloak": [(40, 34, 30), (56, 48, 40), (74, 64, 52), (92, 80, 64), (110, 96, 78)],
    "burlap": [(100, 80, 50), (122, 98, 62), (144, 118, 76), (164, 136, 90), (182, 154, 106)],
    "fairy": [(150, 156, 170), (172, 178, 190), (192, 198, 208), (208, 214, 222), (224, 228, 234)],
    "fairy_wing": [(110, 150, 190), (134, 174, 210), (160, 196, 226), (186, 216, 238), (210, 232, 246)],
    "brood": [(14, 10, 16), (24, 16, 28), (36, 24, 40), (50, 34, 56), (66, 46, 72)],
    "brood_belly": [(46, 24, 34), (66, 34, 46), (86, 46, 58), (106, 60, 72), (126, 76, 88)],
    "egg": [(176, 172, 150), (196, 192, 170), (212, 208, 188), (226, 222, 204), (238, 236, 220)],
    "eel": [(6, 10, 22), (10, 16, 34), (16, 24, 46), (24, 34, 60), (34, 46, 76)],
    "hive": [(90, 60, 50), (116, 80, 64), (140, 100, 80), (162, 120, 96), (182, 140, 114)],
    "crawl": [(90, 88, 84), (112, 110, 104), (132, 130, 124), (150, 148, 142), (168, 166, 160)],
}
GLOW = {"eye": [(255, 60, 70), (255, 150, 140)], "tear": [(150, 8, 26), (220, 28, 48)],
        "core": [(170, 14, 34), (230, 50, 64), (255, 140, 140), (255, 210, 200)], "vein": [(130, 8, 28), (200, 26, 46)],
        "drop": [(200, 20, 40), (255, 120, 120)], "berry": [(170, 20, 30), (230, 60, 60)],
        "violet": [(110, 40, 190), (170, 90, 240), (220, 170, 255)],
        "flame": [(80, 24, 150), (150, 70, 235), (225, 175, 255)], "dreamlight": [(80, 170, 220), (150, 220, 255), (220, 245, 255)],
        "jelly_core": [(90, 60, 200), (160, 120, 255), (230, 200, 255)], "strand": [(70, 90, 200), (120, 160, 255)],
        "nose": [(170, 10, 24), (230, 30, 44), (255, 90, 90)],
        "dreamsand": [(150, 110, 30), (220, 180, 70), (255, 230, 150)],
        "venom": [(50, 120, 30), (110, 210, 60), (200, 255, 140)], "wolf_eye": [(200, 140, 20), (255, 206, 60), (255, 240, 150)], "hourglass": [(140, 8, 18), (220, 28, 38), (255, 110, 100)],
        "egg_glow": [(130, 120, 70), (200, 190, 120), (250, 244, 196)], "eel_light": [(30, 120, 160), (80, 200, 230), (180, 250, 255)]}
# Faces: the material the rest of the head is, and how to paint its front.
FACES = {"red_face": "rosy", "feral_face": "pale", "feral_jaw": "pale", "mime_face": "greasepaint", "twoface_face": "greasepaint", "wail_face": "porcelain", "closet_face": "closet", "wolf_face": "wolf_fur", "sandman_face": "shadow", "tooth_face": "fairy", "brood_face": "brood",
         "eel_face": "eel", "hive_face": "hive", "crawl_face": "crawl", "clown_face": "greasepaint", "mare_face": "nightmare", "hound_face": "smoke", "doll_face": "porcelain", "gazer_face": "eyeball", "clock_face": "brass",
         "button_face": "patch", "hare_face": "hare", "eater_face": "hide", "moth_face": "dust", "faceless_face": "faceless", "sleeper_face": "pale", "manta_face": "manta", "revenant_face": "bone", "mourner_face": "skin", "archer_face": "root", "warden_mask": "stone", "thornling_face": "bramble", "wisp_face": "core"}


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
    if material in ("cloth", "gown"):
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
    elif material == "red_rags":
        if py >= fh - 1 and (px + seed) % 2 == 0:
            return None                                             # torn: its hem in tatters
        if (px * 3 + py + seed) % 7 == 0:
            k -= 1
    elif material == "bloodied":
        h = (px * 73856093 ^ (seed + 7) * 19349663 ^ len(face) * 83492791) % 100
        if h < 14 and py < 1 + h % max(1, fh):
            return (110, 10, 20)                                    # blood run down it, here and there
        if rng.random() < 0.08:
            return (140, 20, 28)                                    # spatters
    elif material == "rosy":
        k = 2 if face == "down" else 3                              # smooth skin, no speckle (it read as stubble)
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
    elif material == "hide":
        if rng.random() < 0.035:
            return (196, 170, 255)                                  # a star in its hide
        if (px * 5 + py * 3 + seed) % 13 == 0:
            k -= 1                                                  # darker swirls, like night cloud
    elif material in ("dust", "wing"):
        if (px + py * 2 + seed) % 5 == 0:
            k += 1                                                  # dusty scales catching the light
        if material == "wing" and face in ("up", "down"):
            # Bands across the wing, and an eyespot at its middle, ringed (its pupil glows: paint()).
            cx, cy = (fw - 1) / 2, (fh - 1) / 2
            r = math.hypot((px - cx) / max(1.0, fw / 2), (py - cy) / max(1.0, fh / 2))
            if r < 0.2:
                return WING_SPOT
            if r < 0.36:
                return (34, 20, 52)
            if r < 0.46:
                return (150, 130, 176)
            if (px + py) % 4 == 0:
                k -= 1
    elif material == "closet":
        if px % 4 == 0 or py in (0, fh - 1):
            k -= 1                                                  # its boards and their seams
        if rng.random() < 0.04:
            k -= 1                                                  # knots in the wood
    elif material == "shawl":
        if (px + py) % 2 == 0:
            k += 1                                                  # knitted
        if py % 4 == 3:
            k -= 1
    elif material == "burlap":
        if (px % 2 == 0) != (py % 2 == 0):
            k -= 1                                                  # its weave
    elif material == "hive":
        # Holes: clusters of pits, each a dark hole in a raised rim.
        for hx, hy in ((2, 2), (5, 3), (3, 6), (7, 6), (1, 9), (5, 9), (8, 1), (8, 10)):
            d = math.hypot(px % 10 - hx, py % 12 - hy)
            if d < 0.8:
                return (20, 10, 8)
            if d < 1.5:
                return (196, 156, 128)
    elif material == "eel":
        if (px * 3 + py + seed) % 9 == 0 and face not in ("down",):
            return (40, 150, 190)                                   # its spots, lit faintly
    elif material == "brood":
        if (px + seed) % 3 == 0:
            k += 1                                                  # bristles
    elif material == "egg":
        if rng.random() < 0.12:
            return (150, 140, 110)                                  # something moving inside
    elif material == "crawl":
        if (py + seed) % 3 == 0:
            k -= 1                                                  # stretched, in lines
    elif material == "harlequin":
        # Diamonds, faded red and black, a thin gold line between them.
        u, v = (px + py) % 6, (px - py) % 6
        if u == 0 or v == 0:
            return (120, 96, 40)
        red = ((px + py) // 6 + (px - py) // 6) % 2 == 0
        reds = [(96, 18, 28), (122, 28, 38), (146, 40, 50)]
        return reds[max(0, min(2, k - 1))] if red else pick(pal, k - 1)
    elif material == "ruff":
        if px % 2 == 0:
            k -= 1                                                  # its pleats
    elif material == "gingham":
        return pick(pal, 1) if (px // 2 + py // 2) % 2 == 0 else pick(pal, 3)  # red and white checks
    elif material == "bread":
        if py == 0 and px % 3 == 1:
            return pick(pal, 4)                                     # scored crust, golden
    elif material == "cape_red":
        if px % 3 == 0:
            k -= 1                                                  # its folds
    elif material == "mime_stripes":
        return pick(pal, 0 + (rng.random() < 0.3)) if py % 2 else pick(pal, 3 + (rng.random() < 0.4))      # black and white, row on row
    elif material == "toybox":
        if px in (0, fw - 1) or py in (0, fh - 1):
            return (200, 160, 50)                                   # its painted gold trim
        if (px * 5 + py * 3 + seed) % 11 == 0:
            return (240, 220, 120)                                  # stars painted on it
    elif material == "candy":
        return [(236, 146, 190), (110, 200, 220), (240, 210, 90)][(px // 2 + seed) % 3] if rng.random() > 0.08 else (250, 250, 250)
    elif material == "afro":
        return pal[(px // 2 + (py // 2) * 3 + seed) % len(pal)]   # rainbow curls
    elif material == "funhouse":
        return pick(pal, 1 + (rng.random() < 0.3)) if ((px // 2) + (py // 2)) % 2 else pick(pal, 3 + (rng.random() < 0.4))
    elif material in ("balloon_red", "balloon_violet", "balloon_cyan", "balloon_yellow", "balloon_green"):
        if face not in ("down",) and px == 1 and py == 1:
            return pick(pal, 4)                                     # a glint on it
    elif material == "greasepaint":
        if rng.random() < 0.05:
            return (150, 144, 140)                                  # the paint cracked
    elif material == "patch":
        return patch_pixel(rng, face, px, py, fw, fh, seed)
    elif material == "eyeball":
        if rng.random() < 0.05 or (px * 3 + py + seed) % 17 == 0:
            return (150, 40, 50)                                    # veins over the white
    elif material == "flesh":
        if (px + py + seed) % 3 == 0:
            k -= 1                                                  # wet folds
    elif material == "brass":
        if py % 3 == 0:
            k += 1                                                  # bands, polished
        if rng.random() < 0.06:
            return (60, 110, 90)                                    # verdigris
    elif material == "smoke":
        if rng.random() < 0.25:
            k += 1 if rng.random() < 0.5 else -1                    # it drifts as you look
    elif material == "nightmare":
        if (px + seed) % 4 == 0 and face not in ("up", "down"):
            k -= 1                                                  # its coat's sheen, in streaks
    elif material in ("jelly", "frill"):
        if (px * 2 + py + seed) % 5 == 0:
            k += 1                                                  # ribs of its bell
    elif material == "hare":
        if (px + py * 3 + seed) % 7 == 0:
            k -= 1                                                  # soft fur, tufted
    elif material == "faceless":
        k = 2 + (rng.random() < 0.12) - (rng.random() < 0.12)       # smooth, barely a mark on it
    elif material == "suit":
        if (px + seed) % 4 == 0:
            k += 1                                                  # a faint pinstripe
    elif material == "membrane":
        if (px + seed) % 4 == 0 or (py * 3 + px) % 7 == 0:
            k -= 1                                                  # its veins, dark across the membrane
    elif material == "hair":
        if (px + seed) % 2 == 0:
            k -= 1                                                  # strands
    elif material == "rope":
        if (px + py) % 2 == 0:
            k -= 1
    return pick(pal, k)


WING_SPOT = (226, 200, 255)
# A Stitchling's patches: worn red, faded blue, mustard and moss, sewn together with dark thread.
PATCHES = [[(92, 30, 34), (116, 42, 44), (138, 58, 56)], [(44, 60, 92), (60, 78, 112), (80, 98, 130)],
           [(120, 96, 34), (146, 118, 46), (168, 140, 62)], [(52, 70, 40), (68, 88, 52), (86, 106, 66)]]


def patch_pixel(rng, face, px, py, fw, fh, seed):
    """A Stitchling's patchwork: squares of cloth, one colour each, with running stitches between them."""
    cell = (px // 5, py // 5)
    if (px % 5 == 4 and py % 3 == 1) or (py % 5 == 4 and px % 3 == 1):
        return (24, 18, 16)                                         # the stitches between patches
    pal = PATCHES[(cell[0] * 7 + cell[1] * 3 + seed) % len(PATCHES)]
    k = 1 + (rng.random() < 0.25) - (rng.random() < 0.25)
    return pal[max(0, min(2, k))]


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
    elif kind == "sleeper_face":
        ey = round(fh * 0.45)
        for ex in (round(cx - fw * 0.22), round(cx + fw * 0.22)):
            if px == ex and py == ey:
                return (255, 40, 50), True                          # its eye: shut in its texture, open (red) in its glow
            if abs(px - ex) <= 1 and py == ey + 1:
                return (96, 92, 110), False                         # the dark under its eyes
        if abs(px - cx) <= 1 and py == round(fh * 0.78):
            return (40, 30, 36), False                              # its mouth, slack
    elif kind == "wail_face":
        # A porcelain face wailing: hollow black sockets with a red pinprick deep in each, blood running from them,
        # a mouth stretched open with a glow far down in it, the porcelain cracked.
        ey = round(fh * 0.34)
        for ex in (round(cx - fw * 0.25), round(cx + fw * 0.25)):
            if px == ex and py == ey:
                return (255, 40, 44), True
            if abs(px - ex) <= 0.5 + (px < cx) * 0.5 - (px > cx) * 0.0 and ey - 1 <= py <= ey + 1 and abs(px - ex) <= 1 and (px - ex) in ((-1, 0) if ex < cx else (0, 1)):
                return (8, 3, 6), False                             # hollow sockets, two wide
            if px == ex and py > ey + 1:
                return (170, 12, 26), True                          # blood down its cheeks
        top = round(fh * 0.62)
        if abs(px - cx) <= 0.6 and top <= py <= fh - 1:
            if py == fh - 2 and abs(px - cx) <= 0.5:
                return (190, 30, 40), True
            return (10, 4, 8), False                                # its mouth, open in a wail
        if (px * 7 + py * 3) % 13 == 0:
            return (120, 112, 110), False                           # cracks
    elif kind == "closet_face":
        # Its doors' front: a seam down the middle, two brass handles, and, between the doors, two eyes looking out.
        if abs(px - cx) < 0.6:
            return ((255, 60, 70), True) if py in (round(fh * 0.38),) else ((10, 6, 6), False)
        if py == round(fh * 0.5) and abs(px - cx) in (1.5, 2, 1):
            return pick(PALETTES["brass"], 3), False
        if px in (0, fw - 1) or py in (0, fh - 1) or py == round(fh * 0.2) or py == round(fh * 0.8):
            return pick(PALETTES["closet"], 0), False               # panel frames
    elif kind == "wolf_face":
        # The front of its snout: its nose and its teeth (its eyes are on its skull, beside the snout).
        if py == fh - 2 and abs(px - cx) <= fw * 0.35:
            return ((236, 226, 200), False) if px % 2 else ((40, 10, 12), False)   # its teeth, too many
        if abs(px - cx) <= 0.5 and py == round(fh * 0.55):
            return (14, 10, 10), False                              # its nose
    elif kind == "sandman_face":
        ey = round(fh * 0.45)
        for ex in (round(cx - fw * 0.2), round(cx + fw * 0.2)):
            if px == ex and py == ey:
                return (255, 220, 120), True                        # two gold eyes in the dark of its hood
        return (6, 4, 8), False
    elif kind == "tooth_face":
        ey = round(fh * 0.3)
        for ex in (round(cx - fw * 0.25), round(cx + fw * 0.25)):
            if px == ex and py == ey:
                return (8, 6, 10), False                            # black little eyes
        if py >= round(fh * 0.55) and py <= fh - 2 and abs(px - cx) <= fw * 0.4:
            return ((240, 236, 220), False) if (px + py) % 2 == 0 else ((90, 20, 30), False)   # a mouth full of teeth
    elif kind == "brood_face":
        for ex, ey in ((cx - 1.5, 1), (cx + 1.5, 1), (cx - 0.5, 2), (cx + 0.5, 2), (cx - 2.5, 2), (cx + 2.5, 2), (cx - 1.5, 3), (cx + 1.5, 3)):
            if round(ex) == px and ey == py:
                return (255, 50, 60), True                          # eight eyes
        if py == fh - 1 and abs(px - cx) <= 1:
            return (180, 170, 140), False                           # fangs
    elif kind == "eel_face":
        ey = round(fh * 0.3)
        for ex in (0, fw - 1):
            if px == ex and py == ey:
                return (180, 250, 255), True
        if py == fh - 1:
            return ((220, 230, 240), False) if px % 2 else ((4, 6, 12), False)   # needle teeth
    elif kind == "hive_face":
        r = math.hypot(px - cx, py - (fh - 1) / 2)
        if r < 1.0:
            return (255, 200, 60), True                             # one eye, deep in its holes
        return None
    elif kind == "crawl_face":
        ey = round(fh * 0.3)
        for ex in (round(cx - fw * 0.3), round(cx + fw * 0.3)):
            if px == ex and py == ey:
                return (240, 244, 255), True                        # wide-set white eyes
        if py == round(fh * 0.7) and abs(px - cx) <= fw * 0.4:
            return (20, 14, 14), False                              # a stretched mouth
    elif kind == "red_face":
        # A sweet face: big dark eyes each with a shine, rosy cheeks, a small smile.
        # Her face is 6 pixels square. Her mouth sits a row up off her chin, so the chin below it reads as skin
        # rather than a beard; her eyes sit a row lower than the top, which her hood's brim shades.
        art = ["SSSSSS",
               "SwSSwS",
               "SkSSkS",
               "pSSSSp",
               "SSmmSS",
               "SSSSSS"]
        c = art[min(5, py * 6 // max(1, fh))][min(5, px * 6 // max(1, fw))]
        if c == "S":
            return None
        return {"w": ((250, 250, 255), False), "k": ((40, 26, 36), False), "p": ((236, 140, 150), False), "m": ((170, 60, 80), False)}[c]
    elif kind == "feral_face":
        # Her true face, 7 by 6: two hollow black sockets with a red pinprick deep in each, blood run from them, and a
        # grin from ear to ear, its corners turned up, a row of needle teeth along it.
        art = ["SSSSSSS",
               "SkkSkkS",
               "SkrSrkS",
               "SbSSSbS",
               "dSSSSSd",
               "tdtdtdt"]
        c = art[min(5, py * 6 // max(1, fh))][min(6, px * 7 // max(1, fw))]
        if c == "S":
            return None
        return {"k": ((6, 4, 8), False), "r": ((255, 40, 50), True), "b": ((110, 10, 20), False),
                "d": ((20, 6, 10), False), "t": ((232, 226, 210), False)}[c]
    elif kind == "feral_jaw":
        art = ["tdtdtdt",
               "dbdddbd"]
        c = art[min(1, py * 2 // max(1, fh))][min(6, px * 7 // max(1, fw))]
        return {"t": ((232, 226, 210), False), "d": ((20, 6, 10), False), "b": ((110, 10, 20), False)}[c]
    elif kind == "mime_face":
        # White paint, black arched brows, black eyes with a pale glint, one painted tear, small black lips.
        ey = round(fh * 0.4)
        for ex in (round(cx - fw * 0.24), round(cx + fw * 0.24)):
            if px == ex and py == ey:
                return (235, 235, 255), True
            if abs(px - ex) <= 1 and py == ey - 2:
                return (14, 12, 16), False
            if abs(px - ex) <= 0.6 and py == ey:
                return (14, 12, 16), False
        if px == round(cx + fw * 0.24) and py in (ey + 2, ey + 3):
            return (14, 12, 16), False                              # the painted tear
        if abs(px - cx) <= 0.6 and py == round(fh * 0.76):
            return (14, 12, 16), False
    elif kind == "twoface_face":
        # Two masks in one, drawn on an 8 by 8 face: laughing on one half (a gold eye under an arched brow, a rosy
        # cheek, a grin rising to its corner), weeping on the other (paler, a blue eye under a sorrowing brow, a tear
        # running, a frown falling to its corner); the grin runs on across a jagged seam into the frown.
        art = ["WWWWsBBB",
               "WkkWsBBB",
               "kWWkskkB",
               "WgkWsbkB",
               "pWWWsBtB",
               "WrWsLBtB",
               "WWrrLLBt",
               "WWWWsBBB"]
        c = art[min(7, py * 8 // max(1, fh))][min(7, px * 8 // max(1, fw))]
        return {"W": ((232, 228, 220), False), "B": ((196, 204, 222), False), "k": ((22, 16, 26), False), "s": ((60, 40, 70), False),
                "g": ((255, 206, 60), True), "b": ((110, 170, 255), True), "t": ((120, 180, 255), True), "p": ((236, 140, 150), False),
                "r": ((190, 24, 44), False), "L": ((40, 44, 100), False)}[c]
    elif kind == "clown_face":
        ey = round(fh * 0.36)
        for ex in (round(cx - fw * 0.24), round(cx + fw * 0.24)):
            if px == ex and py == ey:
                return (255, 220, 60), True                         # its pupils, yellow, lit
            if abs(px - ex) + abs(py - ey) <= 1 or (px == ex and abs(py - ey) == 2):
                return (16, 10, 16), False                          # a black diamond painted round each eye
        gy = round(fh * 0.72)
        if abs(px - cx) <= fw * 0.36 and py == gy:
            return ((236, 226, 200), False) if (px % 2 == 0 and abs(px - cx) < fw * 0.3) else ((176, 20, 36), False)   # the grin, teeth in it
        if abs(px - cx) >= fw * 0.3 and abs(px - cx) <= fw * 0.42 and py == gy - 1:
            return (176, 20, 36), False                             # its corners, turned up
        if abs(px - cx) <= fw * 0.3 and py == gy + 1:
            return (130, 14, 28), False                             # its lower lip
    elif kind == "mare_face":
        if py == fh - 2 and px in (round(cx - fw * 0.25), round(cx + fw * 0.25)):
            return (190, 110, 255), True                            # its nostrils smoulder
    elif kind == "hound_face":
        ey = round(fh * 0.35)
        for ex in (round(cx - fw * 0.25), round(cx + fw * 0.25)):
            if px == ex and py == ey:
                return (200, 230, 255), True                        # pale cold eyes
            if abs(px - ex) <= 1 and py == ey + 1:
                return (12, 12, 16), False
    elif kind == "doll_face":
        ey = round(fh * 0.42)
        for ex in (round(cx - fw * 0.22), round(cx + fw * 0.22)):
            if abs(px - ex) <= 0.5 and abs(py - ey) <= 0:
                return (8, 6, 10), False                            # black glass eyes
            if px == ex and py == ey + 1:
                return (200, 40, 70), True                          # a red tear under each
            if abs(px - ex) <= 1 and py == ey + 2:
                return (220, 150, 160), False                       # painted cheeks
        if abs(px - cx) <= 1 and py == round(fh * 0.78):
            return (110, 30, 46), False                             # a small painted mouth
        if px == round(cx + fw * 0.3) and py < ey - 1 and (py + px) % 2 == 0:
            return (120, 110, 104), False                           # a crack across the porcelain
    elif kind == "gazer_face":
        r = math.hypot(px - cx, py - (fh - 1) / 2) / max(1.0, fw / 2)
        if r < 0.14:
            return (10, 4, 8), False                                # its pupil, a pit
        if r < 0.42:
            return ((255, 70, 60) if r < 0.3 else (190, 30, 40)), True   # the iris, burning
        if r < 0.5:
            return (90, 20, 30), False
    elif kind == "clock_face":
        r = math.hypot(px - cx, py - (fh - 1) / 2) / max(1.0, min(fw, fh) / 2)
        if r > 0.92:
            return pick(PALETTES["brass"], 4), False                # its bezel
        if r > 0.8:
            return (226, 218, 190), False                           # the dial's rim, marked
        if (px == round(cx) and py <= (fh - 1) / 2) or (py == round((fh - 1) / 2) and px >= cx):
            return (20, 16, 10), False                              # its hands
        if py == round(fh * 0.3) and px in (round(cx - fw * 0.28), round(cx + fw * 0.28)):
            return (255, 190, 90), True                             # two lit eyes, at ten and two
        return (214, 206, 176), False                               # the dial
    elif kind == "button_face":
        ey = round(fh * 0.4)
        for ex in (round(cx - fw * 0.22), round(cx + fw * 0.22)):
            if abs(px - ex) <= 1 and abs(py - ey) <= 1:
                return ((20, 16, 14), False) if (px == ex or py == ey) else ((150, 30, 40), True)   # buttons for eyes
        if abs(px - cx) <= 2 and py == round(fh * 0.76):
            return ((20, 16, 14) if px % 2 else (210, 200, 190)), False   # its mouth, sewn shut
    elif kind == "hare_face":
        ey = round(fh * 0.35)
        for ex in (0, fw - 1):
            if px == ex and py == ey:
                return (160, 230, 255), True                        # its eyes, at the sides of its head
        if abs(px - cx) <= 0.5 and py == round(fh * 0.65):
            return (200, 140, 160), False                           # its nose
    elif kind == "eater_face":
        ey = round(fh * 0.3)
        for ex in (round(cx - fw * 0.3), round(cx + fw * 0.3)):
            if px == ex and py == ey:
                return (220, 190, 255), True                        # small pale eyes, wide set
            if abs(px - ex) <= 1 and py == ey - 1:
                return (12, 10, 26), False                          # heavy lids over them
    elif kind == "moth_face":
        for ex in (0, fw - 1):
            if abs(px - ex) <= 0 and py <= fh - 2:
                return (210, 230, 255), True                        # its two great eyes, at the sides of its head
    elif kind == "faceless_face":
        # No face at all: only the faintest hollows where eyes would be, and a seam where a mouth would be.
        ey = round(fh * 0.42)
        for ex in (round(cx - fw * 0.2), round(cx + fw * 0.2)):
            if abs(px - ex) <= 0.5 and py == ey:
                return pick(PALETTES["faceless"], 0), False
        if abs(px - cx) <= 1 and py == round(fh * 0.74):
            return pick(PALETTES["faceless"], 1), False
    elif kind == "manta_face":
        ey = round(fh * 0.4)
        for ex in (1, fw - 2):
            if px == ex and py == ey:
                return (200, 120, 255), True                        # its eyes, violet, at the corners of its head
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
                    if face == "down" and base in PALETTES and col != WING_SPOT:
                        # Its underside, drawn on purpose (the noise above is still drawn, so every other face keeps its
                        # pattern): a sole for what stands on the ground, a clean soft shade for anything else.
                        img.putpixel(at, underside(base, c["origin"][1] <= 0.15, px, py, fw, fh) + (255,))
                        continue
                    if col == WING_SPOT:
                        img.putpixel(at, col + (255,))
                        glow.putpixel(at, col + (255,))
                        continue
                    # Shaded at its edges, darker low on its sides.
                    edge = px == 0 or py == 0 or px == fw - 1 or py == fh - 1
                    f = (0.82 if edge else 1.0) * (1.0 - 0.18 * (py / max(1, fh - 1)) if face not in ("up", "down") else 1.0)
                    img.putpixel(at, shade(col, f) + (255,))
    return img, glow


def underside(material, grounded, px, py, fw, fh):
    """A bottom face. On the ground, a sole: a dark rim round it, a line where the heel begins, ridged tread with a stud
    now and then. Off it (the underside of a body, a jaw, an arm), the material's own colour, clean, a little darker at
    its edges: no noise to read as dirt."""
    pal = PALETTES[material]
    edge = px == 0 or py == 0 or px == fw - 1 or py == fh - 1
    if grounded:
        rim, dark, light = shade(pick(pal, 0), 0.55), shade(pick(pal, 0), 0.8), shade(pick(pal, 1), 0.92)
        if edge or (fh >= 5 and py == round(fh * 0.64)):
            return rim
        if fw >= 3 and fh >= 3 and (px + py * 2) % 5 == 0:
            return light                                            # a stud
        return dark if py % 2 == 0 else light                       # the tread, ridged across it
    return shade(pick(pal, 1), 0.78 if edge else 0.9)


def frames(*pairs):
    return {f"{t}": {"post": list(v), "lerp_mode": "catmullrom"} for t, v in pairs}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def humanoid_walk(arms_forward=0.0, swing=32, stride=40, knee=True, bend=40, lead=0, lift=0):
    """Legs swinging opposite to the arms. With `knee`, each knee bends (`bend` degrees at most) as its leg swings
    forward, `lead` degrees of the stride early, and its thigh lifts `lift` more as it does, to carry the foot clear."""
    swing_r = f"math.max(0, -math.sin({T} * 360 + {lead + 90}))"            # 0..1 while the right leg swings forward
    swing_l = f"math.max(0, math.sin({T} * 360 + {lead + 90}))"
    b = {
        "right_leg": {"rotation": [f"{stride} * math.sin({T} * 360)" + (f" - {lift} * {swing_r}" if knee and lift else ""), 0, 0]},
        "left_leg": {"rotation": [f"-{stride} * math.sin({T} * 360)" + (f" - {lift} * {swing_l}" if knee and lift else ""), 0, 0]},
        "right_arm": {"rotation": [f"{-arms_forward} - {swing} * math.sin({T} * 360)", 0, 0]},
        "left_arm": {"rotation": [f"{-arms_forward} + {swing} * math.sin({T} * 360)", 0, 0]},
        "body": {"rotation": [4, f"4 * math.sin({T} * 360)", 0]},
    }
    if knee:
        # A knee bends as its leg swings forward (the leg's angle falling: +x swings a leg back), and is straight
        # while its foot is down and pushing back; the other way round it looks like walking backwards.
        b["right_shin"] = {"rotation": [f"{bend} * {swing_r}", 0, 0]}
        b["left_shin"] = {"rotation": [f"{bend} * {swing_l}", 0, 0]}
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


def _bone(m, name):
    return next(b for b in m.bones if b["name"] == name)


def _sheet(m, rows, wrap=False):
    """A cloth sheet for CoolCatLib (the armature file's "cloth"): joints by row down it, each row its columns across.
    Their cubes are skinned to each other, so the cloth bends smoothly between them and no seam opens."""
    if not hasattr(m, "cloth_sheets"):
        m.cloth_sheets = []
    m.cloth_sheets.append(dict({"joints": rows}, **({"wrap": True} if wrap else {})))


def skirt(m, parent, waist, hem, half_x, half_z, material, name="skirt", rows=2, legs=("right_leg", "left_leg"), thick=0.45,
          flare=0.0, torn=False, drag=False, colliders=(), fabric="cotton", tune=None):
    """A skirt (or a gown's, a cloak's lower half) as cloth: six panels round the waist (front and back in halves, a
    side each), each a chain of `rows` lengths, each length a spring; the lengths of a row woven to their neighbours
    round the ring (CoolCatLib's spring links) so it moves as one sheet, swinging out as its wearer turns,
    and pushed out by the legs as they stride (a ball at each knee and shin); and skinned as one sheet (`_sheet`), so
    it bends between its joints and stays whole. It hangs straight at rest (`flare` 0): a flare turns the front and side
    panels different ways, and their corners part. `drag`: its hem rests on the ground and trails on the blocks. Returns
    the springs."""
    # Balls down each leg, one at the height of each row's hem (where that row's weight hangs), on the thigh above the
    # knee and the shin below it, big enough to keep the cloth off the leg's surface: as a leg strides, the cloth over
    # it is pushed along with it. (Kept on the model, `m.leg_balls`, for a cape behind to be pushed by too.)
    seg = (waist - hem) / rows
    balls = list(colliders)
    for leg in legs:
        thigh = _bone(m, leg)
        shin = _bone(m, leg.replace("leg", "shin"))
        x = thigh["pivot"][0]
        width = max(c["size"][0] for c in thigh["cubes"]) / 2
        knee = shin["pivot"][1]
        for r in range(rows):
            y = round(waist - (r + 1) * seg, 3)
            on = thigh if y >= knee else shin
            point = f"{leg}_cloth_{r}"
            on.setdefault("locators", {})[point] = [x, y, 0]
            balls.append({"point": point, "radius": round(width + thick + 0.45, 2)})
    m.leg_balls = [b for b in balls if b not in colliders]
    m.cube(_bone(m, parent), [-half_x, waist - 0.5, -half_z], [2 * half_x, 0.5, 2 * half_z], material)   # its waistband
    # The panels round the waist, in order (so neighbours are next to each other): (name, pivot x, z, cube x, z,
    # width x, z, its flare's turn).
    panels = [("front_r", -half_x / 2, -half_z, -half_x, -half_z, half_x, thick, [-flare, 0, 0]),
              ("front_l", half_x / 2, -half_z, 0, -half_z, half_x, thick, [-flare, 0, 0]),
              ("side_l", half_x, 0, half_x - thick, -half_z + thick, thick, 2 * half_z - 2 * thick, [0, 0, -flare]),
              ("back_l", half_x / 2, half_z, 0, half_z - thick, half_x, thick, [flare, 0, 0]),
              ("back_r", -half_x / 2, half_z, -half_x, half_z - thick, half_x, thick, [flare, 0, 0]),
              ("side_r", -half_x, 0, -half_x, -half_z + thick, thick, 2 * half_z - 2 * thick, [0, 0, flare])]
    springs = {}
    for r in range(rows):
        top = waist - r * seg
        for i, (side, px, pz, cx, cz, w, d, turn) in enumerate(panels):
            bone = m.bone(f"{name}_{side}_{r}", [px, top, pz], parent if r == 0 else f"{name}_{side}_{r - 1}",
                          rotation=turn if r == 0 else None)
            m.cube(bone, [cx, top - seg, cz], [w, seg, d], material)                               # meeting the next (skinned to it)
            if torn and r == rows - 1:
                for k, f in enumerate((0.2, 0.65)):
                    tx = cx + (w - 1) * f if d <= thick else cx
                    tz = cz if d <= thick else cz + (d - 1) * f
                    m.cube(bone, [tx, top - seg - 1.3 - k * 0.4, tz], [1 if d <= thick else thick, 1.3 + k * 0.4, thick if d <= thick else 1], material)
            ring = [f"{name}_{panels[(i - 1) % 6][0]}_{r}", f"{name}_{panels[(i + 1) % 6][0]}_{r}"]
            # What it is made of (its weight, how it hangs, flows, ripples, gives and folds), and its cut: held firmer
            # at the waist than at the hem, swinging further toward the hem.
            spring = dict({"fabric": fabric, "stiffness": round(80 - r * 55 / max(1, rows - 1), 1),
                           "max_angle": round(32 + r * 24 / max(1, rows - 1), 1),
                           "length": round(seg, 3), "colliders": balls, "links": ring}, **(tune or {}))
            if drag and r == rows - 1:
                spring["collide"] = {"blocks": True, "entities": False, "radius": 0.6, "bounce": 0.0, "friction": 0.7}
            springs[bone["name"]] = spring
    _sheet(m, [[f"{name}_{panel[0]}_{r}" for panel in panels] for r in range(rows)], wrap=True)
    return springs


def _euler(m):
    """A rotation matrix as a bone's rotation in the geometry file (degrees, as rig_pose.turn reads them)."""
    b = math.asin(max(-1.0, min(1.0, -m[2][0])))
    if abs(math.cos(b)) > 1e-6:
        a, c = math.atan2(m[1][0], m[0][0]), math.atan2(m[2][1], m[2][2])
    else:
        a, c = math.atan2(-m[0][1], m[1][1]), 0.0
    return [round(-math.degrees(c), 3), round(math.degrees(b), 3), round(-math.degrees(a), 3)]


SURFACE = 1.6                   # how far inside a skirt panel its surface point is (pleated_skirt)


def pleated_skirt(m, parent, waist, hem, half_x, half_z, material, fold_material=None, name="skirt", pleats=16, rows=3,
                  legs=("right_leg", "left_leg"), thick=0.3, depth=0.3, flare=14.0, curl=4.0, petticoat=None, colliders=(),
                  fabric="cotton", tune=None, band=None):
    """A real skirt, as cloth: `pleats` panels round an oval waist, edge to edge, one smooth closed ring. Their edges
    zigzag (every other corner of the ring `depth` out), so each panel is a pleat angled a little in or out, the ones
    angled in drawn in `fold_material` (in shadow); every panel is tilted out by `flare` along the ring's own outward
    way (not its pleat's), and each row below by `curl` more, so the whole skirt flares to its hem as one cone, A-line,
    and curves out softly as it goes down; its waistband (`band`, or the skirt's own material) round the top panels.
    Each a chain of `rows` lengths, each length a spring, woven round the ring and
    skinned as one sheet; CoolCatLib welds the seams where the flare parts the square-cut panels a little, so no gap
    opens anywhere. Pushed by the legs as they stride. `petticoat`: a lace edge of that material under the hem (welded
    into one ring too). Each panel carries a point `SURFACE` inside it, so a ball of that radius there is the skirt's
    outer face, swinging with it: `m.skirt_surface` lists them as (point, panel, row), for cloth over it (an apron, a
    cape) to lie on the skirt itself. Returns the springs."""
    import numpy as np
    import rig_pose as rp
    seg = (waist - hem) / rows
    # Balls down the legs, at each row's hem height, as skirt() puts them.
    balls = list(colliders)
    for leg in legs:
        thigh = _bone(m, leg)
        shin = _bone(m, leg.replace("leg", "shin"))
        x = thigh["pivot"][0]
        width = max(c["size"][0] for c in thigh["cubes"]) / 2
        knee = shin["pivot"][1]
        for r in range(rows):
            y = round(waist - (r + 1) * seg, 3)
            on = thigh if y >= knee else shin
            point = f"{leg}_cloth_{r}"
            on.setdefault("locators", {})[point] = [x, y, 0]
            balls.append({"point": point, "radius": round(width + thick + 0.3, 2)})               # just clear of the leg: the hem
    m.leg_balls = [b for b in balls if b not in colliders]                                       # brushes it, not lifted high off it

    def oval(k, out=0.0):
        phi = 2 * math.pi * k / pleats
        nx, nz = math.sin(phi) / half_x, -math.cos(phi) / half_z
        n = math.hypot(nx, nz)
        return half_x * math.sin(phi) + nx / n * out, -half_z * math.cos(phi) + nz / n * out

    def outward(a, b):
        """The yaw (file degrees) of a panel from corner a to corner b, facing out from the middle."""
        dx, dz = b[0] - a[0], b[1] - a[1]
        nx, nz = dz, -dx
        if nx * (a[0] + b[0]) + nz * (a[1] + b[1]) < 0:
            nx, nz = -nx, -nz
        return -math.degrees(math.atan2(nx, -nz))

    names = []
    springs = {}
    m.skirt_surface = []
    for i in range(pleats):
        a, b = oval(i, depth if i % 2 else 0.0), oval(i + 1, depth if (i + 1) % 2 else 0.0)
        mx, mz = (a[0] + b[0]) / 2, (a[1] + b[1]) / 2
        width = math.hypot(b[0] - a[0], b[1] - a[1])
        ring = outward(oval(i), oval(i + 1))                 # the way the ring faces out here
        zig = outward(a, b) - ring                           # how this pleat turns from it
        zig = (zig + 180) % 360 - 180
        mat = fold_material if fold_material and zig < 0 else material
        names.append(f"{name}_{i}")
        # Where each row should lie: the pleat turned from the ring, tilted out along the ring, turned round the waist.
        place = [rp.turn(0, ring, 0) @ rp.turn(-(flare + r * curl), 0, 0) @ rp.turn(0, zig, 0) for r in range(rows)]
        for r in range(rows):
            top = waist - r * seg
            rest = place[0] if r == 0 else np.linalg.inv(place[r - 1]) @ place[r]
            point = f"{name}_{i}_{r}_surface"
            bone = m.bone(f"{name}_{i}_{r}", [round(mx, 3), round(top, 3), round(mz, 3)], parent if r == 0 else f"{name}_{i}_{r - 1}",
                          rotation=_euler(rest), locators={point: [round(mx, 3), round(top - seg / 2, 3), round(mz + SURFACE, 3)]})
            m.skirt_surface.append((point, i, r))
            # Drawn facing the front, hanging from its pivot (the middle of its top edge): its turn sets it on the ring.
            m.cube(bone, [round(mx - width / 2, 3), round(top - seg, 3), round(mz, 3)], [round(width, 3), round(seg, 3), thick], mat)
            if r == 0:
                # Its waistband: a strip round the top of each top panel, standing a little out from it (and its
                # corners apart from the panel's), so with its neighbours it is one smooth band round the oval waist.
                m.cube(bone, [round(mx - width / 2, 3), round(top - 0.8, 3), round(mz - 0.2, 3)], [round(width, 3), 0.9, round(thick + 0.1, 3)],
                       band or material)
            if petticoat and r == rows - 1:
                # Lace peeking under the hem: a thin layer behind the panel (inside the skirt, so none of it shows through
                # the cloth where the pleats fold), its top tucked up behind it (its corners and the panel's apart).
                m.cube(bone, [round(mx - width / 2, 3), round(top - seg - 0.5, 3), round(mz + thick + 0.03, 3)],
                       [round(width, 3), 0.9, 0.15], petticoat)
            ring_links = [f"{name}_{(i - 1) % pleats}_{r}", f"{name}_{(i + 1) % pleats}_{r}"]
            spring = dict({"fabric": fabric, "stiffness": round(80 - r * 50 / max(1, rows - 1), 1),
                           "max_angle": round(30 + r * 22 / max(1, rows - 1), 1), "length": round(seg, 3),
                           "colliders": balls, "links": ring_links}, **(tune or {}))
            springs[bone["name"]] = spring
    _sheet(m, [[f"{n}_{r}" for n in names] for r in range(rows)], wrap=True)
    return springs


def balloon(m, name, parent, x, hand, length, size, colour, lean, segments=4, locators=None):
    """A balloon on a string, the string as cloth: `segments` lengths of string from the fist (`{name}_string_0`, ...)
    each a joint under the last, the balloon (and its knot) on the last, named `name`; one column of a cloth sheet, so
    the string curves smoothly as each length swings. `lean`: how the first length leans at rest. Returns the joint
    names, from the fist up."""
    seg = length / segments
    names = []
    for i in range(segments):
        last = i == segments - 1
        pivot = [x, round(hand + i * seg, 3), 0]
        bone = m.bone(name if last else f"{name}_string_{i}", pivot, parent if i == 0 else names[-1],
                      rotation=lean if i == 0 else None, locators=locators if last else None)
        m.cube(bone, [x - 0.1, pivot[1], -0.1], [0.2, round(seg, 3), 0.2], "string")
        names.append(bone["name"])
    top = hand + length
    m.cube(bone, [x - size / 2, top, -size / 2], [size, size * 1.15, size], colour)
    m.cube(bone, [x - 0.3, top - 0.4, -0.3], [0.6, 0.6, 0.6], colour)                                # its knot
    _sheet(m, [[n] for n in names])
    return names


def balloon_springs(names, length, size, lift, damping, wind, twist, string_colliders, colliders, collide=None, spacing=1.5):
    """The springs of a `balloon()`: the balloon floats free (no stiffness, `lift` its buoyancy, a rope that goes
    slack, wind and twist), its weight at the balloon's middle, bumping the others; each length of string below it a
    cord, lifted a little less than the balloon. Every length `pulls` (CoolCatLib): stretched taut, it tugs the length
    below it toward it, so the balloon draws its string taut toward itself and a jerk of the fist runs up the string as
    a curve, rather than the string standing stiff and the balloon folding over at its top. Balloons lift gently (low
    gravity: a slow, floaty rise) and keep `spacing` pixels of room from each other (CoolCatLib's bump spacing eases
    them apart before they touch), so a bunch opens out into a spread bouquet instead of crowding into a clump."""
    seg = length / len(names)
    out = {}
    for i, n in enumerate(names[:-1]):
        out[n] = {"fabric": "cord", "stiffness": 0, "buoyancy": round(lift * (0.7 + 0.05 * i), 2), "rope": True, "pulls": True,
                  "damping": round(damping + 1.2, 2), "max_angle": 180, "length": round(seg, 3), "colliders": string_colliders}
    out[names[-1]] = {"stiffness": 0, "buoyancy": lift, "damping": damping, "rope": True, "pulls": True, "wind": wind, "twist": twist, "max_angle": 180,
                      "length": round(seg + size * 0.57, 3), "bump": True, "spacing": spacing,
                      "collide": collide or {"blocks": True, "entities": True, "radius": round(size * 0.92, 2), "bounce": 0.45, "friction": 0.2},
                      "colliders": colliders}
    return out


def cape(m, parent, x0, x1, z, top, length, material, name="cape", rows=3, cols=2, thick=0.5, colliders=(), wind=0.0,
         lengths=None, woven=True, stiffness=60.0, fabric="wool", tune=None):
    """A cape (or a veil, or rags) as cloth: `cols` chains side by side, each of `rows` lengths, each length a spring;
    side by side they are woven together (CoolCatLib's spring links) so it moves as one sheet, rippling across as well
    as down; unwoven (`woven=False`) and of their own `lengths`, they are rags that swing apart. Stiff at the shoulders,
    looser and heavier toward the hem, so it trails and folds; skinned as a sheet (`_sheet`; each rag its own), so it
    bends between its joints instead of hinging in slabs. Returns the springs."""
    springs = {}
    width = (x1 - x0) / cols
    for c in range(cols):
        total = lengths[c] if lengths else length
        seg = total / rows
        cx0 = x0 + c * width
        under, y = parent, top
        for r in range(rows):
            bone = m.bone(f"{name}_{c}_{r}", [round(cx0 + width / 2, 3), round(y, 3), z], under)
            m.cube(bone, [cx0, y - seg, z], [width, seg, thick], material)                         # meeting the next (skinned to it)
            spring = dict({"fabric": fabric, "stiffness": round(stiffness - r * stiffness * 0.6 / max(1, rows - 1), 1),
                           "max_angle": round(40 + r * 30 / max(1, rows - 1), 1),
                           "length": round(seg, 3), "colliders": list(colliders)}, **(tune or {}))
            if woven and cols > 1:
                spring["links"] = [f"{name}_{k}_{r}" for k in (c - 1, c + 1) if 0 <= k < cols]
            if wind:
                spring["wind"] = wind
            springs[bone["name"]] = spring
            under, y = bone["name"], y - seg
    if woven:
        _sheet(m, [[f"{name}_{c}_{r}" for c in range(cols)] for r in range(rows)])
    else:
        for c in range(cols):
            _sheet(m, [[f"{name}_{c}_{r}"] for r in range(rows)])
    return springs


def strip(m, parent, x, top, z, length, material, name, width=1.0, thick=0.4, rows=2, wind=0.0, fabric="rags"):
    """A torn strip of cloth hanging from somewhere (a sleeve, a hem): a little chain of springs. Returns them."""
    return cape(m, parent, x - width / 2, x + width / 2, z, top, length, material, name=name, rows=rows, cols=1, thick=thick,
                wind=wind, stiffness=50.0, fabric=fabric)


def mourner():
    m = Model()
    m.bone("root", [0, 0, 0])
    legs(m, "root", (-2, 2), 12, "skin", "cloth", thick=3.6)
    body = m.bone("body", [0, 12, 0], "root")
    m.cube(body, [-4, 12, -2], [8, 11, 4], "cloth")
    cloth = skirt(m, "body", 13, 2, 4.5, 2.6, "cloth", name="shroud", rows=2, torn=True, drag=True,
                  fabric="linen", tune={"mass": 1.1})                                                    # the shroud's skirt: grave linen, damp
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
        cloth.update(strip(m, side, x, 15, 1.4, 6, "cloth", side + "_strip"))                # a torn strip hanging from it
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
                  "springs": dict({"right_arm": {"stiffness": 200, "damping": 14, "weight": 0.3}}, **cloth)}


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
    # Its crown: four branches like antlers, each a joint leaning out from the middle (a positive z turn leans the top
    # toward +x), the back pair further out and back; on each a tine forking further out, and a thorn at the tip.
    for i, (x, z, lean, back, tall) in enumerate(((-2, -1.5, 26, -12, 5), (2, -1.5, 26, -12, 5), (-1.6, 1.4, 48, -26, 4.5), (1.6, 1.4, 48, -26, 4.5))):
        sx = 1 if x > 0 else -1
        branch = m.bone(f"branch_{i}", [x, 30, z], "head", rotation=[back, 0, lean * sx])
        m.cube(branch, [x - 0.5, 30, z - 0.5], [1, tall, 1], "root")
        m.cube(branch, [x - 0.4, 30 + tall * 0.55, z - 0.4], [0.8, 2.6, 0.8], "root", rotation=[0, 0, 38 * sx],
               pivot=[x, 30 + tall * 0.55, z])                                                   # a tine forking out
        m.cube(branch, [x - 0.35, 30 + tall, z - 0.35], [0.7, 1.6, 0.7], "thorn")              # a thorn at its tip
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
    """A Weeping Wisp: a porcelain face hung in the air, wailing, blood running from its hollow eyes and dripping from
    its chin, a tattered veil over its head and a ragged shroud trailing below it to tatters; two thin pale arms reach
    out of the shroud with long fingers; tears of blood circle it."""
    m = Model()
    m.bone("root", [0, 0, 0])
    core = m.bone("core", [0, 12, 0], "root")
    head = m.bone("head", [0, 15, 0], "core")
    m.cube(head, [-4, 14, -4], [8, 8, 7], "wail_face")
    m.cube(head, [-4.5, 21.2, -4.4], [9, 1.8, 8.4], "cloth", inflate=0.1)                       # its veil, over its head
    m.cube(head, [-4.5, 12, 1.2], [9, 10, 3.2], "cloth")                                        # and down its back
    for x in (-4.8, 4.1):
        m.cube(head, [x, 13, -3.4], [0.7, 8.5, 4.6], "cloth")                                   # its sides
    for x, length in ((-2.1, 6), (1.5, 7.5)):
        m.cube(head, [x, 15 - length, -4.25], [0.6, length, 0.3], "tear")                       # blood running from its eyes
        m.cube(head, [x - 0.1, 14.2 - length, -4.35], [0.8, 0.9, 0.5], "drop")
    shroud = m.bone("shroud", [0, 14, 0], "core")
    m.cube(shroud, [-3, 8, -2.5], [6, 6, 5], "cloth")
    m.cube(shroud, [-2.4, 4, -2], [4.8, 4, 4], "cloth")
    tatters = m.bone("tatters", [0, 4, 0], "shroud")                                          # swayed by its clips; the strips hang from it
    cloth = {}
    for i, (x, z, length) in enumerate(((-2.4, -1.6, 5), (-0.8, 0.8, 7), (0.8, -1.4, 6), (1.8, 1.0, 4.5), (-1.6, 1.4, 3.5))):
        cloth.update(strip(m, "tatters", x + 0.5, 4.1, z, length, "cloth", f"tatter_{i}", thick=0.5, wind=1.2, fabric="silk"))  # gossamer tatters, adrift
    for side, x in (("right_arm", -3.2), ("left_arm", 3.2)):
        arm = m.bone(side, [x, 13, -1.5], "shroud", rotation=[22, 0, 0])
        m.cube(arm, [x - 0.5, 12.5, -9.5], [1, 1, 8], "pale")                                   # thin arms, reaching
        for i, fx in enumerate((-0.4, 0.05, 0.5)):
            m.cube(arm, [x + fx - 0.2, 12.4 - i * 0.15, -12.4], [0.4, 0.4, 3], "pale")          # long fingers
    # Tears of blood circling it, wide: each its own joint (so each bobs on its own), a shell of blood with a lit glint
    # on it, a drip hanging under it and a tail of droplets streaming out behind it as it goes round.
    orbit = m.bone("orbit", [0, 17, 0], "core")
    for i, (deg, y) in enumerate(((0, 16.5), (90, 19), (180, 15.5), (270, 18))):
        a = math.radians(deg)
        x, z = round(math.cos(a) * 10, 2), round(math.sin(a) * 10, 2)
        orb = m.bone(f"orb_{i}", [x, y, z], "orbit")
        m.cube(orb, [x - 1, y - 1.1, z - 1], [2, 2.4, 2], "tear")                               # the tear
        m.cube(orb, [x - 0.65, y + 0.45, z - 1.15], [0.7, 0.7, 0.4], "drop")                    # a glint on it
        m.cube(orb, [x - 0.4, y - 2.2, z - 0.4], [0.8, 1.1, 0.8], "tear")                       # its drip
        m.cube(orb, [x - 0.2, y - 2.9, z - 0.2], [0.4, 0.6, 0.4], "drop")
        for k, (back, size) in enumerate(((0.16, 1.2), (0.29, 0.8), (0.4, 0.5))):
            tx, tz = round(math.cos(a + back) * 10, 2), round(math.sin(a + back) * 10, 2)    # behind it, the way it goes round
            m.cube(orb, [tx - size / 2, y - size / 2 + 0.2 * k, tz - size / 2], [size, size, size], "drop" if k else "tear")
    # Now and then its head jerks, sharply, as if at a sound.
    jerk = f"math.max(0, math.sin({T} * 75) - 0.93) * 220"
    a = {
        "idle": {"loop": True, "animation_length": 4.8, "bones": {
            "core": {"position": [0, f"1.2 * math.sin({T} * 75)", 0]},
            "head": {"rotation": [f"6 * math.sin({T} * 37.5)", f"{jerk}", f"10 * math.sin({T} * 75) - {jerk} * 0.4"]},
            "shroud": {"rotation": [f"6 * math.sin({T} * 75 + 90)", 0, f"4 * math.sin({T} * 37.5)"]},
            "tatters": {"rotation": [f"14 * math.sin({T} * 150 + 60)", 0, f"10 * math.sin({T} * 112.5)"]},
            "right_arm": {"rotation": [f"8 * math.sin({T} * 75)", f"6 * math.sin({T} * 37.5)", 0]},
            "left_arm": {"rotation": [f"8 * math.sin({T} * 75 + 120)", f"-6 * math.sin({T} * 37.5)", 0]},
            "orbit": {"rotation": [0, f"{T} * 75", 0]},
            **{f"orb_{i}": {"position": [0, f"0.9 * math.sin({T} * 150 + {i * 90})", 0]} for i in range(4)}}},
        # Coming for someone: leaning in, its arms out, its shroud streaming.
        "walk": {"loop": True, "animation_length": 1.2, "bones": {
            "core": {"position": [0, f"0.8 * math.sin({T} * 300)", 0], "rotation": [14, 0, 0]},
            "head": {"rotation": [-10, 0, f"6 * math.sin({T} * 300)"]},
            "shroud": {"rotation": [18, 0, 0]}, "tatters": {"rotation": [f"30 + 12 * math.sin({T} * 600)", 0, f"8 * math.sin({T} * 300)"]},
            "right_arm": {"rotation": [-12, 6, 0]}, "left_arm": {"rotation": [-12, -6, 0]},
            "orbit": {"rotation": [0, f"{T} * 300", 0]},
            **{f"orb_{i}": {"position": [0, f"0.6 * math.sin({T} * 600 + {i * 90})", 0]} for i in range(4)}}},
    }
    return m, a, {"springs": cloth, "look_at": {"head": {"max_yaw": 70, "max_pitch": 35, "smoothing": 0.3}}}


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


def sleepwalker():
    """A Sleepwalker: a gaunt dreamer in a dirty nightgown to its ankles, torn at the hem and sleeves, pale bare feet
    and forearms, long dark hair down its back, its head lolling; its arms out before it as it shuffles."""
    m = Model()
    m.bone("root", [0, 0, 0])
    legs(m, "root", (-1.8, 1.8), 12, "pale", "pale", thick=3.0)
    body = m.bone("body", [0, 12, 0], "root")
    m.cube(body, [-4, 12, -2.5], [8, 12, 5], "gown")
    cloth = skirt(m, "body", 12.5, 2.5, 4.6, 3.0, "gown", name="nightgown", rows=2, torn=True, fabric="linen")   # a linen nightgown, torn at the hem
    m.cube(body, [-2, 21.5, -2.7], [4, 2.5, 0.4], "gown", inflate=0.05)                   # its collar
    head = m.bone("head", [0, 24, 0], "body")
    m.cube(head, [-3.5, 24, -3.5], [7, 7, 7], "sleeper_face")
    m.cube(head, [-3.8, 30.5, -3.8], [7.6, 1, 7.6], "hair")                               # its hair, long and dark
    m.cube(head, [-3.8, 19, 0.5], [7.6, 12, 3.6], "hair")
    m.cube(head, [-4.1, 22.5, -3.6], [0.7, 8, 5], "hair")
    m.cube(head, [3.4, 22.5, -3.6], [0.7, 8, 5], "hair")
    for side, x in (("right_arm", -5.5), ("left_arm", 5.5)):
        arm = m.bone(side, [x, 23, 0], "body")
        m.cube(arm, [x - 1.5, 16, -1.5], [3, 7, 3], "gown")                                # the sleeve
        cloth.update(strip(m, side, x, 16.5, 1.3, 4, "gown", side + "_strip"))              # a torn strip from it
        fore = m.bone(side.replace("arm", "forearm"), [x, 16, 0], side)
        m.cube(fore, [x - 1.1, 10, -1.1], [2.2, 6, 2.2], "pale")
        for i, fx in enumerate((-0.8, 0, 0.8)):
            m.cube(fore, [x + fx - 0.3, 7.5, -0.6 + i * 0.2], [0.6, 2.5, 0.6], "pale")    # long fingers
    a = {
        "idle": {"loop": True, "animation_length": 4.0, "bones": {
            "right_arm": {"rotation": [f"-80 + 3 * math.sin({T} * 90)", 4, 0]}, "left_arm": {"rotation": [f"-78 + 3 * math.sin({T} * 90 + 90)", -4, 0]},
            "right_forearm": {"rotation": [-6, 0, 0]}, "left_forearm": {"rotation": [-8, 0, 0]},
            "head": {"rotation": [f"14 + 4 * math.sin({T} * 45)", 0, f"16 * math.sin({T} * 45)"]},     # lolling in its sleep
            "body": {"rotation": [6, 0, f"3 * math.sin({T} * 45)"]}}},
        "walk": {"loop": True, "animation_length": 1.6, "bones": dict(humanoid_walk(arms_forward=80, swing=5, stride=20),
                                                                       body={"rotation": [8, 0, f"5 * math.sin({T} * 225)"]},
                                                                       head={"rotation": [12, 0, f"12 * math.sin({T} * 225)"]})},
        "attack": {"loop": False, "animation_length": 0.4, "bones": {
            "right_arm": {"rotation": frames((0.0, [-80, 0, 0]), (0.12, [-130, 0, 0]), (0.25, [-45, 0, 0]), (0.4, [-80, 0, 0]))},
            "left_arm": {"rotation": frames((0.0, [-78, 0, 0]), (0.14, [-128, 0, 0]), (0.27, [-48, 0, 0]), (0.4, [-78, 0, 0]))},
            "head": {"rotation": frames((0.0, [12, 0, 0]), (0.12, [-10, 0, 0]), (0.4, [12, 0, 0]))}}},
    }
    return m, a, {"look_at": {"head": {"max_yaw": 40, "max_pitch": 25, "smoothing": 0.2}},
                  "springs": dict({"right_arm": {"stiffness": 180, "damping": 14, "weight": 0.3}}, **cloth)}


def dread_manta():
    """A Dread Manta: a wide flat ray of shadow, its wings in two parts each with faint violet veins of light, two
    forward fins over a head with violet eyes at its corners, a long jointed tail ending in a glowing barb."""
    m = Model()
    m.bone("root", [0, 0, 0])
    body = m.bone("body", [0, 6, 0], "root")
    m.cube(body, [-4, 4.5, -8], [8, 3, 16], "manta")
    m.cube(body, [-3, 5, -11], [6, 2, 3], "manta_face")                                    # its head
    m.cube(body, [-3.2, 5.2, -13.5], [1.2, 1.2, 2.5], "manta")                             # its forward fins
    m.cube(body, [2, 5.2, -13.5], [1.2, 1.2, 2.5], "manta")
    for x, z in ((-2, -4), (1, -4), (-2, 2), (1, 2)):
        m.cube(body, [x, 4.4, z], [1, 0.2, 1], "violet")                                   # lights on its underside
    for side, sx in (("right_wing", -1), ("left_wing", 1)):
        wing = m.bone(side, [4 * sx, 6, 0], "body")
        x0 = 4 if sx > 0 else -14
        m.cube(wing, [x0, 5.5, -6], [10, 1, 12], "membrane")
        m.cube(wing, [x0 + (2 if sx > 0 else 0), 6.5, -0.4], [8, 0.2, 0.8], "violet")      # a vein of light along it
        tip = m.bone(side.replace("wing", "wingtip"), [14 * sx, 6, 0], side)
        tx0 = 14 if sx > 0 else -20
        m.cube(tip, [tx0, 5.6, -4], [6, 0.8, 8], "membrane")
        m.cube(tip, [tx0 + (0 if sx > 0 else 1), 6.4, -0.3], [5, 0.2, 0.6], "violet")
    tail = m.bone("tail", [0, 6, 8], "body")
    m.cube(tail, [-0.75, 5.25, 8], [1.5, 1.2, 10], "manta")
    tail2 = m.bone("tail_end", [0, 6, 18], "tail")
    m.cube(tail2, [-0.5, 5.4, 18], [1, 0.9, 8], "manta")
    m.cube(tail2, [-0.8, 5.3, 25.5], [1.6, 1.1, 1.6], "violet")                            # its barb
    flap = f"18 * math.sin({T} * 120)"
    tipflap = f"14 * math.sin({T} * 120 - 50)"
    glide = {
        "body": {"position": [0, f"0.8 * math.sin({T} * 120 + 90)", 0]},
        "right_wing": {"rotation": [0, 0, flap]}, "left_wing": {"rotation": [0, 0, f"-({flap})"]},
        "right_wingtip": {"rotation": [0, 0, tipflap]}, "left_wingtip": {"rotation": [0, 0, f"-({tipflap})"]},
        "tail": {"rotation": [f"6 * math.sin({T} * 120)", f"12 * math.sin({T} * 120 - 90)", 0]},
        "tail_end": {"rotation": [0, f"18 * math.sin({T} * 120 - 150)", 0]}}
    a = {"idle": {"loop": True, "animation_length": 3.0, "bones": glide},
         "walk": {"loop": True, "animation_length": 1.5, "bones": glide}}
    return m, a, {}


def dream_eater():
    """A Dream Eater (after the baku): a gaunt beast on four long legs, its hide the night sky with stars in it, a hump
    of drifting violet wisps down its back, small pale eyes under heavy lids, flat ears, two short tusks, and a long
    jointed trunk ending in a glowing tip it draws dreams out with."""
    m = Model()
    m.bone("root", [0, 0, 0])
    body = m.bone("body", [0, 16, 0], "root")
    m.cube(body, [-5, 14, -9], [10, 9, 18], "hide")
    m.cube(body, [-4, 22, -6], [8, 2.5, 10], "hide", inflate=0.1)                             # the hump of its back
    for z in (-5, -1.5, 2, 5.5):
        m.cube(body, [-0.6, 24.2, z], [1.2, 1.4, 1.2], "violet")                            # dream-wisps along its spine
    head = m.bone("head", [0, 20, -9], "body")
    m.cube(head, [-3.5, 15.5, -16], [7, 7, 7], "eater_face")
    for x in (-5.2, 3.6):
        m.cube(head, [x, 19, -12], [1.6, 3.5, 2.5], "hide", rotation=[0, 0, 25 if x < 0 else -25])   # flat ears
    for x in (-3, 2.2):
        m.cube(head, [x, 13.5, -16.4], [0.8, 2.5, 0.8], "bone")                             # short tusks
    t0 = m.bone("trunk_0", [0, 16.5, -16], "head")
    m.cube(t0, [-1.5, 12, -17.5], [3, 5, 3], "hide")
    t1 = m.bone("trunk_1", [0, 12, -16], "trunk_0")
    m.cube(t1, [-1.2, 7, -17.2], [2.4, 5, 2.4], "hide")
    t2 = m.bone("trunk_2", [0, 7, -16], "trunk_1")
    m.cube(t2, [-0.9, 3.5, -16.9], [1.8, 3.5, 1.8], "hide")
    m.cube(t2, [-1.1, 2.4, -17.1], [2.2, 1.2, 2.2], "violet")                               # its tip, glowing
    for name, x, z in (("front_right_leg", -3.5, -6), ("front_left_leg", 3.5, -6), ("back_right_leg", -3.5, 6), ("back_left_leg", 3.5, 6)):
        leg = m.bone(name, [x, 14, z], "body")
        m.cube(leg, [x - 1.5, 7, z - 1.5], [3, 7.5, 3], "hide")
        shin = m.bone(name.replace("leg", "shin"), [x, 7, z], name)
        m.cube(shin, [x - 1.2, 1, z - 1.2], [2.4, 6, 2.4], "hide")
        m.cube(shin, [x - 1.5, 0, z - 1.5], [3, 1.5, 3], "bone")                            # hooves
    tail = m.bone("tail", [0, 21, 9], "body")
    m.cube(tail, [-0.5, 14, 9], [1, 7, 1], "hide")
    m.cube(tail, [-0.8, 12.5, 8.7], [1.6, 2, 1.6], "violet")
    step = f"{T} * 360"
    walk_legs = {
        "front_right_leg": {"rotation": [f"24 * math.sin({step})", 0, 0]}, "back_left_leg": {"rotation": [f"24 * math.sin({step})", 0, 0]},
        "front_left_leg": {"rotation": [f"-24 * math.sin({step})", 0, 0]}, "back_right_leg": {"rotation": [f"-24 * math.sin({step})", 0, 0]},
        "front_right_shin": {"rotation": [f"math.max(0, -30 * math.sin({step} + 90))", 0, 0]},
        "back_left_shin": {"rotation": [f"math.max(0, -30 * math.sin({step} + 90))", 0, 0]},
        "front_left_shin": {"rotation": [f"math.max(0, 30 * math.sin({step} + 90))", 0, 0]},
        "back_right_shin": {"rotation": [f"math.max(0, 30 * math.sin({step} + 90))", 0, 0]}}
    a = {
        "idle": {"loop": True, "animation_length": 4.0, "bones": {
            "body": {"position": [0, f"0.3 * math.sin({T} * 90)", 0]},
            "head": {"rotation": [f"8 + 4 * math.sin({T} * 90)", f"10 * math.sin({T} * 45)", 0]},
            "trunk_0": {"rotation": [f"6 * math.sin({T} * 90)", 0, f"8 * math.sin({T} * 90 + 40)"]},
            "trunk_1": {"rotation": [f"-8 + 8 * math.sin({T} * 90 - 40)", 0, f"10 * math.sin({T} * 90)"]},
            "trunk_2": {"rotation": [f"-14 + 10 * math.sin({T} * 90 - 80)", 0, f"12 * math.sin({T} * 90 - 40)"]},
            "tail": {"rotation": [10, 0, f"10 * math.sin({T} * 90)"]}}},
        "walk": {"loop": True, "animation_length": 1.4, "bones": dict(walk_legs,
            body={"position": [0, f"0.5 * math.abs(math.sin({step}))", 0], "rotation": [0, 0, f"2 * math.sin({step})"]},
            head={"rotation": [f"10 + 3 * math.sin({step} * 2)", 0, 0]},
            trunk_0={"rotation": [f"-8 + 8 * math.sin({step})", 0, f"6 * math.sin({step} + 90)"]},
            trunk_1={"rotation": [f"-10 + 8 * math.sin({step} - 50)", 0, 0]},
            trunk_2={"rotation": [f"-16 + 10 * math.sin({step} - 100)", 0, 0]},
            tail={"rotation": [12, 0, f"14 * math.sin({step})"]})},
        # It reaches with its trunk, curling it up and back, and whips it down across what is before it.
        "attack": {"loop": False, "animation_length": 0.6, "bones": {
            "head": {"rotation": frames((0.0, [8, 0, 0]), (0.2, [-12, 0, 0]), (0.35, [22, 0, 0]), (0.6, [8, 0, 0]))},
            "trunk_0": {"rotation": frames((0.0, [0, 0, 0]), (0.2, [-70, 0, 0]), (0.35, [20, 0, 0]), (0.6, [0, 0, 0]))},
            "trunk_1": {"rotation": frames((0.0, [-8, 0, 0]), (0.2, [-50, 0, 0]), (0.35, [10, 0, 0]), (0.6, [-8, 0, 0]))},
            "trunk_2": {"rotation": frames((0.0, [-14, 0, 0]), (0.2, [-40, 0, 0]), (0.35, [0, 0, 0]), (0.6, [-14, 0, 0]))}}},
    }
    return m, a, {"look_at": {"head": {"max_yaw": 35, "max_pitch": 20, "smoothing": 0.12}},
                  "springs": {"trunk_2": {"stiffness": 120, "damping": 10, "weight": 0.4}}}


def somnimoth():
    """A Somnimoth: a moth of the Dark Between wider across its wings than a bat, its furry body grey-violet with dust, two broad
    forewings and smaller hindwings banded and each with a glowing eyespot, feathery antennae, and two great pale eyes."""
    m = Model()
    m.bone("root", [0, 0, 0])
    body = m.bone("body", [0, 6, 0], "root")
    m.cube(body, [-1.5, 4.5, -2], [3, 3, 3], "dust", inflate=0.1)
    abdomen = m.bone("abdomen", [0, 5.5, 1], "body")
    m.cube(abdomen, [-1.2, 3.8, 1], [2.4, 2.4, 4.5], "dust")
    head = m.bone("head", [0, 6, -2], "body")
    m.cube(head, [-1.3, 4.8, -4], [2.6, 2.4, 2], "moth_face")
    for x, ry in ((-1, 25), (0.6, -25)):
        m.cube(head, [x, 7.1, -6.5], [0.4, 0.3, 3.2], "dust", rotation=[-30, ry, 0], pivot=[x + 0.2, 7, -4])   # feathery antennae
    for side, sx in (("right", -1), ("left", 1)):
        fore = m.bone(f"{side}_wing", [1.5 * sx, 7, -0.5], "body")
        m.cube(fore, [1.5 if sx > 0 else -9.5, 6.9, -4], [8, 0.3, 7], "wing")
        hind = m.bone(f"{side}_hindwing", [1.5 * sx, 6.6, 1.5], "body")
        m.cube(hind, [1.5 if sx > 0 else -6.5, 6.5, 1], [5, 0.3, 4.5], "wing")
    flap = f"55 * math.sin({T} * 1440)"
    hind = f"45 * math.sin({T} * 1440 - 40)"
    fly = {
        "body": {"position": [0, f"0.6 * math.sin({T} * 720)", 0], "rotation": [f"-10 + 4 * math.sin({T} * 720)", 0, 0]},
        "right_wing": {"rotation": [0, 0, flap]}, "left_wing": {"rotation": [0, 0, f"-({flap})"]},
        "right_hindwing": {"rotation": [0, 0, hind]}, "left_hindwing": {"rotation": [0, 0, f"-({hind})"]},
        "abdomen": {"rotation": [f"8 * math.sin({T} * 720 + 90)", 0, 0]}}
    a = {"idle": {"loop": True, "animation_length": 0.5, "bones": fly},
         "walk": {"loop": True, "animation_length": 0.5, "bones": fly},
         "attack": {"loop": False, "animation_length": 0.3, "bones": {
             "body": {"rotation": frames((0.0, [-10, 0, 0]), (0.12, [25, 0, 0]), (0.3, [-10, 0, 0]))},
             "head": {"rotation": frames((0.0, [0, 0, 0]), (0.12, [20, 0, 0]), (0.3, [0, 0, 0]))}}}}
    return m, a, {}


def faceless():
    """The Faceless: too tall and too thin, in a black suit, its arms hanging to its knees, its hands long and pale,
    and where its face should be, smooth skin. It moves only while no one is looking at it."""
    m = Model()
    m.bone("root", [0, 0, 0])
    legs(m, "root", (-1.6, 1.6), 18, "suit", "suit", thick=2.6)
    body = m.bone("body", [0, 18, 0], "root")
    m.cube(body, [-3.5, 18, -2], [7, 12, 4], "suit")
    m.cube(body, [-1, 25, -2.2], [2, 5, 0.4], "faceless")                                   # its shirt, at the collar
    m.cube(body, [-0.4, 23, -2.4], [0.8, 6.4, 0.4], "suit")                                  # a black tie
    m.cube(body, [-1, 30, -1], [2, 2, 2], "faceless")                                        # its neck, long
    head = m.bone("head", [0, 32, 0], "body")
    m.cube(head, [-3, 32, -3], [6, 8.5, 6], "faceless_face")
    for side, x in (("right_arm", -4.5), ("left_arm", 4.5)):
        arm = m.bone(side, [x, 29.5, 0], "body")
        m.cube(arm, [x - 1, 20, -1], [2, 10, 2], "suit")
        fore = m.bone(side.replace("arm", "forearm"), [x, 20, 0], side)
        m.cube(fore, [x - 0.9, 18.5, -0.9], [1.8, 1.5, 1.8], "faceless")                   # its cuff, a shirt's
        m.cube(fore, [x - 0.8, 9, -0.8], [1.6, 9.5, 1.6], "faceless")
        for i, fx in enumerate((-0.55, 0, 0.55)):
            m.cube(fore, [x + fx - 0.25, 4, -0.45 + i * 0.2], [0.5, 5, 0.5], "faceless")   # long fingers
    a = {
        # Standing, it is utterly still, its head tipped a little to one side.
        "idle": {"loop": True, "animation_length": 4.0, "bones": {
            "head": {"rotation": [6, 0, 14]}, "right_arm": {"rotation": [0, 0, 3]}, "left_arm": {"rotation": [0, 0, -3]}}},
        # It walks stiffly, its arms not swinging at all, its head never moving: the more wrong for being so even.
        "walk": {"loop": True, "animation_length": 0.9, "bones": {
            "right_leg": {"rotation": [f"30 * math.sin({T} * 400)", 0, 0]}, "left_leg": {"rotation": [f"-30 * math.sin({T} * 400)", 0, 0]},
            "right_shin": {"rotation": [f"math.max(0, 35 * math.sin({T} * 400 + 90))", 0, 0]},
            "left_shin": {"rotation": [f"math.max(0, -35 * math.sin({T} * 400 + 90))", 0, 0]},
            "head": {"rotation": [6, 0, 14]}, "body": {"rotation": [4, 0, 0]}}},
        # Its arms come up at once and close on what is before it.
        "attack": {"loop": False, "animation_length": 0.4, "bones": {
            "right_arm": {"rotation": frames((0.0, [0, 0, 0]), (0.1, [-110, 0, -15]), (0.25, [-70, 0, 10]), (0.4, [0, 0, 0]))},
            "left_arm": {"rotation": frames((0.0, [0, 0, 0]), (0.1, [-110, 0, 15]), (0.25, [-70, 0, -10]), (0.4, [0, 0, 0]))},
            "head": {"rotation": frames((0.0, [6, 0, 14]), (0.1, [20, 0, 0]), (0.4, [6, 0, 14]))}}},
    }
    return m, a, {}


def quad_legs(m, parent, legs, top, material, hoof, thick=3.0, shin=True):
    """Four legs in two parts each (upper and shin), from `top` down to the ground."""
    for name, x, z in legs:
        leg = m.bone(name, [x, top, z], parent)
        mid = top / 2
        m.cube(leg, [x - thick / 2, mid, z - thick / 2], [thick, top - mid + 0.5, thick], material)
        sh = m.bone(name.replace("leg", "shin"), [x, mid, z], name)
        m.cube(sh, [x - thick / 2 + 0.3, 1, z - thick / 2 + 0.3], [thick - 0.6, mid - 0.5, thick - 0.6], material)
        m.cube(sh, [x - thick / 2, 0, z - thick / 2], [thick, 1.5, thick], hoof)


def quad_walk(step, swing, bend):
    """A trot: the legs of each diagonal pair together, each shin folding as its foot comes forward."""
    out = {}
    for leg, sign in (("front_right", 1), ("back_left", 1), ("front_left", -1), ("back_right", -1)):
        out[f"{leg}_leg"] = {"rotation": [f"{sign * swing} * math.sin({step})", 0, 0]}
        out[f"{leg}_shin"] = {"rotation": [f"math.max(0, {-sign * bend} * math.sin({step} + 90))", 0, 0]}
    return out


FOUR = (("front_right_leg", -1, -1), ("front_left_leg", 1, -1), ("back_right_leg", -1, 1), ("back_left_leg", 1, 1))


def night_mare():
    """A Night Mare: a horse of shadow, gaunt to the ribs, its mane and tail and hooves violet flame, its nostrils
    smouldering. It paws the ground and charges."""
    m = Model()
    m.bone("root", [0, 0, 0])
    body = m.bone("body", [0, 15, 0], "root")
    m.cube(body, [-4, 12.5, -10], [8, 8, 20], "nightmare")
    for z in (-6, -3, 0):
        m.cube(body, [-4.2, 13.5, z], [8.4, 5, 1], "nightmare", inflate=0.1)                   # ribs standing out
    neck = m.bone("neck", [0, 19, -9], "body", rotation=[30, 0, 0])
    m.cube(neck, [-2, 18, -12], [4, 10, 4.5], "nightmare")
    m.cube(neck, [-0.75, 20, -8], [1.5, 10, 2], "flame")                                       # its mane
    head = m.bone("head", [0, 27, -10], "neck", rotation=[-30, 0, 0])
    m.cube(head, [-2.5, 24, -18], [5, 5, 8], "mare_face")
    m.cube(head, [-0.75, 29, -13], [1.5, 1.2, 3], "flame")
    for x in (-2, 1):
        m.cube(head, [x, 29, -11.5], [1, 2.5, 1], "nightmare")                                 # ears
    for x in (-2.8, 2.4):
        m.cube(head, [x, 26.5, -15], [0.4, 1, 1], "flame")                                     # its eyes, burning
    quad_legs(m, "body", [(n, 2.6 * sx, 7.5 * sz) for n, sx, sz in FOUR], 13, "nightmare", "flame", thick=3.0)
    tail = m.bone("tail", [0, 19.5, 10], "body", rotation=[25, 0, 0])
    m.cube(tail, [-1, 9, 9.5], [2, 11, 2.5], "flame")
    step = f"{T} * 360"
    a = {
        "idle": {"loop": True, "animation_length": 3.0, "bones": {
            "body": {"position": [0, f"0.3 * math.sin({T} * 120)", 0]},
            "neck": {"rotation": [f"4 * math.sin({T} * 120)", 0, 0]},
            "head": {"rotation": [f"6 * math.sin({T} * 120 + 60)", f"8 * math.sin({T} * 60)", 0]},
            "tail": {"rotation": [f"6 * math.sin({T} * 240)", 0, f"10 * math.sin({T} * 120)"]}}},
        "walk": {"loop": True, "animation_length": 1.0, "bones": dict(quad_walk(step, 26, 40),
            body={"position": [0, f"0.6 * math.abs(math.sin({step}))", 0]},
            neck={"rotation": [f"6 * math.sin({step} * 2)", 0, 0]},
            tail={"rotation": [f"10 * math.sin({step} * 2)", 0, f"8 * math.sin({step})"]})},
        # Pawing the ground before it charges, its head low.
        "windup": {"loop": True, "animation_length": 0.6, "bones": {
            "front_right_leg": {"rotation": [f"-40 * math.max(0, math.sin({T} * 600))", 0, 0]},
            "front_right_shin": {"rotation": [f"50 * math.max(0, math.sin({T} * 600))", 0, 0]},
            "neck": {"rotation": [25, 0, 0]}, "head": {"rotation": [10, 0, 0]},
            "body": {"rotation": [-4, 0, 0]}, "tail": {"rotation": [-20, 0, f"20 * math.sin({T} * 1200)"]}}},
        # The gallop: legs flung out, head down, tail streaming.
        "charge": {"loop": True, "animation_length": 0.4, "bones": dict(quad_walk(f"{T} * 900", 48, 70),
            body={"position": [0, f"1.0 * math.abs(math.sin({T} * 900))", 0], "rotation": [f"3 * math.sin({T} * 900)", 0, 0]},
            neck={"rotation": [35, 0, 0]}, head={"rotation": [5, 0, 0]}, tail={"rotation": [-50, 0, 0]})},
        # It rears and brings its forehooves down.
        "attack": {"loop": False, "animation_length": 0.6, "bones": {
            "body": {"rotation": frames((0.0, [0, 0, 0]), (0.25, [-28, 0, 0]), (0.45, [6, 0, 0]), (0.6, [0, 0, 0]))},
            "front_right_leg": {"rotation": frames((0.0, [0, 0, 0]), (0.25, [-60, 0, 0]), (0.45, [10, 0, 0]), (0.6, [0, 0, 0]))},
            "front_left_leg": {"rotation": frames((0.0, [0, 0, 0]), (0.25, [-50, 0, 0]), (0.45, [10, 0, 0]), (0.6, [0, 0, 0]))},
            "neck": {"rotation": frames((0.0, [0, 0, 0]), (0.25, [-15, 0, 0]), (0.45, [20, 0, 0]), (0.6, [0, 0, 0]))}}},
    }
    return m, a, {"look_at": {"head": {"max_yaw": 30, "max_pitch": 20, "smoothing": 0.15}},
                  "springs": {"tail": {"stiffness": 90, "damping": 8, "weight": 0.5}}}


def hollow_hound():
    """A Hollow Hound: a wolf of smoke with its ribs showing through, pale cold eyes, bone teeth, a smoking tail."""
    m = Model()
    m.bone("root", [0, 0, 0])
    body = m.bone("body", [0, 10, 0], "root")
    m.cube(body, [-3, 7.5, -6], [6, 6, 11], "smoke")
    m.cube(body, [-3.5, 9, -6.5], [7, 5, 5], "smoke", inflate=0.2)                              # its ruff
    for z in (-2, 0, 2):
        for x in (-3.2, 2.8):
            m.cube(body, [x, 8.5, z], [0.4, 4, 1], "bone")                                     # ribs through the smoke
    head = m.bone("head", [0, 12, -6], "body")
    m.cube(head, [-3, 9.5, -11], [6, 5.5, 5], "hound_face")
    m.cube(head, [-1.5, 9.5, -14], [3, 2.5, 3], "smoke")                                       # its snout
    for x in (-2.5, 1):
        m.cube(head, [x, 15, -8.5], [1.5, 2.5, 1], "smoke")                                    # ears
    jaw = m.bone("jaw", [0, 10, -11], "head")
    m.cube(jaw, [-1.5, 8.6, -14], [3, 1, 3], "bone")
    for name, x, z in (("front_right_leg", -1.8, -4), ("front_left_leg", 1.8, -4), ("back_right_leg", -1.8, 3.5), ("back_left_leg", 1.8, 3.5)):
        leg = m.bone(name, [x, 8, z], "body")
        m.cube(leg, [x - 1, 0, z - 1], [2, 8, 2], "smoke")
    tail = m.bone("tail", [0, 13, 5], "body", rotation=[-35, 0, 0])
    m.cube(tail, [-0.75, 12.2, 5], [1.5, 1.5, 7], "smoke")
    run = f"{T} * 600"
    legs = {"front_right_leg": {"rotation": [f"35 * math.sin({run})", 0, 0]}, "back_left_leg": {"rotation": [f"35 * math.sin({run})", 0, 0]},
            "front_left_leg": {"rotation": [f"-35 * math.sin({run})", 0, 0]}, "back_right_leg": {"rotation": [f"-35 * math.sin({run})", 0, 0]}}
    a = {
        "idle": {"loop": True, "animation_length": 2.0, "bones": {
            "body": {"position": [0, f"0.25 * math.sin({T} * 180)", 0]},
            "head": {"rotation": [f"4 * math.sin({T} * 180)", f"12 * math.sin({T} * 60)", 0]},
            "jaw": {"rotation": [f"6 + 4 * math.sin({T} * 360)", 0, 0]},
            "tail": {"rotation": [0, f"12 * math.sin({T} * 180)", 0]}}},
        "walk": {"loop": True, "animation_length": 0.6, "bones": dict(legs,
            body={"position": [0, f"0.5 * math.abs(math.sin({run}))", 0]},
            head={"rotation": [f"4 * math.sin({run} * 2)", 0, 0]}, jaw={"rotation": [14, 0, 0]},
            tail={"rotation": [20, f"15 * math.sin({run})", 0]})},
        "attack": {"loop": False, "animation_length": 0.4, "bones": {
            "head": {"rotation": frames((0.0, [0, 0, 0]), (0.12, [-20, 0, 0]), (0.24, [15, 0, 0]), (0.4, [0, 0, 0]))},
            "jaw": {"rotation": frames((0.0, [6, 0, 0]), (0.12, [40, 0, 0]), (0.24, [0, 0, 0]), (0.4, [6, 0, 0]))}}},
    }
    return m, a, {"look_at": {"head": {"max_yaw": 45, "max_pitch": 25, "smoothing": 0.2}}}


def lullaby():
    """A Lullaby: a porcelain doll the height of a child, floating, in a faded lace dress gone to tatters, long dark
    hair, a red tear under each glass eye; it holds a little brass music box and turns its crank as it sings."""
    m = Model()
    m.bone("root", [0, 0, 0])
    body = m.bone("body", [0, 10, 0], "root")
    m.cube(body, [-3, 14, -2], [6, 8, 4], "lace")
    m.cube(body, [-4.5, 5, -3], [9, 9.5, 6], "lace")                                           # its skirt
    for x in (-4, -1, 2.5):
        m.cube(body, [x, 2.5, -2.6 + (x % 2)], [1.5, 2.5, 0.5], "lace")                        # torn hem
    m.cube(body, [-3.3, 20.5, -2.3], [6.6, 1.5, 4.6], "porcelain")                              # its collar
    head = m.bone("head", [0, 22, 0], "body")
    m.cube(head, [-3.5, 22, -3.5], [7, 7, 7], "doll_face")
    m.cube(head, [-3.8, 28.5, -3.8], [7.6, 1.2, 7.6], "hair")
    m.cube(head, [-3.8, 17, 0.5], [7.6, 12, 3.6], "hair")
    m.cube(head, [-0.8, 29.2, -1], [1.6, 1, 2], "yarn")                                         # a ribbon in its hair
    box = m.bone("music_box", [0, 15, -4.5], "body")
    m.cube(box, [-2, 13.5, -6.5], [4, 3, 3], "brass")
    crank = m.bone("crank", [2, 15, -5], "music_box")
    m.cube(crank, [2, 14.8, -5.2], [1.4, 0.4, 0.4], "brass")
    m.cube(crank, [3, 14.8, -5.6], [0.4, 0.4, 1.2], "brass")
    for side, x in (("right_arm", -3.8), ("left_arm", 3.8)):
        arm = m.bone(side, [x, 21.5, 0], "body")
        m.cube(arm, [x - 1, 14, -1], [2, 7.5, 2], "porcelain")
    a = {
        "idle": {"loop": True, "animation_length": 4.0, "bones": {
            "root": {"position": [0, f"1.5 + 0.8 * math.sin({T} * 90)", 0]},
            "head": {"rotation": [6, 0, f"14 * math.sin({T} * 45)"]},
            "right_arm": {"rotation": [-45, -20, 0]}, "left_arm": {"rotation": [-45, 20, 0]},
            "crank": {"rotation": [f"{T} * 180", 0, 0]}}},
        "walk": {"loop": True, "animation_length": 2.0, "bones": {
            "root": {"position": [0, f"1.5 + 0.6 * math.sin({T} * 180)", 0]}, "body": {"rotation": [12, 0, 0]},
            "head": {"rotation": [0, 0, f"10 * math.sin({T} * 90)"]},
            "right_arm": {"rotation": [-45, -20, 0]}, "left_arm": {"rotation": [-45, 20, 0]},
            "crank": {"rotation": [f"{T} * 180", 0, 0]}}},
        # Its song: its head tips back, the crank whirls.
        "attack": {"loop": False, "animation_length": 1.2, "bones": {
            "root": {"position": [0, 1.5, 0]},
            "head": {"rotation": frames((0.0, [6, 0, 0]), (0.3, [-25, 0, 10]), (0.9, [-25, 0, -10]), (1.2, [6, 0, 0]))},
            "right_arm": {"rotation": [-60, -20, 0]}, "left_arm": {"rotation": [-60, 20, 0]},
            "crank": {"rotation": [f"{T} * 1440", 0, 0]}}},
    }
    return m, a, {"look_at": {"head": {"max_yaw": 60, "max_pitch": 30, "smoothing": 0.1}}}


def gazer():
    """A Gazer: an eye as big as a head, hung in the air, its iris burning red round a pit of a pupil, heavy lids of
    flesh, and tendrils hanging under it. Its lids are a brow and an under-lid of flesh round the eye, and two thin
    lids tucked under them (scaled to nothing), each a ring of four panels round the whole eye, that sweep down and up
    to meet round its middle when it blinks: an eyeball hanging in the air, it is seen from every side, and shuts from
    every side. It turns its eye on what it looks at (look_at), so its stare aims at its quarry."""
    m = Model()
    m.bone("root", [0, 0, 0])
    eye = m.bone("eye", [0, 14, 0], "root")
    m.cube(eye, [-5, 9, -5], [10, 10, 10], "gazer_face")
    brow = m.bone("brow", [0, 19, 0], "eye")
    m.cube(brow, [-5.4, 17.6, -5.4], [10.8, 2, 10.8], "flesh")                                 # the heavy brow over it
    under = m.bone("under", [0, 9, 0], "eye")
    m.cube(under, [-5.4, 8.6, -5.4], [10.8, 1.6, 10.8], "flesh")                               # and the under-lid
    # The lids proper: rings of thin panels round the eye (front, back and sides), hinged under the brow and over the
    # under-lid, each covering half the eye all round when let out (scale 1), tucked away (scale 0) the rest of the
    # time. Within the brow's edge (5.4), outside the eye (5): front and back across, the sides between them. Hinged
    # a little inside the brow and the under-lid (17.9, 9.9), so tucked away (flat on its hinge) a lid lies hidden
    # inside them, not flat on their faces where it would fight them.
    # The lower ring a little inside the upper (5.3 to 5.35): squinting shut they overlap, and the upper lid closes
    # over the lower one rather than the two fighting in one plane.
    for name, parent, hinge, y0, height, out in (("top_lid", "brow", 17.9, 13.8, 4.1, 5.35), ("bottom_lid", "under", 9.9, 9.9, 3.9, 5.3)):
        lid = m.bone(name, [0, hinge, 0], parent)                                              # (it moves with the brow or under-lid)
        inner = round(out - 0.3, 2)
        for z in (-out, inner):
            m.cube(lid, [-out, y0, z], [2 * out, height, 0.3], "flesh")                            # front, back
        for x in (-out, inner):
            m.cube(lid, [x, y0, -inner], [0.3, height, 2 * inner], "flesh")                        # the sides
    for i, (x, z) in enumerate(((-3, -1), (3, -1), (-2, 3), (2, 3), (0, 1))):
        t = m.bone(f"tendril_{i}", [x, 9.5, z], "eye")
        m.cube(t, [x - 0.5, 4.5, z - 0.5], [1, 4.8, 1], "flesh")                                 # its root inside the eye, clear of the under-lid's face
        tip = m.bone(f"tendril_tip_{i}", [x, 4.5, z], f"tendril_{i}")
        m.cube(tip, [x - 0.35, 0.5, z - 0.35], [0.7, 4, 0.7], "flesh")
    tend = {}
    for i in range(5):
        tend[f"tendril_{i}"] = {"rotation": [f"10 * math.sin({T} * 120 + {i * 70})", 0, f"8 * math.sin({T} * 90 + {i * 50})"]}
        tend[f"tendril_tip_{i}"] = {"rotation": [f"16 * math.sin({T} * 120 + {i * 70 - 60})", 0, 0]}
    OPEN, SHUT = 0.0, 1.0

    def blink(at, double=False):
        """A blink at `at` seconds: the lids sweep shut over 0.07 s, hold a moment, and open over 0.1 s (twice, quick,
        with `double`); the brow and the under-lid draw in with them."""
        keys = [(at, OPEN), (at + 0.07, SHUT), (at + 0.11, SHUT), (at + 0.21, OPEN)]
        if double:
            keys += [(at + 0.3, OPEN), (at + 0.36, SHUT), (at + 0.39, SHUT), (at + 0.48, OPEN)]
        return keys

    def lids(*blinks, length):
        keys = [(0.0, OPEN)]
        for b in blinks:
            keys += b
        keys.append((length, OPEN))
        scale = {f"{round(t, 3)}": {"post": [1, v, 1], "lerp_mode": "linear"} for t, v in keys}
        squint = {f"{round(t, 3)}": {"post": [0, -0.35 * (v - OPEN) / (SHUT - OPEN), 0], "lerp_mode": "linear"} for t, v in keys}
        rise = {f"{round(t, 3)}": {"post": [0, 0.3 * (v - OPEN) / (SHUT - OPEN), 0], "lerp_mode": "linear"} for t, v in keys}
        return {"top_lid": {"scale": scale}, "bottom_lid": {"scale": scale}, "brow": {"position": squint}, "under": {"position": rise}}

    a = {
        # Hanging, bobbing, its tendrils drifting; now and then it blinks (once at 1.2 s, twice quickly at 4.1 s).
        "idle": {"loop": True, "animation_length": 6.0, "bones": dict(tend, **lids(blink(1.2), blink(4.1, double=True), length=6.0),
            eye={"position": [0, f"1.0 * math.sin({T} * 120)", 0]})},
        "walk": {"loop": True, "animation_length": 1.5, "bones": dict(tend, **lids(blink(0.9), length=1.5),
            eye={"position": [0, f"0.8 * math.sin({T} * 240)", 0], "rotation": [10, 0, 0]})},
        # Locking on: its brow and under-lid drawn back, its lids tucked away, the eye staring wide, straining forward
        # and swelling in quick throbs, trembling; its tendrils splayed and quivering.
        "beam": {"loop": True, "animation_length": 0.5, "bones": dict(
            {f"tendril_{i}": {"rotation": [f"{28 if i < 2 else -28} + 6 * math.sin({T} * 1440 + {i * 72})", 0,
                                           f"{(-1) ** i * 28} + 5 * math.sin({T} * 1080 + {i * 50})"]} for i in range(5)},
            **{f"tendril_tip_{i}": {"rotation": [f"-20 + 10 * math.sin({T} * 1440 + {i * 72 + 90})", 0, 0]} for i in range(5)},
            eye={"rotation": [0, f"1.5 * math.sin({T} * 2880)", f"1.5 * math.sin({T} * 2160)"],
                 "position": [0, 0, -0.8],
                 "scale": [f"1.03 + 0.03 * math.sin({T} * 1440)"] * 3},
            brow={"position": [0, 0.9, 0.4]}, under={"position": [0, -0.7, 0.3]},
            top_lid={"scale": [1, 0, 1]}, bottom_lid={"scale": [1, 0, 1]})},
    }
    return m, a, {"look_at": {"eye": {"max_yaw": 40, "max_pitch": 55, "smoothing": 0.3}}}

def tick_tock():
    """A Tick-Tock: a clockwork beetle of brass, a clock's face for a face with two lit eyes on its dial, gears turning
    in its sides, a winding key in its back; six thin iron legs."""
    m = Model()
    m.bone("root", [0, 0, 0])
    body = m.bone("body", [0, 5, 0], "root")
    m.cube(body, [-4, 3.5, -5], [8, 5, 10], "brass", inflate=0.1)
    m.cube(body, [-3.5, 2.5, -4.5], [7, 1.2, 9], "iron")
    for x in (-4.4, 4.0):
        m.cube(body, [x, 4.5, -2], [0.4, 3, 3], "iron")                                        # gears in its sides
    head = m.bone("head", [0, 5.5, -5], "body")
    m.cube(head, [-3, 3, -7.5], [6, 5, 2.5], "clock_face")
    key = m.bone("key", [0, 8.5, 2], "body")
    m.cube(key, [-0.5, 8.5, 1.5], [1, 2.5, 1], "brass")
    m.cube(key, [-2, 11, 1.5], [4, 2, 1], "brass")
    for i, z in enumerate((-3.5, 0, 3.5)):
        for side, sx in (("right", -1), ("left", 1)):
            leg = m.bone(f"{side}_leg_{i}", [3.5 * sx, 4, z], "body")
            x0 = 3.5 if sx > 0 else -8.5
            m.cube(leg, [x0, 3.5, z - 0.4], [5, 0.8, 0.8], "iron", rotation=[0, 0, -35 * sx], pivot=[3.5 * sx, 4, z])
    legs = {}
    for i in range(3):
        for side, sx in (("right", -1), ("left", 1)):
            ph = i * 120 + (0 if sx > 0 else 60)
            legs[f"{side}_leg_{i}"] = {"rotation": [0, f"20 * math.sin({T} * 1080 + {ph})", f"{14 * sx} * math.max(0, math.sin({T} * 1080 + {ph} + 90))"]}
    a = {
        "idle": {"loop": True, "animation_length": 2.0, "bones": {
            "key": {"rotation": [0, 0, f"{T} * 90"]}, "body": {"position": [0, f"0.2 * math.sin({T} * 180)", 0]}}},
        "walk": {"loop": True, "animation_length": 0.34, "bones": dict(legs, key={"rotation": [0, 0, f"{T} * 360"]},
                                                                        body={"position": [0, f"0.3 * math.abs(math.sin({T} * 1080))", 0]})},
        # About to burst: shaking, its key whirling.
        "fuse": {"loop": True, "animation_length": 0.2, "bones": {
            "body": {"position": [f"0.4 * math.sin({T} * 3600)", 0, f"0.4 * math.cos({T} * 2700)"], "rotation": [0, f"4 * math.sin({T} * 1800)", 0]},
            "key": {"rotation": [0, 0, f"{T} * 1800"]}, "head": {"rotation": [-15, 0, 0]}}},
    }
    return m, a, {}


def stitchling():
    """A Stitchling: a ragdoll in a patchwork of old cloth, its head too big for it, buttons for eyes, its mouth sewn
    shut, stuffing coming out of a split, tufts of yarn hair and a needle left through its head."""
    m = Model()
    m.bone("root", [0, 0, 0])
    legs(m, "root", (-1.4, 1.4), 7, "patch", "patch", thick=2.4)
    body = m.bone("body", [0, 7, 0], "root")
    m.cube(body, [-3, 7, -2], [6, 6, 4], "patch")
    m.cube(body, [0.8, 9.5, -2.4], [1.6, 1.6, 0.6], "stuffing")                                 # stuffing out of a split
    head = m.bone("head", [0, 13, 0], "body")
    m.cube(head, [-3.5, 13, -3.5], [7, 7, 7], "button_face")
    for x, z in ((-2.5, -1), (-0.5, 0.5), (1.5, -0.5)):
        m.cube(head, [x, 20, z], [1, 1.5, 1], "yarn")                                           # yarn hair
    m.cube(head, [2.5, 17.5, -1.5], [0.4, 0.4, 5], "iron", rotation=[0, 20, 10], pivot=[2.7, 17.7, 1])   # a needle through it
    for side, x in (("right_arm", -3.8), ("left_arm", 3.8)):
        arm = m.bone(side, [x, 12.5, 0], "body")
        m.cube(arm, [x - 1, 6, -1], [2, 6.5, 2], "patch")
    a = {
        "idle": {"loop": True, "animation_length": 2.5, "bones": {
            "head": {"rotation": [0, 0, f"18 * math.sin({T} * 144)"]},
            "right_arm": {"rotation": [0, 0, f"-8 + 6 * math.sin({T} * 144)"]}, "left_arm": {"rotation": [0, 0, f"8 - 6 * math.sin({T} * 144)"]},
            "body": {"rotation": [0, 0, f"4 * math.sin({T} * 144 + 90)"]}}},
        # A jerky skip, arms flailing.
        "walk": {"loop": True, "animation_length": 0.5, "bones": dict(humanoid_walk(swing=55, stride=45),
            body={"position": [0, f"0.8 * math.abs(math.sin({T} * 720))", 0], "rotation": [6, 0, f"8 * math.sin({T} * 720)"]},
            head={"rotation": [0, 0, f"-14 * math.sin({T} * 720)"]})},
        "attack": {"loop": False, "animation_length": 0.35, "bones": {
            "right_arm": {"rotation": frames((0.0, [0, 0, 0]), (0.12, [-130, 0, 0]), (0.24, [-30, 0, 0]), (0.35, [0, 0, 0]))},
            "left_arm": {"rotation": frames((0.0, [0, 0, 0]), (0.14, [-120, 0, 0]), (0.26, [-25, 0, 0]), (0.35, [0, 0, 0]))}}},
    }
    return m, a, {}


def gloom_jelly():
    """A Gloom Jelly: a jellyfish of the void, a ribbed violet bell with a glowing heart in it, frilled arms under it
    and long glowing stinging strands trailing down."""
    m = Model()
    m.bone("root", [0, 0, 0])
    bell = m.bone("bell", [0, 16, 0], "root")
    m.cube(bell, [-5, 14, -5], [10, 6, 10], "jelly")
    m.cube(bell, [-3.5, 20, -3.5], [7, 2, 7], "jelly")
    m.cube(bell, [-5.5, 13, -5.5], [11, 1.5, 11], "frill")                                     # its rim
    m.cube(bell, [-2, 15, -2], [4, 3, 4], "jelly_core")
    arms = m.bone("arms", [0, 13, 0], "bell")
    for x, z in ((-1.2, -1.2), (0.2, -1.2), (-1.2, 0.2), (0.2, 0.2)):
        m.cube(arms, [x, 5, z], [1, 8, 1], "frill")
    for i in range(8):
        ang = i * math.pi / 4
        x, z = round(math.cos(ang) * 4.5, 2), round(math.sin(ang) * 4.5, 2)
        t = m.bone(f"strand_{i}", [x, 13, z], "bell")
        m.cube(t, [x - 0.3, 1, z - 0.3], [0.6, 12, 0.6], "strand")
    pulse = f"math.sin({T} * 180)"
    sway = {f"strand_{i}": {"rotation": [f"8 * math.sin({T} * 180 - {i * 45 + 90})", 0, f"8 * math.cos({T} * 180 - {i * 45})"]} for i in range(8)}
    clip = {"loop": True, "animation_length": 2.0, "bones": dict(sway,
        bell={"scale": [f"1 + 0.08 * {pulse}", f"1 - 0.07 * {pulse}", f"1 + 0.08 * {pulse}"], "position": [0, f"1.2 * math.sin({T} * 180 - 60)", 0]},
        arms={"rotation": [f"6 * math.sin({T} * 180 - 90)", 0, 0]})}
    return m, {"idle": clip, "walk": clip}, {}


def dream_hare():
    """A Dream Hare: a pale hare with long ears tipped with a light like a waking dream, eyes the same pale blue."""
    m = Model()
    m.bone("root", [0, 0, 0])
    body = m.bone("body", [0, 4, 1], "root")
    m.cube(body, [-2.5, 2, -3], [5, 4.5, 7], "hare")
    m.cube(body, [-1, 4.5, 4], [2, 2, 1.5], "stuffing", inflate=0.2)                           # its tail, a puff
    head = m.bone("head", [0, 6, -3], "body")
    m.cube(head, [-2, 5, -6.5], [4, 4, 4], "hare_face")
    for side, x in (("right_ear", -1.2), ("left_ear", 0.8)):
        ear = m.bone(side, [x + 0.4, 9, -4.5], "head", rotation=[15, 0, -10 if x < 0 else 10])
        m.cube(ear, [x - 0.2, 9, -5], [1.2, 5, 0.6], "hare")
        m.cube(ear, [x - 0.2, 13.5, -5.1], [1.2, 1.5, 0.8], "dreamlight")
    for side, x in (("right_hind", -1.8), ("left_hind", 1.8)):
        leg = m.bone(side, [x, 3.5, 2], "body")
        m.cube(leg, [x - 1, 0, -0.5], [2, 3.5, 4.5], "hare")
    for side, x in (("right_fore", -1.2), ("left_fore", 1.2)):
        leg = m.bone(side, [x, 3, -2], "body")
        m.cube(leg, [x - 0.5, 0, -2.5], [1, 3, 1], "hare")
    a = {
        "idle": {"loop": True, "animation_length": 3.0, "bones": {
            "head": {"rotation": [f"3 * math.sin({T} * 1200) * math.max(0, math.sin({T} * 120))", 0, 0]},   # sniffing
            "right_ear": {"rotation": [f"8 * math.max(0, math.sin({T} * 240) - 0.7) * 3", 0, 0]},
            "left_ear": {"rotation": [f"8 * math.max(0, math.sin({T} * 240 + 120) - 0.7) * 3", 0, 0]}}},
        # It hops.
        "walk": {"loop": True, "animation_length": 0.5, "bones": {
            "body": {"position": [0, f"2.2 * math.max(0, math.sin({T} * 720))", 0], "rotation": [f"-10 * math.sin({T} * 720)", 0, 0]},
            "right_hind": {"rotation": [f"40 * math.max(0, math.sin({T} * 720))", 0, 0]},
            "left_hind": {"rotation": [f"40 * math.max(0, math.sin({T} * 720))", 0, 0]},
            "right_fore": {"rotation": [f"-30 * math.sin({T} * 720)", 0, 0]}, "left_fore": {"rotation": [f"-30 * math.sin({T} * 720)", 0, 0]},
            "right_ear": {"rotation": [f"-20 * math.sin({T} * 720)", 0, 0]}, "left_ear": {"rotation": [f"-20 * math.sin({T} * 720)", 0, 0]}}},
    }
    return m, a, {"look_at": {"head": {"max_yaw": 40, "max_pitch": 20, "smoothing": 0.25}}}


def grinning_clown():
    """A Grinning Clown: too tall, too thin, in a faded harlequin suit with pompom buttons and a ruff, its face in
    cracked greasepaint, black diamonds round lit yellow eyes, a grin too wide, a glowing red nose, tufts of orange hair
    under a crooked cone hat; big shoes; a mallet in one hand and two balloons on strings in the other."""
    m = Model()
    m.bone("root", [0, 0, 0])
    legs(m, "root", (-1.8, 1.8), 13, "harlequin", "shoe", thick=3.0)
    for b in m.bones:
        if b["name"] in ("right_shin", "left_shin"):
            x = b["pivot"][0]
            m.cube(b, [x - 2, 0, -6], [4, 2.2, 7.5], "shoe")                                    # its big shoes
            m.cube(b, [x - 0.5, 2.2, -5.5], [1, 0.6, 1], "pom")
    # Balls the balloons are kept out of (their springs' colliders): its body, its head and its hat.
    body = m.bone("body", [0, 13, 0], "root", locators={"body_ball": [0, 18.5, 0], "ruff_ball": [0, 23, 0]})
    m.cube(body, [-4, 13, -2.5], [8, 10, 5], "harlequin")
    for i in range(3):
        m.cube(body, [-0.6, 15 + i * 2.5, -2.9], [1.2, 1.2, 0.5], "pom")                       # pompom buttons
    m.cube(body, [-5, 22, -3.5], [10, 2, 7], "ruff", inflate=0.2)                               # its ruff
    head = m.bone("head", [0, 24, 0], "body", locators={"head_ball": [0, 27.8, 0], "hat_ball": [0, 34.5, 0]})
    m.cube(head, [-3.5, 24, -3.5], [7, 7.5, 7], "clown_face")
    m.cube(head, [-1, 26.5, -4.7], [2, 2, 1.3], "nose")                                         # its nose, glowing
    for x in (-5.6, 3.6):
        m.cube(head, [x, 26.5, -2], [2, 3.5, 4.5], "clown_hair")                                # tufts of hair
    hat = m.bone("hat", [0, 31.5, 0], "head", rotation=[0, 0, -12])
    m.cube(hat, [-3, 31.5, -3], [6, 2, 6], "harlequin")
    m.cube(hat, [-2, 33.5, -2], [4, 2, 4], "harlequin")
    m.cube(hat, [-1, 35.5, -1], [2, 2, 2], "harlequin")
    m.cube(hat, [-1.2, 37.5, -1.2], [2.4, 1.6, 2.4], "pom")
    for side, x in (("right_arm", -5.2), ("left_arm", 5.2)):
        arm = m.bone(side, [x, 22.5, 0], "body")
        m.cube(arm, [x - 1.25, 15, -1.25], [2.5, 7.5, 2.5], "harlequin")
        m.cube(arm, [x - 1.7, 14.4, -1.7], [3.4, 1, 3.4], "ruff")                              # a frill at its cuff
        fore = m.bone(side.replace("arm", "forearm"), [x, 15, 0], side)
        m.cube(fore, [x - 1.2, 9.5, -1.2], [2.4, 5, 2.4], "ruff")                               # its gloves
    # A giant squeaky toy hammer: a candy-striped handle out of its fist, a big pleated yellow head with red caps.
    mallet = m.bone("mallet", [-5.2, 9.6, 0], "right_forearm", rotation=[0, 0, 90])            # its head stood up: it strikes with a cap
    for i in range(6):
        m.cube(mallet, [-5.65, 9.15, -1.5 - i * 1.5], [0.9, 0.9, 1.5], "balloon_red" if i % 2 else "ruff")   # the handle, striped
    m.cube(mallet, [-8.7, 7.6, -13], [7, 4, 4], "balloon_yellow")                               # its head
    for x in (-7.9, -6.0, -4.1):
        m.cube(mallet, [x - 0.25, 7.35, -13.25], [0.5, 4.5, 4.5], "balloon_yellow", inflate=0.05)   # pleats round it
    for x in (-9.5, -2.0):
        m.cube(mallet, [x, 7.8, -12.8], [0.8, 3.6, 3.6], "balloon_red")                         # its caps
    # Two balloons, each on its own string from its fist, each its own joint: free parts (see the springs below) that
    # float up from where they are held and drift, trail and swing as it moves.
    # The strings come from the tip of its fist (the glove ends at 9.5), where its fingers close on them.
    hand = 9.4
    # Each leans its own way at rest, so the two start apart (their physics takes over from there).
    # The red one carries a point at its middle the violet one is kept off (a collider), so they jostle.
    red = balloon(m, "balloon_red", "left_forearm", 5.2, hand, 17.1, 4.2, "balloon_red", [-8, 0, -14],
                  locators={"red_balloon": [5.2, hand + 17.1 + 4.2 * 0.57, 0]})
    violet = balloon(m, "balloon_violet", "left_forearm", 5.2, hand, 13.6, 3.6, "balloon_violet", [10, 0, 18])
    # The strings kept out of its body, head and hat too (the parts' own size, and a little).
    strings = [{"point": "head_ball", "radius": 7.3}, {"point": "hat_ball", "radius": 4.3},
               {"point": "body_ball", "radius": 6.7}, {"point": "ruff_ball", "radius": 5.9}]
    bob = f"math.sin({T} * 120)"
    a = {
        "idle": {"loop": True, "animation_length": 3.0, "bones": {
            "body": {"rotation": [0, 0, f"5 * math.sin({T} * 120)"]},
            "head": {"rotation": [f"6 * math.sin({T} * 240)", f"10 * math.sin({T} * 60)", f"-14 * math.sin({T} * 120) + 6"]},
            "hat": {"rotation": [0, 0, f"4 * math.sin({T} * 240)"]},
            "right_arm": {"rotation": [-10, 0, 6]}, "right_forearm": {"rotation": [-25, 0, 0]},
            "left_arm": {"rotation": [f"-15 + 6 * {bob}", 0, -16]}, "left_forearm": {"rotation": [-20, 0, 0]}}},   # held out, its balloons clear of it
        # A bouncing, high-kneed strut, arms swinging wide.
        "walk": {"loop": True, "animation_length": 0.8, "bones": dict(humanoid_walk(swing=40, stride=42),
            body={"position": [0, f"1.2 * math.abs(math.sin({T} * 450))", 0], "rotation": [0, 0, f"6 * math.sin({T} * 450)"]},
            head={"rotation": [0, 0, f"-10 * math.sin({T} * 450)"]},
            right_forearm={"rotation": [-30, 0, 0]}, left_forearm={"rotation": [-20, 0, 0]},
            left_arm={"rotation": [f"40 * math.sin({T} * 360)", 0, -14]})},          # its swing as humanoid_walk's, held out
        # The bonk: mallet up over its head and down.
        "attack": {"loop": False, "animation_length": 0.5, "bones": {
            "right_arm": {"rotation": frames((0.0, [-10, 0, 6]), (0.2, [-170, 0, -16]), (0.32, [-50, 0, 0]), (0.5, [-10, 0, 6]))},   # up and out, clear of its head
            "right_forearm": {"rotation": frames((0.0, [-25, 0, 0]), (0.2, [-10, 0, 0]), (0.32, [-40, 0, 0]), (0.5, [-25, 0, 0]))},
            "head": {"rotation": frames((0.0, [0, 0, 0]), (0.2, [-10, 0, -10]), (0.32, [10, 0, 10]), (0.5, [0, 0, 0]))}}},
        # It laughs: head thrown back, shoulders shaking, arms out.
        "laugh": {"loop": True, "animation_length": 0.5, "bones": {
            "body": {"rotation": [f"-6 + 4 * math.sin({T} * 1440)", 0, 0]},
            "head": {"rotation": [f"-22 + 6 * math.sin({T} * 1440)", 0, f"8 * math.sin({T} * 720)"]},
            "right_arm": {"rotation": [-20, 0, f"35 + 6 * math.sin({T} * 1440)"]},
            "left_arm": {"rotation": [-20, 0, f"-35 - 6 * math.sin({T} * 1440)"]}}},
    }
    return m, a, {"look_at": {"head": {"max_yaw": 60, "max_pitch": 30, "smoothing": 0.25}},
                  # The balloons float free (no stiffness: nothing pulls them back to the clip), lifted, on slack
                  # strings, drifting in the wind and turning as they swing; the violet one a little heavier.
                  # Each one's weight is its balloon, at the top of its string (length), and bumps into the world: blocks
                  # overhead and the players and creatures that walk into it (collide, a ball the balloon's size).
                  "springs": dict(
                      balloon_springs(red, 17.1, 4.2, 4.5, 2.1, 1.1, 0.7, strings,
                                      [{"point": "head_ball", "radius": 10.8}, {"point": "hat_ball", "radius": 7.8},
                                       {"point": "body_ball", "radius": 10.2}, {"point": "ruff_ball", "radius": 9.4}]),
                      **balloon_springs(violet, 13.6, 3.6, 3.5, 2.6, 0.9, 0.5, strings,
                                        [{"point": "head_ball", "radius": 10.4}, {"point": "hat_ball", "radius": 7.4},
                                         {"point": "body_ball", "radius": 9.8}, {"point": "ruff_ball", "radius": 9.0}]))}


def closet_boogeyman():
    """The Closet Boogeyman: a tall old wardrobe of dark wood with brass handles, standing alone, its doors a crack
    apart and two eyes in the crack. Its doors burst open on the dark inside, and long black arms with bone claws reach
    out of it."""
    m = Model()
    m.bone("root", [0, 0, 0])
    body = m.bone("body", [0, 0, 0], "root")
    m.cube(body, [-8, 0, -6], [16, 2, 12], "closet")                                            # its plinth
    m.cube(body, [-7.5, 2, -5.5], [15, 26, 11], "shadow")                                       # the dark inside
    m.cube(body, [-8, 2, -1], [1, 26, 7], "closet")                                             # sides
    m.cube(body, [7, 2, -1], [1, 26, 7], "closet")
    m.cube(body, [-8, 2, 5], [16, 26, 1], "closet")                                             # back
    m.cube(body, [-8.5, 28, -6.5], [17, 3, 13], "closet")                                       # its crown
    m.cube(body, [-6, 31, -5], [12, 1.5, 10], "closet")
    for side, x0, sx in (("right_door", -8, -1), ("left_door", 0, 1)):
        door = m.bone(side, [8 * sx, 2, -6], "body")
        m.cube(door, [x0, 2, -6.5], [8, 26, 1.5], "closet_face")
    eyes = m.bone("eyes", [0, 20, -5], "body")
    for x in (-2.5, 1.5):
        m.cube(eyes, [x, 19.5, -5.2], [1, 1, 0.4], "eye")                                       # its eyes, in the dark inside
    for side, x in (("right_arm", -3), ("left_arm", 3)):
        arm = m.bone(side, [x, 16, -2], "body")
        m.cube(arm, [x - 0.8, 15.2, -12], [1.6, 1.6, 10], "shadow")
        fore = m.bone(side.replace("arm", "forearm"), [x, 16, -12], side)
        m.cube(fore, [x - 0.7, 15.3, -21], [1.4, 1.4, 9], "shadow")
        for i, fx in enumerate((-0.7, 0, 0.7)):
            m.cube(fore, [x + fx - 0.2, 15.6 - i * 0.3, -24], [0.4, 0.4, 3], "bone")           # claws
    shut = {"right_door": {"rotation": [0, 0, 0]}, "left_door": {"rotation": [0, 0, 0]},
            "right_arm": {"scale": [1, 1, 0.05]}, "left_arm": {"scale": [1, 1, 0.05]},          # its arms drawn into the dark
            "eyes": {"position": [0, 0, 0]}}
    a = {
        # Shut: still, but for a creak of a door now and then.
        "idle": {"loop": True, "animation_length": 6.0, "bones": dict(shut, right_door={"rotation": [0, f"-6 * math.max(0, math.sin({T} * 60) - 0.85) * 6", 0]})},
        "walk": {"loop": True, "animation_length": 6.0, "bones": shut},
        # Open: doors flung wide, its arms out and groping.
        "open": {"loop": True, "animation_length": 1.2, "bones": {
            "right_door": {"rotation": [0, -110, 0]}, "left_door": {"rotation": [0, 110, 0]},
            "right_arm": {"rotation": [f"-10 + 10 * math.sin({T} * 300)", f"-15 + 12 * math.sin({T} * 300)", 0]},
            "left_arm": {"rotation": [f"-10 + 10 * math.sin({T} * 300 + 120)", f"15 + 12 * math.sin({T} * 300 + 120)", 0]},
            "right_forearm": {"rotation": [f"20 * math.sin({T} * 300 + 60)", 0, 0]}, "left_forearm": {"rotation": [f"20 * math.sin({T} * 300 + 180)", 0, 0]}}},
        # The grab: arms whip out and back.
        "attack": {"loop": False, "animation_length": 0.5, "bones": {
            "right_arm": {"scale": frames((0.0, [1, 1, 1]), (0.15, [1, 1, 1.4]), (0.5, [1, 1, 1])), "rotation": frames((0.0, [0, 0, 0]), (0.15, [-15, 10, 0]), (0.5, [0, 0, 0]))},
            "left_arm": {"scale": frames((0.0, [1, 1, 1]), (0.15, [1, 1, 1.4]), (0.5, [1, 1, 1])), "rotation": frames((0.0, [0, 0, 0]), (0.15, [-15, -10, 0]), (0.5, [0, 0, 0]))}}},
    }
    return m, a, {}


def big_bad_wolf():
    """The Big Bad Wolf: a great grey wolf on its hind legs dressed as grandmother, a lace bonnet over its ears, a knitted
    red shawl, a long nightgown with sleeves; its snout, its yellow eyes under a heavy brow and too many teeth showing
    under the bonnet, its tail out from under the hem. Underneath it is all fur. The clothes are joints of their own
    (bonnet, shawl, gown, sleeves): its transformation tears them off."""
    m = Model()
    m.bone("root", [0, 0, 0])
    legs(m, "root", (-2, 2), 11, "wolf_fur", "wolf_fur", thick=3.4)
    for b in m.bones:
        if b["name"] in ("right_shin", "left_shin"):
            x = b["pivot"][0]
            for i, cx in enumerate((-1.2, 0, 1.2)):
                m.cube(b, [x + cx - 0.25, 0, -3.6 - (i % 2) * 0.4], [0.5, 0.8, 1.2], "bone")     # claws on its feet
    body = m.bone("body", [0, 11, 0], "root")
    m.cube(body, [-4.3, 11, -2.8], [8.6, 12, 5.6], "wolf_fur")                                   # its body: fur
    m.cube(body, [-3.4, 16.5, -3.3], [6.8, 5.5, 0.6], "wolf_fur")                                # a shaggy chest
    gown = m.bone("gown", [0, 11, 0], "body")
    m.cube(gown, [-4.65, 10.8, -3.15], [9.3, 12.4, 6.3], "gown")                                 # the nightgown over it
    cloth = skirt(m, "gown", 11, 4, 5.1, 3.6, "gown", name="gown_skirt", rows=2, torn=False, fabric="cotton")   # granny's cotton, to its shins
    shawl = m.bone("shawl", [0, 22, 0], "body")
    m.cube(shawl, [-5.3, 17, -3.5], [10.6, 6.5, 7], "shawl", inflate=0.1)
    m.cube(shawl, [-1.5, 13.5, -3.75], [3, 4, 0.4], "shawl")                                    # knotted in front
    tail = m.bone("tail", [0, 11, 3], "body", rotation=[-35, 0, 0])
    m.cube(tail, [-1, 4, 3], [2, 8, 2.5], "wolf_fur")
    head = m.bone("head", [0, 23.5, -1], "body")
    m.cube(head, [-3.5, 23, -5], [7, 6.5, 7], "wolf_fur")
    m.cube(head, [-2, 22.5, -10], [4, 3.5, 5], "wolf_face")                                      # its snout
    m.cube(head, [-3.6, 26.6, -5.5], [7.2, 0.8, 0.6], "wolf_fur")                                # a heavy brow
    for x in (-3.3, 2.0):
        m.cube(head, [x, 25.3, -5.35], [1.3, 1, 0.4], "wolf_eye")                               # its eyes, yellow, beside its snout
    for x in (-3.4, 2.2):
        m.cube(head, [x, 29.4, -2], [1.2, 3, 1.5], "wolf_fur")                                  # ears, through holes in the bonnet
    jaw = m.bone("jaw", [0, 23, -5], "head")
    m.cube(jaw, [-1.8, 21.5, -9.5], [3.6, 1.2, 4.5], "wolf_fur")
    m.cube(jaw, [-1.6, 22.7, -9.4], [3.2, 0.5, 0.4], "bone")                                    # its lower teeth
    bonnet = m.bone("bonnet", [0, 27, -1], "head")
    m.cube(bonnet, [-4.2, 27, -5.6], [8.4, 4, 8], "bonnet", inflate=0.1)
    m.cube(bonnet, [-4.6, 26.5, -6], [9.2, 0.6, 1], "bonnet")                                   # its frill, above the eyes
    for side, x in (("right_arm", -5.5), ("left_arm", 5.5)):
        arm = m.bone(side, [x, 22, 0], "body")
        m.cube(arm, [x - 1.3, 14, -1.3], [2.6, 8, 2.6], "wolf_fur")
        sleeve = m.bone(side.replace("arm", "sleeve"), [x, 22, 0], side)
        m.cube(sleeve, [x - 1.6, 14.5, -1.6], [3.2, 7.6, 3.2], "gown")                          # granny's sleeve over it
        fore = m.bone(side.replace("arm", "forearm"), [x, 14, 0], side)
        m.cube(fore, [x - 1.2, 8, -1.2], [2.4, 6, 2.4], "wolf_fur")
        for i, fx in enumerate((-0.8, 0, 0.8)):
            m.cube(fore, [x + fx - 0.2, 6, -0.9 + i * 0.3], [0.4, 2.5, 0.4], "bone")           # claws
    gone = {"bonnet": {"scale": [0, 0, 0]}, "shawl": {"scale": [0, 0, 0]}, "gown": {"scale": [0, 0, 0]},
            "right_sleeve": {"scale": [0, 0, 0]}, "left_sleeve": {"scale": [0, 0, 0]}}
    a = {
        # In disguise: hunched over like an old woman, hands folded, a little sway.
        "disguised": {"loop": True, "animation_length": 4.0, "bones": {
            "body": {"rotation": [28, 0, f"3 * math.sin({T} * 90)"]}, "head": {"rotation": [-18, f"8 * math.sin({T} * 45)", 0]},
            "right_arm": {"rotation": [-55, 25, 0]}, "left_arm": {"rotation": [-55, -25, 0]},
            "right_forearm": {"rotation": [-30, 0, 0]}, "left_forearm": {"rotation": [-30, 0, 0]},
            "tail": {"rotation": [20, 0, 0]}}},
        "hobble": {"loop": True, "animation_length": 1.6, "bones": dict(humanoid_walk(swing=4, stride=14),
            body={"rotation": [28, 0, f"5 * math.sin({T} * 225)"]}, head={"rotation": [-18, 0, 0]},
            right_arm={"rotation": [-55, 25, 0]}, left_arm={"rotation": [-55, -25, 0]}, tail={"rotation": [20, 0, 0]})},
        # The transformation: it shudders, throws its head back, the bonnet flies off, the shawl and the gown tear away,
        # its jaw drops in a roar and it straightens to its full height (the entity grows as this plays).
        "reveal": {"loop": False, "animation_length": 1.5, "bones": {
            "body": {"rotation": frames((0.0, [28, 0, 0]), (0.2, [24, 0, -6]), (0.35, [30, 0, 6]), (0.5, [26, 0, -6]), (0.8, [-22, 0, 0]), (1.5, [8, 0, 0]))},
            "head": {"rotation": frames((0.0, [-18, 0, 0]), (0.5, [-10, 0, 0]), (0.8, [-50, 0, 0]), (1.5, [0, 0, 0]))},
            "jaw": {"rotation": frames((0.0, [0, 0, 0]), (0.6, [5, 0, 0]), (0.85, [60, 0, 0]), (1.5, [12, 0, 0]))},
            "bonnet": {"position": frames((0.0, [0, 0, 0]), (0.75, [0, 0, 0]), (1.0, [0, 9, 5])),
                       "rotation": frames((0.0, [0, 0, 0]), (0.75, [0, 0, 0]), (1.0, [-80, 40, 0])),
                       "scale": frames((0.0, [1, 1, 1]), (0.9, [1, 1, 1]), (1.05, [0, 0, 0]))},
            "shawl": {"scale": frames((0.0, [1, 1, 1]), (0.55, [1, 1, 1]), (0.7, [1.25, 1.1, 1.3]), (0.8, [0, 0, 0]))},
            "gown": {"scale": frames((0.0, [1, 1, 1]), (0.65, [1, 1, 1]), (0.78, [1.12, 1.04, 1.18]), (0.88, [0, 0, 0]))},
            "right_sleeve": {"scale": frames((0.0, [1, 1, 1]), (0.6, [1, 1, 1]), (0.72, [1.3, 1, 1.3]), (0.8, [0, 0, 0]))},
            "left_sleeve": {"scale": frames((0.0, [1, 1, 1]), (0.62, [1, 1, 1]), (0.74, [1.3, 1, 1.3]), (0.82, [0, 0, 0]))},
            "right_arm": {"rotation": frames((0.0, [-55, 25, 0]), (0.5, [-40, 0, 20]), (0.85, [-150, 0, -30]), (1.5, [-25, 0, 20]))},
            "left_arm": {"rotation": frames((0.0, [-55, -25, 0]), (0.5, [-40, 0, -20]), (0.85, [-150, 0, 30]), (1.5, [-25, 0, -20]))},
            "tail": {"rotation": frames((0.0, [20, 0, 0]), (0.8, [-40, 0, 0]), (1.5, [0, 0, 0]))}}},
        # Revealed: upright, jaw working, arms out, claws ready; the clothes gone.
        "idle": {"loop": True, "animation_length": 2.0, "bones": dict(gone,
            body={"rotation": [8, 0, 0]}, jaw={"rotation": [f"12 + 6 * math.sin({T} * 360)", 0, 0]},
            right_arm={"rotation": [-25, 0, 20]}, left_arm={"rotation": [-25, 0, -20]},
            tail={"rotation": [0, f"15 * math.sin({T} * 180)", 0]})},
        "walk": {"loop": True, "animation_length": 0.7, "bones": dict(humanoid_walk(arms_forward=40, swing=30, stride=42), **gone,
            body={"rotation": [16, 0, 0], "position": [0, f"0.8 * math.abs(math.sin({T} * 514))", 0]},
            jaw={"rotation": [18, 0, 0]}, tail={"rotation": [10, f"20 * math.sin({T} * 514)", 0]})},
        "attack": {"loop": False, "animation_length": 0.45, "bones": dict(gone, **{
            "right_arm": {"rotation": frames((0.0, [-25, 0, 20]), (0.15, [-150, 0, -20]), (0.3, [-40, 0, -10]), (0.45, [-25, 0, 20]))},
            "head": {"rotation": frames((0.0, [0, 0, 0]), (0.15, [-15, 0, 0]), (0.3, [20, 0, 0]), (0.45, [0, 0, 0]))},
            "jaw": {"rotation": frames((0.0, [12, 0, 0]), (0.15, [45, 0, 0]), (0.3, [0, 0, 0]), (0.45, [12, 0, 0]))}})},
    }
    return m, a, {"springs": cloth, "look_at": {"head": {"max_yaw": 50, "max_pitch": 30, "smoothing": 0.2}}}


def sandman():
    """The Sandman: a tall stooped figure in a dusty hooded cloak to the ground, nothing in its hood but two gold eyes,
    a long thin hand out of each sleeve, a burlap sack of glowing dream-sand over its shoulder, sand sifting from it."""
    m = Model()
    m.bone("root", [0, 0, 0])
    body = m.bone("body", [0, 2, 0], "root", locators={"cloak_ball": [0, 9, 0]})
    m.cube(body, [-4.5, 12, -3.5], [9, 10, 7], "sand_cloak")                                    # the cloak
    # and below its waist, cloth to the ground, trailing on it (nothing under it but a ball to hang round)
    cloth = skirt(m, "body", 12.5, 0, 4.75, 3.75, "sand_cloak", name="cloak", rows=3, legs=(), drag=True,
                  colliders=[{"point": "cloak_ball", "radius": 3.2}], fabric="canvas")                    # heavy, dusty, dragging
    m.cube(body, [-4, 22, -3], [8, 8, 6], "sand_cloak")                                         # its shoulders
    head = m.bone("head", [0, 30, -0.5], "body")
    m.cube(head, [-3.5, 29.5, -4.5], [7, 8, 7], "sand_cloak", inflate=0.2)                      # the hood
    m.cube(head, [-2.8, 30.5, -4.7], [5.6, 6, 0.4], "sandman_face")                             # the dark in it
    m.cube(head, [-1, 37, 0], [2, 3, 3], "sand_cloak")                                          # its point, drooping back
    sack = m.bone("sack", [3.5, 28, 2], "body")
    m.cube(sack, [2.5, 20, 2], [5, 8, 5], "burlap")
    m.cube(sack, [3.5, 27.5, 3], [3, 1.5, 3], "dreamsand")                                      # its mouth, glowing
    m.cube(sack, [4.5, 15, 3.8], [1, 5, 1], "dreamsand")                                        # sand sifting out
    for side, x in (("right_arm", -5), ("left_arm", 5)):
        arm = m.bone(side, [x, 29, 0], "body")
        m.cube(arm, [x - 1.5, 19, -1.5], [3, 10, 3], "sand_cloak")
        fore = m.bone(side.replace("arm", "forearm"), [x, 19, 0], side)
        m.cube(fore, [x - 0.7, 12, -0.7], [1.4, 7, 1.4], "crawl")
        for i, fx in enumerate((-0.5, 0, 0.5)):
            m.cube(fore, [x + fx - 0.15, 8, -0.4 + i * 0.2], [0.3, 4, 0.3], "crawl")
    a = {
        "idle": {"loop": True, "animation_length": 4.0, "bones": {
            "body": {"rotation": [10, 0, f"2 * math.sin({T} * 90)"]}, "head": {"rotation": [12, f"10 * math.sin({T} * 45)", 0]},
            "right_arm": {"rotation": [-20, 0, 8]}, "left_arm": {"rotation": [-5, 0, -4]},
            "sack": {"rotation": [f"4 * math.sin({T} * 90)", 0, 0]}}},
        # It glides more than walks, the cloak hiding its legs.
        "walk": {"loop": True, "animation_length": 2.0, "bones": {
            "body": {"rotation": [16, 0, f"4 * math.sin({T} * 180)"], "position": [0, f"0.3 * math.sin({T} * 360)", 0]},
            "head": {"rotation": [10, 0, 0]}, "right_arm": {"rotation": [-30, 0, 8]}, "sack": {"rotation": [f"8 * math.sin({T} * 180)", 0, 0]}}},
        # It casts a handful of sand.
        "attack": {"loop": False, "animation_length": 0.7, "bones": {
            "right_arm": {"rotation": frames((0.0, [-20, 0, 8]), (0.25, [-10, 40, 60]), (0.45, [-90, -20, -10]), (0.7, [-20, 0, 8]))},
            "right_forearm": {"rotation": frames((0.0, [0, 0, 0]), (0.25, [-40, 0, 0]), (0.45, [0, 0, 0]), (0.7, [0, 0, 0]))}}},
    }
    return m, a, {"springs": cloth, "look_at": {"head": {"max_yaw": 50, "max_pitch": 30, "smoothing": 0.1}}}


def tooth_taker():
    """A Tooth Taker: the Tooth Fairy gone wrong, a thing the size of a cat with a pale bony body, dragonfly wings, a
    face that is nearly all mouth (every tooth it ever took), and a pair of pliers in its hand."""
    m = Model()
    m.bone("root", [0, 0, 0])
    body = m.bone("body", [0, 5, 0], "root")
    m.cube(body, [-1.5, 3, -1.2], [3, 4, 2.4], "fairy")
    for x in (-1.6, 1.2):
        m.cube(body, [x, 3.5, -1.3], [0.4, 3, 0.2], "bone")                                     # ribs through its skin
    head = m.bone("head", [0, 7, 0], "body")
    m.cube(head, [-2, 7, -2], [4, 4, 4], "tooth_face")
    m.cube(head, [-2.3, 10.6, -2.3], [4.6, 0.6, 4.6], "fairy_wing")                             # a tiny crown, of wire
    for side, sx in (("right_wing", -1), ("left_wing", 1)):
        wing = m.bone(side, [1 * sx, 6.5, 1], "body")
        m.cube(wing, [1 if sx > 0 else -6, 6.4, 0.5], [5, 0.2, 2.5], "fairy_wing")
        m.cube(wing, [1 if sx > 0 else -5, 5.2, 1.2], [4, 0.2, 2], "fairy_wing")
    for side, x in (("right_arm", -1.8), ("left_arm", 1.8)):
        arm = m.bone(side, [x, 6.5, 0], "body", locators={"hand": [x, 3, -1]} if side == "right_arm" else None)
        m.cube(arm, [x - 0.3, 3, -0.3], [0.6, 3.5, 0.6], "fairy")
    m.cube(m.bones[-2], [-2.1, 1.5, -1.5], [0.4, 1.8, 0.4], "iron")                            # its pliers
    m.cube(m.bones[-2], [-1.6, 1.5, -1.5], [0.4, 1.8, 0.4], "iron")
    tail = m.bone("legs", [0, 3, 0], "body")
    m.cube(tail, [-0.8, 0, -0.5], [0.5, 3, 0.5], "fairy")
    m.cube(tail, [0.3, 0, -0.5], [0.5, 3, 0.5], "fairy")
    buzz = f"60 * math.sin({T} * 3600)"
    fly = {"body": {"position": [0, f"0.8 * math.sin({T} * 360)", 0], "rotation": [10, 0, 0]},
           "right_wing": {"rotation": [0, 0, buzz]}, "left_wing": {"rotation": [0, 0, f"-({buzz})"]},
           "legs": {"rotation": [f"15 + 8 * math.sin({T} * 360)", 0, 0]}}
    a = {"idle": {"loop": True, "animation_length": 1.0, "bones": fly},
         "walk": {"loop": True, "animation_length": 1.0, "bones": dict(fly, body={"position": [0, f"0.6 * math.sin({T} * 360)", 0], "rotation": [30, 0, 0]})},
         "attack": {"loop": False, "animation_length": 0.3, "bones": {
             "right_arm": {"rotation": frames((0.0, [0, 0, 0]), (0.12, [-100, 0, 0]), (0.3, [0, 0, 0]))},
             "head": {"rotation": frames((0.0, [0, 0, 0]), (0.12, [20, 0, 0]), (0.3, [0, 0, 0]))}}}}
    return m, a, {}


def spider_legs(m, parent, pairs, y, reach, thick, material, z0, dz, prefix="", rise=36.0, fall=62.0, splay=None, bristle=None, claw="bone"):
    """A spider's legs, each a chain of joints so it stays joined however it bends: the upper leg from a hip at the
    body's side ({prefix}{side}_leg_{i}), rising `rise` degrees to a high knee; the lower leg ({prefix}{side}_foot_{i})
    hung on the knee, falling `fall` degrees below level to the ground, as long as it takes to reach it; a claw at its
    end. `splay` turns each pair forward (front pairs) or back (back pairs), in degrees. Bristles stand on the upper legs.
    A positive z turn leans a part's top toward +x, so the upper legs turn by -rise on the +x side, +rise on the -x."""
    hip, upper = reach
    # Long enough that the claw comes down on the ground (the knee sits a little lower than rise alone puts it, splayed).
    lower = (y + upper * math.sin(math.radians(rise)) * 0.86) / math.sin(math.radians(fall))
    for i in range(pairs):
        z = z0 + i * dz
        yaw = splay[i] if splay else 0.0
        for side, sx in (("right", -1), ("left", 1)):
            name = f"{prefix}{side}_leg_{i}"
            leg = m.bone(name, [hip * sx, y, z], parent, rotation=[0, yaw * sx, -rise * sx])
            m.cube(leg, [hip if sx > 0 else -hip - upper, y - thick / 2, z - thick / 2], [upper, thick, thick], material)
            if bristle:
                for t in (0.3, 0.6, 0.85):
                    bx = sx * (hip + upper * t)
                    m.cube(leg, [bx - 0.15, y + thick / 2, z - 0.15], [0.3, thick * 0.9, 0.3], bristle,
                           rotation=[0, 0, -25 * sx], pivot=[bx, y + thick / 2, z])     # bristles, raked back
            knee = sx * (hip + upper)
            foot = m.bone(f"{prefix}{side}_foot_{i}", [knee, y, z], name, rotation=[0, 0, (rise + fall) * sx])
            m.cube(foot, [knee if sx > 0 else knee - lower, y - thick * 0.42, z - thick * 0.42], [lower, thick * 0.84, thick * 0.84], material)
            tip = knee + sx * lower
            m.cube(foot, [tip if sx > 0 else tip - thick * 1.4, y - thick * 0.3, z - thick * 0.3], [thick * 1.4, thick * 0.6, thick * 0.6], claw)


def spider_walk(pairs, speed, swing, prefix=""):
    out = {}
    for i in range(pairs):
        for side, sx in (("right", -1), ("left", 1)):
            ph = i * (360 // pairs) + (0 if sx > 0 else 180 // pairs)
            out[f"{prefix}{side}_leg_{i}"] = {"rotation": [0, f"{swing} * math.sin({T} * {speed} + {ph})", f"{swing * 0.6 * sx} * math.max(0, math.sin({T} * {speed} + {ph} + 90))"]}
    return out


def broodmother():
    """The Broodmother: a spider the size of a cart, black and bristling, its legs high-kneed and hooked; a head of
    eight burning eyes in clusters over long fangs dripping green venom, feelers working in front of them; a swollen
    abdomen marked with a glowing red hourglass and crusted with egg sacs lit from within, some of them hatched, its
    young riding on its back; spinnerets trailing silk."""
    m = Model()
    m.bone("root", [0, 0, 0])
    body = m.bone("body", [0, 10.5, 0], "root")
    m.cube(body, [-5.5, 7.5, -9], [11, 7, 11], "brood")                                         # its thorax
    m.cube(body, [-4.5, 14.3, -8], [9, 1.2, 9], "brood_belly")                                  # the plate over it
    for x, z in ((-3.5, -6), (2.5, -6), (-3.5, -2), (2.5, -2)):
        m.cube(body, [x, 15.3, z], [1, 1.6, 1], "brood")                                        # bristle tufts
    head = m.bone("head", [0, 11, -9], "body")
    m.cube(head, [-4.5, 7, -15], [9, 7, 6], "brood_face")
    for x, y, big in ((-2.6, 11.6, 1), (1.4, 11.6, 1), (-3.8, 12.6, 0), (3.0, 12.6, 0), (-1.6, 13.1, 0), (0.8, 13.1, 0), (-3.4, 10.4, 0), (2.6, 10.4, 0)):
        size = 1.3 if big else 0.7
        m.cube(head, [x, y, -15.4], [size, size, 0.5], "eye")                                   # eight eyes, the middle pair big
    for side, x in (("right_fang", -2.2), ("left_fang", 2.2)):
        fang = m.bone(side, [x, 8.5, -15], "head")
        m.cube(fang, [x - 0.8, 4, -16.4], [1.6, 4.5, 1.6], "brood")
        m.cube(fang, [x - 0.5, 1.6, -16.1], [1, 2.6, 1], "bone")                                 # the fang
        m.cube(fang, [x - 0.2, 0.4, -15.9], [0.4, 1.4, 0.4], "venom")                            # venom dripping
    for side, x in (("right_palp", -3.3), ("left_palp", 3.3)):
        palp = m.bone(side, [x, 9, -15], "head", rotation=[20, 0, 0])
        m.cube(palp, [x - 0.5, 8.5, -19.5], [1, 1, 4.5], "brood")                                # feelers
        m.cube(palp, [x - 0.4, 6.5, -19.5], [0.8, 2, 0.8], "brood")
    abdomen = m.bone("abdomen", [0, 12, 2], "body")
    m.cube(abdomen, [-8, 6, 2], [16, 14, 20], "brood")
    m.cube(abdomen, [-6.5, 5, 4], [13, 2, 15], "brood_belly")
    for x, z, w, d in ((-2.2, 7, 4.4, 2), (-1, 9, 2, 2), (-2.2, 11, 4.4, 2)):
        m.cube(abdomen, [x, 20, z], [w, 0.4, d], "hourglass")                                   # the hourglass on its back
    for x, y, z, r in ((-8.8, 10, 6, 2.8), (6.2, 12, 9, 2.6), (-8.4, 15, 14, 2.4), (5.8, 8, 16, 2.6), (-7.6, 8.5, 18, 2.0), (6.6, 16, 5, 2.0), (-1.6, 19.6, 15.5, 2.4)):
        m.cube(abdomen, [x, y, z], [r, r, r], "egg_glow")                                       # egg sacs, lit from within
    m.cube(abdomen, [-1.6, 9.5, 21.5], [3.2, 3.2, 2.2], "brood_belly")                          # spinnerets
    m.cube(abdomen, [-0.6, 10.5, 23.4], [1.2, 1.2, 1.6], "brood_belly")
    for x, length in ((-0.6, 9), (0.4, 7)):
        m.cube(abdomen, [x, 10.8 - length, 24.2], [0.2, length, 0.2], "string")                 # silk trailing down
    for x, z in ((-4.5, 5), (2.8, 11), (-3, 15)):
        m.cube(abdomen, [x, 20, z], [1.6, 0.9, 1.8], "egg")                                     # its young, riding on it
        m.cube(abdomen, [x + 0.5, 20.9, z - 0.1], [0.4, 0.3, 0.3], "eye")
    # High-kneed: the knees arch up past its back, as a big spider's do.
    spider_legs(m, "body", 4, 10.5, (5.5, 14), 1.6, "brood", -7.5, 3.2, rise=54, fall=66, splay=[36, 12, -12, -36], bristle="brood")
    twitch = f"math.max(0, math.sin({T} * 160) - 0.8) * 5"
    a = {
        "idle": {"loop": True, "animation_length": 4.0, "bones": {
            "abdomen": {"rotation": [f"2 * math.sin({T} * 90)", 0, 0], "scale": [f"1 + 0.04 * math.sin({T} * 180)", f"1 + 0.03 * math.sin({T} * 180)", 1]},
            "head": {"rotation": [f"3 * math.sin({T} * 45)", f"6 * math.sin({T} * 30)", 0]},
            "right_fang": {"rotation": [f"6 * {twitch}", 0, 0]}, "left_fang": {"rotation": [f"6 * {twitch}", 0, 0]},
            "right_palp": {"rotation": [f"10 * math.sin({T} * 360)", 0, 0]}, "left_palp": {"rotation": [f"10 * math.sin({T} * 360 + 180)", 0, 0]},
            "right_leg_0": {"rotation": [0, 0, f"-10 * {twitch}"]}, "left_leg_0": {"rotation": [0, 0, f"10 * {twitch}"]}}},
        "walk": {"loop": True, "animation_length": 0.8, "bones": dict(spider_walk(4, 450, 20),
            body={"position": [0, f"0.4 * math.abs(math.sin({T} * 900))", 0]}, abdomen={"rotation": [f"4 * math.sin({T} * 450)", f"4 * math.sin({T} * 225)", 0]},
            right_palp={"rotation": [f"15 * math.sin({T} * 900)", 0, 0]}, left_palp={"rotation": [f"15 * math.sin({T} * 900 + 180)", 0, 0]})},
        # It rears, its front legs high, and strikes down with its fangs.
        "attack": {"loop": False, "animation_length": 0.6, "bones": {
            "body": {"rotation": frames((0.0, [0, 0, 0]), (0.25, [-22, 0, 0]), (0.4, [8, 0, 0]), (0.6, [0, 0, 0]))},
            "right_leg_0": {"rotation": frames((0.0, [0, 0, 0]), (0.25, [0, 0, -35]), (0.4, [0, 0, 10]), (0.6, [0, 0, 0]))},
            "left_leg_0": {"rotation": frames((0.0, [0, 0, 0]), (0.25, [0, 0, 35]), (0.4, [0, 0, -10]), (0.6, [0, 0, 0]))},
            "right_fang": {"rotation": frames((0.0, [0, 0, 0]), (0.25, [-30, 0, -15]), (0.4, [25, 0, 5]), (0.6, [0, 0, 0]))},
            "left_fang": {"rotation": frames((0.0, [0, 0, 0]), (0.25, [-30, 0, 15]), (0.4, [25, 0, -5]), (0.6, [0, 0, 0]))},
            "abdomen": {"rotation": frames((0.0, [0, 0, 0]), (0.25, [20, 0, 0]), (0.6, [0, 0, 0]))}}},
    }
    return m, a, {"look_at": {"head": {"max_yaw": 25, "max_pitch": 15, "smoothing": 0.15}}}


def broodling():
    """A Broodling: a spider no bigger than a hand, hatched from the Broodmother's sacs, pale and quick."""
    m = Model()
    m.bone("root", [0, 0, 0])
    body = m.bone("body", [0, 2.5, 0], "root")
    m.cube(body, [-1.2, 1.5, -2], [2.4, 2, 2], "brood_belly")
    m.cube(body, [-1.6, 1.6, 0], [3.2, 2.6, 3.4], "egg")
    head = m.bone("head", [0, 2.5, -2], "body")
    m.cube(head, [-1, 1.6, -3.4], [2, 1.6, 1.4], "brood_face")
    spider_legs(m, "body", 4, 2.5, (1.1, 2.6), 0.4, "brood", -1.5, 1.0, rise=40, fall=66, splay=[30, 10, -10, -30], claw="brood")
    a = {"idle": {"loop": True, "animation_length": 1.0, "bones": {"body": {"position": [0, f"0.1 * math.sin({T} * 360)", 0]}}},
         "walk": {"loop": True, "animation_length": 0.3, "bones": spider_walk(4, 1200, 25)},
         "attack": {"loop": False, "animation_length": 0.25, "bones": {"head": {"rotation": frames((0.0, [0, 0, 0]), (0.1, [-25, 0, 0]), (0.25, [0, 0, 0]))}}}}
    return m, a, {}


def void_eel():
    """A Void Eel: a serpent longer than a house that swims through the void under the islands, dark as the void
    itself so only its rows of pale lights show, its jaw lined with needle teeth, fins along its back."""
    m = Model()
    m.bone("root", [0, 0, 0])
    head = m.bone("head", [0, 10, -8], "root")
    m.cube(head, [-4, 6, -18], [8, 7, 10], "eel")
    m.cube(head, [-3.6, 6.4, -18.4], [7.2, 1, 0.4], "eel_face")
    m.cube(head, [-3.5, 8, -18.6], [7, 4, 0.6], "eel_face")
    jaw = m.bone("jaw", [0, 7, -9], "head")
    m.cube(jaw, [-3.6, 4.5, -17.6], [7.2, 2, 9], "eel")
    for x in (-3, 2.4):
        m.cube(jaw, [x, 6.4, -17.4], [0.6, 1.2, 0.6], "bone")                                    # fangs
    m.cube(head, [-0.5, 13, -14], [1, 3, 5], "eel_light")                                       # a lure on its brow
    parent, z = "head", -8
    for i in range(6):
        w = 7 - i * 0.9
        seg = m.bone(f"segment_{i}", [0, 10, z], parent)
        m.cube(seg, [-w / 2, 10 - w / 2, z], [w, w, 10], "eel")
        m.cube(seg, [-0.4, 10 + w / 2, z + 1], [0.8, 2.5 - i * 0.3, 8], "brood_belly")          # a fin along its back
        for sx in (-1, 1):
            m.cube(seg, [sx * w / 2 - (0.2 if sx > 0 else 0.0), 9.6, z + 3], [0.2, 0.8, 0.8], "eel_light")   # its lights
            m.cube(seg, [sx * w / 2 - (0.2 if sx > 0 else 0.0), 9.6, z + 7], [0.2, 0.8, 0.8], "eel_light")
        parent, z = f"segment_{i}", z + 10
    m.cube(m.bones[-1], [-0.3, 8, z - 2], [0.6, 4, 6], "eel_light")                             # its tail fin
    wave = {f"segment_{i}": {"rotation": [f"4 * math.sin({T} * 180 - {i * 40})", f"14 * math.sin({T} * 180 - {i * 50})", 0]} for i in range(6)}
    a = {"idle": {"loop": True, "animation_length": 2.0, "bones": dict(wave, head={"rotation": [0, f"8 * math.sin({T} * 180 + 50)", 0]},
                                                                          jaw={"rotation": [f"6 + 4 * math.sin({T} * 180)", 0, 0]})},
         "walk": {"loop": True, "animation_length": 1.2, "bones": dict(
             {f"segment_{i}": {"rotation": [f"6 * math.sin({T} * 300 - {i * 40})", f"20 * math.sin({T} * 300 - {i * 50})", 0]} for i in range(6)},
             head={"rotation": [0, f"10 * math.sin({T} * 300 + 50)", 0]}, jaw={"rotation": [10, 0, 0]})},
         # The lunge: mouth wide, body straight behind it.
         "lunge": {"loop": True, "animation_length": 0.5, "bones": dict(
             {f"segment_{i}": {"rotation": [0, f"4 * math.sin({T} * 720 - {i * 60})", 0]} for i in range(6)},
             jaw={"rotation": [55, 0, 0]}, head={"rotation": [-15, 0, 0]})},
         "attack": {"loop": False, "animation_length": 0.3, "bones": {"jaw": {"rotation": frames((0.0, [55, 0, 0]), (0.12, [0, 0, 0]), (0.3, [10, 0, 0]))}}}}
    return m, a, {}


def hive_thing():
    """A Hive-Thing: a lumbering heap of a body pocked all over with holes, things moving in them, one eye deep among
    the holes of its face, long arms dragging; insects drift round it."""
    m = Model()
    m.bone("root", [0, 0, 0])
    legs(m, "root", (-2.2, 2.2), 10, "hive", "hive", thick=3.6)
    body = m.bone("body", [0, 10, 0], "root")
    m.cube(body, [-5, 10, -3.5], [10, 12, 7], "hive")
    m.cube(body, [-4, 18, 2.5], [8, 6, 3], "hive", inflate=0.3)                                 # a hump, riddled
    m.cube(body, [2, 12, -4.2], [3, 4, 1.2], "hive", inflate=0.2)                               # growths
    m.cube(body, [-5.5, 16, -2], [2, 3, 3], "hive", inflate=0.2)
    head = m.bone("head", [0, 22, -1], "body")
    m.cube(head, [-3.5, 21.5, -5], [7, 7, 7], "hive_face")
    m.cube(head, [-2.5, 28.5, -3], [4, 2, 4], "hive", inflate=0.2)
    for side, x in (("right_arm", -6.3), ("left_arm", 6.3)):
        arm = m.bone(side, [x, 21, 0], "body")
        m.cube(arm, [x - 1.5, 11, -1.5], [3, 10, 3], "hive")
        fore = m.bone(side.replace("arm", "forearm"), [x, 11, 0], side)
        m.cube(fore, [x - 1.3, 3, -1.3], [2.6, 8, 2.6], "hive")
    swarm = m.bone("swarm", [0, 20, 0], "body")
    for i in range(10):
        ang = i * 2.4
        r = 6.5 + (i % 3)
        m.cube(swarm, [round(math.cos(ang) * r, 2), 14 + (i * 7) % 14, round(math.sin(ang) * r, 2)], [0.4, 0.4, 0.4], "shadow")   # insects
    a = {
        "idle": {"loop": True, "animation_length": 3.0, "bones": {
            "body": {"rotation": [12, 0, f"4 * math.sin({T} * 120)"]}, "head": {"rotation": [f"8 * math.sin({T} * 60)", 0, f"10 * math.sin({T} * 120)"]},
            "right_arm": {"rotation": [f"6 * math.sin({T} * 120)", 0, 6]}, "left_arm": {"rotation": [f"-6 * math.sin({T} * 120)", 0, -6]},
            "swarm": {"rotation": [0, f"{T} * 240", 0], "position": [0, f"1.5 * math.sin({T} * 240)", 0]}}},
        "walk": {"loop": True, "animation_length": 1.6, "bones": dict(humanoid_walk(swing=10, stride=24),
            body={"rotation": [16, 0, f"7 * math.sin({T} * 225)"]}, swarm={"rotation": [0, f"{T} * 240", 0]})},
        # It shudders, and the swarm pours out.
        "attack": {"loop": False, "animation_length": 0.8, "bones": {
            "body": {"rotation": frames((0.0, [12, 0, 0]), (0.2, [-15, 0, 0]), (0.5, [20, 0, 0]), (0.8, [12, 0, 0]))},
            "right_arm": {"rotation": frames((0.0, [0, 0, 6]), (0.2, [-40, 0, 50]), (0.8, [0, 0, 6]))},
            "left_arm": {"rotation": frames((0.0, [0, 0, -6]), (0.2, [-40, 0, -50]), (0.8, [0, 0, -6]))},
            "swarm": {"scale": frames((0.0, [1, 1, 1]), (0.4, [2.2, 1.6, 2.2]), (0.8, [1, 1, 1]))}}},
    }
    return m, a, {}


def crawlspace():
    """The Crawlspace: a thing pressed flat, a grey stretched skin with a face too wide in it, white eyes set far apart,
    a mouth from side to side, and four long, many-jointed limbs splayed out flat around it."""
    m = Model()
    m.bone("root", [0, 0, 0])
    body = m.bone("body", [0, 2, 0], "root")
    m.cube(body, [-5, 0.5, -6], [10, 3, 14], "crawl")
    head = m.bone("head", [0, 2, -6], "body")
    m.cube(head, [-5.5, 0.4, -11], [11, 3, 5], "crawl_face")
    for side, sx, sz in (("front_right", -1, -4), ("front_left", 1, -4), ("back_right", -1, 5), ("back_left", 1, 5)):
        limb = m.bone(f"{side}_limb", [5 * sx, 1.5, sz], "body")
        x0 = 5 if sx > 0 else -15
        m.cube(limb, [x0, 0.8, sz - 0.6], [10, 1.2, 1.2], "crawl")
        tip = m.bone(f"{side}_hand", [15 * sx, 1.5, sz], f"{side}_limb")
        tx0 = 15 if sx > 0 else -23
        m.cube(tip, [tx0, 0.7, sz - 0.5], [8, 1, 1], "crawl")
        for i in range(3):
            m.cube(tip, [23 if sx > 0 else -24.5, 0.6, sz - 1.1 + i * 0.8], [1.5, 0.5, 0.4], "bone")      # long fingers at its hand's end
    a = {
        "idle": {"loop": True, "animation_length": 4.0, "bones": {
            "body": {"scale": [1, f"1 + 0.08 * math.sin({T} * 90)", 1]},
            "head": {"rotation": [0, f"6 * math.sin({T} * 45)", 0]},
            "front_right_hand": {"rotation": [0, f"8 * math.max(0, math.sin({T} * 90) - 0.6) * 4", 0]}}},
        # It skitters flat, limbs reaching in turn.
        "walk": {"loop": True, "animation_length": 0.6, "bones": {
            "front_right_limb": {"rotation": [0, f"25 * math.sin({T} * 600)", 0]}, "back_left_limb": {"rotation": [0, f"25 * math.sin({T} * 600)", 0]},
            "front_left_limb": {"rotation": [0, f"-25 * math.sin({T} * 600)", 0]}, "back_right_limb": {"rotation": [0, f"-25 * math.sin({T} * 600)", 0]},
            "body": {"rotation": [0, f"6 * math.sin({T} * 600)", 0]}}},
        # Holding someone: limbs closed in round them, its body heaving.
        "grab": {"loop": True, "animation_length": 0.8, "bones": {
            "front_right_limb": {"rotation": [0, 0, -50]}, "front_left_limb": {"rotation": [0, 0, 50]},
            "back_right_limb": {"rotation": [0, 0, -40]}, "back_left_limb": {"rotation": [0, 0, 40]},
            "front_right_hand": {"rotation": [0, 0, -60]}, "front_left_hand": {"rotation": [0, 0, 60]},
            "body": {"scale": [1, f"1.2 + 0.2 * math.sin({T} * 450)", 1]}, "head": {"rotation": [-20, 0, 0]}}},
        "attack": {"loop": False, "animation_length": 0.4, "bones": {
            "head": {"rotation": frames((0.0, [0, 0, 0]), (0.15, [-30, 0, 0]), (0.4, [0, 0, 0]))}}},
    }
    return m, a, {}


def clown_legs_body(m, trousers, shoe, shirt, top=13, shoes=True):
    """A clown's legs and body to its shoulders (the bones legs(), body), big shoes on its feet."""
    legs(m, "root", (-1.8, 1.8), top, trousers, shoe, thick=3.0)
    if shoes:
        for b in m.bones:
            if b["name"] in ("right_shin", "left_shin"):
                x = b["pivot"][0]
                m.cube(b, [x - 2, 0, -6], [4, 2.2, 7.5], shoe)
    body = m.bone("body", [0, top, 0], "root", locators={"body_ball": [0, top + 5.5, 0], "ruff_ball": [0, top + 10, 0]})
    m.cube(body, [-4, top, -2.5], [8, 10, 5], shirt)
    return body


def clown_arms(m, sleeve, glove, x=5.2, top=22.5):
    for side, sx in (("right_arm", -x), ("left_arm", x)):
        arm = m.bone(side, [sx, top, 0], "body")
        m.cube(arm, [sx - 1.25, top - 7.5, -1.25], [2.5, 7.5, 2.5], sleeve)
        fore = m.bone(side.replace("arm", "forearm"), [sx, top - 7.5, 0], side)
        m.cube(fore, [sx - 1.2, top - 13, -1.2], [2.4, 5.5, 2.4], glove)


def mime_clown():
    """The Mime: a clown gone silent, in a black-and-white striped shirt and black trousers with red braces, a black
    beret tipped on its head, white gloves; its face white paint, black brows arched, a painted tear. It makes no sound."""
    m = Model()
    m.bone("root", [0, 0, 0])
    body = clown_legs_body(m, "suit", "suit", "mime_stripes", shoes=False)
    for x in (-2.6, 1.8):
        m.cube(body, [x, 13.5, -2.8], [0.8, 9.5, 0.4], "shawl")                                 # braces
    head = m.bone("head", [0, 23, 0], "body", locators={"head_ball": [0, 26.8, 0]})
    m.cube(head, [-3.5, 23, -3.5], [7, 7.5, 7], "mime_face")
    m.cube(head, [-4.2, 30.3, -4.2], [8.4, 1.2, 8.4], "suit", rotation=[0, 0, 10], pivot=[0, 30.5, 0])   # its beret, tipped
    m.cube(head, [-0.5, 31.4, -0.5], [1, 0.8, 1], "suit")
    clown_arms(m, "mime_stripes", "ruff")
    walls = {"right_arm": {"rotation": [-95, 0, 10]}, "left_arm": {"rotation": [-95, 0, -10]},
             "right_forearm": {"rotation": [-10, 0, 0]}, "left_forearm": {"rotation": [-10, 0, 0]}}
    a = {
        # Feeling along a wall that isn't there.
        "idle": {"loop": True, "animation_length": 3.0, "bones": dict(walls,
            right_arm={"rotation": [f"-95 + 6 * math.sin({T} * 120)", f"8 * math.sin({T} * 120)", 10]},
            left_arm={"rotation": [f"-95 + 6 * math.sin({T} * 120 + 180)", f"8 * math.sin({T} * 120 + 180)", -10]},
            head={"rotation": [0, 0, f"12 * math.sin({T} * 60)"]})},
        # Tiptoeing, exaggerated.
        "walk": {"loop": True, "animation_length": 1.0, "bones": dict(humanoid_walk(swing=10, stride=34),
            body={"position": [0, f"1.4 * math.abs(math.sin({T} * 360))", 0], "rotation": [6, 0, 0]},
            right_arm={"rotation": [-60, 0, 30]}, left_arm={"rotation": [-60, 0, -30]})},
        "attack": {"loop": False, "animation_length": 0.4, "bones": {
            "right_arm": {"rotation": frames((0.0, [-20, 0, 10]), (0.12, [-90, -60, 60]), (0.25, [-80, 50, -20]), (0.4, [-20, 0, 10]))}}},
        # Pressing on the walls of the box it has shut someone in.
        "box": {"loop": True, "animation_length": 1.2, "bones": {
            "right_arm": {"rotation": [-20, 0, f"75 + 6 * math.sin({T} * 300)"]}, "left_arm": {"rotation": [-20, 0, f"-75 - 6 * math.sin({T} * 300 + 90)"]},
            "right_forearm": {"rotation": [0, 0, 20]}, "left_forearm": {"rotation": [0, 0, -20]},
            "head": {"rotation": [-10, 0, f"10 * math.sin({T} * 150)"]}, "body": {"rotation": [0, 0, f"4 * math.sin({T} * 300)"]}}},
    }
    return m, a, {"look_at": {"head": {"max_yaw": 60, "max_pitch": 30, "smoothing": 0.2}}}


def jack_in_the_box():
    """A Jack-in-the-Box: a toy box painted red with gold trim and stars, a crank in its side. Shut, it is only a toy;
    open, a clown springs out of it on a coil, in a jester's cap with bells, grinning."""
    m = Model()
    m.bone("root", [0, 0, 0])
    box = m.bone("box", [0, 0, 0], "root")
    m.cube(box, [-6, 0, -6], [12, 11, 12], "toybox")
    crank = m.bone("crank", [6, 6, 0], "box")
    m.cube(crank, [6, 5.6, -0.4], [2, 0.8, 0.8], "iron")
    m.cube(crank, [7.6, 5.6, -0.4], [0.8, 0.8, 3], "iron")
    m.cube(crank, [7.5, 5.4, 2.4], [1, 1.2, 1.2], "shawl")                                      # its handle
    lid = m.bone("lid", [0, 11, 6], "box")
    m.cube(lid, [-6.3, 11, -6.3], [12.6, 1.2, 12.6], "toybox")
    coil = m.bone("coil", [0, 8, 0], "box")
    for i in range(5):
        m.cube(coil, [-2, 8.5 + i * 2.2, -2], [4, 0.6, 4], "iron")                               # the spring
    m.cube(coil, [-0.4, 8.5, -0.4], [0.8, 10.5, 0.8], "iron")
    torso = m.bone("torso", [0, 19, 0], "coil")
    m.cube(torso, [-3.5, 19, -2.5], [7, 7, 5], "harlequin")
    m.cube(torso, [-4.5, 25.4, -3.5], [9, 1.6, 7], "ruff", inflate=0.1)
    for side, x in (("right_arm", -4.9), ("left_arm", 4.9)):                # out past its head's sides, so raised they clear it
        arm = m.bone(side, [x, 25, 0], "torso")
        m.cube(arm, [x - 1, 19, -1], [2, 6, 2], "harlequin")
        m.cube(arm, [x - 1.1, 17.4, -1.1], [2.2, 1.8, 2.2], "ruff")
    head = m.bone("head", [0, 27, 0], "torso")
    m.cube(head, [-3.5, 27, -3.5], [7, 7, 7], "clown_face")
    m.cube(head, [-1, 29.5, -4.7], [2, 2, 1.3], "nose")
    m.cube(head, [-3.6, 33.6, -3.6], [7.2, 1.4, 7.2], "harlequin")                               # its cap
    for side, sx in (("right_horn", -1), ("left_horn", 1)):
        horn = m.bone(side, [2 * sx, 35, 0], "head", rotation=[0, 0, -40 * sx])
        m.cube(horn, [2 * sx - (0 if sx > 0 else 4), 34.4, -1], [4, 2, 2], "harlequin")         # floppy points
        m.cube(horn, [6 * sx - (0 if sx > 0 else 1.4), 34, -0.7], [1.4, 1.4, 1.4], "pom")       # bells
    shut = {"lid": {"rotation": [0, 0, 0]}, "coil": {"scale": [1, 0.06, 1]}, "crank": {"rotation": [f"{T} * 90", 0, 0]}}
    out = {"lid": {"rotation": [-115, 0, 0]}}
    a = {
        # Shut: the crank turns, slowly.
        "hide": {"loop": True, "animation_length": 4.0, "bones": shut},
        # Out of the box with a bang: the lid flung back, the coil overshooting and settling.
        "pop": {"loop": False, "animation_length": 0.8, "bones": {
            "lid": {"rotation": frames((0.0, [0, 0, 0]), (0.1, [-130, 0, 0]), (0.3, [-110, 0, 0]), (0.8, [-115, 0, 0]))},
            "coil": {"scale": frames((0.0, [1, 0.06, 1]), (0.12, [1, 1.35, 1]), (0.3, [1, 0.85, 1]), (0.5, [1, 1.1, 1]), (0.8, [1, 1, 1]))},
            "head": {"rotation": frames((0.0, [0, 0, 0]), (0.15, [-25, 0, 0]), (0.4, [12, 0, 0]), (0.8, [0, 0, 0]))},     # a nod: no tilt into its arms
            "right_arm": {"rotation": frames((0.0, [0, 0, 0]), (0.15, [-160, 0, -26]), (0.45, [-90, 0, -22]), (0.8, [-30, 0, 20]))},
            "left_arm": {"rotation": frames((0.0, [0, 0, 0]), (0.15, [-160, 0, 26]), (0.45, [-90, 0, 22]), (0.8, [-30, 0, -20]))}}},
        "idle": {"loop": True, "animation_length": 2.0, "bones": dict(out,
            coil={"scale": [1, f"1 + 0.06 * math.sin({T} * 360)", 1]}, crank={"rotation": [f"{T} * 180", 0, 0]},
            head={"rotation": [0, f"15 * math.sin({T} * 90)", f"10 * math.sin({T} * 180)"]},
            right_arm={"rotation": [-30, 0, f"20 + 10 * math.sin({T} * 360)"]}, left_arm={"rotation": [-30, 0, f"-20 - 10 * math.sin({T} * 360)"]})},
        # It gets about in hops, its box and all.
        "walk": {"loop": True, "animation_length": 0.6, "bones": dict(out,
            root={"position": [0, f"3 * math.abs(math.sin({T} * 300))", 0]},
            coil={"scale": [1, f"1 - 0.2 * math.abs(math.sin({T} * 300 + 90))", 1]},
            right_arm={"rotation": [-140, 0, -28]}, left_arm={"rotation": [-140, 0, 28]})},
        "attack": {"loop": False, "animation_length": 0.4, "bones": {
            "torso": {"rotation": frames((0.0, [0, 0, 0]), (0.15, [35, 0, 0]), (0.4, [0, 0, 0]))},
            "right_arm": {"rotation": frames((0.0, [-30, 0, 20]), (0.15, [-150, 0, -20]), (0.4, [-30, 0, 20]))},
            "left_arm": {"rotation": frames((0.0, [-30, 0, -20]), (0.15, [-150, 0, 20]), (0.4, [-30, 0, -20]))}}},
    }
    # Its body wobbles on the coil (a spring), as it hops and as it pops.
    return m, a, {"springs": {"torso": {"stiffness": 70, "damping": 5, "max_angle": 35}}}


def balloon_clown():
    """The Balloon Clown: a clown in candy stripes with a great rainbow afro and a glowing nose, carried up off its feet
    by a bunch of five balloons held in its raised fist, its legs dangling."""
    m = Model()
    m.bone("root", [0, 0, 0])
    body = clown_legs_body(m, "candy", "shoe", "candy")
    for i in range(3):
        m.cube(body, [-0.6, 15 + i * 2.5, -2.9], [1.2, 1.2, 0.5], "pom")
    m.cube(body, [-5, 22, -3.5], [10, 2, 7], "ruff", inflate=0.2)
    head = m.bone("head", [0, 24, 0], "body", locators={"head_ball": [0, 28.5, 0]})
    m.cube(head, [-3.5, 24, -3.5], [7, 7.5, 7], "clown_face")
    m.cube(head, [-1, 26.5, -4.7], [2, 2, 1.3], "nose")
    m.cube(head, [-6, 27, -3], [3, 6, 7], "afro", inflate=0.2)                                  # its rainbow afro
    m.cube(head, [3, 27, -3], [3, 6, 7], "afro", inflate=0.2)
    m.cube(head, [-4.5, 31, -4], [9, 3.5, 8.5], "afro", inflate=0.2)
    clown_arms(m, "candy", "ruff")
    hand = 9.4
    colours = (("balloon_red", 15, 4.2), ("balloon_cyan", 13, 3.8), ("balloon_yellow", 16.5, 3.8), ("balloon_green", 12, 3.6), ("balloon_violet", 14.2, 3.6))
    chains = []
    for i, (name, length, size) in enumerate(colours):
        # A bunch, spread at rest; turned over (180 about x), since its fist is always raised over its head: so they
        # start up above it when it appears, rather than hanging from it and slowly floating round.
        spread = [[170, 0, -16], [192, 0, 6], [180, 0, 0], [174, 0, 18], [194, 0, -10]][i]
        chains.append(balloon(m, name, "left_forearm", 5.2, hand, length, size, name, spread,
                              locators={name + "_at": [5.2, hand + length + size * 0.57, 0]}))
    up = {"left_arm": {"rotation": [-170, 0, 28]}, "left_forearm": {"rotation": [-6, 0, 0]}}       # up and out, clear of its afro
    dangle = {"right_leg": {"rotation": [f"10 * math.sin({T} * 120)", 0, 4]}, "left_leg": {"rotation": [f"-8 * math.sin({T} * 120)", 0, -4]},
              "right_shin": {"rotation": [f"12 + 8 * math.sin({T} * 120 + 60)", 0, 0]}, "left_shin": {"rotation": [f"10 + 8 * math.sin({T} * 120)", 0, 0]}}
    a = {
        "idle": {"loop": True, "animation_length": 3.0, "bones": dict(up, **dangle,
            body={"rotation": [0, 0, f"5 * math.sin({T} * 120)"]}, head={"rotation": [8, f"12 * math.sin({T} * 60)", 0]},
            right_arm={"rotation": [f"-20 + 10 * math.sin({T} * 120)", 0, 20]})},
        "walk": {"loop": True, "animation_length": 1.5, "bones": dict(up, **dangle,
            body={"rotation": [12, 0, f"6 * math.sin({T} * 240)"]}, right_arm={"rotation": [-60, 0, 30]})},
        # It reaches down and ties a balloon on someone.
        "attack": {"loop": False, "animation_length": 0.6, "bones": dict(up,
            right_arm={"rotation": frames((0.0, [-20, 0, 20]), (0.25, [-110, 0, 0]), (0.45, [-60, 0, 10]), (0.6, [-20, 0, 20]))})},
    }
    springs = {}
    strings = [{"point": "head_ball", "radius": 8.0}, {"point": "body_ball", "radius": 7.4}, {"point": "ruff_ball", "radius": 6.6}]
    for i, (name, length, size) in enumerate(colours):
        # Each balloon's ball its corners' reach; the five jostle each other (bump).
        springs.update(balloon_springs(chains[i], length, size, round(4.5 - i * 0.3, 2), round(2.1 + i * 0.13, 2), 1.0, 0.6, strings,
                                       [{"point": "head_ball", "radius": 7.6 + size * 0.76}, {"point": "body_ball", "radius": 7.0 + size * 0.76},
                                        {"point": "ruff_ball", "radius": 6.2 + size * 0.76}]))
    return m, a, {"look_at": {"head": {"max_yaw": 60, "max_pitch": 30, "smoothing": 0.25}}, "springs": springs}


def funhouse_clown():
    """The Funhouse Clown: a ringmaster of the hall of mirrors, in purple and green checks under a tall black hat with a
    red band, a cane in its hand, and two faces in one, laughing on one side and weeping on the other."""
    m = Model()
    m.bone("root", [0, 0, 0])
    body = clown_legs_body(m, "funhouse", "suit", "funhouse")
    m.cube(body, [-4.4, 13, -2.9], [8.8, 3, 5.8], "funhouse", inflate=0.1)                      # its coat's skirt
    m.cube(body, [-5, 22, -3.5], [10, 2, 7], "ruff", inflate=0.2)
    head = m.bone("head", [0, 24, 0], "body", locators={"head_ball": [0, 28, 0]})
    m.cube(head, [-4, 24, -3.5], [8, 8, 7], "twoface_face")
    m.cube(head, [-4.8, 32, -4.5], [9.6, 0.8, 9], "suit")                                       # its top hat's brim
    m.cube(head, [-3, 32.8, -3], [6, 6, 6], "suit")
    m.cube(head, [-3.1, 33.2, -3.1], [6.2, 1.2, 6.2], "shawl")                                  # the band
    clown_arms(m, "funhouse", "ruff")
    # Its cane, topped with a cracked funhouse mirror in a brass frame instead of a knob.
    cane = m.bone("cane", [-5.2, 9.6, 0], "right_forearm")
    m.cube(cane, [-5.6, 0.6, -0.4], [0.8, 9, 0.8], "suit")                                     # long enough to reach the ground,
    m.cube(cane, [-5.7, 0.05, -0.5], [1, 0.6, 1], "bone")                                      # its tip just on it, never in it
    m.cube(cane, [-7.2, 9.6, -0.5], [4, 4.6, 1], "brass")                                       # the mirror's frame
    m.cube(cane, [-6.7, 10.1, -0.7], [3, 3.6, 0.4], "fairy_wing")                               # its glass
    a = {
        "idle": {"loop": True, "animation_length": 3.0, "bones": {
            "head": {"rotation": [0, f"20 * math.sin({T} * 60)", f"10 * math.sin({T} * 120)"]},
            "right_arm": {"rotation": [-10, 0, 8]}, "right_forearm": {"rotation": [-20, 0, 0]},
            "cane": {"rotation": [0, f"{T} * 120", 0]}, "left_arm": {"rotation": [-5, 0, -8]}}},
        "walk": {"loop": True, "animation_length": 0.8, "bones": dict(humanoid_walk(swing=30, stride=38),
            body={"position": [0, f"1.0 * math.abs(math.sin({T} * 450))", 0]}, right_forearm={"rotation": [-25, 0, 0]})},
        "attack": {"loop": False, "animation_length": 0.5, "bones": {
            "right_arm": {"rotation": frames((0.0, [-10, 0, 8]), (0.2, [-150, 0, -32]), (0.32, [-40, 30, -10]), (0.5, [-10, 0, 8]))}}},
        # Shuffling with its copies: spinning, arms flung out.
        "shuffle": {"loop": True, "animation_length": 0.6, "bones": {
            "root": {"rotation": [0, f"{T} * 600", 0]}, "right_arm": {"rotation": [-20, 0, 80]}, "left_arm": {"rotation": [-20, 0, -80]},
            "head": {"rotation": [-15, 0, 0]}}},
    }
    return m, a, {"look_at": {"head": {"max_yaw": 60, "max_pitch": 30, "smoothing": 0.25}}}


def little_red():
    """Little Red Riding Hood: a small girl in a red hooded cape, a white blouse, a dark bodice and a blue skirt, little
    boots, fair braids, a wicker basket on her arm with a gingham cloth and a loaf in it. Her hood hangs down over her
    face; when someone comes near she lifts it back (the hood a joint, turned back over her shoulders in her open
    clips), and her face is a sweet one. Her cape swings (a spring)."""
    m = Model()
    m.bone("root", [0, 0, 0])
    legs(m, "root", (-1.1, 1.1), 7, "blouse", "boot", thick=2.0)
    body = m.bone("body", [0, 7, 0], "root", locators={"body_ball": [0, 9.5, 0]})
    m.cube(body, [-2.4, 7, -1.5], [4.8, 5.4, 3], "blouse")
    m.cube(body, [-2.55, 7, -1.65], [5.1, 3.2, 3.3], "bodice")                                  # her bodice, laced
    m.cube(body, [-2.3, 10.6, -1.8], [4.6, 1.4, 3.6], "cape_red")                               # her capelet, over her shoulders (under her chin)
    m.cube(body, [-0.5, 10.4, -2.0], [1.0, 0.8, 0.4], "gingham")                                # its clasp, at her throat
    body["locators"]["skirt_ball"] = [0, 4.5, 0]
    head = m.bone("head", [0, 12.4, 0], "body")
    m.cube(head, [-3, 12.4, -3], [6, 6, 6], "red_face")
    m.cube(head, [-3.1, 17.1, -3.1], [6.2, 1.4, 0.5], "hair_fair")                             # her fringe
    for sx in (-1, 1):
        # Her braids: from the sides of her head, beside her face (a lock down each temple joining them to her fringe),
        # hanging down over the fronts of her shoulders, a red ribbon at each end.
        m.cube(head, [3.0 if sx > 0 else -3.3, 13.6, -2.7], [0.3, 4.2, 1.3], "hair_fair")             # the lock at her temple
        m.cube(head, [3.15 if sx > 0 else -4.15, 9.6, -2.5], [1.0, 5.2, 1.0], "hair_fair")            # the braid
        m.cube(head, [3.05 if sx > 0 else -4.25, 9.1, -2.6], [1.2, 0.6, 1.2], "cape_red")             # its ribbon
    hood = m.bone("hood", [0, 12.6, 3.6], "head")                                           # hinged at her nape
    m.cube(hood, [-3.5, 18.4, -3.6], [7, 1, 7.2], "cape_red")                                   # its crown
    m.cube(hood, [-3.6, 13.3, -3.6], [0.5, 5.1, 7.2], "cape_red")                               # its sides (kept above her shoulders)
    m.cube(hood, [3.1, 13.3, -3.6], [0.5, 5.1, 7.2], "cape_red")
    m.cube(hood, [-3.5, 12.4, 3.1], [7, 6.1, 0.5], "cape_red")                                  # its back
    m.cube(hood, [-3.5, 17.4, -3.4], [7, 1.0, 0.35], "cape_red")                                # its brim, shading her brow
    m.cube(hood, [-3.5, 13.3, -3.4], [0.9, 4.1, 0.35], "cape_red")                              # its edges, over her cheeks
    m.cube(hood, [2.6, 13.3, -3.4], [0.9, 4.1, 0.35], "cape_red")
    veil = m.bone("veil", [0, 19.4, -3.85], "hood")                                            # hinged at the hood's brow
    m.cube(veil, [-3.2, 14.4, -3.85], [6.4, 4.0, 0.4], "cape_red")                              # hanging down over her eyes, her smile showing
    m.cube(hood, [-1, 17.5, 3.4], [2, 1.2, 0.6], "cape_red")                                    # the root of its point (a tail, below)
    for side, x in (("right_arm", -3.5), ("left_arm", 3.5)):                                   # clear of her bodice and skirt
        arm = m.bone(side, [x, 11.8, 0], "body")
        m.cube(arm, [x - 0.8, 8.2, -0.8], [1.6, 3.6, 1.6], "blouse")
        m.cube(arm, [x - 0.6, 7, -0.6], [1.2, 1.2, 1.2], "rosy")                                # her hands
    # The basket hangs from her hand by its handle (the bone's pivot is her grip), turned against her arm's swing so
    # it always hangs straight down.
    basket = m.bone("basket", [-3.5, 7.6, 0], "right_arm")
    m.cube(basket, [-4.7, 3.4, -1.8], [2.4, 2.6, 3.6], "burlap")                                # wicker, carried lengthwise at her side
    m.cube(basket, [-4.6, 6.0, -1.7], [2.2, 0.4, 3.4], "gingham")                               # its cloth
    m.cube(basket, [-4.2, 6.4, -1.5], [1.2, 0.8, 1.0], "bread")                                 # a loaf peeking out
    m.cube(basket, [-3.7, 6.0, -1.7], [0.4, 1.8, 0.4], "burlap")                                # its handle, up to her hand
    m.cube(basket, [-3.7, 6.0, 1.3], [0.4, 1.8, 0.4], "burlap")
    m.cube(basket, [-3.7, 7.4, -1.7], [0.4, 0.4, 3.4], "burlap")                                # gripped in her fist
    basket["locators"] = {"basket_ball": [-3.5, 4.7, 0]}
    # Her skirt and her cape, as cloth (made after her basket, so it can push her skirt aside). The cape is narrow
    # enough to clear her arms, and hangs out over her skirt's back; her legs push both as she walks.
    # A pleated cotton skirt flaring to her knees, white lace under its hem, and a little white apron over its front.
    cloth = pleated_skirt(m, "body", 7.0, 2.5, 3.0, 2.0, "skirt", "skirt_fold", pleats=16, rows=3, flare=12.0, petticoat="blouse",
                          colliders=[{"point": "basket_ball", "radius": 1.6}], fabric="cotton")
    # The apron and the cape lie on the skirt itself (balls on its front and back panels, whose surface is the
    # skirt's outer face and the cloth's thickness): the legs push the skirt, and the skirt them, so they move with it
    # instead of being flipped out by the legs through it.
    on_skirt = lambda panels, thick: [{"point": pt, "radius": round(SURFACE + thick + 0.05, 2)} for pt, i, r in m.skirt_surface if i in panels]
    # The apron tied at her waistband (its top on the band's front), tilted out as the skirt flares, lying on it.
    cloth.update(cape(m, "body", -1.7, 1.7, -2.5, 7.0, 3.6, "blouse", name="apron", rows=2, cols=2, thick=0.25, fabric="linen",
                      colliders=on_skirt((14, 15, 0, 1), 0.25)))
    for c in range(2):
        _bone(m, f"apron_{c}_0")["rotation"] = [-12, 0, 0]
    # A red cape of light wool, flowing out behind her as she goes, over her back and the back of her skirt.
    cloth.update(cape(m, "body", -2.65, 2.65, 2.25, 12.3, 9.0, "cape_red", rows=3, cols=2, fabric="wool", thick=0.5,
                      colliders=[{"point": "body_ball", "radius": 2.5}, {"point": "skirt_ball", "radius": 3.0}] + on_skirt(range(5, 12), 0.5),
                      tune={"mass": 0.9, "follow": 0.65, "air_drag": 0.05, "flutter": 0.15}))
    # Her hood's point: a soft tail of the same red hanging down its back, swinging as she walks and turns her head.
    hood["locators"] = {"hood_ball": [0, 15.6, 0]}
    cloth.update(cape(m, "hood", -0.9, 0.9, 3.6, 18.7, 4.2, "cape_red", name="hood_tail", rows=3, cols=1, thick=0.45, fabric="wool",
                      stiffness=45.0, colliders=[{"point": "hood_ball", "radius": 3.75}, {"point": "body_ball", "radius": 2.5}],
                      tune={"mass": 0.8, "follow": 0.6}))
    hooded = {"hood": {"rotation": [0, 0, 0]}}
    hooded["veil"] = {"rotation": [0, 0, 0]}
    # Hood still up on her head; only the drape over her face folded up and back over its crown.
    open_ = {"hood": {"rotation": [0, 0, 0]}, "veil": {"rotation": [-270, 0, 0]}}
    shy = {"head": {"rotation": [6, f"8 * math.sin({T} * 45)", 0]}, "right_arm": {"rotation": [-25, 0, 14]},
           "basket": {"rotation": [f"25 + 4 * math.sin({T} * 90)", 0, -14]}}
    # A little walk, her hips turning with her stride and swaying onto each foot, so her skirt swishes from side to
    # side and her cape swings behind; set on the ground after (rise and fall from her feet, not a bob of its own).
    # Her knees: found by search (rig_pose, her walk set on the ground) for the foot she stands on never sliding
    # forward and the other carried clear: 85 degrees, 10 early, the thigh lifting 10 as it swings.
    walk = dict(humanoid_walk(swing=25, stride=24, bend=85, lead=-10, lift=10),
                body={"rotation": [3, f"7 * math.sin({T} * 450)", f"-3 * math.sin({T} * 450)"]},
                right_arm={"rotation": [-25, 0, 14]}, basket={"rotation": [f"25 + 10 * math.sin({T} * 450)", 0, -14]})
    a = {
        # Hood down over her face: head bowed, shy.
        "idle": {"loop": True, "animation_length": 4.0, "bones": dict(shy, **hooded)},
        "walk": {"loop": True, "animation_length": 0.8, "bones": dict(walk, **hooded)},
        # Hood back: looking up, smiling, now and then a little wave.
        "idle_open": {"loop": True, "animation_length": 4.0, "bones": dict(open_,
            head={"rotation": [-6, f"8 * math.sin({T} * 45)", f"-3 + 3 * math.sin({T} * 90)"]}, right_arm={"rotation": [-25, 0, 14]},
            left_arm={"rotation": [f"-128 + 16 * math.max(0, math.sin({T} * 360))", 0, f"34 + 8 * math.sin({T} * 720)"]},
            basket={"rotation": [f"25 + 4 * math.sin({T} * 90)", 0, -14]})},
        "walk_open": {"loop": True, "animation_length": 0.8, "bones": dict(walk, **open_)},
        # Offering something from her basket.
        "attack": {"loop": False, "animation_length": 0.8, "bones": {
            "right_arm": {"rotation": frames((0.0, [-25, 0, 14]), (0.3, [-75, 0, 14]), (0.6, [-75, 0, 14]), (0.8, [-25, 0, 14]))},
            "basket": {"rotation": frames((0.0, [25, 0, -14]), (0.3, [75, 0, -14]), (0.6, [75, 0, -14]), (0.8, [25, 0, -14]))},
            "head": {"rotation": frames((0.0, [0, 0, 0]), (0.3, [10, 0, 8]), (0.8, [0, 0, 0]))}}},
    }
    # Her walks set on the ground: at every moment her lowest boot rests on it, so she rises over the foot she stands
    # on and sinks as her legs part, her front foot lands as her back one pushes off, and the swinging one clears it.
    import rig_pose as rp
    rig = rp.Rig(m.bones)
    boots = [(leg, [x, 0.0, z]) for leg in ("right_shin", "left_shin") for x, z in _boot_corners(_bone(m, leg))]
    for name in ("walk", "walk_open"):
        a[name] = rp.settle(rig, rp.seamless(a[name]), boots, step=0.025)
    return m, a, {"look_at": {"head": {"max_yaw": 60, "max_pitch": 30, "smoothing": 0.25}}, "springs": cloth}


def _boot_corners(shin):
    """The corners of the sole of a shin's lowest cube (x, z)."""
    c = min(shin["cubes"], key=lambda c: c["origin"][1])
    (x, _, z), (w, _, d) = c["origin"], c["size"]
    return [(x, z), (x + w, z), (x, z + d), (x + w, z + d)]


def little_red_feral():
    """Little Red Riding Hood as she truly is, shown when she is harmed at night or before a mind already slipping: the
    same small girl, hunched now, her hood torn and fallen back, her cape in rags, her blouse stained, lank fair hair
    over a grey face, hollow eyes with a red point in each, a grin from ear to ear full of needle teeth on a jaw that
    drops open, and arms grown long to her knees with black claws. Her `reveal` clip is her turning."""
    m = Model()
    m.bone("root", [0, 0, 0])
    legs(m, "root", (-1.1, 1.1), 7, "bloodied", "boot", thick=2.0)
    body = m.bone("body", [0, 7, 0], "root", locators={"body_ball": [0, 9.5, 0]})
    m.cube(body, [-2.4, 7, -1.5], [4.8, 5.4, 3], "bloodied")
    m.cube(body, [-2.55, 7, -1.65], [5.1, 3.2, 3.3], "bodice")
    body["locators"]["skirt_ball"] = [0, 4.5, 0]
    cloth = skirt(m, "body", 7.0, 2.5, 3.1, 2.1, "skirt", name="ragged_skirt", rows=3, fabric="rags")
    head = m.bone("head", [0, 12.4, 0], "body")
    m.cube(head, [-1, 12.4, -1], [2, 1.0, 2], "pale")                                          # a thin neck, holding her head up off her shoulders
    m.cube(head, [-3.5, 14.6, -3.5], [7, 6, 7], "feral_face")
    m.cube(head, [-3.6, 20.6, -3.6], [7.2, 0.6, 7.2], "hair_lank")                             # matted hair
    for x in (-3.9, 3.5):
        m.cube(head, [x, 14.6, -3.2], [0.4, 6.0, 2.6], "hair_lank")                              # hanging lank down her sides, in hanks
        m.cube(head, [x, 14.0, -0.4], [0.4, 6.6, 3.2], "hair_lank")                              # (no lower: they'd brush her shoulders)
    m.cube(head, [-1.6, 16.4, -3.8], [0.7, 4.2, 0.4], "hair_lank")                              # strands across her face, one over an eye
    m.cube(head, [2.3, 15.2, -3.8], [0.7, 5.4, 0.4], "hair_lank")
    m.cube(head, [-3.6, 13.0, 3.4], [7.2, 7.6, 0.4], "hair_lank")                               # and down her back
    jaw = m.bone("jaw", [0, 14.6, 0.3], "head")                                              # hinged mid-skull
    m.cube(jaw, [-2.8, 13.2, -3.3], [5.6, 1.4, 3.6], "feral_jaw")                              # short and narrow: open, it clears her chest and arms
    hood = m.bone("hood", [0, 13.4, 3.8], "head")                                            # torn and fallen back
    m.cube(hood, [-3.4, 12.6, 3.8], [6.8, 3.4, 0.6], "red_rags")
    m.cube(hood, [-2.4, 15.4, 4.4], [4.8, 1.6, 1.0], "red_rags")
    cloth.update(cape(m, "body", -2.6, 2.6, 2.3, 12.0, 0, "red_rags", name="rag", rows=2, cols=3, woven=False, fabric="rags",
                      lengths=[8.5, 5.5, 10.0], colliders=[{"point": "body_ball", "radius": 2.5}, {"point": "skirt_ball", "radius": 3.0}] + m.leg_balls))  # in strips
    for side, x in (("right_arm", -4.1), ("left_arm", 4.1)):
        arm = m.bone(side, [x, 11.8, 0], "body")
        m.cube(arm, [x - 0.8, 5.2, -0.8], [1.6, 6.6, 1.6], "bloodied")                          # grown long, to her knees
        m.cube(arm, [x - 0.7, 4.0, -0.7], [1.4, 1.2, 1.4], "pale")                              # grey hands
        for dx in (-0.6, -0.15, 0.3):
            m.cube(arm, [x + dx, 2.0, -0.6], [0.3, 2.0, 0.3], "claw")                           # black claws
    ragged = {"hood": {"rotation": [0, 0, 0]}}
    a = {
        # Hunched, her head cocked too far, her jaw working.
        "idle": {"loop": True, "animation_length": 4.0, "bones": dict(ragged,
            body={"rotation": [18, 0, 0]},
            head={"rotation": [-16, f"6 * math.sin({T} * 90)", f"12 + 3 * math.sin({T} * 270)"]},
            jaw={"rotation": [f"6 + 5 * math.abs(math.sin({T} * 180))", 0, 0]},
            right_arm={"rotation": [-22, 0, 6]}, left_arm={"rotation": [-22, 0, -6]})},
        # A lurching run, claws forward.
        "walk": {"loop": True, "animation_length": 1.0, "bones": dict(humanoid_walk(arms_forward=30, swing=24, stride=38), **dict(ragged,
            body={"rotation": [22, f"5 * math.sin({T} * 360)", 0], "position": [0, f"0.6 * math.abs(math.sin({T} * 360))", 0]},
            head={"rotation": [-20, 0, f"12 * math.sin({T} * 180)"]},
            jaw={"rotation": [14, 0, 0]}))},
        # Her claws up, her jaw wide, and down they come.
        "attack": {"loop": False, "animation_length": 0.6, "bones": {
            "right_arm": {"rotation": frames((0.0, [-22, 0, 6]), (0.2, [-140, 0, -10]), (0.35, [-40, 0, 6]), (0.6, [-22, 0, 6]))},
            "left_arm": {"rotation": frames((0.0, [-22, 0, -6]), (0.2, [-140, 0, 10]), (0.35, [-40, 0, -6]), (0.6, [-22, 0, -6]))},
            "jaw": {"rotation": frames((0.0, [6, 0, 0]), (0.2, [30, 0, 0]), (0.4, [30, 0, 0]), (0.6, [6, 0, 0]))},
            "head": {"rotation": frames((0.0, [-16, 0, 12]), (0.2, [-30, 0, 0]), (0.6, [-16, 0, 12]))}}},
    }
    return m, a, {"look_at": {"head": {"max_yaw": 70, "max_pitch": 35, "smoothing": 0.4}}, "springs": cloth}



def little_red_turning(painted):
    """Little Red turning, both of her in one armature: every part of her sweet self (`s_` bones) and of her true form
    (`f_` bones) on a shared skeleton, so one clip can burn her away limb by limb and grow what she is in its place,
    seamlessly, then scream. Its texture is copied pixel for pixel from the two models' own, so it is drawn exactly as
    they are at either end, and swapping to it (and from it) shows no seam. The shared bones are the sweet model's."""
    sweet, feral = painted["little_red"][0], painted["little_red_feral"][0]
    shared = [b["name"] for b in sweet.bones if b["name"] in {fb["name"] for fb in feral.bones}]
    m = Model()
    blits = []
    for b in sweet.bones:
        if b["name"] in shared:
            m.bone(b["name"], b["pivot"], b.get("parent"), b.get("rotation"), b.get("locators"))
    pivots = {b["name"]: b["pivot"] for b in feral.bones}

    def copy(src, prefix, name):
        bones = {}
        for b in src.bones:
            n = b["name"]
            parent = b.get("parent")
            if n in shared:
                bones[n] = m.bone(prefix + n, b["pivot"], n)                         # its part, under the shared joint
            else:
                bones[n] = m.bone(prefix + n, b["pivot"], prefix + parent, b.get("rotation"))
        where = {id(c): b["name"] for b in src.bones for c in b["cubes"]}
        for c, size, material, uv in src.cubes:
            bone = bones[where[id(c)]]
            m.cube(bone, c["origin"], c["size"], material, c.get("rotation"), c.get("pivot"), c.get("inflate", 0.0))
            blits.append((prefix, uv, m.cubes[-1][3], size))
    copy(sweet, "s_", None)
    copy(feral, "f_", None)

    # When each part of her burns: her sweet part shrinks away into its joint as her true one grows out of it.
    burns = {"arms": (0.4, 0.75, ["right_arm", "left_arm"]), "legs": (0.6, 0.95, ["right_leg", "left_leg", "right_shin", "left_shin"]),
             "body": (0.8, 1.15, ["body"]), "head": (1.0, 1.35, ["head", "hood"])}
    clip = {}
    for t0, t1, parts in burns.values():
        for part in parts:
            clip["s_" + part] = {"scale": frames((0.0, [1, 1, 1]), (t0, [1, 1, 1]), (t0 + (t1 - t0) * 0.5, [1.12, 0.9, 1.12]), (t1, [0, 0, 0]))}
            clip["f_" + part] = {"scale": frames((0.0, [0, 0, 0]), (t0 + (t1 - t0) * 0.35, [0, 0, 0]), (t1, [1.08, 1.08, 1.08]), (t1 + 0.12, [1, 1, 1]))}
    tremble = lambda a, b, until, base: [(round(i * 0.075, 3), [base[0], base[1], base[2] + (a if i % 2 else b)]) for i in range(int(until / 0.075))]
    clip["body"] = {"rotation": frames(*tremble(4, -4, 1.35, [4, 0, 0]), (1.35, [6, 0, 0]), (1.45, [-10, 0, 0]), (1.6, [8, 0, 0]),
                                       (2.7, [8, 0, 0]), (3.0, [18, 0, 0]))}
    clip["head"] = {"rotation": frames((0.0, [-6, 0, 0]), (0.4, [10, 0, 0]), (1.0, [22, 0, 0]), (1.3, [22, 0, 0]), (1.42, [-38, 0, 0]),
                                       (1.6, [-12, 0, 0]), (2.7, [-12, 0, 0]), (3.0, [-16, 0, 12]))}
    # The scream: her jaw wide and juddering, the length of it.
    clip["f_jaw"] = {"rotation": frames((0.0, [0, 0, 0]), (1.38, [0, 0, 0]), (1.5, [32, 0, 0]),
                                        *[(round(1.5 + i * 0.1, 2), [28 if i % 2 else 33, 0, 0]) for i in range(1, 12)], (2.7, [30, 0, 0]), (3.0, [6, 0, 0]))}
    clip["right_arm"] = {"rotation": frames((0.0, [-25, 0, 14]), (0.4, [-15, 0, 14]), (1.4, [-15, 0, 14]), (1.6, [-20, 0, 45]),
                                            (2.7, [-20, 0, 45]), (3.0, [-22, 0, 6]))}
    clip["left_arm"] = {"rotation": frames((0.0, [0, 0, 0]), (0.4, [-15, 0, -14]), (1.4, [-15, 0, -14]), (1.6, [-20, 0, -45]),
                                           (2.7, [-20, 0, -45]), (3.0, [-22, 0, -6]))}
    clip["s_veil"] = {"rotation": [-270, 0, 0]}
    clip["s_basket"] = {"rotation": [25, 0, -14]}
    a = {"turn": {"loop": False, "animation_length": 3.0, "bones": clip}}
    return m, a, {"look_at": {"head": {"max_yaw": 40, "max_pitch": 25, "smoothing": 0.4}}}, blits


# Built from other models' finished geometry and textures (the painted dict), after them.
MERGED = {"little_red_turning": little_red_turning}


MOBS = {"mourner": mourner, "briar_archer": briar_archer, "thornling": thornling, "weeping_wisp": weeping_wisp, "hedge_warden": hedge_warden, "tomb_revenant": tomb_revenant,
        "sleepwalker": sleepwalker, "dread_manta": dread_manta, "dream_eater": dream_eater, "somnimoth": somnimoth, "faceless": faceless,
        "night_mare": night_mare, "hollow_hound": hollow_hound, "lullaby": lullaby, "gazer": gazer, "tick_tock": tick_tock,
        "stitchling": stitchling, "gloom_jelly": gloom_jelly, "dream_hare": dream_hare,
        "grinning_clown": grinning_clown, "closet_boogeyman": closet_boogeyman, "big_bad_wolf": big_bad_wolf, "sandman": sandman,
        "tooth_taker": tooth_taker, "broodmother": broodmother, "broodling": broodling, "void_eel": void_eel, "hive_thing": hive_thing,
        "crawlspace": crawlspace, "mime_clown": mime_clown, "jack_in_the_box": jack_in_the_box, "balloon_clown": balloon_clown,
        "funhouse_clown": funhouse_clown, "little_red": little_red, "little_red_feral": little_red_feral}


def build_all():
    for i, (name, build) in enumerate(MOBS.items()):
        m, clips, extra = build()
        geo_overlaps.separate(m.bones)                                  # no two faces left fighting in one plane
        write_mob(name, m, clips, extra, 700 + i)


def write_mob(name, m, clips, extra, seed, blits=None):
    import rig_pose
    for clip in clips.values():
        rig_pose.seamless(clip)                                         # every loop joins up where it loops
    arm = os.path.join(ASSETS, "armatures")
    write(os.path.join(arm, f"{name}.geo.json"), {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{name}", "texture_width": TEX, "texture_height": TEX}, "bones": m.bones}]})
    write(os.path.join(arm, f"{name}.animation.json"),
          {"format_version": "1.8.0", "animations": {f"animation.{name}.{k}": v for k, v in clips.items()}})
    write(os.path.join(arm, f"{name}.armature.json"), dict({
        "glow_textures": [f"{NS}:textures/armatures/{name}_glow.png"],
        "transition": {"duration": 0.2, "easing": "sine_in_out"}}, **extra,
        **({"cloth": m.cloth_sheets} if getattr(m, "cloth_sheets", None) else {})))
    if blits is None:
        img, glow = paint(m, seed)
    else:
        # Copied from the models it is made of, each cube's whole UV block, so it is drawn exactly as they are.
        img, glow = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0)), Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
        for source, (sx, sy), (dx, dy), (w, h, d) in blits:
            simg, sglow = PAINTED[source][1], PAINTED[source][2]
            box = (sx, sy, sx + 2 * (w + d), sy + d + h)
            img.paste(simg.crop(box), (dx, dy))
            glow.paste(sglow.crop(box), (dx, dy))
    PAINTED[name] = (m, img, glow)
    if name == "sleepwalker":
        # Its eyes are shut in its texture (lids where the glow's open eyes are); its glow, lit when it wakes
        # (LabyrinthMobsClient), opens them.
        for y in range(img.height):
            for x in range(img.width):
                if glow.getpixel((x, y))[3] > 0:
                    img.putpixel((x, y), (70, 66, 80, 255))
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


PAINTED = {}


def main():
    build_all()
    for name, build in MERGED.items():
        m, clips, extra, blits = build(PAINTED)
        write_mob(name, m, clips, extra, 0, [({"s_": "little_red", "f_": "little_red_feral"}[prefix], src, dst, size)
                                             for prefix, src, dst, size in blits])


if __name__ == "__main__":
    main()
