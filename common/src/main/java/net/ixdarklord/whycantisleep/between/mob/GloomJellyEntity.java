package net.ixdarklord.whycantisleep.between.mob;

import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * A Gloom Jelly: a jellyfish of the void, drifting slowly in the open air between and under the islands, its bell
 * pulsing with a light inside. It means no harm and hunts nothing, but its trailing strands sting whatever brushes
 * them (poison, and a little hurt); struck, it squirts a cloud of dark ink that blinds whoever is near and drifts off
 * quicker. Its gel keeps its lightness: Gloom Gel, taken, lets its taker fall as softly as it drifts.
 */
public class GloomJellyEntity extends PathfinderMob {
    private int sting;
    private int fleeing;
    private @Nullable Vec3 away;

    public GloomJellyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 5, true);
        this.setNoGravity(true);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.FLYING_SPEED, 0.12)
                .add(Attributes.MOVEMENT_SPEED, 0.1).add(Attributes.FOLLOW_RANGE, 16.0);
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
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.random.nextInt(5) == 0) {
                this.level().addParticle(ParticleTypes.GLOW, this.getRandomX(0.6), this.getY() - this.random.nextDouble() * 0.8, this.getRandomZ(0.6),
                        0.0, -0.01, 0.0);
            }
            return;
        }
        if (this.fleeing > 0) this.fleeing--;
        // Its strands, hanging under its bell, sting what they touch.
        if (++this.sting % 10 != 0 || !(this.level() instanceof ServerLevel level)) return;
        AABB strands = this.getBoundingBox().expandTowards(0.0, -1.0, 0.0).inflate(0.15, 0.0, 0.15);
        for (LivingEntity touched : level.getEntitiesOfClass(LivingEntity.class, strands, e -> !(e instanceof GloomJellyEntity))) {
            if (touched instanceof Player player && (player.isCreative() || player.isSpectator())) continue;
            if (touched.hurtServer(level, this.damageSources().mobAttack(this), 2.0F)) {
                touched.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), this);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        Entity by = source.getEntity();
        if (hurt && this.isAlive() && by != null) {
            // A cloud of ink, and away.
            level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 0.5, this.getZ(), 30, 0.8, 0.8, 0.8, 0.05);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLOW_SQUID_SQUIRT, SoundSource.NEUTRAL, 1.0F, 0.8F);
            for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(4.0))) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0), this);
            }
            Vec3 from = this.position().subtract(by.position()).normalize();
            this.away = this.position().add(from.scale(10.0)).add(0.0, 2.0, 0.0);
            this.fleeing = 60;
        }
        return hurt;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GLOW_SQUID_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GLOW_SQUID_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GLOW_SQUID_DEATH;
    }

    // Drifts slowly about where it is; fleeing, makes for the point away from what struck it.
    private final class DriftGoal extends Goal {
        private @Nullable Vec3 drift;

        DriftGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public void tick() {
            GloomJellyEntity jelly = GloomJellyEntity.this;
            if (jelly.fleeing > 0 && jelly.away != null) {
                jelly.getMoveControl().setWantedPosition(jelly.away.x, jelly.away.y, jelly.away.z, 3.0);
                return;
            }
            if (this.drift == null || jelly.position().distanceToSqr(this.drift) < 1.0 || jelly.random.nextInt(200) == 0) {
                this.drift = jelly.position().add((jelly.random.nextDouble() - 0.5) * 12.0, (jelly.random.nextDouble() - 0.5) * 4.0,
                        (jelly.random.nextDouble() - 0.5) * 12.0);
            }
            jelly.getMoveControl().setWantedPosition(this.drift.x, this.drift.y, this.drift.z, 1.0);
        }
    }
}
