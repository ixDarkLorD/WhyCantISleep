package net.ixdarklord.whycantisleep.between;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ixdarklord.coolcatcore.api.attachment.Attachment;
import net.ixdarklord.coolcatcore.api.attachment.SyncPolicy;
import net.ixdarklord.coolcatcore.api.event.v2.common.CommandEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.mind.Mind;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * How far a player has come in the Dark Between, and how its sky shows them that: the bosses they have helped bring down,
 * and their sky's settings. The Dark Sign (the eclipse over the south, shaders/sky/dark_between.fsh) and the storm's
 * vortex over the Lord's castle (CastleVortex) only come out once every minion boss is down ({@link #nearTheEnd}), with
 * the Lord's fight ahead; either can be turned on or off for a player by command, and the Sign drawn in its DS3
 * design. Saved with the player and kept by them through death; synced to them, so their client draws the sky by it.
 */
public record BetweenProgress(List<String> defeated, Shown sign, SignStyle style, Shown vortex) {
    /** The minion bosses, by id: all of them down, the Lord is next. (The Gardener only, so far.) */
    public static final List<String> MINION_BOSSES = List.of("gardener");
    public static final BetweenProgress DEFAULT = new BetweenProgress(List.of(), Shown.AUTO, SignStyle.WATCHER, Shown.AUTO);

    public static final Codec<BetweenProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("defeated", List.of()).forGetter(BetweenProgress::defeated),
            Shown.CODEC.optionalFieldOf("sign", Shown.AUTO).forGetter(BetweenProgress::sign),
            SignStyle.CODEC.lenientOptionalFieldOf("sign_style", SignStyle.WATCHER).forGetter(BetweenProgress::style),
            Shown.CODEC.optionalFieldOf("vortex", Shown.AUTO).forGetter(BetweenProgress::vortex)
    ).apply(i, BetweenProgress::new));
    public static final Attachment<BetweenProgress> PROGRESS = Mind.ATTACHMENTS.builder("between_progress", CODEC, DEFAULT)
            .networkCodec(ByteBufCodecs.fromCodecWithRegistries(CODEC)).sync(SyncPolicy.SELF).copyOnDeath().build();

    public BetweenProgress {
        defeated = List.copyOf(defeated);
    }

    /** Whether something shows in the sky: by the player's progress (auto), or always, or never. */
    public enum Shown implements StringRepresentable {
        AUTO, ON, OFF;

        public static final Codec<Shown> CODEC = StringRepresentable.fromEnum(Shown::values);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    /** Which Dark Sign is drawn: the Watcher (the Dark Between's own), or DS3, the Dark Souls 3 eclipse it was first drawn as. */
    public enum SignStyle implements StringRepresentable {
        WATCHER, DS3;

        public static final Codec<SignStyle> CODEC = StringRepresentable.fromEnum(SignStyle::values);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public static BetweenProgress get(Player player) {
        return PROGRESS.get(player);
    }

    /** Server side: changes a player's progress, which saves it and sends it to them. */
    public static void update(Player player, UnaryOperator<BetweenProgress> change) {
        PROGRESS.update(player, change);
    }

    /** Every minion boss is down: only the Lord is left. */
    public boolean nearTheEnd() {
        return this.defeated.containsAll(MINION_BOSSES);
    }

    public boolean signShown() {
        return this.sign == Shown.ON || this.sign == Shown.AUTO && this.nearTheEnd();
    }

    public boolean vortexShown() {
        return this.vortex == Shown.ON || this.vortex == Shown.AUTO && this.nearTheEnd();
    }

    public BetweenProgress withDefeated(String boss) {
        if (this.defeated.contains(boss)) return this;
        List<String> list = new ArrayList<>(this.defeated);
        list.add(boss);
        return new BetweenProgress(list, this.sign, this.style, this.vortex);
    }

    public BetweenProgress withSign(Shown sign) {
        return new BetweenProgress(this.defeated, sign, this.style, this.vortex);
    }

    public BetweenProgress withStyle(SignStyle style) {
        return new BetweenProgress(this.defeated, this.sign, style, this.vortex);
    }

    public BetweenProgress withVortex(Shown vortex) {
        return new BetweenProgress(this.defeated, this.sign, this.style, vortex);
    }

    /** A boss is down: every player in at its death has it to their name. */
    public static void defeated(Collection<ServerPlayer> players, String boss) {
        for (ServerPlayer player : players) update(player, progress -> progress.withDefeated(boss));
    }

    /**
     * {@code /whycantisleep sky sign|vortex auto|on|off [players]}, {@code /whycantisleep sky sign style
     * watcher|ds3 [players]}, and {@code /whycantisleep progress [players]}, {@code progress defeat <boss>
     * [players]}, {@code progress reset [players]}.
     */
    public static void init() {
        CommandEvents.REGISTER.register((dispatcher, context, selection) -> {
            LiteralArgumentBuilder<CommandSourceStack> sign = Commands.literal("sign");
            LiteralArgumentBuilder<CommandSourceStack> vortex = Commands.literal("vortex");
            for (Shown shown : Shown.values()) {
                sign.then(withTargets(Commands.literal(shown.getSerializedName()), (ctx, players) ->
                        set(ctx, players, p -> p.withSign(shown), "commands.whycantisleep.sky.sign", shown.getSerializedName())));
                vortex.then(withTargets(Commands.literal(shown.getSerializedName()), (ctx, players) ->
                        set(ctx, players, p -> p.withVortex(shown), "commands.whycantisleep.sky.vortex", shown.getSerializedName())));
            }
            LiteralArgumentBuilder<CommandSourceStack> style = Commands.literal("style");
            for (SignStyle signStyle : SignStyle.values()) {
                style.then(withTargets(Commands.literal(signStyle.getSerializedName()), (ctx, players) ->
                        set(ctx, players, p -> p.withStyle(signStyle), "commands.whycantisleep.sky.style", signStyle.getSerializedName())));
            }
            sign.then(style);
            dispatcher.register(Commands.literal(WhyCantISleep.MOD_ID)
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.literal("sky").then(sign).then(vortex))
                    .then(withTargets(Commands.literal("progress"), BetweenProgress::status)
                            .then(Commands.literal("defeat").then(withTargets(Commands.argument("boss", StringArgumentType.word())
                                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(MINION_BOSSES, builder)), (ctx, players) ->
                                    set(ctx, players, p -> p.withDefeated(StringArgumentType.getString(ctx, "boss")),
                                            "commands.whycantisleep.progress.defeat", StringArgumentType.getString(ctx, "boss")))))
                            .then(withTargets(Commands.literal("reset"), (ctx, players) ->
                                    set(ctx, players, p -> new BetweenProgress(List.of(), p.sign(), p.style(), p.vortex()),
                                            "commands.whycantisleep.progress.reset", "")))));
        });
    }

    private interface Action {
        int run(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) throws CommandSyntaxException;
    }

    // A command that acts on the one running it, or on the players named after it.
    private static <T extends com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, T>> T withTargets(T node, Action action) {
        return node.executes(ctx -> action.run(ctx, List.of(ctx.getSource().getPlayerOrException())))
                .then(Commands.argument("players", EntityArgument.players()).executes(ctx -> action.run(ctx, EntityArgument.getPlayers(ctx, "players"))));
    }

    private static int set(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players, UnaryOperator<BetweenProgress> change,
                           String key, String value) {
        for (ServerPlayer player : players) update(player, change);
        int count = players.size();
        ctx.getSource().sendSuccess(() -> Component.translatable(key, value, count), true);
        return count;
    }

    private static int status(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            BetweenProgress p = get(player);
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.whycantisleep.progress.status", player.getDisplayName(),
                    p.defeated().isEmpty() ? "-" : String.join(", ", p.defeated()), p.nearTheEnd() ? "yes" : "no",
                    p.sign().getSerializedName() + (p.signShown() ? " (shown)" : " (hidden)"), p.style().getSerializedName(),
                    p.vortex().getSerializedName() + (p.vortexShown() ? " (shown)" : " (hidden)")), false);
        }
        return players.size();
    }
}
