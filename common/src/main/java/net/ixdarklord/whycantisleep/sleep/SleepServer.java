package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.whycantisleep.api.SleepEvents;
import net.ixdarklord.whycantisleep.api.SleepTrouble;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.network.SleepBlockedPayload;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * The server's side of trying to sleep ({@code ServerPlayerMixin}): it knows exactly which monsters blocked a sleep
 * and tells the player, counts the refusals ({@link SleepTroubles}) and tells other mods ({@link SleepEvents}).
 */
public final class SleepServer {
    private SleepServer() {
    }

    /** The player lay down in a bed. */
    public static void onLayDown(ServerPlayer player, BlockPos bed) {
        SleepTroubles.onLayDown(player);
        SleepNights.onLayDown(player, bed);
    }

    /** The game refused a sleep. "Other problem" is a player already asleep or dead, not a sleep that failed. */
    public static void onSleepFailed(ServerPlayer player, BlockPos bed, Player.BedSleepingProblem problem) {
        if (problem == Player.BedSleepingProblem.OTHER_PROBLEM) return;
        List<Mob> blockers = problem == Player.BedSleepingProblem.NOT_SAFE ? SleepChecks.blockers(player, bed) : List.of();
        if (!blockers.isEmpty()) {
            showBlockers(player, bed, blockers);
            RoomClearing.start(player, bed, blockers);
        }
        SleepTrouble trouble = SleepTroubles.onFailed(player);
        SleepEvents.SLEEP_FAILED.invoke(new SleepEvents.SleepFailed(player, bed.immutable(), problem, List.copyOf(blockers), trouble));
    }

    private static void showBlockers(ServerPlayer player, BlockPos bed, List<Mob> blockers) {
        if (WCISNetwork.hasMod(player)) {
            WCISNetwork.sendToPlayer(player, new SleepBlockedPayload(bed, blockers.stream().map(Entity::getId).toList()));
        } else if (WCISServerConfig.ALLOW_HIGHLIGHTS.get() && WCISServerConfig.SHOW_TO_EVERYONE.get()) {
            // A client without the mod can only be shown the game's own glowing effect, which everyone nearby sees too.
            int ticks = WCISServerConfig.HIGHLIGHT_DURATION.get() * 20;
            for (Mob monster : blockers) {
                monster.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
            }
        }
    }
}
