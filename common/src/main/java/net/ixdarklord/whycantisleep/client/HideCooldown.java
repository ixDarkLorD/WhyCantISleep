package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.client.hud.SleepNotice;
import net.ixdarklord.whycantisleep.client.hud.SleepNotices;
import net.ixdarklord.whycantisleep.network.HideCooldownPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The card a player is shown when they try to hide too soon after coming out of a spot (the server's
 * {@code hideCooldown}): its foot line runs down with the time left, and its hint counts the seconds, until they may
 * hide again, when it goes. Its clock stops while the game is paused, as the cards' do.
 */
public final class HideCooldown {
    private static int tick;
    // The tick the cooldown ends on, and how long the whole cooldown is; -1 when there is none.
    private static int endsAt = -1;
    private static int total;
    // The seconds the card says, so its hint is only written again when they change.
    private static int shownSeconds;

    private HideCooldown() {
    }

    public static void init() {
        ClientTickEvents.END.register(minecraft -> {
            if (minecraft.isPaused()) return;
            tick++;
            if (endsAt < 0) return;
            int left = endsAt - tick;
            if (left <= 0 || SleepNotices.shownKind() != SleepNotice.Kind.HIDE_COOLDOWN) {
                endsAt = -1;
                return;
            }
            int seconds = seconds(left);
            if (seconds != shownSeconds) {
                shownSeconds = seconds;
                SleepNotices.replace(notice(seconds));
            }
        });
        ClientPlayerEvents.LEAVE.register(player -> endsAt = -1);
    }

    /** The server refused a hide: the card, for as long as is left. */
    public static void on(HideCooldownPayload payload) {
        if (payload.ticksLeft() <= 0) return;
        endsAt = tick + payload.ticksLeft();
        total = Math.max(payload.totalTicks(), payload.ticksLeft());
        shownSeconds = seconds(payload.ticksLeft());
        // Asked again while it is up, the card keeps its place and only its time is set again (no second sound).
        if (SleepNotices.shownKind() == SleepNotice.Kind.HIDE_COOLDOWN) SleepNotices.showQuietly(notice(shownSeconds), payload.ticksLeft());
        else SleepNotices.show(notice(shownSeconds), payload.ticksLeft());
    }

    private static SleepNotice notice(int seconds) {
        return new SleepNotice(SleepNotice.Kind.HIDE_COOLDOWN, Component.translatable("gui.whycantisleep.hide.cooldown.title"),
                Component.translatable("gui.whycantisleep.hide.cooldown.hint", seconds))
                .withProgress(HideCooldown::share);
    }

    // The share of the cooldown left, for the foot line.
    private static double share() {
        return endsAt < 0 || total <= 0 ? 0.0 : Mth.clamp((endsAt - tick) / (double) total, 0.0, 1.0);
    }

    private static int seconds(int ticks) {
        return Mth.ceil(ticks / 20.0F);
    }
}
