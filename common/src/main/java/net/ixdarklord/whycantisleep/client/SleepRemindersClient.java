package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.whycantisleep.client.hud.SleepNotice;
import net.ixdarklord.whycantisleep.client.hud.SleepNotices;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.network.ReminderPayload;
import net.minecraft.network.chat.Component;

/**
 * The server's reminders to sleep, as cards: at dusk, the days gone without sleep; at night, the players in bed
 * waiting for the rest. A hiding player's card isn't pushed aside for one.
 */
public final class SleepRemindersClient {
    private static final int CARD_TICKS = 20 * 6;

    private SleepRemindersClient() {
    }

    public static void on(ReminderPayload payload) {
        if (!WCISConfigs.isEnabled(WCISClientConfig.SLEEP_REMINDERS)) return;
        SleepNotice.Kind shown = SleepNotices.shownKind();
        if (shown != null && shown.hiding()) return;
        SleepNotice notice = switch (payload.kind()) {
            case DUSK -> new SleepNotice(SleepNotice.Kind.REMINDER, Component.translatable("gui.whycantisleep.reminder.dusk.title"),
                    Component.translatable(payload.days() == 1 ? "gui.whycantisleep.reminder.dusk.hint.one" : "gui.whycantisleep.reminder.dusk.hint",
                            payload.days()));
            case OTHERS_IN_BED -> new SleepNotice(SleepNotice.Kind.NUDGE,
                    Component.translatable("gui.whycantisleep.reminder.others.title", payload.sleeping(), payload.needed()),
                    Component.translatable("gui.whycantisleep.reminder.others.hint"));
        };
        SleepNotices.show(notice, CARD_TICKS);
    }
}
