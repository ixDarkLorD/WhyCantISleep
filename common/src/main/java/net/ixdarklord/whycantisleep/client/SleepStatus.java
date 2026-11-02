package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcanvas.api.client.gui.placement.GuiAreas;
import net.ixdarklord.coolcatcanvas.api.client.gui.placement.GuiPlacement;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.api.SleepTrouble;
import net.ixdarklord.whycantisleep.api.WakeReason;
import net.ixdarklord.whycantisleep.client.hud.GuiSprites;
import net.ixdarklord.whycantisleep.client.hud.HudStyle;
import net.ixdarklord.whycantisleep.client.hud.SleepTooltip;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.network.TroublePayload;
import net.ixdarklord.whycantisleep.network.WokePayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The player's sleep, as they last heard of it, and the button in the inventory's player preview that shows it: a
 * small moon in the preview's corner, its dot the color of their sleep trouble; clicked, a popup beside it tells how
 * badly they are sleeping (the level, how long they have been up with the sleep debt moon as a meter, refused sleeps
 * in a row) and how their last night went (its score as a meter of pips, what mattered most, whether they woke
 * shaken or well rested, or what woke them).
 * The server sends the trouble ({@link TroublePayload}, every 5 s) and each waking ({@link WokePayload}). The button
 * keeps out of the way of anything another mod puts in that corner ({@link #place}).
 */
public final class SleepStatus {
    // The button: a dark tile with a sleeping moon face, lighter under the pointer and lighter still while open
    // (tools/gen_status_button.py); its rim is drawn in the sleep trouble's color.
    private static final ResourceLocation BUTTON_SPRITE = WhyCantISleep.id("hud/status_button");
    private static final ResourceLocation BUTTON_HOVER = WhyCantISleep.id("hud/status_button_hover");
    private static final ResourceLocation BUTTON_OPEN = WhyCantISleep.id("hud/status_button_open");
    // The same, sad: a frown and a tear, while sleep is awful.
    private static final ResourceLocation BUTTON_SAD = WhyCantISleep.id("hud/status_button_sad");
    private static final ResourceLocation BUTTON_SAD_HOVER = WhyCantISleep.id("hud/status_button_sad_hover");
    private static final ResourceLocation BUTTON_SAD_OPEN = WhyCantISleep.id("hud/status_button_sad_open");
    // Tired: bags under the eyes, while restless or after a poor night.
    private static final ResourceLocation BUTTON_TIRED = WhyCantISleep.id("hud/status_button_tired");
    private static final ResourceLocation BUTTON_TIRED_HOVER = WhyCantISleep.id("hud/status_button_tired_hover");
    private static final ResourceLocation BUTTON_TIRED_OPEN = WhyCantISleep.id("hud/status_button_tired_open");
    // A last night scored under these is awful, or poor (the morning card's words for them).
    private static final int AWFUL_BELOW = 25;
    private static final int POOR_BELOW = 45;
    private static final int DAY = 24000;
    private static final int HOUR = 1000;
    // The button: a square in the preview's top right corner, and the moon in it.
    private static final int BUTTON = 8;
    // The popup: its content width, rows, the details shown, and the room their points take.
    private static final int WIDTH = 164;
    private static final int LINE = 10;
    private static final int DETAILS = 3;
    private static final int POINTS_WIDTH = 30;
    private static final int PIP_HEIGHT = 5;
    private static final int MOON = 9;
    private static final int GAP = 6;
    // The beds listed, and the arrows pointing their way: ahead, then clockwise round in eighths.
    private static final int BEDS = 3;
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private static int ticksSinceRest;
    private static int failedAttempts;
    private static @Nullable WokePayload lastNight;
    private static boolean open;
    // Whether the server has said anything yet: one with the mod sends the trouble every few seconds.
    private static boolean heard;
    // The bed rows drawn in the popup this frame, to be clicked (forgotten), and where the pointer was.
    private static final List<BedHit> BED_HITS = new ArrayList<>();
    private static int popupMouseX;
    private static int popupMouseY;

    private SleepStatus() {
    }

    public static void init() {
        ClientPlayerEvents.LEAVE.register(player -> {
            ticksSinceRest = 0;
            failedAttempts = 0;
            lastNight = null;
            open = false;
            heard = false;
        });
    }

    public static void onTrouble(TroublePayload payload) {
        ticksSinceRest = payload.ticksSinceRest();
        failedAttempts = payload.failedAttempts();
        heard = true;
        BedBookmarks.onRespawn(payload.respawn().orElse(null));
    }

    /** Whether the server has the mod: it has told this client about its sleep. Asking it anything before is no use. */
    public static boolean serverHasMod() {
        return heard;
    }

    public static void onWoke(WokePayload payload) {
        lastNight = payload;
    }

    /** How long the player has gone without lying down, in ticks, as the server last said. */
    public static int ticksSinceRest() {
        return ticksSinceRest;
    }

    /** The popup starts closed each time the inventory opens. */
    public static void reset() {
        open = false;
    }

    // The inventory's player preview, from the panel's corner: 26 to 75 across, 8 to 78 down.
    private static final int PREVIEW_X = 26;
    private static final int PREVIEW_Y = 8;
    private static final int PREVIEW_WIDTH = 49;
    private static final int PREVIEW_HEIGHT = 70;

    /**
     * Where the button goes: the top right corner of the inventory's player preview, unless something of another mod's
     * is there (a button, a slot, whatever the screen draws over the preview; CoolCatLib's {@link GuiPlacement}), when
     * it moves left or down the preview to the nearest clear place.
     */
    private static GuiPlacement.Position place(int left, int top) {
        return GuiPlacement.screen()
                .size(BUTTON, BUTTON)
                .at(left + PREVIEW_X + PREVIEW_WIDTH - BUTTON - 1, top + PREVIEW_Y + 1)
                .gap(1)
                .within(left + PREVIEW_X, top + PREVIEW_Y, PREVIEW_WIDTH, PREVIEW_HEIGHT)
                .directions(GuiPlacement.Direction.LEFT, GuiPlacement.Direction.DOWN)
                .find();
    }

    /**
     * A click on the inventory ({@code AbstractRecipeBookScreenMixin}): on the button it opens or shuts the popup;
     * anywhere else it shuts it and carries on. Returns true when the click was the button's.
     */
    public static boolean click(int left, int top, double mouseX, double mouseY, int button) {
        if (!WCISClientConfig.STATUS_BUTTON.get()) return false;
        if (open && button == 0) {
            for (BedHit hit : BED_HITS) {
                if (mouseX >= hit.x && mouseX < hit.x + hit.width && mouseY >= hit.y && mouseY < hit.y + LINE) {
                    BedBookmarks.forget(hit.pos);
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F, 0.8F));
                    return true;
                }
            }
        }
        if (button == 0 && overButton(left, top, mouseX, mouseY)) {
            open = !open;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.6F));
            return true;
        }
        open = false;
        return false;
    }

    private static boolean overButton(int left, int top, double mouseX, double mouseY) {
        GuiPlacement.Position at = place(left, top);
        return over(at.x(), at.y(), mouseX, mouseY);
    }

    private static boolean over(int x, int y, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + BUTTON && mouseY >= y && mouseY < y + BUTTON;
    }

    /**
     * Draws the button, and the popup when open ({@code InventoryScreenMixin}, after the screen); unrecorded by
     * CoolCatLib's {@link GuiAreas}, so the button isn't in its own way.
     */
    public static void render(GuiGraphics graphics, Font font, int left, int top, int mouseX, int mouseY) {
        if (!WCISClientConfig.STATUS_BUTTON.get()) return;
        GuiAreas.unrecorded(() -> render(graphics, font, place(left, top), mouseX, mouseY));
    }

    private static void render(GuiGraphics graphics, Font font, GuiPlacement.Position at, int mouseX, int mouseY) {
        int x = at.x();
        int y = at.y();
        boolean hover = over(x, y, mouseX, mouseY);
        SleepTrouble.Level level = new SleepTrouble(failedAttempts, ticksSinceRest).level();
        int color = color(level);
        // The moon's face follows how you are sleeping: sad when sleepless or after an awful night, tired when restless
        // or after a poor one, else content.
        boolean awful = level == SleepTrouble.Level.SLEEPLESS || (lastNight != null && lastNight.score() < AWFUL_BELOW);
        boolean tired = level == SleepTrouble.Level.RESTLESS || (lastNight != null && lastNight.score() < POOR_BELOW);
        ResourceLocation sprite = awful ? (open ? BUTTON_SAD_OPEN : hover ? BUTTON_SAD_HOVER : BUTTON_SAD)
                : tired ? (open ? BUTTON_TIRED_OPEN : hover ? BUTTON_TIRED_HOVER : BUTTON_TIRED)
                : open ? BUTTON_OPEN : hover ? BUTTON_HOVER : BUTTON_SPRITE;
        GuiSprites.blit(graphics, sprite, x, y, BUTTON, BUTTON);
        // The rim round the tile, its corners left out, in the trouble's color: green rested, amber restless, red
        // sleepless, when it throbs.
        float glow = level == SleepTrouble.Level.SLEEPLESS ? 0.6F + 0.4F * Mth.sin(Util.getMillis() / 160.0F) : 1.0F;
        int rim = HudStyle.color(color, glow);
        int last = BUTTON - 1;
        graphics.fill(x + 1, y, x + last, y + 1, rim);
        graphics.fill(x + 1, y + last, x + last, y + BUTTON, rim);
        graphics.fill(x, y + 1, x + 1, y + last, rim);
        graphics.fill(x + last, y + 1, x + BUTTON, y + last, rim);
        BED_HITS.clear();
        popupMouseX = mouseX;
        popupMouseY = mouseY;
        if (open) {
            // Over the slots: their items and empty-slot icons (the shield's) are drawn 100 nearer and write depth, so
            // the popup is drawn nearer still, as the game draws its tooltips.
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 400.0F);
            popup(graphics, font, x + BUTTON + 4, y, level, color);
            graphics.pose().popPose();
        } else if (hover) graphics.renderTooltip(font, Component.translatable("gui.whycantisleep.status.button"), mouseX, mouseY);
    }

    private static void popup(GuiGraphics graphics, Font font, int x, int y, SleepTrouble.Level level, int color) {
        List<Row> rows = rows(level);
        int height = 0;
        for (Row row : rows) height += row.height;
        height -= 2;
        // Kept on the screen: to the left of the button if there is no room to its right.
        int width = WIDTH;
        if (x + width + HudStyle.PANEL_PADDING > graphics.guiWidth()) x = x - BUTTON - 8 - width;
        y = Mth.clamp(y, HudStyle.PANEL_PADDING_Y, graphics.guiHeight() - height - HudStyle.PANEL_PADDING_Y);
        // A popup over the inventory: backed solid inside the frame's edges, in its own dark grey, so the slots and
        // labels under it don't show through the card and nothing shows past its wavy top and bottom.
        int padX = HudStyle.PANEL_PADDING - 2;
        int padY = HudStyle.PANEL_PADDING_Y - 5;
        graphics.fill(x - padX, y - padY, x + width + padX, y + height + padY, 0xFF141414);
        HudStyle.box(graphics, x, y, width, height, color, 1.0F);
        int rowY = y;
        for (Row row : rows) {
            row.draw.draw(graphics, font, x, rowY, width);
            rowY += row.height;
        }
    }

    // The popup's rows, top to bottom.
    private static List<Row> rows(SleepTrouble.Level level) {
        List<Row> rows = new ArrayList<>();
        String levelName = level.name().toLowerCase(Locale.ROOT);
        int color = color(level);
        // The trouble: the title, its level at the right, and what that means.
        rows.add(new Row(LINE, (graphics, font, x, y, width) -> {
            HudStyle.text(graphics, font, Component.translatable("gui.whycantisleep.status.title"), x, y, HudStyle.color(HudStyle.WHITE, 1.0F), false);
            Component name = Component.translatable("gui.whycantisleep.status.level." + levelName);
            HudStyle.text(graphics, font, name, x + width - font.width(name), y, HudStyle.color(color, 1.0F), false);
        }));
        rows.add(new Row(LINE + GAP, (graphics, font, x, y, width) -> HudStyle.text(graphics, font,
                Component.translatable("gui.whycantisleep.status.level." + levelName + ".hint"), x, y, HudStyle.color(HudStyle.GRAY, 1.0F), false)));
        rows.add(divider());
        rows.add(pair(Component.translatable("gui.whycantisleep.status.awake_for"), awakeFor(), HudStyle.WHITE));
        // The sleep debt moon, as on the HUD, and a meter running to the three days when phantoms come.
        float debt = SleepDebtMoon.fill(ticksSinceRest);
        rows.add(new Row(MOON + 2, (graphics, font, x, y, width) -> {
            SleepDebtMoon.drawMoon(graphics, x, y, MOON, debt);
            HudStyle.pips(graphics, x + MOON + 4, y + (MOON - PIP_HEIGHT) / 2, width - MOON - 4, PIP_HEIGHT, debt, color, 1.0F);
        }));
        if (failedAttempts > 0) {
            rows.add(pair(Component.translatable("gui.whycantisleep.status.refused"), Component.literal(String.valueOf(failedAttempts)), HudStyle.WHITE));
        }
        rows.add(new Row(GAP - 2, (graphics, font, x, y, width) -> {
        }));
        rows.add(divider());

        // The last night.
        WokePayload night = lastNight;
        if (night == null) {
            rows.add(pair(Component.translatable("gui.whycantisleep.status.last_night"), Component.translatable("gui.whycantisleep.status.none"),
                    HudStyle.MUTED));
            beds(rows);
            return rows;
        }
        int nightColor = night.score() >= 65 ? HudStyle.GREEN : night.score() >= 45 ? HudStyle.YELLOW : HudStyle.RED;
        rows.add(new Row(LINE + 1, (graphics, font, x, y, width) -> {
            HudStyle.text(graphics, font, Component.translatable("gui.whycantisleep.status.last_night"), x, y, HudStyle.color(HudStyle.MUTED, 1.0F), false);
            Component out = Component.literal("/100");
            Component score = Component.literal(String.valueOf(night.score()));
            int outX = x + width - font.width(out);
            HudStyle.text(graphics, font, out, outX, y, HudStyle.color(HudStyle.MUTED, 1.0F), false);
            HudStyle.text(graphics, font, score, outX - font.width(score) - 1, y, HudStyle.color(HudStyle.WHITE, 1.0F), false);
        }));
        rows.add(new Row(PIP_HEIGHT + GAP, (graphics, font, x, y, width) ->
                HudStyle.pips(graphics, x, y, width, PIP_HEIGHT, night.score() / 100.0F, nightColor, 1.0F)));
        List<WokePayload.Line> lines = new ArrayList<>();
        for (WokePayload.Line line : night.lines()) {
            if (line.points() != 0) lines.add(line);
        }
        lines.sort(Comparator.comparingInt(line -> -Math.abs(line.points())));
        for (WokePayload.Line line : lines.subList(0, Math.min(DETAILS, lines.size()))) {
            rows.add(new Row(LINE, (graphics, font, x, y, width) -> {
                Component points = Component.literal((line.points() > 0 ? "+" : "−") + Math.abs(line.points()));
                SleepTooltip.icon(graphics, x, y - 1, line.points());
                HudStyle.text(graphics, font, points, x + POINTS_WIDTH - font.width(points), y, HudStyle.color(line.points() > 0 ? HudStyle.GREEN : HudStyle.RED, 1.0F), false);
                HudStyle.text(graphics, font, font.split(line.label(), width - POINTS_WIDTH - 6).get(0), x + POINTS_WIDTH + 6, y,
                        HudStyle.color(HudStyle.SILVER, 1.0F), false);
            }));
        }
        night.dream().ifPresent(dream -> rows.add(new Row(LINE + 2, (graphics, font, x, y, width) -> {
            SleepTooltip.icon(graphics, x, y + 1, dream.kind());
            Component rarity = Component.translatable("gui.whycantisleep.rarity." + dream.rarity().getSerializedName());
            int rarityX = x + width - font.width(rarity);
            HudStyle.text(graphics, font, font.split(dream.name(), rarityX - x - SleepTooltip.ICON_SIZE - 8).get(0), x + SleepTooltip.ICON_SIZE + 4, y + 2,
                    HudStyle.color(dream.rarity().color(), 1.0F), false);
            HudStyle.text(graphics, font, rarity, rarityX, y + 2, HudStyle.color(dream.rarity().color(), 0.7F), false);
        })));
        Component note = note(night);
        if (note != null) {
            rows.add(new Row(LINE + 2, (graphics, font, x, y, width) -> HudStyle.text(graphics, font, note, x, y + 2, HudStyle.color(HudStyle.MUTED, 1.0F), false)));
        }
        beds(rows);
        return rows;
    }

    // The beds slept in lately or named (BedBookmarks), under a divider: its name, which way it is and how far, or that it
    // is in another dimension; a star on the respawn point; the nights slept in it, and its last full night's score at
    // the right. Under the pointer a row is lit, its score a cross: clicked, the bed is forgotten.
    private static void beds(List<Row> rows) {
        List<BedBookmarks.Bed> beds = BedBookmarks.list();
        Minecraft minecraft = Minecraft.getInstance();
        if (beds.isEmpty() || minecraft.player == null || minecraft.level == null) return;
        rows.add(new Row(GAP - 2, (graphics, font, x, y, width) -> {
        }));
        rows.add(divider());
        rows.add(new Row(LINE, (graphics, font, x, y, width) ->
                HudStyle.text(graphics, font, Component.translatable("gui.whycantisleep.status.beds"), x, y, HudStyle.color(HudStyle.MUTED, 1.0F), false)));
        for (BedBookmarks.Bed bed : beds.subList(0, Math.min(BEDS, beds.size()))) {
            rows.add(new Row(LINE, (graphics, font, x, y, width) -> {
                GlobalPos pos = bed.pos();
                int left = x;
                if (BedBookmarks.isRespawn(pos)) {
                    HudStyle.text(graphics, font, "★", left, y, HudStyle.color(HudStyle.GOLD, 1.0F), false);
                    left += font.width("★") + 3;
                }
                Component where = where(minecraft, pos);
                Component nights = Component.translatable(bed.nights() == 1 ? "gui.whycantisleep.status.beds.night" : "gui.whycantisleep.status.beds.nights",
                        bed.nights());
                boolean hovered = popupMouseX >= x && popupMouseX < x + width && popupMouseY >= y && popupMouseY < y + LINE;
                BED_HITS.add(new BedHit(pos, x, y, width));
                if (hovered) graphics.fill(x - 2, y - 1, x + width + 2, y + LINE - 1, HudStyle.color(HudStyle.WHITE, 0.06F));
                Component score = Component.literal(hovered ? "✕" : bed.nights() > 0 ? String.valueOf(bed.score()) : "—");
                int scoreX = x + width - font.width(score);
                int bedColor = hovered ? HudStyle.RED : bed.nights() <= 0 ? HudStyle.MUTED : bed.score() >= 65 ? HudStyle.GREEN : bed.score() >= 45 ? HudStyle.YELLOW : HudStyle.RED;
                HudStyle.text(graphics, font, score, scoreX, y, HudStyle.color(bedColor, 1.0F), false);
                if (bed.name() != null && !bed.name().isEmpty()) {
                    Component name = Component.literal(bed.name());
                    HudStyle.text(graphics, font, font.split(name, (scoreX - left) / 2).get(0), left, y, HudStyle.color(HudStyle.WHITE, 1.0F), false);
                    left += Math.min(font.width(name), (scoreX - left) / 2) + 4;
                }
                HudStyle.text(graphics, font, font.split(where, scoreX - left - 6).get(0), left, y, HudStyle.color(HudStyle.SILVER, 1.0F), false);
                int nightsX = left + font.width(where) + 4;
                if (bed.nights() > 0 && nightsX + font.width(nights) < scoreX - 4) {
                    HudStyle.text(graphics, font, nights, nightsX, y, HudStyle.color(HudStyle.FAINT, 1.0F), false);
                }
            }));
        }
    }

    // Which way a bed is from the player and how far, as an arrow turned to where they face; or the other dimension.
    private static Component where(Minecraft minecraft, GlobalPos pos) {
        if (!pos.dimension().equals(minecraft.level.dimension())) return Component.translatable("gui.whycantisleep.status.beds.elsewhere");
        double dx = pos.pos().getX() + 0.5 - minecraft.player.getX();
        double dz = pos.pos().getZ() + 0.5 - minecraft.player.getZ();
        int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        if (distance < 2) return Component.translatable("gui.whycantisleep.status.beds.here");
        // The bed's bearing against the way the player faces, in eighths of a turn: 0 ahead, then clockwise.
        double bearing = Math.toDegrees(Math.atan2(-dx, dz)) - minecraft.player.getYRot();
        int eighth = Math.floorMod((int) Math.round(bearing / 45.0), 8);
        return Component.translatable("gui.whycantisleep.status.beds.away", ARROWS[eighth], distance);
    }

    // How the last night ended, when there is more to say than its score.
    private static @Nullable Component note(WokePayload night) {
        if (night.shaken()) return Component.translatable("gui.whycantisleep.status.note.shaken");
        if (night.wellRested()) return Component.translatable("gui.whycantisleep.status.note.rested");
        if (night.reason() != WakeReason.MORNING) {
            return Component.translatable("gui.whycantisleep.status.note.woken." + night.reason().name().toLowerCase(Locale.ROOT));
        }
        return null;
    }

    // How long the player has been up: days and hours of game time.
    private static Component awakeFor() {
        int days = ticksSinceRest / DAY;
        int hours = (ticksSinceRest % DAY) / HOUR;
        return days > 0 ? Component.translatable("gui.whycantisleep.status.days_hours", days, hours)
                : Component.translatable("gui.whycantisleep.status.hours", hours);
    }

    private static int color(SleepTrouble.Level level) {
        return switch (level) {
            case RESTED -> HudStyle.GREEN;
            case RESTLESS -> HudStyle.YELLOW;
            case SLEEPLESS -> HudStyle.RED;
        };
    }

    private static Row pair(Component label, Component value, int valueColor) {
        return new Row(LINE, (graphics, font, x, y, width) -> {
            HudStyle.text(graphics, font, label, x, y, HudStyle.color(HudStyle.MUTED, 1.0F), false);
            HudStyle.text(graphics, font, value, x + width - font.width(value), y, HudStyle.color(valueColor, 1.0F), false);
        });
    }

    private static Row divider() {
        return new Row(GAP, (graphics, font, x, y, width) -> HudStyle.divider(graphics, x, width, y + GAP / 2 - 3, 1.0F));
    }

    private record Row(int height, Draw draw) {
    }

    private record BedHit(GlobalPos pos, int x, int y, int width) {
    }

    @FunctionalInterface
    private interface Draw {
        void draw(GuiGraphics graphics, Font font, int x, int y, int width);
    }
}
