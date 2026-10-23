package net.ixdarklord.whycantisleep.client.hud;

import net.ixdarklord.coolcatcore.api.event.v2.client.ItemTooltipEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.api.SleepEffect;
import net.ixdarklord.whycantisleep.sleep.SleepDecor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The buff, debuff and neutral icons (on the morning card, in the sleep status, in tooltips), and the part of an
 * item's tooltip that says it changes sleep quality when placed by a bed ({@link SleepDecor}). That part closes the
 * tooltip (CoolCatLib's {@link ItemTooltipEvents#APPEND}, after every other mod's lines): the buff or debuff icon,
 * "Soothing" or "Unsettling" and the points each one is worth, how far they go together, and a meter of as many pips
 * as count, the first lit: one of them is one pip. The icon and the pips are set in the text as glyphs of a font of the
 * mod's own ({@code font/icons.json}, the same pictures as the sprites): 1.21.1's text can't hold sprites.
 */
public final class SleepTooltip {
    public static final ResourceLocation BUFF = WhyCantISleep.id("hud/buff");
    public static final ResourceLocation DEBUFF = WhyCantISleep.id("hud/debuff");
    public static final ResourceLocation NEUTRAL = WhyCantISleep.id("hud/neutral");
    public static final int ICON_SIZE = 9;
    // The font the icons are glyphs of in tooltips, and their characters: the buff and debuff icons, and the meter's pips
    // (tools/gen_buffs.py).
    private static final ResourceLocation ICONS = WhyCantISleep.id("icons");
    private static final String BUFF_GLYPH = String.valueOf((char) 0xE000);
    private static final String DEBUFF_GLYPH = String.valueOf((char) 0xE001);
    private static final String PIP_SOOTHING = String.valueOf((char) 0xE003);
    private static final String PIP_UNSETTLING = String.valueOf((char) 0xE004);
    private static final String PIP_EMPTY = String.valueOf((char) 0xE005);

    private SleepTooltip() {
    }

    public static void init() {
        ItemTooltipEvents.APPEND.register((stack, context, player, flag, lines) -> append(stack, lines));
    }

    /** Draws a buff icon for points above nothing, a debuff icon for points below. */
    public static void icon(GuiGraphics graphics, int x, int y, int points) {
        HudStyle.sprite(graphics, points > 0 ? BUFF : DEBUFF, x, y, ICON_SIZE, ICON_SIZE);
    }

    /** Draws the icon of a sleep effect's kind: buff, debuff, or neutral. */
    public static void icon(GuiGraphics graphics, int x, int y, SleepEffect.Kind kind) {
        ResourceLocation sprite = switch (kind) {
            case POSITIVE -> BUFF;
            case NEGATIVE -> DEBUFF;
            case NEUTRAL -> NEUTRAL;
        };
        HudStyle.sprite(graphics, sprite, x, y, ICON_SIZE, ICON_SIZE);
    }

    private static void append(ItemStack stack, List<Component> lines) {
        int points = SleepDecor.points(stack);
        if (points == 0) return;
        boolean soothing = points > 0;
        int most = soothing ? SleepDecor.SOOTHING_MOST : SleepDecor.UNSETTLING_MOST;
        ChatFormatting color = soothing ? ChatFormatting.GREEN : ChatFormatting.RED;
        lines.add(Component.empty());
        lines.add(glyph(soothing ? BUFF_GLYPH : DEBUFF_GLYPH).append(" ")
                .append(Component.translatable(soothing ? "gui.whycantisleep.tooltip.soothing" : "gui.whycantisleep.tooltip.unsettling").withStyle(color))
                .append(Component.literal("  ").append(Component.translatable("gui.whycantisleep.tooltip.points", signed(points))).withStyle(ChatFormatting.WHITE)));
        lines.add(Component.translatable(soothing ? "gui.whycantisleep.tooltip.soothing.hint" : "gui.whycantisleep.tooltip.unsettling.hint", signed(most))
                .withStyle(ChatFormatting.GRAY));
        // As many pips as count before the most is reached, the first lit: one of them is one pip.
        MutableComponent meter = Component.empty();
        int pips = Math.max(1, Math.abs(most / points));
        for (int i = 0; i < pips; i++) meter.append(glyph(i == 0 ? (soothing ? PIP_SOOTHING : PIP_UNSETTLING) : PIP_EMPTY));
        lines.add(meter);
    }

    // An icon of the mod's font set in the text, in white so its colors show as drawn.
    private static MutableComponent glyph(String glyph) {
        return Component.literal(glyph).withStyle(style -> style.withFont(ICONS).withColor(ChatFormatting.WHITE));
    }

    private static String signed(int points) {
        return (points > 0 ? "+" : "−") + Math.abs(points);
    }
}
