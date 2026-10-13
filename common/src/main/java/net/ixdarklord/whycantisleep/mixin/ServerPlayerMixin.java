package net.ixdarklord.whycantisleep.mixin;

import com.mojang.datafixers.util.Either;
import net.ixdarklord.whycantisleep.lurker.LurkerServer;
import net.ixdarklord.whycantisleep.mind.MindServer;
import net.ixdarklord.whycantisleep.sleep.SleepServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * What the server needs to hear from a player that CoolCatLib: Core has no event for: why a sleep failed (the game's own
 * answer), waking after a night's sleep, being hurt and dying.
 */
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

    @Inject(method = "stopSleepInBed", at = @At("HEAD"))
    private void whycantisleep$onWake(boolean forcefulWakeUp, boolean updateLevelList, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.isSleepingLongEnough()) MindServer.onRested(player);
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void whycantisleep$onHurt(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) MindServer.onHurt((ServerPlayer) (Object) this, source, damage);
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void whycantisleep$onDeath(DamageSource source, CallbackInfo ci) {
        MindServer.onPlayerDied((ServerPlayer) (Object) this, source);
    }
}
