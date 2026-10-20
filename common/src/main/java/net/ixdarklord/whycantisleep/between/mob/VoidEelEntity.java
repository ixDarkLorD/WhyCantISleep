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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
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
 * A Void Eel (thalassophobia, the dread of what is under deep water, here the void under the islands): a serpent longer
 * than a house swimming through the dark under the islands, so dark that only the rows of lights down its sides show.
 * It circles below, out of reach. Anyone who stands near an island's edge, or falls or leaps off one, it sees, and it
 * comes up out of the dark at them in a lunge ({@link #isLunging}): a bite that leaves them reeling, and then it sinks
 * back. Its scale, kept, remembers solid ground: the Void Scale takes its keeper back to where they last stood.
 */
public class VoidEelEntity extends Monster {
    private static final EntityDataAccessor<Boolean> LUNGING = SynchedEntityData.defineId(VoidEelEntity.class, EntityDataSerializers.BOOLEAN);
    private static final double SEES = 30.0, CIRCLE = 14.0;

    public VoidEelEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 9.0).add(Attributes.FOLLOW_RANGE, SEES)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LUNGING, false);
    }

    public boolean isLunging() {
        return this.entityData.get(LUNGING);
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
        this.goalSelector.addGoal(1, new DeepGoal());
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    /** Whether a player is where it can reach them: in the air off an island, or standing by an edge with the void beside them. */
    private boolean exposed(Player player) {
        if (player.isCreative() || player.isSpectator()) return false;
        if (!player.onGround() && player.getDeltaMovement().y < -0.3) return true;          // falling
        Level level = this.level();
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0;
            int x = (int) Math.floor(player.getX() + Math.cos(a) * 2.5), z = (int) Math.floor(player.getZ() + Math.sin(a) * 2.5);
            if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) <= level.getMinY() + 1) return true;   // the void beside them
        }
        return false;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ELDER_GUARDIAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ELDER_GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ELDER_GUARDIAN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }

    // Circles in the deep, under where it began; lunges up at an exposed player; sinks back after.
    private final class DeepGoal extends Goal {
        private @Nullable Vec3 home;
        private float angle;
        private int lunge;
        private int cooldown = 60;

        DeepGoal() {
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
            VoidEelEntity eel = VoidEelEntity.this;
            if (this.home == null) this.home = eel.position();
            if (this.cooldown > 0) this.cooldown--;
            LivingEntity target = eel.getTarget();
            if (this.lunge > 0 && target != null && target.isAlive()) {
                this.lunge--;
                Vec3 at = target.getEyePosition();
                eel.getMoveControl().setWantedPosition(at.x, at.y, at.z, 2.4);
                eel.getLookControl().setLookAt(target, 60.0F, 60.0F);
                if (eel.distanceTo(target) < 2.2 && eel.level() instanceof ServerLevel level) {
                    if (eel.doHurtTarget(level, target)) target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0), eel);
                    level.playSound(null, eel.getX(), eel.getY(), eel.getZ(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1.4F, 0.6F);
                    this.lunge = 0;
                }
                if (this.lunge == 0) {
                    eel.entityData.set(LUNGING, false);
                    eel.setTarget(null);
                    this.cooldown = 100 + eel.random.nextInt(80);
                }
                return;
            }
            eel.entityData.set(LUNGING, false);
            if (this.cooldown == 0 && eel.level() instanceof ServerLevel level) {
                for (Player player : level.players()) {
                    if (player.distanceTo(eel) < SEES && eel.exposed(player)) {
                        eel.setTarget(player);
                        eel.entityData.set(LUNGING, true);
                        this.lunge = 50;
                        level.playSound(null, eel.getX(), eel.getY(), eel.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.6F, 0.5F);
                        return;
                    }
                }
            }
            // The deep: a slow wide circle round its home, rising and sinking a little.
            this.angle += 0.012F;
            Vec3 at = this.home.add(Math.cos(this.angle) * CIRCLE, Math.sin(this.angle * 2.0) * 2.0, Math.sin(this.angle) * CIRCLE);
            eel.getMoveControl().setWantedPosition(at.x, at.y, at.z, 0.6);
        }
    }
}
