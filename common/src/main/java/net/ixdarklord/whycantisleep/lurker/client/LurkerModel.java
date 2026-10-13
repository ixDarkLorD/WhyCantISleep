package net.ixdarklord.whycantisleep.lurker.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The thing under the bed: a ribbed chest and a thin belly slung low between four jointed, clawed limbs whose knees
 * stand above its back, like a spider's, a lashing tail, and a wide horned head with a jaw that drops open when it strikes. The parts' places on
 * the skin (laid out on 64 by 64, drawn at twice that) are shared with {@code tools/gen_lurker_textures.ps1}.
 */
public class LurkerModel extends EntityModel<LurkerRenderState> {
    // How far an upper limb leans out from straight up, and how far a lower one leans out from straight down.
    private static final float UPPER_LEAN = 0.8F;
    private static final float LOWER_LEAN = 0.15F;
    private static final int TAIL_PARTS = 3;

    private final ModelPart chest;
    private final ModelPart belly;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] upper = new ModelPart[4];
    private final ModelPart[] lower = new ModelPart[4];
    private final ModelPart[] tail = new ModelPart[TAIL_PARTS];

    public LurkerModel(ModelPart root) {
        super(root);
        this.chest = root.getChild("chest");
        this.belly = this.chest.getChild("belly");
        this.head = root.getChild("head");
        this.jaw = this.head.getChild("jaw");
        ModelPart tailParent = this.belly;
        for (int i = 0; i < TAIL_PARTS; i++) {
            this.tail[i] = tailParent.getChild("tail");
            tailParent = this.tail[i];
        }
        String[] limbs = {"right_arm", "left_arm", "right_leg", "left_leg"};
        for (int i = 0; i < limbs.length; i++) {
            this.upper[i] = root.getChild(limbs[i]);
            this.lower[i] = this.upper[i].getChild("lower");
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition chest = root.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(0, 19).addBox(-4.0F, -2.5F, -8.0F, 8.0F, 5.0F, 8.0F), PartPose.offset(0.0F, 15.0F, 1.0F));
        chest.addOrReplaceChild("belly",
                CubeListBuilder.create().texOffs(0, 32).addBox(-3.0F, -2.0F, 0.0F, 6.0F, 4.0F, 7.0F), PartPose.offset(0.0F, 0.2F, 0.0F));
        chest.addOrReplaceChild("spines", CubeListBuilder.create().texOffs(48, 0)
                .addBox(-1.0F, -4.5F, -7.0F, 2.0F, 2.0F, 2.0F)
                .addBox(-1.0F, -4.5F, -4.0F, 2.0F, 2.0F, 2.0F)
                .addBox(-1.0F, -4.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
        // Ribs standing out from its sides, three to a side.
        chest.addOrReplaceChild("ribs", CubeListBuilder.create().texOffs(52, 4)
                .addBox(-5.0F, -2.0F, -7.0F, 1.0F, 4.0F, 1.0F).addBox(4.0F, -2.0F, -7.0F, 1.0F, 4.0F, 1.0F)
                .addBox(-5.0F, -2.0F, -5.0F, 1.0F, 4.0F, 1.0F).addBox(4.0F, -2.0F, -5.0F, 1.0F, 4.0F, 1.0F)
                .addBox(-5.0F, -2.0F, -3.0F, 1.0F, 4.0F, 1.0F).addBox(4.0F, -2.0F, -3.0F, 1.0F, 4.0F, 1.0F), PartPose.ZERO);
        // A thin tail in three lengths, each hinged on the last.
        PartDefinition tail = chest.getChild("belly");
        for (int i = 0; i < TAIL_PARTS; i++) {
            tail = tail.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(28, 32)
                    .addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 5.0F), PartPose.offset(0.0F, i == 0 ? -0.5F : 0.0F, i == 0 ? 7.0F : 5.0F));
        }

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -3.0F, -6.0F, 7.0F, 5.0F, 6.0F), PartPose.offset(0.0F, 14.0F, -7.0F));
        // Two horns swept back over its skull.
        head.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(48, 10).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(-2.5F, -3.0F, -3.0F, 0.6F, -0.3F, 0.0F));
        head.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(48, 10).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(2.5F, -3.0F, -3.0F, 0.6F, 0.3F, 0.0F));
        // Hinged at the back of the head, so it drops open at the front.
        head.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 11).addBox(-3.5F, 0.0F, -6.0F, 7.0F, 2.0F, 6.0F), PartPose.offset(0.0F, 2.0F, 0.0F));

        // Each limb: an upper part rising from the body to a knee, and a lower part dropping from the knee to the ground.
        limb(root, "right_arm", -4.0F, -6.0F);
        limb(root, "left_arm", 4.0F, -6.0F);
        limb(root, "right_leg", -3.5F, 5.0F);
        limb(root, "left_leg", 3.5F, 5.0F);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void limb(PartDefinition root, String name, float x, float z) {
        PartDefinition upper = root.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(32, 0).addBox(-1.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F), PartPose.offset(x, 14.0F, z));
        PartDefinition lower = upper.addOrReplaceChild("lower",
                CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 15.0F, 2.0F), PartPose.offset(0.0F, -7.0F, 0.0F));
        // Three claws spread forward from each foot.
        lower.addOrReplaceChild("claws", CubeListBuilder.create().texOffs(48, 4)
                .addBox(-1.5F, 14.0F, -3.5F, 1.0F, 1.0F, 3.0F).addBox(-0.5F, 14.0F, -4.0F, 1.0F, 1.0F, 3.0F).addBox(0.5F, 14.0F, -3.5F, 1.0F, 1.0F, 3.0F),
                PartPose.ZERO);
    }

    @Override
    public void setupAnim(LurkerRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        // The strike: the head darts forward and the jaw drops, then both come back.
        float strike = Mth.sin(state.attackAnim * Mth.PI);

        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD - strike * 0.35F;
        // Its head never quite holds still.
        this.head.zRot = Mth.sin(age * 0.35F) * 0.07F + Mth.sin(age * 0.11F) * 0.05F;
        this.head.z -= strike * 3.0F;
        this.jaw.xRot = 0.12F + Mth.sin(age * 0.2F) * 0.06F + strike * 0.9F;

        // It breathes, and its belly sways behind it as it goes.
        float breath = Mth.sin(age * 0.1F);
        this.chest.y += breath * 0.4F;
        this.belly.xRot = -0.14F + breath * 0.04F;
        this.belly.yRot = Mth.cos(state.walkAnimationPos * 0.9F) * 0.25F * state.walkAnimationSpeed;

        // The tail curls and lashes, each length a little behind the one before.
        for (int i = 0; i < TAIL_PARTS; i++) {
            this.tail[i].yRot = Mth.sin(age * 0.18F - i * 0.8F) * 0.3F;
            this.tail[i].xRot = 0.18F + Mth.sin(age * 0.09F - i * 0.6F) * 0.12F;
        }

        // A scuttle: the limbs of each diagonal move together, each knee bending as its foot lifts.
        for (int i = 0; i < 4; i++) {
            boolean right = i % 2 == 0;
            boolean front = i < 2;
            float side = right ? -1.0F : 1.0F;
            float phase = right == front ? 0.0F : Mth.PI;
            float swing = Mth.cos(state.walkAnimationPos * 0.9F + phase) * state.walkAnimationSpeed;
            float lift = Math.max(0.0F, Mth.sin(state.walkAnimationPos * 0.9F + phase)) * state.walkAnimationSpeed;
            ModelPart upper = this.upper[i];
            ModelPart lower = this.lower[i];
            upper.zRot = side * (UPPER_LEAN + lift * 0.25F);
            upper.xRot = swing * 0.6F + (front ? -0.15F : 0.2F);
            upper.yRot = side * (front ? 0.25F : -0.35F);
            // Undo the upper part's lean so the lower one hangs down, a little outward.
            lower.zRot = -upper.zRot + side * LOWER_LEAN;
            lower.xRot = -upper.xRot * 0.8F;
            upper.y += breath * 0.4F;
        }
    }
}
