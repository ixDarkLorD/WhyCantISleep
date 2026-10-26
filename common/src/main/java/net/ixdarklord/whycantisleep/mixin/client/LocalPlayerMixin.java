package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.HidingClient;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Under a bed the player can look around but not move or jump; sneaking brings them out ({@code HidingClient}). Tucked
 * in a chest or under a bed they are inside a block, which the game would push them out of: not while hiding, or the
 * server snaps them back each tick and the view flickers.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "serverAiStep", at = @At("TAIL"))
    private void whycantisleep$stillWhileHiding(CallbackInfo ci) {
        if (!HidingClient.isLocalHiding()) return;
        LocalPlayer self = (LocalPlayer) (Object) this;
        self.xxa = 0.0F;
        self.zza = 0.0F;
        self.setJumping(false);
    }

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$stayInTheSpot(double x, double z, CallbackInfo ci) {
        if (HidingClient.isLocalHiding()) ci.cancel();
    }
}
