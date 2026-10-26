package net.ixdarklord.whycantisleep.client.hud;

import net.ixdarklord.coolcatcore.api.event.v2.client.ItemTooltipEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.api.SleepEffect;
import net.ixdarklord.whycantisleep.sleep.SleepDecor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The buff, debuff and neutral icons (on the morning card, in the sleep status, in tooltips), and the part of an
 * item's tooltip that says it changes sleep quality when placed by a bed ({@link SleepDecor}). That part closes the
 * tooltip (CoolCatLib's {@link ItemTooltipEvents#APPEND}, after every other mod's lines): the buff or debuff icon,
 * "Soothing" or "Unsettling" and the points each one is worth, how far they go together, and a meter of as many pips
 * as count, the first lit: one of them is one pip. The icon and the pips are sprites set in the text.
 */
public final class SleepTooltip {
    public static final Identifier BUFF = WhyCantISleep.id("hud/buff");
    public static final Identifier DEBUFF = WhyCantISleep.id("hud/debuff");
    public static final Identifier NEUTRAL = WhyCantISleep.id("hud/neutral");
    public static final int ICON_SIZE = 9;
    // The meter's pips, sprites of a glyph's size (tools/gen_buffs.py).
    private static final Identifier PIP_SOOTHING = WhyCantISleep.id("hud/pip_soothing");
    private static final Identifier PIP_UNSETTLING = WhyCantISleep.id("hud/pip_unsettling");
    private static final Identifier PIP_EMPTY = WhyCantISleep.id("hud/pip_empty");

    private SleepTooltip() {
    }

    public static void init() {
        ItemTooltipEvents.APPEND.register((stack, context, player, flag, lines) -> append(stack, lines));
    }

    /** Draws a buff icon for points above nothing, a debuff icon for points below. */
    public static void icon(GuiGraphicsExtractor graphics, int x, int y, int points) {
        icon(graphics, x, y, points, 1.0F);
    }

    /** Draws a buff or debuff icon this opaque, so it fades with the card it is on. */
    public static void icon(GuiGraphicsExtractor graphics, int x, int y, int points, float alpha) {
        sprite(graphics, points > 0 ? BUFF : DEBUFF, x, y, alpha);
    }

    /** Draws the icon of a sleep effect's kind: buff, debuff, or neutral. */
    public static void icon(GuiGraphicsExtractor graphics, int x, int y, SleepEffect.Kind kind) {
        icon(graphics, x, y, kind, 1.0F);
    }

    /** Draws the icon of a sleep effect's kind this opaque, so it fades with the card it is on. */
    public static void icon(GuiGraphicsExtractor graphics, int x, int y, SleepEffect.Kind kind, float alpha) {
        Identifier sprite = switch (kind) {
            case POSITIVE -> BUFF;
            case NEGATIVE -> DEBUFF;
            case NEUTRAL -> NEUTRAL;
        };
        sprite(graphics, sprite, x, y, alpha);
    }

    private static void sprite(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y, float alpha) {
        if (alpha <= 0.0F) return;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, ICON_SIZE, ICON_SIZE, ARGB.white(Math.min(alpha, 1.0F)));
    }

    private static void append(ItemStack stack, List<Component> lines) {
        int points = SleepDecor.points(stack);
        if (points == 0) return;
        boolean soothing = points > 0;
        int most = soothing ? SleepDecor.SOOTHING_MOST : SleepDecor.UNSETTLING_MOST;
        ChatFormatting color = soothing ? ChatFormatting.GREEN : ChatFormatting.RED;
        lines.add(Component.empty());
        lines.add(sprite(soothing ? BUFF : DEBUFF, soothing ? "+" : "-").append(" ")
                .append(Component.translatable(soothing ? "gui.whycantisleep.tooltip.soothing" : "gui.whycantisleep.tooltip.unsettling").withStyle(color))
                .append(Component.literal("  ").append(Component.translatable("gui.whycantisleep.tooltip.points", signed(points))).withStyle(ChatFormatting.WHITE)));
        lines.add(Component.translatable(soothing ? "gui.whycantisleep.tooltip.soothing.hint" : "gui.whycantisleep.tooltip.unsettling.hint", signed(most))
                .withStyle(ChatFormatting.GRAY));
        // As many pips as count before the most is reached, the first lit: one of them is one pip.
        MutableComponent meter = Component.empty();
        int pips = Math.max(1, Math.abs(most / points));
        for (int i = 0; i < pips; i++) meter.append(sprite(i == 0 ? (soothing ? PIP_SOOTHING : PIP_UNSETTLING) : PIP_EMPTY, i == 0 ? "#" : "-"));
        lines.add(meter);
    }

    // A sprite of the GUI atlas set in the text, with text to stand for it where sprites can't be shown.
    private static MutableComponent sprite(Identifier sprite, String fallback) {
        return Component.object(new AtlasSprite(AtlasIds.GUI, sprite), Component.literal(fallback));
    }

    private static String signed(int points) {
        return (points > 0 ? "+" : "−") + Math.abs(points);
    }
}
