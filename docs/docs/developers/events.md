# Events

`SleepEvents` and `HidingEvents` tell your mod what happens to sleepers and hiders, and let it change some of it:
cancel a night terror or a player hiding, add to a night's score, swap a sleep effect, spare a monster that would
vanish.

Every event fires on the **server**, on the server thread (the integrated server in single player). None fires on the
client.

## Listening

Each event is a CoolCatLib `net.ixdarklord.coolcatcore.api.event.v1.Event<T>`, a plain list of listeners:

| Method | What it does |
| --- | --- |
| `register(Consumer<T> listener)` | Adds a listener. |
| `unregister(Consumer<T> listener)` | Takes it out again (pass the same instance). |

Listeners run in the order they were registered, and there are no priorities or return values. An event that can be
changed carries the change on the object it hands you: call `cancel()`, a setter, or `add` / `remove` on it. Every
listener sees the changes the ones before it made.

```java title="MyModSleepCompat.java"
import net.ixdarklord.whycantisleep.api.HidingEvents;
import net.ixdarklord.whycantisleep.api.SleepEvents;
import net.ixdarklord.whycantisleep.api.SleepTrouble;

public static void init() {
    SleepEvents.TROUBLE_CHANGED.register(changed -> {
        if (changed.worsened() && changed.now() == SleepTrouble.Level.SLEEPLESS) {
            changed.player().sendSystemMessage(Component.translatable("mymod.message.go_to_bed"));
        }
    });
    HidingEvents.START.register(start -> {
        if (start.player().hasEffect(MobEffects.GLOWING)) start.cancel();   // no hiding while glowing
    });
}
```

Register once, while your mod initialises, from the class only reached when the mod is loaded (see
[Calling it only when the mod is loaded](index.md#calling-it-only-when-the-mod-is-loaded)).

## SleepEvents

| Event | Carries | Fires when | Can |
| --- | --- | --- | --- |
| [`SLEEP_FAILED`](#sleep_failed) | `SleepFailed` | The game refused to let a player sleep. | read |
| [`LAY_DOWN`](#lay_down) | `ServerPlayer` | A player lay down in a bed and fell asleep. | read |
| [`TROUBLE_CHANGED`](#trouble_changed) | `TroubleChanged` | A player's sleep trouble level changed. | read |
| [`NIGHT_TERROR`](#night_terror) | `NightTerror` | A player who lay down is about to have a night terror. | cancel |
| [`SCORE_QUALITY`](#score_quality) | `QualityScoring` | A sleeper is waking and their night is being scored. | add and remove factors |
| [`SLEEP_EFFECT`](#sleep_effect) | `SleepEffectDrawn` | A sleep effect was drawn for a player waking in the morning. | swap, cancel |
| [`WOKE`](#woke) | `Woke` | A player woke up. | read |
| [`NIGHT_SKIPPED`](#night_skipped) | `NightSkipped` | Enough players slept that the night was skipped. | read |
| [`NIGHT_VOTED`](#night_voted) | `NightVoted` | The night was skipped by a sleep vote. | read |
| [`ROOM_SAFE`](#room_safe) | `RoomSafe` | A player dealt with every monster that blocked their sleep. | read |

### SLEEP_FAILED

```java
record SleepFailed(ServerPlayer player, BlockPos bed, Player.BedSleepingProblem problem, List<Mob> blockers, SleepTrouble trouble)
```

The game refused a player's sleep. `bed` is the bed's head and `problem` the game's own reason (`NOT_SAFE`,
`NOT_POSSIBLE_NOW`, `TOO_FAR_AWAY`, `OBSTRUCTED`, `NOT_POSSIBLE_HERE`). For `NOT_SAFE`, `blockers` is exactly the
monsters that kept them awake, with the mod's rules applied; for the rest it is empty. `trouble` already counts this
refusal. It doesn't fire when the player was already asleep or dead (the game's `OTHER_PROBLEM`).

```java
SleepEvents.SLEEP_FAILED.register(failed -> {
    if (failed.problem() == Player.BedSleepingProblem.NOT_SAFE) failed.blockers().forEach(Mob::setPersistenceRequired);
});
```

### LAY_DOWN

`Event<ServerPlayer>`: the listener is given the player itself. They lay down in a bed and fell asleep, so their
refusals in a row start over (the game resets the time since rest). It fires before `NIGHT_TERROR` for the same lying
down.

### TROUBLE_CHANGED

```java
record TroubleChanged(ServerPlayer player, SleepTrouble.Level before, SleepTrouble.Level now, SleepTrouble trouble)
```

A player's [`SleepTrouble.Level`](index.md#sleep-trouble) went from `before` to `now`, for better or worse;
`worsened()` says which. The level is checked when a sleep is refused, when a player lies down, and every 5 seconds
(so a day passing without sleep is noticed within 5 seconds). The first check after a player joins only sets where
they stand, and fires nothing.

### NIGHT_TERROR

```java
final class NightTerror { player(); trouble(); cancel(); isCancelled(); }
```

A player who just lay down is about to have a night terror: they were restless or worse and the server's terror chance
came up, or they were marked for one ([`markNightmare`](index.md#marknightmare), `/whycantisleep nightmare`).
`trouble()` is how badly they had been sleeping before they lay down. `cancel()` spares them: they sleep as usual.
A terror given at once with [`WhyCantISleepApi.nightmare`](index.md#nightmare) doesn't fire this event.

### SCORE_QUALITY

```java
final class QualityScoring {
    ServerPlayer player();
    WakeReason reason();
    List<SleepQuality.Factor> factors();
    void add(SleepQuality.Factor factor);
    void add(Identifier id, Component label, int points);
    void remove(Identifier id);
}
```

A sleeper is waking, for any reason, and every [sleep modifier](index.md#sleep-modifiers-in-code) has scored their
night. `factors()` holds what they made of it, then what listeners before you added. Add factors of your own, or remove
one by id: a mod that makes creepy music soothing removes `SleepQuality.CREEPY_MUSIC` and adds its own. The score is
`SleepQuality.BASE` plus the factors' points, held to 0 to 100, worked out once the listeners are done.

```java
SleepEvents.SCORE_QUALITY.register(scoring -> {
    if (MyBlocks.hasFeatherPillow(scoring.player())) {
        scoring.add(MyMod.id("feather_pillow"), Component.translatable("mymod.quality.feather_pillow"), 8);
    }
});
```

A factor's label is shown on the morning card, in Title Case. For something every night is scored by, a
[sleep modifier](index.md#sleep-modifiers-in-code) is simpler; this event is for the last word.

### SLEEP_EFFECT

```java
final class SleepEffectDrawn { player(); quality(); effect(); setEffect(SleepEffect effect); cancel(); isCancelled(); }
```

A player woke in the morning, the server's sleep effect chance came up, and an effect was drawn for the night's
`quality()`. `setEffect` gives another effect instead; `cancel()` gives none. It doesn't fire after waking early, or
when no effect fits the score. The effect is applied once the listeners are done, before `WOKE`.

### WOKE

```java
record Woke(ServerPlayer player, WakeReason reason, SleepQuality quality, int sleptTicks, boolean shaken, boolean wellRested,
            @Nullable SleepEffect sleepEffect)
```

A player woke up, for any [`WakeReason`](index.md#sleep-quality). `quality` is the night's final score (after
`SCORE_QUALITY`), `sleptTicks` how long they lay asleep, `shaken` whether they had a night terror, `wellRested` whether
the bedtime routine earned them Regeneration, and `sleepEffect` the effect they were given, or `null`. It fires even
when the server's sleep quality option is off (the card just isn't shown).

```java
SleepEvents.WOKE.register(woke -> {
    if (woke.reason().rested() && woke.quality().score() >= 90) MyRewards.giveDreamcatcher(woke.player());
});
```

### NIGHT_SKIPPED

```java
record NightSkipped(ServerLevel level, List<ServerPlayer> sleepers)
```

The game is skipping the night in a level, by sleep or by vote; `sleepers` are those asleep, about to wake. `WOKE`
fires for each of them after it.

### NIGHT_VOTED

```java
record NightVoted(ServerLevel level, List<ServerPlayer> sleepers)
```

The players in bed voted to skip the night, and those still up let them (or didn't answer in time). It fires after
the night was skipped: after `NIGHT_SKIPPED` and the sleepers' `WOKE`. `sleepers` are those who were in bed.

### ROOM_SAFE

```java
record RoomSafe(ServerPlayer player, BlockPos bed, int cleared)
```

Every monster that blocked a player's sleep was dealt with inside the server's clearing time, and nothing else came
near: the room is safe. `cleared` is how many there were. Only with the server's room clearing on. It fires before the
player, if standing by the bed, lies straight down.

## HidingEvents

| Event | Carries | Fires when | Can |
| --- | --- | --- | --- |
| [`START`](#start) | `HideStart` | A player is about to hide. | cancel, change the spot |
| [`STOP`](#stop) | `HideStop` | A player stopped hiding. | read |
| [`SHAKEN_OFF`](#shaken_off) | `ShakenOff` | A player stayed hidden for their hide time. | keep or add hunters |
| [`NOISE_CHANGED`](#noise_changed) | `NoiseChanged` | A hiding player's noise changed. | read |

Entity types in `#whycantisleep:ignores_hiding` (the Wither, the Ender Dragon, the Warden, Elder Guardians) see
through hiding: they keep a hiding player as their target, are never counted as hunters and never vanish.

### START

```java
final class HideStart { player(); spot(); setSpot(HidingSpot spot); cancel(); isCancelled(); }
```

A player is about to get into a hiding spot, by sneak-using it or through
[`WhyCantISleepApi.startHiding`](index.md#starthiding). `spot()` is where. `cancel()` keeps them out (a sneak-use then
does nothing). `setSpot` changes what they get into: another squint (`spot.withSquint(0.8F)`), another hide time scale,
another place to be held.

It fires before the spot is checked for room, so a player who isn't cancelled may still not get in: every place in it
taken, or something already in there that chases them out (the critter chance). A sneak-use too soon after hiding is
refused for the cooldown before this fires.

```java
HidingEvents.START.register(start -> {
    // Deeper in the dark: a tighter squint in the Nether.
    if (start.player().level().dimension() == Level.NETHER) start.setSpot(start.spot().withSquint(0.75F));
});
```

### STOP

```java
record HideStop(ServerPlayer player, HidingSpot spot, HidingEvents.Reason reason, int hidTicks)
```

A player is out of their spot, back where they went in, and the hide cooldown has started. `spot` is where they hid,
`hidTicks` how long. The reason:

| `Reason` | Why |
| --- | --- |
| `LEFT` | They sneaked to come out. |
| `HURT` | They took damage. |
| `INTERRUPTED` | The spot stopped being one (broken, its floor gone), the server turned hiding off, or they died, slept, left or changed dimension. |
| `HEARD` | Their noise reached 1 with monsters near, and they were found. |
| `FOUND` | A monster that chased them in searched the spot and found them. |
| `DISCOVERED` | Another player opened the chest or closet door they hid behind. |
| `FORCED` | A mod brought them out with [`stopHiding`](index.md#stophiding). |
| `STARTLED` | Something was already in the spot. Declared, but not fired at present: a startled player never gets in, so they never come out. |

### SHAKEN_OFF

```java
final class ShakenOff { player(); spot(); List<Entity> hunters(); keep(Entity entity); add(Entity entity); }
```

A player stayed hidden for their [hide time](index.md#hideticks), and what was after them gives up. `hunters()` holds
what is about to vanish: the monsters that were targeting them as they hid, still within 48 blocks, and the monsters
within 12 blocks of the spot. Persistent mobs (named, tamed, kept), players and `#whycantisleep:ignores_hiding` are
never in it. When the listeners are done, every entity left in the list that is still alive vanishes in a puff of
smoke.

`keep(entity)` leaves one alone, to deal with it your own way; `add(entity)` sends one of yours with the rest.

```java
HidingEvents.SHAKEN_OFF.register(shaken -> {
    for (Entity hunter : List.copyOf(shaken.hunters())) {
        if (hunter instanceof MyStalker stalker) {
            shaken.keep(stalker);                     // not discarded: it slinks off instead
            stalker.loseTrackOf(shaken.player());
        }
    }
});
```

Iterate over a copy when you `keep` inside the loop, since it takes from the list you are walking. The player stays
hidden afterwards. It fires only when the server's `hidingClearsMonsters` is on and every other player within 32 blocks
is hiding too; otherwise the monsters only lose the player and stay.

### NOISE_CHANGED

```java
record NoiseChanged(ServerPlayer player, HidingSpot spot, float noise, float previous)
```

The noise a hiding player makes changed, from `previous` to `noise`, 0 to 1 in steps of a tenth, as their client
reports it: trying to move, looking about, gasping when they fail the heartbeat minigame. At 1, with monsters near and
the server's `noiseGivesAway` on, they are found and `STOP` follows with `HEARD`.
