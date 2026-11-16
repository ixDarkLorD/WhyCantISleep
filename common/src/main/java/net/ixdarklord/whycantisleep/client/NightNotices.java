package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.whycantisleep.client.hud.SleepNotice;
import net.ixdarklord.whycantisleep.client.hud.SleepNotices;
import net.ixdarklord.whycantisleep.network.NightNoticePayload;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Cards for what happened round a sleep that isn't a night's score: a nap slept out or cut short, and sleepwalking. */
public final class NightNotices {
    private static final int CARD_TICKS = 20 * 6;

    private NightNotices() {
    }

    public static void on(NightNoticePayload payload) {
        String key = "gui.whycantisleep.night_notice." + switch (payload.kind()) {
            case NAPPED -> "napped";
            case NAP_CUT -> "nap_cut";
            case SLEEPWALKED -> "sleepwalked";
        };
        SleepNotice.Kind kind = payload.kind() == NightNoticePayload.Kind.SLEEPWALKED ? SleepNotice.Kind.SLEEPWALKED : SleepNotice.Kind.NAP;
        // The days of sleep debt left, to a tenth, after a nap: none left is said plainly.
        Component hint = payload.kind() == NightNoticePayload.Kind.NAPPED && payload.value() < 0.05F
                ? Component.translatable(key + ".hint.none")
                : Component.translatable(key + ".hint", String.format("%.1f", Mth.clamp(payload.value(), 0.0F, 99.0F)));
        SleepNotices.show(new SleepNotice(kind, Component.translatable(key + ".title"), hint), CARD_TICKS);
    }
}
