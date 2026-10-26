# Data generation

Sleep effects and sleep quality modifiers are data files. Your mod can ship its own in its `data` folder, and can
write them with the game's data generation rather than by hand. `net.ixdarklord.whycantisleep.api.datagen` has what
you need:

| Class | What it does |
| --- | --- |
| `SleepDataProvider` | An abstract `DataProvider` that writes the files. You extend it. |
| `SleepEffectBuilder` | Builds one sleep effect file. |
| `SleepModifierBuilder` | Builds one sleep modifier file, or one that turns a modifier off. |

The files are the same ones a data pack writes, described on [Sleep effects](../datapacks/sleep-effects.md) and
[Sleep modifiers](../datapacks/sleep-modifiers.md). They land in `data/<namespace>/whycantisleep/sleep_effect/` and
`data/<namespace>/whycantisleep/sleep_modifier/`, named by id.

The generated files need no guard: without Why Can't I Sleep? installed, nothing reads those folders. Your data
generation run does need the mod on its classpath, since the provider uses its classes (the `compileOnly` API from
[Setup](index.md#setup) plus the loader jar in your development runtime).

## SleepDataProvider

```java
public abstract class SleepDataProvider implements DataProvider {
    protected SleepDataProvider(PackOutput output);
    protected abstract void generate();
    protected final void effect(Identifier id, SleepEffectBuilder effect);
    protected final void modifier(Identifier id, SleepModifierBuilder modifier);
}
```

Extend it and add your files in `generate()`: `effect(id, builder)` for a sleep effect, `modifier(id, builder)` for a
modifier. The id is the file's namespace and name, and so the effect's or modifier's id. Giving the same id twice in
one kind throws. A modifier with the id of another (the mod's own, or another mod's) takes its place, and
`SleepModifierBuilder.disabled()` at that id takes it out.

## SleepEffectBuilder

Start with the kind and the name, a translation key or plain text:

| Method | What it does |
| --- | --- |
| `positive(name)`, `negative(name)`, `neutral(name)` | A sleep effect of that kind. |
| `of(kind, name)` | The same, with a `SleepEffect.Kind`. |
| `rarity(rarity)` | `SleepEffect.Rarity`: `COMMON` (the default), `UNCOMMON`, `RARE`, `EPIC` or `LEGENDARY`. Sets the color it is named in and its weight. |
| `weight(weight)` | Its own share of the draw among those of its kind, in place of the rarity's (common 60, uncommon 25, rare 10, epic 4, legendary 1). |
| `quality(min, max)` | The night scores it can follow, 0 to 100, both included. Every score by default. |
| `effect(effect, seconds)` | A mob effect it gives, for so many **seconds**, at level I. |
| `effect(effect, seconds, amplifier)` | The same at an amplifier (0 for level I). |
| `toJson()` | The file, as a `JsonObject`. |

Durations are given in seconds and written in ticks. An effect from code can also run an action on waking; a file
can't, so for that [register it in code](index.md#sleep-effects-in-code).

## SleepModifierBuilder

Start with the label, a translation key or plain text (a name in Title Case), then give its points and conditions:

| Method | What it does |
| --- | --- |
| `of(label)` | A modifier with no points yet. |
| `disabled()` | A file that takes out the modifier with its id. |
| `points(points)` | Fixed points: above 0 a buff, below 0 a debuff. |
| `perMonster(each, most)` | Points for each monster near the bed, held to `most`. |
| `perSoothing(each, most)`, `perUnsettling(each, most)` | Points for each soothing or unsettling thing by the bed. |
| `perPillowLight(each, most)` | Points for each level of block light at the pillow. |
| `perNearBlocks(each, most)` | Points for each block counted by `nearBlocks`. |
| `toJson()` | The file, as a `JsonObject`. |

Fixed points and a count add up. `most` caps the total: for a debuff, give it below 0 (`perMonster(-6, -30)` takes at
most 30). A modifier only gives points when every condition it has holds:

| Condition | Holds when |
| --- | --- |
| `enclosed(bool)`, `lit(bool)` | The room was (or wasn't) closed; lit all through. |
| `pillowLight(min, max)` | The block light at the pillow is in range. |
| `monsters(min, max)`, `soothing(min, max)`, `unsettling(min, max)` | The count is in range. |
| `calmMusic(bool)`, `creepyMusic(bool)`, `loudMusic(bool)` | Such music did (or didn't) play nearby. |
| `nightmare(bool)` | They had (or didn't have) a night terror. |
| `trouble(levels...)` | Their `SleepTrouble.Level` before lying down is one of these. |
| `reason(reasons...)` | They woke for one of these `WakeReason`s. |
| `dimension(key)` | The bed is in this dimension. |
| `biome(key)`, `biome(tag)` | The bed is in this biome, or one in this tag. |
| `weather(name)` | The weather is `"clear"`, `"rain"` or `"thunder"`. |
| `nearBlocks(range, atLeast, blocks...)`, `nearBlocks(range, atLeast, tags...)` | At least `atLeast` of these blocks are within `range` of the bed. They are also what `perNearBlocks` counts. |

## Example

A mod with a dream that follows good nights, a nightmare that follows bad ones, a buff for a campfire's glow, a debuff
for sleeping in the Nether, and no penalty for loud music:

```java title="MySleepData.java"
import net.ixdarklord.whycantisleep.api.SleepEffect;
import net.ixdarklord.whycantisleep.api.SleepQuality;
import net.ixdarklord.whycantisleep.api.WakeReason;
import net.ixdarklord.whycantisleep.api.datagen.SleepDataProvider;
import net.ixdarklord.whycantisleep.api.datagen.SleepEffectBuilder;
import net.ixdarklord.whycantisleep.api.datagen.SleepModifierBuilder;
import net.minecraft.data.PackOutput;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class MySleepData extends SleepDataProvider {
    public MySleepData(PackOutput output) {
        super(output);
    }

    @Override
    protected void generate() {
        effect(MyMod.id("starry_dream"), SleepEffectBuilder.positive("mymod.sleep_effect.starry_dream")
                .rarity(SleepEffect.Rarity.EPIC)
                .quality(70, 100)
                .effect(MobEffects.NIGHT_VISION, 300)
                .effect(MobEffects.LUCK, 600));
        effect(MyMod.id("falling_dream"), SleepEffectBuilder.negative("mymod.sleep_effect.falling_dream")
                .rarity(SleepEffect.Rarity.UNCOMMON)
                .quality(0, 40)
                .effect(MobEffects.WEAKNESS, 120, 1));

        modifier(MyMod.id("campfire_glow"), SleepModifierBuilder.of("mymod.modifier.campfire_glow")
                .perNearBlocks(3, 6)
                .nearBlocks(6, 1, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE)
                .reason(WakeReason.MORNING));
        modifier(MyMod.id("hellish_heat"), SleepModifierBuilder.of("Hellish Heat")
                .points(-12)
                .dimension(Level.NETHER));
        modifier(SleepQuality.LOUD_MUSIC, SleepModifierBuilder.disabled());
    }
}
```

`generate()` runs each time data is generated. The three ids from `MyMod.id` are in your namespace, so they add new
files; `SleepQuality.LOUD_MUSIC` is `whycantisleep:loud_music`, so that file replaces the mod's own and turns it off.
Labels and names may be translation keys or plain text: "Hellish Heat" shows as it is.

The campfire's glow gives 3 points for each campfire within 6 blocks of the bed, at most 6, and only when there is at
least one and the player slept till morning.

### Adding the provider

With CoolCatLib's data generation entry point, in a `DataGenerationConstructor`:

```java title="MyModData.java"
@Override
public void onGatherData(DataGenerationContext context) {
    if (Platform.isModLoaded("whycantisleep")) context.addProvider(MySleepData::new);
}
```

Or with the loader's own:

=== "Fabric"

    ```java
    pack.addProvider(MySleepData::new);
    ```

=== "NeoForge"

    ```java
    event.createProvider(MySleepData::new);
    ```

!!! note mc-120 "1.20.1"
    1.20.1 has Forge in place of NeoForge:
    `event.getGenerator().addProvider(event.includeServer(), MySleepData::new);`.
    `SleepEffectBuilder.effect` takes a `MobEffect` rather than a `Holder<MobEffect>`; `MobEffects.NIGHT_VISION` is the
    right type on each version.

!!! note mc-121 mc-120 "1.21.1 and 1.20.1"
    `Identifier` is `ResourceLocation`. Some vanilla effect names differ before 26.1 (`MobEffects.MOVEMENT_SPEED` for
    speed); the ones in the example are the same on every version.

### The files it writes

```json title="data/mymod/whycantisleep/sleep_effect/starry_dream.json"
{
  "effects": [
    {
      "amplifier": 0,
      "duration": 6000,
      "id": "minecraft:night_vision"
    },
    {
      "amplifier": 0,
      "duration": 12000,
      "id": "minecraft:luck"
    }
  ],
  "kind": "positive",
  "name": "mymod.sleep_effect.starry_dream",
  "quality": {
    "max": 100,
    "min": 70
  },
  "rarity": "epic"
}
```

```json title="data/mymod/whycantisleep/sleep_effect/falling_dream.json"
{
  "effects": [
    {
      "amplifier": 1,
      "duration": 2400,
      "id": "minecraft:weakness"
    }
  ],
  "kind": "negative",
  "name": "mymod.sleep_effect.falling_dream",
  "quality": {
    "max": 40,
    "min": 0
  },
  "rarity": "uncommon"
}
```

```json title="data/mymod/whycantisleep/sleep_modifier/campfire_glow.json"
{
  "each": 3,
  "label": "mymod.modifier.campfire_glow",
  "most": 6,
  "per": "near_blocks",
  "when": {
    "near_blocks": {
      "blocks": [
        "minecraft:campfire",
        "minecraft:soul_campfire"
      ],
      "count": {
        "min": 1
      },
      "range": 6
    },
    "reason": [
      "morning"
    ]
  }
}
```

```json title="data/mymod/whycantisleep/sleep_modifier/hellish_heat.json"
{
  "label": "Hellish Heat",
  "points": -12,
  "when": {
    "dimension": "minecraft:the_nether"
  }
}
```

```json title="data/whycantisleep/whycantisleep/sleep_modifier/loud_music.json"
{
  "disabled": true
}
```

Keys are written in alphabetical order, as the game's data generation writes every file. A sleep effect leaves out
`weight` unless you gave one, and `quality` when it is the whole 0 to 100.
