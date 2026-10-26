package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.HidingClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.MultiblockChestResources;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.ChestType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Seen from inside, crouched in a chest in first person, the chest has no latch: it hangs off the lid's front, right
 * in the gap the player peers out of, and covered part of the view. Everyone else, and the player in third person,
 * sees it as ever.
 */
@Mixin(ChestRenderer.class)
public abstract class ChestRendererMixin {
    @Unique
    private MultiblockChestResources<ChestModel> whycantisleep$latchless;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void whycantisleep$bakeLatchless(BlockEntityRendererProvider.Context context, CallbackInfo ci) {
        this.whycantisleep$latchless = ChestRenderer.LAYERS.map(layer -> {
            ChestModel model = new ChestModel(context.bakeLayer(layer));
            model.root().getChild("lock").visible = false;
            return model;
        });
    }

    @ModifyVariable(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("STORE"), ordinal = 0)
    private ChestModel whycantisleep$noLatchFromInside(ChestModel model, ChestRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                                                       CameraRenderState camera) {
        return whycantisleep$seenFromInside(state) ? this.whycantisleep$latchless.select(state.type) : model;
    }

    // Whether this chest (either half of a double one) is the one the local player is crouched in, seen from their eyes.
    @Unique
    private static boolean whycantisleep$seenFromInside(ChestRenderState state) {
        BlockPos in = HidingClient.localChest();
        if (in == null || !Minecraft.getInstance().options.getCameraType().isFirstPerson()) return false;
        if (in.equals(state.blockPos)) return true;
        // The other half of a double chest, where ChestBlock.getConnectedDirection finds it.
        return state.type != ChestType.SINGLE
                && in.equals(state.blockPos.relative(state.type == ChestType.LEFT ? state.facing.getClockWise() : state.facing.getCounterClockWise()));
    }
}
