"""The Dark Between's wild creatures' drops: the charms they leave (CharmItem, WildCharms: icons, item models and
item definitions under assets/whycantisleep) and each creature's loot table (data/whycantisleep/loot_table/entities).

Icons are 16x16 pixel art drawn in code, each from a few shapes: the shapes are filled with the item's own ramp, then
lit like Minecraft's items (a light edge up and to the left, a dark one down and to the right) and outlined in the
item's darkest colour. Their names and the line under each name are in gen_between_blocks' lang.
"""
import json
import math
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
NS = "whycantisleep"
ASSETS = os.path.join(ROOT, "common", "src", "main", "resources", "assets", NS)
DATA = os.path.join(ROOT, "common", "src", "main", "resources", "data", NS)


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


class Icon:
    def __init__(self, outline):
        self.px = {}
        self.outline = outline

    def put(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            self.px[(int(x), int(y))] = c

    def disc(self, cx, cy, r, colour):
        """Every pixel whose middle is within r of (cx, cy); colour is a colour, or a function of the distance."""
        for y in range(16):
            for x in range(16):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if d <= r:
                    self.put(x, y, colour(d) if callable(colour) else colour)

    def rect(self, x0, y0, x1, y1, colour):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put(x, y, colour(x, y) if callable(colour) else colour)

    def image(self, light=True):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for (x, y), c in self.px.items():
            if light:
                # Lit from up and left: a lighter rim there, a darker one down and right.
                if (x - 1, y) not in self.px or (x, y - 1) not in self.px:
                    c = tuple(min(255, int(v * 1.18 + 12)) for v in c)
                elif (x + 1, y) not in self.px or (x, y + 1) not in self.px:
                    c = tuple(int(v * 0.78) for v in c)
            img.putpixel((x, y), tuple(c) + (255,))
        # The outline: every empty pixel beside a filled one.
        for y in range(16):
            for x in range(16):
                if (x, y) in self.px:
                    continue
                if any((x + dx, y + dy) in self.px for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    img.putpixel((x, y), self.outline + (255,))
        return img


def ramp(colours, t):
    """A colour from a ramp, t from 0 (first) to 1 (last)."""
    t = max(0.0, min(0.999, t))
    return colours[int(t * len(colours))]


def mares_ember():
    """A violet flame: a round burning foot, white-hot at its heart, a main tongue curling up to its tip and two
    smaller tongues licking up either side."""
    flame = [(70, 20, 130), (110, 40, 190), (150, 70, 235), (195, 130, 250), (235, 205, 255), (255, 250, 255)]
    icon = Icon((34, 10, 60))

    def tongue(base_x, base_y, height, width, curl):
        for i in range(height + 1):
            t = i / height                                                       # 0 at its foot, 1 at its tip
            y = base_y - i
            mid = base_x + math.sin(t * math.pi * 0.9) * curl
            half = width * (1.0 - t) ** 0.9
            for x in range(16):
                if abs(x + 0.5 - mid) <= max(half, 0.45):
                    icon.put(x, y, ramp(flame, 0.15 + (1.0 - t) * 0.35))
    tongue(8.0, 10, 9, 3.2, 1.4)                                                  # the main tongue
    tongue(4.6, 11, 5, 1.4, -0.8)                                                 # and either side
    tongue(11.4, 11, 6, 1.4, 0.9)
    icon.disc(8.0, 11.0, 4.3, lambda d: ramp(flame, 1.0 - d / 4.3 * 0.75))       # its burning foot
    return icon.image()


def hollow_fang():
    """A long curved fang of bone, yellowed, its root dark with smoke, a crack along it."""
    bone = [(150, 140, 112), (180, 170, 140), (206, 198, 170), (228, 222, 200)]
    root = [(40, 40, 46), (62, 62, 70)]
    icon = Icon((26, 24, 28))
    steps = 40
    for i in range(steps + 1):
        t = i / steps
        # Along a curve from its root (top left) to its point (bottom right).
        x = 4.0 + 8.0 * t + math.sin(t * math.pi) * -2.2
        y = 2.5 + 11.0 * t
        r = 2.4 * (1.0 - t) ** 0.7 + 0.35
        for py in range(16):
            for px in range(16):
                d = math.hypot(px + 0.5 - x, py + 0.5 - y)
                if d <= r:
                    if t < 0.22:
                        c = root[0 if d > r * 0.5 else 1]
                    else:
                        c = ramp(bone, 1.0 - d / r * 0.8 - t * 0.2)
                    icon.put(px, py, c)
    for x, y in ((7, 7), (8, 8), (8, 9)):
        icon.put(x, y, (110, 100, 80))                                           # the crack
    return icon.image()


def lullaby_box():
    """A little brass music box, its lid inlaid, a crank at its side, a note rising from it."""
    brass = [(96, 70, 28), (130, 98, 42), (166, 128, 58), (204, 166, 84)]
    icon = Icon((44, 28, 10))
    icon.rect(2, 8, 12, 14, lambda x, y: brass[1] if (x + y) % 5 else brass[0])     # the box
    icon.rect(2, 6, 12, 7, brass[3])                                                # its lid
    icon.rect(5, 6, 9, 6, (120, 40, 70))                                            # an inlay in the lid
    icon.rect(6, 10, 8, 12, brass[2])                                               # its plate
    icon.put(7, 11, (40, 26, 10))                                                   # the keyhole
    icon.rect(13, 10, 13, 11, brass[2])                                             # the crank's shaft
    icon.rect(14, 9, 14, 11, brass[3])                                              # and handle
    note = (190, 150, 255)
    for x, y in ((4, 1), (5, 1), (5, 2), (5, 3), (4, 3), (3, 3), (3, 4), (4, 4)):
        icon.put(x, y, note)                                                        # a note, rising
    return icon.image()


def gazers_lens():
    """The Gazer's eye, cut down to its lens: a burning red iris round a black pupil in a rim of flesh."""
    icon = Icon((40, 10, 16))

    def eye(d):
        if d > 6.2:
            return (100, 40, 54)                                                    # the rim of flesh
        if d > 4.6:
            return (226, 214, 208)                                                  # the white
        if d > 1.6:
            return ramp([(150, 20, 30), (210, 40, 46), (255, 90, 70)], (d - 1.6) / 3.0)   # the iris
        return (12, 4, 8)                                                           # the pupil
    icon.disc(8.0, 8.0, 7.0, eye)
    icon.put(6, 6, (255, 230, 220))                                                 # a glint
    for x, y in ((3, 8), (4, 11), (12, 5)):
        icon.put(x, y, (190, 60, 70))                                               # veins in the white
    return icon.image()


def stopwatch():
    """A brass stopwatch, its crown and button at the top, its hands stopped at a quarter to."""
    brass = [(120, 90, 36), (166, 128, 58), (204, 166, 84)]
    icon = Icon((50, 34, 12))
    icon.rect(7, 1, 8, 2, brass[2])                                                 # the crown
    icon.rect(6, 3, 9, 3, brass[1])
    icon.disc(8.0, 9.5, 6.0, lambda d: brass[1] if d > 4.7 else (232, 224, 196))    # the case and its dial
    for a in range(12):
        ang = a * math.pi / 6
        icon.put(round(7.5 + math.cos(ang) * 3.8), round(9.0 + math.sin(ang) * 3.8), (150, 140, 120))   # its hours
    for x, y in ((7, 9), (7, 8), (7, 7), (7, 6)):
        icon.put(x, y, (24, 18, 10))                                                # the long hand, at twelve
    for x, y in ((6, 9), (5, 9), (4, 9)):
        icon.put(x, y, (24, 18, 10))                                                # the short, at nine
    icon.put(7, 9, (180, 40, 40))                                                   # its pin
    return icon.image()


def living_thread():
    """A spool of red thread that still twitches, a needle stuck through it."""
    wood = [(100, 70, 44), (140, 104, 66)]
    thread = [(130, 26, 40), (176, 44, 56), (214, 80, 88)]
    icon = Icon((40, 16, 14))
    icon.rect(3, 3, 11, 4, wood[1])                                                 # the spool's flanges
    icon.rect(3, 12, 11, 13, wood[0])
    icon.rect(4, 5, 10, 11, lambda x, y: thread[2] if x == 5 else thread[0] if x == 10 else thread[(x + y) % 2])
    icon.put(10, 8, thread[2])
    for x, y in ((11, 9), (12, 10), (12, 11), (13, 12)):
        icon.put(x, y, thread[1])                                                   # a loose end
    for i in range(6):
        icon.put(9 + i, 6 - i, (200, 200, 214) if i < 5 else (120, 120, 134))      # the needle through it
    return icon.image()


def gloom_gel():
    """A wobbling blob of the jelly's violet gel with a light still in it, dripping."""
    gel = [(50, 36, 110), (74, 56, 146), (104, 82, 186), (140, 116, 222)]
    icon = Icon((24, 14, 50))
    for y in range(16):
        for x in range(16):
            d = math.hypot((x + 0.5 - 8.0) / 6.0, (y + 0.5 - 8.5) / 5.0)
            if d <= 1.0:
                icon.put(x, y, ramp(gel, 1.0 - d * 0.9 - (y - 4) / 30.0))
    icon.disc(8.5, 9.5, 1.8, lambda d: (220, 200, 255) if d < 0.9 else (170, 140, 255))   # its light
    icon.put(5, 14, gel[1])                                                         # a drop, falling from it
    icon.put(5, 5, (210, 200, 255))
    icon.put(6, 5, (180, 166, 240))                                                 # its sheen
    return icon.image()


def hares_foot():
    """A Dream Hare's hind foot, pale fur, bound at the top with a cord, the light of its ears in a tuft above."""
    fur = [(150, 150, 166), (178, 178, 192), (204, 204, 214), (226, 226, 234)]
    icon = Icon((50, 50, 64))
    for y in range(4, 15):
        t = (y - 4) / 10.0
        cx = 7.0 + t * 1.5
        half = 1.6 + t * 1.2 + (0.8 if y >= 12 else 0.0)                            # wider at the toes
        for x in range(16):
            d = abs(x + 0.5 - cx)
            if d <= half:
                icon.put(x, y, ramp(fur, 1.0 - d / half * 0.7 - (0.15 if (x + y) % 3 == 0 else 0.0)))
    for x in range(5, 10):
        icon.put(x, 5, (110, 70, 40))                                               # the cord
    for x, y, c in ((6, 2, (150, 220, 255)), (7, 1, (220, 245, 255)), (7, 2, (220, 245, 255)), (8, 2, (150, 220, 255)), (7, 3, (150, 220, 255)),
                    (6, 3, (110, 190, 240)), (8, 3, (110, 190, 240))):
        icon.put(x, y, c)                                                           # its dreamlight tuft
    for x in (8, 9, 10):
        icon.put(x, 14, (120, 120, 136))                                            # toes
    return icon.image()


def swap_card():
    """A jester's playing card: cream, bordered red, a pip in each corner, and two arrows chasing each other round its
    middle, one red, one black: the swap."""
    icon = Icon((40, 16, 20))
    icon.rect(3, 1, 12, 14, lambda x, y: (176, 30, 44) if x in (3, 12) or y in (1, 14) else (236, 228, 208))
    icon.put(5, 3, (176, 30, 44))
    icon.put(10, 12, (30, 24, 30))
    red, black = (176, 30, 44), (30, 24, 30)
    for x in range(5, 10):
        icon.put(x, 5, red)                                                         # an arrow pointing right, red
    for x, y in ((10, 5), (9, 4), (9, 6), (8, 3), (8, 7)):
        icon.put(x, y, red)
    for x in range(6, 11):
        icon.put(x, 10, black)                                                      # and one pointing left, black
    for x, y in ((5, 10), (6, 9), (6, 11), (7, 8), (7, 12)):
        icon.put(x, y, black)
    return icon.image()


def closet_key():
    """An old iron key, its bow a ring with an eye in it, its bit toothed, on a frayed ribbon."""
    iron = [(60, 58, 66), (96, 94, 104), (140, 138, 150)]
    icon = Icon((24, 22, 28))
    icon.disc(5.0, 5.0, 3.6, lambda d: iron[2] if d > 2.6 else iron[1] if d > 1.6 else (0, 0, 0))   # the bow
    for (x, y) in list(icon.px):
        if icon.px[(x, y)] == (0, 0, 0):
            del icon.px[(x, y)]                                                    # its hole
    icon.put(5, 5, (255, 60, 70))                                                   # an eye in it
    for i in range(8):
        icon.put(7 + i, 7 + i, iron[1])                                             # the shank
        icon.put(8 + i, 7 + i, iron[0])
    for x, y in ((11, 14), (12, 14), (13, 13), (14, 12)):
        icon.put(x, y, iron[1])                                                     # its bit
    for x, y in ((2, 9), (2, 10), (3, 11), (2, 12)):
        icon.put(x, y, (120, 30, 40))                                               # a ribbon on it
    return icon.image()


def huff_and_puff():
    """A gust: three curling streaks of wind round a puff of cloud."""
    white = [(170, 180, 196), (206, 214, 226), (238, 242, 248)]
    icon = Icon((60, 70, 90))
    icon.disc(5.0, 8.0, 3.4, lambda d: white[2] if d < 1.8 else white[1])
    icon.disc(8.0, 6.5, 2.6, lambda d: white[2] if d < 1.4 else white[1])
    for y, (x0, x1) in ((4, (9, 14)), (8, (10, 15)), (12, (6, 13))):
        for x in range(x0, x1):
            icon.put(x, y, white[1])
        icon.put(x1, y - 1, white[0])                                               # each streak curls up at its end
    return icon.image()


def dream_sand():
    """A small pouch of burlap, open, gold sand glittering at its mouth and spilling."""
    burlap = [(110, 86, 52), (140, 112, 70), (168, 138, 90)]
    sand = [(220, 180, 70), (250, 220, 130), (255, 246, 200)]
    icon = Icon((50, 36, 18))
    for y in range(6, 15):
        half = 3.0 + math.sin((y - 6) / 8.0 * math.pi) * 2.0
        for x in range(16):
            if abs(x + 0.5 - 7.5) <= half:
                icon.put(x, y, burlap[(x + y) % 2 + (1 if y < 9 else 0)])
    icon.rect(5, 5, 10, 5, (120, 70, 40))                                           # its drawstring
    icon.rect(6, 3, 9, 4, sand[1])                                                  # sand at its mouth
    icon.put(7, 2, sand[2])
    for x, y in ((12, 9), (13, 11), (12, 13), (14, 14)):
        icon.put(x, y, sand[0])                                                     # spilling
    return icon.image()


def fairy_dust():
    """A pinch of pale blue dust with sparkles in it."""
    icon = Icon((40, 70, 100))
    for (x, y, c) in ((5, 9, 1), (6, 9, 2), (7, 10, 1), (8, 9, 2), (9, 10, 1), (6, 10, 0), (7, 9, 2), (8, 10, 0), (10, 9, 1), (5, 10, 0),
                      (6, 11, 0), (9, 11, 0), (7, 11, 1), (8, 11, 1)):
        icon.put(x, y, [(110, 170, 220), (170, 220, 250), (230, 248, 255)][c])
    for cx, cy in ((4, 4), (11, 3), (12, 8)):
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            icon.put(cx + dx, cy + dy, (230, 248, 255) if (dx, dy) == (0, 0) else (150, 210, 245))   # sparkles
    return icon.image(light=False)


def broodsilk():
    """A coil of thick white silk with a hook of bone at its end."""
    silk = [(180, 178, 170), (212, 210, 202), (238, 236, 230)]
    icon = Icon((50, 48, 46))
    for ring in range(3):
        cy = 10 - ring * 0.5
        for a in range(24):
            ang = a / 24.0 * math.pi * 2
            icon.put(round(7 + math.cos(ang) * (4.2 - ring * 0.6)), round(cy + math.sin(ang) * 2.4), silk[ring])
    for i in range(5):
        icon.put(11 + i // 2, 7 - i, silk[1])                                       # a line out of the coil
    for x, y in ((13, 2), (14, 2), (15, 3), (15, 4), (14, 5)):
        icon.put(x, y, (200, 190, 150))                                             # its hook
    return icon.image()


def void_scale():
    """A great dark scale from the eel, a row of its lights still glowing down it."""
    eel = [(10, 16, 34), (18, 28, 54), (28, 42, 74), (40, 58, 96)]
    icon = Icon((4, 6, 14))
    for y in range(2, 15):
        t = (y - 2) / 12.0
        half = 5.2 * math.sin(math.pi * (0.15 + t * 0.85)) ** 0.7
        for x in range(16):
            d = abs(x + 0.5 - 8.0)
            if d <= half:
                icon.put(x, y, ramp(eel, 1.0 - d / max(half, 0.6) * 0.7 - t * 0.2))
    for y in (5, 8, 11):
        icon.put(8, y, (180, 250, 255))
        icon.put(7, y, (80, 200, 230))                                              # its lights
    return icon.image()


def swarm_jar():
    """A glass jar stoppered with a cork, dark specks of insects swarming inside."""
    icon = Icon((40, 50, 56))
    glass = (150, 176, 186)
    icon.rect(4, 5, 11, 14, lambda x, y: (196, 214, 220) if x == 5 else glass if x in (4, 11) or y == 14 else (118, 142, 150))
    icon.rect(5, 3, 10, 4, (150, 110, 70))                                          # the cork
    for x, y in ((6, 7), (8, 8), (9, 6), (7, 10), (10, 11), (6, 12), (9, 13), (8, 11)):
        icon.put(x, y, (24, 18, 12))                                                # the swarm
    return icon.image()


def shrinking_skin():
    """A limp, flat scrap of grey skin, wide-set eyes and a stretched mouth still in it."""
    skin = [(110, 108, 102), (132, 130, 124), (152, 150, 144)]
    icon = Icon((50, 48, 46))
    for y in range(3, 14):
        for x in range(2, 14):
            wave = math.sin(x * 0.9 + y * 0.4) * 0.8
            if 3 + wave <= y <= 12 + wave * 0.5 and (x - 7.5) ** 2 / 36 + (y - 8) ** 2 / 30 <= 1.05:
                icon.put(x, y, skin[(x + y) % 3])
    icon.put(5, 7, (240, 244, 255))
    icon.put(10, 7, (240, 244, 255))                                                # its eyes, still
    for x in range(5, 11):
        icon.put(x, 10, (30, 22, 22))                                               # its mouth
    return icon.image()


ICONS = {"closet_key": closet_key, "huff_and_puff": huff_and_puff, "dream_sand": dream_sand, "fairy_dust": fairy_dust,
         "broodsilk": broodsilk, "void_scale": void_scale, "swarm_jar": swarm_jar, "shrinking_skin": shrinking_skin,
         "swap_card": swap_card, "mares_ember": mares_ember, "hollow_fang": hollow_fang, "lullaby_box": lullaby_box, "gazers_lens": gazers_lens,
         "stopwatch": stopwatch, "living_thread": living_thread, "gloom_gel": gloom_gel, "hares_foot": hares_foot}


def item(name, lo=1, hi=1, chance=None, player=False, looting=True, smelt=False):
    entry = {"type": "minecraft:item", "name": name if ":" in name else f"{NS}:{name}", "functions": []}
    if (lo, hi) != (1, 1):
        entry["functions"].append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
    if smelt:
        entry["functions"].append({"function": "minecraft:furnace_smelt", "conditions": [
            {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"flags": {"is_on_fire": True}}}]})
    if looting and chance is None:
        entry["functions"].append({"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
                                   "count": {"type": "minecraft:uniform", "min": 0, "max": 1}})
    if not entry["functions"]:
        del entry["functions"]
    pool = {"rolls": 1, "entries": [entry]}
    conditions = []
    if player:
        conditions.append({"condition": "minecraft:killed_by_player"})
    if chance is not None:
        conditions.append({"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
                           "unenchanted_chance": chance, "enchanted_chance": {"type": "minecraft:linear", "base": chance + 0.05, "per_level_above_first": 0.05}})
    if conditions:
        pool["conditions"] = conditions
    return pool


# What each wild creature leaves: something plain, and its charm (the rarer ones only to a player's hand).
LOOT = {
    "night_mare": [item("minecraft:leather", 0, 2), item("mares_ember", 1, 2, player=True)],
    "hollow_hound": [item("minecraft:bone", 0, 2), item("hollow_fang", chance=0.12, player=True)],
    "lullaby": [item("minecraft:amethyst_shard", 0, 2), item("minecraft:string", 0, 1), item("lullaby_box", chance=0.25, player=True)],
    "gazer": [item("minecraft:spider_eye", 0, 1), item("gazers_lens", chance=0.5, player=True)],
    "tick_tock": [item("minecraft:copper_ingot", 0, 2), item("minecraft:gold_nugget", 0, 3), item("stopwatch", chance=0.15, player=True)],
    "stitchling": [item("minecraft:string", 0, 2), item("minecraft:white_wool", 0, 1), item("living_thread", 0, 2)],
    "gloom_jelly": [item("minecraft:glow_ink_sac", 0, 1), item("minecraft:slime_ball", 0, 1), item("gloom_gel", 1, 2)],
    "grinning_clown": [item("minecraft:string", 0, 2), item("minecraft:red_dye", 0, 1), item("minecraft:cake", chance=0.04),
                       item("swap_card", chance=0.35, player=True)],
    "closet_boogeyman": [item("minecraft:stick", 1, 3), item("minecraft:black_dye", 0, 1), item("closet_key", chance=0.2, player=True)],
    "big_bad_wolf": [item("minecraft:leather", 1, 2), item("minecraft:red_wool", 0, 1), item("huff_and_puff", chance=0.25, player=True)],
    "sandman": [item("minecraft:sand", 1, 3), item("minecraft:glowstone_dust", 0, 2), item("dream_sand", 1, 2, player=True)],
    "tooth_taker": [item("minecraft:bone_meal", 0, 2), item("fairy_dust", 1, 2)],
    "broodmother": [item("minecraft:string", 2, 5), item("minecraft:spider_eye", 1, 2), item("broodsilk", player=True, looting=False)],
    "void_eel": [item("minecraft:prismarine_crystals", 0, 3), item("void_scale", 1, 2, player=True)],
    "hive_thing": [item("minecraft:honeycomb", 0, 2), item("minecraft:rotten_flesh", 0, 2), item("swarm_jar", 1, 2, player=True)],
    "crawlspace": [item("minecraft:leather", 0, 1), item("shrinking_skin", chance=0.4, player=True)],
    "mime_clown": [item("minecraft:black_dye", 0, 1), item("minecraft:white_dye", 0, 1), item("minecraft:string", 0, 2)],
    "jack_in_the_box": [item("minecraft:stick", 1, 3), item("minecraft:iron_nugget", 1, 4), item("minecraft:gold_nugget", 0, 2)],
    "balloon_clown": [item("minecraft:string", 1, 3), item("minecraft:slime_ball", 0, 1), item("minecraft:cake", chance=0.04)],
    "funhouse_clown": [item("minecraft:glass_pane", 0, 2), item("minecraft:red_dye", 0, 1), item("swap_card", chance=0.25, player=True)],
    "little_red": [item("minecraft:apple", 0, 1), item("minecraft:bread", 0, 1), item("minecraft:red_wool", 0, 1)],
    "dream_hare": [item("minecraft:rabbit", smelt=True), item("minecraft:rabbit_hide", 0, 1), item("hares_foot", chance=0.3)],
}


def run():
    for name, draw in ICONS.items():
        draw().save(os.path.join(ASSETS, "textures", "item", name + ".png"))
        write(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})
        write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})
    for name, pools in LOOT.items():
        write(os.path.join(DATA, "loot_table", "entities", name + ".json"),
              {"type": "minecraft:entity", "pools": pools, "random_sequence": f"{NS}:entities/{name}"})
    print("wild items:", ", ".join(ICONS), "| loot:", ", ".join(LOOT))


if __name__ == "__main__":
    run()
