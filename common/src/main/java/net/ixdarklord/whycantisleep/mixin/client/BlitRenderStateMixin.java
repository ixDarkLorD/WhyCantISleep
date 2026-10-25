package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.hud.HudFade;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fades each blit the HUD draws, as it is made ({@link HudFade}). Only the full constructor: the short one passes through it. */
@Mixin(BlitRenderState.class)
public abstract class BlitRenderStateMixin {
    @Shadow @Final @Mutable
    private int color;

    @Inject(method = "<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2f;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V",
            at = @At("TAIL"))
    private void whycantisleep$fade(CallbackInfo ci) {
        this.color = HudFade.fade(this.color, false);
    }
}
