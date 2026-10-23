package net.ixdarklord.whycantisleep.api;

import net.ixdarklord.whycantisleep.hiding.DataHidingSpots;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Every kind of hiding spot: the built-in ones (beds, tables, double chests, closets), those registered by mods, and
 * those described by data packs. The first that {@linkplain HidingSpotType#matches matches} a block gets to answer for
 * it; registered types are asked before data pack ones, in the order they were registered.
 * <p>
 * A data pack describes a spot with a file at {@code data/<namespace>/whycantisleep/hiding_spot/<name>.json}:
 * <pre>{@code
 * {
 *   "blocks": ["mymod:wardrobe", "#mymod:cupboards"],  // block ids and #tags
 *   "posture": "standing",                              // lying, standing or tucked
 *   "offset": [0.0, 0.0, 0.0],                           // from the block's bottom centre, turned with its facing
 *   "eye_height": 1.5,                                   // optional; else the posture's
 *   "squint": 0.5,                                       // how far the eyes close, 0 open to 1 shut; 0.5 by default
 *   "needs_floor": true,                                 // a solid block under it
 *   "hide_time_scale": 0.8,                              // optional, 1 by default
 *   "facing": "block",                                   // block (its facing property), opposite, or player
 *   "where": "mymod.hiding.wardrobe"                     // translation key for "Hiding <where>"
 * }
 * }</pre>
 * Data pack spots are known to the server only; a client predicts nothing for them and lets the server decide.
 */
public final class HidingSpots {
    private static final List<HidingSpotType> TYPES = new ArrayList<>();

    private HidingSpots() {
    }

    /** Adds a kind of hiding spot. Call while the mod initialises, on both sides. */
    public static void register(HidingSpotType type) {
        TYPES.add(type);
    }

    /** The registered types (not data pack ones), in order. */
    public static List<HidingSpotType> registered() {
        return Collections.unmodifiableList(TYPES);
    }

    /** The type with this id, registered or from a data pack, or null. */
    public static @Nullable HidingSpotType get(ResourceLocation id) {
        for (HidingSpotType type : TYPES) {
            if (type.id().equals(id)) return type;
        }
        return DataHidingSpots.get(id);
    }

    /** The type that answers for a block, registered or from a data pack (on the server), or null. */
    public static @Nullable HidingSpotType typeFor(BlockState state) {
        for (HidingSpotType type : TYPES) {
            if (type.matches(state)) return type;
        }
        return DataHidingSpots.typeFor(state);
    }

    /** What a sneak-use of a block finds: a spot, a refusal, or {@link HidingSpotType.Lookup#NONE}. Server side. */
    public static HidingSpotType.Lookup find(Level level, BlockPos pos, BlockState state, Player player) {
        HidingSpotType type = typeFor(state);
        return type == null ? HidingSpotType.Lookup.NONE : type.find(level, pos, state, player);
    }
}
