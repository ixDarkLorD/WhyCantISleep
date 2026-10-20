package net.ixdarklord.whycantisleep.between;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * The Garden's furnishings, shaped by their models: an urn, a planter of dead flowers, a brazier. Each
 * turns to face whoever sets it down; its outline and collision follow its shape (given facing north, turned with it).
 * A burning one ({@link #burning}) throws up flame and smoke.
 */
public class DecorBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<DecorBlock> CODEC = simpleCodec(properties -> new DecorBlock(properties, Shapes.block(), false));
    private final Map<Direction, VoxelShape> shapes;
    private final boolean burning;

    public DecorBlock(BlockBehaviour.Properties properties, VoxelShape north, boolean burning) {
        super(properties);
        this.shapes = Shapes.rotateHorizontal(north);
        this.burning = burning;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapes.get(state.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!this.burning) return;
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
        if (random.nextFloat() < 0.6F) level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.015, 0.0);
        if (random.nextFloat() < 0.3F) level.addParticle(ParticleTypes.SMOKE, x, y + 0.2, z, 0.0, 0.03, 0.0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
