package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.SleepClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sees the game's sleep messages as they are shown, which is all a server without the mod tells the client. */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "setOverlayMessage", at = @At("HEAD"))
    private void whycantisleep$onOverlayMessage(Component message, boolean animate, CallbackInfo ci) {
        SleepClient.onOverlayMessage(message);
    }
}
