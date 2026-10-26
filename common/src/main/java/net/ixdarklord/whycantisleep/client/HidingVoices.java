package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcanvas.api.client.sound.SoundFilter;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Voices from Simple Voice Chat around hiding (the integration is {@code compat/voicechat}, which calls in here from
 * the voice chat's own threads). Speaking while hiding is noise, as loud as the voice ({@link #speak}): whispering
 * hardly counts, talking soon gives the player away and shouting at once, through the same noise as moving about
 * ({@link HidingClient}). And a hiding player's voice reaches others as their spot lets it out ({@link #muffle}): through a
 * chest's lid crack, a closet door's windows, the open sides under a bed, muffled and roughened as the world's sounds
 * are for the hider ({@link HidingAcoustics}); a listener who is hiding hears every voice through their own spot too.
 */
public final class HidingVoices {
    // A voice frame's loudness, from QUIET_DB (nothing) to LOUD_DB (as loud as it counts), in decibels of full scale.
    private static final float QUIET_DB = -45.0F;
    private static final float LOUD_DB = -15.0F;
    // The noise a frame (20 ms) of voice at full loudness makes; whispering, this share of it. Noise falls by
    // HidingClient's NOISE_FALL a tick, so steady talking reaches 1 in about three seconds, a shout in one and a half,
    // and whispering never does.
    private static final float NOISE_PER_FRAME = 0.022F;
    private static final float WHISPER = 0.4F;
    // From a filter's highs (as the game's sound filters have them: the loudness left at 5 kHz) to a cutoff, two
    // one-pole stages; and how hard its distortion clips. A voice is muffled more than the world's sounds: its highs are
    // squared, so a solid side leaves it under 400 Hz, a lid's crack or a door's windows about 1 kHz.
    private static final float REFERENCE_HZ = 5000.0F;
    private static final float SAMPLE_RATE = 48000.0F;
    private static final float DRIVE = 6.0F;

    // The noise spoken since the client last took it, in thousandths (the voice chat's thread adds, the client's takes).
    private static final AtomicInteger SPOKEN = new AtomicInteger();
    // How each player's voice is filtered for the local player now, by their UUID, worked out each tick.
    private static final Map<UUID, SoundFilter> FILTERS = new ConcurrentHashMap<>();
    // Each voice channel's filter state between frames: the two low-pass stages' last samples.
    private static final Map<UUID, float[]> STATES = new ConcurrentHashMap<>();

    private HidingVoices() {
    }

    public static void init() {
        ClientTickEvents.END.register(HidingVoices::tick);
    }

    // Each player's voice as the local player would hear it: out of the speaker's hiding spot, then into their own.
    private static void tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            FILTERS.clear();
            STATES.clear();
            return;
        }
        Vec3 ear = minecraft.player.getEyePosition();
        for (Player player : minecraft.level.players()) {
            if (player == minecraft.player) continue;
            Vec3 mouth = player.getEyePosition();
            SoundFilter filter = HidingAcoustics.heardOutOf(player, ear).then(HidingAcoustics.heardFrom(mouth));
            if (filter.isNone()) FILTERS.remove(player.getUUID());
            else FILTERS.put(player.getUUID(), filter);
        }
        FILTERS.keySet().removeIf(id -> minecraft.level.getPlayerByUUID(id) == null);
    }

    /** The noise spoken since the last call, 0 or more; taken by the hiding client each tick. */
    public static float takeNoise() {
        return SPOKEN.getAndSet(0) / 1000.0F;
    }

    /**
     * A frame of the local player's own voice, about to be sent (16-bit PCM). While they hide, it is noise, as loud as
     * it is, unless the server doesn't let voices give hiders away. Called from the voice chat's thread.
     */
    public static void speak(short[] audio, boolean whispering) {
        if (audio.length == 0 || !HidingClient.isLocalHiding() || !WCISServerConfig.VOICE_GIVES_AWAY.get()) return;
        double sum = 0.0;
        for (short sample : audio) sum += (double) sample * sample;
        float rms = (float) Math.sqrt(sum / audio.length) / 32768.0F;
        float decibels = 20.0F * (float) Math.log10(Math.max(rms, 1.0E-6F));
        float loudness = Mth.clamp((decibels - QUIET_DB) / (LOUD_DB - QUIET_DB), 0.0F, 1.0F);
        float noise = NOISE_PER_FRAME * loudness * (whispering ? WHISPER : 1.0F);
        SPOKEN.addAndGet(Math.round(noise * 1000.0F));
    }

    /**
     * A frame of another player's voice, before it plays (16-bit PCM, a channel's frames in order): muffled and roughened
     * as it would be heard out of their hiding spot, and into the local player's. Null when it is to play as it is.
     * Called from the voice chat's thread.
     */
    public static short @Nullable [] muffle(UUID speaker, UUID channel, short[] audio) {
        if (audio.length == 0) {
            STATES.remove(channel);
            return null;
        }
        SoundFilter filter = FILTERS.get(speaker);
        if (filter == null) {
            STATES.remove(channel);
            return null;
        }
        float smoothing = smoothing(filter.gainHF() * filter.gainHF());
        float distortion = Mth.clamp(filter.distortion(), 0.0F, 1.0F);
        float level = (float) Math.tanh(DRIVE);
        float[] state = STATES.computeIfAbsent(channel, id -> new float[2]);
        short[] out = new short[audio.length];
        for (int i = 0; i < audio.length; i++) {
            float sample = audio[i] / 32768.0F;
            // Two low-pass stages: the highs go as the spot shuts them out.
            state[0] += smoothing * (sample - state[0]);
            state[1] += smoothing * (state[0] - state[1]);
            sample = state[1];
            // A soft clip mixed in, the rougher the more it is shut out.
            sample = Mth.lerp(distortion, sample, (float) Math.tanh(DRIVE * sample) / level);
            out[i] = (short) Mth.clamp(Math.round(sample * filter.gain() * 32767.0F), Short.MIN_VALUE, Short.MAX_VALUE);
        }
        return out;
    }

    // A low-pass stage's smoothing for a filter's highs: two stages that leave that much at the reference frequency.
    private static float smoothing(float highs) {
        if (highs >= 0.999F) return 1.0F;
        float perStage = (float) Math.sqrt(Math.max(highs, 1.0E-3F));
        float cutoff = REFERENCE_HZ / (float) Math.sqrt(1.0F / (perStage * perStage) - 1.0F);
        return 1.0F - (float) Math.exp(-2.0 * Math.PI * Math.min(cutoff, SAMPLE_RATE / 2.0F) / SAMPLE_RATE);
    }
}
