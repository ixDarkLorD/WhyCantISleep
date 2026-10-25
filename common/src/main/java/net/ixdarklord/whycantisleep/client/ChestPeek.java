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
 * The lid is hinged at the back, so the gap is a wedge, tallest at the front and closing to nothing towards the hinge.
 * The band is cut to it: each wall is drawn in narrow strips, each as tall as the lid lets it be where it stands, with
 * the skin cut off rather than squashed, and the cut is capped with the top of the head, so nothing comes up through
 * the lid and nothing hollow shows. The front is kept just inside the chest's front, even with the head turned.
 * <p>
 * It moves: the eyes come up into the gap as the lid lifts and sink back as it is let down, turn a little to where the
 * player looks, bob gently with their breath, and shiver with monsters about.
 */
public final class ChestPeek {
    // A skin pixel at the player's size (a head is 8 of them, 0.5 blocks at 15/16 scale).
    private static final float PIXEL = 0.9375F / 16.0F;
    // The skin's rows the band shows (the face starts at row 8; default skins have their eyes on rows 12 and 13): the
    // eyes and the face under them, down to the mouth.
    private static final float ROW_TOP = 11.4F;
    private static final float ROW_BOTTOM = 15.6F;
    // The hiding spot sits this far over the chest's base, and this far in front of its middle (BuiltinHidingSpots).
    private static final float SPOT_UP = 0.05F;
    private static final float SPOT_FORWARD = 0.3F;
    // The band's foot: just over the chest's rim, where its lid shuts (10/16), in the spot's own space.
    private static final float RIM = 0.625F + 0.01F - SPOT_UP;
    // The head's middle, behind the spot: its hat layer's face just inside the chest's front (inset 1/16 from the
    // block's edge).
    private static final float BACK = -0.16F;
    // The chest's lid, in the model's pixels from the chest's base and back: hinged 9 up and 1 in, 14 deep. Its front
    // wall is 1 in from the block's front, and the band keeps a little inside that.
    private static final float HINGE_Y = 9.0F;
    private static final float HINGE_Z = 1.0F;
    private static final float LID_DEPTH = 14.0F;
    private static final float FRONT = 14.85F;
    // How far under the lid the band's top stays, in the model's pixels.
    private static final float UNDER_LID = 0.2F;
    private static final float SKIN = 64.0F;
    // Down this far inside the chest before the lid lifts (the band's own height and some), the eyes rise from there.
    private static final float SUNK = 0.26F;
    // How far the eyes turn after the player's look, either way; their bob and shiver.
    private static final float LOOK = 30.0F;
    private static final float BOB = 0.006F;
    private static final float BOB_SPEED = 0.11F;
    private static final float SHIVER = 0.008F;
    // The strips each wall is cut in, and the cap's cells across and through.
    private static final int STRIPS = 8;

    private ChestPeek() {
    }

    public static void submit(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, float rise, boolean trembling) {
        poseStack.pushPose();
        // Local +z is the way the player faces (out of the chest's front), +x their left.
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.bodyRot));
        float age = state.ageInTicks;
        float bob = Mth.sin(age * BOB_SPEED) * BOB;
        float shiver = trembling ? Mth.sin(age * 2.9F) * SHIVER : 0.0F;
        float lift = -(1.0F - rise) * SUNK + bob * rise;
        poseStack.translate(shiver, lift, 0.0F);
        Band band = new Band(-Mth.clamp(state.yRot, -LOOK, LOOK) * Mth.DEG_TO_RAD, lift, lidAngle(rise));
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(state.skin.body().texturePath()), (pose, buffer) -> {
            band.walls(pose, buffer, light, 4.0F, 0.0F);
            band.cap(pose, buffer, light, 4.0F, 0.0F, 0.0F);
            // The hat layer, half a pixel out all round, as the game draws it, its cap a hair over the head's so they
            // don't fight; see-through where the skin has no hat, so a bare head has no brim.
            band.walls(pose, buffer, light, 4.5F, 32.0F);
            band.cap(pose, buffer, light, 4.5F, 32.0F, 0.002F);
        });
        poseStack.popPose();
    }

    // The lid's angle, in radians, as the chest's renderer turns it for the crack the hider holds it open by.
    private static float lidAngle(float rise) {
        float shut = 1.0F - rise * HidingClient.LID_AJAR;
        return (1.0F - shut * shut * shut) * Mth.HALF_PI;
    }

    /** The band as it stands this frame: turned after the look, raised by {@code lift}, under a lid this open. */
    private record Band(float turn, float lift, float lid) {
        private static final float TOP = RIM + (ROW_BOTTOM - ROW_TOP) * PIXEL;

        // The four walls of a head cube of this half-size (in skin pixels), the skin this far right.
        void walls(PoseStack.Pose pose, VertexConsumer buffer, int light, float half, float uShift) {
            float h = half * PIXEL;
            // Front (the face), seen from in front: the player's right (-x) at the face's left edge.
            this.wall(pose, buffer, light, -h, h, h, h, 8.0F + uShift, 16.0F + uShift, 0.0F, 1.0F);
            // Their right side (-x), from its back edge to the face.
            this.wall(pose, buffer, light, -h, -h, -h, h, 0.0F + uShift, 8.0F + uShift, -1.0F, 0.0F);
            // Their left side (+x), from the face to its back edge.
            this.wall(pose, buffer, light, h, h, h, -h, 16.0F + uShift, 24.0F + uShift, 1.0F, 0.0F);
            // The back of the head, from their left to their right.
            this.wall(pose, buffer, light, h, -h, -h, -h, 24.0F + uShift, 32.0F + uShift, 0.0F, -1.0F);
        }

        // A wall from (x0, z0) to (x1, z1) about the head's middle, in strips each cut to the lid over it.
        private void wall(PoseStack.Pose pose, VertexConsumer buffer, int light, float x0, float z0, float x1, float z1, float u0, float u1,
                          float nx, float nz) {
            float[] normal = this.turned(nx, nz);
            for (int strip = 0; strip < STRIPS; strip++) {
                float a = strip / (float) STRIPS;
                float b = (strip + 1) / (float) STRIPS;
                float[] start = this.place(Mth.lerp(a, x0, x1), Mth.lerp(a, z0, z1));
                float[] end = this.place(Mth.lerp(b, x0, x1), Mth.lerp(b, z0, z1));
                float topA = this.top(start[1]);
                float topB = this.top(end[1]);
                if (topA <= RIM && topB <= RIM) continue;
                float ua = Mth.lerp(a, u0, u1) / SKIN;
                float ub = Mth.lerp(b, u0, u1) / SKIN;
                vertex(pose, buffer, light, start[0], topA, start[1], ua, row(topA), normal[0], 0.0F, normal[1]);
                vertex(pose, buffer, light, start[0], RIM, start[1], ua, ROW_BOTTOM / SKIN, normal[0], 0.0F, normal[1]);
                vertex(pose, buffer, light, end[0], RIM, end[1], ub, ROW_BOTTOM / SKIN, normal[0], 0.0F, normal[1]);
                vertex(pose, buffer, light, end[0], topB, end[1], ub, row(topB), normal[0], 0.0F, normal[1]);
            }
        }

        // The cut across the top, as the head's top face: the player's right (-x) at the skin's left, the back at its top.
        void cap(PoseStack.Pose pose, VertexConsumer buffer, int light, float half, float uShift, float over) {
            float h = half * PIXEL;
            for (int across = 0; across < STRIPS; across++) {
                for (int through = 0; through < STRIPS; through++) {
                    float[][] corners = new float[4][];
                    float[][] uvs = new float[4][];
                    int[][] at = {{across, through}, {across, through + 1}, {across + 1, through + 1}, {across + 1, through}};
                    boolean shown = false;
                    for (int corner = 0; corner < 4; corner++) {
                        float sx = at[corner][0] / (float) STRIPS;
                        float sz = at[corner][1] / (float) STRIPS;
                        float[] placed = this.place(Mth.lerp(sx, -h, h), Mth.lerp(sz, -h, h));
                        float top = this.top(placed[1]);
                        shown |= top > RIM;
                        corners[corner] = new float[]{placed[0], Math.max(top, RIM) + over, placed[1]};
                        uvs[corner] = new float[]{(8.0F + uShift + sx * 8.0F) / SKIN, sz * 8.0F / SKIN};
                    }
                    if (!shown) continue;
                    // Counter-clockwise seen from above.
                    for (int corner = 0; corner < 4; corner++) {
                        vertex(pose, buffer, light, corners[corner][0], corners[corner][1], corners[corner][2], uvs[corner][0], uvs[corner][1], 0.0F, 1.0F, 0.0F);
                    }
                }
            }
        }

        // A point about the head's middle, turned after the look and set behind the spot, kept inside the chest's front.
        private float[] place(float x, float z) {
            float cos = Mth.cos(this.turn);
            float sin = Mth.sin(this.turn);
            float px = x * cos + z * sin;
            float pz = BACK - x * sin + z * cos;
            return new float[]{px, Math.min(pz, FRONT / 16.0F - 0.5F - SPOT_FORWARD)};
        }

        // A direction turned after the look.
        private float[] turned(float x, float z) {
            float cos = Mth.cos(this.turn);
            float sin = Mth.sin(this.turn);
            return new float[]{x * cos + z * sin, -x * sin + z * cos};
        }

        // How high the band may stand at this depth (the spot's own z): under the lid there, at most its full height.
        private float top(float z) {
            float depth = (z + SPOT_FORWARD + 0.5F) * 16.0F;
            // Past the lid's front edge the lid is no lower than at its edge.
            float along = Math.min(depth - HINGE_Z, LID_DEPTH * Mth.cos(this.lid));
            float under = HINGE_Y + Math.max(0.0F, along) * (float) Math.tan(this.lid) - UNDER_LID;
            return Math.min(TOP, under / 16.0F - SPOT_UP - this.lift);
        }
    }

    // The skin's row at a height of the band: its foot is the band's last row, and a cut top shows less of it.
    private static float row(float top) {
        return (ROW_BOTTOM - (top - RIM) / PIXEL) / SKIN;
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, int light, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        buffer.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
