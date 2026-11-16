package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.coolcatcore.api.event.v2.common.ServerLifecycleEvents;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.hiding.HidingServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Night guards: an iron golem within {@link #GOLEM_RANGE} of a bed, and a player's own tamed wolf or cat sitting (or a
 * cat lying) within {@link #PET_RANGE} of it. They add to the night's sleep quality (the {@code night_guard} modifier),
 * and once a night one of them scares off a monster keeping its sleeper from lying down: the nearest to the bed is gone in
 * a puff, and with no other, they sleep.
 */
public final class NightGuards {
    private static final double GOLEM_RANGE = 16.0;
    private static final double PET_RANGE = 8.0;
    // The night (game day) each player's guards last scared something off.
    private static final Map<UUID, Long> SCARED = new HashMap<>();

    private NightGuards() {
    }

    public static void init() {
        ServerLifecycleEvents.STOPPED.register(server -> SCARED.clear());
    }

    /** The night guards about a bed for a player: iron golems near it, and their own pets sitting by it. */
    public static List<Mob> guards(ServerLevel level, ServerPlayer player, BlockPos bed) {
        List<Mob> guards = new ArrayList<>();
        if (!WCISServerConfig.NIGHT_GUARDS.get()) return guards;
        guards.addAll(level.getEntitiesOfClass(IronGolem.class, new AABB(bed).inflate(GOLEM_RANGE), IronGolem::isAlive));
        guards.addAll(level.getEntitiesOfClass(TamableAnimal.class, new AABB(bed).inflate(PET_RANGE), pet -> pet.isAlive()
                && (pet instanceof Wolf || pet instanceof Cat) && pet.isTame() && pet.isOwnedBy(player)
                && (pet.isInSittingPose() || pet instanceof Cat cat && cat.isLying())));
        return guards;
    }

    /** How many night guards are about a bed for a player. */
    public static int count(ServerLevel level, ServerPlayer player, BlockPos bed) {
        return guards(level, player, bed).size();
    }

    /**
     * The game's monster check, after the mod's rules: with a guard about and none scared off yet tonight, the blocker
     * nearest the bed is scared off (gone), and the rest are what is left.
     */
    public static <T extends Mob> List<T> scareOff(ServerPlayer player, BlockPos bed, List<T> blockers) {
        if (blockers.isEmpty() || !WCISServerConfig.NIGHT_GUARDS.get()) return blockers;
        ServerLevel level = player.serverLevel();
        long night = (level.getDayTime() + 12000L) / 24000L;
        Long last = SCARED.get(player.getUUID());
        if (last != null && last == night) return blockers;
        T blocker = blockers.stream().filter(NightGuards::mayScare).min(Comparator.comparingDouble(mob -> mob.distanceToSqr(Vec3.atCenterOf(bed)))).orElse(null);
        if (blocker == null) return blockers;
        Mob guard = nearest(guards(level, player, bed), blocker);
        if (guard == null) return blockers;
        SCARED.put(player.getUUID(), night);
        guard.getLookControl().setLookAt(blocker);
        // A golem swings, a pet calls out.
        if (guard instanceof IronGolem) level.playSound(null, guard.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        else guard.playAmbientSound();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, blocker.getX(), blocker.getY() + blocker.getBbHeight() / 2.0, blocker.getZ(), 14,
                blocker.getBbWidth() / 2.0, blocker.getBbHeight() / 3.0, blocker.getBbWidth() / 2.0, 0.02);
        level.playSound(null, blocker.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.4F, 0.6F);
        blocker.discard();
        player.sendSystemMessage(Component.translatableWithFallback("gui.whycantisleep.guard.scared", "%s scared off the %s.",
                guard.getDisplayName(), blocker.getDisplayName()), true);
        List<T> left = new ArrayList<>(blockers);
        left.remove(blocker);
        return left;
    }

    // Bosses, what hiding doesn't fool, and anything kept (named, tamed) stand their ground.
    private static boolean mayScare(Mob mob) {
        return !mob.getType().is(HidingServer.IGNORES_HIDING) && !mob.isPersistenceRequired();
    }

    private static @Nullable Mob nearest(List<Mob> guards, Mob to) {
        return guards.stream().min(Comparator.comparingDouble(guard -> guard.distanceToSqr(to))).orElse(null);
    }
}
