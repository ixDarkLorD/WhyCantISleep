package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Server to client: what the player may read of a bed with their tools. With a Dreamcatcher ({@code forecast}), the
 * score a night in it would have (0 to 100) and what makes it so, and the chance in percent of a night terror on lying
 * down; with a Bedside Lantern ({@code spotsShown}), the places in its room a monster could spawn (only for a closed
 * room). And whether the room is closed.
 */
public record BedCheckPayload(BlockPos bed, boolean forecast, int score, List<WokePayload.Line> lines, List<BlockPos> darkSpots, boolean spotsShown,
                              boolean enclosed, int terrorChance) implements CustomPacketPayload {
    public static final Type<BedCheckPayload> TYPE = new Type<>(WhyCantISleep.id("bed_check"));

    @Override
    public Type<BedCheckPayload> type() {
        return TYPE;
    }
}
