package net.ixdarklord.whycantisleep.between.mob;

import net.ixdarklord.whycantisleep.between.WildsServer;
import net.ixdarklord.whycantisleep.lurker.WCISEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The Broodmother (arachnophobia): a spider the size of a cart, bristling and black, eight red eyes, its belly hung with
 * egg sacs. It fights with everything it has: a poisoned bite; a spit of silk that pins its prey in cobweb where they
 * stand (the web rots away after a while); and every so often a brood, two or three Broodlings hatched from its sacs to
 * swarm whoever it hunts. Its silk, kept, still holds: Broodsilk, a line to swing along.
 */
public class BroodmotherEntity extends Monster {
    private static final double WEB_RANGE = 14.0;
    private static final int WEB_EVERY = 90, BROOD_EVERY = 140, BROOD_MAX = 6, WEB_LASTS = 200;
    private int web = 60;
    private int brood = 80;

    public BroodmotherEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 24;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 60.0).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) return;
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;
        if (--this.web <= 0 && this.distanceTo(target) < WEB_RANGE && this.hasLineOfSight(target)) {
            this.web = WEB_EVERY;
            this.spit(level, target);
        }
        if (--this.brood <= 0) {
            this.brood = BROOD_EVERY;
            this.hatch(level, target);
        }
    }

    /** Silk, spat at its prey: a web where they stand. */
    private void spit(ServerLevel level, LivingEntity target) {
        this.swing(InteractionHand.MAIN_HAND);
        Vec3 from = this.getEyePosition(), to = target.position().add(0.0, 0.5, 0.0);
        for (int i = 0; i < 12; i++) {
            Vec3 at = from.lerp(to, i / 12.0);
            level.sendParticles(ParticleTypes.WHITE_ASH, at.x, at.y, at.z, 3, 0.05, 0.05, 0.05, 0.0);
        }
        BlockPos feet = target.blockPosition();
        if (level.getBlockState(feet).isAir()) WildsServer.temporary(level, feet, Blocks.COBWEB.defaultBlockState(), WEB_LASTS);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SPIDER_STEP, SoundSource.HOSTILE, 1.2F, 0.6F);
    }

    /** Two or three Broodlings from its sacs, sent after its prey. */
    private void hatch(ServerLevel level, LivingEntity target) {
        if (level.getEntitiesOfClass(BroodlingEntity.class, this.getBoundingBox().inflate(16.0)).size() >= BROOD_MAX) return;
        for (int i = 2 + this.random.nextInt(2); i > 0; i--) {
            BroodlingEntity young = WCISEntities.BROODLING.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (young == null) continue;
            young.snapTo(this.getX() + this.random.nextGaussian() * 0.6, this.getY() + 0.3, this.getZ() + this.random.nextGaussian() * 0.6,
                    this.random.nextFloat() * 360.0F, 0.0F);
            young.setTarget(target);
            level.addFreshEntity(young);
        }
        level.sendParticles(ParticleTypes.ITEM_SLIME, this.getX(), this.getY() + 0.8, this.getZ(), 16, 0.6, 0.3, 0.6, 0.05);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FROGSPAWN_HATCH, SoundSource.HOSTILE, 1.2F, 0.7F);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), this);
        return hit;
    }

    @Override
    public void makeStuckInBlock(net.minecraft.world.level.block.state.BlockState state, Vec3 multiplier) {
        if (!state.is(Blocks.COBWEB)) super.makeStuckInBlock(state, multiplier);      // its own web doesn't hold it
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SPIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SPIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SPIDER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }
}
