package net.ixdarklord.whycantisleep.between.mob;

import net.ixdarklord.whycantisleep.between.WildsServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Mime (a clown gone silent): striped shirt, white face, a painted tear, a beret. It makes no sound at all, not a
 * step, not when it is hurt. Its trick: the invisible box. Every so often, close to whoever it hunts, it mimes a box
 * round them, and there is one: walls nobody can see close in on all four sides and over their head
 * ({@link #BOX_TICKS}), while it presses its hands on the glass ({@link #isBoxing}) and walks up to them.
 */
public class MimeClownEntity extends Monster {
    private static final EntityDataAccessor<Integer> BOXING = SynchedEntityData.defineId(MimeClownEntity.class, EntityDataSerializers.INT);
    private static final int BOX_TICKS = 80;
    private int trick = 120;

    public MimeClownEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 28.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOXING, 0);
    }

    /** Whether it is miming the box it has shut someone in. */
    public boolean isBoxing() {
        return this.entityData.get(BOXING) > 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) return;
        int boxing = this.entityData.get(BOXING);
        if (boxing > 0) this.entityData.set(BOXING, boxing - 1);
        if (--this.trick > 0) return;
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !target.onGround() || this.distanceTo(target) > 6.0F || !this.hasLineOfSight(target)
                || target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            this.trick = 20;
            return;
        }
        this.box(level, target);
        this.trick = 240 + this.random.nextInt(120);
    }

    /** The invisible box: walls nobody can see on its target's four sides, at its feet and head, and over its head. */
    private void box(ServerLevel level, LivingEntity target) {
        BlockPos feet = target.blockPosition();
        BlockState wall = Blocks.BARRIER.defaultBlockState();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            for (int up = 0; up < 2; up++) this.wall(level, feet.relative(side).above(up), wall);
        }
        this.wall(level, feet.above(2), wall);
        this.entityData.set(BOXING, 40);
        if (target instanceof ServerPlayer player) player.sendOverlayMessage(Component.translatable("message.whycantisleep.mime_box"));
    }

    private void wall(ServerLevel level, BlockPos at, BlockState wall) {
        if (level.getBlockState(at).isAir()) WildsServer.temporary(level, at, wall, BOX_TICKS);
    }

    // It makes no sound at all.
    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }
}
