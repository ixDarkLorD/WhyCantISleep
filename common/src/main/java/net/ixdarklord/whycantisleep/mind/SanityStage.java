package net.ixdarklord.whycantisleep.mind;

import java.util.Locale;

/** What a level of sanity brings. Each stage holds below its {@link #below() mark}; the lowest is the worst. */
public enum SanityStage {
    /** It is real, and it hunts. */
    BREAKING(25.0F),
    /** It comes out: a figure only the player sees, gone when looked at. */
    SHAKEN(50.0F),
    /** Eyes under beds in the dark, and whispers. Only the player sees and hears them. */
    UNEASY(75.0F),
    /** Nothing. */
    STEADY(Float.MAX_VALUE);

    private final float below;

    SanityStage(float below) {
        this.below = below;
    }

    /** Sanity under this is this stage (or a worse one). */
    public float below() {
        return this.below;
    }

    /** Whether this stage is as bad as another, or worse. */
    public boolean reached(SanityStage other) {
        return this.ordinal() <= other.ordinal();
    }

    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static SanityStage of(float sanity) {
        for (SanityStage stage : values()) {
            if (sanity < stage.below) return stage;
        }
        return STEADY;
    }
}
