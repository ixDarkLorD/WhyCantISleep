package net.ixdarklord.whycantisleep.client.hud;

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
 * The sleep HUD's message card, above the hotbar where the game's own line would be: on a slate card of its own, an
 * item for an icon, what happened in the color of its kind, and what to do about it in gray. One card shows at a
 * time; a new one takes the place of the last.
 */
public final class SleepNotices {
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 5;
    private static final int LINE_HEIGHT = 10;
    // How far above the bottom of the screen the box's foot is: clear of the hotbar, the hearts and the armor.
    private static final int BOTTOM_OFFSET = 62;
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
        int textWidth = Math.max(font.width(shown.title()), shown.hint() != null ? font.width(shown.hint()) : 0);
        int width = ICON_SIZE + ICON_GAP + textWidth;
        int height = ICON_SIZE + (shown.hint() != null ? 3 : 0);
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() - BOTTOM_OFFSET - height;

        HudStyle.box(graphics, x, y, width, height, alpha);
        // Items can't be drawn faded, so the icon waits until the box is mostly there.
        if (alpha > 0.6F) graphics.fakeItem(shown.kind().icon(), x, y + (height - ICON_SIZE) / 2);
        int textX = x + ICON_SIZE + ICON_GAP;
        if (shown.hint() != null) {
            graphics.text(font, shown.title(), textX, y, HudStyle.color(shown.kind().color(), alpha), true);
            graphics.text(font, shown.hint(), textX, y + LINE_HEIGHT, HudStyle.color(HudStyle.GRAY, alpha), true);
        } else {
            graphics.text(font, shown.title(), textX, y + (height - 8) / 2, HudStyle.color(shown.kind().color(), alpha), true);
        }
    }
}
