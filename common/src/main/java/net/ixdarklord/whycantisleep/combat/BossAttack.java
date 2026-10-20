package net.ixdarklord.whycantisleep.combat;

/**
 * One of a boss's attacks, as {@link BossCombat#strike} lands it: its name (for listeners and hooks to tell them
 * apart), whether a well-timed shield parries it (a sweep of a blade, yes; roots bursting out of the ground, no), and
 * how much it shakes the posture of a boss whose blow is parried, on top of the profile's
 * {@link CombatProfile#parryPosture}.
 * <pre>{@code
 * static final BossAttack SCYTHE = BossAttack.of("scythe").parryable(true);
 * static final BossAttack ROOTS = BossAttack.of("roots").parryable(false);
 * }</pre>
 */
public final class BossAttack {
    private final String name;
    private boolean parryable = true;
    private float parryPosture = 1.0F;

    private BossAttack(String name) {
        this.name = name;
    }

    public static BossAttack of(String name) {
        return new BossAttack(name);
    }

    /** Whether a shield raised just before it lands parries it; true by default. */
    public BossAttack parryable(boolean parryable) {
        this.parryable = parryable;
        return this;
    }

    /** How much of the profile's parry posture a parry of this attack costs the boss (a heavy blow, more); 1 by default. */
    public BossAttack parryPosture(float scale) {
        this.parryPosture = Math.max(0.0F, scale);
        return this;
    }

    public String name() {
        return this.name;
    }

    public boolean parryable() {
        return this.parryable;
    }

    public float parryPostureScale() {
        return this.parryPosture;
    }

    @Override
    public String toString() {
        return "BossAttack[" + this.name + "]";
    }
}
