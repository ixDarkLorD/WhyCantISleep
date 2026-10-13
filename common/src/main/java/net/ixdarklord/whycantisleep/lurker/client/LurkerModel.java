package net.ixdarklord.whycantisleep.lurker.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * The thing under the bed: a long, low body on four thin limbs splayed like a spider's, a head too large for it, and a
 * ridge down its back. The parts' places on the 64 by 64 skin are shared with {@code tools/gen_lurker_textures.ps1}.
 */
public class LurkerModel extends EntityModel<LivingEntityRenderState> {
    // How far each limb leans out from the body, in radians.
    private static final float SPLAY = 0.3F;

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public LurkerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 14).addBox(-4.0F, -2.5F, -7.0F, 8.0F, 5.0F, 14.0F), PartPose.offset(0.0F, 14.5F, 0.0F));
        body.addOrReplaceChild("spines", CubeListBuilder.create().texOffs(10, 34)
                .addBox(-1.0F, -4.5F, -5.0F, 2.0F, 2.0F, 2.0F)
                .addBox(-1.0F, -4.5F, -1.0F, 2.0F, 2.0F, 2.0F)
                .addBox(-1.0F, -4.5F, 3.0F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -3.5F, -6.0F, 7.0F, 7.0F, 6.0F), PartPose.offset(0.0F, 13.0F, -7.0F));
        // Every limb wears the same patch of skin.
        CubeListBuilder limb = CubeListBuilder.create().texOffs(0, 34).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F);
        root.addOrReplaceChild("right_arm", limb, PartPose.offset(-4.5F, 13.0F, -5.0F));
        root.addOrReplaceChild("left_arm", limb, PartPose.offset(4.5F, 13.0F, -5.0F));
        root.addOrReplaceChild("right_leg", limb, PartPose.offset(-4.5F, 13.0F, 5.0F));
        root.addOrReplaceChild("left_leg", limb, PartPose.offset(4.5F, 13.0F, 5.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        // Its head never quite holds still.
        this.head.zRot = Mth.sin(state.ageInTicks * 0.35F) * 0.06F;
        this.body.y += Mth.sin(state.ageInTicks * 0.1F) * 0.4F;

        // A scuttle: the limbs of each diagonal move together.
        float swing = Mth.cos(state.walkAnimationPos * 0.9F) * 0.9F * state.walkAnimationSpeed;
        this.rightArm.xRot = swing;
        this.leftArm.xRot = -swing;
        this.rightLeg.xRot = -swing;
        this.leftLeg.xRot = swing;
        this.rightArm.zRot = SPLAY;
        this.rightLeg.zRot = SPLAY;
        this.leftArm.zRot = -SPLAY;
        this.leftLeg.zRot = -SPLAY;
    }
}
