package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.highlight.Highlights;
import net.ixdarklord.whycantisleep.deep.client.DreamTransition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Draws the monsters that blocked a sleep with an outline, for this player only; and keeps the bed's "Leave Bed" screen
 * away while a dreamer slips into the Lucid Deep (the game would open it again every tick they lie there).
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$noBedScreenWhileDrifting(@Nullable Screen screen, CallbackInfo ci) {
        if (screen instanceof InBedChatScreen && DreamTransition.isDrifting()) ci.cancel();
    }

    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void whycantisleep$highlightBlockers(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && Highlights.isHighlighted(entity)) cir.setReturnValue(true);
    }
}
