package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.sleep.AwakeReason;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.UUID;

/**
 * Server to a player in bed, once a second: how many players of their dimension are asleep, how many the
 * {@code playersSleepingPercentage} rule needs, and who is still awake.
 */
public record SleepBoardPayload(int sleeping, int needed, List<Entry> awake) implements CustomPacketPayload {
    public static final Type<SleepBoardPayload> TYPE = new Type<>(WhyCantISleep.id("sleep_board"));

    @Override
    public Type<SleepBoardPayload> type() {
        return TYPE;
    }

    /**
     * An awake player.
     *
     * @param counted whether they count toward the rule: only players of the sleeper's dimension do
     */
    public record Entry(UUID id, String name, AwakeReason reason, boolean counted) {
    }
}
