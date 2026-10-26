package net.ixdarklord.whycantisleep.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ixdarklord.whycantisleep.client.HidingClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Seen from inside, crouched in a chest in first person, the chest has no latch: it hangs off the lid's front, right
 * in the gap the player peers out of, and covered part of the view. Everyone else, and the player in third person,
 * sees it as ever. On 1.21.1 one renderer's latch parts serve every chest, so they are hidden for that chest's draw only.
 */
@Mixin(ChestRenderer.class)
public abstract class ChestRendererMixin {
    @Shadow
    @Final
    private ModelPart lock;

    @Shadow
    @Final
    private ModelPart doubleLeftLock;

    @Shadow
    @Final
    private ModelPart doubleRightLock;

    @Inject(method = "render", at = @At("HEAD"))
    private void whycantisleep$noLatchFromInside(BlockEntity chest, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light,
                                                 int overlay, CallbackInfo ci) {
        boolean shown = !whycantisleep$seenFromInside(chest);
        this.lock.visible = shown;
        this.doubleLeftLock.visible = shown;
        this.doubleRightLock.visible = shown;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void whycantisleep$latchBack(BlockEntity chest, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay,
                                         CallbackInfo ci) {
        this.lock.visible = true;
        this.doubleLeftLock.visible = true;
        this.doubleRightLock.visible = true;
    }

    // Whether this chest (either half of a double one) is the one the local player is crouched in, seen from their eyes.
    @Unique
    private static boolean whycantisleep$seenFromInside(BlockEntity chest) {
        BlockPos in = HidingClient.localChest();
        if (in == null || chest.getLevel() == null || !Minecraft.getInstance().options.getCameraType().isFirstPerson()) return false;
        BlockPos at = chest.getBlockPos();
        if (in.equals(at)) return true;
        BlockState state = chest.getBlockState();
        return state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                && in.equals(at.relative(ChestBlock.getConnectedDirection(state)));
    }
}
