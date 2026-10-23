package net.ixdarklord.whycantisleep.mixin;

import net.ixdarklord.whycantisleep.hiding.Hiding;
import net.ixdarklord.whycantisleep.hiding.HidingServer;
import net.ixdarklord.whycantisleep.sleep.SleepNights;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A mob that had a player as its target loses them once they hide under a bed (a target goal asks this to keep its
 * target), unless it is a boss or the like ({@link Hiding#hiddenFrom}). Also hears a player being hurt, which
 * CoolCatLib-1.21 has no event for (26.1's {@code LivingEvents.DAMAGE}): it brings a hider out and marks a sleeper as
 * woken by it.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$hiddenUnderTheBed(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (Hiding.hiddenFrom((LivingEntity) (Object) this, target)) cir.setReturnValue(false);
    }

    // At the head, so a sleeper is marked before the hurt wakes them; damage they are immune to doesn't count.
    @Inject(method = "hurt", at = @At("HEAD"))
    private void whycantisleep$onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player) || player.isInvulnerableTo(source) || player.isDeadOrDying()) return;
        SleepNights.onHurt(player);
        if (amount > 0.0F) HidingServer.onHurt(player);
    }
}
