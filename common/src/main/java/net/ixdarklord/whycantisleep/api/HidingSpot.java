package net.ixdarklord.whycantisleep.api;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A place a player can hide, as a {@link HidingSpotType} found it.
 *
 * @param type          the type that found it
 * @param anchor        the block it belongs to (a bed's head, a door's lower half, one block of a table): checked each
 *                      tick, and the centre of the prowl the hide time shakes off
 * @param position      where the player is held: their feet
 * @param yaw           the way the player's body lies or stands (degrees, as an entity's yaw)
 * @param posture       how they are placed
 * @param eyeHeight     the view's height above {@code position}; negative for the player's own
 * @param squint        how far the hiding player's eyes close, 0 (wide open) to 1 (shut): their view narrows to a
 *                      slit between dark lids, easing in as they get in and out as they come out. Peering out of a
 *                      chest's lid is a narrow squint; standing in a closet, a wide one
 * @param hideTimeScale the server's hide time times this, before cover shortens it further
 * @param icon          what the hiding card shows
 * @param where         how the card says where they are, after "Hiding": "under the bed", "in the chest"
 */
public record HidingSpot(ResourceLocation type, BlockPos anchor, Vec3 position, float yaw, HidingPosture posture, float eyeHeight, float squint,
                         float hideTimeScale, ItemStack icon, Component where) {
    /** The squint a spot has unless it says otherwise. */
    public static final float DEFAULT_SQUINT = 0.5F;

    public HidingSpot {
        squint = Mth.clamp(squint, 0.0F, 1.0F);
    }

    /** The same spot with the eyes closed this far (0 to 1). */
    public HidingSpot withSquint(float squint) {
        return new HidingSpot(this.type, this.anchor, this.position, this.yaw, this.posture, this.eyeHeight, squint, this.hideTimeScale, this.icon, this.where);
    }

    /** The same spot with the player held somewhere else in it: one of the places in a spot that holds two. */
    public HidingSpot withPosition(Vec3 position) {
        return new HidingSpot(this.type, this.anchor, position, this.yaw, this.posture, this.eyeHeight, this.squint, this.hideTimeScale, this.icon, this.where);
    }

    /** The view's height above the spot's position: the spot's own, else its posture's. */
    public float viewHeight() {
        return this.eyeHeight >= 0.0F ? this.eyeHeight : this.posture.defaultEyeHeight();
    }
}
