package net.ixdarklord.whycantisleep.sleep;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The game's own "monsters nearby" check ({@code ServerPlayer.startSleepInBed}), repeated to learn which monsters it
 * found: the box and the test must stay the same as the game's, so only the mobs that really blocked the sleep are named.
 */
public final class SleepChecks {
    /** The game's range: 8 blocks to each side of the bed's head and 5 up and down. */
    public static final double HORIZONTAL_RANGE = 8.0;
    public static final double VERTICAL_RANGE = 5.0;

    private SleepChecks() {
    }

    /** The box the game searches, around the bottom center of the bed's head. */
    public static AABB box(BlockPos bed) {
        Vec3 center = Vec3.atBottomCenterOf(bed);
        return new AABB(center.x() - HORIZONTAL_RANGE, center.y() - VERTICAL_RANGE, center.z() - HORIZONTAL_RANGE,
                center.x() + HORIZONTAL_RANGE, center.y() + VERTICAL_RANGE, center.z() + HORIZONTAL_RANGE);
    }

    /** Server side: exactly the monsters that keep this player from sleeping in the bed. */
    public static List<Monster> blockers(ServerPlayer player, BlockPos bed) {
        return player.level().getEntitiesOfClass(Monster.class, box(bed), monster -> monster.isPreventingPlayerRest(player.level(), player));
    }

    /**
     * Client side, for servers without the mod: the monsters the client knows of in the game's box. A zombified piglin
     * only blocks sleep while angry at the player, which clients aren't told; one that looks aggressive is taken as angry.
     */
    public static List<Monster> blockersOnClient(Level level, Player player, BlockPos bed) {
        return level.getEntitiesOfClass(Monster.class, box(bed), monster -> !(monster instanceof ZombifiedPiglin piglin) || piglin.isAggressive());
    }
}
