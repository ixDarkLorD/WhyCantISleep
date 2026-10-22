package net.ixdarklord.whycantisleep.client.fx;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffects;
import net.ixdarklord.coolcatcanvas.api.client.vfx.Curve;
import net.ixdarklord.coolcatcanvas.api.client.vfx.ParticleSpec;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldParticles;
import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.client.Blockers;
import net.ixdarklord.whycantisleep.client.highlight.Highlights;
import net.ixdarklord.whycantisleep.client.hud.SleepNotice;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.sound.WCISSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What the player sees and hears beyond the HUD, drawn with CoolCatLib: Canvas: a sound and a wash over the screen when
 * a card appears, and a dark mist on the monsters that blocked a sleep. Everything here is client only.
 */
public final class SleepEffects {
    private static final RandomSource RANDOM = RandomSource.create();
    // Each layer of the mist, one look per highlight color.
    private static final Int2ObjectMap<ParticleSpec> MIASMA = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<ParticleSpec> WISP = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<ParticleSpec> EMBER = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<ParticleSpec> EYE = new Int2ObjectOpenHashMap<>();
    // The mist thins out over the highlight's last ticks, and the eyes glint about this often.
    private static final float FADE_TICKS = 20.0F;
    private static final int EYES_INTERVAL = 28;

    private SleepEffects() {
    }

    public static void init() {
        ClientTickEvents.END.register(SleepEffects::tick);
    }

    /** A card appeared on the sleep HUD: its sound, and for the alarming kinds a wash of its color over the world. */
    public static void onNotice(SleepNotice.Kind kind) {
        Minecraft minecraft = Minecraft.getInstance();
        switch (kind) {
            case MONSTERS -> {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(WCISSounds.BLOCKED, 1.0F, 0.9F));
                ScreenEffects.tint(WhyCantISleep.id("blocked"), 0x3CB00000).fade(3, 16, Easing.SINE_IN_OUT).enableFor(3);
            }
            case DANGER -> {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(WCISSounds.WARNING, 0.6F, 0.9F));
                ScreenEffects.tint(WhyCantISleep.id("danger"), 0x40FF5A00).fade(2, 12, Easing.SINE_IN_OUT).enableFor(2);
            }
            default -> minecraft.getSoundManager().play(SimpleSoundInstance.forUI(WCISSounds.NOTICE, 1.25F, 0.5F));
        }
    }

    // The mist on a blocking monster, in layers keyed to its highlight color (its kind of threat): a miasma pooling
    // round its feet and creeping out along the ground; shadowy wisps rising off its body, lit by the color at first
    // and darkening to near black as they climb, swaying as they go; a few embers of the color drifting up unevenly;
    // and now and then a pair of eyes glinting where it looks out. It thins out over the highlight's last ticks
    // rather than stopping dead.
    private static void tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.isPaused() || Blockers.isEmpty()) return;
        if (!WCISConfigs.isEnabled(WCISClientConfig.HIGHLIGHT_MONSTERS)) return;
        int age = Math.round(Blockers.age(0.0F));
        float strength = Math.min(1.0F, Blockers.remaining(0.0F) / FADE_TICKS);
        for (Entity blocker : Blockers.entities()) {
            int color = Highlights.color(blocker);
            int beat = age + blocker.getId();
            AABB box = blocker.getBoundingBox();
            double width = Math.max(box.getXsize(), box.getZsize());
            if (beat % 2 == 0 && RANDOM.nextFloat() < strength) miasma(blocker, color, width);
            if (beat % 2 == 0 && RANDOM.nextFloat() < strength) wisp(box, color);
            if (beat % 5 == 0 && RANDOM.nextFloat() < strength) ember(box, color);
            if (beat % EYES_INTERVAL == 0 && RANDOM.nextFloat() < strength * 0.7F) eyes(blocker, color);
        }
    }

    // Close to the ground round its feet, drifting outwards and a little down: it pools rather than rises.
    private static void miasma(Entity blocker, int color, double width) {
        double angle = RANDOM.nextDouble() * Math.PI * 2.0;
        double radius = width * (0.3 + RANDOM.nextDouble() * 0.4);
        double outX = Math.cos(angle);
        double outZ = Math.sin(angle);
        WorldParticles.spawn(spec(MIASMA, color, rgb -> ParticleSpec.builder(VfxMaterial.alpha(VfxMaterial.SMOKE))
                        .lifetime(34, 52)
                        .size(Curve.of(0.3F, 0.85F, 1.1F).easing(Easing.EXPO_OUT, Easing.LINEAR))
                        .opacity(Curve.of(0.0F, 0.72F, 0.0F).easing(Easing.SINE_IN_OUT))
                        // A deep tone of the color sinking to black: near black from the start, it was lost on dark ground.
                        .color(shade(rgb, 0.5F), 0x07070A)
                        .colorEasing(Easing.SINE_IN)
                        .randomRoll()
                        .spin(Curve.constant(0.004F))
                        .gravity(0.0004F)
                        .drag(0.9F)
                        .build()),
                blocker.getX() + outX * radius, blocker.getY() + 0.05 + RANDOM.nextDouble() * 0.1, blocker.getZ() + outZ * radius,
                outX * 0.012, 0.0, outZ * 0.012);
    }

    // From somewhere in its body, rising slowly and swaying to one side.
    private static void wisp(AABB box, int color) {
        double sway = (RANDOM.nextDouble() - 0.5) * 0.02;
        WorldParticles.spawn(spec(WISP, color, rgb -> ParticleSpec.builder(VfxMaterial.alpha(VfxMaterial.SMOKE))
                        .lifetime(22, 36)
                        .size(Curve.of(0.1F, 0.3F, 0.42F).easing(Easing.EXPO_OUT, Easing.LINEAR))
                        .opacity(Curve.of(0.0F, 0.5F, 0.0F))
                        .color(shade(rgb, 0.85F), 0x0B0B10)
                        .colorEasing(Easing.SINE_IN)
                        .randomRoll()
                        .spin(Curve.of(0.02F, 0.0F))
                        .gravity(-0.0025F)
                        .drag(0.95F)
                        .build()),
                box.minX + RANDOM.nextDouble() * box.getXsize(), box.minY + box.getYsize() * (0.2 + RANDOM.nextDouble() * 0.7), box.minZ + RANDOM.nextDouble() * box.getZsize(),
                sway, 0.012 + RANDOM.nextDouble() * 0.015, -sway);
    }

    // A spark of the color, lifting off unevenly and dimming as it goes.
    private static void ember(AABB box, int color) {
        WorldParticles.spawn(spec(EMBER, color, rgb -> ParticleSpec.builder(VfxMaterial.additive(VfxMaterial.GLOW))
                        .lifetime(18, 32)
                        .size(Curve.of(0.0F, 0.06F, 0.0F).easing(Easing.EXPO_OUT, Easing.SINE_IN))
                        .opacity(Curve.FADE)
                        .color(brighten(rgb), shade(rgb, 0.45F))
                        .gravity(-0.003F)
                        .drag(0.92F)
                        .build()),
                box.minX + RANDOM.nextDouble() * box.getXsize(), box.minY + RANDOM.nextDouble() * box.getYsize(), box.minZ + RANDOM.nextDouble() * box.getZsize(),
                (RANDOM.nextDouble() - 0.5) * 0.03, 0.02 + RANDOM.nextDouble() * 0.02, (RANDOM.nextDouble() - 0.5) * 0.03);
    }

    // Two points of the color at its eyes, either side of where it looks, there and gone.
    private static void eyes(Entity blocker, int color) {
        float yaw = blocker.getYHeadRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Vec3 side = new Vec3(forward.z, 0.0, -forward.x);
        Vec3 face = blocker.getEyePosition().add(forward.scale(blocker.getBbWidth() * 0.5 + 0.02));
        double apart = Math.max(0.08, blocker.getBbWidth() * 0.18);
        ParticleSpec glint = spec(EYE, color, rgb -> ParticleSpec.builder(VfxMaterial.additive(VfxMaterial.GLOW))
                .lifetime(14, 20)
                .size(Curve.of(0.0F, 0.075F, 0.0F).easing(Easing.EXPO_OUT, Easing.EXPO_IN))
                .opacity(Curve.of(0.0F, 1.0F, 0.0F).easing(Easing.EXPO_OUT, Easing.SINE_IN))
                .color(brighten(rgb), rgb)
                .build());
        WorldParticles.spawn(glint, face.add(side.scale(apart)), Vec3.ZERO);
        WorldParticles.spawn(glint, face.add(side.scale(-apart)), Vec3.ZERO);
    }

    // One look per layer and highlight color, made the first time it is needed.
    private static ParticleSpec spec(Int2ObjectMap<ParticleSpec> layer, int color, Int2ObjectFunction<ParticleSpec> make) {
        return layer.computeIfAbsent(color, make);
    }

    // A color darker by a share (0 black, 1 as it is).
    private static int shade(int rgb, float share) {
        return ARGB.color(0, Math.round(ARGB.red(rgb) * share), Math.round(ARGB.green(rgb) * share), Math.round(ARGB.blue(rgb) * share)) & 0xFFFFFF;
    }

    // A color pushed halfway to white, for the hot middle of an ember or an eye.
    private static int brighten(int rgb) {
        return ARGB.color(0, (ARGB.red(rgb) + 255) / 2, (ARGB.green(rgb) + 255) / 2, (ARGB.blue(rgb) + 255) / 2) & 0xFFFFFF;
    }
}
