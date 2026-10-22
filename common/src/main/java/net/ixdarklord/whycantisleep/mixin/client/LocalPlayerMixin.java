package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.HidingClient;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Under a bed the player can look around but not move or jump; sneaking brings them out ({@code HidingClient}). */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "applyInput", at = @At("TAIL"))
    private void whycantisleep$stillWhileHiding(CallbackInfo ci) {
        if (!HidingClient.isLocalHiding()) return;
        LocalPlayer self = (LocalPlayer) (Object) this;
        self.xxa = 0.0F;
        self.zza = 0.0F;
        self.setJumping(false);
    }
}
