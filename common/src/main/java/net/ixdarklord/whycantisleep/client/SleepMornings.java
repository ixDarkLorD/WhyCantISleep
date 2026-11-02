package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.whycantisleep.client.hud.HudStyle;
import net.ixdarklord.whycantisleep.client.hud.SleepNotice;
import net.ixdarklord.whycantisleep.client.hud.SleepNotices;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.network.WokePayload;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
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
        BedBookmarks.onWoke(payload);
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
                    morningHint(payload)).withQuality(quality(payload));
            case HURT -> new SleepNotice(SleepNotice.Kind.WOKE_EARLY, Component.translatable("gui.whycantisleep.woke.hurt.title"),
                    Component.translatable("gui.whycantisleep.woke.hurt.hint"));
            case BED_GONE -> new SleepNotice(SleepNotice.Kind.WOKE_EARLY, Component.translatable("gui.whycantisleep.woke.bed_gone.title"),
                    Component.translatable("gui.whycantisleep.woke.bed_gone.hint"));
            // Getting up is the player's own doing; anything else, a mod's or a command's, has its own say.
            case GOT_UP, OTHER -> null;
        };
    }

    // Under the title: that they woke well rested, and what by the bed they came to know, or that something there
    // did something, without a Dreamcatcher to tell them what.
    private static @Nullable Component morningHint(WokePayload payload) {
        Component rested = payload.wellRested() ? Component.translatable("gui.whycantisleep.woke.rested.hint") : null;
        Component found;
        if (!payload.discovered().isEmpty()) {
            MutableComponent names = Component.empty();
            for (int i = 0; i < payload.discovered().size(); i++) {
                if (i > 0) names.append(", ");
                names.append(new ItemStack(payload.discovered().get(i)).getHoverName().copy().withColor(HudStyle.SILVER));
            }
            found = Component.translatable("gui.whycantisleep.woke.discovered", names);
        } else if (!payload.noticed().isEmpty()) {
            found = Component.translatable("gui.whycantisleep.woke.noticed");
        } else {
            return rested;
        }
        return rested == null ? found : Component.empty().append(rested).append(" ").append(found);
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

    /** The word for a night's score, for the {@code gui.whycantisleep.woke.word.*} keys: great, good, fair, poor, awful. */
    static String word(int score) {
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
