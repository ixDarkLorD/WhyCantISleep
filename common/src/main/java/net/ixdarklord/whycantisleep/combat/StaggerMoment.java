package net.ixdarklord.whycantisleep.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

/**
 * A boss staggering: its guard broken, reeling, open. Listeners and the boss's own hook may change how long it reels,
 * how much harder it is hurt meanwhile, whether its critical point is open to a critical strike, and whether the
 * usual effects play (a crack of breaking guard, sparks, the boss's name over the breaker's hotbar), or cancel it.
 * The boss's {@link CombatBoss#onStagger} plays its own part (its stagger animation, say).
 * <pre>{@code
 * CombatEvents.STAGGER.register(moment -> {
 *     if (moment.cause() == StaggerCause.PARRIED) moment.duration(moment.duration() + 20);   // a parry breaks harder
 * });
 * }</pre>
 */
public final class StaggerMoment extends CombatMoment {
    private final StaggerCause cause;
    private final @Nullable LivingEntity breaker;
    private int duration;
    private float damageMultiplier;
    private boolean exposesCritical = true;
    private boolean effects = true;

    public StaggerMoment(Mob boss, BossCombat combat, ServerLevel level, StaggerCause cause, @Nullable LivingEntity breaker,
                         int duration, float damageMultiplier) {
        super(boss, combat, level);
        this.cause = cause;
        this.breaker = breaker;
        this.duration = duration;
        this.damageMultiplier = damageMultiplier;
    }

    /** What broke its guard. */
    public StaggerCause cause() {
        return this.cause;
    }

    /** Who broke it, if anyone did. */
    public @Nullable LivingEntity breaker() {
        return this.breaker;
    }

    /** How long it reels, in ticks. */
    public int duration() {
        return this.duration;
    }

    public StaggerMoment duration(int ticks) {
        this.duration = Math.max(1, ticks);
        return this;
    }

    /** How much more it is hurt while it reels (1.5 is half again). */
    public float damageMultiplier() {
        return this.damageMultiplier;
    }

    public StaggerMoment damageMultiplier(float multiplier) {
        this.damageMultiplier = Math.max(0.0F, multiplier);
        return this;
    }

    /** Whether its critical point ({@link CombatBoss#criticalPoint}) is open to a critical strike while it reels. */
    public boolean exposesCritical() {
        return this.exposesCritical;
    }

    public StaggerMoment exposesCritical(boolean exposes) {
        this.exposesCritical = exposes;
        return this;
    }

    /** Whether the usual effects of a broken guard play (sound, sparks, a word to those near). */
    public boolean effects() {
        return this.effects;
    }

    public StaggerMoment effects(boolean play) {
        this.effects = play;
        return this;
    }
}
