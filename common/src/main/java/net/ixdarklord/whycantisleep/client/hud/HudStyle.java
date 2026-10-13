package net.ixdarklord.whycantisleep.client.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * The look the mod's HUD cards share, which is the game's own: the tooltip's box and frame, the boss bar for progress,
 * and the chat's colors for text. A resource pack that restyles those restyles these cards with them.
 */
public final class HudStyle {
    private static final Identifier TOOLTIP_BACKGROUND = Identifier.withDefaultNamespace("tooltip/background");
    private static final Identifier TOOLTIP_FRAME = Identifier.withDefaultNamespace("tooltip/frame");
    private static final Identifier BAR_BACKGROUND = Identifier.withDefaultNamespace("boss_bar/green_background");
    private static final Identifier BAR_PROGRESS = Identifier.withDefaultNamespace("boss_bar/green_progress");
    // The tooltip sprites carry an empty margin around the box, and the box pads its contents (TooltipRenderUtil).
    private static final int SPRITE_MARGIN = 9;
    private static final int BOX_PADDING = 3;
    /** The boss bar sprites' size: a bar is drawn at this width. */
    public static final int BAR_WIDTH = 182;
    public static final int BAR_HEIGHT = 5;

    // The chat's colors.
    public static final int WHITE = 0xFFFFFF;
    public static final int GRAY = 0xAAAAAA;
    public static final int DARK_GRAY = 0x555555;
    public static final int RED = 0xFF5555;
    public static final int GOLD = 0xFFAA00;
    public static final int YELLOW = 0xFFFF55;
    public static final int GREEN = 0x55FF55;
    public static final int AQUA = 0x55FFFF;
    public static final int LIGHT_PURPLE = 0xFF55FF;

    private HudStyle() {
    }

    /** A color with an opacity, 0 to 1. Text drawn with almost no alpha is skipped by the game, which is what a fade wants. */
    public static int color(int rgb, float alpha) {
        return ARGB.color(Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F), rgb);
    }

    /** The game's tooltip box around a content area, which it pads by 3 pixels on every side. */
    public static void box(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float alpha) {
        int outset = SPRITE_MARGIN + BOX_PADDING;
        int tint = ARGB.white(Mth.clamp(alpha, 0.0F, 1.0F));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TOOLTIP_BACKGROUND, x - outset, y - outset, width + outset * 2, height + outset * 2, tint);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TOOLTIP_FRAME, x - outset, y - outset, width + outset * 2, height + outset * 2, tint);
    }

    /** The green boss bar, {@link #BAR_WIDTH} wide, filled to a share. */
    public static void bar(GuiGraphicsExtractor graphics, int x, int y, float filled, float alpha) {
        int tint = ARGB.white(Mth.clamp(alpha, 0.0F, 1.0F));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_BACKGROUND, x, y, BAR_WIDTH, BAR_HEIGHT, tint);
        int width = Mth.lerpDiscrete(Mth.clamp(filled, 0.0F, 1.0F), 0, BAR_WIDTH);
        if (width > 0) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_PROGRESS, BAR_WIDTH, BAR_HEIGHT, 0, 0, x, y, width, BAR_HEIGHT, tint);
    }
}
