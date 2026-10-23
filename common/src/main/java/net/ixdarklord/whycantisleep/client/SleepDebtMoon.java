package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.client.hud.HudStyle;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The moon of sleep debt: in the top left corner, a moon that fills up, from the bottom, with each day the player goes
 * without lying down, full at three (when phantoms come), with the days beside it. Out of sight until a whole day has
 * gone without sleep. The time since rest is {@link SleepStatus}'s, as the server last sent it.
 */
public final class SleepDebtMoon {
    private static final ResourceLocation EMPTY = WhyCantISleep.id("hud/moon_empty");
    private static final ResourceLocation FULL = WhyCantISleep.id("hud/moon_full");
    private static final int DAY = 24000;
    private static final int FULL_DAYS = 3;
    private static final int SIZE = 16;
    private static final int MARGIN = 4;

    private SleepDebtMoon() {
    }

    public static void init() {
        ClientGuiEvents.RENDER_HUD.register(SleepDebtMoon::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        int ticksSinceRest = SleepStatus.ticksSinceRest();
        if (ticksSinceRest < DAY || minecraft.options.hideGui || !WCISConfigs.isEnabled(WCISClientConfig.SLEEP_DEBT_MOON)) return;
        float fill = Mth.clamp(ticksSinceRest / (float) (DAY * FULL_DAYS), 0.0F, 1.0F);
        int x = MARGIN;
        int y = MARGIN;
        HudStyle.sprite(graphics, EMPTY, x, y, SIZE, SIZE);
        int filled = Math.round(SIZE * fill);
        // Full, it pulses: phantoms are coming.
        float alpha = fill >= 1.0F && minecraft.level != null ? 0.75F + 0.25F * Mth.sin(minecraft.level.getGameTime() * 0.15F) : 1.0F;
        if (filled > 0) {
            graphics.enableScissor(x, y + SIZE - filled, x + SIZE, y + SIZE);
            HudStyle.sprite(graphics, FULL, x, y, SIZE, SIZE, HudStyle.white(alpha));
            graphics.disableScissor();
        }
        int days = ticksSinceRest / DAY;
        HudStyle.text(graphics, minecraft.font, Component.translatable("gui.whycantisleep.debt.days", days), x + SIZE + 3, y + 4,
                HudStyle.color(fill >= 1.0F ? HudStyle.RED : HudStyle.SILVER, 1.0F), true);
    }
}
