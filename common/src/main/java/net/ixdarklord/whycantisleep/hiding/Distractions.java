package net.ixdarklord.whycantisleep.hiding;

import net.ixdarklord.coolcatcore.api.event.v2.common.EntityEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerLifecycleEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerTickEvents;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Distractions: a snowball or an egg thrown, or an arrow shot, by a hiding player leaves their spot by its mouth (it would
 * only hit the bed or the door), and where it lands makes a noise: the monsters about go to look there, and the hide time
 * is cut short ({@link HidingServer#distract}).
 */
public final class Distractions {
    // How long a thrown thing is followed at most, and how slow an arrow is when it has come to rest.
    private static final int FOLLOW_TICKS = 20 * 6;
    private static final double AT_REST = 1.0E-3;
    private static final Map<UUID, Flight> FLYING = new HashMap<>();
    // Monsters drawn to where something landed, kept walking there (their own wandering would lead them off) until they
    // are there, are after someone, or LURE_TICKS have passed; once there they stand a moment and look about.
    private static final int LURE_TICKS = 20 * 12;
    private static final double LURE_REACH = 2.0;
    private static final Map<UUID, Lure> LURED = new HashMap<>();

    private Distractions() {
    }

    public static void init() {
        EntityEvents.LOAD.register(Distractions::onLoad);
        ServerTickEvents.END_LEVEL.register(Distractions::tick);
        ServerLifecycleEvents.STOPPED.register(server -> {
            FLYING.clear();
            LURED.clear();
        });
    }

    /** Whether something is a distraction a hiding player may throw or shoot. */
    public static boolean isDistraction(Entity entity) {
        return entity instanceof Snowball || entity instanceof ThrownEgg || entity instanceof AbstractArrow;
    }

    private static void onLoad(Entity entity, ServerLevel level) {
        if (!WCISServerConfig.DISTRACTIONS.get() || !(entity instanceof Projectile projectile) || !isDistraction(entity)) return;
        if (!(projectile.getOwner() instanceof ServerPlayer player) || !HidingServer.isHiding(player) || entity.tickCount > 0) return;
        // Out by the spot's mouth, flying on as it was thrown.
        Vec3 mouth = HidingServer.mouth(player);
        if (mouth != null) entity.setPos(mouth.x, mouth.y, mouth.z);
        FLYING.put(entity.getUUID(), new Flight(player.getUUID(), level, entity.position()));
    }

    /** Sends a monster to look where something landed, and keeps it on its way. */
    static void lure(Mob mob, Vec3 to, double speed) {
        LURED.put(mob.getUUID(), new Lure((ServerLevel) mob.level(), to, speed));
        mob.getNavigation().moveTo(to.x, to.y, to.z, speed);
    }

    private static void tick(ServerLevel level) {
        if (!LURED.isEmpty()) walkLured(level);
        if (FLYING.isEmpty()) return;
        for (Iterator<Map.Entry<UUID, Flight>> it = FLYING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Flight> entry = it.next();
            Flight flight = entry.getValue();
            if (flight.level != level) continue;
            Entity entity = level.getEntity(entry.getKey());
            boolean landed;
            if (entity == null || !entity.isAlive()) {
                // A snowball or egg breaks where it hits: the last place it was seen.
                landed = true;
            } else {
                flight.last = entity.position();
                landed = entity.getDeltaMovement().lengthSqr() < AT_REST || ++flight.ticks > FOLLOW_TICKS;
            }
            if (!landed) continue;
            it.remove();
            if (level.getPlayerByUUID(flight.thrower) instanceof ServerPlayer player) HidingServer.distract(player, flight.last);
        }
    }

    private static void walkLured(ServerLevel level) {
        for (Iterator<Map.Entry<UUID, Lure>> it = LURED.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Lure> entry = it.next();
            Lure lure = entry.getValue();
            if (lure.level != level) continue;
            if (!(level.getEntity(entry.getKey()) instanceof Mob mob) || !mob.isAlive() || mob.getTarget() != null || ++lure.ticks > LURE_TICKS) {
                it.remove();
                continue;
            }
            if (mob.position().distanceToSqr(lure.to) < LURE_REACH * LURE_REACH) {
                mob.getNavigation().stop();
                mob.getLookControl().setLookAt(lure.to.x, lure.to.y, lure.to.z);
                it.remove();
                continue;
            }
            HidingServer.walk(mob, lure.to, lure.ticks, lure.speed);
        }
    }

    private static final class Lure {
        private final ServerLevel level;
        private final Vec3 to;
        private final double speed;
        private int ticks;

        private Lure(ServerLevel level, Vec3 to, double speed) {
            this.level = level;
            this.to = to;
            this.speed = speed;
        }
    }

    private static final class Flight {
        private final UUID thrower;
        private final ServerLevel level;
        private Vec3 last;
        private int ticks;

        private Flight(UUID thrower, ServerLevel level, Vec3 from) {
            this.thrower = thrower;
            this.level = level;
            this.last = from;
        }
    }
}
