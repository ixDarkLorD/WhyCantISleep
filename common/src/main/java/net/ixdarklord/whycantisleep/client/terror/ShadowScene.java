package net.ixdarklord.whycantisleep.client.terror;

import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxLayer;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.whycantisleep.sound.WCISSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

/**
 * The shadow at the foot of the bed: it wells up out of the floor beyond the bed's foot and forms itself, its eyes
 * kindling, then comes along the bed in glitches (each leaving a red afterimage where it was), sinking until its face
 * is in the middle of the view by the stare. Shadow hands rise just past the bed's sides on arms out of the floor and
 * claw over it, ash drifting down through the room; then more hands come for the face from past the screen's top and
 * sides ({@link EdgeHands}) and close round it, so by the stare only the demon's face shows between them.
 */
public final class ShadowScene implements TerrorScene {
    // The demon's flipbook (tools/gen_terror.py): DEMON_FORM_FRAMES of it forming out of shadow, then a frame for each
    // glitch pose, with where its eyes are in each (pixels of its 64 by 128 frame, left x, y, right x, y; the script
    // prints them).
    static final int DEMON_FORM_FRAMES = 8;
    static final int[][] DEMON_FRAME_EYES = {
            {27, 31, 37, 31}, {29, 31, 39, 31}, {24, 32, 34, 32}, {27, 31, 37, 31},
            {32, 31, 42, 31}, {27, 34, 37, 34}, {27, 31, 37, 31}, {25, 29, 35, 29}};
    static final int DEMON_FRAMES = DEMON_FORM_FRAMES + DEMON_FRAME_EYES.length;
    static final VfxMaterial DEMON = TerrorArt.pixels("textures/vfx/terror_demon.png");
    // The red afterimages a glitch leaves, drawn under the demon.
    private static final VfxLayer GHOSTS = VfxLayer.of(9);
    private static final int GHOST_TICKS = 5;
    // The demon's size in blocks; how high its face is in its frame (between the eyes and the mouth, as a share of it
    // from the top); how far beyond the bed's foot it forms; and how far in front of the face it ends, bent low.
    private static final float DEMON_HEIGHT = 2.3F;
    private static final float DEMON_WIDTH = 1.15F;
    private static final float DEMON_FACE_ROW = 36.0F / 128.0F;
    private static final double BEYOND_FOOT = 1.4;
    private static final double BEFORE_FACE = 0.35;
    // The demon rises, forms, and comes along the bed until COME_TO.
    private static final float RISE_FROM = 0.08F;
    private static final float RISE_TO = 0.28F;
    private static final float COME_TO = 0.86F;
    // The hands beside the bed: how far out from its middle their arms come up (just past its side, so the bed hides
    // them); how far ahead of the face; how far over the eyes their fingers come, but no higher than an arm reaches.
    private static final double BESIDE_BED = 0.62;
    private static final double HANDS_FAR = 2.6;
    private static final double HANDS_NEAR = 1.0;
    private static final double HAND_OVER_EYES = 0.12;
    private static final float LOWER = 0.35F;
    private static final double ARM_REACH = 1.1;
    private static final float HANDS_FROM = 0.2F;
    private static final float HAND_RISE = 0.12F;
    // A hand's size beside the bed, fingertips to the forearm's end; where its wrist is along it; the arm's strips as
    // shares of the hand's width (see TerrorArt's arms), each running on a little under the next; each arm's piece is
    // this many hand lengths long.
    private static final double HAND_LENGTH = 0.75;
    private static final double HAND_WIDTH = 0.38;
    private static final double WRIST = 0.72;
    private static final double FOREARM_SHARE = 0.77;
    private static final double UPPER_ARM_SHARE = 0.85;
    private static final double JOINT_OVERLAP = 0.35;
    private static final double ARM_SEGMENT = 1.25;
    private final List<BedsideHand> bedside = new ArrayList<>();
    private final EdgeHands grabbing = new EdgeHands();
    private Vec3 rise = Vec3.ZERO;
    private Vec3 end = Vec3.ZERO;
    // Formed, the demon moves in glitches: the pose shown, until when, and how far it has jumped aside meanwhile; and
    // the last pose and jump, left behind as an afterimage, and when.
    private final RandomSource glitch = RandomSource.create();
    private int pose;
    private long poseUntil;
    private double snap;
    private int ghostPose;
    private double ghostSnap;
    private long ghostAt = Long.MIN_VALUE;
    private long tick;

    @Override
    public void lay(TerrorStage stage, RandomSource random) {
        this.rise = stage.onFloor(1.0 + BEYOND_FOOT, 0.0);
        this.end = stage.onFloor(BEFORE_FACE, 0.0);
        this.bedside.clear();
        // Rising here and there along both sides, each in its own stretch of its side (with a little give), so they
        // spread along it rather than bunching up; some on arms out of the floor, some hands alone, some lower.
        int count = 4 + Math.round(3.0F * stage.intensity);
        boolean leftFirst = random.nextBoolean();
        for (int i = 0; i < count; i++) {
            int side = (i % 2 == 0) == leftFirst ? 1 : -1;
            int onSide = (i % 2 == 0) ? (count + 1) / 2 : count / 2;
            double stretch = (HANDS_FAR - HANDS_NEAR) / onSide;
            double ahead = HANDS_NEAR + stretch * (i / 2 + 0.2 + 0.6 * random.nextFloat());
            this.bedside.add(new BedsideHand(stage.onFloor(ahead, side * (BESIDE_BED + random.nextFloat() * 0.25)), side,
                    HANDS_FROM + random.nextFloat() * 0.3F, random.nextFloat() * 6.28F, random.nextInt(TerrorArt.HAND_VARIANTS.length),
                    random.nextFloat() < 0.6F, random.nextFloat() < 0.5F ? 0.0F : random.nextFloat() * LOWER));
        }
        // And those that come for the face: from past the screen's top and sides, two or three more the worse it is.
        this.grabbing.lay(random, 6 + Math.round(3.0F * stage.intensity), -20.0, 200.0, 0.5F, 0.72F);
    }

    @Override
    public void tick(Minecraft minecraft, TerrorStage stage, long age, float t) {
        this.tick++;
        int length = stage.length;
        if (age == Math.round(RISE_FROM * length)) TerrorStage.play(minecraft, WCISSounds.TERROR_EMERGE, 0.8F + 0.2F * stage.intensity, 1.0F);
        if (age == Math.round(0.45F * length)) TerrorStage.play(minecraft, WCISSounds.TERROR_WHISPER, 0.8F, 1.0F);
        if (stage.intensity > 0.6F && age == Math.round(0.7F * length)) TerrorStage.play(minecraft, WCISSounds.TERROR_WHISPER, 0.9F, 0.85F);
        for (BedsideHand hand : this.bedside) {
            if (age == Math.round(hand.from * length)) TerrorStage.play(minecraft, WCISSounds.TERROR_CREEP, (0.3F + 0.2F * stage.intensity), 0.85F + hand.phase * 0.05F);
        }
        for (float from : this.grabbing.starts()) {
            if (age == Math.round(from * length)) TerrorStage.play(minecraft, WCISSounds.TERROR_CREEP, 0.5F + 0.3F * stage.intensity, 0.75F);
        }
        // Formed, the demon glitches: a pose held a few ticks, or flicked through in one, and now and then a jump aside
        // (more the worse the terror), back again at the next; each change leaves the last pose behind a moment.
        if (t >= RISE_TO && this.tick >= this.poseUntil) {
            this.ghostPose = this.pose;
            this.ghostSnap = this.snap;
            this.ghostAt = this.tick;
            this.pose = this.glitch.nextFloat() < 0.3F ? 0 : this.glitch.nextInt(DEMON_FRAME_EYES.length);
            this.poseUntil = this.tick + (this.glitch.nextFloat() < 0.3F ? 1 : 2 + this.glitch.nextInt(8));
            this.snap = this.glitch.nextFloat() < 0.15F + 0.2F * stage.intensity
                    ? (this.glitch.nextBoolean() ? 1 : -1) * (0.08 + this.glitch.nextFloat() * 0.14) : 0.0;
        }
        // Ash drifting down through the room round them.
        if (minecraft.level != null && minecraft.player != null && t < TerrorStage.JOLT) {
            Vec3 eyes = minecraft.player.getEyePosition();
            RandomSource random = minecraft.level.getRandom();
            for (int i = 0; i < 2; i++) {
                minecraft.level.addParticle(ParticleTypes.ASH, eyes.x + (random.nextDouble() - 0.5) * 6.0, eyes.y + random.nextDouble() * 2.0,
                        eyes.z + (random.nextDouble() - 0.5) * 6.0, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public void renderWorld(VfxFrame frame, TerrorStage stage, float age, float t) {
        this.demon(frame, stage, t);
        this.bedsideHands(frame, stage, t);
        this.grabbing.render(frame, stage, age, t, 0xFFFFFFFF);
    }

    // The demon: shadow welling up out of the floor and forming itself into it, then coming along the bed in glitches,
    // bending low until its face is at theirs.
    private void demon(VfxFrame frame, TerrorStage stage, float t) {
        float forming = Mth.clamp((t - RISE_FROM) / (RISE_TO - RISE_FROM), 0.0F, 1.0F);
        if (forming <= 0.0F) return;
        float risen = Easing.CUBIC_OUT.apply(forming);
        Vec3 camera = frame.cameraPosition();
        float time = frame.time();
        float come = Easing.SINE_IN_OUT.apply(Mth.clamp((t - RISE_TO) / (COME_TO - RISE_TO), 0.0F, 1.0F)) * Mth.lerp(stage.intensity, 0.8F, 1.0F);
        Vec3 foot = this.rise.add(this.end.subtract(this.rise).scale(come));
        // Its face is this high above its feet; as it comes, it sinks into the floor until its face is in the middle of
        // the view (where the line of sight crosses it), there by the stare whatever the terror.
        float faceHeight = DEMON_HEIGHT * (1.0F - DEMON_FACE_ROW);
        Vector3fc looking = Minecraft.getInstance().gameRenderer.getMainCamera().forwardVector();
        double flat = Math.max(0.1, Math.hypot(looking.x(), looking.z()));
        double sight = camera.y + looking.y() / flat * Math.hypot(foot.x - camera.x, foot.z - camera.z) - stage.floor;
        float settle = Easing.SINE_IN_OUT.apply(Mth.clamp((t - RISE_TO) / (TerrorStage.STARE - RISE_TO), 0.0F, 1.0F));
        double bend = settle * Math.max(0.0, faceHeight - sight);
        double bottom = stage.floor - DEMON_HEIGHT * 0.35 * (1.0F - risen) - bend;
        // A slow sway, a tremor as it nears, and its glitches' jumps aside, smaller the nearer it is, so close up its face
        // stays in the window the hands leave.
        double sway = 0.04 * Math.sin(time * 1.3) + 0.015 * come * Math.sin(time * 37.0);
        double near = 1.0 - 0.85 * come;
        boolean formed = t >= RISE_TO;
        Vec3 base = new Vec3(foot.x, bottom, foot.z).add(stage.across.scale(sway + (formed ? this.snap * near : 0.0)));
        Vec3 lean = stage.along.scale(0.25 * come);
        Vec3[] corners = stage.upright(base, camera, DEMON_WIDTH, DEMON_HEIGHT, lean);
        // Forming, its frames run from formless shadow to its shape; formed, the glitch pose. It wells up faint.
        int shown = formed ? DEMON_FORM_FRAMES + this.pose : Math.min(DEMON_FORM_FRAMES - 1, (int) (forming * DEMON_FORM_FRAMES));
        int faint = Math.round(255 * Mth.clamp(forming * 3.0F, 0.0F, 1.0F));
        frame.draw(DEMON, TerrorArt.FIGURES).quad(corners[0], corners[1], corners[2], corners[3], 0.0F, shown / (float) DEMON_FRAMES, 1.0F,
                (shown + 1) / (float) DEMON_FRAMES, ARGB.color(faint, 0xFFFFFF));
        // Where it was a moment ago, a red afterimage, fading.
        float ghost = formed ? 1.0F - (this.tick - this.ghostAt + frame.partialTick()) / GHOST_TICKS : 0.0F;
        if (ghost > 0.0F && (this.ghostPose != this.pose || this.ghostSnap != this.snap)) {
            Vec3 ghostBase = new Vec3(foot.x, bottom, foot.z).add(stage.across.scale(sway + this.ghostSnap * near + 0.03));
            Vec3[] was = stage.upright(ghostBase, camera, DEMON_WIDTH * 1.04, DEMON_HEIGHT * 1.02, lean);
            int ghostShown = DEMON_FORM_FRAMES + this.ghostPose;
            frame.draw(DEMON, GHOSTS).quad(was[0], was[1], was[2], was[3], 0.0F, ghostShown / (float) DEMON_FRAMES, 1.0F,
                    (ghostShown + 1) / (float) DEMON_FRAMES, ARGB.color(Math.round(150 * ghost), 0xFF2A2A));
        }
        // A pool of shadow where it rose.
        frame.draw(TerrorArt.FOG).flat(this.rise.x, stage.floor + 0.02, this.rise.z, 0.9F * risen, time * 0.3F, ARGB.color(Math.round(200 * risen), 0x000000));
        // Its eyes kindle once it has risen, flickering, brighter as it nears.
        float lit = Mth.clamp((t - RISE_TO) / 0.1F, 0.0F, 1.0F) * (0.75F + 0.25F * Mth.sin(time * 9.0F));
        if (lit <= 0.0F) return;
        VfxDraw glow = frame.draw(TerrorArt.GLOW, TerrorArt.FIGURES);
        int[] eyes = DEMON_FRAME_EYES[this.pose];
        for (int e = 0; e < 4; e += 2) {
            float sideways = (eyes[e] + 0.5F) / 64.0F;
            float down = (eyes[e + 1] + 0.5F) / 128.0F;
            Vec3 top = TerrorStage.lerp(corners[0], corners[3], sideways);
            Vec3 at = TerrorStage.lerp(top, TerrorStage.lerp(corners[1], corners[2], sideways), down);
            glow.billboard(at, 0.05F, ARGB.color(Math.round(255 * lit), 0xFF3018));
            glow.billboard(at, 0.2F + 0.1F * come, ARGB.color(Math.round(110 * lit), 0xB00000));
        }
    }

    // The hands beside the bed: up out of the floor past its side to the eye line, leaning in over it and clawing, now
    // and then snatching across it. They stay there: those that come for the face are the edge hands.
    private void bedsideHands(VfxFrame frame, TerrorStage stage, float t) {
        Vec3 camera = frame.cameraPosition();
        float time = frame.time();
        Vector3fc forward = Minecraft.getInstance().gameRenderer.getMainCamera().forwardVector();
        Vec3 lookFlat = new Vec3(forward.x(), 0.0, forward.z());
        for (BedsideHand hand : this.bedside) {
            float raised = Easing.CUBIC_OUT.apply(Mth.clamp((t - hand.from) / HAND_RISE, 0.0F, 1.0F));
            if (raised <= 0.0F) continue;
            float lunge = (float) Math.pow(Math.max(0.0, Math.sin(time * 2.3F + hand.phase)), 6.0) * Mth.lerp(stage.intensity, 0.6F, 1.0F) * raised;
            double eye = Math.min(camera.y + HAND_OVER_EYES, stage.floor + ARM_REACH) - hand.lower;
            Vec3 tips = new Vec3(hand.at.x, eye - (1.0F - raised) * ARM_REACH, hand.at.z)
                    .subtract(stage.across.scale(hand.side * (0.12 + 0.3 * lunge))).add(0.0, 0.1 * lunge, 0.0);
            Vec3 shoulder = hand.at.add(0.0, -0.25, 0.0).add(stage.across.scale(hand.side * 0.15));
            // The elbow bends out from the bed, square to the arm and the line of sight, always to the same side.
            double bendSide = new Vec3(0.0, 1.0, 0.0).cross(lookFlat).dot(stage.across.scale(hand.side)) >= 0.0 ? 1.0 : -1.0;
            Vec3 ahead = new Vec3(forward.x(), forward.y(), forward.z());
            double segment = HAND_LENGTH * ARM_SEGMENT;
            Vec3 pointing = tips.subtract(shoulder).normalize();
            Vec3 elbow = elbow(shoulder, tips.subtract(pointing.scale(HAND_LENGTH * WRIST)), segment, ahead, bendSide);
            pointing = tips.subtract(elbow).normalize();
            Vec3 base = tips.subtract(pointing.scale(HAND_LENGTH));
            Vec3 wrist = tips.subtract(pointing.scale(HAND_LENGTH * WRIST));
            if (hand.armed) {
                float forearm = (float) (HAND_WIDTH * FOREARM_SHARE);
                Vec3 upperEnd = elbow.add(elbow.subtract(shoulder).normalize().scale(forearm * JOINT_OVERLAP));
                frame.draw(TerrorArt.ARM_UPPER, TerrorArt.HANDS).beam(shoulder, upperEnd, (float) (HAND_WIDTH * UPPER_ARM_SHARE), 0xFFFFFFFF);
                frame.draw(TerrorArt.ARM_LOWER, TerrorArt.HANDS).beam(elbow, wrist.add(pointing.scale(HAND_LENGTH * 0.08)), forearm, 0xFFFFFFFF);
            }
            // Across the hand: square to it and to the line of sight, so it is seen flat on.
            Vec3 sideways = pointing.cross(camera.subtract(tips));
            sideways = (sideways.lengthSqr() < 1.0E-8 ? stage.across : sideways.normalize()).scale(HAND_WIDTH / 2.0);
            Vec3[] corners = {tips.subtract(sideways), base.subtract(sideways), base.add(sideways), tips.add(sideways)};
            float clench = Mth.clamp(0.15F + 0.85F * lunge + 0.1F * Mth.sin(time * 4.0F + hand.phase), 0.0F, 1.0F);
            int shown = Mth.clamp(Math.round(clench * (TerrorArt.HAND_FRAMES - 1)), 0, TerrorArt.HAND_FRAMES - 1);
            float v0 = shown / (float) TerrorArt.HAND_FRAMES;
            float v1 = (shown + 1) / (float) TerrorArt.HAND_FRAMES;
            // Thumbs towards the bed: a hand on the left of the view is drawn mirrored.
            VfxDraw draw = frame.draw(TerrorArt.HAND_VARIANTS[hand.variant], TerrorArt.HANDS);
            if (onLeft(stage, hand.at, camera)) draw.quad(corners[3], corners[2], corners[1], corners[0], 0.0F, v0, 1.0F, v1, 0xFFFFFFFF);
            else draw.quad(corners[0], corners[1], corners[2], corners[3], 0.0F, v0, 1.0F, v1, 0xFFFFFFFF);
            frame.draw(TerrorArt.FOG).flat(shoulder.x, stage.floor + 0.02, shoulder.z, 0.35F * raised, time * 0.4F + hand.phase,
                    ARGB.color(Math.round(170 * raised), 0x000000));
        }
    }

    // The elbow of an arm from a shoulder to a wrist whose pieces are each this long: bent across the view (square to
    // the arm and to the line of sight), to one side or the other of the arm by `side`, as far as it must to reach;
    // never quite straight, so it doesn't snap straight and back as a snatch stretches the arm.
    private static Vec3 elbow(Vec3 shoulder, Vec3 wrist, double segment, Vec3 ahead, double side) {
        Vec3 span = wrist.subtract(shoulder);
        double reach = span.length();
        Vec3 middle = TerrorStage.lerp(shoulder, wrist, 0.5);
        if (reach < 1.0E-6) return middle;
        Vec3 bend = span.cross(ahead).scale(side);
        if (bend.lengthSqr() < 1.0E-10) return middle;
        double pieces = Math.max(segment, reach * 0.6);
        return middle.add(bend.normalize().scale(Math.sqrt(pieces * pieces - reach * reach / 4.0)));
    }

    // Whether a point is on the left of the view, seen from the camera along the bed (towards its foot).
    private static boolean onLeft(TerrorStage stage, Vec3 at, Vec3 camera) {
        Vec3 toward = at.subtract(camera);
        Vec3 ahead = stage.along.scale(-1.0);
        return ahead.x * toward.z - ahead.z * toward.x < 0.0;
    }

    @Override
    public int fogColor(TerrorStage stage, float t) {
        return 0x0D0405;
    }

    @Override
    public int mistColor(TerrorStage stage, float t) {
        return 0x2A0E12;
    }

    // A hand beside the bed: where it comes up, on which side, when (a share of the terror), its own twitch, which
    // variant, whether it is on an arm, and how much lower than the eye line it stays.
    private record BedsideHand(Vec3 at, int side, float from, float phase, int variant, boolean armed, float lower) {
    }
}
