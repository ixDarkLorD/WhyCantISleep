"""Draws each Dark Between boss's own boss bar (every boss gets one of its own, never the game's).

    python tools/gen_boss_bars.py

Each boss's bar is one texture, assets/whycantisleep/textures/gui/boss/<boss>.png, laid out as BossBars.java reads it:
    frame   (0, 0)    240 x 30   the whole bar: emblem, the track's frame and what dresses it
    fill    (0, 32)   180 x 7    its health, drawn as wide as it has left
    trail   (0, 40)   180 x 7    what it lost a moment ago, draining after
    glow    (184, 32)  12 x 12   a glow over the emblem's eye, pulsing
    kindled (200, 32)   7 x 13   the half-way notch, lit, once the boss has passed it
The track's inside is 180 x 7 at (35, 12) in the frame; the half-way notch sits over it at x = 35 + 90.

The Weeping Gardener: at the left a medallion of its mask, hooded, one red eye weeping, a crown of dead branches
through the ring; the track framed in dead sleepwood, a thorned vine winding along it with leaves and red buds; a
scythe's blade curving off its right end. Its health is crimson sap, glossy, veined; what it loses drains away pale.
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "whycantisleep", "textures", "gui", "boss")

OUTLINE = (10, 8, 12)
BARK = [(34, 30, 40), (50, 45, 58), (68, 61, 78), (92, 84, 104)]
LEAF = [(20, 30, 24), (34, 48, 36), (54, 70, 50)]
BONE = [(150, 142, 138), (186, 178, 172), (220, 214, 208), (240, 236, 230)]
STEEL = [(40, 39, 48), (74, 73, 86), (130, 130, 144), (200, 202, 214), (240, 240, 248)]
CRIMSON = (138, 10, 36)
RED = (255, 45, 70)
HOT = (255, 190, 180)
SAP = [(255, 140, 150), (236, 52, 78), (196, 26, 52), (156, 14, 40), (112, 8, 30), (80, 4, 22)]


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(c, f):
    return tuple(max(0, min(255, int(c[i] * f))) for i in range(3))


class Canvas:
    def __init__(self, w, h):
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))

    def put(self, x, y, c, a=255):
        if 0 <= x < self.img.width and 0 <= y < self.img.height:
            self.img.putpixel((int(x), int(y)), tuple(c[:3]) + (a,))

    def get(self, x, y):
        return self.img.getpixel((int(x), int(y)))

    def outline(self, colour=OUTLINE):
        """A one-pixel dark outline round everything drawn, as the game's own GUI art has."""
        w, h = self.img.size
        edge = []
        for y in range(h):
            for x in range(w):
                if self.img.getpixel((x, y))[3] == 0 and any(
                        0 <= x + dx < w and 0 <= y + dy < h and self.img.getpixel((x + dx, y + dy))[3] > 0
                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    edge.append((x, y))
        for x, y in edge:
            self.put(x, y, colour)


def gardener():
    rng = random.Random(5)
    frame = Canvas(240, 30)
    put = frame.put
    tx, ty, tw, th = 35, 12, 180, 7

    # The track's frame: dead sleepwood, lit along its top, shadowed below, grain along it.
    for y in range(ty - 3, ty + th + 3):
        for x in range(tx - 3, tx + tw + 3):
            inside = tx <= x < tx + tw and ty <= y < ty + th
            if inside:
                continue
            row = y - (ty - 3)
            c = BARK[2] if row <= 1 else BARK[1] if row < th + 4 else BARK[0]
            if (x * 2 + y) % 9 == 0:
                c = shade(c, 0.8)
            if row == 0:
                c = BARK[3]
            put(x, y, c)
    # The track's inside: empty, a dark red-black well, shadowed at its top.
    for y in range(ty, ty + th):
        for x in range(tx, tx + tw):
            put(x, y, (14, 4, 8) if y == ty else (26, 8, 14))
    # The thorned vine, winding over and under the frame, with thorns, leaves and red buds.
    for x in range(tx - 2, tx + tw + 2):
        for phase, base in ((0.0, ty - 4), (math.pi, ty + th + 3)):
            y = base + round(math.sin(x * 0.22 + phase) * 1.2)
            put(x, y, BARK[1])
            put(x, y + (1 if base < ty else -1), BARK[0])
            if x % 9 == 4:
                spike = -1 if base < ty else 1
                put(x, y + spike, BARK[3])
                put(x, y + spike * 2, BARK[2])
            if x % 23 == 11:
                for dx, dy in ((0, 0), (1, 0), (0, -1 if base < ty else 1), (1, -1 if base < ty else 1)):
                    put(x + dx + 1, y + dy - (1 if base < ty else -1), LEAF[2] if (dx, dy) == (0, 0) else LEAF[1])
            if x % 31 == 17:
                put(x, y - (1 if base < ty else -1), RED)
    # The half-way notch: a thorn driven down across the track.
    nx = tx + tw // 2
    for y in range(ty - 4, ty + th + 2):
        put(nx, y, BARK[3] if y < ty else (60, 52, 70))
        put(nx + 1, y, BARK[1] if y < ty else (34, 28, 40))
    put(nx, ty - 5, BARK[2])

    # The scythe's blade off the right end, set square on the track: as tall as the frame where it leaves it, centred
    # on the track's middle, tapering to a point that curls a little up; a dark spine above, steel brightening down to
    # a honed lower edge, and an iron collar where it meets the frame.
    mid = ty + th / 2 - 0.5
    length = 18
    for k in range(length):
        s_ = k / (length - 1)
        x = tx + tw + 4 + k
        centre = mid - 2.2 * s_ ** 2
        half = (th / 2 + 2.5) * (1 - s_) ** 0.85 + 0.4
        top, bottom = round(centre - half), round(centre + half)
        for y in range(top, bottom + 1):
            t = (y - top) / max(bottom - top, 1)
            c = mix(STEEL[1], STEEL[3], t)
            if y == bottom:
                c = STEEL[4]
            elif y == top:
                c = STEEL[0]
            put(x, y, c)
    for y in range(ty - 3, ty + th + 3):
        put(tx + tw + 3, y, STEEL[2] if y in (ty - 3, ty + th + 2) else STEEL[1])
        put(tx + tw + 2, y, BARK[2] if y < ty + 2 else BARK[1])
    # The emblem: a ring of twisted bark, a crown of dead branches through it, and inside, the hooded mask, one
    # eye red and weeping.
    cx, cy, R = 15, 15, 14
    for y in range(30):
        for x in range(31):
            d = math.hypot(x - cx, y - cy)
            a = math.atan2(y - cy, x - cx)
            if R - 3 <= d <= R:
                twist = (a * 6 / math.pi + d * 0.5) % 2 < 1
                c = BARK[2] if twist else BARK[1]
                if d > R - 1:
                    c = shade(c, 0.75 if y > cy else 1.15)
                put(x, y, c)
            elif d < R - 3:
                put(x, y, mix(LEAF[0], (8, 10, 9), d / (R - 3)))
    for x0, lean in ((cx - 5, -1), (cx, 0), (cx + 5, 1)):
        x = x0
        for k in range(5):
            put(x, 1 - k + 1, BARK[3] if k == 0 else BARK[2])
            if k % 2 == 1:
                x += lean
    # The mask: pale, bevelled, under the shadow of the hood.
    for y in range(9, 23):
        for x in range(10, 21):
            dx = (x - 15) / 5.2
            dy = (y - 15.5) / 6.8
            if dx * dx + dy * dy <= 1.0:
                c = mix(BONE[3], BONE[1], (y - 9) / 14)
                if y <= 10:
                    c = shade(c, 0.6)
                if x <= 11 and y < 18:
                    c = mix(c, BONE[3], 0.3)
                put(x, y, c)
    for ex, lit in ((12, True), (17, False)):
        for y in (13, 14):
            for x in (ex, ex + 1):
                put(x, y, (12, 6, 10))
        if lit:
            put(ex, 14, RED)
            put(ex + 1, 14, CRIMSON)
            for y in range(15, 22):
                put(ex, y, CRIMSON if y < 21 else (90, 8, 26))
    for x in (14, 15, 16):
        put(x, 19, (60, 48, 50))
    frame.outline()

    sheet = Canvas(240, 48)
    sheet.img.paste(frame.img, (0, 0))
    # The fill: crimson sap, a glossy highlight along its top, darkening down, veins and a few hot points in it.
    for y in range(7):
        for x in range(180):
            c = SAP[min(y, 5)] if y else SAP[0]
            if y == 1:
                c = mix(SAP[1], SAP[0], 0.3)
            if x % 13 == 6 and 2 <= y <= 4:
                c = mix(c, SAP[0], 0.35)
            if rng.random() < 0.02 and y in (2, 3):
                c = HOT
            sheet.put(x, 32 + y, c)
    # The trail: what it lost, pale as bone, draining.
    for y in range(7):
        for x in range(180):
            sheet.put(x, 40 + y, mix(BONE[3], BONE[0], y / 6) if y else BONE[3])
    # The eye's glow, soft, for pulsing over the emblem.
    for y in range(12):
        for x in range(12):
            d = math.hypot(x - 5.5, y - 5.5)
            if d < 6:
                sheet.put(184 + x, 32 + y, RED, int(255 * (1 - d / 6) ** 1.6))
    # The notch, kindled: the thorn burning red-hot.
    for y in range(13):
        sheet.put(200, 32 + y, HOT if 4 <= y <= 9 else RED)
        sheet.put(201, 32 + y, RED if 4 <= y <= 9 else CRIMSON)
    os.makedirs(OUT, exist_ok=True)
    sheet.img.save(os.path.join(OUT, "gardener.png"))
    return sheet


if __name__ == "__main__":
    gardener()
    print("wrote", os.path.relpath(OUT, ROOT))
