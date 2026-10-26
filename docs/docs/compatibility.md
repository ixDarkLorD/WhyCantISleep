# Compatibility

Why Can't I Sleep? is made to sit alongside other mods. This page lists the ones it works with on purpose, and what to
know about the rest.

## Required

| Mod | Why |
| --- | --- |
| [CoolCatLib](https://modrinth.com/mod/coolcatlib) | The library the mod is built on: its events, networking, configs, visual effects and sound filters. |
| [Folios](https://modrinth.com/mod/folios) | Draws the Insomniac's Handbook, the mod's guide book. |
| [Fabric API](https://modrinth.com/mod/fabric-api) | On Fabric only. |

## Simple Voice Chat

With [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) installed (it's optional), voices take part in
hiding:

- **Your voice is noise.** Speaking while you hide makes noise as loud as your voice: whispering is safe, talking with
  a monster near gives you away in a few seconds, and shouting almost at once. When it fills up you're heard and
  pulled out, just as if you'd moved about.
- **Others hear you through your hiding place.** A hiding player's voice comes out muffled and roughened by how much
  their spot shuts in: a little from under a bed, a lot through a closet door or a chest's lid, almost nothing
  through a solid wall. It depends on which side of the spot faces you.
- **Hiding yourself, you hear everyone through your own spot**, as you hear the rest of the world.

The server can stop voices giving hiders away with `voiceGivesAway` ([Configuration](server/configuration.md)).
Nothing of it loads when Simple Voice Chat isn't installed.

## HUD mods

The sleep debt moon (at the top left by default) keeps out of the way of whatever else is on the screen: minimaps,
[Jade](https://modrinth.com/mod/jade)'s block tooltip, boss bars and any other mod's HUD. It slides clear of them,
and back when they go. The sleep status button in the inventory does the same with other mods' buttons around the
player preview. You can also move the moon to another corner in the client settings.

## Recipe viewers

The Insomniac's Handbook has its own creative tab, so recipe viewers such as JEI list it, with its recipe (a book and any bed).

## Config screens

With [Glazed Menu](https://modrinth.com/mod/glazed-menu) installed, both config files can be edited in game from the
**Mods** menu (Mod Menu on Fabric). Without it, edit the files in the `config` folder.

## Other sleep mods

Mods that change sleeping (skipping the night with fewer players, sleeping in the day, and so on) should work alongside
it: Why Can't I Sleep? reads the game's own answer to every try to sleep, and adds its rules to the game's own monster
check. If you find a conflict, please tell us on [Discord](https://discord.gg/ZapGEPqm2V).

## Behind Closed Eyes

Behind Closed Eyes is a separate mod by the same author, with the scarier side of the night. The two don't need each
other; installed together, Behind Closed Eyes uses Why Can't I Sleep?'s API (for example, its Gardener sees through
hiding).
