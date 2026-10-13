package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server, on joining and whenever the option changes: whether the player wants scary content. The server
 * builds no dread for, and sends no creature after, a player who doesn't.
 */
public record ScaryPreferencePayload(boolean scary) implements CustomPacketPayload {
    public static final Type<ScaryPreferencePayload> TYPE = new Type<>(WhyCantISleep.id("scary_preference"));

    @Override
    public Type<ScaryPreferencePayload> type() {
        return TYPE;
    }
}
