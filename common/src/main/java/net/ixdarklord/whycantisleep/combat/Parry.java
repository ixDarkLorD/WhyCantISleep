package net.ixdarklord.whycantisleep.combat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Parrying: a shield (anything that blocks attacks) raised only a moment before a blow lands, facing it. Raised too
 * early it only blocks, as the game's shields do; raised in time, the blow is turned aside whole.
 */
public final class Parry {
    private Parry() {
    }

    /** Whether {@code defender} parries a blow from {@code attacker} landing now: a shield raised within {@code window} ticks, facing it. */
    public static boolean parries(LivingEntity defender, LivingEntity attacker, int window) {
        if (!defender.isUsingItem() || !defender.getUseItem().has(DataComponents.BLOCKS_ATTACKS)) return false;
        if (defender.getTicksUsingItem() > window) return false;
        Vec3 look = defender.getViewVector(1.0F);
        Vec3 to = attacker.position().subtract(defender.position());
        return new Vec3(to.x, 0.0, to.z).normalize().dot(new Vec3(look.x, 0.0, look.z).normalize()) > 0.0;
    }
}
