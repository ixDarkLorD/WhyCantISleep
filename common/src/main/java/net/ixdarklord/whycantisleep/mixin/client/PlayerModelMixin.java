package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.deep.client.DreamTransition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The dreamer's body as they fall into the Lucid Deep ({@link DreamTransition}), on their own client: laid face down
 * by {@code PlayerMixin}, here their arms and legs flail as they fall, and once they hit the island they lie spread out,
 * arms flung wide. Other players and everything else are untouched.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin extends HumanoidModel<AvatarRenderState> {
    private PlayerModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void whycantisleep$fallIntoTheDeep(AvatarRenderState state, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || state.id != minecraft.player.getId()) return;
        float age = state.ageInTicks;
        if (DreamTransition.isFalling()) {
            // Flailing: arms thrown out and clawing at the air, legs kicking, each out of step with the others.
            this.rightArm.xRot = -2.4F + Mth.sin(age * 0.9F) * 0.7F;
            this.leftArm.xRot = -2.4F + Mth.sin(age * 0.9F + 2.1F) * 0.7F;
            this.rightArm.zRot = 0.9F + Mth.sin(age * 0.7F) * 0.5F;
            this.leftArm.zRot = -0.9F - Mth.sin(age * 0.7F + 1.3F) * 0.5F;
            this.rightLeg.xRot = Mth.sin(age * 0.8F) * 0.7F;
            this.leftLeg.xRot = Mth.sin(age * 0.8F + Mth.PI) * 0.7F;
            this.rightLeg.zRot = 0.25F + Mth.sin(age * 0.5F) * 0.15F;
            this.leftLeg.zRot = -0.25F - Mth.sin(age * 0.5F + 0.8F) * 0.15F;
            this.head.xRot = -0.6F + Mth.sin(age * 0.6F) * 0.15F;
        } else if (DreamTransition.isSprawled()) {
            // Landed flat: arms flung out to the sides, legs apart, head turned to one side.
            this.rightArm.xRot = -0.2F;
            this.leftArm.xRot = -0.35F;
            this.rightArm.zRot = 1.35F;
            this.leftArm.zRot = -1.25F;
            this.rightLeg.xRot = 0.1F;
            this.leftLeg.xRot = -0.05F;
            this.rightLeg.zRot = 0.35F;
            this.leftLeg.zRot = -0.3F;
            this.head.yRot = 0.9F;
            this.head.xRot = 0.0F;
        }
    }
}
