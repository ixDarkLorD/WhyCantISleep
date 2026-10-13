package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.client.hud.BlockerInfo;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.network.SleepBlockedPayload;
import net.ixdarklord.whycantisleep.sleep.SleepChecks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
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
 * ({@link #onOverlayMessage}).
 */
public final class SleepClient {
    private static final String NOT_SAFE = "block.minecraft.bed.not_safe";
    // A server with the mod sends its report just before the game's message, so a report this recent covers it.
    private static final int REPORT_TICKS = 20;

    private static long tick;
    private static long lastReport = Long.MIN_VALUE;
    private static @Nullable BlockPos lastBed;

    private SleepClient() {
    }

    public static void init() {
        ClientTickEvents.END.register(minecraft -> tick++);
        ClientPlayerEvents.LEAVE.register(player -> {
            lastBed = null;
            lastReport = Long.MIN_VALUE;
        });
    }

    /** The player used a block ({@code MultiPlayerGameModeMixin}): remembers the bed, by its head as the game does. */
    public static void onUseBlock(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        BlockState state = minecraft.level.getBlockState(pos);
        if (!(state.getBlock() instanceof BedBlock)) return;
        lastBed = state.getValue(BedBlock.PART) == BedPart.HEAD ? pos.immutable() : pos.relative(state.getValue(BedBlock.FACING));
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
     * A message is about to be put above the hotbar ({@code GuiMixin}): the game's "monsters nearby" one means a failed
     * sleep. Returns the message to show in its place.
     */
    public static Component onOverlayMessage(Component message) {
        if (!(message.getContents() instanceof TranslatableContents contents) || !contents.getKey().equals(NOT_SAFE)) return message;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return message;
        // No recent report: the server doesn't have the mod, so look for the monsters from here.
        if (tick - lastReport > REPORT_TICKS && lastBed != null) {
            Blockers.set(SleepChecks.blockersOnClient(minecraft.level, minecraft.player, lastBed));
        }
        if (!WCISClientConfig.ACTION_BAR_INFO.get()) return message;
        Component info = BlockerInfo.describe(minecraft.player, Blockers.entities());
        return info != null ? info : message;
    }
}