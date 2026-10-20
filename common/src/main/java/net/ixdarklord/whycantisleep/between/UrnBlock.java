package net.ixdarklord.whycantisleep.between;

import com.mojang.serialization.MapCodec;
import net.ixdarklord.whycantisleep.between.mob.HauntedUrnEntity;
import net.ixdarklord.whycantisleep.lurker.WCISEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An urn of the labyrinth (four shapes: the urn, a small jar, a tall amphora, a wide lidded jar). Broken, it spills
 * what was kept in it (its loot table: mostly rubbish, now and then a little of worth; with silk touch, the urn itself).
 * Now and then one is haunted: as it breaks it does not shatter but rises, a {@link HauntedUrnEntity}, and comes for
 * whoever broke it. Any urn now and then stirs (a faint rattle, a soul fleck), so none is broken without a doubt.
 */
public class UrnBlock extends DecorBlock {
    public static final MapCodec<UrnBlock> CODEC = simpleCodec(properties -> new UrnBlock(properties, Shapes.block()));
    /** One urn in so many is haunted. */
    private static final int HAUNTED_ONE_IN = 14;

    public UrnBlock(BlockBehaviour.Properties properties, VoxelShape north) {
        super(properties, north, false);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        // Now and then any urn stirs: a faint rattle, a soul fleck rising from its mouth. Which of them is haunted is
        // never known until one is broken, so none can be broken without a doubt.
        if (random.nextInt(600) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.DECORATED_POT_HIT, SoundSource.BLOCKS,
                    0.25F, 0.4F + random.nextFloat() * 0.2F, false);
            level.addParticle(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 0.0, 0.02, 0.0);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server && !player.isCreative() && server.getRandom().nextInt(HAUNTED_ONE_IN) == 0) {
            HauntedUrnEntity urn = WCISEntities.HAUNTED_URN.get().create(server, EntitySpawnReason.TRIGGERED);
            if (urn != null) {
                urn.setUrn(state);
                urn.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, server.getRandom().nextFloat() * 360.0F, 0.0F);
                urn.setTarget(player);
                server.addFreshEntity(urn);
                // It does not break: the block is gone from under the pick (so nothing drops), and the urn rises.
                server.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                server.playSound(null, pos, SoundEvents.DECORATED_POT_HIT, SoundSource.BLOCKS, 1.0F, 0.5F);
                server.playSound(null, pos, SoundEvents.VEX_CHARGE, SoundSource.HOSTILE, 1.0F, 0.6F);
                server.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.3, 0.4, 0.3, 0.02);
                return state;
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
