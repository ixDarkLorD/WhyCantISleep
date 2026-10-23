package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The game's own "monsters nearby" check ({@code ServerPlayer.startSleepInBed}), repeated to learn which monsters it
 * found: the box and the test must stay the same as the game's, so only the mobs that really blocked the sleep are named.
 * <p>
 * The mod's rules change the game's check, here and in the game's own ({@code ServerPlayerMixin}), the same way:
 * mobs in {@code #whycantisleep:does_not_block_sleep} never block a sleep, those in {@code #whycantisleep:blocks_sleep}
 * do even when they aren't monsters, and a bed beside another player asleep tolerates a few.
 */
public final class SleepChecks {
    /** The game's range: 8 blocks to each side of the bed's head and 5 up and down. */
    public static final double HORIZONTAL_RANGE = 8.0;
    public static final double VERTICAL_RANGE = 5.0;
    /** Mobs that block a sleep, monsters or not. */
    public static final TagKey<EntityType<?>> BLOCKS_SLEEP = TagKey.create(Registries.ENTITY_TYPE, WhyCantISleep.id("blocks_sleep"));
    /** Monsters that never block a sleep. */
    public static final TagKey<EntityType<?>> DOES_NOT_BLOCK_SLEEP = TagKey.create(Registries.ENTITY_TYPE, WhyCantISleep.id("does_not_block_sleep"));
    // How near another sleeper's bed must be for this one to count as shared.
    private static final double SHARED_RANGE = 3.0;

    private SleepChecks() {
    }

    /** The box the game searches, around the bottom center of the bed's head. */
    public static AABB box(BlockPos bed) {
        Vec3 center = Vec3.atBottomCenterOf(bed);
        return new AABB(center.x() - HORIZONTAL_RANGE, center.y() - VERTICAL_RANGE, center.z() - HORIZONTAL_RANGE,
                center.x() + HORIZONTAL_RANGE, center.y() + VERTICAL_RANGE, center.z() + HORIZONTAL_RANGE);
    }

    /** Server side: exactly the mobs that keep this player from sleeping in the bed, the mod's rules applied. */
    public static List<Mob> blockers(ServerPlayer player, BlockPos bed) {
        return applyRules(player, bed, player.level().getEntitiesOfClass(Monster.class, box(bed), monster -> monster.isPreventingPlayerRest(player)));
    }

    /**
     * The mod's rules, over the monsters the game found: the tags, and a shared bed's tolerance. Empty when the sleep
     * may go ahead.
     */
    public static List<Mob> applyRules(ServerPlayer player, BlockPos bed, List<? extends Mob> found) {
        List<Mob> blockers = new ArrayList<>();
        for (Mob mob : found) {
            if (!mob.getType().is(DOES_NOT_BLOCK_SLEEP)) blockers.add(mob);
        }
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, box(bed), mob -> mob.isAlive() && mob.getType().is(BLOCKS_SLEEP))) {
            if (!blockers.contains(mob)) blockers.add(mob);
        }
        return blockers.size() <= tolerance(player, bed) ? List.of() : blockers;
    }

    // How many blockers a bed lets by: none, or a few when another player sleeps in a bed beside it.
    private static int tolerance(ServerPlayer player, BlockPos bed) {
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other == player || !other.isSleeping()) continue;
            BlockPos theirs = other.getSleepingPos().orElse(null);
            if (theirs != null && theirs.closerThan(bed, SHARED_RANGE)) return WCISServerConfig.SHARED_BED_TOLERANCE.get();
        }
        return 0;
    }

    /**
     * Client side, for servers without the mod: the monsters the client knows of in the game's box. A zombified piglin
     * only blocks sleep while angry at the player, which clients aren't told; one that looks aggressive is taken as angry.
     */
    public static List<Monster> blockersOnClient(Level level, Player player, BlockPos bed) {
        return level.getEntitiesOfClass(Monster.class, box(bed), monster -> !(monster instanceof ZombifiedPiglin piglin) || piglin.isAggressive());
    }
}
