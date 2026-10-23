package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerLifecycleEvents;
import net.ixdarklord.whycantisleep.api.SleepEvents;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.network.RoomSafePayload;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Clearing the room: when monsters block a sleep, the player has a few seconds to deal with every one of them. If they
 * do, and nothing else has come near the bed meanwhile, the room is safe; still standing by the bed, they lie straight
 * down in it.
 */
public final class RoomClearing {
    // How often a clearing is looked at, and how near the bed the player must be to lie straight down.
    private static final int CHECK_TICKS = 5;
    private static final double BED_REACH = 3.5;

    private static final Map<UUID, Clearing> CLEARING = new HashMap<>();

    private RoomClearing() {
    }

    public static void init() {
        PlayerEvents.END_TICK.register(player -> {
            if (player instanceof ServerPlayer serverPlayer && player.tickCount % CHECK_TICKS == 0) check(serverPlayer);
        });
        PlayerEvents.LEAVE.register(player -> CLEARING.remove(player.getUUID()));
        ServerLifecycleEvents.STOPPED.register(server -> CLEARING.clear());
    }

    /** Monsters just blocked a sleep in this bed: the clock starts on clearing them. */
    public static void start(ServerPlayer player, BlockPos bed, List<Mob> blockers) {
        if (!WCISServerConfig.ROOM_CLEARING.get()) return;
        CLEARING.put(player.getUUID(), new Clearing(bed.immutable(), blockers.stream().map(Entity::getUUID).toList(), player.level().getGameTime()));
    }

    private static void check(ServerPlayer player) {
        Clearing clearing = CLEARING.get(player.getUUID());
        if (clearing == null) return;
        ServerLevel level = player.serverLevel();
        if (level.getGameTime() - clearing.since > WCISServerConfig.CLEARING_TIME.get() * 20L || player.isSleeping()) {
            CLEARING.remove(player.getUUID());
            return;
        }
        for (UUID id : clearing.blockers) {
            Entity blocker = level.getEntity(id);
            if (blocker != null && blocker.isAlive()) return;
        }
        // Every one of them dealt with. The dying still count for the game, so the room waits until they are gone; and
        // it is safe only if nothing else has come in.
        List<Mob> left = SleepChecks.blockers(player, clearing.bed);
        if (left.stream().anyMatch(Mob::isAlive)) {
            CLEARING.remove(player.getUUID());
            return;
        }
        if (!left.isEmpty()) return;
        CLEARING.remove(player.getUUID());
        boolean byTheBed = level.getBlockState(clearing.bed).getBlock() instanceof BedBlock
                && player.position().distanceTo(Vec3.atBottomCenterOf(clearing.bed)) <= BED_REACH;
        boolean lieDown = WCISServerConfig.AUTO_SLEEP.get() && byTheBed;
        SleepEvents.ROOM_SAFE.invoke(new SleepEvents.RoomSafe(player, clearing.bed, clearing.blockers.size()));
        WCISNetwork.sendToPlayer(player, new RoomSafePayload(lieDown));
        if (lieDown) player.startSleepInBed(clearing.bed);
    }

    private record Clearing(BlockPos bed, List<UUID> blockers, long since) {
    }
}
