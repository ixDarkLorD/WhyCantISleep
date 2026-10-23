package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.api.HidingPosture;
import net.ixdarklord.whycantisleep.client.HidingClient;
import net.ixdarklord.whycantisleep.hiding.HidingView;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * In third person while hiding low (under a bed or table, in a chest), the view orbits a point over the spot instead
 * of the player's eyes in it. Standing in a closet, the view is the game's own.
 * <p>
 * In first person, the view sits at the spot's height above the floor. Under a bed it is low, beneath its frame,
 * looking out between its legs, and never through the bed or the floor. The gap is only 3/16 of a block (the bed's
 * underside is at 3/16), so the view sits in its middle, and the near plane is drawn in close for anything lying flat:
 * at the game's 0.05 its corners reach further than that at any field of view, cutting the bed open when looking up
 * and the floor when looking down ({@link HidingClient#nearPlane}, from {@code GameRendererMixin}).
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    // In third person while hiding: the point the view orbits, this high over the floor (a block over the bed's
    // 9/16 top), and how far back from it the view sits, as far as the blocks round it allow.
    private static final double WHYCANTISLEEP$OVER_BED = 9.0 / 16.0 + 1.0;
    private static final double WHYCANTISLEEP$OVER_BED_DISTANCE = 4.0;
    // Crouched in a chest, the view is this far ahead of the player, just behind the chest's front.
    private static final double WHYCANTISLEEP$TO_THE_GAP = 0.12;

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void move(double distanceOffset, double verticalOffset, double horizontalOffset);

    @Shadow
    private double getMaxZoom(double startingDistance) {
        throw new AssertionError();
    }

    @Shadow
    public abstract Vec3 getPosition();

    @Shadow
    public abstract boolean isDetached();

    @Shadow
    public abstract Entity getEntity();

    @Inject(method = "setup", at = @At("TAIL"))
    private void whycantisleep$underTheBed(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTicks, CallbackInfo ci) {
        HidingView view = this.getEntity() == Minecraft.getInstance().player ? HidingClient.localView() : null;
        if (view == null || view.posture() == HidingPosture.STANDING) return;
        if (!this.isDetached()) {
            Vec3 at = this.getPosition();
            // A hiding player is held still, so their feet are the floor. Crouched in a chest, the eyes are up against
            // the gap under its lid, to see out of it rather than the chest's own front.
            double forward = view.posture() == HidingPosture.TUCKED ? WHYCANTISLEEP$TO_THE_GAP : 0.0;
            float yaw = view.yaw() * Mth.DEG_TO_RAD;
            this.setPosition(at.x - Mth.sin(yaw) * forward, this.getEntity().getY() + view.eyeHeight(), at.z + Mth.cos(yaw) * forward);
        } else {
            // In third person the view orbits a point over the bed rather than the player's eyes beneath it, which
            // left it jammed under the frame: from above, the bed and the room round it are what there is to see.
            // The player lies at the middle of the bed, so its middle is straight above them.
            Entity player = this.getEntity();
            this.setPosition(player.getX(), player.getY() + WHYCANTISLEEP$OVER_BED, player.getZ());
            this.move(-this.getMaxZoom(WHYCANTISLEEP$OVER_BED_DISTANCE), 0.0, 0.0);
        }
    }
}
