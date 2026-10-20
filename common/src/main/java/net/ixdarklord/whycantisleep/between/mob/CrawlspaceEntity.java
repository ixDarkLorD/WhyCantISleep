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
import net.minecraft.world.phys.Vec3;

/**
 * The Crawlspace (claustrophobia): a thing pressed flat, a grey stretched skin with a face too wide in it and long
 * many-jointed limbs splayed out round it. It lies flat on the ground, nearly impossible to see until you are almost on
 * it (WildsClient fades it in as you near it), and fits where nothing else would, under a block's height. What it
 * reaches, it holds ({@link #isHolding}): limbs closed round them, unable to move, the dark pressing in, crushed a little
 * at a time, until {@link #HOLD_TICKS} pass or it is struck hard enough to let go. Its skin, kept, makes its wearer
 * small enough to squeeze where it does: Shrinking Skin.
 */
public class CrawlspaceEntity extends Monster {
    private static final EntityDataAccessor<Integer> HOLDING = SynchedEntityData.defineId(CrawlspaceEntity.class, EntityDataSerializers.INT);
    private static final int HOLD_TICKS = 60;
    private static final float LET_GO_AT = 5.0F;
    private int held;
    private int cooldown;
    private float struck;

    public CrawlspaceEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 2.0).add(Attributes.FOLLOW_RANGE, 18.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HOLDING, -1);
    }

    /** Whether it has someone in its grip. */
    public boolean isHolding() {
        return this.entityData.get(HOLDING) >= 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false) {
            @Override
            public boolean canUse() {
                return !CrawlspaceEntity.this.isHolding() && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && this.cooldown == 0 && !this.isHolding() && target instanceof LivingEntity living) {
            // It closes round them.
            this.entityData.set(HOLDING, living.getId());
            this.held = HOLD_TICKS;
            this.struck = 0.0F;
            living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, HOLD_TICKS + 20, 0), this);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.6F, 1.6F);
        }
        return hit;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) return;
        if (this.cooldown > 0) this.cooldown--;
        if (!this.isHolding()) return;
        Entity held = level.getEntity(this.entityData.get(HOLDING));
        if (!(held instanceof LivingEntity victim) || !victim.isAlive() || --this.held <= 0 || this.struck >= LET_GO_AT || this.distanceTo(victim) > 3.0F) {
            this.entityData.set(HOLDING, -1);
            this.cooldown = 80;
            return;
        }
        // Held where they lie, unable to move, crushed a little at a time.
        this.getNavigation().stop();
        Vec3 v = victim.getDeltaMovement();
        victim.setDeltaMovement(0.0, Math.min(v.y, 0.0), 0.0);
        victim.hurtMarked = true;
        if (this.held % 20 == 0) victim.hurtServer(level, this.damageSources().mobAttack(this), 2.0F);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isHolding()) this.struck += amount;
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SCULK_CLICKING;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }
}
