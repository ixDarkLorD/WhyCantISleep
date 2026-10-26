# Sleep effects

Waking in the morning, you may wake with a **sleep effect**: a dream that helps (positive), one that hurts (negative),
or one that is only odd (neutral). Each gives mob effects, and the morning card names it in its rarity's color. How
this plays is on [Nights](../playing/nights.md).

The mod's own twenty-two sleep effects are data files, and a data pack can add more or change them. Each is a file at
`data/<namespace>/whycantisleep/sleep_effect/<name>.json`, the same on every Minecraft version.

```json title="data/mypack/whycantisleep/sleep_effect/starry_dream.json"
{
  "kind": "positive",
  "rarity": "epic",
  "quality": { "min": 75, "max": 100 },
  "effects": [
    { "id": "minecraft:night_vision", "duration": 6000 },
    { "id": "minecraft:luck", "duration": 6000 }
  ],
  "name": "Starry Dream"
}
```

## Keys

| Key | Type | Default | What it does |
| --- | --- | --- | --- |
| `kind` | string | required | `positive`, `negative` or `neutral`. Decides after which nights it can come, and its icon. |
| `rarity` | string | `"common"` | `common`, `uncommon`, `rare`, `epic` or `legendary`. Gives its weight, unless `weight` is set, and the color it is named in. |
| `weight` | integer, 0 to 10000 | the rarity's | Its share of the draw among those of its kind. `0` means it is never drawn. |
| `quality` | object | `{ "min": 0, "max": 100 }` | The night's scores it can follow: `min` and `max`, each 0 to 100, both optional and both included. |
| `effects` | list of objects | `[]` | The mob effects it gives. See [Effects](#effects). |
| `name` | string | required | What the player is told: a translation key, or plain text. |

### Rarity

| Rarity | Weight | Named in |
| --- | --- | --- |
| `common` | 60 | white |
| `uncommon` | 25 | yellow |
| `rare` | 10 | aqua |
| `epic` | 4 | light purple |
| `legendary` | 1 | gold |

These are the game's own item rarity colors.

### Effects

| Key | Type | Default | What it does |
| --- | --- | --- | --- |
| `id` | mob effect id | required | The effect, such as `"minecraft:speed"`. Effects from other mods work too. |
| `duration` | integer, 1 to 72000 | `600` | How long it lasts, in ticks (20 a second; 72000 is an hour). |
| `amplifier` | integer, 0 to 9 | `0` | Its level, less one: `0` is Speed I, `1` is Speed II. |

The effects are given with their particles and icon showing. A sleep effect with no `effects` still shows on the card,
and gives nothing.

## How one is drawn

When a player wakes in the morning after a night slept through, and the server's `sleepEffects` setting is on, there is
a `sleepEffectChance` in 100 (50 by default) of a sleep effect. Waking early, by getting up or being hurt, never gives
one. See [Configuration](../server/configuration.md).

First the kind is drawn, from the night's [score](sleep-modifiers.md):

| Kind | Weight |
| --- | --- |
| positive | score - 40, or 0 below that |
| negative | 60 - score, or 0 above that |
| neutral | 20 |

So after a night of 75 a positive effect is drawn 35 times in 55 and a neutral one 20 in 55; after a night of 50 it is
10 positive, 10 negative and 20 neutral; after 20, 40 negative and 20 neutral.

Then one effect of that kind is drawn by weight, among those whose `quality` range holds the score and whose weight is
above 0. If none of the kind fits, a neutral one is drawn instead; if no neutral one fits either, there is no sleep
effect that morning.

A mod can change or cancel the effect drawn before it is given. See [For mod developers](../developers/index.md).

## The mod's own effects

They are in `data/whycantisleep/whycantisleep/sleep_effect/`. Their names are translation keys
(`gui.whycantisleep.sleep_effect.<id>`) in the mod's language file.

| Id | Name | Kind | Rarity | Quality | Gives |
| --- | --- | --- | --- | --- | --- |
| `whycantisleep:early_bird` | Early Bird | positive | common | any | Speed, 1 min |
| `whycantisleep:refreshed` | Refreshed | positive | common | any | Haste, 1 min |
| `whycantisleep:sweet_dreams` | Sweet Dreams | positive | common | any | Regeneration, 30 s |
| `whycantisleep:clear_mind` | Clear Mind | positive | uncommon | any | Night Vision, 2 min |
| `whycantisleep:full_belly` | Dreamt of a Feast | positive | uncommon | any | Saturation II, 10 ticks |
| `whycantisleep:lucky_dream` | Lucky Dream | positive | rare | 70 to 100 | Luck, 5 min |
| `whycantisleep:strong_dream` | Dreamt of Victory | positive | rare | 60 to 100 | Strength, 1 min |
| `whycantisleep:heroic_dream` | Heroic Dream | positive | epic | 80 to 100 | Absorption II, 2 min; Resistance, 1 min |
| `whycantisleep:dream_of_flight` | Dream of Flight | positive | legendary | 90 to 100 | Slow Falling, Jump Boost II and Speed II, 1 min |
| `whycantisleep:empty_stomach` | Woke Up Starving | negative | common | any | Hunger, 30 s |
| `whycantisleep:groggy` | Groggy | negative | common | any | Slowness, 1 min |
| `whycantisleep:stiff_back` | Stiff Back | negative | common | any | Mining Fatigue, 1 min |
| `whycantisleep:headache` | Pounding Headache | negative | uncommon | any | Nausea, 10 s |
| `whycantisleep:weak_limbs` | Weak Limbs | negative | uncommon | any | Weakness, 1 min |
| `whycantisleep:bleary_eyes` | Bleary Eyes | negative | rare | 0 to 40 | Blindness, 5 s |
| `whycantisleep:haunted` | Haunted | negative | epic | 0 to 30 | Darkness, 30 s; Bad Luck, 5 min |
| `whycantisleep:cursed_dream` | Cursed Dream | negative | legendary | 0 to 20 | Poison, 5 s; Weakness, 2 min |
| `whycantisleep:night_owl` | Night Owl | neutral | common | any | Night Vision, 45 s |
| `whycantisleep:vivid_dream` | Vivid Dream | neutral | common | any | Glowing, 30 s |
| `whycantisleep:dream_of_fire` | Dream of Fire | neutral | uncommon | any | Fire Resistance, 2 min |
| `whycantisleep:dream_of_the_sea` | Dream of the Sea | neutral | uncommon | any | Water Breathing, 3 min; Dolphin's Grace, 1 min |
| `whycantisleep:restless_legs` | Restless Legs | neutral | rare | any | Jump Boost II, 1 min; Mining Fatigue, 30 s |

```json title="data/whycantisleep/whycantisleep/sleep_effect/lucky_dream.json (the mod's own)"
{
  "kind": "positive",
  "rarity": "rare",
  "quality": { "min": 70, "max": 100 },
  "effects": [
    { "id": "minecraft:luck", "duration": 6000, "amplifier": 0 }
  ],
  "name": "gui.whycantisleep.sleep_effect.lucky_dream"
}
```

## Replacing and turning off

A file with the same id as a sleep effect takes its place. To change one of the mod's own, put a file at the same path
in your data pack: `data/whycantisleep/whycantisleep/sleep_effect/<name>.json`. The whole file is replaced, not merged,
so write every key, `kind` and `name` included.

Sleep effects have no `disabled` key. To take one out, replace it with a weight of 0: it is never drawn.

```json title="data/whycantisleep/whycantisleep/sleep_effect/vivid_dream.json (never drawn)"
{
  "kind": "neutral",
  "name": "gui.whycantisleep.sleep_effect.vivid_dream",
  "weight": 0
}
```

A data pack effect with the same id as one a mod registered in code replaces it too. To have no sleep effects at all,
turn off `sleepEffects` in the [server configuration](../server/configuration.md).

## Examples

```json title="data/mypack/whycantisleep/sleep_effect/sleepwalker.json"
{
  "kind": "neutral",
  "rarity": "uncommon",
  "effects": [
    { "id": "minecraft:speed", "duration": 600, "amplifier": 1 },
    { "id": "minecraft:nausea", "duration": 200 }
  ],
  "name": "Sleepwalker"
}
```

```json title="data/mypack/whycantisleep/sleep_effect/night_sweats.json"
{
  "kind": "negative",
  "rarity": "rare",
  "weight": 15,
  "quality": { "max": 35 },
  "effects": [
    { "id": "minecraft:hunger", "duration": 1200, "amplifier": 1 },
    { "id": "minecraft:weakness", "duration": 1200 }
  ],
  "name": "Night Sweats"
}
```

!!! note mc-120 "1.20.1"
    The format is the same on every version, but use effect ids that exist in your version of the game. All of the
    mod's own effects exist on 1.20.1.

The [Insomniac's Handbook](../playing/nights.md) lists every sleep effect, yours included, with its kind and rarity.
