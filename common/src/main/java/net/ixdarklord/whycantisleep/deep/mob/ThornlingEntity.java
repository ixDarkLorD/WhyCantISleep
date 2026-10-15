package net.ixdarklord.whycantisleep.deep.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;

/**
 * The Thornling: a knot of brambles on six thorned legs, low and quick. It climbs the hedges as a spider climbs a wall
 * and leaps down on whoever passes.
 */
public class ThornlingEntity extends Spider {
    public ThornlingEntity(EntityType<? extends Spider> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Spider.createAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 3.0);
    }
}
