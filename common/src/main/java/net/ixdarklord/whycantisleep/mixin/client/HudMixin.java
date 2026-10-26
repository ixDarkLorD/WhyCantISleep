package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.Eyelids;
import net.ixdarklord.whycantisleep.client.HidingClient;
import net.ixdarklord.whycantisleep.client.HidingMinigame;
import net.ixdarklord.whycantisleep.client.NightTerrorClient;
import net.ixdarklord.whycantisleep.client.hud.HudFade;
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
 * client, and keeps those the sleep HUD shows in its own card off the line above the hotbar. Also draws the player's
 * eyelids ({@link Eyelids#render}: hiding, or in bed) before anything else, under the hotbar and the rest of the HUD,
 * and with the HUD hidden (F1) too. The rest of the HUD is drawn faded ({@link HudFade}) while the heartbeat minigame
 * runs, the crosshair with it, and the crosshair fades out while hiding; and the game's fade to dark in bed waits while a night terror
 * runs.
 */
@Mixin(Hud.class)
public abstract class HudMixin {
    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$onOverlayMessage(Component message, boolean animate, CallbackInfo ci) {
        if (SleepClient.onOverlayMessage(message)) ci.cancel();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void whycantisleep$squint(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Eyelids.render(graphics, deltaTracker);
        // Then the rest of the HUD, faded while the heartbeat minigame has the screen.
        HudFade.set(HidingMinigame.hudVisible(deltaTracker));
        HudFade.begin();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void whycantisleep$endFade(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        HudFade.end();
    }

    // The game's fade to dark in bed waits while a night terror runs: the terror is what they see.
    @Inject(method = "extractSleepOverlay", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$terrorHoldsTheDark(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (NightTerrorClient.running()) ci.cancel();
    }

    // The crosshair fades with the HUD in the minigame, and out while hiding (HudFade.crosshair); gone, not drawn.
    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$fadeCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        float shown = Math.min(HudFade.factor(), HidingClient.crosshair(deltaTracker.getGameTimeDeltaPartialTick(true)));
        if (shown <= 0.01F) {
            ci.cancel();
            return;
        }
        HudFade.crosshair(shown);
    }

    @Inject(method = "extractCrosshair", at = @At("RETURN"))
    private void whycantisleep$crosshairDone(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        HudFade.crosshair(1.0F);
    }
}