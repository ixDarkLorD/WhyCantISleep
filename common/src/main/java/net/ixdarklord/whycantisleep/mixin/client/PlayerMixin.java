package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.deep.client.DreamTransition;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lays the dreamer out flat while they fall into the Lucid Deep, lie on the island and push themselves up off it (the
 * crawling pose, which out of water is the body face down), then crouches them as they come up on to their feet
 * ({@link DreamTransition}). Only for the player of this client, and only on it.
 */
@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
    private PlayerMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$lieFaceDown(CallbackInfo ci) {
        if (!this.level().isClientSide() || (Entity) this != Minecraft.getInstance().player) return;
        if (DreamTransition.isLyingFaceDown()) {
            this.setPose(Pose.SWIMMING);
            ci.cancel();
        } else if (DreamTransition.isCrouchingUp()) {
            this.setPose(Pose.CROUCHING);
            ci.cancel();
        }
    }
}
