package net.ixdarklord.whycantisleep.client.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * Items drawn faded on the HUD. On 1.21.1 a GUI item is drawn straight away, so its opacity goes through the shader
 * color while it is drawn: flat items (drawn translucent) fade with it. Items drawn solid (most block items) can't be
 * seen through, so they are left out for the last moments of a fade instead.
 */
public final class ItemFade {
    // Below this, an item that can't fade is left out.
    private static final float SHOWN = 0.35F;

    private ItemFade() {
    }

    /** Draws an item, as {@code renderFakeItem} does, at an opacity from 0 to 1. */
    public static void item(GuiGraphics graphics, ItemStack stack, int x, int y, float opacity) {
        if (opacity <= 0.0F) return;
        if (opacity >= 1.0F) {
            graphics.renderFakeItem(stack, x, y);
            return;
        }
        if (opacity < SHOWN) return;
        graphics.setColor(1.0F, 1.0F, 1.0F, opacity);
        try {
            graphics.renderFakeItem(stack, x, y);
        } finally {
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
