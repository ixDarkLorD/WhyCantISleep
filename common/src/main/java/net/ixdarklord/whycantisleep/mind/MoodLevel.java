package net.ixdarklord.whycantisleep.mind;

import java.util.Locale;

/** A mood in words. Each level holds below its {@link #below() mark}; the lowest is the worst. */
public enum MoodLevel {
    MISERABLE(25.0F),
    GLOOMY(40.0F),
    /** Neither here nor there: lucidity holds. */
    FLAT(55.0F),
    /** From here up, lucidity recovers. */
    CONTENT(75.0F),
    CHEERFUL(Float.MAX_VALUE);

    private final float below;

    MoodLevel(float below) {
        this.below = below;
    }

    public float below() {
        return this.below;
    }

    /** Whether this mood isn't holding lucidity up any more: flat or worse, when the HUD shows its badge. */
    public boolean isUnstable() {
        return this.ordinal() <= FLAT.ordinal();
    }

    /** Whether this mood is low enough to warn the player about. */
    public boolean isLow() {
        return this.ordinal() <= GLOOMY.ordinal();
    }

    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static MoodLevel of(float mood) {
        for (MoodLevel level : values()) {
            if (mood < level.below) return level;
        }
        return CHEERFUL;
    }
}
