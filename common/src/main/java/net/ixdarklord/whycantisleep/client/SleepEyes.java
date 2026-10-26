package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.coolcatcanvas.api.client.sound.SoundFilter;
import net.ixdarklord.coolcatcanvas.api.client.sound.SoundFilterProvider;
import net.ixdarklord.coolcatcanvas.api.client.sound.SoundFilters;
import net.ixdarklord.whycantisleep.client.board.SleepBoard;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * The eyelids in bed ({@link Eyelids}): lying down, they grow heavy, drooping slowly to a drowsy squint that sways and
 * nods off now and then. Once everyone needed is asleep (the sleep board says so, or the player is alone), the eyes
 * close, fighting it a moment first, by the time the night passes; if someone gets up again, they open back to drowsy.
 * Waking from a full sleep they flutter open, quickly; getting up before that, they simply open.
 * <p>
 * Sounds sink with the eyes ({@link SoundFilters}): from lying down until the eyes are open again, the world is muffled
 * and roughened as much as the eyes are shut, faint and dull once asleep (client option {@code muffleSounds}).
 */
public final class SleepEyes {
    // How shut the eyes are while drowsy, and how long they take to grow heavy lying down.
    private static final float DROWSY = 0.5F;
    private static final float DROWSE_TICKS = 100.0F;
    // How long in bed (the game's sleep timer, which passes the night at 100) before the eyes start to close, and how
    // long closing takes: shut before the night passes.
    private static final int CLOSE_FROM = 20;
    private static final float CLOSE_TICKS = 70.0F;
    // Opening back to drowsy (someone got up), fluttering open on waking, and opening on getting up early.
    private static final float REOPEN_TICKS = 30.0F;
    private static final float WAKE_TICKS = 24.0F;
    private static final float GET_UP_TICKS = 20.0F;
    // How shut the eyes must be on waking to flutter open rather than simply open.
    private static final float ASLEEP = 0.8F;
    // The sounds asleep, with the eyes fully shut: how loud, how much of their highs, and how much distorted.
    private static final float ASLEEP_GAIN = 0.45F;
    private static final float ASLEEP_HIGHS = 0.08F;
    private static final float ASLEEP_DISTORTION = 0.3F;
    private static boolean inBed;
    private static boolean closing;
    // The filter on the sounds now, swapped in each tick for the sound thread; and how deep it was last tick.
    private static volatile SoundFilter muffle = SoundFilter.NONE;
    private static float depth;

    private SleepEyes() {
    }

    public static void init() {
        ClientTickEvents.END.register(SleepEyes::tick);
        SoundFilters.register(SoundFilterProvider.inWorld(position -> muffle));
        ClientPlayerEvents.LEAVE.register(player -> {
            inBed = false;
            closing = false;
            depth = 0.0F;
            muffle = SoundFilter.NONE;
        });
    }

    private static void tick(Minecraft minecraft) {
        // Paused, the eyes in bed wait with the world.
        if (minecraft.isPaused()) return;
        LocalPlayer player = minecraft.player;
        boolean sleeping = player != null && player.isSleeping() && !HidingClient.isLocalHiding();
        // A night terror has the eyes while it runs; once it is over they are shut, and sleep goes on from there.
        if (NightTerrorClient.running()) {
            inBed = sleeping;
            closing = sleeping;
            listen(minecraft);
            return;
        }
        if (sleeping && !inBed) {
            // Lying down: heavy lids, drifting down, lifting a little, then settling.
            inBed = true;
            closing = false;
            Eyelids.play(DROWSE_TICKS, Eyelids.Mood.DROWSY, 0.0F, 0.0F, 0.45F, 0.3F, 0.6F, 0.24F, 1.0F, DROWSY);
        } else if (sleeping) {
            boolean everyone = everyoneAsleep(minecraft);
            if (!closing && everyone && player.getSleepTimer() >= CLOSE_FROM) {
                // Giving in: nearly shut, a last lift, then shut.
                closing = true;
                Eyelids.play(CLOSE_TICKS, Eyelids.Mood.STILL, 0.0F, 0.0F, 0.3F, 0.78F, 0.42F, 0.66F, 0.75F, 0.95F, 0.85F, 0.9F, 1.0F, 1.0F);
            } else if (closing && !everyone) {
                closing = false;
                Eyelids.ease(DROWSY, REOPEN_TICKS, Eyelids.Mood.DROWSY);
            }
        } else if (inBed) {
            inBed = false;
            closing = false;
            if (Eyelids.shut(0.0F) >= ASLEEP) {
                // Waking: half open, shut again, open further, a last droop, then open.
                Eyelids.play(WAKE_TICKS, Eyelids.Mood.STILL, 0.0F, 0.0F, 0.2F, 0.55F, 0.3F, 0.85F, 0.5F, 0.25F, 0.6F, 0.4F, 1.0F, 0.0F);
            } else {
                Eyelids.ease(0.0F, GET_UP_TICKS, Eyelids.Mood.STILL);
            }
        }
        listen(minecraft);
    }

    // The sounds follow the eyes: from lying down until they are open again (not while hiding: that is its own).
    private static void listen(Minecraft minecraft) {
        boolean muffled = minecraft.player != null && !HidingClient.isLocalHiding() && WCISClientConfig.MUFFLE_SOUNDS.get();
        float next = muffled && (inBed || Eyelids.shut(0.0F) > 0.001F) ? Eyelids.shut(0.0F) : 0.0F;
        if (Math.abs(next - depth) < 0.005F && (next > 0.0F) == (depth > 0.0F)) return;
        depth = next;
        muffle = next <= 0.0F ? SoundFilter.NONE
                : new SoundFilter(Mth.lerp(next, 1.0F, ASLEEP_GAIN), Mth.lerp(next, 1.0F, ASLEEP_HIGHS), next * ASLEEP_DISTORTION);
        SoundFilters.refresh();
    }

    // Whether everyone needed is asleep: as the sleep board says, or yes when the player is alone (singleplayer too).
    private static boolean everyoneAsleep(Minecraft minecraft) {
        Boolean enough = SleepBoard.enoughAsleep();
        if (enough != null) return enough;
        ClientPacketListener connection = minecraft.getConnection();
        return connection == null || connection.getOnlinePlayers().size() < 2;
    }
}
