package net.ixdarklord.whycantisleep.between.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ixdarklord.coolcatcore.api.client.registry.EntityRendererRegistry;
import net.ixdarklord.whycantisleep.between.mob.HauntedUrnEntity;
import net.ixdarklord.whycantisleep.lurker.WCISEntities;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * The haunted urn, drawn as the very urn it was (its block's own model): rising out of where it stood, turning slowly
 * as it floats, bobbing and tilting as though something inside it stirs, shuddering and leaning back as it charges a
 * volley, jolting when struck, a violet-red glow in its mouth (FlameBloomClient) that flares as it charges; and its
 * shards, drawn as their item.
 */
public final class HauntedUrnClient {
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();

    private HauntedUrnClient() {
    }

    public static void init() {
        EntityRendererRegistry.register(WCISEntities.HAUNTED_URN, Renderer::new);
        EntityRendererRegistry.register(WCISEntities.URN_SHARD, ThrownItemRenderer::new);
    }

    // The haunting in its mouth: a violet-red glow, flaring as it charges.
    private static final int[] HAUNT = {0x24A0306A, 0x48C0407A, 0x66E0608A, 0x88FFB0C8};

    static final class State extends EntityRenderState {
        final BlockModelRenderState urn = new BlockModelRenderState();
        float spin, bob, tilt, rise, shudder, lean, jolt;
    }

    static final class Renderer extends EntityRenderer<HauntedUrnEntity, State> {
        private final BlockModelResolver models;

        Renderer(EntityRendererProvider.Context context) {
            super(context);
            this.models = context.getBlockModelResolver();
        }

        @Override
        public State createRenderState() {
            return new State();
        }

        @Override
        public void extractRenderState(HauntedUrnEntity urn, State state, float partialTicks) {
            super.extractRenderState(urn, state, partialTicks);
            this.models.update(state.urn, urn.getUrn(), DISPLAY);
            float t = urn.tickCount + partialTicks;
            state.spin = t * 2.5F;
            state.bob = Mth.sin(t * 0.12F) * 0.08F;
            state.tilt = Mth.sin(t * 0.21F) * 6.0F;
            // Rising out of where it stood over its first second.
            state.rise = Mth.clamp(t / 20.0F, 0.0F, 1.0F);
            // Its charge: a shudder growing as it runs down, and a lean back, then the throw.
            float charge = urn.getCharge() > 0 ? 1.0F - (urn.getCharge() - partialTicks) / HauntedUrnEntity.CHARGE_TICKS : 0.0F;
            state.shudder = charge > 0 ? Mth.sin(t * 3.1F) * 7.0F * charge : 0.0F;
            state.lean = -18.0F * charge;
            state.jolt = urn.hurtTime > 0 ? Mth.sin((urn.hurtTime - partialTicks) * 1.6F) * 12.0F * urn.hurtTime / 10.0F : 0.0F;
            // The glow in its mouth, flaring as it charges.
            float flare = 0.8F + 0.6F * charge;
            FlameBloomClient.glow(urn.getPosition(partialTicks).add(0.0, urn.getBbHeight() * 0.9 + state.bob, 0.0), flare, HAUNT);
        }

        @Override
        public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
            poseStack.pushPose();
            poseStack.translate(0.0F, (0.1F + state.bob) * state.rise, 0.0F);           // lifting off from where it stood
            poseStack.mulPose(Axis.YP.rotationDegrees(state.spin + state.shudder));
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.tilt + state.jolt));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.lean));
            poseStack.translate(-0.5F, 0.0F, -0.5F);
            state.urn.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
            super.submit(state, poseStack, collector, camera);
        }
    }
}
