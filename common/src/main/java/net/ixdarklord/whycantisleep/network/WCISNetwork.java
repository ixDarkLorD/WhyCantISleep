package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.whycantisleep.client.SleepClient;
import net.ixdarklord.whycantisleep.client.board.SleepBoard;
import net.ixdarklord.whycantisleep.sleep.SleepBoardServer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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
