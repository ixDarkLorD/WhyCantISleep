package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.hiding.Hiding;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A player hiding under a bed lies flat and still: arms and legs straight in line with the body, the head too but for
 * a small, limited look round (turned at most {@link #WHYCANTISLEEP$HEAD_TURN} either way, nodding a little up or down),
 * rather than not at all or right round like an owl's. The crawling pose tips the head up to look ahead and swings the
 * arms in a paddling stroke, which lifted the head out through the pillow and a hand up through the blanket. A player's
 * sleeves and trouser layers follow, as the player model copies them after this.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    private static final float WHYCANTISLEEP$HEAD_TURN = 90.0F;
    private static final float WHYCANTISLEEP$HEAD_UP = -12.0F;
    private static final float WHYCANTISLEEP$HEAD_DOWN = 18.0F;
    // The arms lie a hair out from the body's sides rather than inside them.
    private static final float WHYCANTISLEEP$ARM_SPREAD = 0.05F;

    @Shadow
    @Final
    public ModelPart head;
    @Shadow
    @Final
    public ModelPart hat;
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

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void whycantisleep$stillUnderTheBed(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                                 float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player) || Hiding.view(player) == null) return;
        // The head follows the look a little, from in line with the body.
        float yaw = Mth.clamp(netHeadYaw, -WHYCANTISLEEP$HEAD_TURN, WHYCANTISLEEP$HEAD_TURN);
        float pitch = Mth.clamp(headPitch, WHYCANTISLEEP$HEAD_UP, WHYCANTISLEEP$HEAD_DOWN);
        this.head.setRotation(pitch * Mth.DEG_TO_RAD, yaw * Mth.DEG_TO_RAD, 0.0F);
        this.hat.copyFrom(this.head);
        this.rightArm.setRotation(0.0F, 0.0F, WHYCANTISLEEP$ARM_SPREAD);
        this.leftArm.setRotation(0.0F, 0.0F, -WHYCANTISLEEP$ARM_SPREAD);
        this.rightLeg.setRotation(0.0F, 0.0F, 0.0F);
        this.leftLeg.setRotation(0.0F, 0.0F, 0.0F);
    }
}
