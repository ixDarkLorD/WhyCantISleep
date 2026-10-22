package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.HidingClient;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A chest someone hides in has its lid raised a crack, for them to peer out of ({@link HidingClient#lidAjar}). The
 * game draws a double chest's lid as open as its more open half, so the half the player went in by is enough.
 */
@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin {
    @Inject(method = "getOpenNess", at = @At("RETURN"), cancellable = true)
    private void whycantisleep$ajarWithSomeoneIn(float partialTicks, CallbackInfoReturnable<Float> cir) {
        ChestBlockEntity self = (ChestBlockEntity) (Object) this;
        if (self.getLevel() == null || !self.getLevel().isClientSide()) return;
        float ajar = HidingClient.lidAjar(self.getBlockPos(), partialTicks);
        if (ajar > cir.getReturnValueF()) cir.setReturnValue(ajar);
    }
}
