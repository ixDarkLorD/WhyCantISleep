package net.ixdarklord.whycantisleep.client.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * Items drawn faded on the HUD. The game draws GUI items from an atlas at full opacity, with no way to ask for less, so
 * a card fading in or out had to hide its icon instead. Drawn through here, an item remembers the opacity it was asked
 * for ({@code GuiItemRenderStateMixin}) and is blitted with it ({@code GuiRendererMixin}).
 */
public final class ItemFade {
    /** The opacity of items being drawn right now; 1 outside {@link #item}. Read by the mixins. */
    public static float alpha = 1.0F;

    private ItemFade() {
    }

    /** Draws an item, as {@code fakeItem} does, at an opacity from 0 to 1. */
    public static void item(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, float opacity) {
        if (opacity <= 0.0F) return;
        alpha = Math.min(opacity, 1.0F);
        try {
            graphics.fakeItem(stack, x, y);
        } finally {
            alpha = 1.0F;
        }
    }

    /** An item render state that knows the opacity it was drawn at. */
    public interface Faded {
        float whycantisleep$alpha();
    }
}
