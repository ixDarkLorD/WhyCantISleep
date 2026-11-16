package net.ixdarklord.whycantisleep.client.terror;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;

/**
 * Where a night terror happens and how it runs, shared by its scenes ({@link TerrorScene}): the bed's head on the
 * floor, the way from its foot to its head and across it, the floor's height, how bad it is and how long it lasts; and
 * its timeline, as shares of that length, which every scene keeps to: it builds, the eyes go wide at {@link #STARE},
 * and at {@link #JOLT} it ends in a jolt, the eyes slamming shut, everything held until {@link #LINGER_TICKS} after.
 */
public final class TerrorStage {
    public static final float STARE = 0.86F;
    public static final float JOLT = 0.94F;
    public static final int LINGER_TICKS = 6;
    /** The bed's head, on the floor; the way from the bed's foot to its head; across the bed; the floor's height. */
    public Vec3 head = Vec3.ZERO;
    public Vec3 along = Vec3.ZERO;
    public Vec3 across = Vec3.ZERO;
    public double floor;
    /** How bad it is, 0 to 1, and how long it lasts, in ticks. */
    public float intensity;
    public int length;

    /** The tick of the jolt, into the terror. */
    public int joltTick() {
        return Math.round(JOLT * this.length);
    }

    /** The tick of the stare, into the terror. */
    public int stareTick() {
        return Math.round(STARE * this.length);
    }

    /** Whether the terror's sights are still up this far in: until a little after the jolt, the eyes shut by then. */
    public boolean held(float age) {
        return age < this.joltTick() + LINGER_TICKS;
    }

    /**
     * How far from the stare to the sights' end this is, 0 to 1: what comes for them creeps in over it, as the eyes
     * go and the view fades.
     */
    public float creep(float age) {
        float from = STARE * this.length;
        return Math.clamp((age - from) / (this.joltTick() + LINGER_TICKS - from), 0.0F, 1.0F);
    }

    /** The point this far ahead of the bed's head (towards its foot) and this far across it, on the floor. */
    public Vec3 onFloor(double ahead, double side) {
        Vec3 at = this.head.subtract(this.along.scale(ahead)).add(this.across.scale(side));
        return new Vec3(at.x, this.floor, at.z);
    }

    /**
     * An upright quad standing on a point and turned to the camera about the vertical, its top leaning this way: the
     * corners top left, bottom left, bottom right, top right.
     */
    public Vec3[] upright(Vec3 base, Vec3 camera, double width, double height, Vec3 lean) {
        Vec3 toCamera = new Vec3(camera.x - base.x, 0.0, camera.z - base.z);
        Vec3 right = toCamera.lengthSqr() < 1.0E-6 ? this.across : new Vec3(toCamera.z, 0.0, -toCamera.x).normalize();
        Vec3 half = right.scale(width / 2.0);
        Vec3 top = base.add(0.0, height, 0.0).add(lean);
        return new Vec3[]{top.subtract(half), base.subtract(half), base.add(half), top.add(half)};
    }

    /**
     * A quad lying flat at a point, its top (the texture's top) pointing this way along the ground: the corners top
     * left, bottom left, bottom right, top right.
     */
    public static Vec3[] flat(Vec3 middle, Vec3 heading, double halfSize) {
        Vec3 forward = new Vec3(heading.x, 0.0, heading.z);
        forward = forward.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : forward.normalize();
        Vec3 f = forward.scale(halfSize);
        Vec3 r = new Vec3(-forward.z, 0.0, forward.x).scale(halfSize);
        return new Vec3[]{middle.add(f).subtract(r), middle.subtract(f).subtract(r), middle.subtract(f).add(r), middle.add(f).add(r)};
    }

    public static Vec3 lerp(Vec3 from, Vec3 to, double share) {
        return from.add(to.subtract(from).scale(share));
    }

    /** A point along a curve from a to c, drawn towards b, at t from 0 to 1. */
    public static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, double t) {
        return lerp(lerp(a, b, t), lerp(b, c, t), t);
    }

    /** Plays a sound in the player's head, as the terror's sounds are: not muffled with the world. */
    public static void play(Minecraft minecraft, SoundEvent sound, float volume, float pitch) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}
