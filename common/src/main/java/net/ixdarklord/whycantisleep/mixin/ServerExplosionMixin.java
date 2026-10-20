package net.ixdarklord.whycantisleep.mixin;

import net.ixdarklord.whycantisleep.between.GardenWard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/** The Garden's ward: no explosion breaks a block it holds (GardenWard). */
@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
    @Shadow @Final private ServerLevel level;

    @Inject(method = "calculateExplodedPositions", at = @At("RETURN"), cancellable = true)
    private void whycantisleep$ward(CallbackInfoReturnable<List<BlockPos>> cir) {
        List<BlockPos> spared = GardenWard.spare(this.level, cir.getReturnValue());
        if (spared != cir.getReturnValue()) cir.setReturnValue(new ArrayList<>(spared));
    }
}
