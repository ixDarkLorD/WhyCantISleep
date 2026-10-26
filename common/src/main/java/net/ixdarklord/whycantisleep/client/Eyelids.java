package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.config.EyelidStyle;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * The local player's eyelids, drawn over the view: dark lids close in from above and below, leaving an almond of view,
 * widest in the middle and pinched at the sides, its edges shading off (and glowing red in the heartbeat minigame): one
 * texture, stretched taller or shorter. Drawn first in the HUD ({@code GuiMixin}), so the HUD stays over it, and with
 * the HUD hidden too: it is the player's eyes, not part of the HUD.
 * <p>
 * How far they are shut (0 open, 1 shut) moves along a track of keyframes ({@link #play}), each eased into the next,
 * from wherever the lids are at the time, so a new move never jumps: a plain slide ({@link #ease}), or a shaped one
 * (drooping and lifting as sleep comes, fluttering open on waking). Settled where a track ends, they live as the
 * {@link Mood} says: a slow sway, a tighter squint while tense (monsters about), and blinks now and then.
 * {@code HidingClient} drives them while hiding, {@code SleepEyes} while in bed.
 */
public final class Eyelids {
    // The textures, one pair per style (the lids, and the glow along their edge), and how many of the opening's middle
    // half-heights they span top to bottom.
    private static final Identifier[] SPRITES = sprites("");
    private static final Identifier[] GLOW_SPRITES = sprites("_glow");
    private static final float SPAN = 2.5F;
    // How much wider the opening is than the screen once the lids are fully open: enough that the almond's pinched
    // corners (PINCH in tools/gen_eyelids.py) are past the screen's edges.
    private static final float CLEAR = 1.6F;
    // How far out the lids are fully dark, in the opening's middle half-heights: past their soft edge (1 + SOFT in
    // tools/gen_eyelids.py).
    private static final float DARK = 1.3F;
    // The glow along the lids' edge is drawn in this red over them, only while the heartbeat minigame runs.
    private static final int DANGER = 0xFF3A28;
    // How strongly the red glow shows at its fullest: kept low, a hint of red at the lids rather than a wash of it.
    private static final float DANGER_STRENGTH = 0.55F;
    // From how shut the lids cover the whole screen.
    private static final float SHUT = 0.985F;
    // A tighter squint while tense, eased in and out TENSION_RATE a tick.
    private static final float TENSION = 0.08F;
    private static final float TENSION_RATE = 0.01F;

    /** How the lids live while settled: their sway, and their blinks (how often, and how long and heavy). */
    public enum Mood {
        /** Still: no sway, no blinks. */
        STILL(0.0F, 0.0F, 0, 0, 0.0F, 0.0F),
        /** Hiding: a slight sway with the breath and quick blinks. */
        ALERT(0.025F, 0.07F, 70, 200, 6.0F, 0.5F),
        /** In bed: a slow, deep sway and heavy blinks that drop shut fast and lift slowly, as if nodding off. */
        DROWSY(0.05F, 0.035F, 50, 120, 32.0F, 0.2F),
        /** Paralysed in a night terror: no blinks, only a fast tremble, as if fighting to move. */
        PARALYSED(0.03F, 0.9F, 0, 0, 0.0F, 0.0F);

        private final float sway;
        private final float swaySpeed;
        private final int blinkMin;
        private final int blinkMax;
        private final float blinkTicks;
        // How far into a blink the lids are shut: they drop shut by then and open over the rest.
        private final float blinkShutAt;

        Mood(float sway, float swaySpeed, int blinkMin, int blinkMax, float blinkTicks, float blinkShutAt) {
            this.sway = sway;
            this.swaySpeed = swaySpeed;
            this.blinkMin = blinkMin;
            this.blinkMax = blinkMax;
            this.blinkTicks = blinkTicks;
            this.blinkShutAt = blinkShutAt;
        }
    }

    // The track: keyframe times (0 to 1 of its length) and how shut the lids are at each, when it began, its length in
    // ticks, and its most shut, for how far the opening widens as the lids leave the screen.
    private static float[] times = {0.0F, 1.0F};
    private static float[] values = {0.0F, 0.0F};
    private static long startedAt;
    private static float length = 1.0F;
    private static float peak;
    private static Mood mood = Mood.STILL;
    private static boolean tense;
    private static float tension;
    private static float tensionO;
    private static long blinkAt = Long.MIN_VALUE;
    private static long nextBlink;
    private static long tick;

    private Eyelids() {
    }

    public static void init() {
        ClientTickEvents.END.register(Eyelids::tick);
        ClientPlayerEvents.LEAVE.register(player -> {
            times = new float[]{0.0F, 1.0F};
            values = new float[]{0.0F, 0.0F};
            peak = 0.0F;
            mood = Mood.STILL;
            tension = 0.0F;
            tensionO = 0.0F;
        });
    }

    /** Slides the lids from where they are to this far shut over this many ticks, then lets them live as the mood says. */
    public static void ease(float to, float ticks, Mood mood) {
        play(ticks, mood, 0.0F, 0.0F, 1.0F, to);
    }

    /**
     * Moves the lids along a track over this many ticks: pairs of a time (0 to 1, the first 0, rising) and how far shut
     * (0 to 1). The first pair's value is replaced by where the lids are now, so the track always starts from them.
     */
    public static void play(float ticks, Mood mood, float... keyframes) {
        float now = shut(0.0F);
        int count = keyframes.length / 2;
        float[] newTimes = new float[count];
        float[] newValues = new float[count];
        float most = 0.0F;
        for (int i = 0; i < count; i++) {
            newTimes[i] = keyframes[i * 2];
            newValues[i] = i == 0 ? now : Mth.clamp(keyframes[i * 2 + 1], 0.0F, 1.0F);
            most = Math.max(most, newValues[i]);
        }
        times = newTimes;
        values = newValues;
        peak = most;
        length = Math.max(1.0F, ticks);
        startedAt = tick;
        Eyelids.mood = mood;
        blinkAt = Long.MIN_VALUE;
        nextBlink = tick + Math.round(length) + mood.blinkMin;
    }

    /** Whether the lids are tense (a tighter squint): while hiding with monsters about. Set each tick. */
    public static void tense(boolean tense) {
        Eyelids.tense = tense;
    }

    /** How far the track has them shut, without the life in them (0 to 1). */
    public static float track(float partialTicks) {
        float t = Mth.clamp((tick - startedAt + partialTicks) / length, 0.0F, 1.0F);
        for (int i = 1; i < times.length; i++) {
            if (t <= times[i]) {
                float span = times[i] - times[i - 1];
                float k = span <= 0.0F ? 1.0F : (t - times[i - 1]) / span;
                return Mth.lerp(Easing.SINE_IN_OUT.apply(k), values[i - 1], values[i]);
            }
        }
        return values[values.length - 1];
    }

    /** How far the lids are shut as drawn (0 to 1): the track, with the sway, tension and blinks while settled. */
    public static float shut(float partialTicks) {
        float base = track(partialTicks);
        float end = values[values.length - 1];
        if (end <= 0.0F) return base;
        // How much of the settled squint is in: the life in it eases in and out with it.
        float held = Mth.clamp(base / end, 0.0F, 1.0F);
        float time = tick + partialTicks;
        float lively = mood.sway * Mth.sin(time * mood.swaySpeed) + Mth.lerp(partialTicks, tensionO, tension);
        float living = Mth.clamp(base + lively * held, 0.0F, 1.0F);
        return Mth.lerp(blink(time) * held, living, 1.0F);
    }

    /**
     * Where the lower eyelid goes fully dark, in the middle of a screen this tall, without the sway and blinks that
     * come and go: what sits under the lids follows it without jumping at each blink. The screen's height when open.
     */
    public static float lowerLidEdge(int height, float partialTicks) {
        float base = track(partialTicks);
        float end = values[values.length - 1];
        if (end <= 0.0F || base <= 0.001F) return height;
        float held = Mth.clamp(base / end, 0.0F, 1.0F);
        float steady = Mth.clamp(base + Mth.lerp(partialTicks, tensionO, tension) * held, 0.0F, 1.0F);
        return height / 2.0F + height / 2.0F * (1.0F - steady) * DARK;
    }

    // How shut a blink has them (0 to 1): dropping shut by the mood's shut point, then lifting, slowly if it is heavy.
    private static float blink(float time) {
        if (blinkAt == Long.MIN_VALUE || mood.blinkTicks <= 0.0F) return 0.0F;
        float t = (time - blinkAt) / mood.blinkTicks;
        if (t < 0.0F || t >= 1.0F) return 0.0F;
        if (t < mood.blinkShutAt) return Easing.SINE_IN_OUT.apply(t / mood.blinkShutAt);
        return 1.0F - Easing.SINE_IN_OUT.apply((t - mood.blinkShutAt) / (1.0F - mood.blinkShutAt));
    }

    private static void tick(Minecraft minecraft) {
        // Paused, the lids hold where they are: no sway, no blinks, the eased keyframes waiting (the client ticks on
        // behind the pause screen).
        if (minecraft.isPaused()) return;
        tick++;
        tensionO = tension;
        tension = Mth.approach(tension, tense ? TENSION : 0.0F, TENSION_RATE);
        if (mood.blinkMax > 0 && tick >= nextBlink && minecraft.level != null) {
            blinkAt = tick;
            nextBlink = tick + mood.blinkMin + minecraft.level.getRandom().nextInt(mood.blinkMax - mood.blinkMin);
        }
    }

    /** Draws the lids over the view. */
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.options.getCameraType().isFirstPerson()) return;
        float partial = delta.getGameTimeDeltaPartialTick(true);
        float shut = shut(partial);
        if (shut <= 0.001F) return;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        // Shut, or so nearly the texture's clear middle would still show a line across the screen: all dark.
        if (shut >= SHUT) {
            graphics.fill(0, 0, width, height, 0xFF000000);
            return;
        }
        // One stretched texture holds the almond and its soft edge (tools/gen_eyelids.py); above and below it, black.
        // Coming and going, the opening also widens until the pinched corners and the soft edge are off the screen,
        // so the lids leave it smoothly rather than vanish with their corners still showing.
        float in = peak > 0.0F ? Mth.clamp(track(partial) / peak, 0.0F, 1.0F) : 0.0F;
        float open = height / 2.0F * (1.0F - shut) * Mth.lerp(in, CLEAR, 1.0F);
        int lids = Math.max(2, Math.round(open * SPAN));
        int top = (height - lids) / 2;
        int style = WCISClientConfig.EYELID_STYLE.get().ordinal();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITES[style], 0, top, width, lids);
        // The edge glows red only with the heartbeat minigame, fading in and out with it.
        float danger = 1.0F - HidingMinigame.hudVisible(delta);
        if (danger > 0.0F) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, GLOW_SPRITES[style], 0, top, width, lids, ARGB.color(danger * DANGER_STRENGTH, DANGER));
        }
        if (top > 0) {
            graphics.fill(0, 0, width, top, 0xFF000000);
            graphics.fill(0, top + lids, width, height, 0xFF000000);
        }
    }

    // Each style's sprite of the eyelids' textures with this suffix, by the style's ordinal.
    private static Identifier[] sprites(String suffix) {
        EyelidStyle[] styles = EyelidStyle.values();
        Identifier[] sprites = new Identifier[styles.length];
        for (EyelidStyle style : styles) sprites[style.ordinal()] = WhyCantISleep.id("hud/eyelids/" + style.id() + suffix);
        return sprites;
    }
}
