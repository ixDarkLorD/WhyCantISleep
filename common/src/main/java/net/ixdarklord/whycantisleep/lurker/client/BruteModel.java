package net.ixdarklord.whycantisleep.lurker.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Brute: a hulk that walks on its knuckles. A barrel of a torso, plated and spiked down the back, on short thick
 * legs; arms as thick as its legs are long, ending in fists it leans on; and a small head set low between its
 * shoulders, with a heavy brow, two blunt horns and tusks rising from its jaw. It lumbers, swinging each arm with the
 * opposite leg, and when it strikes it rears and brings both fists down.
 * <p>
 * Its parts' places on the skin (128 by 128, drawn at twice that) are shared with
 * {@code tools/gen_lurker_textures.ps1}.
 */
public class BruteModel extends LurkerModel {
    private static final float EMERGE_DEPTH = 26.0F;

    private final ModelPart hips;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] forearms = new ModelPart[2];
    private final ModelPart[] legs = new ModelPart[2];

    public BruteModel(ModelPart root) {
        super(root, root.getChild("hips").getChild("torso").getChild("head"));
        this.hips = root.getChild("hips");
        this.torso = this.hips.getChild("torso");
        this.head = this.torso.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.arms[0] = this.torso.getChild("right_arm");
        this.arms[1] = this.torso.getChild("left_arm");
        for (int i = 0; i < 2; i++) this.forearms[i] = this.arms[i].getChild("forearm");
        this.legs[0] = root.getChild("right_leg");
        this.legs[1] = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The hips are the top of its legs, 7 pixels up; the torso turns on them at the waist.
        PartDefinition hips = root.addOrReplaceChild("hips",
                CubeListBuilder.create().texOffs(48, 0).addBox(-5.0F, -5.0F, -4.0F, 10.0F, 5.0F, 8.0F), PartPose.offset(0.0F, 17.0F, 1.0F));
        PartDefinition torso = hips.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -12.0F, -5.0F, 14.0F, 12.0F, 10.0F), PartPose.offset(0.0F, -5.0F, 0.0F));
        // Plates over its shoulders and down its back, and a row of spikes through them.
        torso.addOrReplaceChild("plates", CubeListBuilder.create().texOffs(84, 0)
                .addBox(-5.0F, -13.5F, -4.0F, 10.0F, 2.0F, 8.0F), PartPose.ZERO);
        torso.addOrReplaceChild("spikes", CubeListBuilder.create().texOffs(84, 12)
                .addBox(-1.0F, -17.0F, -3.0F, 2.0F, 4.0F, 2.0F)
                .addBox(-1.0F, -16.0F, 0.5F, 2.0F, 3.0F, 2.0F)
                .addBox(-1.0F, -10.0F, 5.0F, 2.0F, 2.0F, 3.0F)
                .addBox(-1.0F, -5.0F, 5.0F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
        torso.addOrReplaceChild("shoulders", CubeListBuilder.create().texOffs(0, 70)
                .addBox(-13.0F, -13.0F, -3.0F, 6.0F, 3.0F, 6.0F).addBox(7.0F, -13.0F, -3.0F, 6.0F, 3.0F, 6.0F), PartPose.ZERO);

        // A small head, low and forward between the shoulders.
        PartDefinition head = torso.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 24).addBox(-3.5F, -4.0F, -6.0F, 7.0F, 6.0F, 6.0F), PartPose.offset(0.0F, -8.0F, -5.0F));
        head.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(72, 24).addBox(-3.5F, -4.6F, -6.6F, 7.0F, 1.0F, 2.0F), PartPose.ZERO);
        head.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(56, 24).addBox(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offsetAndRotation(-3.5F, -3.5F, -2.0F, -0.5F, 0.9F, 0.0F));
        head.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(56, 24).addBox(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offsetAndRotation(3.5F, -3.5F, -2.0F, -0.5F, -0.9F, 0.0F));
        eyes(head, 1.6F, -1.8F, -6.0F, 0, 60);
        PartDefinition jaw = head.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(26, 24).addBox(-3.5F, 0.0F, -6.5F, 7.0F, 2.0F, 6.0F), PartPose.offset(0.0F, 2.0F, 0.0F));
        // Tusks rising from the front corners of the jaw, past its upper lip.
        jaw.addOrReplaceChild("tusks", CubeListBuilder.create().texOffs(52, 24)
                .addBox(-3.6F, -3.0F, -6.9F, 1.0F, 3.0F, 1.0F).addBox(2.6F, -3.0F, -6.9F, 1.0F, 3.0F, 1.0F), PartPose.ZERO);

        arm(torso, "right_arm", -9.5F);
        arm(torso, "left_arm", 9.5F);
        leg(root, "right_leg", -3.5F);
        leg(root, "left_leg", 3.5F);
        return LayerDefinition.create(mesh, 128, 128);
    }

    // An arm: upper arm, a thicker forearm, and a fist it puts its weight on.
    private static void arm(PartDefinition torso, String name, float x) {
        PartDefinition arm = torso.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 40).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 8.0F, 5.0F),
                PartPose.offset(x, -10.0F, 0.0F));
        PartDefinition forearm = arm.addOrReplaceChild("forearm", CubeListBuilder.create().texOffs(20, 40).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 9.0F, 6.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));
        forearm.addOrReplaceChild("fist", CubeListBuilder.create().texOffs(44, 40).addBox(-3.5F, 9.0F, -4.0F, 7.0F, 4.0F, 7.0F), PartPose.ZERO);
    }

    private static void leg(PartDefinition root, String name, float x) {
        PartDefinition leg = root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(72, 40).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 7.0F, 5.0F),
                PartPose.offset(x, 17.0F, 1.0F));
        leg.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(92, 40).addBox(-3.0F, 5.0F, -5.0F, 6.0F, 2.0F, 7.0F), PartPose.ZERO);
    }

    @Override
    public void setupAnim(LurkerRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float hunt = state.huntAmount;
        float watch = 1.0F - hunt;
        // The strike: it rears, both arms up, and brings them down.
        float strike = Mth.sin(state.attackAnim * Mth.PI);
        float sunk = sunk(state, EMERGE_DEPTH);
        float breath = Mth.sin(age * (0.07F + hunt * 0.08F));
        float walk = state.walkAnimationPos * 0.6F;
        float speed = state.walkAnimationSpeed;

        this.hips.y += sunk;
        this.hips.zRot = Mth.sin(walk) * 0.06F * speed;
        // It carries itself bent forward over its fists; rearing to strike, it straightens.
        this.torso.xRot = 0.32F + watch * 0.08F + breath * 0.02F - strike * 0.45F;
        this.torso.yRot = Mth.sin(walk) * 0.12F * speed;

        // The head is sunk too deep in its shoulders to turn far.
        this.head.yRot = Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD - this.torso.xRot + 0.1F;
        this.head.zRot = watch * Mth.sin(age * 0.035F) * 0.12F;
        this.jaw.xRot = 0.06F + Mth.sin(age * 0.15F) * 0.04F + hunt * 0.25F + strike * 0.6F;
        this.animateEyes(state);

        for (int i = 0; i < 2; i++) {
            float side = i == 0 ? -1.0F : 1.0F;
            // Each arm swings with the leg on the other side.
            float swing = Mth.cos(walk + (i == 0 ? 0.0F : Mth.PI)) * speed;
            this.legs[i].y += sunk;
            this.legs[i].xRot = -swing * 0.7F;
            // Undo the torso's lean so the arms hang to the ground, then swing, and throw them up to strike.
            this.arms[i].xRot = -this.torso.xRot + swing * 0.55F - strike * 2.2F;
            this.arms[i].zRot = side * -(0.1F + hunt * 0.12F + breath * 0.02F);
            this.forearms[i].xRot = -0.12F - Math.max(0.0F, swing) * 0.3F - strike * 0.5F;
        }
    }
}
