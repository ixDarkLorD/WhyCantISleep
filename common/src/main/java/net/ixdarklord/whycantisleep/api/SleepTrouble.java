package net.ixdarklord.whycantisleep.api;

/**
 * How badly a player is sleeping, from two things the server knows: how many times in a row their sleep was refused,
 * and how long it has been since they last lay down in a bed (the game's own "time since rest" statistic, the one
 * that brings phantoms).
 *
 * @param failedAttempts sleeps refused since the player last lay down (a monster nearby, the wrong time, ...)
 * @param ticksSinceRest game ticks since the player last lay down in a bed, or since they joined the world or died
 */
public record SleepTrouble(int failedAttempts, int ticksSinceRest) {
    /** No trouble at all, for players the server knows nothing of. */
    public static final SleepTrouble NONE = new SleepTrouble(0, 0);
    /** One in-game day: 20 minutes. */
    public static final int DAY_TICKS = 24000;

    /** How bad it is, by the thresholds on {@link Level}. */
    public Level level() {
        if (this.failedAttempts >= Level.SLEEPLESS.failedAttempts || this.ticksSinceRest >= Level.SLEEPLESS.ticksSinceRest) return Level.SLEEPLESS;
        if (this.failedAttempts >= Level.RESTLESS.failedAttempts || this.ticksSinceRest >= Level.RESTLESS.ticksSinceRest) return Level.RESTLESS;
        return Level.RESTED;
    }

    /** Whether the player is at least {@linkplain Level#RESTLESS restless}. */
    public boolean isTroubled() {
        return this.level() != Level.RESTED;
    }

    /** How bad a player's sleep is. Either measure reaching a level's threshold puts them at that level. */
    public enum Level {
        /** Sleeping well enough. */
        RESTED(0, 0),
        /** Two refused sleeps in a row, or two days without lying down. */
        RESTLESS(2, DAY_TICKS * 2),
        /** Five refused sleeps in a row, or three days without lying down: when the game starts sending phantoms. */
        SLEEPLESS(5, DAY_TICKS * 3);

        private final int failedAttempts;
        private final int ticksSinceRest;

        Level(int failedAttempts, int ticksSinceRest) {
            this.failedAttempts = failedAttempts;
            this.ticksSinceRest = ticksSinceRest;
        }

        /** Refused sleeps in a row that reach this level. */
        public int failedAttempts() {
            return this.failedAttempts;
        }

        /** Ticks without lying down that reach this level. */
        public int ticksSinceRest() {
            return this.ticksSinceRest;
        }

        /** Whether this level is at least as bad as another. */
        public boolean atLeast(Level other) {
            return this.ordinal() >= other.ordinal();
        }
    }
}
