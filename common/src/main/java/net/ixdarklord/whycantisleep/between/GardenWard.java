package net.ixdarklord.whycantisleep.between;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.ixdarklord.coolcatcore.api.event.v2.common.BlockEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerLifecycleEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerTickEvents;
import net.ixdarklord.coolcatcore.api.event.v2.core.EventResult;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.ixdarklord.whycantisleep.network.WardPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Garden's ward: while its Gardener lives, the Garden of Despair holds itself together. No player (but one in
 * creative) can break a block of it, nor can any explosion; what a player put there themselves they can take away
 * again. Nor will it be skipped: whoever climbs, builds, flies or pearls up over the hedges of its maze is dragged
 * back down by the thorns to where they last stood in its ways. The Gardener's death lifts it, for that garden only,
 * for good ({@link Cleared}).
 * <p>
 * The ward holds across each garden's structure (its pieces: the island, the walls, the plaza, the cells); the climb
 * is watched over the maze, the 155 blocks square inside the walls, all but the open plaza round the tree.
 */
public final class GardenWard {
    private static final ResourceKey<Structure> GARDEN = ResourceKey.create(Registries.STRUCTURE, WhyCantISleep.id("garden_of_despair"));
    /** Half the maze across, from the plaza's middle to the inside of the walls (gen_between_structures: 17 cells of 9). */
    private static final int MAZE_HALF = 76;
    /** The plaza's radius: inside it the ground is open, and nothing is watched. */
    private static final int PLAZA = 26;
    /** The hedges stand 7 high; feet this high over the floor are on them, or over them. */
    private static final int OVER = 6;
    /** How far from where they last stood a player may land in the maze before it is a pearl, not a step. */
    private static final double LEAP = 10.0;
    /** The south gate's corridor, from the plaza's middle: where one who came in over the walls is put. */
    private static final int GATE = 72;

    private static final Map<UUID, BlockPos> LAST_SAFE = new HashMap<>();
    private static final Map<UUID, Long> LAST_WARNED = new HashMap<>();

    private GardenWard() {
    }

    public static void init() {
        BlockEvents.BREAK.register((level, pos, state, player) -> {
            if (refuses(player, pos)) return EventResult.INTERRUPT;
            if (DarkBetween.isBetween(level)) cleared(level).placed.remove(pos.asLong());
            return EventResult.PASS;
        });
        BlockEvents.PLACED.register((level, pos, state, placer) -> {
            if (level instanceof ServerLevel server && placer instanceof Player && gardenAt(server, pos) != null) {
                Cleared data = cleared(server);
                if (data.placed.add(pos.asLong())) data.setDirty();
            }
        });
        ServerTickEvents.END_LEVEL.register(GardenWard::tick);
        PlayerEvents.LEAVE.register(player -> {
            LAST_SAFE.remove(player.getUUID());
            LAST_WARNED.remove(player.getUUID());
        });
        ServerLifecycleEvents.STOPPED.register(server -> {
            LAST_SAFE.clear();
            LAST_WARNED.clear();
        });
    }

    /** Whether the ward refuses this player breaking the block at pos. */
    public static boolean refuses(ServerPlayer player, BlockPos pos) {
        if (player.getAbilities().instabuild || player.isSpectator()) return false;
        return warded(player.level(), pos);
    }

    /**
     * The ward refuses a player's start at breaking a block (ServerPlayerGameModeMixin): the block glints and holds
     * (WardPayload), and now and then they are told why.
     */
    public static void struck(ServerPlayer player, BlockPos pos, Direction face) {
        WCISNetwork.sendToPlayer(player, new WardPayload(pos, face));
        long now = player.level().getGameTime();
        Long last = LAST_WARNED.get(player.getUUID());
        if (last == null || now - last > 100) {
            LAST_WARNED.put(player.getUUID(), now);
            player.sendOverlayMessage(Component.translatable("message.whycantisleep.garden.warded"));
        }
    }

    /** Whether the block at pos is held by an unbroken ward (a garden's, whose Gardener lives, not put there by a player). */
    public static boolean warded(ServerLevel level, BlockPos pos) {
        if (!DarkBetween.isBetween(level)) return false;
        StructureStart start = gardenAt(level, pos);
        return start != null && level.structureManager().structureHasPieceAt(pos, start) && !cleared(level).placed.contains(pos.asLong());
    }

    /** An explosion breaks none of what the ward holds (ServerExplosionMixin). */
    public static List<BlockPos> spare(ServerLevel level, List<BlockPos> positions) {
        if (!DarkBetween.isBetween(level) || positions.isEmpty()) return positions;
        return positions.stream().filter(pos -> !warded(level, pos)).toList();
    }

    /** The Gardener is dead: its garden's ward is lifted, for good. */
    public static void lift(ServerLevel level, BlockPos home) {
        StructureStart start = level.structureManager().getStructureAt(home, structure(level));
        if (!start.isValid()) return;
        Cleared data = cleared(level);
        if (!data.gardens.add(start.getChunkPos().pack())) return;
        data.setDirty();
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().closerThan(home, 128)) {
                player.sendOverlayMessage(Component.translatable("message.whycantisleep.garden.lifted"));
                level.playSound(null, player.blockPosition(), SoundEvents.CREAKING_HEART_HURT, SoundSource.BLOCKS, 1.5F, 0.5F);
            }
        }
    }

    // The garden (its start) whose ward still holds round pos, or null.
    static @Nullable StructureStart gardenAt(ServerLevel level, BlockPos pos) {
        if (!DarkBetween.isBetween(level)) return null;
        StructureStart start = level.structureManager().getStructureAt(pos, structure(level));
        if (!start.isValid() || cleared(level).gardens.contains(start.getChunkPos().pack())) return null;
        return start;
    }

    private static Structure structure(ServerLevel level) {
        return level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(GARDEN);
    }

    // The plaza piece (garden/plaza): its middle is the garden's, its foot a block over the maze's floor.
    private static @Nullable BoundingBox plaza(StructureStart start) {
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof PoolElementStructurePiece pool && pool.getElement().toString().contains("garden/plaza")) return piece.getBoundingBox();
        }
        return null;
    }

    // Each tick, each player in the Dark Between: over the maze and up on (or over) its hedges, or carried through it further
    // than a step, and they are dragged back down to where they last stood in it.
    private static void tick(ServerLevel level) {
        if (!DarkBetween.isBetween(level)) return;
        for (ServerPlayer player : level.players()) {
            if (player.getAbilities().instabuild || player.isSpectator() || !player.isAlive()) {
                LAST_SAFE.remove(player.getUUID());
                continue;
            }
            StructureStart start = gardenAt(level, player.blockPosition());
            BoundingBox plaza = start == null ? null : plaza(start);
            if (plaza == null) {
                LAST_SAFE.remove(player.getUUID());
                continue;
            }
            BlockPos middle = plaza.getCenter();
            int floor = plaza.minY() - 1;
            double dx = player.getX() - (middle.getX() + 0.5), dz = player.getZ() - (middle.getZ() + 0.5);
            if (Math.abs(dx) > MAZE_HALF || Math.abs(dz) > MAZE_HALF || dx * dx + dz * dz < PLAZA * PLAZA) {
                // Outside the maze (on the walls, beyond them, in the plaza): free; but the plaza is a fair place to return to.
                if (dx * dx + dz * dz < PLAZA * PLAZA && player.onGround()) LAST_SAFE.put(player.getUUID(), player.blockPosition());
                else if (Math.abs(dx) > MAZE_HALF || Math.abs(dz) > MAZE_HALF) LAST_SAFE.remove(player.getUUID());
                continue;
            }
            BlockPos safe = LAST_SAFE.get(player.getUUID());
            boolean over = player.getY() >= floor + 1 + OVER;
            // Landed far from where they last stood, a tick ago: a pearl, chorus fruit, or the like, through the hedges.
            boolean leapt = safe != null && player.onGround() && player.position().distanceTo(Vec3.atBottomCenterOf(safe)) > LEAP;
            if (over || leapt) {
                BlockPos back = safe != null ? safe : new BlockPos(middle.getX(), floor + 1, middle.getZ() + GATE);
                dragBack(level, player, back);
            } else if (player.onGround() && player.getY() < floor + 4) {
                LAST_SAFE.put(player.getUUID(), player.blockPosition());
            }
        }
    }

    // The thorns take them: a lash, a sound of brambles, and they are back where they were, slowed a moment.
    private static void dragBack(ServerLevel level, ServerPlayer player, BlockPos to) {
        Vec3 from = player.position();
        level.sendParticles(ParticleTypes.CRIMSON_SPORE, from.x, from.y + 1.0, from.z, 30, 0.4, 0.8, 0.4, 0.02);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.SWEET_BERRY_BUSH_BREAK, SoundSource.HOSTILE, 1.2F, 0.6F);
        player.teleportTo(level, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, Set.<Relative>of(), player.getYRot(), player.getXRot(), true);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.hurtServer(level, level.damageSources().sweetBerryBush(), 2.0F);
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, false, true));
        level.sendParticles(ParticleTypes.CRIMSON_SPORE, to.getX() + 0.5, to.getY() + 1.0, to.getZ() + 0.5, 20, 0.4, 0.8, 0.4, 0.02);
        level.playSound(null, to, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.2F, 0.5F);
        player.sendOverlayMessage(Component.translatable("message.whycantisleep.garden.dragged"));
        LAST_SAFE.put(player.getUUID(), to);
    }

    private static Cleared cleared(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(Cleared.TYPE);
    }

    /** What the ward remembers: the gardens whose Gardener is dead (their starts' chunks), and the blocks players put in warded gardens. */
    public static final class Cleared extends SavedData {
        public static final Codec<Cleared> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.listOf().fieldOf("gardens").forGetter(data -> List.copyOf(data.gardens)),
                Codec.LONG.listOf().fieldOf("placed").forGetter(data -> List.copyOf(data.placed))
        ).apply(instance, Cleared::new));
        public static final SavedDataType<Cleared> TYPE = new SavedDataType<>(WhyCantISleep.id("garden_ward"), Cleared::new, CODEC, null);

        private final LongSet gardens;
        private final LongSet placed;

        public Cleared() {
            this(List.of(), List.of());
        }

        private Cleared(List<Long> gardens, List<Long> placed) {
            this.gardens = new LongOpenHashSet(gardens);
            this.placed = new LongOpenHashSet(placed);
        }
    }
}
