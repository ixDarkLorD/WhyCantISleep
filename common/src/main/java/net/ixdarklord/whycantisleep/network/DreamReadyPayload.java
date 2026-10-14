package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: a dreamer's client has the Deep loaded and its screen showing, so the camera is in place to watch
 * them fall. Until it says so, they hang where they came out of the sky.
 */
public record DreamReadyPayload(boolean ready) implements CustomPacketPayload {
    public static final Type<DreamReadyPayload> TYPE = new Type<>(WhyCantISleep.id("dream_ready"));

    @Override
    public Type<DreamReadyPayload> type() {
        return TYPE;
    }
}
