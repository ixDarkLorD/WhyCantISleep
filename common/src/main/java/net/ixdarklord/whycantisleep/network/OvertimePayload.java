package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;

/**
 * Client to server, from a player hiding hunted, at the end of the hide time: whether the heartbeat minigame wants a
 * beat more ({@code more}: they missed the last beat, so it goes into overtime) or is over (they held, or caught a beat
 * in overtime). The server grants at most {@link net.ixdarklord.whycantisleep.hiding.Hiding#MAX_OVERTIME} beats.
 */
public record OvertimePayload(boolean more) implements CustomPacketPayload {
    public static final Type<OvertimePayload> TYPE = new Type<>(WhyCantISleep.id("overtime"));

    @Override
    public Type<OvertimePayload> type() {
        return TYPE;
    }
}
