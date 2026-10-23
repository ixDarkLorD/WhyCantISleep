package net.ixdarklord.whycantisleep.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Something a night's sleep leaves a player with when they wake in the morning: a dream that helps (positive), one that
 * hurts (negative), or one that is neither, only odd (neutral). After a good night positive ones are likelier, after a
 * bad one negative ones; neutral ones can come after any. Of the kind picked, one is drawn by {@link #weight}, which
 * its {@link Rarity} gives unless it says its own, among those whose quality range the night's score falls in.
 * <p>
 * Data packs add them with a file at {@code data/<namespace>/whycantisleep/sleep_effect/<name>.json}:
 * <pre>{@code
 * {
 *   "kind": "positive",                                   // positive, negative or neutral
 *   "rarity": "rare",                                     // common, uncommon, rare, epic or legendary
 *   "weight": 12,                                         // optional; else the rarity's
 *   "quality": { "min": 60, "max": 100 },                 // optional; the night's scores it can follow
 *   "effects": [ { "id": "minecraft:luck", "duration": 6000, "amplifier": 0 } ],
 *   "name": "mymod.sleep_effect.lucky_dream"              // a translation key, or plain text
 * }
 * }</pre>
 * The mod's own are files of this kind too (in {@code data/whycantisleep/whycantisleep/sleep_effect/}), so a pack can
 * change or replace them. Mods register theirs with {@link SleepEffects#register}, and may give one an action of its
 * own to run on waking, as well as or instead of mob effects.
 *
 * @param id         what it is
 * @param kind       positive, negative or neutral
 * @param rarity     how rare it is, and the color it is named in
 * @param weight     its share of the draw among those of its kind
 * @param minQuality the lowest night's score it can follow
 * @param maxQuality the highest
 * @param effects    the mob effects it gives
 * @param name       how the player is told of it
 * @param action     something more to do on waking, for those registered in code; null for none
 */
public record SleepEffect(ResourceLocation id, Kind kind, Rarity rarity, int weight, int minQuality, int maxQuality, List<Effect> effects, Component name,
                          @Nullable Consumer<ServerPlayer> action) {
    public SleepEffect {
        effects = List.copyOf(effects);
    }

    /** One without an action of its own, its weight the rarity's. */
    public static SleepEffect of(ResourceLocation id, Kind kind, Rarity rarity, Component name, Effect... effects) {
        return new SleepEffect(id, kind, rarity, rarity.weight(), 0, 100, List.of(effects), name, null);
    }

    /** The same, able to follow only nights scored in this range. */
    public SleepEffect withQuality(int min, int max) {
        return new SleepEffect(this.id, this.kind, this.rarity, this.weight, min, max, this.effects, this.name, this.action);
    }

    /** The same, with another weight. */
    public SleepEffect withWeight(int weight) {
        return new SleepEffect(this.id, this.kind, this.rarity, weight, this.minQuality, this.maxQuality, this.effects, this.name, this.action);
    }

    /** The same, running this too on waking. */
    public SleepEffect withAction(Consumer<ServerPlayer> action) {
        return new SleepEffect(this.id, this.kind, this.rarity, this.weight, this.minQuality, this.maxQuality, this.effects, this.name, action);
    }

    /** Whether it can follow a night with this score. */
    public boolean fits(int score) {
        return score >= this.minQuality && score <= this.maxQuality;
    }

    /** Gives it to a player: its mob effects, then its action. */
    public void apply(ServerPlayer player) {
        for (Effect effect : this.effects) player.addEffect(effect.instance());
        if (this.action != null) this.action.accept(player);
    }

    /** Whether it helps, hurts, or neither. */
    public enum Kind implements StringRepresentable {
        POSITIVE, NEGATIVE, NEUTRAL;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    /** How rare it is: its default weight in the draw, and the color it is named in (the game's item rarity colors). */
    public enum Rarity implements StringRepresentable {
        COMMON(60, 0xFFFFFF),
        UNCOMMON(25, 0xFFFF55),
        RARE(10, 0x55FFFF),
        EPIC(4, 0xFF55FF),
        LEGENDARY(1, 0xFFAA00);

        public static final Codec<Rarity> CODEC = StringRepresentable.fromEnum(Rarity::values);
        private final int weight;
        private final int color;

        Rarity(int weight, int color) {
            this.weight = weight;
            this.color = color;
        }

        public int weight() {
            return this.weight;
        }

        /** The color it is named in, as {@code 0xRRGGBB}. */
        public int color() {
            return this.color;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * A mob effect it gives.
     *
     * @param duration in ticks
     */
    public record Effect(MobEffect effect, int duration, int amplifier) {
        public static final Codec<Effect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.MOB_EFFECT.byNameCodec().fieldOf("id").forGetter(Effect::effect),
                Codec.intRange(1, 20 * 60 * 60).optionalFieldOf("duration", 600).forGetter(Effect::duration),
                Codec.intRange(0, 9).optionalFieldOf("amplifier", 0).forGetter(Effect::amplifier)
        ).apply(instance, Effect::new));

        public MobEffectInstance instance() {
            return new MobEffectInstance(this.effect, this.duration, this.amplifier, false, true, true);
        }
    }
}
