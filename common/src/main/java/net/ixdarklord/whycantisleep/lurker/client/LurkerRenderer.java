package net.ixdarklord.whycantisleep.lurker.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.lurker.LurkerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * Draws the thing under the bed: its skin, and over it its eyes, which shine in the dark like a spider's. A
 * hallucination is drawn see-through and flickering.
 */
public class LurkerRenderer extends MobRenderer<LurkerEntity, LurkerRenderState, LurkerModel> {
    private static final Identifier SKIN = WhyCantISleep.id("textures/entity/lurker/lurker.png");
    private static final RenderType EYES = RenderTypes.eyes(WhyCantISleep.id("textures/entity/lurker/lurker_eyes.png"));
    private static final float SCALE = 1.15F;

    public LurkerRenderer(EntityRendererProvider.Context context) {
        // The model is baked here: CoolCatLib: Core has no registry for model layers, and one model needs none.
        super(context, new LurkerModel(LurkerModel.createBodyLayer().bakeRoot()), 0.5F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    public void extractRenderState(LurkerEntity entity, LurkerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.attackAnim = entity.getAttackAnim(partialTicks);
        state.hallucination = entity.isHallucination();
    }

    @Override
    protected void scale(LurkerRenderState state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    protected @Nullable RenderType getRenderType(LurkerRenderState state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
        return state.hallucination && isBodyVisible ? RenderTypes.entityTranslucent(SKIN) : super.getRenderType(state, isBodyVisible, forceTransparent, appearGlowing);
    }

    @Override
    protected int getModelTint(LurkerRenderState state) {
        if (!state.hallucination) return super.getModelTint(state);
        // Mostly there, with a tremble, and now and then nearly gone for an instant.
        float flicker = 0.62F + Mth.sin(state.ageInTicks * 0.9F) * 0.08F - (Mth.sin(state.ageInTicks * 0.23F) > 0.93F ? 0.4F : 0.0F);
        return ARGB.white(Mth.clamp(flicker, 0.1F, 1.0F));
    }

    @Override
    public Identifier getTextureLocation(LurkerRenderState state) {
        return SKIN;
    }

    @Override
    public LurkerRenderState createRenderState() {
        return new LurkerRenderState();
    }
}
