package net.ixdarklord.whycantisleep.sleep;

import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * What a night terror is about: the shadow at the foot of the bed and its hands, the room filling with water, things
 * crawling in from the dark, the bed falling away, or figures that come closer with each blink. Picked at random for
 * each terror on the server ({@code SleepNights}) and shown by the client ({@code client/terror}); everyone sharing a
 * terror has the same one.
 */
public enum NightmareKind {
    SHADOW,
    DROWNING,
    SWARM,
    FALLING,
    WATCHERS;

    /** The kind's name in commands and translation keys. */
    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static NightmareKind random(RandomSource random) {
        NightmareKind[] kinds = values();
        return kinds[random.nextInt(kinds.length)];
    }

    public static @Nullable NightmareKind byId(String id) {
        for (NightmareKind kind : values()) {
            if (kind.id().equals(id)) return kind;
        }
        return null;
    }
}
