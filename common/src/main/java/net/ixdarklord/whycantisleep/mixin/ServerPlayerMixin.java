package net.ixdarklord.whycantisleep.mixin;

import com.mojang.datafixers.util.Either;
import net.ixdarklord.whycantisleep.lurker.LurkerServer;
import net.ixdarklord.whycantisleep.sleep.SleepServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Learns why a sleep failed, from the game's own answer (CoolCatLib: Core has no sleep event). */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "startSleepInBed", at = @At("RETURN"))
    private void whycantisleep$onSleepResult(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        cir.getReturnValue().ifRight(unit -> LurkerServer.onSlept((ServerPlayer) (Object) this, pos)).ifLeft(problem -> {
            ServerPlayer player = (ServerPlayer) (Object) this;
            if (problem == Player.BedSleepingProblem.NOT_SAFE) SleepServer.onBlockedByMonsters(player, pos);
            // "Other problem" is a player already asleep or dead, not a sleep that failed.
            if (problem != Player.BedSleepingProblem.OTHER_PROBLEM) SleepServer.onSleepFailed(player, pos);
        });
    }
}
