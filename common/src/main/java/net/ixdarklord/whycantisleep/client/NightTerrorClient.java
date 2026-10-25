package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcanvas.api.client.camera.CameraShake;
import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffect;
import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffects;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A night terror, as the troubled player falls asleep: sleep paralysis. They can't move; their eyes are stuck half open,
 * trembling; the world goes dull and far away and a red-black fog rolls in. Out of the floor at the foot of the bed a
 * demon of shadow rises, gaunt and hunched, its eyes kindling, and comes slowly along the bed towards them, bending low
 * until it is face to face; shadow hands rise beside the bed one after another and reach in for them. A drone swells
 * under it all, whispers come, bones crack, their heart races; then a hit and a shriek, the drone cut dead, and their
 * eyes slam shut. They sleep on, and wake shaken (the server's part) with a gasp, melodic but unresolved, the world's
 * sounds fading back in slowly. Its sounds are the mod's own ({@code tools/gen_terror_sounds.py}).
 * <p>
 * How bad it is ({@link TerrorPayload#intensity()}, from how badly the night was going to go) sets how long it lasts,
 * how many hands come, how close the demon gets, how thick the fog, and how loud it all is. The demon and the hands are pixel-art
 * shadows ({@code tools/gen_terror.py}) drawn in the world ({@link WorldVfx}), standing in their own place: the floor
 * hides what is still under it, and the bed what is behind it.
 * <p>
 * All round, a dense fog closes in and rolls, in the world (the game's own fog pulled in to a few blocks and
 * breathing, {@code FogRendererMixin}, which leaves the shadows clear; and banks of fog drifting through the room, near
 * and far, low and high), and the surroundings warp ({@code post_effect/terror_warp}: rippling and twisting towards the edges, swelling with each
 * heartbeat).
 */
public final class NightTerrorClient {
    private static final Identifier VFX = WhyCantISleep.id("night_terror");
    private static final VfxMaterial DEMON = VfxMaterial.alpha(WhyCantISleep.id("textures/vfx/terror_demon.png")).withSmoothing(false).withSoftness(0.1F);
    // The hands' variants (tools/gen_terror.py's HANDS), one picked for each hand.
    private static final VfxMaterial[] HAND_VARIANTS = hands("splayed", "grasping", "beckoning", "skeletal", "gnarled");
    private static final VfxMaterial EYE = VfxMaterial.additive(VfxMaterial.GLOW);
    private static final VfxMaterial POOL = VfxMaterial.alpha(WhyCantISleep.id("textures/vfx/terror_fog.png"));
    private static final Identifier VIGNETTE = WhyCantISleep.id("hud/minigame_vignette");
    // The demon's size in blocks; where its eyes are in its texture (tools/gen_terror.py's DEMON_EYES, of 64 by 128);
    // how far beyond the bed's foot it rises; and how far in front of the face it ends, bent low.
    private static final float DEMON_HEIGHT = 2.3F;
    private static final float DEMON_WIDTH = 1.15F;
    private static final float[][] DEMON_EYES = {{27.5F / 64.0F, 31.5F / 128.0F}, {37.5F / 64.0F, 31.5F / 128.0F}};
    private static final double BEYOND_FOOT = 1.4;
    private static final double BEFORE_FACE = 0.6;
    // The hands' size, how far beside the bed they come up, and how far along it, from where the demon rises towards
    // the face: no nearer than HANDS_NEAREST of the way, as nearer they would be under the view (in bed it looks
    // straight ahead); and how far above the floor they reach as they come up.
    private static final float HAND_HEIGHT = 1.5F;
    private static final float HAND_WIDTH = 0.75F;
    private static final double BESIDE_BED = 0.8;
    private static final double HANDS_NEAREST = 0.55;
    private static final double HAND_LIFT = 0.25;
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
    private static final float HAND_REACH = 0.3F;
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
    private static final List<Hand> HANDS = new ArrayList<>();
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
        place(player);
        RandomSource random = RandomSource.create();
        HANDS.clear();
        int hands = 2 + Math.round(4.0F * intensity);
        for (int i = 0; i < hands; i++) {
            // Alternating sides, from the foot towards the head, each a little later than the last.
            int side = i % 2 == 0 ? 1 : -1;
            double share = Mth.clamp(HANDS_NEAREST * (i / 2) / Math.max(1.0, (hands - 1) / 2.0) + random.nextFloat() * 0.08, 0.0, HANDS_NEAREST);
            Vec3 at = rise.add(end.subtract(rise).scale(share)).add(across.scale(side * (BESIDE_BED + random.nextFloat() * 0.15)));
            HANDS.add(new Hand(new Vec3(at.x, floor, at.z), side, HANDS_FROM + i * 0.07F + random.nextFloat() * 0.03F, random.nextFloat() * 6.28F,
                    random.nextInt(HAND_VARIANTS.length)));
        }
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
        floor = Math.floor(head.y);
        across = new Vec3(-along.z, 0.0, along.x);
        // The foot of the bed is a block from the head; the demon rises a little beyond it.
        Vec3 from = head.subtract(along.scale(1.0 + BEYOND_FOOT));
        Vec3 to = head.subtract(along.scale(BEFORE_FACE));
        rise = new Vec3(from.x, floor, from.z);
        end = new Vec3(to.x, floor, to.z);
    }

    private static void tick(Minecraft minecraft) {
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
        // A tremble all through, as if straining to move.
        if (age % 10 == 0 && t < JOLT) CameraShake.shake(0.25F + 0.35F * intensity * t, 12);
        if (age == Math.round(RISE_FROM * length)) play(minecraft, WCISSounds.TERROR_EMERGE, 0.8F + 0.2F * intensity, 1.0F);
        if (age == Math.round(0.45F * length)) play(minecraft, WCISSounds.TERROR_WHISPER, 0.8F, 1.0F);
        if (intensity > 0.6F && age == Math.round(0.7F * length)) play(minecraft, WCISSounds.TERROR_WHISPER, 0.9F, 0.85F);
        for (Hand hand : HANDS) {
            if (age == Math.round(hand.from * length)) play(minecraft, WCISSounds.TERROR_CREEP, 0.5F + 0.3F * intensity, 0.85F + hand.phase * 0.05F);
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
        if (since < 0) return;
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
        // The demon: rising out of the floor, then coming along the bed, bending low until its face is at theirs.
        float risen = Easing.CUBIC_OUT.apply(Mth.clamp((t - RISE_FROM) / (RISE_TO - RISE_FROM), 0.0F, 1.0F));
        if (risen > 0.0F) {
            float come = Easing.SINE_IN_OUT.apply(Mth.clamp((t - RISE_TO) / (COME_TO - RISE_TO), 0.0F, 1.0F)) * Mth.lerp(intensity, 0.45F, 1.0F);
            Vec3 foot = rise.add(end.subtract(rise).scale(come));
            // Its eyes are this high above its feet; coming close, it sinks into the floor so they meet the viewer's.
            float eyeHeight = DEMON_HEIGHT * (1.0F - DEMON_EYES[0][1]);
            float viewHeight = (float) (camera.y - floor);
            float bend = come * come * Math.max(0.0F, eyeHeight - viewHeight);
            double bottom = floor - DEMON_HEIGHT * (1.0F - risen) - bend;
            // A slow sway, and a tremor as it nears.
            double sway = 0.04 * Math.sin(time * 1.3) + 0.015 * come * Math.sin(time * 37.0);
            Vec3 base = new Vec3(foot.x, bottom, foot.z).add(across.scale(sway));
            Vec3[] corners = upright(base, camera, DEMON_WIDTH, DEMON_HEIGHT, along.scale(0.25 * come));
            frame.draw(DEMON).quad(corners[0], corners[1], corners[2], corners[3], 0xFFFFFFFF);
            // A pool of shadow where it rose.
            frame.draw(POOL).flat(rise.x, floor + 0.02, rise.z, 0.9F * risen, time * 0.3F, ARGB.color(Math.round(200 * risen), 0x000000));
            // Its eyes kindle once it has risen, flickering, brighter as it nears.
            float lit = Mth.clamp((t - RISE_TO) / 0.1F, 0.0F, 1.0F) * (0.75F + 0.25F * Mth.sin(time * 9.0F));
            if (lit > 0.0F) {
                VfxDraw glow = frame.draw(EYE);
                for (float[] eye : DEMON_EYES) {
                    // Across the quad at the eye's share, then down it.
                    Vec3 top = lerp(corners[0], corners[3], eye[0]);
                    Vec3 at = lerp(top, lerp(corners[1], corners[2], eye[0]), eye[1]);
                    glow.billboard(at, 0.05F, ARGB.color(Math.round(255 * lit), 0xFF3018));
                    glow.billboard(at, 0.2F + 0.1F * come, ARGB.color(Math.round(110 * lit), 0xB00000));
                }
            }
        }
        // The hands: up out of the floor beside the bed, then leaning in, clawing.
        for (Hand hand : HANDS) {
            float up = Easing.CUBIC_OUT.apply(Mth.clamp((t - hand.from) / HAND_RISE, 0.0F, 1.0F));
            if (up <= 0.0F) continue;
            float reach = Easing.SINE_IN_OUT.apply(Mth.clamp((t - hand.from - HAND_RISE) / HAND_REACH, 0.0F, 1.0F)) * Mth.lerp(intensity, 0.5F, 1.0F);
            float twitch = 0.04F * Mth.sin(time * 6.0F + hand.phase);
            Vec3 base = hand.at.add(0.0, -HAND_HEIGHT * (1.0F - up) + HAND_LIFT * up * reach, 0.0);
            Vec3 lean = across.scale(-hand.side * (0.2 + 0.45 * reach + twitch)).add(0.0, -0.15 * reach, 0.0);
            Vec3[] corners = upright(base, camera, HAND_WIDTH, HAND_HEIGHT, lean);
            // Thumbs towards the bed: a hand on the left of the view is drawn mirrored.
            VfxDraw draw = frame.draw(HAND_VARIANTS[hand.variant]);
            if (onLeft(hand.at, camera)) draw.quad(corners[3], corners[2], corners[1], corners[0], 0xFFFFFFFF);
            else draw.quad(corners[0], corners[1], corners[2], corners[3], 0xFFFFFFFF);
            frame.draw(POOL).flat(hand.at.x, floor + 0.02, hand.at.z, 0.35F * up, time * 0.4F + hand.phase, ARGB.color(Math.round(170 * up), 0x000000));
        }
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

    // The screen's edge a little darker red on each heartbeat.
    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (since < 0) return;
        float partial = delta.getGameTimeDeltaPartialTick(true);
        float age = tick - since + partial;
        float in = held(age) ? Mth.clamp(age / (0.1F * length), 0.0F, 1.0F) : 0.0F;
        float throb = beat(partial);
        // Faint: the fog is in the world; this is only the heart in the eyes.
        float alpha = in * (0.15F + 0.15F * intensity + 0.3F * throb);
        if (alpha <= 0.0F) return;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, VIGNETTE, 0, 0, graphics.guiWidth(), graphics.guiHeight(), ARGB.white(Mth.clamp(alpha, 0.0F, 1.0F)));
    }

    // A hand: where it comes up, on which side of the bed, when (a share of the terror), its own twitch, and which
    // variant it is.
    private record Hand(Vec3 at, int side, float from, float phase, int variant) {
    }

    // A bank of fog: where it starts round the bed (angle, distance, height), how big, its own turn and swell, and how
    // fast it drifts across.
    private record Puff(float angle, float radius, float height, float size, float phase, float speed) {
    }
}
