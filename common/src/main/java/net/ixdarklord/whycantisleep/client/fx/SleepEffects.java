package net.ixdarklord.whycantisleep.client.fx;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.ixdarklord.coolcatcanvas.api.client.camera.CameraShake;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What the player sees and hears beyond the HUD, drawn with CoolCatLib: Canvas: a wash over the screen when a sleep
 * fails, wisps rising off the monsters that blocked it, whispers from where they are, and the "monster under the bed"
 * scare. Everything here is client only.
 */
public final class SleepEffects {
    private static final RandomSource RANDOM = RandomSource.create();
    // The client ticks, after the monsters were pointed out, at which a whisper comes.
    private static final int[] WHISPER_TICKS = {8, 34};

    // Two points of light for a second: eyes.
    private static final ParticleSpec EYE = ParticleSpec.builder(VfxMaterial.additive(VfxMaterial.GLOW))
            .lifetime(26, 30)
            .size(Curve.of(0.0F, 0.07F, 0.0F).easing(Easing.EXPO_OUT, Easing.EXPO_IN))
            .opacity(Curve.of(0.0F, 1.0F, 0.0F).easing(Easing.EXPO_OUT, Easing.SINE_IN))
            .color(0xFF2A1A, 0xAA0000)
            .build();
    // Darkness pouring out from under the bed.
    private static final ParticleSpec SHADOW = ParticleSpec.builder(VfxMaterial.alpha(VfxMaterial.SMOKE))
            .lifetime(24, 44)
            .size(Curve.of(0.15F, 0.55F, 0.7F).easing(Easing.EXPO_OUT, Easing.LINEAR))
            .opacity(Curve.of(0.0F, 0.85F, 0.0F))
            .color(0x08060C)
            .randomRoll()
            .drag(0.9F)
            .build();
    // A wisp's look for each highlight color.
    private static final Int2ObjectMap<ParticleSpec> WISPS = new Int2ObjectOpenHashMap<>();

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

    private static void tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.isPaused() || Blockers.isEmpty()) return;
        boolean highlight = WCISConfigs.isEnabled(WCISClientConfig.HIGHLIGHT_MONSTERS);
        int age = Math.round(Blockers.age(0.0F));
        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Entity blocker : Blockers.entities()) {
            // A wisp of the monster's highlight color every other tick, from somewhere in its body.
            if (highlight && (age + blocker.getId()) % 2 == 0) {
                AABB box = blocker.getBoundingBox();
                WorldParticles.spawn(wisp(Highlights.color(blocker)),
                        box.minX + RANDOM.nextDouble() * box.getXsize(), box.minY + RANDOM.nextDouble() * box.getYsize(), box.minZ + RANDOM.nextDouble() * box.getZsize(),
                        0.0, 0.01 + RANDOM.nextDouble() * 0.02, 0.0);
            }
            double distance = minecraft.player == null ? 0.0 : blocker.distanceToSqr(minecraft.player);
            if (distance < nearestDistance) {
                nearest = blocker;
                nearestDistance = distance;
            }
        }
        if (nearest != null && WCISClientConfig.WHISPERS.get()) {
            for (int whisperTick : WHISPER_TICKS) {
                // Played where the monster is, so it is heard from its side.
                if (age == whisperTick) {
                    minecraft.level.playLocalSound(nearest.getX(), nearest.getEyeY(), nearest.getZ(), WCISSounds.WHISPER, SoundSource.AMBIENT,
                            WCISClientConfig.WHISPER_VOLUME.get() / 100.0F * 1.6F, 0.75F + RANDOM.nextFloat() * 0.3F, false);
                }
            }
        }
    }

    private static ParticleSpec wisp(int color) {
        return WISPS.computeIfAbsent(color, rgb -> ParticleSpec.builder(VfxMaterial.alpha(VfxMaterial.SMOKE))
                .lifetime(16, 28)
                .size(Curve.of(0.08F, 0.26F, 0.32F).easing(Easing.EXPO_OUT, Easing.LINEAR))
                .opacity(Curve.of(0.0F, 0.45F, 0.0F))
                .color(rgb)
                .randomRoll()
                .gravity(-0.002F)
                .drag(0.95F)
                .build());
    }

    /** The "monster under the bed" scare: eyes at the bed's edge, darkness pouring out, a shriek and a jolt. */
    public static void scare(BlockPos bed) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || !WCISConfigs.isEnabled(WCISClientConfig.UNDER_THE_BED_SCARES)) return;
        Vec3 center = Vec3.atBottomCenterOf(bed).add(0.0, 0.12, 0.0);
        // The eyes look out from the side of the bed the player is on.
        Vec3 toPlayer = minecraft.player.position().subtract(center).multiply(1.0, 0.0, 1.0);
        toPlayer = toPlayer.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : toPlayer.normalize();
        Vec3 side = new Vec3(-toPlayer.z, 0.0, toPlayer.x);
        Vec3 face = center.add(toPlayer.scale(0.48));
        WorldParticles.spawn(EYE, face.add(side.scale(0.13)), Vec3.ZERO);
        WorldParticles.spawn(EYE, face.add(side.scale(-0.13)), Vec3.ZERO);
        WorldParticles.burst(SHADOW, center, 26, 0.5, 0.09);

        minecraft.level.playLocalSound(center.x, center.y, center.z, WCISSounds.SCARE, SoundSource.HOSTILE, 1.0F, 0.6F + RANDOM.nextFloat() * 0.15F, false);
        ScreenEffects.tint(WhyCantISleep.id("scare"), 0xB0000000).fade(1, 26, Easing.QUAD_OUT).enableFor(2);
        if (WCISConfigs.isEnabled(WCISClientConfig.SCREEN_SHAKE)) CameraShake.shake(3.0F, 16);
    }
}
