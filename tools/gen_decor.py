"""The Garden's furnishings (DecorBlock): detailed block models and their textures, drawn facing north and turned by
their blockstates. Called from gen_between_blocks.py (decor), which owns the palette and helpers.

    dreamstone_urn   a foot ring and stem, a belly swelling in steps with a band of red runes, two handles, a neck, a
                     flared lip and a lid with a knob
    stone_planter    a trough on four feet, a moulded rim, an engraved front panel with an eye; moss and soil in it,
                     thorns, a bloom and a dead shrub, veins spilling over its front edge
    dread_brazier    four splayed wrought-iron legs on feet, a ring tying them, a stepped bowl studded round its belly,
                     a rolled rim; coals and a tall fire (two crossed flames) burning in it
    dread_fire_lamp  a two-step plinth, a fluted post with a collar, a cage of four bars with a ring at its middle, a
                     stepped roof with a finial; coals and a fire in the cage
    dread_lantern    a stepped foot, four corner posts, smoked red glass with a band of iron round it, a
                     guttered candle and its flame inside, a stepped roof horned at its corners, a finial and hook
"""
import math
import os
import random

from PIL import Image


def run(g):
    """g: the gen_between_blocks module (palette, put, fine, new, write, b, state, item, ASSETS)."""
    tex_dir = os.path.join(g.ASSETS, "textures", "block")
    put, fine, new, mix = g.put, g.fine, g.new, g.mix
    BARK, FACE, LIP, SHADOW, GROOVE, MOSS, IRON = g.BARK, g.FACE, g.LIP, g.SHADOW, g.GROOVE, g.MOSS, g.IRON
    RED, HOT, CRIMSON = g.RED, g.HOT, g.CRIMSON

    def save(name, img):
        img.save(os.path.join(tex_dir, name + ".png"))

    # ---- Textures ----
    def wrought_iron():
        """Hammered dark iron: blotched highlights where the hammer struck, a rivet now and then."""
        rng = random.Random(91)
        img = new()
        for y in range(16):
            for x in range(16):
                c = IRON[1] if rng.random() < 0.7 else IRON[0] if rng.random() < 0.5 else IRON[2]
                put(img, x, y, fine(rng, c, 0.06))
        for x, y in ((3, 3), (12, 3), (3, 12), (12, 12), (7, 7)):
            put(img, x, y, (110, 110, 124))
            put(img, x + 1, y + 1, IRON[0])
        return img

    def stone(seed, mossy=False):
        """Worn polished dreamstone, lit from the upper left; with moss creeping up from the foot if mossy."""
        rng = random.Random(seed)
        img = new()
        for y in range(16):
            for x in range(16):
                c = FACE[2] if (x * 3 + y * 5 + rng.randrange(4)) % 9 else FACE[1]
                if y == 0 or x == 0:
                    c = LIP
                elif y == 15 or x == 15:
                    c = SHADOW
                if mossy and y > 10 - rng.randrange(4):
                    c = MOSS[rng.randrange(1, 4)]
                put(img, x, y, fine(rng, c, 0.05))
        return img

    def urn_band():
        """The urn's belly: dark glazed stone, fine lines above and below a band of red runes."""
        rng = random.Random(94)
        img = new()
        for y in range(16):
            for x in range(16):
                put(img, x, y, fine(rng, FACE[2] if y < 8 else FACE[1], 0.05))
        for x in range(16):
            put(img, x, 4, LIP)
            put(img, x, 11, SHADOW)
            for y in range(6, 10):
                put(img, x, y, FACE[0])
            if x % 4 in (1, 2):
                put(img, x, 7, RED if x % 8 < 4 else CRIMSON)
                put(img, x, 8 if x % 4 == 1 else 6, CRIMSON)
        return img

    def planter_front():
        """The planter's front panel: framed engraved stone, an eye cut in the middle."""
        img = stone(95, mossy=True)
        for k in range(1, 15):
            put(img, k, 1, GROOVE)
            put(img, k, 14, GROOVE)
        for y in range(4, 12):
            for x in range(3, 13):
                d = ((x - 7.5) / 4.5) ** 2 + ((y - 7.5) / 2.6) ** 2
                if 0.7 < d <= 1.0:
                    put(img, x, y, GROOVE)
                elif d <= 0.3:
                    put(img, x, y, CRIMSON if abs(x - 7.5) > 0.6 else (20, 4, 8))
        return img

    def dead_shrub():
        """A dead shrub: bare grey twigs forking up from one stem."""
        rng = random.Random(96)
        img = new()
        for x0 in (5, 8, 11):
            x, y = 8, 15
            while y > 3:
                put(img, x, y, BARK[2] if rng.random() < 0.6 else BARK[1])
                y -= 1
                if rng.random() < 0.5:
                    x += 1 if x0 > x else -1 if x0 < x else 0
        return img

    def spike():
        """A spike's stone, painted by height (each little face of a spike reads the rows level with it, the tip at
        the top): grit and moss at its foot; ridged dreamstone up its shaft, lit down one edge and shaded down the
        other; paler, weathered stone higher; a pale bone-sharp point with a glint. Blood runs down from the points,
        wet and bright at the top, browning as it goes, pooled in the ridges."""
        rng = random.Random(104)
        img = new()
        dark, mid, pale, point = FACE[0], FACE[1], FACE[2], LIP
        for y in range(16):
            h = 16 - y  # the height in the block this row is drawn at
            for x in range(16):
                if h <= 3:
                    c = fine(rng, dark if rng.random() < 0.6 else mid, 0.08)
                    if rng.random() < 0.12:
                        c = MOSS[rng.randrange(1, 3)]
                elif h <= 9:
                    c = mid if x % 3 else SHADOW          # ridges up the shaft
                    if x % 3 == 1:
                        c = pale                           # the lit edge of each ridge
                    c = fine(rng, c, 0.05)
                elif h <= 13:
                    c = fine(rng, pale if (x + y) % 4 else mid, 0.06)
                    if rng.random() < 0.08:
                        c = SHADOW                         # pits in the weathered stone
                else:
                    c = (178, 168, 186) if h < 16 else (214, 206, 220)
                    if x % 4 == 0:
                        c = (232, 226, 236)                # a glint on the point
                put(img, x, y, c)
        # Blood from the points, running down the ridges: bright and wet at the top, browner and thinner lower down.
        for x in range(16):
            if x % 3 == 2 or rng.random() < 0.25:
                length = rng.randint(3, 11)
                for k in range(length):
                    y = 1 + k
                    if y > 15:
                        break
                    t = k / max(1, length - 1)
                    col = (RED if t < 0.25 else CRIMSON if t < 0.6 else (96, 22, 30))
                    if k == length - 1:
                        col = (70, 16, 22)                 # a drop gathered at its end
                    put(img, x, y, col)
        return img

    def ladder():
        """A ladder of sleepwood: two rails, rungs pegged into them, the wood grey and split."""
        rng = random.Random(105)
        img = new()
        for y in range(16):
            for x in (1, 2, 13, 14):
                put(img, x, y, fine(rng, BARK[2] if x in (2, 13) else BARK[1], 0.06))
            if y % 4 == 2:
                for x in range(3, 13):
                    put(img, x, y, fine(rng, BARK[3] if hasattr(g, "BARK") and len(BARK) > 3 else BARK[2], 0.06))
                for x in range(3, 13):
                    put(img, x, y + 1, BARK[0])
                put(img, 3, y, (40, 34, 30))
                put(img, 12, y, (40, 34, 30))
        return img

    for name, draw in (("spike", spike), ("sleepwood_ladder", ladder), ("wrought_iron", wrought_iron), ("decor_stone", lambda: stone(97)), ("decor_stone_mossy", lambda: stone(98, True)),
                       ("urn", urn_band), ("planter_front", planter_front),
                       ("dead_shrub", dead_shrub)):
        save(name, draw())

    def tomb_lid():
        """The tomb's lid, all of it (32 by 64: two blocks across, four along; the head at the top): bevelled edges, a
        groove inside them studded with red runes, a skull at the head, a sword laid down its length, cracks, moss
        creeping over its foot. Cut into the eight parts' tiles below."""
        rng = random.Random(99)
        img = Image.new("RGBA", (32, 64))
        for y in range(64):
            for x in range(32):
                c = FACE[2] if (x * 3 + y * 7 + rng.randrange(5)) % 11 else FACE[1]
                if x < 2 or y < 2:
                    c = LIP
                elif x > 29 or y > 61:
                    c = SHADOW
                img.putpixel((x, y), fine(rng, c, 0.05) + (255,))
        for k in range(4, 28):
            img.putpixel((k, 4), GROOVE + (255,)); img.putpixel((k, 59), GROOVE + (255,))
        for k in range(4, 60):
            img.putpixel((4, k), GROOVE + (255,)); img.putpixel((27, k), GROOVE + (255,))
            if k % 6 == 0:
                img.putpixel((4, k), CRIMSON + (255,)); img.putpixel((27, k), CRIMSON + (255,))
        bone, bone_dark = (150, 140, 160, 255), (118, 110, 130, 255)
        for y in range(7, 15):
            for x in range(11, 21):
                d = ((x - 15.5) / 4.2) ** 2 + ((y - 10.5) / 3.6) ** 2
                if d <= 1.0:
                    img.putpixel((x, y), bone if y < 12 else bone_dark)
        for x, y in ((13, 10), (14, 10), (17, 10), (18, 10)):
            img.putpixel((x, y), CRIMSON + (255,))
        for x in (15, 16):
            img.putpixel((x, 12), GROOVE + (255,))
        for x in range(13, 19):
            img.putpixel((x, 15), bone if x % 2 else GROOVE + (255,))
        steel, steel_dark, steel_hi = (96, 94, 110, 255), (60, 58, 72, 255), (150, 150, 168, 255)
        for y in range(18, 23):
            img.putpixel((15, y), BARK[1] + (255,)); img.putpixel((16, y), BARK[2] + (255,))
        img.putpixel((15, 17), steel_hi); img.putpixel((16, 17), steel_hi)
        for x in range(10, 22):
            img.putpixel((x, 23), steel); img.putpixel((x, 24), steel_dark)
        for y in range(25, 55):
            img.putpixel((14, y), steel_dark); img.putpixel((15, y), steel_hi); img.putpixel((16, y), steel); img.putpixel((17, y), steel_dark)
        for y in range(55, 58):
            img.putpixel((15, y), steel_hi); img.putpixel((16, y), steel)
        x = 24
        for y in range(30, 46):
            img.putpixel((x, y), GROOVE + (255,))
            x += rng.choice((0, 0, 1, -1))
        for y in range(50, 62):
            for x in range(2, 30):
                if rng.random() < (y - 50) / 14:
                    img.putpixel((x, y), MOSS[rng.randrange(1, 4)] + (255,))
        return img

    def tomb_side():
        """The tomb's sides: panels framed by a groove, an arch at the top of each, moss along the foot."""
        rng = random.Random(100)
        img = stone(101, mossy=True)
        for x in range(2, 14):
            put(img, x, 3, GROOVE)
        for y in range(3, 13):
            put(img, 2, y, GROOVE)
            put(img, 13, y, GROOVE)
        for x in range(5, 11):
            put(img, x, 5 if 6 <= x <= 9 else 6, SHADOW)
        put(img, 7, 8, CRIMSON)
        put(img, 8, 8, CRIMSON)
        return img

    lid = tomb_lid()
    for si, side in enumerate(("left", "right")):
        for seg in range(4):
            save(f"tomb_lid_{side}_{seg}", lid.crop((si * 16, seg * 16, si * 16 + 16, seg * 16 + 16)))
    save("tomb_side", tomb_side())

    def tomb_inside():
        """The tomb's floor, seen when its lid is aside: dark earth, a scrap of rotted shroud."""
        rng = random.Random(102)
        img = new()
        for y in range(16):
            for x in range(16):
                c = (14, 10, 14) if rng.random() < 0.7 else (24, 18, 22)
                if rng.random() < 0.08:
                    c = (46, 40, 44)
                put(img, x, y, c)
        return img

    def tomb_bone():
        """Old bone: yellowed, darker in its pits and seams."""
        rng = random.Random(103)
        img = new()
        for y in range(16):
            for x in range(16):
                c = (196, 186, 160) if rng.random() < 0.6 else (172, 162, 136) if rng.random() < 0.7 else (140, 130, 108)
                put(img, x, y, c)
        return img

    save("tomb_inside", tomb_inside())
    save("tomb_bone", tomb_bone())

    # The dread lantern: black iron, smoked red glass, a candle and its flame; and its icon.
    def lantern_frame():
        """Blackened iron, hammered: dark, a cold sheen along its edges where it was handled, rust gone red in its
        pits, rivets."""
        rng = random.Random(121)
        img = new()
        for y in range(16):
            for x in range(16):
                c = IRON[0] if rng.random() < 0.55 else IRON[1]
                if x in (0, 15) or y in (0, 15):
                    c = IRON[2]
                if rng.random() < 0.05:
                    c = g.mix(CRIMSON, IRON[0], 0.45)  # rust in the pits
                put(img, x, y, fine(rng, c, 0.05))
        for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
            put(img, x, y, (120, 116, 132))
            put(img, x + 1, y + 1, IRON[0])
        return img

    def lantern_glass():
        """Smoked red glass, old and finely cracked: see-through, lit warm from within, darkest with soot at its top,
        a pale streak where it catches the light, a run of wax down it."""
        rng = random.Random(122)
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y in range(16):
            for x in range(16):
                # Lit from within: warmest round the flame's height, sooty only near its top.
                soot = max(0.0, 1.0 - y / 4.0)
                warm = max(0.0, 1.0 - abs(y - 10) / 7.0)
                r, gg, bb = g.mix(g.mix((176, 40, 46), (246, 120, 84), warm), (46, 12, 16), soot * 0.85)
                a = int(95 + 110 * soot + rng.randint(-8, 8))
                crack = (x * 5 + y * 3) % 23 == 0 and y > 4  # fine cracks in the old glass
                if crack:
                    r, gg, bb, a = r // 2, gg // 2, bb // 2, min(255, a + 60)
                elif x == 1 and 3 <= y <= 11:
                    r, gg, bb, a = 255, 170, 160, 150  # the streak of light
                img.putpixel((x, y), (r, gg, bb, max(0, min(255, a))))
        for y in range(9, 15):  # wax run down the glass
            img.putpixel((12, y), (210, 196, 176, 230))
        img.putpixel((11, 14), (210, 196, 176, 230))
        return img

    def lantern_wax():
        """Old candle wax, the colour of bone, guttered: drips down its sides, the top sunk round a black wick."""
        rng = random.Random(123)
        img = new()
        for y in range(16):
            for x in range(16):
                c = (196, 182, 160) if rng.random() < 0.7 else (176, 160, 138)
                if y > 11:
                    c = (150, 134, 112)
                put(img, x, y, fine(rng, c, 0.04))
        for x in (3, 9, 13):
            for y in range(rng.randint(5, 11)):
                put(img, x, y, (226, 214, 194))
        put(img, 7, 7, (20, 16, 14))
        put(img, 8, 7, (20, 16, 14))
        return img

    def lantern_flame():
        """A candle's flame (animated, dread_lantern_flame.png.mcmeta): a tear of light, white-gold at its heart, gold,
        orange, a red edge, drawn up into a point that sways and gutters."""
        frames = 8
        img = Image.new("RGBA", (16, 16 * frames), (0, 0, 0, 0))
        for f in range(frames):
            phase = f / frames * math.tau
            tall = 13.5 + 1.5 * math.sin(phase * 2)
            for y in range(16):
                v = (15.5 - y) / tall  # 0 at its root, 1 at its tip
                if v < 0 or v > 1:
                    continue
                sway = math.sin(phase + v * 2.5) * 1.3 * v * v
                half = 5.2 * (math.sin(math.pi * min(1.0, v * 1.15 + 0.08)) ** 0.8) * (1 - v) ** 0.35
                for x in range(16):
                    d = abs(x + 0.5 - 8 - sway)
                    if d > half:
                        continue
                    k = d / max(0.5, half) * 0.6 + v * 0.55  # out from the heart, up to the tip
                    c = (255, 250, 220) if k < 0.3 else (255, 214, 120) if k < 0.5 else (255, 140, 60) if k < 0.72 else (225, 50, 46)
                    img.putpixel((x, f * 16 + y), c + (255,))
        return img

    def lantern_icon():
        """The lantern as an item: its cap and hook, the glass with the flame bright behind it, its foot."""
        img = new()
        frame, glass, flame = IRON[1], (150, 30, 40), (255, 200, 110)
        for x in (7, 8):  # hook
            put(img, x, 0, IRON[2])
        put(img, 6, 1, IRON[2]); put(img, 9, 1, IRON[2])
        for y, (x0, x1) in ((2, (6, 9)), (3, (5, 10)), (4, (4, 11))):  # cap
            for x in range(x0, x1 + 1):
                put(img, x, y, IRON[2] if y == 2 else frame)
        put(img, 4, 3, IRON[2]); put(img, 11, 3, IRON[2])  # horns
        for y in range(5, 13):
            for x in range(4, 12):
                edge = x in (4, 11)
                c = frame if edge else glass
                if not edge and (x + y) % 6 == 0:
                    c = (40, 34, 44)
                if 6 <= x <= 9 and 7 <= y <= 11:
                    c = flame if 7 <= x <= 8 and 8 <= y <= 10 else (255, 120, 60)
                if 7 <= x <= 8 and y == 12:
                    c = (196, 182, 160)
                put(img, x, y, c)
        for x in range(3, 13):
            put(img, x, 13, IRON[2])
        for x in range(4, 12):
            put(img, x, 14, frame)
        return img

    save("dread_lantern_frame", lantern_frame())
    save("dread_lantern_glass", lantern_glass())
    save("dread_lantern_wax", lantern_wax())
    save("dread_lantern_flame", lantern_flame())
    g.write(os.path.join(tex_dir, "dread_lantern_flame.png.mcmeta"), {"animation": {"frametime": 2, "interpolate": True}})
    save("dread_lantern", lantern_icon())


    # ---- The labyrinth's own furnishings: candelabra, weeping basin, mourning statue, iron fence and gate, piers ----
    def pier_base_side():
        """The foot of a pier, moulded as a column's base: a plinth, a fat torus, a cut scotia in shadow, a thin torus,
        a fillet, and the shaft's flutes rising out of it."""
        rng = random.Random(131)
        img = new()
        rows = {15: SHADOW, 14: FACE[1], 13: FACE[2], 12: FACE[2], 11: LIP,            # the plinth, its top edge lit
                10: FACE[3], 9: mix(FACE[3], LIP, 0.5), 8: FACE[2], 7: FACE[1],          # the big torus, lit from above
                6: SHADOW, 5: GROOVE,                                                     # the scotia, cut deep
                4: LIP, 3: FACE[2], 2: SHADOW}                                            # the small torus and its fillet
        for y in range(16):
            for x in range(16):
                if y in rows:
                    c = rows[y]
                else:  # the shaft's flutes, as on the pillar
                    f = x % 4
                    c = LIP if f == 0 else FACE[2] if f == 1 else FACE[1] if f == 2 else SHADOW
                if x in (0, 15) and y >= 11:
                    c = SHADOW if x == 15 else LIP
                put(img, x, y, fine(rng, c, 0.04))
        for x in (3, 8, 12):  # chips knocked out of the plinth
            put(img, x, 13 + rng.randrange(2), SHADOW)
        return img

    def pier_capital_side():
        """The head of a pier: the flutes ending at an astragal, a bell of carved leaves with red veins in them, curled
        over at their tips, and a square abacus over all with a rune cut in its middle."""
        rng = random.Random(132)
        img = new()
        for y in range(16):
            for x in range(16):
                if y >= 14:  # the flutes' ends
                    f = x % 4
                    c = LIP if f == 0 else FACE[2] if f == 1 else FACE[1] if f == 2 else SHADOW
                elif y == 13:
                    c = LIP  # the astragal
                elif y == 12:
                    c = SHADOW
                elif y >= 4:  # the bell of leaves: four tall leaves, each ribbed down its middle, curled over at the top
                    k = x % 8
                    leaf = abs(k - 3.5)
                    top = 4 + int(leaf * 0.9)
                    if y < top:
                        c = SHADOW
                    elif leaf < 0.6:
                        c = mix(CRIMSON, FACE[1], 0.35) if y > 6 else FACE[3]  # the vein down its rib
                    else:
                        c = FACE[3] if leaf < 2 else FACE[2] if leaf < 3.2 else FACE[1]
                    if y == top and leaf < 3.5:
                        c = LIP  # the curl
                elif y == 3:
                    c = SHADOW
                else:  # the abacus
                    c = LIP if y == 0 else FACE[2] if y == 1 else FACE[1]
                put(img, x, y, fine(rng, c, 0.04))
        for x, y in ((7, 1), (8, 1), (6, 2), (9, 2), (7, 2), (8, 2)):  # the rune on the abacus
            put(img, x, y, GROOVE if y == 1 else mix(RED, CRIMSON, 0.5))
        return img

    def pier_end():
        """The end of a pier stone: bevelled, a ring and a cross cut in it."""
        rng = random.Random(133)
        img = new()
        for y in range(16):
            for x in range(16):
                c = LIP if x == 0 or y == 0 else SHADOW if x == 15 or y == 15 else FACE[2]
                r = math.hypot(x - 7.5, y - 7.5)
                if 4.4 < r < 5.4 or (abs(x - 7.5) < 0.6 and r < 4.4) or (abs(y - 7.5) < 0.6 and r < 4.4):
                    c = GROOVE
                put(img, x, y, fine(rng, c, 0.04))
        return img

    def candle_wax():
        """Candle wax gone the colour of old bone, drips frozen down it, a black wick."""
        rng = random.Random(134)
        img = new()
        for y in range(16):
            for x in range(16):
                c = (204, 190, 166) if (x + rng.randrange(3)) % 5 else (226, 214, 194)
                if y > 12:
                    c = (168, 150, 126)
                put(img, x, y, fine(rng, c, 0.04))
        put(img, 7, 0, (24, 18, 16))
        put(img, 8, 0, (24, 18, 16))
        return img

    def candle_flame():
        """A candle flame, animated (candle_flame.png.mcmeta): a small tear of light, white at its root, swaying."""
        frames = 6
        img = Image.new("RGBA", (16, 16 * frames), (0, 0, 0, 0))
        for f in range(frames):
            phase = f / frames * math.tau
            for y in range(16):
                v = (15.5 - y) / (12 + math.sin(phase * 2))
                if v < 0 or v > 1:
                    continue
                sway = math.sin(phase + v * 2) * 0.9 * v
                half = 3.4 * math.sin(math.pi * min(1.0, v * 1.1 + 0.08)) ** 0.7 * (1 - v) ** 0.4
                for x in range(16):
                    d = abs(x + 0.5 - 8 - sway)
                    if d > half:
                        continue
                    k = d / max(0.5, half) * 0.6 + v * 0.55
                    c = (255, 252, 230) if k < 0.3 else (255, 214, 120) if k < 0.55 else (255, 130, 60) if k < 0.75 else (220, 50, 46)
                    img.putpixel((x, f * 16 + y), c + (255,))
        return img

    def tears_surface():
        """The surface of a font of the garden's tears (animated, weeping_tears.png.mcmeta): dark blood red, rings
        spreading slowly from where tears fall into it."""
        frames = 8
        img = Image.new("RGBA", (16, 16 * frames), (0, 0, 0, 255))
        rng = random.Random(135)
        for f in range(frames):
            t = f / frames
            for y in range(16):
                for x in range(16):
                    r = math.hypot(x - 7.5, y - 7.5)
                    ring = max(0.0, 1 - abs(r - t * 9) * 1.2) * (1 - t)
                    ring2 = max(0.0, 1 - abs(math.hypot(x - 4, y - 11) - ((t + 0.5) % 1) * 7) * 1.4) * (1 - (t + 0.5) % 1)
                    c = mix((70, 6, 16), (150, 18, 34), min(1.0, ring + ring2 + rng.random() * 0.08))
                    img.putpixel((x, f * 16 + y), c + (255,))
        return img

    def basin_face():
        """The front of the basin's pedestal: a weeping face carved in it, eyes closed, a red tear run from each."""
        rng = random.Random(136)
        img = new()
        for y in range(16):
            for x in range(16):
                c = FACE[2] if (x * 3 + y * 5 + rng.randrange(4)) % 9 else FACE[1]
                if x in (0, 1) or x in (14, 15):
                    c = LIP if x == 1 else SHADOW
                put(img, x, y, fine(rng, c, 0.05))
        for x in range(4, 12):  # the brow
            put(img, x, 4, LIP)
        for x in (5, 6, 9, 10):  # closed eyes
            put(img, x, 6, GROOVE)
        for y in range(7, 15):  # tears
            put(img, 5, y, mix(RED, CRIMSON, y / 15))
            put(img, 10, y + (1 if y < 14 else 0), mix(RED, CRIMSON, y / 15))
        for x in (6, 7, 8, 9):  # the mouth, downturned
            put(img, x, 11 if x in (7, 8) else 12, GROOVE)
        put(img, 7, 8, SHADOW)
        put(img, 8, 9, SHADOW)
        return img

    # Weathered statue stone: pale grey with a little violet, darker in its folds and pits.
    STATUE = [(84, 76, 96), (118, 110, 128), (146, 138, 156), (172, 166, 182)]

    def statue_shadow():
        """The dark inside a mourner's hood, where its face would be."""
        img = new()
        for y in range(16):
            for x in range(16):
                put(img, x, y, (14, 10, 18) if (x + y) % 3 else (24, 16, 28))
        return img

    def statue_stone():
        """Weathered pale dreamstone for statues: soft, grainy, darker in the folds, moss low down."""
        rng = random.Random(137)
        img = new()
        for y in range(16):
            for x in range(16):
                c = mix(STATUE[1], STATUE[2], rng.random() * 0.7)
                if rng.random() < 0.08:
                    c = STATUE[0]  # pitted
                put(img, x, y, fine(rng, c, 0.05))
        return img

    def statue_robe():
        """A mourner's robe in stone: deep vertical folds, each lit on its left and shadowed on its right, frayed at the
        hem, a red tear-stain run down it."""
        rng = random.Random(138)
        img = new()
        for y in range(16):
            for x in range(16):
                f = (x + (1 if y > 8 else 0)) % 5
                c = STATUE[3] if f == 0 else STATUE[2] if f in (1, 2) else STATUE[1] if f == 3 else STATUE[0]
                if y == 15 and rng.random() < 0.5:
                    c = SHADOW
                put(img, x, y, fine(rng, c, 0.04))
        for y in range(2, 13):
            put(img, 9, y, mix(RED, CRIMSON, y / 12) if y > 3 else mix(FACE[3], RED, 0.5))
        return img

    def statue_hood():
        """The hood: smooth stone draped in a few long folds, the dark inside it."""
        rng = random.Random(139)
        img = new()
        for y in range(16):
            for x in range(16):
                f = x % 6
                c = STATUE[3] if f < 2 else STATUE[2] if f < 4 else STATUE[1]
                put(img, x, y, fine(rng, c, 0.04))
        return img

    def statue_plinth():
        """The statue's plinth: polished, a band of runes round it, the name worn away."""
        rng = random.Random(140)
        img = new()
        for y in range(16):
            for x in range(16):
                c = LIP if y == 0 else SHADOW if y == 15 else FACE[2]
                if 6 <= y <= 9:
                    c = FACE[1]
                    if (x + y) % 4 == 0 or (x * 7 + y) % 9 == 0:
                        c = GROOVE
                put(img, x, y, fine(rng, c, 0.04))
        return img

    def dark_iron():
        """Black wrought iron: hammered, a cold sheen along it, rust gone red in its pits."""
        rng = random.Random(141)
        img = new()
        for y in range(16):
            for x in range(16):
                c = IRON[0] if rng.random() < 0.6 else IRON[1]
                if x % 4 == 0:
                    c = IRON[2]
                if rng.random() < 0.04:
                    c = mix(CRIMSON, IRON[0], 0.5)
                put(img, x, y, fine(rng, c, 0.05))
        return img

    def dark_iron_icon(gate):
        """The fence and gate as items: posts with spear tips, bars between, a crest on the gate."""
        img = new()
        posts = (1, 14) if gate else (6, 7, 8, 9)
        for y in range(2, 16):
            for x in posts:
                put(img, x, y, IRON[1] if x in (1, 7, 14) else IRON[0])
        if gate:
            for x in range(2, 14):
                put(img, x, 5, IRON[2])
                put(img, x, 12, IRON[1])
            for x in (3, 5, 7, 8, 10, 12):
                for y in range(4, 15):
                    put(img, x, y, IRON[1])
                put(img, x, 3, IRON[2])
            for x in (6, 7, 8, 9):
                put(img, x, 2, mix(RED, CRIMSON, 0.5))
        else:
            for x in range(0, 16):
                if x not in posts:
                    put(img, x, 6, IRON[1])
                    put(img, x, 12, IRON[1])
            for x in (2, 13):
                for y in range(4, 15):
                    put(img, x, y, IRON[1])
                put(img, x, 3, IRON[2])
        for x in posts:
            put(img, x, 1, IRON[2])
        return img

    save("dreamstone_pier_base_side", pier_base_side())
    save("dreamstone_pier_capital_side", pier_capital_side())
    save("dreamstone_pier_end", pier_end())
    save("candle_wax", candle_wax())
    save("candle_flame", candle_flame())
    g.write(os.path.join(tex_dir, "candle_flame.png.mcmeta"), {"animation": {"frametime": 2, "interpolate": True}})
    save("weeping_tears", tears_surface())
    g.write(os.path.join(tex_dir, "weeping_tears.png.mcmeta"), {"animation": {"frametime": 4, "interpolate": True}})
    save("basin_face", basin_face())
    save("statue_stone", statue_stone())
    save("statue_robe", statue_robe())
    save("statue_hood", statue_hood())
    save("statue_plinth", statue_plinth())
    save("statue_shadow", statue_shadow())
    save("dark_iron", dark_iron())
    save("dread_iron_fence", dark_iron_icon(False))
    save("dread_iron_gate", dark_iron_icon(True))

    # ---- Models ----
    b = g.b

    def el(f, t, tex, up=None, down=None, rot=None, emit=0, faces=("north", "south", "east", "west", "up", "down"), uv=None, north=None):
        def face_tex(d):
            if d == "up" and up:
                return up
            if d == "down" and down:
                return down
            if d == "north" and north:
                return north
            return tex
        e = {"from": f, "to": t, "faces": {d: dict({"texture": "#" + face_tex(d)}, **({"uv": uv} if uv else {})) for d in faces}}
        if rot:
            e["rotation"] = rot
        if emit:
            e["light_emission"] = emit
        return e

    def cross(cx, cz, y0, y1, half, tex, emit=0, full=False):
        uv = [0, 0, 16, 16] if full else [0, max(0, 16 - (y1 - y0)), 16, 16]
        return [el([cx - half, y0, cz], [cx + half, y1, cz], tex, rot={"origin": [cx, y0, cz], "axis": "y", "angle": a}, emit=emit,
                   faces=("north", "south"), uv=uv) for a in (45, -45)]

    def ring(y0, y1, lo, hi, tex, inset=1):
        """A hollow square ring (four bars) from lo to hi on x and z."""
        return [el([lo, y0, lo], [hi, y1, lo + inset], tex), el([lo, y0, hi - inset], [hi, y1, hi], tex),
                el([lo, y0, lo + inset], [lo + inset, y1, hi - inset], tex), el([hi - inset, y0, lo + inset], [hi, y1, hi - inset], tex)]

    models = {}
    # Urn.
    models["dreamstone_urn"] = ({"stone": b("decor_stone"), "band": b("urn"), "iron": b("wrought_iron"), "particle": b("urn")}, [
        el([5, 0, 5], [11, 1, 11], "stone"), el([6, 1, 6], [10, 2, 10], "stone"),
        el([5, 2, 5], [11, 3, 11], "band"), el([4, 3, 4], [12, 7, 12], "band", up="stone"), el([5, 7, 5], [11, 9, 11], "band"),
        el([6, 9, 6], [10, 11, 10], "stone"), el([5, 11, 5], [11, 12, 11], "stone"),
        el([6, 12, 6], [10, 13, 10], "stone"), el([7.5, 13, 7.5], [8.5, 14, 8.5], "iron"),
        el([2.5, 5, 7], [4, 8, 9], "iron"), el([12, 5, 7], [13.5, 8, 9], "iron"),  # handles
        el([2.5, 8, 7], [5, 9, 9], "iron"), el([11, 8, 7], [13.5, 9, 9], "iron"),
    ])
    # Planter.
    models["stone_planter"] = ({"stone": b("decor_stone"), "front": b("planter_front"), "soil": b("ashen_moss_top"), "iron": b("wrought_iron"),
                                "thorns": b("despair_thorns"), "bloom": b("lucid_bloom"), "shrub": b("dead_shrub"),
                                "veins": b("weeping_veins_plant"), "particle": b("decor_stone")}, [
        el([1.5, 0, 3.5], [3, 1, 5], "iron"), el([13, 0, 3.5], [14.5, 1, 5], "iron"), el([1.5, 0, 11], [3, 1, 12.5], "iron"), el([13, 0, 11], [14.5, 1, 12.5], "iron"),
        el([1, 1, 3], [15, 6, 13], "stone", up="soil", north="front"),
        *ring(6, 7, 0.5, 15.5, "stone", inset=1.2),
        el([0.5, 5, 2.5], [15.5, 6, 3], "stone"),  # moulding under the rim, front
        *cross(4.5, 8, 6, 13, 3, "thorns"), *cross(8, 8, 6, 12, 3, "bloom", emit=9), *cross(11.5, 8, 6, 14, 3, "shrub"),
        el([3, 1, 2.9], [7, 6, 3], "veins", faces=("north",), uv=[4, 0, 8, 6]),  # veins spilling over the front edge
    ])
    models["stone_planter"][1][4]["faces"]["north"]["uv"] = [0, 4, 16, 14]
    # Brazier: filling its block: four splayed legs out at the corners, a ring tying them, a wide stepped bowl studded
    # round its belly with a rolled rim, coals, and a tall fire of two crossed flames and a smaller one inside.
    legs = []
    for x, z, ang in ((0.5, 0.5, -15), (13.5, 0.5, 15), (0.5, 13.5, -15), (13.5, 13.5, 15)):
        legs.append(el([x - 0.5, 0, z - 0.5], [x + 2.5, 1, z + 2.5], "iron"))  # foot
        legs.append(el([x + 0.5, 1, z + 0.5], [x + 1.5, 9, z + 1.5], "iron", rot={"origin": [x + 1, 1, z + 1], "axis": "z", "angle": ang}))
    models["dread_brazier"] = ({"iron": b("wrought_iron"), "coals": b("brazier_coals"), "fire": b("brazier_fire"), "particle": b("wrought_iron")},
                               legs + ring(4, 5, 1.5, 14.5, "iron") + [
        el([4, 7, 4], [12, 8, 12], "iron"), el([2.5, 8, 2.5], [13.5, 10, 13.5], "iron"), el([1, 10, 1], [15, 13, 15], "iron", up="coals"),
        *ring(13, 14.5, 0.5, 15.5, "iron", inset=1.5),
        *[el([7, 10.5, z], [9, 12.5, z + 0.5], "iron") for z in (0.5, 15)],  # studs
        *[el([x, 10.5, 7], [x + 0.5, 12.5, 9], "iron") for x in (0.5, 15)],
        el([2, 12.95, 2], [14, 13, 14], "coals", emit=12, faces=("up",)),
        *cross(8, 8, 13, 28, 7, "fire", emit=15, full=True), *cross(8, 8, 13, 22, 4.5, "fire", emit=15, full=True),
    ])
    # Fire lamp: a post four blocks tall on one block's footing, in three parts (FireLampBlock.Part).
    flutes = lambda y0, y1: [el([x, y0, z], [x + 1, y1, z + 1], "stone") for x, z in ((4.5, 4.5), (10.5, 4.5), (4.5, 10.5), (10.5, 10.5))]
    lamp_tex = {"stone": b("decor_stone"), "post": b("dreamstone_pillar"), "iron": b("wrought_iron"), "coals": b("brazier_coals"),
                "fire": b("brazier_fire"), "particle": b("dreamstone_pillar")}
    models["dread_fire_lamp_base"] = (lamp_tex, [
        el([1.5, 0, 1.5], [14.5, 1, 14.5], "stone"), el([2.5, 1, 2.5], [13.5, 2, 13.5], "stone"), el([3.5, 2, 3.5], [12.5, 3, 12.5], "stone"),
        el([5, 3, 5], [11, 16, 11], "post"), *flutes(3, 16), *ring(12, 13, 4, 12, "iron"),
    ])
    models["dread_fire_lamp_shaft"] = (lamp_tex, [
        el([5, 0, 5], [11, 16, 11], "post"), *flutes(0, 16), *ring(7, 9, 4, 12, "iron"),
    ])
    models["dread_fire_lamp_top"] = (lamp_tex, [
        el([4, 0, 4], [12, 2, 12], "stone"), el([2.5, 2, 2.5], [13.5, 3, 13.5], "iron", up="coals"),
        el([4, 2.95, 4], [12, 3, 12], "coals", emit=12, faces=("up",)),
        *[el([x, 3, z], [x + 1.5, 13, z + 1.5], "iron") for x, z in ((2.5, 2.5), (12, 2.5), (2.5, 12), (12, 12))],
        *ring(8, 9, 2.5, 13.5, "iron"),
        el([1.5, 13, 1.5], [14.5, 14, 14.5], "iron"), el([3, 14, 3], [13, 15, 13], "iron"), el([4.5, 15, 4.5], [11.5, 16, 11.5], "iron"),
        el([6, 16, 6], [10, 17, 10], "iron"), el([7.25, 17, 7.25], [8.75, 20, 8.75], "iron"), el([6.75, 20, 6.75], [9.25, 21.5, 9.25], "iron"),
        *cross(8, 8, 3, 13, 4.5, "fire", emit=15, full=True), *cross(8, 8, 3, 9, 2.5, "fire", emit=15, full=True),
    ])
    # The tomb: eight parts, each with its own tile of the lid (drawn by TombClient, so it can slide aside), hollow:
    # a plinth, a floor, walls round the hollow (panelled outside), and in it a skeleton laid out on its back, its skull
    # at the head. Laid out in the tomb's own space: X 0..32 across (left part 0..16), Z 0..64 from the head (part n at
    # 16n..16n+16); cut into the parts' blocks.
    shell = [((1, 2, 1), (31, 3, 63), "inside"),                     # the floor
             ((1, 3, 1), (3, 10, 63), "wall"), ((29, 3, 1), (31, 10, 63), "wall"),
             ((3, 3, 1), (29, 10, 3), "wall"), ((3, 3, 61), (29, 10, 63), "wall")]
    bones = [((13, 3, 5), (19, 7, 11), "bone"),                      # the skull, looking up
             ((14, 6.98, 7), (15.5, 7.02, 8.5), "dark"), ((16.5, 6.98, 7), (18, 7.02, 8.5), "dark"), ((15.5, 6.98, 9), (16.5, 7.02, 10), "dark"),
             ((14, 3, 11), (18, 4.5, 13), "bone"),                  # its jaw, fallen open
             ((15.5, 3, 13), (16.5, 4, 40), "bone")]                 # the spine
    for z in (15, 18, 21, 24, 27):                                   # ribs, arched over it
        bones += [((11, 3, z), (12.5, 4.5, z + 1), "bone"), ((12.5, 4.5, z), (19.5, 5.5, z + 1), "bone"), ((19.5, 3, z), (21, 4.5, z + 1), "bone")]
    for x0, x1, f0, f1 in ((8.5, 10, 9.5, 11), (22, 23.5, 21, 22.5)):  # arms, hands crossed in toward the hips
        bones += [((x0, 3, 14), (x1, 4, 26), "bone"), ((f0, 3, 26), (f1, 4, 36), "bone"), ((f0 - 0.5, 3, 36), (f1 + 0.5, 3.6, 38.5), "bone")]
    bones += [((12, 3, 39), (20, 5, 43), "bone")]                    # the pelvis
    for x0, x1 in ((12.5, 14), (18, 19.5)):                          # legs and feet
        bones += [((x0, 3, 43), (x1, 4.2, 53), "bone"), ((x0 + 0.2, 3, 53), (x1 - 0.2, 4, 59), "bone"), ((x0 - 1, 3, 59), (x1, 4.5, 61), "bone")]

    def cut(parts, si, seg):
        """The pieces of tomb-space boxes inside one part's block, in its own coordinates."""
        out = []
        X0, Z0 = si * 16, seg * 16
        for (ax, ay, az), (bx, by, bz), tex in parts:
            fx, tx = max(ax, X0), min(bx, X0 + 16)
            fz, tz = max(az, Z0), min(bz, Z0 + 16)
            if tx - fx <= 0 or tz - fz <= 0:
                continue
            out.append(el([fx - X0, ay, fz - Z0], [tx - X0, by, tz - Z0], tex))
        return out

    for si, side in enumerate(("left", "right")):
        for seg in range(4):
            outer = {"west" if side == "left" else "east"} | ({"north"} if seg == 0 else set()) | ({"south"} if seg == 3 else set())
            elements = [el([0, 0, 0], [16, 2, 16], "moss")]
            for e in cut(shell, si, seg):
                for d in ("north", "south", "east", "west"):
                    # The walls' outer faces are panelled; their inner faces worn stone.
                    if e["faces"][d]["texture"] == "#wall":
                        e["faces"][d] = {"texture": "#side" if d in outer else "#stone"}
                if e["faces"]["up"]["texture"] == "#wall":
                    e["faces"]["up"] = {"texture": "#stone"}
                if e["faces"]["down"]["texture"] == "#wall":
                    e["faces"]["down"] = {"texture": "#stone"}
                elements.append(e)
            tex_ = {"stone": b("decor_stone"), "side": b("tomb_side"), "moss": b("decor_stone_mossy"), "inside": b("tomb_inside"),
                    "bone": b("tomb_bone"), "dark": b("tomb_inside"), "particle": b("decor_stone")}
            # With its dead in it, and empty (once the Tomb Revenant has risen out of it).
            models[f"stone_tomb_{side}_{seg}"] = (tex_, list(elements))  # its dead is drawn by TombClient
            models[f"stone_tomb_{side}_{seg}_empty"] = (tex_, list(elements))

    # Dreamstone spikes: a cluster of points of different heights, each a stepped pyramid, turned a little about y.
    spikes = [el([0, 0, 0], [16, 1, 16], "stone", up="stone")]
    # Thin and sharp: five tapering steps each, needles more than towers.
    steps = ((1.2, 0.3), (0.9, 0.55), (0.6, 0.75), (0.35, 0.9), (0.15, 1.0))
    for cx, cz, tall, turn in ((3.5, 3.5, 12, 0), (11, 3, 10, 22.5), (7.5, 7.5, 15, 45), (3, 11.5, 11, -22.5),
                               (12.5, 9, 13, 0), (9.5, 13, 9, 22.5), (13, 13.5, 7, 45)):
        y0 = 1
        for half, top in steps:
            y1 = 1 + round(tall * top, 2)
            step = el([cx - half, y0, cz - half], [cx + half, y1, cz + half], "spike", rot={"origin": [cx, 0, cz], "axis": "y", "angle": turn})
            # Its top reads the texture level with it, as its sides do, not a patch from across the whole block.
            row = max(0.0, 16 - y1)
            step["faces"]["up"] = {"texture": "#spike", "uv": [cx - half, row, cx + half, min(16.0, row + 0.5)]}
            step["faces"]["down"] = {"texture": "#spike", "uv": [cx - half, 16 - y0 - 0.5, cx + half, 16 - y0]}
            spikes.append(step)
            y0 = y1
    models["dreamstone_spikes"] = ({"stone": b("dreamstone"), "spike": b("spike"), "particle": b("spike")}, spikes)


    # Candelabra: three feet under a stepped round base, a stem with a knot, two arms out and up from its head, drip
    # cups, three candles (the middle one taller) and their flames.
    candle = lambda cx, y0, y1: [el([cx - 0.75, y0, 7.25], [cx + 0.75, y1, 8.75], "wax"), *cross(cx, 8, y1, y1 + 3, 1.2, "flame", emit=15, full=True)]
    models["dread_candelabra"] = ({"iron": b("dark_iron"), "wax": b("candle_wax"), "flame": b("candle_flame"), "particle": b("dark_iron")}, [
        *[el([x - 1, 0, z - 1], [x + 1, 0.75, z + 1], "iron") for x, z in ((5, 6), (11, 6), (8, 11))],
        el([5, 0.5, 5], [11, 1.5, 11], "iron"), el([6, 1.5, 6], [10, 2.5, 10], "iron"),
        el([7.25, 2.5, 7.25], [8.75, 10, 8.75], "iron"), el([6.5, 5, 6.5], [9.5, 6.5, 9.5], "iron"),  # the stem and its knot
        el([3, 9, 7.4], [13, 10, 8.6], "iron"),                                                   # the arms
        el([3, 10, 7.4], [4, 11.5, 8.6], "iron"), el([12, 10, 7.4], [13, 11.5, 8.6], "iron"),       # turned up at the ends
        *[el([cx - 1.25, 11.5, 6.75], [cx + 1.25, 12.25, 9.25], "iron") for cx in (3.5, 12.5)],      # drip cups
        el([6.75, 10, 6.75], [9.25, 11, 9.25], "iron"),
        *candle(3.5, 12.25, 14.5), *candle(12.5, 12.25, 14.5), *candle(8, 11, 15),
    ])
    # Weeping basin: a stepped foot, a fluted stem with a weeping face carved on its front, a collar, and a wide bowl
    # with a rolled lip, full to near the brim of the garden's tears, which glow a little; tears run down its outside.
    models["weeping_basin"] = ({"stone": b("decor_stone"), "post": b("dreamstone_pillar"), "face": b("basin_face"), "tears": b("weeping_tears"),
                                "particle": b("decor_stone")}, [
        el([2, 0, 2], [14, 1.5, 14], "stone"), el([3, 1.5, 3], [13, 2.5, 13], "stone"),
        el([5, 2.5, 5], [11, 8, 11], "post", north="face"),
        el([4, 8, 4], [12, 9, 12], "stone"), el([2.5, 9, 2.5], [13.5, 10, 13.5], "stone"),
        el([1.5, 10, 1.5], [14.5, 11, 14.5], "stone"),                                            # the bowl's floor
        *ring(11, 12.5, 1, 15, "stone", inset=1.5), *ring(12.5, 13, 0.5, 15.5, "stone", inset=1.5),  # its wall and lip
        el([2.5, 11.8, 2.5], [13.5, 11.8, 13.5], "tears", emit=8, faces=("up",)),
        *[el([x, 9.5, 0.95], [x + 0.6, 12, 1], "tears", emit=8, faces=("north",)) for x in (4.2, 10.5)],  # tears run down
    ])
    # Mourning statue, two blocks: on a plinth with a band of runes, a hooded mourner in a long robe, bent a little,
    # its face hidden in its hands; a red stain where its tears ran.
    statue_tex = {"stone": b("statue_stone"), "robe": b("statue_robe"), "hood": b("statue_hood"), "plinth": b("statue_plinth"),
                  "shadow": b("statue_shadow"), "particle": b("statue_stone")}
    models["mourning_statue_lower"] = (statue_tex, [
        el([1, 0, 1], [15, 1.5, 15], "plinth"), el([2, 1.5, 2], [14, 2.5, 14], "stone"),        # the plinth and its step
        el([3, 2.5, 3.5], [13, 4, 12.5], "robe"),                                                 # the hem, spread on it
        el([3.5, 4, 4.5], [12.5, 16, 11.5], "robe"),                                              # the robe
        el([4.5, 2.5, 11.5], [11.5, 9, 13.5], "robe"),                                            # its train, trailing behind
        el([7, 4, 4], [9, 16, 4.5], "robe"),                                                      # a deep fold down its front
        el([4, 11, 3.6], [12, 16, 4.5], "robe"),                                                  # falling from the arms
    ])
    models["mourning_statue_upper"] = (statue_tex, [
        el([4, 0, 5], [12, 4, 11.5], "robe"),                                                     # the body
        el([3, 3.5, 5], [13, 6.5, 11], "robe"),                                                   # the shoulders
        el([5, 6, 6], [11, 12, 12], "hood"),                                                      # the hood, bowed
        el([5.5, 6.5, 4.6], [10.5, 12, 6], "hood"),                                               # its brim, deep
        el([6.5, 7, 4.5], [9.5, 11, 4.6], "shadow", faces=("north",)),                            # the dark inside it
        el([6, 11.5, 7], [10, 13.5, 11], "hood"),                                                 # its peak
        el([5, 0.5, 11.5], [11, 12, 12.6], "hood"),                                               # a veil down its back
        # The arms, raised from the shoulders to the face, and the hands pressed over it, fingers and all.
        el([3, 2.5, 3.5], [5.5, 9, 6.5], "robe", rot={"origin": [4.25, 4, 5], "axis": "x", "angle": 22.5}),
        el([10.5, 2.5, 3.5], [13, 9, 6.5], "robe", rot={"origin": [11.75, 4, 5], "axis": "x", "angle": 22.5}),
        el([5.6, 7.4, 3.6], [7.9, 10.2, 4.8], "stone"), el([8.1, 7.4, 3.6], [10.4, 10.2, 4.8], "stone"),
        *[el([x, 10.2, 3.8], [x + 0.55, 11.2, 4.6], "stone") for x in (5.8, 6.5, 7.2, 8.3, 9.0, 9.7)],
    ])

    turns = (("north", 0), ("east", 90), ("south", 180), ("west", 270))
    for name, (textures, elements) in models.items():
        g.write(os.path.join(g.ASSETS, "models", "block", name + ".json"), {"parent": "minecraft:block/block", "textures": textures, "elements": elements})
        if name.startswith("dread_fire_lamp_") or name.startswith("stone_tomb_") or name.startswith("mourning_statue_"):
            continue
        if name == "dreamstone_spikes":
            g.state(name, {"": {"model": b(name)}})
            g.item(name, b(name))
            continue
        g.state(name, {f"facing={f}": dict({"model": b(name)}, **({"y": y} if y else {})) for f, y in turns})
        g.item(name, b(name))
    g.state("dread_fire_lamp", {f"facing={f},part={p}": dict({"model": b("dread_fire_lamp_" + p)}, **({"y": y} if y else {}))
                                for f, y in turns for p in ("base", "shaft", "top")})
    g.item("dread_fire_lamp", b("dread_fire_lamp_top"))
    g.state("mourning_statue", {f"facing={f},half={h}": dict({"model": b("mourning_statue_" + h)}, **({"y": y} if y else {}))
                                for f, y in turns for h in ("lower", "upper")})
    # Its item: the whole statue, shrunk to a block.
    g.write(os.path.join(g.ASSETS, "models", "block", "mourning_statue_item.json"), {"parent": "minecraft:block/block",
        "textures": statue_tex, "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, -2, 0], "scale": [0.36, 0.36, 0.36]}},
        "elements": models["mourning_statue_lower"][1] + [dict(e, **{"from": [e["from"][0], e["from"][1] + 16, e["from"][2]],
                                                                     "to": [e["to"][0], e["to"][1] + 16, e["to"][2]]},
                                                                **({"rotation": dict(e["rotation"], origin=[e["rotation"]["origin"][0], e["rotation"]["origin"][1] + 16, e["rotation"]["origin"][2]])} if "rotation" in e else {}))
                                                          for e in models["mourning_statue_upper"][1]]})
    g.item("mourning_statue", b("mourning_statue_item"))

    # The dread iron fence: a square post capped with a finial and spear tip; to each side, two rails and two bars
    # between them with spear heads above.
    iron_tex = {"iron": b("dark_iron"), "particle": b("dark_iron")}
    spear = lambda x, z, y0, w: [el([x - w, y0, z - w], [x + w, y0 + 1.2, z + w], "iron"),
                                 el([x - w * 0.6, y0 + 1.2, z - w * 0.6], [x + w * 0.6, y0 + 2.6, z + w * 0.6], "iron",
                                    rot={"origin": [x, y0 + 1.9, z], "axis": "y", "angle": 45})]
    post = [el([6, 0, 6], [10, 1, 10], "iron"), el([6.5, 1, 6.5], [9.5, 15, 9.5], "iron"), el([6, 15, 6], [10, 16, 10], "iron"),
            *spear(8, 8, 16, 1.0)]
    side = [el([7.25, 4, 0], [8.75, 5.5, 6.5], "iron"), el([7.25, 12, 0], [8.75, 13.5, 6.5], "iron")]
    for z in (1.75, 4.5):
        side += [el([7.5, 1, z - 0.5], [8.5, 16, z + 0.5], "iron"), *spear(8, z, 16, 0.55)]
    for name, elements in (("dread_iron_fence_post", post), ("dread_iron_fence_side", side)):
        g.write(os.path.join(g.ASSETS, "models", "block", name + ".json"),
                {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": iron_tex, "elements": elements})
    g.write(os.path.join(g.ASSETS, "blockstates", "dread_iron_fence.json"), {"multipart": [{"apply": {"model": b("dread_iron_fence_post")}}] + [
        {"when": {d: "true"}, "apply": dict({"model": b("dread_iron_fence_side"), "uvlock": True}, **({"y": y} if y else {}))}
        for d, y in turns]})
    g.write(os.path.join(g.ASSETS, "items", "dread_iron_fence.json"), {"model": {"type": "minecraft:model", "model": f"{g.NS}:item/dread_iron_fence"}})
    g.write(os.path.join(g.ASSETS, "models", "item", "dread_iron_fence.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": b("dread_iron_fence")}})

    # The dread iron gate: two leaves of spear-headed bars in a frame between square posts with finials, a crest over
    # their meeting; open, each leaf swings in on its hinge.
    def leaf(x0, x1):
        e = [el([x0, 3, 7.25], [x1, 4.5, 8.75], "iron"), el([x0, 12.5, 7.25], [x1, 14, 8.75], "iron")]
        span = x1 - x0
        for k in range(3):
            x = x0 + span * (k + 0.5) / 3
            e += [el([x - 0.5, 1, 7.5], [x + 0.5, 15, 8.5], "iron"), *spear(x, 8, 15, 0.5)]
        return e

    posts = [el([0, 0, 6.5], [2, 16, 9.5], "iron"), el([14, 0, 6.5], [16, 16, 9.5], "iron"), *spear(1, 8, 16, 1.0), *spear(15, 8, 16, 1.0)]
    left, right = leaf(2, 8), leaf(8, 14)
    crest = [el([6.5, 14, 7.5], [9.5, 15, 8.5], "iron")]

    def swing(elements, hx, sign):
        """Turns a leaf a quarter round its hinge at (hx, 8), inward (toward +z)."""
        out = []
        for e in elements:
            pts = [(e["from"][0], e["from"][2]), (e["to"][0], e["to"][2])]
            turned = [(hx + (z - 8) * sign, 8 + (x - hx) * sign) for x, z in pts]
            xs, zs = [p[0] for p in turned], [p[1] for p in turned]
            e2 = dict(e, **{"from": [min(xs), e["from"][1], min(zs)], "to": [max(xs), e["to"][1], max(zs)]})
            if "rotation" in e:
                ox, oz = e["rotation"]["origin"][0], e["rotation"]["origin"][2]
                e2["rotation"] = dict(e["rotation"], origin=[hx + (oz - 8) * sign, e["rotation"]["origin"][1], 8 + (ox - hx) * sign])
            out.append(e2)
        return out

    closed = posts + left + right + crest
    opened = posts + swing(left, 2, 1) + swing(right, 14, -1)
    for name, elements in (("dread_iron_gate", closed), ("dread_iron_gate_open", opened)):
        g.write(os.path.join(g.ASSETS, "models", "block", name + ".json"),
                {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": iron_tex, "elements": elements})
    gate_turns = {"south": 0, "west": 90, "north": 180, "east": 270}
    g.state("dread_iron_gate", {f"facing={f},in_wall={w},open={o}": dict({"model": b("dread_iron_gate" + ("_open" if o == "true" else "")),
                                                                         "uvlock": True}, **({"y": y} if y else {}))
                                for f, y in gate_turns.items() for w in ("false", "true") for o in ("false", "true")})
    g.write(os.path.join(g.ASSETS, "items", "dread_iron_gate.json"), {"model": {"type": "minecraft:model", "model": f"{g.NS}:item/dread_iron_gate"}})
    g.write(os.path.join(g.ASSETS, "models", "item", "dread_iron_gate.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": b("dread_iron_gate")}})

    # The pier stones: columns, their sides moulded, their ends cut.
    for name in ("dreamstone_pier_base", "dreamstone_pier_capital"):
        g.write(os.path.join(g.ASSETS, "models", "block", name + ".json"),
                {"parent": "minecraft:block/cube_column", "textures": {"side": b(name + "_side"), "end": b("dreamstone_pier_end")}})
        g.write(os.path.join(g.ASSETS, "models", "block", name + "_horizontal.json"),
                {"parent": "minecraft:block/cube_column_horizontal", "textures": {"side": b(name + "_side"), "end": b("dreamstone_pier_end")}})
        g.state(name, {"axis=y": {"model": b(name)}, "axis=z": {"model": b(name + "_horizontal"), "x": 90},
                       "axis=x": {"model": b(name + "_horizontal"), "x": 90, "y": 90}})
        g.item(name, b(name))
    # The dread lantern, standing and hanging: a foot in two steps, four corner posts, smoked glass between them with a
    # band of iron round its middle, a candle guttering inside, its flame lit (and the glass glowing a little with
    # it), a stepped roof with four little horns at its corners and a finial, and a hook; hanging, a link of chain up to
    # the block above. Its bloom of light is drawn by DreadLanternClient.
    lantern_tex = {"frame": b("dread_lantern_frame"), "glass": b("dread_lantern_glass"), "wax": b("dread_lantern_wax"),
                   "flame": b("dread_lantern_flame"), "particle": b("dread_lantern_frame")}

    def pane(axis, at, lo, hi, y0, y1):
        if axis == "z":
            return el([lo, y0, at], [hi, y1, at], "glass", emit=10, faces=("north", "south"), uv=[0, 16 - (y1 - y0), hi - lo, 16])
        return el([at, y0, lo], [at, y1, hi], "glass", emit=10, faces=("east", "west"), uv=[0, 16 - (y1 - y0), hi - lo, 16])

    lantern = [
        el([4.5, 0, 4.5], [11.5, 1, 11.5], "frame"), el([5, 1, 5], [11, 1.5, 11], "frame"),
        *[el([x, 1.5, z], [x + 1, 9.5, z + 1], "frame") for x, z in ((4.75, 4.75), (10.25, 4.75), (4.75, 10.25), (10.25, 10.25))],
        pane("z", 5.25, 5.75, 10.25, 1.5, 9.5), pane("z", 10.75, 5.75, 10.25, 1.5, 9.5),
        pane("x", 5.25, 5.75, 10.25, 1.5, 9.5), pane("x", 10.75, 5.75, 10.25, 1.5, 9.5),
        *ring(5, 5.5, 4.5, 11.5, "frame", inset=0.5),  # the band: clear of the posts' and the glass's planes
        el([7, 1.5, 7], [9, 4, 9], "wax", up="wax"), el([6.7, 1.5, 7.6], [7, 2.5, 8.4], "wax"),  # the candle, a drip down it
        *cross(8, 8, 4, 8.5, 1.6, "flame", emit=15, full=True),
        el([4, 9.5, 4], [12, 10.5, 12], "frame"), el([4.75, 10.5, 4.75], [11.25, 11.5, 11.25], "frame"),
        el([5.75, 11.5, 5.75], [10.25, 12.25, 10.25], "frame"), el([6.75, 12.25, 6.75], [9.25, 13, 9.25], "frame"),
        *[el([x, 10.5, z], [x + 0.75, 11.75, z + 0.75], "frame") for x, z in ((4, 4), (11.25, 4), (4, 11.25), (11.25, 11.25))],  # horns
        el([7.4, 13, 7.4], [8.6, 14, 8.6], "frame"),  # finial
    ]
    hook = [el([6.5, 14, 8], [9.5, 16, 8], "frame", faces=("north", "south"), uv=[0, 0, 3, 2], rot={"origin": [8, 14, 8], "axis": "y", "angle": a})
            for a in (45, -45)]
    for name, extra in (("dread_lantern", hook), ("dread_lantern_hanging", hook)):
        g.write(os.path.join(g.ASSETS, "models", "block", f"{name}.json"),
                {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": lantern_tex, "elements": lantern + extra})
    g.state("dread_lantern", {"hanging=false": {"model": b("dread_lantern")}, "hanging=true": {"model": b("dread_lantern_hanging")}})
    # The sleepwood ladder: the vanilla ladder's shape with its own wood, flat against the block it hangs on.
    g.write(os.path.join(g.ASSETS, "models", "block", "sleepwood_ladder.json"), {"parent": "minecraft:block/block",
        "textures": {"texture": b("sleepwood_ladder"), "particle": b("sleepwood_ladder")}, "elements": [
            {"from": [0, 0, 15.2], "to": [16, 16, 15.2], "shade": False, "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#texture"},
                                                                                  "south": {"uv": [16, 0, 0, 16], "texture": "#texture"}}}]})
    g.state("sleepwood_ladder", {f"facing={f}": dict({"model": b("sleepwood_ladder")}, **({"y": y} if y else {})) for f, y in turns})
    g.write(os.path.join(g.ASSETS, "items", "sleepwood_ladder.json"), {"model": {"type": "minecraft:model", "model": f"{g.NS}:item/sleepwood_ladder"}})
    g.write(os.path.join(g.ASSETS, "models", "item", "sleepwood_ladder.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": b("sleepwood_ladder")}})
    g.state("stone_tomb", {f"facing={f},occupied={o},open={op},segment={seg},side={side}":
                           dict({"model": b(f"stone_tomb_{side}_{seg}" + ("" if o == "true" else "_empty"))}, **({"y": y} if y else {}))
                           for f, y in turns for side in ("left", "right") for seg in range(4) for o in ("true", "false") for op in ("true", "false")})
    # Its item: a small whole tomb with its lid on.
    g.write(os.path.join(g.ASSETS, "models", "block", "stone_tomb_item.json"), {"parent": "minecraft:block/block",
        "textures": {"stone": b("decor_stone"), "side": b("tomb_side"), "lid": b("tomb_lid_left_0"), "particle": b("decor_stone")},
        "elements": [el([2, 0, 0], [14, 9, 16], "side"), dict(el([1.5, 9, -0.5], [14.5, 11, 16.5], "stone", up="lid"))]})
    g.item("stone_tomb", b("stone_tomb_item"))
