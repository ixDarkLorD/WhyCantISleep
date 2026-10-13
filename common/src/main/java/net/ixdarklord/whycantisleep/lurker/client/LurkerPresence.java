package net.ixdarklord.whycantisleep.lurker.client;

import net.ixdarklord.coolcatcanvas.api.client.camera.CameraShake;
import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffect;
import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffects;
import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.lurker.LurkerEntity;
import net.ixdarklord.whycantisleep.sound.WCISSounds;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.jetbrains.annotations.Nullable;

/**
 * What a Lurker being near does to the player's screen, ears and view, all through the {@code whycantisleep:dread}
 * screen effect and the camera.
 * <ul>
 *     <li><b>Watched</b>: while the player's own Lurker stands and watches, their world goes dark but for one light,
 *     which falls from where the Lurker is: on the screen where it stands, or from the edge of the view on its side
 *     when it is out of sight. Looking for the light is looking at the Lurker, and that starts its hunt.</li>
 *     <li><b>Lured</b>: a Lurker that was ignored before draws the player's view slowly round towards it. Turning the
 *     other way holds it off.</li>
 *     <li><b>Near</b>: the nearer any Lurker is (a hallucination too), the more color drains and the edges of the view
 *     close in; while it is closing the distance a heartbeat quickens, each beat flushing the edges red and jolting
 *     the camera.</li>
 * </ul>
 * Nothing here happens with the "Scary content" option off.
 */
public final class LurkerPresence {
    // Beyond this many blocks it is not felt at all; within NEAR it is felt in full.
    private static final double RANGE = 24.0;
    private static final double NEAR = 4.0;
    // Blocks a tick it must close by to count as coming at full speed.
    private static final float CHARGE_SPEED = 0.25F;
    // Degrees a tick the view is drawn round, for each time it was ignored beyond the first; and the most it ever is.
    private static final float PULL_PER_SNUB = 0.55F;
    private static final float MAX_PULL = 1.9F;
    private static final String PULSE = "Pulse";
    private static final String APPROACH = "Approach";
    private static final String DARK = "Dark";
    private static final String LIGHT = "Light";

    // How strongly it is felt, how fast it is coming, the last beat's flush and how dark the world has gone, each 0 to 1
    // and eased from tick to tick.
    private static float presence;
    private static float approach;
    private static float pulse;
    private static float dark;
    private static double lastDistance = RANGE;
    private static int beatWait;
    // The player's own Lurker while it only watches, and where its light last fell on the screen.
    private static @Nullable LurkerEntity watcher;
    private static float lightX = 0.5F;
    private static float lightY = 0.5F;

    private LurkerPresence() {
    }

    public static void init() {
        ScreenEffect effect = ScreenEffects.register(WhyCantISleep.id("dread"), WhyCantISleep.id("dread"))
                .selectable(false)
                .fade(15, 30, Easing.SINE_IN_OUT)
                .activeWhen(context -> presence > 0.02F || dark > 0.02F)
                .strength(context -> Math.max(presence, dark));
        effect.bindUniform(PULSE, context -> pulse);
        effect.bindUniform(APPROACH, context -> approach);
        effect.bindUniform(DARK, context -> dark);
        effect.bindUniform(LIGHT, (context, out) -> {
            aimLight();
            out[0] = lightX;
            out[1] = lightY;
        });
        ClientTickEvents.END.register(LurkerPresence::tick);
        ClientGuiEvents.RENDER_HUD.register(LurkerPresence::pull);
        ClientPlayerEvents.LEAVE.register(player -> reset());
    }

    private static void reset() {
        presence = approach = pulse = dark = 0.0F;
        lastDistance = RANGE;
        watcher = null;
    }

    private static void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || !WCISClientConfig.SCARY_CONTENT.get()) {
            reset();
            return;
        }
        if (minecraft.isPaused()) return;
        double distance = RANGE;
        LurkerEntity watching = null;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof LurkerEntity lurker) || !lurker.isAlive()) continue;
            if (!lurker.isHallucination() && !lurker.isHunting() && lurker.isAfter(minecraft.player.getId())) {
                // It only watches: it is not felt as something near, it is the dark and the light in it.
                watching = lurker;
            } else {
                distance = Math.min(distance, lurker.distanceTo(minecraft.player));
            }
        }
        watcher = watching;
        dark += ((watching != null ? 1.0F : 0.0F) - dark) * (watching != null ? 0.06F : 0.1F);

        float felt = distance >= RANGE ? 0.0F : 1.0F - (float) Mth.clamp((distance - NEAR) / (RANGE - NEAR), 0.0, 1.0);
        // Coming closer counts; standing still or backing off lets it ebb.
        float closing = felt <= 0.0F ? 0.0F : Mth.clamp((float) (lastDistance - distance) / CHARGE_SPEED, 0.0F, 1.0F) * Mth.clamp(felt * 1.5F, 0.0F, 1.0F);
        lastDistance = distance;
        presence += (felt - presence) * 0.12F;
        approach += (closing - approach) * (closing > approach ? 0.3F : 0.06F);
        pulse *= 0.82F;

        // A heartbeat, faster the nearer it is; with it coming, each beat is seen and felt as well as heard. Watched,
        // the heart beats slow and steady.
        float beat = Math.max(presence, dark * 0.3F);
        if (beat > 0.25F && --beatWait <= 0) {
            beatWait = Math.round(Mth.lerp(beat, 34.0F, 11.0F));
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(WCISSounds.BLOCKED, 0.9F + beat * 0.3F, 0.35F + beat * 0.55F));
            pulse = Math.max(pulse, 0.35F * presence + 0.65F * approach);
            if (approach > 0.3F && WCISConfigs.isEnabled(WCISClientConfig.SCREEN_SHAKE)) CameraShake.shake(0.9F * approach, 7);
        }
    }

    // Where the watcher's light falls on the screen, each frame: 0 to 1 across and up it, and beyond for a Lurker out of
    // view, so the light spills in from that edge.
    private static void aimLight() {
        LurkerEntity lurker = watcher;
        Minecraft minecraft = Minecraft.getInstance();
        if (lurker == null || minecraft.getWindow().getHeight() == 0) return;
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 to = lurker.getEyePosition().subtract(camera.position());
        Vector3fc forward = camera.forwardVector();
        Vector3fc up = camera.upVector();
        Vector3fc left = camera.leftVector();
        double ahead = to.x * forward.x() + to.y * forward.y() + to.z * forward.z();
        double right = -(to.x * left.x() + to.y * left.y() + to.z * left.z());
        double above = to.x * up.x() + to.y * up.y() + to.z * up.z();
        double tanV = Math.tan(Math.toRadians(camera.getFov()) / 2.0);
        double tanH = tanV * minecraft.getWindow().getWidth() / minecraft.getWindow().getHeight();
        double x;
        double y;
        if (ahead > 0.05) {
            x = Mth.clamp(right / (ahead * tanH), -1.6, 1.6);
            y = Mth.clamp(above / (ahead * tanV), -1.6, 1.6);
        } else {
            // Behind the player: from the edge on the side it stands, or from below when it is straight behind.
            double length = Math.sqrt(right * right + above * above);
            x = length < 1.0E-4 ? 0.0 : right / length * 1.6;
            y = length < 1.0E-4 ? -1.6 : above / length * 1.6;
        }
        lightX = (float) (0.5 + x * 0.5);
        lightY = (float) (0.5 + y * 0.5);
    }

    /**
     * Each frame: a Lurker that was ignored before draws the player's view round towards it, faster for each time it
     * was ignored. It is slow enough that turning the other way holds it off.
     */
    private static void pull(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        LurkerEntity lurker = watcher;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (lurker == null || player == null || minecraft.isPaused() || lurker.lure() < LurkerEntity.LURE_FROM) return;
        float rate = Math.min(MAX_PULL, PULL_PER_SNUB * (lurker.lure() - LurkerEntity.LURE_FROM + 1)) * delta.getGameTimeDeltaTicks() * dark;
        Vec3 to = lurker.getEyePosition().subtract(player.getEyePosition());
        float wantedYaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F;
        float wantedPitch = (float) -(Mth.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * Mth.RAD_TO_DEG);
        float yaw = Mth.clamp(Mth.wrapDegrees(wantedYaw - player.getYRot()), -rate, rate);
        float pitch = Mth.clamp(wantedPitch - player.getXRot(), -rate * 0.5F, rate * 0.5F);
        // Turned as the mouse turns it: the last tick's angles move too, so nothing is smoothed back.
        player.setYRot(player.getYRot() + yaw);
        player.setXRot(Mth.clamp(player.getXRot() + pitch, -90.0F, 90.0F));
        player.yRotO += yaw;
        player.xRotO = Mth.clamp(player.xRotO + pitch, -90.0F, 90.0F);
    }
}
