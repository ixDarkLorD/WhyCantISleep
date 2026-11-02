package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;

/** Server to client: the player studied an item with a Dreamcatcher and found out what it does by a bed. */
public record StudiedPayload(Item item) implements CustomPacketPayload {
    public static final Type<StudiedPayload> TYPE = new Type<>(WhyCantISleep.id("studied"));

    @Override
    public Type<StudiedPayload> type() {
        return TYPE;
    }
}
