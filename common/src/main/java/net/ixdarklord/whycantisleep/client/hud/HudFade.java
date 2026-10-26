package net.ixdarklord.whycantisleep.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * The whole HUD faded down, for the heartbeat minigame to have the screen to itself. On 1.20.1 every GUI draw goes
 * through the shaders' color, so while the HUD is being drawn that color's alpha is {@link #factor()}: the game's
 * blits, fills, text and items all fade with it. The mod's own draws that set a color of their own
 * ({@link GuiSprites}, {@link ItemFade}) scale theirs by the factor and put the fade's color back after
 * ({@link #restore}). The fade is open round the game's HUD ({@code GameRendererMixin}), mod layers included, up to
 * the minigame, which is drawn last and clear. The crosshair, drawn inverted over the world where an alpha does
 * nothing, is darkened instead, which takes the inversion down as smoothly ({@code GuiMixin}).
 */
public final class HudFade {
    // How much of the HUD shows, 0 to 1, set each frame before it is drawn; whether it is being drawn now.
    private static float visible = 1.0F;
    private static boolean drawing;

    private HudFade() {
    }

    /**
     * Opens the fade again for the mod's own HUD layers, which a loader may draw after the game's HUD is done. Called
     * first of the client's setup, so this listener runs before every other; the minigame, drawn last, closes it.
     */
    public static void init() {
        ClientGuiEvents.RENDER_HUD.register((graphics, partialTick) -> begin(graphics));
    }

    /** How much of the HUD shows this frame, 0 to 1. */
    public static void set(float amount) {
        visible = Mth.clamp(amount, 0.0F, 1.0F);
    }

    public static void begin(GuiGraphics graphics) {
        drawing = true;
        restore(graphics);
    }

    public static void end(GuiGraphics graphics) {
        drawing = false;
        restore(graphics);
    }

    /** What the opacity of anything drawn now is scaled by: the HUD's, while the HUD is drawn, and 1 otherwise. */
    public static float factor() {
        return drawing ? visible : 1.0F;
    }

    /** Puts the shaders' color back to the fade's: white at the fade's opacity. */
    public static void restore(GuiGraphics graphics) {
        float factor = factor();
        if (factor < 1.0F) RenderSystem.enableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, factor);
    }

    /**
     * Darkens the crosshair, drawn now, to this much of itself (1 puts it back): it is drawn inverted over the world, so
     * an alpha does nothing to it.
     */
    public static void crosshair(GuiGraphics graphics, float amount) {
        if (amount >= 1.0F) {
            restore(graphics);
            return;
        }
        float shown = Mth.clamp(amount, 0.0F, 1.0F);
        graphics.setColor(shown, shown, shown, 1.0F);
    }

    /** A color with its alpha scaled by {@link #factor()}; {@code text} drops what would be too faint to draw well. */
    public static int fade(int argb, boolean text) {
        float factor = factor();
        if (factor >= 1.0F) return argb;
        int alpha = Math.round((argb >>> 24) * factor);
        // The game draws text whose alpha is under 4 at full strength, so faint text goes away instead.
        if (text && alpha < 5) alpha = 0;
        return (alpha << 24) | (argb & 0xFFFFFF);
    }
}
