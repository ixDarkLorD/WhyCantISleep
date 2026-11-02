package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Server to client: how a night in a bed would go, slept through from now. Its forecast score (0 to 100) and what
 * makes it so, the places in its room a monster could spawn (only for a closed room), whether the room is closed, and
 * the chance in percent of a night terror on lying down.
 */
public record BedCheckPayload(BlockPos bed, int score, List<WokePayload.Line> lines, List<BlockPos> darkSpots, boolean enclosed, int terrorChance)
        implements CustomPacketPayload {
    public static final Type<BedCheckPayload> TYPE = new Type<>(WhyCantISleep.id("bed_check"));

    @Override
    public Type<BedCheckPayload> type() {
        return TYPE;
    }
}
