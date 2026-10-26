package net.ixdarklord.whycantisleep.client.hud;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The look the mod's HUD shares: flat and dark (the author's request, 2026-10-21): charcoal panels with a hairline
 * border, split by thin dividers; flat bars; icons in small squares tinted the color of what they stand for; text
 * without shadows. The panels and the bars are GUI sprites that stretch to any size, drawn by
 * {@code tools/gen_hud_textures.ps1}, so a resource pack can restyle them like any of the game's; the dividers, dots and
 * icon squares are small flat fills.
 */
public final class HudStyle {
    /** A night's sleep card and the status popup: dusk smoke, grey with wavy edges ({@code tools/gen_frames.py}). */
    public static final Identifier CARD = WhyCantISleep.id("hud/card");
    /** The other message cards and the sleep board: moonlit, grey with a pale rim ({@code tools/gen_frames.py}). */
    public static final Identifier NOTICE = WhyCantISleep.id("hud/notice");
    /** The wash of a notice's color over its left side, fading to the right; how far across it reaches, how strong, and its specks. */
    public static final Identifier WASH = WhyCantISleep.id("hud/wash");
    private static final float WASH_SHARE = 0.8F;
    private static final float WASH_ALPHA = 0.24F;
    private static final int WASH_CLIMB = 6;
    /** The wash's foot and head, fading downwards and upwards as well, over this many rows each. */
    public static final Identifier WASH_FADE = WhyCantISleep.id("hud/wash_fade");
    public static final Identifier WASH_FADE_TOP = WhyCantISleep.id("hud/wash_fade_top");
    private static final int WASH_FADE_ROWS = 6;
    /** How far the wash keeps from the frame's top and bottom, inside its border. */
    private static final int WASH_INSET = 2;
    /** A notice's timer, frame and all, is this tall: an outline round a bar two high. */
    public static final int TIMER_HEIGHT = 4;
    /** A bar's empty track, and the sleep board's fill. */
    public static final Identifier BAR_BACK = WhyCantISleep.id("hud/bar_back");
    public static final Identifier SLEEP_FILL = WhyCantISleep.id("hud/sleep_fill");
    /** The square a card's icon sits in: {@link #SLOT_SIZE} across, for a 16 by 16 item 2 in from its corner. */
    public static final int SLOT_SIZE = 20;
    /** How far a panel reaches beyond what it holds: its border, and room to breathe inside it. */
    public static final int PANEL_PADDING = 6;
    /** Above and below, a little more, so the frames' top and bottom edges (rim, shading, shadow) have room. */
    public static final int PANEL_PADDING_Y = PANEL_PADDING + 2;
    /** How far a panel's grey is tinted towards its message's color: a touch, showing most on the bright rim. */
    private static final float PANEL_TINT = 0.45F;
    public static final int BAR_HEIGHT = 3;

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
    // The dark theme's own: a soft white for quiet titles, labels and hints a step back, the dimmest text, and lines.
    public static final int SILVER = 0xD4D4D4;
    public static final int MUTED = 0x6B6F78;
    public static final int FAINT = 0x4B4F57;
    public static final int DIVIDER = 0x22252B;
    public static final int TRACK = 0x1C1F24;

    private HudStyle() {
    }

    /** A color with an opacity, 0 to 1. Text drawn with almost no alpha is skipped by the game, which is what a fade wants. */
    public static int color(int rgb, float alpha) {
        return ARGB.color(Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F), rgb);
    }

    /**
     * A panel around a content area, which it pads by {@link #PANEL_PADDING} at the sides, {@link #PANEL_PADDING_Y}
     * above and below; its grey tinted a little ({@link #PANEL_TINT}) towards a color, its message's (as its wash).
     */
    public static void panel(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y, int width, int height, int rgb, float alpha) {
        int tint = ARGB.srgbLerp(PANEL_TINT, WHITE, rgb);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x - PANEL_PADDING, y - PANEL_PADDING_Y, width + PANEL_PADDING * 2, height + PANEL_PADDING_Y * 2,
                color(tint, alpha));
    }

    /** The card around a content area, tinted towards a color. */
    public static void box(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int rgb, float alpha) {
        panel(graphics, CARD, x, y, width, height, rgb, alpha);
    }

    /** How far in from a notice's and a card's edges a wash keeps, to stay under their rims (the card's top is wavy). */
    public static final int NOTICE_BORDER = 4;
    public static final int CARD_BORDER = 5;

    /** The notice frame around a content area, tinted towards a color. */
    public static void notice(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int rgb, float alpha) {
        panel(graphics, NOTICE, x, y, width, height, rgb, alpha);
    }

    /**
     * A wash of color over a notice's left side, fading out to the right ({@link #WASH}) and, over its last few rows,
     * downwards too ({@link #WASH_FADE}), drawn to a share of its reach, and specks of light drifting along it towards
     * where it fades, fading with it. It starts under the frame's top line and ends as far under the content (the
     * text) as it starts over it. The notice's content area given as for {@link #panel}, to where its text ends; age in
     * ticks, and a seed so each notice's specks are its own.
     */
    public static void wash(GuiGraphicsExtractor graphics, int x, int y, int width, int contentBottom, float reach, int rgb, float alpha, float age,
                            long seed) {
        wash(graphics, x, y, width, contentBottom, reach, rgb, alpha, age, seed, 0.5F, NOTICE_BORDER);
    }

    /**
     * A wash as above, its specks as lively as a share, 0 to 1 (a notice's are 0.5): livelier, there are more of them,
     * brighter, quicker, more of them little crosses, and they rise as they drift; duller, fewer, dimmer and slower,
     * and they sink. A night's sleep card gives its score. It keeps inside a frame's border, as thick as given
     * ({@link #NOTICE_BORDER}, {@link #CARD_BORDER}).
     */
    public static void wash(GuiGraphicsExtractor graphics, int x, int y, int width, int contentBottom, float reach, int rgb, float alpha, float age,
                            long seed, float liveliness, int border) {
        float lively = Mth.clamp(liveliness, 0.0F, 1.0F);
        int specks = Math.round(3 + lively * 12);
        float pace = 0.4F + 1.2F * lively;
        float crossChance = 0.5F * lively;
        float glow = Math.min(1.0F, 0.5F + lively);
        int speckRgb = lively >= 0.5F ? brighten(rgb) : rgb;
        // How far a speck rises (or, dull, sinks) over the wash's length.
        float climb = (lively - 0.5F) * 2.0F * WASH_CLIMB;
        // Just in from the border, and a little further from the frame's top (and, as far under the content as over
        // it, its bottom); it fades in at its top as it fades out at its foot, and in from its left edge too (WASH).
        int left = x - PANEL_PADDING + border;
        int top = y - PANEL_PADDING_Y + border + WASH_INSET;
        int bottom = contentBottom + (y - top) - 2;
        int fadeRows = Math.min(WASH_FADE_ROWS, (bottom - top) / 3);
        int head = top + fadeRows;
        int body = bottom - fadeRows;
        int full = Math.round((width + PANEL_PADDING) * WASH_SHARE);
        int span = Math.round(full * Mth.clamp(reach, 0.0F, 1.0F));
        if (span <= 0) return;
        int tint = color(rgb, alpha * WASH_ALPHA);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WASH_FADE_TOP, left, top, span, fadeRows, tint);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WASH, left, head, span, body - head, tint);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, WASH_FADE, left, body, span, fadeRows, tint);
        RandomSource random = RandomSource.create(seed);
        for (int i = 0; i < specks; i++) {
            float speed = (0.25F + random.nextFloat() * 0.45F) * pace;
            float phase = random.nextFloat() * full;
            int startY = top + 1 + random.nextInt(Math.max(1, bottom - top - 2));
            boolean big = random.nextFloat() < crossChance;
            float twinkle = 0.6F + 0.4F * Mth.sin(age * (0.2F + random.nextFloat() * 0.3F) * pace + i);
            // Along the wash and round again from its edge; as bright as the wash is where the speck is.
            float along = (age * speed + phase) % full;
            if (along > span) continue;
            int speckY = Mth.clamp(startY - Math.round(climb * along / full), top + 1, bottom - 2);
            float down = speckY >= body ? 1.0F - (speckY - body + 0.5F) / fadeRows
                    : speckY < head ? (speckY - top + 0.5F) / fadeRows : 1.0F;
            float fade = (float) Math.pow(1.0F - along / full, 1.6F) * down * glow;
            int speckX = left + Math.round(along);
            int speck = color(speckRgb, alpha * fade * twinkle);
            graphics.fill(speckX, speckY, speckX + 1, speckY + 1, speck);
            if (big && fade > 0.3F) {
                // A little cross of light for some, its arms dimmer.
                int arm = color(speckRgb, alpha * fade * twinkle * 0.45F);
                graphics.fill(speckX - 1, speckY, speckX, speckY + 1, arm);
                graphics.fill(speckX + 1, speckY, speckX + 2, speckY + 1, arm);
                graphics.fill(speckX, speckY - 1, speckX + 1, speckY, arm);
                graphics.fill(speckX, speckY + 1, speckX + 1, speckY + 2, arm);
            }
        }
    }

    // A color halfway to white.
    private static int brighten(int rgb) {
        return ARGB.color(0, (ARGB.red(rgb) + 255) / 2, (ARGB.green(rgb) + 255) / 2, (ARGB.blue(rgb) + 255) / 2) & 0xFFFFFF;
    }

    /**
     * A notice's timer across its content's width, {@link #TIMER_HEIGHT} tall from a top: a frame with rounded
     * corners (its corner pixels left out) round a bar two high, filled to a share, with a brighter head where the
     * fill ends.
     */
    public static void timer(GuiGraphicsExtractor graphics, int x, int top, int width, float share, int rgb, float alpha) {
        int right = x + width;
        int bottom = top + TIMER_HEIGHT;
        int edge = color(FAINT, alpha);
        graphics.fill(x + 1, top, right - 1, top + 1, edge);
        graphics.fill(x + 1, bottom - 1, right - 1, bottom, edge);
        graphics.fill(x, top + 1, x + 1, bottom - 1, edge);
        graphics.fill(right - 1, top + 1, right, bottom - 1, edge);
        int inner = width - 2;
        graphics.fill(x + 1, top + 1, right - 1, bottom - 1, color(TRACK, alpha));
        int filled = Math.round(inner * Mth.clamp(share, 0.0F, 1.0F));
        if (filled <= 0) return;
        graphics.fill(x + 1, top + 1, x + 1 + filled, bottom - 1, color(rgb, alpha * 0.85F));
        graphics.fill(x + 1 + Math.max(0, filled - 2), top + 1, x + 1 + filled, bottom - 1, color(0xFFFFFF, alpha * 0.85F));
    }

    /** A line across a panel at a height, from border to border; the panel's content area given as for {@link #panel}. */
    public static void divider(GuiGraphicsExtractor graphics, int x, int width, int lineY, float alpha) {
        graphics.fill(x - PANEL_PADDING + 1, lineY, x + width + PANEL_PADDING - 1, lineY + 1, color(DIVIDER, alpha));
    }

    /** The square an icon sits in, tinted the color of what it stands for: a faint wash inside a stronger outline. */
    public static void iconSquare(GuiGraphicsExtractor graphics, int x, int y, int rgb, float alpha) {
        int last = SLOT_SIZE - 1;
        int edge = color(rgb, alpha * 0.45F);
        // The outline, its corner pixels left out so the square's corners are soft like the card's.
        graphics.fill(x + 1, y, x + last, y + 1, edge);
        graphics.fill(x + 1, y + last, x + last, y + SLOT_SIZE, edge);
        graphics.fill(x, y + 1, x + 1, y + last, edge);
        graphics.fill(x + last, y + 1, x + SLOT_SIZE, y + last, edge);
        graphics.fill(x + 1, y + 1, x + last, y + last, color(rgb, alpha * 0.14F));
    }

    /** A small round dot, 4 across (a square with its corners left out), its top left corner at a point. */
    public static void dot(GuiGraphicsExtractor graphics, int x, int y, int rgb, float alpha) {
        int fill = color(rgb, alpha);
        graphics.fill(x + 1, y, x + 3, y + 4, fill);
        graphics.fill(x, y + 1, x + 4, y + 3, fill);
    }

    /** A bar of a width and height, filled to a share with one of the fills. */
    public static void bar(GuiGraphicsExtractor graphics, Identifier fill, int x, int y, int width, int height, float filled, float alpha) {
        int tint = ARGB.white(Mth.clamp(alpha, 0.0F, 1.0F));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_BACK, x, y, width, height, tint);
        // The fill shows at least a sliver of anything above nothing.
        int filledWidth = filled <= 0.0F ? 0 : Math.max(1, Math.round(width * Mth.clamp(filled, 0.0F, 1.0F)));
        if (filledWidth > 0) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, fill, x, y, filledWidth, height, tint);
    }

    /**
     * A bar in segments, one for each of a count, the first few of them filled: so many out of so many, readable at a
     * glance. Past what fits with a gap of a pixel or more between them, it is drawn as one bar.
     */
    public static void segments(GuiGraphicsExtractor graphics, Identifier fill, int x, int y, int width, int height, int filled, int count, float alpha) {
        int gap = 2;
        if (count <= 1 || (width - gap * (count - 1)) / count < 3) {
            bar(graphics, fill, x, y, width, height, filled / (float) Math.max(1, count), alpha);
            return;
        }
        int tint = ARGB.white(Mth.clamp(alpha, 0.0F, 1.0F));
        int room = width - gap * (count - 1);
        for (int i = 0; i < count; i++) {
            // Spread the leftover pixels over the segments, so the bar ends exactly at its width.
            int left = x + i * room / count + i * gap;
            int right = x + (i + 1) * room / count + i * gap;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, i < filled ? fill : BAR_BACK, left, y, right - left, height, tint);
        }
    }

    /**
     * A meter of ten pips across a width, lit to a share: the whole ones full, the next one in part, the rest dark; a
     * full pip has a lighter top edge, so the meter reads as lit rather than flat. How well a night was slept.
     */
    public static void pips(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float share, int rgb, float alpha) {
        int count = 10;
        int gap = 2;
        int room = width - gap * (count - 1);
        float lit = share * count;
        for (int i = 0; i < count; i++) {
            int left = x + i * room / count + i * gap;
            int right = x + (i + 1) * room / count + i * gap;
            graphics.fill(left, y, right, y + height, color(TRACK, alpha));
            float part = Mth.clamp(lit - i, 0.0F, 1.0F);
            int filled = Math.round((right - left) * part);
            if (filled > 0) graphics.fill(left, y, left + filled, y + height, color(rgb, alpha));
            if (part >= 1.0F) graphics.fill(left, y, right, y + 1, color(0xFFFFFF, alpha * 0.35F));
        }
    }
}
