package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.client.hud.HudFade;
import net.ixdarklord.whycantisleep.client.hud.HudStyle;
import net.ixdarklord.whycantisleep.hiding.Hiding;
import net.ixdarklord.whycantisleep.sound.WCISSounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * Keeping their heart slow, for a player who ran into a hiding spot with something hunting them. It opens with a
 * warning ("Watch out!" or "Be careful!") slamming in under the crosshair, the rest of the HUD fading away
 * ({@link HudFade}: the cards, the crosshair, the hotbar and all) and their moves going unheard; then a band of smoke
 * billows open across the whole screen under it. While monsters are near, beats come one after another: a heartbeat
 * crosses a pulse line from right to left, and a left click as it passes through the calm window in the middle keeps
 * it slow. Clicking too early or too late, or letting it pass, is a strike, and breaks a heart; at the third they
 * gasp, as loud as can be, and whatever is near the spot hears them (the server decides, as for any noise). It lasts
 * until the hide time is up.
 * <p>
 * The warning stays over the band, trembling a little, and becomes a live line on how they are doing: steady, doing
 * great, too early, too late, missed, last chance, held their nerve. A good line pops in, swelling and settling; a bad
 * one comes in with a hard shake. Under it are three of the game's hearts (the one a strike breaks pops, swelling and
 * fading). The band's smoke drifts and bobs in layers, its edges never still; through it runs the pulse line, fading
 * out at both ends (its calm window a rounded frame that breathes while a beat is in it; a caught beat leaves an echo
 * that swells out of it, and a ring of light), with the game's own mouse just over the window, which swells and shows
 * its left button pressed at each click (and lights it while a beat is in the window), and what to do under it. Round
 * the screen, a dark red
 * edge and veins creep in, further and brighter at each strike and falling back as a streak of caught beats calms
 * them, throbbing at each beat. Each beat is heard coming, a caught one comes deep with a shimmer, a click ticks, a
 * broken heart cracks, and the third gasps ({@link WCISSounds}).
 */
public final class HidingMinigame {
    private static final int STRIKES = 3;
    // A beat: it crosses the line in BEAT_TICKS; the next comes GAP_MIN to GAP_MAX ticks after.
    private static final float BEAT_TICKS = 30.0F;
    private static final int GAP_MIN = 14;
    private static final int GAP_MAX = 34;
    // The calm window, as a share of the line, in its middle, and how fast it breathes while a beat is in it.
    private static final float ZONE = 0.18F;
    private static final float BREATH_SPEED = 0.9F;
    // How far the window's frame reaches above and below the pulse line.
    private static final int ZONE_RISE = 3;
    // The opening: the warning slams in over SLAM_TICKS, the panel pops up from PANEL_AT over POP_IN_TICKS, and the
    // first beat waits for FIRST_BEAT. The HUD fades out over HUD_FADE_TICKS from the start, and back at the end.
    private static final float SLAM_TICKS = 8.0F;
    private static final int PANEL_AT = 14;
    private static final float POP_IN_TICKS = 8.0F;
    private static final int FIRST_BEAT = 40;
    private static final float HUD_FADE_TICKS = 10.0F;
    // How long a hit or a strike lights things up (a heart blinks at a strike), a click's press lasts, and the panel
    // fades out.
    private static final float FLASH_TICKS = 8.0F;
    private static final float PRESS_TICKS = 6.0F;
    private static final float FADE_TICKS = 6.0F;
    // A caught beat leaves an echo that swells out of it and fades over ECHO_TICKS; a broken heart pops, swelling and
    // fading, over POP_TICKS.
    private static final float ECHO_TICKS = 12.0F;
    private static final float POP_TICKS = 12.0F;
    // How much the mouse swells at a click, and breathes while a beat is in the window.
    private static final float PRESS_SWELL = 0.3F;
    private static final float HINT_SWELL = 0.08F;
    // The caught beat's bell climbs these semitones (a minor pentatonic scale) through a streak, and stays at the top.
    private static final int[] STREAK_STEPS = {0, 3, 5, 7, 10, 12};
    // How long the panel stays up after they gasp, or after they held out, to show it.
    private static final int GASPED_TICKS = 40;
    // Gasped, the band fades out over GASP_FADE_TICKS.
    private static final float GASP_FADE_TICKS = 20.0F;
    private static final int HELD_TICKS = 30;
    // A new line: a good one pops (swelling by POP_SWELL and settling), a bad one shakes (SHAKE pixels, dying away),
    // over LINE_TICKS; between them the line trembles by TREMBLE. The warning slams in from SLAM_SCALE to LINE_SCALE.
    private static final float LINE_TICKS = 12.0F;
    private static final float POP_SWELL = 0.5F;
    private static final float SHAKE = 4.0F;
    private static final float TREMBLE = 0.6F;
    private static final float SLAM_SCALE = 3.0F;
    private static final float LINE_SCALE = 1.5F;
    // The band: smoke across the whole screen, in three layers (the band, its front with a faint glow along its edges,
    // and wisps curling off them: hud/minigame_smoke_*, from tools/gen_minigame_fx.py) of SMOKE_WIDTH by
    // SMOKE_HEIGHT, drifting past each other at SMOKE_DRIFT pixels a tick; in it, the pulse line, kept SIDE_SHARE of
    // the screen in from its edges, and what to do under it. Down the middle of the screen: the live line on the
    // crosshair; then, from a top LINE_BELOW_CROSSHAIR under it, the hearts HEARTS_BELOW_TEXT down, each of them and
    // the mouse in a small frame ICON_GAP apart, and the calm window MOUSE_ABOVE_WINDOW under the mouse's frame, the
    // band's middle with it.
    private static final Identifier[] SMOKE = {
            Identifier.fromNamespaceAndPath("whycantisleep", "hud/minigame_smoke_back"),
            Identifier.fromNamespaceAndPath("whycantisleep", "hud/minigame_smoke_front"),
            Identifier.fromNamespaceAndPath("whycantisleep", "hud/minigame_smoke_wisps")};
    private static final int SMOKE_WIDTH = 256;
    private static final int SMOKE_HEIGHT = 96;
    private static final float[] SMOKE_DRIFT = {0.35F, -0.6F, 1.0F};
    // Each layer bobs up and down by SMOKE_BOB pixels at its own speed, and the band swells by SMOKE_SWELL of itself.
    private static final float[] SMOKE_BOB = {1.0F, 1.5F, 2.5F};
    private static final float[] SMOKE_BOB_SPEED = {0.05F, 0.08F, 0.11F};
    private static final float SMOKE_SWELL = 0.06F;
    private static final float SMOKE_SWELL_SPEED = 0.07F;
    private static final float SIDE_SHARE = 0.1F;
    // How much of the pulse line, at each end, it takes to fade in.
    private static final float LINE_FADE_SHARE = 0.18F;
    private static final int SIDE_MIN = 16;
    private static final int LINE_HEIGHT = 16;
    private static final int REST = 10;
    private static final int HINT_BELOW = 29;
    // The soft rule under the hint: how far under it, and how far it reaches past the text either side.
    private static final int RULE_BELOW = 11;
    private static final int RULE_REACH = 24;
    private static final int LINE_BELOW_CROSSHAIR = 20;
    private static final int HEARTS_BELOW_TEXT = 13;
    // From the foot of the mouse's frame to the pulse line: an 8 pixel gap to the top of the window's frame, which
    // reaches ZONE_RISE above the line.
    private static final int MOUSE_ABOVE_WINDOW = 8 + ZONE_RISE;
    // The frames round the hearts and the mouse: how far out from them, and the gap between the two.
    private static final int ICON_PAD = 3;
    private static final int ICON_GAP = 4;
    // From the stack's top to the band's middle, down that stack; how far the band's body reaches above its middle; from
    // the band's middle to the foot of the rule under the hint; and the room kept under the rule at the foot of the
    // screen.
    private static final int TEXT_TO_BAND = HEARTS_BELOW_TEXT + HidingMinigame.HEART_SIZE + ICON_PAD * 2 + ICON_GAP + ICON_PAD
            + HidingMinigame.MOUSE_SIZE + ICON_PAD + MOUSE_ABOVE_WINDOW + HidingMinigame.LINE_HEIGHT / 2;
    private static final int BAND_HALF = 23;
    private static final int FOOT = HINT_BELOW + RULE_BELOW + 1;
    private static final int BOTTOM_ROOM = 4;
    // The smallest GUI scale the minigame is drawn at: below it, it is drawn larger by a whole number to make it up.
    private static final int MIN_SCALE = 3;
    // The least room, in its own pixels, the minigame is laid out in: it is drawn larger only while it keeps this much,
    // and smaller than the GUI's scale when the screen has less. Tall enough for the live line on the crosshair and,
    // under it, the stack, the band and the hint with its rule; wide enough for the hint and its rule, and the pulse
    // line beside them (minWidth()).
    private static final int MIN_WIDTH = 320;
    private static final int MIN_HEIGHT = 2 * (LINE_BELOW_CROSSHAIR + TEXT_TO_BAND + FOOT + BOTTOM_ROOM);
    // Warm, to match the ember band: the pulse line, its window, the frames and the mouse's glow in a soft coral
    // (CALM_DARK its shadow), the spike and its echo in a pale ember white (GLOW), the good lines a warm white of their
    // own so they stay apart from the amber warnings, and the hint a dim rose-brown.
    private static final int CALM = 0xFF9C7E;
    private static final int CALM_DARK = 0xB85A44;
    private static final int GLOW = 0xFFE9DE;
    private static final int GOOD = 0xFFDCCB;
    private static final int HINT = 0x86605A;
    // The screen round it: a dark red edge (hud/minigame_vignette) and veins creeping in from it (hud/minigame_veins_1
    // to 3, each reaching further). How far they have come follows how it is going, from none to the third on a scale
    // of VEIN_LEVELS: up a level at each strike, down as a streak of caught beats calms them, eased there at VEIN_RATE a
    // tick. Each beat makes them throb, by THROB, over THROB_TICKS.
    private static final Identifier VIGNETTE = Identifier.fromNamespaceAndPath("whycantisleep", "hud/minigame_vignette");
    private static final Identifier[] VEINS = {
            Identifier.fromNamespaceAndPath("whycantisleep", "hud/minigame_veins_1"),
            Identifier.fromNamespaceAndPath("whycantisleep", "hud/minigame_veins_2"),
            Identifier.fromNamespaceAndPath("whycantisleep", "hud/minigame_veins_3")};
    private static final float VEIN_LEVELS = 3.0F;
    private static final float VEIN_RATE = 0.06F;
    private static final float THROB = 0.3F;
    private static final float THROB_TICKS = 10.0F;
    // Around the pulse line the veins are gone within VEIN_CLEAR of it (clear of its window's frame too), and fade in
    // over VEIN_FADE beyond, in strips VEIN_STEP tall.
    private static final int VEIN_CLEAR = 6;
    private static final int VEIN_CLEAR_BELOW = 16;
    private static final int VEIN_FADE = 24;
    private static final int VEIN_STEP = 2;
    private static final int WARN = 0xFFB347;
    private static final int OUTLINE = 0x0B0C0F;
    // A heartbeat on the line, by columns from its peak: a little bump, a dip, the tall spike the click is timed to,
    // the drop well under the rest line, and a low swell after (up is negative).
    private static final int[] BEAT = {-1, -2, -1, 0, 3, -10, 6, 2, 0, -1, -2, -2, -1};
    private static final int BEAT_PEAK = 5;
    // Between beats the line is never quite flat: a rounded bump every TRACE_BUMP columns and a quick flick every
    // TRACE_FLICK, over a slow sway (trace()).
    private static final int[] BUMP = {-1, -2, -2, -1};
    private static final int[] FLICK = {1, -2, 1};
    private static final int TRACE_BUMP = 41;
    private static final int TRACE_FLICK = 67;
    // Where the calm window starts along the line, and how far ahead of it the get-ready arrows begin to light.
    private static final float WINDOW_START = 0.5F - ZONE / 2.0F;
    private static final float READY_LEAD = 0.32F;
    // The get-ready chevrons under the window: how far each reaches either side of its point, and how far apart the
    // two are.
    private static final int ARROW_HALF = 4;
    private static final int ARROW_STEP = 5;
    // The game's own mouse (the tutorial toast's), 20 square, over the calm window; its left button's pixels in it:
    // three across on the top two rows, two across on the two under.
    private static final Identifier MOUSE = Identifier.withDefaultNamespace("toast/mouse");
    private static final int MOUSE_SIZE = 20;
    private static final int BUTTON_X = 6;
    private static final int BUTTON_Y = 6;
    // The game's own hearts, 9 square, overlapping by one as on the health bar.
    private static final Identifier HEART = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier HEART_BLINKING = Identifier.withDefaultNamespace("hud/heart/full_blinking");
    private static final Identifier HEART_EMPTY = Identifier.withDefaultNamespace("hud/heart/container");
    private static final Identifier HEART_EMPTY_BLINKING = Identifier.withDefaultNamespace("hud/heart/container_blinking");
    private static final int HEART_SIZE = 9;
    private static final int HEART_STEP = 8;
    private static final int HEARTS_WIDTH = HEART_STEP * (STRIKES - 1) + HEART_SIZE;

    /** The live line over the panel: what it says, and whether it is good news, bad news or a warning. */
    private enum Line {
        WATCH_OUT("watch_out", Mood.WARN),
        CAREFUL("careful", Mood.WARN),
        STEADY("steady", Mood.GOOD),
        GREAT("great", Mood.GOOD),
        COOL("cool", Mood.GOOD),
        EARLY("early", Mood.BAD),
        LATE("late", Mood.BAD),
        MISSED("missed", Mood.BAD),
        LAST_CHANCE("last_chance", Mood.BAD),
        OVERTIME("overtime", Mood.WARN),
        GASPED("gasped", Mood.BAD),
        HELD("held", Mood.GOOD);

        private final String key;
        private final Mood mood;

        Line(String key, Mood mood) {
            this.key = key;
            this.mood = mood;
        }

        private Component text() {
            return Component.translatable("gui.whycantisleep.minigame." + this.key);
        }

        private int color() {
            return switch (this.mood) {
                case GOOD -> GOOD;
                case BAD -> HudStyle.RED;
                case WARN -> WARN;
            };
        }
    }

    private enum Mood { GOOD, BAD, WARN }

    private static boolean active;
    private static int strikes;
    private static int streak;
    private static long tick;
    private static long startedAt = Long.MIN_VALUE;
    private static long endedAt = Long.MIN_VALUE;
    private static long beatAt = -1;
    private static long nextBeat;
    private static long flashAt = Long.MIN_VALUE;
    private static long pressedAt = Long.MIN_VALUE;
    private static long hitAt = Long.MIN_VALUE;
    private static float hitAlong;
    private static long brokenAt = Long.MIN_VALUE;
    private static int brokenHeart;
    private static boolean flashGood;
    private static boolean gasped;
    private static long gaspedAt = Long.MIN_VALUE;
    private static boolean clicked;
    private static boolean landed;
    private static boolean readied;
    // Whether the last beat judged was missed (early, late or let pass), and how many beats of overtime it has had.
    private static boolean missedLast;
    private static int overtimes;
    private static Line line = Line.WATCH_OUT;
    private static long lineAt = Long.MIN_VALUE;
    private static float veins;
    private static float veinsO;
    private static long throbAt = Long.MIN_VALUE;
    // The screen's size as the minigame lays itself out on it, at its own scale (render()).
    private static int screenWidth;
    private static int screenHeight;

    private HidingMinigame() {
    }

    public static void init() {
        // Its clock stops while the game is paused (the client ticks on behind the pause screen), so beats don't cross
        // the line, or slip past the window, unseen.
        ClientTickEvents.END.register(minecraft -> {
            if (!minecraft.isPaused()) tick++;
        });
        ClientGuiEvents.RENDER_HUD.register(HidingMinigame::render);
    }

    /** They went in hunted: the warning, then the beats. */
    public static void begin() {
        active = true;
        strikes = 0;
        streak = 0;
        gasped = false;
        gaspedAt = Long.MIN_VALUE;
        startedAt = tick;
        endedAt = Long.MIN_VALUE;
        beatAt = -1;
        nextBeat = tick + FIRST_BEAT;
        clicked = false;
        landed = false;
        veins = 0.0F;
        veinsO = 0.0F;
        missedLast = false;
        overtimes = 0;
        Minecraft minecraft = Minecraft.getInstance();
        say(minecraft.level != null && minecraft.level.getRandom().nextBoolean() ? Line.WATCH_OUT : Line.CAREFUL);
    }

    /** They came out ({@code heldOut} false), or held out till the hide time was up: it ends. */
    public static void end(boolean heldOut) {
        if (!active) return;
        active = false;
        if (heldOut) {
            say(Line.HELD);
            endedAt = tick + HELD_TICKS;
        } else {
            endedAt = tick;
        }
    }

    /** Whether it is on, from the warning until it ends. While it is, the player's moves make no noise. */
    public static boolean running() {
        return active;
    }

    /**
     * The hide time is up, or an overtime beat's time: whether it goes on for a beat more. It does when they missed the
     * last beat, with something still out there, at most {@link Hiding#MAX_OVERTIME} times; the next beat then comes
     * soon.
     */
    public static boolean overtime(boolean danger) {
        if (!active || !missedLast || !danger || overtimes >= Hiding.MAX_OVERTIME) return false;
        overtimes++;
        say(Line.OVERTIME);
        if (beatAt < 0) nextBeat = Math.min(nextBeat, tick + GAP_MIN);
        return true;
    }

    /** Whether a beat was caught in overtime, which ends it at once. */
    public static boolean settled() {
        return active && overtimes > 0 && !missedLast;
    }

    /** A left click while hiding (taken from the game, so it never swings at anything), for the next tick to judge. */
    public static void click() {
        clicked = true;
        if (!active || tick - startedAt < PANEL_AT) return;
        pressedAt = tick;
        play(Minecraft.getInstance(), WCISSounds.HEARTBEAT_CLICK, 0.3F, 1.0F);
    }

    /** How much of the rest of the HUD shows this frame, 0 to 1: it fades as the minigame opens, and back once it has gone. */
    public static float hudVisible(DeltaTracker delta) {
        if (startedAt == Long.MIN_VALUE) return 1.0F;
        float now = tick + delta.getGameTimeDeltaPartialTick(true);
        float hidden = active ? (now - startedAt) / HUD_FADE_TICKS
                : endedAt == Long.MIN_VALUE ? 0.0F : (endedAt + FADE_TICKS - now) / HUD_FADE_TICKS;
        return 1.0F - Mth.clamp(hidden, 0.0F, 1.0F);
    }

    /**
     * A tick of it, with whether monsters are near (the beats only come while they are). Returns true on the tick they
     * gasp, for their noise to go to its loudest.
     */
    public static boolean tick(Minecraft minecraft, boolean danger) {
        // Paused, nothing moves on: no beats, no strikes, the veins where they are.
        if (minecraft.isPaused()) return false;
        boolean pressed = clicked;
        clicked = false;
        // The veins come in a level a strike, and fall back as a streak calms them; gasped, they take the screen.
        veinsO = veins;
        float target = gasped ? VEIN_LEVELS : Math.max(0.0F, strikes + (streak == 0 ? 0.3F : 0.0F) - Math.min(streak, 4) * 0.15F);
        veins = Mth.approach(veins, target, VEIN_RATE);
        if (!active || gasped) return false;
        if (!landed && tick - startedAt >= SLAM_TICKS) {
            // The warning lands with a deep impact.
            landed = true;
            play(minecraft, WCISSounds.HEARTBEAT_STING, 0.9F, 1.0F);
        }
        if (tick - startedAt < PANEL_AT) return false;
        boolean struck = false;
        if (beatAt < 0) {
            if (danger && tick >= nextBeat) {
                beatAt = tick;
                throbAt = tick;
                readied = false;
                // Each beat is heard coming, faintly: the rhythm to catch.
                play(minecraft, WCISSounds.HEARTBEAT_BEAT, 0.5F, 1.0F);
            }
        } else {
            float along = (tick - beatAt) / BEAT_TICKS;
            if (!readied && along >= WINDOW_START - READY_LEAD) {
                // Get ready: a swell that rises until the spike reaches the window and stops there (at its own pitch,
                // so its length stays in time), with the arrows lighting beside it.
                readied = true;
                if (along < WINDOW_START) play(minecraft, WCISSounds.HEARTBEAT_READY, 0.45F, 1.0F);
            }
            if (pressed) {
                if (inWindow(along)) {
                    hit(minecraft, along);
                } else {
                    struck = true;
                    // The beat comes in from the right, so a click before it reaches the window is early.
                    say(along < 0.5F ? Line.EARLY : Line.LATE);
                }
                nextBeatAfter(minecraft);
            } else if (along > 1.0F) {
                struck = true;
                say(Line.MISSED);
                nextBeatAfter(minecraft);
            }
        }
        if (!struck) return false;
        missedLast = true;
        strikes++;
        streak = 0;
        throbAt = tick;
        flashAt = tick;
        flashGood = false;
        brokenAt = tick;
        brokenHeart = STRIKES - strikes;
        play(minecraft, WCISSounds.HEARTBEAT_HEART_LOST, 0.8F, 1.0F);
        if (strikes == STRIKES - 1) say(Line.LAST_CHANCE);
        if (strikes < STRIKES) return false;
        gasped = true;
        gaspedAt = tick;
        say(Line.GASPED);
        endedAt = tick + GASPED_TICKS;
        active = false;
        play(minecraft, WCISSounds.HEARTBEAT_GASP, 1.0F, 1.0F);
        return true;
    }

    // Whether a beat this far across the line (0 coming in at the right, 1 gone at the left) is in the calm window.
    private static boolean inWindow(float along) {
        return Math.abs(along - 0.5F) <= ZONE / 2.0F;
    }

    // Caught: a bass thump with a bell over it, a step higher up a minor pentatonic scale with each beat of a streak,
    // and an echo where the beat was; the line cheers them on, the more the longer they keep it up.
    private static void hit(Minecraft minecraft, float along) {
        flashAt = tick;
        flashGood = true;
        hitAt = tick;
        hitAlong = along;
        missedLast = false;
        streak++;
        say(streak >= 5 ? Line.COOL : streak >= 3 ? Line.GREAT : Line.STEADY);
        int step = STREAK_STEPS[Math.min(streak, STREAK_STEPS.length) - 1];
        play(minecraft, WCISSounds.HEARTBEAT_CALM, 0.7F, (float) Math.pow(2.0, step / 12.0));
    }

    // A new line, or the same one again: each says it anew, so a second miss shakes as hard as the first, and each beat
    // of a streak pops.
    private static void say(Line next) {
        line = next;
        lineAt = tick;
    }

    private static void nextBeatAfter(Minecraft minecraft) {
        beatAt = -1;
        nextBeat = tick + GAP_MIN + minecraft.level.getRandom().nextInt(GAP_MAX - GAP_MIN);
    }

    private static void play(Minecraft minecraft, SoundEvent sound, float volume, float pitch) {
        if (minecraft.player == null) return;
        minecraft.level.playLocalSound(minecraft.player.getX(), minecraft.player.getEyeY(), minecraft.player.getZ(), sound, SoundSource.PLAYERS, volume, pitch,
                false);
    }

    /** The live line under the crosshair and the panel under it, clear over the faded HUD. */
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        // The last of the mod's HUD layers: the fade they were drawn under ends here.
        HudFade.end();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.hud.isHidden() || startedAt == Long.MIN_VALUE) return;
        // On a small GUI scale the band and its pixel art would shrink to a sliver: drawn larger by a whole number, so
        // it is never smaller on screen than at MIN_SCALE and its pixels stay square. Laid out on the screen as that
        // larger scale sees it.
        // But never so large the screen it is laid out on gets smaller than its least room: on a small window the
        // game's own scale is low because the window is small, and the minigame must still fit in it. Where even the
        // GUI's own scale leaves less than that room, it is drawn smaller, to fit all of it in.
        int minWidth = minWidth(minecraft.font);
        int boost = Math.max(1, Mth.ceil(MIN_SCALE / (float) minecraft.getWindow().getGuiScale()));
        while (boost > 1 && (graphics.guiWidth() / boost < minWidth || graphics.guiHeight() / boost < MIN_HEIGHT)) boost--;
        float scale = Math.min(boost, Math.min(graphics.guiWidth() / (float) minWidth, graphics.guiHeight() / (float) MIN_HEIGHT));
        if (scale >= 1.0F) scale = boost;
        screenWidth = Mth.ceil(graphics.guiWidth() / scale);
        screenHeight = Mth.ceil(graphics.guiHeight() / scale);
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        try {
            draw(graphics, minecraft, delta);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private static void draw(GuiGraphicsExtractor graphics, Minecraft minecraft, DeltaTracker delta) {
        float partial = delta.getGameTimeDeltaPartialTick(true);
        float now = tick + partial;
        // The veins come and go with the HUD's fade; the rest fades out faster at the end.
        float presence = 1.0F - hudVisible(delta);
        // The stack under the crosshair, unless the band at its foot would then start above the lower eyelid's edge:
        // then all of it goes down, the band starting just under the lid. Never so low the hint or its rule falls off
        // the screen.
        int height = screenHeight;
        int lineY = height / 2 + LINE_BELOW_CROSSHAIR;
        float lid = Eyelids.lowerLidEdge(height, partial);
        if (lid < height) lineY = Math.max(lineY, Math.round(lid) + BAND_HALF - TEXT_TO_BAND);
        lineY = Math.min(lineY, height - TEXT_TO_BAND - FOOT - BOTTOM_ROOM);
        int pulseY = lineY + TEXT_TO_BAND - LINE_HEIGHT / 2;
        // Gasped, the band, the veins and the red edge fade out round the line.
        float gasp = gaspedAt == Long.MIN_VALUE ? 0.0F : now - gaspedAt;
        float gaspFade = 1.0F - Mth.clamp(gasp / GASP_FADE_TICKS, 0.0F, 1.0F);
        // Under the band, the screen's edge going dark red; over it, the veins, kept off the pulse line.
        float level = Mth.lerp(partial, veinsO, veins);
        float throb = 1.0F + THROB * (1.0F - Mth.clamp((now - throbAt) / THROB_TICKS, 0.0F, 1.0F));
        float veinsShown = presence * gaspFade;
        if (veinsShown > 0.0F) renderVignette(graphics, level, throb, veinsShown);
        float alpha = active ? 1.0F : endedAt == Long.MIN_VALUE ? 0.0F : Mth.clamp((endedAt - now) / FADE_TICKS, 0.0F, 1.0F);
        if (alpha > 0.0F) {
            // The live line in the middle of the screen, where the crosshair would be.
            renderLiveLine(graphics, minecraft.font, screenWidth / 2, height / 2, now, alpha);
            float popIn = Mth.clamp((now - startedAt - PANEL_AT) / POP_IN_TICKS, 0.0F, 1.0F);
            if (popIn > 0.0F && gaspFade > 0.0F) renderBand(graphics, minecraft.font, lineY, now, popIn, alpha * gaspFade);
        }
        if (veinsShown > 0.0F) renderVeins(graphics, level, throb, veinsShown, pulseY);
    }

    // The screen's edge going dark red, deeper the further the veins have come, throbbing at each beat.
    private static void renderVignette(GuiGraphicsExtractor graphics, float level, float throb, float presence) {
        float edge = Math.min(1.0F, (0.35F + 0.2F * level) * throb) * presence;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, VIGNETTE, 0, 0, screenWidth, screenHeight, HudStyle.color(HudStyle.WHITE, edge));
    }

    // The veins creeping in from the screen's edge: the first texture comes in over the first level (a little of it
    // from the start), the second over the second, the third over the third, all throbbing at each beat. They lie over
    // the band, but thin out toward the pulse line at pulseY and leave it clear: drawn in horizontal strips, each a
    // little fainter the nearer it is, gone within VEIN_CLEAR of the line, full again VEIN_FADE further out.
    private static void renderVeins(GuiGraphicsExtractor graphics, float level, float throb, float presence, int pulseY) {
        int width = screenWidth;
        int height = screenHeight;
        int clearTop = pulseY - VEIN_CLEAR;
        // Further clear below, where the get-ready chevrons rise under the window.
        int clearBottom = pulseY + LINE_HEIGHT + VEIN_CLEAR + VEIN_CLEAR_BELOW;
        for (int i = 0; i < VEINS.length; i++) {
            float shown = Mth.clamp(level - i + (i == 0 ? 0.35F : 0.0F), 0.0F, 1.0F);
            if (shown <= 0.0F) continue;
            float veinAlpha = Math.min(1.0F, shown * 0.85F * throb) * presence;
            // Full above and below the fade, then the fade itself in steps toward the line from either side.
            veinStrip(graphics, VEINS[i], 0, clearTop - VEIN_FADE, veinAlpha, width, height);
            veinStrip(graphics, VEINS[i], clearBottom + VEIN_FADE, height, veinAlpha, width, height);
            for (int step = 0; step < VEIN_FADE; step += VEIN_STEP) {
                // Eased, so they thin out quickly at first and are all but gone as they near the line.
                float k = 1.0F - (step + VEIN_STEP / 2.0F) / VEIN_FADE;
                float faded = veinAlpha * k * k;
                int above = clearTop - VEIN_FADE + step;
                int below = clearBottom + VEIN_FADE - step;
                veinStrip(graphics, VEINS[i], above, above + VEIN_STEP, faded, width, height);
                veinStrip(graphics, VEINS[i], below - VEIN_STEP, below, faded, width, height);
            }
        }
    }

    // The whole screen's veins, shown only between y0 and y1.
    private static void veinStrip(GuiGraphicsExtractor graphics, Identifier veins, int y0, int y1, float alpha, int width, int height) {
        if (y1 <= y0 || alpha <= 0.0F || y1 <= 0 || y0 >= height) return;
        graphics.enableScissor(0, Math.max(0, y0), width, Math.min(height, y1));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, veins, 0, 0, width, height, HudStyle.color(HudStyle.WHITE, alpha));
        graphics.disableScissor();
    }

    // The line: slammed in at the start, then popping or shaking at each change and trembling between, outlined dark
    // so it reads over anything.
    private static void renderLiveLine(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY, float now, float alpha) {
        float slam = Mth.clamp((now - startedAt) / SLAM_TICKS, 0.0F, 1.0F);
        float scale = LINE_SCALE;
        float dx = 0.0F;
        float dy = 0.0F;
        float lineAlpha = alpha;
        if (slam < 1.0F) {
            // The warning: from big and faint, down hard onto its place.
            scale = Mth.lerp(Easing.CUBIC_OUT.apply(slam), SLAM_SCALE, LINE_SCALE);
            lineAlpha *= Easing.QUAD_OUT.apply(slam);
        } else if (line.mood != Mood.GOOD) {
            // Bad news, or the warning as it lands: a hard shake, dying away.
            float from = Math.max(lineAt, startedAt + SLAM_TICKS);
            float left = 1.0F - Mth.clamp((now - from) / LINE_TICKS, 0.0F, 1.0F);
            float hard = SHAKE * left * left;
            dx = Mth.sin(now * 3.1F) * hard;
            dy = Mth.cos(now * 2.3F) * hard * 0.6F;
        } else {
            // Good news: it swells and settles.
            float change = Mth.clamp((now - lineAt) / LINE_TICKS, 0.0F, 1.0F);
            scale *= 1.0F + POP_SWELL * (1.0F - Easing.BACK_OUT.apply(change));
        }
        // And, while it is on, always a little tremble.
        if (active) {
            dx += (Mth.sin(now * 1.7F) + Mth.sin(now * 4.3F) * 0.5F) * TREMBLE;
            dy += Mth.cos(now * 2.1F) * TREMBLE * 0.7F;
        }
        Component text = line.text();
        int color = HudStyle.color(line.color(), lineAlpha);
        int outline = HudStyle.color(OUTLINE, lineAlpha * 0.85F);
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX + dx, centerY + dy);
        graphics.pose().scale(scale, scale);
        int x = -font.width(text) / 2;
        int y = -4;
        graphics.text(font, text, x - 1, y, outline, false);
        graphics.text(font, text, x + 1, y, outline, false);
        graphics.text(font, text, x, y - 1, outline, false);
        graphics.text(font, text, x, y + 1, outline, false);
        graphics.text(font, text, x, y, color, false);
        graphics.pose().popMatrix();
    }

    // The band, across the whole screen and billowing open from its middle line: layers of smoke drifting past each
    // other, the pulse line through them, the mouse over its calm window, what to do under it, and the hearts at the
    // stack's top, textY. A strike gives it a jolt.
    // The least width it is laid out in: MIN_WIDTH, or more for a long hint (a translation), its rule and the line's
    // ends past it.
    private static int minWidth(Font font) {
        return Math.max(MIN_WIDTH, font.width(Component.translatable("gui.whycantisleep.minigame.hint")) + RULE_REACH * 2 + SIDE_MIN * 2);
    }

    private static void renderBand(GuiGraphicsExtractor graphics, Font font, int textY, float now, float popIn, float alpha) {
        // Down the middle, from the stack's top: the hearts in their frame, a gap, the mouse in its frame, and the
        // window of the pulse line, whose middle is the band's.
        int heartsY = textY + HEARTS_BELOW_TEXT;
        int mouseY = heartsY + HEART_SIZE + ICON_PAD * 2 + ICON_GAP + ICON_PAD;
        int centerY = textY + TEXT_TO_BAND;
        float flash = 1.0F - Mth.clamp((now - flashAt) / FLASH_TICKS, 0.0F, 1.0F);
        boolean strikeFlash = !flashGood && flash > 0.0F;
        int shake = strikeFlash ? Math.round(Mth.sin(now * 2.5F) * 2.0F * flash) : 0;
        int width = screenWidth;
        float bandAlpha = alpha * Easing.QUAD_OUT.apply(popIn);
        // It opens from its middle line, and then breathes: swelling and settling a little, slowly.
        float open = Mth.lerp(Easing.BACK_OUT.apply(popIn), 0.2F, 1.0F) * (1.0F + SMOKE_SWELL * Mth.sin(now * SMOKE_SWELL_SPEED));
        int smokeY = centerY - SMOKE_HEIGHT / 2;
        for (int layer = 0; layer < SMOKE.length; layer++) {
            // Each layer laid side by side across the screen, slid along by its drift, and bobbing on its own, so the
            // edges never sit still; the wisps thin and thicken as they go.
            float bob = SMOKE_BOB[layer] * Mth.sin(now * SMOKE_BOB_SPEED[layer] + layer * 2.1F);
            float layerAlpha = layer == SMOKE.length - 1 ? bandAlpha * (0.7F + 0.3F * Mth.sin(now * 0.13F)) : bandAlpha;
            graphics.pose().pushMatrix();
            graphics.pose().translate(0.0F, centerY + bob);
            graphics.pose().scale(1.0F, open);
            graphics.pose().translate(0.0F, -centerY);
            int offset = Math.floorMod(Math.round(now * SMOKE_DRIFT[layer]), SMOKE_WIDTH);
            int tint = HudStyle.color(HudStyle.WHITE, layerAlpha);
            for (int x = offset - SMOKE_WIDTH; x < width; x += SMOKE_WIDTH) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SMOKE[layer], x, smokeY, SMOKE_WIDTH, SMOKE_HEIGHT, tint);
            }
            graphics.pose().popMatrix();
        }

        // The pulse line across the band, its calm window in the middle of the screen; the mouse just over the window,
        // and the hearts higher, under the live line.
        int side = Math.max(SIDE_MIN, Math.round(width * SIDE_SHARE));
        float along = active && beatAt >= 0 ? (now - beatAt) / BEAT_TICKS : -1.0F;
        boolean inWindow = along >= 0.0F && inWindow(along);
        int lineX = side + shake;
        int lineWidth = width - side * 2;
        int lineY = centerY - LINE_HEIGHT / 2;
        renderLine(graphics, lineX, lineY, lineWidth, along, now, flash, strikeFlash, inWindow, bandAlpha);
        int mouseX = (width - MOUSE_SIZE) / 2 + shake;
        int heartsX = (width - HEARTS_WIDTH) / 2;
        iconFrame(graphics, mouseX, mouseY, MOUSE_SIZE, MOUSE_SIZE, CALM, inWindow, bandAlpha);
        iconFrame(graphics, heartsX, heartsY, HEARTS_WIDTH, HEART_SIZE, strikeFlash ? HudStyle.RED : CALM, strikeFlash, bandAlpha);
        renderMouse(graphics, mouseX, mouseY, now, inWindow, bandAlpha);
        renderHearts(graphics, heartsX, heartsY, now, flash, strikeFlash, bandAlpha);

        Component hint = Component.translatable("gui.whycantisleep.minigame.hint");
        int hintWidth = font.width(hint);
        int hintY = centerY + HINT_BELOW;
        graphics.text(font, hint, (width - hintWidth) / 2, hintY, HudStyle.color(HINT, bandAlpha), false);
        // A soft rule under it, brightest in the middle and fading out at both ends.
        int ruleHalf = hintWidth / 2 + RULE_REACH;
        for (int dx = -ruleHalf; dx < ruleHalf; dx++) {
            float k = 1.0F - Math.abs(dx + 0.5F) / ruleHalf;
            graphics.fill(width / 2 + dx, hintY + RULE_BELOW, width / 2 + dx + 1, hintY + RULE_BELOW + 1, HudStyle.color(HINT, bandAlpha * 0.6F * k * k));
        }
        // The echo last, free to spread over the band and past it.
        renderEcho(graphics, lineX, lineY, lineWidth, now, bandAlpha);
    }

    // The mouse: it swells at a click and shows its left button pressed in (darker along its top edge, lit below, the
    // mouse a pixel lower), breathes a little while a beat is in the window with its button lit, and rests dim.
    private static void renderMouse(GuiGraphicsExtractor graphics, int x, int y, float now, boolean inWindow, float alpha) {
        float press = Mth.clamp((now - pressedAt) / PRESS_TICKS, 0.0F, 1.0F);
        boolean pressing = press < 1.0F;
        float swell = pressing ? PRESS_SWELL * Mth.sin(Easing.QUAD_OUT.apply(press) * Mth.PI) : 0.0F;
        if (inWindow && !pressing) swell += HINT_SWELL * (0.5F + 0.5F * Mth.sin(now * 0.9F));
        float centerX = x + MOUSE_SIZE / 2.0F;
        float centerY = y + MOUSE_SIZE / 2.0F + (pressing ? 1.0F : 0.0F);
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(1.0F + swell, 1.0F + swell);
        graphics.pose().translate(-MOUSE_SIZE / 2.0F, -MOUSE_SIZE / 2.0F);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, MOUSE, 0, 0, MOUSE_SIZE, MOUSE_SIZE, HudStyle.color(HudStyle.WHITE, alpha));
        if (!gasped) {
            float lit = pressing ? 1.0F : inWindow ? 0.85F : 0.3F;
            int glow = HudStyle.color(CALM, alpha * lit);
            if (pressing) {
                // Pressed in: the top of the button in shadow, the rest lit.
                graphics.fill(BUTTON_X, BUTTON_Y, BUTTON_X + 3, BUTTON_Y + 1, HudStyle.color(CALM_DARK, alpha));
                graphics.fill(BUTTON_X, BUTTON_Y + 1, BUTTON_X + 3, BUTTON_Y + 2, glow);
            } else {
                graphics.fill(BUTTON_X, BUTTON_Y, BUTTON_X + 3, BUTTON_Y + 2, glow);
            }
            graphics.fill(BUTTON_X, BUTTON_Y + 2, BUTTON_X + 2, BUTTON_Y + 4, glow);
        }
        graphics.pose().popMatrix();
    }

    // Three of the game's hearts, one broken (an empty container) for each strike; the one just broken blinks and
    // trembles, and at a hit the whole ones give a beat.
    private static void renderHearts(GuiGraphicsExtractor graphics, int x, int y, float now, float flash, boolean strikeFlash, float alpha) {
        int whole = STRIKES - strikes;
        int tint = HudStyle.color(HudStyle.WHITE, alpha);
        float beat = flashGood && flash > 0.0F ? 0.25F * Mth.sin((1.0F - flash) * Mth.PI) : 0.0F;
        for (int i = 0; i < STRIKES; i++) {
            int heartX = x + i * HEART_STEP;
            boolean broken = i >= whole;
            boolean justBroken = strikeFlash && i == whole;
            int dx = justBroken ? Math.round(Mth.sin(now * 3.0F) * flash) : 0;
            graphics.pose().pushMatrix();
            graphics.pose().translate(heartX + dx + HEART_SIZE / 2.0F, y + HEART_SIZE / 2.0F);
            float scale = broken ? 1.0F : 1.0F + beat;
            graphics.pose().scale(scale, scale);
            graphics.pose().translate(-HEART_SIZE / 2.0F, -HEART_SIZE / 2.0F);
            boolean blink = justBroken && ((int) (now / 2.0F)) % 2 == 0;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, blink ? HEART_EMPTY_BLINKING : HEART_EMPTY, 0, 0, HEART_SIZE, HEART_SIZE, tint);
            if (!broken) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART, 0, 0, HEART_SIZE, HEART_SIZE, tint);
            else if (blink) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_BLINKING, 0, 0, HEART_SIZE, HEART_SIZE, tint);
            graphics.pose().popMatrix();
        }
        // The heart just broken pops out of its place: a whole heart swelling and fading away over it.
        float pop = (now - brokenAt) / POP_TICKS;
        if (pop >= 0.0F && pop < 1.0F) {
            float eased = Easing.QUAD_OUT.apply(pop);
            float scale = 1.0F + 1.2F * eased;
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + brokenHeart * HEART_STEP + HEART_SIZE / 2.0F, y + HEART_SIZE / 2.0F);
            graphics.pose().scale(scale, scale);
            graphics.pose().translate(-HEART_SIZE / 2.0F, -HEART_SIZE / 2.0F);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART, 0, 0, HEART_SIZE, HEART_SIZE, HudStyle.color(HudStyle.WHITE, alpha * (1.0F - eased)));
            graphics.pose().popMatrix();
        }
    }

    // A caught beat's echo: the beat's own shape swelling out from where it was caught, wider and taller, and a ring of
    // light opening round its peak, both fading; free to spread past the line, over the panel and beyond.
    private static void renderEcho(GuiGraphicsExtractor graphics, int x, int y, int width, float now, float alpha) {
        float echo = (now - hitAt) / ECHO_TICKS;
        if (echo < 0.0F || echo >= 1.0F) return;
        float eased = Easing.QUAD_OUT.apply(echo);
        float fade = alpha * (1.0F - eased) * (1.0F - eased);
        int color = HudStyle.color(GLOW, fade * 0.8F);
        float peakX = x + (width - 1) * (1.0F - hitAlong);
        float rest = y + REST;
        float wide = 1.0F + 1.2F * eased;
        float tall = 1.0F + 1.5F * eased;
        for (int i = 1; i < BEAT.length; i++) {
            float fromX = peakX + (i - 1 - BEAT_PEAK) * wide;
            float fromY = rest + BEAT[i - 1] * tall;
            float toX = peakX + (i - BEAT_PEAK) * wide;
            float toY = rest + BEAT[i] * tall;
            int steps = Math.max(1, Mth.ceil(Math.max(Math.abs(toX - fromX), Math.abs(toY - fromY))));
            for (int step = 0; step <= steps; step++) {
                int px = Math.round(Mth.lerp(step / (float) steps, fromX, toX));
                int py = Math.round(Mth.lerp(step / (float) steps, fromY, toY));
                graphics.fill(px, py, px + 1, py + 1, color);
            }
        }
        // The ring: a square of light with its corners left out, opening round the peak.
        int half = Math.round(2 + 8 * eased);
        int cx = Math.round(peakX);
        int cy = Math.round(rest + BEAT[BEAT_PEAK] / 2.0F);
        int ring = HudStyle.color(CALM, fade);
        graphics.fill(cx - half + 1, cy - half, cx + half, cy - half + 1, ring);
        graphics.fill(cx - half + 1, cy + half, cx + half, cy + half + 1, ring);
        graphics.fill(cx - half, cy - half + 1, cx - half + 1, cy + half, ring);
        graphics.fill(cx + half, cy - half + 1, cx + half + 1, cy + half, ring);
    }

    // The pulse line: the calm window in its middle, the rest line across it, and the beat crossing it from right to
    // left, drawn a column at a time and joined up so it reads as one line; brighter about the beat. A hit lights the
    // window, a strike washes the line red; gasped, it lies flat and red.
    private static void renderLine(GuiGraphicsExtractor graphics, int x, int y, int width, float along, float now, float flash, boolean strikeFlash,
                                   boolean inWindow, float alpha) {
        // The calm window: a frame with rounded corners that breathes while a beat is in it, swelling and glowing.
        int zoneWidth = Math.round(width * ZONE);
        int zoneX = x + (width - zoneWidth) / 2;
        float breath = inWindow ? 0.5F + 0.5F * Mth.sin(now * BREATH_SPEED) : 0.0F;
        int grow = inWindow ? 1 + Math.round(breath) : 0;
        float glow = 0.1F + (inWindow ? 0.12F + 0.14F * breath : 0.0F) + (flashGood ? 0.4F * flash : 0.0F);
        float edge = inWindow ? 0.7F + 0.3F * breath : 0.45F;
        roundedFrame(graphics, zoneX - grow, y - ZONE_RISE - grow, zoneX + zoneWidth + grow, y + LINE_HEIGHT + ZONE_RISE + grow, HudStyle.color(CALM, alpha * glow),
                HudStyle.color(CALM, alpha * edge));

        // Get ready: as the spike comes up on the window, arrows light under it, rising toward it, and its bottom edge
        // brightens.
        float ready = along < 0.0F || inWindow ? 0.0F : Mth.clamp((along - (WINDOW_START - READY_LEAD)) / READY_LEAD, 0.0F, 1.0F);
        if (ready > 0.0F && along < WINDOW_START) {
            int cx = zoneX + zoneWidth / 2;
            int bottom = y + LINE_HEIGHT + ZONE_RISE;
            int slide = Math.round((1.0F - ready) * 2.0F);
            // Two chevrons pointing up at the window, ARROW_HALF wide either way and two pixels thick, each over a dark
            // outline so they read against the band; the nearer one bright, the further one coral.
            for (int k = 0; k < 2; k++) {
                int tip = bottom + 2 + k * ARROW_STEP + slide;
                float shown = alpha * ready * (k == 0 ? 1.0F : 0.7F);
                int arrow = HudStyle.color(k == 0 ? GLOW : CALM, shown);
                int outline = HudStyle.color(OUTLINE, shown * 0.8F);
                for (int d = -ARROW_HALF; d <= ARROW_HALF; d++) {
                    int py = tip + Math.abs(d);
                    graphics.fill(cx + d - 1, py - 1, cx + d + 2, py + 3, outline);
                }
                for (int d = -ARROW_HALF; d <= ARROW_HALF; d++) {
                    int py = tip + Math.abs(d);
                    graphics.fill(cx + d, py, cx + d + 1, py + 2, arrow);
                }
            }
            graphics.fill(zoneX + 2, bottom - 1, zoneX + zoneWidth - 2, bottom, HudStyle.color(GLOW, alpha * ready));
        }

        int lineRgb = gasped || strikeFlash ? HudStyle.RED : CALM;
        int peak = along >= 0.0F ? Math.round((width - 1) * (1.0F - Mth.clamp(along, 0.0F, 1.0F))) : Integer.MIN_VALUE;
        int rest = y + REST;
        // The trace runs right to left as fast as a beat does, so its small waves and the spike move as one.
        float scroll = now * (width - 1) / BEAT_TICKS;
        int last = rest + (gasped ? 0 : trace(scroll));
        for (int column = 0; column < width; column++) {
            int fromPeak = column - peak + BEAT_PEAK;
            boolean spike = !gasped && peak != Integer.MIN_VALUE && fromPeak >= 0 && fromPeak < BEAT.length;
            int offset = gasped ? 0 : spike ? BEAT[fromPeak] : trace(column + scroll);
            int at = rest + offset;
            boolean near = peak != Integer.MIN_VALUE && Math.abs(column - peak) <= 3;
            // Fading in from nothing at both ends, eased, so the beat comes out of the smoke and goes back into it.
            float fromEnd = Math.min(column, width - 1 - column) / (width * LINE_FADE_SHARE);
            float fade = fromEnd >= 1.0F ? 1.0F : fromEnd * fromEnd * (3.0F - 2.0F * fromEnd);
            int color = near ? HudStyle.color(GLOW, alpha * fade) : HudStyle.color(lineRgb, alpha * 0.75F * fade);
            graphics.fill(x + column, Math.min(last, at), x + column + 1, Math.max(last, at) + 1, color);
            last = at;
        }
    }

    // The resting heart's trace at a point along it: a slow sway, a small rounded bump every TRACE_BUMP columns and a
    // quick little flick every TRACE_FLICK, never more than three pixels off the rest line, so the spike stands out.
    private static int trace(float at) {
        float sway = Mth.sin(at * 0.21F) * 0.6F + Mth.sin(at * 0.057F) * 0.9F;
        int bump = Math.floorMod((int) Math.floor(at), TRACE_BUMP);
        int flick = Math.floorMod((int) Math.floor(at), TRACE_FLICK);
        int extra = bump < BUMP.length ? BUMP[bump] : flick < FLICK.length ? FLICK[flick] : 0;
        return Mth.clamp(Math.round(sway) + extra, -3, 3);
    }

    // A small dark frame round an icon of this size at (x, y), ICON_PAD out from it, in the window's rounded style; its
    // edge brightens while lit (the mouse's while a beat is in the window, the hearts' red at a strike).
    private static void iconFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int edgeRgb, boolean lit, float alpha) {
        roundedFrame(graphics, x - ICON_PAD, y - ICON_PAD, x + width + ICON_PAD, y + height + ICON_PAD, HudStyle.color(OUTLINE, alpha * 0.75F),
                HudStyle.color(edgeRgb, alpha * (lit ? 0.8F : 0.4F)));
    }

    // A filled frame from (x0, y0) to (x1, y1) whose corners are rounded by leaving out their pixels: the edge a pixel
    // wide, its corners cut by two, the fill inside it cut by one.
    private static void roundedFrame(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int fill, int edge) {
        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill);
        graphics.fill(x0 + 2, y0, x1 - 2, y0 + 1, edge);
        graphics.fill(x0 + 2, y1 - 1, x1 - 2, y1, edge);
        graphics.fill(x0, y0 + 2, x0 + 1, y1 - 2, edge);
        graphics.fill(x1 - 1, y0 + 2, x1, y1 - 2, edge);
        graphics.fill(x0 + 1, y0 + 1, x0 + 2, y0 + 2, edge);
        graphics.fill(x1 - 2, y0 + 1, x1 - 1, y0 + 2, edge);
        graphics.fill(x0 + 1, y1 - 2, x0 + 2, y1 - 1, edge);
        graphics.fill(x1 - 2, y1 - 2, x1 - 1, y1 - 1, edge);
    }
}
