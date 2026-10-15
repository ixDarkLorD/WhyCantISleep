package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: the Garden's ward held the block the player struck, on that face (GardenWard). */
public record WardPayload(BlockPos pos, Direction face) implements CustomPacketPayload {
    public static final Type<WardPayload> TYPE = new Type<>(WhyCantISleep.id("ward"));

    @Override
    public Type<WardPayload> type() {
        return TYPE;
    }
}
