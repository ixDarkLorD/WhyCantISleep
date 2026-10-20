package net.ixdarklord.whycantisleep.between.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/**
 * The Mourner: one of the garden's dead, risen in its burial shroud, weeping blood down its face. It shambles after the
 * living and its touch leaves them heavy with grief (slowness). It climbs out of the tombs and graves, and walks the
 * tears of the lake without drowning into anything else.
 */
public class MournerEntity extends Zombie {
    public MournerEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.21)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hurt = super.doHurtTarget(level, target);
        if (hurt && target instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0), this);
        return hurt;
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
}
