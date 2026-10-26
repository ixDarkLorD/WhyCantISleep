# Why can't I sleep?

When the game refuses to let you sleep, it usually tells you very little. This mod answers the question properly: it
shows you the monsters that kept you up, points to the ones you can't see, explains every failed sleep with a hint on
what to do about it, and warns you before you use a bed that would explode. If you deal with the monsters quickly, it
tells you the room is safe and puts you straight to bed.

## Sleep cards

The mod doesn't use the game's short line above the hotbar for sleep messages. Each one becomes a **card** instead: a
small dark panel just above the hotbar, with an icon, a title in the card's color, and a hint underneath. A thin line
along the card's foot runs down as its time runs out. Every card plays a soft sound as it appears; the alarming ones
(monsters, the dimension warning) also wash the screen briefly in red.

These are all the reasons a sleep can fail, and what the card says:

| Card | Hint | When |
| --- | --- | --- |
| **Monsters are nearby** | Defeat them, or block them out and light the area | Monsters are too close to the bed (see [Monsters in the way](#monsters-in-the-way)). |
| **It's not time to sleep** | Beds work at night and during thunderstorms | You tried during the day. |
| **You're too far from the bed** | Stand next to the bed | You're out of reach. |
| **The bed is obstructed** | Clear the blocks right above it | Something is blocking the space above the bed. |
| **Someone is already in this bed** | Use another bed | Another player is lying in it. |
| **2 of 3 players asleep** | 1 more player must sleep to skip the night | You're in bed, but not enough players are. |
| **Sleeping can't skip this night** | The world has sleeping through the night turned off | The world won't let a night be skipped. |

Monster cards are red, the "you did something wrong" ones (time, distance, obstruction, occupied) yellow, the player
count aqua.

!!! tip
    The cards work on any server, even one without the mod: the mod recognises the game's own messages. A server with
    the mod gives you more, though: the exact monsters, room clearing, and everything on the other pages.

## Monsters in the way

The game won't let you sleep while a monster is within 8 blocks of the bed's head sideways and 5 blocks up or down. When
that happens, the mod shows you **exactly the monsters the game found**, no more and no fewer.

The card names them: **3 monsters nearby**, over **Closest: Zombie, 6 blocks below**. The closest one is described as
"above" or "below" when it is at least 2 blocks up or down and more up or down than it is to the side; otherwise it
is "away". If you'd rather see the plain explanation ("Monsters are nearby"), turn off the action bar info in the
client settings.

### Highlights

Each blocking monster glows with an outline that only you can see, through walls and all, for **3 seconds** by
default (the server sets this, from 1 to 10 seconds). A dark mist pools round its feet while it glows, with embers
rising off it and a glint of eyes now and then.

With threat colors on (the default), the outline's color says what kind of threat it is:

| Color | Threat | Danger |
| --- | --- | --- |
| Green | Creepers | 5 |
| Magenta | Endermen and endermites | 4 |
| Blue | Raiders and illagers | 4 |
| Red | Undead (zombies, skeletons...) | 3 |
| Purple | Arthropods (spiders, silverfish...) | 2 |
| Orange | Anything else | 2 |

With threat colors off, every outline uses one color of your choice (red by default).

**The top threat**: of all the monsters in the way, the most dangerous kind (and of those, the nearest) stands out.
Its outline is brighter and throbs, and its arrow or ring is larger. Deal with that one first.

### Arrows and rings

Monsters you can't see get a pointer:

- A monster **outside your view** gets an arrow around the crosshair, pointing its way. Nearer monsters get larger
  arrows, and arrows that would overlap are fanned apart. The arrows come in three sizes (small, medium, large) in the
  client settings.
- A monster **in your view but behind blocks** gets a pulsing ring where it is on the screen, like a sound you can
  hear but not see.

A monster in plain sight needs neither: its glow is enough.

### Footprints

While a monster is highlighted, each step it takes leaves a print in its highlight color. Once the glow is gone, the
prints stay as faint specks of dust on the ground for up to **15 seconds**, fading as they go, so you can still tell
where it went. You can turn the trails off in the client settings.

!!! note "Which mobs count"
    The game decides which monsters block sleep; the mod only shows you its answer. A data pack can change it with the
    `#whycantisleep:blocks_sleep` and `#whycantisleep:does_not_block_sleep` entity tags (both empty by default). See
    [Data packs](../datapacks/index.md). In multiplayer, a bed right next to someone who is already asleep lets a
    couple of monsters by: see [Sleeping together](together.md#shared-beds).

## The room is safe

On a server with the mod, a refused sleep starts a clock. You have **10 seconds** by default to deal with every
monster that blocked you. Each one has to be gone, not just further away: kill it, or otherwise get rid of it.

If all of them are gone in time and nothing else has wandered in, a green card says **The room is safe**: "Nothing left
out there. Sleep now". If you're still standing by the bed (within 3.5 blocks), you don't even need to click it again:
you lie straight down, and the card says "Nothing left out there. Off to bed".

A few things to know:

- A dying monster still counts for the game until it is gone, so the card waits for the last one to finish dying.
- If a new monster comes close in the meantime, there's no card: try the bed again to see what's there now.
- The server can change the time (3 to 60 seconds), or turn off room clearing or the automatic lie-down
  ([Configuration](../server/configuration.md)).

## The dimension warning

In the Nether and the End, a bed doesn't let you sleep: it explodes. The first time you use a bed in a dimension where
beds explode, nothing happens except a red card: **Beds explode here!** "Use it again to do it anyway".

If you really mean it (beds make good explosives), let go of the use button and use the bed again within 3 seconds.
Holding the button down doesn't count as a second use. Sneaking with an item in your hand uses the item, not the bed,
so that never warns.

The warning is a client setting, and works on any server.

## Settings

All of this can be turned on and off. Your own client settings cover the highlights, threat colors and highlight
color, the trails, the arrows and their size, the action bar info, the sleep explanations and the dimension warning.
The server can switch off highlights and arrows for everyone, change the highlight duration, room clearing and auto
sleep, and show highlights to players without the mod (using the game's own glowing effect, which everyone nearby
sees). See [Configuration](../server/configuration.md).
