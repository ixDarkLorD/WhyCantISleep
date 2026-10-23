package net.ixdarklord.whycantisleep.sleep;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.api.SleepEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * The sleep effects data packs describe, at {@code data/<namespace>/whycantisleep/sleep_effect/<name>.json} (the
 * format is in {@link SleepEffect}), the mod's own among them. Loaded on the server with its data.
 */
public final class DataSleepEffects extends SimpleJsonResourceReloadListener {
    public static final DataSleepEffects INSTANCE = new DataSleepEffects();
    private static List<SleepEffect> effects = List.of();

    private DataSleepEffects() {
        super(new Gson(), "whycantisleep/sleep_effect");
    }

    public static List<SleepEffect> all() {
        return effects;
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, JsonElement> files, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        List<SleepEffect> loaded = new ArrayList<>();
        new TreeMap<>(files).forEach((id, json) -> Definition.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> WhyCantISleep.LOGGER.error("Skipping the sleep effect {}: {}", id, error))
                .ifPresent(definition -> loaded.add(definition.effect(id))));
        effects = List.copyOf(loaded);
        WhyCantISleep.LOGGER.info("Loaded {} sleep effect(s) from data packs", loaded.size());
    }

    private record Quality(int min, int max) {
        private static final Codec<Quality> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, 100).optionalFieldOf("min", 0).forGetter(Quality::min),
                Codec.intRange(0, 100).optionalFieldOf("max", 100).forGetter(Quality::max)
        ).apply(instance, Quality::new));
    }

    private record Definition(SleepEffect.Kind kind, SleepEffect.Rarity rarity, Optional<Integer> weight, Quality quality,
                              List<SleepEffect.Effect> effects, String name) {
        private static final Codec<Definition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                SleepEffect.Kind.CODEC.fieldOf("kind").forGetter(Definition::kind),
                SleepEffect.Rarity.CODEC.optionalFieldOf("rarity", SleepEffect.Rarity.COMMON).forGetter(Definition::rarity),
                Codec.intRange(0, 10000).optionalFieldOf("weight").forGetter(Definition::weight),
                Quality.CODEC.optionalFieldOf("quality", new Quality(0, 100)).forGetter(Definition::quality),
                SleepEffect.Effect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(Definition::effects),
                Codec.STRING.fieldOf("name").forGetter(Definition::name)
        ).apply(instance, Definition::new));

        private SleepEffect effect(ResourceLocation id) {
            // A plain sentence works as well as a translation key.
            return new SleepEffect(id, this.kind, this.rarity, this.weight.orElse(this.rarity.weight()), this.quality.min(), this.quality.max(),
                    this.effects, Component.translatableWithFallback(this.name, this.name), null);
        }
    }
}
