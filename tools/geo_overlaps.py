"""Finds and fixes faces that fight: two cubes of a Bedrock .geo.json whose faces lie in the same plane, facing the same
way, over the same area, so the game draws both and neither wins (z-fighting: a flickering, striped patch). CoolCatLib
draws entities without culling, so this shows wherever two parts sit flush on a shared side: a plate laid exactly on a
body's face, a cuff the same size as an arm, two boxes of a part sharing an edge face.

    python tools/geo_overlaps.py [files or folders]     reports every such pair (all armatures if none given)

The generators call separate(bones) on each model before writing it: of each fighting pair, the smaller cube is
inflated a little (a few hundredths of a pixel, never seen) so its face stands just in front, and that is repeated
until none are left. Cubes are placed as CoolCatLib places them: their bones' rest turns about their pivots (x and y
reversed, as it reads Bedrock), their own turns, and their inflation.
"""
import glob
import json
import math
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EPS_PLANE = 0.004            # pixels apart and still one plane
MIN_AREA = 0.02              # square pixels shared before it shows
NUDGE = 0.035                # how much the smaller cube is inflated per pass


def rot_matrix(deg):
    """CoolCatLib's turn of a Bedrock rotation [x, y, z] in degrees: x and y reversed, applied z, then y, then x."""
    x, y, z = (math.radians(-deg[0]), math.radians(-deg[1]), math.radians(deg[2]))
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    rx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    ry = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    rz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return mul(mul(rz, ry), rx)


def mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def apply(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]


def flip(p):
    """A Bedrock position in CoolCatLib's own space (x the other way)."""
    return [-p[0], p[1], p[2]]


class Turn:
    """A rigid placement: p -> R p + t."""

    def __init__(self, r=None, t=None):
        self.r = r or [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
        self.t = t or [0.0, 0.0, 0.0]

    def about(self, pivot, deg):
        """This placement, then turned about a pivot (already in this space)."""
        r = rot_matrix(deg)
        # q = R (p - pivot) + pivot, applied after self: total = R*self.r, t' = R*(self.t - pivot) + pivot
        t = [a - b for a, b in zip(self.t, pivot)]
        t = [a + b for a, b in zip(apply(r, t), pivot)]
        return Turn(mul(r, self.r), t)

    def __call__(self, p):
        return [a + b for a, b in zip(apply(self.r, p), self.t)]


def placements(bones):
    """Each bone's rest placement in the model's space (parents first; its rest turn about its pivot)."""
    by_name = {b["name"]: b for b in bones}
    done = {}

    def place(name):
        if name in done:
            return done[name]
        bone = by_name[name]
        parent = place(bone["parent"]) if bone.get("parent") in by_name else Turn()
        pivot = flip(bone.get("pivot", [0, 0, 0]))
        turn = parent
        if any(bone.get("rotation", [0, 0, 0])):
            # The bone turns about its pivot, itself carried by its parent: parent(about(pivot)).
            local = Turn().about(pivot, bone["rotation"])
            turn = Turn(mul(parent.r, local.r), [a + b for a, b in zip(apply(parent.r, local.t), parent.t)])
        done[name] = turn
        return turn

    for b in bones:
        place(b["name"])
    return done


def cube_faces(cube, turn, inflate):
    """A cube's six faces in the model's space: (normal, corners)."""
    o, s = cube["origin"], cube["size"]
    lo = [-(o[0] + s[0]) - inflate, o[1] - inflate, o[2] - inflate]
    hi = [lo[0] + s[0] + 2 * inflate, lo[1] + s[1] + 2 * inflate, lo[2] + s[2] + 2 * inflate]
    own = Turn()
    if cube.get("rotation") and any(cube["rotation"]):
        own = Turn().about(flip(cube.get("pivot", [0, 0, 0])), cube["rotation"])
    place = lambda p: turn(own(p))
    faces = []
    for axis in range(3):
        for side, value in ((-1, lo[axis]), (1, hi[axis])):
            others = [a for a in range(3) if a != axis]
            corners = []
            for u, v in ((0, 0), (1, 0), (1, 1), (0, 1)):
                p = [0.0, 0.0, 0.0]
                p[axis] = value
                p[others[0]] = (lo, hi)[u][others[0]]
                p[others[1]] = (lo, hi)[v][others[1]]
                corners.append(place(p))
            normal = [0.0, 0.0, 0.0]
            normal[axis] = side
            n = apply(turn.r, apply(own.r, normal))
            # A face with no area (a flat cube's edge) fights nothing.
            if s[others[0]] + 2 * inflate > 1e-6 and s[others[1]] + 2 * inflate > 1e-6:
                faces.append((n, corners))
    return faces


def clip(subject, a, b, ub, vb):
    """Sutherland-Hodgman: the part of a convex polygon (2D) on the left of the edge a->b."""
    out = []
    def inside(p):
        return (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0]) >= -1e-9
    def cross(p, q):
        dx, dy = q[0] - p[0], q[1] - p[1]
        ex, ey = b[0] - a[0], b[1] - a[1]
        den = ex * dy - ey * dx
        t = ((p[0] - a[0]) * ey - (p[1] - a[1]) * ex) / den if abs(den) > 1e-12 else 0.0
        return [p[0] + t * dx, p[1] + t * dy]
    for i, p in enumerate(subject):
        q = subject[(i + 1) % len(subject)]
        if inside(q):
            if not inside(p):
                out.append(cross(p, q))
            out.append(q)
        elif inside(p):
            out.append(cross(p, q))
    return out


def area(poly):
    return abs(sum(poly[i][0] * poly[(i + 1) % len(poly)][1] - poly[(i + 1) % len(poly)][0] * poly[i][1] for i in range(len(poly)))) / 2


def shared_area(n, f1, f2):
    """How much of two faces in one plane lie over each other."""
    # A basis in the plane.
    helper = [1, 0, 0] if abs(n[0]) < 0.9 else [0, 1, 0]
    u = [n[1] * helper[2] - n[2] * helper[1], n[2] * helper[0] - n[0] * helper[2], n[0] * helper[1] - n[1] * helper[0]]
    ul = math.sqrt(sum(c * c for c in u))
    u = [c / ul for c in u]
    v = [n[1] * u[2] - n[2] * u[1], n[2] * u[0] - n[0] * u[2], n[0] * u[1] - n[1] * u[0]]
    to2 = lambda p: [sum(a * b for a, b in zip(p, u)), sum(a * b for a, b in zip(p, v))]
    a2, b2 = [to2(p) for p in f1], [to2(p) for p in f2]
    # Both counter-clockwise.
    def ccw(poly):
        s = sum(poly[i][0] * poly[(i + 1) % len(poly)][1] - poly[(i + 1) % len(poly)][0] * poly[i][1] for i in range(len(poly)))
        return poly if s > 0 else poly[::-1]
    a2, b2 = ccw(a2), ccw(b2)
    poly = a2
    for i in range(len(b2)):
        poly = clip(poly, b2[i], b2[(i + 1) % len(b2)], None, None)
        if not poly:
            return 0.0
    return area(poly)


def fights(bones):
    """Every pair of fighting faces: (bone a, cube index a, bone b, cube index b, shared area)."""
    turns = placements(bones)
    cubes = []
    for b in bones:
        for i, c in enumerate(b.get("cubes", [])):
            infl = c.get("inflate", b.get("inflate", 0.0))
            size = c["size"]
            cubes.append((b["name"], i, c, cube_faces(c, turns[b["name"]], infl), size[0] * size[1] * size[2]))
    found = []
    for i in range(len(cubes)):
        for j in range(i + 1, len(cubes)):
            ba, ia, ca, fa, va = cubes[i]
            bb, ib, cb, fb, vb = cubes[j]
            best = 0.0
            for na, pa in fa:
                for nb, pb in fb:
                    if sum(x * y for x, y in zip(na, nb)) < 0.9995:
                        continue
                    gap = sum(n * (p - q) for n, p, q in zip(na, pa[0], pb[0]))
                    if abs(gap) > EPS_PLANE:
                        continue
                    best = max(best, shared_area(na, pa, pb))
            if best > MIN_AREA:
                found.append((ba, ia, bb, ib, best, va, vb))
    return found


def separate(bones, passes=6):
    """Inflates the smaller cube of each fighting pair a little, until none fight; returns how many cubes it moved."""
    moved = set()
    for _ in range(passes):
        found = fights(bones)
        if not found:
            break
        by_name = {b["name"]: b for b in bones}
        nudged = set()
        for ba, ia, bb, ib, _, va, vb in found:
            bone, index = (ba, ia) if va <= vb else (bb, ib)
            if (bone, index) in nudged:
                continue
            cube = by_name[bone]["cubes"][index]
            cube["inflate"] = round(cube.get("inflate", by_name[bone].get("inflate", 0.0)) + NUDGE, 4)
            nudged.add((bone, index))
            moved.add((bone, index))
    return len(moved)


def report(paths):
    total = 0
    for path in paths:
        with open(path, encoding="utf-8") as f:
            geo = json.load(f)
        bones = geo["minecraft:geometry"][0]["bones"]
        found = fights(bones)
        total += len(found)
        if found:
            print(f"{os.path.basename(path)}: {len(found)} fighting pairs")
            for ba, ia, bb, ib, shared, *_ in sorted(found, key=lambda f: -f[4])[:6]:
                print(f"    {ba}[{ia}] / {bb}[{ib}]  {shared:.2f} px²")
    print(f"{total} fighting pairs in {len(paths)} models")


if __name__ == "__main__":
    targets = sys.argv[1:] or [os.path.join(ROOT, "common", "src", "main", "resources", "assets", "whycantisleep", "armatures")]
    files = []
    for t in targets:
        files += sorted(glob.glob(os.path.join(t, "*.geo.json"))) if os.path.isdir(t) else [t]
    report(files)
