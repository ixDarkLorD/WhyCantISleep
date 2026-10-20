package net.ixdarklord.whycantisleep.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * A weak spot on a boss, where it is in the world now and how big: an exposed heart, a cracked carapace. A player's
 * blow strikes it ({@link #struckBy}) when they are within reach, in front of the boss, and looking at it.
 *
 * @param at     where it is, in the world
 * @param radius how near their look must pass to it, in blocks
 */
public record CriticalPoint(Vec3 at, double radius) {
    /** Whether {@code player}'s blow, from where they stand and look, strikes it on {@code boss}. */
    public boolean struckBy(Player player, LivingEntity boss, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 to = this.at.subtract(eye);
        double along = to.dot(player.getLookAngle());
        if (along < 0.0 || along > reach + this.radius) return false;
        // How near their line of sight passes to it.
        Vec3 nearest = eye.add(player.getLookAngle().scale(along));
        if (nearest.distanceToSqr(this.at) > this.radius * this.radius) return false;
        // And from in front of the boss (a heart is not struck through its back).
        Vec3 facing = Vec3.directionFromRotation(0.0F, boss.getYRot());
        Vec3 from = player.position().subtract(boss.position());
        return new Vec3(from.x, 0.0, from.z).normalize().dot(facing) > 0.2;
    }
}
