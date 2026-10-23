package net.ixdarklord.whycantisleep.client.board;

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
import net.ixdarklord.whycantisleep.network.VotePayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * The sleep HUD's board: while the player is in bed on a server, a dark card with the count of sleepers over a bar in
 * segments, one for each sleeper the night needs, then who is still awake: each player's face as on the player list,
 * their name, and a dot and word for why. A footer says how to nudge, and when the next nudge is ready. Clicking an
 * awake player nudges them. When only a player or two is still up, a row under them asks to vote to skip the night;
 * clicking it casts the vote, and it then counts the votes, and the time the players still up have to answer. The card
 * is on the HUD, under the in-bed chat screen, whose clicks {@code ChatScreenMixin} passes here.
 */
public final class SleepBoard {
    // The content area's corner; the box pads and frames around it.
    private static final int X = 10;
    private static final int Y = 10;
    private static final int WIDTH = 170;
    private static final int ROW_HEIGHT = 12;
    // The header (the count, a divider, the segments, the "still awake" label) above the first row.
    private static final int HEADER_HEIGHT = 40;
    private static final int SEGMENTS_HEIGHT = 4;
    private static final int FACE_SIZE = 8;
    private static final int DOT_GAP = 4;
    // Where the "still awake" label sits under the segments, and the footer's height (its divider, room, and a line).
    private static final int LABEL_Y = 28;
    private static final int FOOTER_HEIGHT = 16;
    private static final int MAX_ROWS = 10;
    // The server reports once a second; a board older than this is no longer true.
    private static final int STALE_TICKS = 45;
    private static final float APPEAR_TICKS = 6.0F;

    private static @Nullable SleepBoardPayload board;
    private static long tick;
    private static long receivedAt;
    private static long shownSince = Long.MIN_VALUE;
    private static long nudgedAt = Long.MIN_VALUE;
    // The vote row's top, as last drawn, for clicks; -1 when it isn't shown.
    private static int voteY = -1;
    private static final int VOTE_HEIGHT = ROW_HEIGHT + 4;

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
                && !minecraft.gui.hud.isHidden() && WCISConfigs.isEnabled(WCISClientConfig.SLEEP_BOARD);
    }

    private static int rows() {
        return Math.min(board.awake().size(), MAX_ROWS);
    }

    // Ticks until the player may nudge again, by the server's cooldown.
    private static long cooldown() {
        return nudgedAt == Long.MIN_VALUE ? 0L : Math.max(0L, nudgedAt + WCISServerConfig.NUDGE_COOLDOWN.get() * 20L - tick);
    }

    // Whether the board has a vote row: a vote can be cast, or one is under way.
    private static boolean voteShown() {
        SleepBoardPayload.Vote vote = board.vote();
        return vote.open() || vote.waiting() >= 0 || vote.mine();
    }

    // Whether the vote row is under the pointer and can be clicked.
    private static boolean overVote(double mouseX, double mouseY) {
        SleepBoardPayload.Vote vote = board.vote();
        return voteY >= 0 && vote.open() && !vote.mine() && vote.waiting() < 0
                && mouseX >= X && mouseX < X + WIDTH && mouseY >= voteY - 2 && mouseY < voteY - 2 + VOTE_HEIGHT;
    }

    // The awake player's row under the pointer, or -1.
    private static int rowAt(double mouseX, double mouseY) {
        if (mouseX < X || mouseX >= X + WIDTH) return -1;
        // A row's band starts 2 above its text, where its hover highlight does.
        int row = Mth.floor((mouseY - (Y + HEADER_HEIGHT - 2)) / ROW_HEIGHT);
        return row >= 0 && row < rows() ? row : -1;
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!isShown() || shownSince == Long.MIN_VALUE) return;
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        boolean nudges = WCISServerConfig.ALLOW_NUDGES.get();
        int rows = rows();
        int more = board.awake().size() - rows;
        boolean footer = nudges && rows > 0;
        // The rows (or the one line saying nobody is awake), the "and more" line, and the footer under a divider.
        int bodyHeight = rows > 0 ? rows * ROW_HEIGHT : ROW_HEIGHT;
        boolean voting = rows > 0 && voteShown();
        int height = HEADER_HEIGHT + bodyHeight + (more > 0 ? ROW_HEIGHT : 0) + (voting ? VOTE_HEIGHT : 0) + (footer ? FOOTER_HEIGHT : 0) - 4;
        voteY = -1;
        if (rows == 0) height -= HEADER_HEIGHT - LABEL_Y;
        float alpha = Mth.clamp((tick - shownSince + delta.getGameTimeDeltaPartialTick(true)) / APPEAR_TICKS, 0.0F, 1.0F);
        boolean enough = board.sleeping() >= board.needed();
        // The message cards' frame and wash, the wash the green of the sleep segments.
        HudStyle.notice(graphics, X, Y, WIDTH, height, alpha);
        HudStyle.wash(graphics, X, Y, WIDTH, Y + height, alpha, 0x4ADE80, alpha, tick + delta.getGameTimeDeltaPartialTick(true), shownSince);

        // Header: the title and the count, a divider, and the night's sleepers as segments, filled for those asleep.
        graphics.text(font, Component.translatable("gui.whycantisleep.board.title"), X, Y, HudStyle.color(HudStyle.WHITE, alpha), false);
        Component count = Component.translatable("gui.whycantisleep.board.progress", board.sleeping(), board.needed());
        graphics.text(font, count, X + WIDTH - font.width(count), Y, HudStyle.color(enough ? HudStyle.GREEN : HudStyle.SILVER, alpha), false);
        HudStyle.divider(graphics, X, WIDTH, Y + 12, alpha);
        HudStyle.segments(graphics, HudStyle.SLEEP_FILL, X, Y + 18, WIDTH, SEGMENTS_HEIGHT, board.sleeping(), board.needed(), alpha);

        if (rows == 0) {
            // Nobody is awake; if that still isn't enough, the rule asks for more players than there are.
            String key = enough ? "gui.whycantisleep.board.everyone_asleep" : "gui.whycantisleep.board.not_possible";
            graphics.text(font, Component.translatable(key), X, Y + LABEL_Y, HudStyle.color(HudStyle.GRAY, alpha), false);
            return;
        }
        graphics.text(font, Component.translatable("gui.whycantisleep.board.awake"), X, Y + LABEL_Y, HudStyle.color(HudStyle.MUTED, alpha), false);
        int rowY = Y + HEADER_HEIGHT;
        int hovered = nudges && minecraft.gui.screen() != null
                ? rowAt(minecraft.mouseHandler.getScaledXPos(minecraft.getWindow()), minecraft.mouseHandler.getScaledYPos(minecraft.getWindow())) : -1;
        for (int i = 0; i < rows; i++) {
            SleepBoardPayload.Entry entry = board.awake().get(i);
            int y = rowY + i * ROW_HEIGHT;
            if (i == hovered) graphics.fill(X - HudStyle.PANEL_PADDING + 1, y - 2, X + WIDTH + HudStyle.PANEL_PADDING - 1, y + ROW_HEIGHT - 2,
                    HudStyle.color(HudStyle.WHITE, alpha * 0.06F));
            // The player's face as on the player list, their name, and at the right a dot and word for why they're up.
            // A player in another dimension isn't counted toward the rule, and is drawn dimmer.
            float rowAlpha = entry.counted() ? alpha : alpha * 0.6F;
            PlayerInfo info = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(entry.id());
            if (info != null) PlayerFaceExtractor.extractRenderState(graphics, info.getSkin(), X, y, FACE_SIZE, HudStyle.color(HudStyle.WHITE, rowAlpha));
            Component reason = Component.translatable(entry.reason().translationKey());
            int reasonWidth = font.width(reason);
            int reasonX = X + WIDTH - reasonWidth;
            graphics.text(font, reason, reasonX, y, HudStyle.color(entry.reason().color(), alpha), false);
            HudStyle.dot(graphics, reasonX - DOT_GAP - 4, y + 2, entry.reason().color(), alpha);
            String name = font.plainSubstrByWidth(entry.name(), reasonX - DOT_GAP - 4 - 6 - (X + FACE_SIZE + 4));
            graphics.text(font, name, X + FACE_SIZE + 4, y, HudStyle.color(entry.counted() ? HudStyle.WHITE : HudStyle.GRAY, rowAlpha), false);
        }

        int footerY = rowY + rows * ROW_HEIGHT;
        if (more > 0) {
            graphics.text(font, Component.translatable("gui.whycantisleep.board.more", more), X, footerY, HudStyle.color(HudStyle.MUTED, alpha), false);
            footerY += ROW_HEIGHT;
        }
        if (voting) {
            // The vote: an invitation to vote, or the vote cast, or the wait for the players still up; and the count.
            SleepBoardPayload.Vote vote = board.vote();
            HudStyle.divider(graphics, X, WIDTH, footerY - 1, alpha);
            int textY = footerY + 3;
            voteY = textY;
            boolean hover = minecraft.gui.screen() != null
                    && overVote(minecraft.mouseHandler.getScaledXPos(minecraft.getWindow()), minecraft.mouseHandler.getScaledYPos(minecraft.getWindow()));
            if (hover) graphics.fill(X - HudStyle.PANEL_PADDING + 1, textY - 2, X + WIDTH + HudStyle.PANEL_PADDING - 1, textY - 2 + VOTE_HEIGHT,
                    HudStyle.color(HudStyle.WHITE, alpha * 0.06F));
            Component label;
            int color;
            if (vote.waiting() >= 0) {
                label = Component.translatable("gui.whycantisleep.board.vote_waiting", vote.waiting());
                color = HudStyle.YELLOW;
            } else if (vote.mine()) {
                label = Component.translatable("gui.whycantisleep.board.voted");
                color = HudStyle.GREEN;
            } else {
                label = Component.translatable("gui.whycantisleep.board.vote");
                color = HudStyle.AQUA;
            }
            graphics.text(font, label, X, textY, HudStyle.color(color, alpha), false);
            Component tally = Component.translatable("gui.whycantisleep.board.vote_count", vote.votes(), vote.voters());
            graphics.text(font, tally, X + WIDTH - font.width(tally), textY, HudStyle.color(HudStyle.SILVER, alpha), false);
            footerY += VOTE_HEIGHT;
        }
        if (footer) {
            // The footer: how to nudge on the left, and on the right whether a nudge is ready or how long until it is.
            HudStyle.divider(graphics, X, WIDTH, footerY - 1, alpha);
            int textY = footerY + 4;
            graphics.text(font, Component.translatable("gui.whycantisleep.board.nudge_hint"), X, textY, HudStyle.color(HudStyle.MUTED, alpha), false);
            long cooldown = cooldown();
            Component state = cooldown > 0
                    ? Component.translatable("gui.whycantisleep.board.nudge_cooldown", Mth.ceil(cooldown / 20.0F))
                    : Component.translatable("gui.whycantisleep.board.nudge_ready");
            graphics.text(font, state, X + WIDTH - font.width(state), textY, HudStyle.color(HudStyle.FAINT, alpha), false);
        }
    }

    /**
     * A click on the in-bed screen ({@code ChatScreenMixin}): on the vote row it casts a vote to skip the night, and on an
     * awake player's row it asks the server to nudge them.
     */
    public static boolean onClick(double mouseX, double mouseY, int button) {
        if (button != 0 || !isShown()) return false;
        if (overVote(mouseX, mouseY)) {
            Network.sendToServer(new VotePayload(true));
            return true;
        }
        if (!WCISServerConfig.ALLOW_NUDGES.get()) return false;
        int row = rowAt(mouseX, mouseY);
        if (row < 0) return false;
        if (cooldown() <= 0) {
            nudgedAt = tick;
            Network.sendToServer(new NudgePayload(board.awake().get(row).id()));
        }
        return true;
    }
}
