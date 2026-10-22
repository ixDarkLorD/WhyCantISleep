package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.HidingClient;
import net.ixdarklord.whycantisleep.client.highlight.Highlights;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives a highlighted monster's outline its color (the game asks for the team color when it draws one), and keeps the
 * local player's look within what a chest's gap lets them see while they hide in one.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "turn", at = @At("TAIL"))
    private void whycantisleep$peekLimit(double xo, double yo, CallbackInfo ci) {
        HidingClient.limitLook((Entity) (Object) this);
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$highlightColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide() && Highlights.isHighlighted(self)) cir.setReturnValue(Highlights.color(self));
    }
}
