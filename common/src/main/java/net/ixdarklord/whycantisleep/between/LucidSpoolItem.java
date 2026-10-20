package net.ixdarklord.whycantisleep.between;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The Lucid Spool: a spool of thread spun from a lucid dream. Used, it casts a walkway of glowing dream-thread
 * ({@link DreamBridgeBlock}) out ahead of its user, level with the ground under their feet, the way they look, up to
 * {@link #REACH} blocks or until something stands in the way; the thread fades after a while. It wears with each cast.
 */
public class LucidSpoolItem extends Item {
    public static final int REACH = 24;

    public LucidSpoolItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack spool = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (look.lengthSqr() < 1.0E-4) return InteractionResult.FAIL;   // looking straight up or down: nowhere to go
        look = look.normalize();
        int y = (int) Math.floor(player.getY() - 0.01);                  // the block under their feet: the deck's top at them
        // The cells of the walkway, a step at a time along the look (so it runs straight even on a diagonal), stopping
        // where a block stands in the walkway or where the walker's head would be.
        Set<BlockPos> path = new LinkedHashSet<>();
        Vec3 from = player.position();
        for (double d = 0.5; d <= REACH; d += 0.35) {
            BlockPos at = BlockPos.containing(from.x + look.x * d, y + 0.5, from.z + look.z * d);
            if (!level.getBlockState(at.above()).canBeReplaced() || !level.getBlockState(at.above(2)).canBeReplaced()) break;
            path.add(at);
        }
        BlockState thread = WCISBlocks.DREAM_BRIDGE.get().defaultBlockState();
        int laid = 0;
        for (BlockPos at : path) {
            if (level.getBlockState(at).canBeReplaced()) {
                server.setBlock(at, thread, Block.UPDATE_ALL);
                if (laid++ % 3 == 0) {
                    server.sendParticles(ParticleTypes.END_ROD, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 2, 0.3, 0.05, 0.3, 0.01);
                }
            }
        }
        if (laid == 0) return InteractionResult.FAIL;
        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 0.7F);
        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.4F, 1.8F);
        player.getCooldowns().addCooldown(spool, 40);
        spool.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        return InteractionResult.SUCCESS;
    }
}
