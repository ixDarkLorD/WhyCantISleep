package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: a player tried to hide too soon after coming out of a spot. {@code ticksLeft} until they may hide
 * again, of the server's whole cooldown, {@code totalTicks}, for the card that counts it down.
 */
public record HideCooldownPayload(int ticksLeft, int totalTicks) implements CustomPacketPayload {
    public static final Type<HideCooldownPayload> TYPE = new Type<>(WhyCantISleep.id("hide_cooldown"));

    @Override
    public Type<HideCooldownPayload> type() {
        return TYPE;
    }
}
