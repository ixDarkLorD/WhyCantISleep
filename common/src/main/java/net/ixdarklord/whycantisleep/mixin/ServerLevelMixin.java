package net.ixdarklord.whycantisleep.mixin;

import net.ixdarklord.whycantisleep.sleep.SleepNights;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Knows a night skipped by sleeping from any other waking: the game wakes all its sleepers at once for it. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "wakeUpAllPlayers", at = @At("HEAD"))
    private void whycantisleep$onNightSkipped(CallbackInfo ci) {
        SleepNights.onNightSkipped((ServerLevel) (Object) this);
    }
}
