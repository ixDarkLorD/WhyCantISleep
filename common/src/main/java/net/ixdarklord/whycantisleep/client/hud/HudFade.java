package net.ixdarklord.whycantisleep.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;

/**
 * The whole HUD faded down, for the heartbeat minigame to have the screen to itself. The game draws its HUD with no
 * opacity of its own to ask for. On 1.21.1 every GUI shader multiplies in the shader color, so while the HUD is drawn
 * the alpha of every shader color set is scaled by {@link #factor()}, and blending is kept on so it shows
 * ({@code RenderSystemMixin}); items are drawn with the shader color too. The fade is open from the head of the game's
 * HUD ({@code GuiMixin}) to its end, and again over the mod's own HUD layers, up to the minigame, which is drawn last and
 * clear. The crosshair, drawn inverted over the world where an alpha does nothing, is darkened instead
 * ({@link #crosshair()}), and left out once it is all but gone.
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
     * alpha does nothing to it; its shader color is darkened instead, which takes the inversion down as smoothly.
     */
    public static float crosshair() {
        return crosshair;
    }

    public static void crosshair(float amount) {
        crosshair = Math.clamp(amount, 0.0F, 1.0F);
        // Set as the shader color while the crosshair is drawn: its brightness, at full alpha.
        RenderSystem.setShaderColor(crosshair, crosshair, crosshair, 1.0F);
    }

    /** How much of the HUD shows this frame, 0 to 1. */
    public static void set(float amount) {
        visible = Math.clamp(amount, 0.0F, 1.0F);
    }

    public static void begin() {
        drawing = true;
        if (visible < 1.0F) {
            // The shader color in force now is faded too, and blending is on so the fade shows.
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public static void end() {
        boolean faded = drawing && visible < 1.0F;
        drawing = false;
        if (faded) RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
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
