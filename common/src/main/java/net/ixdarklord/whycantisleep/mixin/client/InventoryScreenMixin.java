package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.SleepStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The sleep status button in the inventory's player preview, and its popup ({@link SleepStatus}), drawn over the
 * screen; and the inventory's clicks, before the recipe book or the slots see them: one on the button is its own.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void whycantisleep$closeSleepStatus(CallbackInfo ci) {
        SleepStatus.reset();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void whycantisleep$sleepStatus(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) this;
        SleepStatus.render(graphics, Minecraft.getInstance().font, screen.whycantisleep$leftPos(), screen.whycantisleep$topPos(), mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$sleepStatusClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) this;
        if (SleepStatus.click(screen.whycantisleep$leftPos(), screen.whycantisleep$topPos(), mouseX, mouseY, button)) cir.setReturnValue(true);
    }
}
