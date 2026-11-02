package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: a reminder to sleep. At dusk, for a player who has gone {@code days} without lying down; or, at
 * night, that {@code sleeping} of the {@code needed} sleepers are in bed and waiting.
 */
public record ReminderPayload(Kind kind, int days, int sleeping, int needed) implements CustomPacketPayload {
    public static final Type<ReminderPayload> TYPE = new Type<>(WhyCantISleep.id("reminder"));

    @Override
    public Type<ReminderPayload> type() {
        return TYPE;
    }

    public enum Kind {
        DUSK, OTHERS_IN_BED
    }
}
