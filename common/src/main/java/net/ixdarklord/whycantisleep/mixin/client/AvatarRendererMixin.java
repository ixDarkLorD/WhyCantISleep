package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.api.HidingPosture;
import net.ixdarklord.whycantisleep.client.HidingClient;
import net.ixdarklord.whycantisleep.client.HidingPose;
import net.ixdarklord.whycantisleep.hiding.Hiding;
import net.ixdarklord.whycantisleep.hiding.HidingView;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A hiding player is drawn still, lying on the floor under a bed or table, or standing in a closet: the head follows where they look only a little
 * (turned at most {@link #WHYCANTISLEEP$HEAD_TURN} either way, nodding a little up or down), rather than not at all or
 * right round like an owl's; and with monsters about ({@link HidingClient#isTrembling}) the whole body trembles.
 * Only the drawing is held; the player still looks around freely.
 * <p>
 * Crouched in a chest, only their eyes show, peering out under the raised lid ({@code ChestPeek}, from
 * {@code LivingEntityRendererMixin}); nor is their name shown over it.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    private static final float WHYCANTISLEEP$HEAD_TURN = 90.0F;
    private static final float WHYCANTISLEEP$HEAD_UP = -12.0F;
    private static final float WHYCANTISLEEP$HEAD_DOWN = 18.0F;
    // The crawling pose is drawn for a crawler's full height; under a bed it lies this much lower, on the floor.
    private static final double WHYCANTISLEEP$FLOOR_DROP = 0.18;
    // The tremble: a few hundredths of a block, quick and uneven.
    private static final double WHYCANTISLEEP$TREMBLE = 0.012;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void whycantisleep$stillUnderTheBed(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        HidingView view = entity instanceof Player player ? Hiding.view(player) : null;
        boolean hiding = view != null;
        HidingPose pose = (HidingPose) state;
        // Render states are reused from frame to frame, so the flags are set either way.
        pose.whycantisleep$setHiding(hiding);
        pose.whycantisleep$setLying(hiding && view.lying());
        pose.whycantisleep$setTucked(hiding && view.posture() == HidingPosture.TUCKED);
        pose.whycantisleep$setPeekRise(pose.whycantisleep$isTucked() ? HidingClient.peekRise(entity.getId(), partialTicks) : 0.0F);
        if (pose.whycantisleep$isTucked()) state.nameTag = null;
        pose.whycantisleep$setTrembling(hiding && HidingClient.isTrembling(entity.getId()));
        if (hiding) {
            state.yRot = Mth.clamp(state.yRot, -WHYCANTISLEEP$HEAD_TURN, WHYCANTISLEEP$HEAD_TURN);
            state.xRot = Mth.clamp(state.xRot, WHYCANTISLEEP$HEAD_UP, WHYCANTISLEEP$HEAD_DOWN);
        }
    }

    @Inject(method = "getRenderOffset(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/world/phys/Vec3;",
            at = @At("RETURN"), cancellable = true)
    private void whycantisleep$onTheFloor(AvatarRenderState state, CallbackInfoReturnable<Vec3> cir) {
        HidingPose pose = (HidingPose) state;
        if (!pose.whycantisleep$isHiding()) return;
        double x = 0.0;
        double z = 0.0;
        if (pose.whycantisleep$isTrembling()) {
            // Two quick waves out of step, so it shivers rather than sways.
            float age = state.ageInTicks;
            x = Mth.sin(age * 2.9F) * WHYCANTISLEEP$TREMBLE;
            z = Mth.cos(age * 3.7F) * WHYCANTISLEEP$TREMBLE;
        }
        double drop = pose.whycantisleep$isLying() ? WHYCANTISLEEP$FLOOR_DROP * state.scale : 0.0;
        cir.setReturnValue(cir.getReturnValue().add(x, -drop, z));
    }
}
