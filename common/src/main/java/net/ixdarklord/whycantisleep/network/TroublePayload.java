package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;

/** Server to client: how long the player has gone without sleep, and their refused sleeps in a row, for the moon of sleep debt. */
public record TroublePayload(int ticksSinceRest, int failedAttempts) implements CustomPacketPayload {
    public static final Type<TroublePayload> TYPE = new Type<>(WhyCantISleep.id("trouble"));

    @Override
    public Type<TroublePayload> type() {
        return TYPE;
    }
}
