package net.ixdarklord.whycantisleep.lurker.client;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.lurker.LurkerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Draws the thing under the bed: its skin, and over it its eyes, which shine in the dark like a spider's. */
public class LurkerRenderer extends MobRenderer<LurkerEntity, LivingEntityRenderState, LurkerModel> {
    private static final Identifier SKIN = WhyCantISleep.id("textures/entity/lurker/lurker.png");
    private static final RenderType EYES = RenderTypes.eyes(WhyCantISleep.id("textures/entity/lurker/lurker_eyes.png"));

    public LurkerRenderer(EntityRendererProvider.Context context) {
        // The model is baked here: CoolCatLib: Core has no registry for model layers, and one model needs none.
        super(context, new LurkerModel(LurkerModel.createBodyLayer().bakeRoot()), 0.45F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return SKIN;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }
}
