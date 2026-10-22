package net.ixdarklord.whycantisleep.api;

import net.ixdarklord.whycantisleep.sleep.DataSleepEffects;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every sleep effect ({@link SleepEffect}): those registered by mods, and those data packs describe (the mod's own
 * among them). A data pack effect with the id of a registered one takes its place. Server side.
 * <pre>{@code
 * SleepEffects.register(SleepEffect.of(MyMod.id("starry_dream"), SleepEffect.Kind.POSITIVE, SleepEffect.Rarity.EPIC,
 *         Component.translatable("mymod.sleep_effect.starry_dream"),
 *         new SleepEffect.Effect(MobEffects.NIGHT_VISION, 20 * 60 * 5, 0))
 *         .withQuality(70, 100)
 *         .withAction(player -> player.giveExperiencePoints(30)));
 * }</pre>
 */
public final class SleepEffects {
    private static final Map<Identifier, SleepEffect> REGISTERED = new LinkedHashMap<>();
    // How likely each kind is after a night: positive with its score above 40, negative below 60, neutral always a little.
    private static final int POSITIVE_FROM = 40;
    private static final int NEGATIVE_BELOW = 60;
    private static final int NEUTRAL_WEIGHT = 20;

    private SleepEffects() {
    }

    /** Adds a sleep effect. Call while the mod initialises. */
    public static void register(SleepEffect effect) {
        REGISTERED.put(effect.id(), effect);
    }

    /** Every sleep effect, registered and from data packs. */
    public static Collection<SleepEffect> all() {
        Map<Identifier, SleepEffect> all = new LinkedHashMap<>(REGISTERED);
        for (SleepEffect effect : DataSleepEffects.all()) all.put(effect.id(), effect);
        return all.values();
    }

    public static @Nullable SleepEffect get(Identifier id) {
        for (SleepEffect effect : all()) {
            if (effect.id().equals(id)) return effect;
        }
        return null;
    }

    /**
     * Draws a sleep effect for a night with this score: first its kind (positive likelier the better the night,
     * negative the worse, neutral now and then), then one of that kind by weight among those that fit the score.
     * Null when none fits.
     */
    public static @Nullable SleepEffect draw(int score, RandomSource random) {
        int positive = Math.max(0, score - POSITIVE_FROM);
        int negative = Math.max(0, NEGATIVE_BELOW - score);
        int roll = random.nextInt(positive + negative + NEUTRAL_WEIGHT);
        SleepEffect.Kind kind = roll < positive ? SleepEffect.Kind.POSITIVE : roll < positive + negative ? SleepEffect.Kind.NEGATIVE : SleepEffect.Kind.NEUTRAL;
        SleepEffect drawn = draw(kind, score, random);
        // A kind with nothing that fits gives way to neutral, which fits every night.
        return drawn != null || kind == SleepEffect.Kind.NEUTRAL ? drawn : draw(SleepEffect.Kind.NEUTRAL, score, random);
    }

    /** Draws one of a kind, by weight, among those that fit the score; null when none does. */
    public static @Nullable SleepEffect draw(SleepEffect.Kind kind, int score, RandomSource random) {
        List<SleepEffect> fitting = new ArrayList<>();
        int total = 0;
        for (SleepEffect effect : all()) {
            if (effect.kind() == kind && effect.fits(score) && effect.weight() > 0) {
                fitting.add(effect);
                total += effect.weight();
            }
        }
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        for (SleepEffect effect : fitting) {
            roll -= effect.weight();
            if (roll < 0) return effect;
        }
        return fitting.getLast();
    }
}
