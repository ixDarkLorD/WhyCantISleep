package net.ixdarklord.whycantisleep.api;

/** Why a sleeping player woke up. */
public enum WakeReason {
    /** Morning: the night was skipped, or the day came. */
    MORNING,
    /** They got up themselves. */
    GOT_UP,
    /** Something hurt them. */
    HURT,
    /** Their bed was broken, or is gone. */
    BED_GONE,
    /** Anything else: a mod, a command, leaving the world. */
    OTHER;

    /** Whether they slept through the night. */
    public boolean rested() {
        return this == MORNING;
    }
}
