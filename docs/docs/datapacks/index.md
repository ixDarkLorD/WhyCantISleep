# Data packs

Most of what Why Can't I Sleep? decides is data. A data pack can change which mobs keep you awake, what counts as
soothing or unsettling by the bed, which music helps or hurts a night, how nights are scored, what you wake up with,
and where players can hide. No code is needed, and nothing has to be installed on the players' side: everything here
is read by the server (or by the world, in single player).

| What | Where | Page |
| --- | --- | --- |
| Which mobs block sleep, which don't, and which see through hiding | entity type tags | [Tags](#tags) |
| Soothing and unsettling things by the bed | item tags | [Tags](#tags) |
| Calm, creepy and loud music | jukebox song tags (music disc item tags on 1.20.1) | [Tags](#tags) |
| What makes a night better or worse | `whycantisleep/sleep_modifier/` | [Sleep modifiers](sleep-modifiers.md) |
| What you wake up with in the morning | `whycantisleep/sleep_effect/` | [Sleep effects](sleep-effects.md) |
| New places to hide | `whycantisleep/hiding_spot/` | [Hiding spots](hiding-spots.md) |

How the features themselves play is on the [Why can't I sleep?](../../wiki/sleeping/why-cant-i-sleep.md),
[Hiding](../../wiki/hiding/index.md) and [Nights](../../wiki/nights/sleep-quality.md) pages; the server settings that turn them on and off
are on [Configuration](../server/configuration.md). For changes that need code, see
[For mod developers](../developers/index.md).

## Where files go

A data pack is a folder (or a zip) in the world's `datapacks` folder, with a `pack.mcmeta` and a `data` folder. The
mod's own files live in the `whycantisleep` namespace; yours can use any namespace you like, except where you mean to
replace one of the mod's own files, which must keep its namespace and path.

=== "26.1 – 26.3 and 1.21.1"

    ```text
    my_pack/
    ├── pack.mcmeta
    └── data/
        ├── whycantisleep/
        │   └── tags/
        │       ├── entity_type/
        │       │   ├── blocks_sleep.json
        │       │   ├── does_not_block_sleep.json
        │       │   └── ignores_hiding.json
        │       ├── item/
        │       │   ├── soothing.json
        │       │   └── unsettling.json
        │       └── jukebox_song/
        │           ├── calm.json
        │           ├── creepy.json
        │           └── loud.json
        └── mypack/
            └── whycantisleep/
                ├── sleep_modifier/
                │   └── rainy_night.json
                ├── sleep_effect/
                │   └── starry_dream.json
                └── hiding_spot/
                    └── wardrobe.json
    ```

=== "1.20.1"

    ```text
    my_pack/
    ├── pack.mcmeta
    └── data/
        ├── whycantisleep/
        │   └── tags/
        │       ├── entity_types/
        │       │   ├── blocks_sleep.json
        │       │   ├── does_not_block_sleep.json
        │       │   └── ignores_hiding.json
        │       └── items/
        │           ├── soothing.json
        │           ├── unsettling.json
        │           ├── calm.json
        │           ├── creepy.json
        │           └── loud.json
        └── mypack/
            └── whycantisleep/
                ├── sleep_modifier/
                │   └── rainy_night.json
                ├── sleep_effect/
                │   └── starry_dream.json
                └── hiding_spot/
                    └── wardrobe.json
    ```

    1.20.1 uses the old plural tag folders (`entity_types`, `items`), and has no jukebox songs: the music tags are
    **item tags of music discs**, next to `soothing` and `unsettling`.

The `whycantisleep/sleep_modifier`, `whycantisleep/sleep_effect` and `whycantisleep/hiding_spot` folders are the same on
every version. A file's id is its namespace and its path under that folder:
`data/mypack/whycantisleep/sleep_effect/starry_dream.json` is `mypack:starry_dream`.

Set `pack_format` in `pack.mcmeta` to the one for your Minecraft version, as for any data pack.

!!! tip "Trying changes"
    Run `/reload` after editing a file. Tags, sleep modifiers, sleep effects and hiding spots are all read again. A file
    the mod can't read is skipped, and the server log says which one and why (`Skipping the sleep modifier ...`).

!!! note "Translation keys and players' clients"
    Names and labels in these files (`label`, `name`, `where`) can be translation keys or plain text. The server sends
    them as they are, so a key only shows as words when the player has it in a resource pack or a mod's language file;
    a player without it sees the key itself. In a data pack meant for a server, plain text is the safe choice.

## Tags

Every tag the mod reads is in the `whycantisleep` namespace. The mod's own files are complete tags: add to them with
`"replace": false` (the default), or start from nothing with `"replace": true`.

### Entity type tags

| Tag | What it does | Default contents |
| --- | --- | --- |
| `#whycantisleep:blocks_sleep` | Mobs that keep you from sleeping when they are near the bed, as monsters do, even if they aren't monsters. | empty |
| `#whycantisleep:does_not_block_sleep` | Monsters that never keep you from sleeping. | empty |
| `#whycantisleep:ignores_hiding` | Mobs that see through [hiding](../../wiki/hiding/index.md): they can still target and chase a hiding player, and are never shaken off when the hide time is up. | `minecraft:wither`, `minecraft:ender_dragon`, `minecraft:warden`, `minecraft:elder_guardian` |

The sleep rules change the game's own check, so they decide whether a sleep really goes ahead, not only what the mod
highlights. The game looks for monsters within 8 blocks of the bed's head to each side and 5 up and down; a mob in
`does_not_block_sleep` is taken out of what it found, and any living mob in `blocks_sleep` inside the same box is added.
A mob in both tags still blocks: `blocks_sleep` is applied after. These tags don't change which monsters count against
a night's [sleep quality](sleep-modifiers.md) (that counts every monster within 24 blocks).

```json title="data/whycantisleep/tags/entity_type/does_not_block_sleep.json"
{
  "values": [
    "minecraft:spider",
    { "id": "mymod:friendly_ghost", "required": false }
  ]
}
```

```json title="data/whycantisleep/tags/entity_type/blocks_sleep.json"
{
  "values": [
    "minecraft:goat"
  ]
}
```

```json title="data/whycantisleep/tags/entity_type/ignores_hiding.json"
{
  "values": [
    { "id": "mymod:shadow_stalker", "required": false }
  ]
}
```

On 1.20.1 these files go in `tags/entity_types/` instead.

### Item tags: things by the bed

| Tag | What it does | Default contents |
| --- | --- | --- |
| `#whycantisleep:soothing` | A block whose item is in this tag, placed near the bed, makes the night better. | `#minecraft:flowers`, `#minecraft:candles`, amethyst blocks, clusters and buds, moss blocks and carpets, azaleas and flowering azaleas, spore blossoms, lily pads, bookshelves and chiseled bookshelves, cake |
| `#whycantisleep:unsettling` | A block whose item is in this tag, placed near the bed, makes the night worse. | `#minecraft:skulls`, cobwebs, sculk, sculk veins, sensors, calibrated sensors, shriekers and catalysts, spawners, wither roses, bone blocks, soul lanterns, soul torches, soul campfires, and where the game has them, creaking hearts, pale hanging moss and open eyeblossoms |

Every block within 6 blocks of the bed to each side, and 3 up and down, is looked at through its item. A flower pot
counts as the plant in it, so a potted poppy is soothing. A block whose item is in both tags counts as unsettling. With
the mod's own [modifiers](sleep-modifiers.md), each soothing thing is worth +2 (up to +10) and each unsettling one -3
(down to -15); those numbers live in the `soothing` and `unsettling` modifier files, so a data pack can change them.
The items' tooltips say which tag they are in.

```json title="data/whycantisleep/tags/item/soothing.json"
{
  "values": [
    "minecraft:lantern",
    "#minecraft:banners",
    { "id": "mymod:dreamcatcher", "required": false }
  ]
}
```

On 1.20.1 these files go in `tags/items/` instead.

### Music tags

A jukebox playing within 16 blocks of the bed while you sleep counts. When several play, the worst of them is what is
heard: creepy, then loud, then calm. A song in none of the tags counts as no music at all for the score.

=== "26.1 – 26.3 and 1.21.1"

    These are **jukebox song** tags, in `tags/jukebox_song/`.

    | Tag | What it does | Default contents |
    | --- | --- | --- |
    | `#whycantisleep:calm` | Calm music, played alone, makes the night better (+10). | `minecraft:cat`, `chirp`, `far`, `mall`, `stal`, `strad`, `wait`, and where the game has them `creator_music_box`, `precipice`, `tears` |
    | `#whycantisleep:creepy` | Eerie music makes the night worse (-20). | `minecraft:11`, `13`, `ward`, `5`, `mellohi` |
    | `#whycantisleep:loud` | Loud music makes the night worse (-15). | `minecraft:pigstep`, `otherside`, `blocks`, and where the game has them `creator`, `lava_chicken` |

    ```json title="data/whycantisleep/tags/jukebox_song/calm.json"
    {
      "values": [
        "minecraft:relic",
        { "id": "mymod:lullaby", "required": false }
      ]
    }
    ```

=== "1.20.1"

    1.20.1 has no jukebox songs, so these are **item** tags of music discs, in `tags/items/`.

    | Tag | What it does | Default contents |
    | --- | --- | --- |
    | `#whycantisleep:calm` | Calm music, played alone, makes the night better (+10). | `minecraft:music_disc_cat`, `music_disc_chirp`, `music_disc_far`, `music_disc_mall`, `music_disc_stal`, `music_disc_strad`, `music_disc_wait`, `music_disc_relic` |
    | `#whycantisleep:creepy` | Eerie music makes the night worse (-20). | `minecraft:music_disc_11`, `music_disc_13`, `music_disc_ward`, `music_disc_5`, `music_disc_mellohi` |
    | `#whycantisleep:loud` | Loud music makes the night worse (-15). | `minecraft:music_disc_pigstep`, `music_disc_otherside`, `music_disc_blocks` |

    ```json title="data/whycantisleep/tags/items/calm.json"
    {
      "values": [
        { "id": "mymod:music_disc_lullaby", "required": false }
      ]
    }
    ```

The points are those of the mod's `calm_music`, `creepy_music` and `loud_music` [modifiers](sleep-modifiers.md). Creepy
and loud both count when both played; calm counts only when neither did.

!!! tip "Mods that may not be installed"
    Write an entry from another mod as `{ "id": "mymod:thing", "required": false }`. A plain `"mymod:thing"` that doesn't
    exist makes the whole tag fail to load.

### Block tags

The mod has no block tags of its own. Block ids and `#tags` are used inside
[sleep modifiers](sleep-modifiers.md#near_blocks) (`near_blocks`) and [hiding spots](hiding-spots.md) (`blocks`), where
any block tag works, the game's or your own.
