package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;

import java.util.ArrayList;
import java.util.List;

/**
 * The places in a bedroom a monster could spawn, from the bed check: each marked for a while with a few specks of
 * dust on the floor, in a dim violet, so the player can see where a light is wanted. A spot lit since goes unmarked.
 */
public final class DarkSpots {
    // How long the spots are marked, how often a speck shows on each, and the specks' color.
    private static final int LIFE = 20 * 10;
    private static final int SHOW_TICKS = 5;
    private static final int COLOR = 0x8A6BFF;

    private static final List<BlockPos> SPOTS = new ArrayList<>();
    private static long tick;
    private static long shownAt;

    private DarkSpots() {
    }

    public static void init() {
        ClientTickEvents.END.register(DarkSpots::tick);
        ClientPlayerEvents.LEAVE.register(player -> SPOTS.clear());
    }

    /** Marks these spots, in place of any marked before. */
    public static void show(List<BlockPos> spots) {
        SPOTS.clear();
        if (!WCISConfigs.isEnabled(WCISClientConfig.DARK_SPOTS)) return;
        SPOTS.addAll(spots);
        shownAt = tick;
    }

    private static void tick(Minecraft minecraft) {
        if (minecraft.isPaused()) return;
        tick++;
        if (SPOTS.isEmpty() || minecraft.level == null) return;
        long age = tick - shownAt;
        if (age > LIFE) {
            SPOTS.clear();
            return;
        }
        if (age % SHOW_TICKS != 0) return;
        RandomSource random = minecraft.level.getRandom();
        // Fading out over the last part of their time: shown less and less often.
        float left = Mth.clamp((LIFE - age) / (LIFE * 0.3F), 0.0F, 1.0F);
        for (BlockPos spot : SPOTS) {
            if (random.nextFloat() > left || minecraft.level.getBrightness(LightLayer.BLOCK, spot) > 0) continue;
            double x = spot.getX() + 0.2 + random.nextDouble() * 0.6;
            double z = spot.getZ() + 0.2 + random.nextDouble() * 0.6;
            minecraft.level.addParticle(new DustParticleOptions(COLOR, 0.9F), x, spot.getY() + 0.05, z, 0.0, 0.01, 0.0);
        }
    }
}
