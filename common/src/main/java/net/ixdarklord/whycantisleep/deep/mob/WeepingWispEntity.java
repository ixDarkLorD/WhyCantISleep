package net.ixdarklord.whycantisleep.deep.mob;

import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The Weeping Wisp: a tear of the garden that never fell, drifting through the air, trailing veins. It drifts toward
 * the living and, close enough, bursts into a spray of blood tears that blinds and weakens whoever is caught in it,
 * and is gone.
 */
public class WeepingWispEntity extends Monster {
    private static final float BURST_REACH = 2.2F;

    public WeepingWispEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.FLYING_SPEED, 0.45)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 20.0);
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
        this.goalSelector.addGoal(1, new DriftAndBurstGoal());
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    /** It bursts: blindness and weakness for those near it, a spray of red, and it is gone. */
    public void burst(ServerLevel level) {
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(BURST_REACH + 0.8),
                e -> e != this && !(e instanceof WeepingWispEntity))) {
            near.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 70, 0), this);
            near.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 0), this);
        }
        level.sendParticles(new DustParticleOptions(0xB01030, 1.6F), this.getX(), this.getY() + 0.4, this.getZ(), 60, 0.6, 0.5, 0.6, 0.1);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.2F, 0.6F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ALLAY_DEATH, SoundSource.HOSTILE, 0.8F, 0.5F);
        this.discard();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }

    // Drifts toward its target (or idly about), and bursts when close.
    private final class DriftAndBurstGoal extends Goal {
        DriftAndBurstGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
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
            WeepingWispEntity wisp = WeepingWispEntity.this;
            LivingEntity target = wisp.getTarget();
            if (target != null && target.isAlive()) {
                if (wisp.distanceTo(target) < BURST_REACH && wisp.level() instanceof ServerLevel level) {
                    wisp.burst(level);
                    return;
                }
                Vec3 to = target.getEyePosition().add(0.0, -0.3, 0.0);
                wisp.getMoveControl().setWantedPosition(to.x, to.y, to.z, 1.0);
            } else if (wisp.getRandom().nextInt(60) == 0) {
                Vec3 to = wisp.position().add((wisp.getRandom().nextDouble() - 0.5) * 8, (wisp.getRandom().nextDouble() - 0.4) * 3,
                        (wisp.getRandom().nextDouble() - 0.5) * 8);
                wisp.getMoveControl().setWantedPosition(to.x, to.y, to.z, 0.6);
            }
        }
    }
}
