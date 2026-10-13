package net.ixdarklord.whycantisleep.sleep;

import java.util.Locale;

/** Why a player isn't in bed, as far as the server can tell; shown on the sleep board. */
public enum AwakeReason {
    AWAKE(0xFFFFFF),
    OTHER_DIMENSION(0xAA77FF),
    MINING(0xFFAA00),
    FIGHTING(0xFF5555),
    AFK(0xAAAAAA);

    private final int color;

    AwakeReason(int color) {
        this.color = color;
    }

    /** The color the reason is written in, as {@code 0xRRGGBB}. */
    public int color() {
        return this.color;
    }

    public String translationKey() {
        return "gui.whycantisleep.board.reason." + this.name().toLowerCase(Locale.ROOT);
    }
}
