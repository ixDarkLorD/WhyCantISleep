package net.ixdarklord.whycantisleep.client.hud;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * The look the mod's HUD shares: panels and bars of its own, drawn in the game's manner (beveled, pixel for pixel,
 * with the chat's colors for text). They are GUI sprites that stretch to any size, drawn by
 * {@code tools/gen_hud_textures.ps1}; a resource pack can restyle them like any of the game's.
 */
public final class HudStyle {
    /** The message card and the sleep board: dusk plum, outlined, shaded and shadowed the way the gauges are. */
    public static final Identifier CARD = WhyCantISleep.id("hud/card");
    /**
     * The mood gauge: a gold coin for its icon, and a round capsule of blocks for its bar. A bar is three layers: the
     * track (its outline and empty blocks), the fill, and the frame over both (the outline again, seams and glints).
     */
    public static final Identifier MOOD_MEDAL = WhyCantISleep.id("hud/mood_medal");
    public static final Identifier MOOD_TRACK = WhyCantISleep.id("hud/mood_track");
    public static final Identifier MOOD_FRAME = WhyCantISleep.id("hud/mood_frame");
    /** The lucidity gauge: a rose diamond for its icon, and a capsule that runs on into a point. */
    public static final Identifier LUCIDITY_MEDAL = WhyCantISleep.id("hud/lucidity_medal");
    public static final Identifier LUCIDITY_TRACK = WhyCantISleep.id("hud/lucidity_track");
    public static final Identifier LUCIDITY_FRAME = WhyCantISleep.id("hud/lucidity_frame");
    /** A medal's size, and a track's (the bar, and a pixel more each way for its shadow): drawn as they are, never stretched. */
    public static final int MEDAL_SIZE = 19;
    public static final int TRACK_WIDTH = 73;
    public static final int TRACK_HEIGHT = 12;
    /** A gauge's fill: it runs along the track from 2 in, 1 down, eight blocks of 8 long. */
    public static final int GAUGE_FILL_WIDTH = 64;
    public static final int GAUGE_FILL_HEIGHT = 9;
    /** How far in from a medal's corner its 9 by 9 icon sits. */
    public static final int MEDAL_INSET = 5;
    /** The sunk slot a card's icon sits in: {@link #SLOT_SIZE} square, for a 16 by 16 item 2 in from its corner. */
    public static final Identifier SLOT = WhyCantISleep.id("hud/slot");
    public static final int SLOT_SIZE = 20;
    private static final Identifier BAR_BACK = WhyCantISleep.id("hud/bar_back");
    /** The plaque that hangs under each gauge with its reading on: a panel like the card, in the gauge's colors. */
    public static final Identifier MOOD_SIGN = WhyCantISleep.id("hud/mood_sign");
    public static final Identifier LUCIDITY_SIGN = WhyCantISleep.id("hud/lucidity_sign");
    /** What covers the part of a gauge the other bar closes off: dim diagonal stripes, tiled along it. */
    public static final Identifier MOOD_LIMIT = WhyCantISleep.id("hud/mood_limit");
    public static final Identifier LUCIDITY_LIMIT = WhyCantISleep.id("hud/lucidity_limit");
    public static final Identifier MOOD_FILL = WhyCantISleep.id("hud/mood_fill");
    public static final Identifier LUCIDITY_FILL = WhyCantISleep.id("hud/lucidity_fill");
    public static final Identifier SLEEP_FILL = WhyCantISleep.id("hud/sleep_fill");
    /** How far a panel reaches beyond what it holds: its border, and a pixel of air inside it. */
    public static final int PANEL_PADDING = 5;
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

    /** A panel around a content area, which it pads by {@link #PANEL_PADDING} on every side. */
    public static void panel(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y, int width, int height, float alpha) {
        // A pixel more each way than the panel itself, for the shadow it casts.
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x - PANEL_PADDING, y - PANEL_PADDING, width + PANEL_PADDING * 2 + 1, height + PANEL_PADDING * 2 + 1,
                ARGB.white(Mth.clamp(alpha, 0.0F, 1.0F)));
    }

    /** The card around a content area. */
    public static void box(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float alpha) {
        panel(graphics, CARD, x, y, width, height, alpha);
    }

    /** A medal, to hold a 9 by 9 icon {@link #MEDAL_INSET} in from its corner. */
    public static void medal(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y, float alpha) {
        // The picture is a pixel bigger each way than the medal, for the shadow it casts.
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, MEDAL_SIZE + 1, MEDAL_SIZE + 1, ARGB.white(Mth.clamp(alpha, 0.0F, 1.0F)));
    }

    /** An icon's slot, drawn as it is. */
    public static void slot(GuiGraphicsExtractor graphics, int x, int y, float alpha) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, x, y, SLOT_SIZE, SLOT_SIZE, ARGB.white(Mth.clamp(alpha, 0.0F, 1.0F)));
    }

    /** A bar of a width, {@link #BAR_HEIGHT} tall, filled to a share with one of the fills. */
    public static void bar(GuiGraphicsExtractor graphics, Identifier fill, int x, int y, int width, float filled, float alpha) {
        int tint = ARGB.white(Mth.clamp(alpha, 0.0F, 1.0F));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_BACK, x, y, width, BAR_HEIGHT, tint);
        // The fill keeps inside the track's outline, and shows at least a sliver of anything above nothing.
        int inner = width - 2;
        int filledWidth = filled <= 0.0F ? 0 : Math.max(1, Math.round(inner * Mth.clamp(filled, 0.0F, 1.0F)));
        if (filledWidth > 0) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, fill, x + 1, y, filledWidth, BAR_HEIGHT, tint);
    }
}
