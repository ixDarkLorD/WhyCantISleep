package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerLifecycleEvents;
import net.ixdarklord.whycantisleep.api.SleepTrouble;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.network.NightNoticePayload;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sleepwalking: a sleepless player, in a room that isn't closed, may wake in the morning a few blocks from their bed,
 * lower down if there is somewhere lower to be (downstairs, down the hill), at the server's chance. A closed room (its
 * doors shut) keeps them in it. The game stands them up beside the bed first, so they are moved the tick after.
 */
public final class Sleepwalking {
    // How far from the bed they wander, how far down or up, and how many places are tried.
    private static final int NEAREST = 3;
    private static final int FARTHEST = 8;
    private static final int DOWN = 4;
    private static final int UP = 1;
    private static final int TRIES = 48;
    // The bed each player who sleepwalked woke from, to move them from on the next tick.
    private static final Map<UUID, BlockPos> WALKED = new HashMap<>();

    private Sleepwalking() {
    }

    public static void init() {
        PlayerEvents.END_TICK.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) tick(serverPlayer);
        });
        PlayerEvents.LEAVE.register(player -> WALKED.remove(player.getUUID()));
        ServerLifecycleEvents.STOPPED.register(server -> WALKED.clear());
    }

    /** A player woke in the morning: whether they sleepwalked in the night, by how they slept and where. */
    public static void onMorning(ServerPlayer player, BlockPos bed, SleepTrouble.Level trouble, boolean enclosed) {
        if (!WCISServerConfig.SLEEPWALKING.get() || trouble != SleepTrouble.Level.SLEEPLESS || enclosed) return;
        if (player.getRandom().nextInt(100) >= WCISServerConfig.SLEEPWALK_CHANCE.get()) return;
        WALKED.put(player.getUUID(), bed.immutable());
    }

    private static void tick(ServerPlayer player) {
        BlockPos bed = WALKED.remove(player.getUUID());
        if (bed == null || player.isSleeping() || !player.isAlive()) return;
        Vec3 to = somewhere(player.level(), player, bed, player.getRandom());
        if (to == null) return;
        player.teleportTo(to.x, to.y, to.z);
        WCISNetwork.sendToPlayer(player, new NightNoticePayload(NightNoticePayload.Kind.SLEEPWALKED, (float) Math.sqrt(bed.distToCenterSqr(to))));
    }

    // A place a few blocks from the bed to stand: a sturdy floor, room to stand, no water or lava; the lowest of those
    // found, so a house with a downstairs sends them down it.
    private static @Nullable Vec3 somewhere(ServerLevel level, ServerPlayer player, BlockPos bed, RandomSource random) {
        BlockPos best = null;
        for (int i = 0; i < TRIES; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int distance = NEAREST + random.nextInt(FARTHEST - NEAREST + 1);
            BlockPos at = bed.offset((int) Math.round(Math.cos(angle) * distance), random.nextInt(DOWN + UP + 1) - DOWN,
                    (int) Math.round(Math.sin(angle) * distance));
            if (!standable(level, player, at)) continue;
            if (best == null || at.getY() < best.getY()) best = at;
        }
        return best == null ? null : Vec3.atBottomCenterOf(best);
    }

    private static boolean standable(ServerLevel level, ServerPlayer player, BlockPos at) {
        BlockPos floor = at.below();
        if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) return false;
        if (!level.getFluidState(at).isEmpty() || !level.getFluidState(at.above()).isEmpty()) return false;
        return level.noCollision(player, player.getDimensions(Pose.STANDING).makeBoundingBox(Vec3.atBottomCenterOf(at)));
    }
}
