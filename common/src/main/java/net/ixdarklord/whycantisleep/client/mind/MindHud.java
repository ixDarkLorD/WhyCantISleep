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
import net.ixdarklord.whycantisleep.mind.LucidityStage;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * The heads-up for a mood or a lucidity that isn't holding. Nothing shows while the player is fine. While mood is flat
 * or worse, or lucidity is anything short of steady, its badge sits beside the hotbar, beating faster the worse it is; and
 * the moment either drops to a low level, a card says so, with what is behind it and what would help.
 */
public final class MindHud {
    // To the right of the hotbar, which is 182 wide.
    private static final int HOTBAR_HALF = 91;
    private static final int MARGIN = 5;

    private static @Nullable MoodLevel lastMood;
    private static @Nullable LucidityStage lastStage;
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

    // A card the moment mood or lucidity drops a level. Nothing on the way back up, and nothing on first sight.
    private static void tick(Minecraft minecraft) {
        tick++;
        if (minecraft.player == null) return;
        Mind mind = Mind.get(minecraft.player);
        MoodLevel mood = mind.moodLevel();
        LucidityStage stage = mind.stage();
        if (isShown(minecraft)) {
            if (lastStage != null && stage.ordinal() < lastStage.ordinal()) {
                String key = "gui.whycantisleep.mind.notice.lucidity." + stage.key();
                SleepNotices.show(new SleepNotice(SleepNotice.Kind.LUCIDITY, Component.translatable(key + ".title"), Component.translatable(key + ".hint")), 120);
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
        // Each is its gauge's medal with its icon on it.
        int x = graphics.guiWidth() / 2 + HOTBAR_HALF + MARGIN;
        int y = graphics.guiHeight() - 19;
        MoodLevel mood = mind.moodLevel();
        if (mood.isUnstable()) {
            float alpha = beat(time, mood == MoodLevel.MISERABLE ? 0.25F : mood == MoodLevel.GLOOMY ? 0.12F : 0.0F);
            HudStyle.medal(graphics, HudStyle.MOOD_MEDAL, x, y, alpha);
            MindStatus.moodIcon(graphics, mood, x + 3, y + 3, alpha);
            x += HudStyle.MEDAL_SIZE + 2;
        }
        LucidityStage stage = mind.stage();
        if (stage != LucidityStage.STEADY) {
            float alpha = beat(time, stage == LucidityStage.BREAKING ? 0.35F : stage == LucidityStage.SHAKEN ? 0.2F : 0.0F);
            HudStyle.medal(graphics, HudStyle.LUCIDITY_MEDAL, x, y, alpha);
            MindStatus.lucidityIcon(graphics, stage, x + 3, y + 3, alpha);
        }
    }
    // A badge's opacity, rising and falling at a pace: the worse things are, the faster. Merely unsteady, it holds still.
    private static float beat(float time, float pace) {
        return pace <= 0.0F ? 1.0F : 0.75F + 0.25F * Mth.sin(time * pace);
    }
}
