package net.ixdarklord.whycantisleep.compat;

import net.ixdarklord.whycantisleep.client.BedBookmarks;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.sleep.SleepDecor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.ArrayList;
import java.util.List;

/**
 * The lines the mod adds to what Jade and WTHIT show of the block looked at, on the client: for a bed, how the last
 * full night in it went and the name the player gave it (from {@link BedBookmarks}), whether it is the player's respawn
 * point, and that it hides a player; for a block that soothes or unsettles a sleeper near it, by how much.
 */
public final class LookedAtLines {
    private LookedAtLines() {
    }

    public static List<Component> lines(Level level, BlockPos pos, BlockState state) {
        List<Component> lines = new ArrayList<>();
        if (state.getBlock() instanceof BedBlock) {
            BlockPos head = state.getValue(BedBlock.PART) == BedPart.HEAD ? pos : pos.relative(state.getValue(BedBlock.FACING));
            GlobalPos bed = GlobalPos.of(level.dimension(), head.immutable());
            BedBookmarks.Bed kept = BedBookmarks.get(bed);
            if (kept != null && kept.name() != null && !kept.name().isEmpty()) {
                lines.add(Component.literal("“" + kept.name() + "”").withStyle(ChatFormatting.WHITE));
            }
            if (kept != null && kept.nights() > 0) {
                ChatFormatting color = kept.score() >= 65 ? ChatFormatting.GREEN : kept.score() >= 45 ? ChatFormatting.YELLOW : ChatFormatting.RED;
                lines.add(Component.translatable("gui.whycantisleep.looked_at.bed.last", Component.literal(String.valueOf(kept.score())).withStyle(color))
                        .withStyle(ChatFormatting.GRAY));
            }
            if (BedBookmarks.isRespawn(bed)) lines.add(Component.translatable("gui.whycantisleep.looked_at.bed.respawn").withStyle(ChatFormatting.GOLD));
            if (WCISServerConfig.ALLOW_HIDING.get() && WCISServerConfig.HIDE_UNDER_BEDS.get()) {
                lines.add(Component.translatable("gui.whycantisleep.looked_at.bed.hide").withStyle(ChatFormatting.DARK_GRAY));
            }
            return lines;
        }
        int points = SleepDecor.points(SleepDecor.item(state));
        if (points > 0) lines.add(Component.translatable("gui.whycantisleep.looked_at.soothing", points).withStyle(ChatFormatting.GREEN));
        else if (points < 0) lines.add(Component.translatable("gui.whycantisleep.looked_at.unsettling", -points).withStyle(ChatFormatting.RED));
        return lines;
    }
}
