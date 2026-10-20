package net.ixdarklord.whycantisleep.between.mob;

import net.minecraft.core.particles.DustParticleOptions;
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
 * A Gazer: an eye as big as a head hung in the air, tendrils trailing under it. It is a sniper: it hangs off at
 * {@link #RANGE} blocks and fixes its stare on someone, a thread of red light running from its eye to theirs for two
 * seconds ({@link #charge}); break its line of sight in that time and the stare is lost, hold still and it lands, a
 * searing that leaves them sick and weak. Its lens, kept, still sees: the Gazer's Lens lends sight in the dark.
 */
public class GazerEntity extends Monster {
    private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(GazerEntity.class, EntityDataSerializers.INT);
    public static final int CHARGE_TICKS = 40;
    private static final double RANGE = 10.0;
    private static final DustParticleOptions STARE = new DustParticleOptions(0xFF2D46, 0.8F);

    public GazerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.FLYING_SPEED, 0.45)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGE, 0);
    }

    /** How far through fixing its stare it is, in ticks; 0 when it isn't. */
    public int charge() {
        return this.entityData.get(CHARGE);
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
        this.goalSelector.addGoal(1, new StareGoal());
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GUARDIAN_AMBIENT_LAND;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GUARDIAN_HURT_LAND;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GUARDIAN_DEATH_LAND;
    }

    // Hangs at RANGE from its target, above it, drifting round; when ready, fixes its stare and holds it, then sears.
    private final class StareGoal extends Goal {
        private int cooldown = 40;
        private float angle;

        StareGoal() {
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
            GazerEntity gazer = GazerEntity.this;
            LivingEntity target = gazer.getTarget();
            int charge = gazer.charge();
            if (target == null || !target.isAlive()) {
                gazer.entityData.set(CHARGE, 0);
                this.angle += 0.01F;
                Vec3 at = gazer.position().add(Math.cos(this.angle) * 2.0, Math.sin(this.angle * 3.0) * 0.3, Math.sin(this.angle) * 2.0);
                gazer.getMoveControl().setWantedPosition(at.x, at.y, at.z, 0.3);
                return;
            }
            gazer.getLookControl().setLookAt(target, 40.0F, 40.0F);
            if (charge > 0) {
                // Holding its stare: still, a thread of red to its target's eyes.
                gazer.getMoveControl().setWantedPosition(gazer.getX(), gazer.getY(), gazer.getZ(), 0.1);
                if (!gazer.hasLineOfSight(target) || gazer.distanceTo(target) > RANGE * 2.0) {
                    gazer.entityData.set(CHARGE, 0);
                    this.cooldown = 30;
                    return;
                }
                if (gazer.level() instanceof ServerLevel level && charge % 2 == 0) {
                    Vec3 from = gazer.getEyePosition(), to = target.getEyePosition();
                    int dots = (int) Math.max(4, from.distanceTo(to) * 2.0);
                    for (int i = 0; i < dots; i++) {
                        Vec3 at = from.lerp(to, i / (double) dots);
                        level.sendParticles(STARE, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                }
                if (charge + 1 >= CHARGE_TICKS) {
                    this.sear(target);
                    gazer.entityData.set(CHARGE, 0);
                    this.cooldown = 60 + gazer.random.nextInt(40);
                } else {
                    gazer.entityData.set(CHARGE, charge + 1);
                }
                return;
            }
            this.angle += 0.02F;
            Vec3 at = target.position().add(Math.cos(this.angle) * RANGE, 3.0 + Math.sin(this.angle * 2.0), Math.sin(this.angle) * RANGE);
            gazer.getMoveControl().setWantedPosition(at.x, at.y, at.z, 0.7);
            if (this.cooldown > 0) {
                this.cooldown--;
            } else if (gazer.hasLineOfSight(target) && gazer.distanceTo(target) < RANGE * 1.6) {
                gazer.entityData.set(CHARGE, 1);
                gazer.level().playSound(null, gazer.getX(), gazer.getY(), gazer.getZ(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 1.0F, 1.2F);
            }
        }

        private void sear(LivingEntity target) {
            GazerEntity gazer = GazerEntity.this;
            if (!(gazer.level() instanceof ServerLevel level)) return;
            target.hurtServer(level, gazer.damageSources().indirectMagic(gazer, gazer), 6.0F);
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0), gazer);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), gazer);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.5F, 1.6F);
        }
    }
}
