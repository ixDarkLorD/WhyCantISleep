package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.HidingClient;
import net.ixdarklord.whycantisleep.hiding.HidingView;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No hand in view while hiding: under a bed it would reach up through the frame, and in a closet it holds still.
 * <p>
 * Lying flat under a bed or table in first person, the near plane is drawn in close: the gap under a bed is only 3/16
 * of a block, and at the game's 0.05 the near plane's corners reach further than half of it at any field of view,
 * cutting the bed open when looking up and the floor when looking down.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    // The near plane under a bed: its corners stay within the gap's half (0.094) up to a 110 degree field of view.
    private static final float WHYCANTISLEEP$UNDER_BED_NEAR = 0.02F;

    @Shadow
    @Final
    private Camera mainCamera;

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$noHandUnderTheBed(CallbackInfo ci) {
        if (HidingClient.isLocalHiding()) ci.cancel();
    }

    @ModifyArg(method = "getProjectionMatrix", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;perspective(FFFF)Lorg/joml/Matrix4f;"), index = 2)
    private float whycantisleep$nearPlaneUnderTheBed(float zNear) {
        if (this.mainCamera.isDetached() || this.mainCamera.getEntity() != Minecraft.getInstance().player) return zNear;
        HidingView view = HidingClient.localView();
        return view != null && view.lying() ? WHYCANTISLEEP$UNDER_BED_NEAR : zNear;
    }
}
