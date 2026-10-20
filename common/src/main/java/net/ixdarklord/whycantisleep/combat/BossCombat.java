package net.ixdarklord.whycantisleep.combat;

import net.ixdarklord.coolcatcanvas.api.armature.ArmatureControl;
import net.ixdarklord.coolcatcanvas.api.armature.Motion;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A boss's side of a souls-like fight: its <b>posture</b>, worn down by blows and most of all by its own blows parried,
 * steadying again when it is left alone; its guard <b>breaking</b> when its posture runs out, so it <b>staggers</b>,
 * reeling and hurt harder for a while; its blows <b>parried</b> by a shield raised in time; and, while it staggers, its
 * weak spot open to a <b>critical strike</b> that plays an animation on the striker. Each of those is a
 * {@link CombatMoment} that listeners ({@link CombatEvents}) and the boss's own hooks ({@link CombatBoss}) may change
 * or cancel. One per boss entity; the boss calls it from its own methods:
 * <pre>{@code
 * private final BossCombat combat = new BossCombat(this, PROFILE, COMBAT_DATA).criticalClip("heart_strike");
 *
 * protected void customServerAiStep(ServerLevel level) {
 *     this.combat.tick(level);
 *     if (this.combat.isStaggered()) return;             // reeling: no attacks
 *     ...
 * }
 *
 * // Its own blows, through the parry check:
 * if (this.combat.strike(level, player, SCYTHE, this.damageSources().mobAttack(this), damage).landed()) player.knockback(...);
 *
 * public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
 *     float dealt = this.combat.incoming(level, source, damage);   // a stagger's extra harm, a critical strike
 *     boolean hurt = super.hurtServer(level, source, dealt);
 *     this.combat.landed(level, source, dealt, hurt);              // posture worn down; a critical strike's aftermath
 *     return hurt;
 * }
 * }</pre>
 */
public final class BossCombat {
    /** The layer a critical strike's animation plays on, on the striker. */
    public static final String STRIKER_LAYER = "combat";

    private final Mob owner;
    private final CombatBoss boss;
    private final CombatProfile profile;
    private final CombatData data;
    private @Nullable String criticalClip = "critical_strike";
    private float posture;
    private int lastPressed = -10000;
    private int staggerTicks;
    private float staggerDamage = 1.0F;
    private boolean exposes;
    // A critical strike worked out on a blow's way in, acted on once the blow lands.
    private @Nullable CriticalStrikeMoment pending;

    public BossCombat(Mob owner, CombatProfile profile, CombatData data) {
        if (!(owner instanceof CombatBoss boss)) throw new IllegalArgumentException(owner + " must implement CombatBoss");
        this.owner = owner;
        this.boss = boss;
        this.profile = profile;
        this.data = data;
    }

    /** The clip a critical strike plays on the striker (of the player's armature), or null for none; "critical_strike" by default. */
    public BossCombat criticalClip(@Nullable String clip) {
        this.criticalClip = clip;
        return this;
    }

    public CombatProfile profile() {
        return this.profile;
    }

    /** Its posture, 0 (steady) to its profile's max (broken). */
    public float posture() {
        return this.posture;
    }

    public boolean isStaggered() {
        return this.staggerTicks > 0;
    }

    /** How much longer it staggers, in ticks. */
    public int staggerTicks() {
        return this.staggerTicks;
    }

    // ---- Each tick ----

    /** Its stagger running out, its posture steadying. Call each server tick. */
    public void tick(ServerLevel level) {
        if (this.staggerTicks > 0) {
            if (--this.staggerTicks == 0) this.end(level, true);
        } else if (this.posture > 0.0F && this.owner.tickCount - this.lastPressed > this.profile.regenDelay()) {
            this.posture = Math.max(0.0F, this.posture - this.profile.regenPerTick());
        }
        this.sync();
    }

    // ---- Its blows ----

    /**
     * One of its blows on {@code target}: parried if the attack can be and the target raised a shield in time (it
     * takes nothing, and this boss's posture takes the shock), otherwise dealt as {@code source} and {@code damage}.
     */
    public StrikeResult strike(ServerLevel level, LivingEntity target, BossAttack attack, DamageSource source, float damage) {
        if (attack.parryable() && Parry.parries(target, this.owner, this.profile.parryWindow())) {
            ParryMoment moment = new ParryMoment(this.owner, this, level, target, attack, damage,
                    this.profile.parryPosture() * attack.parryPostureScale());
            CombatEvents.PARRY.invoke(moment);
            if (!moment.cancelled()) this.boss.onParried(moment);
            if (!moment.cancelled()) {
                this.parryEffects(level, target);
                this.press(level, moment.posture(), StaggerCause.PARRIED, target);
                return StrikeResult.PARRIED;
            }
        }
        return target.hurtServer(level, source, damage) ? StrikeResult.HIT : StrikeResult.MISSED;
    }

    // ---- Blows on it ----

    /**
     * A blow on its way in: the damage it should do, more while it staggers, and a critical strike's if it strikes
     * the open critical point. Call before applying the blow, and {@link #landed} after.
     */
    public float incoming(ServerLevel level, DamageSource source, float damage) {
        this.pending = null;
        if (!this.isStaggered()) return damage;
        float blow = damage * this.staggerDamage;
        if (this.exposes && source.getEntity() instanceof Player player && source.getDirectEntity() == player) {
            CriticalPoint point = this.boss.criticalPoint();
            if (point != null && point.struckBy(player, this.owner, this.profile.criticalReach())) {
                CriticalStrikeMoment moment = new CriticalStrikeMoment(this.owner, this, level, player, blow, point,
                        this.profile.criticalMultiplier(), this.profile.criticalBonus(), this.criticalClip);
                CombatEvents.CRITICAL_STRIKE.invoke(moment);
                if (!moment.cancelled()) {
                    this.pending = moment;
                    return moment.damage();
                }
            }
        }
        return blow;
    }

    /** A blow having landed ({@code hurt}) or not: its posture worn down, or a critical strike's aftermath. */
    public void landed(ServerLevel level, DamageSource source, float dealt, boolean hurt) {
        CriticalStrikeMoment critical = this.pending;
        this.pending = null;
        if (!hurt) return;
        if (critical != null) {
            this.boss.onCriticalStrike(critical);
            this.criticalEffects(level, critical);
            if (critical.endsStagger() && this.isStaggered()) this.end(level, false);
            return;
        }
        if (!this.isStaggered()) {
            LivingEntity by = source.getEntity() instanceof LivingEntity living ? living : null;
            this.press(level, dealt * this.profile.hitPosture(), StaggerCause.POSTURE_BROKEN, by);
        }
    }

    // ---- Posture and stagger ----

    /** Wears its posture down by {@code amount}; if that breaks it, it staggers. */
    public void press(ServerLevel level, float amount, StaggerCause cause, @Nullable LivingEntity by) {
        if (this.isStaggered() || amount <= 0.0F) return;
        this.posture = Math.min(this.profile.maxPosture(), this.posture + amount);
        this.lastPressed = this.owner.tickCount;
        if (this.posture >= this.profile.maxPosture()) this.stagger(level, cause, by);
        this.sync();
    }

    /** Breaks its guard now: it staggers, as its profile says unless the moment is changed. */
    public void stagger(ServerLevel level, StaggerCause cause, @Nullable LivingEntity by) {
        if (this.isStaggered()) return;
        StaggerMoment moment = new StaggerMoment(this.owner, this, level, cause, by, this.profile.staggerTicks(), this.profile.staggerDamage());
        CombatEvents.STAGGER.invoke(moment);
        if (!moment.cancelled()) this.boss.onStagger(moment);
        if (moment.cancelled()) {
            // It held: shaken, but still standing.
            this.posture = this.profile.maxPosture() * 0.75F;
            return;
        }
        this.staggerTicks = moment.duration();
        this.staggerDamage = moment.damageMultiplier();
        this.exposes = moment.exposesCritical();
        this.posture = this.profile.maxPosture();
        this.owner.getNavigation().stop();
        if (moment.effects()) this.breakEffects(level);
        this.sync();
    }

    private void end(ServerLevel level, boolean recovered) {
        StaggerEndMoment moment = new StaggerEndMoment(this.owner, this, level, recovered);
        CombatEvents.STAGGER_END.invoke(moment);
        if (!moment.cancelled()) this.boss.onStaggerEnd(moment);
        if (moment.cancelled() && recovered) {
            this.staggerTicks = moment.extend();
            return;
        }
        this.staggerTicks = 0;
        this.exposes = false;
        this.staggerDamage = 1.0F;
        this.posture = 0.0F;
        this.lastPressed = this.owner.tickCount;
        this.sync();
    }

    private void sync() {
        this.owner.getEntityData().set(this.data.posture(), this.posture / this.profile.maxPosture());
        this.owner.getEntityData().set(this.data.staggered(), this.isStaggered());
    }

    // ---- What is seen and heard ----

    private void parryEffects(ServerLevel level, LivingEntity defender) {
        Vec3 at = defender.getEyePosition().add(this.owner.getEyePosition()).scale(0.5).lerp(defender.getEyePosition(), 0.6);
        level.playSound(null, defender.getX(), defender.getY(), defender.getZ(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 1.4F, 1.5F);
        level.playSound(null, defender.getX(), defender.getY(), defender.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5F, 1.9F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 18, 0.15, 0.15, 0.15, 0.35);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 10, 0.1, 0.1, 0.1, 0.4);
        if (defender instanceof ServerPlayer player) player.sendOverlayMessage(Component.translatable("message.whycantisleep.parry"));
    }

    private void breakEffects(ServerLevel level) {
        Vec3 at = this.owner.position().add(0.0, this.owner.getBbHeight() * 0.6, 0.0);
        level.playSound(null, this.owner.blockPosition(), SoundEvents.SHIELD_BREAK.value(), SoundSource.HOSTILE, 2.0F, 0.5F);
        level.playSound(null, this.owner.blockPosition(), SoundEvents.ANVIL_DESTROY, SoundSource.HOSTILE, 0.8F, 0.6F);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, at.x, at.y, at.z, 40, 0.6, 0.8, 0.6, 0.4);
        Component word = Component.translatable("message.whycantisleep.stagger", this.owner.getDisplayName());
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, this.owner.getBoundingBox().inflate(24.0))) {
            player.sendOverlayMessage(word);
        }
    }

    private void criticalEffects(ServerLevel level, CriticalStrikeMoment moment) {
        Vec3 at = moment.point().at();
        Player striker = moment.striker();
        level.playSound(null, striker.getX(), striker.getY(), striker.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.6F, 0.6F);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 30, 0.2, 0.2, 0.2, 0.5);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, at.x, at.y, at.z, 12, 0.2, 0.2, 0.2, 0.2);
        if (moment.strikerClip() != null) {
            ArmatureControl.play(striker, STRIKER_LAYER, Motion.once(moment.strikerClip()).fade(0.08F).restart());
        }
        if (striker instanceof ServerPlayer player) player.sendOverlayMessage(Component.translatable("message.whycantisleep.critical"));
    }
}
