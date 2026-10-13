package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: the rare "monster under the bed" scare, at the bed the player just failed to sleep in. */
public record ScarePayload(BlockPos bed) implements CustomPacketPayload {
    public static final Type<ScarePayload> TYPE = new Type<>(WhyCantISleep.id("scare"));

    @Override
    public Type<ScarePayload> type() {
        return TYPE;
    }
}
