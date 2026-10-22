package net.ixdarklord.whycantisleep.mixin;

import net.ixdarklord.whycantisleep.api.HidingPosture;
import net.ixdarklord.whycantisleep.hiding.Hiding;
import net.ixdarklord.whycantisleep.hiding.HidingView;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A hiding player lies flat under a bed or table, or curled in a chest (the crawling pose, on both sides so everyone
 * sees it), or stands in a closet. Who can still see them
 * is {@code TargetingConditionsMixin} and {@code LivingEntityMixin}'s business, which know who is looking.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$hidingPose(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        HidingView view = Hiding.view(self);
        if (view != null) {
            self.setPose(view.posture() == HidingPosture.STANDING ? Pose.STANDING : Pose.SWIMMING);
            ci.cancel();
        }
    }
}
