package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.coolcatcore.api.event.v2.common.CommandEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerLifecycleEvents;
import net.ixdarklord.coolcatcore.api.registry.ReloadListeners;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.api.SleepEffect;
import net.ixdarklord.whycantisleep.api.SleepEffects;
import net.ixdarklord.whycantisleep.api.SleepContext;
import net.ixdarklord.whycantisleep.api.SleepEvents;
import net.ixdarklord.whycantisleep.api.SleepModifiers;
import net.ixdarklord.whycantisleep.api.SleepQuality;
import net.ixdarklord.whycantisleep.api.SleepTrouble;
import net.ixdarklord.whycantisleep.api.WakeReason;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.network.TerrorPayload;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.ixdarklord.whycantisleep.network.WokePayload;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Each night a player sleeps, from lying down to waking: the room they lie in, the monsters that prowl near and the
 * music that plays through it, a night terror if they had been sleeping badly, and in the end how well they slept
 * ({@link SleepQuality}) and why they woke. A night kept well, in a closed and lit room, leaves them well rested; a
 * night terror leaves them shaken.
 */
public final class SleepNights {
    // How far round the bed monsters are heard prowling, and how often the night is looked at.
    private static final double PROWL_RANGE = 24.0;
    private static final int LOOK_TICKS = 20;
    // Waking shaken from a night terror, and well rested from a kept routine.
    private static final int SHAKEN_NAUSEA_TICKS = 140;
    private static final int SHAKEN_SLOWNESS_TICKS = 100;
    private static final int RESTED_REGENERATION_TICKS = 20 * 30;

    private static final Map<UUID, Night> NIGHTS = new HashMap<>();
    private static final Map<UUID, SleepTrouble> BEFORE_SLEEP = new HashMap<>();
    private static final Map<UUID, SleepQuality> LAST = new HashMap<>();
    private static final Map<UUID, Long> HURT_AT = new HashMap<>();
    private static final Set<UUID> MORNING = new HashSet<>();

    private SleepNights() {
    }

    public static void init() {
        PlayerEvents.END_TICK.register(player -> {
            if (player instanceof ServerPlayer serverPlayer && player.tickCount % LOOK_TICKS == 0) look(serverPlayer);
        });
        // /whycantisleep nightmare [players]: a night terror on demand, for operators.
        CommandEvents.REGISTER.register((dispatcher, context, selection) -> dispatcher.register(Commands.literal("whycantisleep")
                .then(Commands.literal("nightmare").requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(command -> nightmare(command.getSource(), List.of(command.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(command -> nightmare(command.getSource(), EntityArgument.getPlayers(command, "targets")))))));
        PlayerEvents.LEAVE.register(player -> {
            NIGHTS.remove(player.getUUID());
            BEFORE_SLEEP.remove(player.getUUID());
        });
        ReloadListeners.registerServer(WhyCantISleep.id("sleep_effects"), DataSleepEffects.INSTANCE);
        ReloadListeners.registerServer(WhyCantISleep.id("sleep_modifiers"), DataSleepModifiers.INSTANCE);
        ServerLifecycleEvents.STOPPED.register(server -> {
            NIGHTS.clear();
            BEFORE_SLEEP.clear();
            LAST.clear();
            HURT_AT.clear();
            MORNING.clear();
        });
    }

    /** A player is being hurt ({@code LivingEntityMixin}): a sleeper woken by it woke hurt. */
    public static void onHurt(ServerPlayer player) {
        if (player.isSleeping()) HURT_AT.put(player.getUUID(), player.level().getGameTime());
    }

    /** How well a player slept last, or null when they haven't slept since the server started. */
    public static @Nullable SleepQuality last(ServerPlayer player) {
        return LAST.get(player.getUUID());
    }

    /** Before a try to sleep: the trouble as it stands, before lying down wipes the time since rest. */
    public static void beforeSleep(ServerPlayer player) {
        BEFORE_SLEEP.put(player.getUUID(), SleepTroubles.get(player));
    }

    /** They lay down: the night begins. */
    public static void onLayDown(ServerPlayer player, BlockPos bed) {
        ServerLevel level = player.serverLevel();
        SleepTrouble trouble = BEFORE_SLEEP.remove(player.getUUID());
        if (trouble == null) trouble = SleepTroubles.get(player);
        Night night = new Night(bed.immutable(), level.getGameTime(), trouble, Bedroom.of(level, bed));
        NIGHTS.put(player.getUUID(), night);
        night.observe(level);
        if (WCISServerConfig.NIGHT_TERRORS.get() && trouble.level().atLeast(SleepTrouble.Level.RESTLESS)) {
            int chance = WCISServerConfig.TERROR_CHANCE.get() * (trouble.level() == SleepTrouble.Level.SLEEPLESS ? 2 : 1);
            if (level.getRandom().nextInt(100) < chance) {
                SleepEvents.NightTerror event = new SleepEvents.NightTerror(player, trouble);
                SleepEvents.NIGHT_TERROR.invoke(event);
                if (!event.isCancelled()) {
                    night.terror = true;
                    WCISNetwork.sendToPlayer(player, new TerrorPayload(true));
                }
            }
        }
    }

    /**
     * Gives a player a night terror now, whether or not they sleep badly. One asleep has it as a night's terror: they
     * wake shaken, and the night is scored for it; one awake only sees it.
     */
    public static void nightmare(ServerPlayer player) {
        Night night = NIGHTS.get(player.getUUID());
        if (night != null && player.isSleeping()) night.terror = true;
        WCISNetwork.sendToPlayer(player, new TerrorPayload(true));
    }

    private static int nightmare(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) nightmare(target);
        if (targets.size() == 1) {
            ServerPlayer target = targets.iterator().next();
            source.sendSuccess(() -> Component.translatable(target.isSleeping() ? "commands.whycantisleep.nightmare.asleep" : "commands.whycantisleep.nightmare.awake",
                    target.getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.whycantisleep.nightmare.many", targets.size()), true);
        }
        return targets.size();
    }

    /** The night was skipped: everyone asleep in the level wakes to morning. */
    public static void onNightSkipped(ServerLevel level) {
        List<ServerPlayer> sleepers = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (player.isSleeping()) {
                sleepers.add(player);
                MORNING.add(player.getUUID());
            }
        }
        SleepEvents.NIGHT_SKIPPED.invoke(new SleepEvents.NightSkipped(level, List.copyOf(sleepers)));
    }

    /**
     * A sleeping player is waking: why, how well they slept, and what it left them with.
     *
     * @param forceful the game's own word that they were made to wake, not that they got up
     */
    public static void onWake(ServerPlayer player, boolean forceful) {
        Night night = NIGHTS.remove(player.getUUID());
        boolean morning = MORNING.remove(player.getUUID());
        if (night == null) return;
        ServerLevel level = player.serverLevel();
        WakeReason reason = reason(player, level, night, morning, forceful);
        int slept = (int) (level.getGameTime() - night.since);

        Bedroom room = night.room;
        SleepContext context = new SleepContext(player, night.bed, reason, slept, room.enclosed(), room.lit(), room.pillowLight(), room.decor().soothing(),
                room.decor().unsettling(), night.monsters, night.calm, night.creepy, night.loud, night.trouble.level(), night.terror);
        List<SleepQuality.Factor> factors = SleepModifiers.score(context);

        SleepEvents.QualityScoring scoring = new SleepEvents.QualityScoring(player, reason, factors);
        SleepEvents.SCORE_QUALITY.invoke(scoring);
        SleepQuality quality = SleepQuality.of(scoring.factors());
        LAST.put(player.getUUID(), quality);
        // Well rested when the routine counted, as the modifiers (and any mod's say) left it, and the server allows it.
        boolean wellRested = WCISServerConfig.WELL_RESTED.get()
                && quality.factors().stream().anyMatch(factor -> factor.id().equals(SleepQuality.ROUTINE) && factor.points() > 0);

        if (night.terror) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, SHAKEN_NAUSEA_TICKS, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SHAKEN_SLOWNESS_TICKS, 0, false, false, true));
        }
        if (wellRested) player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, RESTED_REGENERATION_TICKS, 0, false, true, true));
        if (night.terror) WCISNetwork.sendToPlayer(player, new TerrorPayload(false));

        SleepEffect sleepEffect = reason.rested() ? sleepEffect(player, quality) : null;
        SleepEvents.WOKE.invoke(new SleepEvents.Woke(player, reason, quality, slept, night.terror, wellRested, sleepEffect));
        if (WCISServerConfig.SLEEP_QUALITY.get()) {
            List<WokePayload.Line> lines = new ArrayList<>();
            for (SleepQuality.Factor factor : quality.factors()) lines.add(new WokePayload.Line(factor.label(), factor.points()));
            Optional<WokePayload.Dream> dream = Optional.ofNullable(sleepEffect)
                    .map(effect -> new WokePayload.Dream(effect.name(), effect.kind(), effect.rarity()));
            WCISNetwork.sendToPlayer(player, new WokePayload(reason, quality.score(), lines, night.terror, wellRested, dream));
        }
    }

    // A sleep effect for a night slept through, at the server's chance, drawn by the night's score; given, unless a
    // listener says otherwise.
    private static @Nullable SleepEffect sleepEffect(ServerPlayer player, SleepQuality quality) {
        if (!WCISServerConfig.SLEEP_EFFECTS.get() || player.getRandom().nextInt(100) >= WCISServerConfig.SLEEP_EFFECT_CHANCE.get()) return null;
        SleepEffect drawn = SleepEffects.draw(quality.score(), player.getRandom());
        if (drawn == null) return null;
        SleepEvents.SleepEffectDrawn event = new SleepEvents.SleepEffectDrawn(player, quality, drawn);
        SleepEvents.SLEEP_EFFECT.invoke(event);
        if (event.isCancelled()) return null;
        event.effect().apply(player);
        return event.effect();
    }

    private static WakeReason reason(ServerPlayer player, ServerLevel level, Night night, boolean morning, boolean forceful) {
        if (morning || level.isDay()) return WakeReason.MORNING;
        Long hurt = HURT_AT.remove(player.getUUID());
        if (hurt != null && level.getGameTime() - hurt <= 1) return WakeReason.HURT;
        if (!(level.getBlockState(night.bed).getBlock() instanceof BedBlock)) return WakeReason.BED_GONE;
        return forceful ? WakeReason.OTHER : WakeReason.GOT_UP;
    }


    // Through the night: the monsters about, at their most, and the music heard.
    private static void look(ServerPlayer player) {
        Night night = NIGHTS.get(player.getUUID());
        if (night != null && player.isSleeping()) night.observe(player.serverLevel());
    }

    private static final class Night {
        private final BlockPos bed;
        private final long since;
        private final SleepTrouble trouble;
        private final Bedroom room;
        private int monsters;
        private boolean creepy;
        private boolean loud;
        private boolean calm;
        private boolean terror;

        private Night(BlockPos bed, long since, SleepTrouble trouble, Bedroom room) {
            this.bed = bed;
            this.since = since;
            this.trouble = trouble;
            this.room = room;
        }

        private void observe(ServerLevel level) {
            int near = level.getEntitiesOfClass(Monster.class, new AABB(this.bed).inflate(PROWL_RANGE), Monster::isAlive).size();
            this.monsters = Math.max(this.monsters, near);
            Bedroom.Music music = Bedroom.music(level, this.bed);
            if (music == Bedroom.Music.CREEPY) this.creepy = true;
            else if (music == Bedroom.Music.LOUD) this.loud = true;
            else if (music == Bedroom.Music.CALM) this.calm = true;
        }
    }
}
