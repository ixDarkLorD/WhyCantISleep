package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.SleepStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The sleep status button in the inventory's player preview, and its popup ({@link SleepStatus}), drawn over the screen. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void whycantisleep$closeSleepStatus(CallbackInfo ci) {
        SleepStatus.reset();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void whycantisleep$sleepStatus(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) this;
        SleepStatus.render(graphics, Minecraft.getInstance().font, screen.whycantisleep$leftPos(), screen.whycantisleep$topPos(), mouseX, mouseY);
    }
}
