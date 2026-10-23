package net.ixdarklord.whycantisleep.api;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * How well a player slept one night, 0 (awful) to 100 (perfect), and what made it so. Every night starts from
 * {@link #BASE}; each {@link Factor} adds to it or takes from it, and the sum is held to 0 to 100. The factors come
 * from the sleep quality modifiers ({@link SleepModifiers}; the ids below are the mod's own), and
 * {@link SleepEvents#SCORE_QUALITY} has the last word. Read the result from {@link SleepEvents#WOKE} or
 * {@link WhyCantISleepApi#lastSleepQuality}.
 *
 * @param score   0 to 100
 * @param factors what made it so, as they were added
 */
public record SleepQuality(int score, List<Factor> factors) {
    /** Where every night starts, before its factors. */
    public static final int BASE = 75;

    // The mod's own factors.
    /** Sleeping in a closed room: walled, roofed, its doors shut. */
    public static final ResourceLocation ENCLOSED = WhyCantISleep.id("enclosed");
    /** Sleeping somewhere open: the sky over the bed, or a way out of the room. */
    public static final ResourceLocation EXPOSED = WhyCantISleep.id("exposed");
    /** Dark corners in the room, where monsters could spawn. */
    public static final ResourceLocation DARK_CORNERS = WhyCantISleep.id("dark_corners");
    /** A bright light right at the pillow. */
    public static final ResourceLocation TOO_BRIGHT = WhyCantISleep.id("too_bright");
    /** Monsters prowling near the bed through the night. */
    public static final ResourceLocation MONSTERS = WhyCantISleep.id("monsters");
    /** Calm music from a jukebox near the bed. */
    public static final ResourceLocation CALM_MUSIC = WhyCantISleep.id("calm_music");
    /** Creepy music from a jukebox near the bed ({@code #whycantisleep:creepy} songs). */
    public static final ResourceLocation CREEPY_MUSIC = WhyCantISleep.id("creepy_music");
    /** Loud, lively music from a jukebox near the bed ({@code #whycantisleep:loud} songs). */
    public static final ResourceLocation LOUD_MUSIC = WhyCantISleep.id("loud_music");
    /** Soothing things placed by the bed (the {@code #whycantisleep:soothing} item tag): flowers, amethyst, candles. */
    public static final ResourceLocation SOOTHING = WhyCantISleep.id("soothing");
    /** Unsettling things placed by the bed (the {@code #whycantisleep:unsettling} item tag): skulls, cobwebs, sculk. */
    public static final ResourceLocation UNSETTLING = WhyCantISleep.id("unsettling");
    /** Restless before lying down. */
    public static final ResourceLocation RESTLESS = WhyCantISleep.id("restless");
    /** Sleepless before lying down: three days or five refusals, when phantoms come. */
    public static final ResourceLocation SLEEPLESS = WhyCantISleep.id("sleepless");
    /** A night terror. */
    public static final ResourceLocation NIGHTMARE = WhyCantISleep.id("nightmare");
    /** Woken before morning. */
    public static final ResourceLocation WOKE_EARLY = WhyCantISleep.id("woke_early");
    /** A bedtime routine kept: a closed, lit room. */
    public static final ResourceLocation ROUTINE = WhyCantISleep.id("routine");

    public SleepQuality {
        factors = List.copyOf(factors);
    }

    /** The score of a night with these factors: {@link #BASE} plus their points, held to 0 to 100. */
    public static SleepQuality of(List<Factor> factors) {
        int score = BASE;
        for (Factor factor : factors) score += factor.points();
        return new SleepQuality(Math.clamp(score, 0, 100), factors);
    }

    /** The factor that cost the most, or null when none cost anything. */
    public Factor worst() {
        Factor worst = null;
        for (Factor factor : this.factors) {
            if (factor.points() < 0 && (worst == null || factor.points() < worst.points())) worst = factor;
        }
        return worst;
    }

    /** The factor that helped the most, or null when none helped. */
    public Factor best() {
        Factor best = null;
        for (Factor factor : this.factors) {
            if (factor.points() > 0 && (best == null || factor.points() > best.points())) best = factor;
        }
        return best;
    }

    /**
     * One thing that made a night better or worse.
     *
     * @param id     what it is, for code to tell factors apart
     * @param label  how a player is told of it: "creepy music", "monsters prowling"
     * @param points how much it adds (or takes, below 0)
     */
    public record Factor(ResourceLocation id, Component label, int points) {
    }
}
