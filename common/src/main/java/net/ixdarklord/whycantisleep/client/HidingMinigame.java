package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.client.hud.HudStyle;
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
 * Keeping their heart slow, for a player who ran into a hiding spot with something hunting them. While monsters are
 * near, beats come one after another: a heartbeat crosses a pulse line from right to left, and a left click as it
 * passes through the calm window in the middle keeps it slow. Clicking too early or too late, letting it pass, or
 * making noise (moving about, swinging the view) is a strike, and breaks a heart; at the third they gasp, as loud as
 * can be, and whatever is near the spot hears them (the server decides, as for any noise). It lasts until the hide
 * time is up. Drawn as a small panel under the crosshair: the game's own mouse, which swells and shows its left button
 * pressed at each click (and lights it while a beat is in the window), what to do, three of the game's hearts (the
 * one a strike breaks pops, swelling and fading), and the pulse line under them (a caught beat leaves an echo that
 * swells out of it, and a ring of light). Each beat is heard coming, a caught one comes deep with a shimmer, a click
 * ticks, a broken heart cracks, and the third gasps ({@link WCISSounds}).
 */
public final class HidingMinigame {
    private static final int STRIKES = 3;
    // A beat: it crosses the line in BEAT_TICKS; the next comes GAP_MIN to GAP_MAX ticks after.
    private static final float BEAT_TICKS = 30.0F;
    private static final int GAP_MIN = 14;
    private static final int GAP_MAX = 34;
    // The calm window, as a share of the line, in its middle.
    private static final float ZONE = 0.24F;
    // Noise past this is a strike (as the hiding card's call to hold still), and must fall back under it to count again.
    private static final float LOUD = 0.12F;
    // How long a hit or a strike lights things up (the panel shakes and a heart blinks at a strike), a click's press
    // lasts, and the panel fades in and out.
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
    // How long the panel stays up after they gasp, to show it.
    private static final int GASPED_TICKS = 40;
    // The panel: its content width, the pulse line's height and where its rest line is in it, and how far under the
    // crosshair it sits.
    private static final int WIDTH = 160;
    private static final int LINE_HEIGHT = 16;
    private static final int REST = 10;
    private static final int BELOW_CROSSHAIR = 28;
    private static final int CALM = 0x4ADE80;
    private static final int CALM_DARK = 0x2F9E57;
    // A heartbeat on the line, by columns from its peak: a little bump, a dip, the tall spike, the drop under the rest
    // line, and a low swell after (up is negative).
    private static final int[] BEAT = {-1, -1, 0, 0, 2, -8, 5, 1, 0, -1, -1, -1};
    private static final int BEAT_PEAK = 5;
    // The game's own mouse (the tutorial toast's), 20 square, at the panel's left; its left button's pixels in it:
    // three across on the top two rows, two across on the two under.
    private static final Identifier MOUSE = Identifier.withDefaultNamespace("toast/mouse");
    private static final int MOUSE_SIZE = 20;
    private static final int MOUSE_GAP = 8;
    private static final int BUTTON_X = 6;
    private static final int BUTTON_Y = 6;
    // The game's own hearts, 9 square, overlapping by one as on the health bar.
    private static final Identifier HEART = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier HEART_BLINKING = Identifier.withDefaultNamespace("hud/heart/full_blinking");
    private static final Identifier HEART_EMPTY = Identifier.withDefaultNamespace("hud/heart/container");
    private static final Identifier HEART_EMPTY_BLINKING = Identifier.withDefaultNamespace("hud/heart/container_blinking");
    private static final int HEART_SIZE = 9;
    private static final int HEART_STEP = 8;

    private static boolean active;
    private static int strikes;
    private static long tick;
    private static long startedAt;
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
    private static boolean clicked;
    private static boolean wasLoud;

    private HidingMinigame() {
    }

    public static void init() {
        ClientTickEvents.END.register(minecraft -> tick++);
        ClientGuiEvents.RENDER_HUD.register(HidingMinigame::render);
    }

    /** They went in hunted: the beats begin. */
    public static void begin() {
        active = true;
        strikes = 0;
        gasped = false;
        startedAt = tick;
        endedAt = Long.MIN_VALUE;
        beatAt = -1;
        nextBeat = tick + GAP_MAX;
        clicked = false;
        wasLoud = false;
    }

    /** They came out, or held out: it ends. */
    public static void end() {
        if (active) endedAt = tick;
        active = false;
    }

    /** A left click while hiding (taken from the game, so it never swings at anything), for the next tick to judge. */
    public static void click() {
        clicked = true;
        if (!active) return;
        pressedAt = tick;
        play(Minecraft.getInstance(), WCISSounds.HEARTBEAT_CLICK, 0.2F, 1.6F);
    }

    /**
     * A tick of it, with the noise being made and whether monsters are near (the beats only come while they are).
     * Returns true on the tick they gasp, for their noise to go to its loudest.
     */
    public static boolean tick(Minecraft minecraft, float noise, boolean danger) {
        boolean pressed = clicked;
        clicked = false;
        if (!active || gasped) return false;
        boolean loud = noise > LOUD;
        boolean struck = loud && !wasLoud;
        wasLoud = loud;
        if (beatAt < 0) {
            if (danger && tick >= nextBeat) {
                beatAt = tick;
                // Each beat is heard coming, faintly: the rhythm to catch.
                play(minecraft, WCISSounds.HEARTBEAT_BEAT, 0.35F, 1.1F);
            }
        } else {
            float along = (tick - beatAt) / BEAT_TICKS;
            if (pressed) {
                if (inWindow(along)) hit(minecraft, along);
                else struck = true;
                nextBeatAfter(minecraft);
            } else if (along > 1.0F) {
                struck = true;
                nextBeatAfter(minecraft);
            }
        }
        if (!struck) return false;
        strikes++;
        flashAt = tick;
        flashGood = false;
        brokenAt = tick;
        brokenHeart = STRIKES - strikes;
        play(minecraft, WCISSounds.HEARTBEAT_HEART_LOST, 0.55F, 0.8F);
        if (strikes < STRIKES) return false;
        gasped = true;
        endedAt = tick + GASPED_TICKS;
        active = false;
        play(minecraft, WCISSounds.HEARTBEAT_GASP, 0.9F, 0.75F);
        return true;
    }

    // Whether a beat this far across the line (0 coming in at the right, 1 gone at the left) is in the calm window.
    private static boolean inWindow(float along) {
        return Math.abs(along - 0.5F) <= ZONE / 2.0F;
    }

    // Caught: the beat comes deep and slow, with a shimmer over it, and leaves an echo where it was.
    private static void hit(Minecraft minecraft, float along) {
        flashAt = tick;
        flashGood = true;
        hitAt = tick;
        hitAlong = along;
        play(minecraft, WCISSounds.HEARTBEAT_BEAT, 0.7F, 0.75F);
        play(minecraft, WCISSounds.HEARTBEAT_CALM, 0.45F, 1.3F);
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

    /** The panel under the crosshair: the mouse at the left; what to do and the hearts on top, the pulse line under. */
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) return;
        float partial = delta.getGameTimeDeltaPartialTick(true);
        float now = tick + partial;
        float alpha = active ? Mth.clamp((now - startedAt) / FADE_TICKS, 0.0F, 1.0F)
                : endedAt == Long.MIN_VALUE ? 0.0F : Mth.clamp((endedAt - now) / FADE_TICKS, 0.0F, 1.0F);
        if (alpha <= 0.0F) return;

        Font font = minecraft.font;
        float flash = 1.0F - Mth.clamp((now - flashAt) / FLASH_TICKS, 0.0F, 1.0F);
        boolean strikeFlash = !flashGood && flash > 0.0F;
        // A strike shakes the panel, briefly.
        int shake = strikeFlash ? Math.round(Mth.sin(now * 2.5F) * 2.0F * flash) : 0;
        int height = 9 + 3 + LINE_HEIGHT;
        int x = (graphics.guiWidth() - WIDTH) / 2 + shake;
        int y = graphics.guiHeight() / 2 + BELOW_CROSSHAIR;
        int accent = gasped ? HudStyle.RED : CALM;
        HudStyle.notice(graphics, x, y, WIDTH, height, alpha);
        HudStyle.wash(graphics, x, y, WIDTH, y + height, 1.0F, accent, alpha, now, startedAt, 1.0F - strikes / (float) STRIKES,
                HudStyle.NOTICE_BORDER);

        float along = active && beatAt >= 0 ? (now - beatAt) / BEAT_TICKS : -1.0F;
        boolean inWindow = along >= 0.0F && inWindow(along);
        renderMouse(graphics, x, y + (height - MOUSE_SIZE) / 2, now, inWindow, alpha);

        int textX = x + MOUSE_SIZE + MOUSE_GAP;
        int textWidth = WIDTH - MOUSE_SIZE - MOUSE_GAP;
        int heartsWidth = HEART_STEP * (STRIKES - 1) + HEART_SIZE;
        Component title = Component.translatable(gasped ? "gui.whycantisleep.hide.gasped" : "gui.whycantisleep.hide.steady");
        graphics.text(font, font.split(title, textWidth - heartsWidth - 6).getFirst(), textX, y, HudStyle.color(gasped ? HudStyle.RED : HudStyle.SILVER, alpha),
                false);
        renderHearts(graphics, x + WIDTH - heartsWidth, y - 1, now, flash, strikeFlash, alpha);
        renderLine(graphics, textX, y + 12, textWidth, along, flash, strikeFlash, alpha);
        renderEcho(graphics, textX, y + 12, textWidth, now, alpha);
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
        int color = HudStyle.color(0xDFFFE9, fade * 0.8F);
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
    private static void renderLine(GuiGraphicsExtractor graphics, int x, int y, int width, float along, float flash, boolean strikeFlash, float alpha) {
        graphics.fill(x, y, x + width, y + LINE_HEIGHT, HudStyle.color(HudStyle.TRACK, alpha * 0.8F));
        int zoneWidth = Math.round(width * ZONE);
        int zoneX = x + (width - zoneWidth) / 2;
        float zoneGlow = 0.12F + (flashGood ? 0.45F * flash : 0.0F);
        graphics.fill(zoneX, y, zoneX + zoneWidth, y + LINE_HEIGHT, HudStyle.color(CALM, alpha * zoneGlow));
        graphics.fill(zoneX, y, zoneX + 1, y + LINE_HEIGHT, HudStyle.color(CALM, alpha * 0.5F));
        graphics.fill(zoneX + zoneWidth - 1, y, zoneX + zoneWidth, y + LINE_HEIGHT, HudStyle.color(CALM, alpha * 0.5F));

        int lineRgb = gasped || strikeFlash ? HudStyle.RED : CALM;
        int peak = along >= 0.0F ? Math.round((width - 1) * (1.0F - Mth.clamp(along, 0.0F, 1.0F))) : Integer.MIN_VALUE;
        int rest = y + REST;
        int last = rest;
        for (int column = 0; column < width; column++) {
            int offset = 0;
            int fromPeak = column - peak + BEAT_PEAK;
            if (!gasped && peak != Integer.MIN_VALUE && fromPeak >= 0 && fromPeak < BEAT.length) offset = BEAT[fromPeak];
            int at = rest + offset;
            boolean near = peak != Integer.MIN_VALUE && Math.abs(column - peak) <= 3;
            int color = near ? HudStyle.color(0xDFFFE9, alpha) : HudStyle.color(lineRgb, alpha * 0.75F);
            graphics.fill(x + column, Math.min(last, at), x + column + 1, Math.max(last, at) + 1, color);
            last = at;
        }
    }
}
