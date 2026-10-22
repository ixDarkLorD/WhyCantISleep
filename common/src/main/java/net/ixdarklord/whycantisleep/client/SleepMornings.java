package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.whycantisleep.client.hud.SleepNotice;
import net.ixdarklord.whycantisleep.client.hud.SleepNotices;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.network.WokePayload;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The card on waking up. In the morning it says how well the player slept: a word for it, the score out of 100 and a
 * meter of pips filling to it, and the things that mattered most with their points; woken early, it says what woke
 * them; from a night terror, that they wake shaken, with the night's score too.
 */
public final class SleepMornings {
    private static final int CARD_TICKS = 20 * 8;
    // Where a score turns from one word to the next, and from one color of card to the next.
    private static final int GREAT = 85;
    private static final int GOOD = 65;
    private static final int FAIR = 45;
    private static final int POOR = 25;

    private SleepMornings() {
    }

    public static void onWoke(WokePayload payload) {
        SleepStatus.onWoke(payload);
        if (!WCISConfigs.isEnabled(WCISClientConfig.MORNING_CARD)) return;
        SleepNotice notice = notice(payload);
        if (notice != null) SleepNotices.show(notice, CARD_TICKS);
    }

    private static @Nullable SleepNotice notice(WokePayload payload) {
        Component word = Component.translatable("gui.whycantisleep.woke.word." + word(payload.score()));
        if (payload.shaken()) {
            return new SleepNotice(SleepNotice.Kind.SHAKEN, Component.translatable("gui.whycantisleep.woke.shaken.title"),
                    Component.translatable("gui.whycantisleep.woke.shaken.note")).withQuality(quality(payload));
        }
        return switch (payload.reason()) {
            case MORNING -> new SleepNotice(kind(payload.score()), Component.translatable("gui.whycantisleep.woke.quality.title", word),
                    payload.wellRested() ? Component.translatable("gui.whycantisleep.woke.rested.hint") : null).withQuality(quality(payload));
            case HURT -> new SleepNotice(SleepNotice.Kind.WOKE_EARLY, Component.translatable("gui.whycantisleep.woke.hurt.title"),
                    Component.translatable("gui.whycantisleep.woke.hurt.hint"));
            case BED_GONE -> new SleepNotice(SleepNotice.Kind.WOKE_EARLY, Component.translatable("gui.whycantisleep.woke.bed_gone.title"),
                    Component.translatable("gui.whycantisleep.woke.bed_gone.hint"));
            // Getting up is the player's own doing; anything else, a mod's or a command's, has its own say.
            case GOT_UP, OTHER -> null;
        };
    }

    // The score, and what mattered most to it, the biggest first.
    private static SleepNotice.Quality quality(WokePayload payload) {
        List<SleepNotice.Detail> details = new ArrayList<>();
        for (WokePayload.Line line : payload.lines()) {
            if (line.points() != 0) details.add(new SleepNotice.Detail(line.label(), line.points()));
        }
        details.sort(Comparator.comparingInt(detail -> -Math.abs(detail.points())));
        return new SleepNotice.Quality(payload.score(), details, payload.dream().orElse(null));
    }

    private static String word(int score) {
        if (score >= GREAT) return "great";
        if (score >= GOOD) return "good";
        if (score >= FAIR) return "fair";
        if (score >= POOR) return "poor";
        return "awful";
    }

    private static SleepNotice.Kind kind(int score) {
        if (score >= GOOD) return SleepNotice.Kind.WOKE_GOOD;
        if (score >= FAIR) return SleepNotice.Kind.WOKE_FAIR;
        return SleepNotice.Kind.WOKE_POOR;
    }
}
