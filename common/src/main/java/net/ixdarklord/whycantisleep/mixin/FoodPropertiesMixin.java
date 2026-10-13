package net.ixdarklord.whycantisleep.mixin;

import net.ixdarklord.whycantisleep.mind.MindServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hears of a player eating, and what: variety in what they eat is part of their mood. */
@Mixin(FoodProperties.class)
public abstract class FoodPropertiesMixin {
    @Inject(method = "onConsume", at = @At("HEAD"))
    private void whycantisleep$onEaten(Level level, LivingEntity user, ItemStack stack, Consumable consumable, CallbackInfo ci) {
        if (user instanceof ServerPlayer player) MindServer.onAte(player, stack);
    }
}