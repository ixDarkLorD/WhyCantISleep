package net.ixdarklord.whycantisleep.client.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/** The look the mod's HUD cards share: a dark glass panel with an accent color down its left edge. */
public final class HudStyle {
    public static final int PANEL = 0x0D0D14;
    public static final int TEXT = 0xFFFFFF;
    public static final int TEXT_MUTED = 0xA6A6B8;
    public static final int GOOD = 0x6FD06F;
    public static final int TRACK = 0x2A2A3A;
    public static final int PADDING = 6;

    private HudStyle() {
    }

    /** A color with an opacity, 0 to 1. Text drawn with almost no alpha is skipped by the game, which is what a fade wants. */
    public static int color(int rgb, float alpha) {
        return ARGB.color(Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F), rgb);
    }

    /** The card: its glass, a faint wash of the accent from the top, a thin frame and the accent bar on the left. */
    public static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int accent, float alpha) {
        graphics.fill(x, y, x + width, y + height, color(PANEL, alpha * 0.84F));
        graphics.fillGradient(x, y, x + width, y + height, color(accent, alpha * 0.16F), color(accent, 0.0F));
        graphics.outline(x, y, width, height, color(mix(accent, PANEL, 0.55F), alpha * 0.9F));
        graphics.fill(x, y, x + 2, y + height, color(accent, alpha));
    }

    /** A thin bar filled to a share of its width. */
    public static void bar(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float filled, int rgb, float alpha) {
        graphics.fill(x, y, x + width, y + height, color(TRACK, alpha));
        int fill = Math.round(width * Mth.clamp(filled, 0.0F, 1.0F));
        if (fill > 0) graphics.fill(x, y, x + fill, y + height, color(rgb, alpha));
    }

    /** One color moved towards another by a share, 0 to 1. */
    public static int mix(int from, int to, float share) {
        return ARGB.srgbLerp(share, ARGB.opaque(from), ARGB.opaque(to)) & 0xFFFFFF;
    }
}
