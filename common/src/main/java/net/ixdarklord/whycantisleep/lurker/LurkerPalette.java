package net.ixdarklord.whycantisleep.lurker;

/**
 * The Lurker's colors, from the author's reference: a sunset burning red under a sky already gone dark. Everything of
 * the Lurker's draws from these five: its skin and what glows on it ({@code tools/gen_lurker_textures.ps1} has the
 * same values), its particles, and the tints of the {@code dread} screen effect ({@code shaders/post/dread.fsh}).
 * Each is {@code 0xRRGGBB}, darkest first.
 */
public final class LurkerPalette {
    /** The treeline against the sky: its hide, and the dark it brings. */
    public static final int ABYSS = 0x18181A;
    /** The sky above the fire: shadow with color in it, and its horns and claws. */
    public static final int NAVY = 0x1F2D4A;
    /** Cloud with the light gone out of it: its veins. */
    public static final int MAROON = 0x5A2433;
    /** Cloud lit from below: what glows under its skin. */
    public static final int CRIMSON = 0x8A0A24;
    /** The fire itself: its eyes, sparks, the hottest light. */
    public static final int RED = 0xFF2D46;

    private LurkerPalette() {
    }
}