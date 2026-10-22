package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.coolcatcore.api.event.v2.common.CommandEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerLifecycleEvents;
import net.ixdarklord.whycantisleep.api.SleepEvents;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.mixin.ServerLevelAccessor;
import net.ixdarklord.whycantisleep.network.SleepBoardPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Voting to skip the night: when nearly everyone is in bed but a player or two is still up, busy with something, the
 * players in bed can vote from the sleep board to skip the night anyway. Once they all have, the players still up are
 * asked, politely, in chat: "Go ahead" or "Not yet". A "Not yet" stops it (no new vote for a while); hearing nothing
 * for {@link WCISServerConfig#VOTE_WAIT} seconds is taken as a yes, and the night is skipped as if all had slept.
 */
public final class SleepVotes {
    // How long after a "Not yet" before the players in bed can vote again.
    private static final int DECLINED_COOLDOWN_TICKS = 20 * 60;

    private static final Map<ResourceKey<Level>, Vote> VOTES = new HashMap<>();

    private SleepVotes() {
    }

    public static void init() {
        CommandEvents.REGISTER.register((dispatcher, context, selection) -> dispatcher.register(Commands.literal("whycantisleep")
                .then(Commands.literal("vote")
                        .then(Commands.literal("yes").executes(command -> answer(command.getSource().getPlayerOrException(), true)))
                        .then(Commands.literal("no").executes(command -> answer(command.getSource().getPlayerOrException(), false))))));
        ServerLifecycleEvents.STOPPED.register(server -> VOTES.clear());
    }

    /** What the board shows of the vote in a level, for a viewer in bed. */
    public static SleepBoardPayload.Vote state(ServerLevel level, ServerPlayer viewer, int sleeping, int needed, List<ServerPlayer> awake) {
        Vote vote = VOTES.get(level.dimension());
        long now = level.getGameTime();
        boolean open = canVote(sleeping, needed, awake) && (vote == null || now >= vote.declinedUntil);
        if (vote == null) return new SleepBoardPayload.Vote(open, 0, sleeping, false, -1);
        int waiting = vote.askedAt < 0 ? -1 : (int) Math.max(0, (vote.askedAt + WCISServerConfig.VOTE_WAIT.get() * 20L - now) / 20);
        return new SleepBoardPayload.Vote(open, vote.votes.size(), sleeping, vote.votes.contains(viewer.getUUID()), waiting);
    }

    // A vote is possible while some sleepers are missing, but no more than a few players are up.
    private static boolean canVote(int sleeping, int needed, List<ServerPlayer> awake) {
        return WCISServerConfig.SLEEP_VOTE.get() && sleeping > 0 && sleeping < needed && !awake.isEmpty() && awake.size() <= WCISServerConfig.VOTE_MAX_AWAKE.get();
    }

    /** A player in bed votes to skip the night (from the board). */
    public static void onVote(ServerPlayer player) {
        ServerLevel level = player.level();
        if (!player.isSleeping()) return;
        Census census = census(level);
        if (!canVote(census.sleeping, census.needed, census.awake)) return;
        Vote vote = VOTES.computeIfAbsent(level.dimension(), key -> new Vote());
        if (level.getGameTime() < vote.declinedUntil || !vote.votes.add(player.getUUID())) return;
        for (ServerPlayer sleeper : census.sleepers) {
            sleeper.sendSystemMessage(Component.translatable("gui.whycantisleep.vote.cast", player.getDisplayName(), vote.votes.size(), census.sleeping), true);
        }
        if (vote.askedAt < 0 && vote.votes.size() >= census.sleeping) ask(level, vote, census);
    }

    // Everyone in bed has voted: those still up are asked.
    private static void ask(ServerLevel level, Vote vote, Census census) {
        vote.askedAt = level.getGameTime();
        vote.asked.clear();
        for (ServerPlayer up : census.awake) {
            vote.asked.add(up.getUUID());
            MutableComponent yes = button("gui.whycantisleep.vote.yes", "/whycantisleep vote yes", ChatFormatting.GREEN);
            MutableComponent no = button("gui.whycantisleep.vote.no", "/whycantisleep vote no", ChatFormatting.GRAY);
            up.sendSystemMessage(Component.translatable("gui.whycantisleep.vote.prompt", census.sleeping, WCISServerConfig.VOTE_WAIT.get())
                    .append(" ").append(yes).append(" ").append(no));
        }
    }

    private static MutableComponent button(String key, String command, ChatFormatting color) {
        return Component.literal("[").append(Component.translatable(key)).append("]").withStyle(style -> style.withColor(color)
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable(key + ".hover"))));
    }

    // An awake player's answer, from the buttons in chat.
    private static int answer(ServerPlayer player, boolean yes) {
        ServerLevel level = player.level();
        Vote vote = VOTES.get(level.dimension());
        if (vote == null || vote.askedAt < 0 || !vote.asked.remove(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("gui.whycantisleep.vote.none"), true);
            return 0;
        }
        Census census = census(level);
        if (!yes) {
            vote.reset();
            vote.declinedUntil = level.getGameTime() + DECLINED_COOLDOWN_TICKS;
            player.sendSystemMessage(Component.translatable("gui.whycantisleep.vote.declined_self"), true);
            for (ServerPlayer sleeper : census.sleepers) sleeper.sendSystemMessage(Component.translatable("gui.whycantisleep.vote.declined", player.getDisplayName()));
            return 1;
        }
        player.sendSystemMessage(Component.translatable("gui.whycantisleep.vote.accepted_self"), true);
        if (vote.asked.isEmpty()) skip(level, census);
        return 1;
    }

    /** Once a second, from the board: votes of players no longer in bed fall away, and an unanswered ask runs out. */
    public static void tick(ServerLevel level) {
        Vote vote = VOTES.get(level.dimension());
        if (vote == null) return;
        Census census = census(level);
        Set<UUID> sleeping = new HashSet<>();
        for (ServerPlayer sleeper : census.sleepers) sleeping.add(sleeper.getUUID());
        vote.votes.retainAll(sleeping);
        boolean asking = vote.askedAt >= 0;
        if (asking && !canVote(census.sleeping, census.needed, census.awake)) {
            // Enough went to bed by themselves, or more got up: the vote is moot.
            vote.reset();
            return;
        }
        if (asking && level.getGameTime() - vote.askedAt >= WCISServerConfig.VOTE_WAIT.get() * 20L) skip(level, census);
        else if (!asking && vote.votes.isEmpty()) vote.reset();
    }

    // The night is skipped as the game skips it: the clock to the waking hour, and every sleeper woken.
    private static void skip(ServerLevel level, Census census) {
        Vote vote = VOTES.remove(level.dimension());
        if (vote == null) return;
        for (ServerPlayer player : level.players()) player.sendSystemMessage(Component.translatable("gui.whycantisleep.vote.skipped"), true);
        level.dimensionType().defaultClock().ifPresent(clock -> {
            if (level.getGameRules().get(GameRules.ADVANCE_TIME)) level.getServer().clockManager().moveToTimeMarker(clock, ClockTimeMarkers.WAKE_UP_FROM_SLEEP);
        });
        ((ServerLevelAccessor) level).whycantisleep$wakeUpAllPlayers();
        SleepEvents.NIGHT_VOTED.invoke(new SleepEvents.NightVoted(level, census.sleepers));
    }

    // Who is asleep in a level, how many the rule needs, and who is up and counted.
    private static Census census(ServerLevel level) {
        List<ServerPlayer> sleepers = new ArrayList<>();
        List<ServerPlayer> awake = new ArrayList<>();
        int active = 0;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            active++;
            if (player.isSleeping()) sleepers.add(player);
            else awake.add(player);
        }
        int needed = Math.max(1, (int) Math.ceil(active * level.getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE) / 100.0F));
        return new Census(sleepers, awake, sleepers.size(), needed);
    }

    private record Census(List<ServerPlayer> sleepers, List<ServerPlayer> awake, int sleeping, int needed) {
    }

    private static final class Vote {
        private final Set<UUID> votes = new HashSet<>();
        private final Set<UUID> asked = new HashSet<>();
        private long askedAt = -1;
        private long declinedUntil;

        private void reset() {
            this.votes.clear();
            this.asked.clear();
            this.askedAt = -1;
        }
    }
}
