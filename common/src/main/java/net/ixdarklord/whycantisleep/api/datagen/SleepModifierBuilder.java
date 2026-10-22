package net.ixdarklord.whycantisleep.api.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.ixdarklord.whycantisleep.api.SleepModifiers;
import net.ixdarklord.whycantisleep.api.SleepTrouble;
import net.ixdarklord.whycantisleep.api.WakeReason;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

import java.util.Locale;

/**
 * Builds a sleep quality modifier's data pack file ({@link SleepModifiers} has the format), for data generation
 * ({@link SleepDataProvider}).
 * <pre>{@code
 * SleepModifierBuilder.of("mymod.modifier.campfire_glow")
 *         .perNearBlocks(2, 6)
 *         .nearBlocks(6, 1, Blocks.CAMPFIRE)
 *         .enclosed(true);
 * SleepModifierBuilder.disabled();          // takes out the modifier with this file's id
 * }</pre>
 */
public final class SleepModifierBuilder {
    private final JsonObject json = new JsonObject();
    private final JsonObject when = new JsonObject();

    private SleepModifierBuilder() {
    }

    /**
     * A modifier, with no points yet: give them with {@link #points} or a {@code per...} count.
     *
     * @param label a translation key, or plain text; a name, in Title Case
     */
    public static SleepModifierBuilder of(String label) {
        SleepModifierBuilder builder = new SleepModifierBuilder();
        builder.json.addProperty("label", label);
        return builder;
    }

    /** A file that takes out the modifier with its id: the mod's own, or another mod's. */
    public static SleepModifierBuilder disabled() {
        SleepModifierBuilder builder = new SleepModifierBuilder();
        builder.json.addProperty("disabled", true);
        return builder;
    }

    /** Fixed points: above 0 a buff, below a debuff. */
    public SleepModifierBuilder points(int points) {
        this.json.addProperty("points", points);
        return this;
    }

    // Points for each of a count, held to a most.
    private SleepModifierBuilder per(String count, int each, int most) {
        this.json.addProperty("per", count);
        this.json.addProperty("each", each);
        this.json.addProperty("most", most);
        return this;
    }

    public SleepModifierBuilder perMonster(int each, int most) {
        return this.per("monsters", each, most);
    }

    public SleepModifierBuilder perSoothing(int each, int most) {
        return this.per("soothing", each, most);
    }

    public SleepModifierBuilder perUnsettling(int each, int most) {
        return this.per("unsettling", each, most);
    }

    public SleepModifierBuilder perPillowLight(int each, int most) {
        return this.per("pillow_light", each, most);
    }

    /** Points for each block counted by {@link #nearBlocks}. */
    public SleepModifierBuilder perNearBlocks(int each, int most) {
        return this.per("near_blocks", each, most);
    }

    // The conditions: every one given must hold.

    public SleepModifierBuilder enclosed(boolean enclosed) {
        this.when.addProperty("enclosed", enclosed);
        return this;
    }

    public SleepModifierBuilder lit(boolean lit) {
        this.when.addProperty("lit", lit);
        return this;
    }

    public SleepModifierBuilder pillowLight(int min, int max) {
        this.when.add("pillow_light", range(min, max));
        return this;
    }

    public SleepModifierBuilder monsters(int min, int max) {
        this.when.add("monsters", range(min, max));
        return this;
    }

    public SleepModifierBuilder soothing(int min, int max) {
        this.when.add("soothing", range(min, max));
        return this;
    }

    public SleepModifierBuilder unsettling(int min, int max) {
        this.when.add("unsettling", range(min, max));
        return this;
    }

    public SleepModifierBuilder calmMusic(boolean played) {
        this.when.addProperty("calm_music", played);
        return this;
    }

    public SleepModifierBuilder creepyMusic(boolean played) {
        this.when.addProperty("creepy_music", played);
        return this;
    }

    public SleepModifierBuilder loudMusic(boolean played) {
        this.when.addProperty("loud_music", played);
        return this;
    }

    public SleepModifierBuilder nightmare(boolean had) {
        this.when.addProperty("nightmare", had);
        return this;
    }

    /** Only after these levels of sleep trouble. */
    public SleepModifierBuilder trouble(SleepTrouble.Level... levels) {
        JsonArray array = new JsonArray();
        for (SleepTrouble.Level level : levels) array.add(level.name().toLowerCase(Locale.ROOT));
        this.when.add("trouble", array);
        return this;
    }

    /** Only on waking for these reasons. */
    public SleepModifierBuilder reason(WakeReason... reasons) {
        JsonArray array = new JsonArray();
        for (WakeReason reason : reasons) array.add(reason.name().toLowerCase(Locale.ROOT));
        this.when.add("reason", array);
        return this;
    }

    public SleepModifierBuilder dimension(ResourceKey<Level> dimension) {
        this.when.addProperty("dimension", dimension.identifier().toString());
        return this;
    }

    public SleepModifierBuilder biome(ResourceKey<Biome> biome) {
        this.when.addProperty("biome", biome.identifier().toString());
        return this;
    }

    public SleepModifierBuilder biome(TagKey<Biome> biomes) {
        this.when.addProperty("biome", "#" + biomes.location());
        return this;
    }

    /** Only in this weather: {@code "clear"}, {@code "rain"} or {@code "thunder"}. */
    public SleepModifierBuilder weather(String weather) {
        this.when.addProperty("weather", weather);
        return this;
    }

    /** Only with at least so many of these blocks within a range of the bed (and counted for {@link #perNearBlocks}). */
    public SleepModifierBuilder nearBlocks(int range, int atLeast, Block... blocks) {
        JsonArray array = new JsonArray();
        for (Block block : blocks) array.add(block.builtInRegistryHolder().key().identifier().toString());
        return this.nearBlocks(range, atLeast, array);
    }

    /** The same, by block tags. */
    @SafeVarargs
    public final SleepModifierBuilder nearBlocks(int range, int atLeast, TagKey<Block>... tags) {
        JsonArray array = new JsonArray();
        for (TagKey<Block> tag : tags) array.add("#" + tag.location());
        return this.nearBlocks(range, atLeast, array);
    }

    private SleepModifierBuilder nearBlocks(int range, int atLeast, JsonArray blocks) {
        JsonObject near = new JsonObject();
        near.add("blocks", blocks);
        near.addProperty("range", range);
        JsonObject count = new JsonObject();
        count.addProperty("min", atLeast);
        near.add("count", count);
        this.when.add("near_blocks", near);
        return this;
    }

    private static JsonObject range(int min, int max) {
        JsonObject range = new JsonObject();
        range.addProperty("min", min);
        range.addProperty("max", max);
        return range;
    }

    /** The file. */
    public JsonObject toJson() {
        JsonObject json = this.json.deepCopy();
        if (!this.when.isEmpty()) json.add("when", this.when.deepCopy());
        return json;
    }
}
