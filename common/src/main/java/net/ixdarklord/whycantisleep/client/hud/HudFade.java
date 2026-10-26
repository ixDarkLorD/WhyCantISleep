package net.ixdarklord.whycantisleep.client.hud;

import net.ixdarklord.coolcatcanvas.api.client.gui.ItemOpacityRenderer;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;

/**
 * The whole HUD faded down, for the heartbeat minigame to have the screen to itself. The game draws its HUD with no
 * opacity of its own to ask for, so while the HUD is being drawn every blit, fill and line of text it adds is scaled by
 * {@link #factor()} ({@code BlitRenderStateMixin}, {@code TiledBlitRenderStateMixin},
 * {@code ColoredRectangleRenderStateMixin}, {@code GuiTextRenderStateMixin}), and items by way of {@link ItemFade}. The
 * fade is open from the head of the game's HUD ({@code GuiMixin}) to its end, and again over the mod's own HUD layers,
 * up to the minigame, which is drawn last and clear. The crosshair, drawn inverted over the world where an alpha does
 * nothing, is left out once the HUD is half gone.
 */
public final class HudFade {
    // How much of the HUD shows, 0 to 1, set each frame before it is drawn; whether it is being drawn now.
    private static float visible = 1.0F;
    private static boolean drawing;
    // How much of the crosshair shows while it is drawn (1 otherwise).
    private static float crosshair = 1.0F;

    private HudFade() {
    }

    /**
     * Opens the fade again for the mod's own HUD layers, which a loader may draw after the game's HUD is done (Fabric
     * does). Called first of the client's setup, so this listener runs before every other; the minigame, drawn last,
     * closes it.
     */
    public static void init() {
        ClientGuiEvents.RENDER_HUD.register((graphics, delta) -> begin());
    }

    /**
     * How much of the crosshair shows while it is drawn now (1 when it isn't). It is drawn inverted over the world, so an
     * alpha does nothing to it; {@code BlitRenderStateMixin} darkens its color instead, which takes the inversion down as
     * smoothly.
     */
    public static float crosshair() {
        return crosshair;
    }

    public static void crosshair(float amount) {
        crosshair = Math.clamp(amount, 0.0F, 1.0F);
    }

    /** How much of the HUD shows this frame, 0 to 1. */
    public static void set(float amount) {
        visible = Math.clamp(amount, 0.0F, 1.0F);
    }

    public static void begin() {
        // Opened twice a frame at times (the HUD's head, then its mod layers): the items' fade is pushed once.
        if (drawing) return;
        drawing = true;
        // The game's items (the hotbar's and all) fade with the HUD through CoolCatLib (ItemFade).
        ItemOpacityRenderer.pushItemOpacity(visible);
    }

    public static void end() {
        if (!drawing) return;
        drawing = false;
        ItemOpacityRenderer.popItemOpacity();
    }

    /** What the opacity of anything drawn now is scaled by: the HUD's, while the HUD is drawn, and 1 otherwise. */
    public static float factor() {
        return drawing ? visible : 1.0F;
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
