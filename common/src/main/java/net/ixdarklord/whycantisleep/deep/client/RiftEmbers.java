package net.ixdarklord.whycantisleep.deep.client;

import net.ixdarklord.coolcatcanvas.api.client.vfx.Curve;
import net.ixdarklord.coolcatcanvas.api.client.vfx.ParticleSpec;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldParticles;
import net.ixdarklord.whycantisleep.deep.LucidDeep;
import net.ixdarklord.whycantisleep.lurker.LurkerPalette;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Embers blown out of the rift towards the dreamer: glowing motes and a few quick sparks that come out of the tear,
 * along its length, and rush at them, growing as they near and fading as they arrive. The rift is part of
 * the sky, always in the same place from wherever they stand, so the embers start a fixed way off in its direction.
 */
final class RiftEmbers {
    // How far off they start, and how fast they come (blocks a tick): they rush at the dreamer.
    private static final double FROM = 46.0;
    private static final double SPEED = 1.5;
    // The tear's half-length and half-width at its widest, in degrees of sky (from dream_window.fsh: the tear spans
    // 0.9 / 2.2 of the sprite's half-size, which is WINDOW_SIZE * 2.2 / 2).
    private static final float HALF_LENGTH = LucidDeep.WINDOW_SIZE * 0.45F * 0.9F;
    private static final float HALF_WIDTH = 3.5F;

    private static final ParticleSpec MOTE = ParticleSpec.builder(VfxMaterial.additive(VfxMaterial.GLOW))
            .lifetime(24, 30)
            .size(Curve.of(0.1F, 0.22F, 0.28F))
            .opacity(Curve.of(0.0F, 0.9F, 0.0F))
            .color(LurkerPalette.RED, LurkerPalette.CRIMSON)
            .drag(1.0F)
            .build();
    private static final ParticleSpec SPARK = ParticleSpec.builder(VfxMaterial.additive(VfxMaterial.STREAK))
            .shape(ParticleSpec.Shape.SPARK)
            .lifetime(16, 22)
            .size(Curve.of(0.06F, 0.08F, 0.0F))
            .opacity(Curve.of(1.0F, 0.8F, 0.0F))
            .color(0xFFD6D2, LurkerPalette.RED)
            .stretch(6.0F)
            .drag(1.0F)
            .build();
    private static final RandomSource RANDOM = RandomSource.create();

    private RiftEmbers() {
    }

    /** A tick of embers, as many as the rift is open. */
    static void tick(LocalPlayer player, float open) {
        Vec3 eye = player.getEyePosition();
        // Short-lived as they are, more of them come, so the stream is as thick.
        int motes = Math.round(open * (2.0F + RANDOM.nextFloat() * 2.0F));
        for (int i = 0; i < motes; i++) spawn(MOTE, eye, 1.0);
        if (RANDOM.nextFloat() < open * 0.45F) spawn(SPARK, eye, 1.4);
    }

    // One ember from a random point along the tear (wider in its middle, as the tear is), rushing at the dreamer,
    // spreading a little as it comes.
    private static void spawn(ParticleSpec spec, Vec3 eye, double speed) {
        float along = RANDOM.nextFloat() * 2.0F - 1.0F;
        float up = LucidDeep.WINDOW_ELEVATION + along * HALF_LENGTH;
        float across = (RANDOM.nextFloat() * 2.0F - 1.0F) * HALF_WIDTH * (1.0F - along * along);
        Vec3 dir = direction(up, across);
        Vec3 from = eye.add(dir.scale(FROM));
        Vec3 drift = new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian(), RANDOM.nextGaussian()).scale(0.04);
        Vec3 velocity = dir.scale(-SPEED * speed * (0.75 + RANDOM.nextFloat() * 0.5)).add(drift);
        WorldParticles.spawn(spec, from, velocity);
    }

    // A way into the sky: north, this many degrees up and this many to the side.
    private static Vec3 direction(float up, float side) {
        float u = up * Mth.DEG_TO_RAD;
        float s = side * Mth.DEG_TO_RAD;
        return new Vec3(Mth.sin(s) * Mth.cos(u), Mth.sin(u), -Mth.cos(s) * Mth.cos(u));
    }
}
