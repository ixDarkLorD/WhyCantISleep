package net.ixdarklord.whycantisleep.sleep;

import com.mojang.datafixers.util.Either;
import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerLifecycleEvents;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.network.NightNoticePayload;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.Unit;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Naps: a player a day or more without sleep may lie down in the day, where the game would say beds are only for the
 * night. They sleep for {@link #NAP_TICKS}, not long enough to skip anything, and get up with half a day taken off
 * their sleep debt. One nap between nights: it is remembered (a player tag, so it lasts through relogging) until they
 * next sleep a night, which it costs a little ({@code daytime_nap}, the "Daytime Nap" modifier). Getting up before it
 * ends recovers nothing.
 */
public final class Naps {
    /** How long a nap lasts, in ticks: as long as the game takes to call a sleeper deeply asleep. */
    public static final int NAP_TICKS = 100;
    // How much sleep debt a nap takes off: half a day.
    private static final int RECOVERED = 12000;
    private static final int DAY = 24000;
    private static final String NAPPED = "whycantisleep.napped";
    // The game time each napping player's nap ends.
    private static final Map<UUID, Long> NAPPING = new HashMap<>();

    private Naps() {
    }

    public static void init() {
        PlayerEvents.END_TICK.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) tick(serverPlayer);
        });
        PlayerEvents.LEAVE.register(player -> NAPPING.remove(player.getUUID()));
        ServerLifecycleEvents.STOPPED.register(server -> NAPPING.clear());
    }

    /** Whether a player is napping: the game must neither wake them for the day nor count them asleep for the night. */
    public static boolean napping(Player player) {
        return NAPPING.containsKey(player.getUUID());
    }

    /** Whether a player has napped since their last night. */
    public static boolean napped(ServerPlayer player) {
        return player.entityTags().contains(NAPPED);
    }

    /** They slept a night: the nap before it has counted against it, and the next day they may nap again. */
    public static void slept(ServerPlayer player) {
        player.removeTag(NAPPED);
    }

    /**
     * The game turned a player away from a bed: if it was for it being day, and they are tired enough to nap, they lie
     * down for one instead. Returns what to answer in place of the game's refusal, or null to leave it.
     */
    public static @Nullable Either<Player.BedSleepingProblem, Unit> tryNap(ServerPlayer player, BlockPos pos, Player.BedSleepingProblem problem) {
        if (!WCISServerConfig.NAPS.get() || player.isSleeping()) return null;
        BedRule rule = player.level().environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, pos);
        // Only the day's refusal, where beds are for the night (the overworld's rule), not any other.
        if (rule.canSleep() != BedRule.Rule.WHEN_DARK || rule.canSleep(player.level()) || !problem.equals(rule.asProblem())) return null;
        if (debt(player) < DAY) return null;
        if (napped(player)) {
            return Either.left(new Player.BedSleepingProblem(Component.translatableWithFallback("gui.whycantisleep.nap.already",
                    "You've napped since your last night. Sleep tonight.")));
        }
        if (!player.isCreative() && !SleepChecks.blockers(player, pos).isEmpty()) return Either.left(Player.BedSleepingProblem.NOT_SAFE);
        // The game's lying down wipes the time since rest: a nap only takes some off, so it is kept.
        int debt = debt(player);
        player.startSleeping(pos);
        player.getStats().setValue(player, restStat(), debt);
        player.level().updateSleepingPlayerList();
        NAPPING.put(player.getUUID(), player.level().getGameTime() + NAP_TICKS);
        return Either.right(Unit.INSTANCE);
    }

    /** A napping player is getting up: rested, if they slept the nap out. */
    public static void onWake(ServerPlayer player) {
        Long until = NAPPING.remove(player.getUUID());
        if (until == null) return;
        boolean slept = player.level().getGameTime() >= until;
        if (slept) {
            player.getStats().setValue(player, restStat(), Math.max(0, debt(player) - RECOVERED));
            player.addTag(NAPPED);
        }
        WCISNetwork.sendToPlayer(player, new NightNoticePayload(slept ? NightNoticePayload.Kind.NAPPED : NightNoticePayload.Kind.NAP_CUT,
                debt(player) / (float) DAY));
    }

    // At the nap's end, they get up.
    private static void tick(ServerPlayer player) {
        Long until = NAPPING.get(player.getUUID());
        if (until == null) return;
        if (!player.isSleeping()) {
            onWake(player);
        } else if (player.level().getGameTime() >= until) {
            player.stopSleepInBed(false, true);
        }
    }

    private static int debt(ServerPlayer player) {
        return player.getStats().getValue(restStat());
    }

    private static Stat<?> restStat() {
        return Stats.CUSTOM.get(Stats.TIME_SINCE_REST);
    }
}
