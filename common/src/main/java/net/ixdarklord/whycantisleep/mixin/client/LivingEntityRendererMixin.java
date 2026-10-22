package net.ixdarklord.whycantisleep.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ixdarklord.whycantisleep.client.ChestPeek;
import net.ixdarklord.whycantisleep.client.HidingPose;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player crouched in a chest is drawn as only the eyes peering out under its lid ({@link ChestPeek}). */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"), cancellable = true)
    private void whycantisleep$peekFromTheChest(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera,
                                               CallbackInfo ci) {
        if (state instanceof AvatarRenderState avatar && ((HidingPose) avatar).whycantisleep$isTucked()) {
            HidingPose pose = (HidingPose) avatar;
            ChestPeek.submit(avatar, poseStack, collector, pose.whycantisleep$peekRise(), pose.whycantisleep$isTrembling());
            ci.cancel();
        }
    }
}
