package net.ixdarklord.whycantisleep.between.mob;

import net.ixdarklord.whycantisleep.mind.MindServer;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * A Lullaby: a porcelain doll the height of a child, floating, turning the crank of a little music box. It never
 * fights with its hands: it keeps its distance and sings, every {@link #SONG_EVERY} ticks, and its song is a weight on
 * the eyes: players within {@link #SONG_REACH} grow slow and heavy-handed and lose a little lucidity, while every other
 * creature of the Dark Between near it is soothed and healed. Kill it first. Its music box, taken, plays the same
 * song to better ends: the Lullaby Box, which settles a troubled mind.
 */
public class LullabyEntity extends Monster {
    private static final int SONG_EVERY = 70;
    private static final double SONG_REACH = 10.0, KEEP_AWAY = 7.0;
    private int song = 40;

    public LullabyEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 18.0).add(Attributes.FLYING_SPEED, 0.35)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 24.0);
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
        this.goalSelector.addGoal(1, new HoverGoal());
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) return;
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.distanceTo(target) > SONG_REACH + 4.0) return;
        if (--this.song > 0) return;
        this.song = SONG_EVERY;
        this.sing(level);
    }

    /** Its song: slow and heavy for players, soothing for the Dark Between's own. */
    private void sing(ServerLevel level) {
        this.swing(InteractionHand.MAIN_HAND);
        for (int i = 0; i < 3; i++) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.HOSTILE, 1.0F, 0.6F + i * 0.2F);
        }
        level.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + 1.8, this.getZ(), 6, 1.2, 0.6, 1.2, 0.5);
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(SONG_REACH))) {
            if (near == this) continue;
            if (near instanceof Player player) {
                if (player.isCreative() || player.isSpectator()) continue;
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 0), this);
                player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 80, 0), this);
                MindServer.addLucidity(player, -1.5F);
            } else if (near instanceof Monster monster && monster.getHealth() < monster.getMaxHealth()) {
                monster.heal(3.0F);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, monster.getX(), monster.getY() + monster.getBbHeight(), monster.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
            }
        }
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.NOTE_BLOCK_BELL.value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GLASS_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GLASS_BREAK;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }

    // Keeps a little above the ground; with someone to sing to, holds off at about KEEP_AWAY from them, circling slowly.
    private final class HoverGoal extends Goal {
        private float angle;
        private @Nullable Vec3 drift;

        HoverGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LullabyEntity doll = LullabyEntity.this;
            LivingEntity target = doll.getTarget();
            if (target != null && target.isAlive()) {
                this.angle += 0.015F;
                Vec3 at = target.position().add(Math.cos(this.angle) * KEEP_AWAY, 1.5, Math.sin(this.angle) * KEEP_AWAY);
                doll.getMoveControl().setWantedPosition(at.x, at.y, at.z, 0.8);
                doll.getLookControl().setLookAt(target, 20.0F, 20.0F);
                return;
            }
            if (this.drift == null || doll.position().distanceToSqr(this.drift) < 1.0 || doll.random.nextInt(120) == 0) {
                double x = doll.getX() + (doll.random.nextDouble() - 0.5) * 10.0, z = doll.getZ() + (doll.random.nextDouble() - 0.5) * 10.0;
                int ground = doll.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
                double y = ground > doll.level().getMinY() + 1 ? ground + 1.0 + doll.random.nextDouble() : doll.getY();
                this.drift = new Vec3(x, y, z);
            }
            doll.getMoveControl().setWantedPosition(this.drift.x, this.drift.y, this.drift.z, 0.4);
        }
    }
}
