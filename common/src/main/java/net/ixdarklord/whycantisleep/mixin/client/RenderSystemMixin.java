package net.ixdarklord.whycantisleep.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ixdarklord.whycantisleep.client.hud.HudFade;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fades what the HUD draws while {@link HudFade} is open: the alpha of each shader color set then is scaled by its
 * factor (every GUI shader multiplies the shader color in), and blending stays on, so that alpha shows on sprites the
 * game draws without it.
 */
@Mixin(value = RenderSystem.class, remap = false)
public abstract class RenderSystemMixin {
    @ModifyVariable(method = "setShaderColor", at = @At("HEAD"), ordinal = 3, argsOnly = true)
    private static float whycantisleep$fadeHud(float alpha) {
        return alpha * HudFade.factor();
    }

    @Inject(method = "disableBlend", at = @At("HEAD"), cancellable = true)
    private static void whycantisleep$keepBlendWhileFaded(CallbackInfo ci) {
        if (HudFade.factor() < 1.0F) ci.cancel();
    }
}
