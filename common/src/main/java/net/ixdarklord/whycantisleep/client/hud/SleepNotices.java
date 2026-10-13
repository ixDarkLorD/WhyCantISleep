package net.ixdarklord.whycantisleep.client.hud;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.client.fx.SleepEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The sleep HUD's message card, above the hotbar where the game's own line would be. It is laid out like a small
 * notice: the item that stands for it in a slot of its own on the left; beside it what happened, in the color of its
 * kind, ruled off from what to do about it, in gray. Long text wraps rather than widening the card. One card shows
 * at a time; a new one takes the place of the last.
 */
public final class SleepNotices {
    private static final int ICON_SIZE = 16;
    private static final int SLOT_SIZE = HudStyle.SLOT_SIZE;
    private static final int SLOT_GAP = 6;
    private static final int LINE_HEIGHT = 10;
    // Text wraps at this width, so the card stays compact however long the message.
    private static final int TEXT_WIDTH = 124;
    // The rule between the title and the hint: a pixel of line with this much room around it.
    private static final int RULE_HEIGHT = 5;
    // How far above the bottom of the screen the box's foot is: clear of the hotbar, the hearts and the armor.
    private static final int BOTTOM_OFFSET = 56;
    private static final float APPEAR_TICKS = 4.0F;
    private static final float VANISH_TICKS = 10.0F;
    public static final int DEFAULT_TICKS = 90;

    private static @Nullable SleepNotice notice;
    private static long tick;
    private static long shownAt;
    private static int duration;

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

    public static void show(SleepNotice notice) {
        show(notice, DEFAULT_TICKS);
    }

    /** Shows a card for a number of ticks, with the sound and screen effect of its kind. */
    public static void show(SleepNotice notice, int ticks) {
        SleepNotices.notice = notice;
        shownAt = tick;
        duration = ticks;
        SleepEffects.onNotice(notice.kind());
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        SleepNotice shown = notice;
        Minecraft minecraft = Minecraft.getInstance();
        if (shown == null || minecraft.options.hideGui) return;
        float age = tick - shownAt + delta.getGameTimeDeltaPartialTick(true);
        float left = duration - age;
        if (left <= 0.0F) return;
        float alpha = Math.min(Mth.clamp(age / APPEAR_TICKS, 0.0F, 1.0F), Mth.clamp(left / VANISH_TICKS, 0.0F, 1.0F));

        Font font = minecraft.font;
        List<FormattedCharSequence> title = font.split(shown.title(), TEXT_WIDTH);
        List<FormattedCharSequence> hint = shown.hint() != null ? font.split(shown.hint(), TEXT_WIDTH) : List.of();
        int textWidth = 0;
        for (FormattedCharSequence line : title) textWidth = Math.max(textWidth, font.width(line));
        for (FormattedCharSequence line : hint) textWidth = Math.max(textWidth, font.width(line));
        // A line of text is 8 tall in a row of 10, so the last row gives its 2 spare pixels back.
        int textHeight = (title.size() + hint.size()) * LINE_HEIGHT - 2 + (hint.isEmpty() ? 0 : RULE_HEIGHT);
        int width = SLOT_SIZE + SLOT_GAP + textWidth;
        int height = Math.max(SLOT_SIZE, textHeight);
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() - BOTTOM_OFFSET - height;

        HudStyle.box(graphics, x, y, width, height, alpha);
        int slotY = y + (height - SLOT_SIZE) / 2;
        HudStyle.slot(graphics, x, slotY, alpha);
        // Items can't be drawn faded, so the icon waits until the box is mostly there.
        if (alpha > 0.6F) graphics.fakeItem(shown.kind().icon(), x + 2, slotY + 2);

        int textX = x + SLOT_SIZE + SLOT_GAP;
        int lineY = y + (height - textHeight) / 2;
        for (FormattedCharSequence line : title) {
            graphics.text(font, line, textX, lineY, HudStyle.color(shown.kind().color(), alpha), true);
            lineY += LINE_HEIGHT;
        }
        if (hint.isEmpty()) return;
        // The rule: as wide as the text, in the title's color, fainter.
        graphics.fill(textX, lineY + 1, textX + textWidth, lineY + 2, HudStyle.color(shown.kind().color(), alpha * 0.45F));
        lineY += RULE_HEIGHT;
        for (FormattedCharSequence line : hint) {
            graphics.text(font, line, textX, lineY, HudStyle.color(HudStyle.GRAY, alpha), true);
            lineY += LINE_HEIGHT;
        }
    }
}
