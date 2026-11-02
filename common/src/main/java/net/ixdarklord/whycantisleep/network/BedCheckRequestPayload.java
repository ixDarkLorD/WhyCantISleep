package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the player, crouched by a bed and looking at it, asks how a night in it would go. */
public record BedCheckRequestPayload(BlockPos bed) implements CustomPacketPayload {
    public static final Type<BedCheckRequestPayload> TYPE = new Type<>(WhyCantISleep.id("bed_check_request"));

    @Override
    public Type<BedCheckRequestPayload> type() {
        return TYPE;
    }
}
