package net.ixdarklord.whycantisleep.config;

import net.ixdarklord.coolcatcore.api.config.type.EnumType;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Where on the screen a HUD element sits: the sleep debt moon's place ({@code debtMoonPosition}). */
public enum HudCorner implements EnumType.Displayable {
    TOP_LEFT(0.0F, false),
    TOP_CENTER(0.5F, false),
    TOP_RIGHT(1.0F, false),
    BOTTOM_LEFT(0.0F, true),
    BOTTOM_RIGHT(1.0F, true);

    // How far across the screen, 0 the left edge to 1 the right; and whether it sits at the bottom.
    private final float across;
    private final boolean bottom;

    HudCorner(float across, boolean bottom) {
        this.across = across;
        this.bottom = bottom;
    }

    /** Where something this wide starts across a screen this wide, kept a margin in from the edge it is at. */
    public int x(int screenWidth, int width, int margin) {
        if (this.across <= 0.0F) return margin;
        if (this.across >= 1.0F) return screenWidth - margin - width;
        return Math.round((screenWidth - width) * this.across);
    }

    /** Whether it sits at the bottom of the screen. */
    public boolean bottom() {
        return this.bottom;
    }

    /** Where something this tall starts down a screen this tall, kept a margin in from the edge it is at. */
    public int y(int screenHeight, int height, int margin) {
        return this.bottom ? screenHeight - margin - height : margin;
    }

    @Override
    public Component displayName() {
        return Component.translatable("config.whycantisleep.hud_corner." + this.name().toLowerCase(Locale.ROOT));
    }
}
