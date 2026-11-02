package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.whycantisleep.client.hud.SleepNotice;
import net.ixdarklord.whycantisleep.client.hud.SleepNotices;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.hiding.Hiding;
import net.ixdarklord.whycantisleep.network.BedCheckPayload;
import net.ixdarklord.whycantisleep.network.BedCheckRequestPayload;
import net.ixdarklord.whycantisleep.network.WokePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The bed check, on the client. Crouched by a bed and looking at it for a moment, the player asks the server how a
 * night in it would go ({@link BedCheckRequestPayload}); its answer ({@link BedCheckPayload}) comes up as a card like
 * the morning's, the score a forecast, with the places in the room a monster could spawn marked for a while
 * ({@link DarkSpots}). Kept looking, it asks again now and then, as the room may have changed.
 */
public final class BedCheckClient {
    // Looking this long asks; still looking, it asks again this often. The card stays a little longer than that.
    private static final int HOLD_TICKS = 15;
    private static final int AGAIN_TICKS = 20 * 5;
    private static final int CARD_TICKS = 20 * 6;

    private static @Nullable BlockPos looking;
    private static int held;

    private BedCheckClient() {
    }

    public static void init() {
        ClientTickEvents.END.register(BedCheckClient::tick);
        ClientPlayerEvents.LEAVE.register(player -> {
            looking = null;
            held = 0;
        });
    }

    private static void tick(Minecraft minecraft) {
        if (minecraft.isPaused()) return;
        BlockPos bed = target(minecraft);
        if (bed == null) {
            looking = null;
            held = 0;
            return;
        }
        if (!bed.equals(looking)) {
            looking = bed;
            held = 0;
        }
        held++;
        if (held >= HOLD_TICKS && (held - HOLD_TICKS) % AGAIN_TICKS == 0) Network.sendToServer(new BedCheckRequestPayload(bed));
    }

    // The head of the bed the player is crouched by and looking at, or null.
    private static @Nullable BlockPos target(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null || !SleepStatus.serverHasMod()) return null;
        if (!WCISConfigs.isEnabled(WCISClientConfig.BED_CHECK)) return null;
        // Crouching as hiding reads it: the sneak key held, whatever pose the player is in.
        if (!player.isShiftKeyDown() || player.isSleeping() || Hiding.isHiding(player)) return null;
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        BlockState state = minecraft.level.getBlockState(hit.getBlockPos());
        if (!(state.getBlock() instanceof BedBlock)) return null;
        return state.getValue(BedBlock.PART) == BedPart.HEAD ? hit.getBlockPos().immutable() : hit.getBlockPos().relative(state.getValue(BedBlock.FACING));
    }

    /** The server's forecast for a bed. */
    public static void onCheck(BedCheckPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !WCISConfigs.isEnabled(WCISClientConfig.BED_CHECK)) return;
        // Still showing the same bed's card: changed in place, without the card's sound again.
        boolean again = SleepNotices.shownKind() == SleepNotice.Kind.BED_CHECK;
        List<SleepNotice.Detail> details = new ArrayList<>();
        for (WokePayload.Line line : payload.lines()) {
            if (line.points() != 0) details.add(new SleepNotice.Detail(line.label(), line.points()));
        }
        details.sort(Comparator.comparingInt(detail -> -Math.abs(detail.points())));
        Component word = Component.translatable("gui.whycantisleep.woke.word." + SleepMornings.word(payload.score()));
        SleepNotice notice = new SleepNotice(SleepNotice.Kind.BED_CHECK, Component.translatable("gui.whycantisleep.bed_check.title", word), hint(payload))
                .withQuality(new SleepNotice.Quality(payload.score(), details, null, true))
                .withIcon(new ItemStack(minecraft.level.getBlockState(payload.bed()).getBlock().asItem()));
        if (again) SleepNotices.showQuietly(notice, CARD_TICKS);
        else SleepNotices.show(notice, CARD_TICKS);
        DarkSpots.show(payload.darkSpots());
    }

    // What to know besides the score: a night terror waiting, else where to put a light, else that the room is open.
    private static @Nullable Component hint(BedCheckPayload payload) {
        if (payload.terrorChance() > 0) return Component.translatable("gui.whycantisleep.bed_check.terror", payload.terrorChance());
        if (!payload.darkSpots().isEmpty() && WCISConfigs.isEnabled(WCISClientConfig.DARK_SPOTS)) {
            return Component.translatable("gui.whycantisleep.bed_check.dark", payload.darkSpots().size());
        }
        if (!payload.enclosed()) return Component.translatable("gui.whycantisleep.bed_check.open");
        return null;
    }
}
