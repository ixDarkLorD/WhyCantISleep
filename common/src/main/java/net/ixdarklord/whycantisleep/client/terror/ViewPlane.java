package net.ixdarklord.whycantisleep.client.terror;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

/**
 * The view as a flat plane this far in front of the eyes, square to the line of sight: its middle, its right and up,
 * and how far it reaches to the screen's edges there (from the field of view and the window's shape). What is drawn in
 * it lies flat to the view, so it never turns or twists however it moves: the hands over the eyes, things crawling
 * over them.
 */
public record ViewPlane(Vec3 camera, Vec3 ahead, Vec3 right, Vec3 up, double distance, double halfWidth, double halfHeight) {
    public static ViewPlane of(Vec3 camera, double distance) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera view = minecraft.gameRenderer.getMainCamera();
        Vector3fc forward = view.forwardVector();
        Vector3fc left = view.leftVector();
        Vector3fc up = view.upVector();
        double halfHeight = distance * Math.tan(Math.toRadians(view.getFov()) / 2.0);
        double halfWidth = halfHeight * minecraft.getWindow().getWidth() / Math.max(1.0, minecraft.getWindow().getHeight());
        return new ViewPlane(camera, new Vec3(forward.x(), forward.y(), forward.z()), new Vec3(-left.x(), -left.y(), -left.z()),
                new Vec3(up.x(), up.y(), up.z()), distance, halfWidth, halfHeight);
    }

    /** The plane's middle, straight ahead. */
    public Vec3 middle() {
        return this.camera.add(this.ahead.scale(this.distance));
    }

    /** The point this far right and up of the middle. */
    public Vec3 at(double x, double y) {
        return this.middle().add(this.right.scale(x)).add(this.up.scale(y));
    }

    /** The way out from the middle at this angle (0 right, a quarter turn up). */
    public Vec3 out(double angle) {
        return this.right.scale(Math.cos(angle)).add(this.up.scale(Math.sin(angle)));
    }

    /** How far from the middle the screen's edge is at this angle. */
    public double edge(double angle) {
        return Math.min(this.halfWidth / Math.max(1.0E-4, Math.abs(Math.cos(angle))), this.halfHeight / Math.max(1.0E-4, Math.abs(Math.sin(angle))));
    }

    /**
     * A quad lying in the plane, its top (the texture's top) pointing this way across it: the corners top left, bottom
     * left, bottom right, top right.
     */
    public Vec3[] quad(Vec3 middle, Vec3 heading, double halfLength, double halfWidth) {
        Vec3 f = heading.scale(halfLength);
        Vec3 r = heading.cross(this.ahead).normalize().scale(-halfWidth);
        return new Vec3[]{middle.add(f).subtract(r), middle.subtract(f).subtract(r), middle.subtract(f).add(r), middle.add(f).add(r)};
    }
}
