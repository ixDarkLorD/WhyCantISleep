package net.ixdarklord.whycantisleep.mixin;

import com.mojang.datafixers.util.Either;
import net.ixdarklord.whycantisleep.sleep.SleepChecks;
import net.ixdarklord.whycantisleep.sleep.SleepNights;
import net.ixdarklord.whycantisleep.sleep.SleepServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;

import java.util.List;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hears how a try to sleep went (the game's own answer, which CoolCatLib has no event for), and when a sleeper wakes:
 * every way of waking up passes through {@code stopSleepInBed}.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    // The bed of the try to sleep under way, for the monster check's rules (which may run inside a lambda of NeoForge's).
    @Unique
    private @Nullable BlockPos whycantisleep$bed;

    @Inject(method = "startSleepInBed", at = @At("HEAD"))
    private void whycantisleep$beforeSleep(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        this.whycantisleep$bed = pos;
        // Lying down wipes the time since rest; how badly they had been sleeping is taken first.
        SleepNights.beforeSleep((ServerPlayer) (Object) this);
    }

    @Inject(method = "startSleepInBed", at = @At("RETURN"))
    private void whycantisleep$onSleepResult(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        cir.getReturnValue().ifRight(unit -> SleepServer.onLayDown(player, pos)).ifLeft(problem -> SleepServer.onSleepFailed(player, pos, problem));
    }

    // The mod's rules over the game's monster check: the tags, and a shared bed's tolerance (SleepChecks). NeoForge
    // moves the game's own checks into a lambda of startSleepInBed (their answer goes through its sleeping event), so
    // the monster list is stored there; the lambda is matched by pattern, as its number and arguments change between
    // versions, and only the method that stores the list is changed. The bed comes from the head of startSleepInBed.
    @ModifyVariable(method = {"startSleepInBed", "/^lambda\\$startSleepInBed\\$\\d+$/"}, at = @At("STORE"), ordinal = 0)
    private List<Monster> whycantisleep$sleepRules(List<Monster> monsters) {
        BlockPos pos = this.whycantisleep$bed;
        if (pos == null) return monsters;
        @SuppressWarnings("unchecked")
        List<Monster> ruled = (List<Monster>) (List<?>) SleepChecks.applyRules((ServerPlayer) (Object) this, pos, monsters);
        return ruled;
    }

    @Inject(method = "stopSleepInBed", at = @At("HEAD"))
    private void whycantisleep$onWake(boolean forcefulWakeUp, boolean updateLevelList, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.isSleeping()) SleepNights.onWake(player, forcefulWakeUp);
    }
}
