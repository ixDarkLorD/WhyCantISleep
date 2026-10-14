package net.ixdarklord.whycantisleep.deep.client;

import net.ixdarklord.coolcatcanvas.api.client.camera.CameraShake;
import net.ixdarklord.coolcatcanvas.api.client.cutscene.Cutscenes;
import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffect;
import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffectScope;
import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffects;
import net.ixdarklord.coolcatcanvas.api.cutscene.Cutscene;
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
import net.minecraft.world.phys.Vec3;

/**
 * Going under, as the dreamer sees it, watched from above:
 * <ol>
 *     <li><b>Slipping away.</b> The view leaves them: from just over the bed, looking down at them asleep, it pulls
 *     away upwards, faster and faster, turning a little, so they fall away from it into the dark; and it fades to
 *     black.</li>
 *     <li><b>Black.</b> The screen stays black (over everything, the loading screen included) while the Deep loads,
 *     so there is no loading screen to see. They hang in its sky until their screen is ready
 *     ({@link DreamReadyPayload}).</li>
 *     <li><b>The fall.</b> The black lifts on them falling out of the sky face down, arms and legs flailing, seen from a
 *     camera falling with them a few blocks above. They hit the island and lie spread out on their face; the view
 *     shakes, holds on them a moment, then comes back down into their eyes as they get up.</li>
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
    private static final int LIE_TICKS = 34;
    private static final int GET_UP_TICKS = 14;
    // How a falling player moves (the game's own numbers), to know where the camera must be at each moment.
    private static final double GRAVITY = 0.08;
    private static final double DRAG = 0.98;

    private enum Stage { NONE, DRIFTING, BLACK, FALLING, LYING }

    private static ScreenEffect cover;
    private static Stage stage = Stage.NONE;
    private static int ticks;

    private DreamTransition() {
    }

    public static void init() {
        cover = ScreenEffects.tint(WhyCantISleep.id("dream_cover"), 0xFF000000)
                .scope(ScreenEffectScope.SCREEN)
                .selectable(false)
                .fade(0, LIFT_TICKS, Easing.SINE_IN_OUT);
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
        Cutscenes.play(Cutscene.builder()
                .keyframe(0, eye.add(0.0, 1.8, 0.0), yaw, 90)
                .keyframe(20, eye.add(0.0, 2.3, 0.0), yaw + 4, 90).roll(3).easing(Easing.SINE_IN)
                .keyframe(40, eye.add(0.0, 5.0, 0.0), yaw + 12, 90).roll(10).easing(Easing.QUAD_IN)
                .keyframe(DRIFT_TICKS, eye.add(0.0, 18.0, 0.0), yaw + 28, 90).roll(24).easing(Easing.EXPO_IN)
                .fadeOut(24)
                .fadeColor(0x000000)
                .blendIn(10)
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
        fall.keyframe(tick, landed.add(0.0, CAMERA_ABOVE, 0.0), yaw, 90).easing(Easing.LINEAR);
        fall.keyframe(tick + LIE_TICKS - GET_UP_TICKS, landed.add(0.0, CAMERA_ABOVE - 1.0, 0.0), yaw + 6, 90).easing(Easing.SINE_OUT);
        Cutscenes.play(fall
                .blendOut(GET_UP_TICKS)
                .hideHud(true)
                .lockInput(true)
                .skippable(false)
                .editable(false)
                .build());
        // Facing the Window when they are up.
        player.setYRot(yaw);
        player.setXRot(-LucidDeep.WINDOW_ELEVATION * 0.6F);
        player.xRotO = player.getXRot();
        player.yRotO = yaw;
    }

    private static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        ticks++;
        switch (stage) {
            case DRIFTING -> {
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
                    stage = Stage.LYING;
                    ticks = 0;
                }
            }
            case LYING -> {
                if (ticks >= LIE_TICKS - GET_UP_TICKS / 2) {
                    stage = Stage.NONE;
                }
            }
            default -> {
            }
        }
    }

    private static void reset() {
        stage = Stage.NONE;
        if (cover != null) cover.disableInstantly();
    }
}
