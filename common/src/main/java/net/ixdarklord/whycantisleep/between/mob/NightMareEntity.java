package net.ixdarklord.whycantisleep.between.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A Night Mare: a horse of shadow with a mane, tail and hooves of violet flame, roaming the islands. It is a charger:
 * spotting someone between {@link #CHARGE_FROM} and {@link #CHARGE_TO} blocks off, it paws the ground for a second
 * ({@link #isWindingUp}) and then gallops straight at where they stood ({@link #isCharging}), unable to turn; what it
 * runs down is thrown far, and if it runs into a wall instead it stands dazed for a moment, open to blows. Close in, it
 * rears and kicks. It drops Mare's Embers, which lend their swiftness to whoever breathes them in.
 */
public class NightMareEntity extends Monster {
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(NightMareEntity.class, EntityDataSerializers.INT);
    private static final int CALM = 0, WINDING = 1, CHARGING = 2, DAZED = 3;
    private static final double CHARGE_FROM = 5.0, CHARGE_TO = 22.0, CHARGE_SPEED = 0.8;
    private static final int WIND_TICKS = 24, CHARGE_TICKS = 35, DAZE_TICKS = 50;

    public NightMareEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 14;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, CALM);
    }

    public boolean isWindingUp() {
        return this.entityData.get(STATE) == WINDING;
    }

    public boolean isCharging() {
        return this.entityData.get(STATE) == CHARGING;
    }

    public boolean isDazed() {
        return this.entityData.get(STATE) == DAZED;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ChargeGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) return;
        // Its hooves smoulder; galloping, they throw sparks of violet behind it.
        if (this.isCharging() || this.random.nextInt(6) == 0) {
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL, this.getRandomX(0.6), this.getY() + 0.1, this.getRandomZ(0.6), 0.0, 0.02, 0.0);
        }
        if (this.isDazed() && this.tickCount % 4 == 0) {
            this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY() + 2.1, this.getZ(), 0.0, 0.05, 0.0);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_HORSE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HORSE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_HORSE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }

    // Wind up, gallop straight along the line it chose, and either strike or crash.
    private final class ChargeGoal extends Goal {
        private int timer;
        private int cooldown = 40;
        private Vec3 line = Vec3.ZERO;

        ChargeGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            NightMareEntity mare = NightMareEntity.this;
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            LivingEntity target = mare.getTarget();
            if (target == null || !target.isAlive() || !mare.onGround() || !mare.hasLineOfSight(target)) return false;
            double d = mare.distanceTo(target);
            return d >= CHARGE_FROM && d <= CHARGE_TO;
        }

        @Override
        public boolean canContinueToUse() {
            return NightMareEntity.this.entityData.get(STATE) != CALM;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            NightMareEntity mare = NightMareEntity.this;
            mare.getNavigation().stop();
            mare.entityData.set(STATE, WINDING);
            this.timer = WIND_TICKS;
            mare.level().playSound(null, mare.getX(), mare.getY(), mare.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0F, 1.5F);
        }

        @Override
        public void stop() {
            NightMareEntity.this.entityData.set(STATE, CALM);
            this.cooldown = 80 + NightMareEntity.this.random.nextInt(60);
        }

        @Override
        public void tick() {
            NightMareEntity mare = NightMareEntity.this;
            LivingEntity target = mare.getTarget();
            int state = mare.entityData.get(STATE);
            if (state == WINDING) {
                if (target != null) mare.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (--this.timer > 0) return;
                Vec3 to = target != null ? target.position().subtract(mare.position()) : mare.getLookAngle();
                this.line = new Vec3(to.x, 0.0, to.z).normalize();
                mare.entityData.set(STATE, CHARGING);
                this.timer = CHARGE_TICKS;
                return;
            }
            if (state == CHARGING) {
                float yaw = (float) (Mth.atan2(this.line.z, this.line.x) * Mth.RAD_TO_DEG) - 90.0F;
                mare.setYRot(yaw);
                mare.yBodyRot = yaw;
                mare.yHeadRot = yaw;
                Vec3 v = mare.getDeltaMovement();
                mare.setDeltaMovement(this.line.x * CHARGE_SPEED, v.y, this.line.z * CHARGE_SPEED);
                if (target != null && mare.getBoundingBox().inflate(0.4).intersects(target.getBoundingBox()) && mare.level() instanceof ServerLevel level) {
                    if (mare.doHurtTarget(level, target)) {
                        target.push(this.line.x * 1.8, 0.6, this.line.z * 1.8);
                        target.hurtMarked = true;
                    }
                    mare.entityData.set(STATE, CALM);
                    return;
                }
                if (mare.horizontalCollision) {
                    // It ran into something: it stands dazed.
                    mare.level().playSound(null, mare.getX(), mare.getY(), mare.getZ(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 1.0F, 0.6F);
                    mare.setDeltaMovement(this.line.scale(-0.3).add(0.0, 0.2, 0.0));
                    mare.entityData.set(STATE, DAZED);
                    this.timer = DAZE_TICKS;
                    return;
                }
                if (--this.timer <= 0) mare.entityData.set(STATE, CALM);
                return;
            }
            if (state == DAZED) {
                mare.getNavigation().stop();
                if (--this.timer <= 0) mare.entityData.set(STATE, CALM);
            }
        }
    }
}
