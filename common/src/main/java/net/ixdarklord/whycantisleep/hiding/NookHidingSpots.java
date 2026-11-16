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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * The hiding spots of 0.3.0, gaps in what a room is furnished with:
 * <ul>
 *     <li>behind a <b>curtain</b>: a nook two high, walled in on every side but its front, which a wall banner hangs
 *     over from the block above, or which two blocks of wool shut; standing, facing out;</li>
 *     <li>in a <b>hay pile</b>: a gap two long and one high under hay bales, with a floor and walls along its sides;
 *     lying, burrowed in;</li>
 *     <li>in a <b>barrel stack</b> or a <b>bookshelf alcove</b>: a gap two high with barrels, or bookshelves, on both
 *     sides, walled at the back and roofed, open at the front; standing, facing out.</li>
 * </ul>
 * Each is found from the block a player sneak-uses (the banner or wool, a hay bale over or beside the gap, a barrel or
 * bookshelf beside it), and checked again each tick from where the player is held and the way they face.
 */
public final class NookHidingSpots {
    public static final Identifier CURTAIN = WhyCantISleep.id("curtain");
    public static final Identifier HAY = WhyCantISleep.id("hay");
    public static final Identifier BARRELS = WhyCantISleep.id("barrels");
    public static final Identifier BOOKSHELF = WhyCantISleep.id("bookshelf");
    // How far the eyes close: a curtain or an alcove's open front, not much; a barrel stack's gap a little more; burrowed
    // in hay, through the straw, most.
    private static final float CURTAIN_SQUINT = 0.4F;
    private static final float HAY_SQUINT = 0.62F;
    private static final float BARRELS_SQUINT = 0.48F;
    private static final float BOOKSHELF_SQUINT = 0.38F;
    // Lying in hay, the view low in the gap.
    private static final float HAY_EYE = 0.2F;

    private NookHidingSpots() {
    }

    public static void register() {
        HidingSpots.register(new Curtain());
        HidingSpots.register(new Hay());
        HidingSpots.register(new Lined(BARRELS, WCISServerConfig.HIDE_IN_BARRELS::get, state -> state.is(Blocks.BARREL), BARRELS_SQUINT, 0.95F,
                "gui.whycantisleep.hiding.where.barrels", "between the barrels"));
        HidingSpots.register(new Lined(BOOKSHELF, WCISServerConfig.HIDE_IN_BOOKSHELVES::get,
                state -> state.is(Blocks.BOOKSHELF) || state.is(Blocks.CHISELED_BOOKSHELF), BOOKSHELF_SQUINT, 1.0F,
                "gui.whycantisleep.hiding.where.bookshelf", "in the bookshelf alcove"));
    }

    /** Whether a spot is one of these, whose getting in and out sounds of what it is made of. */
    public static boolean isNook(Identifier type) {
        return type.equals(CURTAIN) || type.equals(HAY) || type.equals(BARRELS) || type.equals(BOOKSHELF);
    }

    /**
     * Whether a set of cells is shut in on every side but the given open ones: where a cell meets a block that isn't one
     * of the cells, the two shapes between them must cover the whole face, as a closet's walls must.
     */
    private static boolean shut(Level level, Set<BlockPos> cells, Set<BlockPos> open) {
        for (BlockPos cell : cells) {
            VoxelShape shape = level.getBlockState(cell).getCollisionShape(level, cell);
            for (Direction side : Direction.values()) {
                BlockPos next = cell.relative(side);
                if (cells.contains(next) || open.contains(next)) continue;
                if (!Shapes.mergedFaceOccludes(shape, level.getBlockState(next).getCollisionShape(level, next), side)) return false;
            }
        }
        return true;
    }

    // Whether a player fits standing on a cell's floor.
    private static boolean fitsStanding(Level level, Player player, BlockPos lower) {
        return level.noCollision(player, player.getDimensions(Pose.STANDING).makeBoundingBox(Vec3.atBottomCenterOf(lower)));
    }

    // The horizontal sides, the one facing the player first: a gap is most likely open towards whoever reaches into it.
    private static List<Direction> towards(BlockPos from, Player player) {
        List<Direction> sides = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        Vec3 to = player.position().subtract(Vec3.atBottomCenterOf(from));
        sides.sort(Comparator.comparingDouble(side -> -(side.getStepX() * to.x + side.getStepZ() * to.z)));
        return sides;
    }

    /** A nook two high whose front is hung with a banner from the block above it, or shut by two blocks of wool. */
    private static final class Curtain implements HidingSpotType {
        @Override
        public Identifier id() {
            return CURTAIN;
        }

        @Override
        public boolean matches(BlockState state) {
            return WCISServerConfig.HIDE_BEHIND_CURTAINS.get() && (state.getBlock() instanceof WallBannerBlock || state.is(BlockTags.WOOL));
        }

        @Override
        public Lookup find(Level level, BlockPos pos, BlockState state, Player player) {
            if (state.getBlock() instanceof WallBannerBlock) {
                // The banner hangs from the face of the block over the nook: the nook is under that block, open to the
                // way the banner faces.
                Direction front = state.getValue(WallBannerBlock.FACING);
                BlockPos lower = pos.relative(front.getOpposite()).below(2);
                return at(level, lower, front, pos, player);
            }
            // Wool: the nook is behind it, away from the player, the wool its front two high.
            BlockPos wool = level.getBlockState(pos.below()).is(BlockTags.WOOL) ? pos.below() : pos;
            if (!level.getBlockState(wool.above()).is(BlockTags.WOOL)) return Lookup.NONE;
            List<Direction> sides = towards(wool, player);
            Direction away = sides.get(sides.size() - 1);
            return at(level, wool.relative(away), away.getOpposite(), wool, player);
        }

        @Override
        public boolean stillValid(Level level, HidingSpot spot, Player player) {
            return this.matches(level.getBlockState(spot.anchor()))
                    && at(level, BlockPos.containing(spot.position()), Direction.fromYRot(spot.yaw()), spot.anchor(), player).spot() != null;
        }

        // The nook with its lower cell here, facing out to the front, hung or shut by the curtain at its anchor.
        private Lookup at(Level level, BlockPos lower, Direction front, BlockPos anchor, Player player) {
            BlockState curtain = level.getBlockState(anchor);
            Set<BlockPos> cells = Set.of(lower, lower.above());
            boolean wool = curtain.is(BlockTags.WOOL);
            // A banner leaves the front open (it hangs in front of it); wool is the front.
            Set<BlockPos> open = wool ? Set.of() : Set.of(lower.relative(front), lower.above().relative(front));
            if (wool && !(level.getBlockState(lower.relative(front)).is(BlockTags.WOOL) && level.getBlockState(lower.above().relative(front)).is(BlockTags.WOOL))) {
                return Lookup.NONE;
            }
            if (!shut(level, cells, open) || !fitsStanding(level, player, lower)) return Lookup.NONE;
            return Lookup.found(new HidingSpot(CURTAIN, anchor.immutable(), Vec3.atBottomCenterOf(lower), front.toYRot(), HidingPosture.STANDING, -1.0F,
                    CURTAIN_SQUINT, 0.9F, new ItemStack(curtain.getBlock().asItem()),
                    Component.translatableWithFallback("gui.whycantisleep.hiding.where.curtain", "behind the curtain")));
        }
    }

    /** A gap two long and one high burrowed into hay: hay bales over it, a floor under it, walls along its sides. */
    private static final class Hay implements HidingSpotType {
        @Override
        public Identifier id() {
            return HAY;
        }

        @Override
        public boolean matches(BlockState state) {
            return WCISServerConfig.HIDE_IN_HAY.get() && state.is(Blocks.HAY_BLOCK);
        }

        @Override
        public Lookup find(Level level, BlockPos pos, BlockState state, Player player) {
            // The gap may be under the bale used, or beside it (a bale walling it in, or over its mouth).
            List<BlockPos> starts = new ArrayList<>();
            starts.add(pos.below());
            for (Direction side : towards(pos, player)) starts.add(pos.relative(side));
            for (BlockPos start : starts) {
                // Along the sides facing away from the player first: the gap runs on from where they reach in.
                List<Direction> alongs = towards(start, player);
                Collections.reverse(alongs);
                for (Direction along : alongs) {
                    Lookup lookup = at(level, start, along, player);
                    if (lookup.spot() != null) return lookup;
                }
            }
            return Lookup.NONE;
        }

        @Override
        public boolean stillValid(Level level, HidingSpot spot, Player player) {
            Direction along = Direction.fromYRot(spot.yaw());
            Vec3 back = spot.position().subtract(along.getStepX() * 0.5, 0.0, along.getStepZ() * 0.5);
            return this.matches(level.getBlockState(spot.anchor())) && at(level, BlockPos.containing(back), along, player).spot() != null;
        }

        // The gap from this cell on along a side: both cells free, hay over both, a floor under both, and full blocks or
        // hay along both its long sides. Lying in it, the head away from the way in.
        private static Lookup at(Level level, BlockPos first, Direction along, Player player) {
            BlockPos second = first.relative(along);
            for (BlockPos cell : List.of(first, second)) {
                if (!level.getBlockState(cell).getCollisionShape(level, cell).isEmpty()) return Lookup.NONE;
                if (!level.getBlockState(cell.above()).is(Blocks.HAY_BLOCK)) return Lookup.NONE;
                if (!BuiltinHidingSpots.solidFloor(level, cell.below())) return Lookup.NONE;
                for (Direction side : new Direction[]{along.getClockWise(), along.getCounterClockWise()}) {
                    BlockPos wall = cell.relative(side);
                    BlockState state = level.getBlockState(wall);
                    if (!state.is(Blocks.HAY_BLOCK) && !state.isCollisionShapeFullBlock(level, wall)) return Lookup.NONE;
                }
            }
            Vec3 middle = Vec3.atBottomCenterOf(first).add(along.getStepX() * 0.5, 0.0, along.getStepZ() * 0.5);
            return Lookup.found(new HidingSpot(HAY, first.above().immutable(), middle, along.toYRot(), HidingPosture.LYING, HAY_EYE, HAY_SQUINT, 0.85F,
                    new ItemStack(Blocks.HAY_BLOCK), Component.translatableWithFallback("gui.whycantisleep.hiding.where.hay", "in the hay")));
        }
    }

    /**
     * A gap two high between blocks of one kind (barrels, bookshelves): those on both of its sides, both high, walled at
     * the back and roofed and floored by anything solid, open at the front.
     */
    private record Lined(Identifier id, BooleanSupplier enabled, Predicate<BlockState> lining, float squint, float hideTimeScale,
                         String whereKey, String whereFallback) implements HidingSpotType {
        @Override
        public boolean matches(BlockState state) {
            return this.enabled.getAsBoolean() && this.lining.test(state);
        }

        @Override
        public Lookup find(Level level, BlockPos pos, BlockState state, Player player) {
            // The gap is beside the block used, level with it or with the one under it (the upper of a stack).
            for (BlockPos beside : List.of(pos, pos.below())) {
                for (Direction side : towards(beside, player)) {
                    BlockPos lower = beside.relative(side);
                    for (Direction front : towards(lower, player)) {
                        Lookup lookup = this.at(level, lower, front, pos, player);
                        if (lookup.spot() != null) return lookup;
                    }
                }
            }
            return Lookup.NONE;
        }

        @Override
        public boolean stillValid(Level level, HidingSpot spot, Player player) {
            return this.matches(level.getBlockState(spot.anchor()))
                    && this.at(level, BlockPos.containing(spot.position()), Direction.fromYRot(spot.yaw()), spot.anchor(), player).spot() != null;
        }

        private Lookup at(Level level, BlockPos lower, Direction front, BlockPos anchor, Player player) {
            for (Direction side : new Direction[]{front.getClockWise(), front.getCounterClockWise()}) {
                if (!this.lining.test(level.getBlockState(lower.relative(side))) || !this.lining.test(level.getBlockState(lower.above().relative(side)))) {
                    return Lookup.NONE;
                }
            }
            Set<BlockPos> cells = new HashSet<>(List.of(lower, lower.above()));
            Set<BlockPos> open = Set.of(lower.relative(front), lower.above().relative(front));
            if (!shut(level, cells, open) || !fitsStanding(level, player, lower)) return Lookup.NONE;
            BlockState block = level.getBlockState(anchor);
            return Lookup.found(new HidingSpot(this.id, anchor.immutable(), Vec3.atBottomCenterOf(lower), front.toYRot(), HidingPosture.STANDING, -1.0F,
                    this.squint, this.hideTimeScale, new ItemStack(block.getBlock().asItem()),
                    Component.translatableWithFallback(this.whereKey, this.whereFallback)));
        }
    }
}
