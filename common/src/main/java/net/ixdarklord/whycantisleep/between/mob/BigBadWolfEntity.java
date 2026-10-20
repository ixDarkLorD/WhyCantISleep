package net.ixdarklord.whycantisleep.between.mob;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Big Bad Wolf: a great wolf dressed as grandmother, in a bonnet and shawl and nightgown, hobbling about bent
 * double with its hands folded ({@link #isDisguised}). It harms no one in disguise, and its voice is almost right.
 * Come within {@link #CALL_AT} blocks and it turns its head to you and speaks ("come closer, dear"); come within
 * {@link #REVEAL_AT} (the better to see you with), or strike it, and it transforms ({@link #isRevealing}, a second and a
 * half): it shudders, its bonnet flies off, its shawl and nightgown tear away, it throws its head back and roars and
 * grows to {@link #FULL_SIZE} times its size, then comes for you, fast, with teeth and claws. It hunts Little Red Riding
 * Hood too, once it has shed its disguise. Its breath, kept, still blows houses down: Huff and Puff.
 */
public class BigBadWolfEntity extends Monster {
    private static final EntityDataAccessor<Boolean> DISGUISED = SynchedEntityData.defineId(BigBadWolfEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> REVEALING = SynchedEntityData.defineId(BigBadWolfEntity.class, EntityDataSerializers.INT);
    private static final double REVEAL_AT = 4.0, CALL_AT = 9.0;
    private static final int REVEAL_TICKS = 30;
    private static final double FULL_SIZE = 1.3;
    private boolean called;
    private static final double HOBBLE = 0.14, HUNT = 0.36;

    public BigBadWolfEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 16;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 44.0).add(Attributes.MOVEMENT_SPEED, HOBBLE)
                .add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DISGUISED, true);
        builder.define(REVEALING, 0);
    }

    public boolean isDisguised() {
        return this.entityData.get(DISGUISED);
    }

    /** Whether it is ripping its disguise off right now. */
    public boolean isRevealing() {
        return this.entityData.get(REVEALING) > 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true) {
            @Override
            public boolean canUse() {
                return !BigBadWolfEntity.this.isDisguised() && !BigBadWolfEntity.this.isRevealing() && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return !BigBadWolfEntity.this.isDisguised() && super.canUse();
            }
        });
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LittleRedEntity.class, true) {
            @Override
            public boolean canUse() {
                return !BigBadWolfEntity.this.isDisguised() && super.canUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) return;
        int revealing = this.entityData.get(REVEALING);
        if (revealing > 0) {
            // The transformation: it grows as it plays, shuddering out of its clothes.
            this.entityData.set(REVEALING, revealing - 1);
            this.getNavigation().stop();
            float grown = 1.0F - (revealing - 1) / (float) REVEAL_TICKS;
            this.getAttribute(Attributes.SCALE).setBaseValue(1.0 + (FULL_SIZE - 1.0) * grown);
            if (revealing % 6 == 0) {
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.WHITE_WOOL), this.getX(), this.getY() + 1.4, this.getZ(), 6, 0.5, 0.6, 0.5, 0.12);
            }
            if (revealing == REVEAL_TICKS / 2) level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.6F, 1.1F);
        }
        if (!this.isDisguised() || this.tickCount % 5 != 0) return;
        Player near = level.getNearestPlayer(this, CALL_AT);
        if (near == null || near.isCreative() || near.isSpectator()) return;
        if (!this.called && near instanceof net.minecraft.server.level.ServerPlayer player) {
            // Granny turns to you, and speaks.
            this.called = true;
            this.getLookControl().setLookAt(near, 30.0F, 30.0F);
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.whycantisleep.wolf_granny"));
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.VILLAGER_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.55F);
        }
        if (this.distanceTo(near) < REVEAL_AT) this.reveal(level, near);
    }

    /** Off comes the disguise. */
    public void reveal(ServerLevel level, LivingEntity at) {
        if (!this.isDisguised()) return;
        this.entityData.set(DISGUISED, false);
        this.entityData.set(REVEALING, REVEAL_TICKS);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(HUNT);
        this.setTarget(at);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.RED_WOOL), this.getX(), this.getY() + 1.6, this.getZ(), 16, 0.4, 0.4, 0.4, 0.15);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.WHITE_WOOL), this.getX(), this.getY() + 2.0, this.getZ(), 12, 0.3, 0.2, 0.3, 0.15);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WOLF_SHAKE, SoundSource.HOSTILE, 1.4F, 0.6F);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity by) this.reveal(level, by);
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0), this);
        return hit;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Disguised", this.isDisguised());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        if (!input.getBooleanOr("Disguised", true)) {
            this.entityData.set(DISGUISED, false);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(HUNT);
            this.getAttribute(Attributes.SCALE).setBaseValue(FULL_SIZE);
        }
    }

    // In disguise its voice is nearly an old woman's; out of it, a beast's.
    @Override
    protected SoundEvent getAmbientSound() {
        return this.isDisguised() ? SoundEvents.VILLAGER_AMBIENT : SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isDisguised() ? SoundEvents.VILLAGER_HURT : SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return this.isDisguised() ? 0.62F : 1.3F;
    }
}
