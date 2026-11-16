package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: someone sharing the player's night terror got out of bed and shook them awake: the terror jumps to
 * its jolt and ends.
 */
public record ShakenAwakePayload(boolean shaken) implements CustomPacketPayload {
    public static final Type<ShakenAwakePayload> TYPE = new Type<>(WhyCantISleep.id("shaken_awake"));

    @Override
    public Type<ShakenAwakePayload> type() {
        return TYPE;
    }
}
