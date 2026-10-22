package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerLifecycleEvents;
import net.ixdarklord.whycantisleep.api.SleepEvents;
import net.ixdarklord.whycantisleep.api.SleepTrouble;
import net.ixdarklord.whycantisleep.network.TroublePayload;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps count of each player's refused sleeps since they last lay down, and says when their {@link SleepTrouble}
 * changes level. The count lives for the server's session (a player who rejoins keeps it, a restart clears it); the
 * time since rest is the game's own statistic, which is saved.
 */
public final class SleepTroubles {
    // How often each player's level is looked at, for the time since rest, which grows on its own.
    private static final int CHECK_TICKS = 100;

    private static final Map<UUID, Integer> FAILED = new HashMap<>();
    private static final Map<UUID, SleepTrouble.Level> LEVEL = new HashMap<>();

    private SleepTroubles() {
    }

    public static void init() {
        PlayerEvents.END_TICK.register(player -> {
            if (player instanceof ServerPlayer serverPlayer && player.tickCount % CHECK_TICKS == 0) check(serverPlayer);
        });
        ServerLifecycleEvents.STOPPED.register(server -> {
            FAILED.clear();
            LEVEL.clear();
        });
    }

    public static SleepTrouble get(ServerPlayer player) {
        return new SleepTrouble(FAILED.getOrDefault(player.getUUID(), 0), player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)));
    }

    /** A sleep was refused: one more in a row. */
    public static SleepTrouble onFailed(ServerPlayer player) {
        FAILED.merge(player.getUUID(), 1, Integer::sum);
        check(player);
        return get(player);
    }

    /** The player lay down and slept: it starts over (the game resets the time since rest itself). */
    public static void onLayDown(ServerPlayer player) {
        FAILED.remove(player.getUUID());
        SleepEvents.LAY_DOWN.invoke(player);
        check(player);
    }

    private static void check(ServerPlayer player) {
        SleepTrouble trouble = get(player);
        // The client's moon of sleep debt follows the time since rest.
        WCISNetwork.sendToPlayer(player, new TroublePayload(trouble.ticksSinceRest(), trouble.failedAttempts()));
        SleepTrouble.Level now = trouble.level();
        SleepTrouble.Level before = LEVEL.put(player.getUUID(), now);
        if (before != null && before != now) SleepEvents.TROUBLE_CHANGED.invoke(new SleepEvents.TroubleChanged(player, before, now, trouble));
    }
}
