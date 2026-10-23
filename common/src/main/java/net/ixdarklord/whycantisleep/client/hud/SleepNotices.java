package net.ixdarklord.whycantisleep.client.hud;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.whycantisleep.api.SleepEffect;
import net.ixdarklord.whycantisleep.client.fx.SleepEffects;
import net.ixdarklord.whycantisleep.network.WokePayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The sleep HUD's message card, above the hotbar where the game's own line would be. It is laid out like a small
 * notice on a dark, square-cornered card: a wash of its kind's color over its left side with specks of light drifting
 * along it, the item that stands for it in a square tinted that color, beside it the title in that color and under it
 * what to do about it, in gray, and under them, on a hiding card only, a framed timer of how long they have hidden for.
 * It rises into place as it comes in. A night's sleep has a card of its own, rounded, laid out in parts. Long text
 * wraps rather than widening the card. One card shows at a time; a new one takes the place of the last.
 */
public final class SleepNotices {
    private static final int ICON_SIZE = 16;
    private static final int SLOT_SIZE = HudStyle.SLOT_SIZE;
    private static final int SLOT_GAP = 7;
    private static final int LINE_HEIGHT = 10;
    // Text wraps at this width, so the card stays compact however long the message.
    private static final int TEXT_WIDTH = 124;
    // The room between the title and the hint.
    private static final int HINT_GAP = 2;
    // The room under a hiding card's content for its timer.
    private static final int NOTICE_FOOT_ROOM = 5 + HudStyle.TIMER_HEIGHT;
    // A notice rises this far as it comes in, over this long, its rail growing as it does, and its icon flashes.
    private static final int NOTICE_RISE = 4;
    private static final float NOTICE_IN_TICKS = 6.0F;
    private static final float NOTICE_FLASH_TICKS = 8.0F;
    // How far above the bottom of the screen the box's foot is: clear of the hotbar, the hearts and the armor.
    private static final int BOTTOM_OFFSET = 56;
    private static final float APPEAR_TICKS = 4.0F;
    private static final float VANISH_TICKS = 10.0F;
    public static final int DEFAULT_TICKS = 90;
    // A night's sleep card: its width, the meter's height, the details shown, a factor's row, the room round a divider,
    // how long the meter takes to fill, and when and how fast the rows come in after it.
    private static final int QUALITY_WIDTH = 168;
    private static final int QUALITY_PIP_HEIGHT = 4;
    private static final int QUALITY_DETAILS = 3;
    private static final int QUALITY_ROW = 13;
    private static final int QUALITY_GAP = 4;
    private static final float QUALITY_FILL_TICKS = 24.0F;
    private static final float QUALITY_ROWS_AT = 12.0F;
    private static final float QUALITY_ROW_STAGGER = 4.0F;
    private static final float QUALITY_ROW_TICKS = 8.0F;
    // How far the card rises as it comes in, and a row slides as it does.
    private static final int QUALITY_RISE = 6;
    private static final int QUALITY_SLIDE = 8;
    // A sheen crosses the sleep effect's row as it comes in, and again and again for the rarest.
    private static final float SHEEN_TICKS = 14.0F;
    private static final float SHEEN_EVERY = 50.0F;

    private static @Nullable SleepNotice notice;
    private static long tick;
    private static long shownAt;
    private static int duration;
    // The top of the card's frame where it settles, from the last frame drawn; what sits over the card keeps above it.
    private static int top = Integer.MAX_VALUE;

    private SleepNotices() {
    }

    public static void init() {
        ClientTickEvents.END.register(minecraft -> {
            tick++;
            if (notice != null && tick - shownAt > duration) notice = null;
        });
        ClientPlayerEvents.LEAVE.register(player -> notice = null);
        ClientGuiEvents.RENDER_HUD.register(SleepNotices::render);
    }

    /** The top edge of the card's frame on screen, or {@link Integer#MAX_VALUE} while no card is shown. */
    public static int top() {
        return top;
    }

    public static void show(SleepNotice notice) {
        show(notice, DEFAULT_TICKS);
    }

    /** Takes down the card shown, if it is of this kind. */
    public static void clear(SleepNotice.Kind kind) {
        if (notice != null && notice.kind() == kind) notice = null;
    }

    /**
     * Changes what the card up says, keeping its time, with no sound: a card that follows something as it changes
     * (the hiding card, when monsters come near or go). Does nothing when no card is up.
     */
    public static void replace(SleepNotice notice) {
        if (SleepNotices.notice != null) SleepNotices.notice = notice;
    }

    /** The kind of card up, or null when none is. */
    public static @Nullable SleepNotice.Kind shownKind() {
        return notice == null ? null : notice.kind();
    }

    /** Shows a card for a number of ticks, with the sound and screen effect of its kind. */
    public static void show(SleepNotice notice, int ticks) {
        showQuietly(notice, ticks);
        SleepEffects.onNotice(notice.kind());
    }

    /** Shows a card for a number of ticks, with no sound or screen effect: one coming back after another went. */
    public static void showQuietly(SleepNotice notice, int ticks) {
        SleepNotices.notice = notice;
        shownAt = tick;
        duration = ticks;
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        SleepNotice shown = notice;
        Minecraft minecraft = Minecraft.getInstance();
        top = Integer.MAX_VALUE;
        if (shown == null || minecraft.options.hideGui) return;
        float age = tick - shownAt + delta.getGameTimeDeltaPartialTick(true);
        float left = duration - age;
        if (left <= 0.0F) return;
        float alpha = Math.min(Mth.clamp(age / APPEAR_TICKS, 0.0F, 1.0F), Mth.clamp(left / VANISH_TICKS, 0.0F, 1.0F));

        Font font = minecraft.font;
        if (shown.quality() != null) {
            renderQuality(graphics, font, shown, shown.quality(), age, alpha);
            return;
        }
        List<FormattedCharSequence> title = font.split(shown.title(), TEXT_WIDTH);
        List<FormattedCharSequence> hint = shown.hint() != null ? font.split(shown.hint(), TEXT_WIDTH) : List.of();
        int textWidth = 0;
        for (FormattedCharSequence line : title) textWidth = Math.max(textWidth, font.width(line));
        for (FormattedCharSequence line : hint) textWidth = Math.max(textWidth, font.width(line));
        // A line of text is 8 tall in a row of 10, so the last row gives its 2 spare pixels back.
        int textHeight = (title.size() + hint.size()) * LINE_HEIGHT - 2 + (hint.isEmpty() ? 0 : HINT_GAP);
        int width = SLOT_SIZE + SLOT_GAP + textWidth;
        int contentHeight = Math.max(SLOT_SIZE, textHeight);
        // Only a hiding card has a timer, and only while it has something to count (not all quiet).
        boolean timed = shown.kind().hiding() && shown.progress() != null;
        int height = contentHeight + (timed ? NOTICE_FOOT_ROOM : 0);
        float rise = 1.0F - Easing.CUBIC_OUT.apply(Mth.clamp(age / NOTICE_IN_TICKS, 0.0F, 1.0F));
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() - BOTTOM_OFFSET - height + Math.round(NOTICE_RISE * rise);
        top = graphics.guiHeight() - BOTTOM_OFFSET - height - HudStyle.PANEL_PADDING;
        int rgb = shown.kind().color();

        HudStyle.notice(graphics, x, y, width, height, alpha);
        // The wash over the left side, sweeping in as the card comes in; for danger it throbs.
        float washAlpha = shown.kind().danger() ? alpha * (0.65F + 0.35F * Mth.sin(age * 0.3F)) : alpha;
        HudStyle.wash(graphics, x, y, width, y + contentHeight, 1.0F - rise, rgb, washAlpha, age, shownAt * 31L + shown.kind().ordinal());
        int slotY = y + (contentHeight - SLOT_SIZE) / 2;
        HudStyle.iconSquare(graphics, x, slotY, rgb, alpha);
        // The icon fades in and out with the card (ItemFade: the game draws items at full opacity on its own).
        ItemFade.item(graphics, shown.icon(), x + 2, slotY + 2, alpha);
        // A flash of the kind's color over the icon's square as the card comes in.
        float flash = 1.0F - Mth.clamp(age / NOTICE_FLASH_TICKS, 0.0F, 1.0F);
        if (flash > 0.0F) graphics.fill(x + 1, slotY + 1, x + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, HudStyle.color(rgb, alpha * 0.35F * flash));
        if (timed) {
            float share = (float) shown.progress().getAsDouble();
            HudStyle.timer(graphics, x, y + height - HudStyle.TIMER_HEIGHT, width, share, rgb, alpha);
        }

        int textX = x + SLOT_SIZE + SLOT_GAP;
        int lineY = y + (contentHeight - textHeight) / 2;
        for (FormattedCharSequence line : title) {
            graphics.text(font, line, textX, lineY, HudStyle.color(rgb, alpha), false);
            lineY += LINE_HEIGHT;
        }
        if (hint.isEmpty()) return;
        lineY += HINT_GAP;
        for (FormattedCharSequence line : hint) {
            graphics.text(font, line, textX, lineY, HudStyle.color(HudStyle.GRAY, alpha), false);
            lineY += LINE_HEIGHT;
        }
    }

    /**
     * A night's sleep, in parts split by dividers. At the top, the icon, the title over a quiet label, and the score
     * large at the right, counting up as the meter of ten pips under them fills. Then what mattered most, under a label
     * (and how many more there were): a row each, coming in one after the other, the points counting up at the right
     * and, behind the row, a wash in their color as wide as the points are big next to the biggest. Then the sleep
     * effect woken with, a sheen crossing it, and its rarity on a tag of its color; and last the hint, if any. The whole
     * card rises a little as it comes in.
     */
    private static void renderQuality(GuiGraphicsExtractor graphics, Font font, SleepNotice shown, SleepNotice.Quality quality, float age, float alpha) {
        int rgb = shown.kind().color();
        int width = QUALITY_WIDTH;
        List<SleepNotice.Detail> details = quality.details().subList(0, Math.min(QUALITY_DETAILS, quality.details().size()));
        int more = quality.details().size() - details.size();
        List<FormattedCharSequence> hint = shown.hint() != null ? font.split(shown.hint(), width) : List.of();
        WokePayload.Dream dream = quality.dream();

        // Each part after the head is a divider with room round it and then its rows.
        int part = QUALITY_GAP * 2 + 1;
        int height = SLOT_SIZE + 5 + QUALITY_PIP_HEIGHT;
        if (!details.isEmpty()) height += part + LINE_HEIGHT + details.size() * QUALITY_ROW - 1;
        if (dream != null) height += part + QUALITY_ROW - 2;
        if (!hint.isEmpty()) height += part + hint.size() * LINE_HEIGHT - 2;
        float rise = 1.0F - Easing.CUBIC_OUT.apply(Mth.clamp(age / (APPEAR_TICKS * 2.0F), 0.0F, 1.0F));
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() - BOTTOM_OFFSET - height + Math.round(QUALITY_RISE * rise);
        top = graphics.guiHeight() - BOTTOM_OFFSET - height - HudStyle.PANEL_PADDING;
        HudStyle.box(graphics, x, y, width, height, alpha);
        float fill = Easing.QUAD_OUT.apply(Mth.clamp(age / QUALITY_FILL_TICKS, 0.0F, 1.0F));
        // The wash over the head, sweeping in as the meter fills, its specks as lively as the night was good.
        HudStyle.wash(graphics, x, y, width, y + SLOT_SIZE + 5 + QUALITY_PIP_HEIGHT, fill, rgb, alpha, age, shownAt * 31L, quality.score() / 100.0F,
                HudStyle.CARD_BORDER);

        // The head: the icon, the title over its label, and the score, twice the size, counting up as the pips fill.
        HudStyle.iconSquare(graphics, x, y, rgb, alpha);
        ItemFade.item(graphics, shown.icon(), x + 2, y + 2, alpha);
        String score = Math.round(quality.score() * fill) + "%";
        int scoreX = x + width - font.width(score) * 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate(scoreX, y + 3);
        graphics.pose().scale(2.0F, 2.0F);
        graphics.text(font, score, 0, 0, HudStyle.color(HudStyle.WHITE, alpha), false);
        graphics.pose().popMatrix();
        int textX = x + SLOT_SIZE + SLOT_GAP;
        int textWidth = scoreX - textX - 6;
        graphics.text(font, font.split(shown.title(), textWidth).getFirst(), textX, y + 1, HudStyle.color(rgb, alpha), false);
        graphics.text(font, font.split(Component.translatable("gui.whycantisleep.woke.quality.label"), textWidth).getFirst(), textX, y + 11,
                HudStyle.color(HudStyle.MUTED, alpha), false);
        int cursor = y + SLOT_SIZE + 5;
        HudStyle.pips(graphics, x, cursor, width, QUALITY_PIP_HEIGHT, quality.score() / 100.0F * fill, rgb, alpha);
        cursor += QUALITY_PIP_HEIGHT;

        // The rows come in one after the other once the meter is most of the way: the factors, then the sleep effect.
        int row = 0;
        if (!details.isEmpty()) {
            cursor = partDivider(graphics, x, width, cursor, alpha);
            graphics.text(font, Component.translatable("gui.whycantisleep.woke.factors"), x, cursor, HudStyle.color(HudStyle.MUTED, alpha), false);
            if (more > 0) {
                Component moreText = Component.translatable("gui.whycantisleep.woke.more", more);
                graphics.text(font, moreText, x + width - font.width(moreText), cursor, HudStyle.color(HudStyle.FAINT, alpha), false);
            }
            cursor += LINE_HEIGHT;
            int biggest = 1;
            for (SleepNotice.Detail detail : details) biggest = Math.max(biggest, Math.abs(detail.points()));
            for (int i = 0; i < details.size(); i++) {
                SleepNotice.Detail detail = details.get(i);
                float in = rowIn(age, row++);
                float rowAlpha = alpha * in;
                int slide = Math.round(QUALITY_SLIDE * (1.0F - in));
                int pointsColor = detail.points() > 0 ? HudStyle.GREEN : HudStyle.RED;
                // The wash behind the row, as wide as the points are big, growing in with it, and a stripe at its edge.
                int wash = Math.round(width * Math.abs(detail.points()) / (float) biggest * in);
                if (wash > 0) graphics.fill(x, cursor, x + wash, cursor + QUALITY_ROW - 2, HudStyle.color(pointsColor, rowAlpha * 0.08F));
                graphics.fill(x, cursor, x + 1, cursor + QUALITY_ROW - 2, HudStyle.color(pointsColor, rowAlpha * 0.7F));
                SleepTooltip.icon(graphics, x + 4 + slide, cursor + 1, detail.points());
                int shownPoints = Math.round(Math.abs(detail.points()) * in);
                Component points = Component.literal((detail.points() > 0 ? "+" : "\u2212") + shownPoints);
                int pointsX = x + width - 2 - font.width(points);
                int labelX = x + 4 + SleepTooltip.ICON_SIZE + 4 + slide;
                graphics.text(font, font.split(detail.label(), pointsX - labelX - 4).getFirst(), labelX, cursor + 2, HudStyle.color(HudStyle.SILVER, rowAlpha), false);
                graphics.text(font, points, pointsX, cursor + 2, HudStyle.color(pointsColor, rowAlpha), false);
                // A hairline between rows, short of the card's edges.
                if (i < details.size() - 1) {
                    graphics.fill(x + 4, cursor + QUALITY_ROW - 1, x + width - 4, cursor + QUALITY_ROW, HudStyle.color(HudStyle.DIVIDER, alpha));
                }
                cursor += QUALITY_ROW;
            }
            cursor -= 1;
        }
        if (dream != null) {
            cursor = partDivider(graphics, x, width, cursor, alpha);
            float in = rowIn(age, row);
            float rowAlpha = alpha * in;
            int slide = Math.round(QUALITY_SLIDE * (1.0F - in));
            int rarityColor = dream.rarity().color();
            // Its rarity on a tag of its color, at the right.
            Component rarity = Component.translatable("gui.whycantisleep.rarity." + dream.rarity().getSerializedName());
            int tagX = x + width - font.width(rarity) - 4;
            graphics.fill(tagX, cursor, x + width, cursor + QUALITY_ROW - 2, HudStyle.color(rarityColor, rowAlpha * 0.16F));
            graphics.text(font, rarity, tagX + 2, cursor + 2, HudStyle.color(rarityColor, rowAlpha), false);
            SleepTooltip.icon(graphics, x + slide, cursor + 1, dream.kind());
            int nameX = x + SleepTooltip.ICON_SIZE + 4 + slide;
            graphics.text(font, font.split(dream.name(), tagX - nameX - 4).getFirst(), nameX, cursor + 2, HudStyle.color(rarityColor, rowAlpha), false);
            sheen(graphics, x, cursor, width, QUALITY_ROW - 2, age - rowStart(row), dream.rarity().ordinal() >= SleepEffect.Rarity.EPIC.ordinal(), rowAlpha);
            cursor += QUALITY_ROW - 2;
        }
        if (!hint.isEmpty()) {
            cursor = partDivider(graphics, x, width, cursor, alpha);
            for (FormattedCharSequence line : hint) {
                graphics.text(font, line, x + (width - font.width(line)) / 2, cursor, HudStyle.color(HudStyle.MUTED, alpha), false);
                cursor += LINE_HEIGHT;
            }
        }
    }

    // A divider with room on both sides, after the part that ends at a height; returns where the next part starts.
    private static int partDivider(GuiGraphicsExtractor graphics, int x, int width, int cursor, float alpha) {
        HudStyle.divider(graphics, x, width, cursor + QUALITY_GAP, alpha);
        return cursor + QUALITY_GAP * 2 + 1;
    }

    private static float rowStart(int row) {
        return QUALITY_ROWS_AT + row * QUALITY_ROW_STAGGER;
    }

    // How far in a row is, 0 to 1, eased.
    private static float rowIn(float age, int row) {
        return Easing.CUBIC_OUT.apply(Mth.clamp((age - rowStart(row)) / QUALITY_ROW_TICKS, 0.0F, 1.0F));
    }

    // A slanted band of light crossing a row from left to right, once as it comes in, or over and over for the rarest.
    private static void sheen(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float since, boolean repeat, float alpha) {
        if (since < 0.0F) return;
        float t = repeat ? since % SHEEN_EVERY : since;
        if (t > SHEEN_TICKS) return;
        int band = 10;
        int at = x - band - height + Math.round((width + band + height * 2) * Easing.SINE_IN_OUT.apply(t / SHEEN_TICKS));
        int color = HudStyle.color(HudStyle.WHITE, alpha * 0.14F);
        for (int line = 0; line < height; line++) {
            // Each line of the band a pixel further on than the one under it, so it leans.
            int from = Math.max(x, at + (height - line));
            int to = Math.min(x + width, at + (height - line) + band);
            if (to > from) graphics.fill(from, y + line, to, y + line + 1, color);
        }
    }
}
