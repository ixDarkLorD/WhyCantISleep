package net.ixdarklord.whycantisleep.client.hud;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.client.Blockers;
import net.ixdarklord.whycantisleep.client.highlight.Highlights;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * The last blocker compass: once the highlights of a refused sleep fade, a single arrow round the crosshair keeps
 * pointing to the nearest of those monsters still about, with its name and how far it is under the crosshair (and how
 * many are left) in the HUD's notice frame, tinted its color, until the time to clear the room runs out, the room is
 * safe, or they are all dealt with. A soft ping goes with it, quicker and higher the closer the nearest is; dealing
 * with the last of them chimes, and says so for a moment (client option {@code compassPing}).
 */
public final class BlockerCompass {
    // Smaller and nearer the crosshair than the highlights' arrows; the label under the crosshair, its frame clear of the
    // arrow's ring even when it points straight down (behind the player), and a little see-through.
    private static final float RADIUS = 22.0F;
    private static final float SCALE = 0.8F;
    private static final int LABEL_BELOW = 42;
    private static final float FRAME_ALPHA = 0.92F;
    // The dot before the words, and the gap after it.
    private static final int DOT = 4;
    private static final int DOT_GAP = 5;
    private static final float FADE_IN_TICKS = 8.0F;
    private static final float FADE_OUT_TICKS = 20.0F;
    // The ping: every this many ticks per block away, never quicker or slower than these; the all-clear's line stays this long.
    private static final float PING_TICKS_PER_BLOCK = 1.5F;
    private static final int PING_FASTEST = 5;
    private static final int PING_SLOWEST = 30;
    private static final int CLEAR_TICKS = 40;

    private static final IntList TRACKED = new IntArrayList();
    private static long tick;
    private static long until;
    private static int pingIn;
    private static long clearedAt = Long.MIN_VALUE / 2;

    private BlockerCompass() {
    }

    public static void init() {
        ClientTickEvents.END.register(minecraft -> {
            if (minecraft.isPaused()) return;
            tick++;
            if (TRACKED.isEmpty()) return;
            if (tick > until || minecraft.player == null || minecraft.player.isSleeping() || minecraft.level == null) {
                TRACKED.clear();
                return;
            }
            TRACKED.removeIf(id -> {
                Entity entity = minecraft.level.getEntity(id);
                return entity == null || !entity.isAlive();
            });
            if (TRACKED.isEmpty()) {
                // The last of them dealt with in time.
                clearedAt = tick;
                if (pinging()) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.5F, 0.6F));
                return;
            }
            if (!Blockers.isEmpty() || !pinging() || --pingIn > 0) return;
            Entity nearest = nearest(minecraft);
            if (nearest == null) return;
            float distance = nearest.distanceTo(minecraft.player);
            pingIn = Mth.clamp(Math.round(distance * PING_TICKS_PER_BLOCK), PING_FASTEST, PING_SLOWEST);
            float pitch = Mth.lerp(1.0F - Mth.clamp(distance / 20.0F, 0.0F, 1.0F), 0.8F, 1.6F);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), pitch, 0.35F));
        });
        ClientPlayerEvents.LEAVE.register(player -> TRACKED.clear());
        ClientGuiEvents.RENDER_HUD.register(BlockerCompass::render);
    }

    /** The monsters of a refused sleep, followed for as long as the player has to clear the room. None stops it. */
    public static void track(Collection<? extends Entity> blockers) {
        TRACKED.clear();
        pingIn = 0;
        clearedAt = Long.MIN_VALUE / 2;
        if (blockers.isEmpty() || !WCISServerConfig.ROOM_CLEARING.get()) return;
        for (Entity blocker : blockers) TRACKED.add(blocker.getId());
        until = tick + WCISServerConfig.CLEARING_TIME.get() * 20L;
    }

    private static boolean pinging() {
        return WCISConfigs.isEnabled(WCISClientConfig.BLOCKER_COMPASS) && WCISConfigs.isEnabled(WCISClientConfig.COMPASS_PING);
    }

    private static void render(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (TRACKED.isEmpty() && tick - clearedAt < CLEAR_TICKS && !minecraft.options.hideGui && WCISConfigs.isEnabled(WCISClientConfig.BLOCKER_COMPASS)) {
            allClear(graphics, minecraft.font, (tick - clearedAt + partialTick) / CLEAR_TICKS);
            return;
        }
        // While the highlights show, their own arrows point the way.
        if (TRACKED.isEmpty() || !Blockers.isEmpty() || minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) return;
        if (!WCISConfigs.isEnabled(WCISClientConfig.BLOCKER_COMPASS)) return;
        Entity target = nearest(minecraft);
        if (target == null) return;
        float since = Blockers.age(partialTick) - WCISServerConfig.HIGHLIGHT_DURATION.get() * 20.0F;
        float alpha = Math.min(Mth.clamp(since / FADE_IN_TICKS, 0.0F, 1.0F), Mth.clamp((until - tick - partialTick) / FADE_OUT_TICKS, 0.0F, 1.0F));
        if (alpha <= 0.0F) return;

        BlockerArrows.Aim aim = BlockerArrows.aim(minecraft, graphics, target.getPosition(partialTick).add(0.0, target.getBbHeight() / 2.0, 0.0), partialTick);
        int color = Highlights.color(target);
        float centerX = graphics.guiWidth() / 2.0F;
        float centerY = graphics.guiHeight() / 2.0F;
        // In plain sight it needs no arrow, only its name.
        if (!aim.inView() || !minecraft.player.hasLineOfSight(target)) {
            float beat = 0.85F + 0.15F * Mth.sin((tick + partialTick) * 0.25F);
            BlockerArrows.drawArrow(graphics, centerX, centerY, aim.angle(), RADIUS, SCALE, false, color, alpha * beat);
        }
        // Its name in its color, how far in silver, and the rest a step back.
        Component name = colored(target.getName().copy(), color);
        Component distance = colored(Component.literal(String.valueOf(Mth.floor(aim.distance()))), HudStyle.SILVER);
        Component label = colored(TRACKED.size() > 1
                ? Component.translatable("gui.whycantisleep.compass.more", name, distance, TRACKED.size() - 1)
                : Component.translatable("gui.whycantisleep.compass.one", name, distance), HudStyle.MUTED);
        label(graphics, minecraft.font, label, color, alpha);
    }

    // "All dealt with", in green under the crosshair, fading out.
    private static void allClear(GuiGraphics graphics, Font font, float age) {
        float alpha = Mth.clamp((1.0F - age) * 3.0F, 0.0F, 1.0F);
        label(graphics, font, colored(Component.translatable("gui.whycantisleep.compass.clear"), HudStyle.GREEN), HudStyle.GREEN, alpha);
    }

    // A line under the crosshair in the HUD's notice frame, tinted a color, with a dot of that color before it.
    private static void label(GuiGraphics graphics, Font font, Component text, int rgb, float alpha) {
        int width = DOT + DOT_GAP + font.width(text);
        int x = Math.round(graphics.guiWidth() / 2.0F - width / 2.0F);
        int y = graphics.guiHeight() / 2 + LABEL_BELOW;
        HudStyle.notice(graphics, x, y, width, font.lineHeight - 1, rgb, alpha * FRAME_ALPHA);
        HudStyle.dot(graphics, x, y + 2, rgb, alpha);
        HudStyle.text(graphics, font, text, x + DOT + DOT_GAP, y, HudStyle.color(HudStyle.WHITE, alpha), false);
    }

    private static MutableComponent colored(MutableComponent text, int rgb) {
        return text.withStyle(style -> style.withColor(rgb));
    }

    private static @Nullable Entity nearest(Minecraft minecraft) {
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < TRACKED.size(); i++) {
            Entity entity = minecraft.level.getEntity(TRACKED.getInt(i));
            if (entity == null || !entity.isAlive()) continue;
            double distance = entity.distanceToSqr(minecraft.player);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        return best;
    }
}
