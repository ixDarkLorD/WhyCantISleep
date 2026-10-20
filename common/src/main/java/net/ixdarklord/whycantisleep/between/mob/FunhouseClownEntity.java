package net.ixdarklord.whycantisleep.between.mob;

import net.ixdarklord.whycantisleep.lurker.WCISEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The Funhouse Clown, ringmaster of the hall of mirrors: two faces in one, laughing and weeping, a top hat, a cane with
 * a cracked mirror for a knob. Its trick: the hall of mirrors. It throws up {@link #COPIES} copies of itself out of
 * nothing, then all of them spin and shuffle round whoever it hunts in a burst of confetti ({@link #isShuffling}), and
 * which is which is anyone's guess: the copies ({@link #isCopy}) do no harm, burst at a touch, and fade after a while;
 * the real one waits for a turned back. A copy leaves nothing behind.
 */
public class FunhouseClownEntity extends Monster {
    private static final EntityDataAccessor<Boolean> COPY = SynchedEntityData.defineId(FunhouseClownEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SHUFFLE = SynchedEntityData.defineId(FunhouseClownEntity.class, EntityDataSerializers.INT);
    private static final int COPIES = 3, COPY_LIFE = 300;
    private int trick = 140;

    public FunhouseClownEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 14;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 32.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COPY, false);
        builder.define(SHUFFLE, 0);
    }

    /** Whether this is one of its copies, out of the hall of mirrors. */
    public boolean isCopy() {
        return this.entityData.get(COPY);
    }

    public boolean isShuffling() {
        return this.entityData.get(SHUFFLE) > 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) return;
        int shuffle = this.entityData.get(SHUFFLE);
        if (shuffle > 0) this.entityData.set(SHUFFLE, shuffle - 1);
        if (this.isCopy()) {
            if (this.tickCount > COPY_LIFE) this.burst(level);
            return;
        }
        if (--this.trick > 0) return;
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.distanceTo(target) > 12.0F) {
            this.trick = 20;
            return;
        }
        this.mirrors(level, target);
        this.trick = 300 + this.random.nextInt(120);
    }

    /** The hall of mirrors: copies of it out of nothing, and all of them shuffled round its target. */
    private void mirrors(ServerLevel level, LivingEntity target) {
        List<FunhouseClownEntity> all = new ArrayList<>();
        all.add(this);
        for (int i = 0; i < COPIES; i++) {
            FunhouseClownEntity copy = WCISEntities.FUNHOUSE_CLOWN.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (copy == null) continue;
            copy.entityData.set(COPY, true);
            copy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1.0);
            copy.setHealth(1.0F);
            copy.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
            copy.setTarget(target);
            level.addFreshEntity(copy);
            all.add(copy);
        }
        // Round its target, each at a place of its own, the real one somewhere among them.
        double start = this.random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < all.size(); i++) {
            FunhouseClownEntity one = all.get(i);
            double a = start + i * Math.PI * 2 / all.size();
            Vec3 was = one.position();
            one.randomTeleport(target.getX() + Math.cos(a) * 4.0, target.getY(), target.getZ() + Math.sin(a) * 4.0, false);
            one.entityData.set(SHUFFLE, 20);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, was.x, was.y + 1.2, was.z, 14, 0.3, 0.6, 0.3, 0.2);
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 1.2F, 1.0F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 1.2F, 1.3F);
    }

    /** A copy bursts into confetti and is gone, leaving nothing. */
    private void burst(ServerLevel level) {
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.2, this.getZ(), 20, 0.3, 0.6, 0.3, 0.25);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.8F, 1.6F);
        this.discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isCopy()) {
            this.burst(level);
            return true;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (this.isCopy()) return false;                                // a copy's cane is only a reflection
        boolean hit = super.doHurtTarget(level, target);
        if (hit) level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_HIT, SoundSource.HOSTILE, 1.0F, 1.2F);
        return hit;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return this.isCopy() || super.removeWhenFarAway(distance);
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
        return 1.1F;
    }
}
