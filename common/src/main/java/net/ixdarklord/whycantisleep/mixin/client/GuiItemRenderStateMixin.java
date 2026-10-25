package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.hud.HudFade;
import net.ixdarklord.whycantisleep.client.hud.ItemFade;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Each GUI item remembers the opacity it was drawn at ({@link ItemFade}), for {@code GuiRendererMixin} to blit it with. */
@Mixin(GuiItemRenderState.class)
public abstract class GuiItemRenderStateMixin implements ItemFade.Faded {
    @Unique
    private float whycantisleep$alpha = 1.0F;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void whycantisleep$rememberAlpha(CallbackInfo ci) {
        this.whycantisleep$alpha = ItemFade.alpha * HudFade.factor();
    }

    @Override
    public float whycantisleep$alpha() {
        return this.whycantisleep$alpha;
    }
}
