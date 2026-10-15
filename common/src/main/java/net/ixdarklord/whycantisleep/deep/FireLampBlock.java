package net.ixdarklord.whycantisleep.deep;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.EntityBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The dread fire lamp: a tall lamp post, {@value #HEIGHT} blocks high on a single block's footing: a stepped plinth
 * ({@link Part#BASE}), a fluted shaft ({@link Part#SHAFT}), and at the top a lantern cage with a fire in it under a
 * peaked roof ({@link Part#TOP}). Set down, it raises the rest of itself (if there is room); break any part and it
 * all comes down. Its top keeps a block entity only so the client can find the lamps in view and lay a soft bloom of
 * light round each fire (FlameBloomClient).
 */
public class FireLampBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<FireLampBlock> CODEC = simpleCodec(FireLampBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final int HEIGHT = 4;
    private static final VoxelShape BASE = Shapes.or(Block.box(2, 0, 2, 14, 3, 14), Block.box(4.5, 3, 4.5, 11.5, 16, 11.5));
    private static final VoxelShape SHAFT = Block.box(4.5, 0, 4.5, 11.5, 16, 11.5);
    private static final VoxelShape TOP = Shapes.or(Block.box(4.5, 0, 4.5, 11.5, 2, 11.5), Block.box(2, 2, 2, 14, 16, 14));

    public enum Part implements StringRepresentable {
        BASE, SHAFT, TOP;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public FireLampBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.BASE));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(PART)) {
            case BASE -> BASE;
            case SHAFT -> SHAFT;
            case TOP -> TOP;
        };
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == Part.TOP ? new FireLampBlockEntity(pos, state) : null;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        for (int i = 1; i < HEIGHT; i++) {
            if (pos.getY() + i >= level.getMaxY() || !level.getBlockState(pos.above(i)).canBeReplaced(context)) return null;
        }
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        for (int i = 1; i < HEIGHT; i++) {
            level.setBlock(pos.above(i), state.setValue(PART, i == HEIGHT - 1 ? Part.TOP : Part.SHAFT), Block.UPDATE_ALL);
        }
    }

    // Each part needs the lamp above and below it where it should be; without them it falls, and the rest with it.
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        Part part = state.getValue(PART);
        boolean lamp = neighbor.is(this);
        if (direction == Direction.UP && part != Part.TOP && !lamp) return Blocks.AIR.defaultBlockState();
        if (direction == Direction.DOWN && part != Part.BASE && !lamp) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(PART) != Part.TOP) return;
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
        double y = pos.getY() + 0.45 + random.nextDouble() * 0.2;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
        if (random.nextFloat() < 0.5F) level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.01, 0.0);
        if (random.nextFloat() < 0.25F) level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 0.0, 0.03, 0.0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }
}
