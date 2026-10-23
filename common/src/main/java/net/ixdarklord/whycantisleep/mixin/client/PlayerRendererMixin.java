package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.HidingClient;
import net.ixdarklord.whycantisleep.hiding.Hiding;
import net.ixdarklord.whycantisleep.hiding.HidingView;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A hiding player is drawn where they hide: lying on the floor under a bed or table rather than hanging against its
 * underside, and with monsters about ({@link HidingClient#isTrembling}) trembling. How they are posed is
 * {@code HumanoidModelMixin}'s business, and a player crouched in a chest is drawn by {@code LivingEntityRendererMixin}.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    // The crawling pose is drawn for a crawler's full height; under a bed it lies this much lower, on the floor.
    private static final double WHYCANTISLEEP$FLOOR_DROP = 0.18;
    // The tremble: a few hundredths of a block, quick and uneven.
    private static final double WHYCANTISLEEP$TREMBLE = 0.012;

    @Inject(method = "getRenderOffset(Lnet/minecraft/client/player/AbstractClientPlayer;F)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"),
            cancellable = true)
    private void whycantisleep$onTheFloor(AbstractClientPlayer player, float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        HidingView view = Hiding.view(player);
        if (view == null) return;
        double x = 0.0;
        double z = 0.0;
        if (HidingClient.isTrembling(player.getId())) {
            // Two quick waves out of step, so it shivers rather than sways.
            float age = player.tickCount + partialTicks;
            x = Mth.sin(age * 2.9F) * WHYCANTISLEEP$TREMBLE;
            z = Mth.cos(age * 3.7F) * WHYCANTISLEEP$TREMBLE;
        }
        double drop = view.lying() ? WHYCANTISLEEP$FLOOR_DROP * player.getScale() : 0.0;
        cir.setReturnValue(cir.getReturnValue().add(x, -drop, z));
    }
}
