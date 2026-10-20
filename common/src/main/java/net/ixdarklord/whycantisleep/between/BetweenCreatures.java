package net.ixdarklord.whycantisleep.between;

import net.ixdarklord.coolcatcore.api.event.v2.common.ServerTickEvents;
import net.ixdarklord.whycantisleep.between.mob.DreadMantaEntity;
import net.ixdarklord.whycantisleep.between.mob.DreamEaterEntity;
import net.ixdarklord.whycantisleep.between.mob.FacelessEntity;
import net.ixdarklord.whycantisleep.between.mob.SleepwalkerEntity;
import net.ixdarklord.whycantisleep.between.mob.SomnimothEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;
import net.ixdarklord.whycantisleep.lurker.WCISEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The open Dark Between's creatures, kept about its players (its biome spawns nothing, and the game's own rules would
 * spawn no monster under its open sky): now and then, near each player there, a few Sleepwalkers set down on the
 * islands round them (never in the garden, which keeps its own) and a Dread Manta gliding in the air, both out of
 * their sight's way, 20 blocks off or more. Now and then too a Dream Eater grazing on the islands, a swarm of
 * Somnimoths in the air, and, rarely, a Faceless, set down behind them. Not in Peaceful.
 * <p>
 * Its wild creatures ({@link #WILDS}), of no structure, are kept the same way, each with how many may be about a
 * player, how often one comes, how far off, whether on the ground or in the air, and in what number: the Night Mare,
 * Hollow Hounds (a pack), the Lullaby, Gazers, Tick-Tocks, Stitchlings, Gloom Jellies and Dream Hares. The jellies and
 * hares, being no monsters, come in Peaceful too.
 */
public final class BetweenCreatures {
    private static final int EVERY = 120;
    private static final int SLEEPWALKERS = 3, MANTAS = 1, EATERS = 1, MOTHS = 3, FACELESS = 1;

    private BetweenCreatures() {
    }

    public static void init() {
        ServerTickEvents.END_LEVEL.register(BetweenCreatures::tick);
    }

    /**
     * A wild creature's place in the Dark Between: at most {@code max} within {@code reach} of a player; one try in
     * {@code oneIn} brings more, {@code group} to {@code group + extra} together, {@code near} to {@code near + span}
     * blocks off, on the ground ({@code air} NaN) or in the air, {@code air} blocks above the player (below, if
     * negative), give or take {@code airSpread}.
     */
    private record Wild(Supplier<? extends EntityType<? extends Mob>> type, Class<? extends Mob> kind, int max, double reach, int oneIn,
                        int group, int extra, double near, double span, boolean monster, double air, double airSpread) {
    }

    private static final List<Wild> WILDS = List.of(
            new Wild(() -> WCISEntities.NIGHT_MARE.get(), net.ixdarklord.whycantisleep.between.mob.NightMareEntity.class, 1, 80, 5, 1, 0, 28, 20, true, Double.NaN, 0),
            new Wild(() -> WCISEntities.HOLLOW_HOUND.get(), net.ixdarklord.whycantisleep.between.mob.HollowHoundEntity.class, 1, 64, 5, 2, 2, 26, 18, true, Double.NaN, 0),
            new Wild(() -> WCISEntities.LULLABY.get(), net.ixdarklord.whycantisleep.between.mob.LullabyEntity.class, 1, 64, 5, 1, 0, 22, 18, true, Double.NaN, 0),
            new Wild(() -> WCISEntities.GAZER.get(), net.ixdarklord.whycantisleep.between.mob.GazerEntity.class, 2, 64, 4, 1, 0, 20, 14, true, 7, 3),
            new Wild(() -> WCISEntities.TICK_TOCK.get(), net.ixdarklord.whycantisleep.between.mob.TickTockEntity.class, 2, 48, 3, 1, 1, 20, 16, true, Double.NaN, 0),
            new Wild(() -> WCISEntities.STITCHLING.get(), net.ixdarklord.whycantisleep.between.mob.StitchlingEntity.class, 3, 48, 3, 1, 1, 20, 16, true, Double.NaN, 0),
            new Wild(() -> WCISEntities.GLOOM_JELLY.get(), net.ixdarklord.whycantisleep.between.mob.GloomJellyEntity.class, 3, 64, 3, 1, 1, 14, 18, false, 0, 8),
            new Wild(() -> WCISEntities.GRINNING_CLOWN.get(), net.ixdarklord.whycantisleep.between.mob.GrinningClownEntity.class, 1, 80, 6, 1, 0, 26, 18, true, Double.NaN, 0),
            new Wild(() -> WCISEntities.CLOSET_BOOGEYMAN.get(), net.ixdarklord.whycantisleep.between.mob.ClosetBoogeymanEntity.class, 1, 80, 6, 1, 0, 24, 20, true, Double.NaN, 0),
            new Wild(() -> WCISEntities.BIG_BAD_WOLF.get(), net.ixdarklord.whycantisleep.between.mob.BigBadWolfEntity.class, 1, 80, 6, 1, 0, 26, 18, true, Double.NaN, 0),
            new Wild(() -> WCISEntities.SANDMAN.get(), net.ixdarklord.whycantisleep.between.mob.SandmanEntity.class, 1, 80, 6, 1, 0, 26, 18, true, Double.NaN, 0),
            new Wild(() -> WCISEntities.TOOTH_TAKER.get(), net.ixdarklord.whycantisleep.between.mob.ToothTakerEntity.class, 2, 64, 5, 1, 0, 18, 16, true, 6, 3),
            new Wild(() -> WCISEntities.BROODMOTHER.get(), net.ixdarklord.whycantisleep.between.mob.BroodmotherEntity.class, 1, 96, 8, 1, 0, 28, 18, true, Double.NaN, 0),
            new Wild(() -> WCISEntities.VOID_EEL.get(), net.ixdarklord.whycantisleep.between.mob.VoidEelEntity.class, 1, 96, 6, 1, 0, 10, 20, true, -20, 6),
            new Wild(() -> WCISEntities.HIVE_THING.get(), net.ixdarklord.whycantisleep.between.mob.HiveThingEntity.class, 1, 64, 6, 1, 0, 24, 18, true, Double.NaN, 0),
            new Wild(() -> WCISEntities.CRAWLSPACE.get(), net.ixdarklord.whycantisleep.between.mob.CrawlspaceEntity.class, 2, 48, 4, 1, 0, 18, 16, true, Double.NaN, 0),
            new Wild(() -> WCISEntities.MIME_CLOWN.get(), net.ixdarklord.whycantisleep.between.mob.MimeClownEntity.class, 1, 80, 8, 1, 0, 24, 18, true, Double.NaN, 0),
            new Wild(() -> WCISEntities.JACK_IN_THE_BOX.get(), net.ixdarklord.whycantisleep.between.mob.JackInTheBoxEntity.class, 1, 80, 8, 1, 0, 22, 18, true, Double.NaN, 0),
            new Wild(() -> WCISEntities.BALLOON_CLOWN.get(), net.ixdarklord.whycantisleep.between.mob.BalloonClownEntity.class, 1, 80, 8, 1, 0, 22, 16, true, 5, 2),
            new Wild(() -> WCISEntities.FUNHOUSE_CLOWN.get(), net.ixdarklord.whycantisleep.between.mob.FunhouseClownEntity.class, 1, 80, 8, 1, 0, 24, 18, true, Double.NaN, 0),
            new Wild(() -> WCISEntities.LITTLE_RED.get(), net.ixdarklord.whycantisleep.between.mob.LittleRedEntity.class, 1, 80, 8, 1, 0, 20, 18, false, Double.NaN, 0),
            new Wild(() -> WCISEntities.DREAM_HARE.get(), net.ixdarklord.whycantisleep.between.mob.DreamHareEntity.class, 4, 48, 2, 1, 1, 16, 16, false, Double.NaN, 0));

    private static void tick(ServerLevel level) {
        if (!level.dimension().equals(DarkBetween.LEVEL) || level.getGameTime() % EVERY != 0) return;
        boolean peaceful = level.getDifficulty() == Difficulty.PEACEFUL;
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            for (Wild wild : WILDS) {
                if (peaceful && wild.monster() || random.nextInt(wild.oneIn()) != 0) continue;
                if (level.getEntitiesOfClass(wild.kind(), player.getBoundingBox().inflate(wild.reach())).size() >= wild.max()) continue;
                wild(level, player, wild);
            }
            if (peaceful) continue;
            int walkers = level.getEntitiesOfClass(SleepwalkerEntity.class, player.getBoundingBox().inflate(64.0)).size();
            if (walkers < SLEEPWALKERS) onGround(level, player, WCISEntities.SLEEPWALKER.get(), 20, 24, 0.0, Math.PI * 2);
            int eaters = level.getEntitiesOfClass(DreamEaterEntity.class, player.getBoundingBox().inflate(64.0)).size();
            if (eaters < EATERS && random.nextInt(3) == 0) onGround(level, player, WCISEntities.DREAM_EATER.get(), 24, 20, 0.0, Math.PI * 2);
            // The Faceless is set down behind its player, where they are not looking.
            int faceless = level.getEntitiesOfClass(FacelessEntity.class, player.getBoundingBox().inflate(96.0)).size();
            if (faceless < FACELESS && random.nextInt(8) == 0) {
                double behind = Math.toRadians(player.getYRot() - 90.0);                    // straight behind them (they face yaw + 90 in this frame)
                onGround(level, player, WCISEntities.FACELESS.get(), 28, 18, behind - Math.PI / 3, Math.PI * 2 / 3);
            }
            // A swarm of Somnimoths, in the air round about.
            int moths = level.getEntitiesOfClass(SomnimothEntity.class, player.getBoundingBox().inflate(48.0)).size();
            if (moths < MOTHS && random.nextInt(3) == 0) {
                double a = random.nextDouble() * Math.PI * 2, d = 16 + random.nextDouble() * 14;
                double x = player.getX() + Math.cos(a) * d, y = player.getY() + 3 + random.nextInt(6), z = player.getZ() + Math.sin(a) * d;
                for (int i = 3 + random.nextInt(3); i > 0; i--) {
                    spawn(level, WCISEntities.SOMNIMOTH.get(), x + random.nextGaussian(), y + random.nextGaussian() * 0.5, z + random.nextGaussian());
                }
            }
            int mantas = level.getEntitiesOfClass(DreadMantaEntity.class, player.getBoundingBox().inflate(96.0)).size();
            if (mantas < MANTAS && random.nextInt(3) == 0) {
                double a = random.nextDouble() * Math.PI * 2, d = 30 + random.nextDouble() * 20;
                spawn(level, WCISEntities.DREAD_MANTA.get(), player.getX() + Math.cos(a) * d, player.getY() + 8 + random.nextInt(12),
                        player.getZ() + Math.sin(a) * d);
            }
        }
    }

    /** Brings one of the wild creatures, and the rest of its group beside it. */
    private static void wild(ServerLevel level, ServerPlayer player, Wild wild) {
        RandomSource random = level.getRandom();
        EntityType<? extends Mob> type = wild.type().get();
        int count = wild.group() + (wild.extra() > 0 ? random.nextInt(wild.extra() + 1) : 0);
        Vec3 first;
        if (Double.isNaN(wild.air())) {
            first = onGround(level, player, type, wild.near(), wild.span(), 0.0, Math.PI * 2);
        } else {
            double a = random.nextDouble() * Math.PI * 2, d = wild.near() + random.nextDouble() * wild.span();
            double y = player.getY() + wild.air() + (random.nextDouble() * 2.0 - 1.0) * wild.airSpread();
            first = spawn(level, type, player.getX() + Math.cos(a) * d, y, player.getZ() + Math.sin(a) * d)
                    ? new Vec3(player.getX() + Math.cos(a) * d, y, player.getZ() + Math.sin(a) * d) : null;
        }
        if (first == null) return;
        for (int i = 1; i < count; i++) {
            double x = first.x + random.nextGaussian() * 1.5, z = first.z + random.nextGaussian() * 1.5;
            double y = Double.isNaN(wild.air()) ? level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z)) : first.y;
            spawn(level, type, x, y, z);
        }
    }

    /**
     * Sets one down on island ground (not over the void, outside the garden) {@code near} to {@code near + span}
     * blocks from a player, at an angle from {@code from} through {@code arc} radians round them.
     */
    private static <T extends Mob> @Nullable Vec3 onGround(ServerLevel level, ServerPlayer player, EntityType<T> type, double near, double span,
                                                           double from, double arc) {
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < 8; attempt++) {
            double a = from + random.nextDouble() * arc, d = near + random.nextDouble() * span;
            int x = (int) Math.floor(player.getX() + Math.cos(a) * d), z = (int) Math.floor(player.getZ() + Math.sin(a) * d);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos at = new BlockPos(x, y, z);
            if (y <= level.getMinY() + 1 || !level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)
                    || GardenWard.gardenAt(level, at) != null) {
                continue;
            }
            if (spawn(level, type, x + 0.5, y, z + 0.5)) return new Vec3(x + 0.5, y, z + 0.5);
        }
        return null;
    }

    private static <T extends Mob> boolean spawn(ServerLevel level, EntityType<T> type, double x, double y, double z) {
        T mob = type.create(level, EntitySpawnReason.NATURAL);
        if (mob == null) return false;
        mob.snapTo(x, y, z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(mob)) {
            mob.discard();
            return false;
        }
        return level.addFreshEntity(mob);
    }
}
