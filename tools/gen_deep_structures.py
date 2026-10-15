"""Builds the Lucid Deep's structures and writes them as structure templates, with the worldgen files that place them.

    python tools/gen_deep_structures.py

Every structure carries its own floating island under it, so it can hang anywhere in the void rather than sit on the
terrain. Each one is built block by block here (a Build), written as a structure template (.nbt), and given a
structure, a structure set, a template pool and chest loot.

    haunted_house       a two-storey sleepwood house rotting on its island: porch, parlour, bedrooms, a cellar
    garden_of_despair   a huge garden: a plaza round a dead great tree and a dry fountain at its heart, and round that
                        a labyrinth of withered hedge, with chests in its dead ends
    watching_shrine     an obelisk with an eye carved in it, on a small island, an offering bowl at its foot
    broken_bedroom      a piece of someone's bedroom, torn away and floating: half a floor, two walls, a bed, a chest
    hollow_clocktower   a tall ruined tower with a stair inside and a broken clock face, a chest at the top
    dream_castle        the Lord's castle (a placeholder for the final boss): four floors of dungeon rooms and halls
                        up to the boss hall, and on its roof the dais whose beam feeds the vortex above it

The idol of the final boss is left out until its model is settled.
"""
import gzip
import io
import json
import math
import os
import random
import struct

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
DATA = os.path.join(ROOT, "common", "src", "main", "resources", "data", "whycantisleep")
NS = "whycantisleep"
DATA_VERSION = 4790


# ---- NBT ----
class Tag:
    def __init__(self, kind, value):
        self.kind = kind
        self.value = value


def Int(v):
    return Tag(3, v)


def Float(v):
    return Tag(5, float(v))


def Double(v):
    return Tag(6, float(v))


def Str(v):
    return Tag(8, v)


def List(kind, items):
    return Tag(9, (kind, items))


def Compound(d):
    return Tag(10, d)


def _payload(out, tag):
    k, v = tag.kind, tag.value
    if k == 1:
        out.write(struct.pack(">b", v))
    elif k == 2:
        out.write(struct.pack(">h", v))
    elif k == 4:
        out.write(struct.pack(">q", v))
    elif k == 3:
        out.write(struct.pack(">i", v))
    elif k == 5:
        out.write(struct.pack(">f", v))
    elif k == 6:
        out.write(struct.pack(">d", v))
    elif k == 8:
        b = v.encode("utf-8")
        out.write(struct.pack(">H", len(b)) + b)
    elif k == 9:
        kind, items = v
        out.write(struct.pack(">bi", kind if items else 0, len(items)))
        for item in items:
            _payload(out, item)
    elif k == 10:
        for name, child in v.items():
            out.write(struct.pack(">b", child.kind))
            nb = name.encode("utf-8")
            out.write(struct.pack(">H", len(nb)) + nb)
            _payload(out, child)
        out.write(b"\x00")


def write_nbt(path, root):
    out = io.BytesIO()
    out.write(b"\x0a\x00\x00")
    _payload(out, root)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(gzip.compress(out.getvalue()))


def to_nbt(value):
    """A plain Python value (from a block entity's data) as a tag."""
    if isinstance(value, Tag):
        return value
    if isinstance(value, bool):
        return Tag(1, 1 if value else 0)
    if isinstance(value, int):
        return Int(value)
    if isinstance(value, str):
        return Str(value)
    if isinstance(value, dict):
        return Compound({k: to_nbt(v) for k, v in value.items()})
    raise TypeError(value)


# ---- Building ----
def W(name):
    return name if ":" in name else f"{NS}:{name}"


class Build:
    def __init__(self, seed):
        self.blocks = {}
        self.entities = {}
        # Mobs standing in the template: (x, y, z, nbt) with their own place within the block.
        self.mobs = []
        self.rng = random.Random(seed)

    def set(self, x, y, z, name, nbt=None, **props):
        self.blocks[(x, y, z)] = (W(name), tuple(sorted((k, str(v).lower()) for k, v in props.items())))
        if nbt is not None:
            self.entities[(x, y, z)] = nbt
        else:
            self.entities.pop((x, y, z), None)

    def get(self, x, y, z):
        return self.blocks.get((x, y, z), (None,))[0]

    def air(self, x, y, z):
        self.set(x, y, z, "minecraft:air")

    def fill(self, x0, y0, z0, x1, y1, z1, name, **props):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, **props)

    def clear(self, x0, y0, z0, x1, y1, z1):
        self.fill(x0, y0, z0, x1, y1, z1, "minecraft:air")

    def walls(self, x0, y0, z0, x1, y1, z1, name, **props):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    if x in (x0, x1) or z in (z0, z1):
                        self.set(x, y, z, name, **props)

    def chest(self, x, y, z, table, facing="north"):
        self.set(x, y, z, "minecraft:chest", nbt={"id": "minecraft:chest", "LootTable": f"{NS}:chests/{table}"},
                 facing=facing, type="single", waterlogged=False)

    def decay(self, chance, swaps):
        """Weathers the build: each block named in swaps turns, by chance, into one of what it is swapped for."""
        for pos, (name, props) in list(self.blocks.items()):
            if name in swaps and self.rng.random() < chance:
                choice = self.rng.choice(swaps[name])
                self.blocks[pos] = (W(choice), props if W(choice).endswith(name.split(":")[1].split("_")[-1]) else ())

    def island(self, cx, cz, top, rx, rz, depth, solid_top=True):
        """A floating island under a build: an uneven disc of moss and soil on top, tapering underneath to a ragged
        point of dreamstone, veined here and there, with hanging veins under its rim."""
        rng = self.rng
        bumps = [rng.uniform(0.82, 1.12) for _ in range(16)]
        for x in range(cx - rx - 2, cx + rx + 3):
            for z in range(cz - rz - 2, cz + rz + 3):
                a = math.atan2(z - cz, x - cx)
                i = (a + math.pi) / (2 * math.pi) * 16
                bump = bumps[int(i) % 16] * (1 - (i % 1)) + bumps[(int(i) + 1) % 16] * (i % 1)
                d = math.hypot((x - cx) / rx, (z - cz) / rz) / bump
                if d > 1.0:
                    continue
                below = int(depth * (1 - d) ** 0.8 + rng.random() * 2)
                for y in range(top - below, top + 1):
                    if y == top:
                        name = "ashen_moss" if solid_top else "ashen_soil"
                    elif y >= top - 2:
                        name = "ashen_soil"
                    else:
                        name = "veined_dreamstone" if rng.random() < 0.06 else "dreamstone"
                    if (x, y, z) not in self.blocks:
                        self.set(x, y, z, name)
                if d > 0.75 and rng.random() < 0.25:
                    self.set(x, top - below - 1, z, "hanging_veins", waterlogged=False)

    def broad_island(self, c, half, depth, lobe_width, lobe_reach, flat=0, causeway=0):
        """A great island to carry a walled build: a rounded square (so its corners reach past the walls' corners
        too), its edge ragged, and at the middle of each side a broad lobe reaching out, where the ways in are. Moss
        and soil on top, a few blocks of soil, then dreamstone, veined here and there, tapering below in uneven
        steps to a heavy, ragged keel; hanging veins under its rim. Its top rolls in low hills and hollows, but stays
        level over the square of `flat` round the middle (where the build stands) and along a causeway `causeway`
        each side of the axes (the ways in), easing into them; and it falls away a little to its rim. The top's
        height at each column is kept in self.heights."""
        rng = self.rng
        bumps = [rng.uniform(0.94, 1.08) for _ in range(32)]
        phases = [rng.uniform(0, math.tau) for _ in range(6)]
        self.heights = {}

        def relief(x, z, inside):
            out = max(abs(x - c), abs(z - c)) - flat
            ease = min(max(out / 8.0, 0.0), 1.0)
            across = min(abs(x - c), abs(z - c))
            road = min(max((across - causeway) / 5.0, 0.0), 1.0)
            ease *= road
            wave = (math.sin(x * 0.11 + phases[0]) * math.sin(z * 0.09 + phases[1]) * 1.4
                    + math.sin((x + z) * 0.07 + phases[2]) * 0.9 + math.sin((x - z) * 0.16 + phases[3]) * 0.5
                    + math.sin(x * 0.23 + z * 0.05 + phases[4]) * 0.35)
            height = ease * (wave * 2.2 + 1.3)
            if inside < 0.14:
                height -= (0.14 - inside) * 22.0 * road
            return max(-3, min(6, round(height)))
        reach = half + lobe_reach + 4
        for x in range(c - reach, c + reach + 1):
            for z in range(c - reach, c + reach + 1):
                dx, dz = (x - c) / half, (z - c) / half
                a = math.atan2(dz, dx)
                i = (a + math.pi) / (2 * math.pi) * 32
                bump = bumps[int(i) % 32] * (1 - (i % 1)) + bumps[(int(i) + 1) % 32] * (i % 1)
                square = (abs(dx) ** 8 + abs(dz) ** 8) ** 0.125 / bump
                # How far in it is, 0 at its edge to 1 at its middle; the lobes count as its edge, thin.
                inside = 1.0 - square
                for along, across in ((abs(x - c), abs(z - c)), (abs(z - c), abs(x - c))):
                    # The lobe starts a little inside the square, so where the square's ragged edge dips in under
                    # it there is no seam.
                    t = max((along - half) / lobe_reach, 0.0) if along >= half * 0.8 else -1.0
                    if 0.0 <= t <= 1.0:
                        width = lobe_width * bump * math.sqrt(1.0 - t * t)
                        if across <= width:
                            inside = max(inside, 0.12 * (1.0 - t) * (1.0 - across / max(width, 1.0)))
                if inside < 0.0:
                    continue
                below = int(4 + depth * inside ** 1.4 + rng.random() * 3)
                top = relief(x, z, inside)
                self.heights[(x, z)] = top
                for y in range(-below, top + 1):
                    if y == top:
                        name = "ashen_moss"
                    elif y >= top - 3:
                        name = "ashen_soil"
                    else:
                        name = "veined_dreamstone" if rng.random() < 0.04 else "dreamstone"
                    if (x, y, z) not in self.blocks:
                        self.set(x, y, z, name)
                if inside < 0.12 and rng.random() < 0.25:
                    self.set(x, -below - 1, z, "hanging_veins", waterlogged=False)

    def jigsaw(self, x, y, z, orientation, name, target, pool, final_state, joint="rollable", placement=0, selection=0):
        """A jigsaw block: where a structure piece joins another. It becomes final_state once the structure is built."""
        self.set(x, y, z, "minecraft:jigsaw", nbt={"id": "minecraft:jigsaw", "name": name, "target": target, "pool": pool,
                                                   "final_state": final_state, "joint": joint, "placement_priority": placement,
                                                   "selection_priority": selection}, orientation=orientation)

    def state(self, x, y, z):
        """The block at a place as a block state string, for a jigsaw's final state."""
        name, props = self.blocks.get((x, y, z), ("minecraft:air", ()))
        return name + ("[" + ",".join(f"{k}={v}" for k, v in props) + "]" if props else "")

    def sub(self, keep):
        """A build of only the blocks (and mobs) at places keep(x, y, z) is true for."""
        out = Build(0)
        out.blocks = {p: v for p, v in self.blocks.items() if keep(*p)}
        out.entities = {p: v for p, v in self.entities.items() if p in out.blocks}
        out.mobs = [m for m in self.mobs if keep(int(m[0] // 1), int(m[1] // 1), int(m[2] // 1))]
        return out

    def export(self, name, bounds=None):
        """Writes the build as a structure template. bounds ((x0, y0, z0), (x1, y1, z1)), inclusive, fixes its box (a
        jigsaw piece's box is what it claims); otherwise the box is just what the blocks fill."""
        if bounds:
            (ox, oy, oz), (mx, my, mz) = bounds
            self.blocks = {p: v for p, v in self.blocks.items() if ox <= p[0] <= mx and oy <= p[1] <= my and oz <= p[2] <= mz}
            xs, ys, zs = [ox, mx], [oy, my], [oz, mz]
        else:
            xs = [p[0] for p in self.blocks]
            ys = [p[1] for p in self.blocks]
            zs = [p[2] for p in self.blocks]
            ox, oy, oz = min(xs), min(ys), min(zs)
        palette = []
        index = {}
        blocks = []
        for pos, (block, props) in sorted(self.blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            key = (block, props)
            if key not in index:
                index[key] = len(palette)
                entry = {"Name": Str(block)}
                if props:
                    entry["Properties"] = Compound({k: Str(v) for k, v in props})
                palette.append(Compound(entry))
            item = {"pos": List(3, [Int(pos[0] - ox), Int(pos[1] - oy), Int(pos[2] - oz)]), "state": Int(index[key])}
            if pos in self.entities:
                item["nbt"] = to_nbt(self.entities[pos])
            blocks.append(Compound(item))
        size = [max(xs) - ox + 1, max(ys) - oy + 1, max(zs) - oz + 1]
        write_nbt(os.path.join(DATA, "structure", name + ".nbt"), Compound({
            "DataVersion": Int(DATA_VERSION), "size": List(3, [Int(s) for s in size]),
            "palette": List(10, palette), "blocks": List(10, blocks), "entities": List(10, [
                Compound({"blockPos": List(3, [Int(int(x) - ox), Int(int(y) - oy), Int(int(z) - oz)]),
                          "pos": List(6, [Double(x - ox), Double(y - oy), Double(z - oz)]), "nbt": to_nbt(nbt)})
                for x, y, z, nbt in self.mobs])}))
        print(f"{name}: {size[0]}x{size[1]}x{size[2]}, {len(blocks)} blocks")
        info = {"size": size}
        if getattr(self, "marker", None):
            info["marker"] = [self.marker[0] - ox, self.marker[1] - oy, self.marker[2] - oz]
            print(f"  marker at {info['marker']}")
        return info


WEATHER = {f"{NS}:dreamstone_bricks": ["cracked_dreamstone_bricks"],
           f"{NS}:sleepwood_planks": ["rotten_sleepwood"]}


# ---- Haunted house ----
def haunted_house():
    b = Build(101)
    b.island(10, 10, 0, 15, 13, 14)
    # Ground floor 0..12 x 0..10, walls 1..5, upper floor 6, walls 7..10, gable roof.
    x0, z0, x1, z1 = 3, 4, 17, 15
    b.fill(x0, 0, z0, x1, 0, z1, "sleepwood_planks")
    b.clear(x0 + 1, 1, z0 + 1, x1 - 1, 11, z1 - 1)
    for y in range(1, 6):
        b.walls(x0, y, z0, x1, y, z1, "torn_wallpaper")
    for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        b.fill(x, 0, z, x, 11, z, "sleepwood_log", axis="y")
    b.fill(x0, 6, z0, x1, 6, z1, "sleepwood_planks")
    for y in range(7, 11):
        b.walls(x0, y, z0, x1, y, z1, "rotten_sleepwood")
    # Roof: stairs sloping up from both long sides to a ridge, broken open in places.
    for i in range(0, 6):
        y = 11 + i
        for x in range(x0 - 1, x1 + 2):
            if b.rng.random() < 0.12 and 0 < i < 5:
                continue
            b.set(x, y, z0 - 1 + i, "sleepwood_stairs", facing="south", half="bottom", shape="straight", waterlogged=False)
            b.set(x, y, z1 + 1 - i, "sleepwood_stairs", facing="north", half="bottom", shape="straight", waterlogged=False)
        for z in range(z0 + i, z1 - i + 1):
            b.set(x0, y, z, "rotten_sleepwood")
            b.set(x1, y, z, "rotten_sleepwood")
    b.fill(x0 - 1, 17, z0 + 5, x1 + 1, 17, z0 + 6, "sleepwood_slab", type="bottom", waterlogged=False)
    # Door and windows: gaps with fences as broken panes.
    b.clear(10, 1, z0, 10, 2, z0)
    for x in (6, 14):
        b.set(x, 3, z0, "sleepwood_fence", north=False, south=False, east=False, west=False, waterlogged=False)
        b.set(x, 8, z0, "minecraft:air")
        b.set(x, 3, z1, "minecraft:air")
    for z in (8, 12):
        b.set(x0, 3, z, "minecraft:air")
        b.set(x1, 8, z, "sleepwood_fence", north=False, south=False, east=False, west=False, waterlogged=False)
    # Porch with a sagging rail.
    b.fill(x0 + 2, 0, z0 - 3, x1 - 2, 0, z0 - 1, "sleepwood_planks")
    for x in range(x0 + 2, x1 - 1):
        if b.rng.random() < 0.8:
            b.set(x, 1, z0 - 3, "sleepwood_fence", east=True, west=True, north=False, south=False, waterlogged=False)
    for x in (x0 + 2, x1 - 2):
        b.fill(x, 1, z0 - 3, x, 5, z0 - 3, "sleepwood_log", axis="y")
    b.fill(x0 + 2, 6, z0 - 3, x1 - 2, 6, z0 - 1, "sleepwood_slab", type="bottom", waterlogged=False)
    # Parlour: dusty carpet, a hearth, a table, a staircase up.
    b.fill(x0 + 2, 1, z0 + 2, x1 - 6, 1, z1 - 2, "dusty_carpet")
    b.fill(x0 + 1, 1, z1 - 1, x0 + 3, 4, z1 - 1, "dreamstone_bricks")
    b.set(x0 + 2, 1, z1 - 1, "minecraft:soul_campfire", lit=True, facing="north", signal_fire=False, waterlogged=False)
    b.set(x0 + 2, 2, z1 - 1, "minecraft:air")
    b.set(7, 1, 9, "sleepwood_fence", north=False, south=False, east=False, west=False, waterlogged=False)
    b.set(7, 2, 9, "minecraft:dark_oak_pressure_plate", powered=False)
    b.set(7, 3, 9, "minecraft:air")
    b.set(8, 1, 9, "sleepwood_stairs", facing="west", half="bottom", shape="straight", waterlogged=False)
    for i in range(5):
        b.set(x1 - 1, 1 + i, z0 + 2 + i, "sleepwood_stairs", facing="south", half="bottom", shape="straight", waterlogged=False)
        b.clear(x1 - 1, 6, z0 + 2 + i, x1 - 1, 6, z0 + 2 + i)
    b.set(x0 + 5, 1, z0 + 1, "minecraft:bookshelf")
    b.set(x0 + 6, 1, z0 + 1, "minecraft:bookshelf")
    b.set(x0 + 5, 2, z0 + 1, "minecraft:cobweb")
    b.chest(x0 + 1, 1, z0 + 1, "haunted_house", "east")
    b.set(11, 5, 10, "dread_lantern", hanging=True, waterlogged=False)
    # Upstairs: two bedrooms with old beds, a partition, cobwebs, a chest.
    b.fill(10, 7, z0 + 1, 10, 10, z1 - 1, "rotten_sleepwood")
    b.clear(10, 7, 11, 10, 8, 11)
    b.set(x0 + 2, 7, z0 + 2, "minecraft:red_bed", facing="south", part="head", occupied=False)
    b.set(x0 + 2, 7, z0 + 1, "minecraft:red_bed", facing="south", part="foot", occupied=False)
    b.set(x1 - 3, 7, z1 - 2, "minecraft:gray_bed", facing="north", part="head", occupied=False)
    b.set(x1 - 3, 7, z1 - 1, "minecraft:gray_bed", facing="north", part="foot", occupied=False)
    b.chest(x0 + 1, 7, z1 - 1, "haunted_house", "east")
    for _ in range(10):
        x, y, z = b.rng.randrange(x0 + 1, x1), b.rng.randrange(7, 11), b.rng.randrange(z0 + 1, z1)
        if b.get(x, y, z) == "minecraft:air":
            b.set(x, y, z, "minecraft:cobweb")
    b.set(6, 10, 10, "dread_lantern", hanging=True, waterlogged=False)
    # Cellar under the floor, down a hatch.
    b.clear(x0 + 2, -4, z0 + 3, x0 + 6, -1, z0 + 7)
    b.walls(x0 + 1, -4, z0 + 2, x0 + 7, -1, z0 + 8, "dreamstone_bricks")
    b.fill(x0 + 1, -5, z0 + 2, x0 + 7, -5, z0 + 8, "dreamstone_bricks")
    b.set(x0 + 4, 0, z0 + 5, "minecraft:dark_oak_trapdoor", facing="north", half="top", open=False, powered=False, waterlogged=False)
    b.fill(x0 + 4, -4, z0 + 5, x0 + 4, -1, z0 + 5, "minecraft:ladder", facing="north", waterlogged=False)
    b.set(x0 + 4, -4, z0 + 6, "minecraft:air")
    b.chest(x0 + 2, -4, z0 + 3, "haunted_house_cellar", "east")
    b.set(x0 + 6, -4, z0 + 7, "wake_crystal", facing="up", waterlogged=False)
    # The yard: a dead tree, thorns, a broken fence, a grave marker.
    b.fill(1, 1, 1, 1, 6, 1, "sleepwood_log", axis="y")
    b.set(1, 7, 1, "withered_hedge")
    b.set(2, 6, 1, "withered_hedge")
    for _ in range(14):
        x, z = b.rng.randrange(-3, 23), b.rng.randrange(-1, 22)
        if b.get(x, 0, z) == f"{NS}:ashen_moss" and b.get(x, 1, z) is None:
            b.set(x, 1, z, b.rng.choice(["despair_thorns", "despair_thorns", "lucid_bloom"]))
    b.set(18, 1, 2, "dreamstone_brick_wall", up=True, north="none", south="none", east="none", west="none", waterlogged=False)
    b.set(18, 2, 2, "chiseled_dreamstone")
    b.decay(0.25, WEATHER)
    return b


# ---- Garden of despair ----
WALL = dict(up=True, north="none", south="none", east="none", west="none", waterlogged=False)


def bezier(a, c, b, steps):
    """Points along a quadratic curve from a to b, pulled towards c."""
    out = []
    for k in range(steps + 1):
        t = k / steps
        out.append(tuple((1 - t) ** 2 * a[i] + 2 * (1 - t) * t * c[i] + t * t * b[i] for i in range(3)))
    return out


def limb(b, points, thick_from, thick_to, name="sleepwood_log"):
    """A thick, curving branch through the given points, tapering from one thickness to the other; each log turned
    along the way the branch runs there."""
    for k in range(len(points) - 1):
        p, q = points[k], points[k + 1]
        d = [q[i] - p[i] for i in range(3)]
        axis = "xyz"[max(range(3), key=lambda i: abs(d[i]))]
        r = thick_from + (thick_to - thick_from) * k / max(len(points) - 2, 1)
        n = int(max(abs(v) for v in d) * 2) + 1
        for j in range(n):
            x, y, z = (p[i] + d[i] * j / n for i in range(3))
            ir = int(math.ceil(r))
            for dx in range(-ir, ir + 1):
                for dy in range(-ir, ir + 1):
                    for dz in range(-ir, ir + 1):
                        if dx * dx + dy * dy + dz * dz <= r * r + 0.3:
                            b.set(round(x) + dx, round(y) + dy, round(z) + dz, name, axis=axis)


def weeping_tree(b, cx, cz, y0):
    """The great weeping tree at the heart of the garden: a broad, twisting trunk on flared roots, limbs that reach
    out and arch over, a dome of dead leaves, and from all round its rim long strands weeping down nearly to the
    ground, red vine and glowing veins at their ends, lanterns hanging among them. A hollow at its foot keeps the
    garden's best chest."""
    rng = random.Random(311)  # its own, so changes elsewhere in the garden don't reshape it
    height = 21
    # The trunk: broad at its foot, swelling into its roots, narrowing as it rises, with a slow twist and lean.
    lean = lambda u: (math.sin(u * 2.4) * 1.4, math.cos(u * 1.9) * 0.9 - 0.9)

    def radius(y):
        u = (y - y0) / height
        return 3.0 - 1.4 * u + 2.8 * max(0.0, 1.0 - (y - y0) / 8.0) ** 2

    for y in range(y0, y0 + height + 1):
        r = radius(y)
        ox, oz = lean((y - y0) / height)
        ir = int(math.ceil(r))
        for dx in range(-ir, ir + 1):
            for dz in range(-ir, ir + 1):
                if dx * dx + dz * dz <= r * r:
                    b.set(round(cx + ox) + dx, y, round(cz + oz) + dz, "sleepwood_log", axis="y")
    # Roots: eight of them, set round it, each a run of logs laid along its own way out: two thick where it leaves
    # the trunk, high on it, a buttress of upright logs under it there; stepping down as it goes, one thick, until it
    # sinks into the ground. Diagonal ones step, each log turned the way that step went.
    for k in range(8):
        # Set between the eight ways, so none runs straight south, where the door is and the Gardener kneels.
        ang = (k + 0.5) / 8 * math.tau + rng.uniform(-0.08, 0.08)
        # The two either side of south are pushed further off it, leaving the door a clear way in.
        off = ang - math.pi / 2
        if abs(off) < 0.8:
            ang = math.pi / 2 + math.copysign(0.8, off)
        dx, dz = math.cos(ang), math.sin(ang)
        length = rng.randint(7, 11)
        start = radius(y0) - 1.0
        last = None
        for step in range(length + 1):
            t = step / length
            x, z = round(cx + dx * (start + step)), round(cz + dz * (start + step))
            top = y0 + round(3.0 * (1.0 - t) ** 1.6) - (1 if t > 0.85 else 0)
            axis = "x" if abs(dx) >= abs(dz) else "z"
            if last is not None and (x != last[0]) != (z != last[1]):
                axis = "x" if x != last[0] else "z"
            last = (x, z)
            b.set(x, top, z, "sleepwood_log", axis=axis)
            if t < 0.45:
                # Thick near the trunk: a second log beside it, and the buttress under it down to the ground.
                sx, sz = (0, 1) if axis == "x" else (1, 0)
                b.set(x + sx, top, z + sz, "sleepwood_log", axis=axis)
                for y in range(y0, top):
                    b.set(x, y, z, "sleepwood_log", axis="y")
            elif top > y0:
                b.set(x, top - 1, z, "sleepwood_log", axis=axis)
            if t > 0.85:
                b.set(x, y0 - 1, z, "sleepwood_log", axis=axis)
    # The heart of the tree: a chamber hollowed in its foot, five across, its corners left as the trunk's own wood so
    # it is round, three high and vaulted a block higher in the middle, then the middle once more: its walls are the
    # trunk still, two blocks or more thick all round. Sealed at its door with heartwood carved in runes that no tool
    # can cut (it breaks only when the Gardener dies: GardenerEntity.unseal).
    def hollow(dx, dz, y):
        if y < 3:
            return max(abs(dx), abs(dz)) <= 2 and not (abs(dx) == 2 and abs(dz) == 2)
        return max(abs(dx), abs(dz)) <= (1 if y == 3 else 0)
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            for y in range(5):
                if hollow(dx, dz, y):
                    b.air(cx + dx, y0 + y, cz + dz)
            if hollow(dx, dz, 0):
                # The floor: a chiseled eye at the middle, a ring of polished stone round it, engraved bricks at the rim.
                d = max(abs(dx), abs(dz))
                b.set(cx + dx, y0 - 1, cz + dz, "chiseled_dreamstone" if d == 0 else "polished_dreamstone" if d == 1 else "engraved_dreamstone_bricks")
    # Weeping veins down the walls from the low ceiling's edge: either side of the reliquary, and mid-way along
    # each side wall (kept off the middle, so the veil on its pedestal is seen from the door).
    for dx, dz in ((-1, -2), (1, -2), (-2, 0), (2, 0)):
        b.set(cx + dx, y0 + 2, cz + dz, "weeping_veins", tip=True, waterlogged=False)

    # The door: through the trunk to the south, an arch three wide, two high at its sides and three in the middle;
    # the seal fills it a block in from the trunk's face. At its mouth a carved frame set into the bark: pillars for
    # jambs, a lintel of engraved runes with an eye in it looking out, a cornice of stairs over it.
    door_end = cz + 3
    while b.get(cx, y0 + 1, door_end + 1) == f"{NS}:sleepwood_log":
        door_end += 1
    seal = max(cz + 3, door_end - 1)
    for z in range(cz + 3, door_end + 1):
        for x in range(cx - 1, cx + 2):
            for y in range(y0, y0 + 3):
                if y < y0 + 2 or x == cx:
                    b.set(x, y, z, "heartwood_seal" if z == seal else "minecraft:air")
            b.set(x, y0 - 1, z, "polished_dreamstone" if x == cx else "dreamstone_bricks")
        # The arch's shoulders: upside-down stairs, so the opening rounds over.
        for x, facing in ((cx - 1, "west"), (cx + 1, "east")):  # their full sides out against the jambs
            if z == seal:
                b.set(x, y0 + 2, z, "heartwood_seal")  # the seal fills the arch's shoulders too, its full 3 by 3
            else:
                b.set(x, y0 + 2, z, "polished_dreamstone_stairs", facing=facing, half="top", shape="straight", waterlogged=False)
    b.set(cx, y0 - 1, seal, "chiseled_dreamstone")  # the threshold under the seal
    # The frame at the mouth.
    for x in (cx - 2, cx + 2):
        for y in range(y0, y0 + 3):
            b.set(x, y, door_end, "dreamstone_pillar", axis="y")
        b.set(x, y0 - 1, door_end, "polished_dreamstone")
    for x in range(cx - 2, cx + 3):
        b.set(x, y0 + 3, door_end, "chiseled_dreamstone" if x == cx else "engraved_dreamstone_bricks")
    # The cornice: two rows of stairs over the middle of the lintel, stepping back up into the trunk, a lantern on
    # each end of the lintel; the second row sits on logs just behind the frame, filling the gap the narrowing trunk
    # leaves over the seal.
    # (The trunk leans and narrows here, so the rows behind the frame are holed: fill them back to it, a block higher
    # behind the second row of stairs so it has wood at its back too.)
    for z in range(cz + 3, door_end):
        for x in range(cx - 2, cx + 3):
            for y in range(y0 + 3, y0 + (6 if z < door_end - 1 else 5)):
                if b.get(x, y, z) in (None, "minecraft:air"):
                    b.set(x, y, z, "sleepwood_log", axis="y")
    for x in range(cx - 1, cx + 2):
        b.set(x, y0 + 4, door_end, "polished_dreamstone_stairs", facing="north", half="bottom", shape="straight", waterlogged=False)
        b.set(x, y0 + 5, door_end - 1, "polished_dreamstone_stairs", facing="north", half="bottom", shape="straight", waterlogged=False)
    for x in (cx - 2, cx + 2):
        b.set(x, y0 + 4, door_end, "dread_lantern", hanging=False, waterlogged=False)

    # Before the door, a doorstep clear of roots, paved with a way of runes leading in, a candle burning either side
    # of it and a lamp post at its corners.
    for x in range(cx - 2, cx + 3):
        for z in range(door_end + 1, door_end + 5):
            for y in range(y0, y0 + 5):
                if b.get(x, y, z) == f"{NS}:sleepwood_log":
                    b.air(x, y, z)
            if b.get(x, y0 - 1, z) == f"{NS}:sleepwood_log":
                b.set(x, y0 - 1, z, "dreamstone_bricks")
    for z in range(door_end + 1, door_end + 5):
        b.set(cx, y0 - 1, z, "engraved_dreamstone_bricks" if z % 2 else "polished_dreamstone")
    for x in (cx - 2, cx + 2):
        b.set(x, y0, door_end + 2, "minecraft:red_candle", candles=3, lit=True, waterlogged=False)
        clutter(Raised(b, y0 - 1), rng, x, door_end + 4, "lamp")

    # Inside. At the back the heartwood reliquary, the tree's hoard; a candle burning either side of it. In the
    # middle, on a pedestal, the Weeping Veil laid in a glowing frame, candles at its foot. A bowl of
    # blood, old pots, candles round the walls; a lantern hung in the vault.
    b.set(cx, y0, cz - 2, "heartwood_reliquary", nbt={"id": f"{NS}:heartwood_reliquary", "LootTable": f"{NS}:chests/garden_heart"},
          facing="south", open=False)
    b.set(cx - 1, y0, cz - 2, "minecraft:red_candle", candles=4, lit=True, waterlogged=False)
    b.set(cx + 1, y0, cz - 2, "minecraft:red_candle", candles=4, lit=True, waterlogged=False)
    b.set(cx, y0, cz, "chiseled_dreamstone")
    b.set(cx - 1, y0, cz + 1, "minecraft:red_candle", candles=2, lit=True, waterlogged=False)
    b.set(cx + 1, y0, cz + 1, "minecraft:red_candle", candles=2, lit=True, waterlogged=False)
    b.set(cx + 2, y0, cz - 1, "minecraft:water_cauldron", level=3)
    b.set(cx - 2, y0, cz - 1, "minecraft:decorated_pot", facing="east", cracked=True, waterlogged=False)
    b.set(cx - 2, y0, cz + 1, "minecraft:black_candle", candles=3, lit=True, waterlogged=False)
    b.set(cx + 2, y0, cz + 1, "minecraft:decorated_pot", facing="west", cracked=False, waterlogged=False)
    b.set(cx, y0 + 3, cz, "dread_lantern", hanging=True, waterlogged=False)
    # Limbs: out and up from high on the trunk, arching over and down at their ends, each with two smaller boughs.
    foliage = []
    ox, oz = lean(1.0)
    top = (cx + ox, y0 + height, cz + oz)
    # Long low limbs that sweep far out and droop, and shorter high ones that rise and arch over, alternating, so
    # the crown is a dome rather than a ring.
    for k in range(10):
        ang = k / 10 * math.tau + rng.uniform(-0.2, 0.2)
        low = k % 2 == 0
        ys = y0 + height * (rng.uniform(0.5, 0.7) if low else rng.uniform(0.8, 1.0))
        reach = rng.uniform(14.0, 19.0) if low else rng.uniform(7.0, 11.0)
        rise = rng.uniform(6.0, 9.0) if low else rng.uniform(8.0, 11.0)
        drop = rng.uniform(-3.0, 0.0) if low else rng.uniform(2.0, 5.0)
        a = (top[0] + math.cos(ang) * 1.2, ys, top[2] + math.sin(ang) * 1.2)
        c = (top[0] + math.cos(ang) * reach * 0.45, ys + rise, top[2] + math.sin(ang) * reach * 0.45)
        e = (top[0] + math.cos(ang) * reach, ys + drop, top[2] + math.sin(ang) * reach)
        path = bezier(a, c, e, 14)
        limb(b, path, 1.5, 0.4)
        foliage += path[4:]
        for j in (6, 10):
            o = path[j]
            side = ang + rng.choice((-1, 1)) * rng.uniform(0.6, 1.0)
            span = rng.uniform(5.0, 8.0)
            tip = (o[0] + math.cos(side) * span, o[1] + rng.uniform(0.0, 3.0), o[2] + math.sin(side) * span)
            mid = ((o[0] + tip[0]) / 2, max(o[1], tip[1]) + 2.0, (o[2] + tip[2]) / 2)
            twig = bezier(o, mid, tip, 6)
            limb(b, twig, 0.6, 0.2)
            foliage += twig[2:]
    # A crown over the top of the trunk, closing the dome.
    for _ in range(14):
        ang = rng.uniform(0, math.tau)
        out = rng.uniform(0.0, 7.0)
        foliage.append((top[0] + math.cos(ang) * out, y0 + height + rng.uniform(4.0, 9.0) - out * 0.4, top[2] + math.sin(ang) * out))
    # The dome: flattened, ragged clumps of dead leaves along the outer limbs.
    leaves = set()
    for (x, y, z) in foliage:
        rx, ry = rng.uniform(2.0, 3.2), rng.uniform(1.4, 2.2)
        for dx in range(-4, 5):
            for dy in range(-3, 4):
                for dz in range(-4, 5):
                    if (dx * dx + dz * dz) / (rx * rx) + dy * dy / (ry * ry) <= 1.0 and rng.random() < 0.82:
                        pos = (round(x) + dx, round(y) + dy, round(z) + dz)
                        if b.get(*pos) in (None, "minecraft:air"):
                            b.set(*pos, "withered_hedge")
                            leaves.add(pos)
    # Weeping: strands hang from the underside of the dome, away from the trunk: leaves first, then red vine, ending
    # in a vine tip, a glowing vein or now and then a lantern.
    for (x, y, z) in sorted(leaves):
        out = math.hypot(x - cx, z - cz)
        if (x, y - 1, z) in leaves or out < 6.5 or rng.random() > 0.2 + 0.08 * min(out / 18.0, 1.0):
            continue
        # Longest at the rim, where they nearly reach the ground; short wisps further in.
        floor = y0 + 2 + int(rng.random() * 3)
        reach = min(out / 17.0, 1.0)
        length = int((y - floor) * rng.uniform(0.15, 0.55 + 0.45 * reach))
        if length < 2:
            continue
        leafy = rng.randint(1, 3) + int(length * rng.uniform(0.0, 0.5))
        for k in range(1, length + 1):
            yy = y - k
            if b.get(x, yy, z) not in (None, "minecraft:air"):
                break
            if k <= leafy:
                b.set(x, yy, z, "withered_hedge")
            elif k < length:
                b.set(x, yy, z, "minecraft:weeping_vines_plant")
            elif rng.random() < 0.15:
                b.set(x, yy, z, "dread_lantern", hanging=True, waterlogged=False)
            elif rng.random() < 0.5:
                b.set(x, yy, z, "hanging_veins", waterlogged=False)
            else:
                b.set(x, yy, z, "minecraft:weeping_vines", age=25)

    # Last, the author's own hand-made finish of the heart, entrance and chamber (2026-10-14), read block by block
    # from their edit in the world: garden_heart_edit.json, [dx, dy, dz, block, properties] from the tree's middle at
    # its foot. It sets the reliquary near the door and, at the back where the reliquary stood, the heart portal
    # (HeartPortalBlock: nine blocks, facing the door, its middle the core), which wakes when the reliquary is opened
    # and puts whoever steps in out past the walls.
    with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "garden_heart_edit.json"), encoding="utf-8") as f:
        for dx, dy, dz, name, props in json.load(f):
            nbt = {"id": f"{NS}:heartwood_reliquary", "LootTable": f"{NS}:chests/garden_heart"} if name.endswith("heartwood_reliquary") else None
            b.set(cx + dx, y0 + dy, cz + dz, name, nbt=nbt, **props)


def statue(b, x, z, y0, facing):
    """A weeping figure on a plinth, worn and cracked: a stepped base, a body, bowed shoulders and a hooded head."""
    b.fill(x - 1, y0, z - 1, x + 1, y0, z + 1, "dreamstone_bricks")
    b.set(x, y0 + 1, z, "chiseled_dreamstone")
    b.fill(x, y0 + 2, z, x, y0 + 4, z, "dreamstone_pillar", axis="y")
    for dx, dz in ((1, 0), (-1, 0)) if facing in ("north", "south") else ((0, 1), (0, -1)):
        b.set(x + dx, y0 + 4, z + dz, "dreamstone_brick_stairs", facing=facing, half="top", shape="straight", waterlogged=False)
        b.set(x + dx, y0 + 3, z + dz, "dreamstone_brick_wall", **WALL)
    b.set(x, y0 + 5, z, "chiseled_dreamstone")
    b.set(x, y0 + 6, z, "dreamstone_brick_slab", type="bottom", waterlogged=False)


def dead_tree(b, x, z, y0, height):
    """A dead tree standing up over the hedges: a bare trunk and a few crooked boughs."""
    rng = b.rng
    b.fill(x, y0, z, x, y0 + height, z, "sleepwood_log", axis="y")
    for _ in range(rng.randint(3, 5)):
        ang = rng.uniform(0, math.tau)
        ys = y0 + height * rng.uniform(0.55, 0.95)
        span = rng.uniform(2.5, 5.0)
        e = (x + math.cos(ang) * span, ys + rng.uniform(1.5, 3.5), z + math.sin(ang) * span)
        limb(b, bezier((x, ys, z), (x + math.cos(ang) * span * 0.4, ys + 2.5, z + math.sin(ang) * span * 0.4), e, 5), 0.3, 0.2)


def garden_of_despair():
    """A great walled labyrinth of withered hedge. Huge, weathered walls of dreamstone close it in, buttressed,
    crenellated and broken in places, with an arched gate in the middle of each side. Inside, hedges higher than a
    house wind three blocks apart round dead trees, worn statues, lanterns and the chests the lost left in its dead
    ends, towards a wide plaza at its heart where a great weeping tree stands."""
    b = Build(202)
    rng = b.rng
    W = 7               # a corridor's width, before the hedges grow in (cells: maze_cell)
    cell = W + 2        # a maze cell: a corridor with a hedge one thick round it (two between cells)
    n = 17              # cells across (155 blocks inside the walls)
    span = n * cell + 2
    mid = n // 2
    centre = 1 + mid * cell + cell // 2
    gate = 1 + mid * cell + 1
    plaza = 27          # the plaza's radius: the Gardener's arena
    reach = 3           # the plaza piece's box: the middle cell and three either side of it
    plaza_lo, plaza_hi = 1 + (mid - reach) * cell, (mid + reach + 1) * cell
    hedge_high = 7
    # Its island: broad enough to carry the walls, their towers and buttresses, with a lobe out from each gate.
    # (Its reach, half + 18 + the lobe's 16, stays within the 8 chunks round the centre the game builds pieces in.)
    b.broad_island(span // 2, span // 2 + 18, 38, W + 14, 16, flat=span // 2 + 11, causeway=W // 2 + 4)

    # The ground inside the walls, and the plaza. The maze itself is not built here: the structure lays it out
    # afresh every time from maze cells joined by jigsaws (maze_cell, export_garden), each garden its own maze. Here
    # its ground is hedge, solid, so anywhere no cell reaches stays a thicket.
    for x in range(span):
        for z in range(span):
            r = math.hypot(x - centre, z - centre)
            if r <= plaza:
                if r < 9.5:
                    floor = "ashen_moss" if rng.random() < 0.7 else "ashen_soil"
                else:
                    floor = "dreamstone_bricks" if r % 6 < 4.5 else "chiseled_dreamstone" if rng.random() < 0.15 else "cracked_dreamstone_bricks"
                b.set(x, 0, z, floor)
                b.clear(x, 1, z, x, hedge_high + 1, z)
            else:
                b.set(x, 0, z, "ashen_soil")
                hedge_top(b, rng, x, z, hedge_high)  # coped like the maze's hedges
    # The plaza's four ways out, to the edges of its piece, where the maze joins them; but only the south one (the
    # side the tree's door and the Gardener face) is a way in: the other three are fenced across at the plaza's rim,
    # ground to hedge tops. And the gaps in the hedge along the walls' foot at the gates.
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        way_in = (dx, dz) == (0, 1)
        for k in range(plaza - 3, centre - plaza_lo + 1):
            for w in range(-(W // 2), W // 2 + 1):
                x, z = centre + dx * k + dz * w, centre + dz * k + dx * w
                b.set(x, 0, z, "cracked_dreamstone_bricks" if rng.random() < 0.3 else "dreamstone_bricks")
                b.clear(x, 1, z, x, hedge_high + 1, z)
        if not way_in:
            k = plaza + 1
            across = ("north", "south") if dx else ("west", "east")
            for w in range(-(W // 2), W // 2 + 1):
                x, z = centre + dx * k + dz * w, centre + dz * k + dx * w
                # A wall of fence across the whole way, from the ground to the hedge tops, joined along the row (at its
                # ends the rails run on into the hedge either side): the maze comes up to it, and one sees the tree
                # through it, but the only way in is the south one.
                for y in range(1, hedge_high + 1):
                    b.set(x, y, z, "sleepwood_fence", waterlogged=False, **{d: d in across for d in ("north", "south", "east", "west")})
    for w in range(W):
        for x, z in ((0, gate + w), (span - 1, gate + w), (gate + w, 0), (gate + w, span - 1)):
            b.set(x, 0, z, "dreamstone_bricks")
            b.clear(x, 1, z, x, hedge_high + 1, z)

    # The outer walls: three thick and fourteen high. Seen from outside, from the foot up: a battered plinth of
    # polished stone; the face, pilasters every ten blocks and between them framed panels crossed by a band of runes,
    # with an arrow slit or a carved eye at the middle of each; an engraved frieze; a cornice of corbels carrying a
    # walkway out over the face; and the battlements, capped merlons with low sills between them, a rail along the
    # inner edge. Towers stand at the corners and either side of each gate.
    wall_high = 14
    lo, hi = -3, span + 2
    sides = ("north", "south", "west", "east")
    inward = {"north": "south", "south": "north", "west": "east", "east": "west"}

    def at(side, k, t):
        """A place on a side's wall: k along it, t in from its outer face (negative is outside it)."""
        return [(k, lo + t), (k, hi - t), (lo + t, k), (hi - t, k)][side]

    def put(side, k, t, y, name, **props):
        x, z = at(side, k, t)
        b.set(x, y, z, name, **props)

    def brick():
        roll = rng.random()
        return "cracked_dreamstone_bricks" if roll < 0.2 else "veined_dreamstone_bricks" if roll < 0.25 else "dreamstone_bricks"

    H = wall_high
    near_gate = lambda k: gate - 6 <= k <= gate + W + 5
    for side in range(4):
        face = sides[side]
        into = inward[face]
        for k in range(lo, hi + 1):
            # The body, and the walkway on it.
            for t in range(3):
                put(side, k, t, 0, "dreamstone")
                for y in range(1, H):
                    put(side, k, t, y, brick() if t != 1 else "dreamstone")
                put(side, k, t, H, "polished_dreamstone")
            # The plinth: a sloped course along the foot, and a polished course over it.
            put(side, k, -1, 0, "dreamstone")
            put(side, k, -1, 1, "polished_dreamstone_stairs", facing=into, half="bottom", shape="straight", waterlogged=False)
            put(side, k, 0, 1, "polished_dreamstone")
            put(side, k, 0, 2, "polished_dreamstone")
            # The frieze of runes under the cornice, and the cornice: corbels carrying the walkway's lip out over it.
            put(side, k, 0, H - 2, "engraved_dreamstone_bricks")
            put(side, k, -1, H - 1, "polished_dreamstone_stairs", facing=into, half="top", shape="straight", waterlogged=False)
            put(side, k, -1, H, "polished_dreamstone")
            # Battlements on the lip: merlons three wide with a gap of two, capped; a low sill in each gap.
            if (k - lo) % 5 < 3:
                put(side, k, -1, H + 1, "dreamstone_bricks")
                put(side, k, -1, H + 2, "engraved_dreamstone_bricks" if (k - lo) % 5 == 1 else "dreamstone_bricks")
                put(side, k, -1, H + 3, "polished_dreamstone_slab", type="bottom", waterlogged=False)
            else:
                put(side, k, -1, H + 1, "polished_dreamstone_slab", type="bottom", waterlogged=False)
            # A rail along the inner edge of the walkway, and under it, over the maze, a frieze and corbels.
            put(side, k, 2, H + 1, "dreamstone_brick_wall", **WALL)
            put(side, k, 2, H - 2, "engraved_dreamstone_bricks")
            if 0 <= k < span:
                put(side, k, 3, H - 1, "polished_dreamstone_stairs", facing=face, half="top", shape="straight", waterlogged=False)
                put(side, k, 3, H, "polished_dreamstone_slab", type="bottom", waterlogged=False)
        # Pilasters every ten blocks, and between them, panels.
        posts = [k for k in range(lo + 5, hi - 4, 10) if not near_gate(k)]
        for k in posts:
            for y in range(2, H - 1):
                put(side, k, -1, y, "dreamstone_pillar", axis="y")
            put(side, k, -1, 2, "polished_dreamstone")
            put(side, k, -1, H - 3, "chiseled_dreamstone")
            # Its base and capital: stairs turned in against it, at its foot and under the cornice.
            put(side, k, -2, 1, "polished_dreamstone_stairs", facing=into, half="bottom", shape="straight", waterlogged=False)
            put(side, k, -2, 2, "polished_dreamstone_slab", type="bottom", waterlogged=False)
            put(side, k, -2, H - 2, "polished_dreamstone_stairs", facing=into, half="top", shape="straight", waterlogged=False)
            # A lantern on a bracket either side of it: a stub of dreamstone wall set against the face, the lantern hung
            # under it.
            for dk in (-2, 2):
                if lo + 2 < k + dk < hi - 2:
                    put(side, k + dk, -1, H - 5, "dreamstone_wall", up=True, waterlogged=False,
                        **{d: ("low" if d == into else "none") for d in ("north", "south", "east", "west")})
                    put(side, k + dk, -1, H - 6, "dread_lantern", hanging=True, waterlogged=False)
        for a, c in zip(posts, posts[1:]):
            if c - a != 10:
                continue
            k0, k1 = a + 2, c - 2
            y0, y1 = 4, H - 4
            mid = (y0 + y1) // 2
            for k in range(k0, k1 + 1):
                for y in range(y0, y1 + 1):
                    border = k in (k0, k1) or y in (y0, y1)
                    put(side, k, 0, y, "polished_dreamstone" if border else "dreamstone")
                if k0 < k < k1:
                    put(side, k, 0, mid, "engraved_dreamstone_bricks")
            panel_mid = (k0 + k1) // 2
            if rng.random() < 0.5:
                # An arrow slit: dark, and deep, through the face.
                for y in range(mid - 2, mid + 3):
                    if y != mid:
                        x, z = at(side, panel_mid, 0)
                        b.air(x, y, z)
                put(side, panel_mid, 0, mid, "polished_dreamstone")
            else:
                put(side, panel_mid, 0, mid, "chiseled_dreamstone")
            # Veins weeping from under the cornice, here and there: one strand of weeping veins, its tip at the bottom.
            if rng.random() < 0.6:
                vk = rng.randrange(k0, k1 + 1)
                bottom = H - 2 - rng.randint(1, 4)
                for y in range(bottom, H - 1):
                    put(side, vk, -1, y, "weeping_veins", tip=y == bottom, waterlogged=False)

    def tower(cx, cz, r, top, door_side=None):
        """A square tower: polished plinth, brick body with pillar corners, bands of runes, a carved eye on each face,
        a corbelled crown wider than the body, and battlements round it with a lantern on a post in the middle."""
        for x in range(cx - r - 1, cx + r + 2):
            for z in range(cz - r - 1, cz + r + 2):
                dx, dz = abs(x - cx), abs(z - cz)
                edge = max(dx, dz)
                if edge == r + 1:
                    # The plinth's slope, and the crown's corbels and lip.
                    facing = "south" if z < cz - r else "north" if z > cz + r else "east" if x < cx - r else "west"
                    if dx <= r or dz <= r:
                        b.set(x, 1, z, "polished_dreamstone_stairs", facing=facing, half="bottom", shape="straight", waterlogged=False)
                        b.set(x, top - 1, z, "polished_dreamstone_stairs", facing=facing, half="top", shape="straight", waterlogged=False)
                    b.set(x, 0, z, "dreamstone")
                    b.set(x, top, z, "polished_dreamstone")
                    if (x + z) % 2 == 0:
                        b.set(x, top + 1, z, "dreamstone_bricks")
                        b.set(x, top + 2, z, "polished_dreamstone_slab", type="bottom", waterlogged=False)
                    continue
                if edge > r:
                    continue
                corner = dx == r and dz == r
                for y in range(-2, top):
                    if y <= 2:
                        name = "polished_dreamstone"
                    elif corner:
                        name = "dreamstone_pillar"
                    elif edge == r and y in (6, top - 3):
                        name = "engraved_dreamstone_bricks"
                    else:
                        name = brick() if edge == r else "dreamstone"
                    if name == "dreamstone_pillar":
                        b.set(x, y, z, name, axis="y")
                    else:
                        b.set(x, y, z, name)
                b.set(x, top, z, "polished_dreamstone")
        for dx, dz in ((0, -r), (0, r), (-r, 0), (r, 0)):
            b.set(cx + dx, top - 6, cz + dz, "chiseled_dreamstone")
            # A slit under each eye.
            b.air(cx + dx, top - 9, cz + dz)
            b.air(cx + dx, top - 10, cz + dz)
        b.fill(cx, top + 1, cz, cx, top + 2, cz, "dreamstone_brick_wall", **WALL)
        b.set(cx, top + 3, cz, "dread_lantern", hanging=False, waterlogged=False)

    # The corner towers, taller, and the gate towers either side of each gate, standing out from the wall.
    # (Out at the corners, clear of the maze's ground.)
    for cx, cz in ((lo - 2, lo - 2), (hi + 2, lo - 2), (lo - 2, hi + 2), (hi + 2, hi + 2)):
        tower(cx, cz, 4, H + 8)
    for side in range(4):
        for k in (gate - 4, gate + W + 3):
            x, z = at(side, k, -1)
            tower(x, z, 2, H + 5)

    # Breaches: the top of the wall fallen in, battlements, walkway and all.
    for _ in range(4):
        side = rng.randrange(4)
        k = rng.randrange(12, span - 14)
        if near_gate(k) or abs(k - gate) < 12:
            continue
        width = rng.randint(3, 6)
        depth = rng.randint(3, 5)
        for w in range(width):
            for d in range(depth - abs(w - width // 2) + 1):
                for t in range(-1, 3):
                    x, z = at(side, k + w, t)
                    b.air(x, H + 3 - d, z)
            # Rubble at the foot, outside.
            x, z = at(side, k + w, -2 - rng.randrange(2))
            if rng.random() < 0.6:
                b.set(x, 1, z, "cracked_dreamstone_bricks")

    # The gates: an arch through the wall, its springing stepped in with corbels, a chiseled keystone over it under a
    # band of runes, and a lantern hung in it.
    for side in range(4):
        face = sides[side]
        for t in range(-1, 3):
            for w in range(-1, W + 1):
                for y in range(1, 8):
                    if w in (-1, W) and y >= 6:
                        continue
                    x, z = at(side, gate + w, t)
                    b.air(x, y, z)
        along = "east" if side < 2 else "south"
        against = "west" if side < 2 else "north"
        for t in range(-1, 3):
            x, z = at(side, gate - 1, t)
            b.set(x, 6, z, "polished_dreamstone_stairs", facing=against, half="top", shape="straight", waterlogged=False)
            x, z = at(side, gate + W, t)
            b.set(x, 6, z, "polished_dreamstone_stairs", facing=along, half="top", shape="straight", waterlogged=False)
        for w in range(-2, W + 2):
            put(side, gate + w, -1, 8, "polished_dreamstone")
            put(side, gate + w, -1, 9, "engraved_dreamstone_bricks")
        put(side, gate + W // 2, -1, 8, "chiseled_dreamstone")
        x, z = at(side, gate + W // 2, 1)
        b.set(x, 7, z, "dread_lantern", hanging=True, waterlogged=False)

    # A worn paved way from each gate out across its lobe of the island, lanterns on posts either side of it.
    for side in range(4):
        for t in range(-12, -1):
            for w in range(-1, W + 1):
                x, z = at(side, gate + w, t)
                y = b.heights.get((x, z), 0)
                if b.get(x, y, z) in (f"{NS}:ashen_moss", f"{NS}:ashen_soil") and rng.random() < 0.85 - (-t) * 0.03:
                    b.set(x, y, z, "cracked_dreamstone_bricks" if rng.random() < 0.35 else "dreamstone_bricks")
        for t in (-5, -10):
            for w in (-2, W + 1):
                x, z = at(side, gate + w, t)
                y = b.heights.get((x, z), 0)
                fire_lamp(b, x, y + 1, z, "north")
    # On the slopes outside the walls: dead trees, thorns, blooms, a few fallen stones.
    for (x, z), y in sorted(b.heights.items()):
        if lo - 9 <= x <= hi + 9 and lo - 9 <= z <= hi + 9:
            continue
        if b.get(x, y, z) != f"{NS}:ashen_moss" or b.get(x, y + 1, z) not in (None, "minecraft:air"):
            continue
        roll = rng.random()
        if roll < 0.006:
            dead_tree(b, x, z, y + 1, rng.randint(6, 10))
        elif roll < 0.05:
            b.set(x, y + 1, z, rng.choice(["despair_thorns", "despair_thorns", "lucid_bloom"]))
        elif roll < 0.056:
            stone = rng.choice(["cracked_dreamstone_bricks", "dreamstone", "polished_dreamstone_slab"])
            b.set(x, y + 1, z, stone, **({"type": "bottom", "waterlogged": False} if stone.endswith("slab") else {}))

    # The plaza: lantern posts and broken standing stones round its edge, worn statues at its four corners, stone
    # benches round the tree, blooms in the moss, and the tree at the heart of it.
    for a in range(16):
        ang = (a + 0.5) / 16 * math.tau
        x, z = round(centre + math.cos(ang) * (plaza - 1.5)), round(centre + math.sin(ang) * (plaza - 1.5))
        if a % 2 == 0:
            fire_lamp(b, x, 1, z, "north")
        else:
            b.fill(x, 1, z, x, rng.randint(2, 5), z, "dreamstone_pillar", axis="y")
    for k, facing in enumerate(("north", "west", "south", "east")):
        ang = math.pi / 4 + k * math.pi / 2
        statue(b, round(centre + math.cos(ang) * (plaza - 6)), round(centre + math.sin(ang) * (plaza - 6)), 1, facing)
    for a in range(10):
        ang = a / 10 * math.tau + 0.2
        x, z = round(centre + math.cos(ang) * 11), round(centre + math.sin(ang) * 11)
        b.set(x, 1, z, "dreamstone_brick_slab", type="bottom", waterlogged=False)
    weeping_tree(b, centre, centre, 1)
    # The lake of its tears: round the weeping tree a ring of them (the Deep's water, red as blood), ragged at its
    # shore, a causeway of stone across it to the door (south) where the Gardener kneels; the tree's roots and
    # anything standing in it left as islands.
    tears = random.Random(613)
    for x in range(centre - 16, centre + 17):
        for z in range(centre - 16, centre + 17):
            r = math.hypot(x - centre, z - centre)
            wobble = math.sin(math.atan2(z - centre, x - centre) * 5 + 1.3) * 1.2 + tears.uniform(-0.4, 0.4)
            if not (7.5 + wobble * 0.4 < r < 13.5 + wobble):
                continue
            if abs(x - centre) <= 2 and z > centre:
                b.set(x, 0, z, "polished_dreamstone" if abs(x - centre) < 2 else "dreamstone_bricks")  # the causeway
                continue
            if b.get(x, 1, z) not in (None, "minecraft:air") or b.get(x, 0, z) == f"{NS}:sleepwood_log":
                continue
            b.set(x, 0, z, "minecraft:water", level=0)
            b.set(x, -1, z, "ashen_soil")
    for x in range(centre - 17, centre + 18):
        for z in range(centre - 17, centre + 18):
            # Its shore: moss where the tears lap at the paving.
            if b.get(x, 0, z) not in ("minecraft:water",) and b.get(x, 1, z) == "minecraft:air" and b.get(x, 0, z) != f"{NS}:sleepwood_log"                     and not (abs(x - centre) <= 2 and z > centre)                     and any(b.get(x + dx, 0, z + dz) == "minecraft:water" for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                b.set(x, 0, z, "ashen_moss")
    for _ in range(90):
        x, z = rng.randrange(centre - plaza, centre + plaza), rng.randrange(centre - plaza, centre + plaza)
        if b.get(x, 0, z) == f"{NS}:ashen_moss" and b.get(x, 1, z) == "minecraft:air":
            b.set(x, 1, z, rng.choice(["lucid_bloom", "lucid_bloom", "despair_thorns"]))

    # Its lord: the Weeping Gardener, kneeling before the weeping tree, facing it, between it and the south way in.
    b.mobs.append((centre + 0.5, 1.0, centre + 9.5, {
        "id": f"{NS}:gardener", "PersistenceRequired": True, "Rotation": List(5, [Float(180.0), Float(0.0)])}))
    # Last, the author's own hand-made finish of the plaza (2026-10-15), read block by block from their edit in the
    # world: garden_plaza_edit.json, [x, y, z, block, properties] in the garden's own coordinates (the plaza's piece and
    # the ground under it). Taken the same way as the tree's heart (garden_heart_edit.json, in weeping_tree).
    with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "garden_plaza_edit.json"), encoding="utf-8") as f:
        for x, y, z, name, props in json.load(f):
            b.set(x, y, z, name, **props)

    b.garden = dict(span=span, centre=centre, gate=gate, W=W, lo=lo, hi=hi, plaza_lo=plaza_lo, plaza_hi=plaza_hi, n=n, cell=cell)
    return b


SIGNS = [["", "TURN BACK", "", ""], ["IT KNOWS", "YOUR", "NAME", ""], ["", "WAKE UP", "WAKE UP", ""],
         ["THE GARDEN", "KEEPS", "WHAT IT", "GROWS"], ["", "WE ARE", "STILL HERE", ""], ["DONT LOOK", "AT THE", "TREE", ""],
         ["", "NO WAY", "OUT", ""], ["HE WEEPS", "FOR YOU", "", ""]]


def sign(b, x, y, z, rotation, lines, rng):
    """A standing sign of dark wood, its words scratched into it; now and then they glow red."""
    glow = rng.random() < 0.3
    text = {"messages": List(8, [Str(line) for line in lines]), "color": Str("red" if glow else "black"),
            "has_glowing_text": Tag(1, 1 if glow else 0)}
    b.set(x, y, z, "minecraft:dark_oak_sign", nbt={"id": "minecraft:sign", "front_text": Compound(text),
                                                   "back_text": Compound({"messages": List(8, [Str("")] * 4)}),
                                                   "is_waxed": Tag(1, 1)}, rotation=rotation, waterlogged=False)


class Raised:
    """A build seen y0 blocks higher: what is set at y is set at y + y0. For putting clutter on a lower floor."""

    def __init__(self, b, y0):
        self.b, self.y0 = b, y0

    def set(self, x, y, z, *args, **kwargs):
        self.b.set(x, y + self.y0, z, *args, **kwargs)

    def get(self, x, y, z):
        return self.b.get(x, y + self.y0, z)

    def fill(self, x0, y0, z0, x1, y1, z1, *args, **kwargs):
        self.b.fill(x0, y0 + self.y0, z0, x1, y1 + self.y0, z1, *args, **kwargs)


def fire_lamp(b, x, y, z, facing):
    """A dread fire lamp (FireLampBlock), four blocks tall from (x, y, z)."""
    for k, part in enumerate(("base", "shaft", "shaft", "top")):
        b.set(x, y + k, z, "dread_fire_lamp", facing=facing, part=part)


def clutter(b, rng, x, z, kind=None, y0=0):
    """One of the maze's grim things at (x, z), its floor at y 0: a broken statue, a bowl of blood (the Deep's water
    is blood red), a lamp post, a sign, a skeleton, a trail of blood, cracked pots, candles burnt down."""
    if y0:
        b = Raised(b, y0)
    kind = kind or rng.choice(["statue", "blood", "lamp", "lamp_hook", "sign", "skeleton", "trail", "pots", "candles"])
    if kind == "statue":
        # A statue fallen to pieces: its plinth, the stump of it, its head rolled off beside it.
        b.set(x, 1, z, "polished_dreamstone")
        b.set(x, 2, z, "dreamstone_pillar", axis="y")
        if rng.random() < 0.5:
            b.set(x, 3, z, "cracked_dreamstone_bricks")
        else:
            b.set(x, 3, z, "dreamstone_brick_slab", type="bottom", waterlogged=False)
        dx, dz = rng.choice(((1, 0), (-1, 0), (0, 1), (0, -1)))
        b.set(x + dx, 1, z + dz, "chiseled_dreamstone")
        b.set(x - dz, 1, z + dx, "dreamstone_brick_slab", type="bottom", waterlogged=False)
    elif kind == "blood":
        # A bowl of blood on a step, a candle by it.
        b.set(x, 1, z, "minecraft:water_cauldron", level=3)
        b.set(x + 1, 1, z, "minecraft:red_candle", candles=rng.randint(1, 3), lit=True, waterlogged=False)
        b.set(x, 0, z, "polished_dreamstone")
    elif kind == "lamp":
        # A lamp post: a dread fire lamp, four blocks tall.
        fire_lamp(b, x, 1, z, rng.choice(("north", "south", "east", "west")))
    elif kind == "lamp_hook":
        # A taller post with an arm, the lantern hung from it.
        b.fill(x, 1, z, x, 4, z, "dreamstone_wall", up=True, north="none", south="none", east="none", west="none", waterlogged=False)
        dx, dz, d = rng.choice(((1, 0, "east"), (-1, 0, "west"), (0, 1, "south"), (0, -1, "north")))
        b.set(x, 4, z, "dreamstone_wall", up=True, waterlogged=False, **{k: ("low" if k == d else "none") for k in ("north", "south", "east", "west")})
        b.set(x + dx, 4, z + dz, "dreamstone_wall", up=False, waterlogged=False,
              **{k: ("low" if k == {"east": "west", "west": "east", "north": "south", "south": "north"}[d] else "none")
                 for k in ("north", "south", "east", "west")})
        b.set(x + dx, 3, z + dz, "dread_lantern", hanging=True, waterlogged=False)
    elif kind == "sign":
        sign(b, x, 1, z, rng.randrange(16), rng.choice(SIGNS), rng)
    elif kind == "skeleton":
        # Someone who never got out: a skull, ribs and long bones in the moss.
        b.set(x, 1, z, "minecraft:skeleton_skull", rotation=rng.randrange(16), powered=False)
        dx, dz, axis = rng.choice(((1, 0, "x"), (0, 1, "z")))
        b.set(x + dx, 0, z + dz, "minecraft:bone_block", axis=axis)
        b.set(x + 2 * dx, 0, z + 2 * dz, "minecraft:bone_block", axis=axis)
        if rng.random() < 0.5:
            b.set(x + dx + dz, 1, z + dz + dx, "minecraft:cobweb")
    elif kind == "trail":
        # A trail of blood dragged along the floor.
        dx, dz = rng.choice(((1, 0), (0, 1)))
        for k in range(rng.randint(2, 4)):
            if b.get(x + dx * k, 1, z + dz * k) == "minecraft:air":
                b.set(x + dx * k, 1, z + dz * k, "minecraft:redstone_wire", power=0, north="none", south="none", east="none", west="none")
    elif kind == "pots":
        b.set(x, 1, z, "minecraft:decorated_pot", facing=rng.choice(["north", "south", "east", "west"]), cracked=True, waterlogged=False)
        if rng.random() < 0.6:
            b.set(x + 1, 1, z, "minecraft:decorated_pot", facing="north", cracked=True, waterlogged=False)
    else:
        b.set(x, 1, z, "minecraft:black_candle", candles=rng.randint(2, 4), lit=rng.random() < 0.5, waterlogged=False)
        b.set(x, 0, z, "polished_dreamstone")


MAZE = f"{NS}:garden_maze"
MAZE_POOL = f"{NS}:garden/maze"
MAZE_WAY = f"{NS}:garden/maze_way"  # plain corridors only, for the ways in
OPEN_SIDES = {"N": ((4, 0), "north_up"), "S": ((4, 8), "south_up"), "W": ((0, 4), "west_up"), "E": ((8, 4), "east_up")}


def hedge_top(b, rng, x, z, high):
    """A hedge from the ground to `high`, coped with polished slabs, gone ragged with neglect: here a slab missing,
    there a worn brick one in its place, the hedge grown up through the coping, or its top broken away a block down."""
    roll = rng.random()
    if roll < 0.06:
        b.fill(x, 1, z, x, high - 1, z, "withered_hedge")  # broken away: a notch in the wall's top
    elif roll < 0.14:
        b.fill(x, 1, z, x, high + 1, z, "withered_hedge")  # grown up through the coping
    else:
        b.fill(x, 1, z, x, high, z, "withered_hedge")
        if roll < 0.24:
            pass  # the slab gone
        elif roll < 0.36:
            b.set(x, high + 1, z, "dreamstone_brick_slab", type="bottom", waterlogged=False)  # a worn one in its place
        else:
            b.set(x, high + 1, z, "polished_dreamstone_slab", type="bottom", waterlogged=False)


def maze_cell(seed, opens, kind):
    """One cell of the Garden's maze, 9 by 9: a corridor seven wide with withered hedge round it, open on the sides in
    opens (N, S, W, E), with a jigsaw in the middle of each opening for the next cell to join to. A stone pier stands
    at each corner (four cells meeting make it a pier two by two). Along its closed sides the hedge has grown in here
    and there (rarely), so the corridors mostly run seven wide. Then what the cell holds, by kind."""
    b = Build(seed)
    rng = b.rng
    H = 7
    # (Rare: the ways stay wide; and never in a trap, whose works are set in the hedge itself.)
    grown = {side for side in "NSWE" if side not in opens and not kind.startswith("trap") and rng.random() < 0.15}

    def opening(x, z):
        return ((z == 0 and "N" in opens) or (z == 8 and "S" in opens)) and 1 <= x <= 7 or \
               ((x == 0 and "W" in opens) or (x == 8 and "E" in opens)) and 1 <= z <= 7

    def hedge_at(x, z):
        if x in (0, 8) or z in (0, 8):
            return not opening(x, z)
        return (x == 1 and "W" in grown) or (x == 7 and "E" in grown) or (z == 1 and "N" in grown) or (z == 7 and "S" in grown)

    for x in range(9):
        for z in range(9):
            # The floor: ash and moss, and worn flagstones down the middle of each way through it.
            on_path = (abs(x - 4) <= 1 and ((z <= 4 and "N" in opens) or (z >= 4 and "S" in opens))) or \
                      (abs(z - 4) <= 1 and ((x <= 4 and "W" in opens) or (x >= 4 and "E" in opens)))
            if on_path and rng.random() < 0.7:
                b.set(x, 0, z, "cracked_dreamstone_bricks" if rng.random() < 0.35 else "dreamstone_bricks")
            else:
                b.set(x, 0, z, "ashen_moss" if rng.random() < 0.45 else "ashen_soil")
            for y in range(1, 10):
                b.air(x, y, z)
            corner = x in (0, 8) and z in (0, 8)
            if corner:
                runes = rng.random() < 0.3
                b.set(x, 1, z, "polished_dreamstone")
                # The pier stands a block above the hedges' coping, capped.
                for y in range(2, H + 2):
                    b.set(x, y, z, "engraved_dreamstone_bricks" if runes and y == 4 else
                          "cracked_dreamstone_bricks" if rng.random() < 0.2 else "dreamstone_bricks")
                b.set(x, H + 2, z, "polished_dreamstone")
                roll = rng.random()
                if roll < 0.18:
                    b.set(x, H + 3, z, "dread_lantern", hanging=False, waterlogged=False)
                elif roll < 0.3:
                    b.set(x, H + 3, z, "chiseled_dreamstone")
            elif hedge_at(x, z):
                # The hedge trimmed, and along its top a coping of slabs, gone ragged and ruined in places.
                hedge_top(b, rng, x, z, H)
    # A trap is a corridor and must lead on: its ways are joined first (placement priority), before the cells round
    # it can take the ground beyond, so it is not left facing hedge, a dead end with a trap in it.
    first = 10 if kind.startswith("trap_") and kind != "trap_tnt" else 0
    for side in opens:
        (jx, jz), orientation = OPEN_SIDES[side]
        b.jigsaw(jx, 1, jz, orientation, MAZE, MAZE, MAZE_POOL, "minecraft:air", placement=first, selection=first)

    # Over one of its openings, maybe, an arch of polished stone with a lantern hung in it; or across it, the remains
    # of a garden fence, its gate swung open.
    if opens and rng.random() < 0.45:
        side = rng.choice(sorted(opens))
        line = {"N": [(x, 0) for x in range(1, 8)], "S": [(x, 8) for x in range(1, 8)],
                "W": [(0, z) for z in range(1, 8)], "E": [(8, z) for z in range(1, 8)]}[side]
        ends = ("west", "east") if side in "NS" else ("north", "south")
        if rng.random() < 0.6:
            for k, (x, z) in enumerate(line):
                if k == 0:
                    b.set(x, 5, z, "polished_dreamstone_stairs", facing=ends[1], half="top", shape="straight", waterlogged=False)
                elif k == len(line) - 1:
                    b.set(x, 5, z, "polished_dreamstone_stairs", facing=ends[0], half="top", shape="straight", waterlogged=False)
                elif k in (1, len(line) - 2):
                    b.set(x, 5, z, "polished_dreamstone_slab", type="top", waterlogged=False)
                b.set(x, 6, z, "engraved_dreamstone_bricks" if k == 3 else "dreamstone_bricks")
                b.set(x, 7, z, "polished_dreamstone_slab", type="bottom", waterlogged=False)
            b.set(line[3][0], 5, line[3][1], "dread_lantern", hanging=True, waterlogged=False)
        else:
            inner = {"N": [(x, 1) for x in range(1, 8)], "S": [(x, 7) for x in range(1, 8)],
                     "W": [(1, z) for z in range(1, 8)], "E": [(7, z) for z in range(1, 8)]}[side]
            sides = {"east", "west"} if side in "NS" else {"north", "south"}
            facing = "south" if side in "NS" else "east"
            for k, (x, z) in enumerate(inner):
                if k in (2, 3, 4):
                    b.set(x, 1, z, "minecraft:dark_oak_fence_gate", facing=facing, open=True, in_wall=False, powered=False)
                elif rng.random() < 0.75:
                    b.set(x, 1, z, "sleepwood_fence", waterlogged=False, **{d: (d in sides) for d in ("north", "south", "east", "west")})

    # What it holds, by kind: dead ends keep what the lost left there, or a secret; some cells are traps, some have
    # things in them that are not dead yet; the rest are dressed by their shape (dress, below).
    closed = [side for side in "NSWE" if side not in opens]
    if kind == "chest":
        b.chest(4, 1, 6, "garden_dead_end", "north") if "N" in opens else None
        b.chest(4, 1, 2, "garden_dead_end", "south") if "S" in opens else None
        b.chest(6, 1, 4, "garden_dead_end", "west") if "W" in opens else None
        b.chest(2, 1, 4, "garden_dead_end", "east") if "E" in opens else None
        b.set(3, 1, 3, "minecraft:skeleton_skull", rotation=rng.randrange(16), powered=False)
    elif kind == "bench":
        # A seat of stairs with its back to the far hedge, slabs either side, an urn at each end.
        far = {"N": (4, 7, "south", (1, 0)), "S": (4, 1, "north", (1, 0)), "W": (7, 4, "east", (0, 1)), "E": (1, 4, "west", (0, 1))}
        x, z, facing, (ax, az) = far[next(iter(opens))]
        b.set(x, 1, z, "polished_dreamstone_stairs", facing=facing, half="bottom", shape="straight", waterlogged=False)
        b.set(x - ax, 1, z - az, "polished_dreamstone_slab", type="bottom", waterlogged=False)
        b.set(x + ax, 1, z + az, "polished_dreamstone_slab", type="bottom", waterlogged=False)
        b.set(x - 2 * ax, 1, z - 2 * az, "dreamstone_urn", facing=facing)
        b.set(x + 2 * ax, 1, z + 2 * az, "dreamstone_urn", facing=facing)
    elif kind == "statue":
        far = {"N": (4, 5, "north"), "S": (4, 3, "south"), "W": (5, 4, "west"), "E": (3, 4, "east")}
        x, z, facing = far[next(iter(opens))]
        statue(b, x, z, 1, facing)
    elif kind == "secret_curtain":
        # It looks like a dead end, but the hedge across it is a curtain of weeping vines at the middle, and behind it
        # a hidden chamber: a stone floor, a chest, a bowl of blood, a skeleton, a sign.
        # (Its roof is hedge at the walls' own height, coped like them, so from above it is one more hedge.)
        for x in range(1, 8):
            for z in range(4, 8):
                for y in range(1, 9):
                    b.air(x, y, z)
                b.set(x, 0, z, "dreamstone_bricks" if rng.random() < 0.7 else "cracked_dreamstone_bricks")
                b.set(x, H, z, "withered_hedge")
                b.air(x, H + 1, z)
                if rng.random() < 0.85:
                    b.set(x, H + 1, z, "polished_dreamstone_slab" if rng.random() < 0.8 else "dreamstone_brick_slab", type="bottom", waterlogged=False)
            b.fill(x, 1, 4, x, H, 4, "withered_hedge")
        # The curtain: two blocks of vine in the hedge, which a body can push through.
        b.set(4, 2, 4, "minecraft:weeping_vines_plant")
        b.set(4, 1, 4, "minecraft:weeping_vines", age=25)
        b.chest(4, 1, 7, "garden_secret", "north")
        clutter(b, rng, 2, 6, "blood")
        clutter(b, rng, 6, 5, "skeleton")
        sign(b, 6, 1, 7, 8, ["YOU FOUND", "IT", "", "NOW HIDE"], rng)
        b.set(4, H - 1, 6, "dread_lantern", hanging=True, waterlogged=False)
    elif kind in ("secret_hatch", "hatch"):
        # A loose flagstone, a trapdoor under it, a ladder down, and under the garden a vault dug into the island:
        # brick walls, chests, a bowl of blood, the bones of whoever hid there. In a dead end the flagstone is at its
        # foot; in a corridor (hatch), against one of its hedges, where nobody looks.
        if kind == "secret_hatch":
            tx, tz, ladder = 4, 7, "north"
        else:
            side = rng.choice(closed)
            tx, tz, ladder = {"N": (4, 1, "south"), "S": (4, 7, "north"), "W": (1, 4, "east"), "E": (7, 4, "west")}[side]
        vault(b, rng, tx, tz, ladder)
        if kind == "secret_hatch":
            return b
    elif kind == "trap_arrows":
        # A tripwire across the corridor at its middle, hooked to stone hidden in the hedges either side; trip it and
        # the dispensers set in the hedge over the hooks loose arrows, some of them poisoned, from both sides.
        across = "NS" if "N" in opens else "WE"
        line = [(x, 4) for x in range(9)] if across == "NS" else [(4, z) for z in range(9)]
        faces = {"NS": ("east", "west"), "WE": ("south", "north")}[across]
        for k, (x, z) in enumerate(line):
            if k in (0, 8):
                b.set(x, 1, z, "dreamstone_bricks")
                arrows = [{"Slot": Tag(1, 0), "id": "minecraft:arrow", "count": 24},
                          {"Slot": Tag(1, 1), "id": "minecraft:tipped_arrow", "count": 12,
                           "components": {"minecraft:potion_contents": {"potion": "minecraft:poison"}}}]
                b.set(x, 2, z, "minecraft:dispenser", nbt={"id": "minecraft:dispenser", "Items": List(10, [to_nbt(a) for a in arrows])},
                      facing=faces[0 if k == 0 else 1], triggered=False)
            elif k in (1, 7):
                b.set(x, 1, z, "minecraft:tripwire_hook", facing=faces[0 if k == 1 else 1], attached=True, powered=False)
            else:
                ns = across == "WE"
                b.set(x, 1, z, "minecraft:tripwire", attached=True, disarmed=False, powered=False,
                      north=ns, south=ns, east=not ns, west=not ns)
        dress(b, rng, opens, closed, avoid={3, 4, 5})
        return b
    elif kind == "trap_pit":
        # The floor fallen in across the corridor's middle: a pit five deep, stone-walled, its bottom set with stone
        # spikes and old webs, a ladder up each end; over it the stub of a bridge, its middle slab gone, to be jumped.
        across = "NS" if "N" in opens else "WE"
        pit = {(x, z) for x in range(1, 8) for z in range(3, 6)} if across == "NS" else {(x, z) for x in range(3, 6) for z in range(1, 8)}
        for x in range(0, 9):
            for z in range(0, 9):
                if (x, z) in pit:
                    for y in range(-4, 2):
                        b.air(x, y, z)
                    b.set(x, -5, z, "dreamstone")
                    roll = rng.random()
                    if roll < 0.65:
                        b.set(x, -4, z, "dreamstone_spikes")
                    elif roll < 0.75:
                        b.set(x, -4, z, "minecraft:cobweb")
                elif any(abs(x - px) <= 1 and abs(z - pz) <= 1 for px, pz in pit):
                    for y in range(-5, 0):
                        b.set(x, y, z, "cracked_dreamstone_bricks" if rng.random() < 0.3 else "dreamstone_bricks")
        for k in (3, 5):
            x, z = (4, k) if across == "NS" else (k, 4)
            b.set(x, 0, z, "dreamstone_brick_slab", type="top", waterlogged=False)
        # A way out for whoever falls in: old ladders up the pit's walls, one at each end of it, on opposite sides,
        # clear of the spikes at their foot.
        for k, side in ((2, 0), (6, 1)):
            if across == "NS":
                x, z, facing = k, (3, 5)[side], ("south", "north")[side]
            else:
                x, z, facing = (3, 5)[side], k, ("east", "west")[side]
            for y in range(-4, 1):
                b.set(x, y, z, "sleepwood_ladder", facing=facing, waterlogged=False)
        if across == "NS":
            sign(b, 6, 1, 1, 0, ["", "MIND", "THE GAP", ""], rng)
        else:
            sign(b, 1, 1, 6, 4, ["", "MIND", "THE GAP", ""], rng)
        return b
    elif kind == "trap_tnt":
        # A dead end with a fine chest in it, the bones of the last who knelt before it; it is a trapped chest, and
        # under it, blasting powder.
        far = {"N": (4, 6, "north"), "S": (4, 2, "south"), "W": (6, 4, "west"), "E": (2, 4, "east")}
        x, z, facing = far[next(iter(opens))]
        b.set(x, 1, z, "minecraft:trapped_chest", nbt={"id": "minecraft:trapped_chest", "LootTable": f"{NS}:chests/garden_treasure"},
              facing=facing, type="single", waterlogged=False)
        b.set(x, 0, z, "minecraft:tnt", unstable=False)
        b.set(x, -1, z, "minecraft:tnt", unstable=False)
        for dx, dz in ((1, 0), (-1, 0)) if facing in ("north", "south") else ((0, 1), (0, -1)):
            b.set(x + dx, 1, z + dz, "minecraft:red_candle", candles=rng.randint(1, 3), lit=True, waterlogged=False)
        sx, sz = {"N": (2, 3), "S": (6, 5), "W": (3, 6), "E": (5, 2)}[next(iter(opens))]
        clutter(b, rng, sx, sz, "skeleton")
        return b
    elif kind == "treasure":
        # A vault at a dead end: a chest on a dais between two braziers, a barrel, an urn, and a Hedge Warden and a Briar Archer keeping it.
        far = {"N": (4, 6, "north", 1, 0), "S": (4, 2, "south", 1, 0), "W": (6, 4, "west", 0, 1), "E": (2, 4, "east", 0, 1)}
        x, z, facing, ax, az = far[next(iter(opens))]
        for k in range(-2, 3):
            b.set(x + ax * k, 0, z + az * k, "polished_dreamstone")
        b.chest(x, 1, z, "garden_treasure", facing)
        b.set(x - 2 * ax, 1, z - 2 * az, "dread_brazier", facing=facing)
        b.set(x + 2 * ax, 1, z + 2 * az, "dread_brazier", facing=facing)
        b.set(x - ax, 1, z - az, "minecraft:barrel", nbt={"id": "minecraft:barrel", "LootTable": f"{NS}:chests/garden_dead_end"}, facing="up", open=False)
        b.set(x + ax, 1, z + az, "dreamstone_urn", facing=facing)
        b.mobs.append((3.5, 1.0, 3.5, creature(WARDEN)))
        b.mobs.append((5.5, 1.0, 5.5, creature(ARCHER)))
        return b
    elif kind == "ambush":
        # A crossing whose flagstones are set round a cage in the floor: a spawner of Briar Archers, made to wake in the
        # Deep's open dark (its own spawn rules: any light), whenever someone lingers.
        for x in range(2, 7):
            for z in range(2, 7):
                d = max(abs(x - 4), abs(z - 4))
                b.set(x, 0, z, "polished_dreamstone" if d == 1 else "engraved_dreamstone_bricks" if d == 2 else "dreamstone_bricks")
        b.set(4, 0, 4, "minecraft:spawner", nbt=spawner(ARCHER))
        for x, z in ((2, 2), (6, 6), (2, 6), (6, 2)):
            if rng.random() < 0.5:
                b.set(x, 1, z, "minecraft:skeleton_skull", rotation=rng.randrange(16), powered=False)
        return b
    elif kind == "graves":
        # A little graveyard at a dead end: two stone tombs lying side by side either side of a narrow aisle, each two
        # wide and four long with its head to the far hedge and candles over it; set in the ground under each
        # tomb's head, a spawner of Mourners, who climb out into the aisle between. A lantern on a post between the
        # tombs' heads, skulls by the way in.
        (fx, fz), facing = {"N": ((0, 1), "north"), "S": ((0, -1), "south"), "W": ((1, 0), "west"), "E": ((-1, 0), "east")}[next(iter(opens))]
        head_dir = {"north": "south", "south": "north", "west": "east", "east": "west"}[facing]  # the tombs' heads point away from the way in

        def at(a, d):  # a: across the cell (from its middle), d: how far in from the way in (1 to 7)
            if fz:
                return (4 + a, d if fz > 0 else 8 - d)
            return (d if fx > 0 else 8 - d, 4 + a)
        for cols in ((-2, -1), (1, 2)):  # a block in from the side hedges, a narrow aisle between them
            tomb(b, [at(a, 6) for a in cols], head_dir)
            inner = cols[1] if cols[0] < 0 else cols[0]
            outer = cols[0] if cols[0] < 0 else cols[1]
            gx, gz = at(inner, 7)
            b.set(gx, 1, gz, "minecraft:black_candle", candles=4, lit=True, waterlogged=False)
            ox, oz = at(outer, 7)
            b.set(ox, 1, oz, "minecraft:red_candle", candles=3, lit=True, waterlogged=False)
            hx, hz = at(inner, 6)
            b.set(hx, 0, hz, "minecraft:spawner", nbt=spawner(MOURNER))
        cx_, cz_ = at(0, 7)
        b.set(cx_, 1, cz_, "dreamstone_wall", up=True, north="none", south="none", east="none", west="none", waterlogged=False)
        b.set(cx_, 2, cz_, "dread_lantern", hanging=False, waterlogged=False)
        for a in (-1, 1):
            sx, sz = at(a, 2)
            b.set(sx, 1, sz, "minecraft:skeleton_skull", rotation=rng.randrange(16), powered=False)
        return b
    elif kind == "nest":
        # A dead end grown over with webs, a Thornling spawner in the middle of it, a chest half buried in silk.
        far = {"N": (4, 7, "north"), "S": (4, 1, "south"), "W": (7, 4, "west"), "E": (1, 4, "east")}
        cx_, cz_, facing = far[next(iter(opens))]
        b.chest(cx_, 1, cz_, "garden_dead_end", facing)
        b.set(4, 1, 4, "minecraft:spawner", nbt=spawner(THORNLING))
        for x in range(1, 8):
            for z in range(1, 8):
                for y in (1, 2, 5, 6):
                    if b.get(x, y, z) == "minecraft:air" and rng.random() < (0.25 if y < 3 else 0.5):
                        b.set(x, y, z, "minecraft:cobweb")
        return b

    if kind.startswith("secret"):
        return b
    if len(opens) == 1:
        if rng.random() < 0.4:
            puddle(b, rng)  # tears gathered in the dead end
        return b
    # The things in the corridors that are not dead yet.
    if kind == "haunted":
        # A cage sunk in the floor at the foot of one hedge, half hidden among the bed there, skulls round it: a gentle
        # spawner, one at a time (a Briar Archer, Mourner, Thornling or Weeping Wisp).
        side = rng.choice(closed)
        x, z = {"N": (4, 1), "S": (4, 7), "W": (1, 4), "E": (7, 4)}[side]
        b.set(x, 0, z, "minecraft:spawner", nbt=spawner(rng.choice([ARCHER, MOURNER, THORNLING, WISP]), gentle=True))
        for dx, dz in ((1, 0), (-1, 0)) if side in "NS" else ((0, 1), (0, -1)):
            b.set(x + dx, 1, z + dz, "minecraft:skeleton_skull", rotation=rng.randrange(16), powered=False)
    elif kind == "patrol":
        mob = rng.choice([ARCHER, MOURNER, WISP])
        b.mobs.append((4.5, 2.5 if mob == WISP else 1.0, 4.5, creature(mob)))
    elif kind == "spiders":
        b.mobs.append((3.5, 1.0, 3.5, creature(THORNLING)))
        b.mobs.append((5.5, 1.0, 5.5, creature(THORNLING)))
    dress(b, rng, opens, closed)
    return b


# The labyrinth's own creatures (deep/mob), never vanilla's.
MOURNER, ARCHER, THORNLING, WISP, WARDEN = (f"{NS}:mourner", f"{NS}:briar_archer", f"{NS}:thornling", f"{NS}:weeping_wisp", f"{NS}:hedge_warden")


def creature(mob):
    return {"id": mob, "PersistenceRequired": True}

STEP = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
CLOCKWISE = {"north": "east", "east": "south", "south": "west", "west": "north"}


def tomb(b, heads, facing):
    """A stone tomb (TombBlock): its two head cells `heads` (either order), its head pointing `facing`; it runs four
    blocks back from them. Its parts are laid as the block lays them: left is the head cell its right lies clockwise
    of."""
    (ax, az), (bx, bz) = heads
    cw = STEP[CLOCKWISE[facing]]
    left = (ax, az) if (ax + cw[0], az + cw[1]) == (bx, bz) else (bx, bz)
    back = STEP[facing]
    for side, (sx, sz) in (("left", left), ("right", (left[0] + cw[0], left[1] + cw[1]))):
        for seg in range(4):
            b.set(sx - back[0] * seg, 1, sz - back[1] * seg, "stone_tomb", facing=facing, side=side, segment=seg, open=False, occupied=True)


def spawner(mob, gentle=False):
    """A monster spawner's block entity: the mob, a few at a time, near it, and spawn rules of its own (any light),
    since the Deep's open sky is never dark enough by the game's own rules. A gentle one (in a corridor) wakes only
    close by, brings one at a time, keeps no more than two about, and is slow to bring the next."""
    rules = {"block_light_limit": {"min_inclusive": 0, "max_inclusive": 15}, "sky_light_limit": {"min_inclusive": 0, "max_inclusive": 15}}
    count, nearby, reach, lo, hi = (1, 2, 7, 400, 800) if gentle else (2, 4, 10, 240, 520)
    return {"id": "minecraft:mob_spawner", "SpawnData": {"entity": {"id": mob}, "custom_spawn_rules": rules},
            "Delay": Tag(2, 40), "MinSpawnDelay": Tag(2, lo), "MaxSpawnDelay": Tag(2, hi), "SpawnCount": Tag(2, count),
            "MaxNearbyEntities": Tag(2, nearby), "RequiredPlayerRange": Tag(2, reach), "SpawnRange": Tag(2, 3)}


def vault(b, rng, tx, tz, ladder):
    """The hidden vault under a cell: a trapdoor at (tx, tz) in its floor, a ladder down from it on the side facing
    `ladder`, brick walls, two chests, a bowl of blood, bones, a lantern."""
    for y in range(-5, 0):
        for x in range(0, 9):
            for z in range(0, 9):
                edge = x in (0, 8) or z in (0, 8)
                b.set(x, y, z, ("cracked_dreamstone_bricks" if rng.random() < 0.2 else "dreamstone_bricks") if edge else "minecraft:air")
    for x in range(0, 9):
        for z in range(0, 9):
            b.set(x, -6, z, "polished_dreamstone" if (x + z) % 2 else "dreamstone_bricks")
            b.set(x, -1, z, "dreamstone_bricks")
    # The ladder hangs on the block behind the trapdoor's hole.
    bx, bz = {"north": (0, 1), "south": (0, -1), "east": (-1, 0), "west": (1, 0)}[ladder]
    for y in range(-5, 0):
        b.set(tx + bx, y, tz + bz, "dreamstone_bricks")
        b.set(tx, y, tz, "minecraft:ladder", facing=ladder, waterlogged=False)
    b.set(tx, 0, tz, "minecraft:dark_oak_trapdoor", facing=ladder, half="top", open=False, powered=False, waterlogged=False)
    spots = [p for p in ((2, 2), (6, 2), (2, 6), (6, 6)) if abs(p[0] - tx) + abs(p[1] - tz) > 3]
    b.chest(spots[0][0], -5, spots[0][1], "garden_secret", "south" if spots[0][1] < 4 else "north")
    if len(spots) > 1:
        b.chest(spots[1][0], -5, spots[1][1], "garden_secret", "south" if spots[1][1] < 4 else "north")
    clutter(b, rng, 4, 4, "blood", y0=-6)
    if len(spots) > 2:
        clutter(b, rng, spots[2][0], spots[2][1], "skeleton", y0=-6)
    b.set(4, -2, 4, "dread_lantern", hanging=True, waterlogged=False)


def puddle(b, rng, avoid=(), y0=0):
    """A puddle of the garden's tears (the Deep's water, red as blood) sunk in the floor of a cell: a ragged round of
    three to a dozen blocks wherever the floor is open above, its rim gone to moss where the tears lap at the stone."""
    blocked = avoid_cells(avoid)
    cx, cz, r = rng.uniform(2.2, 5.8), rng.uniform(2.2, 5.8), rng.uniform(1.1, 2.2)
    wobble = [rng.uniform(-0.35, 0.35) for _ in range(8)]
    wet = []
    for x in range(1, 8):
        for z in range(1, 8):
            ang = math.atan2(z - cz, x - cx)
            edge = r + wobble[int((ang + math.pi) / (2 * math.pi) * 8) % 8]
            if math.hypot(x - cx, z - cz) > edge or (x, z) in blocked:
                continue
            if b.get(x, y0 + 1, z) != "minecraft:air" or b.get(x, y0, z) in ("minecraft:spawner", "minecraft:dark_oak_trapdoor", "minecraft:jigsaw"):
                continue
            wet.append((x, z))
    for x, z in wet:
        b.set(x, y0, z, "minecraft:water", level=0)
    for x, z in wet:
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            if (nx, nz) not in wet and 0 < nx < 8 and 0 < nz < 8 and b.get(nx, y0 + 1, nz) == "minecraft:air"                     and b.get(nx, y0, nz) != "minecraft:water" and rng.random() < 0.55:
                b.set(nx, y0, nz, "ashen_moss")
    return wet


def avoid_cells(avoid):
    """The cells near a trap's tripwire line, kept dry and clear."""
    return {(x, z) for k in avoid for x in range(9) for z in (k,)} | {(x, z) for k in avoid for z in range(9) for x in (k,)}


def dress(b, rng, opens, closed, avoid=()):
    """Dresses a corridor cell by its shape, on purpose rather than at random: what stands where is set by which
    sides are hedge and which are ways through. Beds at the foot of each hedge, blooms in the middle and thorns either
    side, in step; a rune stone set in the floor where ways meet; webs only high in the corners by the piers; and by
    shape, one of a few scenes:
      straight  a colonnade of stubby pillars along both hedges, or fire lamps facing across, or planters and urns
      corner    in the outer corner (where the two hedges meet): a stone tomb with candles, a brazier, or a dead tree
      tee       against the hedge across from the way in: a bench between urns, an altar with a bowl of blood and
                braziers, or a stone tomb with candles and a sign
      cross     a rune rose in the floor, braziers on two opposite inner corners
    `avoid` is positions along a straight's length (2 to 6) to keep clear, for a trap's tripwire."""
    straight = set(opens) in ({"N", "S"}, {"W", "E"})
    inset = {"N": lambda k: (k, 1), "S": lambda k: (k, 7), "W": lambda k: (1, k), "E": lambda k: (7, k)}
    facing_in = {"N": "south", "S": "north", "W": "east", "E": "west"}

    def free(x, z):
        return b.get(x, 1, z) == "minecraft:air"

    def plant(x, z, name):
        if free(x, z):
            b.set(x, 0, z, "ashen_moss")
            b.set(x, 1, z, name)

    for side in closed:
        for k, name in ((2, "despair_thorns"), (3, "lucid_bloom"), (5, "lucid_bloom"), (6, "despair_thorns")):
            if k not in avoid:
                plant(*inset[side](k), name)
    # Now and then, a puddle of blood tears in the flagstones (about one corridor in four).
    if rng.random() < 0.25:
        puddle(b, rng, avoid)

    if straight:
        scene = rng.choice(["colonnade", "lamps", "planters"])
        for side in closed:
            for k in {"colonnade": (2, 6), "lamps": (4,), "planters": (2, 4, 6)}[scene]:
                if k in avoid:
                    continue
                x, z = inset[side](k)
                if scene == "colonnade":
                    b.set(x, 1, z, "dreamstone_pillar", axis="y")
                    b.set(x, 2, z, "chiseled_dreamstone" if k == 2 else "polished_dreamstone")
                    b.set(x, 3, z, "polished_dreamstone_slab", type="bottom", waterlogged=False)
                elif scene == "lamps":
                    # Tall fire lamps facing each other across the way, four blocks high.
                    for k_, part in enumerate(("base", "shaft", "shaft", "top")):
                        b.set(x, 1 + k_, z, "dread_fire_lamp", facing=facing_in[side], part=part)
                else:
                    b.set(x, 1, z, "stone_planter" if k == 4 else "dreamstone_urn", facing=facing_in[side])
    elif len(opens) == 2:
        cx = 2 if "W" in closed else 6
        cz = 2 if "N" in closed else 6
        facing = facing_in["N" if "N" in closed else "S"]
        scene = rng.choice(["grave", "brazier", "tree"])
        if scene == "grave":
            # A stone tomb along the hedge across the corner, its head into the corner, candles at its foot.
            rows = (1, 2) if "N" in closed else (7, 6)
            hx, head = (1, "west") if "W" in closed else (7, "east")
            tomb(b, [(hx, rows[0]), (hx, rows[1])], head)
            foot = hx + (4 if hx == 1 else -4)
            b.set(foot, 1, rows[0], "minecraft:black_candle", candles=3, lit=True, waterlogged=False)
            b.set(foot, 1, rows[1], "minecraft:red_candle", candles=2, lit=True, waterlogged=False)
        elif scene == "brazier":
            b.set(cx, 1, cz, "dread_brazier", facing=facing)
        else:
            dead_tree(b, cx, cz, 1, rng.randint(5, 7))
    elif len(opens) == 3:
        side = closed[0]
        x, z = inset[side](4)
        dx, dz = (1, 0) if side in "NS" else (0, 1)
        f = facing_in[side]
        scene = rng.choice(["bench", "altar", "grave"])
        if scene == "bench":
            back = {"south": "north", "north": "south", "east": "west", "west": "east"}[f]
            b.set(x, 1, z, "polished_dreamstone_stairs", facing=back, half="bottom", shape="straight", waterlogged=False)
            for s_ in (-1, 1):
                b.set(x + dx * s_, 1, z + dz * s_, "polished_dreamstone_slab", type="bottom", waterlogged=False)
                b.set(x + 2 * dx * s_, 1, z + 2 * dz * s_, "dreamstone_urn", facing=f)
        elif scene == "altar":
            b.set(x, 1, z, "minecraft:water_cauldron", level=3)
            for s_ in (-1, 1):
                b.set(x + dx * s_, 1, z + dz * s_, "polished_dreamstone")
                b.set(x + dx * s_, 2, z + dz * s_, "minecraft:red_candle", candles=3, lit=True, waterlogged=False)
                b.set(x + 2 * dx * s_, 1, z + 2 * dz * s_, "dread_brazier", facing=f)
        else:
            # A stone tomb lying along the hedge, candles at either end, a sign by its head.
            ins = {"N": lambda k, t: (k, t), "S": lambda k, t: (k, 8 - t), "W": lambda k, t: (t, k), "E": lambda k, t: (8 - t, k)}[side]
            tomb(b, [ins(3, 1), ins(3, 2)], "west" if side in "NS" else "north")
            b.set(*ins(2, 1)[:1], 1, ins(2, 1)[1], "minecraft:black_candle", candles=2, lit=True, waterlogged=False)
            b.set(*ins(7, 1)[:1], 1, ins(7, 1)[1], "minecraft:black_candle", candles=2, lit=True, waterlogged=False)
            sx, sz = ins(2, 2)
            sign(b, sx, 1, sz, {"south": 0, "west": 4, "north": 8, "east": 12}[f], rng.choice(SIGNS), rng)
    else:
        for x in range(3, 6):
            for z in range(3, 6):
                b.set(x, 0, z, "chiseled_dreamstone" if (x, z) == (4, 4) else "engraved_dreamstone_bricks" if (x + z) % 2 else "polished_dreamstone")
        for x, z in ((2, 2), (6, 6)) if rng.random() < 0.5 else ((2, 6), (6, 2)):
            b.set(x, 1, z, "dread_brazier", facing="north")
    if len(opens) == 3 and b.get(4, 0, 4) != f"{NS}:chiseled_dreamstone":
        b.set(4, 0, 4, "engraved_dreamstone_bricks")
    for x, z in ((1, 1), (7, 1), (1, 7), (7, 7)):
        if rng.random() < 0.35 and b.get(x, 6, z) == "minecraft:air":
            b.set(x, 6, z, "minecraft:cobweb")


# The maze's cells: (name, open sides, kind, weight). Dead ends are also the fallback, closing off every branch at
# the depth limit.
MAZE_CELLS = [
    # Dead ends are rare (about one cell in nine) and branches common, so the maze spreads over the whole garden and
    # it is more walks than hedge; the pool's fallback closes every branch with a dead end at the depth limit anyway.
    # Dead ends: what the lost left, secrets, a trapped hoard, a guarded vault, a nest.
    ("end_chest", "N", "chest", 1), ("end_bench", "N", "bench", 1), ("end_statue", "N", "statue", 1),
    ("secret_curtain", "N", "secret_curtain", 1), ("secret_hatch", "N", "secret_hatch", 1),
    ("end_trapped", "N", "trap_tnt", 1), ("end_treasure", "N", "treasure", 1), ("end_nest", "N", "nest", 1),
    ("end_graves", "N", "graves", 1),
    # Straights: dressed, kept by a Bogged, an arrow trap, a pit, a hidden hatch.
    ("straight", "NS", "", 8), ("straight_patrol", "NS", "patrol", 3), ("straight_arrows", "NS", "trap_arrows", 3),
    ("straight_haunted", "NS", "haunted", 2),
    ("straight_pit", "NS", "trap_pit", 3), ("straight_hatch", "NS", "hatch", 2),
    ("corner", "NE", "", 10), ("corner_patrol", "NE", "patrol", 3), ("corner_hatch", "NE", "hatch", 2),
    ("corner_haunted", "NE", "haunted", 2),
    ("tee", "NEW", "", 18), ("tee_spiders", "NEW", "spiders", 5),
    ("cross", "NSEW", "", 12), ("cross_ambush", "NSEW", "ambush", 5),
]


def export_garden(b):
    """Cuts the Garden into jigsaw pieces and writes them with their pools and structure:
        start       the island, the ground, the hedge over the maze's ground; under each wall and the plaza, a jigsaw
        wall_<side> each side of the walls with its towers and gate (a jigsaw at the gate leads into the maze)
        plaza       the plaza, the weeping tree and the Gardener (a jigsaw at each way out leads into the maze)
        maze/<cell> the maze's cells, which the structure joins one to the next from the gates and the plaza
    The walls and the plaza are placed first, and each piece claims its box, so the cells fill only the ground
    between them, a different maze every time."""
    g = b.garden
    span, centre, gate, W, lo, hi = g["span"], g["centre"], g["gate"], g["W"], g["lo"], g["hi"]
    pl, ph = g["plaza_lo"], g["plaza_hi"]
    xs = [p[0] for p in b.blocks]
    ys = [p[1] for p in b.blocks]
    zs = [p[2] for p in b.blocks]
    gx0, gx1, gy0, gy1, gz0, gz1 = min(xs), max(xs), min(ys), max(ys), min(zs), max(zs)
    regions = {
        "wall_north": lambda x, y, z: y >= 1 and z <= 0,
        "wall_south": lambda x, y, z: y >= 1 and z >= span - 1,
        "wall_west": lambda x, y, z: y >= 1 and x <= 0 and 0 < z < span - 1,
        "wall_east": lambda x, y, z: y >= 1 and x >= span - 1 and 0 < z < span - 1,
        "plaza": lambda x, y, z: y >= 1 and pl <= x <= ph and pl <= z <= ph,
    }
    boxes = {
        "wall_north": ((gx0, 1, gz0), (gx1, gy1, 0)),
        "wall_south": ((gx0, 1, span - 1), (gx1, gy1, gz1)),
        "wall_west": ((gx0, 1, 1), (0, gy1, span - 2)),
        "wall_east": ((span - 1, 1, 1), (gx1, gy1, span - 2)),
        "plaza": ((pl, 1, pl), (ph, gy1, ph)),
    }
    # Joints: each wall and the plaza sits on the start by a vertical jigsaw (aligned, so it keeps the start's turn).
    anchors = {"wall_north": (20, lo + 1), "wall_south": (20, hi - 1), "wall_west": (lo + 1, 20), "wall_east": (hi - 1, 20),
               "plaza": (centre, centre)}
    for piece, (x, z) in anchors.items():
        pool = f"{NS}:garden/{piece}"
        b.jigsaw(x, 1, z, "down_north", f"{NS}:garden_{piece}", "minecraft:empty", "minecraft:empty", b.state(x, 1, z), joint="aligned")
        b.jigsaw(x, 0, z, "up_north", "minecraft:empty", f"{NS}:garden_{piece}", pool, b.state(x, 0, z), joint="aligned",
                 placement=10, selection=10)
    # The anchor the structure is set by (start_jigsaw_name): the garden's middle, under the tree, so it stands centred
    # on the chunk it starts in. The game builds a structure's pieces only within 8 chunks of that chunk; anchored by
    # a corner, the garden (some 190 blocks across) reached 12 and its far side was never built.
    b.jigsaw(centre, -1, centre, "up_north", f"{NS}:garden_anchor", "minecraft:empty", "minecraft:empty", b.state(centre, -1, centre))
    # Into the maze: from each gate, and from each of the plaza's ways out. The gates and the plaza's one way in
    # (south) open onto a plain corridor (garden/maze_way: no dead end, secret, or trap), so no way in is ever shut by
    # what grows behind it; the fenced ways out of the plaza take any cell.
    mid = gate + W // 2
    for x, z, orientation, pool_ in ((0, mid, "east_up", MAZE_WAY), (span - 1, mid, "west_up", MAZE_WAY), (mid, 0, "south_up", MAZE_WAY),
                                     (mid, span - 1, "north_up", MAZE_WAY), (pl, centre, "west_up", MAZE_POOL), (ph, centre, "east_up", MAZE_POOL),
                                     (centre, pl, "north_up", MAZE_POOL), (centre, ph, "south_up", MAZE_WAY)):
        b.jigsaw(x, 1, z, orientation, MAZE, MAZE, pool_, "minecraft:air")
    # And seeds of it out in the thicket, one in each quarter and between: grown only from the gates and the plaza,
    # a branch that died early left a whole quarter thicket. Each is a jigsaw in the start, in the hedge just north of
    # a cell, that grows the maze from that cell (the hedge stays where it is, unless another cell takes its place).
    n, cell = g["n"], g["cell"]
    for i, j in ((3, 3), (n - 4, 3), (3, n - 4), (n - 4, n - 4), (3, n // 2), (n - 4, n // 2), (n // 2, 3), (n // 2 - 4, n // 2 + 5)):
        x0, z0 = 1 + i * cell, 1 + j * cell
        b.jigsaw(x0 + cell // 2, 1, z0 - 1, "south_up", MAZE, MAZE, MAZE_POOL, f"{NS}:withered_hedge")
    sizes = {}
    taken = lambda x, y, z: any(keep(x, y, z) for keep in regions.values())
    # The start's box reaches far below the keel and well over the tree: with the structure's terrain adaptation
    # (beard_box) the world clears its own islands out of all of it, so nothing grows through the garden or hangs
    # over it. (The little skirt of ground that adaptation leaves under a box falls far below, near the world's foot.)
    clear_below, clear_above = -95, gy1 + 25
    sizes["garden/start"] = b.sub(lambda x, y, z: not taken(x, y, z)).export("garden/start", ((gx0, clear_below, gz0), (gx1, clear_above, gz1)))
    for piece, keep in regions.items():
        sizes[f"garden/{piece}"] = b.sub(keep).export(f"garden/{piece}", boxes[piece])
    for i, (name, opens, kind, _) in enumerate(MAZE_CELLS):
        deep = kind in ("secret_hatch", "hatch", "trap_pit", "trap_tnt")  # dug down into the island
        bounds = ((0, -6, 0), (8, 10, 8)) if deep else ((0, 0, 0), (8, 10, 8))
        maze_cell(900 + i, set(opens), kind).export(f"garden/maze/{name}", bounds)

    def pool(name, elements, fallback="minecraft:empty"):
        write(f"worldgen/template_pool/{name}.json", {"fallback": fallback, "elements": [{"weight": w, "element": {
            "element_type": "minecraft:single_pool_element", "location": f"{NS}:{loc}", "processors": "minecraft:empty",
            "projection": "rigid"}} for loc, w in elements]})

    pool("garden/start", [("garden/start", 1)])
    for piece in regions:
        pool(f"garden/{piece}", [(f"garden/{piece}", 1)])
    pool("garden/maze", [(f"garden/maze/{name}", w) for name, _, _, w in MAZE_CELLS], fallback=f"{NS}:garden/maze_end")
    pool("garden/maze_way", [(f"garden/maze/{name}", w) for name, opens, kind, w in MAZE_CELLS if len(opens) >= 2 and kind == ""])
    pool("garden/maze_end", [(f"garden/maze/{name}", 1) for name, opens, _, _ in MAZE_CELLS if len(opens) == 1])
    return sizes


# ---- Watching shrine ----
def watching_shrine():
    b = Build(303)
    b.island(6, 6, 0, 7, 6, 9)
    b.fill(3, 1, 3, 9, 1, 9, "dreamstone_bricks")
    b.fill(4, 2, 4, 8, 2, 8, "dreamstone_brick_slab", type="bottom", waterlogged=False)
    b.fill(5, 2, 5, 7, 2, 7, "dreamstone_bricks")
    b.fill(6, 3, 6, 6, 10, 6, "dreamstone_pillar", axis="y")
    for face in ((5, 6), (7, 6), (6, 5), (6, 7)):
        b.fill(face[0], 3, face[1], face[0], 7, face[1], "dreamstone_bricks")
    b.set(6, 8, 5, "chiseled_dreamstone")
    b.set(6, 11, 6, "veined_dreamstone")
    b.set(6, 12, 6, "wake_crystal", facing="up", waterlogged=False)
    b.set(6, 2, 3, "minecraft:decorated_pot", facing="north", cracked=True, waterlogged=False)
    b.chest(8, 2, 3, "watching_shrine", "south")
    for x, z in ((3, 3), (9, 3), (3, 9), (9, 9)):
        b.fill(x, 2, z, x, 4, z, "dreamstone_brick_wall", up=True, north="none", south="none", east="none", west="none", waterlogged=False)
        b.set(x, 5, z, "dread_lantern", hanging=False, waterlogged=False)
    b.decay(0.3, WEATHER)
    return b


# ---- Broken bedroom ----
def broken_bedroom():
    b = Build(404)
    b.island(5, 5, -1, 7, 5, 7, solid_top=False)
    # Half a floor of oak, torn along a jagged line, two walls of wallpaper, the rest of the room gone.
    for x in range(0, 11):
        for z in range(0, 10):
            if x + z * 0.8 + b.rng.random() * 2.5 < 14:
                b.set(x, 0, z, "minecraft:oak_planks")
    for y in range(1, 6):
        for x in range(0, 9 - y // 2):
            b.set(x, y, 0, "torn_wallpaper" if b.rng.random() < 0.9 else "minecraft:air")
        for z in range(0, 8 - y):
            b.set(0, y, z, "torn_wallpaper" if b.rng.random() < 0.9 else "minecraft:air")
    b.set(2, 1, 2, "minecraft:white_bed", facing="south", part="head", occupied=False)
    b.set(2, 1, 1, "minecraft:white_bed", facing="south", part="foot", occupied=False)
    b.set(4, 1, 1, "minecraft:oak_trapdoor", facing="south", half="top", open=False, powered=False, waterlogged=False)
    b.set(4, 2, 1, "minecraft:candle", candles=3, lit=False, waterlogged=False)
    b.set(1, 1, 5, "minecraft:bookshelf")
    b.set(1, 2, 5, "minecraft:bookshelf")
    b.fill(3, 1, 4, 5, 1, 6, "dusty_carpet")
    b.chest(6, 1, 1, "broken_bedroom", "south")
    b.set(1, 4, 3, "minecraft:lantern", hanging=False, waterlogged=False)
    return b


# ---- Hollow clocktower ----
def hollow_clocktower():
    b = Build(505)
    b.island(5, 5, 0, 8, 8, 12)
    h = 26
    b.fill(1, 1, 1, 9, 1, 9, "dreamstone_bricks")
    for y in range(2, h):
        for x in range(1, 10):
            for z in range(1, 10):
                edge = x in (1, 9) or z in (1, 9)
                corner = x in (1, 9) and z in (1, 9)
                if corner:
                    b.set(x, y, z, "dreamstone_pillar", axis="y")
                elif edge:
                    b.set(x, y, z, "dreamstone_bricks")
                else:
                    b.air(x, y, z)
    # A stair winding up the inside walls.
    path = [(x, 2) for x in range(2, 9)] + [(8, z) for z in range(3, 9)] + [(x, 8) for x in range(7, 1, -1)] + [(2, z) for z in range(7, 2, -1)]
    y = 2
    for i in range(h - 4):
        x, z = path[i % len(path)]
        b.set(x, y, z, "dreamstone_brick_slab", type="top" if i % 2 else "bottom", waterlogged=False)
        if i % 2:
            y += 1
    # Narrow windows, and the clock face high on the north side: a ring of chiseled stone with two hands.
    for y in (6, 12, 18):
        b.air(5, y, 1)
        b.air(5, y + 1, 1)
        b.air(9, y, 5)
        b.air(1, y, 5)
    cy = h - 5
    for x in range(1, 10):
        for yy in range(cy - 4, cy + 5):
            r = math.hypot(x - 5, yy - cy)
            if 3.4 < r < 4.6:
                b.set(x, yy, 0, "chiseled_dreamstone")
            elif r <= 3.4:
                b.set(x, yy, 0, "minecraft:smooth_basalt")
    b.fill(5, cy, 0, 5, cy + 3, 0, "veined_dreamstone")
    b.fill(5, cy, 0, 7, cy, 0, "veined_dreamstone")
    # The top: a broken crenellated floor, a bell with no rope, and the chest.
    b.fill(1, h, 1, 9, h, 9, "dreamstone_bricks")
    b.clear(2, h, 2, 3, h, 3)
    for x in range(1, 10, 2):
        b.set(x, h + 1, 1, "dreamstone_brick_wall", up=True, north="none", south="none", east="none", west="none", waterlogged=False)
        b.set(x, h + 1, 9, "dreamstone_brick_wall", up=True, north="none", south="none", east="none", west="none", waterlogged=False)
    b.set(5, h + 1, 5, "minecraft:bell", attachment="floor", facing="north", powered=False)
    b.chest(7, h + 1, 7, "hollow_clocktower", "west")
    b.set(3, h + 1, 7, "dread_lantern", hanging=False, waterlogged=False)
    b.decay(0.3, WEATHER)
    # The top third has fallen in on one side.
    for x in range(6, 10):
        for yy in range(h - 6 + (x - 6) * 2, h + 2):
            for z in range(1, 4):
                if b.get(x, yy, z) and b.rng.random() < 0.8:
                    b.air(x, yy, z)
    return b


# ---- Dream castle (placeholder) ----
CASTLE_FLOORS = 4
CASTLE_FLOOR_HEIGHT = 8
CASTLE_HALF = 18


def dream_castle():
    b = Build(606)
    c = CASTLE_HALF
    b.island(c, c, 0, c + 12, c + 10, 26)
    top = CASTLE_FLOORS * CASTLE_FLOOR_HEIGHT
    # The keep: thick walls, buttressed corners, floors every eight blocks.
    for y in range(1, top + 1):
        for x in range(0, 2 * c + 1):
            for z in range(0, 2 * c + 1):
                edge = x in (0, 2 * c) or z in (0, 2 * c)
                inner = x in (1, 2 * c - 1) or z in (1, 2 * c - 1)
                if edge:
                    b.set(x, y, z, "dreamstone_bricks")
                elif inner:
                    b.set(x, y, z, "dreamstone")
                else:
                    b.air(x, y, z)
    for f in range(CASTLE_FLOORS + 1):
        b.fill(1, f * CASTLE_FLOOR_HEIGHT, 1, 2 * c - 1, f * CASTLE_FLOOR_HEIGHT, 2 * c - 1, "dreamstone_bricks" if f else "dreamstone")
    for x, z in ((0, 0), (2 * c, 0), (0, 2 * c), (2 * c, 2 * c)):
        b.fill(x - 2, 1, z - 2, x + 2, top + 8, z + 2, "dreamstone_bricks")
        b.fill(x - 1, top + 9, z - 1, x + 1, top + 12, z + 1, "dreamstone_brick_wall", up=True, north="none", south="none", east="none", west="none", waterlogged=False)
        b.set(x, top + 13, z, "dread_lantern", hanging=False, waterlogged=False)
    # The gate, on the south side: a tall arch, and a bridge of slabs out over the void.
    for y in range(1, 7):
        width = 3 if y < 5 else (2 if y == 5 else 1)
        b.clear(c - width + 1, y, 2 * c - 1, c + width - 1, y, 2 * c)
    b.fill(c - 2, 0, 2 * c + 1, c + 2, 0, 2 * c + 12, "dreamstone_brick_slab", type="top", waterlogged=False)
    for z in range(2 * c + 2, 2 * c + 13, 3):
        b.set(c - 3, 1, z, "dreamstone_brick_wall", up=True, north="none", south="none", east="none", west="none", waterlogged=False)
        b.set(c + 3, 1, z, "dreamstone_brick_wall", up=True, north="none", south="none", east="none", west="none", waterlogged=False)
        b.set(c - 3, 2, z, "dread_lantern", hanging=False, waterlogged=False)
        b.set(c + 3, 2, z, "dread_lantern", hanging=False, waterlogged=False)
    rng = b.rng
    # Each floor is a grid of rooms (three by three, eleven blocks each) with doors between them, every room dressed
    # as a dungeon cell, guard room, store or shrine; a stair in a corner room climbs to the next floor, each floor's in
    # a different corner so the way up winds through the whole of it.
    corners = [(0, 0), (2, 0), (2, 2), (0, 2)]
    for f in range(CASTLE_FLOORS - 1):
        y0 = f * CASTLE_FLOOR_HEIGHT + 1
        for i in range(1, 3):
            b.fill(2 + i * 11 - 1, y0, 2, 2 + i * 11 - 1, y0 + CASTLE_FLOOR_HEIGHT - 2, 2 * c - 2, "dreamstone_bricks")
            b.fill(2, y0, 2 + i * 11 - 1, 2 * c - 2, y0 + CASTLE_FLOOR_HEIGHT - 2, 2 + i * 11 - 1, "dreamstone_bricks")
        for rx in range(3):
            for rz in range(3):
                x0, z0 = 2 + rx * 11, 2 + rz * 11
                # Doors east and south of each room, so every room is reachable.
                if rx < 2:
                    b.clear(x0 + 10, y0, z0 + 4, x0 + 10, y0 + 2, z0 + 5)
                if rz < 2:
                    b.clear(x0 + 4, y0, z0 + 10, x0 + 5, y0 + 2, z0 + 10)
                kind = rng.choice(["cell", "guard", "store", "shrine"])
                if (rx, rz) == corners[f]:
                    for s in range(CASTLE_FLOOR_HEIGHT):
                        b.set(x0 + 1 + s, y0 + s, z0 + 1, "dreamstone_brick_stairs", facing="east", half="bottom", shape="straight", waterlogged=False)
                        b.clear(x0 + 1 + s, y0 + s + 1, z0 + 1, x0 + 1 + s, y0 + s + 3, z0 + 1)
                    b.clear(x0 + 1, y0 + CASTLE_FLOOR_HEIGHT - 1, z0 + 1, x0 + 9, y0 + CASTLE_FLOOR_HEIGHT - 1, z0 + 2)
                    continue
                if kind == "cell":
                    for k in range(1, 9, 3):
                        b.fill(x0 + k, y0, z0 + 6, x0 + k, y0 + 3, z0 + 6, "minecraft:iron_bars", east=True, west=True, north=False, south=False, waterlogged=False)
                    b.set(x0 + 2, y0, z0 + 8, "minecraft:cobweb")
                    b.set(x0 + 7, y0, z0 + 8, "minecraft:skeleton_skull", rotation=rng.randrange(16), powered=False)
                elif kind == "guard":
                    b.set(x0 + 4, y0, z0 + 4, "dreamstone_brick_slab", type="bottom", waterlogged=False)
                    b.set(x0 + 5, y0, z0 + 4, "dreamstone_brick_slab", type="bottom", waterlogged=False)
                    b.set(x0 + 4, y0 + 1, z0 + 4, "minecraft:candle", candles=2, lit=False, waterlogged=False)
                    b.chest(x0 + 1, y0, z0 + 8, "castle_room", "east")
                elif kind == "store":
                    for k in range(1, 9):
                        if rng.random() < 0.5:
                            b.set(x0 + k, y0, z0 + 1, rng.choice(["minecraft:barrel", "minecraft:bookshelf", "cracked_dreamstone_bricks"]),
                                  **({"facing": "up", "open": False} if False else {}))
                    b.chest(x0 + 8, y0, z0 + 8, "castle_room", "west")
                else:
                    b.set(x0 + 4, y0, z0 + 4, "chiseled_dreamstone")
                    b.set(x0 + 4, y0 + 1, z0 + 4, "wake_crystal", facing="up", waterlogged=False)
                    for dx, dz in ((-2, 0), (2, 0), (0, -2), (0, 2)):
                        b.set(x0 + 4 + dx, y0, z0 + 4 + dz, "lucid_bloom")
                b.set(x0 + 5, y0 + CASTLE_FLOOR_HEIGHT - 2, z0 + 5, "dread_lantern", hanging=True, waterlogged=False)
    # The boss hall: the whole top floor, one hall, pillars round it, a dais at the north end.
    y0 = (CASTLE_FLOORS - 1) * CASTLE_FLOOR_HEIGHT + 1
    for x in range(4, 2 * c - 3, 6):
        for z in (5, 2 * c - 5):
            b.fill(x, y0, z, x, y0 + CASTLE_FLOOR_HEIGHT - 2, z, "dreamstone_pillar", axis="y")
            b.set(x, y0 + 3, z + (1 if z < c else -1), "dread_lantern", hanging=False, waterlogged=False)
    b.fill(c - 5, y0, 3, c + 5, y0, 7, "veined_dreamstone")
    b.fill(c - 4, y0 + 1, 3, c + 4, y0 + 1, 6, "dreamstone_brick_slab", type="bottom", waterlogged=False)
    b.set(c, y0 + 1, 4, "chiseled_dreamstone")
    b.chest(c, y0 + 1, 2 + 1, "castle_hoard", "south")
    # The roof: crenellations, and the dais at its middle whose beam rises into the vortex.
    for x in range(0, 2 * c + 1):
        for z in range(0, 2 * c + 1):
            if (x in (0, 2 * c) or z in (0, 2 * c)) and (x + z) % 2 == 0:
                b.set(x, top + 1, z, "dreamstone_bricks")
    b.clear(1, top + 1, 1, 2 * c - 1, top + 6, 2 * c - 1)
    for x in range(c - 4, c + 5):
        for z in range(c - 4, c + 5):
            r = math.hypot(x - c, z - c)
            if r < 4.6:
                b.set(x, top + 1, z, "veined_dreamstone" if r < 2.5 else "chiseled_dreamstone")
    b.set(c, top + 2, c, "minecraft:end_rod", facing="up")
    # Where the beam rises from, for CastleVortex (its place in the template is written to deep_structure_sizes.json).
    b.marker = (c, top + 2, c)
    b.decay(0.08, {f"{NS}:dreamstone_bricks": ["cracked_dreamstone_bricks"]})
    return b


# ---- Worldgen files ----
def write(path, data):
    path = os.path.join(DATA, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def place(name, spacing, separation, salt, low, high, placement=None):
    write(f"worldgen/template_pool/{name}.json", {"fallback": "minecraft:empty", "elements": [{"weight": 1, "element": {
        "element_type": "minecraft:single_pool_element", "location": f"{NS}:{name}", "processors": "minecraft:empty", "projection": "rigid"}}]})
    write(f"worldgen/structure/{name}.json", {
        "type": "minecraft:jigsaw", "biomes": f"#{NS}:has_structure/{name}", "step": "surface_structures",
        "terrain_adaptation": "none", "start_pool": f"{NS}:{name}", "size": 1, "max_distance_from_center": 80,
        "start_height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": low}, "max_inclusive": {"absolute": high}},
        "spawn_overrides": {}, "use_expansion_hack": False})
    write(f"worldgen/structure_set/{name}.json", {"structures": [{"structure": f"{NS}:{name}", "weight": 1}], "placement": placement or {
        "type": "minecraft:random_spread", "spacing": spacing, "separation": separation, "salt": salt}})
    write(f"tags/worldgen/biome/has_structure/{name}.json", {"values": [f"{NS}:the_shallows"]})


def loot(name, rolls, entries, always=()):
    """A chest's loot: rolls of the weighted entries, and each item in always, once, every time."""
    pools = [{
        "rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "bonus_rolls": 0.0,
        "entries": [dict({"type": "minecraft:item", "name": item, "weight": weight},
                         **({"functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}, "add": False}]} if hi > 1 else {}))
                    for item, weight, lo, hi in entries]}]
    for item in always:
        pools.append({"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": item}]})
    write(f"loot_table/chests/{name}.json", {"type": "minecraft:chest", "random_sequence": f"{NS}:chests/{name}", "pools": pools})


COMMON = [("minecraft:bread", 10, 1, 4), ("minecraft:candle", 8, 1, 3), ("minecraft:string", 8, 2, 6), ("minecraft:bone", 8, 2, 5),
          (f"{NS}:lucid_bloom", 10, 1, 3), (f"{NS}:wake_crystal", 6, 1, 2), (f"{NS}:dread_lantern", 4, 1, 1),
          ("minecraft:book", 6, 1, 2), ("minecraft:iron_ingot", 6, 1, 4), ("minecraft:gold_ingot", 4, 1, 3)]
RARE = [("minecraft:diamond", 3, 1, 2), ("minecraft:emerald", 4, 1, 3), ("minecraft:enchanted_golden_apple", 1, 1, 1),
        ("minecraft:music_disc_11", 1, 1, 1), ("minecraft:name_tag", 2, 1, 1), ("minecraft:ender_pearl", 3, 1, 2)]


def main():
    sizes = {}
    sizes.update(export_garden(garden_of_despair()))
    for name, build in (("haunted_house", haunted_house), ("watching_shrine", watching_shrine),
                        ("broken_bedroom", broken_bedroom), ("hollow_clocktower", hollow_clocktower), ("dream_castle", dream_castle)):
        sizes[name] = build().export(name)
    place("haunted_house", 28, 10, 71230001, 90, 170)
    place("garden_of_despair", 44, 16, 71230002, 80, 150)
    # Its maze is joined from cells: a jigsaw structure that goes deep, starting from the garden's own pieces.
    write("worldgen/structure/garden_of_despair.json", {
        "type": "minecraft:jigsaw", "biomes": f"#{NS}:has_structure/garden_of_despair", "step": "surface_structures",
        "terrain_adaptation": "beard_box", "start_pool": f"{NS}:garden/start", "start_jigsaw_name": f"{NS}:garden_anchor",
        "size": 20, "max_distance_from_center": 116,
        "start_height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 100}, "max_inclusive": {"absolute": 150}},
        "spawn_overrides": {}, "use_expansion_hack": False})
    os.remove(os.path.join(DATA, "worldgen", "template_pool", "garden_of_despair.json"))
    stale = os.path.join(DATA, "structure", "garden_of_despair.nbt")
    if os.path.exists(stale):
        os.remove(stale)
    place("watching_shrine", 18, 6, 71230003, 70, 190)
    place("broken_bedroom", 14, 5, 71230004, 60, 200)
    place("hollow_clocktower", 30, 12, 71230005, 70, 140)
    # One castle, a fair way from where dreamers arrive.
    place("dream_castle", 0, 0, 0, 110, 110, {"type": "minecraft:concentric_rings", "distance": 6, "spread": 1, "count": 1,
                                              "preferred_biomes": f"#{NS}:has_structure/dream_castle", "salt": 71230006})
    loot("haunted_house", (3, 6), COMMON + [("minecraft:cobweb", 6, 1, 3), ("minecraft:clock", 2, 1, 1)])
    loot("haunted_house_cellar", (4, 7), COMMON + RARE)
    loot("garden_heart", (8, 12), RARE + [(f"{NS}:lucid_bloom", 8, 3, 6), ("minecraft:diamond", 8, 2, 5), ("minecraft:emerald", 6, 4, 9),
                                         ("minecraft:golden_apple", 6, 2, 4), ("minecraft:netherite_scrap", 3, 1, 2),
                                         ("minecraft:totem_of_undying", 1, 1, 1), (f"{NS}:wake_crystal", 5, 2, 4)],
         always=[f"{NS}:weeping_veil"])  # the tree's own gift, every time
    loot("garden_dead_end", (2, 4), COMMON + [("minecraft:skeleton_skull", 2, 1, 1)])
    # What a tomb holds when it holds no dead (TombBlock): a little of the garden's worth.
    loot("garden_tomb", (2, 5), [("minecraft:gold_ingot", 8, 1, 4), ("minecraft:emerald", 6, 1, 3), ("minecraft:bone", 10, 2, 6),
                                 ("minecraft:diamond", 2, 1, 1), (f"{NS}:wake_crystal", 5, 1, 2), (f"{NS}:lucid_bloom", 6, 1, 3),
                                 ("minecraft:experience_bottle", 4, 1, 3), ("minecraft:golden_apple", 2, 1, 1)])
    loot("garden_secret", (5, 8), COMMON + RARE + [(f"{NS}:lucid_bloom", 6, 2, 5), ("minecraft:golden_apple", 4, 1, 2)])
    # The maze's vaults (guarded, or trapped): the garden's best after the tree's own heart.
    loot("garden_treasure", (6, 10), RARE + [("minecraft:diamond", 6, 1, 3), ("minecraft:emerald", 8, 2, 6), ("minecraft:gold_ingot", 8, 2, 6),
                                             ("minecraft:golden_apple", 5, 1, 2), (f"{NS}:wake_crystal", 6, 1, 3),
                                             ("minecraft:arrow", 6, 8, 20), ("minecraft:experience_bottle", 5, 2, 6)])
    loot("watching_shrine", (3, 5), COMMON + [("minecraft:ender_eye", 3, 1, 2), ("minecraft:spyglass", 2, 1, 1)])
    loot("broken_bedroom", (2, 5), COMMON + [("minecraft:white_wool", 6, 1, 3), ("minecraft:clock", 3, 1, 1), ("minecraft:painting", 3, 1, 1)])
    loot("hollow_clocktower", (4, 7), COMMON + RARE + [("minecraft:clock", 6, 1, 1), ("minecraft:compass", 4, 1, 1)])
    loot("castle_room", (3, 6), COMMON + RARE)
    loot("castle_hoard", (8, 12), RARE + [("minecraft:netherite_scrap", 4, 1, 2), ("minecraft:diamond", 6, 2, 5), ("minecraft:golden_apple", 6, 1, 3)])
    with open(os.path.join(ROOT, "tools", "deep_structure_sizes.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(sizes, f, indent=2)


if __name__ == "__main__":
    main()
