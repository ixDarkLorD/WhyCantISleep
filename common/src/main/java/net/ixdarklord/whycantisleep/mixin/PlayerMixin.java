package net.ixdarklord.whycantisleep.mixin;

import net.ixdarklord.whycantisleep.api.HidingPosture;
import net.ixdarklord.whycantisleep.hiding.Hiding;
import net.ixdarklord.whycantisleep.hiding.HidingView;
import net.ixdarklord.whycantisleep.sleep.SleepNights;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A hiding player lies flat under a bed or table, or curled in a chest (the crawling pose, on both sides so everyone
 * sees it), or stands in a closet. Who can still see them
 * is {@code TargetingConditionsMixin} and {@code LivingEntityMixin}'s business, which know who is looking. And they
 * don't move: no momentum carries them on, so the client never drifts from where the server holds them. A player in
 * a night terror hasn't slept long enough yet: the night waits for the terror to end before it passes.
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

    @Inject(method = "isSleepingLongEnough", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$terrorHoldsTheNight(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer player && SleepNights.inTerror(player)) cir.setReturnValue(false);
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$stillWhileHiding(Vec3 input, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!Hiding.isHiding(self)) return;
        self.setDeltaMovement(Vec3.ZERO);
        ci.cancel();
    }
}
