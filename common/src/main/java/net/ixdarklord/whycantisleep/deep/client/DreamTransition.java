package net.ixdarklord.whycantisleep.deep.client;

import net.ixdarklord.coolcatcanvas.api.client.camera.CameraShake;
import net.ixdarklord.coolcatcanvas.api.client.cutscene.Cutscenes;
import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffect;
import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffectScope;
import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffects;
import net.ixdarklord.coolcatcanvas.api.cutscene.Cutscene;
import net.ixdarklord.coolcatcanvas.api.cutscene.Interpolation;
import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.deep.LucidDeep;
import net.ixdarklord.whycantisleep.network.DreamReadyPayload;
import net.ixdarklord.whycantisleep.network.DreamTransitionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Going under, as the dreamer sees it, watched from above:
 * <ol>
 *     <li><b>Slipping away.</b> The view leaves them: from just over the bed, looking down at them asleep, it rises
 *     smoothly away, turning a little, so they fall away from it into the dark. As it rises the picture warps, and
 *     black smoke and tentacles creep in from the edges of the screen until there is nothing left but black (the
 *     {@code whycantisleep:slip} screen effect).</li>
 *     <li><b>Black.</b> The screen stays black (over everything, the loading screen included) while the Deep loads,
 *     so there is no loading screen to see. They hang in its sky until their screen is ready
 *     ({@link DreamReadyPayload}).</li>
 *     <li><b>The fall.</b> The black lifts on them falling out of the sky face down, arms and legs flailing, seen from a
 *     camera falling with them a few blocks above. They hit the island and lie spread out on their face; the view
 *     shakes.</li>
 *     <li><b>Waking up there.</b> They pick themselves up facing the rift. The camera, in front of them, turns slowly
 *     to look up at the rift, and comes back to them and into their eyes, while they say what they are thinking.</li>
 * </ol>
 * The server says when the first part starts ({@link DreamTransitionPayload}); everything here is client only. The
 * falling and lying poses are put on the player by {@code PlayerMixin} and {@code PlayerModelMixin}.
 */
public final class DreamTransition {
    // The pull away from the bed, and when in it the screen goes wholly black (it fades to black over its last part).
    private static final int DRIFT_TICKS = 58;
    private static final int BLACK_AT = 52;
    // How long the black lingers after the Deep is there, and how long it takes to lift.
    private static final int SETTLE_TICKS = 4;
    private static final int LIFT_TICKS = 24;
    // If the Deep never comes (the server changed its mind), the black lifts anyway after this long.
    private static final int GIVE_UP_TICKS = 200;
    // The camera that watches the fall: this far above them, looking straight down. It lingers this long on them
    // lying there, then comes back down into their eyes over the last part of that.
    private static final double CAMERA_ABOVE = 6.5;
    // How long they lie there after the fall before they get up, in the scene that follows it.
    private static final int LIE_TICKS = 26;
    // How a falling player moves (the game's own numbers), to know where the camera must be at each moment.
    private static final double GRAVITY = 0.08;
    private static final double DRAG = 0.98;

    private enum Stage { NONE, DRIFTING, BLACK, FALLING, LYING }

    // When the smoke and tentacles start to creep in, and when they have covered the screen.
    private static final int SLIP_FROM = 6;
    private static final int SLIP_TO = BLACK_AT;

    private static ScreenEffect cover;
    private static ScreenEffect slip;
    private static float slipped;
    private static Stage stage = Stage.NONE;
    private static int ticks;

    private DreamTransition() {
    }

    public static void init() {
        cover = ScreenEffects.tint(WhyCantISleep.id("dream_cover"), 0xFF000000)
                .scope(ScreenEffectScope.SCREEN)
                .selectable(false)
                .fade(0, LIFT_TICKS, Easing.SINE_IN_OUT);
        slip = ScreenEffects.register(WhyCantISleep.id("slip"), WhyCantISleep.id("slip"))
                .scope(ScreenEffectScope.SCREEN)
                .selectable(false)
                .fade(0, 0, Easing.LINEAR)
                .activeWhen(context -> stage == Stage.DRIFTING)
                .strength(context -> slipped);
        ClientTickEvents.END.register(DreamTransition::tick);
        ClientPlayerEvents.LEAVE.register(player -> reset());
    }

    /** Whether the view is pulling away from them in bed, or the screen is black. */
    public static boolean isDrifting() {
        return stage == Stage.DRIFTING || stage == Stage.BLACK;
    }

    /** Whether the player is falling or lying face down: their body is laid flat. */
    public static boolean isLyingFaceDown() {
        return stage == Stage.FALLING || stage == Stage.LYING;
    }

    /** Whether they are falling: their arms and legs flail. */
    public static boolean isFalling() {
        return stage == Stage.FALLING;
    }

    /** Whether they are lying where they landed: spread out on their face. */
    public static boolean isSprawled() {
        return stage == Stage.LYING;
    }

    public static void on(DreamTransitionPayload payload) {
        if (payload.stage() == DreamTransitionPayload.DRIFT) {
            drift(new Vec3(payload.x(), payload.y(), payload.z()), payload.yaw());
        } else if (payload.stage() == DreamTransitionPayload.CANCEL) {
            Cutscenes.stop();
            reset();
        }
    }

    // The view over the sleeper, looking down at them with their head to the top, pulls away upwards and turns.
    private static void drift(Vec3 eye, float yaw) {
        // The bed's "Leave Bed" screen goes away without waking them (closing it the usual way would).
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) minecraft.setScreen(null);
        // One long, even rise, the keyframes on a smooth curve through each other, gathering pace gently.
        Cutscenes.play(Cutscene.builder()
                .keyframe(0, eye.add(0.0, 1.9, 0.0), yaw, 90)
                .keyframe(20, eye.add(0.0, 2.6, 0.0), yaw + 3, 90).roll(2).easing(Easing.SINE_IN_OUT).interpolation(Interpolation.SMOOTH)
                .keyframe(40, eye.add(0.0, 4.6, 0.0), yaw + 9, 90).roll(6).easing(Easing.SINE_IN_OUT).interpolation(Interpolation.SMOOTH)
                .keyframe(DRIFT_TICKS, eye.add(0.0, 8.5, 0.0), yaw + 16, 90).roll(11).easing(Easing.SINE_IN_OUT).interpolation(Interpolation.SMOOTH)
                .blendIn(14)
                .hideHud(true)
                .lockInput(true)
                .skippable(false)
                .editable(false)
                .build());
        stage = Stage.DRIFTING;
        ticks = 0;
    }

    // The fall, from where they hang now to the island: the camera keeps the same few blocks above them all the way
    // down (worked out from how they will fall), holds on them lying there, and comes back down into their eyes.
    private static void fall(LocalPlayer player) {
        Vec3 start = player.position();
        float yaw = 180.0F;
        Cutscene.Builder fall = Cutscene.builder();
        double y = start.y;
        double speed = 0.0;
        int tick = 0;
        while (y > LucidDeep.ISLAND_TOP && tick < 400) {
            if (tick % 4 == 0) fall.keyframe(tick, new Vec3(start.x, y + CAMERA_ABOVE, start.z), yaw, 90).easing(Easing.LINEAR);
            y += speed;
            speed = (speed - GRAVITY) * DRAG;
            tick++;
        }
        Vec3 landed = new Vec3(start.x, LucidDeep.ISLAND_TOP, start.z);
        // A little past the landing, in case it comes late: the arrival takes over the moment they hit.
        fall.keyframe(tick + 20, landed.add(0.0, CAMERA_ABOVE, 0.0), yaw, 90).easing(Easing.LINEAR);
        Cutscenes.play(fall
                .hideHud(true)
                .lockInput(true)
                .skippable(false)
                .editable(false)
                .build());
    }

    // After the fall: they lie a moment, then pick themselves up facing the rift. The camera starts out in front of
    // them, between them and the rift, looking back at them; slowly turns and lifts to look up at the rift, showing
    // what they see; holds on it; then comes back round to them and down into their eyes. What they say, shaken, is
    // written along the bottom.
    private static void arrive(LocalPlayer player) {
        Vec3 feet = player.position();
        float faceRift = 180.0F;
        float upToRift = -LucidDeep.WINDOW_ELEVATION;
        // North of them is towards the rift: -z.
        Vec3 front = feet.add(0.0, 1.5, -3.2);
        Vec3 eye = feet.add(0.0, 1.62, 0.0);
        Cutscenes.play(Cutscene.builder()
                .keyframe(0, feet.add(0.0, 4.5, -1.0), 0, 70)
                .keyframe(22, front.add(0.0, 0.5, 0.0), 0, 18).easing(Easing.SINE_IN_OUT).interpolation(Interpolation.SMOOTH)
                .keyframe(46, front, 0, 8).easing(Easing.SINE_IN_OUT).interpolation(Interpolation.SMOOTH)
                .keyframe(100, front.add(0.4, 0.6, 0.5), faceRift, upToRift).easing(Easing.SINE_IN_OUT).interpolation(Interpolation.SMOOTH)
                .keyframe(140, front.add(0.5, 0.7, 0.6), faceRift, upToRift - 2).easing(Easing.SINE_IN_OUT).interpolation(Interpolation.SMOOTH)
                .keyframe(176, feet.add(0.9, 2.0, 1.6), faceRift - 15, -8).easing(Easing.SINE_IN_OUT).interpolation(Interpolation.SMOOTH)
                .keyframe(200, eye, faceRift, upToRift * 0.6F).easing(Easing.SINE_IN_OUT).interpolation(Interpolation.SMOOTH)
                .subtitle(30, 44, Component.translatable("subtitle.whycantisleep.deep.where"))
                .subtitle(84, 50, Component.translatable("subtitle.whycantisleep.deep.me"))
                .subtitle(150, 46, Component.translatable("subtitle.whycantisleep.deep.wake"))
                .blendOut(8)
                .hideHud(true)
                .lockInput(true)
                .skippable(true)
                .editable(false)
                .build());
        // Standing, they face the rift and look up at it: where the camera ends.
        player.setYRot(faceRift);
        player.yRotO = faceRift;
        player.setXRot(upToRift * 0.6F);
        player.xRotO = player.getXRot();
    }
    private static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        ticks++;
        switch (stage) {
            case DRIFTING -> {
                float progress = Mth.clamp((ticks - SLIP_FROM) / (float) (SLIP_TO - SLIP_FROM), 0.0F, 1.0F);
                // Slow to start, then swallowing the screen.
                slipped = progress * progress * (3.0F - 2.0F * progress);
                if (ticks == BLACK_AT) {
                    cover.enableInstantly();
                    stage = Stage.BLACK;
                    ticks = 0;
                }
            }
            case BLACK -> {
                boolean there = player != null && LucidDeep.isDeep(player.level()) && minecraft.screen == null;
                if (there && ticks > SETTLE_TICKS) {
                    cover.disable();
                    Network.sendToServer(new DreamReadyPayload(true));
                    fall(player);
                    stage = Stage.FALLING;
                    ticks = 0;
                } else if (ticks > GIVE_UP_TICKS) {
                    reset();
                }
            }
            case FALLING -> {
                if (player == null || !LucidDeep.isDeep(player.level())) {
                    reset();
                } else if (player.onGround() && ticks > 2) {
                    CameraShake.shake(7.0F, 14);
                    arrive(player);
                    stage = Stage.LYING;
                    ticks = 0;
                }
            }
            case LYING -> {
                if (ticks >= LIE_TICKS) {
                    stage = Stage.NONE;
                }
            }
            default -> {
            }
        }
    }

    private static void reset() {
        stage = Stage.NONE;
        slipped = 0.0F;
        if (cover != null) cover.disableInstantly();
    }
}
