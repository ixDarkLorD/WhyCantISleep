package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.Eyelids;
import net.ixdarklord.whycantisleep.client.HidingClient;
import net.ixdarklord.whycantisleep.client.HidingMinigame;
import net.ixdarklord.whycantisleep.client.NightTerrorClient;
import net.ixdarklord.whycantisleep.client.hud.HudFade;
import net.ixdarklord.whycantisleep.client.SleepClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
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
// After other mods' injections at the HUD's tail (CoolCatLib's Forge HUD event among them), so the minigame is drawn last.
@Mixin(value = Gui.class, priority = 1500)
public abstract class GuiMixin {
    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$onOverlayMessage(Component message, boolean animate, CallbackInfo ci) {
        if (SleepClient.onOverlayMessage(message)) ci.cancel();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void whycantisleep$squint(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Eyelids.render(graphics, deltaTracker);
        // Then the rest of the HUD, faded while the heartbeat minigame has the screen.
        HudFade.set(HidingMinigame.hudVisible(deltaTracker));
        HudFade.begin();
    }

    // The heartbeat minigame, over the whole HUD; it ends the fade the HUD was drawn under.
    @Inject(method = "render", at = @At("TAIL"))
    private void whycantisleep$endFade(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        HidingMinigame.render(graphics, deltaTracker);
    }

    // The game's fade to dark in bed waits while a night terror runs: the terror is what they see.
    @Inject(method = "renderSleepOverlay", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$terrorHoldsTheDark(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (NightTerrorClient.running()) ci.cancel();
    }

    // The crosshair fades with the HUD in the minigame, and out while hiding (HudFade.crosshair); gone, not drawn.
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$fadeCrosshair(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        float shown = Math.min(HudFade.factor(), HidingClient.crosshair(deltaTracker.getGameTimeDeltaPartialTick(true)));
        if (shown <= 0.01F) {
            ci.cancel();
            return;
        }
        HudFade.crosshair(shown);
    }

    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void whycantisleep$crosshairDone(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        HudFade.crosshair(1.0F);
    }
}