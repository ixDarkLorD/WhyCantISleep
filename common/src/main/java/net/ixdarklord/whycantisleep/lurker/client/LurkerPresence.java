package net.ixdarklord.whycantisleep.lurker.client;

import net.ixdarklord.coolcatcanvas.api.client.camera.CameraShake;
import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffect;
import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffects;
import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.sound.WCISSounds;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.lurker.LurkerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * What a Lurker being near does to the player's screen and ears, a real one or a hallucination alike. The nearer it
 * is, the more the {@code whycantisleep:dread} screen effect closes in: color drains, the edges of the view go dark
 * and crawl. While it is closing the distance the view tightens further and splits at the edges, a heartbeat quickens
 * with each block, and each beat flushes the edges red and jolts the camera.
 * <p>
 * Nothing here happens with the "Scary content" option off.
 */
public final class LurkerPresence {
    // Beyond this many blocks it is not felt at all; within NEAR it is felt in full.
    private static final double RANGE = 24.0;
    private static final double NEAR = 4.0;
    // Blocks a tick it must close by to count as coming at full speed.
    private static final float CHARGE_SPEED = 0.25F;
    private static final String PULSE = "Pulse";
    private static final String APPROACH = "Approach";

    // How strongly it is felt, how fast it is coming, and the last beat's flush, each 0 to 1 and eased from tick to tick.
    private static float presence;
    private static float approach;
    private static float pulse;
    private static double lastDistance = RANGE;
    private static int beatWait;

    private LurkerPresence() {
    }

    public static void init() {
        ScreenEffect effect = ScreenEffects.register(WhyCantISleep.id("dread"), WhyCantISleep.id("dread"))
                .selectable(false)
                .fade(15, 30, Easing.SINE_IN_OUT)
                .activeWhen(context -> presence > 0.02F)
                .strength(context -> presence);
        effect.bindUniform(PULSE, context -> pulse);
        effect.bindUniform(APPROACH, context -> approach);
        ClientTickEvents.END.register(LurkerPresence::tick);
        ClientPlayerEvents.LEAVE.register(player -> reset());
    }

    private static void reset() {
        presence = approach = pulse = 0.0F;
        lastDistance = RANGE;
    }

    private static void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || !WCISClientConfig.SCARY_CONTENT.get()) {
            reset();
            return;
        }
        if (minecraft.isPaused()) return;
        double distance = RANGE;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof LurkerEntity lurker && lurker.isAlive()) distance = Math.min(distance, lurker.distanceTo(minecraft.player));
        }
        float felt = distance >= RANGE ? 0.0F : 1.0F - (float) Mth.clamp((distance - NEAR) / (RANGE - NEAR), 0.0, 1.0);
        // Coming closer counts; standing still or backing off lets it ebb.
        float closing = felt <= 0.0F ? 0.0F : Mth.clamp((float) (lastDistance - distance) / CHARGE_SPEED, 0.0F, 1.0F) * Mth.clamp(felt * 1.5F, 0.0F, 1.0F);
        lastDistance = distance;
        presence += (felt - presence) * 0.12F;
        approach += (closing - approach) * (closing > approach ? 0.3F : 0.06F);
        pulse *= 0.82F;

        // A heartbeat, faster the nearer it is; with it coming, each beat is seen and felt as well as heard.
        if (presence > 0.25F && --beatWait <= 0) {
            beatWait = Math.round(Mth.lerp(presence, 34.0F, 11.0F));
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(WCISSounds.BLOCKED, 0.9F + presence * 0.3F, 0.35F + presence * 0.55F));
            pulse = Math.max(pulse, 0.35F * presence + 0.65F * approach);
            if (approach > 0.3F && WCISConfigs.isEnabled(WCISClientConfig.SCREEN_SHAKE)) CameraShake.shake(0.9F * approach, 7);
        }
    }
}
