package net.ixdarklord.whycantisleep.between.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Faceless: a figure too tall and too thin, in a black suit, with smooth skin where its face should be. It moves
 * only while no one is looking at it ({@link #isFrozen}): in anyone's view it stands utterly still, turned however it
 * was, and cannot strike; the moment every back is turned it comes, fast and without a sound. Whoever looks round to
 * find it nearer than it was hears it.
 */
public class FacelessEntity extends Monster {
    private static final EntityDataAccessor<Boolean> FROZEN = SynchedEntityData.defineId(FacelessEntity.class, EntityDataSerializers.BOOLEAN);
    /** How far off a player's look it still counts as seen (the cosine of the angle), and from how far. */
    private static final double SEEN_WITHIN = 0.55, SEEN_FROM = 64.0;
    /** How long it must have gone unseen, and how near it must be, for being caught out to sting. */
    private static final int STING_AFTER = 40;
    private static final double STING_NEAR = 12.0;
    private float frozenYaw, frozenBodyYaw, frozenHeadYaw;
    private int unseen;

    public FacelessEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 16;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.38)
                .add(Attributes.ATTACK_DAMAGE, 9.0).add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FROZEN, false);
    }

    /** Whether someone is looking at it, so it may not move. */
    public boolean isFrozen() {
        return this.entityData.get(FROZEN);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false) {
            @Override
            public boolean canUse() {
                return !FacelessEntity.this.isFrozen() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !FacelessEntity.this.isFrozen() && super.canContinueToUse();
            }
        });
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void tick() {
        if (this.level() instanceof ServerLevel level) {
            Player watcher = this.watcher(level);
            boolean frozen = watcher != null;
            if (frozen && !this.isFrozen()) {
                this.frozenYaw = this.getYRot();
                this.frozenBodyYaw = this.yBodyRot;
                this.frozenHeadYaw = this.yHeadRot;
                // Caught out, nearer than it was: whoever turned round hears it.
                if (this.unseen >= STING_AFTER && this.distanceTo(watcher) < STING_NEAR) {
                    level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 0.8F, 0.5F);
                }
            }
            this.unseen = frozen ? 0 : this.unseen + 1;
            this.entityData.set(FROZEN, frozen);
            if (frozen) {
                this.getNavigation().stop();
                Vec3 v = this.getDeltaMovement();
                this.setDeltaMovement(0.0, Math.min(v.y, 0.0), 0.0);
            }
        }
        super.tick();
        if (this.isFrozen()) {
            // Turned however it was when it was seen.
            this.setYRot(this.frozenYaw);
            this.yRotO = this.frozenYaw;
            this.yBodyRot = this.yBodyRotO = this.frozenBodyYaw;
            this.yHeadRot = this.yHeadRotO = this.frozenHeadYaw;
            this.setXRot(0.0F);
        }
    }

    /** Someone who can see it now, or null. */
    private @Nullable Player watcher(ServerLevel level) {
        for (Player player : level.players()) {
            if (player.isSpectator() || player.distanceToSqr(this) > SEEN_FROM * SEEN_FROM) continue;
            Vec3 to = this.getBoundingBox().getCenter().subtract(player.getEyePosition());
            double far = to.length();
            if (far < 1.0E-3) return player;
            if (player.getViewVector(1.0F).dot(to.scale(1.0 / far)) > SEEN_WITHIN && player.hasLineOfSight(this)) return player;
        }
        return null;
    }

    @Override
    public void travel(Vec3 input) {
        super.travel(this.isFrozen() ? Vec3.ZERO : input);
    }

    @Override
    public boolean isPushable() {
        return !this.isFrozen() && super.isPushable();
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        return !this.isFrozen() && super.doHurtTarget(level, target);
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    // It makes no sound at all as it comes; only when hurt.
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.4F;
    }
}
