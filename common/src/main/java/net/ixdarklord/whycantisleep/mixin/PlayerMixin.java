package net.ixdarklord.whycantisleep.mixin;

import net.ixdarklord.whycantisleep.between.DarkBetween;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A sleeper slipping under into the Dark Between hasn't slept long enough to end the night, however long they have
 * lain there: otherwise the night would pass before they went under, and wake them.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "isSleepingLongEnough", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$notWhileSlipping(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide() && DarkBetween.isSlipping(player)) cir.setReturnValue(false);
    }
}
