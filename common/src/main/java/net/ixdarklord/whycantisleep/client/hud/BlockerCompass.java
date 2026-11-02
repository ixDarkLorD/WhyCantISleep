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
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * The last blocker compass: once the highlights of a refused sleep fade, a single arrow round the crosshair keeps
 * pointing to the nearest of those monsters still about, with its name and how far it is under the crosshair (and how
 * many are left), until the time to clear the room runs out, the room is safe, or they are all dealt with.
 */
public final class BlockerCompass {
    // Smaller and nearer the crosshair than the highlights' arrows; the label under the crosshair, clear of the arrow's
    // ring even when it points straight down (behind the player).
    private static final float RADIUS = 22.0F;
    private static final float SCALE = 0.8F;
    private static final int LABEL_BELOW = 33;
    private static final float FADE_IN_TICKS = 8.0F;
    private static final float FADE_OUT_TICKS = 20.0F;

    private static final IntList TRACKED = new IntArrayList();
    private static long tick;
    private static long until;

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
        });
        ClientPlayerEvents.LEAVE.register(player -> TRACKED.clear());
        ClientGuiEvents.RENDER_HUD.register(BlockerCompass::render);
    }

    /** The monsters of a refused sleep, followed for as long as the player has to clear the room. None stops it. */
    public static void track(Collection<? extends Entity> blockers) {
        TRACKED.clear();
        if (blockers.isEmpty() || !WCISServerConfig.ROOM_CLEARING.get()) return;
        for (Entity blocker : blockers) TRACKED.add(blocker.getId());
        until = tick + WCISServerConfig.CLEARING_TIME.get() * 20L;
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        // While the highlights show, their own arrows point the way.
        if (TRACKED.isEmpty() || !Blockers.isEmpty() || minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) return;
        if (!WCISConfigs.isEnabled(WCISClientConfig.BLOCKER_COMPASS)) return;
        Entity target = nearest(minecraft);
        if (target == null) return;
        float partialTick = delta.getGameTimeDeltaPartialTick(true);
        float since = Blockers.age(partialTick) - WCISServerConfig.HIGHLIGHT_DURATION.get() * 20.0F;
        float alpha = Math.min(Mth.clamp(since / FADE_IN_TICKS, 0.0F, 1.0F), Mth.clamp((until - tick - partialTick) / FADE_OUT_TICKS, 0.0F, 1.0F));
        if (alpha <= 0.0F) return;

        BlockerArrows.Aim aim = BlockerArrows.aim(minecraft, graphics, target.getPosition(partialTick).add(0.0, target.getBbHeight() / 2.0, 0.0));
        int color = Highlights.color(target);
        float centerX = graphics.guiWidth() / 2.0F;
        float centerY = graphics.guiHeight() / 2.0F;
        // In plain sight it needs no arrow, only its name.
        if (!aim.inView() || !minecraft.player.hasLineOfSight(target)) {
            float beat = 0.85F + 0.15F * Mth.sin((tick + partialTick) * 0.25F);
            BlockerArrows.drawArrow(graphics, centerX, centerY, aim.angle(), RADIUS, SCALE, false, color, alpha * beat);
        }
        Font font = minecraft.font;
        Component label = TRACKED.size() > 1
                ? Component.translatable("gui.whycantisleep.compass.more", target.getName(), Mth.floor(aim.distance()), TRACKED.size() - 1)
                : Component.translatable("gui.whycantisleep.compass.one", target.getName(), Mth.floor(aim.distance()));
        int x = Math.round(centerX - font.width(label) / 2.0F);
        int y = Math.round(centerY) + LABEL_BELOW;
        graphics.fill(x - 3, y - 2, x + font.width(label) + 3, y + 9, HudStyle.color(0x111317, alpha * 0.6F));
        graphics.text(font, label, x, y, HudStyle.color(color, alpha), false);
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
