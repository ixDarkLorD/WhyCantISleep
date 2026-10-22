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
public record SleepBoardPayload(int sleeping, int needed, List<Entry> awake, Vote vote) implements CustomPacketPayload {
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

    /**
     * The vote to skip the night, as the viewer sees it.
     *
     * @param open    whether a vote can be cast now
     * @param votes   how many in bed have voted
     * @param voters  how many are in bed
     * @param mine    whether the viewer has voted
     * @param waiting seconds left for the players still up to answer, or -1 before they are asked
     */
    public record Vote(boolean open, int votes, int voters, boolean mine, int waiting) {
    }
}
