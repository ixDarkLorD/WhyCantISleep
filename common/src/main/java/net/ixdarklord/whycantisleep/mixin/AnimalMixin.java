package net.ixdarklord.whycantisleep.mixin;

import net.ixdarklord.whycantisleep.mind.MindServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hears of a player feeding an animal: the game puts the animal in love mode with the player who fed it. */
@Mixin(Animal.class)
public abstract class AnimalMixin {
    @Inject(method = "setInLove", at = @At("HEAD"))
    private void whycantisleep$onFed(@Nullable Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) MindServer.onFedAnimal(serverPlayer);
    }
}