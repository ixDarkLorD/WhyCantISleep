package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.coolcatcore.api.event.v2.common.InteractionEvents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import org.jetbrains.annotations.Nullable;

/**
 * Naming a bed: using a named name tag on a bed names it instead of lying down. The name is the player's own, kept with
 * the beds they know (the client's {@code BedBookmarks}); the server only takes the tag, as naming a mob does.
 */
public final class BedNaming {
    private BedNaming() {
    }

    public static void init() {
        InteractionEvents.USE_BLOCK.register((player, hand, hit) -> {
            if (player.level().isClientSide() || name(player, hand) == null) return InteractionResult.PASS;
            if (!(player.level().getBlockState(hit.getBlockPos()).getBlock() instanceof BedBlock)) return InteractionResult.PASS;
            if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
            player.level().playSound(null, hit.getBlockPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.2F);
            return InteractionResult.SUCCESS;
        });
    }

    /** The name a name tag in this hand gives, or null when it isn't a named name tag. */
    public static @Nullable String name(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // 1.20.1 keeps a stack's name in its NBT: only a name given at an anvil counts.
        if (!stack.is(Items.NAME_TAG) || !stack.hasCustomHoverName()) return null;
        String text = stack.getHoverName().getString().strip();
        return text.isEmpty() ? null : text;
    }
}
