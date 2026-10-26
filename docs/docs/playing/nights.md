# Nights

Getting into bed is only half of it. With the mod on the server, every night you sleep is followed from lying down to
waking and scored for how well you slept: the room, the light, the monsters about, the music, the things by your bed
and how long you'd gone without sleep. A good night can leave you well rested or with a lucky dream; a bad one can end
in a night terror.

## Sleep quality

Every night starts at **75** and is moved up or down by what it was like, then held between 0 and 100. These are the
factors, with the names the morning card gives them:

| Factor | Points | When |
| --- | --- | --- |
| **Behind Closed Doors** | +10 | The room is closed (see [The room](#the-room)). |
| **Out in the Open** | −5 | The room isn't closed. |
| **Unlit Corners** | −10 | The room is closed, but some of it has no light at all. |
| **Good Bedtime Routine** | +10 | You slept till morning in a closed, fully lit room. |
| **Light in Your Eyes** | −5 | A bright light right at the pillow (block light 13 or more). |
| **Prowling Monsters** | −6 each, −30 at most | Monsters within 24 blocks of the bed, counted at their most during the night. |
| **Calming Things Nearby** | +2 each, +10 at most | Soothing things by the bed (see [Things by the bed](#things-by-the-bed)). |
| **Creepy Things Nearby** | −3 each, −15 at most | Unsettling things by the bed. |
| **Eerie Music** | −20 | A creepy song played near the bed. |
| **Blaring Music** | −15 | A loud song played near the bed. |
| **Soothing Melody** | +10 | A calm song played, and nothing creepy or loud all night. |
| **Restless Mind** | −10 | You were restless when you lay down. |
| **Days Without Sleep** | −20 | You were sleepless when you lay down. |
| **Haunting Nightmare** | −15 | You had a night terror. |
| **Woke Before Dawn** | −25 | You got up, or were woken, before morning. |

So a closed, lit room slept in till morning, with nothing else going on, scores 95.

Data packs can change these factors, take them out or add new ones ([Sleep modifiers](../datapacks/sleep-modifiers.md)).

### Restless and sleepless

How badly you've been sleeping comes in three levels:

- **Rested**: you're sleeping well.
- **Restless**: 2 refused sleeps in a row, or 2 days without lying down. Night terrors can find you.
- **Sleepless**: 5 refused sleeps in a row, or 3 days without lying down. Phantoms are coming.

Any refused try counts, whatever the reason. Lying down in a bed starts both counts over.

### The room

The room is worked out when you lie down, by flooding the air from the space above the bed's head: through anything you
could walk through, and through open doors, trapdoors and fence gates, but not through walls or shut doors. The room
is **closed** if the flood:

- never reaches a block that can see the sky,
- stays under 600 blocks, and
- doesn't stretch more than 24 blocks from the bed (counting across, along and up together).

A closed room is **lit** if every air block in it has some block light. A dark corner is somewhere a monster could
spawn, and costs you.

!!! tip "The bedtime routine"
    Shut the door, light every corner, and keep the torch off the pillow. That's the routine: +10 for the closed room,
    +10 for the routine, and you wake [well rested](#well-rested).

### Things by the bed

Blocks placed within 6 blocks of the bed (and 3 up or down) can soothe or unsettle you:

- **Soothing** (+2 each, up to +10): flowers, candles, amethyst, moss, azaleas, spore blossoms, lily pads, bookshelves
  and cake.
- **Unsettling** (−3 each, up to −15): skulls and heads, cobwebs, sculk, spawners, wither roses, bone blocks, soul
  lanterns, torches and campfires, and, where they exist, creaking hearts, pale hanging moss and open eyeblossoms.

A flower in a pot counts as the flower. The lists are the `#whycantisleep:soothing` and `#whycantisleep:unsettling`
item tags, so data packs can change them ([Data packs](../datapacks/index.md)).

These items say so in their tooltip: a closing section with a buff or debuff icon, **Soothing** or **Unsettling**, the
points each one is worth, how far they stack, and a meter with one pip for each that counts.

### Music

A jukebox playing within 16 blocks of the bed is heard through the night. Songs come in three moods:

| Mood | Songs |
| --- | --- |
| Creepy (−20) | 11, 13, ward, 5, mellohi |
| Loud (−15) | Pigstep, otherside, blocks, Creator, Lava Chicken |
| Calm (+10) | cat, chirp, far, mall, stal, strad, wait, Creator (Music Box), Precipice, Tears |

Any other song doesn't count. If several jukeboxes play at once, the worst mood is the one heard; a creepy song and a
loud one at different times in the night both count. A calm song only helps if nothing creepy or loud played all
night. The moods are jukebox song tags ([Data packs](../datapacks/index.md)).

## The morning card

When you wake in the morning, a card tells you how you slept: **Great Sleep** (85 and up), **Good** (65), **Fair**
(45), **Poor** (25) or **Awful**. It shows the score as a meter, and the three things that mattered most with their
points ("+2 more" if there were others), and the [sleep effect](#sleep-effects) you woke with, if any.

Woken early, the card says why instead: **Something Woke You!** if you were hurt, **Your Bed Is Gone** if it was
broken. Getting up yourself shows no card. You can turn the morning card off in the client settings.

## Well rested

Sleep till morning in a closed, lit room (the bedtime routine) and you wake **well rested**: Regeneration for 30
seconds, and "Well rested! Your bedtime routine paid off." on the card.

## Sleep effects

Waking in the morning, there's a **50%** chance (the server's setting) you wake with a sleep effect. There are 22:

- **Positive** (9), likelier the better you slept: Early Bird, Refreshed, Sweet Dreams, Clear Mind, Dreamt of a Feast,
  Dreamt of Victory, Lucky Dream, Heroic Dream and Dream of Flight.
- **Negative** (8), likelier the worse you slept: Groggy, Stiff Back, Woke Up Starving, Pounding Headache, Weak Limbs,
  Bleary Eyes, Haunted and Cursed Dream.
- **Neutral** (5), possible after any night: Night Owl, Vivid Dream, Dream of Fire, Dream of the Sea and Restless Legs.

First the kind is drawn: positive weighs your score minus 40, negative 60 minus your score, and neutral always 20. So a
score of 95 is mostly positive with a few neutral, and 20 mostly negative. Then one effect of that kind is drawn by
its **rarity**, among those whose quality range fits your score (Dream of Flight needs 90 or more, Cursed Dream 20 or
less):

| Rarity | Weight |
| --- | --- |
| Common | 60 |
| Uncommon | 25 |
| Rare | 10 |
| Epic | 4 |
| Legendary | 1 |

Each effect gives one or more of the game's potion effects, and is shown on the card and in the sleep status, named in
its rarity's color with a buff, debuff or neutral icon. Data packs can add more ([Sleep effects](../datapacks/sleep-effects.md)).

## Eyes in bed

Lying down, your eyelids grow heavy, drooping half shut with slow, heavy blinks. Once everyone the night needs is
asleep, they close, fighting it once, and the world's sounds sink away with them. Waking from a full night, they flutter
open. The muffling is the same client setting as for hiding.

## Night terrors

When you're **restless** and lie down, there's a **30%** chance of a night terror, twice that (60%) when you're
**sleepless**.

It's sleep paralysis. You can't move, your eyes are stuck half open, the world goes muffled and warped, and a red-black
fog rolls in. Something forms out of the shadow at the foot of the bed and comes along it towards you, while shadow
hands rise beside the bed, claw, and close in round your view. It lasts 7 to 12 seconds, and ends with a shriek as your
eyes slam shut. The night waits for it to finish.

How bad it is depends on how badly the night was going to go (scored as if you'd slept through it), and is worse
sleepless. It sets how long it lasts, how many hands come, how close the shadow gets and how loud it all is.

You wake **shaken**: a purple card ("You Wake Up Shaken", "Bad dreams have left you unsteady.") with Nausea and
Slowness for a few seconds, and −15 on the night's score.

### Harsh terrors

With harsh terrors on (the default), a terror grows with the days you've stayed awake: worse by 0.1 for each day past
the first, up to 0.3. Waking from a bad one leaves more than Nausea and Slowness: longer effects, then Slowness II,
Weakness, Mining Fatigue and a moment of Darkness as it gets worse. From **3 days awake**, the terror also hurts, up to
5 hearts, but never below half a heart.

Server operators can hand out a nightmare with a command ([Commands](../server/commands.md)), and the server can change
the chance or turn terrors and harsh terrors off ([Configuration](../server/configuration.md)).

## The sleep debt moon

Once you've gone a whole day without lying down, a small moon appears in the top left of the screen. It fills from the
bottom with each day, the days written beside it, and is full (and pulsing) at three days, when phantoms come. It pops
larger for a moment when it appears and each time another day passes.

The moon keeps out of the way of the rest of the HUD, whichever mod drew it (a minimap, a tooltip like Jade's, boss
bars): it glides clear of them. You can move it to another corner (top left, top center, top right, bottom left,
bottom right), nudge it by a number of pixels, or turn it off in the client settings.

## Sleep status

In your survival inventory, a small moon button sits in the corner of your player preview. Its rim is the color of
your sleep trouble, and its face looks tired or sad when you've been sleeping badly. Click it for a popup:

- your level (Rested, Restless or Sleepless) with a word on what it means,
- **Awake For**: how long since you last lay down, with the sleep debt moon as a meter,
- **Refused Sleeps in a Row**,
- **Last Night**: its score, what mattered most, the sleep effect, and whether you woke shaken, well rested or early.

Last night is remembered for the session. The button moves out of the way of anything another mod puts in that
corner, and can be turned off in the client settings.

## The guide book

The **Insomniac's Handbook**, by The Sleepless Knight, is the mod's guide book. You're given one the first time you
join a world (the server can turn this off). If you lose it, craft another from **a book and any bed**. It has its own
tab in the creative menu.

It covers sleeping, hiding, sleep quality and dreams, with live lists of the sleep effects, the factors, the bedtime
songs and the soothing and unsettling things, and two quests: the **Bedtime Checklist** and **Hide and Seek**. The
book needs Folios (see [Compatibility](../compatibility.md)).
