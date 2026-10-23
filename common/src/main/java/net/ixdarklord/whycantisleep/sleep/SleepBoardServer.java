package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.coolcatcore.api.event.v2.common.BlockEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerLifecycleEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerTickEvents;
import net.ixdarklord.coolcatcore.api.event.v2.core.EventResult;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.network.NudgePayload;
import net.ixdarklord.whycantisleep.network.SleepBoardPayload;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ixdarklord.whycantisleep.sound.WCISSounds;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.level.GameRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The sleep board's server side: while someone is in bed, it tells them once a second who is still awake and why, and
 * how far the dimension is from the {@code playersSleepingPercentage} rule, and how a vote to skip the night stands
 * ({@link SleepVotes}). It also carries out nudges.
 */
public final class SleepBoardServer {
    private static final int UPDATE_TICKS = 20;
    // How long after the last block broken, or the last blow given or taken, a player still counts as busy with it.
    private static final int BUSY_TICKS = 100;
    private static final long AFK_MILLIS = 60_000L;

    // Player to the server tick they last broke a block at, and last nudged someone at.
    private static final Map<UUID, Integer> LAST_MINED = new HashMap<>();
    private static final Map<UUID, Integer> LAST_NUDGE = new HashMap<>();

    private SleepBoardServer() {
    }

    public static void init() {
        BlockEvents.BREAK.register((level, pos, state, player) -> {
            LAST_MINED.put(player.getUUID(), level.getServer().getTickCount());
            return EventResult.PASS;
        });
        ServerTickEvents.END_LEVEL.register(SleepBoardServer::tick);
        PlayerEvents.LEAVE.register(player -> {
            LAST_MINED.remove(player.getUUID());
            LAST_NUDGE.remove(player.getUUID());
        });
        ServerLifecycleEvents.STOPPED.register(server -> {
            LAST_MINED.clear();
            LAST_NUDGE.clear();
        });
    }

    private static void tick(ServerLevel level) {
        MinecraftServer server = level.getServer();
        if (server.getTickCount() % UPDATE_TICKS != 0 || !WCISServerConfig.SLEEP_BOARD.get()) return;
        SleepVotes.tick(level);
        // The panel is for sleeping together: alone (singleplayer, or the only one on a server) there is nobody to show.
        if (server.getPlayerList().getPlayerCount() < 2) return;
        List<ServerPlayer> viewers = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (player.isSleeping() && WCISNetwork.hasMod(player)) viewers.add(player);
        }
        if (viewers.isEmpty()) return;

        // Counted the way the game counts: spectators aside, the players of this dimension.
        int active = 0;
        int sleeping = 0;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            active++;
            if (player.isSleeping()) sleeping++;
        }
        int needed = Math.max(1, Mth.ceil(active * level.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE) / 100.0F));
        List<ServerPlayer> up = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && !player.isSleeping()) up.add(player);
        }

        List<SleepBoardPayload.Entry> awake = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator() || player.isSleeping()) continue;
            boolean counted = player.level() == level;
            awake.add(new SleepBoardPayload.Entry(player.getUUID(), player.getScoreboardName(), reason(player, counted, server.getTickCount()), counted));
        }
        for (ServerPlayer viewer : viewers) {
            WCISNetwork.sendToPlayer(viewer, new SleepBoardPayload(sleeping, needed, awake, SleepVotes.state(level, viewer, sleeping, needed, up)));
        }
    }

    private static AwakeReason reason(ServerPlayer player, boolean sameDimension, int serverTick) {
        if (!sameDimension) return AwakeReason.OTHER_DIMENSION;
        if (recent(player.getLastHurtByMobTimestamp(), player.tickCount) || recent(player.getLastHurtMobTimestamp(), player.tickCount)) return AwakeReason.FIGHTING;
        Integer mined = LAST_MINED.get(player.getUUID());
        if (mined != null && serverTick - mined < BUSY_TICKS) return AwakeReason.MINING;
        if (Util.getMillis() - player.getLastActionTime() > AFK_MILLIS) return AwakeReason.AFK;
        return AwakeReason.AWAKE;
    }

    // The timestamps are the player's own tick count at the blow, and 0 before the first one.
    private static boolean recent(int timestamp, int tickCount) {
        return timestamp > 0 && tickCount - timestamp < BUSY_TICKS;
    }

    /** A player asked to nudge another. Only someone in bed may, at most once per cooldown, and only an awake player. */
    public static void onNudge(ServerPlayer sender, NudgePayload payload) {
        if (!WCISServerConfig.SLEEP_BOARD.get() || !WCISServerConfig.ALLOW_NUDGES.get() || !sender.isSleeping()) return;
        MinecraftServer server = sender.level().getServer();
        ServerPlayer target = server.getPlayerList().getPlayer(payload.target());
        if (target == null || target == sender || target.isSleeping() || target.isSpectator()) return;

        int now = server.getTickCount();
        Integer last = LAST_NUDGE.get(sender.getUUID());
        int wait = last == null ? 0 : WCISServerConfig.NUDGE_COOLDOWN.get() * 20 - (now - last);
        if (wait > 0) {
            sender.sendSystemMessage(Component.translatableWithFallback("gui.whycantisleep.nudge.cooldown", "You can nudge again in %s seconds", Mth.ceil(wait / 20.0F)), true);
            return;
        }
        LAST_NUDGE.put(sender.getUUID(), now);

        // The message carries its English text as well as its key, so it reads right whatever the target's language files hold.
        target.connection.send(new ClientboundSoundPacket(Holder.direct(WCISSounds.NUDGE), SoundSource.PLAYERS,
                target.getX(), target.getY(), target.getZ(), 1.0F, 1.0F, target.getRandom().nextLong()));
        target.sendSystemMessage(Component.translatableWithFallback("gui.whycantisleep.nudge.received", "%s is in bed and waiting for you to sleep", sender.getDisplayName()));
        sender.sendSystemMessage(Component.translatableWithFallback("gui.whycantisleep.nudge.sent", "You nudged %s", target.getDisplayName()), true);
    }
}
