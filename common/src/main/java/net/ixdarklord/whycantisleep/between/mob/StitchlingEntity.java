package net.ixdarklord.whycantisleep.between.mob;

import net.ixdarklord.whycantisleep.lurker.WCISEntities;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
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

/**
 * A Stitchling: a ragdoll of old patchwork with buttons for eyes and its mouth sewn shut, a needle left through its
 * head, skipping after whoever it sees. Tear one apart and its halves get up again: a whole one splits into two
 * smaller ones ({@link #isSmall}), quicker and weaker. The thread that held it together still mends: Living Thread
 * repairs whatever is held in the other hand.
 */
public class StitchlingEntity extends Monster {
    private static final float SMALL_SCALE = 0.6F;

    public StitchlingEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 4;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    /** Whether this is one of the halves of a torn Stitchling, which tear no further. */
    public boolean isSmall() {
        return this.getAttributeBaseValue(Attributes.SCALE) < 0.9;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt) {
            // Stuffing flies out of it.
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.WHITE_WOOL), this.getX(), this.getY() + this.getBbHeight() * 0.6,
                    this.getZ(), 6, 0.2, 0.2, 0.2, 0.08);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        // Torn in two, its halves get up again.
        if (!this.isSmall() && this.level() instanceof ServerLevel level && !this.isRemoved()) {
            for (int i = 0; i < 2; i++) {
                StitchlingEntity half = new StitchlingEntity(WCISEntities.STITCHLING.get(), level);
                half.getAttribute(Attributes.SCALE).setBaseValue(SMALL_SCALE);
                half.getAttribute(Attributes.MAX_HEALTH).setBaseValue(6.0);
                half.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.36);
                half.setHealth(6.0F);
                half.snapTo(this.getX() + (i == 0 ? -0.3 : 0.3), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                half.setTarget(this.getTarget());
                half.finalizeSpawn(level, level.getCurrentDifficultyAt(this.blockPosition()), EntitySpawnReason.TRIGGERED, null);
                level.addFreshEntity(half);
            }
        }
        super.die(source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VEX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOOL_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOOL_BREAK;
    }

    @Override
    public float getVoicePitch() {
        return this.isSmall() ? 1.3F : 0.8F;
    }
}
