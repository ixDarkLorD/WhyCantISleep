package net.ixdarklord.whycantisleep.lurker;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.ixdarklord.whycantisleep.sound.WCISSounds;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * The thing under the bed, made real: it comes for one player, the one whose dread called it, and for nobody else.
 * It is gone with the daylight, when its player is out of reach, or after a few minutes; killing it ends the dread.
 * <p>
 * The same entity, never known to the server, is what a haunted player's client shows as a hallucination
 * ({@code LurkerHaunt}).
 */
public class LurkerEntity extends Monster {
    private static final int LIFETIME_TICKS = 20 * 180;
    // How long it waits, with its player gone or out of reach, before it gives up.
    private static final int LOST_TICKS = 20 * 20;
    private static final double REACH = 64.0;

    /** Set on clients: draws the darkness that clings to it, each tick (world effects are client code). */
    public static Consumer<LurkerEntity> clientTick = lurker -> {
    };

    private @Nullable UUID victim;
    private int lostTicks;
    private boolean hallucination;

    public LurkerEntity(EntityType<? extends LurkerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, REACH);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        // No target goals: it only ever hunts its own player, set in tick().
    }

    public @Nullable UUID victim() {
        return this.victim;
    }

    public void setVictim(UUID victim) {
        this.victim = victim;
    }

    /** Client side: whether this one is only in its player's head, and so drawn as something not quite there. */
    public boolean isHallucination() {
        return this.hallucination;
    }

    public void setHallucination(boolean hallucination) {
        this.hallucination = hallucination;
    }

    /** It turns on nobody but its own player, whoever strikes it. */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target != null && target.getUUID().equals(this.victim) ? target : null);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            clientTick.accept(this);
        } else if (this.level() instanceof ServerLevel level && this.isAlive() && this.tickCount % 10 == 0) {
            Player player = this.victim == null ? null : level.getPlayerByUUID(this.victim);
            boolean huntable = player != null && player.isAlive() && !player.isCreative() && !player.isSpectator() && player.distanceToSqr(this) < REACH * REACH;
            this.setTarget(huntable ? player : null);
            this.lostTicks = huntable ? 0 : this.lostTicks + 10;
            boolean daylight = level.isBrightOutside() && level.canSeeSky(this.blockPosition());
            if (daylight || this.lostTicks >= LOST_TICKS || this.tickCount > LIFETIME_TICKS) this.vanish(level);
        }
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
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.victim = input.read("Victim", UUIDUtil.CODEC).orElse(null);
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
