package net.ixdarklord.whycantisleep.between.mob;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A Tick-Tock: a clockwork beetle of brass with a clock's face, a winding key turning in its back. It scuttles up to
 * whoever it finds and starts to tick ({@link #fuse}), faster and faster; get {@link #DEFUSE_AT} blocks away and it
 * winds down, stay and it bursts, its spring letting go all at once: a blast that breaks no blocks, and a slip in time
 * that leaves everyone near it moving and working as if through syrup. Killed before it bursts, it leaves its
 * mainspring and its stopped heart: the Stopwatch, which slips time the other way.
 */
public class TickTockEntity extends Monster {
    private static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(TickTockEntity.class, EntityDataSerializers.INT);
    public static final int FUSE_TICKS = 36;
    private static final double LIGHT_AT = 2.8, DEFUSE_AT = 6.0;

    public TickTockEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 0.0).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FUSE, 0);
    }

    /** How far its spring has wound towards bursting, in ticks; 0 while it is calm. */
    public int fuse() {
        return this.entityData.get(FUSE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) return;
        LivingEntity target = this.getTarget();
        int fuse = this.fuse();
        if (target != null && target.isAlive() && (fuse > 0 ? this.distanceTo(target) < DEFUSE_AT : this.distanceTo(target) < LIGHT_AT)) {
            fuse++;
            // Ticking, faster as it winds.
            int every = fuse < FUSE_TICKS / 2 ? 6 : fuse < FUSE_TICKS * 3 / 4 ? 3 : 1;
            if (fuse % every == 0) {
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.COMPARATOR_CLICK, SoundSource.HOSTILE, 1.0F,
                        1.0F + fuse / (float) FUSE_TICKS);
            }
            if (fuse >= FUSE_TICKS) {
                this.burst(level);
                return;
            }
        } else if (fuse > 0) {
            fuse = Math.max(0, fuse - 2);                                       // winding down
        }
        this.entityData.set(FUSE, fuse);
    }

    /** Its spring lets go: a blast that breaks nothing, and time slipping for everyone near. */
    private void burst(ServerLevel level) {
        level.explode(this, this.getX(), this.getY() + 0.3, this.getZ(), 1.6F, Level.ExplosionInteraction.NONE);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.5F, 0.5F);
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(5.0))) {
            if (near == this) continue;
            near.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1), this);
            near.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 100, 1), this);
        }
        this.discard();
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        return false;                                                           // it only bursts
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.COMPARATOR_CLICK;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 2.0F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }
}
