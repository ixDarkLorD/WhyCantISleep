package net.ixdarklord.whycantisleep.between.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * A Somnimoth: a moth of the Dark Between wider across than a bat, glowing eyespots on its wings. They come in swarms and
 * flutter aimlessly in the dark, and they cannot keep away from light: anyone holding something that shines (a torch,
 * a lantern, a candle) within {@link #DRAWN_FROM} blocks has the swarm about them, fluttering round the light and now
 * and then darting in to bite; the dust off their wings, in the eyes, blinds for a moment. Put the light away and they
 * lose interest.
 */
public class SomnimothEntity extends Monster {
    private static final double DRAWN_FROM = 20.0;

    public SomnimothEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.FLYING_SPEED, 0.7)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 2.0).add(Attributes.FOLLOW_RANGE, DRAWN_FROM);
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
        this.goalSelector.addGoal(1, new FlutterGoal());
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    /** Whether a player holds something that shines, in either hand. */
    public static boolean holdsLight(Player player) {
        return shines(player.getMainHandItem()) || shines(player.getOffhandItem());
    }

    private static boolean shines(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock().defaultBlockState().getLightEmission() > 0;
    }

    /** The nearest player holding a light it can see, or null. */
    private @Nullable Player light() {
        Player nearest = null;
        double best = DRAWN_FROM * DRAWN_FROM;
        for (Player player : this.level().players()) {
            if (player.isSpectator() || player.isCreative() || !holdsLight(player)) continue;
            double d = player.distanceToSqr(this);
            if (d < best && this.hasLineOfSight(player)) {
                best = d;
                nearest = player;
            }
        }
        return nearest;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.5F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.3F;
    }

    // Round a light, or whoever struck it, in a jittering orbit, darting in to bite every so often; otherwise drifting
    // about where it is.
    private final class FlutterGoal extends Goal {
        private float angle;
        private int dart;
        private int cooldown = 40;
        private @Nullable Vec3 wander;

        FlutterGoal() {
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
            SomnimothEntity moth = SomnimothEntity.this;
            LivingEntity drawn = moth.light();
            if (drawn == null && moth.getTarget() != null && moth.getTarget().isAlive()) drawn = moth.getTarget();
            if (this.cooldown > 0) this.cooldown--;
            if (drawn == null) {
                this.dart = 0;
                if (this.wander == null || moth.position().distanceToSqr(this.wander) < 1.0 || moth.random.nextInt(60) == 0) {
                    this.wander = moth.position().add((moth.random.nextDouble() - 0.5) * 8.0, (moth.random.nextDouble() - 0.5) * 3.0,
                            (moth.random.nextDouble() - 0.5) * 8.0);
                }
                moth.getMoveControl().setWantedPosition(this.wander.x, this.wander.y, this.wander.z, 0.6);
                return;
            }
            Vec3 light = drawn.getEyePosition().add(drawn.getLookAngle().scale(0.6)).subtract(0.0, 0.5, 0.0);
            if (this.dart > 0) {
                // The dart: straight at the light, a bite as it gets there.
                this.dart--;
                moth.getMoveControl().setWantedPosition(light.x, light.y, light.z, 1.6);
                if (moth.distanceToSqr(drawn) < 2.0 && moth.level() instanceof ServerLevel level) {
                    moth.swing(InteractionHand.MAIN_HAND);
                    if (moth.doHurtTarget(level, drawn) && moth.random.nextInt(3) == 0) {
                        drawn.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), moth);   // dust off its wings
                    }
                    this.dart = 0;
                    this.cooldown = 30 + moth.random.nextInt(40);
                }
                return;
            }
            if (this.cooldown == 0 && moth.random.nextInt(20) == 0) {
                this.dart = 20;
                return;
            }
            // The orbit, close round the light, never quite steady.
            this.angle += 0.25F + moth.random.nextFloat() * 0.1F;
            double r = 1.4 + Math.sin(this.angle * 0.7) * 0.5;
            moth.getMoveControl().setWantedPosition(light.x + Math.cos(this.angle) * r, light.y + Math.sin(this.angle * 1.3) * 0.6,
                    light.z + Math.sin(this.angle) * r, 1.1);
            moth.getLookControl().setLookAt(light.x, light.y, light.z);
        }
    }
}
