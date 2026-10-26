package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.NightTerrorClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A night terror's dense fog, rolling in and out: the game's fog, closed in round the bed ({@link NightTerrorClient#fog}),
 * in its own dark color ({@link NightTerrorClient#fogColor}). 1.20.1 sets both straight on the shaders.
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private static void whycantisleep$terrorFog(Camera camera, FogRenderer.FogMode fogMode, float farPlaneDistance, boolean thick, float partialTick,
                                                CallbackInfo ci) {
        NightTerrorClient.fog(partialTick);
    }

    @Inject(method = "levelFogColor", at = @At("RETURN"))
    private static void whycantisleep$terrorFogColor(CallbackInfo ci) {
        NightTerrorClient.fogColor(Minecraft.getInstance().getFrameTime());
    }
}
