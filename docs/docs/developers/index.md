# For mod developers

Why Can't I Sleep? has a small Java API for other mods. With it you can read how badly a player is sleeping and how
well they slept, find the monsters keeping them awake, put players into hiding spots and bring them out, add hiding
spots of your own, score nights and give sleep effects from code, and listen to what happens: refused sleeps, wakings,
night terrors, players hiding and the monsters they shake off.

Nothing here is needed for data packs. Sleep modifiers, sleep effects, hiding spots and the tags are all plain JSON:
see [Data packs](../datapacks/index.md).

| Class | Side | For | Page |
| --- | --- | --- | --- |
| `net.ixdarklord.whycantisleep.api.WhyCantISleepApi` | server, one method both | Sleep trouble, sleep quality, sleep blockers, hiding, night terrors. | [Below](#whycantisleepapi) |
| `net.ixdarklord.whycantisleep.api.SleepEvents` | server | Refused sleeps, lying down, scoring and waking, skipped nights, night terrors. | [Events](events.md#sleepevents) |
| `net.ixdarklord.whycantisleep.api.HidingEvents` | server | Players starting and stopping hiding, shaking off monsters, noise. | [Events](events.md#hidingevents) |
| `net.ixdarklord.whycantisleep.api.HidingSpots` | both | Registering kinds of hiding spot. | [Hiding spots in code](hiding-spots.md) |
| `net.ixdarklord.whycantisleep.api.HidingSpotType`, `HidingSpot`, `HidingPosture` | both | A kind of hiding spot, the spot it finds, how the player is placed. | [Hiding spots in code](hiding-spots.md) |
| `net.ixdarklord.whycantisleep.api.SleepTrouble` | server | How badly a player is sleeping, and its level. | [Below](#sleep-trouble) |
| `net.ixdarklord.whycantisleep.api.SleepQuality`, `WakeReason` | server | How well a night went and why it ended. | [Below](#sleep-quality) |
| `net.ixdarklord.whycantisleep.api.SleepModifiers`, `SleepModifier`, `SleepContext` | server | Sleep quality modifiers in code. | [Below](#sleep-modifiers-in-code) |
| `net.ixdarklord.whycantisleep.api.SleepEffects`, `SleepEffect` | server | Sleep effects in code. | [Below](#sleep-effects-in-code) |
| `net.ixdarklord.whycantisleep.api.datagen.*` | data generation | Writing sleep effect and modifier files. | [Data generation](datagen.md) |

Only the `api` package is supported. Everything else in the mod is internal and may change in any release.

!!! note mc-121 mc-120 "1.21.1 and 1.20.1"
    The examples use `Identifier`, as on 26.1 and later. On 1.21.1 and 1.20.1 the same methods take and return a
    `ResourceLocation`. Other differences are noted where they come up.

## Setup

Why Can't I Sleep? is built on [CoolCatLib](https://modrinth.com/mod/coolcatlib) and needs it at runtime. The API's
events are CoolCatLib events, so to listen to them you compile against CoolCatLib too.

The API will be published to the [ModResources Maven](https://ixdarklord.github.io/ModResources/) with the mod's first
release, free to use with no token, the same Maven [Folios](https://ixdarklord.github.io/Folios/) and CoolCatLib are on.
Until then it is not on any public Maven. Once it is, the Maven's page lists every version, ready to copy:

```groovy title="build.gradle"
repositories {
    maven {
        name = "ModResources"
        url = "https://ixdarklord.github.io/ModResources/maven/"
    }
}

dependencies {
    // In the common module: the API, and CoolCatLib for its events.
    compileOnly "net.ixdarklord.whycantisleep:whycantisleep-common:<whycantisleep version>"
    compileOnly "net.ixdarklord.coolcatlib:coolcatlib-common:<coolcatlib version>"
}
```

The API is `compileOnly`: your mod doesn't ship it and doesn't need it to run. Each loader has its own artifact too
(<span class="mc-v mc-all"><code>whycantisleep-fabric</code>, <code>whycantisleep-neoforge</code>, and
<code>whycantisleep-forge</code> on 1.21.1 and 1.20.1</span><span class="mc-v mc-only mc-26"><code>whycantisleep-fabric</code>,
<code>whycantisleep-neoforge</code></span><span class="mc-v mc-only mc-121"><code>whycantisleep-fabric</code>,
<code>whycantisleep-neoforge</code>, <code>whycantisleep-forge</code></span><span class="mc-v mc-only mc-120"><code>whycantisleep-fabric</code>,
<code>whycantisleep-forge</code></span>), which you can add to your loader modules' development runtime to try your
code in game. Declare Why Can't I Sleep? as an optional dependency in your mod's metadata, not a required one.

!!! note mc-121 mc-120 "1.21.1 and 1.20.1"
    Minecraft is obfuscated before 26.1, so with Loom the loader jars go in the `mod*` configurations
    (`modLocalRuntime`, `modCompileOnly`). 1.21.1 has Fabric, NeoForge and Forge; 1.20.1 has Fabric and Forge.

## Calling it only when the mod is loaded

Your mod must still load when Why Can't I Sleep? isn't installed. Keep every call to the API in a class of your own,
and reach that class only after checking that the mod is loaded:

```java title="MyMod.java"
import net.ixdarklord.coolcatcore.api.core.ModConstructor;
import net.ixdarklord.coolcatcore.api.platform.Platform;

public final class MyMod implements ModConstructor {
    @Override
    public void onConstructMod() {
        if (Platform.isModLoaded("whycantisleep")) MyModSleepCompat.init();
    }
}
```

```java title="MyModSleepCompat.java"
import net.ixdarklord.whycantisleep.api.HidingSpots;
import net.ixdarklord.whycantisleep.api.SleepEvents;

/** Everything that touches Why Can't I Sleep?, loaded only when it is installed. */
public final class MyModSleepCompat {
    private MyModSleepCompat() {
    }

    public static void init() {
        HidingSpots.register(new WardrobeHidingSpot());
        SleepEvents.WOKE.register(woke -> {
            if (woke.reason().rested() && woke.quality().score() >= 90) MyRewards.giveDreamcatcher(woke.player());
        });
    }
}
```

The JVM loads `MyModSleepCompat`, and with it the API's classes, only when `init()` is first called, so without the
mod nothing of it is touched. Write the mod id as a plain string in the check: `WhyCantISleepApi.MOD_ID` holds the same
value, but naming the API class where the mod may be missing is the habit to avoid. `onConstructMod()` runs on both
sides, which is where [hiding spots](hiding-spots.md) must be registered. If your mod doesn't use CoolCatLib, your
loader's own check (`FabricLoader.getInstance().isModLoaded`, `ModList.get().isLoaded`) does the same job.

## WhyCantISleepApi

Static methods, for reading and changing what the mod keeps on players. Most are **server side**: they take a
`ServerPlayer` and must be called on the server thread. A player on whom the server knows nothing gets an empty answer
(no trouble, no quality, not hiding), never an exception.

### MOD_ID

```java
public static final String MOD_ID = "whycantisleep";
```

The mod's id.

### sleepTrouble

```java
public static SleepTrouble sleepTrouble(ServerPlayer player)
```

**Server.** How badly the player is sleeping: their refused sleeps in a row and the ticks since they last lay down,
with the [level](#sleep-trouble) those add up to. Refusals are kept for the server's session; the time since rest is the
game's own statistic, so it survives restarts.

### sleepBlockers

```java
public static List<Mob> sleepBlockers(ServerPlayer player, BlockPos bed)
```

**Server.** Exactly the mobs that would keep the player from sleeping in this bed: the game's own monster check with
the mod's rules over it (`#whycantisleep:blocks_sleep`, `#whycantisleep:does_not_block_sleep`, the shared bed
tolerance). Pass the bed's head. It doesn't try to sleep, so it changes nothing; an empty list means no monster stands
in the way (the time of day, distance and the rest are not checked).

### lastSleepQuality

```java
public static @Nullable SleepQuality lastSleepQuality(ServerPlayer player)
```

**Server.** How well the player slept the last time they slept, or `null` when they haven't slept since the server
started. Every waking is scored, getting up early included.

### isHiding

```java
public static boolean isHiding(Player player)
```

**Both sides.** Whether the player is hiding. A client knows it of every player it can see, not only its own.

### hidingSpot

```java
public static @Nullable HidingSpot hidingSpot(ServerPlayer player)
```

**Server.** Where the player hides, or `null` when they aren't hiding. See [`HidingSpot`](hiding-spots.md#hidingspot).

### hiddenTicks

```java
public static int hiddenTicks(ServerPlayer player)
```

**Server.** How long the player has been hiding, in ticks; 0 when they aren't.

### hideTicks

```java
public static int hideTicks(ServerPlayer player)
```

**Server.** How long the player must stay hidden in their spot before the monsters after them give up
([`HidingEvents.SHAKEN_OFF`](events.md#shaken_off)), in ticks: the server's hide time scaled by the spot's
`hideTimeScale` and by the walls round it. 0 when they aren't hiding.

### baseHideTicks

```java
public static int baseHideTicks()
```

The server's hide time (the `hideTime` option) in ticks, before any spot or walls scale it. It reads the server
config, which is synced to clients, so it answers on either side.

### hidingNoise

```java
public static float hidingNoise(ServerPlayer player)
```

**Server.** The noise the hiding player makes, 0 to 1, as their client last reported it, in steps of a tenth. 0 when
they aren't hiding. At 1, with monsters near, they are heard and found.

### startHiding

```java
public static boolean startHiding(ServerPlayer player, BlockPos pos)
public static boolean startHiding(ServerPlayer player, HidingSpot spot)
```

**Server.** Puts the player into hiding, as if they had got in themselves. The first form asks the hiding spot types
for the block at `pos`, as a sneak-use of it would; the second takes a spot a [`HidingSpotType`](hiding-spots.md) found,
whose `type` must be the id of a registered or data pack type.

It returns `false` when they can't hide: no spot at the block, the spot's type unknown, already hiding, a
[`HidingEvents.START`](events.md#start) listener cancelled it, or every place in the spot is taken. It skips what a
player's own sneak-use checks: empty hands, sneaking and the hide cooldown. With the critter chance, something may be
in the spot already: the method then returns `true`, but the player is chased out rather than hidden, so check
[`isHiding`](#ishiding) if you need to know.

### stopHiding

```java
public static boolean stopHiding(ServerPlayer player)
```

**Server.** Brings a hiding player out of their spot, where they went in. [`HidingEvents.STOP`](events.md#stop) fires
with the reason `FORCED`, and the hide cooldown starts as it does for any other way out. Returns `false` when they
weren't hiding.

### nightmare

```java
public static void nightmare(ServerPlayer player)
```

**Server.** Gives the player a night terror now. Asleep, it is the night's terror: they wake shaken and the night is
scored for it. Awake, they only see it. [`SleepEvents.NIGHT_TERROR`](events.md#night_terror) does not fire for it.

### markNightmare

```java
public static void markNightmare(ServerPlayer player)
```

**Server.** Marks the player for a night terror the next time they lie down, whether or not they sleep badly, as
`/whycantisleep nightmare` does. The mark is a player tag, so it lasts through relogging. When they lie down,
[`SleepEvents.NIGHT_TERROR`](events.md#night_terror) fires for it as for any other and can still cancel it. A player
already asleep has it at once instead, as with [`nightmare`](#nightmare).

## SleepTrouble { #sleep-trouble }

`record SleepTrouble(int failedAttempts, int ticksSinceRest)`: how badly a player is sleeping.

| Member | What it is |
| --- | --- |
| `failedAttempts()` | Sleeps refused in a row since the player last lay down. |
| `ticksSinceRest()` | Ticks since they last lay down in a bed (the game's "time since rest", the one that brings phantoms). |
| `level()` | The `SleepTrouble.Level` the two add up to. |
| `isTroubled()` | Whether the level is at least `RESTLESS`. |
| `NONE` | No trouble at all. |
| `DAY_TICKS` | One in-game day, 24000 ticks. |

`SleepTrouble.Level` is `RESTED`, `RESTLESS` or `SLEEPLESS`. Either measure reaching a level's threshold puts the
player at that level:

| Level | Refusals in a row | Or days without lying down |
| --- | --- | --- |
| `RESTED` | fewer than 2 | under 2 |
| `RESTLESS` | 2 | 2 |
| `SLEEPLESS` | 5 | 3, when phantoms come |

Each level has `failedAttempts()` and `ticksSinceRest()` for its thresholds, and `atLeast(other)` to compare.

## SleepQuality { #sleep-quality }

`record SleepQuality(int score, List<Factor> factors)`: how well a player slept one night, 0 (awful) to 100
(perfect), and what made it so. Every night starts from `SleepQuality.BASE` (75); each factor adds its points, and the
sum is held to 0 to 100.

| Member | What it is |
| --- | --- |
| `score()` | 0 to 100. |
| `factors()` | What made it so, in the order they were added. |
| `worst()` | The factor that cost the most, or `null` when none cost anything. |
| `best()` | The factor that helped the most, or `null` when none helped. |
| `of(factors)` | The quality of a night with these factors. |

A `SleepQuality.Factor` is `(Identifier id, Component label, int points)`. The ids of the mod's own factors are
constants on `SleepQuality`: `ENCLOSED`, `EXPOSED`, `DARK_CORNERS`, `TOO_BRIGHT`, `MONSTERS`, `CALM_MUSIC`,
`CREEPY_MUSIC`, `LOUD_MUSIC`, `SOOTHING`, `UNSETTLING`, `RESTLESS`, `SLEEPLESS`, `NIGHTMARE`, `WOKE_EARLY` and `ROUTINE`
(the routine is what makes a player well rested). They are the ids of the mod's [sleep
modifiers](../datapacks/sleep-modifiers.md), so a data pack can change or remove them.

`WakeReason` says why a sleeper woke: `MORNING` (the night was skipped, or day came), `GOT_UP`, `HURT`, `BED_GONE` or
`OTHER` (a mod, a command, leaving the world). `rested()` is true only for `MORNING`.

## Sleep modifiers in code

Every night is scored by every sleep modifier. The mod's own are data files, and a data pack adds more with no code
(see [Sleep modifiers](../datapacks/sleep-modifiers.md)). When a modifier needs something a file can't say, register one
in code:

```java title="MyModSleepCompat.java"
SleepModifiers.register(SleepModifier.of(MyMod.id("feather_pillow"),
        Component.translatable("mymod.modifier.feather_pillow"),
        context -> MyBlocks.hasFeatherPillow(context.player().level(), context.bed()) ? 8 : 0));
```

`points` returns what the modifier makes of the night: above 0 a buff, below 0 a debuff, 0 for no say (it isn't
listed on the morning card). The label is a name in Title Case ("Feather Pillow"). Register while your mod
initialises; a modifier with the id of another takes its place.

The `SleepContext` it is given is the night as its sleeper wakes:

| Field | What it is |
| --- | --- |
| `player`, `bed` | The sleeper and the bed they slept in. |
| `reason`, `sleptTicks` | Why they woke, and how long they lay asleep. |
| `enclosed`, `lit` | Whether the room was closed (walled, roofed, doors shut, no sky), and lit all through. |
| `pillowLight` | The block light at the pillow, 0 to 15. |
| `soothing`, `unsettling` | How many soothing and unsettling things were placed near the bed. |
| `monsters` | The most monsters within 24 blocks at any time of the night. |
| `calmMusic`, `creepyMusic`, `loudMusic` | Whether such music played from a jukebox nearby. |
| `trouble` | Their `SleepTrouble.Level` before they lay down. |
| `nightmare` | Whether they had a night terror. |

`SleepModifiers` also has `remove(id)` (only for modifiers registered in code: the mod's own are data, turned off with
a data pack), `get(id)`, `all()` and `score(context)`. To change a night's factors after every modifier has had its
say, listen to [`SleepEvents.SCORE_QUALITY`](events.md#score_quality).

## Sleep effects in code

A sleep effect is what a night leaves a player with when they wake in the morning. Data packs add them with no code
(see [Sleep effects](../datapacks/sleep-effects.md)); in code, an effect can also run an action of its own:

```java title="MyModSleepCompat.java"
SleepEffects.register(SleepEffect.of(MyMod.id("starry_dream"), SleepEffect.Kind.POSITIVE, SleepEffect.Rarity.EPIC,
                Component.translatable("mymod.sleep_effect.starry_dream"),
                new SleepEffect.Effect(MobEffects.NIGHT_VISION, 20 * 60 * 5, 0))
        .withQuality(70, 100)
        .withAction(player -> player.giveExperiencePoints(30)));
```

`SleepEffect.of` gives an effect the rarity's weight (common 60, uncommon 25, rare 10, epic 4, legendary 1) and every
night's score; `withQuality(min, max)` narrows the scores it can follow, `withWeight(weight)` sets its own weight, and
`withAction(action)` runs something after its mob effects. A `SleepEffect.Effect` is a mob effect, its duration in
ticks and its amplifier. A data pack effect with the same id takes the place of one registered in code.

`SleepEffects` also has `get(id)`, `all()` and `draw(score, random)`, which draws an effect as the mod does on waking.
To change or cancel the effect a player is given, listen to [`SleepEvents.SLEEP_EFFECT`](events.md#sleep_effect).

!!! note mc-120 "1.20.1"
    `SleepEffect.Effect` takes a `MobEffect` rather than a `Holder<MobEffect>`. `MobEffects.NIGHT_VISION` is the right
    type on each version, so the example compiles unchanged.
