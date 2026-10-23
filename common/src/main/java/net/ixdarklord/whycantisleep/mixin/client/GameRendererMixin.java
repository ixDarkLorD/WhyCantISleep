package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.HidingClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No hand in view while hiding: under a bed it would reach up through the frame, and in a closet it holds still. The
 * near plane drawn in close under a bed ({@link HidingClient#nearPlane}), and the squint drawn as the GUI starts, before
 * the HUD and whether or not the HUD is hidden ({@link HidingClient#renderEyelids}).
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    Minecraft minecraft;

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$noHandUnderTheBed(CallbackInfo ci) {
        if (HidingClient.isLocalHiding()) ci.cancel();
    }

    @ModifyConstant(method = "getProjectionMatrix", constant = @Constant(floatValue = 0.05F))
    private float whycantisleep$nearPlaneUnderTheBed(float zNear) {
        return HidingClient.nearPlane(zNear);
    }

    @Inject(method = "render", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
            args = "ldc=gui"))
    private void whycantisleep$squint(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        GuiGraphics graphics = new GuiGraphics(this.minecraft, this.minecraft.renderBuffers().bufferSource());
        HidingClient.renderEyelids(graphics, partialTicks);
        graphics.flush();
    }
}
