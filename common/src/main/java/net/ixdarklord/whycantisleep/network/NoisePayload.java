package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the noise the hiding player makes, 0 to 1, in steps of a tenth, sent when it changes. */
public record NoisePayload(float noise) implements CustomPacketPayload {
    public static final Type<NoisePayload> TYPE = new Type<>(WhyCantISleep.id("noise"));

    @Override
    public Type<NoisePayload> type() {
        return TYPE;
    }
}
