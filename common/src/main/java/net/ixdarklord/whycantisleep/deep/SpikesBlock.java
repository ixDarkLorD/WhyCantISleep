package net.ixdarklord.whycantisleep.deep;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Dreamstone spikes: a cluster of sharp stone points, stained with what fell on them before. Whatever walks through them
 * is slowed and cut; whatever falls on them is hurt far worse than by the fall alone (as on pointed dripstone). They
 * line the floors of the Garden's pits.
 */
public class SpikesBlock extends Block {
    public static final MapCodec<SpikesBlock> CODEC = simpleCodec(SpikesBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 11, 15);

    public SpikesBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // Nothing stands on the points; it is among them.
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean past) {
        if (!(entity instanceof LivingEntity living)) return;
        entity.makeStuckInBlock(state, new Vec3(0.6, 0.75, 0.6));
        if (level instanceof ServerLevel server && (entity.xOld != entity.getX() || entity.zOld != entity.getZ() || server.getGameTime() % 20 == 0)) {
            living.hurtServer(server, server.damageSources().stalagmite(), 2.0F);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double distance) {
        if (level instanceof ServerLevel server) {
            entity.causeFallDamage(distance + 2.5, 2.0F, server.damageSources().stalagmite());
        }
    }
}
