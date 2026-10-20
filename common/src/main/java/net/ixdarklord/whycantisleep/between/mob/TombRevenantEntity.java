package net.ixdarklord.whycantisleep.between.mob;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * The Tomb Revenant: whoever was laid in a stone tomb, risen when the tomb is opened (TombBlock): the skeleton lying in
 * the tomb is this one's, and the tomb is empty after. It sits up out of the hollow and stands ({@link #RISE_TICKS},
 * still while it does, its eyes kindling), then goes for whoever woke it with the rusted blade it was buried with.
 * <p>
 * Until it rises the tomb draws it (TombClient): a stand-in of this same mob, {@link #lying}, held at the first frame
 * of its rise, where it will be spawned; so the one that rises takes up exactly where the one lying was.
 */
public class TombRevenantEntity extends Zombie {
    public static final int RISE_TICKS = 34;
    /** The ticks of its rise still to go (0: it is up, or never lay in a tomb), seen by the clients. */
    private static final EntityDataAccessor<Integer> RISING = SynchedEntityData.defineId(TombRevenantEntity.class, EntityDataSerializers.INT);
    /** Client only: the stand-in a tomb draws lying in it, before it has risen (TombClient). */
    public boolean lying;

    public TombRevenantEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 32.0).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.ARMOR, 4.0).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RISING, 0);
    }

    /** It is rising out of its tomb (TombBlock), from now. */
    public void startRising() {
        this.entityData.set(RISING, RISE_TICKS);
    }

    /** Still rising out of its tomb. */
    public boolean isRising() {
        return this.entityData.get(RISING) > 0;
    }

    /** How far through its rise it is, in ticks (0 lying, RISE_TICKS up). */
    public float riseTicks(float partialTicks) {
        int left = this.entityData.get(RISING);
        return left <= 0 ? RISE_TICKS : Math.min(RISE_TICKS, RISE_TICKS - left + partialTicks);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.isRising()) this.entityData.set(RISING, this.entityData.get(RISING) - 1);
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.isRising();
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        return !this.isRising() && super.doHurtTarget(level, target);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                                  @Nullable SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
        this.setBaby(false);
        // Its blade is drawn as part of it; the item is for its hand's reach and its damage.
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        return out;
    }

    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, DifficultyInstance difficulty) {
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.SKELETON_STEP;
    }
}
