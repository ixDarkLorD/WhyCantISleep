# Configuration

Why Can't I Sleep? has two config files. **Client** settings change what *you* see and hear; **server** settings decide
what everyone on a server gets. The server's settings are sent to every player when they join and whenever they
change, and a feature the server turns off stays off for everyone: its client settings show greyed out.

With [Glazed Menu](https://modrinth.com/mod/glazed-menu) installed, both are edited in game from the **Mods** menu
(Mod Menu on Fabric). Otherwise, edit the files in the game's `config` folder; they reload when saved.

| File | Holds |
| --- | --- |
| `config/whycantisleep-client.toml` | Client settings, for your own game |
| `config/whycantisleep-server.toml` | Server settings, for everyone on the server (and in singleplayer) |

## Client settings

Your own settings. Some only show while the server allows the feature behind them.

### Highlights

Highlighting the monsters that blocked your sleep.

| Option | Key | Default | What it does |
| --- | --- | --- | --- |
| Highlight monsters | `highlightMonsters` | On | Makes the monsters that blocked your sleep glow. |
| Threat colors | `threatColors` | On | Colors each highlight by the kind of threat. Off, every highlight uses the highlight color. |
| Highlight color | `highlightColor` | `#FF5555` | The highlight color when threat colors are off. |
| Blocker trails | `blockerTrails` | On | Leaves faint footprints where the blocking monsters walked, for a while after their highlight is gone. |

### HUD

Arrows around the crosshair, the action bar text, and the eyelids while hiding.

| Option | Key | Default | What it does |
| --- | --- | --- | --- |
| Off-screen arrows | `offScreenArrows` | On | Draws arrows around the crosshair pointing to blocking monsters that are off-screen or behind walls. |
| Arrow size | `arrowSize` | Medium | How large the arrows are. |
| Eyelid style | `eyelidStyle` | Retro | How the eyelids look while you hide: soft, banded, grainy, or retro pixel art (banded and grainy together). |
| Sleep debt moon | `sleepDebtMoon` | On | Shows a moon on the screen that fills up with each day you go without sleep. Needs the mod on the server. |
| Sleep debt moon position | `debtMoonPosition` | Top left | Where on the screen the sleep debt moon sits. |
| Sleep debt moon offset (across) | `debtMoonOffsetX` | 0 (-200 – 200) | Moves the sleep debt moon this many pixels right of its place (left when negative). |
| Sleep debt moon offset (down) | `debtMoonOffsetY` | 0 (-200 – 200) | Moves the sleep debt moon this many pixels down from its place (up when negative). |
| Morning card | `morningCard` | On | Tells you in the morning how well you slept, and what woke you if something did. Needs the mod on the server. |
| Sleep status button | `statusButton` | On | Shows a moon button on your player in the inventory, which opens how you have been sleeping. Needs the mod on the server. |
| Action bar info | `actionBarInfo` | On | Shows how many monsters are nearby and where the closest one is, above the hotbar. |

### Sounds

How the world sounds while you hide or sleep.

| Option | Key | Default | What it does |
| --- | --- | --- | --- |
| Muffle sounds while hiding and asleep | `muffleSounds` | On | Muffles and roughens the sounds around you while you hide, as much as your hiding spot shuts them out (through a chest's lid crack, a door's windows, the open sides under a bed), and while you sleep, the deeper the sleep the more. |

### Explanations

Why a sleep failed, and what to do about it.

| Option | Key | Default | What it does |
| --- | --- | --- | --- |
| Sleep explanations | `sleepExplanations` | On | Explains every failed sleep with its reason and a hint. |
| Dimension warning | `dimensionWarning` | On | Warns before you use a bed where beds explode, such as the Nether and the End. |

### Bedtime panel

The multiplayer bedtime panel.

| Option | Key | Default | What it does |
| --- | --- | --- | --- |
| Bedtime panel | `sleepBoard` | On | While you're in bed, shows who is still awake and why. Needs the mod on the server. |

## Server settings

Set once for the whole server. In singleplayer, your world is the server.

### Highlights

Highlighting the monsters that blocked a sleep.

| Option | Key | Default | What it does |
| --- | --- | --- | --- |
| Allow highlights | `allowHighlights` | On | Whether players may see the monsters that blocked their sleep glow. |
| Highlight duration | `highlightDuration` | 3 s (1 – 10 s) | Seconds the blocking monsters glow. |
| Allow arrows | `allowArrows` | On | Whether players may see arrows pointing to blocking monsters that are off-screen or behind walls. |
| Show to everyone | `showToEveryone` | Off | Shows highlights to players without the mod too, using the vanilla glowing effect. |
| Room clearing | `roomClearing` | On | When monsters block a sleep and the player deals with all of them in time, tells them the room is safe. |
| Clearing time | `clearingTime` | 10 s (3 – 60 s) | Seconds the player has to deal with the monsters that blocked their sleep. |
| Auto sleep | `autoSleep` | On | Whether a player who cleared the room while standing by the bed lies straight down in it. |

### Bedtime panel

The multiplayer bedtime panel, nudges and votes.

| Option | Key | Default | What it does |
| --- | --- | --- | --- |
| Bedtime panel | `sleepBoard` | On | Whether players in bed are shown who is still awake, and why. |
| Allow nudges | `allowNudges` | On | Whether a player in bed may nudge an awake player with a sound and a message. |
| Sleep vote | `sleepVote` | On | Whether the players in bed can vote from the board to skip the night while a player or two is still up. Those still up are asked first, and can say not yet. |
| Most players up for a vote | `voteMaxAwake` | 1 (1 – 10) | The most players that may still be up for the players in bed to vote. |
| Vote answer time | `voteWait` | 30 s (5 – 120 s) | Seconds the players still up have to answer; with no answer the night is skipped. |
| Nudge cooldown | `nudgeCooldown` | 30 s (0 – 600 s) | Seconds a player waits between nudges. |

### Hiding

Hiding under beds and tables, in chests and closets.

| Option | Key | Default | What it does |
| --- | --- | --- | --- |
| Allow hiding | `allowHiding` | On | Whether players may hide (sneak and use a bed, table, double chest or closet door with empty hands), where nothing can see them. |
| Hide under beds | `hideUnderBeds` | On | Whether players may hide under beds. |
| Hide under tables | `hideUnderTables` | On | Whether players may hide under tables (two top slabs or upside-down stairs side by side, with carpet on them). |
| Hide in chests | `hideInChests` | On | Whether players may hide in double chests. |
| Hide in closets | `hideInClosets` | On | Whether players may hide in closets: a door, or a double door, with a nook behind it walled in by blocks or trapdoors. |
| Hide time | `hideTime` | 15 s (3 – 120 s) | Seconds a player must stay hidden before the monsters after them give up and vanish. |
| Hide cooldown | `hideCooldown` | 10 s (0 – 300 s) | Seconds a player must wait after coming out of a hiding spot before they may hide again (0 for none). Trying sooner shows them a card counting it down. |
| Hiding clears monsters | `hidingClearsMonsters` | On | Whether, once the hide time is up, the monsters after a hiding player and those about their spot (the ones keeping them from sleeping in that bed) vanish. Only when every other player near is hiding too; else they just lose the player. |
| Cover | `cover` | On | Walls close around a hiding spot shorten the hide time: a bed in a corner shakes monsters off sooner. |
| Critter chance | `critterChance` | 5% (0 – 100%) | Percent chance a silverfish or cave spider is already in a hiding spot, and chases the player out. |
| Noise gives away | `noiseGivesAway` | On | Whether a hiding player who makes too much noise with monsters near is found, and pulled out. |
| Voice gives away | `voiceGivesAway` | On | With Simple Voice Chat: whether speaking while hiding is noise, as loud as the voice (whispering hardly counts), so talking with monsters near gives a hiding player away. |
| Heartbeat minigame | `heartbeatMinigame` | On | Whether a player who hides with monsters chasing them must keep their heart slow: clicking on the beat, three misses and they gasp. |
| Monsters search | `hunterSearch` | On | Whether a monster that chased a player into a hiding spot comes to search it, looks about, and then finds them or walks off. |
| Search chance | `searchChance` | 25% (0 – 100%) | Percent chance a monster that chased a player into a hiding spot, and came to search it, finds them and drags them out. This is on normal difficulty: half on easy, half as much again on hard, never on peaceful. The noise they make adds to it. While a player plays the heartbeat minigame there is no chance: only failing it gives them away. |

### Nights

How well players sleep, and what happens in the night.

| Option | Key | Default | What it does |
| --- | --- | --- | --- |
| Shared bed tolerance | `sharedBedTolerance` | 2 (0 – 10) | How many monsters a bed lets by when another player sleeps in a bed beside it. |
| Sleep quality | `sleepQuality` | On | Scores each night's sleep, 0 to 100, from the room, the light, monsters about, music and sleep trouble, and tells the player in the morning. |
| Night terrors | `nightTerrors` | On | Whether restless players can have a night terror when they lie down, and wake up shaken. |
| Terror chance | `terrorChance` | 30% (0 – 100%) | Percent chance of a night terror for a restless player; a sleepless one has twice the chance. |
| Harsh night terrors | `harshTerrors` | On | Whether night terrors grow worse the longer a player has stayed awake: harsher effects on waking (weakness, mining fatigue, darkness), and after three days without sleep, the terror hurts (never below half a heart). |
| Sleep effects | `sleepEffects` | On | Whether waking in the morning can leave a sleep effect: positive after good nights, negative after bad ones, neutral after any. Data packs add more. |
| Sleep effect chance | `sleepEffectChance` | 50% (0 – 100%) | Percent chance of a sleep effect on waking in the morning. |
| Well rested | `wellRested` | On | Whether sleeping through the night in a closed, lit room (doors shut, no dark corners) leaves the player well rested. |

### Guide book

The mod's guide book, read in Folios.

| Option | Key | Default | What it does |
| --- | --- | --- | --- |
| Give the guide book | `giveGuideBook` | On | Gives each player the mod's guide book the first time they join. |

!!! tip "Turning the scary parts off"
    Night terrors (`nightTerrors`), harsh night terrors (`harshTerrors`), the heartbeat minigame
    (`heartbeatMinigame`) and monsters searching hiding spots (`hunterSearch`) are server settings, each on its own.
    On your own client, `muffleSounds` turns off the muffled world while you hide and sleep.
