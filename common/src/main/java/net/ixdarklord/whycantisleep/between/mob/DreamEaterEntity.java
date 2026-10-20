package net.ixdarklord.whycantisleep.between.mob;

import net.ixdarklord.whycantisleep.mind.Mind;
import net.ixdarklord.whycantisleep.mind.MindServer;
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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A Dream Eater (after the baku): a gaunt beast with a hide like the night sky, grazing over the Dark Between's islands
 * on what dreamers leave behind. It smells a mind giving way: it comes for any player near it whose lucidity has
 * fallen below {@link #HUNGRY_BELOW} (anyone else it leaves alone unless struck). Its trunk draws the dream out of
 * whoever it touches: their lucidity drops, the dark closes on them for a moment, and it heals by what it took. Badly
 * hurt, it fades out and comes back together somewhere else nearby.
 */
public class DreamEaterEntity extends Monster {
    /** Lucidity under which a player draws it. */
    private static final float HUNGRY_BELOW = 60.0F;
    /** What each touch of its trunk takes from its victim's lucidity, and heals it. */
    private static final float DRAIN = 4.0F;
    private static final int FADE_COOLDOWN = 200;
    private int fadeCooldown;

    public DreamEaterEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 34.0).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (target, level) -> target instanceof Player player && Mind.get(player).lucidity() < HUNGRY_BELOW));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.fadeCooldown > 0) this.fadeCooldown--;
        // Dream-stuff drifts off the wisps along its back.
        if (this.level().isClientSide() && this.random.nextInt(4) == 0) {
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL, this.getRandomX(0.4), this.getY() + 1.5, this.getRandomZ(0.4), 0.0, 0.02, 0.0);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof Player player) {
            // It draws the dream out of them: a stream of it from them to its trunk.
            MindServer.addLucidity(player, -DRAIN);
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0), this);
            this.heal(DRAIN);
            Vec3 from = player.getEyePosition(), to = this.getEyePosition();
            for (int i = 0; i < 8; i++) {
                Vec3 at = from.lerp(to, i / 8.0);
                level.sendParticles(ParticleTypes.WITCH, at.x, at.y, at.z, 1, 0.1, 0.1, 0.1, 0.0);
            }
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SNIFFER_EAT, SoundSource.HOSTILE, 1.0F, 0.6F);
        }
        return hit;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        // Badly hurt, it fades and comes back together somewhere else.
        if (hurt && this.isAlive() && this.getHealth() < this.getMaxHealth() / 3.0F && this.fadeCooldown == 0 && this.random.nextInt(2) == 0) {
            this.fade(level);
        }
        return hurt;
    }

    private void fade(ServerLevel level) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double x = this.getX() + (this.random.nextDouble() - 0.5) * 24.0;
            double y = this.getY() + (this.random.nextInt(9) - 4);
            double z = this.getZ() + (this.random.nextDouble() - 0.5) * 24.0;
            Vec3 was = this.position();
            if (this.randomTeleport(x, y, z, true)) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, was.x, was.y + 1.0, was.z, 40, 0.5, 0.8, 0.5, 0.05);
                level.playSound(null, was.x, was.y, was.z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 1.0F, 0.6F);
                this.fadeCooldown = FADE_COOLDOWN;
                return;
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SNIFFER_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SNIFFER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SNIFFER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }
}
