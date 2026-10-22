package net.ixdarklord.whycantisleep.mixin;

import net.ixdarklord.whycantisleep.hiding.Hiding;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A mob that had a player as its target loses them once they hide under a bed (a target goal asks this to keep its
 * target), unless it is a boss or the like ({@link Hiding#hiddenFrom}).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$hiddenUnderTheBed(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (Hiding.hiddenFrom((LivingEntity) (Object) this, target)) cir.setReturnValue(false);
    }
}
