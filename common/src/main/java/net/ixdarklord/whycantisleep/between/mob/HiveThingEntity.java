package net.ixdarklord.whycantisleep.between.mob;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The Hive-Thing (trypophobia): a lumbering heap of a body pocked all over with holes, something moving in every one,
 * a single eye deep in the holes of its face, insects drifting round it. Near it, specks crawl across the edges of your
 * sight (WildsClient). Every few seconds it shudders and its holes empty: a swarm settles where its prey stands, stinging
 * whoever is in it and leaving them sick to the stomach, for a few seconds; striking it sets more loose. Its swarm,
 * caught in a jar, can be set on something else: the Swarm Jar.
 */
public class HiveThingEntity extends Monster {
    private static final DustParticleOptions BUGS = new DustParticleOptions(0x1C140E, 0.5F);
    private static final int SWARM_EVERY = 90, SWARM_LASTS = 100;
    private static final double SWARM_REACH = 9.0, SWARM_SIZE = 3.2;
    // The swarms it has set loose: where, and how many ticks they have left.
    private final List<double[]> swarms = new ArrayList<>();
    private int swarm = 50;

    public HiveThingEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 14;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 36.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) return;
        if (this.tickCount % 4 == 0) level.sendParticles(BUGS, this.getX(), this.getY() + 1.4, this.getZ(), 3, 0.7, 0.8, 0.7, 0.02);
        LivingEntity target = this.getTarget();
        if (--this.swarm <= 0 && target != null && target.isAlive() && this.distanceTo(target) < SWARM_REACH) {
            this.swarm = SWARM_EVERY;
            this.release(level, target.position());
        }
        // Each swarm buzzes where it settled, stinging what is in it.
        for (int i = this.swarms.size() - 1; i >= 0; i--) {
            double[] s = this.swarms.get(i);
            s[3]--;
            level.sendParticles(BUGS, s[0], s[1] + 1.0, s[2], 6, SWARM_SIZE * 0.5, 0.8, SWARM_SIZE * 0.5, 0.05);
            if ((int) s[3] % 10 == 0) {
                Vec3 at = new Vec3(s[0], s[1], s[2]);
                for (LivingEntity stung : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().move(at.subtract(this.position())).inflate(SWARM_SIZE),
                        e -> !(e instanceof HiveThingEntity))) {
                    if (stung instanceof Player player && (player.isCreative() || player.isSpectator())) continue;
                    if (stung.hurtServer(level, this.damageSources().mobAttack(this), 1.0F)) {
                        stung.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 1), this);
                        stung.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0), this);
                    }
                }
            }
            if (s[3] <= 0) this.swarms.remove(i);
        }
    }

    /** It shudders and its holes empty, a swarm settling where its prey stands. */
    private void release(ServerLevel level, Vec3 at) {
        this.swing(InteractionHand.MAIN_HAND);
        this.swarms.add(new double[]{at.x, at.y, at.z, SWARM_LASTS});
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEEHIVE_EXIT, SoundSource.HOSTILE, 1.4F, 0.5F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.0F, 0.5F);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && this.isAlive() && this.random.nextInt(3) == 0) this.release(level, this.position());   // struck, more pour out
        return hurt;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BEE_LOOP_AGGRESSIVE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }
}
