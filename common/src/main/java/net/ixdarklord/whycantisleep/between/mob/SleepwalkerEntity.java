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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Sleepwalker: a dreamer lost in the Dark Between, in a tattered nightgown, eyes shut, shuffling over the islands
 * with its arms out before it. It harms no one while it sleeps; but come too near it (unless creeping) or strike it,
 * and it wakes ({@link #isAwake}): its eyes open red, it shrieks, and it rushes whoever woke it, its touch leaving them
 * reeling. It never sleeps again.
 */
public class SleepwalkerEntity extends Monster {
    private static final EntityDataAccessor<Boolean> AWAKE = SynchedEntityData.defineId(SleepwalkerEntity.class, EntityDataSerializers.BOOLEAN);
    /** How near a player may come before it wakes (creeping, they may pass closer). */
    private static final double WAKE_REACH = 4.5, WAKE_REACH_CREEPING = 1.5;
    private static final double SHUFFLE = 0.16, RUSH = 0.34;

    public SleepwalkerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 22.0).add(Attributes.MOVEMENT_SPEED, SHUFFLE)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AWAKE, false);
    }

    public boolean isAwake() {
        return this.entityData.get(AWAKE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false) {
            @Override
            public boolean canUse() {
                return SleepwalkerEntity.this.isAwake() && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level) || this.isAwake() || this.tickCount % 5 != 0) return;
        Player near = level.getNearestPlayer(this, WAKE_REACH);
        if (near != null && !near.isCreative() && !near.isSpectator()
                && (!near.isShiftKeyDown() || this.distanceTo(near) < WAKE_REACH_CREEPING)) {
            this.wake(near);
        }
    }

    /** It wakes: eyes open, a shriek, and it rushes whoever woke it. */
    public void wake(LivingEntity by) {
        if (this.isAwake()) return;
        this.entityData.set(AWAKE, true);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(RUSH);
        this.setTarget(by);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.2F, 0.7F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GHAST_HURT, SoundSource.HOSTILE, 0.6F, 1.3F);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity by) this.wake(by);
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0), this);   // its touch leaves them reeling
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0), this);
        }
        return hit;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Awake", this.isAwake());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        if (input.getBooleanOr("Awake", false)) {
            this.entityData.set(AWAKE, true);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(RUSH);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isAwake() ? SoundEvents.HUSK_AMBIENT : SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;   // a murmur in its sleep
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HUSK_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HUSK_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return this.isAwake() ? 0.8F : 0.45F;
    }

    @Override
    protected float getSoundVolume() {
        return this.isAwake() ? 1.0F : 0.4F;
    }
}
