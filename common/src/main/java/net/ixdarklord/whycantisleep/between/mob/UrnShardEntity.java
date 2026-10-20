package net.ixdarklord.whycantisleep.between.mob;

import net.ixdarklord.whycantisleep.between.WCISItems;
import net.ixdarklord.whycantisleep.lurker.WCISEntities;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A cursed shard of pottery, thrown by a haunted urn: it cuts whoever it strikes and shatters into dust. */
public class UrnShardEntity extends ThrowableItemProjectile {
    private static final float DAMAGE = 4.0F;

    public UrnShardEntity(EntityType<? extends UrnShardEntity> type, Level level) {
        super(type, level);
    }

    public UrnShardEntity(Level level, LivingEntity owner) {
        super(WCISEntities.URN_SHARD.get(), owner, level, new ItemStack(WCISItems.URN_SHARD.get()));
    }

    @Override
    protected Item getDefaultItem() {
        return WCISItems.URN_SHARD.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.01;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level() instanceof ServerLevel level && !(result.getEntity() instanceof HauntedUrnEntity)) {
            result.getEntity().hurtServer(level, this.damageSources().thrown(this, this.getOwner()), DAMAGE);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, this.getItem().getItem()), this.getX(), this.getY(), this.getZ(), 8,
                    0.1, 0.1, 0.1, 0.08);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.DECORATED_POT_SHATTER, SoundSource.HOSTILE, 0.5F, 1.4F);
            this.discard();
        }
    }
}
