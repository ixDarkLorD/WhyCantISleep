package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Server to client: what is moving in a dreamer's room, a few times a second, and how much daylight is in it. Positions
 * are from the spot they slept on.
 */
public record DreamRoomEntitiesPayload(List<Visitor> visitors, float daylight) implements CustomPacketPayload {
    public static final Type<DreamRoomEntitiesPayload> TYPE = new Type<>(WhyCantISleep.id("dream_room_entities"));

    /** One thing in the room: which it is (its network id, to keep telling it apart), its type, where and facing which way. */
    public record Visitor(int id, String type, float x, float y, float z, float yaw) {
    }

    @Override
    public Type<DreamRoomEntitiesPayload> type() {
        return TYPE;
    }
}
