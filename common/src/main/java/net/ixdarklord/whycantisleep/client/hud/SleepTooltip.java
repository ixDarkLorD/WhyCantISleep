package net.ixdarklord.whycantisleep.client.hud;

import net.ixdarklord.coolcatcore.api.event.v2.client.ItemTooltipEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.api.SleepEffect;
import net.ixdarklord.whycantisleep.client.SleepStatus;
import net.ixdarklord.whycantisleep.sleep.DecorDiscovery;
import net.ixdarklord.whycantisleep.sleep.SleepDecor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The buff, debuff and neutral icons (on the morning card, in the sleep status, in tooltips), and the part of an
 * item's tooltip that says it changes sleep quality when placed by a bed ({@link SleepDecor}). That part closes the
 * tooltip (CoolCatLib's {@link ItemTooltipEvents#APPEND}, after every other mod's lines): "Soothing" or "Unsettling" by
 * the buff or debuff icon, and under it what one is worth by a bed, and a meter of as many pips as count, the first lit
 * (one of them is one pip), with how far they go together. The icon and the pips are set in the text as glyphs of a font of the
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
    private static final String NEUTRAL_GLYPH = String.valueOf((char) 0xE002);
    private static final String PIP_SOOTHING = String.valueOf((char) 0xE003);
    private static final String PIP_UNSETTLING = String.valueOf((char) 0xE004);
    private static final String PIP_EMPTY = String.valueOf((char) 0xE005);
    // A space of 2 after the icon (10 wide in text) puts the heading's word 12 in, where the lines under it start after
    // three spaces of 4.
    private static final String GAP_GLYPH = String.valueOf((char) 0xE006);
    private static final String INDENT = "   ";

    private SleepTooltip() {
    }

    public static void init() {
        ItemTooltipEvents.APPEND.register((stack, context, player, flag, lines) -> append(stack, lines));
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
        HudStyle.sprite(graphics, sprite, x, y, ICON_SIZE, ICON_SIZE, HudStyle.white(Math.min(alpha, 1.0F)));
    }

    /**
     * Whether the player knows what an item does by a bed ({@link DecorDiscovery}). Without a world, or on a server
     * without the mod (which keeps no record of it), everything is known.
     */
    public static boolean known(ItemStack stack) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null || !SleepStatus.serverHasMod() || DecorDiscovery.knows(player, stack);
    }

    /** Whether the player has at least slept near an item, so knows it does something to sleep. */
    public static boolean noticed(ItemStack stack) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null || !SleepStatus.serverHasMod() || DecorDiscovery.noticed(player, stack);
    }

    private static void append(ItemStack stack, List<Component> lines) {
        int points = SleepDecor.points(stack);
        if (points == 0) return;
        if (!known(stack)) {
            // Never slept near: nothing. Slept near: that it is something, by the neutral icon, and how to find out what.
            if (!noticed(stack)) return;
            if (lines.size() > 1) lines.add(Component.empty());
            lines.add(Component.empty().append(glyph(NEUTRAL_GLYPH + GAP_GLYPH))
                    .append(Component.translatable("gui.whycantisleep.tooltip.unknown").withStyle(ChatFormatting.GRAY)));
            lines.add(Component.literal(INDENT).append(Component.translatable("gui.whycantisleep.tooltip.unknown.hint").withStyle(ChatFormatting.DARK_GRAY)));
            return;
        }
        boolean soothing = points > 0;
        int most = soothing ? SleepDecor.SOOTHING_MOST : SleepDecor.UNSETTLING_MOST;
        ChatFormatting color = soothing ? ChatFormatting.GREEN : ChatFormatting.RED;
        // A blank line sets the section apart from the lines above it, but not from the name alone, where it would only
        // leave a gap.
        if (lines.size() > 1) lines.add(Component.empty());
        // The kind by its icon, as a heading; under it, in line with its word, what one is worth by a bed, then the
        // meter: as many pips as count before the most is reached, the first lit (one of them is one pip), and that most.
        // The icon sits beside the words, not holding them: what is appended to a component takes on its font.
        lines.add(Component.empty().append(glyph((soothing ? BUFF_GLYPH : DEBUFF_GLYPH) + GAP_GLYPH))
                .append(Component.translatable(soothing ? "gui.whycantisleep.tooltip.soothing" : "gui.whycantisleep.tooltip.unsettling").withStyle(color)));
        lines.add(Component.literal(INDENT).append(Component.translatable("gui.whycantisleep.tooltip.worth",
                Component.literal(signed(points)).withStyle(color)).withStyle(ChatFormatting.GRAY)));
        MutableComponent meter = Component.literal(INDENT);
        int pips = Math.max(1, Math.abs(most / points));
        for (int i = 0; i < pips; i++) meter.append(glyph(i == 0 ? (soothing ? PIP_SOOTHING : PIP_UNSETTLING) : PIP_EMPTY));
        meter.append(Component.literal(" ").append(Component.translatable(soothing ? "gui.whycantisleep.tooltip.soothing.most" : "gui.whycantisleep.tooltip.unsettling.most",
                signed(most))).withStyle(ChatFormatting.DARK_GRAY));
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
