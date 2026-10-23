package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;

/** Client to server: the player, in bed, votes from the sleep board to skip the night. */
public record VotePayload(boolean skip) implements CustomPacketPayload {
    public static final Type<VotePayload> TYPE = new Type<>(WhyCantISleep.id("vote"));

    @Override
    public Type<VotePayload> type() {
        return TYPE;
    }
}
