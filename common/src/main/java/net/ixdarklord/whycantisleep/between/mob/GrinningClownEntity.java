package net.ixdarklord.whycantisleep.between.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A Grinning Clown (coulrophobia, the fear of clowns): too tall and too thin, in a faded harlequin suit, its face in
 * cracked greasepaint round a grin too wide and a nose that glows. It is a trickster more than a fighter:
 * <ul>
 *     <li>every so often it <b>swaps places</b> with whoever it is after, both of them standing on ground, so they turn
 *     round to find it where they were, giggling ({@link #isLaughing});</li>
 *     <li>badly hurt, it <b>pops</b> in a burst of confetti and stands behind them instead, once in a while;</li>
 *     <li>its mallet <b>bonks</b> them up into the air;</li>
 *     <li>its balloons hold it up: it never falls hard, only drifts down.</li>
 * </ul>
 * It drops the Swap Card, which plays its trick for whoever holds it.
 */
public class GrinningClownEntity extends Monster {
    private static final EntityDataAccessor<Integer> LAUGH = SynchedEntityData.defineId(GrinningClownEntity.class, EntityDataSerializers.INT);
    private static final double SWAP_FROM = 4.0, SWAP_TO = 20.0;
    private int swapCooldown = 100;
    private int popCooldown;

    public GrinningClownEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LAUGH, 0);
    }

    /** Whether it is laughing at its own trick. */
    public boolean isLaughing() {
        return this.entityData.get(LAUGH) > 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false) {
            @Override
            public boolean canUse() {
                return !GrinningClownEntity.this.isLaughing() && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        // Its balloons hold it up: it drifts down rather than falls.
        Vec3 v = this.getDeltaMovement();
        if (!this.onGround() && v.y < -0.12) this.setDeltaMovement(v.x, -0.12, v.z);
        if (!(this.level() instanceof ServerLevel level)) return;
        int laugh = this.entityData.get(LAUGH);
        if (laugh > 0) this.entityData.set(LAUGH, laugh - 1);
        if (this.popCooldown > 0) this.popCooldown--;
        if (this.swapCooldown > 0) {
            this.swapCooldown--;
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.onGround() || !target.onGround() || !this.hasLineOfSight(target)) return;
        double d = this.distanceTo(target);
        if (d < SWAP_FROM || d > SWAP_TO) return;
        this.swap(level, target);
        this.swapCooldown = 160 + this.random.nextInt(140);
    }

    /** The trick: it and its target change places. */
    private void swap(ServerLevel level, LivingEntity target) {
        Vec3 mine = this.position(), theirs = target.position();
        confetti(level, mine);
        confetti(level, theirs);
        this.teleportTo(theirs.x, theirs.y, theirs.z);
        target.teleportTo(mine.x, mine.y, mine.z);
        target.hurtMarked = true;
        level.playSound(null, mine.x, mine.y, mine.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.5F);
        this.giggle(level);
    }

    /** Badly hurt, now and then it pops and stands behind whoever it is after. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        LivingEntity target = this.getTarget();
        if (hurt && this.isAlive() && this.popCooldown == 0 && this.getHealth() < this.getMaxHealth() * 0.5F && target != null) {
            Vec3 behind = target.position().subtract(target.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(2.5));
            Vec3 was = this.position();
            if (this.randomTeleport(behind.x, target.getY(), behind.z, false)) {
                confetti(level, was);
                level.playSound(null, was.x, was.y, was.z, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.HOSTILE, 1.0F, 1.2F);
                this.giggle(level);
                this.popCooldown = 300;
            }
        }
        return hurt;
    }

    private void giggle(ServerLevel level) {
        this.entityData.set(LAUGH, 30);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 1.2F, 1.5F);
    }

    private static void confetti(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, at.x, at.y + 1.2, at.z, 30, 0.4, 0.6, 0.4, 0.25);
    }

    /** Its mallet: a bonk that pops them up into the air. */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            target.push(0.0, 0.55, 0.0);
            target.hurtMarked = true;
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SLIME_ATTACK, SoundSource.HOSTILE, 1.0F, 1.7F);
        }
        return hit;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITCH_CELEBRATE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITCH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITCH_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.4F;
    }
}
