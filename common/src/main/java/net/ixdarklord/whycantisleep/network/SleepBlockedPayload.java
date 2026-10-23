package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.core.BlockPos;
import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;

import java.util.List;

/**
 * Server to client: the player's sleep failed because of monsters. Carries the bed and the network ids of exactly the
 * monsters the game's check found.
 */
public record SleepBlockedPayload(BlockPos bed, List<Integer> blockers) implements CustomPacketPayload {
    public static final Type<SleepBlockedPayload> TYPE = new Type<>(WhyCantISleep.id("sleep_blocked"));

    @Override
    public Type<SleepBlockedPayload> type() {
        return TYPE;
    }
}
