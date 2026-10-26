package net.ixdarklord.whycantisleep.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ixdarklord.whycantisleep.client.hud.HudFade;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps blending on while the HUD is faded ({@link HudFade}): the fade is the shader color's alpha, which shows only
 * with blending, and the game (and an item drawn see-through, whose render type turns blending off after it) would
 * otherwise draw the rest of the HUD at full strength.
 */
@Mixin(value = RenderSystem.class, remap = false)
public abstract class RenderSystemMixin {
    @Inject(method = "disableBlend", at = @At("HEAD"), cancellable = true)
    private static void whycantisleep$keepBlendWhileFaded(CallbackInfo ci) {
        if (HudFade.factor() < 1.0F) ci.cancel();
    }
}
