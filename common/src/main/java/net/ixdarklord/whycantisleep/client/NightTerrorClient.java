package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcanvas.api.client.camera.CameraShake;
import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffect;
import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffects;
import net.ixdarklord.coolcatcanvas.api.client.sound.SoundFilter;
import net.ixdarklord.coolcatcanvas.api.client.sound.SoundFilterProvider;
import net.ixdarklord.coolcatcanvas.api.client.sound.SoundFilters;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldVfx;
import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.client.terror.DrowningScene;
import net.ixdarklord.whycantisleep.client.terror.FallingScene;
import net.ixdarklord.whycantisleep.client.terror.ShadowScene;
import net.ixdarklord.whycantisleep.client.terror.SwarmScene;
import net.ixdarklord.whycantisleep.client.terror.TerrorArt;
import net.ixdarklord.whycantisleep.client.terror.TerrorScene;
import net.ixdarklord.whycantisleep.client.terror.TerrorStage;
import net.ixdarklord.whycantisleep.client.terror.WatchersScene;
import net.ixdarklord.whycantisleep.network.ShakenAwakePayload;
import net.ixdarklord.whycantisleep.network.TerrorPayload;
import net.ixdarklord.whycantisleep.sleep.NightmareKind;
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
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A night terror: sleep paralysis. The eyes stick half open and won't close, the world goes muffled and far away, a
 * heartbeat quickens, fog closes in and rolls through the room, the view warps, and the dark closes in round it; what
 * comes is the nightmare's own ({@link TerrorScene}, one of {@link NightmareKind}, picked by the server): the shadow and
 * its hands, the room filling with water, things crawling in, the bed falling away, or figures coming closer with each
 * blink. At the stare the eyes go wide; at the jolt, a roar and a shake, and the eyes slam shut. Had asleep, it is laid
 * out along the bed; had awake (a command), ahead of where they look, and the eyes open again after with a gasp.
 */
public final class NightTerrorClient {
    private static final Identifier VFX = WhyCantISleep.id("night_terror");
    private static final Identifier VIGNETTE = WhyCantISleep.id("hud/minigame_vignette");
    // The dark closing in round the view (tools/gen_terror.py): fully dark at its edge, in this color, which the HUD
    // fills past it as it closes in, by this share of the screen from each side by the jolt.
    private static final Identifier TERROR_VIGNETTE = WhyCantISleep.id("hud/terror_vignette");
    private static final int TERROR_VIGNETTE_EDGE = 0x0A0003;
    private static final float TERROR_VIGNETTE_CLOSE = 0.16F;
    private static final float CLOSE_FROM = 0.08F;
    // The eyes slam shut over SHUT_TICKS at the jolt.
    private static final float SHUT_TICKS = 3.0F;
    // How shut the eyes are, paralysed; and the heartbeat's gap, from slow to racing, in ticks.
    private static final float PARALYSED = 0.4F;
    private static final float SLOW_BEAT = 26.0F;
    private static final float FAST_BEAT = 5.0F;
    private static final int BED_WAIT = 40;

    private static long tick;
    private static long since = -1;
    private static long lastBeat;
    private static long beatAt = Long.MIN_VALUE;
    private static final TerrorStage STAGE = new TerrorStage();
    private static TerrorScene scene = new ShadowScene();
    private static boolean placed;
    private static boolean inBed;
    private static final List<Puff> PUFFS = new ArrayList<>();
    private static volatile SoundFilter muffle = SoundFilter.NONE;
    // The fog: how far it closes in (the far end of it, in blocks, rolling round this by FOG_ROLL).
    private static final float FOG_END = 4.5F;
    private static final float FOG_ROLL = 1.5F;
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
        if (since >= 0) scene.stop(minecraft);
        since = tick;
        STAGE.length = payload.ticks();
        STAGE.intensity = Mth.clamp(payload.intensity(), 0.0F, 1.0F);
        scene = switch (payload.kind()) {
            case SHADOW -> new ShadowScene();
            case DROWNING -> new DrowningScene();
            case SWARM -> new SwarmScene();
            case FALLING -> new FallingScene();
            case WATCHERS -> new WatchersScene();
        };
        lastBeat = tick;
        beatAt = Long.MIN_VALUE;
        // Had asleep, laid out along the bed once the game shows them in it (a terror that comes as they lie down gets
        // here before the game says they are in bed), or after BED_WAIT ticks at most; had awake, ahead of their look,
        // straight away.
        placed = false;
        inBed = payload.asleep();
        if (player.isSleeping() || !inBed) lay(player);
        RandomSource random = RandomSource.create();
        PUFFS.clear();
        for (int i = 0; i < PUFF_COUNT; i++) {
            // Low and heavy near the floor mostly, some higher; near and far, bigger the farther.
            float out = 1.0F + random.nextFloat() * (PUFF_REACH - 1.0F);
            float height = random.nextFloat() < 0.7F ? random.nextFloat() * 0.7F : 0.7F + random.nextFloat() * 1.6F;
            PUFFS.add(new Puff(random.nextFloat() * 6.28F, out, height, 0.8F + out * 0.2F + random.nextFloat() * 0.6F,
                    random.nextFloat() * 6.28F, 0.04F + random.nextFloat() * 0.05F));
        }
        warp.setUniform("Amount", scene.warp(STAGE));
        warp.enableFor(STAGE.joltTick() + TerrorStage.LINGER_TICKS);
        // Paralysed: the eyes come open part way and stay there, trembling.
        Eyelids.play(16.0F, Eyelids.Mood.PARALYSED, 0.0F, 0.0F, 1.0F, PARALYSED);
        // The drone under it all, kept to be cut off at the jolt.
        wokeAt = Long.MIN_VALUE;
        drone = SimpleSoundInstance.forUI(WCISSounds.TERROR_DRONE, 1.0F, 0.55F + 0.45F * STAGE.intensity);
        minecraft.getSoundManager().play(drone);
        muffle = new SoundFilter(0.55F, 0.12F, 0.25F + 0.35F * STAGE.intensity);
        SoundFilters.refresh();
    }

    // Where it all happens, from the bed (or the look), and the scene laid out there.
    private static void lay(LocalPlayer player) {
        placed = true;
        Optional<BlockPos> bed = player.getSleepingPos();
        Direction facing = bed.isPresent() ? player.getBedOrientation() : null;
        Vec3 head;
        if (bed.isPresent() && facing != null) {
            head = Vec3.atBottomCenterOf(bed.get());
            STAGE.along = Vec3.atLowerCornerOf(facing.getUnitVec3i());
        } else {
            Vec3 look = player.getLookAngle();
            STAGE.along = new Vec3(-look.x, 0.0, -look.z).normalize();
            head = player.position();
        }
        STAGE.floor = Math.floor(head.y);
        STAGE.head = new Vec3(head.x, STAGE.floor, head.z);
        STAGE.across = new Vec3(-STAGE.along.z, 0.0, STAGE.along.x);
        scene.lay(STAGE, RandomSource.create());
    }

    // Out of it: a gasp, melodic and tense, the world's sounds fading back in, and the eyes opening if asked to (with the
    // flutter of waking, as in bed).
    private static void wake(Minecraft minecraft, boolean openEyes) {
        wokeAt = tick;
        TerrorStage.play(minecraft, WCISSounds.TERROR_WAKE, 0.9F, 1.0F);
        if (openEyes) Eyelids.play(24.0F, Eyelids.Mood.STILL, 0.0F, 0.0F, 0.2F, 0.55F, 0.3F, 0.85F, 0.5F, 0.25F, 0.6F, 0.4F, 1.0F, 0.0F);
    }

    /** Shaken awake by someone sharing the terror: it jumps to its jolt, and ends. */
    public static void shake(ShakenAwakePayload payload) {
        if (!running()) return;
        long jumped = tick - (STAGE.joltTick() - 1);
        if (jumped < since) return;
        since = jumped;
    }

    private static void stop() {
        if (since >= 0) scene.stop(Minecraft.getInstance());
        since = -1;
        PUFFS.clear();
        muffle = SoundFilter.NONE;
        if (warp != null) warp.disable();
        if (drone != null) Minecraft.getInstance().getSoundManager().stop(drone);
        drone = null;
    }

    /** How far into its fog the terror is, 0 to 1: rolling in at the start, and there until the eyes are shut. */
    private static float fogged(float partialTicks) {
        float age = tick - since + partialTicks;
        return STAGE.held(age) ? Mth.clamp(age / (0.12F * STAGE.length), 0.0F, 1.0F) : 0.0F;
    }

    /** How strongly the last heartbeat throbs, 0 to 1. */
    public static float beat(float partialTicks) {
        return beatAt == Long.MIN_VALUE ? 0.0F : 1.0F - Mth.clamp((tick - beatAt + partialTicks) / 8.0F, 0.0F, 1.0F);
    }

    /**
     * The game's fog, closed in round the bed while a terror runs: to a few blocks, rolling nearer and farther, thinner
     * the milder the terror, in the nightmare's color. Its sights aren't fogged: they are drawn as world effects.
     */
    public static void fog(FogData data, float partialTicks) {
        if (since < 0) return;
        float in = fogged(partialTicks);
        if (in <= 0.0F) return;
        float time = tick + partialTicks;
        float t = (time - since) / STAGE.length;
        float usual = (FOG_END + FOG_ROLL * Mth.sin(time * 0.06F) + 0.8F * Mth.sin(time * 0.023F + 1.0F)) * (1.4F - 0.4F * STAGE.intensity);
        float end = scene.fogEnd(STAGE, t, usual);
        int color = scene.fogColor(STAGE, t);
        data.environmentalStart = Mth.lerp(in, data.environmentalStart, 0.0F);
        data.environmentalEnd = Mth.lerp(in, data.environmentalEnd, end);
        data.renderDistanceStart = Mth.lerp(in, data.renderDistanceStart, 0.0F);
        data.renderDistanceEnd = Mth.lerp(in, data.renderDistanceEnd, end);
        data.skyEnd = Mth.lerp(in, data.skyEnd, end);
        data.cloudEnd = Mth.lerp(in, data.cloudEnd, end);
        data.color.set(Mth.lerp(in, data.color.x(), ARGB.redFloat(color)), Mth.lerp(in, data.color.y(), ARGB.greenFloat(color)),
                Mth.lerp(in, data.color.z(), ARGB.blueFloat(color)), data.color.w());
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
        if (age > STAGE.length || minecraft.player == null) {
            // Awake (given one by a command), the eyes open again: asleep, sleep goes on and the morning opens them.
            boolean awake = minecraft.player != null && !minecraft.player.isSleeping();
            stop();
            if (awake) wake(minecraft, true);
            return;
        }
        float t = age / (float) STAGE.length;
        // The world dull and far away, and rough; worse the worse the terror.
        muffle = new SoundFilter(0.55F, 0.12F, 0.25F + 0.35F * STAGE.intensity);
        // The heart, slow at first and racing by the end.
        float gap = Mth.lerp(Easing.QUAD_IN.apply(Mth.clamp(t / TerrorStage.JOLT, 0.0F, 1.0F)), SLOW_BEAT, FAST_BEAT);
        if (t < TerrorStage.JOLT && tick - lastBeat >= gap) {
            lastBeat = tick;
            beatAt = tick;
            TerrorStage.play(minecraft, WCISSounds.HEARTBEAT_BEAT, 0.5F + 0.5F * t, 0.85F + 0.25F * t);
        }
        // A tremble all through, as if straining to move.
        if (age % 10 == 0 && t < TerrorStage.JOLT) CameraShake.shake(0.25F + 0.35F * STAGE.intensity * t, 12);
        if (placed) scene.tick(minecraft, STAGE, age, t);
        if (age == STAGE.stareTick()) {
            TerrorStage.play(minecraft, WCISSounds.TERROR_STARE, 0.8F, 1.0F);
            // Face to face: the eyes go wide.
            Eyelids.play(6.0F, Eyelids.Mood.PARALYSED, 0.0F, Eyelids.shut(0.0F), 1.0F, 0.12F);
        }
        if (age == STAGE.joltTick()) {
            // The drone cut off dead under the hit.
            if (drone != null) minecraft.getSoundManager().stop(drone);
            drone = null;
            TerrorStage.play(minecraft, WCISSounds.TERROR_JOLT, 0.7F + 0.3F * STAGE.intensity, 1.0F);
            CameraShake.shake(2.0F + 4.0F * STAGE.intensity, 14);
            // And the eyes slam shut; they sleep on.
            Eyelids.play(SHUT_TICKS, Eyelids.Mood.STILL, 0.0F, Eyelids.shut(0.0F), 1.0F, 1.0F);
        }
    }

    // The fog rolling through the room, then the nightmare's own sights.
    private static void renderWorld(VfxFrame frame) {
        if (since < 0 || !placed) return;
        float age = tick - since + frame.partialTick();
        float t = age / STAGE.length;
        if (!STAGE.held(age)) return;
        float time = frame.time();
        // Banks of fog rolling through the room: all drifting one way across it, slowly turning and swelling, and coming
        // round again from the other side; fading in and out as they go, so none pops.
        float fog = fogged(frame.partialTick());
        int mist = scene.mistColor(STAGE, t);
        if (fog > 0.0F && mist >= 0) {
            Vec3 middle = STAGE.onFloor(1.2, 0.0);
            Vec3 drift = STAGE.across.scale(0.8).add(STAGE.along.scale(0.6)).normalize();
            VfxDraw smoke = frame.draw(TerrorArt.FOG);
            for (Puff puff : PUFFS) {
                double travel = (puff.radius * Mth.cos(puff.angle) + time * puff.speed * 10.0 + PUFF_REACH) % (2.0 * PUFF_REACH) - PUFF_REACH;
                double sideways = puff.radius * Mth.sin(puff.angle);
                Vec3 at = middle.add(drift.scale(travel)).add(new Vec3(-drift.z, 0.0, drift.x).scale(sideways));
                double y = STAGE.floor + puff.height + 0.15 * Mth.sin(time * 0.4F + puff.phase);
                float edge = (float) Mth.clamp((PUFF_REACH - Math.abs(travel)) / 1.5, 0.0, 1.0);
                float swell = 1.0F + 0.15F * Mth.sin(time * 0.35F + puff.phase);
                int alpha = Math.round(fog * edge * (95 + 35 * Mth.sin(time * 0.3F + puff.phase)));
                if (alpha > 0) smoke.billboard(at.x, y, at.z, puff.size * swell, time * 0.05F + puff.phase, ARGB.color(alpha, mist));
            }
        }
        scene.renderWorld(frame, STAGE, age, t);
    }

    // What the nightmare lays over the view, the dark closing in round it as it comes, and the screen's edge a little
    // darker red on each heartbeat.
    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (since < 0) return;
        float partial = delta.getGameTimeDeltaPartialTick(true);
        float age = tick - since + partial;
        if (!STAGE.held(age)) return;
        float in = Mth.clamp(age / (0.1F * STAGE.length), 0.0F, 1.0F);
        float t = age / STAGE.length;
        if (placed) scene.renderHud(graphics, STAGE, age, t);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        // How near it has come: darker and closer in the nearer, worse the worse the terror, breathing a little with the
        // heart. The middle stays clear.
        float near = Easing.SINE_IN_OUT.apply(Mth.clamp((t - CLOSE_FROM) / (TerrorStage.JOLT - CLOSE_FROM), 0.0F, 1.0F));
        float throb = beat(partial);
        float dark = in * Mth.clamp(0.25F + 0.75F * near * Mth.lerp(STAGE.intensity, 0.8F, 1.0F) + 0.06F * throb, 0.0F, 1.0F);
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
        float alpha = in * (0.15F + 0.15F * STAGE.intensity + 0.3F * throb);
        if (alpha <= 0.0F) return;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, VIGNETTE, 0, 0, width, height, ARGB.white(Mth.clamp(alpha, 0.0F, 1.0F)));
    }

    // A bank of fog: where it starts round the bed (angle, distance, height), how big, its own turn and swell, and how
    // fast it drifts across.
    private record Puff(float angle, float radius, float height, float size, float phase, float speed) {
    }
}
