package net.ixdarklord.whycantisleep.api;

/** How a player is placed in a hiding spot, which decides how they are drawn and what they see. */
public enum HidingPosture {
    /** Flat on the floor under something low: a bed, a table. Drawn lying still, the view at the spot's eye height. */
    LYING(0.09F),
    /** On their feet in a tight space: a closet. Drawn standing still, the view at the player's own eye height. */
    STANDING(-1.0F),
    /** Crouched inside something: a chest. Only the eyes show, peering out under the raised lid, and so does the view. */
    TUCKED(0.45F);

    private final float defaultEyeHeight;

    HidingPosture(float defaultEyeHeight) {
        this.defaultEyeHeight = defaultEyeHeight;
    }

    /** The view's height above the spot's position when a spot doesn't give its own; negative for the player's own. */
    public float defaultEyeHeight() {
        return this.defaultEyeHeight;
    }
}
