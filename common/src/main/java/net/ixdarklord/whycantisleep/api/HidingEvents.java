package net.ixdarklord.whycantisleep.api;

import net.ixdarklord.coolcatcore.api.event.v1.Event;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Hiding: under a bed or table, in a double chest or closet, or any spot a mod or data pack adds ({@link HidingSpots}),
 * by sneaking and using it with empty hands. While a player hides, nothing can take them as a target.
 * Once they have stayed hidden for the server's hide time, what was after them gives up and is gone: that is
 * {@link #SHAKEN_OFF}. Listeners run on the server thread.
 * <pre>{@code
 * HidingEvents.START.register(start -> {
 *     if (start.player().hasEffect(MobEffects.GLOWING)) start.cancel();      // no hiding while glowing
 * });
 * HidingEvents.SHAKEN_OFF.register(shaken -> {
 *     for (Entity hunter : shaken.hunters()) {
 *         if (hunter instanceof MyStalker stalker) {
 *             shaken.keep(stalker);                                           // gone in its own way, not discarded
 *             stalker.loseTrackOf(shaken.player());
 *         }
 *     }
 * });
 * }</pre>
 * Entity types in the tag {@code #whycantisleep:ignores_hiding} (bosses, the Warden) see through hiding: they keep
 * a hiding player as their target, and are never counted as hunters, so they never vanish.
 */
public final class HidingEvents {
    /** A player is about to hide; cancel to stop them. */
    public static final Event<HideStart> START = new Event<>();
    /** A player stopped hiding: crawled out, or was made to. */
    public static final Event<HideStop> STOP = new Event<>();
    /** A player stayed hidden long enough: the hunters in the list are about to give up and vanish. */
    public static final Event<ShakenOff> SHAKEN_OFF = new Event<>();
    /**
     * The noise a hiding player makes changed: trying to move, looking about, gasping at the heartbeat minigame's third
     * miss. From 0 to 1, as their client reports it, in steps of a tenth; at 1, with monsters near, they are heard and found.
     */
    public static final Event<NoiseChanged> NOISE_CHANGED = new Event<>();

    private HidingEvents() {
    }

    /** A player about to hide. */
    public static final class HideStart {
        private final ServerPlayer player;
        private HidingSpot spot;
        private boolean cancelled;

        public HideStart(ServerPlayer player, HidingSpot spot) {
            this.player = player;
            this.spot = spot;
        }

        public ServerPlayer player() {
            return this.player;
        }

        /** Where they are about to hide. */
        public HidingSpot spot() {
            return this.spot;
        }

        /**
         * Changes the spot they get into: a squint of your own ({@link HidingSpot#withSquint}), another hide time, a
         * different place to be held.
         */
        public void setSpot(HidingSpot spot) {
            this.spot = spot;
        }

        public void cancel() {
            this.cancelled = true;
        }

        public boolean isCancelled() {
            return this.cancelled;
        }
    }

    /**
     * A player no longer hiding.
     *
     * @param spot      where they hid
     * @param hidTicks  how long they were hidden
     */
    public record HideStop(ServerPlayer player, HidingSpot spot, Reason reason, int hidTicks) {
    }

    /** Why a player stopped hiding. */
    public enum Reason {
        /** They chose to come out. */
        LEFT,
        /** They were hurt. */
        HURT,
        /** The spot was broken, or they died, slept, left the world or changed dimension. */
        INTERRUPTED,
        /** They made too much noise with monsters near, and were found. */
        HEARD,
        /** Something was already in the spot, and chased them out. */
        STARTLED,
        /** A mod pulled them out ({@link WhyCantISleepApi#stopHiding}). */
        FORCED,
        /** Another player opened the chest or closet door they hid behind, and found them. */
        DISCOVERED,
        /** A monster that chased them in came and searched the spot, and found them. */
        FOUND
    }

    /**
     * A player who stayed hidden long enough, and what was after them. Every entity left in {@link #hunters()} when
     * the listeners are done vanishes in a puff of smoke; take one out with {@link #keep} to deal with it yourself, or
     * {@link #add} one of your own to see it go too.
     */
    public static final class ShakenOff {
        private final ServerPlayer player;
        private final HidingSpot spot;
        private final List<Entity> hunters;

        public ShakenOff(ServerPlayer player, HidingSpot spot, List<? extends Entity> hunters) {
            this.player = player;
            this.spot = spot;
            this.hunters = new ArrayList<>(hunters);
        }

        public ServerPlayer player() {
            return this.player;
        }

        /** Where they hid. */
        public HidingSpot spot() {
            return this.spot;
        }

        /** What is about to vanish: the monsters that were after the player or prowling near the spot. */
        public List<Entity> hunters() {
            return this.hunters;
        }

        /** Leaves an entity alone: it doesn't vanish. */
        public void keep(Entity entity) {
            this.hunters.remove(entity);
        }

        /** Makes another entity vanish with the rest. */
        public void add(Entity entity) {
            if (!this.hunters.contains(entity)) this.hunters.add(entity);
        }
    }

    /**
     * A hiding player's noise changed.
     *
     * @param noise    now, 0 to 1
     * @param previous before
     */
    public record NoiseChanged(ServerPlayer player, HidingSpot spot, float noise, float previous) {
    }
}
