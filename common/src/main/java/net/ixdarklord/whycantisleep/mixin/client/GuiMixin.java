package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.SleepClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Sees the game's sleep messages as they are shown, which is all a server without the mod tells the client, and puts
 * the mod's own line in their place.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @ModifyVariable(method = "setOverlayMessage", at = @At("HEAD"), argsOnly = true)
    private Component whycantisleep$onOverlayMessage(Component message) {
        return SleepClient.onOverlayMessage(message);
    }
}