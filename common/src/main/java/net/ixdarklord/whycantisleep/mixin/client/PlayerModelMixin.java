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
 * The dreamer's body as they fall into the Lucid Deep and get up again ({@link DreamTransition}), on their own client
 * only. {@code PlayerMixin} lays them face down, and crouches them as they come up; here the limbs move:
 * <ul>
 *     <li><b>Falling</b>: spread out like a skydiver, buffeted by the air rushing past: arms wide and pressed back,
 *     legs apart, everything shaking and now and then thrown about as they try to right themselves.</li>
 *     <li><b>Landed</b>: sprawled flat where they hit, arms flung out, head turned aside; a twitch, then nothing.</li>
 *     <li><b>Getting up</b>: they draw their hands in under their shoulders and push their chest up off the ground,
 *     head lifting; then, up on their feet in a crouch, one hand on a knee, head hung, they straighten.</li>
 * </ul>
 * On this model a positive zRot takes the right arm and leg out from the body and a negative one the left; a positive
 * xRot swings a limb behind the body (towards the sky, face down) and a negative one in front of it.
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
            // The rush of air: a fast, small shake on everything, and every so often a slower heave of one arm or leg.
            float rush = Mth.sin(age * 2.7F) * 0.06F + Mth.sin(age * 4.1F + 1.0F) * 0.04F;
            float heaveR = Math.max(0.0F, Mth.sin(age * 0.33F)) * 0.6F;
            float heaveL = Math.max(0.0F, Mth.sin(age * 0.29F + 2.0F)) * 0.6F;
            this.rightArm.zRot = 1.35F + rush - heaveR * 0.5F;
            this.leftArm.zRot = -1.35F - rush + heaveL * 0.5F;
            this.rightArm.xRot = 0.35F + rush * 2.0F - heaveR;
            this.leftArm.xRot = 0.35F - rush * 2.0F - heaveL;
            this.rightLeg.zRot = 0.3F + rush;
            this.leftLeg.zRot = -0.3F - rush;
            this.rightLeg.xRot = 0.35F + Mth.sin(age * 0.6F) * 0.3F;
            this.leftLeg.xRot = 0.35F + Mth.sin(age * 0.6F + 2.4F) * 0.3F;
            this.head.xRot = -0.65F + rush;
            this.head.yRot = Mth.sin(age * 0.2F) * 0.25F;
        } else if (DreamTransition.isSprawled()) {
            // Flat where they hit: arms flung out, legs a little apart, head on its side; a hand twitches now and then.
            float twitch = Math.max(0.0F, Mth.sin(age * 1.7F) - 0.92F) * 3.0F;
            this.rightArm.zRot = 1.4F;
            this.leftArm.zRot = -1.2F;
            this.rightArm.xRot = -0.15F + twitch * 0.3F;
            this.leftArm.xRot = 0.1F;
            this.rightLeg.zRot = 0.32F;
            this.leftLeg.zRot = -0.24F;
            this.rightLeg.xRot = 0.05F;
            this.leftLeg.xRot = -0.08F;
            this.head.yRot = 1.0F;
            this.head.xRot = 0.0F;
        } else if (DreamTransition.isRising()) {
            float progress = DreamTransition.riseProgress(age - (float) Math.floor(age));
            float part = DreamTransition.pushUpPart();
            if (progress < part) {
                // Pushing up: the hands come in under the shoulders, the arms straighten under the chest, the head lifts.
                float push = ease(progress / part);
                this.rightArm.zRot = Mth.lerp(push, 1.4F, 0.15F);
                this.leftArm.zRot = Mth.lerp(push, -1.2F, -0.15F);
                this.rightArm.xRot = Mth.lerp(push, -0.15F, -1.45F);
                this.leftArm.xRot = Mth.lerp(push, 0.1F, -1.45F);
                this.rightLeg.zRot = Mth.lerp(push, 0.32F, 0.05F);
                this.leftLeg.zRot = Mth.lerp(push, -0.24F, -0.05F);
                this.rightLeg.xRot = 0.0F;
                this.leftLeg.xRot = 0.0F;
                this.head.yRot = Mth.lerp(push, 1.0F, 0.0F);
                this.head.xRot = Mth.lerp(push, 0.0F, -0.55F);
            } else {
                // Up in a crouch: a hand braced on a knee, the other hanging, head hung; straightening as they rise.
                float rise = ease((progress - part) / (1.0F - part));
                this.rightArm.xRot = Mth.lerp(rise, -0.75F, 0.0F);
                this.rightArm.zRot = Mth.lerp(rise, 0.25F, 0.05F);
                this.leftArm.xRot = Mth.lerp(rise, -0.25F, 0.0F);
                this.leftArm.zRot = Mth.lerp(rise, -0.1F, -0.05F);
                this.head.xRot = Mth.lerp(rise, 0.75F, 0.0F);
                this.head.yRot = 0.0F;
            }
        }
    }

    private static float ease(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
