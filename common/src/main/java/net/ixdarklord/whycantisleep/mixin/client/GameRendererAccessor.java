package net.ixdarklord.whycantisleep.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The field of view the world is drawn with, for placing HUD pointers over it; 1.21.1's camera doesn't keep it. */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Invoker("getFov")
    double whycantisleep$getFov(Camera camera, float partialTick, boolean useFovSetting);
}
