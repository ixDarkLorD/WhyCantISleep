package net.ixdarklord.whycantisleep.client.fx;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.sounds.SoundEvent;

/**
 * The mod's sounds. They are only ever played on the client, so they need no registry entry (which a server without
 * the mod would not know): {@code assets/whycantisleep/sounds.json} gives each its sounds and its subtitle.
 */
public final class WCISSounds {
    /** A card appearing on the sleep HUD. */
    public static final SoundEvent NOTICE = sound("notice");
    /** A sleep blocked by monsters: a heartbeat. */
    public static final SoundEvent BLOCKED = sound("blocked");
    /** The warning before a bed explodes. */
    public static final SoundEvent WARNING = sound("warning");
    /** A whisper from where a blocking monster is. */
    public static final SoundEvent WHISPER = sound("whisper");
    /** The "monster under the bed" scare. */
    public static final SoundEvent SCARE = sound("scare");

    private WCISSounds() {
    }

    private static SoundEvent sound(String name) {
        return SoundEvent.createVariableRangeEvent(WhyCantISleep.id(name));
    }
}
