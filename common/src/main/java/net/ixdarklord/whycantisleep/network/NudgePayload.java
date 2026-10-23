package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;

import java.util.UUID;

/** Client to server: the player, in bed, asks to nudge an awake player. The server decides whether it happens. */
public record NudgePayload(UUID target) implements CustomPacketPayload {
    public static final Type<NudgePayload> TYPE = new Type<>(WhyCantISleep.id("nudge"));

    @Override
    public Type<NudgePayload> type() {
        return TYPE;
    }
}
