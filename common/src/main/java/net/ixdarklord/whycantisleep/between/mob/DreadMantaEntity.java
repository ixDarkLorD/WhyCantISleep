package net.ixdarklord.whycantisleep.between.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A Dread Manta: a great ray of shadow that glides in slow wide circles round the islands of the Dark Between. When it
 * marks someone it dives at them, and its blow throws them hard (often off the island's edge) and slows them; then it
 * climbs away and circles again before its next dive.
 */
public class DreadMantaEntity extends Monster {
    private static final double CIRCLE = 12.0;

    public DreadMantaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 26.0).add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new GlideAndDiveGoal());
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PHANTOM_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F;
    }

    // Glides round a point (where it first was) in a wide slow circle; with a target in sight and its dive ready, dives
    // at it, strikes as it passes (a blow that throws hard), and climbs away up and off before circling again.
    private final class GlideAndDiveGoal extends Goal {
        private Vec3 centre;
        private float angle;
        private int cooldown = 60;
        private int climb;

        GlideAndDiveGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            DreadMantaEntity manta = DreadMantaEntity.this;
            if (this.centre == null) this.centre = manta.position();
            LivingEntity target = manta.getTarget();
            if (this.cooldown > 0) this.cooldown--;
            if (this.climb > 0) {
                this.climb--;
                Vec3 away = manta.position().add(manta.getLookAngle().multiply(1, 0, 1).normalize().scale(6.0)).add(0.0, 4.0, 0.0);
                manta.getMoveControl().setWantedPosition(away.x, away.y, away.z, 1.0);
                return;
            }
            if (target != null && target.isAlive() && this.cooldown == 0 && manta.hasLineOfSight(target)) {
                // The dive.
                Vec3 at = target.getEyePosition().subtract(0.0, 0.4, 0.0);
                manta.getMoveControl().setWantedPosition(at.x, at.y, at.z, 1.6);
                manta.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (manta.distanceTo(target) < 2.0F && manta.level() instanceof ServerLevel level) {
                    if (manta.doHurtTarget(level, target)) {
                        Vec3 throwAt = target.position().subtract(manta.position()).multiply(1, 0, 1).normalize();
                        target.push(throwAt.x * 1.3, 0.45, throwAt.z * 1.3);
                        target.hurtMarked = true;
                        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), manta);
                    }
                    level.playSound(null, manta.getX(), manta.getY(), manta.getZ(), SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE, 1.2F, 0.6F);
                    this.centre = target.position().add(0.0, 10.0, 0.0);      // it circles over where it struck
                    this.climb = 30;
                    this.cooldown = 90 + manta.getRandom().nextInt(60);
                }
                return;
            }
            // The glide: round its centre, a little above it.
            this.angle += 0.03F;
            Vec3 at = this.centre.add(Math.cos(this.angle) * CIRCLE, Math.sin(this.angle * 0.5F) * 1.5, Math.sin(this.angle) * CIRCLE);
            manta.getMoveControl().setWantedPosition(at.x, at.y, at.z, 0.7);
        }
    }
}
