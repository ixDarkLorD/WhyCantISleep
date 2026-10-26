package net.ixdarklord.whycantisleep.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * Items drawn faded on the HUD, so a card fading in or out fades its icon with it. On 1.20.1 a GUI item is drawn at
 * once, through the shaders' color, so the opacity is that color for the one item: items drawn translucent (most flat
 * ones) fade, and solid block models only darken.
 */
public final class ItemFade {
    private ItemFade() {
    }

    /** Draws an item, as {@code renderFakeItem} does, at an opacity from 0 to 1. */
    public static void item(GuiGraphics graphics, ItemStack stack, int x, int y, float opacity) {
        if (opacity <= 0.0F) return;
        // Faded with the HUD too ({@link HudFade}).
        float alpha = Math.min(opacity, 1.0F) * HudFade.factor();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(alpha, alpha, alpha, alpha);
        try {
            graphics.renderFakeItem(stack, x, y);
        } finally {
            HudFade.restore(graphics);
        }
    }
}
