package net.ixdarklord.whycantisleep.between;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The haunted dining table: a long table of sleepwood under a tattered runner. Set side by side along its length
 * ({@link #AXIS}), its blocks join into one table: where a neighbour along it is another table ({@link #NEG} toward
 * the axis's negative end, {@link #POS} toward its positive), that end is open and the top runs on; where it is not,
 * the table ends there, its legs under it and its runner hanging over the edge.
 */
public class DiningTableBlock extends Block {
    public static final MapCodec<DiningTableBlock> CODEC = simpleCodec(DiningTableBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty NEG = BooleanProperty.create("neg");
    public static final BooleanProperty POS = BooleanProperty.create("pos");
    private static final VoxelShape TOP = Block.box(0, 13, 0, 16, 16, 16);

    public DiningTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Z).setValue(NEG, false).setValue(POS, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // The top, and a leg at each corner of an end (simplified to the top's outline down to the floor at its ends).
        VoxelShape shape = TOP;
        boolean x = state.getValue(AXIS) == Direction.Axis.X;
        if (!state.getValue(NEG)) shape = Shapes.or(shape, x ? Block.box(1, 0, 1, 3, 13, 15) : Block.box(1, 0, 1, 15, 13, 3));
        if (!state.getValue(POS)) shape = Shapes.or(shape, x ? Block.box(13, 0, 1, 15, 13, 15) : Block.box(1, 0, 13, 15, 13, 15));
        return shape;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
        return this.joined(state, context.getLevel(), context.getClickedPos());
    }

    private BlockState joined(BlockState state, BlockGetter level, BlockPos pos) {
        Direction.Axis axis = state.getValue(AXIS);
        Direction neg = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE);
        return state.setValue(NEG, this.joins(level.getBlockState(pos.relative(neg)), axis))
                .setValue(POS, this.joins(level.getBlockState(pos.relative(neg.getOpposite())), axis));
    }

    private boolean joins(BlockState neighbour, Direction.Axis axis) {
        return neighbour.is(this) && neighbour.getValue(AXIS) == axis;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        Direction.Axis axis = state.getValue(AXIS);
        if (direction.getAxis() != axis) return state;
        return state.setValue(direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? NEG : POS, this.joins(neighbour, axis));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, NEG, POS);
    }
}
