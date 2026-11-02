package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ItemTooltipEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.InteractionEvents;
import net.ixdarklord.whycantisleep.client.hud.SleepNotice;
import net.ixdarklord.whycantisleep.client.hud.SleepNotices;
import net.ixdarklord.whycantisleep.compat.WCISItems;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.network.BedCheckPayload;
import net.ixdarklord.whycantisleep.network.WokePayload;
import net.ixdarklord.whycantisleep.sleep.BedTools;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The bed check, on the client. Sneaking and using a bed with a tool to read it by in hand ({@link BedTools#checksByClick}:
 * a Dreamcatcher for how a night in it would go, a Bedside Lantern for where a monster could spawn), the player asks the
 * server: the server answers its own side of the use, and here it only stops the client predicting anything else. The
 * answer ({@link BedCheckPayload}) comes up as a card like the morning's, the score a forecast set against the last night
 * slept in that bed ({@link BedBookmarks}), with the places in the room a monster could spawn marked for a while
 * ({@link DarkSpots}). For a few seconds after, the server answers again by itself as the room is changed.
 */
public final class BedCheckClient {
    private static final int CARD_TICKS = 20 * 6;

    // An answer this soon after asking is the one asked for; later, it is the server following the room as it changes.
    private static final int ASKED_TICKS = 10;

    private static long tick;
    private static long askedAt = Long.MIN_VALUE / 2;

    private BedCheckClient() {
    }

    public static void init() {
        ClientTickEvents.END.register(minecraft -> {
            if (!minecraft.isPaused()) tick++;
        });
        // The tools say what they're for.
        ItemTooltipEvents.APPEND.register((stack, player, flag, lines) -> {
            String key = stack.is(WCISItems.DREAMCATCHER.get()) ? "dreamcatcher" : stack.is(WCISItems.BEDSIDE_LANTERN.get()) ? "bedside_lantern" : null;
            if (key == null) return;
            lines.add(Component.translatable("item.whycantisleep." + key + ".tooltip").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("item.whycantisleep.bed_tool.how").withStyle(ChatFormatting.DARK_GRAY));
            if (key.equals("dreamcatcher")) lines.add(Component.translatable("item.whycantisleep.dreamcatcher.learn").withStyle(ChatFormatting.DARK_GRAY));
        });
        InteractionEvents.USE_BLOCK.register((player, hand, hit) -> {
            if (!player.level().isClientSide() || hand != InteractionHand.MAIN_HAND || !SleepStatus.serverHasMod() || !WCISServerConfig.BED_CHECK.get()) {
                return InteractionResult.PASS;
            }
            if (!(player.level().getBlockState(hit.getBlockPos()).getBlock() instanceof BedBlock) || !BedTools.checksByClick(player)) {
                return InteractionResult.PASS;
            }
            // The answer is the one asked for, so it may take the place of another card.
            askedAt = tick;
            return InteractionResult.SUCCESS;
        });
    }

    /** The server's answer for a bed: a forecast with a Dreamcatcher, the dark spots with a Bedside Lantern. */
    public static void onCheck(BedCheckPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !WCISConfigs.isEnabled(WCISClientConfig.BED_CHECK)) return;
        GlobalPos bed = GlobalPos.of(minecraft.level.dimension(), payload.bed());
        BedBookmarks.Bed kept = BedBookmarks.get(bed);
        String name = kept != null ? kept.name() : null;
        SleepNotice notice;
        if (payload.forecast()) {
            List<SleepNotice.Detail> details = new ArrayList<>();
            for (WokePayload.Line line : payload.lines()) {
                if (line.points() != 0) details.add(new SleepNotice.Detail(line.label(), line.points()));
            }
            details.sort(Comparator.comparingInt(detail -> -Math.abs(detail.points())));
            Component word = Component.translatable("gui.whycantisleep.woke.word." + SleepMornings.word(payload.score()));
            Component title = name != null && !name.isEmpty() ? Component.translatable("gui.whycantisleep.bed_check.title.named", word, name)
                    : Component.translatable("gui.whycantisleep.bed_check.title", word);
            notice = new SleepNotice(SleepNotice.Kind.BED_CHECK, title, hint(payload))
                    .withQuality(new SleepNotice.Quality(payload.score(), details, null, true, label(kept, payload.score())))
                    .withIcon(new ItemStack(minecraft.level.getBlockState(payload.bed()).getBlock().asItem()));
        } else {
            // Only the lantern: what it shows of the room, on a card of one or two lines.
            Component title = !payload.enclosed() ? Component.translatable("gui.whycantisleep.bed_check.lantern.open")
                    : payload.darkSpots().isEmpty() ? Component.translatable("gui.whycantisleep.bed_check.lantern.lit")
                    : Component.translatable("gui.whycantisleep.bed_check.lantern.dark", payload.darkSpots().size());
            notice = new SleepNotice(SleepNotice.Kind.BED_CHECK, title, Component.translatable("gui.whycantisleep.bed_check.lantern.hint"))
                    .withIcon(new ItemStack(WCISItems.BEDSIDE_LANTERN.get()));
        }
        if (payload.spotsShown()) DarkSpots.show(payload.darkSpots());
        // Still showing a bed's card: changed in place, without the card's sound again. Another card up (a refused sleep,
        // hiding) isn't pushed aside by the server following the room, only by a check the player just asked for.
        SleepNotice.Kind shown = SleepNotices.shownKind();
        if (shown == SleepNotice.Kind.BED_CHECK) SleepNotices.refresh(notice, CARD_TICKS);
        else if (shown == null || tick - askedAt <= ASKED_TICKS) SleepNotices.show(notice, CARD_TICKS);
    }

    // Under the title: the last full night slept in this bed, and how far the forecast is from it.
    private static @Nullable Component label(@Nullable BedBookmarks.Bed kept, int score) {
        if (kept == null || kept.nights() <= 0) return null;
        int change = score - kept.score();
        Component delta = Component.literal(change == 0 ? "±0" : (change > 0 ? "+" : "−") + Math.abs(change))
                .withStyle(change > 0 ? ChatFormatting.GREEN : change < 0 ? ChatFormatting.RED : ChatFormatting.GRAY);
        return Component.translatable("gui.whycantisleep.bed_check.label.last", kept.score(), delta);
    }

    // What to know besides the score: a night terror waiting, else where to put a light, else that the room is open.
    private static @Nullable Component hint(BedCheckPayload payload) {
        if (payload.terrorChance() > 0) return Component.translatable("gui.whycantisleep.bed_check.terror", payload.terrorChance());
        if (payload.spotsShown() && !payload.darkSpots().isEmpty() && WCISConfigs.isEnabled(WCISClientConfig.DARK_SPOTS)) {
            return Component.translatable("gui.whycantisleep.bed_check.dark", payload.darkSpots().size());
        }
        if (!payload.enclosed()) return Component.translatable("gui.whycantisleep.bed_check.open");
        // Unlit corners and no lantern to show them: say what would.
        if (!payload.spotsShown() && payload.lines().stream().anyMatch(line -> line.points() < 0 && "gui.whycantisleep.quality.dark_corners".equals(key(line.label())))) {
            return Component.translatable("gui.whycantisleep.bed_check.need_lantern");
        }
        return null;
    }

    private static @Nullable String key(Component label) {
        return label.getContents() instanceof TranslatableContents contents ? contents.getKey() : null;
    }
}
