package net.ixdarklord.whycantisleep.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ixdarklord.coolcatcanvas.api.client.gui.ItemOpacityRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * Items drawn faded on the HUD, so a card fading in or out fades its icon with it, through CoolCatLib's
 * {@link ItemOpacityRenderer}, which draws even solid items (blocks, beds) see-through. The game's own items fade with the HUD
 * the same way ({@link HudFade} scales the shader color's alpha, and CoolCatLib draws any GUI item see-through while
 * it is under 1).
 */
public final class ItemFade {
    private ItemFade() {
    }

    /** Draws an item, as {@code renderFakeItem} does, at an opacity from 0 to 1. */
    public static void item(GuiGraphics graphics, ItemStack stack, int x, int y, float opacity) {
        ItemOpacityRenderer.render(graphics, stack, x, y, opacity);
        // ItemOpacityRenderer puts back the color it found, which RenderSystemMixin fades again as it is set: set the HUD's own.
        if (opacity < 1.0F) RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
