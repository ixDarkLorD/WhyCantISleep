package net.ixdarklord.whycantisleep.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * One of a boss's blows parried: a shield raised in time turns it aside whole. Listeners and the boss's hook may
 * change how much it shakes the boss's posture, whether the boss is thrown off its attack (its {@link CombatBoss#onParried}
 * plays the recoil), or cancel it, and the blow lands after all (an attack that cannot be parried by this defender).
 */
public final class ParryMoment extends CombatMoment {
    private final LivingEntity defender;
    private final BossAttack attack;
    private final float damage;
    private float posture;
    private boolean interrupts = true;

    public ParryMoment(Mob boss, BossCombat combat, ServerLevel level, LivingEntity defender, BossAttack attack, float damage, float posture) {
        super(boss, combat, level);
        this.defender = defender;
        this.attack = attack;
        this.damage = damage;
        this.posture = posture;
    }

    /** Who parried. */
    public LivingEntity defender() {
        return this.defender;
    }

    /** The attack parried. */
    public BossAttack attack() {
        return this.attack;
    }

    /** The damage it would have done. */
    public float damage() {
        return this.damage;
    }

    /** How much it shakes the boss's posture. */
    public float posture() {
        return this.posture;
    }

    public ParryMoment posture(float posture) {
        this.posture = Math.max(0.0F, posture);
        return this;
    }

    /** Whether the boss is thrown off its attack (its hook plays the recoil and stops the attack). */
    public boolean interrupts() {
        return this.interrupts;
    }

    public ParryMoment interrupts(boolean interrupts) {
        this.interrupts = interrupts;
        return this;
    }
}
