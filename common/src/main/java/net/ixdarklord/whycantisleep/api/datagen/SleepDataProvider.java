package net.ixdarklord.whycantisleep.api.datagen;

import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Generates sleep effect and sleep quality modifier files for a mod's data, from {@link SleepEffectBuilder}s and
 * {@link SleepModifierBuilder}s: a plain {@link DataProvider}, for Fabric's and NeoForge's data generation alike.
 * <pre>{@code
 * public final class MySleepData extends SleepDataProvider {
 *     public MySleepData(PackOutput output) {
 *         super(output);
 *     }
 *
 *     protected void generate() {
 *         effect(MyMod.id("starry_dream"), SleepEffectBuilder.positive("mymod.sleep_effect.starry_dream")
 *                 .rarity(SleepEffect.Rarity.EPIC).quality(70, 100).effect(MobEffects.NIGHT_VISION, 300));
 *         modifier(MyMod.id("campfire_glow"), SleepModifierBuilder.of("mymod.modifier.campfire_glow")
 *                 .points(5).nearBlocks(6, 1, Blocks.CAMPFIRE));
 *         modifier(SleepQuality.LOUD_MUSIC, SleepModifierBuilder.disabled());   // no penalty for loud music
 *     }
 * }
 * // Fabric:   pack.addProvider((FabricDataOutput output) -> new MySleepData(output));
 * // NeoForge: event.createProvider(MySleepData::new);
 * }</pre>
 * The files land in {@code data/<namespace>/whycantisleep/sleep_effect/} and {@code .../sleep_modifier/}, named by id.
 */
public abstract class SleepDataProvider implements DataProvider {
    private final PackOutput.PathProvider effects;
    private final PackOutput.PathProvider modifiers;
    private final Map<ResourceLocation, JsonObject> effectFiles = new LinkedHashMap<>();
    private final Map<ResourceLocation, JsonObject> modifierFiles = new LinkedHashMap<>();

    protected SleepDataProvider(PackOutput output) {
        this.effects = output.createPathProvider(PackOutput.Target.DATA_PACK, "whycantisleep/sleep_effect");
        this.modifiers = output.createPathProvider(PackOutput.Target.DATA_PACK, "whycantisleep/sleep_modifier");
    }

    /** Adds the effects and modifiers, with {@link #effect} and {@link #modifier}. */
    protected abstract void generate();

    /** A sleep effect, at its id. */
    protected final void effect(ResourceLocation id, SleepEffectBuilder effect) {
        if (this.effectFiles.put(id, effect.toJson()) != null) throw new IllegalStateException("Two sleep effects with the id " + id);
    }

    /** A sleep quality modifier, at its id (the id of another, to replace it or, {@link SleepModifierBuilder#disabled}, to take it out). */
    protected final void modifier(ResourceLocation id, SleepModifierBuilder modifier) {
        if (this.modifierFiles.put(id, modifier.toJson()) != null) throw new IllegalStateException("Two sleep modifiers with the id " + id);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        this.effectFiles.clear();
        this.modifierFiles.clear();
        this.generate();
        List<CompletableFuture<?>> writes = new ArrayList<>();
        this.effectFiles.forEach((id, json) -> writes.add(DataProvider.saveStable(cache, json, this.effects.json(id))));
        this.modifierFiles.forEach((id, json) -> writes.add(DataProvider.saveStable(cache, json, this.modifiers.json(id))));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Why Can't I Sleep? sleep effects and modifiers";
    }
}
