package net.ixdarklord.whycantisleep.lurker;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.ixdarklord.coolcatcore.api.event.v2.common.CommandEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerLifecycleEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ixdarklord.whycantisleep.sound.WCISSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The thing under the bed's server side: it keeps each player's {@link Dread}, and when dread has gone on long enough
 * it makes the creature real. Players who turned scary content off, and worlds with the creature off, are left alone:
 * no dread builds, and nothing comes.
 * <p>
 * Every number here is a first guess, to be tuned by play.
 */
public final class LurkerServer {
    /** Block light under which a bed counts as being in the dark. */
    public static final int DARK_LIGHT = 4;
    // The sky's light is 4 on a clear night and 15 by day; from here up, a place is lit by the day.
    private static final int DAYLIGHT = 8;
    private static final int DARK_SLEEP_DREAD = 15;
    private static final int LIT_SLEEP_RELIEF = 20;
    // Without sleep: a day and a half before it starts to tell, three days before it tells twice as fast.
    private static final int RESTLESS_TICKS = 36_000;
    private static final int SLEEPLESS_TICKS = 72_000;
    private static final int RESTLESS_DREAD = 2;
    private static final int SLEEPLESS_DREAD = 4;
    private static final int RESTLESS_INTERVAL = 1200;
    // Once hunted, each second it has this chance in one to come.
    private static final int SUMMON_ODDS = 15;
    private static final int DREAD_AFTER_DEATH = 30;
    // Ignored, it stays away this long before it tries again.
    private static final int RETURN_TICKS = 20 * 40;

    // Players who asked for no scary content, each player's last bed, and the creature out for each.
    private static final Set<UUID> OPTED_OUT = new HashSet<>();
    private static final Map<UUID, Bed> LAST_BED = new HashMap<>();
    private static final Map<UUID, LurkerEntity> ACTIVE = new HashMap<>();
    // How many times in a row each player ignored it, and the server tick before which it won't come back for them.
    private static final Map<UUID, Integer> IGNORED = new HashMap<>();
    private static final Map<UUID, Integer> NOT_BEFORE = new HashMap<>();

    private LurkerServer() {
    }

    public static void init() {
        PlayerEvents.END_TICK.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) tick(serverPlayer);
        });
        PlayerEvents.LEAVE.register(player -> {
            OPTED_OUT.remove(player.getUUID());
            LAST_BED.remove(player.getUUID());
            IGNORED.remove(player.getUUID());
            NOT_BEFORE.remove(player.getUUID());
        });
        ServerLifecycleEvents.STOPPED.register(server -> {
            OPTED_OUT.clear();
            LAST_BED.clear();
            ACTIVE.clear();
            IGNORED.clear();
            NOT_BEFORE.clear();
        });
        CommandEvents.REGISTER.register((dispatcher, context, selection) -> dispatcher.register(Commands.literal(WhyCantISleep.MOD_ID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("ignored").then(Commands.argument("times", IntegerArgumentType.integer(0, 9)).executes(ctx -> {
                    int times = IntegerArgumentType.getInteger(ctx, "times");
                    IGNORED.put(ctx.getSource().getPlayerOrException().getUUID(), times);
                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.whycantisleep.ignored.set", times), true);
                    return times;
                })))
                .then(Commands.literal("summon").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    boolean came = summon(player);
                    ctx.getSource().sendSuccess(() -> Component.translatable(came ? "commands.whycantisleep.summon.success" : "commands.whycantisleep.summon.failed"), true);
                    return came ? 1 : 0;
                }))
                .then(Commands.literal("dread")
                        .executes(ctx -> {
                            int dread = Dread.get(ctx.getSource().getPlayerOrException());
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.whycantisleep.dread.get", dread, Dread.MAX), false);
                            return dread;
                        })
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, Dread.MAX)).executes(ctx -> {
                            int dread = IntegerArgumentType.getInteger(ctx, "value");
                            Dread.set(ctx.getSource().getPlayerOrException(), dread);
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.whycantisleep.dread.set", dread), true);
                            return dread;
                        })))));
    }

    /** A player's client said whether they want scary content. Turning it off also lays their dread to rest. */
    public static void onPreference(ServerPlayer player, boolean scary) {
        if (scary) {
            OPTED_OUT.remove(player.getUUID());
        } else {
            OPTED_OUT.add(player.getUUID());
            Dread.set(player, 0);
            LurkerEntity lurker = ACTIVE.get(player.getUUID());
            if (lurker != null) lurker.discard();
        }
    }

    /** Whether the creature may trouble this player at all. Never in Peaceful, where the game lets nothing hunt a player. */
    public static boolean troubles(ServerPlayer player) {
        return WCISServerConfig.BED_CREATURE.get() && !OPTED_OUT.contains(player.getUUID()) && WCISNetwork.hasMod(player)
                && !player.isCreative() && !player.isSpectator() && player.level().getDifficulty() != Difficulty.PEACEFUL;
    }

    /** A player lay down to sleep ({@code ServerPlayerMixin}): in the dark it feeds the dread, in the light it eases it. */
    public static void onSlept(ServerPlayer player, BlockPos bed) {
        LAST_BED.put(player.getUUID(), new Bed(player.level().dimension(), bed.immutable()));
        if (!troubles(player)) return;
        if (isDark(player.level(), bed)) Dread.add(player, DARK_SLEEP_DREAD);
        else Dread.add(player, -LIT_SLEEP_RELIEF);
    }

    /**
     * Whether a bed is in the dark: no torch or lamp lights it, and no daylight reaches it. A bed under the open sky is
     * dark by night (the moon's light doesn't count) and lit by day. Both sides agree on it.
     */
    public static boolean isDark(Level level, BlockPos bed) {
        BlockPos above = bed.above();
        return level.getBrightness(LightLayer.BLOCK, above) < DARK_LIGHT && level.getMaxLocalRawBrightness(above) < DAYLIGHT;
    }

    private static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 0 || !troubles(player)) return;
        if (player.tickCount % RESTLESS_INTERVAL == 0) {
            int awake = player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
            if (awake >= SLEEPLESS_TICKS) Dread.add(player, SLEEPLESS_DREAD);
            else if (awake >= RESTLESS_TICKS) Dread.add(player, RESTLESS_DREAD);
        }
        // It comes in the dark: at night, or wherever the light doesn't reach.
        ServerLevel level = player.level();
        if (Dread.stage(player) == Dread.Stage.HUNTED && !ACTIVE.containsKey(player.getUUID())
                && level.getServer().getTickCount() >= NOT_BEFORE.getOrDefault(player.getUUID(), 0)
                && (level.isDarkOutside() || level.getMaxLocalRawBrightness(player.blockPosition()) < DARK_LIGHT)
                && player.getRandom().nextInt(SUMMON_ODDS) == 0) {
            summon(player);
        }
    }

    /** Makes the creature real for a player: out from under their bed when it is near, else from the dark around them. */
    public static boolean summon(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos spot = null;
        Bed bed = LAST_BED.get(player.getUUID());
        if (bed != null && bed.dimension == level.dimension() && bed.pos.closerThan(player.blockPosition(), 24.0)
                && level.getBlockState(bed.pos).getBlock() instanceof BedBlock) {
            spot = standableNear(level, bed.pos, 2, player.getRandom());
        }
        for (int attempt = 0; spot == null && attempt < 12; attempt++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
            double distance = 8.0 + player.getRandom().nextDouble() * 5.0;
            spot = standableNear(level, BlockPos.containing(player.getX() + Math.cos(angle) * distance, player.getY(), player.getZ() + Math.sin(angle) * distance), 3, player.getRandom());
        }
        if (spot == null) return false;
        LurkerEntity lurker = WCISEntities.LURKER.get().create(level, EntitySpawnReason.TRIGGERED);
        if (lurker == null) return false;
        lurker.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, player.getRandom().nextFloat() * 360.0F, 0.0F);
        lurker.setVictim(player.getUUID());
        lurker.setLure(IGNORED.getOrDefault(player.getUUID(), 0));
        if (!level.addFreshEntity(lurker)) return false;
        ACTIVE.put(player.getUUID(), lurker);
        level.playSound(null, spot, WCISSounds.LURKER_EMERGE, SoundSource.HOSTILE, 0.8F, 1.0F);
        return true;
    }

    // An open spot to stand on near a block: solid below, room for the creature, within a few blocks up or down.
    private static @Nullable BlockPos standableNear(ServerLevel level, BlockPos center, int radius, RandomSource random) {
        for (int attempt = 0; attempt < 16; attempt++) {
            BlockPos column = center.offset(random.nextInt(radius * 2 + 1) - radius, 0, random.nextInt(radius * 2 + 1) - radius);
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos pos = column.above(dy);
                if (level.getBlockState(pos.below()).isSolidRender() && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                        && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                    return pos;
                }
            }
        }
        return null;
    }

    /** Its player never looked at it, and it left: it will be back, and less patient. */
    static void onIgnored(LurkerEntity lurker) {
        UUID victim = lurker.victim();
        if (victim == null || lurker.level().getServer() == null) return;
        IGNORED.merge(victim, 1, Integer::sum);
        NOT_BEFORE.put(victim, lurker.level().getServer().getTickCount() + RETURN_TICKS);
    }

    /** Its player outran it: it lets them go, for now. */
    static void onEscaped(LurkerEntity lurker) {
        UUID victim = lurker.victim();
        if (victim == null || lurker.level().getServer() == null) return;
        NOT_BEFORE.put(victim, lurker.level().getServer().getTickCount() + RETURN_TICKS);
    }

    /** Its player looked at it: being ignored is over. */
    static void onHuntStarted(LurkerEntity lurker) {
        if (lurker.victim() != null) IGNORED.remove(lurker.victim());
    }

    /** The creature was killed: its player's dread is over. */
    static void onLurkerKilled(LurkerEntity lurker) {
        UUID victim = lurker.victim();
        Player player = victim == null ? null : lurker.level().getPlayerByUUID(victim);
        if (player != null) Dread.set(player, 0);
    }

    /** The creature got its player: it is sated for a while, not for good. */
    static void onVictimKilled(Player player) {
        Dread.set(player, DREAD_AFTER_DEATH);
    }

    static void forget(LurkerEntity lurker) {
        if (lurker.victim() != null) ACTIVE.remove(lurker.victim(), lurker);
    }

    private record Bed(ResourceKey<Level> dimension, BlockPos pos) {
    }
}
