package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.whycantisleep.between.DarkBetween;
import net.ixdarklord.whycantisleep.between.client.CastleVortex;
import net.ixdarklord.whycantisleep.between.client.DreamRoomView;
import net.ixdarklord.whycantisleep.between.client.DreamTransition;
import net.ixdarklord.whycantisleep.between.client.GardenWardClient;
import net.ixdarklord.whycantisleep.client.SleepClient;
import net.ixdarklord.whycantisleep.client.board.SleepBoard;
import net.ixdarklord.whycantisleep.client.fx.SleepEffects;
import net.ixdarklord.whycantisleep.lurker.LurkerServer;
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
        Network.registerClientbound(ScarePayload.class, (payload, context) -> context.queue(() -> SleepEffects.scare(payload.bed())));
        Network.registerClientbound(SleepBoardPayload.class, (payload, context) -> context.queue(() -> SleepBoard.onBoard(payload)));
        Network.registerClientbound(DreamTransitionPayload.class, (payload, context) -> context.queue(() -> DreamTransition.on(payload)));
        Network.registerClientbound(DreamRoomBlocksPayload.class, (payload, context) -> context.queue(() -> DreamRoomView.onBlocks(payload)));
        Network.registerClientbound(DreamCastlePayload.class, (payload, context) -> context.queue(() -> CastleVortex.on(payload)));
        Network.registerClientbound(WardPayload.class, (payload, context) -> context.queue(() -> GardenWardClient.on(payload)));
        Network.registerClientbound(DreamRoomEntitiesPayload.class, (payload, context) -> context.queue(() -> DreamRoomView.onVisitors(payload)));
        Network.registerServerbound(ScaryPreferencePayload.class, (payload, context) -> context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) LurkerServer.onPreference(player, payload.scary());
        }));
        Network.registerServerbound(DreamReadyPayload.class, (payload, context) -> context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) DarkBetween.release(player);
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
