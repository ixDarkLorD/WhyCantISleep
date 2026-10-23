package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.whycantisleep.client.HidingClient;
import net.ixdarklord.whycantisleep.client.NightTerrorClient;
import net.ixdarklord.whycantisleep.client.SleepMornings;
import net.ixdarklord.whycantisleep.client.SleepStatus;
import net.ixdarklord.whycantisleep.client.SleepClient;
import net.ixdarklord.whycantisleep.client.board.SleepBoard;
import net.ixdarklord.whycantisleep.hiding.HidingServer;
import net.ixdarklord.whycantisleep.sleep.SleepBoardServer;
import net.ixdarklord.whycantisleep.sleep.SleepVotes;
import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * The mod's payloads, registered from the common constructor on both sides. Clientbound handlers are lambdas into
 * {@code client} package classes, so a dedicated server never loads them.
 */
public final class WCISNetwork {
    private WCISNetwork() {
    }

    public static void register() {
        Network.registerClientbound(SleepBlockedPayload.class, (payload, context) -> context.queue(() -> SleepClient.onBlockedByServer(payload)));
        Network.registerClientbound(SleepBoardPayload.class, (payload, context) -> context.queue(() -> SleepBoard.onBoard(payload)));
        Network.registerClientbound(HidingPayload.class, (payload, context) -> context.queue(() -> HidingClient.on(payload)));
        Network.registerClientbound(WokePayload.class, (payload, context) -> context.queue(() -> SleepMornings.onWoke(payload)));
        Network.registerClientbound(TerrorPayload.class, (payload, context) -> context.queue(() -> NightTerrorClient.on(payload)));
        Network.registerClientbound(TroublePayload.class, (payload, context) -> context.queue(() -> SleepStatus.onTrouble(payload)));
        Network.registerClientbound(RoomSafePayload.class, (payload, context) -> context.queue(() -> SleepClient.onRoomSafe(payload)));
        Network.registerServerbound(LeaveHidingPayload.class, (payload, context) -> context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) HidingServer.onLeaveRequest(player);
        }));
        Network.registerServerbound(NoisePayload.class, (payload, context) -> context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) HidingServer.onNoise(player, payload.noise());
        }));
        Network.registerServerbound(VotePayload.class, (payload, context) -> context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player && payload.skip()) SleepVotes.onVote(player);
        }));
        Network.registerServerbound(NudgePayload.class, (payload, context) -> context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) SleepBoardServer.onNudge(player, payload);
        }));
    }

    /** Whether the player's client has the mod: it accepts the mod's payloads. */
    public static boolean hasMod(ServerPlayer player) {
        return Network.canPlayerReceive(player, SleepBlockedPayload.TYPE);
    }

    /** Sends to a player whose client has the mod; does nothing for one without it. */
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (Network.canPlayerReceive(player, payload.type())) Network.sendToPlayer(player, payload);
    }
}
