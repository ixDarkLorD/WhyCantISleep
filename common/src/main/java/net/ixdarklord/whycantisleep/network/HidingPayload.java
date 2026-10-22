package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.api.HidingPosture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client: a player, by network id, went into a hiding spot or came out. Sent to everyone in their level, so
 * they are drawn as they hide. With it come how they are placed (posture, the way they face, the view's height), how
 * far their eyes close, how long they must stay hidden, whether something was hunting them as they went in, and for their own card what they hide in and how to say where.
 */
public record HidingPayload(int player, boolean hiding, BlockPos anchor, HidingPosture posture, float yaw, float eyeHeight, float squint, int hideTicks,
                            boolean hunted, ItemStack icon, Component where) implements CustomPacketPayload {
    public static final Type<HidingPayload> TYPE = new Type<>(WhyCantISleep.id("hiding"));

    /** A player who came out. */
    public static HidingPayload out(int player, BlockPos anchor) {
        return new HidingPayload(player, false, anchor, HidingPosture.LYING, 0.0F, -1.0F, 0.0F, 0, false, ItemStack.EMPTY, Component.empty());
    }

    @Override
    public Type<HidingPayload> type() {
        return TYPE;
    }
}
