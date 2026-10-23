package net.ixdarklord.whycantisleep.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ixdarklord.whycantisleep.api.HidingPosture;
import net.ixdarklord.whycantisleep.client.ChestPeek;
import net.ixdarklord.whycantisleep.client.HidingClient;
import net.ixdarklord.whycantisleep.hiding.Hiding;
import net.ixdarklord.whycantisleep.hiding.HidingView;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A hiding player is drawn still, lying on the floor under a bed or table, or standing in a closet, and with monsters
 * about ({@link HidingClient#isTrembling}) the whole body trembles; the head's limited look is {@code HumanoidModelMixin}'s.
 * Only the drawing is held; the player still looks around freely.
 * <p>
 * Crouched in a chest, only their eyes show, peering out under the raised lid ({@link ChestPeek}), drawn in place of the
 * whole player, so no name shows over it either.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    // The crawling pose is drawn for a crawler's full height; under a bed it lies this much lower, on the floor.
    private static final double WHYCANTISLEEP$FLOOR_DROP = 0.18;
    // The tremble: a few hundredths of a block, quick and uneven.
    private static final double WHYCANTISLEEP$TREMBLE = 0.012;

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void whycantisleep$peekFromTheChest(AbstractClientPlayer player, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers,
                                               int light, CallbackInfo ci) {
        HidingView view = Hiding.view(player);
        if (view == null || view.posture() != HidingPosture.TUCKED) return;
        ChestPeek.render(player, partialTicks, poseStack, buffers, light, HidingClient.peekRise(player.getId(), partialTicks), HidingClient.isTrembling(player.getId()));
        ci.cancel();
    }

    @Inject(method = "getRenderOffset(Lnet/minecraft/client/player/AbstractClientPlayer;F)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void whycantisleep$onTheFloor(AbstractClientPlayer player, float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        HidingView view = Hiding.view(player);
        if (view == null) return;
        double x = 0.0;
        double z = 0.0;
        if (HidingClient.isTrembling(player.getId())) {
            // Two quick waves out of step, so it shivers rather than sways.
            float age = player.tickCount + partialTicks;
            x = Mth.sin(age * 2.9F) * WHYCANTISLEEP$TREMBLE;
            z = Mth.cos(age * 3.7F) * WHYCANTISLEEP$TREMBLE;
        }
        double drop = view.lying() ? WHYCANTISLEEP$FLOOR_DROP : 0.0;
        cir.setReturnValue(cir.getReturnValue().add(x, -drop, z));
    }
}
