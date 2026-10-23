package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.board.SleepBoard;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hands the in-bed screen's clicks to the sleep board first (the in-bed screen is a chat screen with a button). */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$clickSleepBoard(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof InBedChatScreen && SleepBoard.onClick(mouseX, mouseY, button)) cir.setReturnValue(true);
    }
}
