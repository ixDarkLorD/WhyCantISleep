package net.ixdarklord.whycantisleep.client.terror;

import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxLayer;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.resources.Identifier;

/**
 * The textures and draw layers the terror scenes share (the pixel art from {@code tools/gen_terror.py} and
 * {@code tools/gen_nightmares.py}). World effects write no depth, so the layers say what covers what: what stands in
 * the room first, the hands over it, sorted among themselves by distance, and what is laid over the eyes last.
 */
public final class TerrorArt {
    public static final int HAND_FRAMES = 5;
    public static final VfxMaterial[] HAND_VARIANTS = hands("splayed", "grasping", "beckoning", "skeletal", "gnarled");
    public static final VfxMaterial ARM_UPPER = pixels("textures/vfx/terror_arm_upper.png");
    public static final VfxMaterial ARM_LOWER = pixels("textures/vfx/terror_arm_lower.png");
    public static final VfxMaterial FOG = VfxMaterial.alpha(WhyCantISleep.id("textures/vfx/terror_fog.png"));
    public static final VfxMaterial GLOW = VfxMaterial.additive(VfxMaterial.GLOW);
    public static final VfxLayer FIGURES = VfxLayer.of(10);
    public static final VfxLayer HANDS = VfxLayer.sorted(11);
    public static final VfxLayer OVER_EYES = VfxLayer.sorted(12);

    private TerrorArt() {
    }

    /** A pixel-art texture of this mod's, drawn painted over the scene, unsmoothed, its edge a little soft. */
    public static VfxMaterial pixels(String path) {
        return pixels(WhyCantISleep.id(path));
    }

    public static VfxMaterial pixels(Identifier texture) {
        return VfxMaterial.alpha(texture).withSmoothing(false).withSoftness(0.1F);
    }

    private static VfxMaterial[] hands(String... names) {
        VfxMaterial[] materials = new VfxMaterial[names.length];
        for (int i = 0; i < names.length; i++) materials[i] = pixels("textures/vfx/terror_hand_" + names[i] + ".png");
        return materials;
    }
}
