# Hiding spots

Players hide by sneaking and using a hiding spot with both hands empty: under a bed or a table, inside a double chest,
in a closet. A data pack can make any other block a hiding spot, such as a modded wardrobe or cupboard, with one file.
How hiding plays is on [Hiding](../playing/hiding.md).

Each spot is a file at `data/<namespace>/whycantisleep/hiding_spot/<name>.json`, the same on every Minecraft version.
The mod has no hiding spot files of its own: its four spots are built in.

```json title="data/mypack/whycantisleep/hiding_spot/wardrobe.json"
{
  "blocks": ["mymod:wardrobe", "#mymod:cupboards"],
  "posture": "standing",
  "facing": "block",
  "squint": 0.35,
  "hide_time_scale": 0.85,
  "where": "in the wardrobe"
}
```

## Keys

| Key | Type | Default | What it does |
| --- | --- | --- | --- |
| `blocks` | list of strings | required | The blocks that are this spot: block ids and `#tags`. |
| `posture` | string | `"lying"` | How the player is placed: `lying`, `standing` or `tucked`. See [Postures](#postures). |
| `offset` | list of 3 numbers | `[0, 0, 0]` | Where the player is held, from the bottom centre of the block, turned with the spot's facing. See [Placing the player](#placing-the-player). |
| `eye_height` | number | the posture's | The view's height above where the player is held. Leave it out, or give a negative number, for the posture's own. |
| `squint` | number, 0 to 1 | `0.5` | How far the eyelids close while hiding: 0 wide open, 1 shut. |
| `needs_floor` | boolean | `true` | Whether the block under the spot must have a solid top. |
| `hide_time_scale` | number, 0.05 to 10 | `1` | The server's hide time is multiplied by this: below 1 a quicker spot, above 1 a slower one. |
| `facing` | string | `"block"` | Which way the player faces: `block`, `opposite` or `player`. See [Placing the player](#placing-the-player). |
| `where` | string | `"gui.whycantisleep.hiding.where.default"` | Finishes the hiding card's title, "Hiding ...": a translation key, or plain text. The default reads "out of sight". |

The hiding card's icon is the block's own item.

### Postures

| Posture | Like | Drawn | View |
| --- | --- | --- | --- |
| `lying` | under a bed or a table | Flat on the floor, still, lying the way the spot faces. | At `eye_height` above the floor, 0.09 by default. |
| `standing` | in a closet | On their feet, still, turned the way the spot faces. | The player's own eye height. `eye_height` has no effect. |
| `tucked` | in a chest | Only the band of the head with the eyes shows, peering out. | At `eye_height`, 0.45 by default, a little forward, to see out of the gap. |

Only chests raise their lid a crack for a tucked player. In another block, a tucked player shows as a pair of eyes
inside it, so `tucked` suits blocks you can see into, or a spot that pokes a little out of its block.

### Placing the player

The player is held at the **bottom centre** of the block that was used, moved by `offset`. `offset` is `[x, y, z]` in
blocks, written as if the player faced south, and turned with the way they face: `z` is forward (the way they face),
`x` is to their left, and `y` is up. `[0, 0, 0.3]` moves them 0.3 towards the front of the spot.

`facing` decides which way that is:

| `facing` | The player faces |
| --- | --- |
| `block` | The way the block faces (its `facing` property, when it has a horizontal one). For most furniture that is out of its front. |
| `opposite` | The other way: into the block. |
| `player` | The way the player was facing when they used it. |

A block with no horizontal `facing` property falls back on the player: `block` is the way the player was facing, and
`opposite` the way back towards them.

### The floor

With `needs_floor` on (the default), the block under the used block must have a solid top face. Without one the player
is told "There's no floor here to hide on." and stays out. The spot is checked again every tick: if the block is broken
or changed, or the floor under it is broken, the player comes out.

For a block two tall, such as a wardrobe, the half that was used is the block the player is placed in, and `offset` is
the same for both halves. Using the top half puts them a block up, on the bottom half, which counts as a floor only if
it has a solid top. List only the bottom half's block in `blocks` when the two halves are different blocks; when they
are one block with two states, both halves are the spot.

## How a spot is found

When a player sneaks and uses a block with both hands empty (and the server allows hiding, and their hide cooldown has
passed), the mod asks each kind of spot in turn whether it is that block. The first that says yes answers for it:

1. The built-in spots: beds, tables, chests and closets.
2. Spots other mods add in code, in the order they were added.
3. Data pack spots, in the order of their ids.

A data pack spot only answers for blocks the ones before it don't claim, so it can't change a bed or a chest. The
built-in spots claim:

| Built-in spot | Blocks it claims | Posture | Squint | Hide time |
| --- | --- | --- | --- | --- |
| Bed | every bed | lying | 0.5 | x1 |
| Table | top slabs and upside-down stairs, and wool carpets | lying | 0.45 | x1 |
| Chest | every chest (a single chest refuses: "too small") | tucked | 0.72 | x0.9 |
| Closet | every door | standing | 0.35 | x0.85 |

A built-in spot only claims its blocks while its server setting (`hideUnderBeds`, `hideUnderTables`, `hideInChests`,
`hideInClosets`) is on. With one off, a data pack spot can answer for those blocks instead. Data pack spots have no
setting of their own: `allowHiding` turns all hiding off, theirs included. See
[Configuration](../server/configuration.md).

Using the top of the floor in front of a spot, crouched and looking in under it, counts as using the spot: the block
above the one used, or the one above and one further from the player, is tried.

A data pack spot holds one player. While someone hides in it, another is told "Someone's already hiding there."

## What goes with it

A data pack spot is a hiding spot like any other. While a player is in it, monsters can't see or target them (except
those in [`#whycantisleep:ignores_hiding`](index.md#entity-type-tags)), the hide time runs, and when it is up the
monsters hunting them are shaken off. Each full block on a side of the spot takes 10% off the hide time, at most three
(the server's `cover` setting), the hide time is never under one second, and everything else on the
[Hiding](../playing/hiding.md) page applies: noise, critters, the heartbeat minigame, the cooldown, and muffled sounds.

!!! note "Server side only"
    Data pack spots are known to the server only. A player's client doesn't know them, so it predicts nothing when they
    use such a block and waits for the server to answer. Write `where` as plain text, or give its translation key in a
    resource pack, since the client shows a key it doesn't have as the key itself.

## Examples

A cupboard to crouch in, the view low and a little in from its front:

```json title="data/mypack/whycantisleep/hiding_spot/cupboard.json"
{
  "blocks": ["#mypack:cupboards"],
  "posture": "tucked",
  "offset": [0.0, 0.0, 0.2],
  "eye_height": 0.5,
  "squint": 0.65,
  "where": "in the cupboard"
}
```

Under a modded desk, lying along the way the player faced:

```json title="data/mypack/whycantisleep/hiding_spot/desk.json"
{
  "blocks": ["mymod:oak_desk", "mymod:spruce_desk"],
  "posture": "lying",
  "facing": "player",
  "eye_height": 0.2,
  "squint": 0.45,
  "hide_time_scale": 1.2,
  "where": "under the desk"
}
```

Inside a hollow log, out in the open, slower to lose them:

```json title="data/mypack/whycantisleep/hiding_spot/hollow_log.json"
{
  "blocks": ["mymod:hollow_oak_log"],
  "posture": "lying",
  "needs_floor": false,
  "hide_time_scale": 1.5,
  "where": "in the hollow log"
}
```

For a spot that needs more than a block and a place, such as a check of the blocks round it, a mod can add one in code.
See [For mod developers](../developers/index.md).
