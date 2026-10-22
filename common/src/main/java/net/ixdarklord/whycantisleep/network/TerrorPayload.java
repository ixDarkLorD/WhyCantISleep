package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: the player has a night terror as they fall asleep (start), or woke from one. */
public record TerrorPayload(boolean start) implements CustomPacketPayload {
    public static final Type<TerrorPayload> TYPE = new Type<>(WhyCantISleep.id("terror"));

    @Override
    public Type<TerrorPayload> type() {
        return TYPE;
    }
}
