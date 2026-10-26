package net.ixdarklord.whycantisleep.client.hud;

import net.ixdarklord.coolcatcanvas.api.client.gui.FadedItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * Items drawn faded on the HUD, so a card fading in or out fades its icon with it, through CoolCatLib's
 * {@link FadedItems}, which draws even solid items (blocks, beds) see-through. On 1.20.1 the shader color holds the
 * HUD's fade while the HUD is drawn ({@link HudFade}), and the item's opacity scales it, so the two go together; the
 * game's own items fade with the HUD the same way (CoolCatLib draws any GUI item see-through while that alpha is
 * under 1).
 */
public final class ItemFade {
    private ItemFade() {
    }

    /** Draws an item, as {@code renderFakeItem} does, at an opacity from 0 to 1. */
    public static void item(GuiGraphics graphics, ItemStack stack, int x, int y, float opacity) {
        FadedItems.render(graphics, stack, x, y, opacity);
    }
}
