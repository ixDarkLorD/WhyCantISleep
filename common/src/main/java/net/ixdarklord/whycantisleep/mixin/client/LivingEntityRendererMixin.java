package net.ixdarklord.whycantisleep.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ixdarklord.whycantisleep.api.HidingPosture;
import net.ixdarklord.whycantisleep.client.ChestPeek;
import net.ixdarklord.whycantisleep.client.HidingClient;
import net.ixdarklord.whycantisleep.hiding.Hiding;
import net.ixdarklord.whycantisleep.hiding.HidingView;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A player crouched in a chest is drawn as only the eyes peering out under its lid ({@link ChestPeek}), and no name
 * over it: the rest of them, name tag and all, isn't drawn.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void whycantisleep$peekFromTheChest(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers,
                                               int light, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player)) return;
        HidingView view = Hiding.view(player);
        if (view == null || view.posture() != HidingPosture.TUCKED) return;
        ChestPeek.render(player, partialTicks, poseStack, buffers, light, HidingClient.peekRise(player.getId(), partialTicks),
                HidingClient.isTrembling(player.getId()));
        ci.cancel();
    }
}
