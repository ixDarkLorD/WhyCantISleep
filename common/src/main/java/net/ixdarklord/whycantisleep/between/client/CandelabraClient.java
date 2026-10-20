package net.ixdarklord.whycantisleep.between.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ixdarklord.coolcatcore.api.client.registry.BlockEntityRendererRegistry;
import net.ixdarklord.whycantisleep.between.CandelabraBlock;
import net.ixdarklord.whycantisleep.between.CandelabraBlockEntity;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The candles in a dread candelabra ({@link CandelabraBlockEntity}), each drawn from its own lit model (a single candle
 * of its colour) standing on its slot's tray, turned with the candelabra; the stand is the chunk's model, the flames
 * the block's own particles, and their glow FlameBloomClient's (each candle noted here as it is drawn).
 */
public final class CandelabraClient {
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();

    private CandelabraClient() {
    }

    public static void init() {
        BlockEntityRendererRegistry.register(WCISBlocks.CANDELABRA_ENTITY, Renderer::new);
    }

    static final class State extends BlockEntityRenderState {
        final List<Placed> placed = new ArrayList<>();
        int count;
        float turn;
    }

    static final class Placed {
        final BlockModelRenderState model = new BlockModelRenderState();
        Vec3 slot = Vec3.ZERO;
    }

    static final class Renderer implements BlockEntityRenderer<CandelabraBlockEntity, State> {
        private final BlockModelResolver models;

        Renderer(BlockEntityRendererProvider.Context context) {
            this.models = context.blockModelResolver();
        }

        @Override
        public State createRenderState() {
            return new State();
        }

        @Override
        public void extractRenderState(CandelabraBlockEntity candelabra, State state, float partialTicks, Vec3 cameraPosition,
                                       ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
            BlockEntityRenderState.extractBase(candelabra, state, breakProgress);
            BlockState block = candelabra.getBlockState();
            state.count = 0;
            if (!(block.getBlock() instanceof CandelabraBlock stand)) return;
            // As the blockstate turns the model: north as drawn, east a quarter turn clockwise, and on round.
            state.turn = (block.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180.0F) % 360.0F;
            List<Block> candles = candelabra.candles();
            for (int i = 0; i < candles.size() && i < stand.slots(block).size(); i++) {
                Block candle = candles.get(i);
                if (candle == Blocks.AIR) continue;
                if (state.placed.size() <= state.count) state.placed.add(new Placed());
                Placed placed = state.placed.get(state.count++);
                BlockState lit = candle.defaultBlockState();
                if (lit.hasProperty(BlockStateProperties.LIT)) lit = lit.setValue(BlockStateProperties.LIT, true);
                this.models.update(placed.model, lit, DISPLAY);
                placed.slot = stand.slots(block).get(i);
                // Its flame blooms as a lantern's does, small and warm (FlameBloomClient), over its wick.
                FlameBloomClient.candle(CandelabraBlock.slotInWorld(placed.slot, block.getValue(HorizontalDirectionalBlock.FACING),
                        candelabra.getBlockPos()).add(0.0, 7.6 / 16.0, 0.0));
            }
        }

        @Override
        public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
            for (int i = 0; i < state.count; i++) {
                Placed placed = state.placed.get(i);
                poseStack.pushPose();
                poseStack.translate(0.5F, 0.0F, 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(-state.turn));
                // A single candle's model stands in the middle of its block, so it is moved by its slot's offset.
                poseStack.translate((float) (placed.slot.x / 16.0 - 0.5) - 0.5F, (float) (placed.slot.y / 16.0),
                        (float) (placed.slot.z / 16.0 - 0.5) - 0.5F);
                placed.model.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
        }
    }
}
