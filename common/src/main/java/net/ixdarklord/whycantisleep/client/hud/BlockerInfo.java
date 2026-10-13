package net.ixdarklord.whycantisleep.client.hud;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/** The notice after a sleep blocked by monsters: how many there are and where the closest one is. */
public final class BlockerInfo {
    private BlockerInfo() {
    }

    /** "3 monsters nearby" over "Closest: Zombie, 6 blocks below", or null when no monster is known. */
    public static @Nullable SleepNotice describe(Player player, Collection<? extends Entity> blockers) {
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity blocker : blockers) {
            double distance = blocker.distanceToSqr(player);
            if (distance < closestDistance) {
                closest = blocker;
                closestDistance = distance;
            }
        }
        if (closest == null) return null;
        Component title = blockers.size() == 1
                ? Component.translatable("gui.whycantisleep.blockers.one")
                : Component.translatable("gui.whycantisleep.blockers.many", blockers.size());
        return new SleepNotice(SleepNotice.Kind.MONSTERS, title,
                Component.translatable("gui.whycantisleep.blockers.closest", closest.getName(), where(player, closest)));
    }

    // "6 blocks below" for a monster more under (or over) the player than beside them, else "6 blocks away".
    private static Component where(Player player, Entity entity) {
        double dx = entity.getX() - player.getX();
        double dy = entity.getY() - player.getY();
        double dz = entity.getZ() - player.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        String direction = Math.abs(dy) >= 2.0 && Math.abs(dy) > horizontal ? (dy < 0.0 ? "below" : "above") : "away";
        int blocks = Math.max(1, (int) Math.round(direction.equals("away") ? Math.sqrt(dx * dx + dy * dy + dz * dz) : Math.abs(dy)));
        return Component.translatable("gui.whycantisleep.blockers." + direction + (blocks == 1 ? ".one" : ""), blocks);
    }
}
