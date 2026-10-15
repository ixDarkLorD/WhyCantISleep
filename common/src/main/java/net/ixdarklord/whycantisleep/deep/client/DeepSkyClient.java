package net.ixdarklord.whycantisleep.deep.client;

import net.ixdarklord.coolcatcanvas.api.client.sky.Skyboxes;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.deep.DeepProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * The Lucid Deep's own sky (assets/whycantisleep/skybox/lucid_deep.json, shaders/sky/lucid_deep.fsh), taken over in
 * code so it can be told about the dreamer: whether the Dark Sign shows for them ({@link DeepProgress#signShown}, its
 * coming out and going faded over a few seconds) and which design it is drawn in.
 */
public final class DeepSkyClient {
    /** How much of the way out the Sign is, per tick: about five seconds from nothing to all. */
    private static final float FADE = 0.01F;
    private static float sign;
    private static float signBefore;

    private DeepSkyClient() {
    }

    public static void init() {
        Skyboxes.register(WhyCantISleep.id("lucid_deep"), WhyCantISleep.id("lucid_deep"))
                .layer("abyss").bindParam(0, (context, out) -> {
                    out[0] = Mth.lerp(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false), signBefore, sign);
                    out[1] = progress().style() == DeepProgress.SignStyle.DS3 ? 1.0F : 0.0F;
                });
        ClientTickEvents.END.register(minecraft -> {
            signBefore = sign;
            float target = minecraft.player != null && progress().signShown() ? 1.0F : 0.0F;
            sign = Mth.approach(sign, target, FADE);
        });
    }

    private static DeepProgress progress() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null ? DeepProgress.get(minecraft.player) : DeepProgress.DEFAULT;
    }

    /** Whether the storm's vortex over the Lord's castle shows for this dreamer (CastleVortex). */
    public static boolean vortexShown() {
        return progress().vortexShown();
    }
}
