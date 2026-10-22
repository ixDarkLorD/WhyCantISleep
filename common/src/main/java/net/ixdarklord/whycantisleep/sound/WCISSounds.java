package net.ixdarklord.whycantisleep.sound;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.sounds.SoundEvent;

/**
 * The mod's sounds. They need no registry entry: the server names one by its id and the client looks it up in
 * {@code assets/whycantisleep/sounds.json}, which {@code tools/gen_sounds_json.ps1} writes. Each sound plays the
 * author's own {@code .ogg} files when they are in {@code assets/whycantisleep/sounds/}, and one of the game's sounds
 * until then; the table of names and stand-ins is in that script.
 */
public final class WCISSounds {
    /** A card appearing on the sleep HUD. */
    public static final SoundEvent NOTICE = sound("notice");
    /** A heartbeat: a sleep blocked by monsters. */
    public static final SoundEvent BLOCKED = sound("blocked");
    /** The warning before a bed explodes. */
    public static final SoundEvent WARNING = sound("warning");
    /** A player in bed nudging one who is awake. */
    public static final SoundEvent NUDGE = sound("nudge");
    /** The heartbeat minigame (hiding hunted): a beat coming in, and (deeper) one caught. */
    public static final SoundEvent HEARTBEAT_BEAT = sound("heartbeat.beat");
    /** A beat caught in the calm window: a soft shimmer over the deep beat. */
    public static final SoundEvent HEARTBEAT_CALM = sound("heartbeat.calm");
    /** Any click in the minigame. */
    public static final SoundEvent HEARTBEAT_CLICK = sound("heartbeat.click");
    /** A strike: a heart breaks. */
    public static final SoundEvent HEARTBEAT_HEART_LOST = sound("heartbeat.heart_lost");
    /** The third strike: they gasp. */
    public static final SoundEvent HEARTBEAT_GASP = sound("heartbeat.gasp");

    private WCISSounds() {
    }

    private static SoundEvent sound(String name) {
        return SoundEvent.createVariableRangeEvent(WhyCantISleep.id(name));
    }
}