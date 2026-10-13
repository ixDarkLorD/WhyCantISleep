package net.ixdarklord.whycantisleep.client.hud;

import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.client.fx.SleepEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * The sleep HUD's message card, above the hotbar where the game's own line would be: an icon, what happened, and what
 * to do about it, with a line along its foot running down the time it stays. One card shows at a time; a new one
 * takes the place of the last.
 */
public final class SleepNotices {
    private static final int ICON_SIZE = 16;
    private static final int HEIGHT = 30;
    // How far above the bottom of the screen the card's foot is: clear of the hotbar, the hearts and the armor.
    private static final int BOTTOM_OFFSET = 56;
    private static final float APPEAR_TICKS = 6.0F;
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
        float appear = Mth.clamp(age / APPEAR_TICKS, 0.0F, 1.0F);
        float alpha = Math.min(appear, Mth.clamp(left / VANISH_TICKS, 0.0F, 1.0F));

        Font font = minecraft.font;
        int textWidth = Math.max(font.width(shown.title()), shown.hint() != null ? font.width(shown.hint()) : 0);
        int width = Math.min(graphics.guiWidth() - 12, HudStyle.PADDING * 3 + ICON_SIZE + textWidth + 2);
        int height = shown.hint() != null ? HEIGHT : HEIGHT - 8;
        int x = (graphics.guiWidth() - width) / 2;
        // It rises into place with a small overshoot.
        int y = graphics.guiHeight() - BOTTOM_OFFSET - height + Math.round(10.0F * (1.0F - Easing.BACK_OUT.apply(appear)));
        int accent = shown.kind().color();

        HudStyle.panel(graphics, x, y, width, height, accent, alpha);
        // Items can't be drawn faded, so the icon waits until the card is mostly there.
        if (alpha > 0.6F) graphics.fakeItem(shown.kind().icon(), x + HudStyle.PADDING + 1, y + (height - ICON_SIZE) / 2);
        int textX = x + HudStyle.PADDING * 2 + ICON_SIZE;
        if (shown.hint() != null) {
            graphics.text(font, shown.title(), textX, y + 5, HudStyle.color(HudStyle.mix(accent, HudStyle.TEXT, 0.35F), alpha), true);
            graphics.text(font, shown.hint(), textX, y + 16, HudStyle.color(HudStyle.TEXT_MUTED, alpha), true);
        } else {
            graphics.text(font, shown.title(), textX, y + (height - 8) / 2, HudStyle.color(HudStyle.mix(accent, HudStyle.TEXT, 0.35F), alpha), true);
        }
        // The time left, as a line along the foot.
        int lineWidth = Math.round((width - 4) * Mth.clamp(left / duration, 0.0F, 1.0F));
        graphics.fill(x + 2, y + height - 2, x + 2 + lineWidth, y + height - 1, HudStyle.color(accent, alpha * 0.7F));
    }
}
