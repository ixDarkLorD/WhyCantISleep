package net.ixdarklord.whycantisleep.between.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ixdarklord.coolcatcore.api.client.registry.BlockEntityRendererRegistry;
import net.ixdarklord.whycantisleep.between.PlanterBlockEntity;
import net.ixdarklord.whycantisleep.between.WCISBlocks;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The flowers in a stone planter ({@link PlanterBlockEntity}), drawn from their own block models, small, standing in its
 * soil each at its own place and turned its own way (a tall flower with its top half over it); the planter's own model
 * is drawn by the chunk as any block's.
 */
public final class PlanterClient {
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();
    /** A flower's size in the tray, and the height of the soil it stands in. */
    private static final float SCALE = 0.55F, SOIL = 6.5F / 16.0F;

    private PlanterClient() {
    }

    public static void init() {
        BlockEntityRendererRegistry.register(WCISBlocks.PLANTER_ENTITY, Renderer::new);
    }

    static final class State extends BlockEntityRenderState {
        final List<Planted> planted = new ArrayList<>();
        int count;
        float turn;
    }

    static final class Planted {
        final BlockModelRenderState lower = new BlockModelRenderState();
        final BlockModelRenderState upper = new BlockModelRenderState();
        boolean tall;
        float x, z, yaw;
    }

    static final class Renderer implements BlockEntityRenderer<PlanterBlockEntity, State> {
        private final BlockModelResolver models;

        Renderer(BlockEntityRendererProvider.Context context) {
            this.models = context.blockModelResolver();
        }

        @Override
        public State createRenderState() {
            return new State();
        }

        @Override
        public void extractRenderState(PlanterBlockEntity planter, State state, float partialTicks, Vec3 cameraPosition,
                                       ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
            BlockEntityRenderState.extractBase(planter, state, breakProgress);
            BlockState block = planter.getBlockState();
            // As the blockstate turns the model: north as drawn, east a quarter turn clockwise, and on round.
            state.turn = block.hasProperty(HorizontalDirectionalBlock.FACING)
                    ? (block.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180.0F) % 360.0F : 0.0F;
            List<PlanterBlockEntity.Flower> flowers = planter.flowers();
            state.count = flowers.size();
            while (state.planted.size() < state.count) state.planted.add(new Planted());
            for (int i = 0; i < state.count; i++) {
                PlanterBlockEntity.Flower flower = flowers.get(i);
                Planted planted = state.planted.get(i);
                BlockState lower = flower.block().defaultBlockState();
                planted.tall = lower.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF);
                if (planted.tall) {
                    lower = lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
                    this.models.update(planted.upper, lower.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), DISPLAY);
                }
                this.models.update(planted.lower, lower, DISPLAY);
                planted.x = flower.x() / 32.0F;
                planted.z = flower.z() / 32.0F;
                planted.yaw = flower.yaw();
            }
        }

        @Override
        public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
            for (int i = 0; i < state.count; i++) {
                Planted planted = state.planted.get(i);
                poseStack.pushPose();
                poseStack.translate(0.5F, 0.0F, 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(-state.turn));
                poseStack.translate(planted.x - 0.5F, SOIL, planted.z - 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(planted.yaw));
                poseStack.scale(SCALE, SCALE, SCALE);
                poseStack.translate(-0.5F, 0.0F, -0.5F);
                planted.lower.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                if (planted.tall) {
                    poseStack.translate(0.0F, 1.0F, 0.0F);
                    planted.upper.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                }
                poseStack.popPose();
            }
        }
    }
}
