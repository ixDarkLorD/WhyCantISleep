package net.ixdarklord.whycantisleep.hiding;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.api.HidingPosture;
import net.ixdarklord.whycantisleep.api.HidingSpot;
import net.ixdarklord.whycantisleep.api.HidingSpotType;
import net.ixdarklord.whycantisleep.api.HidingSpots;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The hiding spots the mod knows of itself, all made of the game's own blocks:
 * <ul>
 *     <li>under a <b>bed</b>, flat on the floor (it needs one);</li>
 *     <li>under a <b>table</b>: two top slabs or upside-down stairs side by side with carpet on both, the gap under them
 *     to lie in, over a floor;</li>
 *     <li>inside a <b>double chest</b>, crouched with the lid raised a crack to peer out (a single one is too small),
 *     room for two, one in each half;</li>
 *     <li>in a <b>closet</b>: a door, or a double door, with a nook behind it two high and as wide as the doors, shut in
 *     on every other side by blocks or by trapdoors lying flush against it, with room to stand; standing in it facing
 *     the doors, which close behind them; two wide, room for two, one in each nook.</li>
 * </ul>
 */
public final class BuiltinHidingSpots {
    public static final Identifier BED = WhyCantISleep.id("bed");
    public static final Identifier TABLE = WhyCantISleep.id("table");
    public static final Identifier CHEST = WhyCantISleep.id("chest");
    public static final Identifier CLOSET = WhyCantISleep.id("closet");
    // The view under a bed sits midway between the floor and its underside (3/16); under a table, low in its half block.
    private static final float BED_EYE = 1.5F / 16.0F;
    private static final float TABLE_EYE = 0.2F;
    // Peering out under a chest's lid, raised a crack: the view in the gap, just over the rim where the lid shuts (10/16).
    private static final float CHEST_EYE = 0.67F;
    // Crouched in a chest, they are this far forward of its middle, so their head is under the lid's raised front edge.
    private static final double CHEST_FORWARD = 0.3;
    // How far the eyes close in each: lying low, half; peering out of a chest, to a slit; in a closet, not much.
    private static final float BED_SQUINT = 0.5F;
    private static final float TABLE_SQUINT = 0.45F;
    private static final float CHEST_SQUINT = 0.72F;
    private static final float CLOSET_SQUINT = 0.35F;

    private BuiltinHidingSpots() {
    }

    public static void register() {
        HidingSpots.register(new Bed());
        HidingSpots.register(new Table());
        HidingSpots.register(new Chest());
        HidingSpots.register(new Closet());
    }

    /** Whether a block has a solid top to lie on. */
    public static boolean solidFloor(BlockGetter level, BlockPos below) {
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    private static final class Bed implements HidingSpotType {
        @Override
        public Identifier id() {
            return BED;
        }

        @Override
        public boolean matches(BlockState state) {
            return WCISServerConfig.HIDE_UNDER_BEDS.get() && state.getBlock() instanceof BedBlock;
        }

        @Override
        public Lookup find(Level level, BlockPos pos, BlockState state, Player player) {
            Direction facing = state.getValue(BedBlock.FACING);
            BlockPos head = state.getValue(BedBlock.PART) == BedPart.HEAD ? pos.immutable() : pos.relative(facing);
            // A bed over air has nothing under it to lie on.
            if (!Hiding.hasFloor(level, head)) {
                return Lookup.refused(Component.translatableWithFallback("gui.whycantisleep.hide.no_floor", "There's no floor under this bed to hide on."));
            }
            // Flat on the floor, halfway along the bed, lying along it with the head to the pillow.
            Vec3 middle = Vec3.atBottomCenterOf(head).subtract(facing.getStepX() * 0.5, 0.0, facing.getStepZ() * 0.5);
            return Lookup.found(new HidingSpot(BED, head, middle, facing.toYRot(), HidingPosture.LYING, BED_EYE, BED_SQUINT, 1.0F,
                    new ItemStack(state.getBlock().asItem()), Component.translatableWithFallback("gui.whycantisleep.hiding.where.bed", "under the bed")));
        }
    }

    private static final class Table implements HidingSpotType {
        @Override
        public Identifier id() {
            return TABLE;
        }

        // The tabletop, or the carpet on it: either is what a player would reach for.
        @Override
        public boolean matches(BlockState state) {
            return WCISServerConfig.HIDE_UNDER_TABLES.get() && (isTop(state) || state.is(BlockTags.WOOL_CARPETS));
        }

        @Override
        public Lookup find(Level level, BlockPos pos, BlockState state, Player player) {
            if (state.is(BlockTags.WOOL_CARPETS)) {
                pos = pos.below();
                if (!isTop(level.getBlockState(pos))) return Lookup.NONE;
            }
            if (!level.getBlockState(pos.above()).is(BlockTags.WOOL_CARPETS)) return Lookup.NONE;
            // The other half of the table: the first neighbour that is a carpeted top too.
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos other = pos.relative(side);
                if (!isTop(level.getBlockState(other)) || !level.getBlockState(other.above()).is(BlockTags.WOOL_CARPETS)) continue;
                if (!solidFloor(level, pos.below()) || !solidFloor(level, other.below())) {
                    return Lookup.refused(Component.translatableWithFallback("gui.whycantisleep.hide.no_floor_table", "There's no floor under this table to hide on."));
                }
                Vec3 middle = Vec3.atBottomCenterOf(pos).add(side.getStepX() * 0.5, 0.0, side.getStepZ() * 0.5);
                return Lookup.found(new HidingSpot(TABLE, pos.immutable(), middle, side.toYRot(), HidingPosture.LYING, TABLE_EYE, TABLE_SQUINT, 1.0F,
                        new ItemStack(level.getBlockState(pos.above()).getBlock().asItem()),
                        Component.translatableWithFallback("gui.whycantisleep.hiding.where.table", "under the table")));
            }
            return Lookup.NONE;
        }

        // A tabletop: a slab in the top half of its block, or stairs turned upside down, so there is room under it.
        private static boolean isTop(BlockState state) {
            if (state.getBlock() instanceof SlabBlock) return state.getValue(SlabBlock.TYPE) == SlabType.TOP;
            if (state.getBlock() instanceof StairBlock) return state.getValue(StairBlock.HALF) == Half.TOP;
            return false;
        }
    }

    private static final class Chest implements HidingSpotType {
        @Override
        public Identifier id() {
            return CHEST;
        }

        @Override
        public boolean matches(BlockState state) {
            return WCISServerConfig.HIDE_IN_CHESTS.get() && state.getBlock() instanceof ChestBlock;
        }

        @Override
        public Lookup find(Level level, BlockPos pos, BlockState state, Player player) {
            if (state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
                return Lookup.refused(Component.translatableWithFallback("gui.whycantisleep.hide.chest_too_small", "This chest is too small to climb into."));
            }
            BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
            Direction front = state.getValue(ChestBlock.FACING);
            Vec3 middle = Vec3.atBottomCenterOf(pos).add(Vec3.atBottomCenterOf(other)).scale(0.5)
                    .add(front.getStepX() * CHEST_FORWARD, 0.05, front.getStepZ() * CHEST_FORWARD);
            return Lookup.found(new HidingSpot(CHEST, pos.immutable(), middle, front.toYRot(), HidingPosture.TUCKED, CHEST_EYE, CHEST_SQUINT, 0.9F,
                    new ItemStack(state.getBlock().asItem()), Component.translatableWithFallback("gui.whycantisleep.hiding.where.chest", "in the chest")));
        }
    }

    /**
     * The places in a spot, one for each player it holds: a double chest and a closet two wide hold two, side by side,
     * one in each half (the half the player used first); every other spot holds one, at its own position.
     */
    public static List<Vec3> places(Level level, HidingSpot spot, Player player) {
        BlockPos anchor = spot.anchor();
        BlockState state = level.getBlockState(anchor);
        if (spot.type().equals(CHEST) && state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            Vec3 middle = spot.position();
            Vec3 half = Vec3.atBottomCenterOf(anchor.relative(ChestBlock.getConnectedDirection(state))).subtract(Vec3.atBottomCenterOf(anchor)).scale(0.5);
            return List.of(middle.subtract(half), middle.add(half));
        }
        if (spot.type().equals(CLOSET) && state.getBlock() instanceof DoorBlock) {
            BlockPos partner = Closet.partner(level, anchor, state);
            if (partner != null && Closet.enclosed(level, List.of(anchor, partner), state, player).spot() != null) {
                Direction back = state.getValue(DoorBlock.FACING).getOpposite();
                double y = spot.position().y;
                Vec3 own = Vec3.atBottomCenterOf(anchor.relative(back));
                Vec3 other = Vec3.atBottomCenterOf(partner.relative(back));
                return List.of(new Vec3(own.x, y, own.z), new Vec3(other.x, y, other.z));
            }
        }
        return List.of(spot.position());
    }

    /**
     * The doors of a closet someone hides in, to shut behind them: its own, and the other of a double door when the
     * closet is two wide.
     */
    public static List<BlockPos> closetDoors(Level level, HidingSpot spot, Player player) {
        BlockPos door = spot.anchor();
        BlockState state = level.getBlockState(door);
        if (!(state.getBlock() instanceof DoorBlock)) return List.of(door);
        BlockPos partner = Closet.partner(level, door, state);
        boolean wide = partner != null && Closet.enclosed(level, List.of(door, partner), state, player).spot() != null;
        return wide ? List.of(door, partner) : List.of(door);
    }

    private static final class Closet implements HidingSpotType {
        // Low things on the floor a player stands on rather than in, like carpet; anything taller is in the way.
        private static final double FLOOR_LIFT = 0.25;

        @Override
        public Identifier id() {
            return CLOSET;
        }

        @Override
        public boolean matches(BlockState state) {
            return WCISServerConfig.HIDE_IN_CLOSETS.get() && state.getBlock() instanceof DoorBlock;
        }

        @Override
        public Lookup find(Level level, BlockPos pos, BlockState state, Player player) {
            BlockPos door = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos.immutable() : pos.below();
            return closet(level, door, level.getBlockState(door), player);
        }

        @Override
        public boolean stillValid(Level level, HidingSpot spot, Player player) {
            BlockState state = level.getBlockState(spot.anchor());
            return this.matches(state) && closet(level, spot.anchor(), state, player).spot() != null;
        }

        // A double door's closet is two wide, behind both; failing that, a single door's is one wide, behind it.
        private static Lookup closet(Level level, BlockPos door, BlockState doorState, Player player) {
            BlockPos partner = partner(level, door, doorState);
            if (partner != null) {
                Lookup wide = enclosed(level, List.of(door, partner), doorState, player);
                if (wide.spot() != null) return wide;
            }
            return enclosed(level, List.of(door), doorState, player);
        }

        // The other half of a double door: a door beside this one, facing the same way.
        static @Nullable BlockPos partner(Level level, BlockPos door, BlockState doorState) {
            Direction facing = doorState.getValue(DoorBlock.FACING);
            for (Direction side : new Direction[]{facing.getClockWise(), facing.getCounterClockWise()}) {
                BlockPos next = door.relative(side);
                BlockState state = level.getBlockState(next);
                if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.FACING) == facing && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                    return next;
                }
            }
            return null;
        }

        /**
         * A closet behind doors: the nook behind each, two high, shut in on every side but the doors', and room in it
         * for the player standing. The nook is on the side each door was placed from, so the doors swing out of it.
         * Shut in is told from the blocks' own shapes, face by face: where a nook's cell meets the next block, the two
         * between them must cover the whole face, so a full block does, and so does a trapdoor lying flush against
         * the nook, open as a wall or shut as a roof or floor, whether in the next block or in the nook's own cell.
         */
        static Lookup enclosed(Level level, List<BlockPos> doors, BlockState doorState, Player player) {
            Direction facing = doorState.getValue(DoorBlock.FACING);
            Set<BlockPos> cells = new HashSet<>();
            Set<BlockPos> doorways = new HashSet<>();
            for (BlockPos door : doors) {
                BlockPos nook = door.relative(facing.getOpposite());
                for (int dy = 0; dy < 2; dy++) {
                    cells.add(nook.above(dy));
                    doorways.add(door.above(dy));
                }
            }
            for (BlockPos cell : cells) {
                VoxelShape shape = level.getBlockState(cell).getCollisionShape(level, cell);
                for (Direction side : Direction.values()) {
                    BlockPos next = cell.relative(side);
                    if (cells.contains(next) || doorways.contains(next)) continue;
                    if (!Shapes.mergedFaceOccludes(shape, level.getBlockState(next).getCollisionShape(level, next), side)) return Lookup.NONE;
                }
            }
            // The player stands in the middle of the nook's floor, on a carpet if there is one, and must fit there.
            double x = 0.0;
            double z = 0.0;
            double floor = 0.0;
            for (BlockPos door : doors) {
                BlockPos nook = door.relative(facing.getOpposite());
                x += nook.getX() + 0.5;
                z += nook.getZ() + 0.5;
                VoxelShape shape = level.getBlockState(nook).getCollisionShape(level, nook);
                if (!shape.isEmpty() && shape.max(Direction.Axis.Y) <= FLOOR_LIFT) floor = Math.max(floor, shape.max(Direction.Axis.Y));
            }
            Vec3 at = new Vec3(x / doors.size(), doors.getFirst().getY() + floor, z / doors.size());
            if (!level.noCollision(player, player.getDimensions(Pose.STANDING).makeBoundingBox(at))) return Lookup.NONE;
            return Lookup.found(new HidingSpot(CLOSET, doors.getFirst(), at, facing.toYRot(), HidingPosture.STANDING, -1.0F, CLOSET_SQUINT, 0.85F,
                    new ItemStack(doorState.getBlock().asItem()), Component.translatableWithFallback("gui.whycantisleep.hiding.where.closet", "in the closet")));
        }
    }
}
