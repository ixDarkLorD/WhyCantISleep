package net.ixdarklord.whycantisleep.between.mob;

import net.ixdarklord.whycantisleep.mind.Mind;
import net.ixdarklord.whycantisleep.mind.MindServer;
import net.ixdarklord.coolcatcanvas.api.vfx.ShakeControl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Little Red Riding Hood: a small girl in a red hooded cape with a basket on her arm, wandering the islands, the one
 * kind face in the Dark Between. Her hood hangs down over her face until someone comes within {@link #LIFT_AT} blocks;
 * then she lifts it back ({@link #isHoodUp}) and smiles, and lowers it again once they have gone. Used, she hands over
 * something from her basket, once every {@link #GIFT_EVERY} ticks for each player: nearly always rubbish she is very
 * proud of (a stick, a bone, a rotten something), now and then ({@link #GOOD_GIFT}) a real treat. She runs from the Big
 * Bad Wolf, and from anything else hostile that comes close.
 * <p>
 * She is not what she seems. Strike her while it is night in the waking world, or while your own mind is slipping
 * (lucidity under {@link #SLIPPING}), and she turns ({@link #isTurning}, {@link #TURN_TICKS} ticks, untouchable): she
 * shudders, then burns away limb by limb in smoke and flame, arms, legs, body and last her head, what she truly is
 * growing in each one's place (a grey face with hollow eyes and a grin full of needles, long clawed arms); then she
 * screams at whoever struck her, a scream that shakes the ground and drives a wind of dust at them, and she hunts
 * ({@link #isFeral}). By day, with no one left to hunt for {@link #CALM_AFTER} ticks, she is a little girl again.
 * Harming her in daylight, with a clear head, she only runs, and killing her then weighs on you: it costs lucidity.
 */
public class LittleRedEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> HOOD_UP = SynchedEntityData.defineId(LittleRedEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FERAL = SynchedEntityData.defineId(LittleRedEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TURNING = SynchedEntityData.defineId(LittleRedEntity.class, EntityDataSerializers.INT);
    private static final double LIFT_AT = 4.5, LOWER_AT = 8.0;
    private static final double SWEET = 0.25, HUNTING = 0.34;
    private static final int GIFT_EVERY = 1200, CALM_AFTER = 600;
    // Her turning, as its clip has it (little_red_turning's "turn", 3 s): when each part of her burns, in seconds from
    // its start (arms, legs, body, head), and her scream after.
    public static final int TURN_TICKS = 60;
    private static final float[][] BURNS = {{0.4F, 0.75F}, {0.6F, 0.95F}, {0.8F, 1.15F}, {1.0F, 1.35F}};
    private static final int SCREAM_FROM = 29, SCREAM_TO = 54;
    private static final double SCREAM_REACH = 16.0;
    private static final float GUILT = 10.0F, SLIPPING = 50.0F, GOOD_GIFT = 0.1F;
    private static final Item[] RUBBISH = {Items.STICK, Items.BONE, Items.ROTTEN_FLESH, Items.STRING, Items.DEAD_BUSH, Items.FLINT,
            Items.FEATHER, Items.SPIDER_EYE, Items.POISONOUS_POTATO, Items.WHEAT_SEEDS, Items.BOWL, Items.LEATHER};
    // When each player last had a gift from her, in game ticks.
    private final Map<UUID, Long> gifts = new HashMap<>();
    private int alone, calm;

    public LittleRedEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, SWEET)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HOOD_UP, false);
        builder.define(FERAL, false);
        builder.define(TURNING, 0);
    }

    /** Whether her hood is lifted back, her face showing. */
    public boolean isHoodUp() {
        return this.entityData.get(HOOD_UP);
    }

    /** Whether she shows what she truly is, and hunts. */
    public boolean isFeral() {
        return this.entityData.get(FERAL);
    }

    /** Whether she is turning right now. */
    public boolean isTurning() {
        return this.entityData.get(TURNING) > 0;
    }

    /** How far into her turning she is, in ticks (0 when she is not turning). */
    public int turned() {
        int turning = this.entityData.get(TURNING);
        return turning > 0 ? TURN_TICKS - turning : 0;
    }

    private boolean screaming() {
        int turned = this.turned();
        return this.isTurning() && turned >= SCREAM_FROM && turned <= SCREAM_TO;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6) {
            @Override
            public boolean canUse() {
                return !LittleRedEntity.this.isFeral() && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true) {
            @Override
            public boolean canUse() {
                return LittleRedEntity.this.isFeral() && !LittleRedEntity.this.isTurning() && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, BigBadWolfEntity.class, 12.0F, 1.2, 1.6) {
            @Override
            public boolean canUse() {
                return !LittleRedEntity.this.isFeral() && super.canUse();
            }
        });
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Monster.class, 6.0F, 1.1, 1.4, e -> !(e instanceof BigBadWolfEntity wolf) || !wolf.isDisguised()) {
            @Override
            public boolean canUse() {
                return !LittleRedEntity.this.isFeral() && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return LittleRedEntity.this.isFeral() && super.canUse();
            }
        });
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return LittleRedEntity.this.isFeral() && super.canUse();
            }
        });
    }

    /** Whether it is night in the waking world (the Dark Between has no days of its own). */
    private static boolean night(ServerLevel level) {
        return level.getServer().overworld().isDarkOutside();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.isTurning()) this.turningEffects();
            return;
        }
        if (!(this.level() instanceof ServerLevel level)) return;
        int turning = this.entityData.get(TURNING);
        if (turning > 0) {
            this.entityData.set(TURNING, turning - 1);
            this.turningTick(level, TURN_TICKS - turning);
        }
        if (this.isFeral()) {
            // By day, with no one left to hunt, she settles back into a little girl.
            this.calm = this.getTarget() == null ? this.calm + 1 : 0;
            if (this.calm > CALM_AFTER && !night(level)) this.calmDown(level);
            return;
        }
        if (this.tickCount % 5 != 0) return;
        Player near = level.getNearestPlayer(this, LOWER_AT);
        boolean someone = near != null && !near.isSpectator();
        if (someone && this.distanceTo(near) < LIFT_AT && !this.isHoodUp()) {
            this.entityData.set(HOOD_UP, true);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 0.6F, 1.4F);
        }
        this.alone = someone ? 0 : this.alone + 5;
        if (this.alone > 60 && this.isHoodUp()) this.entityData.set(HOOD_UP, false);
    }

    /** Her turning, on the server: the sounds of it, and her scream (a shake, and a wind at whoever is before her). */
    private void turningTick(ServerLevel level, int turned) {
        this.getNavigation().stop();
        for (float[] burn : BURNS) {
            if (turned == Math.round(burn[0] * 20)) {
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.7F, 0.6F + this.random.nextFloat() * 0.3F);
            }
        }
        if (turned == Math.round(BURNS[3][1] * 20)) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.VEX_CHARGE, SoundSource.HOSTILE, 1.6F, 0.5F);
        }
        LivingEntity at = this.getTarget();
        if (turned >= SCREAM_FROM - 2 && at != null) this.face(at);                  // she turns on whoever struck her
        if (turned == SCREAM_FROM) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 3.0F, 1.25F);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 1.7F);
            ShakeControl.shake(level, this.position(), (float) SCREAM_REACH + 6.0F, 5.0F, SCREAM_TO - SCREAM_FROM + 10);
        }
        if (turned == SCREAM_FROM + 9 || turned == SCREAM_FROM + 18) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 2.5F, 1.1F);
        }
        if (turned >= SCREAM_FROM && turned <= SCREAM_TO) {
            // The wind of it, pushing back whoever stands before her.
            Vec3 look = Vec3.directionFromRotation(0.0F, this.getYHeadRot());
            for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(SCREAM_REACH))) {
                if (player.isSpectator() || player.isCreative()) continue;
                Vec3 away = player.position().subtract(this.position());
                double d = away.length();
                if (d < 0.01 || d > SCREAM_REACH || away.normalize().dot(look) < 0.35) continue;
                double push = 0.07 * (1.0 - d / SCREAM_REACH);
                player.push(away.x / d * push, 0.01, away.z / d * push);
                player.hurtMarked = true;
            }
        }
    }

    /** Turns her whole body to look straight at {@code at}. */
    private void face(LivingEntity at) {
        float yaw = (float) (Math.toDegrees(Math.atan2(at.getZ() - this.getZ(), at.getX() - this.getX())) - 90.0);
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.getLookControl().setLookAt(at, 90.0F, 90.0F);
    }

    /**
     * Her turning, as it is seen: smoke and flame off each part of her as it burns away, and through her scream a wind of
     * dust, cloud and grit from her mouth to the player before her (or straight ahead), and dust torn up off the ground
     * along the way.
     */
    private void turningEffects() {
        Level level = this.level();
        float seconds = this.turned() / 20.0F;
        double yaw = Math.toRadians(this.yBodyRot);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = -fz, rz = fx;              // forward, and to her side
        // Where each part burns from (sideways, low, high, in blocks): both arms, both legs, her body, her head.
        double[][][] parts = {{{-0.25, 0.48, 0.74}, {0.25, 0.48, 0.74}}, {{-0.07, 0.04, 0.44}, {0.07, 0.04, 0.44}},
                {{0.0, 0.44, 0.78}}, {{0.0, 0.8, 1.2}}};
        for (int i = 0; i < BURNS.length; i++) {
            if (seconds < BURNS[i][0] || seconds > BURNS[i][1] + 0.1F) continue;
            for (double[] part : parts[i]) {
                for (int n = 0; n < 3; n++) {
                    double side = part[0] + (this.random.nextDouble() - 0.5) * 0.18, up = part[1] + this.random.nextDouble() * (part[2] - part[1]);
                    double forward = (this.random.nextDouble() - 0.5) * 0.2;
                    double x = this.getX() + rx * side + fx * forward, y = this.getY() + up, z = this.getZ() + rz * side + fz * forward;
                    level.addParticle(n == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, x, y, z, 0.0, 0.02 + this.random.nextDouble() * 0.03, 0.0);
                    level.addParticle(this.random.nextInt(3) == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, x, y, z,
                            (this.random.nextDouble() - 0.5) * 0.02, 0.04, (this.random.nextDouble() - 0.5) * 0.02);
                }
                if (this.random.nextInt(2) == 0) level.addParticle(ParticleTypes.ASH, this.getX() + rx * part[0], this.getY() + part[2], this.getZ() + rz * part[0], 0.0, 0.0, 0.0);
            }
        }
        if (!this.screaming()) return;
        Vec3 mouth = new Vec3(this.getX() + fx * 0.35, this.getY() + 1.0, this.getZ() + fz * 0.35);
        Player at = level.getNearestPlayer(this, SCREAM_REACH);
        Vec3 dir = at != null && !at.isSpectator() ? at.getEyePosition().subtract(mouth).normalize() : new Vec3(fx, 0.0, fz);
        for (int n = 0; n < 5; n++) {
            double spread = 0.18, speed = 0.7 + this.random.nextDouble() * 0.5;
            double vx = (dir.x + (this.random.nextDouble() - 0.5) * spread) * speed, vy = (dir.y + (this.random.nextDouble() - 0.5) * spread) * speed;
            double vz = (dir.z + (this.random.nextDouble() - 0.5) * spread) * speed;
            level.addParticle(n % 2 == 0 ? ParticleTypes.DUST_PLUME : ParticleTypes.CLOUD, mouth.x, mouth.y, mouth.z, vx, vy, vz);
        }
        if (this.random.nextInt(2) == 0) {
            double along = 0.5 + this.random.nextDouble() * 2.0;
            level.addParticle(ParticleTypes.SMALL_GUST, mouth.x + dir.x * along, mouth.y + dir.y * along, mouth.z + dir.z * along, 0.0, 0.0, 0.0);
        }
        // Grit torn up off the ground between her and them, blown their way.
        double reach = at != null ? Math.min(SCREAM_REACH, at.distanceTo(this)) : 6.0;
        for (int n = 0; n < 4; n++) {
            double along = 0.6 + this.random.nextDouble() * reach;
            double x = this.getX() + dir.x * along + (this.random.nextDouble() - 0.5) * 1.2, z = this.getZ() + dir.z * along + (this.random.nextDouble() - 0.5) * 1.2;
            BlockPos below = BlockPos.containing(x, this.getY() - 0.5, z);
            BlockState ground = level.getBlockState(below);
            if (ground.isAir()) continue;
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), x, this.getY() + 0.1, z, dir.x * 0.6, 0.15, dir.z * 0.6);
        }
    }

    /** Her true face. */
    private void turn(ServerLevel level, Player by) {
        this.entityData.set(FERAL, true);
        this.entityData.set(TURNING, TURN_TICKS);
        this.entityData.set(HOOD_UP, false);
        this.calm = 0;
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(HUNTING);
        this.setHealth(this.getMaxHealth());
        this.xpReward = 10;
        this.setTarget(by);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ALLAY_DEATH, SoundSource.HOSTILE, 1.2F, 0.4F);
        if (by instanceof ServerPlayer player) player.sendOverlayMessage(Component.translatable("message.whycantisleep.red_turns"));
    }

    private void calmDown(ServerLevel level) {
        this.entityData.set(FERAL, false);
        this.calm = 0;
        this.xpReward = 0;
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(SWEET);
        level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isTurning()) return false;                                     // nothing touches her while she turns
        if (!this.isFeral() && source.getEntity() instanceof Player player && !player.isCreative()
                && (night(level) || Mind.get(player).lucidity() < SLIPPING)) {
            this.turn(level, player);
            return false;                                                       // she shrugs that blow off as she turns
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isFeral()) return InteractionResult.PASS;
        if (!(this.level() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        if (!this.isHoodUp()) return InteractionResult.PASS;
        long now = level.getGameTime();
        Long last = this.gifts.get(player.getUUID());
        if (last != null && now - last < GIFT_EVERY) {
            level.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + 1.6, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            return InteractionResult.SUCCESS;                                   // nothing more for now, but a smile
        }
        this.gifts.put(player.getUUID(), now);
        boolean good = this.random.nextFloat() < GOOD_GIFT;
        ItemStack gift = good ? this.treat() : new ItemStack(RUBBISH[this.random.nextInt(RUBBISH.length)]);
        this.swing(InteractionHand.MAIN_HAND);
        if (!player.getInventory().add(gift)) player.drop(gift, false);
        Mind.update(player, mind -> mind.withMood(Math.min(Mind.MAX, mind.mood() + (good ? 6.0F : 1.0F))));
        level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 1.5, this.getZ(), good ? 4 : 1, 0.3, 0.2, 0.3, 0.0);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.NEUTRAL, 1.0F, good ? 1.3F : 1.0F);
        if (player instanceof ServerPlayer server) {
            server.sendOverlayMessage(Component.translatable(good ? "message.whycantisleep.red_gift_good" : "message.whycantisleep.red_gift"));
        }
        return InteractionResult.SUCCESS;
    }

    /** One of her rare good gifts. */
    private ItemStack treat() {
        int roll = this.random.nextInt(100);
        if (roll < 3) return new ItemStack(Items.GOLDEN_APPLE);
        if (roll < 10) return new ItemStack(Items.CAKE);
        if (roll < 30) return new ItemStack(Items.PUMPKIN_PIE, 2);
        if (roll < 55) return new ItemStack(Items.COOKIE, 4);
        if (roll < 80) return new ItemStack(Items.BREAD, 2);
        return new ItemStack(Items.APPLE, 2);
    }

    @Override
    public void die(DamageSource source) {
        if (!this.isFeral() && source.getEntity() instanceof ServerPlayer killer) {
            // That weighs on a mind.
            MindServer.addLucidity(killer, -GUILT);
            killer.sendOverlayMessage(Component.translatable("message.whycantisleep.red_guilt"));
        }
        super.die(source);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Feral", this.isFeral());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        if (input.getBooleanOr("Feral", false)) {
            this.entityData.set(FERAL, true);
            this.xpReward = 10;
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(HUNTING);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isFeral() ? SoundEvents.VEX_AMBIENT : SoundEvents.ALLAY_AMBIENT_WITH_ITEM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isFeral() ? SoundEvents.VEX_HURT : SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isFeral() ? SoundEvents.VEX_DEATH : SoundEvents.ALLAY_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return this.isFeral() ? 1.0F : 0.5F;
    }

    @Override
    public float getVoicePitch() {
        return this.isFeral() ? 0.6F : 1.25F;
    }
}
