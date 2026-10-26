package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.NightTerrorClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The game's fade to dark in bed waits while a night terror runs: the terror is what they see. The fade is drawn from
 * the local player's sleep timer, inline in the HUD on Fabric and in Forge's own HUD, so the timer reads as not yet
 * started for the HUD while the terror runs (the server keeps its own).
 */
@Mixin(Player.class)
public abstract class PlayerSleepTimerMixin {
    @Inject(method = "getSleepTimer", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$terrorHoldsTheDark(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof LocalPlayer && NightTerrorClient.running()) cir.setReturnValue(0);
    }
}
