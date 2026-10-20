package net.ixdarklord.whycantisleep.combat;

/** What broke a boss's guard ({@link StaggerMoment#cause}). */
public enum StaggerCause {
    /** Blows wore its posture down until it broke. */
    POSTURE_BROKEN,
    /** A parry broke it: the shock of its own blow turned aside. */
    PARRIED,
    /** The boss's own doing, or a fight's script: {@link BossCombat#stagger} called directly. */
    SCRIPTED
}
