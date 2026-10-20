"""Writes the dreamer's animations for CoolCatLib's Armature system: the clips the vanilla player model plays as they
fall into the Dark Between, lie where they landed and get up again.

    python tools/gen_dreamer_anim.py

It writes assets/whycantisleep/armatures/dreamer.animation.json, a Bedrock animation file with no model of its own: its
bones are named after the vanilla player's parts, so CoolCatLib plays it on the player as it is
(VanillaModels.player().animations(...) in DreamerAnimations). Values are Bedrock's: rotations in degrees, positions in
pixels. On a limb, a negative x swings it forward and a positive z takes the right one out from the body (the left one
in). `root` is the whole body, turned about its middle (12 px up): a negative x tips it forward on to its face, and a
negative y position lowers it. `bend` curves a limb at its middle: [degrees, direction], direction 0 bending it forward
(an elbow), 180 back (a knee).

The whole of each pose is the clip's: the player stays in the game's standing pose throughout, so nothing of the game's
own swimming or crouching turns the body as a stiff plank.

    fall     looping: face down in the air, spread out like a skydiver, buffeted by the rush of air
    sprawl   looping: flat on their face where they hit, arms flung out, head on its side, a hand twitching
    get_up   once, held at its end: the arms dragged in, palms planted beside the chest, the chest pushed up with the
             hips still down, the hips drawn back on to the knees, the right foot brought forward, a hand on that knee,
             a push up off the left knee with the weight thrown over the right foot, the left foot stepping in, and
             straightening
    heart_strike  once: a critical strike driven into a staggered boss's heart (BossCombat, the "combat" layer)

Lying and getting up rest on the ground: the generator models the player's parts and, at every step, lowers the body
until its lowest part touches the ground and turns the limbs bearing weight until they reach it.

    python tools/gen_dreamer_anim.py --report     also prints how high each part is off the ground as they get up
"""
import json
import math
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "whycantisleep", "armatures", "dreamer.animation.json")

# DreamTransition.RISE_TICKS: how long getting up takes.
RISE_SECONDS = 56 / 20.0


def deg(rad):
    return round(math.degrees(rad), 2)


# ---- Falling: in Molang, so the flail never repeats. In degrees; t is the clip's time in seconds. ----
T = "q.anim_time"
RUSH = f"(3.44 * math.sin({T} * 3094) + 2.29 * math.sin({T} * 4698 + 57.3))"
HEAVE_R = f"(34.4 * math.max(0, math.sin({T} * 378)))"
HEAVE_L = f"(34.4 * math.max(0, math.sin({T} * 332 + 114.6)))"


def fall():
    return {
        "loop": True,
        "animation_length": 120.0,
        "bones": {
            # Face down, rocked by the air: a slow roll and pitch on the whole body.
            "root": {"rotation": [f"-90 + 0.6 * {RUSH} + 4 * math.sin({T} * 150)", 0, f"6 * math.sin({T} * 115 + 40)"]},
            "right_arm": {"rotation": [f"20 + 2 * {RUSH} - {HEAVE_R}", 0, f"77.3 + {RUSH} - 0.5 * {HEAVE_R}"], "bend": [f"10 + 0.5 * {HEAVE_R}", 0]},
            "left_arm": {"rotation": [f"20 - 2 * {RUSH} - {HEAVE_L}", 0, f"-77.3 - {RUSH} + 0.5 * {HEAVE_L}"], "bend": [f"10 + 0.5 * {HEAVE_L}", 0]},
            "right_leg": {"rotation": [f"20 + 17.2 * math.sin({T} * 688)", 0, f"17.2 + {RUSH}"], "bend": [f"20 + 15 * math.sin({T} * 688)", 180]},
            "left_leg": {"rotation": [f"20 + 17.2 * math.sin({T} * 688 + 137.5)", 0, f"-17.2 - {RUSH}"], "bend": [f"20 + 15 * math.sin({T} * 688 + 137.5)", 180]},
            "head": {"rotation": [f"-37.2 + {RUSH}", f"14.3 * math.sin({T} * 229)", 0]},
        },
    }


# ---- Sprawled ----
SPRAWL = {
    "root": [-90.0, 0.0, 3.0],
    "right_arm": [deg(-0.15), 0.0, deg(1.4)],
    "left_arm": [deg(0.1), 0.0, deg(-1.2)],
    "right_leg": [deg(0.05), 0.0, deg(0.32)],
    "left_leg": [deg(-0.08), 0.0, deg(-0.24)],
    "head": [0.0, deg(1.0), 0.0],
}
TWITCH = f"(3 * math.max(0, math.sin({T} * 1948) - 0.92))"


def sprawl():
    # As written above, but resting on the ground: the arms turned until they lie on it, the body lowered on to it.
    pose, drop = lying()
    bones = {name: {"rotation": list(rot)} for name, rot in SPRAWL.items()}
    bones["root"]["position"] = [0.0, round(drop, 2), 0.0]
    for name in ("left_arm", "right_leg", "left_leg"):
        bones[name]["rotation"][0] = round(pose[name][0], 2)
    bones["right_arm"]["rotation"][0] = f"{round(pose['right_arm'][0], 2)} + 17.2 * {TWITCH}"
    bones["right_arm"]["bend"] = [f"15 + 25 * {TWITCH}", 0]
    for name in ("left_arm", "right_leg", "left_leg"):
        bones[name]["bend"] = [pose[name][2], LIMBS[name]["bend"]]
    return {"loop": True, "animation_length": 120.0, "bones": bones}


# ---- Getting up: key poses, then the body set on the ground at every step between them. ----
# The keys say how each part is turned. The clip is sampled finely from them (Catmull-Rom), and at every sample the
# body is lowered or raised until its lowest point rests on the ground, so it is always held up by what touches the
# ground and never drifts up or sinks between keys. A limb bearing weight in two keys in a row (hands under the
# shoulders, a knee down) is turned at every sample between them so it still reaches the ground: it stays planted
# rather than hovering while the body moves over it. Nothing moves the body forward or back: it gets up over the spot
# it lay on.
#
# The model, in pixels from the hips (where the root turns), forward / up / to their right. Each limb: where it hangs
# from, how far it runs along itself from there, how far it reaches to each side of that line, its bend's stretch (the
# middle third, as CoolCatLib bends it) and which way that bends.
KNEE = 180
ELBOW = 0
LIMBS = {
    "right_arm": {"pivot": (0.0, 10.0, 5.0), "along": (-2.0, 10.0), "side": (-1.0, 3.0), "zone": (2.0, 6.0), "bend": ELBOW},
    "left_arm": {"pivot": (0.0, 10.0, -5.0), "along": (-2.0, 10.0), "side": (-3.0, 1.0), "zone": (2.0, 6.0), "bend": ELBOW},
    "right_leg": {"pivot": (0.0, 0.0, 1.9), "along": (0.0, 12.0), "side": (-2.0, 2.0), "zone": (4.0, 8.0), "bend": KNEE},
    "left_leg": {"pivot": (0.0, 0.0, -1.9), "along": (0.0, 12.0), "side": (-2.0, 2.0), "zone": (4.0, 8.0), "bend": KNEE},
}
HIPS = 12.0
TORSO = [(f, u, s) for f in (-2.0, 2.0) for u in (0.0, 12.0) for s in (-4.0, 4.0)]
HEAD = [(f, u, s) for f in (-4.0, 4.0) for u in (0.0, 8.0) for s in (-4.0, 4.0)]
STEP = 0.025


def pitch(v, degrees):
    # Tips the top forward (the root's -x, a limb's +x).
    a = math.radians(degrees)
    f, u, s = v
    return (f * math.cos(a) + u * math.sin(a), -f * math.sin(a) + u * math.cos(a), s)


def roll(v, degrees):
    # Takes the bottom out to the right (a limb's +z).
    a = math.radians(degrees)
    f, u, s = v
    return (f, u * math.cos(a) + s * math.sin(a), s * math.cos(a) - u * math.sin(a))


def yaw(v, degrees):
    a = math.radians(degrees)
    f, u, s = v
    return (f * math.cos(a) - s * math.sin(a), u, f * math.sin(a) + s * math.cos(a))


def bent(along, across, side, amount, zone, direction):
    # A point of the straight limb where its bend puts it (CoolCatLib's LimbBend.bendPoint): an arc through the zone,
    # the rest beyond it turned whole. `across` is toward the limb's front.
    a, b = zone
    toward = 1.0 if direction == ELBOW else -1.0
    s = across * toward
    angle = math.radians(amount)
    if along <= a or abs(angle) < 1e-6:
        return (across, -along, side)
    radius = (b - a) / angle
    if along < b:
        turn = angle * (along - a) / (b - a)
        y, out = a + (radius - s) * math.sin(turn), radius - (radius - s) * math.cos(turn)
    else:
        y = a + (radius - s) * math.sin(angle) + (along - b) * math.cos(angle)
        out = radius - (radius - s) * math.cos(angle) + (along - b) * math.sin(angle)
    return (out * toward, -y, side)


def limb_points(name, pose):
    limb = LIMBS[name]
    x, z, amount = pose[name]
    start, end = limb["along"]
    a, b = limb["zone"]
    stations = sorted({start, a, a + (b - a) * 0.25, (a + b) * 0.5, a + (b - a) * 0.75, b, end})
    points = []
    for along in stations:
        for across in (-2.0, 2.0):
            for side in limb["side"]:
                local = bent(along, across, side, amount, limb["zone"], limb["bend"])
                p = roll(pitch(local, x), z)
                points.append(tuple(p[i] + limb["pivot"][i] for i in range(3)))
    return points


def to_world(points, pose, drop):
    tilt, lean = pose["root"]
    out = []
    for p in points:
        f, u, s = roll(pitch(p, tilt), lean)
        out.append((f, HIPS + drop + u, s))
    return out


def lowest(part, pose, drop=0.0):
    if part == "torso":
        points = TORSO
    elif part == "head":
        hx, hy = pose["head"]
        points = [tuple(q[i] + (0.0, 12.0, 0.0)[i] for i in range(3)) for q in (yaw(pitch(p, hx), hy) for p in HEAD)]
    else:
        points = limb_points(part, pose)
    return min(p[1] for p in to_world(points, pose, drop))


def reach(name, pose, drop, spread=40.0):
    # The turn about its shoulder or hip, nearest the one asked for, at which a limb just reaches the ground; or, when
    # none does, the one that comes closest. Returns (x, how far above the ground it is left: below it if negative).
    x0, z, amount = pose[name]

    def gap(x):
        trial = dict(pose)
        trial[name] = (x, z, amount)
        return lowest(name, trial, drop)

    best, closest = None, (abs(gap(x0)), x0)
    x, previous = x0 - spread, gap(x0 - spread)
    while x < x0 + spread:
        nxt = x + 0.5
        value = gap(nxt)
        closest = min(closest, (abs(value), nxt))
        if (previous <= 0.0) != (value <= 0.0):
            lo, hi = x, nxt
            for _ in range(30):
                mid = (lo + hi) * 0.5
                if (gap(lo) <= 0.0) == (gap(mid) <= 0.0):
                    lo = mid
                else:
                    hi = mid
            root = (lo + hi) * 0.5
            if best is None or abs(root - x0) < abs(best - x0):
                best = root
        x, previous = nxt, value
    x = best if best is not None else closest[1]
    return x, gap(x)


def settle(pose, planted):
    # Rests the body on whatever of it is lowest, then turns each planted limb until it just reaches the ground. A
    # planted limb that can't help going into the ground (a straight arm under a low shoulder) holds the body up off it
    # instead. Returns the drop: the root's y position.
    drop = -min(lowest(part, pose) for part in ["torso", *LIMBS] if part not in planted)
    for _ in range(4):
        sunk = 0.0
        for name in planted:
            x, left = reach(name, pose, drop)
            pose[name] = (x,) + tuple(pose[name][1:])
            sunk = min(sunk, left)
        if sunk > -0.01:
            break
        drop -= sunk
    return drop


def key(time, tilt, lean=0.0, head=(0.0, 0.0), planted=(), **limbs):
    """One pose. `tilt`: how far the body leans forward from upright (90 face down), `lean` its roll. Each limb:
    (pitch, z, bend), its pitch where it points in the world, not on the body: 0 straight down, -90 straight out in
    front, 90 straight out behind. `head`: (x, y) on the body. `planted`: the limbs bearing weight on the ground."""
    pose = {"root": (tilt, lean), "head": head}
    for name, (world, z, amount) in limbs.items():
        pose[name] = (world - tilt, z, amount)
    return {"time": time, "pose": pose, "planted": set(planted)}


def sprawled(name, amount):
    x, _, z = SPRAWL[name]
    return (x + 90.0, z, amount)


KEYS = [
    # Where they lay: flat on their face, arms flung out along the ground (the sprawl).
    key(0.0, 90.0, lean=3.0, head=(SPRAWL["head"][0], SPRAWL["head"][1]), planted=tuple(LIMBS),
        right_arm=sprawled("right_arm", 15), left_arm=sprawled("left_arm", 8),
        right_leg=sprawled("right_leg", 8), left_leg=sprawled("left_leg", 12)),
    # Stirring: the head comes off its side, the arms start to drag in along the ground.
    key(0.45, 90.0, lean=2.0, head=(-10.0, 28.0), planted=tuple(LIMBS),
        right_arm=(92.0, 52.0, 30), left_arm=(92.0, -48.0, 26),
        right_leg=(92.0, 14.0, 8), left_leg=(90.0, -11.0, 10)),
    # Palms flat beside the chest, elbows up and back, the head lifting: bracing to push.
    key(0.85, 88.0, head=(-26.0, 0.0), planted=("right_arm", "left_arm"),
        right_arm=(100.0, 12.0, 95), left_arm=(100.0, -12.0, 95),
        right_leg=(90.0, 6.0, 6), left_leg=(90.0, -6.0, 6)),
    # The chest pushed up off the ground on straightening arms, the hips and legs still down.
    key(1.2, 52.0, head=(-26.0, 0.0), planted=("right_arm", "left_arm"),
        right_arm=(-12.0, 9.0, 30), left_arm=(-12.0, -9.0, 30),
        right_leg=(90.0, 4.0, 10), left_leg=(90.0, -4.0, 10)),
    # Hips drawn back and up on to the knees: on all fours, head hanging.
    key(1.62, 84.0, head=(-22.0, 0.0), planted=("right_arm", "left_arm"),
        right_arm=(0.0, 6.0, 10), left_arm=(0.0, -6.0, 10),
        right_leg=(-4.0, 3.0, 94), left_leg=(-4.0, -3.0, 94)),
    # Weight rocked back on to the left knee and the hands, the right knee lifted and coming forward.
    key(1.86, 82.0, head=(-22.0, 0.0), planted=("right_arm", "left_arm"),
        right_arm=(2.0, 6.0, 12), left_arm=(0.0, -6.0, 10),
        right_leg=(-88.0, 4.0, 130), left_leg=(-2.0, -3.0, 94)),
    # The right foot planted in front: kneeling on the left knee, a hand on the right knee, the other just leaving
    # the ground.
    key(2.1, 42.0, head=(-12.0, 0.0), planted=("left_leg",),
        right_arm=(5.0, 8.0, 10), left_arm=(-14.0, -8.0, 16),
        right_leg=(-60.0, 3.0, 100), left_leg=(12.0, -3.0, 96)),
    # Pushing up: the weight thrown forward over the right foot, the hand shoving down on the knee, the left leg
    # straightening behind on its toes.
    key(2.38, 36.0, head=(-6.0, 0.0), planted=("left_leg",),
        right_arm=(10.0, 8.0, 8), left_arm=(-12.0, -10.0, 16),
        right_leg=(-25.0, 2.0, 45), left_leg=(22.0, -2.0, 34)),
    # Up on the right leg, the left foot stepping in to join it, the body straightening.
    key(2.6, 10.0, head=(4.0, 0.0),
        right_arm=(-4.0, 7.0, 12), left_arm=(6.0, -8.0, 10),
        right_leg=(-8.0, 1.5, 16), left_leg=(-30.0, -1.5, 70)),
    # Standing, a little unsteady still.
    key(RISE_SECONDS, 0.0, head=(0.0, 0.0),
        right_arm=(0.0, 4.0, 5), left_arm=(0.0, -4.0, 5),
        right_leg=(0.0, 1.0, 0), left_leg=(0.0, -1.0, 0)),
]
SETTLED = []


def settle_keys():
    if not SETTLED:
        for k in KEYS:
            k["drop"] = settle(k["pose"], k["planted"])
        SETTLED.append(True)


def lying():
    settle_keys()
    return KEYS[0]["pose"], KEYS[0]["drop"]


def catmull_rom(t, p0, p1, p2, p3):
    return 0.5 * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (-p0 + 3 * p1 - 3 * p2 + p3) * t ** 3)


def sample(time):
    # The pose at a time, smoothed between the keys, and the limbs planted in the keys on both sides of it.
    i = max(j for j in range(len(KEYS)) if KEYS[j]["time"] <= time + 1e-9)
    if i == len(KEYS) - 1:
        return {k: v for k, v in KEYS[i]["pose"].items()}, set()
    a, b = KEYS[i], KEYS[i + 1]
    before, after = KEYS[max(i - 1, 0)], KEYS[min(i + 2, len(KEYS) - 1)]
    t = (time - a["time"]) / (b["time"] - a["time"])
    pose = {}
    for name in a["pose"]:
        values = [before["pose"][name], a["pose"][name], b["pose"][name], after["pose"][name]]
        pose[name] = tuple(catmull_rom(t, *(v[axis] for v in values)) for axis in range(len(values[0])))
    return pose, a["planted"] & b["planted"]


def get_up(report=False):
    settle_keys()
    bones = {}
    steps = int(round(RISE_SECONDS / STEP))
    for n in range(steps + 1):
        time = n * STEP
        pose, planted = sample(time)
        drop = settle(pose, planted)
        at = f"{round(time, 4)}"
        tilt, lean = pose["root"]
        bones.setdefault("root", {"rotation": {}, "position": {}})
        bones["root"]["rotation"][at] = [round(-tilt, 2), 0.0, round(lean, 2)]
        bones["root"]["position"][at] = [0.0, round(drop, 2), 0.0]
        bones.setdefault("head", {"rotation": {}})["rotation"][at] = [round(pose["head"][0], 2), round(pose["head"][1], 2), 0.0]
        for name in LIMBS:
            x, z, amount = pose[name]
            bone = bones.setdefault(name, {"rotation": {}, "bend": {}})
            bone["rotation"][at] = [round(x, 2), 0.0, round(z, 2)]
            bone["bend"][at] = [round(amount, 2), LIMBS[name]["bend"]]
        if report and n % 4 == 0:
            heights = " ".join(f"{part[:1]}{part.split('_')[-1][:1] if '_' in part else ''}={lowest(part, pose, drop):5.1f}"
                               for part in ["torso", "head", *LIMBS])
            print(f"{time:5.2f}s drop={drop:6.2f} tilt={tilt:5.1f} {heights} planted={sorted(planted)}")
    return {"loop": "hold_on_last_frame", "animation_length": RISE_SECONDS, "bones": bones}


def heart_strike():
    """A critical strike on a boss's heart (BossCombat, played on the striker's "combat" layer): the right arm drawn back
    with the weight on the back foot, then a step in and a thrust driven deep (the body leaning into it, the left arm
    bracing), held and twisted, and ripped back out."""
    def keys(*pairs):
        return {f"{t}": {"post": list(v), "lerp_mode": "catmullrom"} for t, v in pairs}
    return {"loop": False, "animation_length": 1.1, "bones": {
        "root": {"rotation": keys((0.0, [0, 0, 0]), (0.22, [5, 8, 0]), (0.42, [-14, -6, 0]), (0.75, [-12, -4, 0]), (0.95, [4, 4, 0]), (1.1, [0, 0, 0]))},
        "right_arm": {"rotation": keys((0.0, [0, 0, 0]), (0.22, [38, 0, 18]), (0.42, [-96, 0, -6]), (0.6, [-92, 12, -6]),
                                       (0.75, [-92, 24, -6]), (0.95, [-36, 0, 26]), (1.1, [0, 0, 0])),
                      "bend": keys((0.0, [0, 0]), (0.22, [70, 0]), (0.42, [0, 0]), (0.75, [6, 0]), (0.95, [55, 0]), (1.1, [0, 0]))},
        "left_arm": {"rotation": keys((0.0, [0, 0, 0]), (0.22, [-30, 0, -10]), (0.42, [-24, 0, -22]), (0.95, [-10, 0, -10]), (1.1, [0, 0, 0])),
                     "bend": keys((0.0, [0, 0]), (0.42, [40, 0]), (1.1, [0, 0]))},
        "right_leg": {"rotation": keys((0.0, [0, 0, 0]), (0.22, [8, 0, 0]), (0.42, [-26, 0, 0]), (0.95, [-10, 0, 0]), (1.1, [0, 0, 0]))},
        "left_leg": {"rotation": keys((0.0, [0, 0, 0]), (0.22, [-6, 0, 0]), (0.42, [18, 0, 0]), (0.95, [6, 0, 0]), (1.1, [0, 0, 0]))},
        "head": {"rotation": keys((0.0, [0, 0, 0]), (0.42, [10, 0, 0]), (0.75, [12, 0, 0]), (1.1, [0, 0, 0]))},
    }}


def main():
    data = {"format_version": "1.8.0", "animations": {
        "animation.dreamer.fall": fall(),
        "animation.dreamer.sprawl": sprawl(),
        "animation.dreamer.get_up": get_up("--report" in sys.argv),
        "animation.dreamer.heart_strike": heart_strike(),
    }}
    import rig_pose
    for clip in data["animations"].values():
        rig_pose.seamless(clip)                                         # every loop joins up where it loops
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")
    print("wrote", os.path.relpath(OUT, ROOT))


if __name__ == "__main__":
    main()
