package net.ixdarklord.whycantisleep.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ixdarklord.whycantisleep.client.Eyelids;
import net.ixdarklord.whycantisleep.client.HidingClient;
import net.ixdarklord.whycantisleep.client.HidingMinigame;
import net.ixdarklord.whycantisleep.client.hud.HudFade;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.GL11;
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
 * near plane drawn in close under a bed ({@link HidingClient#nearPlane}). The eyelids drawn as the GUI starts, before
 * the HUD and whether or not the HUD is hidden ({@link Eyelids#render}: hiding, or in bed), and then the HUD drawn faded
 * while the heartbeat minigame runs ({@link HudFade}): Forge draws its HUD without {@code Gui.render}, so the fade
 * opens and closes round the game's call to draw the HUD, on both loaders.
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
        Eyelids.render(graphics, partialTicks);
        graphics.flush();
        // The lids write depth, and the hotbar is drawn behind them (the game sets it 90 back): it would stay hidden
        // under a lid still drawn, while the rest of the HUD showed. The HUD is drawn over the lids, so their depth goes.
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        // Then the rest of the HUD, faded while the heartbeat minigame has the screen.
        HudFade.set(HidingMinigame.hudVisible(partialTicks));
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;F)V"))
    private void whycantisleep$beginFade(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        HudFade.begin(new GuiGraphics(this.minecraft, this.minecraft.renderBuffers().bufferSource()));
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;F)V",
            shift = At.Shift.AFTER))
    private void whycantisleep$endFade(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        HudFade.end(new GuiGraphics(this.minecraft, this.minecraft.renderBuffers().bufferSource()));
    }
}
