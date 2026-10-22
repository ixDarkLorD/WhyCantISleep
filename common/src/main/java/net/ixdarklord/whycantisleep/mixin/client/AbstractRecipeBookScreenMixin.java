package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.SleepStatus;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Clicks on the inventory, before the recipe book or the slots see them: one on the sleep status button is its own
 * ({@link SleepStatus#click}). The inventory takes its clicks here, in the recipe book screen it is one of.
 */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$sleepStatusClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof InventoryScreen)) return;
        AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) this;
        if (SleepStatus.click(screen.whycantisleep$leftPos(), screen.whycantisleep$topPos(), event.x(), event.y(), event.button())) cir.setReturnValue(true);
    }
}
