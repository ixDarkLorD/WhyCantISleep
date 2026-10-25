package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.NightTerrorClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A night terror's dense fog, rolling in and out: the game's fog, closed in round the bed ({@link NightTerrorClient#fog}). */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void whycantisleep$terrorFog(Camera camera, int renderDistance, DeltaTracker delta, float darken, ClientLevel level,
                                         CallbackInfoReturnable<FogData> cir) {
        NightTerrorClient.fog(cir.getReturnValue(), delta.getGameTimeDeltaPartialTick(true));
    }
}
