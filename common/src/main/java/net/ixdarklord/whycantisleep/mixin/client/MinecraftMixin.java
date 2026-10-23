package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.highlight.Highlights;
import net.ixdarklord.whycantisleep.client.hud.GuiSprites;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/**
 * Draws the monsters that blocked a sleep with an outline, for this player only; and has the HUD's sprites read again
 * once resources reload ({@link GuiSprites}).
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void whycantisleep$highlightBlockers(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && Highlights.isHighlighted(entity)) cir.setReturnValue(true);
    }

    @Inject(method = "reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"))
    private void whycantisleep$rereadSprites(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        cir.getReturnValue().thenRun(GuiSprites::clear);
    }
}
