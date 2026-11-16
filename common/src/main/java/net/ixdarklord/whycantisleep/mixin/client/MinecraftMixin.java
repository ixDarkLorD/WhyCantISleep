package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.LucidDreamClient;
import net.ixdarklord.whycantisleep.client.highlight.Highlights;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Draws the monsters that blocked a sleep with an outline, for this player only; and holds back the in-bed screen while
 * a lucid dream runs, which the game would open again every tick, taking the keys and the mouse the dream drifts by.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void whycantisleep$highlightBlockers(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && Highlights.isHighlighted(entity)) cir.setReturnValue(true);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;openScreen(Lnet/minecraft/client/gui/components/ChatComponent$ChatMethod;Lnet/minecraft/client/gui/screens/ChatScreen$ChatConstructor;)V"))
    private void whycantisleep$noBedScreenInADream(ChatComponent chat, ChatComponent.ChatMethod method, ChatScreen.ChatConstructor<?> screen) {
        if (!LucidDreamClient.running()) chat.openScreen(method, screen);
    }
}
