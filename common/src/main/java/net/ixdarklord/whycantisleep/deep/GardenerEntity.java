package net.ixdarklord.whycantisleep.deep;

import net.ixdarklord.coolcatcanvas.api.armature.ArmatureControl;
import net.ixdarklord.coolcatcanvas.api.armature.Motion;
import net.ixdarklord.coolcatcanvas.api.bossbar.CustomBossBars;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Weeping Gardener, lord of the Garden of Despair: the mini-boss kneeling before the weeping tree at the heart of
 * the labyrinth ({@code tools/gen_deep_structures.py} puts one in each garden; its model, clips and texture come from
 * {@code tools/gen_gardener.py}, drawn by CoolCatLib's Armature system, {@code GardenerClient}).
 * <ul>
 *     <li><b>Dormant</b>: it kneels, head bowed, and takes no harm, until a player comes near or strikes it. Then it
 *     rises, and its bar shows to everyone near.</li>
 *     <li><b>Scythe</b>: close in, a wide sweep in front of it, hard, throwing those it hits back.</li>
 *     <li><b>Root snare</b>: further off, it brings both arms down; under each player near it the ground stirs, and a
 *     moment later roots burst up there, hurting and holding whoever is still on the spot.</li>
 *     <li><b>Hedge stride</b>: when its quarry is far or out of its reach, it sinks into the ground, untouchable, and
 *     rises again beside them.</li>
 *     <li><b>Weeping</b>: at half its health it roars, darkening and slowing all near it; from then on it is quicker,
 *     its attacks come sooner, and now and then a weeping pulse darkens the eyes of those round it.</li>
 * </ul>
 * It keeps to its garden: with nobody left to fight, it walks back to where it knelt, heals, and kneels again.
 */
public class GardenerEntity extends Monster {
    private static final EntityDataAccessor<Boolean> DORMANT = SynchedEntityData.defineId(GardenerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final String ACTION = "action";
    // How near a player must come to wake it, how far from its place it will follow, and how far its bar shows.
    private static final double WAKE_RANGE = 12.0;
    private static final double LEASH = 40.0;
    private static final double BAR_RANGE = 48.0;
    // With nobody to fight for this long it goes back to kneel.
    private static final int GIVE_UP_TICKS = 20 * 10;

    private enum Action {
        NONE(0), AWAKEN(48), SWING(24), SLAM(28), SINK(20), RISE(24), ROAR(36);

        final int ticks;

        Action(int ticks) {
            this.ticks = ticks;
        }
    }

    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getUUID(), this.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private @Nullable BlockPos home;
    private Action action = Action.NONE;
    private int actionTick;
    private int swingCooldown;
    private int slamCooldown = 60;
    private int sinkCooldown = 200;
    private int auraCooldown = 100;
    private int idleTicks;
    private boolean weeping;
    private @Nullable Vec3 riseAt;
    // Where roots are about to burst, and in how many ticks.
    private final List<Snare> snares = new ArrayList<>();

    private record Snare(Vec3 at, int[] ticks) {
    }

    public GardenerEntity(EntityType<? extends GardenerEntity> type, Level level) {
        super(type, level);
        this.xpReward = 150;
        this.setPersistenceRequired();
        this.bossEvent.setDarkenScreen(true);
        // Its own bar (BossBars in CoolCatLib): drawn by GardenerClient, linked to it.
        CustomBossBars.style(this.bossEvent, WhyCantISleep.id("gardener"), this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, LEASH);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DORMANT, true);
    }

    @Override
    protected void registerGoals() {
        // Everything else it does is its own, from customServerAiStep.
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    /** Whether it kneels, waiting: the client draws it so. */
    public boolean isDormant() {
        return this.entityData.get(DORMANT);
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    // ---- Each tick ----

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.home == null) this.home = this.blockPosition();
        this.fallDistance = 0.0;
        this.tickSnares(level);
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.tickCount % 20 == 0) this.updateBar(level);
        if (this.isDormant()) {
            this.getNavigation().stop();
            Player near = level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), WAKE_RANGE, this::fair);
            if (near != null) this.awaken(level, near);
            return;
        }
        if (this.action != Action.NONE) {
            this.tickAction(level);
            return;
        }
        Player target = this.quarry(level);
        if (target == null) {
            this.goHome(level);
            return;
        }
        this.idleTicks = 0;
        this.setTarget(target);
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distance = this.distanceTo(target);
        if (this.swingCooldown > 0) this.swingCooldown--;
        if (this.slamCooldown > 0) this.slamCooldown--;
        if (this.sinkCooldown > 0) this.sinkCooldown--;
        if (this.weeping && --this.auraCooldown <= 0) this.weep(level);
        if (distance < 4.5 && this.swingCooldown <= 0) {
            this.begin(level, Action.SWING);
        } else if (distance > 7.0 && this.slamCooldown <= 0) {
            this.begin(level, Action.SLAM);
        } else if ((distance > 16.0 || this.getNavigation().isDone() && distance > 6.0) && this.sinkCooldown <= 0) {
            this.begin(level, Action.SINK);
        } else if (this.tickCount % 10 == 0 || this.getNavigation().isDone()) {
            this.getNavigation().moveTo(target, this.weeping ? 1.25 : 1.0);
        }
    }

    // A player it would fight: alive, in play, in its garden.
    private boolean fair(Entity entity) {
        return entity instanceof Player player && player.isAlive() && !player.isSpectator() && !player.isCreative()
                && (this.home == null || player.distanceToSqr(Vec3.atCenterOf(this.home)) < LEASH * LEASH);
    }

    private @Nullable Player quarry(ServerLevel level) {
        if (this.getTarget() instanceof Player player && this.fair(player)) return player;
        return level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), LEASH, this::fair);
    }

    private void awaken(ServerLevel level, @Nullable Player by) {
        this.entityData.set(DORMANT, false);
        if (by != null) this.setTarget(by);
        this.begin(level, Action.AWAKEN);
        level.playSound(null, this.blockPosition(), SoundEvents.CREAKING_ACTIVATE, SoundSource.HOSTILE, 3.0F, 0.5F);
        level.playSound(null, this.blockPosition(), SoundEvents.CREAKING_HEART_SPAWN, SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    // Nobody to fight: back to where it knelt, and once there, heal and kneel.
    private void goHome(ServerLevel level) {
        this.setTarget(null);
        if (++this.idleTicks < GIVE_UP_TICKS || this.home == null) {
            this.getNavigation().stop();
            return;
        }
        if (this.distanceToSqr(Vec3.atBottomCenterOf(this.home)) > 4.0) {
            if (this.getNavigation().isDone()) this.getNavigation().moveTo(this.home.getX() + 0.5, this.home.getY(), this.home.getZ() + 0.5, 1.0);
            return;
        }
        this.getNavigation().stop();
        this.setHealth(this.getMaxHealth());
        this.weeping = false;
        this.entityData.set(DORMANT, true);
        level.playSound(null, this.blockPosition(), SoundEvents.CREAKING_DEACTIVATE, SoundSource.HOSTILE, 2.0F, 0.5F);
    }

    private void updateBar(ServerLevel level) {
        boolean show = !this.isDormant() && this.isAlive();
        for (ServerPlayer player : new ArrayList<>(this.bossEvent.getPlayers())) {
            if (!show || player.level() != level || player.distanceTo(this) > BAR_RANGE) this.bossEvent.removePlayer(player);
        }
        if (!show) return;
        for (ServerPlayer player : level.players()) {
            if (player.distanceTo(this) <= BAR_RANGE) this.bossEvent.addPlayer(player);
        }
    }

    // ---- What it does ----

    private void begin(ServerLevel level, Action next) {
        this.action = next;
        this.actionTick = 0;
        this.getNavigation().stop();
        String clip = switch (next) {
            case AWAKEN -> "awaken";
            case SWING -> "swing";
            case SLAM -> "slam";
            case SINK -> "sink";
            case RISE -> "rise";
            case ROAR -> "roar";
            case NONE -> null;
        };
        if (clip != null) ArmatureControl.play(this, ACTION, Motion.once(clip).restart());
        float pace = this.weeping ? 0.65F : 1.0F;
        switch (next) {
            case SWING -> {
                this.swingCooldown = Math.round(30 * pace);
                level.playSound(null, this.blockPosition(), SoundEvents.CREAKING_ATTACK, SoundSource.HOSTILE, 2.0F, 0.6F);
            }
            case SLAM -> this.slamCooldown = Math.round(160 * pace);
            case SINK -> {
                this.sinkCooldown = Math.round(300 * pace);
                level.playSound(null, this.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
            }
            case ROAR -> level.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3.0F, 0.55F);
            default -> {
            }
        }
    }

    private void tickAction(ServerLevel level) {
        int tick = ++this.actionTick;
        if (this.getTarget() != null && this.action != Action.SINK) this.getLookControl().setLookAt(this.getTarget(), 30.0F, 30.0F);
        switch (this.action) {
            case SWING -> {
                if (tick == 10) this.sweep(level);
            }
            case SLAM -> {
                if (tick == 15) this.slam(level);
            }
            case SINK -> {
                if (tick % 3 == 0) this.dust(level, this.position(), 12);
                if (tick == this.action.ticks) {
                    this.surface(level);
                    this.begin(level, Action.RISE);
                    return;
                }
            }
            case RISE -> {
                if (tick == 9) {
                    this.dust(level, this.position(), 40);
                    level.playSound(null, this.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 2.5F, 0.6F);
                    for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(3.5), this::fair)) {
                        player.knockback(1.0, this.getX() - player.getX(), this.getZ() - player.getZ());
                        player.hurtServer(level, this.damageSources().mobAttack(this), 6.0F);
                    }
                }
            }
            case ROAR -> {
                if (tick == 8) {
                    for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16.0), this::fair)) {
                        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
                        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
                    }
                }
            }
            default -> {
            }
        }
        if (tick >= this.action.ticks) this.action = Action.NONE;
    }

    // The scythe: everyone in a wide arc in front of it.
    private void sweep(ServerLevel level) {
        Vec3 facing = Vec3.directionFromRotation(0.0F, this.getYRot());
        level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.0F, 0.5F);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX() + facing.x * 2.5, this.getY() + 1.4, this.getZ() + facing.z * 2.5, 3, 1.0, 0.2, 1.0, 0.0);
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(5.5, 1.0, 5.5), this::fair)) {
            Vec3 to = player.position().subtract(this.position());
            if (to.horizontalDistance() > 5.5 || new Vec3(to.x, 0.0, to.z).normalize().dot(facing) < 0.1) continue;
            float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * (this.weeping ? 1.2F : 1.0F);
            if (player.hurtServer(level, this.damageSources().mobAttack(this), damage)) {
                player.knockback(1.6, -facing.x, -facing.z);
            }
        }
    }

    // The slam: under each player near it, up to three, roots will burst.
    private void slam(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.CREAKING_HEART_HURT, SoundSource.HOSTILE, 3.0F, 0.5F);
        this.dust(level, this.position(), 30);
        level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(24.0), this::fair).stream()
                .sorted(Comparator.comparingDouble(this::distanceToSqr)).limit(3)
                .forEach(player -> this.snares.add(new Snare(player.position(), new int[]{this.weeping ? 14 : 20})));
    }

    private void tickSnares(ServerLevel level) {
        this.snares.removeIf(snare -> {
            int left = --snare.ticks[0];
            BlockParticleOption hedge = new BlockParticleOption(ParticleTypes.BLOCK, WCISBlocks.WITHERED_HEDGE.get().defaultBlockState());
            if (left > 0) {
                // The ground stirring where they will burst: a warning, growing.
                level.sendParticles(ParticleTypes.CRIMSON_SPORE, snare.at.x, snare.at.y + 0.2, snare.at.z, 6, 0.8, 0.05, 0.8, 0.0);
                if (left % 4 == 0) level.sendParticles(hedge, snare.at.x, snare.at.y + 0.1, snare.at.z, 6, 0.6, 0.05, 0.6, 0.1);
                return false;
            }
            level.sendParticles(hedge, snare.at.x, snare.at.y + 0.6, snare.at.z, 60, 0.7, 1.0, 0.7, 0.25);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, snare.at.x, snare.at.y + 1.0, snare.at.z, 8, 0.6, 0.6, 0.6, 0.1);
            level.playSound(null, BlockPos.containing(snare.at), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 2.0F, 0.7F);
            for (Player player : level.getEntitiesOfClass(Player.class, new AABB(snare.at, snare.at).inflate(1.8, 2.0, 1.8), this::fair)) {
                player.hurtServer(level, this.damageSources().mobAttack(this), this.weeping ? 11.0F : 9.0F);
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 3));
                player.setDeltaMovement(player.getDeltaMovement().add(0.0, 0.55, 0.0));
                player.hurtMarked = true;
            }
            return true;
        });
    }

    // Under the ground: where it comes up, beside its quarry, on solid ground with room to stand.
    private void surface(ServerLevel level) {
        Entity target = this.getTarget();
        Vec3 to = this.home != null ? Vec3.atBottomCenterOf(this.home) : this.position();
        if (target != null) {
            for (int tries = 0; tries < 12; tries++) {
                double angle = this.random.nextDouble() * Mth.TWO_PI;
                double out = 3.5 + this.random.nextDouble() * 3.0;
                BlockPos spot = BlockPos.containing(target.getX() + Math.cos(angle) * out, target.getY(), target.getZ() + Math.sin(angle) * out);
                if (level.getBlockState(spot.below()).isSolid() && level.isEmptyBlock(spot) && level.isEmptyBlock(spot.above())
                        && level.isEmptyBlock(spot.above(2)) && level.isEmptyBlock(spot.above(3))) {
                    to = Vec3.atBottomCenterOf(spot);
                    break;
                }
            }
        }
        this.teleportTo(to.x, to.y, to.z);
        if (target != null) this.lookAt(target, 360.0F, 360.0F);
    }

    // At half its health, and then now and then: its weeping darkens the eyes of those round it.
    private void weep(ServerLevel level) {
        this.auraCooldown = 140;
        level.sendParticles(ParticleTypes.FALLING_DRIPSTONE_LAVA, this.getX(), this.getY() + 3.0, this.getZ(), 30, 4.0, 0.5, 4.0, 0.0);
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(10.0), this::fair)) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
        }
    }

    private void dust(ServerLevel level, Vec3 at, int count) {
        BlockParticleOption hedge = new BlockParticleOption(ParticleTypes.BLOCK, WCISBlocks.ASHEN_SOIL.get().defaultBlockState());
        level.sendParticles(hedge, at.x, at.y + 0.2, at.z, count, 1.2, 0.1, 1.2, 0.15);
    }

    // ---- Harm ----

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        // What nothing can be shielded from (the void, /kill) still kills it.
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurtServer(level, source, damage);
        if (this.isDormant()) {
            // Struck while it kneels, it wakes, but the blow is lost on it.
            if (source.getEntity() instanceof Player player && this.fair(player)) this.awaken(level, player);
            return false;
        }
        // Rising out of its kneeling, roaring, or under the ground, nothing touches it.
        if (this.action == Action.AWAKEN || this.action == Action.ROAR || this.action == Action.SINK
                || this.action == Action.RISE && this.actionTick < 10) return false;
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && !this.weeping && this.isAlive() && this.getHealth() <= this.getMaxHealth() / 2.0F) {
            this.weeping = true;
            this.auraCooldown = 120;
            this.begin(level, Action.ROAR);
        }
        if (hurt && source.getEntity() instanceof Player player && this.fair(player)) this.setTarget(player);
        return hurt;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            ArmatureControl.play(this, ACTION, Motion.hold("death").restart());
            level.playSound(null, this.blockPosition(), SoundEvents.CREAKING_DEATH, SoundSource.HOSTILE, 3.0F, 0.5F);
            this.unseal(level);
            GardenWard.lift(level, this.home != null ? this.home : this.blockPosition());
            // Everyone in at its death has it to their name: those it showed its bar to, and anyone near.
            java.util.Set<ServerPlayer> in = new java.util.HashSet<>(this.bossEvent.getPlayers());
            for (ServerPlayer player : level.players()) {
                if (player.distanceTo(this) < BAR_RANGE) in.add(player);
            }
            DeepProgress.defeated(in, "gardener");
        }
        this.bossEvent.removeAllPlayers();
    }

    // Its death breaks the seal on the weeping tree's heart: every heartwood seal near where it knelt shatters, and
    // the tree sighs.
    private void unseal(ServerLevel level) {
        BlockPos around = this.home != null ? this.home : this.blockPosition();
        boolean any = false;
        for (BlockPos pos : BlockPos.betweenClosed(around.offset(-16, -4, -16), around.offset(16, 8, 16))) {
            if (!level.getBlockState(pos).is(WCISBlocks.HEARTWOOD_SEAL.get())) continue;
            level.levelEvent(2001, pos, Block.getId(level.getBlockState(pos)));
            level.removeBlock(pos, false);
            level.sendParticles(ParticleTypes.CRIMSON_SPORE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.5, 0.5, 0.5, 0.05);
            if (!any) {
                any = true;
                level.playSound(null, pos, SoundEvents.CREAKING_HEART_BREAK, SoundSource.BLOCKS, 3.0F, 0.6F);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 2.0F, 0.5F);
            }
        }
    }

    // It takes its time dying, as its clip does.
    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (this.level() instanceof ServerLevel level && this.deathTime % 4 == 0) {
            level.sendParticles(ParticleTypes.CRIMSON_SPORE, this.getX(), this.getY() + 1.5, this.getZ(), 10, 0.6, 0.8, 0.6, 0.02);
        }
        if (this.deathTime >= 52 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        this.bossEvent.removeAllPlayers();
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    // ---- Sounds ----

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isDormant() ? SoundEvents.CREAKING_HEART_IDLE : SoundEvents.CREAKING_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CREAKING_HEART_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CREAKING_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F + this.random.nextFloat() * 0.1F;
    }

    // ---- Saving ----

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Dormant", this.isDormant());
        output.putBoolean("Weeping", this.weeping);
        if (this.home != null) output.store("Home", BlockPos.CODEC, this.home);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DORMANT, input.getBooleanOr("Dormant", true));
        this.weeping = input.getBooleanOr("Weeping", false);
        this.home = input.read("Home", BlockPos.CODEC).orElse(null);
        if (this.hasCustomName()) this.bossEvent.setName(this.getDisplayName());
    }
}
