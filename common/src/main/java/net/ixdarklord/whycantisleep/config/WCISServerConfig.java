package net.ixdarklord.whycantisleep.config;

import net.ixdarklord.coolcatcore.api.config.Config;
import net.ixdarklord.coolcatcore.api.config.ConfigBuilder;
import net.ixdarklord.coolcatcore.api.config.ConfigScope;
import net.ixdarklord.coolcatcore.api.config.ConfigValue;
import net.ixdarklord.whycantisleep.WhyCantISleep;

/**
 * What the server allows and decides. CoolCatLib syncs it to every client that joins and whenever it changes, so on a
 * server with the mod these values override the client's; a client on a server without the mod reads its own file.
 */
public final class WCISServerConfig {
    private static final ConfigBuilder BUILDER = Config.builder(WhyCantISleep.MOD_ID, ConfigScope.SERVER)
            .comment("Why Can't I Sleep? server settings. On a server these override each player's client settings.");

    static {
        BUILDER.push("highlights", "Highlighting the monsters that blocked a sleep.");
    }
    public static final ConfigValue<Boolean> ALLOW_HIGHLIGHTS = BUILDER.bool("allowHighlights", true)
            .comment("Whether players may see the monsters that blocked their sleep glow.").build();
    public static final ConfigValue<Integer> HIGHLIGHT_DURATION = BUILDER.intValue("highlightDuration", 3)
            .range(1, 10).slider().comment("Seconds the blocking monsters glow.").build();
    public static final ConfigValue<Boolean> ALLOW_ARROWS = BUILDER.bool("allowArrows", true)
            .comment("Whether players may see arrows pointing to blocking monsters that are off-screen or behind walls.").build();
    public static final ConfigValue<Boolean> SHOW_TO_EVERYONE = BUILDER.bool("showToEveryone", false)
            .comment("Shows highlights to players without the mod too, using the vanilla glowing effect.").build();
    static {
        BUILDER.pop();
        BUILDER.push("sleepBoard", "The multiplayer sleep board and nudges.");
    }
    public static final ConfigValue<Boolean> SLEEP_BOARD = BUILDER.bool("sleepBoard", true)
            .comment("Whether players in bed are shown who is still awake, and why.").build();
    public static final ConfigValue<Boolean> ALLOW_NUDGES = BUILDER.bool("allowNudges", true)
            .comment("Whether a player in bed may nudge an awake player with a sound and a message.").build();
    public static final ConfigValue<Integer> NUDGE_COOLDOWN = BUILDER.intValue("nudgeCooldown", 30)
            .range(0, 600).comment("Seconds a player waits between nudges.").build();
    static {
        BUILDER.pop();
        BUILDER.push("spooky", "Scares and advancements.");
    }
    public static final ConfigValue<Integer> SCARE_CHANCE = BUILDER.intValue("scareChance", 2)
            .range(0, 100).slider().comment("Chance, in percent, of a \"monster under the bed\" scare on a failed sleep. 0 disables it.").build();
    public static final ConfigValue<Boolean> ADVANCEMENTS = BUILDER.bool("advancements", true)
            .comment("Whether the mod's advancements can be earned.").build();
    static {
        BUILDER.pop();
    }

    public static final Config CONFIG = BUILDER.build();

    private WCISServerConfig() {
    }
}
