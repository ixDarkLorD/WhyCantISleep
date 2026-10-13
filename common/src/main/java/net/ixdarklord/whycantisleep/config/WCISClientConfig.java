package net.ixdarklord.whycantisleep.config;

import net.ixdarklord.coolcatcore.api.config.Config;
import net.ixdarklord.coolcatcore.api.config.ConfigBuilder;
import net.ixdarklord.coolcatcore.api.config.ConfigScope;
import net.ixdarklord.coolcatcore.api.config.ConfigValue;
import net.ixdarklord.whycantisleep.WhyCantISleep;

/**
 * Each player's own preferences. An option a server setting gates is declared {@code enabledWhen} that setting, so the
 * config screen greys it out (locked) while the server has it off; code reads those through {@link WCISConfigs}, which
 * applies the same gate.
 */
public final class WCISClientConfig {
    private static final ConfigBuilder BUILDER = Config.builder(WhyCantISleep.MOD_ID, ConfigScope.CLIENT)
            .comment("Why Can't I Sleep? client settings. A server with the mod can turn some of these off for everyone.");

    static {
        BUILDER.push("highlights", "Highlighting the monsters that blocked your sleep.");
    }
    public static final ConfigValue<Boolean> HIGHLIGHT_MONSTERS = BUILDER.bool("highlightMonsters", true)
            .comment("Makes the monsters that blocked your sleep glow.").enabledWhen(WCISServerConfig.ALLOW_HIGHLIGHTS).build();
    public static final ConfigValue<Boolean> THREAT_COLORS = BUILDER.bool("threatColors", true)
            .comment("Colors each highlight by the kind of threat. Off, every highlight uses the highlight color.").enabledWhen(HIGHLIGHT_MONSTERS).build();
    public static final ConfigValue<Integer> HIGHLIGHT_COLOR = BUILDER.color("highlightColor", 0xFF5555)
            .comment("The highlight color when threat colors are off.").enabledWhen(THREAT_COLORS, enabled -> !enabled).build();
    static {
        BUILDER.pop();
        BUILDER.push("hud", "Arrows around the crosshair and the action bar text.");
    }
    public static final ConfigValue<Boolean> OFF_SCREEN_ARROWS = BUILDER.bool("offScreenArrows", true)
            .comment("Draws arrows around the crosshair pointing to blocking monsters that are off-screen or behind walls.").enabledWhen(WCISServerConfig.ALLOW_ARROWS).build();
    public static final ConfigValue<ArrowSize> ARROW_SIZE = BUILDER.enumValue("arrowSize", ArrowSize.MEDIUM)
            .comment("How large the arrows are.").enabledWhen(OFF_SCREEN_ARROWS).build();
    public static final ConfigValue<Boolean> ACTION_BAR_INFO = BUILDER.bool("actionBarInfo", true)
            .comment("Shows how many monsters are nearby and where the closest one is, above the hotbar.").build();
    static {
        BUILDER.pop();
        BUILDER.push("explanations", "Why a sleep failed, and what to do about it.");
    }
    public static final ConfigValue<Boolean> SLEEP_EXPLANATIONS = BUILDER.bool("sleepExplanations", true)
            .comment("Explains every failed sleep with its reason and a hint.").build();
    public static final ConfigValue<Boolean> DIMENSION_WARNING = BUILDER.bool("dimensionWarning", true)
            .comment("Warns before you use a bed where beds explode, such as the Nether and the End.").build();
    static {
        BUILDER.pop();
        BUILDER.push("sleepBoard", "The multiplayer sleep board.");
    }
    public static final ConfigValue<Boolean> SLEEP_BOARD = BUILDER.bool("sleepBoard", true)
            .comment("While you're in bed, shows who is still awake and why. Needs the mod on the server.").enabledWhen(WCISServerConfig.SLEEP_BOARD).build();
    static {
        BUILDER.pop();
        BUILDER.push("spooky", "Whispers and scares.");
    }
    public static final ConfigValue<Boolean> WHISPERS = BUILDER.bool("whispers", true)
            .comment("Plays whispers from the direction of the nearest blocking monster.").build();
    public static final ConfigValue<Integer> WHISPER_VOLUME = BUILDER.intValue("whisperVolume", 70)
            .range(0, 100).slider().comment("How loud the whispers are, in percent.").enabledWhen(WHISPERS).build();
    public static final ConfigValue<Boolean> UNDER_THE_BED_SCARES = BUILDER.bool("underTheBedScares", true)
            .comment("Allows the rare \"monster under the bed\" scare on a failed sleep.").enabledWhen(WCISServerConfig.SCARE_CHANCE, chance -> chance > 0).build();
    public static final ConfigValue<Boolean> SCREEN_SHAKE = BUILDER.bool("screenShake", true)
            .comment("Shakes the screen during a scare.").enabledWhen(UNDER_THE_BED_SCARES).build();
    static {
        BUILDER.pop();
    }

    public static final Config CONFIG = BUILDER.build();

    private WCISClientConfig() {
    }
}
