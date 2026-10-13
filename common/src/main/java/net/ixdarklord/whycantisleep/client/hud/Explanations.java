package net.ixdarklord.whycantisleep.client.hud;

import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Map;

/**
 * The game's short sleep messages, said again with their reason and what to do about it. They are recognised by their
 * translation keys, so this works on any server, with the mod or without.
 */
public final class Explanations {
    public static final String NOT_SAFE = "block.minecraft.bed.not_safe";
    private static final String PLAYERS_SLEEPING = "sleep.players_sleeping";
    // The game's key to the mod's explanation of it.
    private static final Map<String, String> EXPLAINED = Map.of(
            NOT_SAFE, "gui.whycantisleep.explain.monsters",
            "block.minecraft.bed.no_sleep", "gui.whycantisleep.explain.wrong_time",
            "block.minecraft.bed.too_far_away", "gui.whycantisleep.explain.too_far",
            "block.minecraft.bed.obstructed", "gui.whycantisleep.explain.obstructed",
            "block.minecraft.bed.occupied", "gui.whycantisleep.explain.occupied",
            "sleep.not_possible", "gui.whycantisleep.explain.not_possible");

    private Explanations() {
    }

    /** The explanation to show in place of one of the game's sleep messages; the message itself for any other. */
    public static Component explain(Component message) {
        if (!WCISClientConfig.SLEEP_EXPLANATIONS.get() || !(message.getContents() instanceof TranslatableContents contents)) return message;
        if (contents.getKey().equals(PLAYERS_SLEEPING)) return playersSleeping(contents, message);
        String key = EXPLAINED.get(contents.getKey());
        return key != null ? Component.translatable(key) : message;
    }

    // "1/3 players sleeping": how many more are needed says what is missing.
    private static Component playersSleeping(TranslatableContents contents, Component message) {
        Object[] args = contents.getArgs();
        if (args.length < 2) return message;
        try {
            int sleeping = number(args[0]);
            int needed = number(args[1]);
            int missing = needed - sleeping;
            if (missing <= 0) return message;
            return Component.translatable("gui.whycantisleep.explain.players_awake" + (missing == 1 ? ".one" : ""), sleeping, needed, missing);
        } catch (NumberFormatException e) {
            return message;
        }
    }

    // An argument is a number on the server's side and may come as text over the network.
    private static int number(Object arg) {
        if (arg instanceof Number number) return number.intValue();
        return Integer.parseInt(arg instanceof Component component ? component.getString() : arg.toString());
    }
}
