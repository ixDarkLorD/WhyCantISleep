# Hiding spots in code

Players hide by sneaking and using a hiding spot with both hands empty: under a bed or table, in a double chest, in a
closet. A mod can add its own kinds of spot: a wardrobe to stand in, a crate to tuck into, a bunk to crawl under.

Most spots need no code. A data pack file names the blocks, the posture and an offset, and the mod does the rest: see
[Hiding spots](../datapacks/hiding-spots.md). Write a `HidingSpotType` when the spot depends on more than the block:
its other half, whether it is open, what is round it, which side the player used.

| Class | What it is |
| --- | --- |
| `HidingSpots` | Where kinds of spot are registered and looked up. |
| `HidingSpotType` | A kind of spot: which blocks it is about, and the spot it finds at one. |
| `HidingSpot` | One place a player can hide, as a type found it. |
| `HidingPosture` | How the player is placed: lying, standing or tucked. |

All are in `net.ixdarklord.whycantisleep.api`.

## Registering

```java
HidingSpots.register(new WardrobeHidingSpot());
```

Register while your mod initialises, on **both sides**: in common code that runs on the client and on a dedicated
server, such as a CoolCatLib `ModConstructor.onConstructMod()`. The server finds the spot and puts the player in it.
The client needs the type too: it answers a sneak-use of the block itself, so it doesn't predict the block's own use
(a door swinging, a screen opening) before the server's answer comes.

Registered types are asked in the order they were registered (the mod registers its built-in ones as it initialises),
then data pack ones. The first whose `matches` accepts a block answers for it, so give your type blocks of your own
rather than ones a built-in spot already answers for (beds, doors, chests, slabs and stairs with carpet).

`HidingSpots` also has:

| Method | What it does |
| --- | --- |
| `registered()` | The registered types, in order (not data pack ones). |
| `get(id)` | The type with this id, registered or from a data pack, or `null`. |
| `typeFor(state)` | The type that answers for a block, or `null`. Data pack types are known on the server only. |
| `find(level, pos, state, player)` | What a sneak-use of a block finds. Server side. |

## HidingSpotType

```java
public interface HidingSpotType {
    Identifier id();
    boolean matches(BlockState state);
    Lookup find(Level level, BlockPos pos, BlockState state, Player player);
    default boolean stillValid(Level level, HidingSpot spot, Player player);
}
```

| Method | Side | What it does |
| --- | --- | --- |
| `id()` | both | The type's id. Every spot it finds carries it, and the mod looks the type up by it. |
| `matches(state)` | both | Whether the type is about this block at all. Asked on every use of every block a player makes while sneaking with empty hands, so keep it a cheap check of the block. |
| `find(level, pos, state, player)` | server | The spot at the block the player used: a `Lookup`. |
| `stillValid(level, spot, player)` | server | Whether the spot a player hides in still stands. Checked every tick; when it doesn't, they are brought out (`Reason.INTERRUPTED`). |

`find` returns one of three answers:

| `Lookup` | Means |
| --- | --- |
| `Lookup.found(spot)` | They can hide here: the player gets into `spot`. |
| `Lookup.refused(reason)` | This is a hiding spot, but not now. `reason` (a `Component`) is shown on their action bar, and the block isn't used. |
| `Lookup.NONE` | Not a hiding spot after all: the block is used as it would be. |

The default `stillValid` checks that the block at the spot's anchor still `matches` and that `find` there still finds
a spot. It runs `find` again with the player already inside, so keep `find`'s checks to ones that still hold while
they are in it (that the block is closed, that it has a floor), or override `stillValid`. A check that the player has
room to fit, say, fails once they are standing in that room.

## HidingSpot

```java
public record HidingSpot(Identifier type, BlockPos anchor, Vec3 position, float yaw, HidingPosture posture,
                         float eyeHeight, float squint, float hideTimeScale, ItemStack icon, Component where)
```

| Field | What it is |
| --- | --- |
| `type` | The id of the type that found it. It must be the id of a registered (or data pack) type: a spot whose type can't be found isn't hidden in. |
| `anchor` | The block it belongs to: a bed's head, a door's lower half, one block of a table. `stillValid` checks it each tick, and the monsters within 12 blocks of it are what the hide time shakes off. |
| `position` | Where the player is held: their feet. They are put there and kept there, and come out where they went in. |
| `yaw` | The way their body lies or stands, in degrees, as an entity's yaw (`Direction.toYRot()`). Standing or tucked, the view starts facing it too. |
| `posture` | How they are placed: a [`HidingPosture`](#hidingposture). |
| `eyeHeight` | The view's height above `position`. Negative for the posture's own (`viewHeight()` gives the one in use). |
| `squint` | How far their eyes close, 0 (wide open) to 1 (shut); clamped. The view narrows between dark lids as they get in. `HidingSpot.DEFAULT_SQUINT` is 0.5; the built-in spots go from 0.35 (a closet) to 0.72 (peering out of a chest). |
| `hideTimeScale` | The server's hide time is multiplied by this, before full blocks round the spot shorten it further. Below 1 for a spot that hides better. |
| `icon` | The item the hiding card shows. |
| `where` | How the card says where they are, after "Hiding": "under the bed", "in the chest". A translatable `Component`. |

Two methods return a changed copy:

| Method | What it does |
| --- | --- |
| `withSquint(squint)` | The same spot with the eyes closed this far. Handy in a [`HidingEvents.START`](events.md#start) listener. |
| `withPosition(position)` | The same spot with the player held elsewhere in it. |

## HidingPosture

| Posture | Drawn | View |
| --- | --- | --- |
| `LYING` | Flat on the floor, still: under a bed or a table. | 0.09 above `position` unless the spot gives its own. The player can look round freely. |
| `STANDING` | On their feet, still: in a closet. | The player's own eye height. The view starts facing the spot's `yaw`. |
| `TUCKED` | Only the eyes show, peering out under a raised lid: in a chest. | 0.45 above `position` unless the spot gives its own. The view starts facing the spot's `yaw`. |

`defaultEyeHeight()` gives each posture's own view height, negative for the player's own.

!!! note "Tucked is made for chests"
    Getting into and out of a `TUCKED` spot plays the chest's close and open sounds, and the lid the mod raises a crack
    is a chest's (`ChestBlockEntity`). A container of your own with a lid drawn its own way stays shut. Pick the
    posture by what the player should look like, and test it in game.

## Example: a wardrobe

A wardrobe, two blocks tall, with a door at its front. Closed, a player can step into it and stand facing out; open,
it hides nobody.

```java title="WardrobeHidingSpot.java"
import net.ixdarklord.whycantisleep.api.HidingPosture;
import net.ixdarklord.whycantisleep.api.HidingSpot;
import net.ixdarklord.whycantisleep.api.HidingSpotType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/** Standing hidden in a closed wardrobe, facing its door. */
public final class WardrobeHidingSpot implements HidingSpotType {
    public static final Identifier ID = MyMod.id("wardrobe");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public boolean matches(BlockState state) {
        return state.is(MyBlocks.WARDROBE);
    }

    @Override
    public Lookup find(Level level, BlockPos pos, BlockState state, Player player) {
        // Whichever half was used, the spot belongs to the lower one.
        BlockPos lower = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        BlockState base = level.getBlockState(lower);
        if (!base.is(MyBlocks.WARDROBE)) return Lookup.NONE;
        if (base.getValue(BlockStateProperties.OPEN)) {
            return Lookup.refused(Component.translatable("mymod.hiding.wardrobe_open"));
        }
        Direction front = base.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return Lookup.found(new HidingSpot(
                ID,
                lower,                              // the anchor: checked every tick
                Vec3.atBottomCenterOf(lower),       // standing on the wardrobe's floor
                front.toYRot(),                     // facing out through its door
                HidingPosture.STANDING,
                -1.0F,                              // the player's own eye height
                0.4F,                               // a wide squint, as in a closet
                0.8F,                               // hides a little better than a bed
                new ItemStack(MyBlocks.WARDROBE),
                Component.translatable("mymod.hiding.where.wardrobe")));
    }
}
```

```java title="MyModSleepCompat.java"
public static void init() {
    HidingSpots.register(new WardrobeHidingSpot());
}
```

```json title="assets/mymod/lang/en_us.json"
{
  "mymod.hiding.where.wardrobe": "in the wardrobe",
  "mymod.hiding.wardrobe_open": "Close the wardrobe first."
}
```

`matches` is a cheap check of the block, run on both sides. `find`, on the server, works out the spot from the
wardrobe's lower half, so using either half finds the same spot and the anchor is always the lower block. An open
wardrobe refuses with a message instead of opening a menu or doing nothing.

The default `stillValid` suits it: it runs `find` at the anchor each tick, so a wardrobe that is broken or opened (by
another player: a hiding player can't use blocks) brings the player out. The card reads "Hiding in the wardrobe", with
the wardrobe's item as its icon.

Call `init()` only when Why Can't I Sleep? is loaded, from a class of its own: see
[Calling it only when the mod is loaded](index.md#calling-it-only-when-the-mod-is-loaded).

!!! note mc-121 mc-120 "1.21.1 and 1.20.1"
    `Identifier` is `ResourceLocation`. The rest of the example is the same.

## Putting a player in from code

[`WhyCantISleepApi.startHiding(player, pos)`](index.md#starthiding) asks the types for the block at `pos` as a
sneak-use would, and `startHiding(player, spot)` takes a spot you made yourself (its `type` still has to be a
registered type's id). [`stopHiding`](index.md#stophiding) brings them out.
