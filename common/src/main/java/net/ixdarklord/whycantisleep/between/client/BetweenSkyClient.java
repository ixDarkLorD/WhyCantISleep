package net.ixdarklord.whycantisleep.between.client;

import net.ixdarklord.coolcatcanvas.api.client.sky.Skyboxes;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.between.BetweenProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * The Dark Between's own sky (assets/whycantisleep/skybox/dark_between.json, shaders/sky/dark_between.fsh), taken over in
 * code so it can be told about the dreamer: whether the Dark Sign shows for them ({@link BetweenProgress#signShown}, its
 * coming out and going faded over a few seconds) and which design it is drawn in.
 */
public final class BetweenSkyClient {
    /** How much of the way out the Sign is, per tick: about five seconds from nothing to all. */
    private static final float FADE = 0.01F;
    private static float sign;
    private static float signBefore;

    private BetweenSkyClient() {
    }

    public static void init() {
        Skyboxes.register(WhyCantISleep.id("dark_between"), WhyCantISleep.id("dark_between"))
                .layer("abyss").bindParam(0, (context, out) -> {
                    out[0] = Mth.lerp(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false), signBefore, sign);
                    out[1] = progress().style() == BetweenProgress.SignStyle.DS3 ? 1.0F : 0.0F;
                });
        ClientTickEvents.END.register(minecraft -> {
            signBefore = sign;
            float target = minecraft.player != null && progress().signShown() ? 1.0F : 0.0F;
            sign = Mth.approach(sign, target, FADE);
        });
    }

    private static BetweenProgress progress() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null ? BetweenProgress.get(minecraft.player) : BetweenProgress.DEFAULT;
    }

    /** Whether the storm's vortex over the Lord's castle shows for this dreamer (CastleVortex). */
    public static boolean vortexShown() {
        return progress().vortexShown();
    }
}
