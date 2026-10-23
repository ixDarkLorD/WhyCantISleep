package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;

/** Server to client: the player cleared the room of the monsters that blocked their sleep, and is lying down if so. */
public record RoomSafePayload(boolean lyingDown) implements CustomPacketPayload {
    public static final Type<RoomSafePayload> TYPE = new Type<>(WhyCantISleep.id("room_safe"));

    @Override
    public Type<RoomSafePayload> type() {
        return TYPE;
    }
}
