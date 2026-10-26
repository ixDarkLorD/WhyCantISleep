# Sleep quality

Getting into bed is only half of it. With the mod on the server, every night you sleep is followed from lying down to
waking and scored for how well you slept: the room, the light, the monsters about, the music, the things by your bed
and how long you'd gone without sleep. A good night can leave you well rested or with a lucky dream; a bad one can end
in a night terror.

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

Data packs can change these factors, take them out or add new ones ([Sleep modifiers](../../docs/datapacks/sleep-modifiers.md)).

## The room

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

## Things by the bed

Blocks placed within 6 blocks of the bed (and 3 up or down) can soothe or unsettle you:

- **Soothing** (+2 each, up to +10): flowers, candles, amethyst, moss, azaleas, spore blossoms, lily pads, bookshelves
  and cake.
- **Unsettling** (−3 each, up to −15): skulls and heads, cobwebs, sculk, spawners, wither roses, bone blocks, soul
  lanterns, torches and campfires, and, where they exist, creaking hearts, pale hanging moss and open eyeblossoms.

A flower in a pot counts as the flower. The lists are the `#whycantisleep:soothing` and `#whycantisleep:unsettling`
item tags, so data packs can change them ([Data packs](../../docs/datapacks/index.md)).

These items say so in their tooltip: a closing section with a buff or debuff icon, **Soothing** or **Unsettling**, the
points each one is worth, how far they stack, and a meter with one pip for each that counts.

## Music

A jukebox playing within 16 blocks of the bed is heard through the night. Songs come in three moods:

| Mood | Songs |
| --- | --- |
| Creepy (−20) | 11, 13, ward, 5, mellohi |
| Loud (−15) | Pigstep, otherside, blocks, Creator, Lava Chicken |
| Calm (+10) | cat, chirp, far, mall, stal, strad, wait, Creator (Music Box), Precipice, Tears |

Any other song doesn't count. If several jukeboxes play at once, the worst mood is the one heard; a creepy song and a
loud one at different times in the night both count. A calm song only helps if nothing creepy or loud played all
night. The moods are jukebox song tags ([Data packs](../../docs/datapacks/index.md)).

## The morning card

When you wake in the morning, a card tells you how you slept: **Great Sleep** (85 and up), **Good** (65), **Fair**
(45), **Poor** (25) or **Awful**. It shows the score as a meter, and the three things that mattered most with their
points ("+2 more" if there were others), and the [sleep effect](dreams.md) you woke with, if any.

Woken early, the card says why instead: **Something Woke You!** if you were hurt, **Your Bed Is Gone** if it was
broken. Getting up yourself shows no card. You can turn the morning card off in the client settings.

## Well rested

Sleep till morning in a closed, lit room (the bedtime routine) and you wake **well rested**: Regeneration for 30
seconds, and "Well rested! Your bedtime routine paid off." on the card.
