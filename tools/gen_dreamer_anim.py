"""Writes the dreamer's animations for CoolCatLib's Armature system: the clips the vanilla player model plays as they
fall into the Lucid Deep, lie where they landed and get up again.

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
    get_up   once, held at its end: hands drawn in under the shoulders, the chest pushed up off the ground with the
             hips still down, one knee brought forward, up on to that foot off the other knee, and straightening
"""
import json
import math
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "whycantisleep", "armatures", "dreamer.animation.json")

# DreamTransition.RISE_TICKS: how long getting up takes.
RISE_SECONDS = 48 / 20.0
# Face down on the ground: tipped forward flat, and lowered from the body's middle to lying height.
PRONE = [-90.0, 0.0, 0.0]
PRONE_DROP = [0.0, -10.0, 0.0]


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
    bones = {name: {"rotation": list(rot)} for name, rot in SPRAWL.items()}
    bones["root"]["position"] = list(PRONE_DROP)
    bones["right_arm"]["rotation"][0] = f"{SPRAWL['right_arm'][0]} + 17.2 * {TWITCH}"
    bones["right_arm"]["bend"] = [f"15 + 25 * {TWITCH}", 0]
    bones["left_arm"]["bend"] = [15, 0]
    bones["right_leg"]["bend"] = [8, 180]
    bones["left_leg"]["bend"] = [12, 180]
    return {"loop": True, "animation_length": 120.0, "bones": bones}


# ---- Getting up: key poses, smoothed between with Catmull-Rom. ----
# Each pose: (time in seconds, {joint: {channel: value}}). Channels: r rotation, p position, b bend [degrees, direction].
KNEE = 180
ELBOW = 0
POSES = [
    (0.0, {  # where they lay: flat on their face, arms flung out
        "root": {"r": SPRAWL["root"], "p": PRONE_DROP},
        "right_arm": {"r": SPRAWL["right_arm"], "b": [15, ELBOW]}, "left_arm": {"r": SPRAWL["left_arm"], "b": [15, ELBOW]},
        "right_leg": {"r": SPRAWL["right_leg"], "b": [8, KNEE]}, "left_leg": {"r": SPRAWL["left_leg"], "b": [12, KNEE]},
        "head": {"r": SPRAWL["head"]},
    }),
    (0.35, {  # hands drawn in under the shoulders, elbows bent up, head lifting off the ground
        "root": {"r": [-90.0, 0.0, 0.0], "p": PRONE_DROP},
        "right_arm": {"r": [-62.0, 0.0, 14.0], "b": [85, ELBOW]}, "left_arm": {"r": [-62.0, 0.0, -14.0], "b": [85, ELBOW]},
        "right_leg": {"r": [0.0, 0.0, 5.0], "b": [5, KNEE]}, "left_leg": {"r": [0.0, 0.0, -5.0], "b": [5, KNEE]},
        "head": {"r": [-25.0, 15.0, 0.0]},
    }),
    (0.8, {  # arms straightened: the chest pushed up off the ground, hips and legs still down, looking ahead
        "root": {"r": [-58.0, 0.0, 0.0], "p": [0.0, -8.5, 0.0]},
        "right_arm": {"r": [-92.0, 0.0, 9.0], "b": [12, ELBOW]}, "left_arm": {"r": [-92.0, 0.0, -9.0], "b": [12, ELBOW]},
        "right_leg": {"r": [30.0, 0.0, 4.0], "b": [10, KNEE]}, "left_leg": {"r": [30.0, 0.0, -4.0], "b": [10, KNEE]},
        "head": {"r": [-40.0, 0.0, 0.0]},
    }),
    (1.2, {  # the right knee drawn up under the chest, the left still trailing, weight on the hands
        "root": {"r": [-40.0, 0.0, -3.0], "p": [0.0, -8.0, 0.0]},
        "right_arm": {"r": [-80.0, 0.0, 8.0], "b": [18, ELBOW]}, "left_arm": {"r": [-84.0, 0.0, -8.0], "b": [15, ELBOW]},
        "right_leg": {"r": [-55.0, 0.0, 6.0], "b": [95, KNEE]}, "left_leg": {"r": [35.0, 0.0, -4.0], "b": [40, KNEE]},
        "head": {"r": [-22.0, 0.0, 0.0]},
    }),
    (1.6, {  # kneeling on the left knee, the right foot planted, a hand pressed on the right knee, head hung
        "root": {"r": [-22.0, 0.0, -2.0], "p": [0.0, -6.5, 0.0]},
        "right_arm": {"r": [-58.0, 0.0, 6.0], "b": [35, ELBOW]}, "left_arm": {"r": [-28.0, 0.0, -10.0], "b": [10, ELBOW]},
        "right_leg": {"r": [-78.0, 0.0, 5.0], "b": [100, KNEE]}, "left_leg": {"r": [8.0, 0.0, -3.0], "b": [92, KNEE]},
        "head": {"r": [14.0, 0.0, 0.0]},
    }),
    (2.0, {  # pushing up off the knee: both legs straightening, the body coming up over them
        "root": {"r": [-12.0, 0.0, -1.0], "p": [0.0, -2.5, 0.0]},
        "right_arm": {"r": [-38.0, 0.0, 6.0], "b": [25, ELBOW]}, "left_arm": {"r": [-12.0, 0.0, -7.0], "b": [8, ELBOW]},
        "right_leg": {"r": [-36.0, 0.0, 3.0], "b": [48, KNEE]}, "left_leg": {"r": [-6.0, 0.0, -2.0], "b": [40, KNEE]},
        "head": {"r": [8.0, 0.0, 0.0]},
    }),
    (RISE_SECONDS, {  # standing, a little unsteady still
        "root": {"r": [0.0, 0.0, 0.0], "p": [0.0, 0.0, 0.0]},
        "right_arm": {"r": [0.0, 0.0, 4.0], "b": [5, ELBOW]}, "left_arm": {"r": [0.0, 0.0, -4.0], "b": [5, ELBOW]},
        "right_leg": {"r": [0.0, 0.0, 1.0], "b": [0, KNEE]}, "left_leg": {"r": [0.0, 0.0, -1.0], "b": [0, KNEE]},
        "head": {"r": [0.0, 0.0, 0.0]},
    }),
]
CHANNELS = {"r": "rotation", "p": "position", "b": "bend"}


def get_up():
    bones = {}
    for time, pose in POSES:
        for joint, channels in pose.items():
            for key, value in channels.items():
                track = bones.setdefault(joint, {}).setdefault(CHANNELS[key], {})
                track[f"{round(time, 4)}"] = {"post": list(value), "lerp_mode": "catmullrom"}
    return {"loop": "hold_on_last_frame", "animation_length": RISE_SECONDS, "bones": bones}


def main():
    data = {"format_version": "1.8.0", "animations": {
        "animation.dreamer.fall": fall(),
        "animation.dreamer.sprawl": sprawl(),
        "animation.dreamer.get_up": get_up(),
    }}
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")
    print("wrote", os.path.relpath(OUT, ROOT))


if __name__ == "__main__":
    main()
