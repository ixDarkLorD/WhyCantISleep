package net.ixdarklord.whycantisleep.client.terror;

import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.RandomSource;

/**
 * What one kind of night terror shows ({@code sleep/NightmareKind}): its sights in the world, its sounds, its fog and
 * whatever it puts over the view. {@code NightTerrorClient} runs what every terror shares (the eyes stuck half open,
 * the heartbeat, the drone, the muffled world, the warp, the dark closing in, the stare and the jolt) and hands the
 * rest to a fresh scene for each terror, on the same {@link TerrorStage} timeline.
 */
public interface TerrorScene {
    /** The terror is laid out round the bed (or ahead of where they look, awake): place what it needs. */
    void lay(TerrorStage stage, RandomSource random);

    /** Each tick while it runs, this many ticks in (t the share of its length): its sounds and timing. */
    void tick(Minecraft minecraft, TerrorStage stage, long age, float t);

    /** Its sights in the world, this far in (with the partial tick). */
    void renderWorld(VfxFrame frame, TerrorStage stage, float age, float t);

    /** Anything over the view, under the dark closing in. */
    default void renderHud(GuiGraphicsExtractor graphics, TerrorStage stage, float age, float t) {
    }

    /** The fog's color (RGB) this far in. */
    int fogColor(TerrorStage stage, float t);

    /** How far the fog reaches this far in, given the usual terror's rolling reach. */
    default float fogEnd(TerrorStage stage, float t, float usual) {
        return usual;
    }

    /** The banks of fog rolling through the room: their color (RGB), or -1 for none this far in. */
    int mistColor(TerrorStage stage, float t);

    /** How strongly the view is warped, 0 to about 1.2, from how bad it is. */
    default float warp(TerrorStage stage) {
        return 0.6F + 0.6F * stage.intensity;
    }

    /** It ended, however: let go of anything still playing. */
    default void stop(Minecraft minecraft) {
    }
}
