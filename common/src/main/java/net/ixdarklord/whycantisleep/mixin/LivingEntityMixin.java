package net.ixdarklord.whycantisleep.mixin;

import net.ixdarklord.whycantisleep.mind.MindServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hears of every death (CoolCatLib: Core has no death event): what a player killed is what they may feel guilty for. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void whycantisleep$onDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        // Players' deaths are heard in ServerPlayerMixin.
        if (!self.level().isClientSide() && !(self instanceof ServerPlayer)) MindServer.onKilled(self, source);
    }
}