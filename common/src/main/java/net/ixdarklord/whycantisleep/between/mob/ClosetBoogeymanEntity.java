package net.ixdarklord.whycantisleep.between.mob;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Closet Boogeyman (the monster in the closet): an old wardrobe standing alone where no house is, its doors a crack
 * apart, and in the crack two eyes. It never moves. Come within {@link #WAKE_AT} blocks, open it, or strike it, and its
 * doors burst open ({@link #isOpen}) and long black arms reach out and drag whoever is near in towards the dark: a blow,
 * a moment's blindness, a pull. Walk off far enough and its doors creak shut again. Shut, it is only wood, and takes
 * half harm. Its key, kept, still opens a closet to hide in: the Closet Key.
 */
public class ClosetBoogeymanEntity extends Monster {
    private static final EntityDataAccessor<Boolean> OPEN = SynchedEntityData.defineId(ClosetBoogeymanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final double WAKE_AT = 3.5, REACH = 5.5, SHUT_AT = 10.0;
    private float facing;
    private int grab = 10;
    private int alone;

    public ClosetBoogeymanEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 14;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 12.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OPEN, false);
    }

    public boolean isOpen() {
        return this.entityData.get(OPEN);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        // It stands square to the world, as furniture does.
        this.facing = this.random.nextInt(4) * 90.0F;
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    public void tick() {
        super.tick();
        // It never turns.
        this.setYRot(this.facing);
        this.yRotO = this.yBodyRot = this.yBodyRotO = this.yHeadRot = this.yHeadRotO = this.facing;
        this.setXRot(0.0F);
        if (!(this.level() instanceof ServerLevel level)) return;
        Player near = level.getNearestPlayer(this, SHUT_AT);
        boolean someone = near != null && !near.isCreative() && !near.isSpectator();
        if (!this.isOpen()) {
            if (someone && this.distanceTo(near) < WAKE_AT) this.open(level);
            return;
        }
        this.alone = someone ? 0 : this.alone + 1;
        if (this.alone > 60) {
            this.entityData.set(OPEN, false);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.HOSTILE, 1.0F, 0.6F);
            return;
        }
        if (--this.grab > 0 || !someone || this.distanceTo(near) > REACH || !this.hasLineOfSight(near)) return;
        this.grab = 30;
        this.swing(InteractionHand.MAIN_HAND);
        if (this.doHurtTarget(level, near)) {
            // Dragged in towards the dark.
            Vec3 pull = this.position().subtract(near.position()).normalize().scale(0.7);
            near.push(pull.x, 0.15, pull.z);
            near.hurtMarked = true;
            near.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0), this);
        }
    }

    private void open(ServerLevel level) {
        if (this.isOpen()) return;
        this.entityData.set(OPEN, true);
        this.alone = 0;
        this.grab = 8;
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WOODEN_DOOR_OPEN, SoundSource.HOSTILE, 1.4F, 0.5F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 1.4F, 0.4F);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        // Opening it is a mistake.
        if (this.level() instanceof ServerLevel level && !this.isOpen()) {
            this.open(level);
            this.grab = 2;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean wasOpen = this.isOpen();
        this.open(level);
        return super.hurtServer(level, source, wasOpen ? amount : amount * 0.5F);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("Facing", this.facing);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.facing = input.getFloatOr("Facing", 0.0F);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isOpen() ? SoundEvents.ZOMBIE_AMBIENT : null;
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
        return 0.45F;
    }
}
