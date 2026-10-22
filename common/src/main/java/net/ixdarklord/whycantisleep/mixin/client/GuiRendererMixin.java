package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.hud.ItemFade;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blits a GUI item at the opacity it was drawn at ({@link ItemFade}), so a fading card's icon fades with it. The item
 * atlas is premultiplied, so the color scales every channel, not only alpha.
 */
@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    // The item being blitted, between the two injections below (one render thread, one item at a time).
    @Unique
    private static float whycantisleep$itemAlpha = 1.0F;

    @Inject(method = "submitBlitFromItemAtlas", at = @At("HEAD"))
    private void whycantisleep$readAlpha(GuiItemRenderState itemState, GuiItemAtlas.SlotView slotView, CallbackInfo ci) {
        // The state is a final class, so the interface GuiItemRenderStateMixin adds is reached through Object.
        whycantisleep$itemAlpha = (Object) itemState instanceof ItemFade.Faded faded ? faded.whycantisleep$alpha() : 1.0F;
    }

    @ModifyArg(method = "submitBlitFromItemAtlas",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;addBlitToCurrentLayer(Lnet/minecraft/client/renderer/state/gui/BlitRenderState;)V"))
    private BlitRenderState whycantisleep$fade(BlitRenderState blit) {
        float alpha = whycantisleep$itemAlpha;
        if (alpha >= 1.0F) return blit;
        return new BlitRenderState(blit.pipeline(), blit.textureSetup(), blit.pose(), blit.x0(), blit.y0(), blit.x1(), blit.y1(),
                blit.u0(), blit.u1(), blit.v0(), blit.v1(), ARGB.colorFromFloat(alpha, alpha, alpha, alpha), blit.scissorArea(), blit.bounds());
    }
}
