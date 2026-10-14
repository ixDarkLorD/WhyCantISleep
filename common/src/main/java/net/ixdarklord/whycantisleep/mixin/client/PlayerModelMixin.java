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
            // Falling face down, spread out like a skydiver: arms flung out to the sides and arched back towards the
            // sky, legs wide and kicked back, all of it flailing out of step. On this model the right arm and leg go
            // out with a positive turn about z, the left ones with a negative one; a positive turn about x swings a
            // limb behind the body, which face down is up.
            this.rightArm.zRot = 1.25F + Mth.sin(age * 0.7F) * 0.35F;
            this.leftArm.zRot = -1.25F - Mth.sin(age * 0.7F + 1.3F) * 0.35F;
            this.rightArm.xRot = 0.45F + Mth.sin(age * 0.95F) * 0.55F;
            this.leftArm.xRot = 0.45F + Mth.sin(age * 0.95F + 2.1F) * 0.55F;
            this.rightLeg.zRot = 0.35F + Mth.sin(age * 0.5F) * 0.12F;
            this.leftLeg.zRot = -0.35F - Mth.sin(age * 0.5F + 0.8F) * 0.12F;
            this.rightLeg.xRot = 0.5F + Mth.sin(age * 0.8F) * 0.45F;
            this.leftLeg.xRot = 0.5F + Mth.sin(age * 0.8F + Mth.PI) * 0.45F;
            // Head craned up, to see what is coming.
            this.head.xRot = -0.7F + Mth.sin(age * 0.6F) * 0.12F;
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
