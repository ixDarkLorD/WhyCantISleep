package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;
import net.ixdarklord.whycantisleep.WhyCantISleep;

/**
 * Server to client: the local player's hide time changed while they hide (a distraction cut it short): {@code ticksLeft}
 * of it are left from now, so the card and the heartbeat minigame keep to the server's.
 */
public record HideTimePayload(int ticksLeft) implements CustomPacketPayload {
    public static final Type<HideTimePayload> TYPE = new Type<>(WhyCantISleep.id("hide_time"));

    @Override
    public Type<HideTimePayload> type() {
        return TYPE;
    }
}
