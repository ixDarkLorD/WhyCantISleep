# Monsters in the way

The game won't let you sleep while a monster is within 8 blocks of the bed's head sideways and 5 blocks up or down. When
that happens, the mod shows you **exactly the monsters the game found**, no more and no fewer.

The card names them: **3 monsters nearby**, over **Closest: Zombie, 6 blocks below**. The closest one is described as
"above" or "below" when it is at least 2 blocks up or down and more up or down than it is to the side; otherwise it
is "away". If you'd rather see the plain explanation ("Monsters are nearby"), turn off the action bar info in the
client settings.

## Highlights

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

## Arrows and rings

Monsters you can't see get a pointer:

- A monster **outside your view** gets an arrow around the crosshair, pointing its way. Nearer monsters get larger
  arrows, and arrows that would overlap are fanned apart. The arrows come in three sizes (small, medium, large) in the
  client settings.
- A monster **in your view but behind blocks** gets a pulsing ring where it is on the screen, like a sound you can
  hear but not see.

A monster in plain sight needs neither: its glow is enough.

## Footprints

While a monster is highlighted, each step it takes leaves a print in its highlight color. Once the glow is gone, the
prints stay as faint specks of dust on the ground for up to **15 seconds**, fading as they go, so you can still tell
where it went. You can turn the trails off in the client settings.

!!! note "Which mobs count"
    The game decides which monsters block sleep; the mod only shows you its answer. A data pack can change it with the
    `#whycantisleep:blocks_sleep` and `#whycantisleep:does_not_block_sleep` entity tags (both empty by default). See
    [Data packs](../../docs/datapacks/index.md). In multiplayer, a bed right next to someone who is already asleep lets a
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
  ([Configuration](../../docs/server/configuration.md)).
