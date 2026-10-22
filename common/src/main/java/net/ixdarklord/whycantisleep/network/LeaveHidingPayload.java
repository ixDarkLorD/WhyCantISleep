package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: the hiding player sneaked, to come out from under the bed. The flag is always true; it is there
 * because a payload needs something to carry.
 */
public record LeaveHidingPayload(boolean leave) implements CustomPacketPayload {
    public static final Type<LeaveHidingPayload> TYPE = new Type<>(WhyCantISleep.id("leave_hiding"));

    @Override
    public Type<LeaveHidingPayload> type() {
        return TYPE;
    }
}
