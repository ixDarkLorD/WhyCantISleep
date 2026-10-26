package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.hud.HudFade;
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fades each fill the HUD draws, as it is made ({@link HudFade}). Only the full constructor: the short one passes through it. */
@Mixin(ColoredRectangleRenderState.class)
public abstract class ColoredRectangleRenderStateMixin {
    @Shadow @Final @Mutable
    private int col1;
    @Shadow @Final @Mutable
    private int col2;

    @Inject(method = "<init>(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2fc;IIIIIILnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V",
            at = @At("TAIL"))
    private void whycantisleep$fade(CallbackInfo ci) {
        this.col1 = HudFade.fade(this.col1, false);
        this.col2 = HudFade.fade(this.col2, false);
    }
}
