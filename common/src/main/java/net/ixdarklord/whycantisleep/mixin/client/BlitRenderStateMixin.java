package net.ixdarklord.whycantisleep.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.ixdarklord.whycantisleep.client.hud.HudFade;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fades each blit the HUD draws, as it is made ({@link HudFade}); the crosshair's, drawn inverted where an alpha does
 * nothing, by darkening its color instead. Only the full constructor: the short one passes through it.
 */
@Mixin(BlitRenderState.class)
public abstract class BlitRenderStateMixin {
    @Shadow @Final @Mutable
    private int color;

    @Inject(method = "<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2f;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V",
            at = @At("TAIL"))
    private void whycantisleep$fade(RenderPipeline pipeline, TextureSetup textures, Matrix3x2f pose, int x0, int y0, int x1, int y1, float u0, float u1,
                                    float v0, float v1, int argb, @Nullable ScreenRectangle scissor, @Nullable ScreenRectangle bounds, CallbackInfo ci) {
        this.color = HudFade.fade(this.color, false);
        float crosshair = HudFade.crosshair();
        if (pipeline == RenderPipelines.CROSSHAIR && crosshair < 1.0F) {
            this.color = ARGB.color(ARGB.alpha(this.color), ARGB.scaleRGB(this.color & 0xFFFFFF, crosshair));
        }
    }
}
