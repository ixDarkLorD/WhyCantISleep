package net.ixdarklord.whycantisleep.client.terror;

import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxLayer;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The room fills with water: black water seeps up over the floor, dripping from above, and rises past the bed, the
 * covers, and over the face, the world turning dark blue and silent but for bubbles. Pale, drowned shapes stand in it,
 * nearer each time they are seen, and at the end pale hands come up from below the view and pull ({@link EdgeHands}).
 */
public final class DrowningScene implements TerrorScene {
    // The game's own still water, a flipbook of 32 frames down its texture; tinted dark.
    private static final VfxMaterial WATER = VfxMaterial.alpha(Identifier.withDefaultNamespace("textures/block/water_still.png")).withSmoothing(false).withSoftness(0.0F);
    private static final int WATER_FRAMES = 32;
    private static final VfxMaterial DROWNED = TerrorArt.pixels("textures/vfx/terror_demon.png");
    private static final int DEMON_FRAMES = 16;
    // The drowned stand behind the water's surface: drawn first.
    private static final VfxLayer UNDER = VfxLayer.of(5);
    private static final VfxLayer SURFACE = VfxLayer.of(6);
    // How far round the bed the surface is drawn, in blocks.
    private static final int REACH = 7;
    // The water starts rising, reaches the eyes, and is over the head by the jolt.
    private static final float SEEP_FROM = 0.1F;
    private static final float EYES_AT = 0.62F;
    // The drowned rise out of it once it is high, past the covers.
    private static final float EMERGE = 0.42F;
    private static final int HAND_TINT = 0xFFA6BCC6;
    private final List<Drowned> drowned = new ArrayList<>();
    private final EdgeHands pulling = new EdgeHands();
    private double eyeHeight;
    private boolean under;

    @Override
    public void lay(TerrorStage stage, RandomSource random) {
        this.drowned.clear();
        int count = 3 + Math.round(2.0F * stage.intensity);
        for (int i = 0; i < count; i++) {
            // Out in front of the bed and to its sides, where the half-open eyes see.
            double angle = Math.toRadians(-80.0 + 160.0 * (i + random.nextFloat() * 0.6) / count);
            double distance = 3.2 + random.nextDouble() * 1.6;
            Vec3 way = stage.along.scale(-Math.cos(angle)).add(stage.across.scale(Math.sin(angle)));
            this.drowned.add(new Drowned(way, distance, random.nextInt(8), random.nextFloat() * 6.28F));
        }
        // From below the view: they pull down.
        this.pulling.lay(random, 5 + Math.round(3.0F * stage.intensity), 200.0, 340.0, 0.6F, 0.78F);
        this.eyeHeight = Double.NaN;
    }

    // Where the water's surface is this far in, from the floor up past the eyes.
    private double level(TerrorStage stage, float t) {
        double eyes = Double.isNaN(this.eyeHeight) ? stage.floor + 0.7 : this.eyeHeight;
        float rising = Mth.clamp((t - SEEP_FROM) / (EYES_AT - SEEP_FROM), 0.0F, 1.0F);
        double below = stage.floor - 0.02 + Easing.QUAD_IN_OUT.apply(rising) * (eyes + 0.05 - stage.floor);
        return t <= EYES_AT ? below : below + (t - EYES_AT) / (TerrorStage.JOLT - EYES_AT) * 1.2;
    }

    @Override
    public void tick(Minecraft minecraft, TerrorStage stage, long age, float t) {
        if (minecraft.player == null || minecraft.level == null) return;
        Vec3 eyes = minecraft.player.getEyePosition();
        if (Double.isNaN(this.eyeHeight)) this.eyeHeight = eyes.y;
        RandomSource random = minecraft.level.getRandom();
        int length = stage.length;
        if (age == Math.round(SEEP_FROM * length)) TerrorStage.play(minecraft, SoundEvents.BUCKET_EMPTY, 0.8F, 0.5F);
        boolean nowUnder = this.level(stage, t) > eyes.y;
        if (nowUnder && !this.under) TerrorStage.play(minecraft, SoundEvents.AMBIENT_UNDERWATER_ENTER, 1.0F, 0.8F);
        this.under = nowUnder;
        if (!this.under && t < EYES_AT) {
            // Dripping from above, and the water lapping as it rises.
            if (random.nextFloat() < 0.6F) {
                minecraft.level.addParticle(ParticleTypes.DRIPPING_WATER, eyes.x + (random.nextDouble() - 0.5) * 4.0, eyes.y + 1.2,
                        eyes.z + (random.nextDouble() - 0.5) * 4.0, 0.0, 0.0, 0.0);
            }
            if (age % 25 == 0 && t > SEEP_FROM) TerrorStage.play(minecraft, SoundEvents.PLAYER_SPLASH, 0.15F + 0.25F * t, 0.5F);
        }
        if (this.under && age % 18 == 0) TerrorStage.play(minecraft, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 0.6F, 0.7F);
        if (age % 50 == 20 && t > 0.3F) TerrorStage.play(minecraft, SoundEvents.DROWNED_AMBIENT_WATER, 0.5F, 0.6F);
        for (float from : this.pulling.starts()) {
            if (age == Math.round(from * length)) TerrorStage.play(minecraft, SoundEvents.PLAYER_SPLASH, 0.6F, 0.4F);
        }
        if (age == stage.stareTick() - 4) TerrorStage.play(minecraft, SoundEvents.PLAYER_HURT_DROWN, 0.9F, 0.7F);
    }

    @Override
    public void renderWorld(VfxFrame frame, TerrorStage stage, float age, float t) {
        Vec3 camera = frame.cameraPosition();
        float time = frame.time();
        double water = this.level(stage, t);
        // The drowned: once the water is high, they rise up out of it, each in its own time, and come nearer to the
        // bed; pale and dim.
        float seen = Mth.clamp((t - EMERGE) / 0.08F, 0.0F, 1.0F);
        if (seen > 0.0F) {
            float come = Easing.SINE_IN_OUT.apply(Mth.clamp((t - EMERGE) / (TerrorStage.STARE - EMERGE), 0.0F, 1.0F));
            for (Drowned one : this.drowned) {
                float risen = Easing.CUBIC_OUT.apply(Mth.clamp((t - EMERGE - one.phase * 0.012F) / 0.14F, 0.0F, 1.0F));
                double distance = Mth.lerp(come, one.distance, 1.6);
                Vec3 foot = stage.onFloor(0.0, 0.0).add(one.way.scale(distance));
                double sink = 2.1 * (1.0F - risen) + 0.6 * (1.0F - come) + 0.05 * Math.sin(time * 0.7 + one.phase);
                Vec3 base = new Vec3(foot.x, stage.floor - sink, foot.z);
                Vec3[] corners = stage.upright(base, camera, 1.0, 2.0, Vec3.ZERO);
                int pose = 8 + one.pose;
                frame.draw(DROWNED, UNDER).quad(corners[0], corners[1], corners[2], corners[3], 0.0F, pose / (float) DEMON_FRAMES, 1.0F,
                        (pose + 1) / (float) DEMON_FRAMES, ARGB.color(Math.round(170 * seen), 0x8FB0C0));
            }
        }
        // The surface, a block at a time round the bed, rippling; dark and see-through, blacker the higher it is.
        if (t > SEEP_FROM) {
            VfxDraw surface = frame.draw(WATER, SURFACE);
            int shown = (int) (time * 8.0F) % WATER_FRAMES;
            // Half a texel in from the frame's edges, so the next frame down the texture never bleeds in along a tile's
            // border and outlines it.
            float u0 = 0.5F / 16.0F;
            float u1 = 1.0F - u0;
            float v0 = (shown + 0.5F / 16.0F) / WATER_FRAMES;
            float v1 = (shown + 1 - 0.5F / 16.0F) / WATER_FRAMES;
            int color = ARGB.color(this.under ? 120 : 200, 0x1C3550);
            int cx = Mth.floor(stage.head.x);
            int cz = Mth.floor(stage.head.z);
            // The ripple's height at each corner, shared by the tiles that meet there, so the surface is one sheet with
            // no steps between its tiles.
            int size = REACH * 2 + 2;
            double[][] heights = new double[size][size];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    int x = cx - REACH + i;
                    int z = cz - REACH + j;
                    heights[i][j] = water + 0.03 * Math.sin(time * 1.6 + x * 0.9 + z * 0.6);
                }
            }
            for (int i = 0; i < size - 1; i++) {
                for (int j = 0; j < size - 1; j++) {
                    int x = cx - REACH + i;
                    int z = cz - REACH + j;
                    surface.quad(new Vec3(x, heights[i][j], z), new Vec3(x, heights[i][j + 1], z + 1), new Vec3(x + 1, heights[i + 1][j + 1], z + 1),
                            new Vec3(x + 1, heights[i + 1][j], z), u0, v0, u1, v1, color);
                }
            }
        }
        // Under it, bubbles of their last breath rising past the face, and more up from the floor.
        if (this.under) {
            VfxDraw bubbles = frame.draw(TerrorArt.GLOW);
            for (int i = 0; i < 18; i++) {
                double phase = i * 2.39;
                double rise = (time * (0.35 + (i % 5) * 0.08) + i * 0.37) % 2.2;
                Vec3 at = camera.add(stage.across.scale(Math.sin(phase) * (0.3 + (i % 3) * 0.5)))
                        .add(stage.along.scale(-0.6 - (i % 4) * 0.5)).add(0.0, rise - 1.0, 0.0);
                bubbles.billboard(at, 0.02F + (i % 3) * 0.012F, ARGB.color(110, 0xCFE8FF));
            }
        }
        this.pulling.render(frame, stage, age, t, HAND_TINT);
    }

    @Override
    public void renderHud(GuiGraphicsExtractor graphics, TerrorStage stage, float age, float t) {
        if (!this.under) return;
        // Under the water: everything dark blue, deeper as it goes on.
        float deep = Mth.clamp((t - EYES_AT) / 0.15F, 0.0F, 1.0F);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), ARGB.color(Math.round(40 + 70 * deep), 0x06233F));
    }

    @Override
    public int fogColor(TerrorStage stage, float t) {
        return this.under ? 0x062038 : 0x050C14;
    }

    @Override
    public float fogEnd(TerrorStage stage, float t, float usual) {
        return this.under ? 2.6F : usual;
    }

    @Override
    public int mistColor(TerrorStage stage, float t) {
        return this.under ? -1 : 0x0C1C2A;
    }

    @Override
    public float warp(TerrorStage stage) {
        return 0.8F + 0.6F * stage.intensity;
    }

    // A drowned shape: the way out from the bed it stands, how far at first, which of the shadow's poses, its sway.
    private record Drowned(Vec3 way, double distance, int pose, float phase) {
    }
}
