package net.ixdarklord.whycantisleep.lurker.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * What every kind of Lurker's model shares. The kinds are different creatures to look at ({@link StalkerModel},
 * {@link FlickerModel}, {@link BruteModel}, {@link LeechModel}), each with its own parts, skin and way of moving; what
 * they have in common is their eyes, and that they come up out of the ground.
 * <p>
 * An eye is three parts set in the face: the ball (which glows), a slit pupil standing just proud of it so it can
 * slide about, and a lid hinged along the top that comes down over both. The lid is as tall as the eye; how much of it
 * shows is its yScale. Every kind's skin keeps the three in the same arrangement, from a corner of its own choosing.
 */
public abstract class LurkerModel extends EntityModel<LurkerRenderState> {
    // Ticks it takes to rise out of the ground.
    private static final float EMERGE_TICKS = 24.0F;

    private final ModelPart[] pupils = new ModelPart[2];
    private final ModelPart[] lids = new ModelPart[2];

    protected LurkerModel(ModelPart root, ModelPart head) {
        super(root);
        String[] eyes = {"right_eye", "left_eye"};
        for (int i = 0; i < eyes.length; i++) {
            ModelPart eye = head.getChild(eyes[i]);
            this.pupils[i] = eye.getChild("pupil");
            this.lids[i] = eye.getChild("lid");
        }
    }

    /** Adds the two eyes to a head, each {@code spread} out from its middle, on its front face at {@code z}; their skin starts at (u, v). */
    protected static void eyes(PartDefinition head, float spread, float y, float z, int u, int v) {
        eye(head, "right_eye", -spread, y, z, u, v);
        eye(head, "left_eye", spread, y, z, u, v);
    }

    private static void eye(PartDefinition head, String name, float x, float y, float z, int u, int v) {
        PartDefinition eye = head.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(-1.0F, -1.0F, -0.35F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(x, y, z));
        eye.addOrReplaceChild("pupil", CubeListBuilder.create().texOffs(u + 8, v).addBox(-0.3F, -0.8F, -0.5F, 0.6F, 1.6F, 1.0F), PartPose.ZERO);
        eye.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(u + 14, v).addBox(-1.1F, 0.0F, -0.65F, 2.2F, 2.2F, 1.0F),
                PartPose.offset(0.0F, -1.1F, 0.0F));
    }

    /**
     * Its pupils slide towards what it is looking at, as far as the eye allows; its lids hang low while it watches,
     * draw right back while it hunts, and come down together when it blinks.
     */
    protected void animateEyes(LurkerRenderState state) {
        float hunt = state.huntAmount;
        float lid = Mth.clamp((1.0F - hunt) * 0.34F + hunt * 0.08F + state.blink, 0.08F, 1.0F);
        for (int i = 0; i < 2; i++) {
            // A positive turn of the head swings its face towards -x, and a positive tilt towards +y: so do these.
            this.pupils[i].x += -state.lookX * 0.65F;
            this.pupils[i].y += state.lookY * 0.3F + lid * 0.25F;
            // Narrow slits while it hunts.
            this.pupils[i].xScale = 1.0F - hunt * 0.35F;
            this.lids[i].yScale = lid;
        }
    }

    /** How far through rising out of the ground it is: 0 as it appears, 1 once it is out. */
    protected static float emerged(LurkerRenderState state) {
        return Mth.clamp(state.ageInTicks / EMERGE_TICKS, 0.0F, 1.0F);
    }

    /** How far under the ground it still is, in model pixels, starting {@code depth} down and easing up. */
    protected static float sunk(LurkerRenderState state, float depth) {
        float left = 1.0F - emerged(state);
        return left * left * depth;
    }
}
