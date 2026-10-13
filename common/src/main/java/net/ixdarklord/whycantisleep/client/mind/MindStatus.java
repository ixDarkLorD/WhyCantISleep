package net.ixdarklord.whycantisleep.client.mind;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.client.hud.HudStyle;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.mind.Mind;
import net.ixdarklord.whycantisleep.mind.MoodLevel;
import net.ixdarklord.whycantisleep.mind.LucidityStage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * The player's mood and lucidity as the game shows any status: an icon and a bar for each. It is drawn above the
 * inventory ({@code InventoryScreenMixin}) as two gauges, each a medal holding its icon with its bar running out of
 * it, shaped unlike each other and unlike the HUD's cards; pointing at either one says in words how the
 * player feels and what is behind it. The icons are also what {@link MindHud} puts under the crosshair.
 */
public final class MindStatus {
    // Drawn by tools/gen_mind_icons.ps1: a row of four faces for mood, a row of four eyes for lucidity, each 9 by 9.
    private static final Identifier ICONS = WhyCantISleep.id("textures/gui/mind_icons.png");
    public static final int ICON_SIZE = 9;
    private static final int SHEET_WIDTH = 36;
    private static final int SHEET_HEIGHT = 18;
    // A gauge: a medal with the icon on it, and a track running out from under its right side for the bar.
    private static final int TRACK_X = HudStyle.MEDAL_SIZE - 4;
    private static final int TRACK_Y = (HudStyle.MEDAL_SIZE - HudStyle.TRACK_HEIGHT) / 2;
    // The groove inside the track that the fill runs along: clear of the medal on the left and of the end on the right.
    private static final int FILL_X = TRACK_X + 6;
    private static final int FILL_WIDTH = HudStyle.TRACK_WIDTH - 6 - 8;
    private static final int GAUGE_WIDTH = TRACK_X + HudStyle.TRACK_WIDTH;
    // The inventory's own width; the two gauges sit side by side, centered over it.
    private static final int INVENTORY_WIDTH = 176;
    private static final int BETWEEN = 8;
    private static final int WIDTH = GAUGE_WIDTH * 2 + BETWEEN;
    private static final int RISE = HudStyle.MEDAL_SIZE + 3;
    // Where each bar changes what it means, marked on its track: mood's levels, lucidity's stages.
    private static final float[] MOOD_MARKS = {25.0F, 40.0F, 55.0F, 75.0F};
    private static final float[] LUCIDITY_MARKS = {25.0F, 50.0F, 75.0F};
    // A glint runs along each fill every few seconds: this long between glints, this long to cross.
    private static final int GLINT_EVERY = 90;
    private static final int GLINT_TICKS = 18;

    private MindStatus() {
    }

    /** Whether the status has anything to show: the server runs the system, and the option is on. */
    public static boolean isShown() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && !minecraft.player.isCreative() && !minecraft.player.isSpectator() && WCISConfigs.isEnabled(WCISClientConfig.INVENTORY_STATUS);
    }

    /** Draws the status above the inventory whose top left corner is given, with a tooltip for whichever is pointed at. */
    public static void render(GuiGraphicsExtractor graphics, int left, int top, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isShown()) return;
        Mind mind = Mind.get(minecraft.player);
        int x = left + (INVENTORY_WIDTH - WIDTH) / 2;
        int y = top - RISE;
        int lucidityX = x + GAUGE_WIDTH + BETWEEN;
        float time = Util.getMillis() / 50.0F;
        gauge(graphics, HudStyle.MOOD_TRACK, HudStyle.MOOD_MEDAL, HudStyle.MOOD_FILL, x, y, mind.mood(), mind.moodCeiling(), MOOD_MARKS, time);
        moodIcon(graphics, mind.moodLevel(), x + HudStyle.MEDAL_INSET, y + HudStyle.MEDAL_INSET, 1.0F);
        gauge(graphics, HudStyle.LUCIDITY_TRACK, HudStyle.LUCIDITY_MEDAL, HudStyle.LUCIDITY_FILL, lucidityX, y, mind.lucidity(), mind.lucidityCeiling(), LUCIDITY_MARKS, time + GLINT_EVERY / 2.0F);
        lucidityIcon(graphics, mind.stage(), lucidityX + HudStyle.MEDAL_INSET, y + HudStyle.MEDAL_INSET, 1.0F);
        if (mouseY >= y && mouseY < y + HudStyle.MEDAL_SIZE) {
            Font font = minecraft.font;
            if (mouseX >= x && mouseX < x + GAUGE_WIDTH) graphics.setComponentTooltipForNextFrame(font, moodLines(mind), mouseX, mouseY);
            else if (mouseX >= lucidityX && mouseX < lucidityX + GAUGE_WIDTH) graphics.setComponentTooltipForNextFrame(font, lucidityLines(mind), mouseX, mouseY);
        }
    }

    // One gauge: the track first, its fill with a glint passing over it, the marks where its meaning changes, what the
    // other bar closes off hatched at the far end with a bright stop where it begins, then the medal over the track's
    // near end.
    private static void gauge(GuiGraphicsExtractor graphics, Identifier track, Identifier medal, Identifier fill, int x, int y, float value, float ceiling,
                              float[] marks, float time) {
        int fillX = x + FILL_X;
        int trackY = y + TRACK_Y;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, track, x + TRACK_X, trackY, HudStyle.TRACK_WIDTH, HudStyle.TRACK_HEIGHT);
        int filled = value <= 0.0F ? 0 : Math.max(1, Math.round(FILL_WIDTH * Mth.clamp(value / Mind.MAX, 0.0F, 1.0F)));
        if (filled > 0) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, fill, fillX, trackY + 2, filled, HudStyle.GAUGE_FILL_HEIGHT);

        float glint = time % GLINT_EVERY;
        if (glint < GLINT_TICKS && filled > 3) {
            int at = fillX + Math.round(glint / GLINT_TICKS * (filled + 2)) - 2;
            for (int i = 0; i < 3; i++) {
                int column = at + i;
                if (column >= fillX && column < fillX + filled) graphics.fill(column, trackY + 3, column + 1, trackY + 8, i == 1 ? 0x90FFFFFF : 0x40FFFFFF);
            }
        }
        // A mark is a nick in the groove's top and bottom edges, dark over the fill and faint over the empty track.
        for (float mark : marks) {
            int column = fillX + Math.round(FILL_WIDTH * mark / Mind.MAX);
            int color = column < fillX + filled ? 0xA0101014 : 0x50FFFFFF;
            graphics.fill(column, trackY + 3, column + 1, trackY + 4, color);
            graphics.fill(column, trackY + 7, column + 1, trackY + 8, color);
        }

        int closed = Math.round(FILL_WIDTH * (1.0F - Mth.clamp(ceiling / Mind.MAX, 0.0F, 1.0F)));
        for (int i = 0; i < closed; i++) {
            int column = fillX + FILL_WIDTH - 1 - i;
            graphics.fill(column, trackY + 3, column + 1, trackY + 8, i % 2 == 0 ? 0xE0101014 : 0x70101014);
        }
        if (closed > 0) graphics.fill(fillX + FILL_WIDTH - closed - 1, trackY + 3, fillX + FILL_WIDTH - closed, trackY + 8, 0xD0FFFFFF);
        HudStyle.medal(graphics, medal, x, y, 1.0F);
    }

    public static void moodIcon(GuiGraphicsExtractor graphics, MoodLevel level, int x, int y, float alpha) {
        int column = switch (level) {
            case CHEERFUL -> 0;
            case CONTENT, FLAT -> 1;
            case GLOOMY -> 2;
            case MISERABLE -> 3;
        };
        icon(graphics, column, 0, x, y, alpha);
    }

    public static void lucidityIcon(GuiGraphicsExtractor graphics, LucidityStage stage, int x, int y, float alpha) {
        icon(graphics, 3 - stage.ordinal(), 1, x, y, alpha);
    }

    private static void icon(GuiGraphicsExtractor graphics, int column, int row, int x, int y, float alpha) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, ICONS, x, y, column * ICON_SIZE, row * ICON_SIZE, ICON_SIZE, ICON_SIZE, SHEET_WIDTH, SHEET_HEIGHT,
                ARGB.white(Mth.clamp(alpha, 0.0F, 1.0F)));
    }

    // "Mood: Content", then each need in a word, then what is lifting or weighing on it just now.
    private static List<Component> moodLines(Mind mind) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.whycantisleep.mind.mood", Component.translatable("gui.whycantisleep.mind.mood." + mind.moodLevel().key())).withStyle(ChatFormatting.YELLOW));
        lines.add(need("rest", mind.rest()));
        lines.add(need("food", mind.food()));
        lines.add(need("outdoors", mind.outdoors()));
        if (mind.cheer() >= 2.0F) lines.add(Component.translatable("gui.whycantisleep.mind.cheer").withStyle(ChatFormatting.GREEN));
        if (mind.strain() >= 2.0F) lines.add(Component.translatable("gui.whycantisleep.mind.strain").withStyle(ChatFormatting.RED));
        if (mind.moodCeiling() < Mind.MAX) lines.add(Component.translatable("gui.whycantisleep.mind.limit.mood").withStyle(ChatFormatting.LIGHT_PURPLE));
        return lines;
    }

    private static Component need(String name, float met) {
        String word = met >= 0.7F ? "good" : met >= 0.4F ? "fair" : "poor";
        ChatFormatting color = met >= 0.7F ? ChatFormatting.GRAY : met >= 0.4F ? ChatFormatting.GOLD : ChatFormatting.RED;
        return Component.translatable("gui.whycantisleep.mind.need." + name, Component.translatable("gui.whycantisleep.mind.need." + word).withStyle(color)).withStyle(ChatFormatting.GRAY);
    }

    // "Lucidity: Steady", then which way it is going and why.
    private static List<Component> lucidityLines(Mind mind) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.whycantisleep.mind.lucidity", Component.translatable("gui.whycantisleep.mind.lucidity." + mind.stage().key())).withStyle(ChatFormatting.LIGHT_PURPLE));
        MoodLevel mood = mind.moodLevel();
        String drift = mood.ordinal() >= MoodLevel.CONTENT.ordinal() ? (mind.lucidity() >= mind.lucidityCeiling() - 0.5F ? "full" : "recovering") : mood.isLow() ? "wearing" : "holding";
        lines.add(Component.translatable("gui.whycantisleep.mind.lucidity.drift." + drift).withStyle(ChatFormatting.GRAY));
        if (mind.lucidityCeiling() < Mind.MAX) lines.add(Component.translatable("gui.whycantisleep.mind.limit.lucidity").withStyle(ChatFormatting.YELLOW));
        return lines;
    }
}
