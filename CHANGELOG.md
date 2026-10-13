# Changelog
This file is for listing all the changes to this project

## v26.1.2-1 Release | Unreleased
### ✨ New Features
- Project set up for Fabric and NeoForge (Minecraft 26.1 to 26.1.2), built on CoolCatLib: Core and CoolCatLib: Canvas.
- **Monster highlights**: when monsters keep you from sleeping, exactly the ones that blocked it (the game's own check range) glow for a few seconds, colored by the kind of threat or in one color. Only you see it; a server can also show it to players without the mod through the game's glowing effect.
- **Off-screen arrows**: arrows around the crosshair point to the blocking monsters you can't see, those outside your view and those behind blocks, in the same colors as their highlights and in three sizes. A solid arrow is a monster outside your view, a hollow one a monster behind blocks; nearer monsters get larger arrows, and arrows that would overlap fan apart.
- **Sleep HUD**: sleep messages no longer use the line above the hotbar. A card of the mod's own shows an icon, what happened and what to do about it, with a line running down the time it stays; a blocked sleep reads "3 monsters nearby" over "Closest: Zombie, 6 blocks below". The sleep board is a card in the same style.
- **Visuals and sounds** (CoolCatLib: Canvas): a heartbeat and a red wash over the screen when monsters block a sleep, wisps rising off the blocking monsters in their highlight colors, whispers from where the nearest one is, and a rare "monster under the bed" scare: eyes at the bed's edge, darkness pouring out, a shriek and a jolt of the camera.
- **Sleep explanations**: every failed sleep says why and what to do: monsters nearby, the wrong time, too far from the bed, an obstructed or occupied bed, a night that can't be skipped, and how many more players must sleep.
- **Dimension warning**: the first use of a bed where beds explode (the Nether, the End) only warns; using it again within three seconds goes through.
- **Sleep board** (needs the mod on the server): while you're in bed, a panel lists who is still awake and why (awake, mining, fighting, AFK, in another dimension) with the progress toward the `playersSleepingPercentage` rule.
- **Nudges**: click an awake player on the board to send them a bell and a message, with a cooldown. Players without the mod get them too.
- Client and server configs, with client options locked while the server turns their feature off.
