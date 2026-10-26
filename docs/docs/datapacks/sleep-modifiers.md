# Sleep modifiers

Every night you sleep is scored from 0 to 100 as you wake. The score starts at 75, and each **sleep modifier** that has
a say about the night adds points to it or takes them away; the sum is held to 0 to 100. The morning card lists the
modifiers that counted, each with its points. How this plays is on [Nights](../playing/nights.md).

The mod's own fifteen modifiers are data files, and a data pack can add more, change them, or take them out. Each is a
file at `data/<namespace>/whycantisleep/sleep_modifier/<name>.json`, the same on every Minecraft version.

```json title="data/mypack/whycantisleep/sleep_modifier/rainy_night.json"
{
  "label": "Rain on the Roof",
  "points": 5,
  "when": {
    "enclosed": true,
    "weather": "rain"
  }
}
```

## Keys

| Key | Type | Default | What it does |
| --- | --- | --- | --- |
| `label` | string | `""` | The name shown on the morning card: a translation key, or plain text. Write it in Title Case, as the mod's own are. |
| `points` | integer, -100 to 100 | `0` | Fixed points, given whenever the conditions hold. Above 0 it is a buff, below a debuff. |
| `per` | string | none | Also gives points for each one of something: `monsters`, `soothing`, `unsettling`, `pillow_light` or `near_blocks`. |
| `each` | integer, -100 to 100 | `0` | The points for each one counted by `per`. |
| `most` | integer, -100 to 100 | none | Holds the total to this: at most `most` when it is 0 or above, at least `most` when it is below 0. |
| `when` | object | always | The [conditions](#conditions). Every condition given must hold, or the modifier gives nothing. |
| `disabled` | boolean | `false` | `true` takes the modifier with this file's id out. See [Replacing and turning off](#replacing-and-turning-off). |

A modifier's points are `points`, plus `each` times the count of `per` when `per` is given, held to `most`. A modifier
whose points come to 0 for a night isn't shown on the card for it.

### What `per` counts

| `per` | Counts |
| --- | --- |
| `monsters` | The most monsters that were within 24 blocks of the bed at any one time in the night. |
| `soothing` | Blocks within 6 of the bed (3 up and down) whose item is in `#whycantisleep:soothing`. See [tags](index.md#item-tags-things-by-the-bed). |
| `unsettling` | The same for `#whycantisleep:unsettling`. |
| `pillow_light` | The block light at the pillow, 0 to 15. |
| `near_blocks` | The blocks matched by this modifier's own [`near_blocks`](#near_blocks) condition. Without one, the count is 0. |

## Conditions

All keys of `when` are optional. A **range** is an object with `min` and `max`, both optional and both included:
`{ "min": 1 }` is "1 or more", `{ "max": 0 }` is "none".

| Key | Type | Holds when |
| --- | --- | --- |
| `enclosed` | boolean | The room was closed (`true`) or not (`false`). See [The room](#the-room). |
| `lit` | boolean | The room was lit all through, with no dark corner (`true`), or not (`false`). |
| `pillow_light` | range | The block light at the pillow, 0 to 15, is in the range. |
| `monsters` | range | The most monsters within 24 blocks of the bed at once is in the range. |
| `soothing` | range | The count of soothing things near the bed is in the range. |
| `unsettling` | range | The count of unsettling things near the bed is in the range. |
| `calm_music` | boolean | Calm music played near the bed during the night, or didn't. |
| `creepy_music` | boolean | Creepy music did, or didn't. |
| `loud_music` | boolean | Loud music did, or didn't. |
| `trouble` | list of strings | How the player had been sleeping before they lay down is one of these: `rested`, `restless`, `sleepless`. |
| `nightmare` | boolean | The player had a night terror, or didn't. |
| `reason` | list of strings | Why they woke is one of these: `morning`, `got_up`, `hurt`, `bed_gone`, `other`. |
| `dimension` | dimension id | The bed is in this dimension, such as `"minecraft:overworld"`. |
| `biome` | biome id or `#tag` | The bed is in this biome, or a biome of this tag: `"minecraft:cherry_grove"`, `"#minecraft:is_forest"`. |
| `weather` | string | The weather as they wake: `clear`, `rain` or `thunder`. |
| `near_blocks` | object | Enough of some blocks are near the bed. See [`near_blocks`](#near_blocks). |

Music is heard from jukeboxes within 16 blocks, and the music tags decide which is which (see
[Music tags](index.md#music-tags)). Sleep trouble is `restless` from 2 refused sleeps in a row or 2 days without sleep,
and `sleepless` from 5 or 3 days. The wake reasons: `morning` is a night slept through; `got_up` is leaving the bed
early; `hurt` is being woken by damage; `bed_gone` is the bed being broken; `other` is anything else that woke them.

### near_blocks

| Key | Type | Default | What it does |
| --- | --- | --- | --- |
| `blocks` | list of strings | required | Block ids and `#tags`: `["minecraft:campfire", "#minecraft:candles"]`. |
| `range` | integer, 1 to 12 | `6` | How far from the bed to look, to each side. Up and down it looks as far, but never more than 4. |
| `count` | range | `{ "min": 1 }` | How many there must be. |

Each block counts once, so a candle block with four candles is one. With `"per": "near_blocks"`, the count is also what
`each` is multiplied by.

```json title="data/mypack/whycantisleep/sleep_modifier/crackling_fire.json"
{
  "label": "Crackling Fire",
  "per": "near_blocks",
  "each": 3,
  "most": 6,
  "when": {
    "near_blocks": {
      "blocks": ["minecraft:campfire", "minecraft:fire"],
      "range": 8
    },
    "dimension": "minecraft:overworld"
  }
}
```

### The room

When you lie down, the mod fills the air round the bed to find the room you sleep in. Air is anything you could walk
through, and doors, trapdoors and fence gates that are open; closed ones are walls. The room is **closed** (`enclosed`)
when it stays under 600 blocks of air, never strays more than 24 blocks from the pillow (counted along each axis, about
12 in a straight line), and never reaches the open sky. A closed room is **lit** when no block of air in it has a block
light of 0, so no monster could spawn in it; a room that isn't closed is never lit. The pillow light is the block light
just above the bed's head.

The room, the pillow light and the things near the bed are read as you lie down. Monsters and music are watched all
through the night, and the weather and the reason are read as you wake.

## The mod's own modifiers

They are in `data/whycantisleep/whycantisleep/sleep_modifier/`.

| Id | Label | Points | When |
| --- | --- | --- | --- |
| `whycantisleep:enclosed` | Behind Closed Doors | +10 | The room is closed. |
| `whycantisleep:exposed` | Out in the Open | -5 | The room isn't closed. |
| `whycantisleep:dark_corners` | Unlit Corners | -10 | The room is closed but not lit all through. |
| `whycantisleep:too_bright` | Light in Your Eyes | -5 | The light at the pillow is 13 or more. |
| `whycantisleep:monsters` | Prowling Monsters | -6 each, down to -30 | Monsters within 24 blocks. |
| `whycantisleep:soothing` | Calming Things Nearby | +2 each, up to +10 | Soothing things near the bed. |
| `whycantisleep:unsettling` | Creepy Things Nearby | -3 each, down to -15 | Unsettling things near the bed. |
| `whycantisleep:calm_music` | Soothing Melody | +10 | Calm music, and no creepy or loud music. |
| `whycantisleep:creepy_music` | Eerie Music | -20 | Creepy music. |
| `whycantisleep:loud_music` | Blaring Music | -15 | Loud music. |
| `whycantisleep:restless` | Restless Mind | -10 | Restless before lying down. |
| `whycantisleep:sleepless` | Days Without Sleep | -20 | Sleepless before lying down. |
| `whycantisleep:nightmare` | Haunting Nightmare | -15 | A night terror. |
| `whycantisleep:woke_early` | Woke Before Dawn | -25 | Woke for any reason but the morning. |
| `whycantisleep:routine` | Good Bedtime Routine | +10 | Slept till morning in a closed room lit all through. |

The routine modifier does one thing more: when it counts with points above 0 (and the server's `wellRested` setting is
on), the player wakes **well rested**, with Regeneration for 30 seconds. Change its conditions to change what well
rested takes; turn it off and nobody wakes well rested.

The labels are translation keys (`gui.whycantisleep.quality.<id>`) in the mod's language file, so they show in each
player's language.

```json title="data/whycantisleep/whycantisleep/sleep_modifier/monsters.json (the mod's own)"
{
  "label": "gui.whycantisleep.quality.monsters",
  "per": "monsters",
  "each": -6,
  "most": -30,
  "when": {
    "monsters": { "min": 1 }
  }
}
```

## Replacing and turning off

A file with the same id as a modifier takes its place. To change one of the mod's own, put a file at the same path in
your data pack: `data/whycantisleep/whycantisleep/sleep_modifier/<name>.json`. The whole file is replaced, not merged,
so write every key you want.

```json title="data/whycantisleep/whycantisleep/sleep_modifier/monsters.json (harsher)"
{
  "label": "gui.whycantisleep.quality.monsters",
  "per": "monsters",
  "each": -10,
  "most": -50,
  "when": {
    "monsters": { "min": 1 }
  }
}
```

To take a modifier out, replace its file with one that only says so:

```json title="data/whycantisleep/whycantisleep/sleep_modifier/exposed.json"
{
  "disabled": true
}
```

`"disabled": true` also takes out a modifier another mod registered in code, when the file has its id. A modifier from
a data pack with the same id as one from code replaces it.

!!! note "Other mods"
    Mods can add modifiers in code, and can change a night's factors after every modifier has had its say. See
    [For mod developers](../developers/index.md).

## Examples

```json title="data/mypack/whycantisleep/sleep_modifier/forest_air.json"
{
  "label": "Forest Air",
  "points": 5,
  "when": {
    "biome": "#minecraft:is_forest",
    "enclosed": false,
    "monsters": { "max": 0 }
  }
}
```

```json title="data/mypack/whycantisleep/sleep_modifier/thunderstorm.json"
{
  "label": "Thunder Overhead",
  "points": -10,
  "when": {
    "weather": "thunder",
    "reason": ["morning"]
  }
}
```

```json title="data/mypack/whycantisleep/sleep_modifier/night_light.json"
{
  "label": "Night Light",
  "points": 5,
  "when": {
    "pillow_light": { "min": 3, "max": 7 },
    "trouble": ["restless", "sleepless"]
  }
}
```

The [Insomniac's Handbook](../playing/nights.md) lists every modifier from data packs on its What Counts pages, yours
included, with their points.
