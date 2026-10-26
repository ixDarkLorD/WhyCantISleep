package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.hud.HudFade;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fades each line of text the HUD draws, as it is made ({@link HudFade}). */
@Mixin(GuiTextRenderState.class)
public abstract class GuiTextRenderStateMixin {
    @Shadow @Final @Mutable
    public int color;
    @Shadow @Final @Mutable
    public int backgroundColor;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void whycantisleep$fade(CallbackInfo ci) {
        this.color = HudFade.fade(this.color, true);
        this.backgroundColor = HudFade.fade(this.backgroundColor, false);
    }
}
