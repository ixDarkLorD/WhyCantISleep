package net.ixdarklord.whycantisleep.lurker;

/**
 * The Lurker's colors, from the author's "Electric Feel" reference: a storm at night lit violet from inside. Everything
 * of the Lurker's draws from these six: its skin and what glows on it ({@code tools/gen_lurker_textures.ps1} has the
 * same values), its particles, and the tints of the {@code dread} screen effect ({@code shaders/post/dread.fsh}).
 * Each is {@code 0xRRGGBB}, darkest first.
 */
public final class LurkerPalette {
    /** The night sky: its hide, and the dark it brings. */
    public static final int ABYSS = 0x14181C;
    /** Storm cloud: shadow with color in it. */
    public static final int PLUM = 0x3A2A3E;
    /** The cloud lit from within: its veins. */
    public static final int VIOLET = 0x663A63;
    public static final int MAUVE = 0xA86488;
    /** The glow around the bolt: its eyes. */
    public static final int ROSE = 0xCB8CAA;
    /** The bolt itself: sparks, teeth, the hottest light. */
    public static final int BLUSH = 0xE9B6B8;

    private LurkerPalette() {
    }
}
