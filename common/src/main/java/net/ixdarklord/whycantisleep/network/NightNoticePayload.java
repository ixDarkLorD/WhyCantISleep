package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: something of the night to show on a card, with a number for it: a nap slept out or cut short
 * ({@code value}, the days of sleep debt left), or waking somewhere else (sleepwalking).
 */
public record NightNoticePayload(Kind kind, float value) implements CustomPacketPayload {
    public static final Type<NightNoticePayload> TYPE = new Type<>(WhyCantISleep.id("night_notice"));

    public enum Kind {
        NAPPED, NAP_CUT, SLEEPWALKED
    }

    @Override
    public Type<NightNoticePayload> type() {
        return TYPE;
    }
}
