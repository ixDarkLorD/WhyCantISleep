package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.coolcatcore.api.event.v2.common.InteractionEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractBedBlock;
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
            if (!(player.level().getBlockState(hit.getBlockPos()).getBlock() instanceof AbstractBedBlock)) return InteractionResult.PASS;
            player.getItemInHand(hand).consume(1, player);
            player.level().playSound(null, hit.getBlockPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.2F);
            return InteractionResult.SUCCESS;
        });
    }

    /** The name a name tag in this hand gives, or null when it isn't a named name tag. */
    public static @Nullable String name(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.NAME_TAG)) return null;
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        if (name == null) return null;
        String text = name.getString().strip();
        return text.isEmpty() ? null : text;
    }
}
