package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.SleepClient;
import net.ixdarklord.whycantisleep.deep.client.GardenWardClient;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Notes which bed the player tried to sleep in, so a failed sleep can be checked around it, and holds back a bed that would explode. */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$onUseBlock(LocalPlayer player, InteractionHand hand, BlockHitResult blockHit, CallbackInfoReturnable<InteractionResult> cir) {
        if (SleepClient.onUseBlock(player, blockHit.getBlockPos())) cir.setReturnValue(InteractionResult.FAIL);
    }

    // A block the Garden's ward holds is not cracked: the blow rings off it (GardenWardClient).
    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$wardHolds(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (GardenWardClient.held(pos, face)) cir.setReturnValue(false);
    }
}
