package net.ixdarklord.whycantisleep.api;

import net.ixdarklord.coolcatcore.api.event.v1.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Sleeping, as the server sees it. Listeners run on the server thread.
 * <pre>{@code
 * SleepEvents.SLEEP_FAILED.register(failed -> {
 *     if (failed.problem() == Player.BedSleepingProblem.NOT_SAFE) failed.blockers().forEach(Mob::setPersistenceRequired);
 * });
 * SleepEvents.TROUBLE_CHANGED.register(changed -> {
 *     if (changed.now() == SleepTrouble.Level.SLEEPLESS) sendSomethingAfter(changed.player());
 * });
 * SleepEvents.SCORE_QUALITY.register(scoring -> {
 *     if (hasMyPillow(scoring.player())) scoring.add(MyMod.id("pillow"), Component.literal("a soft pillow"), 10);
 * });
 * SleepEvents.WOKE.register(woke -> {
 *     if (woke.reason().rested() && woke.quality().score() >= 90) giveSomething(woke.player());
 * });
 * }</pre>
 */
public final class SleepEvents {
    /** The game refused to let a player sleep. Not fired when they were already asleep or dead. */
    public static final Event<SleepFailed> SLEEP_FAILED = new Event<>();
    /** A player lay down in a bed and fell asleep: their sleep trouble starts over. */
    public static final Event<ServerPlayer> LAY_DOWN = new Event<>();
    /** A player's {@link SleepTrouble.Level} changed, for better or worse. */
    public static final Event<TroubleChanged> TROUBLE_CHANGED = new Event<>();
    /** A sleeping player is waking: their night is being scored. Add factors of your own. */
    public static final Event<QualityScoring> SCORE_QUALITY = new Event<>();
    /** A player woke up: how well they slept, and why they woke. */
    public static final Event<Woke> WOKE = new Event<>();
    /** Enough players slept that the night was skipped; the sleepers are about to wake. */
    public static final Event<NightSkipped> NIGHT_SKIPPED = new Event<>();
    /** A restless player who lay down is about to have a night terror; cancel to spare them. */
    public static final Event<NightTerror> NIGHT_TERROR = new Event<>();
    /** A player dealt with every monster that blocked their sleep in time, and nothing else came near: the room is safe. */
    public static final Event<RoomSafe> ROOM_SAFE = new Event<>();
    /** A sleep effect was drawn for a waking player; change it, or cancel to give none. */
    public static final Event<SleepEffectDrawn> SLEEP_EFFECT = new Event<>();
    /** The players in bed voted to skip the night, and those still up let them: the night was skipped. */
    public static final Event<NightVoted> NIGHT_VOTED = new Event<>();

    private SleepEvents() {
    }

    /**
     * A refused sleep.
     *
     * @param bed      the bed's head
     * @param problem  the game's reason
     * @param blockers for {@link Player.BedSleepingProblem#NOT_SAFE}, exactly the monsters that kept the player awake; else empty
     * @param trouble  the player's sleep trouble, this refusal counted
     */
    public record SleepFailed(ServerPlayer player, BlockPos bed, Player.BedSleepingProblem problem, List<Mob> blockers, SleepTrouble trouble) {
    }

    /** A player's sleep trouble went from one level to another. */
    public record TroubleChanged(ServerPlayer player, SleepTrouble.Level before, SleepTrouble.Level now, SleepTrouble trouble) {
        public boolean worsened() {
            return this.now.ordinal() > this.before.ordinal();
        }
    }

    /** A night being scored, as its sleeper wakes. Add to it with {@link #add}. */
    public static final class QualityScoring {
        private final ServerPlayer player;
        private final WakeReason reason;
        private final List<SleepQuality.Factor> factors;

        public QualityScoring(ServerPlayer player, WakeReason reason, List<SleepQuality.Factor> factors) {
            this.player = player;
            this.reason = reason;
            this.factors = new ArrayList<>(factors);
        }

        public ServerPlayer player() {
            return this.player;
        }

        public WakeReason reason() {
            return this.reason;
        }

        /** The factors so far: the mod's own, then those listeners added. */
        public List<SleepQuality.Factor> factors() {
            return this.factors;
        }

        public void add(SleepQuality.Factor factor) {
            this.factors.add(factor);
        }

        public void add(ResourceLocation id, Component label, int points) {
            this.add(new SleepQuality.Factor(id, label, points));
        }

        /** Takes out the factor with this id, if there is one: a mod that makes, say, creepy music soothing. */
        public void remove(ResourceLocation id) {
            this.factors.removeIf(factor -> factor.id().equals(id));
        }
    }

    /**
     * A player who woke up.
     *
     * @param quality    how well they slept
     * @param sleptTicks how long they lay asleep
     * @param shaken     whether they had a night terror
     * @param wellRested whether they kept a bedtime routine, and were rewarded for it
     */
    public record Woke(ServerPlayer player, WakeReason reason, SleepQuality quality, int sleptTicks, boolean shaken, boolean wellRested,
                       @Nullable SleepEffect sleepEffect) {
    }

    /** A sleep effect about to be given to a waking player. */
    public static final class SleepEffectDrawn {
        private final ServerPlayer player;
        private final SleepQuality quality;
        private SleepEffect effect;
        private boolean cancelled;

        public SleepEffectDrawn(ServerPlayer player, SleepQuality quality, SleepEffect effect) {
            this.player = player;
            this.quality = quality;
            this.effect = effect;
        }

        public ServerPlayer player() {
            return this.player;
        }

        /** How well they slept, which the effect was drawn for. */
        public SleepQuality quality() {
            return this.quality;
        }

        public SleepEffect effect() {
            return this.effect;
        }

        /** Gives another effect instead. */
        public void setEffect(SleepEffect effect) {
            this.effect = effect;
        }

        public void cancel() {
            this.cancelled = true;
        }

        public boolean isCancelled() {
            return this.cancelled;
        }
    }

    /**
     * A night skipped by sleeping.
     *
     * @param sleepers the players asleep, about to wake
     */
    public record NightSkipped(ServerLevel level, List<ServerPlayer> sleepers) {
    }

    /**
     * A room cleared of the monsters that blocked a sleep.
     *
     * @param bed     the bed they tried to sleep in
     * @param cleared how many monsters there were
     */
    public record RoomSafe(ServerPlayer player, BlockPos bed, int cleared) {
    }

    /**
     * A night skipped by vote.
     *
     * @param sleepers the players who were in bed
     */
    public record NightVoted(ServerLevel level, List<ServerPlayer> sleepers) {
    }

    /** A night terror about to begin. */
    public static final class NightTerror {
        private final ServerPlayer player;
        private final SleepTrouble trouble;
        private boolean cancelled;

        public NightTerror(ServerPlayer player, SleepTrouble trouble) {
            this.player = player;
            this.trouble = trouble;
        }

        public ServerPlayer player() {
            return this.player;
        }

        /** How badly they had been sleeping, before they lay down. */
        public SleepTrouble trouble() {
            return this.trouble;
        }

        public void cancel() {
            this.cancelled = true;
        }

        public boolean isCancelled() {
            return this.cancelled;
        }
    }
}
