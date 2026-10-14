package net.ixdarklord.whycantisleep.lurker.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The Leech: a crawler. A blunt head, then five segments each smaller than the last, dragging a stinger; a fin of
 * bone on each segment's back; three pairs of little legs under the first three; and round its mouth four mandibles,
 * one to each side, that open like a flower and close on what it bites. It goes along the ground in a slither, its
 * legs scrabbling, and lifts its head when it hunts.
 * <p>
 * Its parts' places on the skin (64 by 64, drawn at twice that) are shared with {@code tools/gen_lurker_textures.ps1}.
 */
public class LeechModel extends LurkerModel {
    private static final int SEGMENTS = 5;
    private static final int LEGS = 6;
    private static final float EMERGE_DEPTH = 9.0F;
    // Each segment: its width and height, and where its skin is. All are 5 long.
    private static final float[][] SIZES = {{7.0F, 6.0F}, {6.0F, 5.0F}, {5.0F, 4.0F}, {4.0F, 3.0F}, {2.0F, 2.0F}};
    private static final int[][] SKINS = {{0, 10}, {0, 21}, {0, 31}, {24, 10}, {24, 18}};

    private final ModelPart head;
    private final ModelPart[] segments = new ModelPart[SEGMENTS];
    private final ModelPart[] legs = new ModelPart[LEGS];
    // Right, left, top, bottom.
    private final ModelPart[] mandibles = new ModelPart[4];

    public LeechModel(ModelPart root) {
        super(root, root.getChild("head"));
        this.head = root.getChild("head");
        String[] mandibles = {"right_mandible", "left_mandible", "top_mandible", "bottom_mandible"};
        for (int i = 0; i < mandibles.length; i++) this.mandibles[i] = this.head.getChild(mandibles[i]);
        ModelPart parent = root;
        for (int i = 0; i < SEGMENTS; i++) {
            this.segments[i] = parent.getChild("segment");
            parent = this.segments[i];
            if (i < LEGS / 2) {
                this.legs[i * 2] = parent.getChild("right_leg");
                this.legs[i * 2 + 1] = parent.getChild("left_leg");
            }
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The head, hinged where it meets the first segment, a pixel off the ground.
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -2.5F, -5.0F, 6.0F, 5.0F, 5.0F), PartPose.offset(0.0F, 20.5F, -5.0F));
        eyes(head, 1.6F, -1.2F, -5.0F, 0, 46);
        // Four mandibles round its mouth, each hinged at the rim and lying forward.
        mandible(head, "right_mandible", -2.6F, 0.8F);
        mandible(head, "left_mandible", 2.6F, 0.8F);
        mandible(head, "top_mandible", 0.0F, -0.6F);
        mandible(head, "bottom_mandible", 0.0F, 2.2F);

        // The segments, each hinged on the one before so the whole length can bend.
        PartDefinition segment = root;
        for (int i = 0; i < SEGMENTS; i++) {
            float width = SIZES[i][0];
            float height = SIZES[i][1];
            // Every segment's foot is at the same height, a pixel off the ground, however tall it is.
            float drop = i == 0 ? 0.0F : (SIZES[i - 1][1] - height) / 2.0F;
            segment = segment.addOrReplaceChild("segment",
                    CubeListBuilder.create().texOffs(SKINS[i][0], SKINS[i][1]).addBox(-width / 2.0F, -height / 2.0F, 0.0F, width, height, 5.0F),
                    i == 0 ? PartPose.offset(0.0F, 20.0F, -5.0F) : PartPose.offset(0.0F, drop, 5.0F));
            // A fin of bone on its back, shorter towards the tail.
            if (i < 4) {
                float fin = i < 2 ? 2.0F : 1.0F;
                segment.addOrReplaceChild("fin", CubeListBuilder.create().texOffs(32, 0).addBox(-0.5F, -height / 2.0F - fin, 1.5F, 1.0F, fin, 2.0F), PartPose.ZERO);
            }
            // A pair of legs under each of the first three, splayed out to either side.
            if (i < LEGS / 2) {
                float reach = height / 2.0F - 1.0F;
                segment.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(36, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                        PartPose.offsetAndRotation(-width / 2.0F, reach, 2.5F, 0.0F, 0.0F, 0.9F));
                segment.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(36, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                        PartPose.offsetAndRotation(width / 2.0F, reach, 2.5F, 0.0F, 0.0F, -0.9F));
            }
        }
        segment.addOrReplaceChild("stinger", CubeListBuilder.create().texOffs(24, 25).addBox(-0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 5.0F, 0.5F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void mandible(PartDefinition head, String name, float x, float y) {
        head.addOrReplaceChild(name, CubeListBuilder.create().texOffs(22, 0).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(x, y, -5.0F));
    }

    @Override
    public void setupAnim(LurkerRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float hunt = state.huntAmount;
        float watch = 1.0F - hunt;
        float strike = Mth.sin(state.attackAnim * Mth.PI);
        float sunk = sunk(state, EMERGE_DEPTH);
        float walk = state.walkAnimationPos;
        float speed = state.walkAnimationSpeed;

        // A wave runs down its length: slow and slight while it lies in wait, wide while it moves.
        float wave = walk * 0.9F + age * 0.07F;
        float bend = 0.1F + speed * 0.3F;
        for (int i = 0; i < SEGMENTS; i++) {
            this.segments[i].yRot = Mth.sin(wave - i * 0.9F) * bend * (i == 0 ? 0.5F : 1.0F);
            // Its tail end lifts a little, the stinger with it.
            this.segments[i].xRot = i >= 3 ? -0.12F - hunt * 0.15F + Mth.sin(age * 0.1F - i) * 0.05F : 0.0F;
        }
        this.segments[0].y += sunk;

        // Hunting, it lifts its head off the ground; striking, it lunges.
        this.head.y += sunk - hunt * 1.0F;
        this.head.z -= strike * 3.0F;
        this.head.yRot = Mth.clamp(state.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD + Mth.sin(wave + 0.9F) * bend * 0.5F;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD - hunt * 0.2F;
        this.head.zRot = watch * Mth.sin(age * 0.05F) * 0.12F;
        this.animateEyes(state);

        // The mandibles work open and shut, faster while it hunts, and gape wide as it strikes.
        float open = 0.2F + (0.5F + 0.5F * Mth.sin(age * (0.25F + hunt * 0.5F))) * 0.4F + strike * 0.6F;
        this.mandibles[0].yRot = open;
        this.mandibles[1].yRot = -open;
        this.mandibles[2].xRot = -open;
        this.mandibles[3].xRot = open;

        // Its legs scrabble, each pair a step behind the one in front, the two sides in turn.
        for (int i = 0; i < LEGS; i++) {
            float side = i % 2 == 0 ? 1.0F : -1.0F;
            float step = walk * 1.6F + (i / 2) * 1.3F + (i % 2) * Mth.PI;
            this.legs[i].xRot = Mth.cos(step) * 0.7F * speed;
            this.legs[i].zRot += side * Math.max(0.0F, Mth.sin(step)) * 0.3F * speed;
        }
    }
}
