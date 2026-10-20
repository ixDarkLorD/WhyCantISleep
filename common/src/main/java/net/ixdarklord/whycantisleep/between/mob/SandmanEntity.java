package net.ixdarklord.whycantisleep.between.mob;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Sandman: a tall cloaked figure with nothing in its hood but two gold eyes, a sack of dream-sand over its
 * shoulder. Every few seconds it casts a handful of sand at whoever it hunts, from up to {@link #CAST_RANGE} blocks:
 * each handful weighs on them more (slow, then slower and dim), and the {@link #SLEEP_AFTER}th puts them to sleep where
 * they stand, unable to move, blind, for a few seconds, while it walks up to them. Stay out of its sight, or kill it
 * first. Its sand, kept, gives a moment's real rest: Dream Sand.
 */
public class SandmanEntity extends Monster {
    private static final double CAST_RANGE = 14.0;
    private static final int CAST_EVERY = 60, SLEEP_AFTER = 3, SLEEP_TICKS = 80;
    private static final DustParticleOptions SAND = new DustParticleOptions(0xE6C878, 1.1F);
    // How many handfuls each of its victims has had lately, and when the last one landed.
    private final Map<UUID, int[]> drowsy = new HashMap<>();
    private int cast = 40;

    public SandmanEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 14;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 32.0).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 14.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) return;
        if (this.tickCount % 20 == 0) {
            level.sendParticles(SAND, this.getX(), this.getY() + 1.6, this.getZ(), 2, 0.3, 0.4, 0.3, 0.0);   // sand sifts from its sack
        }
        LivingEntity target = this.getTarget();
        if (--this.cast > 0 || target == null || !target.isAlive() || this.distanceTo(target) > CAST_RANGE || this.distanceTo(target) < 2.5
                || !this.hasLineOfSight(target)) {
            return;
        }
        this.cast = CAST_EVERY;
        this.castAt(level, target);
    }

    /** A handful of sand, thrown at its target's eyes. */
    private void castAt(ServerLevel level, LivingEntity target) {
        this.swing(InteractionHand.MAIN_HAND);
        Vec3 from = this.getEyePosition(), to = target.getEyePosition();
        for (int i = 0; i < 14; i++) {
            Vec3 at = from.lerp(to, i / 14.0);
            level.sendParticles(SAND, at.x, at.y, at.z, 2, 0.15, 0.15, 0.15, 0.0);
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 1.0F, 0.6F);
        int[] dose = this.drowsy.computeIfAbsent(target.getUUID(), u -> new int[2]);
        if (this.tickCount - dose[1] > 400) dose[0] = 0;                       // it wears off, given time
        dose[0]++;
        dose[1] = this.tickCount;
        if (dose[0] >= SLEEP_AFTER) {
            // Asleep where they stand.
            dose[0] = 0;
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLEEP_TICKS, 9), this);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, SLEEP_TICKS + 20, 0), this);
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, SLEEP_TICKS + 20, 0), this);
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, SLEEP_TICKS, 4), this);
            target.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, SLEEP_TICKS, 250), this);   // and cannot jump
            if (target instanceof net.minecraft.server.level.ServerPlayer player) player.sendOverlayMessage(Component.translatable("message.whycantisleep.sandman_sleep"));
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.0F, 0.5F);
        } else {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, dose[0] - 1), this);
            if (dose[0] > 1) target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), this);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SAND_STEP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SAND_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SOUL_ESCAPE.value();
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }
}
