package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * Server to client: what is moving in a dreamer's room, every tick while anything is there, and how much daylight is in
 * it. Positions are from the spot they slept on.
 */
public record DreamRoomEntitiesPayload(List<Visitor> visitors, float daylight) implements CustomPacketPayload {
    public static final Type<DreamRoomEntitiesPayload> TYPE = new Type<>(WhyCantISleep.id("dream_room_entities"));

    /**
     * One thing in the room, other players among them.
     *
     * @param id      its network id, to keep telling it apart from update to update
     * @param type    its entity type
     * @param player  for a player, their UUID (so their own skin is drawn); empty for anything else
     * @param name    for a player, their name
     * @param yaw     which way it faces; {@code bodyYaw} its body, {@code headYaw} and {@code pitch} its head
     * @param pose    its pose's name: standing, crouching, swimming, asleep...
     * @param walk    how fast it is walking, to swing its legs and arms
     * @param held    what it holds in its main hand, an item id; empty for nothing
     */
    public record Visitor(int id, String type, String player, String name, float x, float y, float z, float yaw, float bodyYaw,
                          float headYaw, float pitch, String pose, float walk, String held) {
    }

    @Override
    public Type<DreamRoomEntitiesPayload> type() {
        return TYPE;
    }
}
