package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: a step of going under into the Lucid Deep, for the client to show.
 *
 * @param stage {@link #DRIFT} (they are slipping into deep sleep: the camera falls away from the bed, at the eye
 *              position and facing given), {@link #CANCEL} (they woke before going under) or {@link #LANDED} (they
 *              hit the arrival island face first)
 */
public record DreamTransitionPayload(int stage, double x, double y, double z, float yaw) implements CustomPacketPayload {
    public static final int DRIFT = 0;
    public static final int CANCEL = 1;
    public static final int LANDED = 2;
    public static final Type<DreamTransitionPayload> TYPE = new Type<>(WhyCantISleep.id("dream_transition"));

    @Override
    public Type<DreamTransitionPayload> type() {
        return TYPE;
    }
}
