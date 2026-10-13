package net.ixdarklord.whycantisleep.client.hud;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.client.Blockers;
import net.ixdarklord.whycantisleep.client.highlight.Highlights;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.util.List;

/**
 * Arrows around the crosshair, one for each blocking monster the player can't see: those outside the view, and those
 * in it but behind blocks. A monster in plain sight has its glow and needs no arrow.
 */
public final class BlockerArrows {
    private static final Identifier ARROW = WhyCantISleep.id("textures/gui/blocker_arrow.png");
    private static final int TEXTURE_SIZE = 16;
    // How far from the crosshair the arrows sit, in GUI pixels at the medium size.
    private static final float RADIUS = 28.0F;

    private BlockerArrows() {
    }

    public static void init() {
        ClientGuiEvents.RENDER_HUD.register(BlockerArrows::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (Blockers.isEmpty() || minecraft.player == null || minecraft.options.hideGui) return;
        if (!WCISConfigs.isEnabled(WCISClientConfig.OFF_SCREEN_ARROWS)) return;
        List<Entity> blockers = Blockers.entities();
        if (blockers.isEmpty()) return;

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 eye = camera.position();
        Vector3fc forward = camera.forwardVector();
        Vector3fc up = camera.upVector();
        Vector3fc left = camera.leftVector();
        float partialTick = delta.getGameTimeDeltaPartialTick(true);
        // Half the view's angles, as tangents: the vertical one from the camera's FOV, the horizontal one by the window's shape.
        double tanV = Math.tan(Math.toRadians(camera.getFov()) / 2.0);
        double tanH = tanV * graphics.guiWidth() / graphics.guiHeight();

        float scale = WCISClientConfig.ARROW_SIZE.get().scale();
        float centerX = graphics.guiWidth() / 2.0F;
        float centerY = graphics.guiHeight() / 2.0F;
        for (Entity blocker : blockers) {
            Vec3 target = blocker.getPosition(partialTick).add(0.0, blocker.getBbHeight() / 2.0, 0.0);
            Vec3 to = target.subtract(eye);
            // The monster in the camera's own axes: ahead, to the right and up.
            double ahead = to.x * forward.x() + to.y * forward.y() + to.z * forward.z();
            double right = -(to.x * left.x() + to.y * left.y() + to.z * left.z());
            double above = to.x * up.x() + to.y * up.y() + to.z * up.z();
            boolean inView = ahead > 0.0 && Math.abs(right) <= ahead * tanH && Math.abs(above) <= ahead * tanV;
            if (inView && minecraft.player.hasLineOfSight(blocker)) continue;

            // On the screen up is negative; a monster straight behind points down.
            float angle = Math.abs(right) < 1.0E-4 && Math.abs(above) < 1.0E-4 ? (float) (Math.PI / 2.0) : (float) Math.atan2(-above, right);
            graphics.pose().pushMatrix();
            graphics.pose().translate(centerX, centerY);
            graphics.pose().rotate(angle);
            graphics.pose().translate(RADIUS * scale, 0.0F);
            graphics.pose().scale(scale, scale);
            graphics.blit(RenderPipelines.GUI_TEXTURED, ARROW, -TEXTURE_SIZE / 2, -TEXTURE_SIZE / 2, 0.0F, 0.0F,
                    TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, ARGB.opaque(Highlights.color(blocker)));
            graphics.pose().popMatrix();
        }
    }
}
