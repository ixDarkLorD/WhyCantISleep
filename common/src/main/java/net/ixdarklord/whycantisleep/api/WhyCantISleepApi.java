package net.ixdarklord.whycantisleep.api;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.hiding.Hiding;
import net.ixdarklord.whycantisleep.hiding.HidingServer;
import net.ixdarklord.whycantisleep.sleep.SleepChecks;
import net.ixdarklord.whycantisleep.sleep.SleepNights;
import net.ixdarklord.whycantisleep.sleep.SleepTroubles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What Why Can't I Sleep? offers other mods. Everything here is safe to call only while the mod is loaded: check
 * {@code Platform.isModLoaded(WhyCantISleepApi.MOD_ID)} first, and keep the calls in a class of their own so nothing
 * loads them when it isn't.
 * <p>
 * For things happening, listen to {@link SleepEvents} and {@link HidingEvents}; for new places to hide, see
 * {@link HidingSpots}.
 */
public final class WhyCantISleepApi {
    public static final String MOD_ID = WhyCantISleep.MOD_ID;

    private WhyCantISleepApi() {
    }

    /** How well a player slept the last time they slept, or null when they haven't since the server started. Server side. */
    public static @Nullable SleepQuality lastSleepQuality(ServerPlayer player) {
        return SleepNights.last(player);
    }

    /**
     * Gives a player a night terror now. Asleep, they wake shaken and the night
     * is scored for it; awake, they only see it. Server side.
     */
    public static void nightmare(ServerPlayer player) {
        SleepNights.nightmare(player);
    }

    /**
     * Marks a player for a night terror the next time they lie down (as {@code /whycantisleep nightmare} does), whether
     * or not they sleep badly; they wake shaken and the night is scored for it. One already asleep has it now.
     */
    public static void markNightmare(ServerPlayer player) {
        SleepNights.markNightmare(player);
    }

    /** How badly a player is sleeping. Server side. */
    public static SleepTrouble sleepTrouble(ServerPlayer player) {
        return SleepTroubles.get(player);
    }

    /**
     * Exactly the mobs that would keep a player from sleeping in a bed: the game's own check with the mod's rules over it
     * (the {@code #whycantisleep:blocks_sleep} and {@code #whycantisleep:does_not_block_sleep} tags, a shared bed), so
     * the same mobs the game would refuse the sleep for. Server side.
     *
     * @param bed the bed's head
     */
    public static List<Mob> sleepBlockers(ServerPlayer player, BlockPos bed) {
        return SleepChecks.blockers(player, bed);
    }

    /** Whether a player is hiding. Both sides: a client knows it of every player it can see. */
    public static boolean isHiding(Player player) {
        return Hiding.isHiding(player);
    }

    /** Where a player hides, or null when they aren't hiding. Server side. */
    public static @Nullable HidingSpot hidingSpot(ServerPlayer player) {
        return HidingServer.spot(player);
    }

    /** How long a player has been hiding, in ticks; 0 when they aren't. Server side. */
    public static int hiddenTicks(ServerPlayer player) {
        return HidingServer.hiddenTicks(player);
    }

    /**
     * How long a player must stay hidden in their spot before {@link HidingEvents#SHAKEN_OFF}, in ticks: the server's
     * hide time, scaled by the spot and the walls round it. 0 when they aren't hiding. Server side.
     */
    public static int hideTicks(ServerPlayer player) {
        return HidingServer.hideTicks(player);
    }

    /** The server's hide time, in ticks, before a spot or its walls scale it. */
    public static int baseHideTicks() {
        return WCISServerConfig.HIDE_TIME.get() * 20;
    }

    /** The noise a hiding player makes, 0 to 1, as their client last reported it. Server side. */
    public static float hidingNoise(ServerPlayer player) {
        return HidingServer.noise(player);
    }

    /** Puts a player in the hiding spot at a block, as if they had got in themselves. Returns false when they can't hide there. */
    public static boolean startHiding(ServerPlayer player, BlockPos pos) {
        return HidingServer.start(player, pos);
    }

    /** Puts a player in a hiding spot (one a {@link HidingSpotType} found). Returns false when they can't hide. */
    public static boolean startHiding(ServerPlayer player, HidingSpot spot) {
        return HidingServer.start(player, spot);
    }

    /** Brings a hiding player out of their spot. Returns false when they weren't hiding. */
    public static boolean stopHiding(ServerPlayer player) {
        return HidingServer.stop(player, HidingEvents.Reason.FORCED);
    }
}
