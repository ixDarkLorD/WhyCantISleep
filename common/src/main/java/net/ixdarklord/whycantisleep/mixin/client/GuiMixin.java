package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.HidingClient;
import net.ixdarklord.whycantisleep.client.SleepClient;
import net.ixdarklord.whycantisleep.client.hud.HudFade;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sees the game's sleep messages as they are about to be shown, which is all a server without the mod tells the
 * client, and keeps those the sleep HUD shows in its own card off the line above the hotbar. The crosshair fades with
 * the HUD in the heartbeat minigame and out while hiding ({@link HudFade}); both loaders draw it through
 * {@code renderCrosshair}. The eyelids and the HUD's fade itself are {@code GameRendererMixin}'s: Forge draws its HUD
 * without {@code Gui.render}.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$onOverlayMessage(Component message, boolean animate, CallbackInfo ci) {
        if (SleepClient.onOverlayMessage(message)) ci.cancel();
    }

    // The game sets the shader color back to white after its vignette and its effect icons, which would undo the
    // HUD's fade (the shader color's alpha, HudFade) for all drawn after them: the fade's color is put back.
    @Inject(method = "renderVignette", at = @At("RETURN"))
    private void whycantisleep$fadeAfterVignette(GuiGraphics graphics, Entity entity, CallbackInfo ci) {
        HudFade.restore(graphics);
    }

    @Inject(method = "renderEffects", at = @At("RETURN"))
    private void whycantisleep$fadeAfterEffects(GuiGraphics graphics, CallbackInfo ci) {
        HudFade.restore(graphics);
    }

    // The crosshair fades with the HUD in the minigame, and out while hiding (HudFade.crosshair); gone, not drawn.
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$fadeCrosshair(GuiGraphics graphics, CallbackInfo ci) {
        float shown = Math.min(HudFade.factor(), HidingClient.crosshair(Minecraft.getInstance().getFrameTime()));
        if (shown <= 0.01F) {
            ci.cancel();
            return;
        }
        HudFade.crosshair(graphics, shown);
    }

    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void whycantisleep$crosshairDone(GuiGraphics graphics, CallbackInfo ci) {
        HudFade.crosshair(graphics, 1.0F);
    }
}
