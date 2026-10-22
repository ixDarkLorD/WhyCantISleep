package net.ixdarklord.whycantisleep.hiding;

import net.ixdarklord.whycantisleep.api.HidingPosture;
import net.ixdarklord.whycantisleep.api.HidingSpot;

/**
 * What both sides know of how a player hides, enough to place, pose and draw them: how they are placed, the way their
 * body faces, how high above their feet the view is (negative for their own eye height), and how far their eyes close.
 */
public record HidingView(HidingPosture posture, float yaw, float eyeHeight, float squint) {
    public static HidingView of(HidingSpot spot) {
        return new HidingView(spot.posture(), spot.yaw(), spot.viewHeight(), spot.squint());
    }

    /** Whether the player is drawn flat on the floor: lying under something low. */
    public boolean lying() {
        return this.posture == HidingPosture.LYING;
    }
}
