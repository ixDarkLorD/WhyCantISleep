package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffects;
import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.network.LucidPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A lucid dream, seen and drifted through: the eyes come open on a pale, glowing copy of the room, motes of light
 * rising through it, and the player can float about it for a while, the view led by their look and moved by the walking
 * keys (jump to rise, sneak to sink), never through a wall and never further than {@link #REACH} from the bed. Their
 * body stays asleep in bed: only the view moves, so the server never sees them leave it. The in-bed screen, which the
 * game keeps open while asleep and which takes the keys and the mouse, is held back meanwhile
 * ({@code MinecraftMixin}); the camera is moved by {@code CameraMixin}, the eyes left alone by {@code SleepEyes}, and the
 * game's sleep fade waits ({@code GuiMixin}). At the end the eyes close, and the night goes on.
 */
public final class LucidDreamClient {
    private static final Identifier TINT = WhyCantISleep.id("lucid_tint");
    private static final Identifier GLOW = WhyCantISleep.id("lucid_glow");
    // How far from where the dream begins the view may drift, how fast, how quickly it slows, and the room it needs.
    private static final double REACH = 6.0;
    private static final double PUSH = 0.03;
    private static final double TOP_SPEED = 0.16;
    private static final double DRAG = 0.82;
    private static final double SIZE = 0.4;
    // The view starts a little over the sleeper's eyes, and bobs as it floats.
    private static final double START_UP = 0.35;
    private static final double BOB = 0.05;
    // How long the eyes take to open on the dream, and to close at its end.
    private static final float OPEN_TICKS = 24.0F;
    private static final float CLOSE_TICKS = 30.0F;
    private static final RandomSource RANDOM = RandomSource.create();
    private static long tick;
    private static long since = -1;
    private static int length;
    private static Vec3 start = Vec3.ZERO;
    private static Vec3 at = Vec3.ZERO;
    private static Vec3 atO = Vec3.ZERO;
    private static Vec3 velocity = Vec3.ZERO;

    private LucidDreamClient() {
    }

    public static void init() {
        ClientTickEvents.END.register(LucidDreamClient::tick);
        ClientPlayerEvents.LEAVE.register(player -> stop());
    }

    /** Whether a lucid dream is under way: it has the view, the eyes and the keys until it ends. */
    public static boolean running() {
        return since >= 0;
    }

    /** Where the dreaming view is, between ticks, with its bob. */
    public static Vec3 view(float partialTicks) {
        double bob = Math.sin((tick + partialTicks) * 0.12) * BOB;
        return atO.lerp(at, partialTicks).add(0.0, bob, 0.0);
    }

    public static void on(LucidPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        since = tick;
        length = payload.ticks();
        start = player.getEyePosition().add(0.0, START_UP, 0.0);
        at = start;
        atO = start;
        velocity = Vec3.ZERO;
        // The eyes come open on the dream, the room washed pale and glowing.
        Eyelids.ease(0.0F, OPEN_TICKS, Eyelids.Mood.STILL);
        ScreenEffects.tint(TINT, 0x4CF2EEFF).fade(30, 40, Easing.SINE_IN_OUT).enable();
        ScreenEffects.bloom(GLOW, 1.6F).fade(30, 40, Easing.SINE_IN_OUT).enable();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7F, 0.8F));
        if (minecraft.screen instanceof InBedChatScreen) minecraft.setScreen(null);
    }

    private static void tick(Minecraft minecraft) {
        if (minecraft.isPaused()) return;
        tick++;
        if (since < 0) return;
        LocalPlayer player = minecraft.player;
        // Woken from it (hurt, the bed gone): the dream is gone with it.
        if (player == null || !player.isSleeping()) {
            stop();
            return;
        }
        long age = tick - since;
        if (age > length) {
            // The dream fades, the eyes close, and sleep goes on: the morning opens them (SleepEyes).
            Eyelids.ease(1.0F, CLOSE_TICKS, Eyelids.Mood.STILL);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 0.6F));
            stop();
            return;
        }
        if (minecraft.screen instanceof InBedChatScreen) minecraft.setScreen(null);
        drift(minecraft, player);
        motes(minecraft);
        if (age % 40 == 0) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F + RANDOM.nextFloat() * 0.4F, 0.35F));
    }

    // The walking keys push the view along the look, jump and sneak up and down; it slows of itself, and stops short of
    // walls and of REACH from where it began.
    private static void drift(Minecraft minecraft, LocalPlayer player) {
        var options = minecraft.options;
        double forward = (options.keyUp.isDown() ? 1 : 0) - (options.keyDown.isDown() ? 1 : 0);
        double strafe = (options.keyRight.isDown() ? 1 : 0) - (options.keyLeft.isDown() ? 1 : 0);
        double rise = (options.keyJump.isDown() ? 1 : 0) - (options.keyShift.isDown() ? 1 : 0);
        Vec3 look = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
        Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
        right = right.lengthSqr() < 1.0E-6 ? Vec3.ZERO : right.normalize();
        Vec3 wish = look.scale(forward).add(right.scale(strafe)).add(0.0, rise, 0.0);
        if (wish.lengthSqr() > 1.0) wish = wish.normalize();
        velocity = velocity.scale(DRAG).add(wish.scale(PUSH));
        if (velocity.length() > TOP_SPEED) velocity = velocity.normalize().scale(TOP_SPEED);
        atO = at;
        // Each way on its own, so it slides along a wall rather than stopping dead at it.
        Vec3 next = at;
        for (Vec3 step : new Vec3[]{new Vec3(velocity.x, 0.0, 0.0), new Vec3(0.0, velocity.y, 0.0), new Vec3(0.0, 0.0, velocity.z)}) {
            Vec3 tried = next.add(step);
            if (tried.distanceTo(start) <= REACH && minecraft.level.noCollision(AABB.ofSize(tried, SIZE, SIZE, SIZE))) next = tried;
        }
        at = next;
    }

    // Motes of pale light round the view, rising slowly.
    private static void motes(Minecraft minecraft) {
        for (int i = 0; i < 2; i++) {
            double x = at.x + (RANDOM.nextDouble() - 0.5) * 6.0;
            double y = at.y + (RANDOM.nextDouble() - 0.6) * 3.0;
            double z = at.z + (RANDOM.nextDouble() - 0.5) * 6.0;
            minecraft.level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.01 + RANDOM.nextDouble() * 0.01, 0.0);
        }
        if (tick % 2 == 0) {
            minecraft.level.addParticle(ParticleTypes.WHITE_ASH, at.x + (RANDOM.nextDouble() - 0.5) * 4.0, at.y + RANDOM.nextDouble() * 2.0,
                    at.z + (RANDOM.nextDouble() - 0.5) * 4.0, 0.0, 0.0, 0.0);
        }
    }

    private static void stop() {
        if (since < 0) return;
        since = -1;
        ScreenEffects.tint(TINT, 0x4CF2EEFF).disable();
        ScreenEffects.bloom(GLOW, 1.6F).disable();
    }
}
