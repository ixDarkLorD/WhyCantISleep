package net.ixdarklord.whycantisleep.combat;

import net.ixdarklord.coolcatcore.api.event.v1.Event;

/**
 * Everyone's say in every boss fight: listeners here see each {@link CombatMoment} of every boss using
 * {@link BossCombat}, before the boss's own hook does, and may change or cancel it. For one boss's own part, implement
 * the hooks of {@link CombatBoss} instead.
 * <pre>{@code
 * CombatEvents.STAGGER.register(moment -> moment.duration(moment.duration() * 2));   // a harder game: longer openings
 * CombatEvents.CRITICAL_STRIKE.register(moment -> moment.bonus(moment.bonus() + 5)); // a charm that hits hearts harder
 * }</pre>
 */
public final class CombatEvents {
    /** A boss's guard breaks: it is about to stagger. */
    public static final Event<StaggerMoment> STAGGER = new Event<>();
    /** A boss's stagger is about to end. */
    public static final Event<StaggerEndMoment> STAGGER_END = new Event<>();
    /** One of a boss's blows is about to be parried. */
    public static final Event<ParryMoment> PARRY = new Event<>();
    /** A player is about to strike a staggered boss's critical point. */
    public static final Event<CriticalStrikeMoment> CRITICAL_STRIKE = new Event<>();

    private CombatEvents() {
    }
}
