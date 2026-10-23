package net.ixdarklord.whycantisleep.api;

import net.ixdarklord.whycantisleep.sleep.DataSleepModifiers;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every sleep quality modifier ({@link SleepModifier}): those data packs describe (the mod's own among them; their ids
 * are the constants on {@link SleepQuality}) and those mods register in code. A night is scored by all of them, then
 * {@link SleepEvents#SCORE_QUALITY} has the last word. Server side.
 * <p>
 * No code is needed: a data pack adds one with a file at {@code data/<namespace>/whycantisleep/sleep_modifier/<name>.json}.
 * <pre>{@code
 * {
 *   "label": "mymod.modifier.rainy_night",     // a translation key, or plain text; a name in Title Case
 *   "points": 5,                               // fixed points: above 0 a buff, below a debuff
 *   "per": "near_blocks", "each": 2,           // and/or points per count of: monsters, soothing, unsettling,
 *   "most": 10,                                //   pillow_light or near_blocks; held to "most" (optional)
 *   "when": {                                  // optional; every condition given must hold
 *     "enclosed": true, "lit": true,           // the room: closed, lit all through
 *     "pillow_light": { "min": 0, "max": 7 },  // ranges: "min" and "max", both optional
 *     "monsters": { "max": 0 }, "soothing": { "min": 1 }, "unsettling": { "max": 0 },
 *     "calm_music": true, "creepy_music": false, "loud_music": false, "nightmare": false,
 *     "trouble": ["rested", "restless", "sleepless"],
 *     "reason": ["morning", "got_up", "hurt", "bed_gone", "other"],
 *     "dimension": "minecraft:overworld",
 *     "biome": "#minecraft:is_forest",         // a biome id or #tag
 *     "weather": "rain",                       // clear, rain or thunder
 *     "near_blocks": { "blocks": ["minecraft:campfire", "#minecraft:candles"], "range": 6, "count": { "min": 1 } }
 *   }
 * }
 * }</pre>
 * A file with the id of a modifier, the mod's own or another mod's, takes its place; {@code {"disabled": true}} takes
 * it out. The mod's own are files of this kind (in {@code data/whycantisleep/whycantisleep/sleep_modifier/}).
 * <p>
 * Mods that need more than this register one in code with {@link #register}.
 */
public final class SleepModifiers {
    private static final Map<ResourceLocation, SleepModifier> MODIFIERS = new LinkedHashMap<>();

    private SleepModifiers() {
    }

    /** Adds a modifier, or replaces the one with its id (keeping its place). Call while the mod initialises. */
    public static void register(SleepModifier modifier) {
        MODIFIERS.put(modifier.id(), modifier);
    }

    /** Takes out a modifier registered in code. Returns false when there was none with this id (the mod's own are data: turn them off with a data pack). */
    public static boolean remove(ResourceLocation id) {
        return MODIFIERS.remove(id) != null;
    }

    public static @Nullable SleepModifier get(ResourceLocation id) {
        for (SleepModifier modifier : all()) {
            if (modifier.id().equals(id)) return modifier;
        }
        return null;
    }

    /** Every modifier in play: those registered in code, then those from data packs, less any a data pack turned off. */
    public static Collection<SleepModifier> all() {
        Map<ResourceLocation, SleepModifier> all = new LinkedHashMap<>(MODIFIERS);
        for (SleepModifier modifier : DataSleepModifiers.all()) all.put(modifier.id(), modifier);
        for (ResourceLocation off : DataSleepModifiers.disabled()) all.remove(off);
        return Collections.unmodifiableCollection(all.values());
    }

    /** What every modifier makes of a night: the factors of those with a say, in order. */
    public static List<SleepQuality.Factor> score(SleepContext context) {
        List<SleepQuality.Factor> factors = new ArrayList<>();
        for (SleepModifier modifier : all()) {
            int points = modifier.points(context);
            if (points != 0) factors.add(new SleepQuality.Factor(modifier.id(), modifier.label(), points));
        }
        return factors;
    }
}
