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
    /** The heartbeat minigame (hiding hunted): its warning slamming in, a deep impact with a dark ring. */
    public static final SoundEvent HEARTBEAT_STING = sound("heartbeat.sting");
    /** A heartbeat coming in, the rhythm to catch. */
    public static final SoundEvent HEARTBEAT_BEAT = sound("heartbeat.beat");
    /** A beat about to reach the calm window: a bright cue to get ready, with the arrows lighting beside the window. */
    public static final SoundEvent HEARTBEAT_READY = sound("heartbeat.ready");
    /** A beat caught in the calm window: a bass thump with a bell ringing over it. */
    public static final SoundEvent HEARTBEAT_CALM = sound("heartbeat.calm");
    /** Any click in the minigame. */
    public static final SoundEvent HEARTBEAT_CLICK = sound("heartbeat.click");
    /** A strike: a heart breaks. */
    public static final SoundEvent HEARTBEAT_HEART_LOST = sound("heartbeat.heart_lost");
    /** The third strike: they gasp. */
    public static final SoundEvent HEARTBEAT_GASP = sound("heartbeat.gasp");

    /** A night terror (all synthesized, {@code tools/gen_terror_sounds.py}): the drone under it, swelling. */
    public static final SoundEvent TERROR_DRONE = sound("terror.drone");
    /** The demon rising out of the floor. */
    public static final SoundEvent TERROR_EMERGE = sound("terror.emerge");
    /** Whispers in the dark. */
    public static final SoundEvent TERROR_WHISPER = sound("terror.whisper");
    /** A shadow hand coming up: bones cracking. */
    public static final SoundEvent TERROR_CREEP = sound("terror.creep");
    /** Face to face with the demon. */
    public static final SoundEvent TERROR_STARE = sound("terror.stare");
    /** The end of the terror: a huge hit and a shriek. */
    public static final SoundEvent TERROR_JOLT = sound("terror.jolt");
    /** Waking from a night terror: a gasp with a rising, unresolved chord. */
    public static final SoundEvent TERROR_WAKE = sound("terror.wake");

    private WCISSounds() {
    }

    private static SoundEvent sound(String name) {
        return SoundEvent.createVariableRangeEvent(WhyCantISleep.id(name));
    }
}