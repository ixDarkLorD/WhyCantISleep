package net.ixdarklord.whycantisleep.lurker.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.lurker.LurkerEntity;
import net.ixdarklord.whycantisleep.lurker.LurkerKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Draws the thing under the bed: its skin, and over it what glows on it, its eyes above all, which shine in the dark
 * and pulse like embers breathed on. It also works out where its pupils point and when it blinks. A hallucination is
 * drawn see-through and flickering.
 */
public class LurkerRenderer extends MobRenderer<LurkerEntity, LurkerRenderState, LurkerModel> {
    // Each kind's skin, and what glows on it (tools/gen_lurker_textures.ps1 draws a pair for each).
    private static final Map<LurkerKind, Identifier> SKINS = new EnumMap<>(LurkerKind.class);
    private static final Map<LurkerKind, RenderType> GLOWS = new EnumMap<>(LurkerKind.class);

    static {
        for (LurkerKind kind : LurkerKind.values()) {
            SKINS.put(kind, WhyCantISleep.id("textures/entity/lurker/" + kind.key() + ".png"));
            GLOWS.put(kind, RenderTypes.eyes(WhyCantISleep.id("textures/entity/lurker/" + kind.key() + "_eyes.png")));
        }
    }

    private static final float SCALE = 1.15F;
    // How fast its glow rises and falls, in radians a tick: a slow breath, and a racing pulse.
    private static final float PULSE_WATCHING = 0.12F;
    private static final float PULSE_HUNTING = 0.42F;
    // How far off straight ahead, in degrees, puts a pupil at the edge of its eye.
    private static final float LOOK_RANGE = 35.0F;
    // A blink takes this many ticks to close and open again, and comes at two unequal intervals so it never settles
    // into a rhythm.
    private static final float BLINK_TICKS = 7.0F;
    private static final float[] BLINK_EVERY = {83.0F, 131.0F};

    // Each kind is a different creature to look at, with a model of its own.
    private final Map<LurkerKind, LurkerModel> models = new EnumMap<>(LurkerKind.class);

    public LurkerRenderer(EntityRendererProvider.Context context) {
        // The models are baked here: CoolCatLib: Core has no registry for model layers, and these need none.
        super(context, new StalkerModel(StalkerModel.createBodyLayer().bakeRoot()), 0.5F);
        this.models.put(LurkerKind.STALKER, this.model);
        this.models.put(LurkerKind.FLICKER, new FlickerModel(FlickerModel.createBodyLayer().bakeRoot()));
        this.models.put(LurkerKind.BRUTE, new BruteModel(BruteModel.createBodyLayer().bakeRoot()));
        this.models.put(LurkerKind.LEECH, new LeechModel(LeechModel.createBodyLayer().bakeRoot()));
        // The game's own eyes layer, but with a brightness that rises and falls: slowly while it watches, quick as a
        // pulse while it hunts.
        this.addLayer(new RenderLayer<>(this) {
            @Override
            public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, LurkerRenderState state, float yRot, float xRot) {
                float beat = Mth.sin(state.ageInTicks * (PULSE_WATCHING + (PULSE_HUNTING - PULSE_WATCHING) * state.huntAmount));
                int glow = Math.round(255.0F * (0.72F + 0.28F * beat));
                collector.order(1).submitModel(this.getParentModel(), state, poseStack, GLOWS.get(state.kind), lightCoords, OverlayTexture.NO_OVERLAY, ARGB.color(255, glow, glow, glow),
                        null, state.outlineColor, null);
            }
        });
    }

    @Override
    public void extractRenderState(LurkerEntity entity, LurkerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.attackAnim = entity.getAttackAnim(partialTicks);
        state.hallucination = entity.isHallucination();
        state.huntAmount = entity.huntAmount(partialTicks);
        state.kind = entity.kind();

        // Its pupils turn to whoever is looking at it, ahead of its head, which follows more slowly.
        float lookX = 0.0F;
        float lookY = 0.0F;
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            Vec3 to = player.getEyePosition(partialTicks).subtract(entity.getEyePosition(partialTicks));
            float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F;
            float pitch = (float) -(Mth.atan2(to.y, to.horizontalDistance()) * Mth.RAD_TO_DEG);
            lookX = Mth.clamp(Mth.wrapDegrees(yaw - Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot)) / LOOK_RANGE, -1.0F, 1.0F);
            lookY = Mth.clamp((pitch - entity.getXRot(partialTicks)) / LOOK_RANGE, -1.0F, 1.0F);
        }
        // Watching, they also dart: a small jump to somewhere else a few times a second.
        float dart = (float) Math.floor(state.ageInTicks / 9.0F) + entity.getId();
        float still = 1.0F - state.huntAmount;
        state.lookX = Mth.clamp(lookX + Mth.sin(dart * 12.9898F) * 0.3F * still, -1.0F, 1.0F);
        state.lookY = Mth.clamp(lookY + Mth.sin(dart * 78.233F) * 0.2F * still, -1.0F, 1.0F);

        float blink = 0.0F;
        for (float every : BLINK_EVERY) {
            float into = (state.ageInTicks + entity.getId() * 17) % every;
            if (into < BLINK_TICKS) blink = Math.max(blink, Mth.sin(into / BLINK_TICKS * Mth.PI));
        }
        state.blink = blink;
    }

    /** Draws it with its own kind's model (the layers draw with whichever model is current, so they follow). */
    @Override
    public void submit(LurkerRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        this.model = this.models.get(state.kind);
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    protected void scale(LurkerRenderState state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    protected @Nullable RenderType getRenderType(LurkerRenderState state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
        // A hallucination is not quite there, and neither is a Flicker.
        boolean thin = state.hallucination || state.kind == LurkerKind.FLICKER;
        return thin && isBodyVisible ? RenderTypes.entityTranslucent(SKINS.get(state.kind)) : super.getRenderType(state, isBodyVisible, forceTransparent, appearGlowing);
    }

    @Override
    protected int getModelTint(LurkerRenderState state) {
        // A Flicker wavers, without ever quite going.
        if (!state.hallucination && state.kind == LurkerKind.FLICKER) return ARGB.white(0.7F + Mth.sin(state.ageInTicks * 0.6F) * 0.12F);
        if (!state.hallucination) return super.getModelTint(state);
        // Mostly there, with a tremble, and now and then nearly gone for an instant.
        float flicker = 0.62F + Mth.sin(state.ageInTicks * 0.9F) * 0.08F - (Mth.sin(state.ageInTicks * 0.23F) > 0.93F ? 0.4F : 0.0F);
        return ARGB.white(Mth.clamp(flicker, 0.1F, 1.0F));
    }

    @Override
    public Identifier getTextureLocation(LurkerRenderState state) {
        return SKINS.get(state.kind);
    }

    @Override
    public LurkerRenderState createRenderState() {
        return new LurkerRenderState();
    }
}
