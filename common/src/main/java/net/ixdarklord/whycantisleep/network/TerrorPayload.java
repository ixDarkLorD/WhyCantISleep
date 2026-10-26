package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;

/**
 * Server to client: the player has a night terror as they fall asleep (start), or woke from one; how bad it is, 0 to
 * 1, from how badly the night was going to go ({@code SleepNights}); and whether it is had asleep, in bed (laid out
 * along the bed once the client sees them in it) or awake (ahead of where they look).
 */
public record TerrorPayload(boolean start, float intensity, boolean asleep) implements CustomPacketPayload {
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
