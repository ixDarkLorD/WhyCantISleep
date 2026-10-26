package net.ixdarklord.whycantisleep.client.hud;

import net.ixdarklord.coolcatcanvas.api.client.gui.FadedItems;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * Items drawn faded on the HUD, so a card fading in or out fades its icon with it, through CoolCatLib's
 * {@link FadedItems} (the game draws GUI items from an atlas at full opacity, with no way to ask for less). While the
 * HUD itself is faded ({@link HudFade}) its strength is pushed to {@code FadedItems} for the items, and multiplies
 * this.
 */
public final class ItemFade {
    private ItemFade() {
    }

    /** Draws an item, as {@code fakeItem} does, at an opacity from 0 to 1. */
    public static void item(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, float opacity) {
        FadedItems.render(graphics, stack, x, y, opacity);
    }
}
