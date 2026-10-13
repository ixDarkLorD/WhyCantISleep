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
 * or worse, or lucidity is anything short of lucid, its badge sits just under the crosshair, where the player is
 * already looking: it swells in when things get worse, then rests faint enough to see past, beating faster the worse
 * it is. The moment either drops to a low level, a card says so too, with what is behind it and what would help.
 */
public final class MindHud {
    // How far under the crosshair, which reaches 7 down from the screen's middle.
    private static final int BELOW_CROSSHAIR = 11;
    private static final int BETWEEN = 3;
    // A badge that has just appeared or got worse swells in over this long, then settles.
    private static final int POP_TICKS = 50;
    // Small enough to stay clear of the crosshair above and the message card below.
    private static final float POP_SCALE = 0.4F;
    // How solid a badge rests: the mild levels faint, the bad ones plain.
    private static final float REST_ALPHA = 0.55F;
    private static @Nullable MoodLevel lastMood;
    private static @Nullable LucidityStage lastStage;
    private static long tick;
    // When each badge last appeared or got worse; long ago, to begin with.
    private static long moodPop = -POP_TICKS;
    private static long lucidityPop = -POP_TICKS;

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
        if (lastMood != null && mood.ordinal() < lastMood.ordinal() && mood.isUnstable()) moodPop = tick;
        if (lastStage != null && stage.ordinal() < lastStage.ordinal()) lucidityPop = tick;
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
        MoodLevel mood = mind.moodLevel();
        LucidityStage stage = mind.stage();
        boolean showMood = mood.isUnstable();
        boolean showLucidity = stage != LucidityStage.STEADY;
        if (!showMood && !showLucidity) return;
        // Each is its gauge's medal with its icon on it; the two share the middle.
        int width = showMood && showLucidity ? HudStyle.MEDAL_SIZE * 2 + BETWEEN : HudStyle.MEDAL_SIZE;
        float moodPop = showMood ? pop(time, MindHud.moodPop) : 0.0F;
        float lucidityPop = showLucidity ? pop(time, MindHud.lucidityPop) : 0.0F;
        // The two swell as one, about their shared middle, so neither grows over the other.
        float scale = 1.0F + POP_SCALE * Math.max(moodPop, lucidityPop);
        graphics.pose().pushMatrix();
        graphics.pose().translate(graphics.guiWidth() / 2.0F, graphics.guiHeight() / 2 + BELOW_CROSSHAIR + HudStyle.MEDAL_SIZE / 2.0F);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-width / 2.0F, -HudStyle.MEDAL_SIZE / 2.0F);
        int x = 0;
        if (showMood) {
            boolean bad = mood.isLow();
            float alpha = Mth.lerp(moodPop, bad ? 1.0F : REST_ALPHA, 1.0F) * beat(time, mood == MoodLevel.MISERABLE ? 0.25F : bad ? 0.12F : 0.0F);
            HudStyle.medal(graphics, HudStyle.MOOD_MEDAL, x, 0, alpha);
            MindStatus.moodIcon(graphics, mood, x + HudStyle.MEDAL_INSET, HudStyle.MEDAL_INSET, alpha);
            x += HudStyle.MEDAL_SIZE + BETWEEN;
        }
        if (showLucidity) {
            boolean bad = stage.ordinal() <= LucidityStage.SHAKEN.ordinal();
            float alpha = Mth.lerp(lucidityPop, bad ? 1.0F : REST_ALPHA, 1.0F) * beat(time, stage == LucidityStage.BREAKING ? 0.35F : bad ? 0.2F : 0.0F);
            HudStyle.medal(graphics, HudStyle.LUCIDITY_MEDAL, x, 0, alpha);
            MindStatus.lucidityIcon(graphics, stage, x + HudStyle.MEDAL_INSET, HudStyle.MEDAL_INSET, alpha);
        }
        graphics.pose().popMatrix();
    }

    // How far into its swell a badge is: 1 the moment it appears or worsens, easing down to 0.
    private static float pop(float time, long since) {
        float left = 1.0F - (time - since) / POP_TICKS;
        return left <= 0.0F ? 0.0F : left * left;
    }
    // A badge's opacity, rising and falling at a pace: the worse things are, the faster. Merely unsteady, it holds still.
    private static float beat(float time, float pace) {
        return pace <= 0.0F ? 1.0F : 0.75F + 0.25F * Mth.sin(time * pace);
    }
}
