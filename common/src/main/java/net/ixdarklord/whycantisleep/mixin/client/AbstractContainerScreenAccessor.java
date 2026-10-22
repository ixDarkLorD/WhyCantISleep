package net.ixdarklord.whycantisleep.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Where a container screen's panel sits, for the sleep status button in the inventory's player preview. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int whycantisleep$leftPos();

    @Accessor("topPos")
    int whycantisleep$topPos();
}
