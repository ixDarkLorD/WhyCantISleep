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
    /** A heartbeat: a sleep blocked by monsters, and the Lurker near. */
    public static final SoundEvent BLOCKED = sound("blocked");
    /** The warning before a bed explodes. */
    public static final SoundEvent WARNING = sound("warning");
    /** A whisper from where a blocking monster is, or from under a bed. */
    public static final SoundEvent WHISPER = sound("whisper");
    /** The "monster under the bed" scare. */
    public static final SoundEvent SCARE = sound("scare");
    /** A player in bed nudging one who is awake. */
    public static final SoundEvent NUDGE = sound("nudge");

    public static final SoundEvent LURKER_AMBIENT = sound("lurker.ambient");
    public static final SoundEvent LURKER_HURT = sound("lurker.hurt");
    public static final SoundEvent LURKER_DEATH = sound("lurker.death");
    /** The Lurker coming out into the world. */
    public static final SoundEvent LURKER_EMERGE = sound("lurker.emerge");
    /** The Lurker, ignored before, calling to be looked at. */
    public static final SoundEvent LURKER_LURE = sound("lurker.lure");
    /** The Lurker seen, and starting its hunt. */
    public static final SoundEvent LURKER_NOTICE = sound("lurker.notice");
    /** The Lurker gone: at daylight, or with its player out of reach. */
    public static final SoundEvent LURKER_VANISH = sound("lurker.vanish");

    private WCISSounds() {
    }

    private static SoundEvent sound(String name) {
        return SoundEvent.createVariableRangeEvent(WhyCantISleep.id(name));
    }
}