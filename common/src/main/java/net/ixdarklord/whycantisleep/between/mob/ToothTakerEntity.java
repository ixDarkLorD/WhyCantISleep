package net.ixdarklord.whycantisleep.between.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * A Tooth Taker: the Tooth Fairy gone wrong, a bony thing the size of a cat on dragonfly wings, its face nearly all
 * mouth, pliers in its hand. It is a thief: it darts at whoever it finds and, touching them, takes something from their
 * hotbar ({@link #carrying}), then flees high and far with it in its pliers. Kill it and it drops what it took (it is
 * never lost, and a Tooth Taker carrying something never wanders off for good). It drops Fairy Dust too, which lifts.
 */
public class ToothTakerEntity extends Monster {
    public ToothTakerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.setDropChance(EquipmentSlot.MAINHAND, 2.0F);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.FLYING_SPEED, 0.75)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 1.0).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    /** Whether it has something it stole. */
    public boolean carrying() {
        return !this.getMainHandItem().isEmpty();
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
        this.goalSelector.addGoal(1, new ThiefGoal());
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Takes something from a player's hotbar: one of what they carry there, at random. */
    private void steal(ServerLevel level, Player player) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (!player.getInventory().getItem(i).isEmpty()) slots.add(i);
        }
        if (slots.isEmpty()) return;
        int slot = slots.get(this.random.nextInt(slots.size()));
        ItemStack taken = player.getInventory().removeItemNoUpdate(slot);
        this.setItemSlot(EquipmentSlot.MAINHAND, taken);
        this.setPersistenceRequired();
        this.swing(InteractionHand.MAIN_HAND);
        if (player instanceof net.minecraft.server.level.ServerPlayer server) server.sendOverlayMessage(Component.translatable("message.whycantisleep.tooth_taker_stole", taken.getHoverName()));
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 1.0F, 2.0F);
        level.sendParticles(ParticleTypes.WAX_OFF, this.getX(), this.getY() + 0.3, this.getZ(), 10, 0.2, 0.2, 0.2, 0.2);
    }

    // What it stole is never lost: carrying, it never despawns, not even in Peaceful.
    @Override
    public void checkDespawn() {
        if (!this.carrying()) super.checkDespawn();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !this.carrying() && super.removeWhenFarAway(distance);
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VEX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VEX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.3F;
    }

    // Empty-handed: dart at its target, steal on touch. Carrying: flee, high and away from whoever is near.
    private final class ThiefGoal extends Goal {
        private @Nullable Vec3 away;

        ThiefGoal() {
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
            ToothTakerEntity thief = ToothTakerEntity.this;
            LivingEntity target = thief.getTarget();
            if (thief.carrying()) {
                Player near = thief.level().getNearestPlayer(thief, 20.0);
                if (this.away == null || thief.position().distanceToSqr(this.away) < 4.0 || near != null && thief.random.nextInt(40) == 0) {
                    Vec3 from = near != null ? thief.position().subtract(near.position()).multiply(1, 0, 1).normalize() : new Vec3(1, 0, 0);
                    this.away = thief.position().add(from.scale(16.0)).add(0.0, 4.0 + thief.random.nextInt(6), 0.0);
                }
                thief.getMoveControl().setWantedPosition(this.away.x, this.away.y, this.away.z, 1.4);
                return;
            }
            if (target == null || !target.isAlive()) {
                if (thief.random.nextInt(60) == 0) {
                    Vec3 at = thief.position().add((thief.random.nextDouble() - 0.5) * 8, (thief.random.nextDouble() - 0.5) * 3, (thief.random.nextDouble() - 0.5) * 8);
                    thief.getMoveControl().setWantedPosition(at.x, at.y, at.z, 0.6);
                }
                return;
            }
            Vec3 at = target.getEyePosition().subtract(0.0, 0.5, 0.0);
            thief.getMoveControl().setWantedPosition(at.x, at.y, at.z, 1.3);
            thief.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (thief.distanceTo(target) < 1.4 && target instanceof Player player && !player.isCreative() && !player.isSpectator()
                    && thief.level() instanceof ServerLevel level) {
                thief.steal(level, player);
                this.away = null;
            }
        }
    }
}
