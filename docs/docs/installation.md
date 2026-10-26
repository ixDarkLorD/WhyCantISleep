# Installation

## In single player

1. Install the mod loader for your version: Fabric, NeoForge or Forge (see the table below).
2. Download Why Can't I Sleep? from [Modrinth](https://modrinth.com/mod/why-cant-i-sleep) or
   [CurseForge](https://www.curseforge.com/minecraft/mc-mods/why-cant-i-sleep), and put it in your `mods` folder.
3. Add what it needs:
    - [CoolCatLib](https://modrinth.com/mod/coolcatlib), the library it is built on;
    - [Folios](https://modrinth.com/mod/folios), which draws its guide book;
    - [Fabric API](https://modrinth.com/mod/fabric-api), on Fabric only.

| Minecraft | Loaders |
| --- | --- |
| 26.1 – 26.3 | Fabric, NeoForge |
| 1.21.1 | Fabric, NeoForge, Forge |
| 1.20.1 | Fabric, Forge |

Then start the game and see [Getting started](../wiki/getting-started.md) for your first night.

## On a server

Install the mod **on the server and on every client**, with the same libraries on both. Hiding, sleep quality, night
terrors, the sleep debt moon and the Bedtime panel need it on the server, because the server decides them; the cards,
highlights and arrows need it on the client to be drawn.

The server's settings are synced to every client as it joins, and win where the two overlap: a feature the server
turns off is greyed out in the players' settings. See [Configuration](server/configuration.md).

## Optional mods

| Mod | What it adds |
| --- | --- |
| [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) | Your voice counts as noise while you hide, and others hear you muffled through your hiding place ([Noise and searches](../wiki/hiding/noise.md#voice-chat)). |
| [Glazed Menu](https://modrinth.com/mod/glazed-menu) | Edit both config files in game, from the Mods menu. |

More on how it works with other mods in [Compatibility](compatibility.md).

## Updating

Replace the jar in your `mods` folder, and update CoolCatLib and Folios with it when the new version asks for newer
ones. The mod is in beta: back up your worlds before updating.
