package net.ixdarklord.whycantisleep.deep.client;

import net.ixdarklord.coolcatcanvas.api.armature.Motion;
import net.ixdarklord.coolcatcanvas.api.armature.Transition;
import net.ixdarklord.coolcatcanvas.api.client.armature.Armature;
import net.ixdarklord.coolcatcanvas.api.client.armature.ArmatureContext;
import net.ixdarklord.coolcatcanvas.api.client.armature.vanilla.VanillaModels;
import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * The dreamer's body as they fall into the Lucid Deep and get up again ({@link DreamTransition}), played on the vanilla
 * player model with CoolCatLib's Armature system, on their own client only. The clips are in
 * {@code armatures/dreamer.animation.json} (written by {@code tools/gen_dreamer_anim.py}): {@code fall}, flailing like a
 * skydiver; {@code sprawl}, flat where they landed; {@code get_up}, pushing up and rising out of a crouch. Each moves
 * only the limbs and head: {@code PlayerMixin} lies them face down and crouches them, as the game's own poses. Between
 * clips, and back to the game's own animation after, they cross-fade.
 */
final class DreamerAnimations {
    private static final String LAYER = "dream";
    private static final Motion FALL = Motion.loop("fall");
    private static final Motion SPRAWL = Motion.loop("sprawl");
    private static final Motion GET_UP = Motion.hold("get_up");

    private DreamerAnimations() {
    }

    static void init() {
        VanillaModels.player()
                .animations(Armature.of(WhyCantISleep.id("dreamer")))
                .animate(animator -> animator.layer(LAYER, layer -> layer
                        .transition(Transition.crossfade(0.25F, Easing.SINE_IN_OUT))
                        .drive(DreamerAnimations::motion)));
    }

    // Asked every frame for each player drawn: what the dreamer's body is doing now, or nothing (the game's own).
    private static @Nullable Motion motion(ArmatureContext context) {
        if (context.entity() == null || context.entity() != Minecraft.getInstance().player) return null;
        if (DreamTransition.isFalling()) return FALL;
        if (DreamTransition.isSprawled()) return SPRAWL;
        if (DreamTransition.isRising()) return GET_UP;
        return null;
    }
}
