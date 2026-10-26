package net.ixdarklord.whycantisleep.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ixdarklord.whycantisleep.client.NightTerrorClient;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A night terror's dense fog, rolling in and out: the game's fog, closed in round the bed ({@link NightTerrorClient#fog}),
 * and its color drawn towards the terror's ({@link NightTerrorClient#fogColor}). 1.21.1 sets its fog straight into the
 * render system, so it is changed there once the game has set it.
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Shadow
    private static float fogRed;

    @Shadow
    private static float fogGreen;

    @Shadow
    private static float fogBlue;

    @Inject(method = "setupFog", at = @At("RETURN"))
    private static void whycantisleep$terrorFog(Camera camera, FogRenderer.FogMode mode, float farPlaneDistance, boolean shouldCreateFog, float partialTick,
                                                CallbackInfo ci) {
        NightTerrorClient.fog(partialTick);
    }

    @Inject(method = "setupColor", at = @At("RETURN"))
    private static void whycantisleep$terrorFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darken, CallbackInfo ci) {
        float[] rgb = {fogRed, fogGreen, fogBlue};
        if (!NightTerrorClient.fogColor(rgb, partialTick)) return;
        fogRed = rgb[0];
        fogGreen = rgb[1];
        fogBlue = rgb[2];
        RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
    }
}
