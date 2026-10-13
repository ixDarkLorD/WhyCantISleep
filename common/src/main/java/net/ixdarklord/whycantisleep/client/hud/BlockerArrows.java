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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Arrows around the crosshair, one for each blocking monster the player can't see: a solid one for a monster outside
 * the view, a hollow one for a monster in view but behind blocks. A monster in plain sight has its glow and needs no
 * arrow. Nearer monsters get larger arrows, and arrows that would overlap are fanned apart.
 */
public final class BlockerArrows {
    // Two frames side by side, drawn by tools/gen_arrow_texture.ps1: solid, then hollow.
    private static final Identifier ARROWS = WhyCantISleep.id("textures/gui/blocker_arrow.png");
    private static final int FRAME_SIZE = 16;
    private static final int SHEET_WIDTH = FRAME_SIZE * 2;
    // How far from the crosshair the arrows sit, in GUI pixels at the medium size.
    private static final float RADIUS = 30.0F;
    // The room one arrow takes along the ring, in GUI pixels: closer ones are fanned apart.
    private static final float SPACING = 15.0F;
    private static final float FADE_IN_TICKS = 5.0F;
    private static final float FADE_OUT_TICKS = 12.0F;
    // Beyond this many blocks an arrow is at its smallest.
    private static final double FAR_DISTANCE = 16.0;

    private BlockerArrows() {
    }

    public static void init() {
        ClientGuiEvents.RENDER_HUD.register(BlockerArrows::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (Blockers.isEmpty() || minecraft.player == null || minecraft.options.hideGui) return;
        if (!WCISConfigs.isEnabled(WCISClientConfig.OFF_SCREEN_ARROWS)) return;
        float partialTick = delta.getGameTimeDeltaPartialTick(true);
        List<Arrow> arrows = collect(minecraft, graphics, partialTick);
        if (arrows.isEmpty()) return;

        float size = WCISClientConfig.ARROW_SIZE.get().scale();
        fanOut(arrows, SPACING / RADIUS);

        float age = Blockers.age(partialTick);
        float appear = Mth.clamp(age / FADE_IN_TICKS, 0.0F, 1.0F);
        float alpha = Math.min(appear, Mth.clamp(Blockers.remaining(partialTick) / FADE_OUT_TICKS, 0.0F, 1.0F));
        // The arrows slide out from the crosshair as they appear, then nudge outward in a slow beat.
        float eased = 1.0F - (1.0F - appear) * (1.0F - appear);
        float radius = (RADIUS - 10.0F * (1.0F - eased) + 1.5F * Mth.sin(age * 0.3F)) * size;

        float centerX = graphics.guiWidth() / 2.0F;
        float centerY = graphics.guiHeight() / 2.0F;
        for (Arrow arrow : arrows) {
            float scale = size * arrow.scale;
            graphics.pose().pushMatrix();
            graphics.pose().translate(centerX, centerY);
            graphics.pose().rotate(arrow.angle);
            graphics.pose().translate(radius, 0.0F);
            graphics.pose().scale(scale, scale);
            graphics.blit(RenderPipelines.GUI_TEXTURED, ARROWS, -FRAME_SIZE / 2, -FRAME_SIZE / 2, arrow.hollow ? FRAME_SIZE : 0.0F, 0.0F,
                    FRAME_SIZE, FRAME_SIZE, SHEET_WIDTH, FRAME_SIZE, ARGB.color(Math.round(alpha * 255.0F), arrow.color));
            graphics.pose().popMatrix();
        }
    }

    // An arrow for each blocker the player can't see, with the angle on the screen that points to it.
    private static List<Arrow> collect(Minecraft minecraft, GuiGraphicsExtractor graphics, float partialTick) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 eye = camera.position();
        Vector3fc forward = camera.forwardVector();
        Vector3fc up = camera.upVector();
        Vector3fc left = camera.leftVector();
        // Half the view's angles, as tangents: the vertical one from the camera's FOV, the horizontal one by the window's shape.
        double tanV = Math.tan(Math.toRadians(camera.getFov()) / 2.0);
        double tanH = tanV * graphics.guiWidth() / graphics.guiHeight();

        List<Arrow> arrows = new ArrayList<>();
        for (Entity blocker : Blockers.entities()) {
            Vec3 to = blocker.getPosition(partialTick).add(0.0, blocker.getBbHeight() / 2.0, 0.0).subtract(eye);
            // The monster in the camera's own axes: ahead, to the right and up.
            double ahead = to.x * forward.x() + to.y * forward.y() + to.z * forward.z();
            double right = -(to.x * left.x() + to.y * left.y() + to.z * left.z());
            double above = to.x * up.x() + to.y * up.y() + to.z * up.z();
            boolean inView = ahead > 0.0 && Math.abs(right) <= ahead * tanH && Math.abs(above) <= ahead * tanV;
            if (inView && minecraft.player.hasLineOfSight(blocker)) continue;

            // On the screen up is negative; a monster straight behind points down.
            float angle = Math.abs(right) < 1.0E-4 && Math.abs(above) < 1.0E-4 ? Mth.HALF_PI : (float) Math.atan2(-above, right);
            float nearness = 1.0F - (float) Mth.clamp(to.length() / FAR_DISTANCE, 0.0, 1.0);
            arrows.add(new Arrow(angle, Mth.lerp(nearness, 0.8F, 1.15F), inView, Highlights.color(blocker)));
        }
        return arrows;
    }

    // Moves arrows closer than the spacing apart, evenly to both sides, so each one stays as near its true angle as it can.
    private static void fanOut(List<Arrow> arrows, float spacing) {
        int count = arrows.size();
        if (count < 2) return;
        spacing = Math.min(spacing, Mth.TWO_PI / count);
        arrows.sort(Comparator.comparingDouble(arrow -> arrow.angle));
        for (int pass = 0; pass < 8; pass++) {
            boolean moved = false;
            for (int i = 0; i < count; i++) {
                Arrow a = arrows.get(i);
                Arrow b = arrows.get((i + 1) % count);
                // The gap from one arrow to the next around the ring; the last one's neighbor is the first, a turn on.
                float gap = b.angle - a.angle + (i == count - 1 ? Mth.TWO_PI : 0.0F);
                if (gap >= spacing - 1.0E-3F) continue;
                float push = (spacing - gap) / 2.0F;
                a.angle -= push;
                b.angle += push;
                moved = true;
            }
            if (!moved) break;
        }
    }

    private static final class Arrow {
        private float angle;
        private final float scale;
        private final boolean hollow;
        private final int color;

        private Arrow(float angle, float scale, boolean hollow, int color) {
            this.angle = angle;
            this.scale = scale;
            this.hollow = hollow;
            this.color = color;
        }
    }
}
