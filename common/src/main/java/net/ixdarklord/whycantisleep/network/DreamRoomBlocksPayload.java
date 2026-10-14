package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the room a dreamer fell asleep in, for their Window. Every block of a box round the spot they
 * slept on ({@code DreamRoom.RADIUS} out to the sides, {@code DreamRoom.BELOW} down and {@code DreamRoom.ABOVE} up),
 * as block state ids, x fastest, then z, then y. Sent when the dream begins and again whenever the room changes.
 *
 * @param facing which way the bed points, from its foot to its head, as a yaw: the Window looks down on the sleeper
 *               from the foot of the bed
 */
public record DreamRoomBlocksPayload(int[] states, float facing) implements CustomPacketPayload {
    public static final Type<DreamRoomBlocksPayload> TYPE = new Type<>(WhyCantISleep.id("dream_room_blocks"));

    @Override
    public Type<DreamRoomBlocksPayload> type() {
        return TYPE;
    }
}
