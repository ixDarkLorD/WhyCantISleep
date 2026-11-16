package net.ixdarklord.whycantisleep.client.terror;

import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxLayer;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Things crawling in from the dark: a skittering in the fog, then many-legged bugs pouring over the floor from every
 * side, up the bed's sides and over the sleeper lying in it (their head, chest, arms and legs), working their way up to
 * the face; the sound of them everywhere. Then they are on the face: crawling in over the view from its edges, more and
 * more, until by the jolt it is covered. The bugs are solid ({@link Solid}): a segmented shell, jointed legs stepping
 * in turn, feelers and two red eyes.
 */
public final class SwarmScene implements TerrorScene {
    private static final VfxMaterial SHELL = VfxMaterial.alpha(WhyCantISleep.id("textures/vfx/terror_chitin.png"))
            .withSmoothing(false).withSoftness(0.0F);
    // Sorted, so one bug (and one part of one) covers another as their distance says.
    private static final VfxLayer BUGS = VfxLayer.sorted(10);
    // A bug's segments, head first: each one's length, width and height, as shares of the bug's length.
    private static final double[] SEGMENT_LENGTH = {0.14, 0.2, 0.22, 0.2, 0.16};
    private static final double[] SEGMENT_WIDTH = {0.16, 0.26, 0.3, 0.24, 0.14};
    private static final double[] SEGMENT_HEIGHT = {0.1, 0.15, 0.17, 0.14, 0.08};
    // How high a bed's top is over the floor, and how far its side is from its middle.
    private static final double BED_TOP = 0.5625;
    private static final double BED_SIDE = 0.5;
    // When the first come out of the dark and the last; when they start crawling over the view.
    private static final float COME_FROM = 0.1F;
    private static final float COME_TO = 0.5F;
    private static final float ON_FACE = 0.6F;
    // How near the head of the bed those on the sleeper come (just short of the chin): nearer, they'd be at the eyes,
    // which is the ones on the face's business.
    private static final double CLOSEST = 0.6;
    // How far in front of the eyes those on the face are.
    private static final double FACE_DISTANCE = 0.3;
    private final List<Crawler> crawlers = new ArrayList<>();
    private final List<FaceCrawler> face = new ArrayList<>();

    @Override
    public void lay(TerrorStage stage, RandomSource random) {
        this.crawlers.clear();
        int count = 55 + Math.round(55.0F * stage.intensity);
        Vec3 middle = stage.onFloor(0.9, 0.0);
        for (int i = 0; i < count; i++) {
            // Out of the dark from any side, to the bed's side on theirs, up it and over the sleeper to a place on them.
            double angle = random.nextDouble() * Math.PI * 2.0;
            double out = 4.5 + random.nextDouble() * 3.0;
            Vec3 start = middle.add(Math.cos(angle) * out, 0.0, Math.sin(angle) * out);
            int side = start.subtract(middle).dot(stage.across) >= 0.0 ? 1 : -1;
            double aheadTarget = CLOSEST + random.nextDouble() * (1.9 - CLOSEST);
            double aheadEdge = Mth.clamp(aheadTarget + (random.nextDouble() - 0.5) * 0.5, CLOSEST, 1.9);
            float from = COME_FROM + random.nextFloat() * (COME_TO - COME_FROM);
            float arrive = from + 0.22F + random.nextFloat() * 0.15F;
            this.crawlers.add(new Crawler(start, side, aheadEdge, aheadTarget, (random.nextDouble() - 0.5) * 0.8, from, arrive,
                    random.nextFloat() * 6.28F, 0.22 + random.nextDouble() * 0.14));
        }
        this.face.clear();
        int onFace = 110 + Math.round(60.0F * stage.intensity);
        for (int i = 0; i < onFace; i++) {
            // In from a point on the screen's edge (by angle), towards somewhere inside.
            float from = ON_FACE + (float) Math.pow(random.nextFloat(), 0.8) * (TerrorStage.JOLT - ON_FACE);
            this.face.add(new FaceCrawler(random.nextDouble() * Math.PI * 2.0, random.nextDouble() * 2.0 - 1.0, random.nextDouble() * 2.0 - 1.0,
                    from, random.nextFloat() * 6.28F, 0.22F + random.nextFloat() * 0.2F));
        }
    }

    @Override
    public void tick(Minecraft minecraft, TerrorStage stage, long age, float t) {
        if (t < COME_FROM * 0.5F || t > TerrorStage.JOLT) return;
        // The skittering, from now and then to all the time, louder and nearer.
        float busy = Mth.clamp((t - COME_FROM * 0.5F) / (TerrorStage.JOLT - COME_FROM * 0.5F), 0.0F, 1.0F);
        int gap = Math.max(1, Math.round(Mth.lerp(busy, 9.0F, 1.0F)));
        RandomSource random = minecraft.level != null ? minecraft.level.getRandom() : RandomSource.create();
        if (age % gap == 0) {
            TerrorStage.play(minecraft, random.nextBoolean() ? SoundEvents.SILVERFISH_STEP : SoundEvents.SPIDER_STEP,
                    0.15F + 0.5F * busy, 0.9F + random.nextFloat() * 0.6F);
        }
        if (age % 45 == 10) TerrorStage.play(minecraft, SoundEvents.SILVERFISH_AMBIENT, 0.4F + 0.4F * busy, 0.7F + random.nextFloat() * 0.3F);
        if (age == Math.round(0.4F * stage.length) || age == Math.round(ON_FACE * stage.length)) {
            TerrorStage.play(minecraft, SoundEvents.SPIDER_AMBIENT, 0.8F, 0.6F);
        }
    }

    /**
     * How high the sleeper lying in the bed is over its top, this far from the head of the bed (towards its foot) and
     * this far across it: the head, the chest with the arms beside it, and the legs, their edges rounded off a little so
     * nothing crawling over them jumps.
     */
    private static double sleeper(double ahead, double side) {
        double head = part(ahead, side, -0.05, 0.45, 0.25) * 0.5;
        double chest = part(ahead, side, 0.45, 1.2, 0.5) * 0.25;
        double legs = part(ahead, side, 1.2, 1.95, 0.25) * 0.25;
        return Math.max(head, Math.max(chest, legs));
    }

    // How far inside a part of the sleeper a point is, 0 out to 1 in, eased over its edge.
    private static double part(double ahead, double side, double from, double to, double halfWidth) {
        double inside = Math.min(Math.min(ahead - from, to - ahead), halfWidth - Math.abs(side));
        double k = Mth.clamp(inside / 0.08 + 0.5, 0.0, 1.0);
        return k * k * (3.0 - 2.0 * k);
    }

    @Override
    public void renderWorld(VfxFrame frame, TerrorStage stage, float age, float t) {
        float time = frame.time();
        Vec3 camera = frame.cameraPosition();
        VfxDraw shell = frame.draw(SHELL, BUGS);
        VfxDraw glow = frame.draw(TerrorArt.GLOW, BUGS);
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        float faded = 1.0F - Mth.clamp((t - TerrorStage.STARE) / 0.05F, 0.0F, 1.0F);
        if (faded > 0.0F) {
            int alpha = Math.round(255 * faded);
            for (Crawler crawler : this.crawlers) {
                if (t < crawler.from) continue;
                float s = Mth.clamp((t - crawler.from) / (crawler.arrive - crawler.from), 0.0F, 1.0F);
                Vec3 edge = stage.onFloor(crawler.aheadEdge, crawler.side * BED_SIDE);
                // The way there, split by length: over the floor, up the bed's side, over the bed and the sleeper.
                double floorWay = Math.hypot(edge.x - crawler.start.x, edge.z - crawler.start.z);
                double topWay = Math.hypot(crawler.aheadEdge - crawler.aheadTarget, crawler.side * BED_SIDE - crawler.sideTarget);
                double gone = s * (floorWay + BED_TOP + topWay);
                Vec3 at;
                Vec3 forward;
                Vec3 normal = up;
                if (gone < floorWay) {
                    double u = gone / floorWay;
                    Vec3 way = edge.subtract(crawler.start);
                    Vec3 aside = new Vec3(-way.z, 0.0, way.x).normalize();
                    double weave = Math.sin(u * 10.0 + crawler.phase) * 0.25 * Math.sin(u * Math.PI);
                    at = TerrorStage.lerp(crawler.start, edge, u).add(aside.scale(weave));
                    forward = way.normalize().add(aside.scale(Math.cos(u * 10.0 + crawler.phase) * 0.6 * Math.sin(u * Math.PI)));
                } else if (gone < floorWay + BED_TOP) {
                    // Up the side of the bed, head up, belly to it.
                    at = edge.add(0.0, gone - floorWay, 0.0);
                    forward = up;
                    normal = stage.across.scale(crawler.side);
                } else {
                    double u = s >= 1.0F ? 1.0 : (gone - floorWay - BED_TOP) / Math.max(1.0E-3, topWay);
                    double ahead = Mth.lerp(u, crawler.aheadEdge, crawler.aheadTarget);
                    double side = Mth.lerp(u, crawler.side * BED_SIDE, crawler.sideTarget);
                    Vec3 heading = stage.along.scale(crawler.aheadEdge - crawler.aheadTarget).add(stage.across.scale(crawler.sideTarget - crawler.side * BED_SIDE));
                    // There, working their way up the sleeper to the face, and milling about.
                    if (s >= 1.0F) {
                        double mill = time * 1.4 + crawler.phase;
                        ahead = Math.max(CLOSEST, crawler.aheadTarget - (t - crawler.arrive) * 2.0) + 0.06 * Math.sin(mill);
                        side = crawler.sideTarget * 0.8 + 0.1 * Math.sin(mill * 0.7);
                        heading = stage.along.scale(-1.0).add(stage.across.scale(Math.cos(mill * 0.7) * 0.6));
                    }
                    Vec3 floor = stage.onFloor(ahead, side);
                    at = new Vec3(floor.x, stage.floor + BED_TOP + sleeper(ahead, side), floor.z);
                    forward = heading.lengthSqr() < 1.0E-6 ? stage.along.scale(-1.0) : heading;
                }
                bug(shell, glow, at, forward, normal, crawler.length, time, crawler.phase, camera, alpha);
            }
        }
        // On the face: in from the screen's edges, across the view to a place inside, then scuttling about there, their
        // bellies to the eyes.
        if (t < ON_FACE) return;
        ViewPlane plane = ViewPlane.of(camera, FACE_DISTANCE);
        VfxDraw overShell = frame.draw(SHELL, TerrorArt.OVER_EYES);
        VfxDraw overGlow = frame.draw(TerrorArt.GLOW, TerrorArt.OVER_EYES);
        double unit = plane.halfHeight();
        Vec3 towards = plane.ahead().scale(-1.0);
        for (FaceCrawler crawler : this.face) {
            if (t < crawler.from) continue;
            float s = Mth.clamp((t - crawler.from) / 0.08F, 0.0F, 1.0F);
            double edge = plane.edge(crawler.angle) + unit * 0.3;
            Vec3 entry = plane.middle().add(plane.out(crawler.angle).scale(edge));
            Vec3 inside = plane.at(crawler.x * plane.halfWidth() * 0.9, crawler.y * plane.halfHeight() * 0.9);
            double mill = time * 2.0 + crawler.phase;
            Vec3 at = TerrorStage.lerp(entry, inside, Easing.QUAD_OUT.apply(s))
                    .add(plane.right().scale(unit * 0.08 * Math.sin(mill))).add(plane.up().scale(unit * 0.06 * Math.cos(mill * 1.4)));
            Vec3 heading = s < 1.0F ? inside.subtract(entry) : plane.right().scale(Math.cos(mill)).add(plane.up().scale(-Math.sin(mill * 1.4)));
            // Pushed back a touch from the eyes, so its legs (towards them) stay in front of the near plane.
            bug(overShell, overGlow, at.subtract(towards.scale(unit * 0.15)), heading, towards, unit * crawler.size, time, crawler.phase, camera, 255);
        }
    }

    /**
     * A bug, standing on a surface at a point, facing this way along it, its back this way (the surface's normal):
     * its shell in segments weaving behind its head, six legs stepping in turn, feelers and two red eyes.
     */
    private static void bug(VfxDraw shell, VfxDraw glow, Vec3 at, Vec3 heading, Vec3 normal, double length, float time, float phase, Vec3 camera, int alpha) {
        Vec3 n = normal.normalize();
        Vec3 f = heading.subtract(n.scale(heading.dot(n)));
        if (f.lengthSqr() < 1.0E-8) return;
        f = f.normalize();
        Vec3 r = f.cross(n);
        int color = ARGB.color(alpha, 0xFFFFFF);
        double cursor = 0.46 * length;
        Vec3 headFront = Vec3.ZERO;
        double headHeight = 0.0;
        for (int i = 0; i < SEGMENT_LENGTH.length; i++) {
            double segment = SEGMENT_LENGTH[i] * length;
            double width = SEGMENT_WIDTH[i] * length;
            double height = SEGMENT_HEIGHT[i] * length;
            double weave = Math.sin(time * 9.0 + phase - i * 0.9) * 0.05 * length * i / 4.0;
            Vec3 middle = at.add(f.scale(cursor - segment / 2.0)).add(r.scale(weave)).add(n.scale(height / 2.0 + 0.005));
            if (i == 0) {
                headFront = middle.add(f.scale(segment / 2.0));
                headHeight = height;
            }
            Solid.box(shell, middle, f.scale(segment / 2.0), r.scale(width / 2.0), n.scale(height / 2.0), camera, color);
            // Legs on the middle three: up and out to the knee, down to the ground, swinging in turn as it walks.
            if (i >= 1 && i <= 3) {
                for (int side = -1; side <= 1; side += 2) {
                    double swing = Math.sin(time * 14.0 + phase + i * Math.PI + (side > 0 ? Math.PI : 0.0));
                    Vec3 hip = middle.add(r.scale(side * width / 2.0));
                    Vec3 knee = hip.add(r.scale(side * 0.16 * length)).add(n.scale(0.08 * length)).add(f.scale(swing * 0.05 * length));
                    Vec3 foot = hip.add(r.scale(side * 0.3 * length)).subtract(n.scale(height / 2.0))
                            .add(f.scale(swing * 0.09 * length + (2 - i) * 0.07 * length));
                    shell.beam(hip, knee, (float) (0.03 * length), color);
                    shell.beam(knee, foot, (float) (0.022 * length), color);
                }
            }
            cursor -= segment * 0.92;
        }
        // Feelers, waving, and two red eyes.
        for (int side = -1; side <= 1; side += 2) {
            double wave = Math.sin(time * 6.0 + phase + side) * 0.05 * length;
            Vec3 root = headFront.add(r.scale(side * 0.04 * length)).add(n.scale(headHeight * 0.3));
            shell.beam(root, root.add(f.scale(0.28 * length)).add(r.scale(side * 0.12 * length + wave)).add(n.scale(0.1 * length)), (float) (0.012 * length), color);
            Vec3 eye = headFront.subtract(f.scale(0.02 * length)).add(r.scale(side * 0.05 * length)).add(n.scale(headHeight * 0.75));
            glow.billboard(eye, (float) (0.04 * length), ARGB.color(Math.round(alpha * 0.85F), 0xFF2010));
        }
    }

    @Override
    public int fogColor(TerrorStage stage, float t) {
        return 0x0E0A05;
    }

    @Override
    public int mistColor(TerrorStage stage, float t) {
        return 0x1C160B;
    }

    // One crawling in: where it comes from, the bed's side it climbs (1 or -1), how far along the bed it climbs it and
    // where it goes on the sleeper (along and across), when it sets off and gets there (shares of the terror), its
    // weave, and its length in blocks.
    private record Crawler(Vec3 start, int side, double aheadEdge, double aheadTarget, double sideTarget, float from, float arrive, float phase,
                           double length) {
    }

    // One on the face: the way in from the edge, where it goes inside (shares of the view's half-size), when, its
    // scuttle, and its length (a share of the view's half-height).
    private record FaceCrawler(double angle, double x, double y, float from, float phase, float size) {
    }
}
