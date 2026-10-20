package net.ixdarklord.whycantisleep.between;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * The dread candle stand: stacked on itself it grows as tall as it is built ({@link #PART}): a stand on its own is a
 * short one; stacked, the lowest is its foot and shaft, those between plain shaft with a knot, and the highest its
 * tray, which alone holds a candle. A stand that stops being the highest (another set on it) lets fall the candle it
 * held.
 */
public class CandleStandBlock extends CandelabraBlock {
    public static final MapCodec<CandleStandBlock> CODEC = simpleCodec(properties -> new CandleStandBlock(properties, Shapes.block(), List.of()));
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public CandleStandBlock(BlockBehaviour.Properties properties, VoxelShape north, List<Vec3> slots) {
        super(properties, north, slots);
        this.registerDefaultState(this.defaultBlockState().setValue(PART, Part.SINGLE));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public List<Vec3> slots(BlockState state) {
        return state.getValue(PART).holdsCandle() ? this.slots() : List.of();
    }

    private Part part(BlockGetter level, BlockPos pos) {
        boolean below = level.getBlockState(pos.below()).is(this), above = level.getBlockState(pos.above()).is(this);
        return below ? (above ? Part.MIDDLE : Part.TOP) : (above ? Part.BASE : Part.SINGLE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
        // Stacked on a stand, it takes that stand's facing so the column stays true.
        if (below.is(this)) state = state.setValue(FACING, below.getValue(FACING));
        return state.setValue(PART, this.part(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        if (direction.getAxis() != Direction.Axis.Y) return state;
        Part part = this.part(level, pos);
        if (!part.holdsCandle() && state.getValue(PART).holdsCandle()) ticks.scheduleTick(pos, this, 1);
        return state.setValue(PART, part);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // No longer the highest: the candle it held falls.
        if (state.getValue(PART).holdsCandle() || !(level.getBlockEntity(pos) instanceof CandelabraBlockEntity stand)) return;
        Block candle;
        while ((candle = stand.takeLast()) != null) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, new ItemStack(candle));
        }
        level.setBlock(pos, state.setValue(CANDLES, 0), Block.UPDATE_ALL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    public enum Part implements StringRepresentable {
        SINGLE("single"), BASE("base"), MIDDLE("middle"), TOP("top");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        boolean holdsCandle() {
            return this == SINGLE || this == TOP;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
