package net.ixdarklord.whycantisleep.client.board;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.coolcatcore.api.network.Network;
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
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * The sleep board: while the player is in bed on a server with the mod, a panel listing who is still awake and why,
 * with the progress toward skipping the night. Clicking an awake player nudges them. The panel is drawn on the HUD,
 * under the in-bed chat screen, whose clicks {@code ChatScreenMixin} passes here.
 */
public final class SleepBoard {
    private static final int X = 6;
    private static final int Y = 6;
    private static final int WIDTH = 170;
    private static final int PADDING = 5;
    private static final int ROW_HEIGHT = 11;
    private static final int HEADER_HEIGHT = 20;
    private static final int MAX_ROWS = 10;
    // The server reports once a second; a board older than this is no longer true.
    private static final int STALE_TICKS = 45;

    private static final int BACKGROUND = 0xC0101018;
    private static final int BORDER = 0xFF3A3A55;
    private static final int BAR_BACKGROUND = 0xFF2A2A3A;
    private static final int BAR_FILL = 0xFF6FD06F;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_MUTED = 0xFFA0A0B0;
    private static final int ROW_HOVER = 0x40FFFFFF;

    private static @Nullable SleepBoardPayload board;
    private static long tick;
    private static long receivedAt;
    private static long nudgedAt = Long.MIN_VALUE;

    private SleepBoard() {
    }

    public static void init() {
        ClientTickEvents.END.register(minecraft -> tick++);
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
        int row = Mth.floor((mouseY - (Y + HEADER_HEIGHT + PADDING)) / ROW_HEIGHT);
        return row >= 0 && row < rows() ? row : -1;
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!isShown()) return;
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        boolean nudges = WCISServerConfig.ALLOW_NUDGES.get();
        int rows = rows();
        int more = board.awake().size() - rows;
        int footerLines = (more > 0 ? 1 : 0) + (nudges && rows > 0 ? 1 : 0);
        int height = HEADER_HEIGHT + PADDING + Math.max(rows, 1) * ROW_HEIGHT + footerLines * ROW_HEIGHT + PADDING;

        graphics.fill(X, Y, X + WIDTH, Y + height, BACKGROUND);
        graphics.outline(X, Y, WIDTH, height, BORDER);

        // Header: the title, the count, and the progress toward the rule under them.
        graphics.text(font, Component.translatable("gui.whycantisleep.board.title"), X + PADDING, Y + PADDING, TEXT, true);
        Component count = Component.translatable("gui.whycantisleep.board.progress", board.sleeping(), board.needed());
        graphics.text(font, count, X + WIDTH - PADDING - font.width(count), Y + PADDING, board.sleeping() >= board.needed() ? BAR_FILL : TEXT_MUTED, true);
        int barY = Y + PADDING + 11;
        int barWidth = WIDTH - PADDING * 2;
        graphics.fill(X + PADDING, barY, X + PADDING + barWidth, barY + 3, BAR_BACKGROUND);
        graphics.fill(X + PADDING, barY, X + PADDING + Math.round(barWidth * Mth.clamp(board.sleeping() / (float) Math.max(1, board.needed()), 0.0F, 1.0F)), barY + 3, BAR_FILL);

        int rowY = Y + HEADER_HEIGHT + PADDING;
        if (rows == 0) {
            // Nobody is awake; if that still isn't enough, the rule asks for more players than there are.
            String key = board.sleeping() >= board.needed() ? "gui.whycantisleep.board.everyone_asleep" : "gui.whycantisleep.board.not_possible";
            graphics.text(font, Component.translatable(key), X + PADDING, rowY + 1, TEXT_MUTED, true);
            return;
        }
        int hovered = nudges && minecraft.screen != null
                ? rowAt(minecraft.mouseHandler.getScaledXPos(minecraft.getWindow()), minecraft.mouseHandler.getScaledYPos(minecraft.getWindow())) : -1;
        for (int i = 0; i < rows; i++) {
            SleepBoardPayload.Entry entry = board.awake().get(i);
            int y = rowY + i * ROW_HEIGHT;
            if (i == hovered) graphics.fill(X + 1, y - 1, X + WIDTH - 1, y + ROW_HEIGHT - 1, ROW_HOVER);
            Component reason = Component.translatable(entry.reason().translationKey());
            int reasonWidth = font.width(reason);
            graphics.text(font, reason, X + WIDTH - PADDING - reasonWidth, y + 1, ARGB.opaque(entry.reason().color()), true);
            // A player in another dimension isn't counted toward the rule, and is written dimmer.
            String name = font.plainSubstrByWidth(entry.name(), WIDTH - PADDING * 3 - reasonWidth);
            graphics.text(font, name, X + PADDING, y + 1, entry.counted() ? TEXT : TEXT_MUTED, true);
        }

        int footerY = rowY + rows * ROW_HEIGHT;
        if (more > 0) {
            graphics.text(font, Component.translatable("gui.whycantisleep.board.more", more), X + PADDING, footerY + 1, TEXT_MUTED, true);
            footerY += ROW_HEIGHT;
        }
        if (nudges) {
            long cooldown = cooldown();
            Component hint = cooldown > 0
                    ? Component.translatable("gui.whycantisleep.board.nudge_cooldown", Mth.ceil(cooldown / 20.0F))
                    : Component.translatable("gui.whycantisleep.board.nudge_hint");
            graphics.text(font, hint, X + PADDING, footerY + 1, TEXT_MUTED, true);
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
