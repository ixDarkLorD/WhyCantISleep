package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerLifecycleEvents;
import net.ixdarklord.whycantisleep.api.SleepQuality;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.network.BedCheckPayload;
import net.ixdarklord.whycantisleep.network.BedCheckRequestPayload;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.ixdarklord.whycantisleep.network.WokePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The bed check: a player crouched by a bed, looking at it, is told how a night in it would go before they lie down
 * ({@link SleepNights#forecast}): the score it would have, what makes it so, where in a closed room a monster could
 * spawn, and the chance of a night terror. The client asks; the server looks the room over and answers.
 */
public final class BedCheck {
    // How near the bed the player must be (as for using it, with room to spare), and how often one player may ask:
    // looking a room over is a flood fill of up to a few hundred blocks.
    private static final double REACH = 6.0;
    private static final int COOLDOWN_TICKS = 20;

    private static final Map<UUID, Long> LAST = new HashMap<>();

    private BedCheck() {
    }

    public static void init() {
        PlayerEvents.LEAVE.register(player -> LAST.remove(player.getUUID()));
        ServerLifecycleEvents.STOPPED.register(server -> LAST.clear());
    }

    /** The client asks how a night in this bed would go. */
    public static void onRequest(ServerPlayer player, BedCheckRequestPayload payload) {
        if (!WCISServerConfig.BED_CHECK.get() || player.isSleeping() || player.isSpectator()) return;
        ServerLevel level = player.level();
        long now = level.getGameTime();
        Long last = LAST.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_TICKS) return;
        BlockPos pos = payload.bed();
        if (!level.isLoaded(pos) || player.getEyePosition().distanceTo(Vec3.atCenterOf(pos)) > REACH) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BedBlock)) return;
        LAST.put(player.getUUID(), now);

        BlockPos head = state.getValue(BedBlock.PART) == BedPart.HEAD ? pos.immutable() : pos.relative(state.getValue(BedBlock.FACING));
        List<BlockPos> dark = new ArrayList<>();
        Bedroom room = Bedroom.of(level, head, dark);
        SleepQuality quality = SleepNights.forecast(player, head, room);
        List<WokePayload.Line> lines = new ArrayList<>();
        for (SleepQuality.Factor factor : quality.factors()) lines.add(new WokePayload.Line(factor.label(), factor.points()));
        WCISNetwork.sendToPlayer(player, new BedCheckPayload(head, quality.score(), lines, dark, room.enclosed(), SleepNights.terrorChance(player)));
    }
}
