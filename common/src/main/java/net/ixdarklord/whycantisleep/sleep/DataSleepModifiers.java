package net.ixdarklord.whycantisleep.sleep;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.api.SleepContext;
import net.ixdarklord.whycantisleep.api.SleepModifier;
import net.ixdarklord.whycantisleep.api.SleepTrouble;
import net.ixdarklord.whycantisleep.api.WakeReason;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The sleep quality modifiers data packs describe, at {@code data/<namespace>/whycantisleep/sleep_modifier/<name>.json}
 * (the format is in {@link net.ixdarklord.whycantisleep.api.SleepModifiers}), the mod's own among them. A file with the
 * id of a modifier takes its place; one with {@code "disabled": true} takes it out. Loaded on the server with its data.
 */
public final class DataSleepModifiers extends SimpleJsonResourceReloadListener {
    public static final DataSleepModifiers INSTANCE = new DataSleepModifiers();
    private static List<SleepModifier> modifiers = List.of();
    private static Set<ResourceLocation> disabled = Set.of();

    private DataSleepModifiers() {
        super(new Gson(), "whycantisleep/sleep_modifier");
    }

    /** The modifiers data packs add or replace. */
    public static List<SleepModifier> all() {
        return modifiers;
    }

    /** The ids data packs turned off, the mod's own or another mod's. */
    public static Set<ResourceLocation> disabled() {
        return disabled;
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, JsonElement> files, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        List<SleepModifier> loaded = new ArrayList<>();
        Set<ResourceLocation> off = new TreeSet<>();
        new TreeMap<>(files).forEach((id, json) -> Definition.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> WhyCantISleep.LOGGER.error("Skipping the sleep modifier {}: {}", id, error))
                .ifPresent(definition -> {
                    if (definition.disabled()) off.add(id);
                    else loaded.add(new DataModifier(id, definition));
                }));
        modifiers = List.copyOf(loaded);
        disabled = Set.copyOf(off);
        WhyCantISleep.LOGGER.info("Loaded {} sleep modifier(s) from data packs ({} turned off)", loaded.size(), off.size());
    }

    // An enum by its name in lower case, as data packs write it.
    private static <E extends Enum<E>> Codec<E> lowerCase(Class<E> type) {
        return Codec.STRING.comapFlatMap(name -> {
            try {
                return DataResult.success(Enum.valueOf(type, name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                return DataResult.error(() -> "Unknown " + type.getSimpleName() + " " + name);
            }
        }, value -> value.name().toLowerCase(Locale.ROOT));
    }

    /** From and to, both included. */
    private record Range(int min, int max) {
        private static final Codec<Range> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("min", Integer.MIN_VALUE).forGetter(Range::min),
                Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(Range::max)
        ).apply(instance, Range::new));

        private boolean test(int value) {
            return value >= this.min && value <= this.max;
        }
    }

    /** What a count of a modifier's points is of. */
    private enum Per {
        MONSTERS, SOOTHING, UNSETTLING, PILLOW_LIGHT, NEAR_BLOCKS
    }

    private enum Weather {
        CLEAR, RAIN, THUNDER
    }

    /** Blocks placed near the bed: ids and #tags, how far round it, and how many there must be. */
    private record NearBlocks(List<String> blocks, int range, Range count) {
        private static final Codec<NearBlocks> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf().fieldOf("blocks").forGetter(NearBlocks::blocks),
                Codec.intRange(1, 12).optionalFieldOf("range", 6).forGetter(NearBlocks::range),
                Range.CODEC.optionalFieldOf("count", new Range(1, Integer.MAX_VALUE)).forGetter(NearBlocks::count)
        ).apply(instance, NearBlocks::new));

        private int count(ServerLevel level, BlockPos bed) {
            List<ResourceLocation> ids = new ArrayList<>();
            List<TagKey<Block>> tags = new ArrayList<>();
            for (String entry : this.blocks) {
                if (entry.startsWith("#")) tags.add(TagKey.create(Registries.BLOCK, new ResourceLocation(entry.substring(1))));
                else ids.add(new ResourceLocation(entry));
            }
            int count = 0;
            int up = Math.min(this.range, 4);
            for (BlockPos pos : BlockPos.betweenClosed(bed.offset(-this.range, -up, -this.range), bed.offset(this.range, up, this.range))) {
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) continue;
                if (ids.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock())) || tags.stream().anyMatch(state::is)) count++;
            }
            return count;
        }
    }

    /** When a modifier has its say: every condition given must hold. */
    private record Conditions(Optional<Boolean> enclosed, Optional<Boolean> lit, Optional<Range> pillowLight, Optional<Range> monsters,
                              Optional<Range> soothing, Optional<Range> unsettling, Optional<Boolean> calmMusic, Optional<Boolean> creepyMusic,
                              Optional<Boolean> loudMusic, Optional<List<SleepTrouble.Level>> trouble, Optional<Boolean> nightmare,
                              Optional<List<WakeReason>> reason, Optional<ResourceKey<Level>> dimension, Optional<String> biome,
                              Optional<Weather> weather, Optional<NearBlocks> nearBlocks) {
        private static final Conditions ALWAYS = new Conditions(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        private static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("enclosed").forGetter(Conditions::enclosed),
                Codec.BOOL.optionalFieldOf("lit").forGetter(Conditions::lit),
                Range.CODEC.optionalFieldOf("pillow_light").forGetter(Conditions::pillowLight),
                Range.CODEC.optionalFieldOf("monsters").forGetter(Conditions::monsters),
                Range.CODEC.optionalFieldOf("soothing").forGetter(Conditions::soothing),
                Range.CODEC.optionalFieldOf("unsettling").forGetter(Conditions::unsettling),
                Codec.BOOL.optionalFieldOf("calm_music").forGetter(Conditions::calmMusic),
                Codec.BOOL.optionalFieldOf("creepy_music").forGetter(Conditions::creepyMusic),
                Codec.BOOL.optionalFieldOf("loud_music").forGetter(Conditions::loudMusic),
                lowerCase(SleepTrouble.Level.class).listOf().optionalFieldOf("trouble").forGetter(Conditions::trouble),
                Codec.BOOL.optionalFieldOf("nightmare").forGetter(Conditions::nightmare),
                lowerCase(WakeReason.class).listOf().optionalFieldOf("reason").forGetter(Conditions::reason),
                Level.RESOURCE_KEY_CODEC.optionalFieldOf("dimension").forGetter(Conditions::dimension),
                Codec.STRING.optionalFieldOf("biome").forGetter(Conditions::biome),
                lowerCase(Weather.class).optionalFieldOf("weather").forGetter(Conditions::weather),
                NearBlocks.CODEC.optionalFieldOf("near_blocks").forGetter(Conditions::nearBlocks)
        ).apply(instance, Conditions::new));

        private boolean test(SleepContext context) {
            ServerLevel level = context.player().serverLevel();
            if (this.enclosed.isPresent() && this.enclosed.get() != context.enclosed()) return false;
            if (this.lit.isPresent() && this.lit.get() != context.lit()) return false;
            if (this.pillowLight.isPresent() && !this.pillowLight.get().test(context.pillowLight())) return false;
            if (this.monsters.isPresent() && !this.monsters.get().test(context.monsters())) return false;
            if (this.soothing.isPresent() && !this.soothing.get().test(context.soothing())) return false;
            if (this.unsettling.isPresent() && !this.unsettling.get().test(context.unsettling())) return false;
            if (this.calmMusic.isPresent() && this.calmMusic.get() != context.calmMusic()) return false;
            if (this.creepyMusic.isPresent() && this.creepyMusic.get() != context.creepyMusic()) return false;
            if (this.loudMusic.isPresent() && this.loudMusic.get() != context.loudMusic()) return false;
            if (this.trouble.isPresent() && !this.trouble.get().contains(context.trouble())) return false;
            if (this.nightmare.isPresent() && this.nightmare.get() != context.nightmare()) return false;
            if (this.reason.isPresent() && !this.reason.get().contains(context.reason())) return false;
            if (this.dimension.isPresent() && level.dimension() != this.dimension.get()) return false;
            if (this.biome.isPresent() && !biome(level, context.bed(), this.biome.get())) return false;
            if (this.weather.isPresent() && weather(level) != this.weather.get()) return false;
            return this.nearBlocks.isEmpty() || this.nearBlocks.get().count().test(this.nearBlocks.get().count(level, context.bed()));
        }

        private static boolean biome(ServerLevel level, BlockPos bed, String biome) {
            Holder<Biome> here = level.getBiome(bed);
            if (biome.startsWith("#")) return here.is(TagKey.create(Registries.BIOME, new ResourceLocation(biome.substring(1))));
            return here.is(new ResourceLocation(biome));
        }

        private static Weather weather(ServerLevel level) {
            return level.isThundering() ? Weather.THUNDER : level.isRaining() ? Weather.RAIN : Weather.CLEAR;
        }
    }

    private record Definition(boolean disabled, String label, int points, Optional<Per> per, int each, Optional<Integer> most, Conditions when) {
        private static final Codec<Definition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("disabled", false).forGetter(Definition::disabled),
                Codec.STRING.optionalFieldOf("label", "").forGetter(Definition::label),
                Codec.intRange(-100, 100).optionalFieldOf("points", 0).forGetter(Definition::points),
                lowerCase(Per.class).optionalFieldOf("per").forGetter(Definition::per),
                Codec.intRange(-100, 100).optionalFieldOf("each", 0).forGetter(Definition::each),
                Codec.intRange(-100, 100).optionalFieldOf("most").forGetter(Definition::most),
                Conditions.CODEC.optionalFieldOf("when", Conditions.ALWAYS).forGetter(Definition::when)
        ).apply(instance, Definition::new));
    }

    private record DataModifier(ResourceLocation id, Definition definition) implements SleepModifier {
        @Override
        public Component label() {
            // A plain sentence works as well as a translation key.
            return Component.translatableWithFallback(this.definition.label(), this.definition.label());
        }

        @Override
        public int points(SleepContext context) {
            Definition definition = this.definition;
            if (!definition.when().test(context)) return 0;
            int points = definition.points();
            if (definition.per().isPresent()) points += definition.each() * count(definition.per().get(), context);
            if (definition.most().isPresent()) {
                int most = definition.most().get();
                points = most >= 0 ? Math.min(points, most) : Math.max(points, most);
            }
            return points;
        }

        private int count(Per per, SleepContext context) {
            return switch (per) {
                case MONSTERS -> context.monsters();
                case SOOTHING -> context.soothing();
                case UNSETTLING -> context.unsettling();
                case PILLOW_LIGHT -> context.pillowLight();
                case NEAR_BLOCKS -> {
                    NearBlocks near = this.definition.when().nearBlocks().orElse(null);
                    yield near == null ? 0 : near.count(context.player().serverLevel(), context.bed());
                }
            };
        }
    }
}
