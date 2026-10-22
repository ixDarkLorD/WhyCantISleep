package net.ixdarklord.whycantisleep.network;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.api.SleepEffect;
import net.ixdarklord.whycantisleep.api.WakeReason;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.Optional;

/**
 * Server to client: the player woke up. Why, how well they slept (0 to 100) and what made it so, and whether they
 * woke shaken from a night terror or well rested, and the sleep effect they woke with, if any.
 */
public record WokePayload(WakeReason reason, int score, List<Line> lines, boolean shaken, boolean wellRested, Optional<Dream> dream)
        implements CustomPacketPayload {
    public static final Type<WokePayload> TYPE = new Type<>(WhyCantISleep.id("woke"));

    @Override
    public Type<WokePayload> type() {
        return TYPE;
    }

    /** One thing that made the night better or worse, and by how much. */
    public record Line(Component label, int points) {
    }

    /** The sleep effect they woke with: its name, kind and rarity. */
    public record Dream(Component name, SleepEffect.Kind kind, SleepEffect.Rarity rarity) {
    }
}
