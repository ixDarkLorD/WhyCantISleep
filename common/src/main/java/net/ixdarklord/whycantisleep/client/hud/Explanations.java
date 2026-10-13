package net.ixdarklord.whycantisleep.client.hud;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * The game's short sleep messages, said again with their reason and what to do about it. They are recognised by their
 * translation keys, so this works on any server, with the mod or without.
 */
public final class Explanations {
    public static final String NOT_SAFE = "block.minecraft.bed.not_safe";
    private static final String PLAYERS_SLEEPING = "sleep.players_sleeping";
    // The game's key to the notice that explains it: "<name>.title" and "<name>.hint" in the lang file.
    private static final Map<String, Explained> EXPLAINED = Map.of(
            NOT_SAFE, new Explained(SleepNotice.Kind.MONSTERS, "monsters"),
            "block.minecraft.bed.no_sleep", new Explained(SleepNotice.Kind.WRONG_TIME, "wrong_time"),
            "block.minecraft.bed.too_far_away", new Explained(SleepNotice.Kind.TOO_FAR, "too_far"),
            "block.minecraft.bed.obstructed", new Explained(SleepNotice.Kind.OBSTRUCTED, "obstructed"),
            "block.minecraft.bed.occupied", new Explained(SleepNotice.Kind.OCCUPIED, "occupied"),
            "sleep.not_possible", new Explained(SleepNotice.Kind.NOT_POSSIBLE, "not_possible"));

    private Explanations() {
    }

    /** The notice explaining one of the game's sleep messages, or null for any other message. */
    public static @Nullable SleepNotice explain(TranslatableContents contents) {
        if (contents.getKey().equals(PLAYERS_SLEEPING)) return playersSleeping(contents);
        Explained explained = EXPLAINED.get(contents.getKey());
        return explained == null ? null : notice(explained.kind, explained.name);
    }

    public static SleepNotice notice(SleepNotice.Kind kind, String name, Object... titleArgs) {
        String key = "gui.whycantisleep.explain." + name;
        return new SleepNotice(kind, Component.translatable(key + ".title", titleArgs), Component.translatable(key + ".hint"));
    }

    // "1/3 players sleeping": how many more are needed says what is missing.
    private static @Nullable SleepNotice playersSleeping(TranslatableContents contents) {
        Object[] args = contents.getArgs();
        if (args.length < 2) return null;
        try {
            int sleeping = number(args[0]);
            int needed = number(args[1]);
            int missing = needed - sleeping;
            if (missing <= 0) return null;
            return new SleepNotice(SleepNotice.Kind.PLAYERS_AWAKE,
                    Component.translatable("gui.whycantisleep.explain.players_awake.title", sleeping, needed),
                    Component.translatable("gui.whycantisleep.explain.players_awake.hint" + (missing == 1 ? ".one" : ""), missing));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // An argument is a number on the server's side and may come as text over the network.
    private static int number(Object arg) {
        if (arg instanceof Number number) return number.intValue();
        return Integer.parseInt(arg instanceof Component component ? component.getString() : arg.toString());
    }

    private record Explained(SleepNotice.Kind kind, String name) {
    }
}
