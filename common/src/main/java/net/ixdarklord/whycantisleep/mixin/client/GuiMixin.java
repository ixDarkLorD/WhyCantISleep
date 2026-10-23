package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.HidingClient;
import net.ixdarklord.whycantisleep.client.SleepClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sees the game's sleep messages as they are about to be shown, which is all a server without the mod tells the
 * client, and keeps those the sleep HUD shows in its own card off the line above the hotbar. Also draws a hiding
 * player's squint ({@link HidingClient#renderEyelids}) before anything else, under the hotbar and the rest of the HUD,
 * and with the HUD hidden (F1) too.
 */
@Mixin(Hud.class)
public abstract class GuiMixin {
    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$onOverlayMessage(Component message, boolean animate, CallbackInfo ci) {
        if (SleepClient.onOverlayMessage(message)) ci.cancel();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void whycantisleep$squint(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        HidingClient.renderEyelids(graphics, deltaTracker);
    }
}