package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcanvas.api.client.sound.SoundFilter;
import net.ixdarklord.coolcatcanvas.api.client.sound.SoundFilterProvider;
import net.ixdarklord.coolcatcanvas.api.client.sound.SoundFilters;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.network.DeepSleepPayload;
import net.ixdarklord.whycantisleep.sound.WCISSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.Mth;

/**
 * A deep sleep, the good end of a night as a night terror is the bad one: the eyes fall shut at once, the world goes
 * almost silent (a filter deeper than {@code SleepEyes}' own, eased in and out; client option {@code muffleSounds}), and
 * a slow heartbeat, close and quiet, slows further and fades. {@code SleepEyes} leaves the eyes to it while it runs; the
 * game's own sleep fade goes on as usual. Nothing is seen: the reward is the morning's stronger sleep effect.
 */
public final class DeepSleepClient {
    // How quickly the eyes fall shut, and how long the hush takes to come and to go.
    private static final float SHUT_TICKS = 30.0F;
    private static final float HUSH_IN = 40.0F;
    private static final float HUSH_OUT = 20.0F;
    // The world at the deepest: how loud, and how much of its highs.
    private static final float DEEP_GAIN = 0.2F;
    private static final float DEEP_HIGHS = 0.03F;
    // The heartbeat: the gap between beats at the start and at the end (ticks), and how loud at the start and end.
    private static final float FIRST_GAP = 22.0F;
    private static final float LAST_GAP = 36.0F;
    private static final float FIRST_VOLUME = 0.35F;
    private static final float LAST_VOLUME = 0.12F;
    private static long tick;
    private static long since = -1;
    private static int length;
    private static long nextBeat;
    private static float depth;
    private static volatile SoundFilter hush = SoundFilter.NONE;

    private DeepSleepClient() {
    }

    public static void init() {
        ClientTickEvents.END.register(DeepSleepClient::tick);
        SoundFilters.register(SoundFilterProvider.inWorld(position -> hush));
        ClientPlayerEvents.LEAVE.register(player -> {
            since = -1;
            depth = 0.0F;
            hush = SoundFilter.NONE;
        });
    }

    /** Whether a deep sleep is under way: it has the eyes until it ends. */
    public static boolean running() {
        return since >= 0;
    }

    public static void on(DeepSleepPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        since = tick;
        length = payload.ticks();
        nextBeat = tick + 10;
        // The lids drop shut, a flicker of a lift, then shut for good.
        Eyelids.play(SHUT_TICKS, Eyelids.Mood.STILL, 0.0F, Eyelids.shut(0.0F), 0.5F, 0.95F, 0.65F, 0.85F, 1.0F, 1.0F);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(WCISSounds.HEARTBEAT_CALM, 0.8F, 0.35F));
    }

    private static void tick(Minecraft minecraft) {
        if (minecraft.isPaused()) return;
        tick++;
        LocalPlayer player = minecraft.player;
        if (since >= 0 && (player == null || !player.isSleeping() || tick - since > length)) since = -1;
        if (since >= 0) beat(minecraft, (tick - since) / (float) length);
        listen(player);
    }

    // A slow heartbeat, slowing and fading as the sleep deepens; not filtered, it is their own.
    private static void beat(Minecraft minecraft, float progress) {
        if (tick < nextBeat) return;
        float volume = Mth.lerp(progress, FIRST_VOLUME, LAST_VOLUME);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(WCISSounds.HEARTBEAT_BEAT, 0.75F, volume));
        nextBeat = tick + Math.round(Mth.lerp(progress, FIRST_GAP, LAST_GAP));
    }

    // The hush eases in while it runs and out after, swapped in for the sound thread when it changes.
    private static void listen(LocalPlayer player) {
        boolean on = since >= 0 && player != null && WCISClientConfig.MUFFLE_SOUNDS.get();
        float next = on ? Math.min(1.0F, depth + 1.0F / HUSH_IN) : Math.max(0.0F, depth - 1.0F / HUSH_OUT);
        if (next == depth) return;
        depth = next;
        float eased = depth * depth * (3.0F - 2.0F * depth);
        hush = depth <= 0.0F ? SoundFilter.NONE : new SoundFilter(Mth.lerp(eased, 1.0F, DEEP_GAIN), Mth.lerp(eased, 1.0F, DEEP_HIGHS));
        SoundFilters.refresh();
    }
}
