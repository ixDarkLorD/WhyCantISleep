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
import net.ixdarklord.coolcatcore.api.utils.ARGB;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The buff, debuff and neutral icons (on the morning card, in the sleep status, in tooltips), and the part of an
 * item's tooltip that says it changes sleep quality when placed by a bed ({@link SleepDecor}). That part closes the
 * tooltip (CoolCatLib's {@link ItemTooltipEvents#APPEND}, after every other mod's lines): the buff or debuff icon,
 * "Soothing" or "Unsettling" and the points each one is worth, how far they go together, and a meter of as many pips
 * as count, the first lit: one of them is one pip. 1.20.1 can't set sprites in text, so in the tooltip the icon and
 * the pips are glyphs in their colors.
 */
public final class SleepTooltip {
    public static final ResourceLocation BUFF = WhyCantISleep.id("hud/buff");
    public static final ResourceLocation DEBUFF = WhyCantISleep.id("hud/debuff");
    public static final ResourceLocation NEUTRAL = WhyCantISleep.id("hud/neutral");
    public static final int ICON_SIZE = 9;

    private SleepTooltip() {
    }

    public static void init() {
        ItemTooltipEvents.APPEND.register((stack, player, flag, lines) -> append(stack, lines));
    }

    /** Draws a buff icon for points above nothing, a debuff icon for points below. */
    public static void icon(GuiGraphics graphics, int x, int y, int points) {
        icon(graphics, x, y, points, 1.0F);
    }

    /** Draws a buff or debuff icon this opaque, so it fades with the card it is on. */
    public static void icon(GuiGraphics graphics, int x, int y, int points, float alpha) {
        sprite(graphics, points > 0 ? BUFF : DEBUFF, x, y, alpha);
    }

    /** Draws the icon of a sleep effect's kind: buff, debuff, or neutral. */
    public static void icon(GuiGraphics graphics, int x, int y, SleepEffect.Kind kind) {
        icon(graphics, x, y, kind, 1.0F);
    }

    /** Draws the icon of a sleep effect's kind this opaque, so it fades with the card it is on. */
    public static void icon(GuiGraphics graphics, int x, int y, SleepEffect.Kind kind, float alpha) {
        ResourceLocation sprite = switch (kind) {
            case POSITIVE -> BUFF;
            case NEGATIVE -> DEBUFF;
            case NEUTRAL -> NEUTRAL;
        };
        sprite(graphics, sprite, x, y, alpha);
    }

    private static void sprite(GuiGraphics graphics, ResourceLocation sprite, int x, int y, float alpha) {
        if (alpha <= 0.0F) return;
        GuiSprites.blit(graphics, sprite, x, y, ICON_SIZE, ICON_SIZE, ARGB.white(Math.min(alpha, 1.0F)));
    }

    private static void append(ItemStack stack, List<Component> lines) {
        int points = SleepDecor.points(stack);
        if (points == 0) return;
        boolean soothing = points > 0;
        int most = soothing ? SleepDecor.SOOTHING_MOST : SleepDecor.UNSETTLING_MOST;
        ChatFormatting color = soothing ? ChatFormatting.GREEN : ChatFormatting.RED;
        lines.add(Component.empty());
        lines.add(Component.literal(soothing ? "▲" : "▼").withStyle(color).append(" ")
                .append(Component.translatable(soothing ? "gui.whycantisleep.tooltip.soothing" : "gui.whycantisleep.tooltip.unsettling").withStyle(color))
                .append(Component.literal("  ").append(Component.translatable("gui.whycantisleep.tooltip.points", signed(points))).withStyle(ChatFormatting.WHITE)));
        lines.add(Component.translatable(soothing ? "gui.whycantisleep.tooltip.soothing.hint" : "gui.whycantisleep.tooltip.unsettling.hint", signed(most))
                .withStyle(ChatFormatting.GRAY));
        // As many pips as count before the most is reached, the first lit: one of them is one pip.
        MutableComponent meter = Component.empty();
        int pips = Math.max(1, Math.abs(most / points));
        for (int i = 0; i < pips; i++) meter.append(Component.literal("■").withStyle(i == 0 ? color : ChatFormatting.DARK_GRAY));
        lines.add(meter);
    }

    private static String signed(int points) {
        return (points > 0 ? "+" : "−") + Math.abs(points);
    }
}
