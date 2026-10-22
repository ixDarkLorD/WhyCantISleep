package net.ixdarklord.whycantisleep.mixin;

import net.ixdarklord.whycantisleep.hiding.Hiding;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A player hiding under a bed can't be picked out as a target, by target goals and sensors alike (all ask this), except
 * by bosses and the like ({@link Hiding#hiddenFrom}).
 */
@Mixin(TargetingConditions.class)
public abstract class TargetingConditionsMixin {
    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$hiddenUnderTheBed(ServerLevel level, @Nullable LivingEntity targeter, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (Hiding.hiddenFrom(targeter, target)) cir.setReturnValue(false);
    }
}
