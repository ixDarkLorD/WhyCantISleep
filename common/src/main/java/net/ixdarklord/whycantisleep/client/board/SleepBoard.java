package net.ixdarklord.whycantisleep.client.board;

import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.whycantisleep.client.hud.HudStyle;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.network.NudgePayload;
import net.ixdarklord.whycantisleep.network.SleepBoardPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * The sleep HUD's board: while the player is in bed on a server with the mod, a card listing who is still awake and
 * why, with the progress toward skipping the night. Clicking an awake player nudges them. The card is drawn on the
 * HUD, under the in-bed chat screen, whose clicks {@code ChatScreenMixin} passes here.
 */
public final class SleepBoard {
    private static final int X = 6;
    private static final int Y = 6;
    private static final int WIDTH = 176;
    private static final int ROW_HEIGHT = 11;
    private static final int HEADER_HEIGHT = 30;
    private static final int MAX_ROWS = 10;
    // The server reports once a second; a board older than this is no longer true.
    private static final int STALE_TICKS = 45;
    private static final float APPEAR_TICKS = 8.0F;
    private static final int ACCENT = 0x7C8CFF;

    private static @Nullable SleepBoardPayload board;
    private static @Nullable ItemStack icon;
    private static long tick;
    private static long receivedAt;
    private static long shownSince = Long.MIN_VALUE;
    private static long nudgedAt = Long.MIN_VALUE;

    private SleepBoard() {
    }

    public static void init() {
        ClientTickEvents.END.register(minecraft -> {
            tick++;
            if (!isShown()) shownSince = Long.MIN_VALUE;
            else if (shownSince == Long.MIN_VALUE) shownSince = tick;
        });
        ClientPlayerEvents.LEAVE.register(player -> {
            board = null;
            nudgedAt = Long.MIN_VALUE;
        });
        ClientGuiEvents.RENDER_HUD.register(SleepBoard::render);
    }

    public static void onBoard(SleepBoardPayload payload) {
        board = payload;
        receivedAt = tick;
    }

    private static boolean isShown() {
        Minecraft minecraft = Minecraft.getInstance();
        return board != null && tick - receivedAt <= STALE_TICKS && minecraft.player != null && minecraft.player.isSleeping()
                && !minecraft.options.hideGui && WCISConfigs.isEnabled(WCISClientConfig.SLEEP_BOARD);
    }

    private static int rows() {
        return Math.min(board.awake().size(), MAX_ROWS);
    }

    // Ticks until the player may nudge again, by the server's cooldown.
    private static long cooldown() {
        return nudgedAt == Long.MIN_VALUE ? 0L : Math.max(0L, nudgedAt + WCISServerConfig.NUDGE_COOLDOWN.get() * 20L - tick);
    }

    // The awake player's row under the pointer, or -1.
    private static int rowAt(double mouseX, double mouseY) {
        if (mouseX < X || mouseX >= X + WIDTH) return -1;
        int row = Mth.floor((mouseY - (Y + HEADER_HEIGHT)) / ROW_HEIGHT);
        return row >= 0 && row < rows() ? row : -1;
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!isShown() || shownSince == Long.MIN_VALUE) return;
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int padding = HudStyle.PADDING;
        boolean nudges = WCISServerConfig.ALLOW_NUDGES.get();
        int rows = rows();
        int more = board.awake().size() - rows;
        int footerLines = (more > 0 ? 1 : 0) + (nudges && rows > 0 ? 1 : 0);
        int height = HEADER_HEIGHT + Math.max(rows, 1) * ROW_HEIGHT + footerLines * ROW_HEIGHT + padding;

        // The card slides in from the left edge as the player lies down.
        float appear = Mth.clamp((tick - shownSince + delta.getGameTimeDeltaPartialTick(true)) / APPEAR_TICKS, 0.0F, 1.0F);
        int x = X - Math.round((WIDTH + X) * (1.0F - Easing.CUBIC_OUT.apply(appear)));
        float alpha = appear;
        boolean enough = board.sleeping() >= board.needed();
        HudStyle.panel(graphics, x, Y, WIDTH, height, enough ? HudStyle.GOOD : ACCENT, alpha);

        // Header: a bed, the title, the count, and the progress toward the rule under them.
        if (icon == null) icon = new ItemStack(Items.RED_BED);
        if (alpha > 0.6F) graphics.fakeItem(icon, x + padding, Y + 3);
        graphics.text(font, Component.translatable("gui.whycantisleep.board.title"), x + padding + 20, Y + 7, HudStyle.color(HudStyle.TEXT, alpha), true);
        Component count = Component.translatable("gui.whycantisleep.board.progress", board.sleeping(), board.needed());
        graphics.text(font, count, x + WIDTH - padding - font.width(count), Y + 7, HudStyle.color(enough ? HudStyle.GOOD : HudStyle.TEXT_MUTED, alpha), true);
        HudStyle.bar(graphics, x + padding, Y + 21, WIDTH - padding * 2, 3, board.sleeping() / (float) Math.max(1, board.needed()), HudStyle.GOOD, alpha);

        int rowY = Y + HEADER_HEIGHT;
        if (rows == 0) {
            // Nobody is awake; if that still isn't enough, the rule asks for more players than there are.
            String key = enough ? "gui.whycantisleep.board.everyone_asleep" : "gui.whycantisleep.board.not_possible";
            graphics.text(font, Component.translatable(key), x + padding, rowY + 1, HudStyle.color(HudStyle.TEXT_MUTED, alpha), true);
            return;
        }
        int hovered = nudges && minecraft.screen != null
                ? rowAt(minecraft.mouseHandler.getScaledXPos(minecraft.getWindow()), minecraft.mouseHandler.getScaledYPos(minecraft.getWindow())) : -1;
        for (int i = 0; i < rows; i++) {
            SleepBoardPayload.Entry entry = board.awake().get(i);
            int y = rowY + i * ROW_HEIGHT;
            int reasonColor = entry.reason().color();
            if (i == hovered) graphics.fill(x + 2, y - 1, x + WIDTH - 1, y + ROW_HEIGHT - 1, HudStyle.color(HudStyle.TEXT, alpha * 0.14F));
            // A dot in the reason's color, the name, and the reason at the right.
            graphics.fill(x + padding, y + 3, x + padding + 3, y + 6, HudStyle.color(reasonColor, alpha));
            Component reason = Component.translatable(entry.reason().translationKey());
            int reasonWidth = font.width(reason);
            graphics.text(font, reason, x + WIDTH - padding - reasonWidth, y + 1, HudStyle.color(reasonColor, alpha), true);
            // A player in another dimension isn't counted toward the rule, and is written dimmer.
            String name = font.plainSubstrByWidth(entry.name(), WIDTH - padding * 3 - reasonWidth - 6);
            graphics.text(font, name, x + padding + 6, y + 1, HudStyle.color(entry.counted() ? HudStyle.TEXT : HudStyle.TEXT_MUTED, alpha), true);
        }

        int footerY = rowY + rows * ROW_HEIGHT;
        if (more > 0) {
            graphics.text(font, Component.translatable("gui.whycantisleep.board.more", more), x + padding, footerY + 1, HudStyle.color(HudStyle.TEXT_MUTED, alpha), true);
            footerY += ROW_HEIGHT;
        }
        if (nudges) {
            long cooldown = cooldown();
            Component hint = cooldown > 0
                    ? Component.translatable("gui.whycantisleep.board.nudge_cooldown", Mth.ceil(cooldown / 20.0F))
                    : Component.translatable("gui.whycantisleep.board.nudge_hint");
            graphics.text(font, hint, x + padding, footerY + 1, HudStyle.color(HudStyle.TEXT_MUTED, alpha * 0.8F), true);
        }
    }

    /** A click on the in-bed screen ({@code ChatScreenMixin}): on an awake player's row it asks the server to nudge them. */
    public static boolean onClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !isShown() || !WCISServerConfig.ALLOW_NUDGES.get()) return false;
        int row = rowAt(mouseX, mouseY);
        if (row < 0) return false;
        if (cooldown() <= 0) {
            nudgedAt = tick;
            Network.sendToServer(new NudgePayload(board.awake().get(row).id()));
        }
        return true;
    }
}
