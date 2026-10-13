package net.ixdarklord.whycantisleep.mind;

import java.util.Locale;

/** What a level of lucidity brings. Each stage holds below its {@link #below() mark}; the lowest is the worst. */
public enum LucidityStage {
    /** It is real, and it hunts. */
    BREAKING(25.0F),
    /** It comes out: a figure only the player sees, gone when looked at. */
    SHAKEN(50.0F),
    /** Eyes under beds in the dark, and whispers. Only the player sees and hears them. */
    UNEASY(75.0F),
    /** Nothing. */
    STEADY(Float.MAX_VALUE);

    private final float below;

    LucidityStage(float below) {
        this.below = below;
    }

    /** Lucidity under this is this stage (or a worse one). */
    public float below() {
        return this.below;
    }

    /** Whether this stage is as bad as another, or worse. */
    public boolean reached(LucidityStage other) {
        return this.ordinal() <= other.ordinal();
    }

    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static LucidityStage of(float lucidity) {
        for (LucidityStage stage : values()) {
            if (lucidity < stage.below) return stage;
        }
        return STEADY;
    }
}
