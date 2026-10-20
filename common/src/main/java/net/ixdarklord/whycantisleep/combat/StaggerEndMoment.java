package net.ixdarklord.whycantisleep.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/**
 * A boss's stagger ending: it steadied in time, or a critical strike cut it short. Cancelling it while it {@link #recovered}
 * keeps it reeling a moment longer ({@link #extend}), for a listener that wants it open longer; a stagger cut short by
 * a critical strike ends regardless. The boss's {@link CombatBoss#onStaggerEnd} plays its own part (rising, say).
 */
public final class StaggerEndMoment extends CombatMoment {
    private final boolean recovered;
    private int extend = 20;

    public StaggerEndMoment(Mob boss, BossCombat combat, ServerLevel level, boolean recovered) {
        super(boss, combat, level);
        this.recovered = recovered;
    }

    /** Whether it steadied in its own time (rather than being struck out of it). */
    public boolean recovered() {
        return this.recovered;
    }

    /** How much longer it reels if this is cancelled, in ticks; 20 by default. */
    public int extend() {
        return this.extend;
    }

    public StaggerEndMoment extend(int ticks) {
        this.extend = Math.max(1, ticks);
        return this;
    }
}
