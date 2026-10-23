package net.ixdarklord.whycantisleep.api.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.ixdarklord.whycantisleep.api.SleepEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

/**
 * Builds a sleep effect's data pack file ({@link SleepEffect} has the format), for data generation
 * ({@link SleepDataProvider}).
 * <pre>{@code
 * SleepEffectBuilder.positive("mymod.sleep_effect.starry_dream")
 *         .rarity(SleepEffect.Rarity.EPIC)
 *         .quality(70, 100)
 *         .effect(MobEffects.NIGHT_VISION, 300)
 *         .effect(MobEffects.MOVEMENT_SPEED, 60, 1);
 * }</pre>
 */
public final class SleepEffectBuilder {
    private final SleepEffect.Kind kind;
    private final String name;
    private SleepEffect.Rarity rarity = SleepEffect.Rarity.COMMON;
    private Integer weight;
    private int minQuality;
    private int maxQuality = 100;
    private final JsonArray effects = new JsonArray();

    private SleepEffectBuilder(SleepEffect.Kind kind, String name) {
        this.kind = kind;
        this.name = name;
    }

    /**
     * A sleep effect of a kind.
     *
     * @param name a translation key, or plain text
     */
    public static SleepEffectBuilder of(SleepEffect.Kind kind, String name) {
        return new SleepEffectBuilder(kind, name);
    }

    public static SleepEffectBuilder positive(String name) {
        return of(SleepEffect.Kind.POSITIVE, name);
    }

    public static SleepEffectBuilder negative(String name) {
        return of(SleepEffect.Kind.NEGATIVE, name);
    }

    public static SleepEffectBuilder neutral(String name) {
        return of(SleepEffect.Kind.NEUTRAL, name);
    }

    /** How rare it is (common by default); its weight is the rarity's unless {@link #weight} says otherwise. */
    public SleepEffectBuilder rarity(SleepEffect.Rarity rarity) {
        this.rarity = rarity;
        return this;
    }

    /** Its own share of the draw among those of its kind. */
    public SleepEffectBuilder weight(int weight) {
        this.weight = weight;
        return this;
    }

    /** The nights' scores it can follow, 0 to 100 both included. */
    public SleepEffectBuilder quality(int min, int max) {
        this.minQuality = min;
        this.maxQuality = max;
        return this;
    }

    /** A mob effect it gives, for a number of seconds, at level I. */
    public SleepEffectBuilder effect(MobEffect effect, int seconds) {
        return this.effect(effect, seconds, 0);
    }

    /** A mob effect it gives, for a number of seconds, at an amplifier (0 for level I). */
    public SleepEffectBuilder effect(MobEffect effect, int seconds, int amplifier) {
        JsonObject entry = new JsonObject();
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        if (id == null) throw new IllegalArgumentException("An unregistered mob effect");
        entry.addProperty("id", id.toString());
        entry.addProperty("duration", seconds * 20);
        entry.addProperty("amplifier", amplifier);
        this.effects.add(entry);
        return this;
    }

    /** The file. */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("kind", this.kind.getSerializedName());
        json.addProperty("rarity", this.rarity.getSerializedName());
        if (this.weight != null) json.addProperty("weight", this.weight);
        if (this.minQuality != 0 || this.maxQuality != 100) {
            JsonObject quality = new JsonObject();
            quality.addProperty("min", this.minQuality);
            quality.addProperty("max", this.maxQuality);
            json.add("quality", quality);
        }
        json.add("effects", this.effects.deepCopy());
        json.addProperty("name", this.name);
        return json;
    }
}
