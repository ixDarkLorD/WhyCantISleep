package net.ixdarklord.whycantisleep.hiding;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.api.HidingPosture;
import net.ixdarklord.whycantisleep.api.HidingSpot;
import net.ixdarklord.whycantisleep.api.HidingSpotType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * The hiding spots data packs describe, at {@code data/<namespace>/whycantisleep/hiding_spot/<name>.json} (the format is
 * in {@link net.ixdarklord.whycantisleep.api.HidingSpots}). Loaded on the server with its data; a client never has them,
 * and lets the server decide what a sneak-use of such a block does.
 */
public final class DataHidingSpots extends SimpleJsonResourceReloadListener {
    public static final DataHidingSpots INSTANCE = new DataHidingSpots();
    private static List<DataType> types = List.of();

    private DataHidingSpots() {
        super(new Gson(), "whycantisleep/hiding_spot");
    }

    public static @Nullable HidingSpotType get(ResourceLocation id) {
        for (DataType type : types) {
            if (type.id.equals(id)) return type;
        }
        return null;
    }

    public static @Nullable HidingSpotType typeFor(BlockState state) {
        for (DataType type : types) {
            if (type.matches(state)) return type;
        }
        return null;
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, JsonElement> files, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        List<DataType> loaded = new ArrayList<>();
        new TreeMap<>(files).forEach((id, json) -> Definition.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> WhyCantISleep.LOGGER.error("Skipping the hiding spot {}: {}", id, error))
                .ifPresent(definition -> loaded.add(new DataType(id, definition))));
        types = List.copyOf(loaded);
        if (!loaded.isEmpty()) WhyCantISleep.LOGGER.info("Loaded {} hiding spot(s) from data packs", loaded.size());
    }

    /** Which way the player faces in a data pack spot. */
    private enum Facing {
        BLOCK, OPPOSITE, PLAYER;

        private static final Codec<Facing> CODEC = Codec.STRING.comapFlatMap(Facing::parse, facing -> facing.name().toLowerCase(Locale.ROOT));

        private static DataResult<Facing> parse(String name) {
            try {
                return DataResult.success(valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                return DataResult.error(() -> "Unknown facing " + name + " (block, opposite or player)");
            }
        }
    }

    private static final Codec<HidingPosture> POSTURE = Codec.STRING.comapFlatMap(name -> {
        try {
            return DataResult.success(HidingPosture.valueOf(name.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return DataResult.error(() -> "Unknown posture " + name + " (lying, standing or tucked)");
        }
    }, posture -> posture.name().toLowerCase(Locale.ROOT));

    private record Definition(List<String> blocks, HidingPosture posture, Vec3 offset, float eyeHeight, float squint, boolean needsFloor, float hideTimeScale,
                              Facing facing, String where) {
        private static final Codec<Definition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf().fieldOf("blocks").forGetter(Definition::blocks),
                POSTURE.optionalFieldOf("posture", HidingPosture.LYING).forGetter(Definition::posture),
                Vec3.CODEC.optionalFieldOf("offset", Vec3.ZERO).forGetter(Definition::offset),
                Codec.FLOAT.optionalFieldOf("eye_height", -1.0F).forGetter(Definition::eyeHeight),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("squint", HidingSpot.DEFAULT_SQUINT).forGetter(Definition::squint),
                Codec.BOOL.optionalFieldOf("needs_floor", true).forGetter(Definition::needsFloor),
                Codec.floatRange(0.05F, 10.0F).optionalFieldOf("hide_time_scale", 1.0F).forGetter(Definition::hideTimeScale),
                Facing.CODEC.optionalFieldOf("facing", Facing.BLOCK).forGetter(Definition::facing),
                Codec.STRING.optionalFieldOf("where", "gui.whycantisleep.hiding.where.default").forGetter(Definition::where)
        ).apply(instance, Definition::new));
    }

    private static final class DataType implements HidingSpotType {
        private final ResourceLocation id;
        private final Definition definition;
        private final List<ResourceLocation> blocks = new ArrayList<>();
        private final List<TagKey<Block>> tags = new ArrayList<>();

        private DataType(ResourceLocation id, Definition definition) {
            this.id = id;
            this.definition = definition;
            for (String entry : definition.blocks()) {
                if (entry.startsWith("#")) this.tags.add(TagKey.create(Registries.BLOCK, new ResourceLocation(entry.substring(1))));
                else this.blocks.add(new ResourceLocation(entry));
            }
        }

        @Override
        public ResourceLocation id() {
            return this.id;
        }

        @Override
        public boolean matches(BlockState state) {
            if (this.blocks.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) return true;
            for (TagKey<Block> tag : this.tags) {
                if (state.is(tag)) return true;
            }
            return false;
        }

        @Override
        public Lookup find(Level level, BlockPos pos, BlockState state, Player player) {
            if (this.definition.needsFloor() && !BuiltinHidingSpots.solidFloor(level, pos.below())) {
                return Lookup.refused(Component.translatableWithFallback("gui.whycantisleep.hide.no_floor_spot", "There's no floor here to hide on."));
            }
            float yaw = this.yaw(state, player);
            // The offset is given as if the block faced south, and turns with it.
            Vec3 position = Vec3.atBottomCenterOf(pos).add(this.definition.offset().yRot(-yaw * Mth.DEG_TO_RAD));
            // A plain sentence works as well as a translation key.
            Component where = Component.translatableWithFallback(this.definition.where(),
                    this.definition.where().equals("gui.whycantisleep.hiding.where.default") ? "out of sight" : this.definition.where());
            return Lookup.found(new HidingSpot(this.id, pos.immutable(), position, yaw, this.definition.posture(), this.definition.eyeHeight(),
                    this.definition.squint(), this.definition.hideTimeScale(), new ItemStack(state.getBlock().asItem()), where));
        }

        private float yaw(BlockState state, Player player) {
            Direction facing = null;
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            else if (state.hasProperty(BlockStateProperties.FACING) && state.getValue(BlockStateProperties.FACING).getAxis().isHorizontal()) {
                facing = state.getValue(BlockStateProperties.FACING);
            }
            return switch (this.definition.facing()) {
                case PLAYER -> player.getDirection().toYRot();
                case OPPOSITE -> facing == null ? player.getDirection().getOpposite().toYRot() : facing.getOpposite().toYRot();
                case BLOCK -> facing == null ? player.getDirection().toYRot() : facing.toYRot();
            };
        }
    }
}
