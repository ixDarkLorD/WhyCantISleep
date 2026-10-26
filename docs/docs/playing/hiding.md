# Hiding

When something is after you at night, you don't always have to fight. You can crawl under a bed or a table, climb into
a double chest or shut yourself in a closet, and wait. Hidden, nothing can see you. Stay hidden long enough and what
was hunting you gives up, and is gone. Make a sound, though, and it will find you.

Hiding needs the mod on the server.

## How to hide

**Sneak (++shift++) and use (right-click) a hiding spot with both hands empty.** You're held in place, the view
narrows, and a card above the hotbar says where you are: **Hiding under the bed**, with "All quiet out there. Sneak to
come out" under it.

To come out, **sneak again**. You also come out if you're hurt, if the spot is broken, if you die, sleep, change
dimension or leave the game, or if the server turns hiding off. You come out where you went in; out of a chest or a
closet, you step out in front of it.

While hidden you can look around but not move, and you can't use or break anything, not even the bed over your head.
Left clicks never swing at anything (they belong to the [heartbeat minigame](#the-heartbeat-minigame)), the crosshair
fades away, and your hand is hidden.

## The hiding spots

| Spot | What it needs | You are |
| --- | --- | --- |
| **Under a bed** | A solid floor under both halves of the bed. | Lying flat, halfway along it. |
| **Under a table** | Two top slabs or upside-down stairs side by side, carpet on both, and a solid floor under both. | Lying flat under it. |
| **In a double chest** | A double chest. A single one is "too small to climb into". | Crouched, peering out under the lid. |
| **In a closet** | A door with a nook behind it (see below). | Standing, facing the door. |

### Beds and tables

A bed over air answers "There's no floor under this bed to hide on." The same goes for a table: both halves need
something solid under them. For a table you can use the slab, the carpet on it, or (crouched in front of it) the floor
right under or just past it.

### Chests

Inside a chest, the lid stays raised a crack so you can see out. You can only turn your view so far from the chest's
front: 50 degrees to either side, a little up and down. Other players see just the top of your head and your eyes in
the gap.

A double chest has room for two: one in each half.

### Closets

A closet is a door, or a double door, with a **nook behind it**: one block wide per door and two blocks high, shut in
on every other side, with room to stand. "Shut in" means every face of the nook is covered: by full blocks, or by
**trapdoors** lying flush against it (an open trapdoor works as a wall, a shut one as a roof or a floor), either in the
next block or in the nook itself. A carpet on the nook's floor is fine.

The nook is on the side you placed the door from, so the easiest way to build one is to stand inside it and place the
door in front of you. The door swings shut behind you when you go in.

Behind a **double door**, the closet is two wide and holds two players, one behind each door.

!!! warning "Someone could open it"
    If another player opens the chest or the closet door you're hiding behind, they find you: "Someone opened it and
    found you!" Beds and tables have nothing to open.

The server can turn each kind of spot off, and data packs can add new ones ([Hiding spots](../datapacks/hiding-spots.md)).

## Out of sight

While you hide, **nothing can take you as a target**, and anything already after you loses you. The only exceptions
are bosses, which find you as easily as anywhere: the Wither, the Ender Dragon, the Warden and Elder Guardians. These
are the `#whycantisleep:ignores_hiding` entity tag, which data packs and other mods can add to ([Data packs](../datapacks/index.md)).
Remember that being hurt brings you out, so hiding from a boss doesn't help for long.

When a monster is within 12 blocks, you tremble, and the card turns red: **Something's out there** "Stay still and
don't make a sound". Its foot line then counts down the time left until they give up.

## Shaking them off

Stay hidden for the **hide time** (15 seconds by default) and the monsters give up. Then:

- the monsters that were after you (within 48 blocks of you), and
- any monsters prowling within 12 blocks of the spot

vanish in a puff of smoke, and you're told "It's quiet now. Whatever was out there is gone." If nothing came looking
for you at all: "All quiet. Nothing came looking for you." You stay hidden until you sneak out.

Some monsters never vanish: anything named, tamed or otherwise kept from despawning, and anything in the
`#whycantisleep:ignores_hiding` tag.

!!! note "Everyone has to hide"
    Monsters only vanish if nobody else is out in the open to see it. With another player within 32 blocks who isn't
    hiding, they just lose you: "They lost you, but they're still out there: everyone nearby must hide too." The server
    can also turn the vanishing off, in which case they always just lose you.

### The hide time

The hide time depends on the spot and on what's around it:

- **Each spot** takes its own share: the full time under a bed or a table, 90% in a chest, 85% in a closet.
- **Cover**: each side of the spot closed off by a full block (not the spot's own blocks) takes 10% off, for up to
  three sides. A bed tucked into a corner shakes them off sooner.

The hide time is never less than one second. The server sets the base time (3 to 120 seconds) and can turn cover off.

## Eyes and ears

Hiding, your **eyes narrow**: dark eyelids close in from above and below, leaving an almond of view. How far depends on
the spot: a little in a closet, half under a bed or table, to a slit in a chest. They sway with your breath, blink now
and then, and squint tighter while monsters are near. You can pick the eyelids' look in the client settings: retro
(the default), soft, banded or grainy.

**Sounds come muffled**, as much as the spot shuts them out. Air lets sound through, glass a little, a solid block or
wool almost nothing, a door or trapdoor as much as its window lets through. In a chest only the crack under the lid is
open. A sound from the open side of your spot comes through clearer than one from behind a wall. Your own sounds (your
heartbeat) aren't muffled.

You also **hear the monsters prowling** within 10 blocks: muffled footsteps on whatever they walk on, and shuffling
when they stand about. Lying under a bed or table, you see a puff of shadow where each step lands nearby.

You can turn the muffling off in the client settings.

## Noise

Monsters can't see you, but they can hear you. Trying to walk or jump, or swinging your view about, makes noise; it
fades again while you keep still. When the noise builds up with monsters near, the card says **Hold still!** "They'll
hear you. Don't move until they're gone", and its foot line shows how much noise you're making.

If the noise reaches its peak while something can hear it (a monster that was after you, or a monster within 12
blocks of the spot), **it heard you**: you're pulled out, and they all come for you at once. The server can turn this
off.

## Monsters searching

When you run into a spot with something on your heels, the nearest of your hunters comes looking. After a moment it
walks up to the front of your spot, stops, and looks around (now and then right at the spot). Then it either finds you
and drags you out ("It found you!"), or walks off.

The chance it finds you is the server's search chance, **25%** on Normal difficulty, half that on Easy, half as much
again on Hard and none on Peaceful, plus more the more noise you're making. While a search is under way, the hide time
waits for it to finish.

While you're playing the heartbeat minigame, the search **never** finds you by chance: only failing the minigame gives
you away.

## The heartbeat minigame

If you hide with something hunting you, you have to keep your heart slow. A warning slams in ("Watch out!" or "Be
careful!"), the rest of the HUD fades, and a band of smoke opens across the screen with a pulse line through it.

- While a monster is within 12 blocks, heartbeats come one after another, each crossing the line from right to left.
- **Left-click as the beat passes through the calm window in the middle.** Each caught beat chimes a little higher
  through a streak ("Steady...", "Doing great!", "Cool as ice!").
- Clicking too early or too late, or letting a beat pass, is a **strike** and breaks one of your three hearts. Your
  second strike brings "Last chance!".
- At the third strike **you gasp**, as loud as you can be. Anything near enough to hear (as for [noise](#noise))
  finds you.
- When the hide time is up, it ends: "You held your nerve!"

While it runs, your movements make no noise: only your heart can give you away. A dark red edge and veins creep in from
the screen's edges with each strike, and fall back as a streak of caught beats calms you.

**Overtime**: if you missed the last beat when the hide time runs out, and monsters are still near, you get "One more
beat!": the hide time stretches for one more beat, which you must catch. This can happen at most twice. Catching an
overtime beat ends the minigame at once.

The server can turn the minigame off, and hunted players then just hide as anyone else does.

## Critters

Now and then, something got there first. There's a **5%** chance a silverfish or a cave spider is already in the spot:
it scurries out and leaps at you, and you don't get in ("Something was already in there!"). If a monster is sitting in
the spot, you're told "Something's in there."

## The hide cooldown

After coming out of a spot, however it happened, you have to wait before hiding again: **10 seconds** by default. Try
sooner and a gold card says **Still catching your breath**, "You can hide again in 7s", its foot line running down
until you can. (Without the mod on your client, you get the seconds above the hotbar instead.) The server can change
the cooldown (up to 300 seconds) or turn it off with 0.

## Simple Voice Chat { #voice-chat }

With Simple Voice Chat installed ([Compatibility](../compatibility.md)), hiding hears your voice too.

- **Your voice is noise.** Speaking while hidden adds to your [noise](#noise), as loud as you speak, even during the
  heartbeat minigame. Steady talking gives you away in about three seconds with a monster near, a shout in about one
  and a half.
- **Whispering is the safe way to talk.** Simple Voice Chat's whisper counts for much less, and a normal whisper never
  builds up to being heard.
- **Others hear you muffled.** A hiding player's voice reaches everyone else as their spot lets it out: through a
  chest's lid crack, a closet door's window, the open sides under a bed. If you're hiding yourself, you hear every
  voice through your own spot as well.

The server can stop voices from giving hiders away ([Configuration](../server/configuration.md)).
