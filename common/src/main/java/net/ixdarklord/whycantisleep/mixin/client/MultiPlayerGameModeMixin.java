package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.HidingClient;
import net.ixdarklord.whycantisleep.client.SleepClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Notes which bed the player tried to sleep in, so a failed sleep can be checked around it, and holds back a bed that
 * would explode. A hiding player mines nothing: neither the first hit nor holding the button on (which only the first
 * hit's event sees), so the spot can't be broken from inside (the server refuses it too).
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$onUseBlock(LocalPlayer player, InteractionHand hand, BlockHitResult blockHit, CallbackInfoReturnable<InteractionResult> cir) {
        if (SleepClient.onUseBlock(player, blockHit.getBlockPos())) cir.setReturnValue(InteractionResult.FAIL);
    }

    @Inject(method = {"startDestroyBlock", "continueDestroyBlock"}, at = @At("HEAD"), cancellable = true)
    private void whycantisleep$noMiningWhileHiding(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (HidingClient.isLocalHiding()) cir.setReturnValue(false);
    }
}
