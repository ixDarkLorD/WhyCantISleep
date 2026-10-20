package net.ixdarklord.whycantisleep.between.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
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
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * The Balloon Clown: carried up off its feet by a bunch of five balloons, it drifts after whoever it finds, just out
 * of reach, giggling. Its trick: it ties a balloon to them. Up they go ({@link #LIFT_TICKS}), off the ground, off the
 * island's edge if that is where they stood; then it pops the balloon, and down they come.
 */
public class BalloonClownEntity extends Monster {
    private static final int LIFT_TICKS = 50;
    private int trick = 100;

    public BalloonClownEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 22.0).add(Attributes.FLYING_SPEED, 0.3)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 28.0);
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
        this.goalSelector.addGoal(1, new DriftGoal());
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level) || --this.trick > 0) return;
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.distanceTo(target) > 10.0F || !this.hasLineOfSight(target)
                || target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            this.trick = 20;
            return;
        }
        this.tie(level, target);
        this.trick = 260 + this.random.nextInt(120);
    }

    /** A balloon tied on: up they go, and then it pops. */
    private void tie(ServerLevel level, LivingEntity target) {
        this.swing(InteractionHand.MAIN_HAND);
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, LIFT_TICKS, 1), this);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, target.getX(), target.getY() + 2.2, target.getZ(), 16, 0.3, 0.3, 0.3, 0.1);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 1.0F, 1.6F);
        if (target instanceof ServerPlayer player) player.sendOverlayMessage(Component.translatable("message.whycantisleep.balloon_tied"));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && this.isAlive()) {
            // A balloon pops.
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 2.6, this.getZ(), 10, 0.4, 0.3, 0.4, 0.2);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 0.8F, 1.6F);
        }
        return hurt;
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
        return 1.6F;
    }

    // Drifts after its target, just out of reach and a little above it; alone, it drifts about where it is.
    private final class DriftGoal extends Goal {
        private float angle;
        private @Nullable Vec3 drift;

        DriftGoal() {
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
            BalloonClownEntity clown = BalloonClownEntity.this;
            LivingEntity target = clown.getTarget();
            if (target != null && target.isAlive()) {
                this.angle += 0.012F;
                Vec3 at = target.position().add(Math.cos(this.angle) * 5.0, 2.5 + Math.sin(this.angle * 3.0) * 0.6, Math.sin(this.angle) * 5.0);
                clown.getMoveControl().setWantedPosition(at.x, at.y, at.z, 0.8);
                clown.getLookControl().setLookAt(target, 20.0F, 20.0F);
                return;
            }
            if (this.drift == null || clown.position().distanceToSqr(this.drift) < 1.0 || clown.random.nextInt(160) == 0) {
                this.drift = clown.position().add((clown.random.nextDouble() - 0.5) * 10.0, (clown.random.nextDouble() - 0.5) * 3.0,
                        (clown.random.nextDouble() - 0.5) * 10.0);
            }
            clown.getMoveControl().setWantedPosition(this.drift.x, this.drift.y, this.drift.z, 0.4);
        }
    }
}
