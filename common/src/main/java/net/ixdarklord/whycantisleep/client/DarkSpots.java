package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxBlend;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldVfx;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.client.hud.HudStyle;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The places in a bedroom a monster could spawn, from the bed check, marked for a while so the player can see where a
 * light is wanted: a soft violet tile of light on the floor of each, breathing slowly, with a mote now and then
 * drifting up from it. They come up one after another from the bed out, and fade away over their last few seconds; a
 * spot lit since goes unmarked. Drawn with CoolCatLib's world effects, worked out from the clock each frame rather than
 * spawned: two shapes a spot, in two draw calls for them all.
 */
public final class DarkSpots {
    private static final int LIFE = 20 * 10;
    // How long each takes to come up, and how far apart they start (the spots come nearest the bed first), at most.
    private static final float FADE_IN_TICKS = 12.0F;
    private static final float STAGGER_TICKS = 1.0F;
    private static final float STAGGER_MOST = 24.0F;
    // The share of their time they fade out over, how often whether each is still dark is looked at, and how far away
    // they are drawn.
    private static final float FADE_OUT_SHARE = 0.3F;
    private static final int LIGHT_CHECK_TICKS = 10;
    private static final double RANGE = 32.0;
    private static final int COLOR = 0x8A6BFF;
    // The tile: its half width, its brightness, and how deep and slow its breathing is (a cycle every 4 seconds).
    private static final float TILE_HALF = 0.42F;
    private static final float TILE_ALPHA = 0.95F;
    private static final float BREATH_DEPTH = 0.2F;
    private static final float BREATH_SPEED = Mth.TWO_PI / (20 * 4);
    // The mote: its size, how high it drifts, and how long it takes to rise and fade (3 seconds).
    private static final float MOTE_HALF = 0.045F;
    private static final float MOTE_RISE = 0.65F;
    private static final float MOTE_TICKS = 20 * 3;
    private static final float MOTE_ALPHA = 0.85F;

    // A flat tile on the floor has no softness: it would fade into the very floor it lies on.
    private static final VfxMaterial TILE = new VfxMaterial(WhyCantISleep.id("textures/vfx/dark_spot.png"), VfxBlend.ADDITIVE, 0.0F);
    private static final VfxMaterial MOTE = VfxMaterial.additive(VfxMaterial.GLOW).withSoftness(0.1F);

    private static final List<Spot> SPOTS = new ArrayList<>();
    private static long tick;
    private static long shownAt;

    private DarkSpots() {
    }

    public static void init() {
        ClientTickEvents.END.register(DarkSpots::tick);
        ClientPlayerEvents.LEAVE.register(player -> SPOTS.clear());
        WorldVfx.register(WhyCantISleep.id("dark_spots"), DarkSpots::render);
    }

    /** Marks these spots, nearest the bed first, in place of any marked before. */
    public static void show(List<BlockPos> spots) {
        SPOTS.clear();
        if (!WCISConfigs.isEnabled(WCISClientConfig.DARK_SPOTS)) return;
        for (BlockPos spot : spots) SPOTS.add(new Spot(spot.immutable(), phase(spot)));
        shownAt = tick;
    }

    private static void tick(Minecraft minecraft) {
        if (minecraft.isPaused()) return;
        tick++;
        if (SPOTS.isEmpty()) return;
        if (minecraft.level == null || tick - shownAt > LIFE) {
            SPOTS.clear();
            return;
        }
        if ((tick - shownAt) % LIGHT_CHECK_TICKS != 0) return;
        for (Spot spot : SPOTS) {
            if (!spot.lit && minecraft.level.getBrightness(LightLayer.BLOCK, spot.pos) > 0) spot.lit = true;
        }
    }

    private static void render(VfxFrame frame) {
        if (SPOTS.isEmpty()) return;
        // The game's own clock, which stops with the pause screen.
        float now = tick + (Minecraft.getInstance().isPaused() ? 0.0F : frame.partialTick());
        float age = now - shownAt;
        float fadeOut = Mth.clamp((LIFE - age) / (LIFE * FADE_OUT_SHARE), 0.0F, 1.0F);
        if (fadeOut <= 0.0F) return;
        Vec3 camera = frame.cameraPosition();
        VfxDraw tiles = frame.draw(TILE);
        VfxDraw motes = frame.draw(MOTE);
        for (int i = 0; i < SPOTS.size(); i++) {
            Spot spot = SPOTS.get(i);
            if (spot.lit) continue;
            double x = spot.pos.getX() + 0.5;
            double y = spot.pos.getY();
            double z = spot.pos.getZ() + 0.5;
            if (camera.distanceToSqr(x, y, z) > RANGE * RANGE) continue;
            float fadeIn = Mth.clamp((age - Math.min(i * STAGGER_TICKS, STAGGER_MOST)) / FADE_IN_TICKS, 0.0F, 1.0F);
            float alpha = ease(fadeIn) * fadeOut;
            if (alpha <= 0.0F) continue;

            float breath = 1.0F - BREATH_DEPTH * (0.5F + 0.5F * Mth.sin(now * BREATH_SPEED + spot.phase * Mth.TWO_PI));
            tiles.flat(x, y + 0.02, z, TILE_HALF, 0.0F, HudStyle.color(COLOR, alpha * TILE_ALPHA * breath));

            // Rising from somewhere on the tile, swaying a little, brightest halfway up.
            float rise = ((now / MOTE_TICKS) + spot.phase) % 1.0F;
            float sway = Mth.sin((now / MOTE_TICKS + spot.phase) * Mth.TWO_PI * 1.5F) * 0.06F;
            double mx = x + (spot.phase - 0.5F) * 0.4 + sway;
            double mz = z + (((spot.phase * 7.31F) % 1.0F) - 0.5F) * 0.4;
            motes.billboard(mx, y + 0.08 + rise * MOTE_RISE, mz, MOTE_HALF, 0.0F, HudStyle.color(COLOR, alpha * MOTE_ALPHA * Mth.sin(rise * Mth.PI)));
        }
    }

    // Comes up quickly and settles in gently.
    private static float ease(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t);
    }

    // A fixed 0 to 1 for a spot, so the spots breathe and drift out of step with each other.
    private static float phase(BlockPos pos) {
        return (Mth.getSeed(pos.getX(), pos.getY(), pos.getZ()) & 0xFFFF) / 65536.0F;
    }

    private static final class Spot {
        private final BlockPos pos;
        private final float phase;
        private boolean lit;

        private Spot(BlockPos pos, float phase) {
            this.pos = pos;
            this.phase = phase;
        }
    }
}
