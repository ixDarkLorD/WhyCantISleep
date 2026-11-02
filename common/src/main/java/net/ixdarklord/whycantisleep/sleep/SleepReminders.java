package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerLifecycleEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerTickEvents;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.network.ReminderPayload;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reminders to sleep. At dusk, a player who has gone the server's {@code reminderDays} or more without lying down is
 * reminded, with the days. At night, once some of the players are in bed and have waited {@code reminderWait} seconds for the rest, each player still up is
 * told, once a night: an automatic nudge, on top of the Bedtime panel's own.
 */
public final class SleepReminders {
    private static final int CHECK_TICKS = 20;
    private static final int DAY = 24000;
    // The time of day beds can first be slept in, on a clear night: dusk.
    private static final int DUSK = 12542;

    private static final Map<ResourceKey<Level>, Long> LAST_TIME = new HashMap<>();
    // Since when (game time) some of a level's players have been in bed, short of the night's needs.
    private static final Map<ResourceKey<Level>, Long> WAITING_SINCE = new HashMap<>();
    // The night (in days) each player was last told about the others in bed.
    private static final Map<UUID, Long> TOLD = new HashMap<>();

    private SleepReminders() {
    }

    public static void init() {
        ServerTickEvents.END_LEVEL.register(SleepReminders::tick);
        PlayerEvents.LEAVE.register(player -> TOLD.remove(player.getUUID()));
        ServerLifecycleEvents.STOPPED.register(server -> {
            LAST_TIME.clear();
            WAITING_SINCE.clear();
            TOLD.clear();
        });
    }

    private static void tick(ServerLevel level) {
        if (level.getServer().getTickCount() % CHECK_TICKS != 0 || !WCISServerConfig.SLEEP_REMINDERS.get()) return;
        long time = level.getDayTime() % DAY;
        Long before = LAST_TIME.put(level.dimension(), time);
        if (before != null && before < DUSK && time >= DUSK) dusk(level);
        othersInBed(level);
    }

    // Night is falling: those who haven't slept in a day or more are reminded.
    private static void dusk(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (player.isSleeping() || player.isSpectator()) continue;
            int days = SleepTroubles.get(player).ticksSinceRest() / DAY;
            if (days >= WCISServerConfig.REMINDER_DAYS.get()) WCISNetwork.sendToPlayer(player, new ReminderPayload(ReminderPayload.Kind.DUSK, days, 0, 0));
        }
    }

    // Some are in bed, not yet enough to skip the night, and have waited a while: those still up are told, once a night.
    private static void othersInBed(ServerLevel level) {
        int active = 0;
        int sleeping = 0;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            active++;
            if (player.isSleeping()) sleeping++;
        }
        int needed = Math.max(1, Mth.ceil(active * level.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE) / 100.0F));
        if (active < 2 || sleeping == 0 || sleeping >= needed || level.isDay()) {
            WAITING_SINCE.remove(level.dimension());
            return;
        }
        long now = level.getGameTime();
        long since = WAITING_SINCE.computeIfAbsent(level.dimension(), key -> now);
        if (now - since < WCISServerConfig.REMINDER_WAIT.get() * 20L) return;
        long night = level.getDayTime() / DAY;
        for (ServerPlayer player : level.players()) {
            if (player.isSleeping() || player.isSpectator()) continue;
            Long told = TOLD.put(player.getUUID(), night);
            if (told != null && told == night) continue;
            WCISNetwork.sendToPlayer(player, new ReminderPayload(ReminderPayload.Kind.OTHERS_IN_BED, 0, sleeping, needed));
        }
    }
}
