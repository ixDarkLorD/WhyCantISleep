package net.ixdarklord.whycantisleep.combat;

/**
 * How a boss fights, for {@link BossCombat}: its posture (how much pressure it takes before its guard breaks, and how
 * fast it steadies), how long it staggers and how much harder it is hurt meanwhile, what a parry of its attacks costs
 * it, and what a critical strike on it does. Built once per kind of boss and shared.
 * <pre>{@code
 * static final CombatProfile PROFILE = CombatProfile.builder()
 *         .posture(100).regen(0.6F, 40).hitPosture(0.7F).parryPosture(32)
 *         .stagger(70, 1.5F).critical(3.0F, 12.0F, 4.5)
 *         .build();
 * }</pre>
 *
 * @param maxPosture        how much posture it can take before it breaks and it staggers
 * @param regenPerTick      how much posture it steadies by each tick, once it has gone {@code regenDelay} ticks unhurt
 * @param regenDelay        how long after being pressed it starts to steady
 * @param hitPosture        posture it takes per point of damage from a blow
 * @param parryPosture      posture it takes when one of its attacks is parried
 * @param parryWindow       how soon after raising a shield a blow must land to be parried, in ticks
 * @param staggerTicks      how long it staggers, unless a moment says otherwise
 * @param staggerDamage     how much more it is hurt while staggered (1.5 is half again)
 * @param criticalMultiplier a critical strike's damage, times the blow's
 * @param criticalBonus     and added to it
 * @param criticalReach     how near a player must be to strike its critical point, in blocks
 */
public record CombatProfile(float maxPosture, float regenPerTick, int regenDelay, float hitPosture, float parryPosture,
                            int parryWindow, int staggerTicks, float staggerDamage, float criticalMultiplier,
                            float criticalBonus, double criticalReach) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private float maxPosture = 100.0F, regenPerTick = 0.5F, hitPosture = 0.6F, parryPosture = 30.0F;
        private int regenDelay = 40, parryWindow = 6, staggerTicks = 60;
        private float staggerDamage = 1.5F, criticalMultiplier = 3.0F, criticalBonus = 8.0F;
        private double criticalReach = 4.5;

        private Builder() {
        }

        /** How much posture it can take before its guard breaks; 100 by default. */
        public Builder posture(float max) {
            this.maxPosture = Math.max(1.0F, max);
            return this;
        }

        /** How fast it steadies (posture a tick) and how long after being pressed it starts to; 0.5 and 40 by default. */
        public Builder regen(float perTick, int delayTicks) {
            this.regenPerTick = Math.max(0.0F, perTick);
            this.regenDelay = Math.max(0, delayTicks);
            return this;
        }

        /** Posture it takes per point of damage from a blow; 0.6 by default. */
        public Builder hitPosture(float perDamage) {
            this.hitPosture = Math.max(0.0F, perDamage);
            return this;
        }

        /** Posture it takes when one of its attacks is parried; 30 by default. */
        public Builder parryPosture(float posture) {
            this.parryPosture = Math.max(0.0F, posture);
            return this;
        }

        /** How soon after raising a shield a blow must land to be parried, in ticks; 6 by default. */
        public Builder parryWindow(int ticks) {
            this.parryWindow = Math.max(0, ticks);
            return this;
        }

        /** How long it staggers, and how much more it is hurt meanwhile; 60 ticks and 1.5 by default. */
        public Builder stagger(int ticks, float damageMultiplier) {
            this.staggerTicks = Math.max(1, ticks);
            this.staggerDamage = Math.max(0.0F, damageMultiplier);
            return this;
        }

        /** A critical strike: the blow times {@code multiplier}, plus {@code bonus}, from within {@code reach} blocks. */
        public Builder critical(float multiplier, float bonus, double reach) {
            this.criticalMultiplier = Math.max(0.0F, multiplier);
            this.criticalBonus = Math.max(0.0F, bonus);
            this.criticalReach = Math.max(0.5, reach);
            return this;
        }

        public CombatProfile build() {
            return new CombatProfile(this.maxPosture, this.regenPerTick, this.regenDelay, this.hitPosture, this.parryPosture,
                    this.parryWindow, this.staggerTicks, this.staggerDamage, this.criticalMultiplier, this.criticalBonus, this.criticalReach);
        }
    }
}
