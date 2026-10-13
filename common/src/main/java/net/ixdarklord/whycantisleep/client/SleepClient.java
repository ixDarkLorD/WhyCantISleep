package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.client.fx.SleepEffects;
import net.ixdarklord.whycantisleep.client.hud.BlockerInfo;
import net.ixdarklord.whycantisleep.client.hud.Explanations;
import net.ixdarklord.whycantisleep.client.hud.SleepNotice;
import net.ixdarklord.whycantisleep.client.hud.SleepNotices;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.lurker.client.LurkerHaunt;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.network.SleepBlockedPayload;
import net.ixdarklord.whycantisleep.sleep.SleepChecks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The client's side of a failed sleep. A server with the mod reports the exact monsters ({@link #onBlockedByServer});
 * on a server without it the client notices the game's own message and repeats the check with the monsters it can see
 * ({@link #onOverlayMessage}). Either way the game's line above the hotbar gives way to the sleep HUD's card. It also
 * stops the first use of a bed that would explode ({@link #onUseBlock}).
 */
public final class SleepClient {
    // A server with the mod sends its report just before the game's message, so a report this recent covers it.
    private static final int REPORT_TICKS = 20;
    // How long after the warning a second use of the bed goes through.
    private static final int WARNING_TICKS = 60;
    private static final String NUDGE_KEYS = "gui.whycantisleep.nudge.";

    private static long tick;
    private static long lastReport = Long.MIN_VALUE;
    private static @Nullable BlockPos lastBed;
    private static @Nullable BlockPos warnedBed;
    private static long warnedAt;
    // Whether the use key was let go since the warning: holding it down must not count as the second use.
    private static boolean releasedSinceWarning;

    private SleepClient() {
    }

    public static void init() {
        ClientTickEvents.END.register(minecraft -> {
            tick++;
            if (warnedBed != null && !minecraft.options.keyUse.isDown()) releasedSinceWarning = true;
        });
        ClientPlayerEvents.LEAVE.register(player -> {
            lastBed = null;
            warnedBed = null;
            lastReport = Long.MIN_VALUE;
        });
    }

    /**
     * The player is about to use a block ({@code MultiPlayerGameModeMixin}). Remembers a bed, by its head as the game
     * does, and returns true when the use must not happen: the first use of a bed that would explode only warns.
     */
    public static boolean onUseBlock(LocalPlayer player, BlockPos pos) {
        BlockState state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof BedBlock)) return false;
        BlockPos bed = state.getValue(BedBlock.PART) == BedPart.HEAD ? pos.immutable() : pos.relative(state.getValue(BedBlock.FACING));
        lastBed = bed;
        LurkerHaunt.onBedUsed(bed);

        if (!WCISClientConfig.DIMENSION_WARNING.get()) return false;
        // Sneaking with an item in hand uses the item, not the bed.
        if (player.isSecondaryUseActive() && !(player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty())) return false;
        if (!player.level().environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, bed).explodes()) return false;

        if (bed.equals(warnedBed) && tick - warnedAt <= WARNING_TICKS) {
            // The other hand's turn in the same click is stopped like the first.
            if (tick == warnedAt || !releasedSinceWarning) return true;
            warnedBed = null;
            return false;
        }
        warnedBed = bed;
        warnedAt = tick;
        releasedSinceWarning = false;
        SleepNotices.show(Explanations.notice(SleepNotice.Kind.DANGER, "explodes"), WARNING_TICKS);
        return true;
    }

    /** The server's report: these monsters blocked the sleep. */
    public static void onBlockedByServer(SleepBlockedPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        lastReport = tick;
        List<Entity> blockers = new ArrayList<>();
        for (int id : payload.blockers()) {
            Entity entity = minecraft.level.getEntity(id);
            if (entity != null) blockers.add(entity);
        }
        Blockers.set(blockers);
    }

    /**
     * A message is about to be put above the hotbar ({@code GuiMixin}). One of the game's sleep messages is a failed
     * sleep: "monsters nearby" also points the monsters out. Returns true when the sleep HUD shows it instead.
     */
    public static boolean onOverlayMessage(Component message) {
        if (!(message.getContents() instanceof TranslatableContents contents)) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return false;
        String key = contents.getKey();
        // The server's answers to a nudge are the mod's own, and always belong on its HUD.
        if (key.startsWith(NUDGE_KEYS)) {
            SleepNotices.show(new SleepNotice(SleepNotice.Kind.NUDGE, message, null));
            return true;
        }

        SleepNotice notice = null;
        int ticks = SleepNotices.DEFAULT_TICKS;
        if (key.equals(Explanations.NOT_SAFE)) {
            // No recent report: the server doesn't have the mod, so look for the monsters from here.
            if (tick - lastReport > REPORT_TICKS && lastBed != null) {
                Blockers.set(SleepChecks.blockersOnClient(minecraft.level, minecraft.player, lastBed));
            }
            if (WCISClientConfig.ACTION_BAR_INFO.get()) notice = BlockerInfo.describe(minecraft.player, Blockers.entities());
            ticks = Math.max(ticks, WCISServerConfig.HIGHLIGHT_DURATION.get() * 20);
        }
        if (notice == null && WCISClientConfig.SLEEP_EXPLANATIONS.get()) notice = Explanations.explain(contents);
        if (notice == null) return false;
        SleepNotices.show(notice, ticks);
        // On a server without the mod nobody rolls for the scare, so the client does, by its own server settings.
        if (lastBed != null && !minecraft.hasSingleplayerServer() && !WCISServerConfig.CONFIG.isRemote() && notice.kind() != SleepNotice.Kind.PLAYERS_AWAKE
                && minecraft.level.getRandom().nextInt(100) < WCISServerConfig.SCARE_CHANCE.get()) {
            SleepEffects.scare(lastBed);
        }
        return true;
    }
}
