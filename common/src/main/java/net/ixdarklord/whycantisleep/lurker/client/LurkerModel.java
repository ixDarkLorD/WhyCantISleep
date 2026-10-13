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
 * The thing under the bed: a ribbed chest under a plate of armor and a thin belly slung low between four jointed,
 * clawed limbs whose knuckled, spurred knees stand above its back, like a spider's, a lashing bladed tail, and a wide head with swept-back horns, a jaw
 * that drops open when it strikes under a heavy brow and two fangs, and two eyes that follow what it is looking at
 * and blink. The parts' places on the skin (laid out on 64 by 64, drawn at twice that) are shared
 * with {@code tools/gen_lurker_textures.ps1}.
 * <p>
 * It has three ways of holding itself, eased from one to the next: rising out of the ground as it appears, crouched
 * low and nearly still while it watches, and reared up and scuttling while it hunts.
 */
public class LurkerModel extends EntityModel<LurkerRenderState> {
    // How far an upper limb leans out from straight up, and how far a lower one leans out from straight down.
    private static final float UPPER_LEAN = 0.8F;
    private static final float LOWER_LEAN = 0.15F;
    private static final int TAIL_PARTS = 4;
    // Ticks it takes to rise out of the ground, and how far under it starts, in model pixels.
    private static final float EMERGE_TICKS = 24.0F;
    private static final float EMERGE_DEPTH = 15.0F;

    private final ModelPart chest;
    private final ModelPart belly;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] upper = new ModelPart[4];
    private final ModelPart[] lower = new ModelPart[4];
    private final ModelPart[] tail = new ModelPart[TAIL_PARTS];
    private final ModelPart[] pupils = new ModelPart[2];
    private final ModelPart[] lids = new ModelPart[2];

    public LurkerModel(ModelPart root) {
        super(root);
        this.chest = root.getChild("chest");
        this.belly = this.chest.getChild("belly");
        this.head = root.getChild("head");
        this.jaw = this.head.getChild("jaw");
        String[] eyes = {"right_eye", "left_eye"};
        for (int i = 0; i < eyes.length; i++) {
            ModelPart eye = this.head.getChild(eyes[i]);
            this.pupils[i] = eye.getChild("pupil");
            this.lids[i] = eye.getChild("lid");
        }
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
        PartDefinition belly = chest.addOrReplaceChild("belly",
                CubeListBuilder.create().texOffs(0, 32).addBox(-3.0F, -2.0F, 0.0F, 6.0F, 4.0F, 7.0F), PartPose.offset(0.0F, 0.2F, 0.0F));
        // A plate of armor over its shoulders, standing a little proud of the chest all round.
        chest.addOrReplaceChild("carapace", CubeListBuilder.create().texOffs(0, 52).addBox(-4.5F, -3.5F, -7.5F, 9.0F, 1.0F, 7.0F), PartPose.ZERO);
        // A ridge of bone down its back, taller over the shoulders.
        chest.addOrReplaceChild("spines", CubeListBuilder.create().texOffs(48, 0)
                .addBox(-1.0F, -5.5F, -7.0F, 2.0F, 3.0F, 2.0F)
                .addBox(-1.0F, -4.5F, -4.0F, 2.0F, 2.0F, 2.0F)
                .addBox(-1.0F, -4.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
        belly.addOrReplaceChild("spines", CubeListBuilder.create().texOffs(48, 0)
                .addBox(-1.0F, -3.5F, 1.5F, 2.0F, 2.0F, 2.0F)
                .addBox(-0.5F, -3.0F, 4.5F, 1.0F, 1.0F, 2.0F), PartPose.ZERO);
        // Ribs standing out from its sides, three to a side.
        chest.addOrReplaceChild("ribs", CubeListBuilder.create().texOffs(56, 0)
                .addBox(-5.0F, -2.0F, -7.0F, 1.0F, 4.0F, 1.0F).addBox(4.0F, -2.0F, -7.0F, 1.0F, 4.0F, 1.0F)
                .addBox(-5.0F, -2.0F, -5.0F, 1.0F, 4.0F, 1.0F).addBox(4.0F, -2.0F, -5.0F, 1.0F, 4.0F, 1.0F)
                .addBox(-5.0F, -2.0F, -3.0F, 1.0F, 4.0F, 1.0F).addBox(4.0F, -2.0F, -3.0F, 1.0F, 4.0F, 1.0F), PartPose.ZERO);
        // A thin tail in lengths, each hinged on the last, ending in a blade of bone.
        PartDefinition tail = belly;
        for (int i = 0; i < TAIL_PARTS; i++) {
            tail = tail.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(28, 32)
                    .addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 5.0F), PartPose.offset(0.0F, i == 0 ? -0.5F : 0.0F, i == 0 ? 7.0F : 5.0F));
            // A spike of bone on each length, smaller towards the tip.
            float spike = i < 2 ? 2.0F : 1.0F;
            tail.addOrReplaceChild("spike", CubeListBuilder.create().texOffs(48, 0).addBox(-0.5F, -1.0F - spike, 1.5F, 1.0F, spike, 2.0F), PartPose.ZERO);
        }
        tail.addOrReplaceChild("blade", CubeListBuilder.create().texOffs(34, 20)
                .addBox(-0.5F, -2.5F, 4.0F, 1.0F, 5.0F, 4.0F), PartPose.ZERO);

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -3.0F, -6.0F, 7.0F, 5.0F, 6.0F), PartPose.offset(0.0F, 14.0F, -7.0F));
        // Two horns swept back over its skull, each in two lengths so they curve.
        horn(head, "right_horn", -2.5F, -0.3F);
        horn(head, "left_horn", 2.5F, 0.3F);
        // A heavy brow over its eyes, so they look out from under it, and a fang hanging at each corner of its mouth.
        head.addOrReplaceChild("brow", CubeListBuilder.create().texOffs(34, 44).addBox(-3.5F, -3.6F, -6.6F, 7.0F, 1.0F, 2.0F), PartPose.ZERO);
        head.addOrReplaceChild("fangs", CubeListBuilder.create().texOffs(34, 48)
                .addBox(-3.1F, 1.5F, -6.3F, 0.7F, 1.8F, 0.7F).addBox(2.4F, 1.5F, -6.3F, 0.7F, 1.8F, 0.7F), PartPose.ZERO);
        eye(head, "right_eye", -1.5F);
        eye(head, "left_eye", 1.5F);
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

    private static void horn(PartDefinition head, String name, float x, float yaw) {
        PartDefinition horn = head.addOrReplaceChild(name, CubeListBuilder.create().texOffs(34, 20).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(x, -3.0F, -3.0F, 0.6F, yaw, 0.0F));
        horn.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(34, 20).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, 0.55F, 0.0F, 0.0F));
    }

    // An eye, set in its socket on the face: the ball (which glows), a slit pupil standing just proud of it so it can
    // slide about, and a lid hinged along the top that comes down over both. The lid is as tall as the eye; how much
    // of it shows is its yScale.
    private static void eye(PartDefinition head, String name, float x) {
        PartDefinition eye = head.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 46).addBox(-1.0F, -1.0F, -0.35F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(x, -1.0F, -6.0F));
        eye.addOrReplaceChild("pupil", CubeListBuilder.create().texOffs(8, 46).addBox(-0.3F, -0.8F, -0.5F, 0.6F, 1.6F, 1.0F), PartPose.ZERO);
        eye.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(14, 46).addBox(-1.1F, 0.0F, -0.65F, 2.2F, 2.2F, 1.0F),
                PartPose.offset(0.0F, -1.1F, 0.0F));
    }

    private static void limb(PartDefinition root, String name, float x, float z) {
        PartDefinition upper = root.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(32, 0).addBox(-1.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F), PartPose.offset(x, 14.0F, z));
        // A spur of bone standing off each knee.
        upper.addOrReplaceChild("spur", CubeListBuilder.create().texOffs(56, 6).addBox(-0.5F, -10.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.ZERO);
        PartDefinition lower = upper.addOrReplaceChild("lower",
                CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 15.0F, 2.0F), PartPose.offset(0.0F, -7.0F, 0.0F));
        // The knee: a knuckle of armor the two parts hinge in, cracked and lit from inside.
        lower.addOrReplaceChild("joint", CubeListBuilder.create().texOffs(40, 50).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.ZERO);
        // Three claws spread forward from each foot.
        lower.addOrReplaceChild("claws", CubeListBuilder.create().texOffs(56, 6)
                .addBox(-1.5F, 14.0F, -3.5F, 1.0F, 1.0F, 3.0F).addBox(-0.5F, 14.0F, -4.0F, 1.0F, 1.0F, 3.0F).addBox(0.5F, 14.0F, -3.5F, 1.0F, 1.0F, 3.0F),
                PartPose.ZERO);
    }

    @Override
    public void setupAnim(LurkerRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        // 0 while it watches, 1 while it hunts, eased between by the renderer.
        float hunt = state.huntAmount;
        float watch = 1.0F - hunt;
        // The strike: the head darts forward and the jaw drops, then both come back.
        float strike = Mth.sin(state.attackAnim * Mth.PI);
        // Rising out of the ground: everything starts sunk and comes up, limbs first reaching, body after.
        float emerge = Mth.clamp(age / EMERGE_TICKS, 0.0F, 1.0F);
        float risen = 1.0F - (1.0F - emerge) * (1.0F - emerge);
        float sunk = (1.0F - risen) * EMERGE_DEPTH;
        float breath = Mth.sin(age * (0.1F + hunt * 0.12F));
        // Watching it crouches, belly near the ground; hunting it rears up on its limbs.
        float crouch = watch * 3.2F - hunt * 0.8F;
        float walk = state.walkAnimationPos * 0.9F;
        float speed = state.walkAnimationSpeed;

        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD - strike * 0.35F - hunt * 0.12F;
        // Watching, its head tips slowly to one side and back, like something listening; hunting, it shudders.
        this.head.zRot = watch * (Mth.sin(age * 0.045F) * 0.38F + Mth.sin(age * 0.35F) * 0.03F) + hunt * Mth.sin(age * 1.3F) * 0.07F;
        this.head.y += crouch + sunk + breath * 0.3F;
        this.head.z -= strike * 3.0F + hunt * 1.0F;
        // Its jaw hangs a little open, works as it breathes, gapes while it hunts and drops wide as it strikes.
        this.jaw.xRot = 0.1F + Mth.sin(age * 0.2F) * 0.06F + hunt * (0.35F + Mth.sin(age * 0.9F) * 0.12F) + strike * 0.75F;

        // Its pupils slide towards what it is looking at, as far as the eye allows; its lids hang low while it
        // watches, draw right back while it hunts, and come down together when it blinks.
        float lid = Mth.clamp(watch * 0.34F + hunt * 0.08F + state.blink, 0.08F, 1.0F);
        for (int i = 0; i < 2; i++) {
            // A positive turn of the head swings its face towards -x, and a positive tilt towards +y: so do these.
            this.pupils[i].x += -state.lookX * 0.65F;
            this.pupils[i].y += state.lookY * 0.3F + lid * 0.25F;
            // Narrow slits while it hunts.
            this.pupils[i].xScale = 1.0F - hunt * 0.35F;
            this.lids[i].yScale = lid;
        }

        this.chest.y += crouch + sunk + breath * 0.4F;
        // Hunting, its shoulders roll with each stride.
        this.chest.zRot = Mth.sin(walk) * 0.09F * speed * hunt;
        this.chest.xRot = -hunt * 0.1F + watch * 0.06F;
        this.belly.xRot = -0.14F + breath * 0.04F - watch * 0.1F;
        this.belly.yRot = Mth.cos(walk) * 0.25F * speed;

        // The tail: a slow curl while it watches, a lash while it hunts, each length a little behind the one before.
        float lash = 0.18F + hunt * 0.5F;
        for (int i = 0; i < TAIL_PARTS; i++) {
            this.tail[i].yRot = Mth.sin(age * lash - i * 0.8F) * (0.22F + hunt * 0.25F);
            this.tail[i].xRot = 0.16F + hunt * 0.14F + Mth.sin(age * 0.09F - i * 0.6F) * 0.12F;
        }

        for (int i = 0; i < 4; i++) {
            boolean right = i % 2 == 0;
            boolean front = i < 2;
            float side = right ? -1.0F : 1.0F;
            // A scuttle: the limbs of each diagonal move together, each knee bending as its foot lifts.
            float phase = right == front ? 0.0F : Mth.PI;
            float swing = Mth.cos(walk + phase) * speed;
            float lift = Math.max(0.0F, Mth.sin(walk + phase)) * speed;
            // Standing still, one foot at a time lifts and taps, now and then.
            float tap = Math.max(0.0F, Mth.sin(age * 0.23F + i * 1.9F) - 0.86F) * 7.0F * watch * (1.0F - Math.min(1.0F, speed * 4.0F));
            // Coming up out of the ground its limbs reach high and wide before they take its weight.
            float reach = (1.0F - emerge) * 0.9F;
            ModelPart upper = this.upper[i];
            ModelPart lower = this.lower[i];
            // Crouched, the knees fold out further so the body can sink between them.
            upper.zRot = side * (UPPER_LEAN + lift * 0.25F + watch * 0.28F + tap * 0.12F - reach * 0.5F);
            upper.xRot = swing * (0.6F + hunt * 0.25F) + (front ? -0.15F - hunt * 0.2F : 0.2F) - reach * (front ? 0.6F : -0.3F);
            upper.yRot = side * (front ? 0.25F : -0.35F);
            // Undo the upper part's lean so the lower one hangs down, a little outward.
            lower.zRot = -upper.zRot + side * (LOWER_LEAN + tap * 0.2F);
            lower.xRot = -upper.xRot * 0.8F - tap * 0.25F;
            upper.y += crouch * 0.85F + sunk + breath * 0.4F;
        }
    }
}
