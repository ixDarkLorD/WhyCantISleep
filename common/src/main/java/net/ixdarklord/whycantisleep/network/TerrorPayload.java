package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the player has a night terror as they fall asleep (start), or woke from one; and how bad it is, 0 to
 * 1, from how badly the night was going to go ({@code SleepNights}).
 */
public record TerrorPayload(boolean start, float intensity) implements CustomPacketPayload {
    public static final Type<TerrorPayload> TYPE = new Type<>(WhyCantISleep.id("terror"));

    /** How long a terror this bad lasts, in ticks: the night waits for it to end before it passes. */
    public static int ticks(float intensity) {
        return 140 + Math.round(100.0F * intensity);
    }

    /** How long this one lasts, in ticks. */
    public int ticks() {
        return ticks(this.intensity);
    }

    @Override
    public Type<TerrorPayload> type() {
        return TYPE;
    }
}
