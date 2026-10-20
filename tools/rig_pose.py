"""Poses a generated Bedrock armature as CoolCatLib does, for animations that know where the ground is.

A clip written by hand cannot tell whether a knee it kneels on reaches the ground, or a hand it plants sinks into it;
this can. Given a model's bones (the generators' Model) it works out where any point of any bone is for a pose, samples
a clip (keyframes, Catmull-Rom, and the Molang the generators write) at any time, solves joints to put points where
they are wanted (a knee on the ground, a second hand on a shaft) and settles a clip on the ground (lowers or lifts the
whole body, at every sample, until its lowest contact point rests on y = 0).

Coordinates are the geometry file's: pixels, y up, the model facing -z. A bone's turn is CoolCatLib's: about its pivot,
by its rest rotation plus the pose's, Z then Y then X, each as the file has it (Bedrock's mirrored x: +x tips a part's
top toward -z, +z leans it toward +x). A bone's position channel moves it (and all under it) in those axes.
"""
import math

import numpy as np


def _rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def _ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def _rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def turn(rx, ry, rz):
    """A file's rotation (degrees) as a matrix: the renderer mirrors x, so x and z turn the other way to a right-handed one."""
    return _rz(math.radians(-rz)) @ _ry(math.radians(ry)) @ _rx(math.radians(-rx))


class Rig:
    """A model's bones, ready to pose: by name, parents before children."""

    def __init__(self, bones):
        self.bones = {b["name"]: b for b in bones}
        self.order = [b["name"] for b in bones]
        self._chains = {}

    def chain(self, names):
        """The bones and every bone above them, in posing order (what placing them needs)."""
        key = frozenset(names)
        if key not in self._chains:
            need = set()
            for name in names:
                while name and name not in need:
                    need.add(name)
                    name = self.bones[name].get("parent")
            self._chains[key] = [n for n in self.order if n in need]
        return self._chains[key]

    def world(self, pose, only=None):
        """Each bone's placement (a 4x4 matrix, model pixels) for a pose: {bone: {"rotation", "position", "scale"}}.
        `only`: just the bones a caller needs (and those above them), for speed."""
        out = {}
        for name in (self.order if only is None else only):
            b = self.bones[name]
            p = np.array(b["pivot"], dtype=float)
            ch = pose.get(name, {})
            rest = b.get("rotation") or [0, 0, 0]
            r = ch.get("rotation", [0, 0, 0])
            t = np.array(ch.get("position", [0, 0, 0]), dtype=float)
            s = ch.get("scale", [1, 1, 1])
            if not isinstance(s, (list, tuple)):
                s = [s, s, s]
            m = np.eye(4)
            m[:3, :3] = turn(rest[0] + r[0], rest[1] + r[1], rest[2] + r[2]) @ np.diag(s)
            m[:3, 3] = p + t - m[:3, :3] @ p
            parent = b.get("parent")
            out[name] = out[parent] @ m if parent in out else m
        return out

    @staticmethod
    def at(world, bone, point):
        """Where a point of a bone (given at rest, model pixels) is in this pose."""
        return (world[bone] @ np.array([*point, 1.0]))[:3]


# ---- Sampling clips ----

def _mol(expr, t):
    """A Molang value the generators write, at time t: arithmetic, math.sin/cos/abs/max/min/pow (degrees, as Molang),
    q.anim_time, and one a ? b : c."""
    if not isinstance(expr, str):
        return float(expr)
    e = expr.replace("q.anim_time", "t").replace("math.", "_m_")
    if "?" in e:
        cond, rest = e.split("?", 1)
        yes, no = rest.split(":", 1)
        e = f"(({yes}) if ({cond}) else ({no}))"
    env = {"t": t, "_m_sin": lambda d: math.sin(math.radians(d)), "_m_cos": lambda d: math.cos(math.radians(d)),
           "_m_abs": abs, "_m_max": max, "_m_min": min, "_m_pow": pow, "_m_pi": math.pi}
    return float(eval(e, {"__builtins__": {}}, env))


def _cr(p0, p1, p2, p3, u):
    return 0.5 * (2 * p1 + (-p0 + p2) * u + (2 * p0 - 5 * p1 + 4 * p2 - p3) * u * u + (-p0 + 3 * p1 - 3 * p2 + p3) * u * u * u)


def channel(value, t):
    """A channel's value at time t: a fixed [x, y, z] (numbers or Molang), or keyframes {"time": {"post": [...]}}."""
    if isinstance(value, dict):
        keys = sorted((float(k), v["post"] if isinstance(v, dict) else v) for k, v in value.items())
        times = [k for k, _ in keys]
        vals = [[_mol(c, t) for c in v] for _, v in keys]
        if t <= times[0]:
            return vals[0]
        if t >= times[-1]:
            return vals[-1]
        i = max(j for j in range(len(times)) if times[j] <= t)
        u = (t - times[i]) / (times[i + 1] - times[i])
        p0, p1, p2, p3 = vals[max(0, i - 1)], vals[i], vals[i + 1], vals[min(len(vals) - 1, i + 2)]
        return [_cr(p0[k], p1[k], p2[k], p3[k], u) for k in range(3)]
    if isinstance(value, (list, tuple)):
        return [_mol(c, t) for c in value]
    v = _mol(value, t)
    return [v, v, v]


def sample(clip, t):
    """A clip's pose at time t."""
    pose = {}
    for bone, chans in clip["bones"].items():
        pose[bone] = {k: channel(v, t) for k, v in chans.items() if k in ("rotation", "position", "scale")}
    return pose


# ---- Solving ----

def solve(rig, pose, unknowns, error, step=12.0, tolerance=0.02, rounds=400):
    """Turns the unknowns ((bone, axis) pairs, axis 0-2 of its rotation) of a pose until error(world) is as small as it
    goes: a coordinate search, halving its step when nothing improves. Returns the pose, changed in place."""
    for bone, _ in unknowns:
        pose.setdefault(bone, {}).setdefault("rotation", [0.0, 0.0, 0.0])
        pose[bone]["rotation"] = list(pose[bone]["rotation"])
    # Which bones the error reads (asked once of a full posing), so each step after poses only those.
    read = set()

    class Probe(dict):
        def __getitem__(self, key):
            read.add(key)
            return dict.__getitem__(self, key)

    best = error(Probe(rig.world(pose)))
    only = rig.chain(read)
    world = lambda: rig.world(pose, only)
    while step > tolerance and rounds > 0:
        rounds -= 1
        better = False
        for bone, axis in unknowns:
            for sign in (1, -1):
                pose[bone]["rotation"][axis] += sign * step
                e = error(world())
                if e < best - 1e-9:
                    best, better = e, True
                    break
                pose[bone]["rotation"][axis] -= sign * step
        if not better:
            step /= 2
    return pose


def lowest(rig, world, contacts):
    """The lowest of the contact points ((bone, point) pairs) in this pose, y."""
    return min(Rig.at(world, bone, p)[1] for bone, p in contacts)


def settle(rig, clip, contacts, step=0.05, ground=0.0, only_up=False, bone="root", after=None):
    """The clip set on the ground: sampled every `step` seconds, and at each, `bone` (the whole body, by default) moved
    up or down until its lowest contact point is at `ground` (only up, with `only_up`: nothing sinks, but nothing is
    pulled down either, so a leap still leaves the ground). Its other channels are sampled at the same times, so the
    clip comes back as dense keyframes, each linear to the next. `after(pose)`, if given, then adjusts each sample's
    pose (what is held, say, made to give way to the ground rather than lifting the body)."""
    length = clip["animation_length"]
    n = max(1, round(length / step))
    times = [round(i * length / n, 4) for i in range(n + 1)]
    poses = [sample(clip, t) for t in times]
    for pose in poses:
        base = pose.setdefault(bone, {}).get("position", [0.0, 0.0, 0.0])
        pose[bone]["position"] = list(base)
        drop = lowest(rig, rig.world(pose), contacts) - ground
        if not (only_up and drop >= 0):
            pose[bone]["position"][1] -= drop
    looping = clip.get("loop") is True
    if after is not None:
        if looping:
            # A loop's adjustments carry over from where it ends to where it begins: once round first, so it starts as
            # it will end.
            import copy
            for pose in poses:
                after(copy.deepcopy(pose))
        for pose in poses:
            after(pose)
    if looping:
        import copy
        poses[-1] = copy.deepcopy(poses[0])                             # it ends exactly where it begins
    bones = {}
    for name in set(clip["bones"]) | {bone} | {n for p in poses for n in p}:
        chans = {}
        for k in ("rotation", "position", "scale"):
            if not any(k in p.get(name, {}) for p in poses):
                continue
            chans[k] = {f"{t}": {"post": [round(v, 3) for v in p.get(name, {}).get(k, [0, 0, 0] if k != "scale" else [1, 1, 1])],
                                 "lerp_mode": "linear"} for t, p in zip(times, poses)}
        bones[name] = chans
    out = dict(clip)
    out["bones"] = bones
    return out


def report(rig, clip, contacts, step=0.1):
    """How high the lowest contact point is at each sample (for checking a clip is on the ground)."""
    length = clip["animation_length"]
    n = max(1, round(length / step))
    return [(round(i * length / n, 2), round(lowest(rig, rig.world(sample(clip, i * length / n)), contacts), 2)) for i in range(n + 1)]


# ---- Seamless loops ----

import re as _re

_TIME = _re.compile(r"q\.anim_time((?:\s*\*\s*[0-9.]+)+)")


def seamless(clip):
    """A looping clip made to join up where it loops: every `q.anim_time * k` in its Molang (chained factors included,
    `q.anim_time * 225 * 0.5`) set to the nearest rate that turns a whole number of cycles (at least one) over the
    clip's length. Without it, a walk written at one cycle a second in a clip 0.8 s long jumps each time it loops."""
    if clip.get("loop") is not True:
        return clip
    length = float(clip["animation_length"])

    def fix(m):
        k = 1.0
        for f in _re.findall(r"[0-9.]+", m.group(1)):
            k *= float(f)
        cycles = max(1, round(k * length / 360.0))
        return f"q.anim_time * {round(cycles * 360.0 / length, 4)}"

    def walk(v):
        if isinstance(v, str):
            return _TIME.sub(fix, v)
        if isinstance(v, list):
            return [walk(x) for x in v]
        if isinstance(v, dict):
            return {k: walk(x) for k, x in v.items()}
        return v
    clip["bones"] = walk(clip["bones"])
    return clip
