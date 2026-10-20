package net.ixdarklord.whycantisleep.between.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * A Jack-in-the-Box: a toy box, red with gold stars, a crank in its side, sitting where no child is. Shut
 * ({@link #isHidden}) it does nothing but wait, and as someone comes near its crank turns by itself and its tune plays,
 * a note at a time. Then, close enough (or opened, or struck), it pops: the lid flies off, a clown springs up on its
 * coil with a shriek in their face, and the box comes after them in great hops, the clown bonking whoever it lands
 * on. Left alone for half a minute, it folds itself back into its box.
 */
public class JackInTheBoxEntity extends Monster {
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(JackInTheBoxEntity.class, EntityDataSerializers.INT);
    public static final int HIDDEN = 0, POPPING = 1, OUT = 2;
    private static final double POP_AT = 3.5, TUNE_AT = 12.0;
    // Its tune, a phrase of it: the notes' pitches.
    private static final float[] TUNE = {0.84F, 0.94F, 1.06F, 0.94F, 1.12F, 1.06F, 0.84F};
    private int timer;
    private int note;
    private int alone;

    public JackInTheBoxEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, HIDDEN);
    }

    public boolean isHidden() {
        return this.entityData.get(STATE) == HIDDEN;
    }

    public boolean isPopping() {
        return this.entityData.get(STATE) == POPPING;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new HopGoal());
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) return;
        int state = this.entityData.get(STATE);
        if (state == HIDDEN) {
            Player near = level.getNearestPlayer(this, TUNE_AT);
            boolean someone = near != null && !near.isCreative() && !near.isSpectator();
            // Someone near: its crank turns and its tune plays, a note every half second.
            if (someone && ++this.timer % 10 == 0) {
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 0.8F, TUNE[this.note++ % TUNE.length]);
            }
            if (someone && this.distanceTo(near) < POP_AT) this.pop(level, near);
            return;
        }
        if (state == POPPING && --this.timer <= 0) this.entityData.set(STATE, OUT);
        // Left alone long enough, back into its box.
        this.alone = this.getTarget() == null ? this.alone + 1 : 0;
        if (this.alone > 600) {
            this.entityData.set(STATE, HIDDEN);
            this.alone = 0;
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CHEST_CLOSE, SoundSource.HOSTILE, 1.0F, 1.4F);
        }
    }

    /** Out it springs, with a shriek in their face. */
    private void pop(ServerLevel level, LivingEntity at) {
        if (!this.isHidden()) return;
        this.entityData.set(STATE, POPPING);
        this.timer = 16;
        this.setTarget(at);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 1.5F, 1.6F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 1.6F, 0.7F);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.5, this.getZ(), 30, 0.4, 0.5, 0.4, 0.3);
        if (this.distanceTo(at) < POP_AT + 1.0) {
            Vec3 away = at.position().subtract(this.position()).multiply(1, 0, 1).normalize();
            at.push(away.x * 0.6, 0.25, away.z * 0.6);
            at.hurtMarked = true;
            at.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0), this);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level() instanceof ServerLevel level && this.isHidden()) this.pop(level, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity by) this.pop(level, by);
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean isPushable() {
        return !this.isHidden() && super.isPushable();
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Hidden", this.isHidden());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(STATE, input.getBooleanOr("Hidden", true) ? HIDDEN : OUT);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isHidden() ? null : SoundEvents.WITCH_CELEBRATE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOOD_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOOD_BREAK;
    }

    @Override
    public float getVoicePitch() {
        return 1.5F;
    }

    // Out of its box: great hops toward its target, a bonk on whoever it lands on.
    private final class HopGoal extends Goal {
        private int hop;

        HopGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return JackInTheBoxEntity.this.entityData.get(STATE) == OUT;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            JackInTheBoxEntity jack = JackInTheBoxEntity.this;
            LivingEntity target = jack.getTarget();
            if (target == null || !target.isAlive()) return;
            jack.getLookControl().setLookAt(target, 30.0F, 30.0F);
            float yaw = (float) (Mth.atan2(target.getZ() - jack.getZ(), target.getX() - jack.getX()) * Mth.RAD_TO_DEG) - 90.0F;
            jack.setYRot(yaw);
            jack.yBodyRot = yaw;
            if (jack.onGround()) {
                if (jack.getBoundingBox().inflate(0.5).intersects(target.getBoundingBox()) && jack.level() instanceof ServerLevel level) {
                    jack.swing(InteractionHand.MAIN_HAND);
                    if (jack.doHurtTarget(level, target)) {
                        target.push(0.0, 0.5, 0.0);
                        target.hurtMarked = true;
                        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SLIME_ATTACK, SoundSource.HOSTILE, 1.0F, 1.8F);
                    }
                }
                if (--this.hop <= 0) {
                    Vec3 to = target.position().subtract(jack.position()).multiply(1, 0, 1);
                    double far = to.length();
                    Vec3 dir = far > 1.0E-3 ? to.scale(1.0 / far) : Vec3.ZERO;
                    double strength = Math.min(0.75, 0.3 + far * 0.06);
                    jack.setDeltaMovement(dir.x * strength, 0.62, dir.z * strength);
                    jack.level().playSound(null, jack.getX(), jack.getY(), jack.getZ(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 0.7F, 1.4F);
                    this.hop = 22 + jack.random.nextInt(10);
                }
            }
        }
    }
}
