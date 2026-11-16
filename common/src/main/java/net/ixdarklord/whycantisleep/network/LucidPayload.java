package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the player, asleep, has a lucid dream, lasting {@code ticks}: the night waits for it to end before it
 * passes. The client shows the dream, a pale copy of the room to drift through ({@code client/LucidDreamClient}).
 */
public record LucidPayload(int ticks) implements CustomPacketPayload {
    public static final Type<LucidPayload> TYPE = new Type<>(WhyCantISleep.id("lucid"));
    /** How long a lucid dream lasts, in ticks. */
    public static final int TICKS = 20 * 15;

    @Override
    public Type<LucidPayload> type() {
        return TYPE;
    }
}
