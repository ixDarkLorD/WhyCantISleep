package net.ixdarklord.whycantisleep.client.mind;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.client.hud.HudStyle;
import net.ixdarklord.whycantisleep.client.hud.SleepNotice;
import net.ixdarklord.whycantisleep.client.hud.SleepNotices;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.mind.Mind;
import net.ixdarklord.whycantisleep.mind.MoodLevel;
import net.ixdarklord.whycantisleep.mind.SanityStage;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * The heads-up for a mood or a sanity that isn't holding. Nothing shows while the player is fine. While mood is flat
 * or worse, or sanity is anything short of steady, its badge sits beside the hotbar, beating faster the worse it is; and
 * the moment either drops to a low level, a card says so, with what is behind it and what would help.
 */
public final class MindHud {
    // To the right of the hotbar, which is 182 wide.
    private static final int HOTBAR_HALF = 91;
    private static final int MARGIN = 5;

    private static @Nullable MoodLevel lastMood;
    private static @Nullable SanityStage lastStage;
    private static long tick;

    private MindHud() {
    }

    public static void init() {
        ClientTickEvents.END.register(MindHud::tick);
        ClientPlayerEvents.LEAVE.register(player -> {
            lastMood = null;
            lastStage = null;
        });
        ClientGuiEvents.RENDER_HUD.register(MindHud::render);
    }

    private static boolean isShown(Minecraft minecraft) {
        return minecraft.player != null && !minecraft.player.isCreative() && !minecraft.player.isSpectator() && WCISConfigs.isEnabled(WCISClientConfig.STATUS_INDICATORS);
    }

    // A card the moment mood or sanity drops a level. Nothing on the way back up, and nothing on first sight.
    private static void tick(Minecraft minecraft) {
        tick++;
        if (minecraft.player == null) return;
        Mind mind = Mind.get(minecraft.player);
        MoodLevel mood = mind.moodLevel();
        SanityStage stage = mind.stage();
        if (isShown(minecraft)) {
            if (lastStage != null && stage.ordinal() < lastStage.ordinal()) {
                String key = "gui.whycantisleep.mind.notice.sanity." + stage.key();
                SleepNotices.show(new SleepNotice(SleepNotice.Kind.SANITY, Component.translatable(key + ".title"), Component.translatable(key + ".hint")), 120);
            } else if (lastMood != null && mood.ordinal() < lastMood.ordinal() && mood.isLow()) {
                SleepNotices.show(new SleepNotice(SleepNotice.Kind.MOOD, Component.translatable("gui.whycantisleep.mind.notice.mood." + mood.key() + ".title"),
                        Component.translatable("gui.whycantisleep.mind.notice.mood.hint." + worstNeed(mind))), 120);
            }
        }
        lastMood = mood;
        lastStage = stage;
    }

    // What is most behind a low mood: the least met of the three needs, or the hard time the player is having.
    private static String worstNeed(Mind mind) {
        if (mind.strain() >= 8.0F && mind.strain() > mind.cheer()) return "strain";
        if (mind.rest() <= mind.food() && mind.rest() <= mind.outdoors()) return "rest";
        return mind.food() <= mind.outdoors() ? "food" : "outdoors";
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || !isShown(minecraft)) return;
        Mind mind = Mind.get(minecraft.player);
        float time = tick + delta.getGameTimeDeltaPartialTick(true);
        // Each is a badge the size of a hotbar slot's inside: its own panel with its icon on it.
        int x = graphics.guiWidth() / 2 + HOTBAR_HALF + MARGIN + HudStyle.PANEL_PADDING;
        int y = graphics.guiHeight() - 16;
        MoodLevel mood = mind.moodLevel();
        if (mood.isUnstable()) {
            float alpha = beat(time, mood == MoodLevel.MISERABLE ? 0.25F : mood == MoodLevel.GLOOMY ? 0.12F : 0.0F);
            HudStyle.panel(graphics, HudStyle.MOOD_PANEL, x, y, MindStatus.ICON_SIZE, MindStatus.ICON_SIZE, alpha);
            MindStatus.moodIcon(graphics, mood, x, y, alpha);
            x += MindStatus.ICON_SIZE + HudStyle.PANEL_PADDING * 2 + 1;
        }
        SanityStage stage = mind.stage();
        if (stage != SanityStage.STEADY) {
            float alpha = beat(time, stage == SanityStage.BREAKING ? 0.35F : stage == SanityStage.SHAKEN ? 0.2F : 0.0F);
            HudStyle.panel(graphics, HudStyle.SANITY_PANEL, x, y, MindStatus.ICON_SIZE, MindStatus.ICON_SIZE, alpha);
            MindStatus.sanityIcon(graphics, stage, x, y, alpha);
        }
    }

    // A badge's opacity, rising and falling at a pace: the worse things are, the faster. Merely unsteady, it holds still.
    private static float beat(float time, float pace) {
        return pace <= 0.0F ? 1.0F : 0.75F + 0.25F * Mth.sin(time * pace);
    }
}
