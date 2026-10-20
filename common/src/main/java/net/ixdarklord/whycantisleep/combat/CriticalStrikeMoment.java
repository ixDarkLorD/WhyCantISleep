package net.ixdarklord.whycantisleep.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * A critical strike: a player's blow on a staggered boss's open critical point. Listeners and the boss's hook may
 * change its damage (the blow's, times {@link #multiplier}, plus {@link #bonus}), whether it ends the stagger, the
 * animation the striker plays (a clip of the player's armature, on its {@code combat} layer; null for none), or cancel
 * it, and the blow lands as an ordinary one.
 */
public final class CriticalStrikeMoment extends CombatMoment {
    private final Player striker;
    private final float blow;
    private final CriticalPoint point;
    private float multiplier;
    private float bonus;
    private boolean endsStagger = true;
    private @Nullable String strikerClip;

    public CriticalStrikeMoment(Mob boss, BossCombat combat, ServerLevel level, Player striker, float blow, CriticalPoint point,
                                float multiplier, float bonus, @Nullable String strikerClip) {
        super(boss, combat, level);
        this.striker = striker;
        this.blow = blow;
        this.point = point;
        this.multiplier = multiplier;
        this.bonus = bonus;
        this.strikerClip = strikerClip;
    }

    public Player striker() {
        return this.striker;
    }

    /** The blow's own damage (with the stagger's multiplier). */
    public float blow() {
        return this.blow;
    }

    /** The point struck. */
    public CriticalPoint point() {
        return this.point;
    }

    public float multiplier() {
        return this.multiplier;
    }

    public CriticalStrikeMoment multiplier(float multiplier) {
        this.multiplier = Math.max(0.0F, multiplier);
        return this;
    }

    public float bonus() {
        return this.bonus;
    }

    public CriticalStrikeMoment bonus(float bonus) {
        this.bonus = Math.max(0.0F, bonus);
        return this;
    }

    /** The damage it does. */
    public float damage() {
        return this.blow * this.multiplier + this.bonus;
    }

    /** Whether it ends the stagger (the boss reels out of it). */
    public boolean endsStagger() {
        return this.endsStagger;
    }

    public CriticalStrikeMoment endsStagger(boolean ends) {
        this.endsStagger = ends;
        return this;
    }

    /** The clip the striker plays, of the player's armature (on its {@code combat} layer), or null for none. */
    public @Nullable String strikerClip() {
        return this.strikerClip;
    }

    public CriticalStrikeMoment strikerClip(@Nullable String clip) {
        this.strikerClip = clip;
        return this;
    }
}
