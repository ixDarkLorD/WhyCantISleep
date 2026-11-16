package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the player, asleep, sinks into a deep sleep, lasting {@code ticks}: the night waits for it to end
 * before it passes. The client shuts the eyes and hushes the world ({@code client/DeepSleepClient}).
 */
public record DeepSleepPayload(int ticks) implements CustomPacketPayload {
    public static final Type<DeepSleepPayload> TYPE = new Type<>(WhyCantISleep.id("deep_sleep"));
    /** How long a deep sleep lasts, in ticks: a little past the game's own time in bed, so its slow heartbeats are heard. */
    public static final int TICKS = 20 * 8;

    @Override
    public Type<DeepSleepPayload> type() {
        return TYPE;
    }
}
