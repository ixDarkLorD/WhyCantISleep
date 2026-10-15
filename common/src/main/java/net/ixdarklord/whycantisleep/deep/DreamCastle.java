package net.ixdarklord.whycantisleep.deep;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.network.DreamCastlePayload;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Lord's castle: one in the whole of the Lucid Deep ({@code worldgen/structure/dream_castle.json}, placed by a
 * single concentric ring, a few hundred blocks from the arrival island). Over its roof turns a storm, drawn by each
 * dreamer's client ({@code CastleVortex}); this finds where, and tells each dreamer once they are in the Deep.
 * <p>
 * Finding it means knowing the ring's chunk and generating that chunk as far as its structure starts (cheap: no
 * terrain), then turning the beam's place in the template ({@link #MARKER}) by however the castle was rotated.
 */
public final class DreamCastle {
    private static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, WhyCantISleep.id("dream_castle"));
    private static final ResourceKey<StructureSet> SET = ResourceKey.create(Registries.STRUCTURE_SET, WhyCantISleep.id("dream_castle"));
    // The roof dais's beam source inside the template, from tools/deep_structure_sizes.json.
    private static final BlockPos MARKER = new BlockPos(31, 60, 26);

    // Where the castle's beam rises from, once found; the level it was found in, so another world finds its own.
    @Nullable
    private static ServerLevel foundIn;
    @Nullable
    private static Vec3 found;
    // Dreamers who have been told where it is since they last came in.
    private static final Set<UUID> TOLD = new HashSet<>();

    private DreamCastle() {
    }

    /** Tells a dreamer in the Deep where the castle is, if they haven't been told since they came in. */
    public static void tell(ServerPlayer player, ServerLevel deep) {
        if (!TOLD.add(player.getUUID())) return;
        Vec3 at = locate(deep);
        WCISNetwork.sendToPlayer(player, at == null ? new DreamCastlePayload(false, 0.0, 0.0, 0.0) : new DreamCastlePayload(true, at.x, at.y, at.z));
    }

    /** A dreamer left the Deep: they will be told again when they come back. */
    public static void forget(ServerPlayer player) {
        TOLD.remove(player.getUUID());
    }

    @Nullable
    public static Vec3 locate(ServerLevel deep) {
        if (foundIn == deep) return found;
        foundIn = deep;
        found = null;
        StructureSet set = deep.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getValue(SET);
        Structure structure = deep.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(STRUCTURE);
        if (set == null || structure == null || !(set.placement() instanceof ConcentricRingsStructurePlacement rings)) return null;
        List<ChunkPos> chunks = deep.getChunkSource().getGeneratorState().getRingPositionsFor(rings);
        if (chunks == null || chunks.isEmpty()) return null;
        ChunkPos chunk = chunks.getFirst();
        ChunkAccess access = deep.getChunkSource().getChunk(chunk.x(), chunk.z(), ChunkStatus.STRUCTURE_STARTS, true);
        if (access == null) return null;
        StructureStart start = access.getStartForStructure(structure);
        if (start == null || !start.isValid() || !(start.getPieces().getFirst() instanceof PoolElementStructurePiece piece)) return null;
        BlockPos beam = StructureTemplate.transform(MARKER, Mirror.NONE, piece.getRotation(), BlockPos.ZERO).offset(piece.getPosition());
        found = Vec3.atBottomCenterOf(beam);
        WhyCantISleep.LOGGER.debug("The Lord's castle is at {}", beam);
        return found;
    }
}
