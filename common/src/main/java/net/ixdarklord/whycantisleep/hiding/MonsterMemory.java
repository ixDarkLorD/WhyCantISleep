package net.ixdarklord.whycantisleep.hiding;

import net.ixdarklord.coolcatcore.api.event.v2.common.EntityEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerLifecycleEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerTickEvents;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * What monsters remember of the spots players shook them off from. Each time a spot shakes monsters off it is
 * remembered once more; at night, a monster that turns up within {@link #CALL_RANGE} of a remembered spot goes to look
 * there first, and finds whoever hides in it by the search's chance. A remembered spot takes longer to hide in and is
 * searched harder ({@link HidingServer}). A spot is forgotten {@link #FORGET_DAYS} days after it was last used.
 * Kept for the server's session.
 */
public final class MonsterMemory {
    // How far from a remembered spot a monster turning up is drawn to it, how near it must come to look in, how long it
    // tries, and how fast it walks.
    private static final double CALL_RANGE = 24.0;
    private static final double LOOK_REACH = 2.5;
    private static final int GIVE_UP_TICKS = 20 * 30;
    private static final double SPEED = 0.85;
    private static final int FORGET_DAYS = 3;
    // The most times a spot is counted: past it, no worse.
    public static final int MOST = 4;
    private static final Map<ResourceKey<Level>, Map<BlockPos, Memory>> SPOTS = new HashMap<>();
    private static final Map<UUID, Visit> VISITS = new HashMap<>();

    private MonsterMemory() {
    }

    public static void init() {
        EntityEvents.LOAD.register(MonsterMemory::onLoad);
        ServerTickEvents.END_LEVEL.register(MonsterMemory::tick);
        ServerLifecycleEvents.STOPPED.register(server -> {
            SPOTS.clear();
            VISITS.clear();
        });
    }

    /** Remembers that monsters were shaken off from a spot. */
    public static void remember(ServerLevel level, BlockPos anchor) {
        if (!WCISServerConfig.MONSTER_MEMORY.get()) return;
        Memory memory = SPOTS.computeIfAbsent(level.dimension(), key -> new HashMap<>()).computeIfAbsent(anchor.immutable(), pos -> new Memory());
        memory.times = Math.min(MOST, memory.times + 1);
        memory.lastDay = day(level);
    }

    /** How many times monsters were shaken off from a spot and remember it, 0 to {@link #MOST}. */
    public static int times(ServerLevel level, BlockPos anchor) {
        if (!WCISServerConfig.MONSTER_MEMORY.get()) return 0;
        Map<BlockPos, Memory> spots = SPOTS.get(level.dimension());
        Memory memory = spots == null ? null : spots.get(anchor);
        return memory == null || forgotten(level, memory) ? 0 : memory.times;
    }

    private static long day(ServerLevel level) {
        return level.getGameTime() / 24000L;
    }

    private static boolean forgotten(ServerLevel level, Memory memory) {
        return day(level) - memory.lastDay >= FORGET_DAYS;
    }

    // A monster turning up at night near a remembered spot goes to look there: the nearest one it remembers.
    private static void onLoad(Entity entity, ServerLevel level) {
        if (!(entity instanceof Monster monster) || !WCISServerConfig.MONSTER_MEMORY.get() || level.isBrightOutside()) return;
        Map<BlockPos, Memory> spots = SPOTS.get(level.dimension());
        if (spots == null || spots.isEmpty()) return;
        BlockPos nearest = null;
        double best = CALL_RANGE * CALL_RANGE;
        for (Map.Entry<BlockPos, Memory> entry : spots.entrySet()) {
            if (forgotten(level, entry.getValue())) continue;
            double distance = entry.getKey().distToCenterSqr(monster.position());
            if (distance < best) {
                best = distance;
                nearest = entry.getKey();
            }
        }
        if (nearest != null) VISITS.put(monster.getUUID(), new Visit(level.dimension(), nearest));
    }

    private static void tick(ServerLevel level) {
        if (level.getGameTime() % 20 == 0) {
            Map<BlockPos, Memory> spots = SPOTS.get(level.dimension());
            if (spots != null) spots.values().removeIf(memory -> forgotten(level, memory));
        }
        if (VISITS.isEmpty()) return;
        for (Iterator<Map.Entry<UUID, Visit>> it = VISITS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Visit> entry = it.next();
            Visit visit = entry.getValue();
            if (visit.dimension != level.dimension()) continue;
            // Gone, after someone, or too long about it: the visit is over.
            if (!(level.getEntity(entry.getKey()) instanceof Mob mob) || !mob.isAlive() || mob.getTarget() != null || ++visit.ticks > GIVE_UP_TICKS) {
                it.remove();
                continue;
            }
            Vec3 spot = Vec3.atCenterOf(visit.spot);
            if (mob.position().distanceToSqr(spot) > LOOK_REACH * LOOK_REACH) {
                // Kept on its way, or its own wandering would lead it off.
                HidingServer.walk(mob, spot, visit.ticks, SPEED);
                continue;
            }
            // There: it looks the spot over, and whoever hides in it may be found.
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(spot.x, spot.y, spot.z);
            mob.playAmbientSound();
            HidingServer.lookedIn(level, mob, visit.spot);
            it.remove();
        }
    }

    private static final class Memory {
        private int times;
        private long lastDay;
    }

    private static final class Visit {
        private final ResourceKey<Level> dimension;
        private final BlockPos spot;
        private int ticks;

        private Visit(ResourceKey<Level> dimension, BlockPos spot) {
            this.dimension = dimension;
            this.spot = spot;
        }
    }
}
