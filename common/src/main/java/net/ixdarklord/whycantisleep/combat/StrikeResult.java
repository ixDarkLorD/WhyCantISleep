package net.ixdarklord.whycantisleep.combat;

/** How one of a boss's blows on a target went ({@link BossCombat#strike}). */
public enum StrikeResult {
    /** It struck home (or a shield took some of it). */
    HIT,
    /** It was parried: the target took nothing, and the boss's posture took the shock. */
    PARRIED,
    /** It did nothing (the target could not be hurt). */
    MISSED;

    /** Whether the blow landed at all, so its knockback and effects follow. */
    public boolean landed() {
        return this == HIT;
    }
}
