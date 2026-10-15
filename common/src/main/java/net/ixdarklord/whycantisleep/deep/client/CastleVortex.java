package net.ixdarklord.whycantisleep.deep.client;

import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldVfx;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.deep.LucidDeep;
import net.ixdarklord.whycantisleep.network.DreamCastlePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The storm over the Lord's castle, drawn by each dreamer's client once the server says where the castle is
 * ({@code DreamCastle}). It is what the castle is doing to the sky:
 * <ul>
 *     <li>a tunnel of heaped, billowing storm cloud rising high over it, turning and climbing, into a blinding light at
 *     its top, with a ceiling of storm cloud spreading out round its mouth; lit by the light, glowing white-hot near
 *     it and fading through red into darkness away from it;</li>
 *     <li>a beam of red-white light rising from the dais on the castle's roof, straight up the tunnel into the light,
 *     flowing upwards, its base flaring;</li>
 *     <li>red lightning thrown out of the tunnel's walls and the ceiling every second or two, forking down to the
 *     islands or arcing through the clouds, each strike lighting up the clouds for a moment, with thunder.</li>
 * </ul>
 * The clouds are a true volume: the shader ({@code shaders/vfx/castle_vortex.fsh}) marches each pixel's ray through
 * them, so they have depth from every side and can be flown into. Only the lightning is built on the CPU.
 */
public final class CastleVortex {
    private static final VfxMaterial CLOUDS = VfxMaterial.alpha(VfxMaterial.GLOW).withSoftness(0.0F).withShader(WhyCantISleep.id("vfx/castle_vortex"));
    private static final VfxMaterial BEAM = VfxMaterial.additive(VfxMaterial.GLOW).withSoftness(2.0F).withShader(WhyCantISleep.id("vfx/castle_beam"));
    private static final VfxMaterial BOLT = VfxMaterial.additive(VfxMaterial.GLOW).withSoftness(1.0F).withShader(WhyCantISleep.id("vfx/castle_bolt"));
    private static final VfxMaterial GLOW = VfxMaterial.additive(VfxMaterial.GLOW).withSoftness(6.0F);
    private static final VfxMaterial FOG = VfxMaterial.alpha(VfxMaterial.SMOKE).withSoftness(10.0F);
    private static final VfxMaterial WIND = VfxMaterial.additive(VfxMaterial.STREAK).withSoftness(3.0F);
    // The wind round the light: wisps of fog and streaks of wind born where the beam ends, swirling round it and out
    // through the cloud. How many of each there are at once, how long they live (seconds), and how far out they go.
    private static final int FOG_COUNT = 140;
    private static final int WIND_COUNT = 70;
    private static final float WISP_LIFE_MIN = 14.0F;
    private static final float WISP_LIFE_MAX = 22.0F;
    private static final float WISP_REACH = 175.0F;

    // The tunnel of cloud (castle_vortex.fsh has the same numbers): from this high over the roof, where it is this
    // wide (its radius), up to the light at its top, where it is this wide.
    private static final double TUNNEL_FOOT = 22.0;
    private static final double CLOUD_HEIGHT = 120.0;
    private static final float TUNNEL_NARROW = 16.0F;
    private static final float TUNNEL_WIDE = 64.0F;
    // The box the clouds fill, round the roof: this wide each way, from this far over the roof to this far.
    private static final double BOX_HALF = 200.0;
    private static final double BOX_LOW = 14.0;
    private static final double BOX_HIGH = 146.0;
    // Nobody further than this (across) sees the storm: past it the castle is well out of sight anyway.
    private static final double VISIBLE = 900.0;
    // How long a strike lasts, in seconds, and the wait between strikes.
    private static final float STRIKE_LIFE = 0.42F;
    private static final float STRIKE_GAP_MIN = 0.7F;
    private static final float STRIKE_GAP_MAX = 2.4F;

    private static final Random RANDOM = new Random();
    @Nullable
    private static Vec3 castle;
    private static final List<Strike> STRIKES = new ArrayList<>();
    private static final List<Wisp> WISPS = new ArrayList<>();
    private static float nextStrike;
    private static float lastTime = -1.0F;
    // How bright the latest strike still is, 0 to 1 (it falls off quickly), and where it left the cloud, for the shader
    // to light the cloud round it: how far round the castle (0 to 1), and a place code (0 to 7 up the tunnel's wall,
    // 8 to 15 out across the ceiling).
    private static float flash;
    private static float seen = 0.0F;
    private static float seenAt = -1.0F;
    /** How far the castle reaches down from its beam (four floors of 8 and its foundations). */
    private static final int CASTLE_DEPTH = 48;
    private static final float FADE_SECONDS = 1.2F;
    private static float strikeAround;
    private static int strikePlace;

    private CastleVortex() {
    }

    public static void init() {
        WorldVfx.register(WhyCantISleep.id("castle_vortex"), CastleVortex::render);
    }

    /** The server says where the castle is (or that there is none). */
    public static void on(DreamCastlePayload payload) {
        castle = payload.known() ? new Vec3(payload.x(), payload.y(), payload.z()) : null;
        STRIKES.clear();
        WISPS.clear();
    }

    // Whether the game is drawing the castle: whether the renderer holds a built mesh for the sections down its keep
    // under the beam (the same test LevelRenderer uses; it covers view distance and chunks not yet built or loaded).
    private static boolean castleDrawn(Vec3 beam) {
        LevelRenderer renderer = Minecraft.getInstance().levelRenderer;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int down = 0; down <= CASTLE_DEPTH; down += 16) {
            if (renderer.isSectionCompiledAndVisible(at.set(beam.x, beam.y - down, beam.z))) return true;
        }
        return false;
    }

    private static void render(VfxFrame frame) {
        Vec3 beamFrom = castle;
        // The storm over the castle gathers only once the dreamer is near the end (DeepProgress), or by command.
        if (beamFrom == null || !LucidDeep.isDeep(frame.level()) || !DeepSkyClient.vortexShown()) {
            WISPS.clear();
            return;
        }
        Vec3 camera = frame.cameraPosition();
        double across = Math.hypot(camera.x - beamFrom.x, camera.z - beamFrom.z);
        if (across > VISIBLE) return;
        // Seen exactly while the castle is (castleDrawn), fading in or out over about a second as that changes, as the
        // castle itself comes and goes in the fog at the edge of sight.
        float time = frame.time();
        float since = seenAt < 0.0F ? 1.0F : Mth.clamp(time - seenAt, 0.0F, 0.25F);
        seenAt = time;
        seen = Mth.approach(seen, castleDrawn(beamFrom) ? 1.0F : 0.0F, since / FADE_SECONDS);
        if (seen <= 0.005F) {
            WISPS.clear();
            return;
        }
        float delta = lastTime < 0.0F ? 0.0F : Mth.clamp(time - lastTime, 0.0F, 0.25F);
        lastTime = time;
        double eyeY = beamFrom.y + CLOUD_HEIGHT;
        Vec3 eye = new Vec3(beamFrom.x, eyeY, beamFrom.z);

        tickStrikes(frame, beamFrom, eyeY, time, delta, across);
        float lit = flash;

        // The clouds are a volume, marched through in the shader: here only the faces of the box they fill that face
        // the camera (all of them from inside), so each pixel is marched once. The shader needs to know where the
        // castle is from the camera: across in the UVs (the same at every corner), and the rest in the colour, read
        // as one 32-bit word (castle_vortex.fsh): bits 0-13 how far up the castle is (1/8 block, over 1024 down to
        // 1024 up), 14-18 how far round the castle the latest strike was, 19-22 its place (0-7 up the tunnel's wall,
        // 8-15 out across the ceiling), 23-26 how bright it still is, 27-31 how much of the storm shows (never 0 while
        // drawn, so the colour's alpha, its top byte, never is either).
        double relX = beamFrom.x - camera.x;
        double relY = beamFrom.y - camera.y;
        double relZ = beamFrom.z - camera.z;
        int up = Mth.clamp((int) Math.round((relY + 1024.0) * 8.0), 0, 16383);
        int around = Mth.floor(strikeAround * 32.0F) & 31;
        int bright = Mth.clamp(Math.round(lit * 15.0F), 0, 15);
        int shown = Mth.clamp(Math.round(seen * 31.0F), 1, 31);
        int word = up | around << 14 | (strikePlace & 15) << 19 | bright << 23 | shown << 27;
        int color = ARGB.color(word >>> 24, word >>> 16 & 255, word >>> 8 & 255, word & 255);
        double x0 = beamFrom.x - BOX_HALF, x1 = beamFrom.x + BOX_HALF;
        double z0 = beamFrom.z - BOX_HALF, z1 = beamFrom.z + BOX_HALF;
        double y0 = beamFrom.y + BOX_LOW, y1 = beamFrom.y + BOX_HIGH;
        boolean inside = camera.x > x0 && camera.x < x1 && camera.y > y0 && camera.y < y1 && camera.z > z0 && camera.z < z1;
        VfxDraw clouds = frame.draw(CLOUDS);
        float u = (float) relX;
        float v = (float) relZ;
        if (inside || camera.y < y0) face(clouds, new Vec3(x0, y0, z0), new Vec3(x0, y0, z1), new Vec3(x1, y0, z1), new Vec3(x1, y0, z0), u, v, color);
        if (inside || camera.y > y1) face(clouds, new Vec3(x0, y1, z0), new Vec3(x0, y1, z1), new Vec3(x1, y1, z1), new Vec3(x1, y1, z0), u, v, color);
        if (inside || camera.x < x0) face(clouds, new Vec3(x0, y0, z0), new Vec3(x0, y1, z0), new Vec3(x0, y1, z1), new Vec3(x0, y0, z1), u, v, color);
        if (inside || camera.x > x1) face(clouds, new Vec3(x1, y0, z0), new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), new Vec3(x1, y0, z1), u, v, color);
        if (inside || camera.z < z0) face(clouds, new Vec3(x0, y0, z0), new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), new Vec3(x1, y0, z0), u, v, color);
        if (inside || camera.z > z1) face(clouds, new Vec3(x0, y0, z1), new Vec3(x0, y1, z1), new Vec3(x1, y1, z1), new Vec3(x1, y0, z1), u, v, color);

        // The light at the top of the tunnel, blinding, glowing out through the clouds round it; and the beam from the
        // dais up into it, a wide soft glow and a hot core, both flowing upwards, its base flaring on the roof.
        float pulse = 0.88F + 0.12F * Mth.sin(time * 2.3F) + 0.04F * Mth.sin(time * 7.1F);
        Vec3 core = eye.add(0.0, 4.0, 0.0);
        VfxDraw beam = frame.draw(BEAM);
        beam.beam(beamFrom, core, 16.0F * pulse, faded(0.7F, 1.0F, 0.16F, 0.24F), faded(0.5F, 1.0F, 0.3F, 0.34F));
        beam.beam(beamFrom, core, 3.0F + lit * 1.2F, faded(1.0F, 1.0F, 0.78F, 0.76F), faded(1.0F, 1.0F, 0.85F, 0.82F));
        VfxDraw glow = frame.draw(GLOW);
        glow.billboard(beamFrom.x, beamFrom.y + 0.6, beamFrom.z, 5.5F * pulse, time * 0.4F, faded(0.9F, 1.0F, 0.3F, 0.32F));
        glow.billboard(beamFrom.x, beamFrom.y + 0.6, beamFrom.z, 1.8F, 0.0F, faded(1.0F, 1.0F, 0.9F, 0.88F));
        glow.billboard(core.x, core.y, core.z, 110.0F * pulse, 0.0F, faded(0.4F, 1.0F, 0.2F, 0.26F));
        glow.billboard(core.x, core.y, core.z, 50.0F * pulse, 0.0F, faded(0.7F, 1.0F, 0.45F, 0.46F));
        glow.billboard(core.x, core.y, core.z, 22.0F, 0.0F, faded(1.0F, 1.0F, 0.9F, 0.88F));
        glow.billboard(core.x, core.y, core.z, 7.0F, 0.0F, faded(1.0F, 1.0F, 1.0F, 1.0F));

        drawWinds(frame, beamFrom, core.y, delta);
        drawStrikes(frame, time);
    }

    // A colour, with its alpha faded by how much of the storm shows.
    private static int faded(float alpha, float red, float green, float blue) {
        return ARGB.colorFromFloat(alpha * seen, red, green, blue);
    }

    private static void face(VfxDraw draw, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float u, float v, int color) {
        draw.quad(a, b, c, d, u, v, u, v, color);
    }

    // A point on the tunnel's wall: `around` 0 to 1, `up` 0 at its foot to 1 at its top.
    private static Vec3 tunnel(Vec3 base, double eyeY, float around, float up) {
        float radius = TUNNEL_NARROW + (TUNNEL_WIDE - TUNNEL_NARROW) * (float) Math.pow(up, 1.6);
        double y = base.y + TUNNEL_FOOT + (eyeY - base.y - TUNNEL_FOOT) * up;
        float angle = around * Mth.TWO_PI;
        return new Vec3(base.x + Mth.cos(angle) * radius, y, base.z + Mth.sin(angle) * radius);
    }

    // ---- Lightning ----

    private static void tickStrikes(VfxFrame frame, Vec3 base, double eyeY, float time, float delta, double across) {
        flash = Math.max(0.0F, flash - delta * 3.2F);
        STRIKES.removeIf(strike -> time - strike.born > STRIKE_LIFE);
        nextStrike -= delta;
        if (nextStrike > 0.0F) return;
        nextStrike = STRIKE_GAP_MIN + RANDOM.nextFloat() * (STRIKE_GAP_MAX - STRIKE_GAP_MIN);
        // Out of the underside of the wheel, somewhere between its eye and its rim, either down at the islands below
        // or sideways through the cloud.
        float angle = RANDOM.nextFloat() * Mth.TWO_PI;
        float out = TUNNEL_WIDE * 0.9F + RANDOM.nextFloat() * 100.0F;
        Vec3 from = new Vec3(base.x + Mth.cos(angle) * out, eyeY - 1.0, base.z + Mth.sin(angle) * out);
        strikeAround = angle / Mth.TWO_PI;
        strikePlace = 8 + Mth.clamp(Math.round((out - 60.0F) / 140.0F * 7.0F), 0, 7);
        if (RANDOM.nextFloat() < 0.4F) {
            // Out of the tunnel's wall.
            float up = 0.25F + RANDOM.nextFloat() * 0.6F;
            strikeAround = RANDOM.nextFloat();
            from = tunnel(base, eyeY, strikeAround, up);
            strikePlace = Mth.clamp(Math.round(up * 7.0F), 0, 7);
            angle = strikeAround * Mth.TWO_PI;
        }
        Vec3 to;
        if (RANDOM.nextFloat() < 0.65F) {
            float reach = 20.0F + RANDOM.nextFloat() * 50.0F;
            float turn = angle + (RANDOM.nextFloat() - 0.5F) * 1.2F;
            to = new Vec3(from.x + Mth.cos(turn) * reach, base.y - 30.0 + RANDOM.nextFloat() * 40.0, from.z + Mth.sin(turn) * reach);
        } else {
            float turn = angle + (RANDOM.nextBoolean() ? 1.0F : -1.0F) * (0.5F + RANDOM.nextFloat() * 0.6F);
            float reach = 40.0F + RANDOM.nextFloat() * 50.0F;
            to = new Vec3(base.x + Mth.cos(turn) * reach, eyeY - 6.0 - RANDOM.nextFloat() * 10.0, base.z + Mth.sin(turn) * reach);
        }
        STRIKES.add(new Strike(time, from, bolt(from, to, 2)));
        flash = 0.7F + RANDOM.nextFloat() * 0.3F;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        if (across < 260.0 && seen > 0.5F) minecraft.level.setSkyFlashTime(2);
        float loud = (float) Mth.clamp(1.0 - across / VISIBLE, 0.15, 1.0) * seen;
        minecraft.level.playLocalSound(from.x, from.y, from.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 40.0F * loud,
                0.55F + RANDOM.nextFloat() * 0.25F, false);
    }

    // A jagged bolt from one point to another, as a list of segments (pairs of points, then a width), forking: each
    // fork a thinner bolt of its own, `depth` levels deep.
    private static List<Segment> bolt(Vec3 from, Vec3 to, int depth) {
        List<Segment> segments = new ArrayList<>();
        branch(segments, from, to, 1.0F, depth);
        return segments;
    }

    private static void branch(List<Segment> segments, Vec3 from, Vec3 to, float width, int depth) {
        Vec3 span = to.subtract(from);
        double length = span.length();
        int steps = Math.max(4, (int) (length / 4.0));
        Vec3 dir = span.normalize();
        Vec3 sideA = dir.cross(new Vec3(0.0, 1.0, 0.0));
        if (sideA.lengthSqr() < 1.0E-4) sideA = new Vec3(1.0, 0.0, 0.0);
        sideA = sideA.normalize();
        Vec3 sideB = dir.cross(sideA).normalize();
        Vec3 last = from;
        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            double jag = length * 0.06 * Mth.sin(t * Mth.PI);
            Vec3 point = i == steps ? to : from.add(span.scale(t))
                    .add(sideA.scale((RANDOM.nextDouble() - 0.5) * 2.0 * jag)).add(sideB.scale((RANDOM.nextDouble() - 0.5) * 2.0 * jag));
            float w = width * (1.0F - t * 0.6F);
            segments.add(new Segment(last, point, w, t));
            if (depth > 0 && i < steps - 1 && RANDOM.nextFloat() < 0.22F) {
                Vec3 forkDir = dir.add(sideA.scale((RANDOM.nextDouble() - 0.5) * 1.6)).add(sideB.scale((RANDOM.nextDouble() - 0.5) * 1.6)).normalize();
                branch(segments, point, point.add(forkDir.scale(length * (0.18 + RANDOM.nextDouble() * 0.25) * (1.0 - t * 0.5))), w * 0.55F, depth - 1);
            }
            last = point;
        }
    }

    // Each strike: grows down its path in the first few frames, flickers, and fades; a wide red glow round each
    // segment and a white-hot core along it; and a burst of light in the cloud where it left.
    private static void drawStrikes(VfxFrame frame, float time) {
        if (STRIKES.isEmpty()) return;
        VfxDraw bolt = frame.draw(BOLT);
        VfxDraw glow = frame.draw(GLOW);
        for (Strike strike : STRIKES) {
            float age = (time - strike.born) / STRIKE_LIFE;
            float grown = Mth.clamp(age * 6.0F, 0.0F, 1.0F);
            float flicker = (age < 0.15F || ((int) (age * 14.0F + strike.seed) & 1) == 0) ? 1.0F : 0.55F;
            float fade = (1.0F - age) * (1.0F - age) * flicker;
            for (Segment segment : strike.segments) {
                if (segment.along > grown) continue;
                bolt.beam(segment.from, segment.to, 9.0F * segment.width, faded(0.5F * fade, 1.0F, 0.14F, 0.22F));
                bolt.beam(segment.from, segment.to, 2.2F * segment.width, faded(fade, 1.0F, 0.72F, 0.70F));
            }
            glow.billboard(strike.origin.x, strike.origin.y, strike.origin.z, 48.0F, strike.seed, faded(0.85F * fade, 1.0F, 0.2F, 0.26F));
        }
    }

    // ---- Wind ----

    // Each wisp moves on a spiral out from the light: round it fast close in and slower further out (as the cloud
    // turns), out ever faster, and down a little as it goes, as if the light breathed it out into the storm.
    private static void drawWinds(VfxFrame frame, Vec3 base, double coreY, float delta) {
        int fog = 0;
        for (Wisp wisp : WISPS) if (wisp.fog) fog++;
        // Topped up a few a frame, so they come out steadily rather than all at once.
        for (int i = 0; i < 4 && fog < FOG_COUNT; i++, fog++) WISPS.add(Wisp.born(true));
        for (int i = 0, wind = WISPS.size() - fog; i < 2 && wind < WIND_COUNT; i++, wind++) WISPS.add(Wisp.born(false));
        VfxDraw fogDraw = frame.draw(FOG);
        VfxDraw windDraw = frame.draw(WIND);
        WISPS.removeIf(wisp -> {
            wisp.age += delta;
            if (wisp.age >= wisp.life) return true;
            float r = wisp.radius;
            float spin = 2.6F / (1.0F + r / 18.0F);
            wisp.angle += spin * wisp.swirl * delta;
            wisp.radius += (2.0F + r * 0.2F) * wisp.speed * delta;
            wisp.drop += (0.6F + r * 0.02F) * delta;
            if (wisp.radius > WISP_REACH) return true;
            float t = wisp.age / wisp.life;
            float fade = Mth.clamp(t * 6.0F, 0.0F, 1.0F) * Mth.clamp((1.0F - t) * 3.0F, 0.0F, 1.0F)
                    * Mth.clamp((WISP_REACH - wisp.radius) / 40.0F, 0.0F, 1.0F);
            Vec3 at = wisp.at(base, coreY);
            // How near the light it is, 1 at it: the fog there glows hot, further out it is dark storm.
            float hot = (float) Math.exp(-wisp.radius / 26.0F);
            if (wisp.fog) {
                // Each wisp is a few soft puffs, layered: a dense heart, a wider veil about it, and a faint halo
                // turning the other way, so it reads as a body of fog, not a flat sprite. It breathes as it goes.
                float size = wisp.size * (1.6F + wisp.radius / 14.0F) * (1.0F + 0.12F * Mth.sin(wisp.age * 0.9F + wisp.roll));
                float red = Mth.lerp(hot, 0.16F, 1.0F);
                float green = Mth.lerp(hot, 0.03F, 0.72F);
                float blue = Mth.lerp(hot, 0.07F, 0.70F);
                float alpha = fade * Mth.lerp(hot, 0.5F, 0.62F);
                float turn = wisp.roll + wisp.age * 0.25F * wisp.swirl;
                fogDraw.billboard(at.x, at.y, at.z, size * 1.45F, -turn * 0.6F, faded(alpha * 0.22F, red * 0.8F, green * 0.8F, blue * 0.8F));
                fogDraw.billboard(at.x, at.y + size * 0.08F, at.z, size, turn, faded(alpha * 0.38F, red, green, blue));
                fogDraw.billboard(at.x, at.y - size * 0.06F, at.z, size * 0.62F, turn * 1.4F + 1.7F,
                        faded(alpha * 0.42F, Math.min(1.0F, red * 1.15F), Math.min(1.0F, green * 1.15F), Math.min(1.0F, blue * 1.15F)));
            } else {
                // A streak curving along its path: a few short pieces back along the spiral, fading out to its tail
                // and thinning, so it is a smooth, bright-headed comet of wind rather than a ruled line.
                float back = 0.5F;
                int pieces = 5;
                float bright = fade * (0.16F + 0.5F * hot);
                float width = 0.55F + wisp.radius * 0.012F;
                float green = Mth.lerp(hot, 0.3F, 0.75F);
                float blue = Mth.lerp(hot, 0.35F, 0.72F);
                Vec3 head = at;
                for (int k = 1; k <= pieces; k++) {
                    float t0 = (float) (k - 1) / pieces;
                    float t1 = (float) k / pieces;
                    float behind = back * t1;
                    Vec3 tail = wisp.at(base, coreY, wisp.angle - spin * wisp.swirl * behind, wisp.radius - (2.0F + wisp.radius * 0.2F) * wisp.speed * behind);
                    windDraw.beam(tail, head, width * (1.0F - t0 * 0.7F),
                            faded(bright * (1.0F - t1), 1.0F, green, blue), faded(bright * (1.0F - t0), 1.0F, green, blue));
                    head = tail;
                }
            }
            return false;
        });
    }

    private static final class Wisp {
        final boolean fog;
        final float life;
        final float speed;
        final float swirl;
        final float size;
        final float roll;
        final float height;
        float age;
        float angle;
        float radius;
        float drop;

        private Wisp(boolean fog) {
            this.fog = fog;
            this.life = WISP_LIFE_MIN + RANDOM.nextFloat() * (WISP_LIFE_MAX - WISP_LIFE_MIN);
            this.speed = 0.7F + RANDOM.nextFloat() * 0.6F;
            // All turning the same way as the cloud, some a little faster.
            this.swirl = 0.8F + RANDOM.nextFloat() * 0.5F;
            this.size = 3.0F + RANDOM.nextFloat() * 3.5F;
            this.roll = RANDOM.nextFloat() * Mth.TWO_PI;
            this.height = -4.0F + (RANDOM.nextFloat() - 0.5F) * 10.0F;
            this.angle = RANDOM.nextFloat() * Mth.TWO_PI;
            this.radius = 2.0F + RANDOM.nextFloat() * 6.0F;
        }

        static Wisp born(boolean fog) {
            Wisp wisp = new Wisp(fog);
            // The first ones start part way through their lives, so the swirl is full from the moment it is seen.
            if (WISPS.size() < FOG_COUNT / 2) {
                float skip = RANDOM.nextFloat() * wisp.life * 0.8F;
                for (float t = 0.0F; t < skip; t += 0.1F) {
                    wisp.angle += 2.6F / (1.0F + wisp.radius / 18.0F) * wisp.swirl * 0.1F;
                    wisp.radius += (2.0F + wisp.radius * 0.2F) * wisp.speed * 0.1F;
                    wisp.drop += (0.6F + wisp.radius * 0.02F) * 0.1F;
                }
                wisp.age = skip;
            }
            return wisp;
        }

        Vec3 at(Vec3 base, double coreY) {
            return this.at(base, coreY, this.angle, this.radius);
        }

        Vec3 at(Vec3 base, double coreY, float angle, float radius) {
            return new Vec3(base.x + Mth.cos(angle) * radius, coreY + this.height - this.drop, base.z + Mth.sin(angle) * radius);
        }
    }

    private record Segment(Vec3 from, Vec3 to, float width, float along) {
    }

    private record Strike(float born, Vec3 origin, List<Segment> segments, float seed) {
        Strike(float born, Vec3 origin, List<Segment> segments) {
            this(born, origin, segments, RANDOM.nextFloat() * 10.0F);
        }
    }
}
