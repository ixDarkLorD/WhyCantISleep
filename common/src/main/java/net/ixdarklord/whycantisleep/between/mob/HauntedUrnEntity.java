package net.ixdarklord.whycantisleep.between.mob;

import net.ixdarklord.whycantisleep.between.WCISBlocks;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A haunted urn: one of the labyrinth's urns that would not break. It rises from where it stood and floats after whoever
 * disturbed it, keeping a few blocks off and a little above, turning slowly, throwing cursed shards of itself
 * ({@link UrnShardEntity}) in volleys of three, each after a short charge in which it shudders and leans back ({@link #getCharge})
 * so the volley can be seen coming; destroyed, it shatters into pieces of its own stone. Broken at last, it gives up what it held (its loot table: better than a
 * plain urn's). It is drawn as the very urn it was ({@link #getUrn}).
 */
public class HauntedUrnEntity extends Monster {
    private static final EntityDataAccessor<Integer> URN = SynchedEntityData.defineId(HauntedUrnEntity.class, EntityDataSerializers.INT);
    /** Ticks left of its charge before a volley (it shudders and leans back meanwhile); 0 when not charging. */
    private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(HauntedUrnEntity.class, EntityDataSerializers.INT);
    public static final int CHARGE_TICKS = 15;
    /** How far off it keeps, and how high over its target's eyes. */
    private static final double KEEP_OFF = 5.0, HOVER = 1.5;
    /** The turns round its target it tries for a place, nearest its own side first. */
    private static final float[] TURNS = {0.0F, 0.6F, -0.6F, 1.2F, -1.2F, 1.9F, -1.9F, (float) Math.PI};

    public HauntedUrnEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.FLYING_SPEED, 0.4)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 28.0).add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(URN, 0);
        builder.define(CHARGE, 0);
    }

    /** The urn it is (drawn as it); one summoned without one is the dreamstone urn. */
    public BlockState getUrn() {
        int id = this.entityData.get(URN);
        return id == 0 ? WCISBlocks.DREAMSTONE_URN.get().defaultBlockState() : Block.stateById(id);
    }

    /** Ticks left of its charge (0 when not charging), for its shudder. */
    public int getCharge() {
        return this.entityData.get(CHARGE);
    }

    public void setUrn(BlockState state) {
        this.entityData.set(URN, Block.getId(state));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Urn", this.entityData.get(URN));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(URN, input.getIntOr("Urn", 0));
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
        this.goalSelector.addGoal(1, new HauntGoal());
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        // Its charge runs down; at its end, the volley.
        int charge = this.getCharge();
        if (!this.level().isClientSide() && charge > 0) {
            this.entityData.set(CHARGE, charge - 1);
            LivingEntity target = this.getTarget();
            if (charge == 1 && target != null && target.isAlive()) this.throwShards(target);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        // It shatters at last, into pieces of its own stone.
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, this.getUrn()), this.getX(), this.getY() + 0.45, this.getZ(),
                    50, 0.3, 0.35, 0.3, 0.15);
            level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 0.5, this.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            // A haunting about it: soul flecks rising, and now and then a puff of dust from its cracks.
            if (this.random.nextInt(8) == 0) {
                this.level().addParticle(ParticleTypes.SOUL, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0.0, 0.03, 0.0);
            }
            if (this.random.nextInt(8) == 0) {
                this.level().addParticle(ParticleTypes.ASH, this.getRandomX(0.5), this.getY(), this.getRandomZ(0.5), 0.0, -0.02, 0.0);
            }
        }
    }

    /** A volley of three shards at the target, the outer two a little to either side. */
    private void throwShards(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel level)) return;
        Vec3 from = this.position().add(0.0, this.getBbHeight() * 0.6, 0.0);
        Vec3 to = target.getEyePosition().subtract(0.0, 0.3, 0.0);
        Vec3 aim = to.subtract(from);
        for (int k = -1; k <= 1; k++) {
            UrnShardEntity shard = new UrnShardEntity(level, this);
            shard.setPos(from.x, from.y, from.z);
            Vec3 spread = aim.yRot(k * 0.18F);
            shard.shoot(spread.x, spread.y + aim.horizontalDistance() * 0.04, spread.z, 0.9F, 2.0F);
            level.addFreshEntity(shard);
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.DECORATED_POT_HIT, SoundSource.HOSTILE, 1.0F, 0.7F);
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
        return SoundEvents.DECORATED_POT_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DECORATED_POT_SHATTER;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }

    // Keeps a few blocks off its target and a little above, circling slowly, and throws a volley now and then; with
    // no target, it bobs where it is.
    private final class HauntGoal extends Goal {
        private int cooldown = 40;
        private float circle;

        HauntGoal() {
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
            HauntedUrnEntity urn = HauntedUrnEntity.this;
            LivingEntity target = urn.getTarget();
            if (target == null || !target.isAlive()) {
                if (urn.getRandom().nextInt(40) == 0) {
                    urn.getMoveControl().setWantedPosition(urn.getX(), urn.getY() + (urn.getRandom().nextDouble() - 0.4), urn.getZ(), 0.4);
                }
                return;
            }
            this.circle += 0.025F;
            Vec3 away = urn.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() < 0.01) away = new Vec3(1.0, 0.0, 0.0);
            // Its place on the circle, drawn in as close as it must be to fit (a corridor's walls, a low roof); with
            // nowhere on the circle to be, it hangs over its target's head.
            Vec3 over = target.position().add(0.0, target.getEyeHeight() + HOVER, 0.0);
            Vec3 at = over.add(0.0, 0.5, 0.0);
            search:
            for (double r = KEEP_OFF; r >= 2.5; r -= 0.75) {
                for (float turn : TURNS) {                     // its own side of the circle first, then round it
                    Vec3 spot = over.add(away.normalize().yRot(this.circle + turn).scale(r));
                    if (urn.level().noCollision(urn, urn.getBoundingBox().move(spot.subtract(urn.position())).inflate(0.2))) {
                        at = spot;
                        break search;
                    }
                }
            }
            urn.getMoveControl().setWantedPosition(at.x, at.y, at.z, 1.0);
            urn.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (--this.cooldown <= 0 && urn.getCharge() == 0 && urn.hasLineOfSight(target) && urn.distanceTo(target) < 20.0F) {
                // It charges first (shuddering, leaning back), and throws when the charge is spent (tick).
                urn.entityData.set(CHARGE, CHARGE_TICKS);
                urn.playSound(SoundEvents.VEX_CHARGE, 0.8F, 0.6F);
                this.cooldown = 40 + urn.getRandom().nextInt(25);
            }
        }
    }
}
