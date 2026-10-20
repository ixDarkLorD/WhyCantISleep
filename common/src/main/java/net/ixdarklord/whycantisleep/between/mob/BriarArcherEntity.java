package net.ixdarklord.whycantisleep.between.mob;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Briar Archer: a gaunt figure of woven roots and thorns keeping watch in the maze, its bow strung with sinew. Its
 * arrows are tipped with the garden's poison.
 */
public class BriarArcherEntity extends Skeleton {
    public BriarArcherEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Skeleton.createAttributes().add(Attributes.MAX_HEALTH, 18.0).add(Attributes.MOVEMENT_SPEED, 0.24);
    }

    @Override
    protected AbstractArrow getArrow(ItemStack arrow, float power, ItemStack weapon) {
        AbstractArrow shot = super.getArrow(arrow, power, weapon);
        if (shot instanceof Arrow tipped) tipped.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
        return shot;
    }

    @Override
    public boolean isFreezing() {
        return false;
    }
}
