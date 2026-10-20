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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A Hollow Hound: a wolf of smoke with its ribs showing through. They run in packs. The first to catch a scent howls,
 * and every hound within {@link #PACK_CALL} blocks takes up the hunt; hurt one and the whole pack turns. Each alone is
 * weak, but they leap and harry, and their bite chills to the bone (a moment's slowness). Their fangs, kept, can still
 * smell out whatever hunts in the dark: the Hollow Fang.
 */
public class HollowHoundEntity extends Monster {
    private static final double PACK_CALL = 24.0;

    public HollowHoundEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, HollowHoundEntity.class).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        LivingEntity had = this.getTarget();
        super.setTarget(target);
        // The first to catch the scent howls, and the pack comes.
        if (target != null && had == null && this.level() instanceof ServerLevel level) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FOX_SCREECH, SoundSource.HOSTILE, 1.4F, 0.55F);
            for (HollowHoundEntity other : level.getEntitiesOfClass(HollowHoundEntity.class, this.getBoundingBox().inflate(PACK_CALL))) {
                if (other != this && other.getTarget() == null) other.setTargetQuietly(target);
            }
        }
    }

    private void setTargetQuietly(LivingEntity target) {
        super.setTarget(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 0.0, 0.02, 0.0);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 0), this);
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.getTarget() != null ? SoundEvents.FOX_AGGRO : SoundEvents.FOX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }
}
