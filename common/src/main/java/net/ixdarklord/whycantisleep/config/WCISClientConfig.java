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
    public static final ConfigValue<Boolean> BLOCKER_TRAILS = BUILDER.bool("blockerTrails", true)
            .comment("Leaves faint footprints where the blocking monsters walked, for a while after their highlight is gone.").enabledWhen(HIGHLIGHT_MONSTERS).build();
    static {
        BUILDER.pop();
        BUILDER.push("hud", "Arrows around the crosshair, the action bar text, and the eyelids while hiding.");
    }
    public static final ConfigValue<Boolean> OFF_SCREEN_ARROWS = BUILDER.bool("offScreenArrows", true)
            .comment("Draws arrows around the crosshair pointing to blocking monsters that are off-screen or behind walls.").enabledWhen(WCISServerConfig.ALLOW_ARROWS).build();
    public static final ConfigValue<ArrowSize> ARROW_SIZE = BUILDER.enumValue("arrowSize", ArrowSize.MEDIUM)
            .comment("How large the arrows are.").enabledWhen(OFF_SCREEN_ARROWS).build();
    public static final ConfigValue<EyelidStyle> EYELID_STYLE = BUILDER.enumValue("eyelidStyle", EyelidStyle.RETRO)
            .comment("How the eyelids look while you hide: soft, banded, grainy, or retro pixel art (banded and grainy together).").build();
    public static final ConfigValue<Boolean> SLEEP_DEBT_MOON = BUILDER.bool("sleepDebtMoon", true)
            .comment("Shows a moon in the corner of the screen that fills up with each day you go without sleep. Needs the mod on the server.").build();
    public static final ConfigValue<Boolean> MORNING_CARD = BUILDER.bool("morningCard", true)
            .comment("Tells you in the morning how well you slept, and what woke you if something did. Needs the mod on the server.").enabledWhen(WCISServerConfig.SLEEP_QUALITY).build();
    public static final ConfigValue<Boolean> STATUS_BUTTON = BUILDER.bool("statusButton", true)
            .comment("Shows a moon button on your player in the inventory, which opens how you have been sleeping. Needs the mod on the server.").build();
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
        BUILDER.push("sleepBoard", "The multiplayer bedtime panel.");
    }
    public static final ConfigValue<Boolean> SLEEP_BOARD = BUILDER.bool("sleepBoard", true)
            .comment("While you're in bed, shows who is still awake and why. Needs the mod on the server.").enabledWhen(WCISServerConfig.SLEEP_BOARD).build();
    static {
        BUILDER.pop();
    }

    public static final Config CONFIG = BUILDER.build();

    private WCISClientConfig() {
    }
}
