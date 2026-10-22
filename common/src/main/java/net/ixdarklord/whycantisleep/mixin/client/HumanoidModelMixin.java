package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.HidingPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A player hiding under a bed lies flat and still: arms and legs straight in line with the body, the head too but for
 * a small, limited look round. The crawling pose tips the head up to look ahead and swings the arms in a paddling
 * stroke, which lifted the head out through the pillow and a hand up through the blanket.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    // The arms lie a hair out from the body's sides rather than inside them.
    private static final float WHYCANTISLEEP$ARM_SPREAD = 0.05F;

    @Shadow
    @Final
    public ModelPart head;

    @Shadow
    @Final
    public ModelPart rightArm;

    @Shadow
    @Final
    public ModelPart leftArm;

    @Shadow
    @Final
    public ModelPart rightLeg;

    @Shadow
    @Final
    public ModelPart leftLeg;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void whycantisleep$stillUnderTheBed(HumanoidRenderState state, CallbackInfo ci) {
        if (!((Object) state instanceof HidingPose pose) || !pose.whycantisleep$isHiding()) return;
        // The head follows the look a little (AvatarRendererMixin limits how far), from in line with the body.
        this.head.setRotation(state.xRot * Mth.DEG_TO_RAD, state.yRot * Mth.DEG_TO_RAD, 0.0F);
        this.rightArm.setRotation(0.0F, 0.0F, WHYCANTISLEEP$ARM_SPREAD);
        this.leftArm.setRotation(0.0F, 0.0F, -WHYCANTISLEEP$ARM_SPREAD);
        this.rightLeg.setRotation(0.0F, 0.0F, 0.0F);
        this.leftLeg.setRotation(0.0F, 0.0F, 0.0F);
    }
}
