package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.coolcatcanvas.api.client.gui.placement.GuiAreas;
import net.ixdarklord.coolcatcanvas.api.client.gui.placement.GuiPlacement;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.client.hud.HudStyle;
import net.ixdarklord.whycantisleep.config.HudCorner;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * The moon of sleep debt: a moon that fills up, from the bottom, with each day the player goes without lying down,
 * full at three (when phantoms come), with the days beside it. Out of sight until a whole day has gone without sleep.
 * It sits where the client config says ({@code debtMoonPosition}, top left unless set, nudged by
 * {@code debtMoonOffsetX} / {@code debtMoonOffsetY}), and pops larger for a moment when it shows and each time another
 * day goes by, so it is noticed. It keeps out of the way of the rest of the HUD, whichever mod drew it (a minimap,
 * Jade's tooltip, the boss bars), gliding clear of them. The time since rest is {@link SleepStatus}'s, as the server last sent it.
 */
public final class SleepDebtMoon {
    private static final Identifier EMPTY = WhyCantISleep.id("hud/moon_empty");
    private static final Identifier FULL = WhyCantISleep.id("hud/moon_full");
    private static final int DAY = 24000;
    private static final int FULL_DAYS = 3;
    private static final int SIZE = 16;
    private static final int MARGIN = 4;
    private static final int LABEL_GAP = 3;
    // The pop: how long it lasts, and how much larger it starts.
    private static final float POP_TICKS = 16.0F;
    private static final float POP_SWELL = 0.6F;

    // A clock of the moon's own, stopped while paused; the days it last showed, and when it last popped.
    private static int tick;
    private static int shownDays;
    private static int poppedAt = Integer.MIN_VALUE;
    // Pixels kept from the rest of the HUD, and how quickly the moon glides out of its way (per second).
    private static final int GAP = 2;
    private static final float GLIDE = 14.0F;
    // Where the moon is drawn as it glides (NaN when it isn't shown), and when it last was.
    private static float shownX = Float.NaN;
    private static float shownY = Float.NaN;
    private static long lastFrame;

    private SleepDebtMoon() {
    }

    public static void init() {
        ClientTickEvents.END.register(minecraft -> {
            if (!minecraft.isPaused()) tick++;
        });
        ClientGuiEvents.RENDER_HUD.register(SleepDebtMoon::render);
    }

    /** How full the moon is for this long without lying down: 0 to 1, full at three days. */
    public static float fill(int ticksSinceRest) {
        return Mth.clamp(ticksSinceRest / (float) (DAY * FULL_DAYS), 0.0F, 1.0F);
    }

    /**
     * The moon at this size, filled from the bottom by {@code fill}, pulsing once full (phantoms are coming); the sleep
     * status popup's meter draws it too.
     */
    public static void drawMoon(GuiGraphicsExtractor graphics, int x, int y, int size, float fill) {
        Minecraft minecraft = Minecraft.getInstance();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EMPTY, x, y, size, size);
        int filled = Math.round(size * fill);
        float alpha = fill >= 1.0F && minecraft.level != null ? 0.75F + 0.25F * Mth.sin(minecraft.level.getGameTime() * 0.15F) : 1.0F;
        if (filled > 0) {
            graphics.enableScissor(x, y + size - filled, x + size, y + size);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FULL, x, y, size, size, ARGB.white(alpha));
            graphics.disableScissor();
        }
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        int ticksSinceRest = SleepStatus.ticksSinceRest();
        int days = ticksSinceRest / DAY;
        if (days < 1 || minecraft.gui.hud.isHidden() || !WCISConfigs.isEnabled(WCISClientConfig.SLEEP_DEBT_MOON)) {
            shownDays = 0;
            shownX = Float.NaN;
            shownY = Float.NaN;
            return;
        }
        // Unrecorded, so the moon isn't in its own way next frame.
        GuiAreas.unrecorded(() -> render(graphics, delta, ticksSinceRest, days));
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta, int ticksSinceRest, int days) {
        Minecraft minecraft = Minecraft.getInstance();
        // Showing for the first time, or another day gone: it pops.
        if (days != shownDays) {
            if (days > shownDays) poppedAt = tick;
            shownDays = days;
        }
        float fill = fill(ticksSinceRest);
        Component label = Component.translatable("gui.whycantisleep.debt.days", days);
        int width = SIZE + LABEL_GAP + minecraft.font.width(label);
        HudCorner corner = WCISClientConfig.DEBT_MOON_POSITION.get();
        int placeX = corner.x(graphics.guiWidth(), width, MARGIN) + WCISClientConfig.DEBT_MOON_OFFSET_X.get();
        int placeY = corner.y(graphics.guiHeight(), SIZE, MARGIN) + WCISClientConfig.DEBT_MOON_OFFSET_Y.get();
        // Out of the way of the rest of the HUD (CoolCatLib's GuiPlacement): away from its edge or sideways, whichever
        // is nearer, but never past the screen's middle.
        int half = graphics.guiHeight() / 2;
        GuiPlacement.Position at = GuiPlacement.hud()
                .size(width, SIZE).at(placeX, placeY).gap(GAP)
                .within(0, corner.bottom() ? half : 0, graphics.guiWidth(), graphics.guiHeight() - half)
                .directions(corner.bottom() ? GuiPlacement.Direction.UP : GuiPlacement.Direction.DOWN,
                        GuiPlacement.Direction.LEFT, GuiPlacement.Direction.RIGHT)
                .find();
        // Gliding there rather than jumping.
        long now = System.nanoTime();
        if (Float.isNaN(shownX)) {
            shownX = at.x();
            shownY = at.y();
        } else {
            float seconds = Math.min((now - lastFrame) / 1.0E9F, 0.25F);
            float follow = 1.0F - (float) Math.exp(-GLIDE * seconds);
            shownX += (at.x() - shownX) * follow;
            shownY += (at.y() - shownY) * follow;
            if (Math.abs(at.x() - shownX) < 0.05F) shownX = at.x();
            if (Math.abs(at.y() - shownY) < 0.05F) shownY = at.y();
        }
        lastFrame = now;
        int x = Math.round(shownX);
        int y = Math.round(shownY);

        float popAge = (tick - poppedAt + delta.getGameTimeDeltaPartialTick(true)) / POP_TICKS;
        float scale = popAge < 1.0F ? 1.0F + POP_SWELL * (1.0F - Easing.CUBIC_OUT.apply(Mth.clamp(popAge, 0.0F, 1.0F))) : 1.0F;
        graphics.pose().pushMatrix();
        // Swelling about the moon's middle.
        graphics.pose().translate(x + SIZE / 2.0F, y + SIZE / 2.0F);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-SIZE / 2.0F, -SIZE / 2.0F);
        drawMoon(graphics, 0, 0, SIZE, fill);
        graphics.pose().popMatrix();
        graphics.text(minecraft.font, label, x + SIZE + LABEL_GAP, y + 4, HudStyle.color(fill >= 1.0F ? HudStyle.RED : HudStyle.SILVER, 1.0F), true);
    }
}
