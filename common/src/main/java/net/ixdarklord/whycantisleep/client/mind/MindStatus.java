package net.ixdarklord.whycantisleep.client.mind;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.client.hud.HudStyle;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.mind.Mind;
import net.ixdarklord.whycantisleep.mind.MoodLevel;
import net.ixdarklord.whycantisleep.mind.SanityStage;
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
 * The player's mood and sanity as the game shows any status: an icon and a bar for each. It is drawn above the
 * inventory ({@code InventoryScreenMixin}), each on a panel of its own; pointing at either one says in words how the
 * player feels and what is behind it. The icons are also what {@link MindHud} puts beside the hotbar.
 */
public final class MindStatus {
    // Drawn by tools/gen_mind_icons.ps1: a row of four faces for mood, a row of four eyes for sanity, each 9 by 9.
    private static final Identifier ICONS = WhyCantISleep.id("textures/gui/mind_icons.png");
    public static final int ICON_SIZE = 9;
    private static final int SHEET_WIDTH = 36;
    private static final int SHEET_HEIGHT = 18;
    private static final int BAR_WIDTH = 60;
    private static final int GAP = 3;
    // The inventory's own width; the two panels sit side by side, centered over it.
    private static final int INVENTORY_WIDTH = 176;
    private static final int PART_WIDTH = ICON_SIZE + GAP + BAR_WIDTH;
    private static final int BETWEEN = HudStyle.PANEL_PADDING * 2 + 4;
    private static final int WIDTH = PART_WIDTH * 2 + BETWEEN;
    private static final int RISE = 16;
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
        int sanityX = x + PART_WIDTH + BETWEEN;
        // Each on a panel of its own: mood's warm, sanity's the color of the storm.
        HudStyle.panel(graphics, HudStyle.MOOD_PANEL, x, y, PART_WIDTH, ICON_SIZE, 1.0F);
        HudStyle.panel(graphics, HudStyle.SANITY_PANEL, sanityX, y, PART_WIDTH, ICON_SIZE, 1.0F);

        moodIcon(graphics, mind.moodLevel(), x, y, 1.0F);
        HudStyle.bar(graphics, HudStyle.MOOD_FILL, x + ICON_SIZE + GAP, y + 2, BAR_WIDTH, mind.mood() / Mind.MAX, 1.0F);
        sanityIcon(graphics, mind.stage(), sanityX, y, 1.0F);
        HudStyle.bar(graphics, HudStyle.SANITY_FILL, sanityX + ICON_SIZE + GAP, y + 2, BAR_WIDTH, mind.sanity() / Mind.MAX, 1.0F);
        // Guilt shows as the part of the sanity bar that can't be won back: hatched off from its far end.
        int lost = Math.round((BAR_WIDTH - 2) * (1.0F - mind.sanityCap() / Mind.MAX));
        for (int i = 0; i < lost; i++) {
            int column = sanityX + ICON_SIZE + GAP + BAR_WIDTH - 2 - i;
            graphics.fill(column, y + 3, column + 1, y + 6, i % 2 == 0 ? 0xE0101014 : 0x70101014);
        }
        if (mouseY >= y - HudStyle.PANEL_PADDING && mouseY < y + ICON_SIZE + HudStyle.PANEL_PADDING) {
            Font font = minecraft.font;
            if (mouseX >= x - HudStyle.PANEL_PADDING && mouseX < x + PART_WIDTH + HudStyle.PANEL_PADDING) graphics.setComponentTooltipForNextFrame(font, moodLines(mind), mouseX, mouseY);
            else if (mouseX >= sanityX - HudStyle.PANEL_PADDING && mouseX < sanityX + PART_WIDTH + HudStyle.PANEL_PADDING) graphics.setComponentTooltipForNextFrame(font, sanityLines(mind), mouseX, mouseY);
        }
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

    public static void sanityIcon(GuiGraphicsExtractor graphics, SanityStage stage, int x, int y, float alpha) {
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
        return lines;
    }

    private static Component need(String name, float met) {
        String word = met >= 0.7F ? "good" : met >= 0.4F ? "fair" : "poor";
        ChatFormatting color = met >= 0.7F ? ChatFormatting.GRAY : met >= 0.4F ? ChatFormatting.GOLD : ChatFormatting.RED;
        return Component.translatable("gui.whycantisleep.mind.need." + name, Component.translatable("gui.whycantisleep.mind.need." + word).withStyle(color)).withStyle(ChatFormatting.GRAY);
    }

    // "Sanity: Steady", then which way it is going and why, then what guilt is doing to it.
    private static List<Component> sanityLines(Mind mind) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.whycantisleep.mind.sanity", Component.translatable("gui.whycantisleep.mind.sanity." + mind.stage().key())).withStyle(ChatFormatting.LIGHT_PURPLE));
        MoodLevel mood = mind.moodLevel();
        String drift = mood.ordinal() >= MoodLevel.CONTENT.ordinal() ? (mind.sanity() >= mind.sanityCap() - 0.5F ? "full" : "recovering") : mood.isLow() ? "wearing" : "holding";
        lines.add(Component.translatable("gui.whycantisleep.mind.sanity.drift." + drift).withStyle(ChatFormatting.GRAY));
        if (mind.guilt() >= 5.0F) lines.add(Component.translatable("gui.whycantisleep.mind.guilt." + (mind.guilt() >= 40.0F ? "heavy" : "some")).withStyle(ChatFormatting.DARK_RED));
        return lines;
    }
}
