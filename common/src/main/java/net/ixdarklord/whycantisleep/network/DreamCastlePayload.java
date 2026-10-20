package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: where the Lord's castle stands in the Dark Between, so the dreamer's client can draw the storm over it
 * ({@code CastleVortex}) from as far away as they can see it.
 *
 * @param known whether there is a castle at all (a world without the structure has none)
 * @param x     the top of the castle's roof dais, where its beam rises from
 */
public record DreamCastlePayload(boolean known, double x, double y, double z) implements CustomPacketPayload {
    public static final Type<DreamCastlePayload> TYPE = new Type<>(WhyCantISleep.id("dream_castle"));

    @Override
    public Type<DreamCastlePayload> type() {
        return TYPE;
    }
}
