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
 * target), unless it is a boss or the like ({@link Hiding#hiddenFrom}). Also tells of a player being hurt, before it
 * wakes them, as CoolCatLib 1.20.1 has no damage event: it brings a hider out, and wakes a sleeper as hurt.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "hurt", at = @At("HEAD"))
    private void whycantisleep$onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player) || amount <= 0.0F || player.isInvulnerableTo(source) || player.isDeadOrDying()) return;
        SleepNights.onHurt(player);
        HidingServer.onHurt(player, amount);
    }

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$hiddenUnderTheBed(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (Hiding.hiddenFrom((LivingEntity) (Object) this, target)) cir.setReturnValue(false);
    }
}
