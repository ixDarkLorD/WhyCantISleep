package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcanvas.api.client.camera.CameraShake;
import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffect;
import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffects;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxLayer;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldVfx;
import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.coolcatcanvas.api.client.sound.SoundFilter;
import net.ixdarklord.coolcatcanvas.api.client.sound.SoundFilterProvider;
import net.ixdarklord.coolcatcanvas.api.client.sound.SoundFilters;
import net.ixdarklord.whycantisleep.network.TerrorPayload;
import net.ixdarklord.whycantisleep.sound.WCISSounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A night terror, as the troubled player falls asleep: sleep paralysis. They can't move; their eyes are stuck half open,
 * trembling; the world goes dull and far away and a red-black fog rolls in. Out of the floor at the foot of the bed a
 * shadow gathers and forms itself into a demon, gaunt and hunched, its eyes kindling, and comes slowly along the bed
 * towards them in glitching jerks, bending low until it is face to face; shadow arms reach up beside the bed one after
 * another, their hands clawing, then come at them over the bed, snatching, until they hold round the view with only
 * a window left in its middle, on the demon. A drone swells
 * under it all, whispers come, bones crack, their heart races; then a hit and a shriek, the drone cut dead, and their
 * eyes slam shut. They sleep on, and wake shaken (the server's part) with a gasp, melodic but unresolved, the world's
 * sounds fading back in slowly. Its sounds are the mod's own ({@code tools/gen_terror_sounds.py}).
 * <p>
 * How bad it is ({@link TerrorPayload#intensity()}, from how badly the night was going to go) sets how long it lasts,
 * how many hands come, how close the demon gets, how thick the fog, and how loud it all is. The demon and the hands are pixel-art
 * shadows ({@code tools/gen_terror.py}), flipbooks of frames stacked down their textures, drawn in the world
 * ({@link WorldVfx}), standing in their own place: the floor hides what is still under it, and the bed what is behind
 * it.
 * <p>
 * All round, a dense fog closes in and rolls, in the world (the game's own fog pulled in to a few blocks and
 * breathing, {@code FogRendererMixin}, which leaves the shadows clear; and banks of fog drifting through the room, near
 * and far, low and high), and the surroundings warp ({@code post_effect/terror_warp}: rippling and twisting towards the edges, swelling with each
 * heartbeat).
 */
public final class NightTerrorClient {
    private static final Identifier VFX = WhyCantISleep.id("night_terror");
    // The demon's flipbook (tools/gen_terror.py): DEMON_FORM_FRAMES of it forming out of shadow, then a frame for each
    // glitch pose, with where its eyes are in each (pixels of its 64 by 128 frame, left x, y, right x, y; the script
    // prints them). The hands' flipbooks: HAND_FRAMES of the fingers curling from open to a clenched claw.
    private static final int DEMON_FORM_FRAMES = 8;
    private static final int[][] DEMON_FRAME_EYES = {
            {27, 31, 37, 31}, {29, 31, 39, 31}, {24, 32, 34, 32}, {27, 31, 37, 31},
            {32, 31, 42, 31}, {27, 34, 37, 34}, {27, 31, 37, 31}, {25, 29, 35, 29}};
    private static final int DEMON_FRAMES = DEMON_FORM_FRAMES + DEMON_FRAME_EYES.length;
    private static final int HAND_FRAMES = 5;
    private static final VfxMaterial DEMON = VfxMaterial.alpha(WhyCantISleep.id("textures/vfx/terror_demon.png")).withSmoothing(false).withSoftness(0.1F);
    // The hands' variants (tools/gen_terror.py's HANDS), one picked for each hand; and the arms they reach on, drawn
    // along from the shoulder (out of the floor's shadow) to the elbow, and on to the wrist.
    private static final VfxMaterial[] HAND_VARIANTS = hands("splayed", "grasping", "beckoning", "skeletal", "gnarled");
    private static final VfxMaterial ARM_UPPER = VfxMaterial.alpha(WhyCantISleep.id("textures/vfx/terror_arm_upper.png")).withSmoothing(false).withSoftness(0.1F);
    private static final VfxMaterial ARM_LOWER = VfxMaterial.alpha(WhyCantISleep.id("textures/vfx/terror_arm_lower.png")).withSmoothing(false).withSoftness(0.1F);
    // World effects write no depth of their own, so what is drawn later covers what is drawn earlier. The demon and its
    // eyes go in a layer of their own, under the hands, so every hand covers it; the hands and arms share a sorted
    // layer, so they cover each other by how near each is to the eyes, whatever their textures.
    private static final VfxLayer DEMON_LAYER = VfxLayer.of(10);
    private static final VfxLayer HANDS_LAYER = VfxLayer.sorted(11);
    private static final VfxMaterial EYE = VfxMaterial.additive(VfxMaterial.GLOW);
    private static final VfxMaterial POOL = VfxMaterial.alpha(WhyCantISleep.id("textures/vfx/terror_fog.png"));
    private static final Identifier VIGNETTE = WhyCantISleep.id("hud/minigame_vignette");
    // The dark closing in round the view as they come (tools/gen_terror.py): its edge color, which it is filled with
    // past it, and how far in from each side it closes, as a share of the screen, by the jolt.
    private static final Identifier TERROR_VIGNETTE = WhyCantISleep.id("hud/terror_vignette");
    private static final int TERROR_VIGNETTE_EDGE = 0x0A0003;
    private static final float TERROR_VIGNETTE_CLOSE = 0.16F;
    // The demon's size in blocks; how high its face is in its frame (between the eyes and the mouth, as a share of it
    // from the top); how far beyond the bed's foot it forms; and how far in front of the face it ends, bent low.
    private static final float DEMON_HEIGHT = 2.3F;
    private static final float DEMON_WIDTH = 1.15F;
    private static final float DEMON_FACE_ROW = 36.0F / 128.0F;
    private static final double BEYOND_FOOT = 1.4;
    private static final double BEFORE_FACE = 0.35;
    // The hands: how far out from the bed's middle their arms come up (just past its side, so the bed hides them); how
    // far ahead of the face, from HANDS_FAR down to HANDS_NEAR, near enough to the middle of the view to be seen (in
    // bed it looks straight along the bed); and how far over the eyes their fingers come, so they claw at the eye line,
    // where the half-open eyes see, but no higher than an arm reaches from the floor.
    private static final double BESIDE_BED = 0.62;
    private static final double HANDS_FAR = 2.6;
    private static final double HANDS_NEAR = 1.2;
    private static final double HAND_OVER_EYES = 0.12;
    // The hands that never come for them: how far ahead of the face they rise, and how much lower than the others
    // they may stay.
    private static final double IDLE_NEAR = 1.0;
    private static final double IDLE_FAR = 3.0;
    private static final float IDLE_LOWER = 0.35F;
    private static final double ARM_REACH = 1.1;
    // A hand's size beside the bed, fingertips to the forearm's end; where its wrist is along it (the arm joins there,
    // under the smoke its forearm ends in); and how far its elbow bends out and up, and how far along the arm it is.
    // The arm's strips are as wide as shares of the hand's width that make the forearm as thick at the wrist as the
    // forearm painted on the hand (a share 0.29 of its width, where the forearm's strip is drawn 0.375 of its own),
    // and the upper arm as thick at the elbow as the forearm's knob there; each strip runs on a little under the
    // next, so the joints have no seams.
    private static final double HAND_LENGTH = 0.75;
    private static final double HAND_WIDTH = 0.38;
    private static final double WRIST = 0.72;
    private static final double FOREARM_SHARE = 0.77;
    private static final double UPPER_ARM_SHARE = 0.85;
    private static final double JOINT_OVERLAP = 0.35;
    // Each arm is two pieces this many hand lengths long, bent at the elbow to reach; and coming for them, its shoulder
    // moves off to the hand's own side of the view (by ARM_SHIFT of the way in), this far out from the hand in its
    // segments, so the arm comes in from the side and never lies across the middle of the view.
    private static final double ARM_SEGMENT = 1.25;
    private static final float ARM_SHIFT = 0.45F;
    private static final double SIDE_SHOULDER = 1.7;
    // Having clawed a while beside the bed, they come at the face, over the bed on an arc this high, until the stare,
    // and hold round a window in the view's middle, where the demon's face comes: this far in front of the eyes, the
    // window's size a share of the view's half-height there, each hand fanned evenly round its own side of the view
    // and long enough to reach past the screen's edge, whatever the window's shape.
    private static final float CLAW_WHILE = 0.08F;
    private static final double ARC = 0.35;
    private static final double GRAB_DISTANCE = 0.4;
    private static final double WINDOW_SHARE = 0.32;
    // From the stare, as the eyes go and the view fades, they keep creeping in: the window closing by this share of
    // itself, and the hands growing by this much, by the time the sights are gone.
    private static final double CREEP_WINDOW = 0.45;
    private static final double CREEP_GROW = 0.35;
    // The timeline, as shares of the terror's length: the demon rises, comes along the bed, stares, and the jolt.
    private static final float RISE_FROM = 0.08F;
    private static final float RISE_TO = 0.28F;
    private static final float COME_TO = 0.86F;
    private static final float STARE = 0.86F;
    private static final float JOLT = 0.94F;
    // The eyes slam shut over SHUT_TICKS at the jolt; everything holds until LINGER_TICKS after it, when they are
    // surely shut, so the plain world never shows between.
    private static final float SHUT_TICKS = 3.0F;
    private static final int LINGER_TICKS = 6;
    private static final float HANDS_FROM = 0.24F;
    private static final float HAND_RISE = 0.12F;
    // How shut the eyes are, paralysed; and the heartbeat's gap, from slow to racing, in ticks.
    private static final float PARALYSED = 0.4F;
    private static final float SLOW_BEAT = 26.0F;
    private static final float FAST_BEAT = 5.0F;

    private static long tick;
    private static long since = -1;
    private static int length;
    private static float intensity;
    private static long lastBeat;
    private static long beatAt = Long.MIN_VALUE;
    // Where it all happens: the floor beyond the bed's foot where the demon rises, where it ends, the way from foot to
    // head, across the bed, and the floor's height.
    private static Vec3 rise = Vec3.ZERO;
    private static Vec3 end = Vec3.ZERO;
    private static Vec3 along = Vec3.ZERO;
    private static Vec3 across = Vec3.ZERO;
    private static double floor;
    private static Vec3 head = Vec3.ZERO;
    private static boolean placed;
    private static boolean inBed;
    private static final int BED_WAIT = 40;
    private static final List<Hand> HANDS = new ArrayList<>();
    // Formed, the demon moves in glitches: the pose shown, until when, and how far it has jumped aside meanwhile.
    private static final RandomSource GLITCH = RandomSource.create();
    private static int glitchPose;
    private static long glitchUntil;
    private static double snap;
    private static final List<Puff> PUFFS = new ArrayList<>();
    private static volatile SoundFilter muffle = SoundFilter.NONE;
    // The fog: how far it closes in (the far end of it, in blocks, rolling round this by FOG_ROLL), and its color.
    private static final float FOG_END = 4.5F;
    private static final float FOG_ROLL = 1.5F;
    private static final float[] FOG_COLOR = {0.05F, 0.015F, 0.02F};
    // The banks of fog rolling through the room: how many, and how far out from the bed they reach.
    private static final int PUFF_COUNT = 40;
    private static final float PUFF_REACH = 7.0F;
    private static final Identifier WARP_ID = WhyCantISleep.id("terror_warp");
    private static ScreenEffect warp;
    // The drone playing under the terror, and when they woke from one (the world's sounds coming back over
    // WAKE_FADE_TICKS).
    private static @Nullable SimpleSoundInstance drone;
    private static long wokeAt = Long.MIN_VALUE;
    private static final float WAKE_FADE_TICKS = 70.0F;

    private NightTerrorClient() {
    }

    public static void init() {
        ClientTickEvents.END.register(NightTerrorClient::tick);
        ClientGuiEvents.RENDER_HUD.register(NightTerrorClient::render);
        ClientPlayerEvents.LEAVE.register(player -> stop());
        WorldVfx.register(VFX, NightTerrorClient::renderWorld);
        SoundFilters.register(SoundFilterProvider.inWorld(position -> muffle));
        warp = ScreenEffects.register(WARP_ID, WARP_ID).fade(20, 8, Easing.SINE_IN_OUT).bindUniform("Beat", context -> beat(0.0F));
    }

    /** Whether a night terror is running: it has the eyes until it ends. */
    public static boolean running() {
        return since >= 0;
    }

    public static void on(TerrorPayload payload) {
        if (!payload.start()) {
            // Woken from it (the server says so only after one); the bed's eyes open them (SleepEyes).
            stop();
            wake(Minecraft.getInstance(), false);
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        since = tick;
        length = payload.ticks();
        intensity = Mth.clamp(payload.intensity(), 0.0F, 1.0F);
        lastBeat = tick;
        beatAt = Long.MIN_VALUE;
        // Had asleep, laid out along the bed once the game shows them in it (a terror that comes as they lie down gets
        // here before the game says they are in bed), or after BED_WAIT ticks at most; had awake, ahead of their look,
        // straight away.
        placed = false;
        inBed = payload.asleep();
        if (player.isSleeping() || !inBed) lay(player);
        RandomSource random = RandomSource.create();
        glitchPose = 0;
        glitchUntil = 0;
        snap = 0.0;
        PUFFS.clear();
        for (int i = 0; i < PUFF_COUNT; i++) {
            // Low and heavy near the floor mostly, some higher; near and far, bigger the farther.
            float out = 1.0F + random.nextFloat() * (PUFF_REACH - 1.0F);
            float height = random.nextFloat() < 0.7F ? random.nextFloat() * 0.7F : 0.7F + random.nextFloat() * 1.6F;
            PUFFS.add(new Puff(random.nextFloat() * 6.28F, out, height, 0.8F + out * 0.2F + random.nextFloat() * 0.6F,
                    random.nextFloat() * 6.28F, 0.04F + random.nextFloat() * 0.05F));
        }
        warp.setUniform("Amount", 0.6F + 0.6F * intensity);
        warp.enableFor(joltTick() + LINGER_TICKS);
        // Paralysed: the eyes come open part way and stay there, trembling.
        Eyelids.play(16.0F, Eyelids.Mood.PARALYSED, 0.0F, 0.0F, 1.0F, PARALYSED);
        // The drone under it all, kept to be cut off at the jolt.
        wokeAt = Long.MIN_VALUE;
        drone = SimpleSoundInstance.forUI(WCISSounds.TERROR_DRONE, 1.0F, 0.55F + 0.45F * intensity);
        minecraft.getSoundManager().play(drone);
        muffle = new SoundFilter(0.55F, 0.12F, 0.25F + 0.35F * intensity);
        SoundFilters.refresh();
    }

    // Where it all happens, from the bed (or the look), and the hands placed beside it.
    private static void lay(LocalPlayer player) {
        placed = true;
        place(player);
        RandomSource random = RandomSource.create();
        HANDS.clear();
        // Two or three each side, each wide enough round the window to meet the next.
        int hands = 4 + Math.round(2.0F * intensity);
        for (int i = 0; i < hands; i++) {
            // Alternating sides, anywhere along the bed and a little further out or in, coming up at their own times; and
            // its place round the window, spread up and down among the others of its side.
            int side = i % 2 == 0 ? 1 : -1;
            int onSide = side > 0 ? (hands + 1) / 2 : hands / 2;
            int ofSide = i / 2;
            double ahead = HANDS_NEAR + random.nextFloat() * (HANDS_FAR - HANDS_NEAR);
            Vec3 at = head.subtract(along.scale(ahead)).add(across.scale(side * (BESIDE_BED + random.nextFloat() * 0.2)));
            HANDS.add(new Hand(new Vec3(at.x, floor, at.z), side, HANDS_FROM + random.nextFloat() * 0.2F, random.nextFloat() * 6.28F,
                    random.nextInt(HAND_VARIANTS.length), ofSide - (onSide - 1) / 2.0F, onSide, true, 0.0F));
        }
        // And more that never come for them: rising here and there along both sides, some lower than others, clawing and
        // snatching over the bed.
        int idle = 1 + Math.round(2.0F * intensity);
        boolean leftFirst = random.nextBoolean();
        for (int i = 0; i < idle; i++) {
            // Alternating sides, each in its own stretch of its side of the bed (with a little give), so they spread
            // along it rather than bunching up.
            int side = (i % 2 == 0) == leftFirst ? 1 : -1;
            int onSide = (i % 2 == 0) ? (idle + 1) / 2 : idle / 2;
            double stretch = (IDLE_FAR - IDLE_NEAR) / onSide;
            double ahead = IDLE_NEAR + stretch * (i / 2 + 0.2 + 0.6 * random.nextFloat());
            Vec3 at = head.subtract(along.scale(ahead)).add(across.scale(side * (BESIDE_BED + random.nextFloat() * 0.3)));
            HANDS.add(new Hand(new Vec3(at.x, floor, at.z), side, HANDS_FROM + random.nextFloat() * 0.35F, random.nextFloat() * 6.28F,
                    random.nextInt(HAND_VARIANTS.length), 0.0F, 1, false, random.nextFloat() * IDLE_LOWER));
        }
    }

    // Out of it: a gasp, melodic and tense, the world's sounds fading back in, and the eyes opening if asked to (with the
    // flutter of waking, as in bed).
    private static void wake(Minecraft minecraft, boolean openEyes) {
        wokeAt = tick;
        play(minecraft, WCISSounds.TERROR_WAKE, 0.9F, 1.0F);
        if (openEyes) Eyelids.play(24.0F, Eyelids.Mood.STILL, 0.0F, 0.0F, 0.2F, 0.55F, 0.3F, 0.85F, 0.5F, 0.25F, 0.6F, 0.4F, 1.0F, 0.0F);
    }

    private static void stop() {
        since = -1;
        HANDS.clear();
        PUFFS.clear();
        muffle = SoundFilter.NONE;
        if (warp != null) warp.disable();
        if (drone != null) Minecraft.getInstance().getSoundManager().stop(drone);
        drone = null;
    }

    /** How far into its fog the terror is, 0 to 1: rolling in at the start, and there until the eyes are shut. */
    private static float fogged(float partialTicks) {
        float age = tick - since + partialTicks;
        return held(age) ? Mth.clamp(age / (0.12F * length), 0.0F, 1.0F) : 0.0F;
    }

    // The tick of the jolt, into the terror.
    private static int joltTick() {
        return Math.round(JOLT * length);
    }

    // Whether the terror's sights are still up this far in: until a little after the jolt, the eyes shut by then.
    private static boolean held(float age) {
        return age < joltTick() + LINGER_TICKS;
    }

    // How strongly the last heartbeat throbs, 0 to 1.
    private static float beat(float partialTicks) {
        return beatAt == Long.MIN_VALUE ? 0.0F : 1.0F - Mth.clamp((tick - beatAt + partialTicks) / 8.0F, 0.0F, 1.0F);
    }

    /**
     * The game's fog, closed in round the bed while a terror runs: to a few blocks, rolling nearer and farther, thinner
     * the milder the terror, in a dark red-black. The shadows aren't fogged: they are drawn as world effects.
     */
    public static void fog(FogData data, float partialTicks) {
        if (since < 0) return;
        float in = fogged(partialTicks);
        if (in <= 0.0F) return;
        float time = tick + partialTicks;
        float end = (FOG_END + FOG_ROLL * Mth.sin(time * 0.06F) + 0.8F * Mth.sin(time * 0.023F + 1.0F)) * (1.4F - 0.4F * intensity);
        data.environmentalStart = Mth.lerp(in, data.environmentalStart, 0.0F);
        data.environmentalEnd = Mth.lerp(in, data.environmentalEnd, end);
        data.renderDistanceStart = Mth.lerp(in, data.renderDistanceStart, 0.0F);
        data.renderDistanceEnd = Mth.lerp(in, data.renderDistanceEnd, end);
        data.skyEnd = Mth.lerp(in, data.skyEnd, end);
        data.cloudEnd = Mth.lerp(in, data.cloudEnd, end);
        data.color.set(Mth.lerp(in, data.color.x(), FOG_COLOR[0]), Mth.lerp(in, data.color.y(), FOG_COLOR[1]), Mth.lerp(in, data.color.z(), FOG_COLOR[2]),
                data.color.w());
    }

    // From the bed: the demon rises beyond its foot and ends just before the face; the hands come up beside it. With no
    // bed (a terror sent while awake), the same laid out ahead of where they look.
    private static void place(LocalPlayer player) {
        Optional<BlockPos> bed = player.getSleepingPos();
        Direction facing = bed.isPresent() ? player.getBedOrientation() : null;
        Vec3 head;
        if (bed.isPresent() && facing != null) {
            head = Vec3.atBottomCenterOf(bed.get());
            along = Vec3.atLowerCornerOf(facing.getUnitVec3i());
        } else {
            Vec3 look = player.getLookAngle();
            along = new Vec3(-look.x, 0.0, -look.z).normalize();
            head = player.position();
        }
        NightTerrorClient.head = new Vec3(head.x, Math.floor(head.y), head.z);
        floor = Math.floor(head.y);
        across = new Vec3(-along.z, 0.0, along.x);
        // The foot of the bed is a block from the head; the demon rises a little beyond it.
        Vec3 from = head.subtract(along.scale(1.0 + BEYOND_FOOT));
        Vec3 to = head.subtract(along.scale(BEFORE_FACE));
        rise = new Vec3(from.x, floor, from.z);
        end = new Vec3(to.x, floor, to.z);
    }

    private static void tick(Minecraft minecraft) {
        // Paused, the terror waits with the world (in singleplayer the server waits too): nothing comes closer, the
        // heart keeps its beat.
        if (minecraft.isPaused()) return;
        tick++;
        // Woken from it: the world's sounds come back slowly, not all at once.
        if (wokeAt != Long.MIN_VALUE) {
            float back = Easing.SINE_IN_OUT.apply(Mth.clamp((tick - wokeAt) / WAKE_FADE_TICKS, 0.0F, 1.0F));
            muffle = back >= 1.0F ? SoundFilter.NONE : new SoundFilter(Mth.lerp(back, 0.5F, 1.0F), Mth.lerp(back, 0.1F, 1.0F), Mth.lerp(back, 0.4F, 0.0F));
            if (back >= 1.0F) wokeAt = Long.MIN_VALUE;
            SoundFilters.refresh();
        }
        if (since < 0) return;
        long age = tick - since;
        if (!placed && minecraft.player != null && (minecraft.player.isSleeping() || !inBed || age >= BED_WAIT)) lay(minecraft.player);
        if (age > length || minecraft.player == null) {
            // Awake (given one by a command), the eyes open again: asleep, sleep goes on and the morning opens them.
            boolean awake = minecraft.player != null && !minecraft.player.isSleeping();
            stop();
            if (awake) wake(minecraft, true);
            return;
        }
        float t = age / (float) length;
        // The world dull and far away, and rough; worse the worse the terror.
        muffle = new SoundFilter(0.55F, 0.12F, 0.25F + 0.35F * intensity);
        // The heart, slow at first and racing by the end.
        float gap = Mth.lerp(Easing.QUAD_IN.apply(Mth.clamp(t / JOLT, 0.0F, 1.0F)), SLOW_BEAT, FAST_BEAT);
        if (t < JOLT && tick - lastBeat >= gap) {
            lastBeat = tick;
            beatAt = tick;
            play(minecraft, WCISSounds.HEARTBEAT_BEAT, 0.5F + 0.5F * t, 0.85F + 0.25F * t);
        }
        // Formed, the demon glitches: a pose held a few ticks, or flicked through in one, and now and then a jump aside
        // (more the worse the terror), back again at the next.
        if (t >= RISE_TO && tick >= glitchUntil) {
            glitchPose = GLITCH.nextFloat() < 0.3F ? 0 : GLITCH.nextInt(DEMON_FRAME_EYES.length);
            glitchUntil = tick + (GLITCH.nextFloat() < 0.3F ? 1 : 2 + GLITCH.nextInt(8));
            snap = GLITCH.nextFloat() < 0.15F + 0.2F * intensity ? (GLITCH.nextBoolean() ? 1 : -1) * (0.08 + GLITCH.nextFloat() * 0.14) : 0.0;
        }
        // A tremble all through, as if straining to move.
        if (age % 10 == 0 && t < JOLT) CameraShake.shake(0.25F + 0.35F * intensity * t, 12);
        if (age == Math.round(RISE_FROM * length)) play(minecraft, WCISSounds.TERROR_EMERGE, 0.8F + 0.2F * intensity, 1.0F);
        if (age == Math.round(0.45F * length)) play(minecraft, WCISSounds.TERROR_WHISPER, 0.8F, 1.0F);
        if (intensity > 0.6F && age == Math.round(0.7F * length)) play(minecraft, WCISSounds.TERROR_WHISPER, 0.9F, 0.85F);
        for (Hand hand : HANDS) {
            if (age == Math.round(hand.from * length)) {
                play(minecraft, WCISSounds.TERROR_CREEP, (0.5F + 0.3F * intensity) * (hand.grabs ? 1.0F : 0.5F), 0.85F + hand.phase * 0.05F);
            }
        }
        if (age == Math.round(STARE * length)) {
            play(minecraft, WCISSounds.TERROR_STARE, 0.8F, 1.0F);
            // Face to face: the eyes go wide.
            Eyelids.play(6.0F, Eyelids.Mood.PARALYSED, 0.0F, 0.0F, 1.0F, 0.12F);
        }
        if (age == Math.round(JOLT * length)) {
            // The drone cut off dead under the hit.
            if (drone != null) minecraft.getSoundManager().stop(drone);
            drone = null;
            play(minecraft, WCISSounds.TERROR_JOLT, 0.7F + 0.3F * intensity, 1.0F);
            CameraShake.shake(2.0F + 4.0F * intensity, 14);
            // And the eyes slam shut; they sleep on.
            Eyelids.play(SHUT_TICKS, Eyelids.Mood.STILL, 0.0F, 0.0F, 1.0F, 1.0F);
        }
    }

    private static void play(Minecraft minecraft, SoundEvent sound, float volume, float pitch) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    // The demon and the hands, in the world.
    private static void renderWorld(VfxFrame frame) {
        if (since < 0 || !placed) return;
        float age = tick - since + frame.partialTick();
        float t = age / length;
        if (!held(age)) return;
        Vec3 camera = frame.cameraPosition();
        float time = frame.time();
        // Banks of fog rolling through the room: all drifting one way across it, slowly turning and swelling, and
        // coming round again from the other side; fading in and out as they go, so none pops.
        float fog = fogged(frame.partialTick());
        if (fog > 0.0F) {
            Vec3 middle = lerp(rise, end, 0.5);
            Vec3 drift = across.scale(0.8).add(along.scale(0.6)).normalize();
            VfxDraw smoke = frame.draw(POOL);
            for (Puff puff : PUFFS) {
                // Where it starts round the bed, carried along the drift and round again across the room's width.
                double travel = (puff.radius * Mth.cos(puff.angle) + time * puff.speed * 10.0 + PUFF_REACH) % (2.0 * PUFF_REACH) - PUFF_REACH;
                double sideways = puff.radius * Mth.sin(puff.angle);
                Vec3 at = middle.add(drift.scale(travel)).add(new Vec3(-drift.z, 0.0, drift.x).scale(sideways));
                double y = floor + puff.height + 0.15 * Mth.sin(time * 0.4F + puff.phase);
                // Thinner at the ends of its run, where it comes round.
                float edge = (float) Mth.clamp((PUFF_REACH - Math.abs(travel)) / 1.5, 0.0, 1.0);
                float swell = 1.0F + 0.15F * Mth.sin(time * 0.35F + puff.phase);
                int alpha = Math.round(fog * edge * (95 + 35 * Mth.sin(time * 0.3F + puff.phase)));
                if (alpha > 0) smoke.billboard(at.x, y, at.z, puff.size * swell, time * 0.05F + puff.phase, ARGB.color(alpha, 0x2A0E12));
            }
        }
        // The demon: shadow welling up out of the floor and forming itself into it, then coming along the bed in
        // glitches, bending low until its face is at theirs.
        float forming = Mth.clamp((t - RISE_FROM) / (RISE_TO - RISE_FROM), 0.0F, 1.0F);
        float risen = Easing.CUBIC_OUT.apply(forming);
        if (forming > 0.0F) {
            float come = Easing.SINE_IN_OUT.apply(Mth.clamp((t - RISE_TO) / (COME_TO - RISE_TO), 0.0F, 1.0F)) * Mth.lerp(intensity, 0.8F, 1.0F);
            Vec3 foot = rise.add(end.subtract(rise).scale(come));
            // Its face is this high above its feet; as it comes, it sinks into the floor until its face is in the middle
            // of the view (where the line of sight crosses it), there by the stare whatever the terror.
            float faceHeight = DEMON_HEIGHT * (1.0F - DEMON_FACE_ROW);
            Vector3fc looking = Minecraft.getInstance().gameRenderer.getMainCamera().forwardVector();
            double flat = Math.max(0.1, Math.hypot(looking.x(), looking.z()));
            double sight = camera.y + looking.y() / flat * Math.hypot(foot.x - camera.x, foot.z - camera.z) - floor;
            float settle = Easing.SINE_IN_OUT.apply(Mth.clamp((t - RISE_TO) / (STARE - RISE_TO), 0.0F, 1.0F));
            double bend = settle * Math.max(0.0, faceHeight - sight);
            double bottom = floor - DEMON_HEIGHT * 0.35 * (1.0F - risen) - bend;
            // A slow sway, a tremor as it nears, and its glitches' jumps aside, smaller the nearer it is, so close up its
            // face stays in the window the hands leave.
            double sway = 0.04 * Math.sin(time * 1.3) + 0.015 * come * Math.sin(time * 37.0) + (t >= RISE_TO ? snap * (1.0 - 0.85 * come) : 0.0);
            Vec3 base = new Vec3(foot.x, bottom, foot.z).add(across.scale(sway));
            Vec3[] corners = upright(base, camera, DEMON_WIDTH, DEMON_HEIGHT, along.scale(0.25 * come));
            // Forming, its frames run from formless shadow to its shape; formed, the glitch pose. It wells up faint.
            int shown = t < RISE_TO ? Math.min(DEMON_FORM_FRAMES - 1, (int) (forming * DEMON_FORM_FRAMES)) : DEMON_FORM_FRAMES + glitchPose;
            int faint = Math.round(255 * Mth.clamp(forming * 3.0F, 0.0F, 1.0F));
            frame.draw(DEMON, DEMON_LAYER).quad(corners[0], corners[1], corners[2], corners[3], 0.0F, shown / (float) DEMON_FRAMES, 1.0F,
                    (shown + 1) / (float) DEMON_FRAMES, ARGB.color(faint, 0xFFFFFF));
            // A pool of shadow where it rose.
            frame.draw(POOL).flat(rise.x, floor + 0.02, rise.z, 0.9F * risen, time * 0.3F, ARGB.color(Math.round(200 * risen), 0x000000));
            // Its eyes kindle once it has risen, flickering, brighter as it nears.
            float lit = Mth.clamp((t - RISE_TO) / 0.1F, 0.0F, 1.0F) * (0.75F + 0.25F * Mth.sin(time * 9.0F));
            if (lit > 0.0F) {
                VfxDraw glow = frame.draw(EYE, DEMON_LAYER);
                int[] eyes = DEMON_FRAME_EYES[glitchPose];
                for (int e = 0; e < 4; e += 2) {
                    // Across the quad at the eye's share, then down it.
                    float sideways = (eyes[e] + 0.5F) / 64.0F;
                    float down = (eyes[e + 1] + 0.5F) / 128.0F;
                    Vec3 top = lerp(corners[0], corners[3], sideways);
                    Vec3 at = lerp(top, lerp(corners[1], corners[2], sideways), down);
                    glow.billboard(at, 0.05F, ARGB.color(Math.round(255 * lit), 0xFF3018));
                    glow.billboard(at, 0.2F + 0.1F * come, ARGB.color(Math.round(110 * lit), 0xB00000));
                }
            }
        }
        // The hands: arms of shadow out of the floor beside the bed, bent at the elbow, their hands rising past its side to
        // the eye line and clawing; then coming at the face over the bed, snatching at it, growing as they near, until
        // they hold round the window, sized to the view, palms to the eyes and fingers pointing in, their arms leaving
        // past the screen's edge.
        Minecraft minecraft = Minecraft.getInstance();
        Camera view = minecraft.gameRenderer.getMainCamera();
        Vector3fc forward = view.forwardVector();
        Vector3fc left = view.leftVector();
        Vector3fc up = view.upVector();
        Vec3 ahead = new Vec3(forward.x(), forward.y(), forward.z());
        Vec3 right = new Vec3(-left.x(), -left.y(), -left.z());
        Vec3 upward = new Vec3(up.x(), up.y(), up.z());
        // The view at the hands' distance, from its field of view and the window's shape: half its height and width.
        double halfHeight = GRAB_DISTANCE * Math.tan(Math.toRadians(view.getFov()) / 2.0);
        double halfWidth = halfHeight * minecraft.getWindow().getWidth() / Math.max(1.0, minecraft.getWindow().getHeight());
        // Held round the window from the stare, they creep in on the eyes, slowly, until the sights are gone.
        float creep = Easing.SINE_IN_OUT.apply(Mth.clamp((age - STARE * length) / (joltTick() + LINGER_TICKS - STARE * length), 0.0F, 1.0F));
        double window = halfHeight * WINDOW_SHARE * (1.0 - CREEP_WINDOW * creep);
        double grow = 1.0 + CREEP_GROW * creep;
        Vec3 middle = camera.add(ahead.scale(GRAB_DISTANCE));
        for (Hand hand : HANDS) {
            float raised = Easing.CUBIC_OUT.apply(Mth.clamp((t - hand.from) / HAND_RISE, 0.0F, 1.0F));
            if (raised <= 0.0F) continue;
            float from = hand.from + HAND_RISE + CLAW_WHILE;
            // Only those that grab come for them; the rest stay beside the bed.
            float approach = hand.grabs ? Easing.SINE_IN_OUT.apply(Mth.clamp((t - from) / Math.max(0.05F, STARE - from), 0.0F, 1.0F)) : 0.0F;
            // Now and then it snatches at them: closer for a moment, its fingers snapping shut.
            float lunge = (float) Math.pow(Math.max(0.0, Math.sin(time * 2.3F + hand.phase)), 6.0) * Mth.lerp(intensity, 0.6F, 1.0F) * raised;
            float closing = Mth.clamp(approach + (hand.grabs ? 0.2F : 0.12F) * lunge * (1.0F - approach), 0.0F, 1.0F);
            // How far round it has turned, and its arm over to its side, follow only its steady approach: a snatch moves
            // the hand, not the whole arm, so the arm doesn't swing about with each one.
            float turn = approach * approach;
            boolean onLeft = onLeft(hand.at, camera);
            // Beside the bed, at the eye line, leaning in over it; risen out of the floor.
            double eye = Math.min(camera.y + HAND_OVER_EYES, floor + ARM_REACH) - hand.lower;
            Vec3 bedside = new Vec3(hand.at.x, eye - (1.0F - raised) * ARM_REACH, hand.at.z).subtract(across.scale(hand.side * 0.12));
            // Its place round the window, fanned evenly round its own side of the view; how far the screen's edge is
            // that way, so it is long enough to reach past it, and wide enough to close the gap to the next.
            double spacing = Math.PI / hand.onSide;
            double angle = (onLeft ? Math.PI - hand.slot * spacing : hand.slot * spacing) + 0.04 * Math.sin(time * 2.0F + hand.phase);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Vec3 out = right.scale(cos).add(upward.scale(sin));
            double edge = Math.min(halfWidth / Math.max(1.0E-4, Math.abs(cos)), halfHeight / Math.max(1.0E-4, Math.abs(sin)));
            double coverLength = ((edge - window) * 1.25 + 0.03) * grow;
            // Wide enough to meet the next round the window, a little over, so there is no gap between them.
            double coverWidth = Mth.clamp((window + (edge - window) * 0.5) * spacing * 1.5, coverLength * 0.45, coverLength * 1.1) * grow;
            Vec3 near = middle.add(out.scale(window * (1.0 + 0.08 * Math.sin(time * 5.0F + hand.phase))));
            // Over the bed on an arc, up and then in at the face, growing to its size round the window.
            Vec3 tips = bezier(bedside, lerp(bedside, near, 0.5).add(0.0, ARC, 0.0), near, closing);
            double length = Mth.lerp(turn, HAND_LENGTH, coverLength);
            double width = Mth.lerp(turn, HAND_WIDTH, coverWidth);
            // Its arm: two pieces of a length to the hand's, bent at the elbow. Beside the bed its shoulder is in the floor
            // past the bed's side, the elbow bent out over the floor and up; coming for them, the shoulder moves off to the
            // hand's own side of the view, out past it, the elbow bent down and out, so the arm comes in from that side.
            double segment = length * ARM_SEGMENT;
            float shift = Easing.SINE_IN_OUT.apply(Mth.clamp(approach / ARM_SHIFT, 0.0F, 1.0F));
            Vec3 floorShoulder = new Vec3(hand.at.x, floor - 0.25, hand.at.z).add(across.scale(hand.side * 0.15));
            Vec3 sideShoulder = tips.add(out.scale(segment * SIDE_SHOULDER)).subtract(upward.scale(segment * 0.35 * (1.0F - turn))).add(ahead.scale(0.12 * turn));
            Vec3 shoulder = lerp(floorShoulder, sideShoulder, shift);
            // Which way its elbow bends: square to the arm across the view, always to the same side of it, chosen so that
            // standing up beside the bed it bends out away from the bed. As the arm turns the elbow turns with it, and
            // never flips over to the other side.
            Vec3 lookFlat = new Vec3(ahead.x, 0.0, ahead.z);
            double bendSide = new Vec3(0.0, 1.0, 0.0).cross(lookFlat).dot(across.scale(hand.side)) >= 0.0 ? 1.0 : -1.0;
            // The hand lies along its forearm so the two meet in line; turning as it nears to show its palm, its fingers
            // pointing in. Found twice: along the arm held straight, then along the forearm the elbow makes.
            Vec3 pointing = tips.subtract(shoulder).normalize().scale(1.0F - turn).add(out.scale(-turn)).normalize();
            Vec3 elbow = elbow(shoulder, tips.subtract(pointing.scale(length * WRIST)), segment, ahead, bendSide);
            pointing = tips.subtract(elbow).normalize().scale(1.0F - turn).add(out.scale(-turn)).normalize();
            Vec3 base = tips.subtract(pointing.scale(length));
            Vec3 wrist = tips.subtract(pointing.scale(length * WRIST));
            // Only the hands that come for them have arms; the rest are hands alone, out of the dark beside the bed.
            if (hand.grabs) {
                float forearm = (float) (width * FOREARM_SHARE);
                Vec3 upperEnd = elbow.add(elbow.subtract(shoulder).normalize().scale(forearm * JOINT_OVERLAP));
                Vec3 forearmEnd = wrist.add(pointing.scale(length * 0.08));
                frame.draw(ARM_UPPER, HANDS_LAYER).beam(shoulder, upperEnd, (float) (width * UPPER_ARM_SHARE), 0xFFFFFFFF);
                frame.draw(ARM_LOWER, HANDS_LAYER).beam(elbow, forearmEnd, forearm, 0xFFFFFFFF);
            }
            // Across the hand: square to it and to the line of sight, so it is seen flat on as it turns.
            Vec3 sideways = pointing.cross(camera.subtract(tips));
            sideways = (sideways.lengthSqr() < 1.0E-8 ? across : sideways.normalize()).scale(width / 2.0);
            Vec3[] corners = {tips.subtract(sideways), base.subtract(sideways), base.add(sideways), tips.add(sideways)};
            // Clawing: snapping shut with each snatch; held round the window, a slow clench.
            float clench = Mth.clamp(0.15F + 0.85F * lunge + 0.1F * Mth.sin(time * 4.0F + hand.phase), 0.0F, 1.0F);
            clench = Mth.lerp(Mth.clamp((approach - 0.85F) / 0.15F, 0.0F, 1.0F), clench, 0.55F + 0.25F * Mth.sin(time * 1.5F + hand.phase));
            int shown = Mth.clamp(Math.round(clench * (HAND_FRAMES - 1)), 0, HAND_FRAMES - 1);
            float v0 = shown / (float) HAND_FRAMES;
            float v1 = (shown + 1) / (float) HAND_FRAMES;
            // Thumbs towards the bed: a hand on the left of the view is drawn mirrored.
            VfxDraw draw = frame.draw(HAND_VARIANTS[hand.variant], HANDS_LAYER);
            if (onLeft) draw.quad(corners[3], corners[2], corners[1], corners[0], 0.0F, v0, 1.0F, v1, 0xFFFFFFFF);
            else draw.quad(corners[0], corners[1], corners[2], corners[3], 0.0F, v0, 1.0F, v1, 0xFFFFFFFF);
            frame.draw(POOL).flat(floorShoulder.x, floor + 0.02, floorShoulder.z, 0.35F * raised * (1.0F - turn), time * 0.4F + hand.phase,
                    ARGB.color(Math.round(170 * raised * (1.0F - turn)), 0x000000));
        }
    }

    // The elbow of an arm from a shoulder to a wrist whose pieces are each this long: bent across the view (square to
    // the arm and to the line of sight), to one side or the other of the arm by `side`, as far as it must to reach.
    private static Vec3 elbow(Vec3 shoulder, Vec3 wrist, double segment, Vec3 ahead, double side) {
        Vec3 span = wrist.subtract(shoulder);
        double reach = span.length();
        Vec3 middle = lerp(shoulder, wrist, 0.5);
        if (reach < 1.0E-6) return middle;
        Vec3 bend = span.cross(ahead).scale(side);
        if (bend.lengthSqr() < 1.0E-10) return middle;
        // Never quite straight: the pieces are at least long enough to keep a bend however far it reaches, so the elbow
        // doesn't snap straight and back as a snatch stretches the arm.
        double pieces = Math.max(segment, reach * 0.6);
        double rise = Math.sqrt(pieces * pieces - reach * reach / 4.0);
        return middle.add(bend.normalize().scale(rise));
    }

    // A point along a curve from a to c, drawn towards b, at t from 0 to 1.
    private static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, double t) {
        return lerp(lerp(a, b, t), lerp(b, c, t), t);
    }

    /**
     * An upright quad standing on a point and turned to the camera about the vertical, its top leaning this way: the
     * corners top left, bottom left, bottom right, top right.
     */
    private static Vec3[] upright(Vec3 base, Vec3 camera, float width, float height, Vec3 lean) {
        Vec3 toCamera = new Vec3(camera.x - base.x, 0.0, camera.z - base.z);
        Vec3 right = toCamera.lengthSqr() < 1.0E-6 ? across : new Vec3(toCamera.z, 0.0, -toCamera.x).normalize();
        Vec3 half = right.scale(width / 2.0);
        Vec3 top = base.add(0.0, height, 0.0).add(lean);
        return new Vec3[]{top.subtract(half), base.subtract(half), base.add(half), top.add(half)};
    }

    // Whether a point is on the left of the view, seen from the camera along the bed (towards its foot).
    private static boolean onLeft(Vec3 at, Vec3 camera) {
        Vec3 toward = at.subtract(camera);
        Vec3 ahead = along.scale(-1.0);
        return ahead.x * toward.z - ahead.z * toward.x < 0.0;
    }

    private static VfxMaterial[] hands(String... names) {
        VfxMaterial[] materials = new VfxMaterial[names.length];
        for (int i = 0; i < names.length; i++) {
            materials[i] = VfxMaterial.alpha(WhyCantISleep.id("textures/vfx/terror_hand_" + names[i] + ".png")).withSmoothing(false).withSoftness(0.1F);
        }
        return materials;
    }

    private static Vec3 lerp(Vec3 from, Vec3 to, double share) {
        return from.add(to.subtract(from).scale(share));
    }

    // The dark closing in round the view as the demon and the hands come, and the screen's edge a little darker red on
    // each heartbeat.
    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (since < 0) return;
        float partial = delta.getGameTimeDeltaPartialTick(true);
        float age = tick - since + partial;
        if (!held(age)) return;
        float in = Mth.clamp(age / (0.1F * length), 0.0F, 1.0F);
        float t = age / length;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        // How near they have come: from the demon formed to the stare, and on to the jolt; darker and closer in the
        // nearer, worse the worse the terror, breathing a little with the heart. The middle stays clear, where the
        // demon's face is.
        float near = Easing.SINE_IN_OUT.apply(Mth.clamp((t - RISE_FROM) / (JOLT - RISE_FROM), 0.0F, 1.0F));
        float throb = beat(partial);
        float dark = in * Mth.clamp(0.25F + 0.75F * near * Mth.lerp(intensity, 0.8F, 1.0F) + 0.06F * throb, 0.0F, 1.0F);
        if (dark > 0.0F) {
            int insetX = Math.round(width * TERROR_VIGNETTE_CLOSE * near * (1.0F + 0.03F * throb));
            int insetY = Math.round(height * TERROR_VIGNETTE_CLOSE * near * (1.0F + 0.03F * throb));
            int edge = ARGB.color(Math.round(255 * dark), TERROR_VIGNETTE_EDGE);
            // Past the vignette, as it closes in, the dark it ends in.
            if (insetY > 0) {
                graphics.fill(0, 0, width, insetY, edge);
                graphics.fill(0, height - insetY, width, height, edge);
            }
            if (insetX > 0) {
                graphics.fill(0, insetY, insetX, height - insetY, edge);
                graphics.fill(width - insetX, insetY, width, height - insetY, edge);
            }
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TERROR_VIGNETTE, insetX, insetY, width - 2 * insetX, height - 2 * insetY, ARGB.white(dark));
        }
        // Faint: the fog is in the world; this is only the heart in the eyes.
        float alpha = in * (0.15F + 0.15F * intensity + 0.3F * throb);
        if (alpha <= 0.0F) return;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, VIGNETTE, 0, 0, width, height, ARGB.white(Mth.clamp(alpha, 0.0F, 1.0F)));
    }

    // A hand: where it comes up, on which side of the bed, when (a share of the terror), its own twitch, which variant
    // it is, and its place round the window among the others of its side (0 the middle, up and down from it), and how
    // many there are on its side; whether it comes for them at all, and how much lower than the eye line it stays.
    private record Hand(Vec3 at, int side, float from, float phase, int variant, float slot, int onSide, boolean grabs, float lower) {
    }

    // A bank of fog: where it starts round the bed (angle, distance, height), how big, its own turn and swell, and how
    // fast it drifts across.
    private record Puff(float angle, float radius, float height, float size, float phase, float speed) {
    }
}
