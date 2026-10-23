package net.ixdarklord.whycantisleep.sleep;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * What a bed's room is like, for how well its sleeper sleeps: whether it is closed (walled and roofed, its doors shut),
 * whether it is lit all through (no dark corner a monster could spawn in), how bright it is at the pillow, what is placed
 * round it that soothes or unsettles ({@link SleepDecor}), and what music plays near it.
 */
public record Bedroom(boolean enclosed, boolean lit, int pillowLight, SleepDecor.Count decor) {
    // 1.20.1 has no jukebox songs: the tags are of the music disc items instead.
    /** Discs that keep a sleeper up: the eerie ones (11, 13, ward, 5, mellohi) and any a pack adds. */
    public static final TagKey<Item> CREEPY = TagKey.create(Registries.ITEM, WhyCantISleep.id("creepy"));
    /** Discs that lull a sleeper off. */
    public static final TagKey<Item> CALM = TagKey.create(Registries.ITEM, WhyCantISleep.id("calm"));
    /** Discs too loud and lively to sleep through (Pigstep, Otherside). */
    public static final TagKey<Item> LOUD = TagKey.create(Registries.ITEM, WhyCantISleep.id("loud"));
    // A room is the air round the bed reached without passing walls or shut doors; one bigger than ROOM_LIMIT blocks,
    // or reaching further than ROOM_RADIUS, or under the open sky, isn't closed.
    private static final int ROOM_LIMIT = 600;
    private static final int ROOM_RADIUS = 12;
    // How far a jukebox is heard from the bed.
    public static final double MUSIC_RANGE = 16.0;

    /** Music heard at a bed, from no matter to the worst to sleep through. */
    public enum Music {
        NONE, OTHER, CALM, LOUD, CREEPY
    }

    /** Looks over the room round a bed. */
    public static Bedroom of(ServerLevel level, BlockPos head) {
        BlockPos start = head.above();
        LongSet seen = new LongOpenHashSet();
        Deque<BlockPos> open = new ArrayDeque<>();
        seen.add(start.asLong());
        open.add(start);
        boolean enclosed = passable(level, start, level.getBlockState(start));
        boolean lit = true;
        while (enclosed && !open.isEmpty()) {
            BlockPos at = open.poll();
            if (level.canSeeSky(at) || seen.size() > ROOM_LIMIT || at.distManhattan(start) > ROOM_RADIUS * 2) {
                enclosed = false;
                break;
            }
            // A monster spawns where no block lights it.
            if (level.getBrightness(LightLayer.BLOCK, at) == 0) lit = false;
            for (Direction side : Direction.values()) {
                BlockPos next = at.relative(side);
                if (seen.add(next.asLong()) && passable(level, next, level.getBlockState(next))) open.add(next);
            }
        }
        return new Bedroom(enclosed, enclosed && lit, level.getBrightness(LightLayer.BLOCK, start), SleepDecor.count(level, head));
    }

    // Air, and what lets air through: anything without a shape to bump into, and doors, trapdoors and gates left open.
    private static boolean passable(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.hasProperty(BlockStateProperties.OPEN)) return state.getValue(BlockStateProperties.OPEN);
        return state.getCollisionShape(level, pos).isEmpty();
    }

    /** The music playing near a bed, the worst of it to sleep through: creepy, then loud, then calm, then anything else. */
    public static Music music(ServerLevel level, BlockPos bed) {
        Music heard = Music.NONE;
        int chunkX = bed.getX() >> 4;
        int chunkZ = bed.getZ() >> 4;
        int reach = ((int) MUSIC_RANGE >> 4) + 1;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX + dx, chunkZ + dz);
                if (chunk == null) continue;
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (!(entity instanceof JukeboxBlockEntity jukebox) || !jukebox.isRecordPlaying()) continue;
                    if (jukebox.getBlockPos().distSqr(bed) > MUSIC_RANGE * MUSIC_RANGE) continue;
                    ItemStack disc = jukebox.getFirstItem();
                    Music music = disc.is(CREEPY) ? Music.CREEPY : disc.is(LOUD) ? Music.LOUD : disc.is(CALM) ? Music.CALM : Music.OTHER;
                    if (music.ordinal() > heard.ordinal()) heard = music;
                }
            }
        }
        return heard;
    }
}
