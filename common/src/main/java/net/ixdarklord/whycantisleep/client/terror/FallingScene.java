package net.ixdarklord.whycantisleep.client.terror;

import net.ixdarklord.coolcatcanvas.api.client.camera.CameraShake;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxLayer;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The bed falls away: a creak, then the floor gives with a crack, and they fall down a shaft of black stone rushing up
 * past them faster and faster, red eyes opening in its walls as they go by and closing behind, streaks of the dark
 * flying up, blocks of the floor tumbling with them, the wind roaring and the view shaking. Shadows fly with them, round
 * and round the shaft, shrieking; near the bottom they dive in and their hands close on the face from every side; then
 * the ground comes, and the jolt is the landing (the one that wakes people from a dream of falling).
 */
public final class FallingScene implements TerrorScene {
    private static final VfxMaterial STREAK = VfxMaterial.additive(VfxMaterial.STREAK);
    private static final VfxMaterial[] WALLS = {wall("deepslate"), wall("cobbled_deepslate"), wall("stone"), wall("tuff")};
    private static final VfxMaterial[] BLOCKS = {wall("oak_planks"), wall("cobblestone"), wall("dirt"), wall("stone"), wall("red_wool")};
    // The shaft's walls first, the blocks and demons in it over them, sorted among themselves.
    private static final VfxLayer SHAFT = VfxLayer.of(4);
    private static final VfxLayer FALLING = VfxLayer.sorted(10);
    // The creak, then the floor gives; when the demons come up out of the dark, when they dive, and when they reach.
    private static final float CREAK = 0.1F;
    private static final float GIVES = 0.2F;
    private static final float DEMONS_FROM = 0.38F;
    private static final float DIVE = 0.76F;
    private static final float DIVED = 0.86F;
    // The shaft: how far its walls are from the eyes (blocks), how far up and down it is drawn, and how many tiles in
    // each are eyes.
    private static final int SHAFT_HALF = 3;
    private static final int SHAFT_SPAN = 8;
    private static final int EYES_ONE_IN = 9;
    private static final int STREAKS = 110;
    private static final int PIECES = 16;
    // How tall a stretch the streaks cycle through, round the eyes.
    private static final double SPAN = 10.0;
    private final List<Streak> streaks = new ArrayList<>();
    private final List<Piece> pieces = new ArrayList<>();
    private final List<Flyer> flyers = new ArrayList<>();
    private final EdgeHands grabbing = new EdgeHands();
    private @Nullable SimpleSoundInstance wind;
    // How far they have fallen (in blocks) as of the last frame, worked out from the clock.
    private double fallen;
    private float fallenAt = -1.0F;

    private static VfxMaterial wall(String block) {
        return VfxMaterial.alpha(Identifier.withDefaultNamespace("textures/block/" + block + ".png")).withSmoothing(false).withSoftness(0.0F);
    }

    @Override
    public void lay(TerrorStage stage, RandomSource random) {
        this.streaks.clear();
        for (int i = 0; i < STREAKS; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            this.streaks.add(new Streak(angle, 0.7 + random.nextDouble() * 2.0, random.nextDouble() * SPAN, 0.6 + random.nextDouble() * 0.8,
                    0.015F + random.nextFloat() * 0.03F));
        }
        this.pieces.clear();
        for (int i = 0; i < PIECES; i++) {
            Vec3 axis = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).normalize();
            this.pieces.add(new Piece(random.nextDouble() * Math.PI * 2.0, 0.9 + random.nextDouble() * 1.6, (random.nextDouble() - 0.5) * 3.0,
                    random.nextInt(BLOCKS.length), 0.08 + random.nextDouble() * 0.16, axis, (random.nextFloat() - 0.5F) * 5.0F, random.nextFloat() * 6.28F));
        }
        this.flyers.clear();
        int count = 3 + Math.round(2.0F * stage.intensity);
        for (int i = 0; i < count; i++) {
            this.flyers.add(new Flyer(Math.PI * 2.0 * i / count, 1.7 + random.nextDouble() * 0.6, -1.5 + (random.nextDouble() - 0.5) * 1.4,
                    0.9 + random.nextDouble() * 0.5, random.nextInt(ShadowScene.DEMON_FRAME_EYES.length), random.nextFloat() * 6.28F));
        }
        // Their hands, from every side at once.
        this.grabbing.lay(random, 8 + Math.round(3.0F * stage.intensity), -90.0, 270.0 - 360.0 / (8 + Math.round(3.0F * stage.intensity)), DIVE + 0.02F, DIVED);
        this.fallen = 0.0;
        this.fallenAt = -1.0F;
    }

    // How fast they fall this far in, in blocks a tick: still, then faster and faster.
    private static double speed(float t) {
        float falling = Mth.clamp((t - GIVES) / (TerrorStage.JOLT - GIVES), 0.0F, 1.0F);
        return t < GIVES ? 0.0 : 0.12 + 1.3 * Easing.QUAD_IN.apply(falling);
    }

    @Override
    public void tick(Minecraft minecraft, TerrorStage stage, long age, float t) {
        int length = stage.length;
        if (age == Math.round(CREAK * length)) TerrorStage.play(minecraft, SoundEvents.WOODEN_DOOR_OPEN, 0.7F, 0.5F);
        if (age == Math.round(GIVES * length)) {
            TerrorStage.play(minecraft, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 0.9F, 0.6F);
            CameraShake.shake(3.0F, 10);
            this.wind = SimpleSoundInstance.forUI(SoundEvents.ELYTRA_FLYING, 0.7F, 0.6F);
            minecraft.getSoundManager().play(this.wind);
        }
        if (t > GIVES && t < TerrorStage.JOLT) {
            float falling = (t - GIVES) / (TerrorStage.JOLT - GIVES);
            // Buffeted, harder the faster.
            if (age % 4 == 0) CameraShake.shake(0.4F + 1.8F * falling, 8);
            // The wind louder and higher as it goes: played again over the last.
            if (age % 40 == 0 && falling > 0.3F) {
                this.stopWind(minecraft);
                this.wind = SimpleSoundInstance.forUI(SoundEvents.ELYTRA_FLYING, 0.6F + 0.5F * falling, 0.5F + 0.5F * falling);
                minecraft.getSoundManager().play(this.wind);
            }
        }
        // The shadows: their wings round the shaft, a shriek as they come up, and a swoop as they dive.
        if (t > DEMONS_FROM && t < DIVE && age % 14 == 0) TerrorStage.play(minecraft, SoundEvents.PHANTOM_FLAP, 0.6F, 0.5F);
        if (age == Math.round(DEMONS_FROM * length)) TerrorStage.play(minecraft, SoundEvents.PHANTOM_AMBIENT, 0.9F, 0.5F);
        if (age == Math.round(0.6F * length)) TerrorStage.play(minecraft, SoundEvents.VEX_CHARGE, 0.8F, 0.6F);
        if (age == Math.round(DIVE * length)) TerrorStage.play(minecraft, SoundEvents.PHANTOM_SWOOP, 1.0F, 0.55F);
        if (age == stage.joltTick()) {
            this.stopWind(minecraft);
            TerrorStage.play(minecraft, SoundEvents.GENERIC_BIG_FALL, 1.0F, 0.6F);
        }
    }

    private void stopWind(Minecraft minecraft) {
        if (this.wind != null) minecraft.getSoundManager().stop(this.wind);
        this.wind = null;
    }

    @Override
    public void renderWorld(VfxFrame frame, TerrorStage stage, float age, float t) {
        if (t < GIVES) return;
        Vec3 camera = frame.cameraPosition();
        float time = frame.time();
        // How far they have fallen: the speed added up frame by frame (the clock moving on, the speed at the time).
        if (this.fallenAt >= 0.0F && age > this.fallenAt) this.fallen += speed(t) * (age - this.fallenAt);
        this.fallenAt = age;
        float falling = Mth.clamp((t - GIVES) / (TerrorStage.JOLT - GIVES), 0.0F, 1.0F);
        float opened = Mth.clamp((t - GIVES) / 0.04F, 0.0F, 1.0F);
        this.shaft(frame, camera, time, opened);
        // Streaks of the dark rushing up past, longer and brighter the faster.
        double speed = speed(t);
        VfxDraw rush = frame.draw(STREAK);
        int bright = Math.round(70 + 150 * falling);
        for (Streak streak : this.streaks) {
            double y = ((streak.offset + this.fallen * streak.pace) % SPAN) - SPAN / 2.0;
            Vec3 bottom = camera.add(Math.cos(streak.angle) * streak.out, y, Math.sin(streak.angle) * streak.out);
            Vec3 top = bottom.add(0.0, 0.4 + speed * 5.0 * streak.pace, 0.0);
            rush.beam(bottom, top, streak.width, ARGB.color(bright, 0xB8C0D8), ARGB.color(0, 0xB8C0D8));
        }
        // Blocks of the floor and the stone under it, falling with them, tumbling, drifting a little up and down.
        VfxDraw[] blocks = new VfxDraw[BLOCKS.length];
        for (int i = 0; i < BLOCKS.length; i++) blocks[i] = frame.draw(BLOCKS[i], FALLING);
        int lit = ARGB.color(Math.round(255 * opened), 0x8A8A8A);
        for (Piece piece : this.pieces) {
            double drift = 0.4 * Math.sin(time * 0.6 + piece.phase) + piece.height;
            double round = piece.angle + time * 0.08;
            Vec3 at = camera.add(Math.cos(round) * piece.out, drift, Math.sin(round) * piece.out);
            double spin = time * piece.spin + piece.phase;
            Vec3 x = Solid.turn(new Vec3(piece.size, 0.0, 0.0), piece.axis, spin);
            Vec3 y = Solid.turn(new Vec3(0.0, piece.size, 0.0), piece.axis, spin);
            Vec3 z = Solid.turn(new Vec3(0.0, 0.0, piece.size), piece.axis, spin);
            Solid.box(blocks[piece.texture], at, z, x, y, camera, lit);
        }
        this.flyers(frame, stage, camera, time, t);
        this.grabbing.render(frame, stage, age, t, 0xFFFFFFFF);
    }

    // The shaft: four walls of dark stone round them, a block at a time, rushing up as they fall, fading up and down into
    // the dark; here and there a pair of red eyes in the stone, opening as they pass and shutting behind.
    private void shaft(VfxFrame frame, Vec3 camera, float time, float opened) {
        VfxDraw[] walls = new VfxDraw[WALLS.length];
        for (int i = 0; i < WALLS.length; i++) walls[i] = frame.draw(WALLS[i], SHAFT);
        VfxDraw eyes = frame.draw(TerrorArt.GLOW, SHAFT);
        long passed = (long) Math.floor(this.fallen);
        double slide = this.fallen - passed;
        Vec3[] outs = {new Vec3(1, 0, 0), new Vec3(-1, 0, 0), new Vec3(0, 0, 1), new Vec3(0, 0, -1)};
        for (int w = 0; w < outs.length; w++) {
            Vec3 out = outs[w];
            Vec3 along = new Vec3(-out.z, 0.0, out.x);
            for (int k = -SHAFT_HALF; k < SHAFT_HALF; k++) {
                for (int j = -SHAFT_SPAN; j < SHAFT_SPAN; j++) {
                    // Rows coming up from below as they fall: a row's place in the shaft is fixed, so it keeps its look.
                    long row = j - passed;
                    double bottom = camera.y + j + slide;
                    double middle = bottom + 0.5 - camera.y;
                    float near = (float) Mth.clamp(1.0 - Math.abs(middle) / SHAFT_SPAN, 0.0, 1.0);
                    int alpha = Math.round(255 * opened * Math.min(1.0F, near * 2.0F));
                    if (alpha <= 0) continue;
                    int dark = Math.round(18 + 52 * near * near);
                    int color = ARGB.color(alpha, dark, dark, dark + 6);
                    long hash = row * 73856093L ^ (long) (k + 17) * 19349663L ^ (long) w * 83492791L;
                    int texture = (int) Math.floorMod(hash, (long) WALLS.length);
                    Vec3 base = camera.add(out.scale(SHAFT_HALF)).add(along.scale(k));
                    Vec3 a = new Vec3(base.x, bottom + 1.0, base.z);
                    Vec3 b = new Vec3(base.x, bottom, base.z);
                    Vec3 c = b.add(along);
                    Vec3 d = a.add(along);
                    walls[texture].quad(a, b, c, d, 0.0F, 0.0F, 1.0F, 1.0F, color);
                    // Eyes in the stone, open only while they are near.
                    if (Math.floorMod(hash >> 7, (long) EYES_ONE_IN) == 0) {
                        float open = (float) Mth.clamp(1.0 - Math.abs(middle) / 2.5, 0.0, 1.0);
                        if (open > 0.0F) {
                            Vec3 eye = TerrorStage.lerp(TerrorStage.lerp(a, d, 0.5), TerrorStage.lerp(b, c, 0.5), 0.45).subtract(out.scale(0.03));
                            float blink = 0.7F + 0.3F * Mth.sin(time * 7.0F + hash);
                            for (int side = -1; side <= 1; side += 2) {
                                eyes.billboard(eye.add(along.scale(side * 0.14)), 0.06F, ARGB.color(Math.round(230 * open * blink), 0xFF2A10));
                                eyes.billboard(eye.add(along.scale(side * 0.14)), 0.16F, ARGB.color(Math.round(90 * open * blink), 0xA00000));
                            }
                        }
                    }
                }
            }
        }
    }

    // The shadows flying with them: up out of the dark below, round and round the shaft, glitching; then they dive at
    // the face, one after another, and their hands close in.
    private void flyers(VfxFrame frame, TerrorStage stage, Vec3 camera, float time, float t) {
        if (t < DEMONS_FROM) return;
        VfxDraw draw = frame.draw(ShadowScene.DEMON, FALLING);
        VfxDraw glow = frame.draw(TerrorArt.GLOW, FALLING);
        Vec3 look = new Vec3(stage.along.x, 0.0, stage.along.z).scale(-1.0);
        double lookAngle = Math.atan2(look.z, look.x);
        for (int i = 0; i < this.flyers.size(); i++) {
            Flyer flyer = this.flyers.get(i);
            float up = Easing.CUBIC_OUT.apply(Mth.clamp((t - DEMONS_FROM - i * 0.03F) / 0.1F, 0.0F, 1.0F));
            if (up <= 0.0F) continue;
            float dive = Easing.SINE_IN_OUT.apply(Mth.clamp((t - DIVE - i * 0.015F) / (DIVED - DIVE), 0.0F, 1.0F));
            double angle = flyer.angle + time * flyer.speed;
            double radius = flyer.radius;
            double height = flyer.height + 0.3 * Math.sin(time * 1.7 + flyer.phase) - 6.0 * (1.0F - up);
            // Diving: round to the front of the view, close in, the face at the eyes.
            if (dive > 0.0F) {
                double spread = (i - (this.flyers.size() - 1) / 2.0) * 0.5;
                double target = lookAngle + spread;
                double turn = Mth.wrapDegrees(Math.toDegrees(target - angle)) * Math.PI / 180.0;
                angle += turn * dive;
                radius = Mth.lerp(dive, radius, 0.9);
                height = Mth.lerp(dive, height, -1.6 + 0.2 * Math.abs(spread));
            }
            Vec3 at = camera.add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
            int pose = (flyer.pose + (int) (time * 3.0F + flyer.phase)) % ShadowScene.DEMON_FRAME_EYES.length;
            Vec3[] corners = stage.upright(at, camera, 1.0, 2.0, Vec3.ZERO);
            int shown = ShadowScene.DEMON_FORM_FRAMES + pose;
            draw.quad(corners[0], corners[1], corners[2], corners[3], 0.0F, shown / (float) ShadowScene.DEMON_FRAMES, 1.0F,
                    (shown + 1) / (float) ShadowScene.DEMON_FRAMES, ARGB.color(Math.round(255 * up), 0xFFFFFF));
            int[] eyes = ShadowScene.DEMON_FRAME_EYES[pose];
            for (int e = 0; e < 4; e += 2) {
                Vec3 top = TerrorStage.lerp(corners[0], corners[3], (eyes[e] + 0.5F) / 64.0F);
                Vec3 eye = TerrorStage.lerp(top, TerrorStage.lerp(corners[1], corners[2], (eyes[e] + 0.5F) / 64.0F), (eyes[e + 1] + 0.5F) / 128.0F);
                glow.billboard(eye, 0.04F, ARGB.color(Math.round(255 * up), 0xFF3018));
                glow.billboard(eye, 0.15F, ARGB.color(Math.round(100 * up), 0xB00000));
            }
        }
    }

    @Override
    public void renderHud(GuiGraphicsExtractor graphics, TerrorStage stage, float age, float t) {
        // Just before the ground, a glimpse of it coming: the view's foot lighting dimly red, then the hit.
        float coming = Mth.clamp((t - (TerrorStage.JOLT - 0.05F)) / 0.05F, 0.0F, 1.0F);
        if (coming <= 0.0F || t > TerrorStage.JOLT) return;
        int height = graphics.guiHeight();
        int band = Math.round(height * 0.5F * coming);
        graphics.fillGradient(0, height - band, graphics.guiWidth(), height, ARGB.color(0, 0x4A1A12), ARGB.color(Math.round(170 * coming), 0x4A1A12));
    }

    @Override
    public int fogColor(TerrorStage stage, float t) {
        return t < GIVES ? 0x070507 : 0x000000;
    }

    @Override
    public float fogEnd(TerrorStage stage, float t, float usual) {
        // Once the floor gives, the room is gone: only the bed they fall with.
        float gone = Mth.clamp((t - GIVES) / 0.06F, 0.0F, 1.0F);
        return Mth.lerp(gone, usual, 2.2F);
    }

    @Override
    public int mistColor(TerrorStage stage, float t) {
        return t < GIVES ? 0x141014 : -1;
    }

    @Override
    public float warp(TerrorStage stage) {
        return 0.9F + 0.6F * stage.intensity;
    }

    @Override
    public void stop(Minecraft minecraft) {
        this.stopWind(minecraft);
    }

    // A streak: its way round the eyes and how far out, where in the stretch it starts, how fast it passes compared
    // with the rest, and how wide.
    private record Streak(double angle, double out, double offset, double pace, float width) {
    }

    // A block falling with them: its way round and how far out, how high, which texture, its half-size, the axis it
    // tumbles round and how fast, and its own sway.
    private record Piece(double angle, double out, double height, int texture, double size, Vec3 axis, float spin, float phase) {
    }

    // A shadow flying with them: where round the shaft it starts, how far out, how high, how fast it goes round, which
    // pose it starts in, and its own bob.
    private record Flyer(double angle, double radius, double height, double speed, int pose, float phase) {
    }
}
