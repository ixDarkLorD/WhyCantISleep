package net.ixdarklord.whycantisleep.lurker;

import net.ixdarklord.whycantisleep.deep.WCISItems;
import net.ixdarklord.whycantisleep.sound.WCISSounds;
import net.ixdarklord.whycantisleep.mind.Mind;
import net.ixdarklord.whycantisleep.mind.MindServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * The thing under the bed, made real: it comes for one player, the one whose lucidity gave way, and for nobody else.
 * <p>
 * It begins by {@linkplain #isHunting() watching}: it stands where it came out and stares, and its player's world
 * goes dark but for the light it gives off ({@code LurkerPresence}). The moment its player looks at it, it hunts. A
 * player who never looks is left alone, for now: after a while it is gone, and {@link LurkerServer} counts the snub.
 * Come back after being ignored twice or more, it {@linkplain #lure() lures}: it calls out, and its player's view is
 * drawn slowly round towards it.
 * <p>
 * Hunting, it is gone with the daylight, after a few minutes, or when its player gets well clear of it and stays
 * clear for a while; killing it gives a good part of their lucidity back. It comes in several {@linkplain LurkerKind
 * kinds}, each with something of its own it does while it hunts. The same entity, never known to the server, is what
 * a haunted player's client shows as a hallucination ({@code LurkerHaunt}).
 */
public class LurkerEntity extends Monster {
    private static final EntityDataAccessor<Boolean> HUNTING = SynchedEntityData.defineId(LurkerEntity.class, EntityDataSerializers.BOOLEAN);
    // Its player's network id, so that player's client knows the watcher is theirs; 0 for none.
    private static final EntityDataAccessor<Integer> VICTIM_ID = SynchedEntityData.defineId(LurkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LURE = SynchedEntityData.defineId(LurkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(LurkerEntity.class, EntityDataSerializers.INT);

    private static final int LIFETIME_TICKS = 20 * 180;
    // How long it waits, with its player gone or out of reach, before it gives up.
    private static final int LOST_TICKS = 20 * 20;
    private static final double REACH = 64.0;
    /** How long it watches, unlooked at, before it leaves. */
    public static final int WATCH_TICKS = 20 * 25;
    // How squarely its player must face it, as the cosine of the angle, and for how many ticks, to have looked at it.
    private static final double LOOK_DOT = 0.93;
    private static final int LOOK_TICKS = 5;
    // A player who walks right into it has found it, looking or not.
    private static final double TOUCH_DISTANCE = 3.0;
    /** Hunting, a player who stays this many blocks clear of it for {@link #ESCAPE_TICKS} has got away. */
    public static final double ESCAPE_DISTANCE = 14.0;
    public static final int ESCAPE_TICKS = 20 * 6;
    /** How many times it must have been ignored in a row before it lures. */
    public static final int LURE_FROM = 2;
    private static final int LURE_CALL_INTERVAL = 50;
    // The Flicker: how often it may step behind its player, how far off it must be to bother, how far behind them it
    // lands, and how squarely they must be facing it (as a cosine) for it to stay put.
    private static final int BLINK_INTERVAL = 20 * 3;
    private static final double BLINK_FROM = 5.0;
    private static final double BLINK_BEHIND = 3.5;
    private static final double BLINK_SEEN_DOT = 0.35;
    // The Brute: how often it roars, from how far its player feels it, and what it costs their lucidity.
    private static final int ROAR_INTERVAL = 20 * 8;
    private static final double ROAR_RANGE = 14.0;
    private static final float ROAR_LUCIDITY = 2.0F;
    // The Leech: what its bite takes from its player's mood, and gives back to it in health.
    private static final float BITE_MOOD = 4.0F;
    private static final float BITE_HEAL = 4.0F;

    /** Set on clients: draws the darkness that clings to it, each tick (world effects are client code). */
    public static Consumer<LurkerEntity> clientTick = lurker -> {
    };

    private @Nullable UUID victim;
    private int lostTicks;
    private int watchedTicks;
    private int lookedTicks;
    private int outrunTicks;
    private int abilityTicks;
    // Client side: how far into its hunting stance it is, 0 to 1, this tick and the last.
    private float huntAnim;
    private float huntAnimO;
    private boolean hallucination;

    public LurkerEntity(EntityType<? extends LurkerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                // About a player's sprint, so running flat out holds the distance and sprint-jumping opens it.
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, REACH);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HUNTING, false);
        builder.define(VICTIM_ID, 0);
        builder.define(LURE, 0);
        builder.define(KIND, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        // No wandering and no target goals: it stands and watches, then hunts its own player, both from tick().
    }

    public @Nullable UUID victim() {
        return this.victim;
    }

    public void setVictim(UUID victim) {
        this.victim = victim;
    }

    public LurkerKind kind() {
        return LurkerKind.byId(this.entityData.get(KIND));
    }

    /** Makes it one of the kinds: its build, and with it a full measure of that kind's health. */
    public void setKind(LurkerKind kind) {
        this.entityData.set(KIND, kind.ordinal());
        this.setBase(Attributes.MAX_HEALTH, kind.health);
        this.setBase(Attributes.MOVEMENT_SPEED, kind.speed);
        this.setBase(Attributes.ATTACK_DAMAGE, kind.damage);
        this.setBase(Attributes.SCALE, kind.scale);
        this.setBase(Attributes.KNOCKBACK_RESISTANCE, kind.knockbackResistance);
        this.setHealth(this.getMaxHealth());
    }

    private void setBase(Holder<Attribute> attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    /** It is named for its kind: a Flicker, a Brute. */
    @Override
    protected Component getTypeName() {
        return Component.translatable("entity.whycantisleep.lurker." + this.kind().key());
    }

    /** Whether it has been looked at and is coming; until then it only watches. */
    public boolean isHunting() {
        return this.entityData.get(HUNTING);
    }

    /** Whether this is the Lurker of the player with this network id. */
    public boolean isAfter(int playerId) {
        return this.entityData.get(VICTIM_ID) == playerId;
    }

    /** How many times in a row its player ignored it before this coming; from {@link #LURE_FROM} it lures. */
    public int lure() {
        return this.entityData.get(LURE);
    }

    public void setLure(int ignored) {
        this.entityData.set(LURE, ignored);
    }

    /** Client side: how far into its hunting stance it is, 0 (watching) to 1. */
    public float huntAmount(float partialTick) {
        return this.huntAnimO + (this.huntAnim - this.huntAnimO) * partialTick;
    }

    /** Client side: whether this one is only in its player's head, and so drawn as something not quite there. */
    public boolean isHallucination() {
        return this.hallucination;
    }

    public void setHallucination(boolean hallucination) {
        this.hallucination = hallucination;
    }

    /** It turns on nobody but its own player, whoever strikes it; and not even on them until it hunts. */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target != null && this.isHunting() && target.getUUID().equals(this.victim) ? target : null);
    }

    /** Striking it is one way of finding it. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && !this.isHunting() && this.isAlive()) this.startHunting();
        return hurt;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.huntAnimO = this.huntAnim;
            this.huntAnim += ((this.isHunting() ? 1.0F : 0.0F) - this.huntAnim) * 0.14F;
            clientTick.accept(this);
            return;
        }
        if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) return;
        Player player = this.victim == null ? null : level.getPlayerByUUID(this.victim);
        // The Weeping Veil hides its wearer: to a Lurker they are as good as gone.
        boolean present = player != null && player.isAlive() && !player.isCreative() && !player.isSpectator() && player.distanceToSqr(this) < REACH * REACH
                && !WCISItems.wearsVeil(player);
        this.entityData.set(VICTIM_ID, present ? player.getId() : 0);
        if (!this.isHunting() && present) this.watch(level, player);
        // Out of reach altogether counts as clear of it too.
        if (this.isHunting() && player != null && player.isAlive()) this.chase(level, player);

        if (this.tickCount % 10 == 0) {
            this.setTarget(present ? player : null);
            this.lostTicks = present ? 0 : this.lostTicks + 10;
            boolean daylight = level.isBrightOutside() && level.canSeeSky(this.blockPosition());
            if (daylight || this.lostTicks >= LOST_TICKS || this.tickCount > LIFETIME_TICKS + WATCH_TICKS) this.vanish(level);
        }
    }

    // Watching: it stares at its player and waits to be seen. Seen, it hunts; never seen, it leaves.
    private void watch(ServerLevel level, Player player) {
        this.getNavigation().stop();
        this.getLookControl().setLookAt(player, 30.0F, 30.0F);
        Vec3 toLurker = this.getEyePosition().subtract(player.getEyePosition());
        boolean looked = toLurker.normalize().dot(player.getLookAngle()) > LOOK_DOT && player.hasLineOfSight(this);
        this.lookedTicks = looked ? this.lookedTicks + 1 : 0;
        if (this.lookedTicks >= LOOK_TICKS || toLurker.lengthSqr() < TOUCH_DISTANCE * TOUCH_DISTANCE) {
            this.startHunting();
            return;
        }
        // Ignored before, it calls out to be looked at.
        if (this.lure() >= LURE_FROM && this.watchedTicks % LURE_CALL_INTERVAL == 0) {
            this.playSound(WCISSounds.LURKER_LURE, 1.0F, 0.8F + this.random.nextFloat() * 0.3F);
        }
        if (++this.watchedTicks >= WATCH_TICKS) {
            LurkerServer.onIgnored(this);
            this.vanish(level);
        }
    }

    // Hunting: a player who gets well clear of it, and stays clear, has got away. Meanwhile it does what its kind does.
    private void chase(ServerLevel level, Player player) {
        this.abilityTicks++;
        switch (this.kind()) {
            case FLICKER -> this.blink(level, player);
            case BRUTE -> this.roar(level, player);
            default -> {
            }
        }
        this.outrunTicks = player.distanceToSqr(this) > ESCAPE_DISTANCE * ESCAPE_DISTANCE ? this.outrunTicks + 1 : Math.max(0, this.outrunTicks - 2);
        if (this.outrunTicks >= ESCAPE_TICKS) {
            LurkerServer.onEscaped(this);
            this.vanish(level);
        }
    }

    // The Flicker: when its player isn't facing it, it is gone from where it was and standing behind them.
    private void blink(ServerLevel level, Player player) {
        if (this.abilityTicks < BLINK_INTERVAL || player.distanceToSqr(this) < BLINK_FROM * BLINK_FROM) return;
        Vec3 toLurker = this.getEyePosition().subtract(player.getEyePosition()).normalize();
        if (toLurker.dot(player.getLookAngle()) > BLINK_SEEN_DOT) return;
        Vec3 behind = player.position().subtract(player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(BLINK_BEHIND));
        BlockPos spot = LurkerServer.standableNear(level, BlockPos.containing(behind), 1, this.random);
        if (spot == null) return;
        this.abilityTicks = 0;
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.4, this.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        this.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        this.getNavigation().stop();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.4, this.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        this.playSound(WCISSounds.LURKER_VANISH, 0.7F, 1.5F);
    }

    // The Brute: a roar its player feels in their legs, their eyes and their head.
    private void roar(ServerLevel level, Player player) {
        if (this.abilityTicks < ROAR_INTERVAL || player.distanceToSqr(this) > ROAR_RANGE * ROAR_RANGE) return;
        this.abilityTicks = 0;
        this.playSound(WCISSounds.LURKER_NOTICE, 2.0F, 0.5F);
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), this);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0), this);
        MindServer.addLucidity(player, -ROAR_LUCIDITY);
        level.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getEyeY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
    }

    // The Leech: its bite leaves its player starving, weak and low, and feeds it.
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hurt = super.doHurtTarget(level, target);
        if (hurt && this.kind() == LurkerKind.LEECH && target instanceof Player player && player.getUUID().equals(this.victim)) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 1), this);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), this);
            Mind.update(player, mind -> mind.withMood(Math.max(0.0F, mind.mood() - BITE_MOOD)));
            this.heal(BITE_HEAL);
        }
        return hurt;
    }

    private void startHunting() {
        this.entityData.set(HUNTING, true);
        this.playSound(WCISSounds.LURKER_NOTICE, 1.2F, 1.0F);
        LurkerServer.onHuntStarted(this);
    }

    // Gone in a puff of smoke, with nothing left behind.
    private void vanish(ServerLevel level) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.4, this.getZ(), 20, 0.3, 0.3, 0.3, 0.02);
        this.playSound(WCISSounds.LURKER_VANISH, 0.6F, 1.0F);
        this.discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel) LurkerServer.onLurkerKilled(this);
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity entity, DamageSource source) {
        boolean result = super.killedEntity(level, entity, source);
        if (entity.getUUID().equals(this.victim)) {
            LurkerServer.onVictimKilled((Player) entity);
            this.vanish(level);
        }
        return result;
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!this.level().isClientSide()) LurkerServer.forget(this);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.victim != null) output.store("Victim", UUIDUtil.CODEC, this.victim);
        output.putBoolean("Hunting", this.isHunting());
        output.putString("Kind", this.kind().key());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.victim = input.read("Victim", UUIDUtil.CODEC).orElse(null);
        this.entityData.set(HUNTING, input.getBooleanOr("Hunting", false));
        this.setKind(LurkerKind.byKey(input.getStringOr("Kind", LurkerKind.STALKER.key())));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return WCISSounds.LURKER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return WCISSounds.LURKER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return WCISSounds.LURKER_DEATH;
    }
}
