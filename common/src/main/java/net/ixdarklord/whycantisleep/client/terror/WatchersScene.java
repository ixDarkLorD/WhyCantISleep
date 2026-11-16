package net.ixdarklord.whycantisleep.client.terror;

import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.whycantisleep.client.Eyelids;
import net.ixdarklord.whycantisleep.sound.WCISSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

/**
 * Figures in the mist: tall, thin and pale-faced, standing far off round the bed, perfectly still. The eyes blink on
 * their own, and each time they open the figures are nearer, never seen moving; one tilts its head. At the last blink
 * one is right there, its face in theirs.
 */
public final class WatchersScene implements TerrorScene {
    // The figure (tools/gen_nightmares.py): FRAMES of 32 by 96 down the texture, standing and with its head tilted;
    // its face this far down a frame.
    private static final VfxMaterial WATCHER = TerrorArt.pixels("textures/vfx/terror_watcher.png");
    private static final int FRAMES = 2;
    private static final float FACE_ROW = 13.0F / 96.0F;
    private static final double HEIGHT = 2.7;
    private static final double WIDTH = HEIGHT / 3.0;
    // The blinks: how many before the last, from when to when (shares of the terror); how long each, and when in it
    // the eyes are shut and the figures move.
    private static final int BLINKS = 6;
    private static final float BLINKS_FROM = 0.18F;
    private static final float BLINKS_TO = 0.76F;
    private static final int BLINK_TICKS = 7;
    private static final int SHUT_AT = 3;
    // How far off they stand at first, how near the last blink before the end leaves them, and how far in front of the
    // eyes the one at the end is.
    private static final double FAR = 7.5;
    private static final double NEAR = 1.7;
    private static final double FACE_TO_FACE = 0.75;
    private final List<Watcher> watchers = new ArrayList<>();
    private final List<Integer> blinks = new ArrayList<>();
    private int step;
    private int lastBlink;
    private boolean revealed;

    @Override
    public void lay(TerrorStage stage, RandomSource random) {
        this.watchers.clear();
        int count = 4 + Math.round(3.0F * stage.intensity);
        for (int i = 0; i < count; i++) {
            // Out in front of the bed and to its sides, where the half-open eyes see, spread out.
            double angle = Math.toRadians(-70.0 + 140.0 * (i + 0.2 + random.nextFloat() * 0.6) / count);
            Vec3 way = stage.along.scale(-Math.cos(angle)).add(stage.across.scale(Math.sin(angle)));
            this.watchers.add(new Watcher(way, FAR - random.nextDouble() * 1.5, random.nextInt(BLINKS + 1), random.nextFloat()));
        }
        this.blinks.clear();
        for (int i = 0; i < BLINKS; i++) {
            float at = BLINKS_FROM + (BLINKS_TO - BLINKS_FROM) * (i + 0.25F + random.nextFloat() * 0.5F) / BLINKS;
            this.blinks.add(Math.round(at * stage.length));
        }
        // And the last, just before the stare.
        this.lastBlink = stage.stareTick() - BLINK_TICKS - 1;
        this.blinks.add(this.lastBlink);
        this.step = 0;
        this.revealed = false;
    }

    @Override
    public void tick(Minecraft minecraft, TerrorStage stage, long age, float t) {
        for (int blink : this.blinks) {
            // The eyes shut and open again, of themselves.
            if (age == blink) Eyelids.play(BLINK_TICKS, Eyelids.Mood.PARALYSED, 0.0F, 0.0F, 0.4F, 1.0F, 0.6F, 1.0F, 1.0F, 0.4F);
            if (age == blink + SHUT_AT) {
                // While they're shut, the figures come nearer; at the last, one is face to face.
                if (blink == this.lastBlink) this.revealed = true;
                else this.step++;
                TerrorStage.play(minecraft, WCISSounds.TERROR_CREEP, 0.25F + 0.1F * this.step, 0.6F);
            }
        }
        if (age == Math.round(0.5F * stage.length)) TerrorStage.play(minecraft, WCISSounds.TERROR_WHISPER, 0.6F, 0.8F);
    }

    @Override
    public void renderWorld(VfxFrame frame, TerrorStage stage, float age, float t) {
        Vec3 camera = frame.cameraPosition();
        float seen = Mth.clamp((t - 0.08F) / 0.1F, 0.0F, 1.0F);
        if (seen <= 0.0F) return;
        Vec3 middle = stage.onFloor(0.0, 0.0);
        double near = (double) this.step / BLINKS;
        for (Watcher watcher : this.watchers) {
            double distance = Mth.lerp(Math.pow(near, 0.9), watcher.distance, NEAR + watcher.spread * 0.8);
            Vec3 base = middle.add(watcher.way.scale(distance));
            Vec3[] corners = stage.upright(base, camera, WIDTH, HEIGHT, Vec3.ZERO);
            int shown = this.step >= watcher.tiltsFrom ? 1 : 0;
            // Pale out of the mist, dimmer the farther.
            float clear = (float) Mth.clamp(1.3 - distance / 9.0, 0.3, 1.0) * seen;
            frame.draw(WATCHER, TerrorArt.FIGURES).quad(corners[0], corners[1], corners[2], corners[3], 0.0F, shown / (float) FRAMES, 1.0F,
                    (shown + 1) / (float) FRAMES, ARGB.color(Math.round(255 * clear), 0xD8D8DC));
        }
        if (!this.revealed) return;
        // Face to face: straight ahead, its face at the eyes.
        Vector3fc forward = Minecraft.getInstance().gameRenderer.getMainCamera().forwardVector();
        Vec3 ahead = new Vec3(forward.x(), 0.0, forward.z());
        ahead = ahead.lengthSqr() < 1.0E-6 ? stage.along.scale(-1.0) : ahead.normalize();
        Vec3 foot = camera.add(ahead.scale(FACE_TO_FACE));
        double bottom = camera.y - HEIGHT * (1.0F - FACE_ROW) + 0.04 * Math.sin(frame.time() * 1.1);
        Vec3[] corners = stage.upright(new Vec3(foot.x, bottom, foot.z), camera, WIDTH, HEIGHT, Vec3.ZERO);
        frame.draw(WATCHER, TerrorArt.HANDS).quad(corners[0], corners[1], corners[2], corners[3], 0.0F, 1.0F / FRAMES, 1.0F, 1.0F, 0xFFE4E4E8);
    }

    @Override
    public int fogColor(TerrorStage stage, float t) {
        return 0x15171C;
    }

    @Override
    public float fogEnd(TerrorStage stage, float t, float usual) {
        // A thinner mist, far enough to see them standing in it.
        return usual * 1.7F;
    }

    @Override
    public int mistColor(TerrorStage stage, float t) {
        return 0x4A4E57;
    }

    @Override
    public float warp(TerrorStage stage) {
        return 0.35F + 0.35F * stage.intensity;
    }

    // A figure: the way out from the bed it stands, how far at first, from which blink it has its head tilted, and how
    // far apart from the rest it ends.
    private record Watcher(Vec3 way, double distance, int tiltsFrom, float spread) {
    }
}
