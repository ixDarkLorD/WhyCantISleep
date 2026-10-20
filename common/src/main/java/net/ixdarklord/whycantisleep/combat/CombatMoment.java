package net.ixdarklord.whycantisleep.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/**
 * A moment in a boss fight that others may see and change before it happens: a stagger, a parry, a critical strike.
 * {@link BossCombat} hands each one first to everyone listening ({@link CombatEvents}), then to the boss's own
 * {@link CombatBoss} hook, then acts on what it says. Change its fields to change what happens; {@link #cancel} it to
 * stop it happening at all.
 */
public abstract class CombatMoment {
    private final Mob boss;
    private final BossCombat combat;
    private final ServerLevel level;
    private boolean cancelled;

    protected CombatMoment(Mob boss, BossCombat combat, ServerLevel level) {
        this.boss = boss;
        this.combat = combat;
        this.level = level;
    }

    /** The boss it happens to. */
    public Mob boss() {
        return this.boss;
    }

    /** Its combat (posture, stagger). */
    public BossCombat combat() {
        return this.combat;
    }

    public ServerLevel level() {
        return this.level;
    }

    /** Stops it happening. */
    public void cancel() {
        this.cancelled = true;
    }

    public boolean cancelled() {
        return this.cancelled;
    }
}
