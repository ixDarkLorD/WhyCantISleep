package net.ixdarklord.whycantisleep.between;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * The Dream Lens. Held while falling asleep with a clear mind, it takes its holder into the Dark Between. There, raised
 * to the eye and held while looking up at their Window, it focuses them on waking ({@link DarkBetween#focus}).
 */
public class DreamLensItem extends Item {
    // How long it takes to focus: two seconds of looking.
    private static final int FOCUS_TICKS = 40;

    public DreamLensItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!DarkBetween.isBetween(level)) return InteractionResult.PASS;
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return FOCUS_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.SPYGLASS;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) DarkBetween.focus(player);
        return stack;
    }
}
