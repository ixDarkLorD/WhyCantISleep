package net.ixdarklord.whycantisleep.combat;

import org.jetbrains.annotations.Nullable;

/**
 * A boss that fights with {@link BossCombat}: its hooks are its own part in each {@link CombatMoment}, after everyone
 * listening on {@link CombatEvents} has had theirs. Each may still change or cancel the moment.
 * <pre>{@code
 * public class MyBoss extends Monster implements CombatBoss {
 *     private final BossCombat combat = new BossCombat(this, PROFILE, COMBAT);
 *     public BossCombat combat() { return this.combat; }
 *     public void onStagger(StaggerMoment moment) { ArmatureControl.play(this, "action", Motion.once("stagger")); }
 *     public CriticalPoint criticalPoint() { return new CriticalPoint(this.getEyePosition(), 0.6); }
 * }
 * }</pre>
 */
public interface CombatBoss {
    /** Its combat. */
    BossCombat combat();

    /** Its guard breaks: play its stagger. */
    default void onStagger(StaggerMoment moment) {
    }

    /** It steadies, or is struck out of its stagger: play its recovery. */
    default void onStaggerEnd(StaggerEndMoment moment) {
    }

    /** One of its blows is parried: if the moment {@link ParryMoment#interrupts}, stop the attack and play its recoil. */
    default void onParried(ParryMoment moment) {
    }

    /** Its weak spot as it is now, or null if it has none (or none open now). Asked only while it staggers. */
    default @Nullable CriticalPoint criticalPoint() {
        return null;
    }

    /** Its critical point is struck: play its reaction. */
    default void onCriticalStrike(CriticalStrikeMoment moment) {
    }
}
