package net.ixdarklord.whycantisleep.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * A player crouched in a chest, as seen from outside: only the band of their head with their eyes in it, peering out
 * of the gap under the raised lid. The rest of them is inside, out of sight, so it isn't drawn at all; and a whole head
 * wouldn't fit the gap, its crown would come up through the lid. So the band is cut from their own skin, the face and
 * the sides and back of the head with the hat layer over them, and drawn as a slice of the head.
 * <p>
 * It moves: the eyes come up into the gap as the lid lifts and sink back as it is let down, turn a little to where the
 * player looks, bob gently with their breath, and shiver with monsters about.
 */
public final class ChestPeek {
    // A skin pixel at the player's size (a head is 8 of them, 0.5 blocks at 15/16 scale).
    private static final float PIXEL = 0.9375F / 16.0F;
    // The skin's rows the band shows (the face starts at row 8; default skins have their eyes on rows 12 and 13).
    private static final float ROW_TOP = 11.4F;
    private static final float ROW_BOTTOM = 13.8F;
    // The band's foot: just over the chest's rim, where its lid shuts (10/16), above the hiding spot, which sits 0.05
    // over the chest's base.
    private static final float RIM = 0.625F + 0.01F - 0.05F;
    // The head's middle, behind the spot: its hat layer's face just inside the chest's front (inset 1/16 from the
    // block's edge), the spot being 0.3 in front of the chest's middle.
    private static final float BACK = -0.16F;
    private static final float SKIN = 64.0F;
    // Down this far inside the chest before the lid lifts (the band's own height and some), the eyes rise from there.
    private static final float SUNK = 0.16F;
    // How far the eyes turn after the player's look, either way; their bob and shiver.
    private static final float LOOK = 30.0F;
    private static final float BOB = 0.006F;
    private static final float BOB_SPEED = 0.11F;
    private static final float SHIVER = 0.008F;

    private ChestPeek() {
    }

    public static void submit(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, float rise, boolean trembling) {
        poseStack.pushPose();
        // Local +z is the way the player faces (out of the chest's front), +x their left.
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.bodyRot));
        float age = state.ageInTicks;
        float bob = Mth.sin(age * BOB_SPEED) * BOB;
        float shiver = trembling ? Mth.sin(age * 2.9F) * SHIVER : 0.0F;
        poseStack.translate(shiver, -(1.0F - rise) * SUNK + bob * rise, BACK);
        // Turned about the head's middle, after the look.
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.clamp(state.yRot, -LOOK, LOOK)));
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(state.skin.body().texturePath()), (pose, buffer) -> {
            band(pose, buffer, light, 4.0F, 0.0F);
            // The hat layer, half a pixel out all round, as the game draws it.
            band(pose, buffer, light, 4.5F, 32.0F);
        });
        poseStack.popPose();
    }

    // The eye band of a head cube of this half-size (in skin pixels), its texture this far right on the skin.
    private static void band(PoseStack.Pose pose, VertexConsumer buffer, int light, float half, float uShift) {
        float h = half * PIXEL;
        float bottom = RIM;
        float top = RIM + (ROW_BOTTOM - ROW_TOP) * PIXEL;
        // Front (the face), seen from in front: the player's right (-x) at the face's left edge.
        quad(pose, buffer, light, -h, h, h, h, bottom, top, 8.0F + uShift, 16.0F + uShift, 0.0F, 0.0F, 1.0F);
        // Their right side (-x), from its back edge to the face.
        quad(pose, buffer, light, -h, -h, -h, h, bottom, top, 0.0F + uShift, 8.0F + uShift, -1.0F, 0.0F, 0.0F);
        // Their left side (+x), from the face to its back edge.
        quad(pose, buffer, light, h, h, h, -h, bottom, top, 16.0F + uShift, 24.0F + uShift, 1.0F, 0.0F, 0.0F);
        // The back of the head, from their left to their right.
        quad(pose, buffer, light, h, -h, -h, -h, bottom, top, 24.0F + uShift, 32.0F + uShift, 0.0F, 0.0F, -1.0F);
    }

    // A wall from (x0, z0) to (x1, z1), between two heights, textured from u0 to u1 across and the band's rows down.
    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int light, float x0, float z0, float x1, float z1, float bottom, float top,
                             float u0, float u1, float nx, float ny, float nz) {
        float v0 = ROW_TOP / SKIN;
        float v1 = ROW_BOTTOM / SKIN;
        vertex(pose, buffer, light, x0, top, z0, u0 / SKIN, v0, nx, ny, nz);
        vertex(pose, buffer, light, x0, bottom, z0, u0 / SKIN, v1, nx, ny, nz);
        vertex(pose, buffer, light, x1, bottom, z1, u1 / SKIN, v1, nx, ny, nz);
        vertex(pose, buffer, light, x1, top, z1, u1 / SKIN, v0, nx, ny, nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, int light, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        buffer.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
