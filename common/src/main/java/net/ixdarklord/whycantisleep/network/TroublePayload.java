package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.core.GlobalPos;

import java.util.Optional;

/**
 * Server to client: how long the player has gone without sleep, and their refused sleeps in a row, for the moon of
 * sleep debt; and where they respawn, if they have set it (a bed's head, mostly), for the beds they keep.
 */
public record TroublePayload(int ticksSinceRest, int failedAttempts, Optional<GlobalPos> respawn) implements CustomPacketPayload {
    public static final Type<TroublePayload> TYPE = new Type<>(WhyCantISleep.id("trouble"));

    @Override
    public Type<TroublePayload> type() {
        return TYPE;
    }
}
