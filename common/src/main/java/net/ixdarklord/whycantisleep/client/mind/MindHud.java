package net.ixdarklord.whycantisleep.client.mind;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
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
 * The heads-up for a mood or a sanity gone low. Nothing shows while the player is fine. When either is low its icon
 * sits beside the hotbar, beating faster the worse it is; and the moment either drops a level, a card says so, with
 * what is behind it and what would help.
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
        int x = graphics.guiWidth() / 2 + HOTBAR_HALF + MARGIN;
        int y = graphics.guiHeight() - 15;
        MoodLevel mood = mind.moodLevel();
        if (mood.isLow()) {
            MindStatus.moodIcon(graphics, mood, x, y, beat(time, mood == MoodLevel.MISERABLE ? 0.25F : 0.1F));
            x += MindStatus.ICON_SIZE + 2;
        }
        SanityStage stage = mind.stage();
        if (stage != SanityStage.STEADY) {
            MindStatus.sanityIcon(graphics, stage, x, y, beat(time, stage == SanityStage.BREAKING ? 0.35F : stage == SanityStage.SHAKEN ? 0.2F : 0.08F));
        }
    }

    // An icon's opacity, rising and falling at a pace: the worse things are, the faster.
    private static float beat(float time, float pace) {
        return 0.7F + 0.3F * Mth.sin(time * pace);
    }
}
