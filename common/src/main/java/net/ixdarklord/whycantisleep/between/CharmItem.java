package net.ixdarklord.whycantisleep.between;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Something taken from one of the Dark Between's creatures that still does what it did (WildCharms has each one's
 * effect): used, it works its effect, then is spent (one off the stack) or, if it wears, takes a point of wear; either
 * way it rests a while before it works again.
 */
public class CharmItem extends Item {
    /** What it does, on the server; false if it had nothing to do (nothing is spent then). */
    @FunctionalInterface
    public interface Effect {
        boolean apply(ServerLevel level, Player player, InteractionHand hand);
    }

    private final Effect effect;
    private final int cooldown;
    private final SoundEvent sound;
    private final float pitch;

    public CharmItem(Item.Properties properties, Effect effect, int cooldown, SoundEvent sound, float pitch) {
        super(properties);
        this.effect = effect;
        this.cooldown = cooldown;
        this.sound = sound;
        this.pitch = pitch;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (!this.effect.apply(server, player, hand)) return InteractionResult.FAIL;
        server.playSound(null, player.getX(), player.getY(), player.getZ(), this.sound, SoundSource.PLAYERS, 1.0F, this.pitch);
        if (this.cooldown > 0) player.getCooldowns().addCooldown(stack, this.cooldown);
        if (stack.isDamageableItem()) {
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        } else if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
