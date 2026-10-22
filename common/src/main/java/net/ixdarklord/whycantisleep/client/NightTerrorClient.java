package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcanvas.api.client.camera.CameraShake;
import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffects;
import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.network.TerrorPayload;
import net.ixdarklord.whycantisleep.sound.WCISSounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A night terror, as the restless player falls asleep: the dark goes red, a heartbeat quickens, something whispers,
 * eyes open in the dark one pair after another, and at the end a pair right in front of them, a shriek, and a jolt.
 * Then they sleep on, and wake shaken (the server's part).
 */
public final class NightTerrorClient {
    private static final int LENGTH = 90;
    // When the heart beats, faster and faster; when the whisper comes; when the last eyes open, and the jolt.
    private static final int[] HEARTBEATS = {0, 18, 32, 44, 54, 62, 68, 73, 77, 80};
    private static final int WHISPER = 12;
    private static final int STARE = 74;
    private static final int JOLT = 82;
    // The eyes in the dark: each pair where on the screen (fractions of it), when it opens and for how long.
    private static final int PAIRS = 4;
    private static final int PAIR_TICKS = 16;
    private static final float[][] EYES = new float[PAIRS][3];
    private static long tick;
    private static long since = -1;

    private NightTerrorClient() {
    }

    public static void init() {
        ClientTickEvents.END.register(NightTerrorClient::tick);
        ClientGuiEvents.RENDER_HUD.register(NightTerrorClient::render);
        ClientPlayerEvents.LEAVE.register(player -> since = -1);
    }

    public static void on(TerrorPayload payload) {
        if (!payload.start()) {
            since = -1;
            return;
        }
        since = tick;
        RandomSource random = RandomSource.create();
        for (int i = 0; i < PAIRS; i++) {
            EYES[i][0] = 0.15F + random.nextFloat() * 0.7F;
            EYES[i][1] = 0.15F + random.nextFloat() * 0.5F;
            EYES[i][2] = 20 + i * 13 + random.nextInt(6);
        }
        ScreenEffects.tint(WhyCantISleep.id("night_terror"), 0x70400000).fade(20, 30, Easing.SINE_IN_OUT).enableFor(LENGTH - 30);
    }

    private static void tick(Minecraft minecraft) {
        tick++;
        if (since < 0) return;
        long age = tick - since;
        if (age > LENGTH || minecraft.player == null) {
            since = -1;
            return;
        }
        var sounds = minecraft.getSoundManager();
        for (int beat : HEARTBEATS) {
            if (age == beat) sounds.play(SimpleSoundInstance.forUI(WCISSounds.BLOCKED, 0.9F + age / 200.0F, 0.6F + age / 300.0F));
        }
        if (age == WHISPER) sounds.play(SimpleSoundInstance.forUI(SoundEvents.AMBIENT_CAVE.value(), 0.7F, 0.6F));
        if (age == STARE) sounds.play(SimpleSoundInstance.forUI(SoundEvents.ENDERMAN_STARE, 0.6F, 0.7F));
        if (age == JOLT) {
            sounds.play(SimpleSoundInstance.forUI(SoundEvents.ENDERMAN_SCREAM, 0.5F, 0.6F));
            CameraShake.shake(4.0F, 12);
        }
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (since < 0) return;
        float age = tick - since + delta.getGameTimeDeltaPartialTick(true);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        for (float[] pair : EYES) {
            float open = window(age, pair[2], PAIR_TICKS);
            if (open > 0.0F) eyes(graphics, Math.round(width * pair[0]), Math.round(height * pair[1]), 1, open);
        }
        float stare = window(age, STARE, JOLT + 4 - STARE);
        if (stare > 0.0F) eyes(graphics, width / 2, height / 2 - 10, 4, stare);
    }

    // How open a pair of eyes is that opens at a time for a while: quickly open, held, slowly shut.
    private static float window(float age, float from, float length) {
        float t = (age - from) / length;
        if (t < 0.0F || t > 1.0F) return 0.0F;
        return t < 0.2F ? t / 0.2F : t > 0.7F ? (1.0F - t) / 0.3F : 1.0F;
    }

    // A pair of glinting red eyes, a little apart, with a faint glow round each.
    private static void eyes(GuiGraphicsExtractor graphics, int x, int y, int scale, float open) {
        int alpha = Mth.clamp(Math.round(open * 255), 0, 255);
        int glow = alpha / 4;
        int eyeWidth = 4 * scale;
        int eyeHeight = Math.max(1, Math.round(2 * scale * open));
        int gap = 5 * scale;
        for (int side = -1; side <= 1; side += 2) {
            int left = x + side * gap - (side < 0 ? eyeWidth : 0);
            graphics.fill(left - scale, y - scale, left + eyeWidth + scale, y + eyeHeight + scale, glow << 24 | 0xB00000);
            graphics.fill(left, y, left + eyeWidth, y + eyeHeight, alpha << 24 | 0xFF2A2A);
        }
    }
}
