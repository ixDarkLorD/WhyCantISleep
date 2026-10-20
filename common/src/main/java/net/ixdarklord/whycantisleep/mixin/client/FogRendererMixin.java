package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.between.client.LabyrinthAtmosphere;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The labyrinth's fog, laid over the game's own once it has been worked out (LabyrinthAtmosphere). */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void whycantisleep$labyrinthFog(Camera camera, int renderDistance, DeltaTracker deltaTracker, float darken, ClientLevel level,
                                            CallbackInfoReturnable<FogData> cir) {
        LabyrinthAtmosphere.fog(cir.getReturnValue());
    }
}
