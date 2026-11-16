package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.highlight.Highlights;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Draws the monsters that blocked a sleep with an outline, for this player only. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void whycantisleep$highlightBlockers(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && Highlights.isHighlighted(entity)) cir.setReturnValue(true);
    }
}
