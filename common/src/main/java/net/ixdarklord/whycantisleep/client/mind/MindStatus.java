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

import java.util.ArrayList;
import java.util.List;

/**
 * The player's mood and lucidity as the game shows any status: an icon and a bar for each. It is drawn above the
 * inventory ({@code InventoryScreenMixin}) as two gauges, each a medal holding its icon with its bar running out of
 * it, shaped unlike each other and unlike the HUD's cards; pointing at either one says in words how the
 * player feels and what is behind it. The icons are also what {@link MindHud} puts beside the hotbar.
 */
public final class MindStatus {
    // Drawn by tools/gen_mind_icons.ps1: a row of four faces for mood, a row of four eyes for lucidity, each 9 by 9.
    private static final Identifier ICONS = WhyCantISleep.id("textures/gui/mind_icons.png");
    public static final int ICON_SIZE = 9;
    private static final int SHEET_WIDTH = 36;
    private static final int SHEET_HEIGHT = 18;
    // A gauge: a medal with the icon on it, and a track running out from under its right side for the bar.
    private static final int ICON_INSET = 3;
    private static final int TRACK_X = HudStyle.MEDAL_SIZE - 3;
    private static final int TRACK_Y = (HudStyle.MEDAL_SIZE - HudStyle.TRACK_HEIGHT) / 2;
    // The groove inside the track that the fill runs along: clear of the medal on the left and of the end on the right.
    private static final int FILL_X = TRACK_X + 5;
    private static final int FILL_WIDTH = HudStyle.TRACK_WIDTH - 5 - 7;
    private static final int GAUGE_WIDTH = TRACK_X + HudStyle.TRACK_WIDTH;
    // The inventory's own width; the two gauges sit side by side, centered over it.
    private static final int INVENTORY_WIDTH = 176;
    private static final int BETWEEN = 10;
    private static final int WIDTH = GAUGE_WIDTH * 2 + BETWEEN;
    private static final int RISE = HudStyle.MEDAL_SIZE + 3;
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
        gauge(graphics, HudStyle.MOOD_TRACK, HudStyle.MOOD_MEDAL, HudStyle.MOOD_FILL, x, y, mind.mood(), mind.moodCeiling());
        moodIcon(graphics, mind.moodLevel(), x + ICON_INSET, y + ICON_INSET, 1.0F);
        gauge(graphics, HudStyle.LUCIDITY_TRACK, HudStyle.LUCIDITY_MEDAL, HudStyle.LUCIDITY_FILL, lucidityX, y, mind.lucidity(), mind.lucidityCeiling());
        lucidityIcon(graphics, mind.stage(), lucidityX + ICON_INSET, y + ICON_INSET, 1.0F);

        if (mouseY >= y && mouseY < y + HudStyle.MEDAL_SIZE) {
            Font font = minecraft.font;
            if (mouseX >= x && mouseX < x + GAUGE_WIDTH) graphics.setComponentTooltipForNextFrame(font, moodLines(mind), mouseX, mouseY);
            else if (mouseX >= lucidityX && mouseX < lucidityX + GAUGE_WIDTH) graphics.setComponentTooltipForNextFrame(font, lucidityLines(mind), mouseX, mouseY);
        }
    }

    // One gauge: the track first, its fill, what the other bar closes off hatched at the far end, then the medal over
    // the track's near end.
    private static void gauge(GuiGraphicsExtractor graphics, Identifier track, Identifier medal, Identifier fill, int x, int y, float value, float ceiling) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, track, x + TRACK_X, y + TRACK_Y, HudStyle.TRACK_WIDTH, HudStyle.TRACK_HEIGHT);
        int filled = value <= 0.0F ? 0 : Math.max(1, Math.round(FILL_WIDTH * Mth.clamp(value / Mind.MAX, 0.0F, 1.0F)));
        if (filled > 0) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, fill, x + FILL_X, y + TRACK_Y + 2, filled, HudStyle.BAR_HEIGHT);
        int closed = Math.round(FILL_WIDTH * (1.0F - Mth.clamp(ceiling / Mind.MAX, 0.0F, 1.0F)));
        for (int i = 0; i < closed; i++) {
            int column = x + FILL_X + FILL_WIDTH - 1 - i;
            graphics.fill(column, y + TRACK_Y + 3, column + 1, y + TRACK_Y + 6, i % 2 == 0 ? 0xE0101014 : 0x70101014);
        }
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
