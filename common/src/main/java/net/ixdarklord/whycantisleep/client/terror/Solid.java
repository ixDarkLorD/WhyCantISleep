package net.ixdarklord.whycantisleep.client.terror;

import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.minecraft.world.phys.Vec3;

/**
 * Solid shapes out of world effects' quads: boxes, turned any way, with only the faces towards the camera drawn (world
 * effects write no depth, so a box's far faces would otherwise show through its near ones), each face shaded by which
 * way it faces, as the game shades blocks: lit from above, darker on the sides and darkest underneath. Draw them in a
 * sorted layer, so one box covers another as their distance says.
 */
public final class Solid {
    private Solid() {
    }

    /**
     * A box round a middle, its half-sizes along its own forward, right and up (which needn't be level), its texture
     * across each face, tinted this color and shaded.
     */
    public static void box(VfxDraw draw, Vec3 middle, Vec3 forward, Vec3 right, Vec3 up, Vec3 camera, int color) {
        face(draw, middle, up, right, forward, camera, color, 1.0F);
        face(draw, middle, up.scale(-1.0), right.scale(-1.0), forward, camera, color, 0.5F);
        face(draw, middle, forward, right, up, camera, color, 0.82F);
        face(draw, middle, forward.scale(-1.0), right.scale(-1.0), up, camera, color, 0.82F);
        face(draw, middle, right, forward.scale(-1.0), up, camera, color, 0.65F);
        face(draw, middle, right.scale(-1.0), forward, up, camera, color, 0.65F);
    }

    // One face: out from the middle along `out` (its half-size that way), spanned by `across` and `along` (half-sizes);
    // drawn only if it faces the camera, darkened by `shade`.
    private static void face(VfxDraw draw, Vec3 middle, Vec3 out, Vec3 across, Vec3 along, Vec3 camera, int color, float shade) {
        Vec3 center = middle.add(out);
        if (out.dot(camera.subtract(center)) <= 0.0) return;
        int a = color >>> 24;
        int r = Math.round(((color >> 16) & 0xFF) * shade);
        int g = Math.round(((color >> 8) & 0xFF) * shade);
        int b = Math.round((color & 0xFF) * shade);
        int shaded = a << 24 | r << 16 | g << 8 | b;
        draw.quad(center.subtract(across).add(along), center.subtract(across).subtract(along), center.add(across).subtract(along),
                center.add(across).add(along), 0.0F, 0.0F, 1.0F, 1.0F, shaded);
    }

    /** A vector turned round an axis (a unit vector) by an angle in radians. */
    public static Vec3 turn(Vec3 v, Vec3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return v.scale(cos).add(axis.cross(v).scale(sin)).add(axis.scale(axis.dot(v) * (1.0 - cos)));
    }
}
