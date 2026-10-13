package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.highlight.Highlights;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gives a highlighted monster's outline its color (the game asks for the team color when it draws one). */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$highlightColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide() && Highlights.isHighlighted(self)) cir.setReturnValue(Highlights.color(self));
    }
}
