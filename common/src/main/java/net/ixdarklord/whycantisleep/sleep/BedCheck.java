package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.coolcatcore.api.event.v2.common.BlockEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.InteractionEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerLifecycleEvents;
import net.ixdarklord.coolcatcore.api.event.v2.core.EventResult;
import net.ixdarklord.whycantisleep.api.SleepQuality;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.network.BedCheckPayload;
import net.ixdarklord.whycantisleep.network.BedCheckRequestPayload;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.ixdarklord.whycantisleep.network.WokePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
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
 * The bed check: a player sneaking and using a bed with a tool in hand ({@link BedTools#checksByClick}) is told how a
 * night in it would go before they lie down ({@link SleepNights#forecast}): the score it would have, what makes it so,
 * and the chance of a night terror, with a Dreamcatcher; where in a closed room a monster could spawn, with a Bedside
 * Lantern. The server looks the room over and answers. While a player is reading a bed, a change round it (a block
 * placed or broken, a door or a light used) answers again a moment later, so the card follows the room as it is changed.
 */
public final class BedCheck {
    // How near the bed the player must be (as for using it, with room to spare), and how often one player may ask:
    // looking a room over is a flood fill of up to a few hundred blocks.
    private static final double REACH = 6.0;
    private static final int COOLDOWN_TICKS = 20;
    // A bed read is followed this long after the last ask, and a change
    // this near it is looked at again after a few ticks, once the light has settled, at most this often.
    private static final int WATCH_TICKS = 20 * 6;
    private static final double CHANGE_RANGE = 14.0;
    private static final int SETTLE_TICKS = 3;
    private static final int LIVE_EVERY_TICKS = 5;

    private static final Map<UUID, Long> LAST = new HashMap<>();
    private static final Map<UUID, Watch> WATCHING = new HashMap<>();

    private BedCheck() {
    }

    public static void init() {
        BlockEvents.BREAK.register((level, pos, state, player) -> {
            changed(level, pos);
            return EventResult.PASS;
        });
        BlockEvents.PLACED.register((level, pos, state, placer) -> changed(level, pos));
        InteractionEvents.USE_BLOCK.register((player, hand, hit) -> {
            if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            // Using the bed itself changes nothing of the room: it is a try to sleep or to hide, or, sneaking with a tool in
            // hand, a check of it (answered on the client too, so it predicts nothing else).
            if (!(player.level().getBlockState(hit.getBlockPos()).getBlock() instanceof BedBlock)) {
                changed(player.level(), hit.getBlockPos());
                return InteractionResult.PASS;
            }
            if (hand != InteractionHand.MAIN_HAND || !WCISServerConfig.BED_CHECK.get() || !BedTools.checksByClick(player)) {
                return InteractionResult.PASS;
            }
            check(serverPlayer, hit.getBlockPos());
            return InteractionResult.SUCCESS;
        });
        PlayerEvents.END_TICK.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) tick(serverPlayer);
        });
        PlayerEvents.LEAVE.register(player -> {
            LAST.remove(player.getUUID());
            WATCHING.remove(player.getUUID());
        });
        ServerLifecycleEvents.STOPPED.register(server -> {
            LAST.clear();
            WATCHING.clear();
        });
    }

    /** The client asks how a night in this bed would go. */
    public static void onRequest(ServerPlayer player, BedCheckRequestPayload payload) {
        check(player, payload.bed());
    }

    // Answers a player reading the bed at this spot, at most once a second.
    private static void check(ServerPlayer player, BlockPos pos) {
        if (!WCISServerConfig.BED_CHECK.get() || player.isSleeping() || player.isSpectator()) return;
        ServerLevel level = player.level();
        long now = level.getGameTime();
        Long last = LAST.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_TICKS) return;
        if (!level.isLoaded(pos) || player.getEyePosition().distanceTo(Vec3.atCenterOf(pos)) > REACH) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BedBlock)) return;
        BlockPos head = state.getValue(BedBlock.PART) == BedPart.HEAD ? pos.immutable() : pos.relative(state.getValue(BedBlock.FACING));
        if (!BedTools.canCheck(player, head)) return;
        LAST.put(player.getUUID(), now);
        WATCHING.put(player.getUUID(), new Watch(level.dimension(), head, now + WATCH_TICKS));
        answer(player, level, head);
    }

    // Something changed at a spot: everyone reading a bed near it is answered again shortly.
    private static void changed(Level level, BlockPos pos) {
        if (level.isClientSide() || WATCHING.isEmpty()) return;
        long now = level.getGameTime();
        for (Watch watch : WATCHING.values()) {
            if (watch.dimension == level.dimension() && watch.head.closerThan(pos, CHANGE_RANGE)) {
                watch.due = Math.max(now + SETTLE_TICKS, watch.answered + LIVE_EVERY_TICKS);
            }
        }
    }

    private static void tick(ServerPlayer player) {
        Watch watch = WATCHING.get(player.getUUID());
        if (watch == null) return;
        ServerLevel level = player.level();
        long now = level.getGameTime();
        if (now > watch.until || level.dimension() != watch.dimension || !(level.getBlockState(watch.head).getBlock() instanceof BedBlock)) {
            WATCHING.remove(player.getUUID());
            return;
        }
        if (watch.due < 0 || now < watch.due) return;
        watch.due = -1;
        if (!WCISServerConfig.BED_CHECK.get() || player.isSleeping() || !BedTools.canCheck(player, watch.head)) return;
        answer(player, level, watch.head);
    }

    // Looks the room over and tells the player what their tools let them read of it.
    private static void answer(ServerPlayer player, ServerLevel level, BlockPos head) {
        Watch watch = WATCHING.get(player.getUUID());
        if (watch != null) watch.answered = level.getGameTime();
        boolean forecast = BedTools.canForecast(player, head);
        boolean spots = BedTools.canSeeDarkSpots(player);
        List<BlockPos> dark = new ArrayList<>();
        Bedroom room = Bedroom.of(level, head, dark);
        List<WokePayload.Line> lines = new ArrayList<>();
        int score = 0;
        if (forecast) {
            SleepQuality quality = SleepNights.forecast(player, head, room);
            score = quality.score();
            for (SleepQuality.Factor factor : quality.factors()) lines.add(new WokePayload.Line(factor.label(), factor.points()));
        }
        WCISNetwork.sendToPlayer(player, new BedCheckPayload(head, forecast, score, lines, spots ? dark : List.of(), spots, room.enclosed(),
                forecast ? SleepNights.terrorChance(player) : 0));
    }

    private static final class Watch {
        private final ResourceKey<Level> dimension;
        private final BlockPos head;
        private final long until;
        private long due = -1;
        private long answered = Long.MIN_VALUE / 2;

        private Watch(ResourceKey<Level> dimension, BlockPos head, long until) {
            this.dimension = dimension;
            this.head = head;
            this.until = until;
        }
    }
}
