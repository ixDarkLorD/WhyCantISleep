package net.ixdarklord.whycantisleep.lurker.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Flicker: a wraith. It has no legs and touches nothing: a narrow ribbed torso hangs in the air, trailing a
 * spine that tapers to nothing where its legs should be, under a long skull crowned with two tall horns. Its arms are
 * too long for it, hang past where its knees would be and end in three claws each. It drifts rather than walks,
 * bobbing; hunting, it leans in and reaches for its player with both arms.
 * <p>
 * Its parts' places on the skin (64 by 64, drawn at twice that) are shared with {@code tools/gen_lurker_textures.ps1}.
 */
public class FlickerModel extends LurkerModel {
    private static final int SPINE_PARTS = 4;
    // It rises from further down than the others: it is tall.
    private static final float EMERGE_DEPTH = 26.0F;

    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] spine = new ModelPart[SPINE_PARTS];
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] forearms = new ModelPart[2];

    public FlickerModel(ModelPart root) {
        super(root, root.getChild("torso").getChild("head"));
        this.torso = root.getChild("torso");
        this.head = this.torso.getChild("head");
        this.jaw = this.head.getChild("jaw");
        ModelPart parent = this.torso;
        for (int i = 0; i < SPINE_PARTS; i++) {
            this.spine[i] = parent.getChild("spine");
            parent = this.spine[i];
        }
        this.arms[0] = this.torso.getChild("right_arm");
        this.arms[1] = this.torso.getChild("left_arm");
        for (int i = 0; i < 2; i++) this.forearms[i] = this.arms[i].getChild("forearm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The torso hangs with its foot 12 pixels off the ground; everything else hangs from it.
        PartDefinition torso = root.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -8.0F, -2.0F, 6.0F, 8.0F, 4.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        // Ribs standing out round its chest, and a blade of bone on each shoulder.
        torso.addOrReplaceChild("ribs", CubeListBuilder.create().texOffs(48, 20)
                .addBox(-4.0F, -7.0F, -2.5F, 1.0F, 1.0F, 5.0F).addBox(3.0F, -7.0F, -2.5F, 1.0F, 1.0F, 5.0F)
                .addBox(-4.0F, -5.0F, -2.5F, 1.0F, 1.0F, 5.0F).addBox(3.0F, -5.0F, -2.5F, 1.0F, 1.0F, 5.0F)
                .addBox(-4.0F, -3.0F, -2.5F, 1.0F, 1.0F, 5.0F).addBox(3.0F, -3.0F, -2.5F, 1.0F, 1.0F, 5.0F), PartPose.ZERO);
        torso.addOrReplaceChild("shoulder_blades", CubeListBuilder.create().texOffs(48, 12)
                .addBox(-4.5F, -11.0F, 0.0F, 1.0F, 4.0F, 2.0F).addBox(3.5F, -11.0F, 0.0F, 1.0F, 4.0F, 2.0F), PartPose.ZERO);
        // The spine that trails from it: lengths hinged each on the last, thinner as they go, ending in a wisp.
        PartDefinition spine = torso;
        for (int i = 0; i < SPINE_PARTS; i++) {
            boolean thin = i >= 2;
            spine = spine.addOrReplaceChild("spine", thin
                    ? CubeListBuilder.create().texOffs(8, 12).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F)
                    : CubeListBuilder.create().texOffs(0, 12).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offset(0.0F, i == 0 ? 0.0F : 4.0F, 0.0F));
            if (!thin) spine.addOrReplaceChild("vertebra", CubeListBuilder.create().texOffs(48, 12).addBox(-0.5F, 1.0F, 1.0F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);
        }

        // A long skull, its jaw hinged at the back, and two tall horns that sweep up and back.
        PartDefinition head = torso.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(20, 0).addBox(-2.5F, -6.0F, -4.0F, 5.0F, 6.0F, 6.0F), PartPose.offset(0.0F, -8.0F, 0.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 20).addBox(-2.0F, 0.0F, -5.5F, 4.0F, 2.0F, 5.0F), PartPose.offset(0.0F, -0.5F, 1.5F));
        horn(head, "right_horn", -1.8F, -0.25F);
        horn(head, "left_horn", 1.8F, 0.25F);
        eyes(head, 1.2F, -3.6F, -4.0F, 0, 46);

        arm(torso, "right_arm", -4.0F);
        arm(torso, "left_arm", 4.0F);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void horn(PartDefinition head, String name, float x, float roll) {
        PartDefinition horn = head.addOrReplaceChild(name, CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(x, -6.0F, 0.0F, -0.35F, 0.0F, roll));
        horn.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, -0.5F, 0.0F, 0.0F));
    }

    // An arm: hinged at the shoulder, again at the elbow, and three long claws for a hand.
    private static void arm(PartDefinition torso, String name, float x) {
        PartDefinition arm = torso.addOrReplaceChild(name, CubeListBuilder.create().texOffs(28, 20).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 9.0F, 2.0F),
                PartPose.offset(x, -7.0F, 0.0F));
        PartDefinition forearm = arm.addOrReplaceChild("forearm", CubeListBuilder.create().texOffs(36, 20).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 9.0F, 2.0F),
                PartPose.offset(0.0F, 9.0F, 0.0F));
        forearm.addOrReplaceChild("claws", CubeListBuilder.create().texOffs(44, 20)
                .addBox(-1.5F, 9.0F, -0.5F, 1.0F, 4.0F, 1.0F).addBox(-0.5F, 9.0F, -1.0F, 1.0F, 5.0F, 1.0F).addBox(0.5F, 9.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.ZERO);
    }

    @Override
    public void setupAnim(LurkerRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float hunt = state.huntAmount;
        float watch = 1.0F - hunt;
        float strike = Mth.sin(state.attackAnim * Mth.PI);
        float speed = state.walkAnimationSpeed;

        // It hangs in the air and bobs; hunting, it leans in towards its player and comes down a little.
        this.torso.y += Mth.sin(age * 0.09F) * 1.2F + hunt * 2.0F + sunk(state, EMERGE_DEPTH);
        this.torso.xRot = hunt * 0.35F + speed * 0.2F + Mth.sin(age * 0.07F) * 0.04F;
        this.torso.zRot = Mth.sin(age * 0.05F) * 0.05F;

        // The head keeps to where it is looking whatever the body does; watching, it tips to one side.
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD - this.torso.xRot - strike * 0.3F;
        this.head.zRot = watch * Mth.sin(age * 0.04F) * 0.3F + hunt * Mth.sin(age * 1.5F) * 0.06F;
        this.jaw.xRot = 0.08F + hunt * (0.4F + Mth.sin(age * 0.8F) * 0.1F) + strike * 0.7F;
        this.animateEyes(state);

        // The spine trails behind and sways, each length after the one above it.
        for (int i = 0; i < SPINE_PARTS; i++) {
            this.spine[i].xRot = 0.12F + speed * 0.3F + hunt * 0.1F + Mth.sin(age * 0.11F - i * 0.7F) * 0.14F;
            this.spine[i].zRot = Mth.sin(age * 0.08F - i * 0.9F) * 0.16F;
        }

        for (int i = 0; i < 2; i++) {
            float side = i == 0 ? -1.0F : 1.0F;
            // Watching, its arms hang, the hands turning slowly; hunting, they come up and reach, and rake as it strikes.
            float sway = Mth.sin(age * 0.06F + i * 2.1F);
            this.arms[i].xRot = watch * (0.08F + sway * 0.06F) + hunt * (-1.25F + Mth.sin(age * 0.5F + i * Mth.PI) * 0.18F) - strike * 0.6F;
            this.arms[i].zRot = side * (0.12F + watch * sway * 0.05F - hunt * 0.1F);
            this.forearms[i].xRot = -0.25F - hunt * 0.35F + strike * 0.5F;
        }
    }
}
